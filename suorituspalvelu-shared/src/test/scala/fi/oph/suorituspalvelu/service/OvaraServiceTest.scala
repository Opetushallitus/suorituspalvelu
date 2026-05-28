package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, KKOpiskeluoikeusTila, KantaOperaatiot, Koodi, Lahdejarjestelma, Opiskeluoikeus, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, AtaruValintalaskentaHakemus, HakemuspalveluClient, Hakutoive, KoutaHaku, KoutaHakuaika, SiirtotiedostoClient}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, ConvertedAtaruHakemus, EnsikertalaisuusConstants, EnsikertalaisuusTulos, HakemuksenHarkinnanvaraisuus, HakutoiveenHarkinnanvaraisuus, HarkinnanvaraisuudenSyy, MenettamisenPeruste}
import fi.oph.suorituspalvelu.parsing.OpiskeluoikeusParsingService
import org.junit.jupiter.api.{Assertions, Test}
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.ArgumentMatchers.{any, anyBoolean}

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

@Test
@TestInstance(Lifecycle.PER_CLASS)
class OvaraServiceTest {

  val HAKEMUS_OID   = "1.2.246.562.11.00000000000000006321"
  val HENKILO_OID   = "1.2.246.562.24.10000000001"
  val KK_HAKU_OID   = "1.2.246.562.29.00000000000000000001"
  val TA_HAKU_OID   = "1.2.246.562.29.00000000000000000002"
  val HAKUKOHDE_OID = "1.2.246.562.20.00000000000000000001"
  val EXECUTION_ID  = "test-execution-id"

  val KK_HAKU = KoutaHaku(
    oid = KK_HAKU_OID,
    tila = "julkaistu",
    nimi = Map("fi" -> "KK-testihaku"),
    hakutapaKoodiUri = "hakutapa_01",
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_12#1"),
    hakuajat = List(KoutaHakuaika("2025-01-15T08:00:00", Some("2025-03-31T15:00:00"))),
    kohdejoukonTarkenneKoodiUri = None,
    hakuvuosi = Some(2025)
  )

  val TOISEN_ASTEEN_YHTEISHAKU = KoutaHaku(
    oid = TA_HAKU_OID,
    tila = "julkaistu",
    nimi = Map("fi" -> "Toisen asteen yhteishaku"),
    hakutapaKoodiUri = "hakutapa_01",
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_11#1"),
    hakuajat = List.empty,
    kohdejoukonTarkenneKoodiUri = None,
    hakuvuosi = Some(2025)
  )

  val BASE_HAKIJA = AtaruHakemuksenHenkilotiedot(
    oid = HAKEMUS_OID,
    personOid = Some(HENKILO_OID),
    ssn = None
  )

  val BASE_VALINTALASKENTA_HAKEMUS = AtaruValintalaskentaHakemus(
    hakemusOid = HAKEMUS_OID,
    personOid = HENKILO_OID,
    hakuOid = KK_HAKU_OID,
    asiointikieli = "fi",
    hakutoiveet = List(
      Hakutoive(
        processingState = "unprocessed",
        eligibilityState = "eligible",
        paymentObligation = "not-obligated",
        kkApplicationPaymentObligation = "unreviewed",
        hakukohdeOid = HAKUKOHDE_OID,
        languageRequirement = "unreviewed",
        degreeRequirement = "unreviewed",
        harkinnanvaraisuus = None
      )
    ),
    maksuvelvollisuus = Map.empty,
    keyValues = Map.empty,
    korkeakoulututkintoVuosi = None
  )

  val BASE_CONVERTED_HAKEMUS = ConvertedAtaruHakemus(
    hakemusOid = HAKEMUS_OID,
    hakutoiveet = List.empty,
    avainArvot = Set.empty
  )

  private def baseValintaData(
    avainArvot: Seq[CombinedAvainArvoContainer] = Seq.empty,
    harkinnanvaraisuudet: Option[HakemuksenHarkinnanvaraisuus] = None,
    ensikertalaisuus: Option[EnsikertalaisuusTulos] = None,
  ) = ValintaData(
    personOid = HENKILO_OID,
    paatellytAvainArvot = avainArvot,
    avainArvoMetadatat = Seq.empty,
    hakemus = Some(BASE_CONVERTED_HAKEMUS),
    opiskeluoikeudet = Seq.empty,
    vahvistettuViimeistaan = LocalDate.of(2025, 3, 31),
    laskennanAlkaminen = Instant.now(),
    harkinnanvaraisuudet = harkinnanvaraisuudet,
    ensikertalaisuus = ensikertalaisuus
  )

  private def ensikertalaisuusAvainArvo(ensikertalainen: Boolean, selite: Option[String] = None) =
    CombinedAvainArvoContainer(
      avain = AvainArvoConstants.ensikertalainenKey,
      arvo = ensikertalainen.toString,
      metadata = AvainArvoMetadata(
        selitteet = selite.toSeq,
        arvoEnnenYliajoa = None,
        yliajo = None,
        arvoOnHakemukselta = false
      )
    )

  private def buildService(): (OvaraService, ValintaDataService, HakemuspalveluClient, OnrIntegration) = {
    val mockValintaDataService   = Mockito.mock(classOf[ValintaDataService])
    val mockSiirtotiedostoClient = Mockito.mock(classOf[SiirtotiedostoClient])
    val mockHakemuspalveluClient = Mockito.mock(classOf[HakemuspalveluClient])
    val mockOnrIntegration       = Mockito.mock(classOf[OnrIntegration])

    val service = new OvaraService(500, 500) {
      override val valintaDataService: ValintaDataService     = mockValintaDataService
      override val siirtotiedostoClient: SiirtotiedostoClient = mockSiirtotiedostoClient
      override val hakemuspalveluClient: HakemuspalveluClient = mockHakemuspalveluClient
      override val onrIntegration: OnrIntegration             = mockOnrIntegration
    }

    (service, mockValintaDataService, mockHakemuspalveluClient, mockOnrIntegration)
  }

  private def setupMocks(
    hakemuspalveluClient: HakemuspalveluClient,
    onrIntegration: OnrIntegration,
    valintaDataService: ValintaDataService,
    haku: KoutaHaku,
    valintaData: ValintaData
  ): Unit = {
    Mockito.when(hakemuspalveluClient.getHaunHakijat(haku.oid))
      .thenReturn(Future.successful(Seq(BASE_HAKIJA)))
    Mockito.when(onrIntegration.getAliasesForPersonOids(any()))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(HENKILO_OID -> Set(HENKILO_OID)))))
    Mockito.when(valintaDataService.fetchValintalaskentaHakemukset(any(), any(), any()))
      .thenReturn(Future.successful(Seq(BASE_VALINTALASKENTA_HAKEMUS)))
    Mockito.when(valintaDataService.doAvainArvoConversions(any(), any(), any(), any()))
      .thenReturn(valintaData)
  }

  // tiedostoNumero alkaa arvosta 1 ja kasvaa aina kun ei-tyhjä tiedosto tallennetaan.
  // Arvo 1 tarkoittaa ettei tiedostoa tallennettu; arvo 2 tarkoittaa että yksi tiedosto tallennettiin.

  @Test def testValintadataTallennetaan(): Unit = {
    val (service, valintaDataService, hakemuspalveluClient, onrIntegration) = buildService()
    setupMocks(hakemuspalveluClient, onrIntegration, valintaDataService, KK_HAKU, baseValintaData())

    val tila = Await.result(service.kasitteleHaku(KK_HAKU, OvaraParams(executionId = EXECUTION_ID)), 30.seconds)

    Assertions.assertEquals(1, tila.onnistuneet)
    Assertions.assertTrue(tila.epaonnistuneet.isEmpty)
    Assertions.assertEquals(2, tila.valintaDataTiedostoNumero)
  }

  @Test def testHarkinnanvaraisuudetTallennetaanYhteisHaulle(): Unit = {
    val (service, valintaDataService, hakemuspalveluClient, onrIntegration) = buildService()
    val harkinnanvaraisuus = HakemuksenHarkinnanvaraisuus(
      hakemusOid = HAKEMUS_OID,
      henkiloOid = HENKILO_OID,
      hakutoiveet = List(HakutoiveenHarkinnanvaraisuus(HAKUKOHDE_OID, HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA))
    )
    setupMocks(hakemuspalveluClient, onrIntegration, valintaDataService, TOISEN_ASTEEN_YHTEISHAKU,
      baseValintaData(harkinnanvaraisuudet = Some(harkinnanvaraisuus)))

    val tila = Await.result(service.kasitteleHaku(TOISEN_ASTEEN_YHTEISHAKU, OvaraParams(executionId = EXECUTION_ID)), 30.seconds)

    Assertions.assertEquals(2, tila.harkinnanvaraisuusTiedostoNumero)
  }

  @Test def testHarkinnanvaraisuudetEiTallennetaIlmanDataa(): Unit = {
    val (service, valintaDataService, hakemuspalveluClient, onrIntegration) = buildService()
    setupMocks(hakemuspalveluClient, onrIntegration, valintaDataService, TOISEN_ASTEEN_YHTEISHAKU, baseValintaData())

    val tila = Await.result(service.kasitteleHaku(TOISEN_ASTEEN_YHTEISHAKU, OvaraParams(executionId = EXECUTION_ID)), 30.seconds)

    Assertions.assertEquals(1, tila.harkinnanvaraisuusTiedostoNumero)
  }

  @Test def testEnsikertalaisuudetTallennetaanKKHaulle(): Unit = {
    val (service, valintaDataService, hakemuspalveluClient, onrIntegration) = buildService()
    val valintaData = baseValintaData(
      ensikertalaisuus = Some(EnsikertalaisuusTulos(
        henkiloOid = HENKILO_OID,
        hakemusOid = Some(HAKEMUS_OID),
        hakuOid = KK_HAKU_OID,
        isEnsikertalainen = false,
        menettamisenPeruste = Some(MenettamisenPeruste(EnsikertalaisuusConstants.seliteKkVastaanotto, LocalDate.of(2024, 7, 15)))
      ))
    )

    setupMocks(hakemuspalveluClient, onrIntegration, valintaDataService, KK_HAKU, valintaData)

    val tila = Await.result(service.kasitteleHaku(KK_HAKU, OvaraParams(executionId = EXECUTION_ID)), 30.seconds)

    Assertions.assertEquals(2, tila.ensikertalaisuusTiedostoNumero)
  }

  @Test def testEnsikertalaisuudetEiTallennetaToisenAsteenHaulle(): Unit = {
    val (service, valintaDataService, hakemuspalveluClient, onrIntegration) = buildService()
    setupMocks(hakemuspalveluClient, onrIntegration, valintaDataService, TOISEN_ASTEEN_YHTEISHAKU,
      baseValintaData(avainArvot = Seq(ensikertalaisuusAvainArvo(ensikertalainen = true))))

    val tila = Await.result(service.kasitteleHaku(TOISEN_ASTEEN_YHTEISHAKU, OvaraParams(executionId = EXECUTION_ID)), 30.seconds)

    Assertions.assertEquals(1, tila.ensikertalaisuusTiedostoNumero)
  }

  @Test def testEpaonnistuneetHakemuksetKirjataan(): Unit = {
    val (service, valintaDataService, hakemuspalveluClient, onrIntegration) = buildService()
    Mockito.when(hakemuspalveluClient.getHaunHakijat(KK_HAKU.oid))
      .thenReturn(Future.successful(Seq(BASE_HAKIJA)))
    Mockito.when(onrIntegration.getAliasesForPersonOids(any()))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(HENKILO_OID -> Set(HENKILO_OID)))))
    Mockito.when(valintaDataService.fetchValintalaskentaHakemukset(any(), any(), any()))
      .thenReturn(Future.successful(Seq(BASE_VALINTALASKENTA_HAKEMUS)))
    Mockito.when(valintaDataService.doAvainArvoConversions(any(), any(), any(), any()))
      .thenThrow(new RuntimeException("testi-virhe"))

    val tila = Await.result(service.kasitteleHaku(KK_HAKU, OvaraParams(executionId = EXECUTION_ID)), 30.seconds)

    Assertions.assertEquals(1, tila.epaonnistuneet.size)
    Assertions.assertEquals(HENKILO_OID, tila.epaonnistuneet.head._1)
    Assertions.assertEquals(1, tila.valintaDataTiedostoNumero)
  }

  val WINDOW_END = Instant.parse("2024-01-15T12:00:00Z")

  val BASE_VERSIO = VersioEntiteetti(
    tunniste         = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    henkiloOid       = HENKILO_OID,
    alku             = Instant.parse("2024-01-01T00:00:00Z"),
    loppu            = None,
    lahdeJarjestelma = Lahdejarjestelma.KOSKI,
    lahdeTunniste    = "koski-tunniste",
    lahdeVersio      = None,
    parserVersio     = Some(3),
    luontiHetki      = None,
    paivitysHetki    = Some(Instant.parse("2024-01-15T10:00:00Z")),
    parserointiHetki = None
  )

  val BASE_KK_OPISKELUOIKEUS = KKOpiskeluoikeus(
    tunniste            = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    virtaTunniste       = "virta-1",
    tyyppiKoodi         = "1",
    koulutusKoodi       = None,
    alkuPvm             = LocalDate.of(2020, 9, 1),
    loppuPvm            = LocalDate.of(2024, 6, 1),
    virtaTila           = Koodi("1", "virtaopiskeluoikeudentila", None),
    supaTila            = KKOpiskeluoikeusTila.PAATTYNYT,
    myontaja            = "1.2.246.562.10.00000000001",
    isTutkintoonJohtava = true,
    kieli               = None,
    suoritukset         = Set.empty
  )

  private def buildServiceForOpiskeluoikeudet(batchSize: Int = 500): (OvaraService, KantaOperaatiot, OpiskeluoikeusParsingService, SiirtotiedostoClient) = {
    val mockKantaOperaatiot      = Mockito.mock(classOf[KantaOperaatiot])
    val mockParsingService       = Mockito.mock(classOf[OpiskeluoikeusParsingService])
    val mockSiirtotiedostoClient = Mockito.mock(classOf[SiirtotiedostoClient])

    val service = new OvaraService(500, batchSize) {
      override val kantaOperaatiot: KantaOperaatiot                          = mockKantaOperaatiot
      override val opiskeluoikeusParsingService: OpiskeluoikeusParsingService = mockParsingService
      override val siirtotiedostoClient: SiirtotiedostoClient                 = mockSiirtotiedostoClient
    }

    (service, mockKantaOperaatiot, mockParsingService, mockSiirtotiedostoClient)
  }

  @Test def testOpiskeluoikeudetTallennetaan(): Unit = {
    val (service, mockKantaOperaatiot, mockParsingService, mockSiirtotiedostoClient) = buildServiceForOpiskeluoikeudet()
    Mockito.when(mockKantaOperaatiot.haeMuuttuneetHenkiloOidit(any(), any(), any(), any()))
      .thenReturn(Seq(HENKILO_OID))
      .thenReturn(Seq.empty)
    Mockito.when(mockParsingService.haeSuoritukset(any(), anyBoolean()))
      .thenReturn(Map(BASE_VERSIO -> Set[Opiskeluoikeus](BASE_KK_OPISKELUOIKEUS)))

    val count = service.muodostaOpiskeluoikeusSiirtotiedostot(OvaraParams(executionId = EXECUTION_ID), Instant.parse("2024-01-01T00:00:00Z"), WINDOW_END)

    Assertions.assertEquals(1, count)
    Mockito.verify(mockSiirtotiedostoClient, Mockito.times(1))
      .tallennaSiirtotiedosto(any(), any(), any(), any(), any())
  }

  @Test def testTyhjaVersioEiTallennetaSiirtotiedostoon(): Unit = {
    val (service, mockKantaOperaatiot, mockParsingService, mockSiirtotiedostoClient) = buildServiceForOpiskeluoikeudet()
    Mockito.when(mockKantaOperaatiot.haeMuuttuneetHenkiloOidit(any(), any(), any(), any()))
      .thenReturn(Seq(HENKILO_OID))
      .thenReturn(Seq.empty)
    Mockito.when(mockParsingService.haeSuoritukset(any(), anyBoolean()))
      .thenReturn(Map(BASE_VERSIO -> Set.empty[Opiskeluoikeus]))

    val count = service.muodostaOpiskeluoikeusSiirtotiedostot(OvaraParams(executionId = EXECUTION_ID), Instant.parse("2024-01-01T00:00:00Z"), WINDOW_END)

    Assertions.assertEquals(1, count)
    Mockito.verify(mockSiirtotiedostoClient, Mockito.never())
      .tallennaSiirtotiedosto(any(), any(), any(), any(), any())
  }

  @Test def testKaksiVersiotaSamalleHenkilolleTuottaaYhdenRecordin(): Unit = {
    val (service, mockKantaOperaatiot, mockParsingService, mockSiirtotiedostoClient) = buildServiceForOpiskeluoikeudet()
    val versioVirta = BASE_VERSIO.copy(
      tunniste         = UUID.fromString("00000000-0000-0000-0000-000000000003"),
      lahdeJarjestelma = Lahdejarjestelma.VIRTA,
      lahdeTunniste    = "virta-tunniste"
    )
    val ooVirta = BASE_KK_OPISKELUOIKEUS.copy(tunniste = UUID.fromString("00000000-0000-0000-0000-000000000004"))
    // GROUP BY varmistaa että henkilö esiintyy kyselyssä vain kerran vaikka hänellä on useita muuttuneita versioita
    Mockito.when(mockKantaOperaatiot.haeMuuttuneetHenkiloOidit(any(), any(), any(), any()))
      .thenReturn(Seq(HENKILO_OID))
      .thenReturn(Seq.empty)
    Mockito.when(mockParsingService.haeSuoritukset(any(), anyBoolean()))
      .thenReturn(Map(BASE_VERSIO -> Set[Opiskeluoikeus](BASE_KK_OPISKELUOIKEUS), versioVirta -> Set[Opiskeluoikeus](ooVirta)))

    val count = service.muodostaOpiskeluoikeusSiirtotiedostot(OvaraParams(executionId = EXECUTION_ID), Instant.parse("2024-01-01T00:00:00Z"), WINDOW_END)

    // 1 henkilö → 1 record kaikilla opiskeluoikeuksilla, 1 tiedosto
    Assertions.assertEquals(1, count)
    Mockito.verify(mockSiirtotiedostoClient, Mockito.times(1))
      .tallennaSiirtotiedosto(any(), any(), any(), any(), any())
  }

  // HenkiloOid-perusteinen sivutus: varmistaa että afterHenkiloOid-kursorin arvot ovat oikein sivujen välillä.
  // Kursori seuraavalle kutsulle on oltava nykyisen sivun VIIMEINEN henkiloOid
  // (Seq säilyttää SQL ORDER BY henkilo_oid ASC -järjestyksen, joten page.last antaa suurimman henkiloOidin).

  @Test def testPagingKursoriOnViimeinenHenkiloOidillaSivulta(): Unit = {
    val oid1 = "1.2.246.562.24.10000000001"
    val oid2 = "1.2.246.562.24.10000000002"

    val (service, mockKantaOperaatiot, mockParsingService, _) = buildServiceForOpiskeluoikeudet(batchSize = 2)
    Mockito.when(mockKantaOperaatiot.haeMuuttuneetHenkiloOidit(any(), any(), any(), any()))
      .thenReturn(Seq(oid1, oid2))
      .thenReturn(Seq.empty)
    Mockito.when(mockParsingService.haeSuoritukset(any(), anyBoolean()))
      .thenReturn(Map(BASE_VERSIO -> Set[Opiskeluoikeus](BASE_KK_OPISKELUOIKEUS)))

    service.muodostaOpiskeluoikeusSiirtotiedostot(OvaraParams(executionId = EXECUTION_ID), Instant.parse("2024-01-01T00:00:00Z"), WINDOW_END)

    val captor = ArgumentCaptor.forClass(classOf[Option[_]]).asInstanceOf[ArgumentCaptor[Option[String]]]
    Mockito.verify(mockKantaOperaatiot, Mockito.times(2))
      .haeMuuttuneetHenkiloOidit(any(), any(), any(), captor.capture())

    val cursors = captor.getAllValues
    Assertions.assertEquals(None,       cursors.get(0), "ensimmäinen kutsu ilman kursoria")
    Assertions.assertEquals(Some(oid2), cursors.get(1), "kursori on viimeinen (suurin) henkiloOid sivulta")
  }

  @Test def testPagingKursoriEteneeKolmenSivunLapi(): Unit = {
    // Sivu 1: oid1 + oid2 (täysi erä, koko 2), Sivu 2: oid3 + oid4 (täysi erä, koko 2),
    // Sivu 3: oid5 (osittainen), sitten tyhjä.
    val oid1 = "1.2.246.562.24.10000000001"
    val oid2 = "1.2.246.562.24.10000000002"
    val oid3 = "1.2.246.562.24.10000000003"
    val oid4 = "1.2.246.562.24.10000000004"
    val oid5 = "1.2.246.562.24.10000000005"

    val (service, mockKantaOperaatiot, mockParsingService, _) = buildServiceForOpiskeluoikeudet(batchSize = 2)
    Mockito.when(mockKantaOperaatiot.haeMuuttuneetHenkiloOidit(any(), any(), any(), any()))
      .thenReturn(Seq(oid1, oid2))
      .thenReturn(Seq(oid3, oid4))
      .thenReturn(Seq(oid5))
      .thenReturn(Seq.empty)
    Mockito.when(mockParsingService.haeSuoritukset(any(), anyBoolean()))
      .thenReturn(Map(BASE_VERSIO -> Set[Opiskeluoikeus](BASE_KK_OPISKELUOIKEUS)))

    val count = service.muodostaOpiskeluoikeusSiirtotiedostot(OvaraParams(executionId = EXECUTION_ID), Instant.parse("2024-01-01T00:00:00Z"), WINDOW_END)

    Assertions.assertEquals(5, count)

    val captor = ArgumentCaptor.forClass(classOf[Option[_]]).asInstanceOf[ArgumentCaptor[Option[String]]]
    Mockito.verify(mockKantaOperaatiot, Mockito.times(4))
      .haeMuuttuneetHenkiloOidit(any(), any(), any(), captor.capture())

    val cursors = captor.getAllValues
    Assertions.assertEquals(None,       cursors.get(0), "sivu 1: ei kursoria")
    Assertions.assertEquals(Some(oid2), cursors.get(1), "sivu 2: kursori sivun 1 viimeinen henkiloOid")
    Assertions.assertEquals(Some(oid4), cursors.get(2), "sivu 3: kursori sivun 2 viimeinen henkiloOid")
    Assertions.assertEquals(Some(oid5), cursors.get(3), "sivu 4 (tyhjä): kursori sivun 3 viimeinen henkiloOid")
  }

  @Test def testOpiskeluoikeudetSivutusLuoUseampiaTiedostoja(): Unit = {
    val (service, mockKantaOperaatiot, mockParsingService, mockSiirtotiedostoClient) = buildServiceForOpiskeluoikeudet(batchSize = 2)
    Mockito.when(mockKantaOperaatiot.haeMuuttuneetHenkiloOidit(any(), any(), any(), any()))
      .thenReturn(Seq("1.2.246.562.24.10000000001", "1.2.246.562.24.10000000002"))
      .thenReturn(Seq("1.2.246.562.24.10000000003"))
      .thenReturn(Seq.empty)
    Mockito.when(mockParsingService.haeSuoritukset(any(), anyBoolean()))
      .thenReturn(Map(BASE_VERSIO -> Set[Opiskeluoikeus](BASE_KK_OPISKELUOIKEUS)))

    val count = service.muodostaOpiskeluoikeusSiirtotiedostot(OvaraParams(executionId = EXECUTION_ID), Instant.parse("2024-01-01T00:00:00Z"), WINDOW_END)

    Assertions.assertEquals(3, count)
    // sivu1 (2 henkilöä) → tiedosto 1, sivu2 (1 henkilö) → tiedosto 2
    Mockito.verify(mockSiirtotiedostoClient, Mockito.times(2))
      .tallennaSiirtotiedosto(any(), any(), any(), any(), any())
  }

  @Test def testTiedostoNumerotKasvavatBatchienValilla(): Unit = {
    val (service, valintaDataService, hakemuspalveluClient, onrIntegration) = buildService()
    val hakijat = (1 to 600).map(i => AtaruHakemuksenHenkilotiedot(s"hakemus-$i", Some(s"henkilo-$i"), None))
    Mockito.when(hakemuspalveluClient.getHaunHakijat(KK_HAKU.oid))
      .thenReturn(Future.successful(hakijat))
    Mockito.when(onrIntegration.getAliasesForPersonOids(any()))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map.empty)))
    Mockito.when(valintaDataService.fetchValintalaskentaHakemukset(any(), any(), any()))
      .thenReturn(Future.successful(Seq(BASE_VALINTALASKENTA_HAKEMUS)))
    Mockito.when(valintaDataService.doAvainArvoConversions(any(), any(), any(), any()))
      .thenReturn(baseValintaData())

    val tila = Await.result(service.kasitteleHaku(KK_HAKU, OvaraParams(executionId = EXECUTION_ID)), 30.seconds)

    // 600 hakijaa → 2 erää (500 + 100) → 2 valintadata-tiedostoa
    Assertions.assertEquals(3, tila.valintaDataTiedostoNumero)
  }
}
