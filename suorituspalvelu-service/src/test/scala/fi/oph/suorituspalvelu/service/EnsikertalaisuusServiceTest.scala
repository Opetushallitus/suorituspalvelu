package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.BaseIntegraatioTesti
import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, KKOpiskeluoikeusTila, KKTutkinto, Koodi, Lahdejarjestelma, Opiskeluoikeus, ParserVersions, Suoritus, SuoritusTila}
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemusBaseFields, AtaruValintalaskentaHakemus, Ensikertalaisuus, HakemuspalveluClientImpl, KoutaHaku, KoutaHakuaika, VTSClient}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, PersonOidsWithAliases, TarjontaIntegration}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoContainer, EnsikertalaisuusConstants, EnsikertalaisuusService}
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
      suoritukset = suoritukset
    )

  private def assertEnsikertalainen(result: AvainArvoContainer): Unit = {
    Assertions.assertEquals(AvainArvoConstants.ensikertalainenKey, result.avain)
    Assertions.assertEquals("true", result.arvo)
    Assertions.assertTrue(result.selitteet.isEmpty)
  }

  private def assertEiEnsikertalainen(result: AvainArvoContainer, expectedPeruste: String): Unit = {
    Assertions.assertEquals(AvainArvoConstants.ensikertalainenKey, result.avain)
    Assertions.assertEquals("false", result.arvo)
    Assertions.assertEquals(Seq(expectedPeruste), result.selitteet)
  }

  /**
   * 1. Henkilöllä ei ole KK-dataa lainkaan → ensikertalainen
   */
  @Test def testEnsikertalainen(): Unit = {
    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq.empty, false)
    assertEnsikertalainen(result)
  }

  /**
   * 2. Henkilöllä VALMIS KK-tutkinto ennen leikkuripäivämäärää → SuoritettuKkTutkinto
   */
  @Test def testSuoritettuKkTutkinto(): Unit = {
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(LocalDate.of(2024, 6, 15)))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 15), true, Set(tutkinto))

    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, false)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkinto)
  }

  /**
   * 3. Henkilöllä VALMIS KK-tutkinto leikkuripäivämäärän JÄLKEEN → ensikertalainen
   */
  @Test def testSuoritettuKkTutkintoAfterCutoff(): Unit = {
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(LocalDate.of(2026, 3, 15)))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2025, 9, 1), LocalDate.of(2026, 3, 15), true, Set(tutkinto))

    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, false)
    assertEnsikertalainen(result)
  }

  /**
   * 4. Henkilöllä KESKEN-tilainen KK-tutkinto → ei ensikertalainen (OpiskeluoikeusAlkanut)
   */
  @Test def testKeskenTilainenTutkinto(): Unit = {
    val tutkinto = luoKKTutkinto(SuoritusTila.KESKEN, Some(LocalDate.of(2024, 6, 15)))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 15), true, Set(tutkinto))

    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, false)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteOpiskeluoikeusAlkanut)
  }

  /**
   * 5. Henkilöllä tutkintoon johtava KK-opiskeluoikeus alkanut ≥ 1.8.2014 ennen leikkuria → OpiskeluoikeusAlkanut
   */
  @Test def testOpiskeluoikeusAlkanut(): Unit = {
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2024, 9, 1), LocalDate.of(2027, 6, 30), true)

    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, false)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteOpiskeluoikeusAlkanut)
  }

  /**
   * 6. Henkilöllä KK-opiskeluoikeus alkanut ENNEN 1.8.2014 → ensikertalainen
   */
  @Test def testOpiskeluoikeusAlkanutEnnen2014(): Unit = {
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2013, 9, 1), LocalDate.of(2016, 6, 30), true)

    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, false)
    assertEnsikertalainen(result)
  }

  /**
   * 7. Ei-tutkintoon johtava opiskeluoikeus ei vie ensikertalaisuutta
   */
  @Test def testEiTutkintoonJohtavaOpiskeluoikeus(): Unit = {
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30), false)

    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, false)
    assertEnsikertalainen(result)
  }

  /**
   * 8. VTS ensikertalaisuus päättynyt ennen leikkuria → KkVastaanotto
   */
  @Test def testKkVastaanotto(): Unit = {
    val vtsData = Seq(Ensikertalaisuus(HENKILO_OID, Some("2024-07-15T09:47:20Z")))

    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, vtsData, false)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteKkVastaanotto)
  }

  /**
   * 9. VTS ensikertalaisuus päättynyt leikkurin JÄLKEEN → ensikertalainen
   */
  @Test def testKkVastaanottoAfterCutoff(): Unit = {
    val vtsData = Seq(Ensikertalaisuus(HENKILO_OID, Some("2025-07-15T09:47:20Z")))

    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, vtsData, false)
    assertEnsikertalainen(result)
  }

  /**
   * 10. Hakemuksella korkeakoulututkintoVuosi → SuoritettuKkTutkintoHakemukselta
   */
  @Test def testSuoritettuKkTutkintoHakemukselta(): Unit = {
    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq.empty, Seq.empty, true)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkintoHakemukselta)
  }

  /**
   * 11. Prioriteetti: SuoritettuKkTutkinto voittaa OpiskeluoikeusAlkanut kun molemmat täyttyvät
   */
  @Test def testPrioriteettiSuoritettuKkTutkintoVoittaa(): Unit = {
    // Sekä valmis tutkinto että tutkintoon johtava opiskeluoikeus
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(LocalDate.of(2023, 6, 15)))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2020, 9, 1), LocalDate.of(2023, 6, 15), true, Set(tutkinto))

    val result = ensikertalaisuusService.paatteleEnsikertalaisuusAvainArvo(
      HENKILO_OID, leikkuriLocalDate, Seq(oo), Seq.empty, false)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkinto)
  }

  /**
   * 12. haeEnsikertalaisuusAvainArvo hakee VTS-datan ja suodattaa opiskeluoikeudet oikein
   */
  @Test def testHaeEnsikertalaisuusAvainArvo(): Unit = {
    setupDefaultMocks()
    val tutkinto = luoKKTutkinto(SuoritusTila.VALMIS, Some(LocalDate.of(2024, 6, 15)))
    val oo = luoKKOpiskeluoikeus(LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 15), true, Set(tutkinto))

    val result = ensikertalaisuusService.haeEnsikertalaisuusAvainArvo(
      HENKILO_OID, testHaku, Set(HENKILO_OID), Seq(oo), None)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkinto)
  }

  /**
   * 13. haeEnsikertalaisuusAvainArvo: aliaksilla oleva VTS-data löydetään oikein
   */
  @Test def testAliastenKauttaLoytyyVtsData(): Unit = {
    Mockito.when(vtsClient.fetchEnsikertalaisuudet(any()))
      .thenReturn(Future.successful(Seq(Ensikertalaisuus(ALIAS_OID, Some("2024-07-15T09:47:20Z")))))

    val result = ensikertalaisuusService.haeEnsikertalaisuusAvainArvo(
      HENKILO_OID, testHaku, Set(HENKILO_OID, ALIAS_OID), Seq.empty, None)
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteKkVastaanotto)
  }

  /**
   * 14. haeEnsikertalaisuusAvainArvo: hakemusdata välitetään oikein
   */
  @Test def testHaeEnsikertalaisuusAvainArvoHakemuksella(): Unit = {
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

    val result = ensikertalaisuusService.haeEnsikertalaisuusAvainArvo(
      HENKILO_OID, testHaku, Set(HENKILO_OID), Seq.empty, Some(hakemus))
    assertEiEnsikertalainen(result, EnsikertalaisuusConstants.seliteSuoritettuKkTutkintoHakemukselta)
  }
}
