package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.BaseIntegraatioTesti
import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, KKOpiskeluoikeusTila, KKSynteettinenOpiskeluoikeus, KKTutkinto, Koodi, Lahdejarjestelma, Opiskeluoikeus, ParserVersions, Suoritus, SuoritusTila}
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemusBaseFields, AtaruValintalaskentaHakemus, Ensikertalaisuus, HakemuspalveluClientImpl, KoutaHaku, KoutaHakuaika, VTSClient}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, PersonOidsWithAliases, TarjontaIntegration}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoContainer, EnsikertalaisuusConstants, EnsikertalaisuusService, EnsikertalaisuusTulos, MenettamisenPeruste}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import org.junit.jupiter.api.{Assertions, Test}
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.mockito.Mockito
import org.mockito.ArgumentMatchers.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.`override`.mockito.MockitoBean

import java.time.{Instant, LocalDate}
import java.util.{Optional, UUID}
import scala.concurrent.Future

@Test
@TestInstance(Lifecycle.PER_CLASS)
class EnsikertalaisuusServiceTest extends BaseIntegraatioTesti {

  @MockitoBean
  var onrIntegration: OnrIntegration = null

  @MockitoBean
  var tarjontaIntegration: TarjontaIntegration = null

  @MockitoBean
  var vtsClient: VTSClient = null

  @MockitoBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @Autowired
  var ensikertalaisuusService: EnsikertalaisuusService = null

  val HENKILO_OID = "1.2.246.562.24.10000000001"
  val ALIAS_OID = "1.2.246.562.24.10000000002"
  val HAKU_OID = "1.2.246.562.29.00000000000000099901"
  val HAKEMUS_OID = "1.2.246.562.11.00000000000000006321"

  // Haku jonka viimeinen hakuaika päättyy 2025-03-31
  val testHaku = KoutaHaku(
    oid = HAKU_OID,
    tila = "julkaistu",
    nimi = Map("fi" -> "Testihaku"),
    hakutapaKoodiUri = "hakutapa_01",
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_12"),
    hakuajat = List(KoutaHakuaika("2025-01-15T08:00:00", Some("2025-03-31T15:00:00"))),
    kohdejoukonTarkenneKoodiUri = None,
    hakuvuosi = Some(2025)
  )

  // Leikkuripäivämäärä testihaulle: 2025-03-31
  val leikkuriLocalDate = LocalDate.of(2025, 3, 31)

  private def setupDefaultMocks(): Unit = {
    Mockito.when(vtsClient.fetchEnsikertalaisuudet(any()))
      .thenReturn(Future.successful(Seq.empty))
  }

  private def luoKKTutkinto(supaTila: SuoritusTila, suoritusPvm: Option[LocalDate]): KKTutkinto =
    KKTutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Some(Kielistetty(Some("Testiutkinto"), None, None)),
      supaTila = supaTila,
      komoTunniste = "koulutus_000000",
      opintoPisteet = BigDecimal(180),
      aloitusPvm = Some(LocalDate.of(2020, 9, 1)),
      suoritusPvm = suoritusPvm,
      myontaja = "1.2.246.562.10.00000000001",
      kieli = Some("fi"),
      koulutusKoodi = Some("000000"),
      opiskeluoikeusAvain = Some("avain1"),
      avain = Some("avain1")
    )

  private def luoKKOpiskeluoikeus(alkuPvm: LocalDate, loppuPvm: LocalDate, isTutkintoonJohtava: Boolean, suoritukset: Set[Suoritus] = Set.empty): KKOpiskeluoikeus =
    KKOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      virtaTunniste = "vt-" + UUID.randomUUID().toString.take(8),
      tyyppiKoodi = "1",
      koulutusKoodi = Some("000000"),
      alkuPvm = alkuPvm,
      loppuPvm = loppuPvm,
      virtaTila = Koodi("1", "virtaopiskeluoikeudentila", None),
      supaTila = KKOpiskeluoikeusTila.VOIMASSA,
      myontaja = "1.2.246.562.10.00000000001",
      isTutkintoonJohtava = isTutkintoonJohtava,
      kieli = Some("fi"),
      suoritukset = suoritukset,
      luokittelu = None,
      rahoitusLahde = None,
      nimi = None
    )

  private def luoKKSynteettinenOpiskeluoikeus(containsKKTutkinto: Boolean, suoritukset: Set[Suoritus] = Set.empty): KKSynteettinenOpiskeluoikeus =
    KKSynteettinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      myontaja = "1.2.246.562.10.00000000001",
      containsKKTutkinto = containsKKTutkinto,
      suoritukset = suoritukset
    )

  private def assertEnsikertalainen(result: Option[MenettamisenPeruste]): Unit = {
    Assertions.assertTrue(result.isEmpty, s"Expected ensikertalainen (no menettamisenPeruste) but got: $result")
  }

  private def assertEiEnsikertalainen(result: Option[MenettamisenPeruste], expectedPeruste: String): Unit = {
    Assertions.assertTrue(result.isDefined, "Expected ei-ensikertalainen but got None")
    Assertions.assertEquals(expectedPeruste, result.get.peruste)
  }

  private def assertEnsikertalainen(result: EnsikertalaisuusTulos): Unit = {
    Assertions.assertTrue(result.isEnsikertalainen)
    Assertions.assertTrue(result.menettamisenPeruste.isEmpty)
  }

  private def assertEiEnsikertalainen(result: EnsikertalaisuusTulos, expectedPeruste: String): Unit = {
    Assertions.assertFalse(result.isEnsikertalainen)
    Assertions.assertEquals(expectedPeruste, result.menettamisenPeruste.map(_.peruste).orNull)
  }

  /**
   * 1. Henkilöllä ei ole KK-dataa lainkaan → ensikertalainen
   */
  @Test def testEnsikertalainen(): Unit = {
    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq.empty, Seq.empty, None)
    assertEnsikertalainen(result)
  }

  /**
   * 2. Henkilöllä VALMIS KK-tutkinto ennen leikkuripäivämäärää → SuoritettuKkTutkinto
   */
  @Test def testSuoritettuKkTutkinto(): Unit = {
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(LocalDate.of(2024, 6, 15)))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 15), true, Set(tutkinto))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, Seq.empty, None)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkinto)
  }

  /**
   * 3. Henkilöllä VALMIS KK-tutkinto leikkuripäivämäärän JÄLKEEN → ensikertalainen
   */
  @Test def testSuoritettuKkTutkintoAfterCutoff(): Unit = {
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(LocalDate.of(2026, 3, 15)))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2025, 9, 1), LocalDate.of(2026, 3, 15), true, Set(tutkinto))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, Seq.empty, None)
    assertEnsikertalainen(result)
  }

  /**
   * 4. Henkilöllä KESKEN-tilainen KK-tutkinto → ei ensikertalainen (OpiskeluoikeusAlkanut)
   */
  @Test def testKeskenTilainenTutkinto(): Unit = {
    val tutkinto = luoKKTutkinto(SuoritusTila.KESKEN, Some(LocalDate.of(2024, 6, 15)))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 15), true, Set(tutkinto))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, Seq.empty, None)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteOpiskeluoikeusAlkanut)
  }

  /**
   * 5. Henkilöllä tutkintoon johtava KK-opiskeluoikeus alkanut ≥ 1.8.2014 ennen leikkuria → OpiskeluoikeusAlkanut
   */
  @Test def testOpiskeluoikeusAlkanut(): Unit = {
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2024, 9, 1), LocalDate.of(2027, 6, 30), true)

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, Seq.empty, None)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteOpiskeluoikeusAlkanut)
  }

  /**
   * 6. Henkilöllä KK-opiskeluoikeus alkanut ENNEN 1.8.2014 → ensikertalainen
   */
  @Test def testOpiskeluoikeusAlkanutEnnen2014(): Unit = {
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2013, 9, 1), LocalDate.of(2016, 6, 30), true)

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, Seq.empty, None)
    assertEnsikertalainen(result)
  }

  /**
   * 7. Ei-tutkintoon johtava opiskeluoikeus ei vie ensikertalaisuutta
   */
  @Test def testEiTutkintoonJohtavaOpiskeluoikeus(): Unit = {
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30), false)

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, Seq.empty, None)
    assertEnsikertalainen(result)
  }

  /**
   * 8. VTS ensikertalaisuus päättynyt ennen leikkuria → KkVastaanotto
   */
  @Test def testKkVastaanotto(): Unit = {
    val vtsData = Seq(Ensikertalaisuus(HENKILO_OID, Some("2024-07-15T09:47:20Z")))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq.empty, vtsData, None)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteKkVastaanotto)
  }

  /**
   * 9. VTS ensikertalaisuus päättynyt leikkurin JÄLKEEN → ensikertalainen
   */
  @Test def testKkVastaanottoAfterCutoff(): Unit = {
    val vtsData = Seq(Ensikertalaisuus(HENKILO_OID, Some("2025-07-15T09:47:20Z")))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq.empty, vtsData, None)
    assertEnsikertalainen(result)
  }

  /**
   * 10. Hakemuksella korkeakoulututkintoVuosi → SuoritettuKkTutkintoHakemukselta
   */
  @Test def testSuoritettuKkTutkintoHakemukselta(): Unit = {
    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq.empty, Seq.empty, Some(2000))
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkintoHakemukselta)
  }

  /**
   * 11. Prioriteetti: SuoritettuKkTutkinto voittaa OpiskeluoikeusAlkanut kun molemmat täyttyvät
   */
  @Test def testPrioriteettiSuoritettuKkTutkintoVoittaa(): Unit = {
    // Sekä valmis tutkinto että tutkintoon johtava opiskeluoikeus
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(LocalDate.of(2023, 6, 15)))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2020, 9, 1), LocalDate.of(2023, 6, 15), true, Set(tutkinto))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, Seq.empty, None)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkinto)
  }

  /**
   * 12. haeEnsikertalaisuusTulos hakee VTS-datan ja suodattaa opiskeluoikeudet oikein
   */
  @Test def testHaeEnsikertalaisuusTulos(): Unit = {
    setupDefaultMocks()
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(LocalDate.of(2024, 6, 15)))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 15), true, Set(tutkinto))

    val result = ensikertalaisuusService.haeEnsikertalaisuusTulos(
      HENKILO_OID, testHaku, Set(HENKILO_OID), Seq(oo), None)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkinto)
  }

  /**
   * 13. haeEnsikertalaisuusTulos: aliaksilla oleva VTS-data löydetään oikein
   */
  @Test def testAliastenKauttaLoytyyVtsData(): Unit = {
    Mockito.when(vtsClient.fetchEnsikertalaisuudet(any()))
      .thenReturn(Future.successful(Seq(Ensikertalaisuus(ALIAS_OID, Some("2024-07-15T09:47:20Z")))))

    val result = ensikertalaisuusService.haeEnsikertalaisuusTulos(
      HENKILO_OID, testHaku, Set(HENKILO_OID, ALIAS_OID), Seq.empty, None)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteKkVastaanotto)
  }

  /**
   * 14. haeEnsikertalaisuusTulos: hakemusdata välitetään oikein
   */
  @Test def testHaeEnsikertalaisuusTulosHakemuksella(): Unit = {
    setupDefaultMocks()
    val hakemus = AtaruValintalaskentaHakemus(
      hakemusOid = "1.2.246.562.11.00000000001",
      personOid = HENKILO_OID,
      hakuOid = HAKU_OID,
      asiointikieli = "fi",
      hakutoiveet = List.empty,
      maksuvelvollisuus = Map.empty,
      keyValues = Map.empty,
      korkeakoulututkintoVuosi = Some(2000)
    )

    val result = ensikertalaisuusService.haeEnsikertalaisuusTulos(
      HENKILO_OID, testHaku, Set(HENKILO_OID), Seq.empty, Some(hakemus))
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkintoHakemukselta)
  }

  /**
   * 15. Synteettisessä opiskeluoikeudessa VALMIS KK-tutkinto ennen leikkuria → SuoritettuKkTutkintoSynteettisesta
   */
  @Test def testSynteettinenOpiskeluoikeusSuoritettuTutkinto(): Unit = {
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(LocalDate.of(2024, 6, 15)))
    val syntOo = luoKKSynteettinenOpiskeluoikeus(containsKKTutkinto = true, suoritukset = Set(tutkinto))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq(syntOo), Seq.empty, None)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkintoSynteettisesta)
  }

  /**
   * 16. Synteettisessä opiskeluoikeudessa VALMIS KK-tutkinto leikkurin JÄLKEEN → ensikertalainen
   */
  @Test def testSynteettinenOpiskeluoikeusTutkintoAfterCutoff(): Unit = {
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(LocalDate.of(2026, 3, 15)))
    val syntOo = luoKKSynteettinenOpiskeluoikeus(containsKKTutkinto = true, suoritukset = Set(tutkinto))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq(syntOo), Seq.empty, None)
    assertEnsikertalainen(result)
  }

  /**
   * 17. Synteettinen opiskeluoikeus ilman tutkintoa (containsKKTutkinto=false) → ensikertalainen
   */
  @Test def testSynteettinenOpiskeluoikeusIlmanTutkintoa(): Unit = {
    val syntOo = luoKKSynteettinenOpiskeluoikeus(containsKKTutkinto = false)

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq(syntOo), Seq.empty, None)
    assertEnsikertalainen(result)
  }

  /**
   * 18. Synteettisessä opiskeluoikeudessa KESKEN-tilainen tutkinto → ensikertalainen (tällaista tilannetta ei nykyisellään voi olla, mutta ollaan defensiivisiä)
   */
  @Test def testSynteettinenOpiskeluoikeusKeskenTutkinto(): Unit = {
    val tutkinto = luoKKTutkinto(SuoritusTila.KESKEN, Some(LocalDate.of(2024, 6, 15)))
    val syntOo = luoKKSynteettinenOpiskeluoikeus(containsKKTutkinto = true, suoritukset = Set(tutkinto))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq(syntOo), Seq.empty, None)
    assertEnsikertalainen(result)
  }

  // --- paivamaara-testit ---

  /**
   * 19. SuoritettuKkTutkinto: paivamaara on tutkinnon suoritusPvm
   */
  @Test def testSuoritettuKkTutkintoPaivamaara(): Unit = {
    val suoritusPvm = LocalDate.of(2024, 6, 15)
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(suoritusPvm))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2020, 9, 1), suoritusPvm, true, Set(tutkinto))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, Seq.empty, None)
    Assertions.assertEquals(suoritusPvm, result.get.paivamaara)
  }

  /**
   * 20. SuoritettuKkTutkinto: paivamaara on aikaisin suoritusPvm kun useita tutkintoja
   */
  @Test def testSuoritettuKkTutkintoAikaisinPaivamaara(): Unit = {
    val earlier = LocalDate.of(2022, 5, 10)
    val later = LocalDate.of(2024, 6, 15)
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2020, 9, 1), later, true, Set(
      luoKKTutkinto(SuoritusTila.VALMIS, Some(later)),
      luoKKTutkinto(SuoritusTila.VALMIS, Some(earlier))
    ))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, Seq.empty, None)
    Assertions.assertEquals(earlier, result.get.paivamaara)
  }

  /**
   * 21. OpiskeluoikeusAlkanut: paivamaara on opiskeluoikeuden alkuPvm
   */
  @Test def testOpiskeluoikeusAlkanutPaivamaara(): Unit = {
    val alkuPvm = LocalDate.of(2024, 9, 1)
    val oo = luoKKOpiskeluoikeus(alkuPvm, LocalDate.of(2027, 6, 30), true)

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, Seq.empty, None)
    Assertions.assertEquals(alkuPvm, result.get.paivamaara)
  }

  /**
   * 22. KkVastaanotto: paivamaara on VTS paattyi-päivä Helsingin aikavyöhykkeellä
   */
  @Test def testKkVastaanottoPaivamaara(): Unit = {
    val vtsData = Seq(Ensikertalaisuus(HENKILO_OID, Some("2024-07-15T09:47:20Z")))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq.empty, vtsData, None)
    Assertions.assertEquals(LocalDate.of(2024, 7, 15), result.get.paivamaara)
  }

  /**
   * 23. KkVastaanotto: paivamaara on aikaisin paattyi-päivä kun useita VTS-merkintöjä
   */
  @Test def testKkVastaanottoAikaisinPaivamaara(): Unit = {
    val vtsData = Seq(
      Ensikertalaisuus(HENKILO_OID, Some("2024-07-15T09:47:20Z")),
      Ensikertalaisuus(HENKILO_OID, Some("2023-03-01T12:00:00Z"))
    )

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq.empty, vtsData, None)
    Assertions.assertEquals(LocalDate.of(2023, 3, 1), result.get.paivamaara)
  }

  /**
   * 24. SuoritettuKkTutkintoHakemukselta: paivamaara on tammikuun 1. päivä annetulta vuodelta
   */
  @Test def testSuoritettuKkTutkintoHakemukseltaPaivamaara(): Unit = {
    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq.empty, Seq.empty, Some(2000))
    Assertions.assertEquals(LocalDate.of(2000, 1, 1), result.get.paivamaara)
  }

  /**
   * 25. SuoritettuKkTutkintoSynteettisesta: paivamaara on synteettisen tutkinnon suoritusPvm
   */
  @Test def testSynteettinenOpiskeluoikeusPaivamaara(): Unit = {
    val suoritusPvm = LocalDate.of(2024, 6, 15)
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(suoritusPvm))
    val syntOo = luoKKSynteettinenOpiskeluoikeus(containsKKTutkinto = true, suoritukset = Set(tutkinto))

    val result = ensikertalaisuusService.paatteleMenettamisenPeruste(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq(syntOo), Seq.empty, None)
    Assertions.assertEquals(suoritusPvm, result.get.paivamaara)
  }

  /**
   * 26. toAvainArvo: ensikertalainen ilman menettämisen perustetta
   */
  @Test def testToAvainArvoEnsikertalainen(): Unit = {
    val tulos = EnsikertalaisuusTulos(HENKILO_OID, Some(HAKEMUS_OID), HAKU_OID, isEnsikertalainen = true, menettamisenPeruste = None)
    val avainArvo = tulos.toAvainArvo
    Assertions.assertEquals(AvainArvoConstants.ensikertalainenKey, avainArvo.avain)
    Assertions.assertEquals("true", avainArvo.arvo)
    Assertions.assertEquals(Seq.empty, avainArvo.selitteet)
  }

  /**
   * 27. toAvainArvo: ei ensikertalainen, menettämisen peruste sisältyy selitteisiin
   */
  @Test def testToAvainArvoEiEnsikertalainen(): Unit = {
    val peruste = EnsikertalaisuusConstants.seliteKkVastaanotto
    val tulos = EnsikertalaisuusTulos(HENKILO_OID, Some(HAKEMUS_OID), HAKU_OID, isEnsikertalainen = false,
      menettamisenPeruste = Some(MenettamisenPeruste(peruste, LocalDate.of(2020, 1, 1))))
    val avainArvo = tulos.toAvainArvo
    Assertions.assertEquals(AvainArvoConstants.ensikertalainenKey, avainArvo.avain)
    Assertions.assertEquals("false", avainArvo.arvo)
    Assertions.assertEquals(Seq(peruste), avainArvo.selitteet)
  }
}
