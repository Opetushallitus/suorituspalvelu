package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, AtaruValintalaskentaHakemus, HakemuspalveluClient, Hakutoive, KoutaHaku, KoutaHakuaika, SiirtotiedostoClient}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoContainer, ConvertedAtaruHakemus, EnsikertalaisuusConstants, HakemuksenHarkinnanvaraisuus, HakutoiveenHarkinnanvaraisuus, HarkinnanvaraisuudenSyy}
import org.junit.jupiter.api.{Assertions, Test}
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.mockito.Mockito
import org.mockito.ArgumentMatchers.any

import java.time.{Instant, LocalDate}
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
    ensikertalaisuus: Option[AvainArvoContainer] = None,
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

  // Mockito.mock uses Objenesis so the SiirtotiedostoClient constructor (which references
  // the external SiirtotiedostoPalvelu) is never called. Unit-returning methods do nothing
  // by default, so no stubbing is needed — we verify behaviour via HaunKasittelyTila.
  private def buildService(): (OvaraService, ValintaDataService, HakemuspalveluClient, OnrIntegration) = {
    val mockValintaDataService   = Mockito.mock(classOf[ValintaDataService])
    val mockSiirtotiedostoClient = Mockito.mock(classOf[SiirtotiedostoClient])
    val mockHakemuspalveluClient = Mockito.mock(classOf[HakemuspalveluClient])
    val mockOnrIntegration       = Mockito.mock(classOf[OnrIntegration])

    val service = new OvaraService(500) {
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

  // tiedostoNumero starts at 1 and increments each time a non-empty file is saved.
  // Staying at 1 means no file was saved; becoming 2 means one file was saved.

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
      ensikertalaisuus = Some(AvainArvoContainer("ensikertalainen", "false", Seq(EnsikertalaisuusConstants.seliteKkVastaanotto)))
    )

    setupMocks(hakemuspalveluClient, onrIntegration, valintaDataService, KK_HAKU, valintaData)

    val tila = Await.result(service.kasitteleHaku(KK_HAKU, OvaraParams(executionId = EXECUTION_ID, ensikertalaisuudet = true)), 30.seconds)

    Assertions.assertEquals(2, tila.ensikertalaisuusTiedostoNumero)
  }

  @Test def testEnsikertalaisuuksiaEiTallennetaKunLippuPoissa(): Unit = {
    val (service, valintaDataService, hakemuspalveluClient, onrIntegration) = buildService()
    setupMocks(hakemuspalveluClient, onrIntegration, valintaDataService, KK_HAKU,
      baseValintaData(avainArvot = Seq(ensikertalaisuusAvainArvo(ensikertalainen = true))))

    val tila = Await.result(service.kasitteleHaku(KK_HAKU, OvaraParams(executionId = EXECUTION_ID, ensikertalaisuudet = false)), 30.seconds)

    Assertions.assertEquals(1, tila.ensikertalaisuusTiedostoNumero)
  }

  @Test def testEnsikertalaisuudetEiTallennetaToisenAsteenHaulle(): Unit = {
    val (service, valintaDataService, hakemuspalveluClient, onrIntegration) = buildService()
    setupMocks(hakemuspalveluClient, onrIntegration, valintaDataService, TOISEN_ASTEEN_YHTEISHAKU,
      baseValintaData(avainArvot = Seq(ensikertalaisuusAvainArvo(ensikertalainen = true))))

    val tila = Await.result(service.kasitteleHaku(TOISEN_ASTEEN_YHTEISHAKU, OvaraParams(executionId = EXECUTION_ID, ensikertalaisuudet = true)), 30.seconds)

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

    // 600 hakijat → 2 batches (500 + 100) → 2 valintadata files
    Assertions.assertEquals(3, tila.valintaDataTiedostoNumero)
  }
}
