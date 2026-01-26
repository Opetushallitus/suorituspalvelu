package fi.oph.suorituspalvelu.business.valinnat.mankeli

import fi.oph.suorituspalvelu.business.SuoritusTila.KESKEN
import fi.oph.suorituspalvelu.business.{Koodi, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, SuoritusTila}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, DateParam, Hakutoive, KoutaHakukohde, Ohjausparametrit}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, HarkinnanvaraisuudenSyy, HarkinnanvaraisuusPaattely}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import org.junit.jupiter.api.{Assertions, Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle

import java.time.{Instant, LocalDate, ZoneId}
import java.util.UUID

@Test
@TestInstance(Lifecycle.PER_CLASS)
class HarkinnanvaraisuusPaattelyTest {

  val PERSON_OID = "1.2.246.562.24.12345678901"
  val HAKEMUS_OID = "1.2.246.562.11.12345678901"
  val HAKU_OID = "1.2.246.562.29.12345678901"
  val HAKUKOHDE_OID_1 = "1.2.246.562.20.12345678901"
  val HAKUKOHDE_OID_2 = "1.2.246.562.20.12345678902"

  val BASE_HAKEMUS = AtaruValintalaskentaHakemus(
    hakemusOid = HAKEMUS_OID,
    personOid = PERSON_OID,
    hakuOid = HAKU_OID,
    asiointikieli = "fi",
    hakutoiveet = List(
      Hakutoive(
        processingState = "unprocessed",
        eligibilityState = "eligible",
        paymentObligation = "not-obligated",
        kkApplicationPaymentObligation = "unreviewed",
        hakukohdeOid = HAKUKOHDE_OID_1,
        languageRequirement = "unreviewed",
        degreeRequirement = "unreviewed",
        harkinnanvaraisuus = None
      ),
      Hakutoive(
        processingState = "unprocessed",
        eligibilityState = "eligible",
        paymentObligation = "not-obligated",
        kkApplicationPaymentObligation = "unreviewed",
        hakukohdeOid = HAKUKOHDE_OID_2,
        languageRequirement = "unreviewed",
        degreeRequirement = "unreviewed",
        harkinnanvaraisuus = None
      )
    ),
    maksuvelvollisuus = Map.empty,
    keyValues = Map.empty
  )

  val DEFAULT_OHJAUSPARAMETRIT = Ohjausparametrit(
    PH_HKP = None,
    suoritustenVahvistuspaiva = Some(DateParam(
      date = Instant.now().minusMillis(1000 * 60 * 60 * 24 * 30).toEpochMilli  // 30 days ago
    ))
  )

  @Test
  def testHasYksilollistettyMatematiikkaJaAidinkieli(): Unit = {
    val vahvistettuViimeistaan = LocalDate.now()

    //Case 1: Sekä AI että MA yksilöllistetty
    val opiskeluoikeudet1 = Seq(createOpiskeluoikeusWithOppimaara(true, true, SuoritusTila.VALMIS, Some(LocalDate.parse("2025-05-30"))))
    Assertions.assertTrue(
      HarkinnanvaraisuusPaattely.hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet1, vahvistettuViimeistaan))

    //Case 2: Vain MA yksilöllistetty
    val opiskeluoikeudet2 = Seq(createOpiskeluoikeusWithOppimaara(true, false, SuoritusTila.VALMIS, Some(LocalDate.parse("2025-05-30"))))
    Assertions.assertFalse(
      HarkinnanvaraisuusPaattely.hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet2, vahvistettuViimeistaan))

    // Case 3: Vain AI yksilöllistetty
    val opiskeluoikeudet3 = Seq(createOpiskeluoikeusWithOppimaara(false, true, SuoritusTila.VALMIS, Some(LocalDate.parse("2025-05-30"))))
    Assertions.assertFalse(
      HarkinnanvaraisuusPaattely.hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet3, vahvistettuViimeistaan))

    // Case 4: Ei mitään yksilöllistetty
    val opiskeluoikeudet4 = Seq(createOpiskeluoikeusWithOppimaara(false, false, SuoritusTila.VALMIS, Some(LocalDate.parse("2025-05-30"))))
    Assertions.assertFalse(
      HarkinnanvaraisuusPaattely.hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet4, vahvistettuViimeistaan))
  }

  @Test
  def testHasYksilollistettyWithKumoavaKorotus(): Unit = {
    // Oppimäärässä sekä AI että MA yksilöllistetty, mutta löytyy erillinen oppiaineen suoritus jossa MA ei yksilöllistetty
    val opiskeluoikeudet = Seq(
      createOpiskeluoikeusWithOppimaara(true, true, SuoritusTila.VALMIS),
      createOpiskeluoikeusWithOppiaineenOppimaara("MA", false)
    )

    val vahvistettuViimeistaan = LocalDate.now()

    Assertions.assertFalse(
      HarkinnanvaraisuusPaattely.hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet, vahvistettuViimeistaan))
  }

  //Testi tapaukselle, jossa yksilöllistetty suoritus on vahvistettu ennen 2025-08-01.
  @Test
  def testHasYksilollistettyWithVahvistusPaivamaaraFilter(): Unit = {
    val leikkuriPaiva = LocalDate.parse("2026-05-30")
    val leikkuriPaivanJalkeen = leikkuriPaiva.plusDays(2)
    val ennenLeikkuripaivaa = LocalDate.parse("2025-06-01")

    // Case 1: Suoritus vahvistettu leikkuripäivän jälkeen
    val opiskeluoikeudet1 = Seq(
      createOpiskeluoikeusWithOppimaara(true, true, SuoritusTila.VALMIS, Some(leikkuriPaivanJalkeen))
    )

    Assertions.assertFalse(
      HarkinnanvaraisuusPaattely.hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet1, leikkuriPaiva))

    // Case 2: Suoritus vahvistettu ennen leikkuripäivää ja myös ennen 2025-08-01.
    val opiskeluoikeudet2 = Seq(
      createOpiskeluoikeusWithOppimaara(true, true, SuoritusTila.VALMIS, Some(ennenLeikkuripaivaa))
    )

    Assertions.assertTrue(
      HarkinnanvaraisuusPaattely.hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet2, leikkuriPaiva))
  }

  //Testi tapaukselle, jossa yksilöllistetty suoritus on 2025-08-01 jälkeen, eli yksilöllistämistietoa ei huomioida.
  @Test
  def testHasYksilollistettyWithVahvistusPaivamaaraFilterAfter20250801(): Unit = {
    val leikkuriPaiva = LocalDate.parse("2026-05-30")
    val leikkuriPaivanJalkeen = leikkuriPaiva.plusDays(2)
    val ennenLeikkuripaivaa = LocalDate.parse("2025-10-22")

    // Case 1: Suoritus vahvistettu leikkuripäivän jälkeen
    val opiskeluoikeudet1 = Seq(
      createOpiskeluoikeusWithOppimaara(true, true, SuoritusTila.VALMIS, Some(leikkuriPaivanJalkeen))
    )

    Assertions.assertFalse(
      HarkinnanvaraisuusPaattely.hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet1, leikkuriPaiva))

    // Case 2: Suoritus vahvistettu ennen leikkuripäivää mutta 2025-08-01 jälkeen.
    val opiskeluoikeudet2 = Seq(
      createOpiskeluoikeusWithOppimaara(true, true, SuoritusTila.VALMIS, Some(ennenLeikkuripaivaa))
    )

    Assertions.assertFalse(
      HarkinnanvaraisuusPaattely.hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet2, leikkuriPaiva))
  }

  @Test
  def testSyncHarkinnanvaraisuusEiHarkinnanvarainenHakukohde(): Unit = {
    val hakukohteet = Map(
      HAKUKOHDE_OID_1 -> createHakukohde(HAKUKOHDE_OID_1, true),
      HAKUKOHDE_OID_2 -> createHakukohde(HAKUKOHDE_OID_2, false)
    )

    val hakemus = BASE_HAKEMUS
    val opiskeluoikeudet = Seq[Opiskeluoikeus]()
    val ohjausparametrit = DEFAULT_OHJAUSPARAMETRIT

    val result = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.ATARU_EI_PAATTOTODISTUSTA,
      result.hakutoiveet(0).harkinnanvaraisuudenSyy,
      "First hakukohde should have EI_HARKINNANVARAINEN with empty data"
    )

    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN_HAKUKOHDE,
      result.hakutoiveet(1).harkinnanvaraisuudenSyy,
      "Second hakukohde should have EI_HARKINNANVARAINEN_HAKUKOHDE because it doesn't allow harkinnanvaraisuus"
    )
  }

  @Test
  def testSyncHarkinnanvaraisuusSUREYksMatAi(): Unit = {
    val hakukohteet = Map(
      HAKUKOHDE_OID_1 -> createHakukohde(HAKUKOHDE_OID_1, true),
      HAKUKOHDE_OID_2 -> createHakukohde(HAKUKOHDE_OID_2, true)
    )

    val tuoreinPeruskoulu = createOpiskeluoikeusWithOppimaara(true, true, SuoritusTila.VALMIS, Some(LocalDate.parse("2025-01-01")))
    val opiskeluoikeudet = Seq(tuoreinPeruskoulu)

    val hakemus = BASE_HAKEMUS
    val ohjausparametrit = DEFAULT_OHJAUSPARAMETRIT

    val result = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.SURE_YKS_MAT_AI,
      result.hakutoiveet(0).harkinnanvaraisuudenSyy,
      "Should return SURE_YKS_MAT_AI when both MA and AI are individualized"
    )
  }

 @Test
  def testSyncHarkinnanvaraisuusSUREEiPaattotodistusta(): Unit = {
    val hakukohteet = Map(
      HAKUKOHDE_OID_1 -> createHakukohde(HAKUKOHDE_OID_1, true),
      HAKUKOHDE_OID_2 -> createHakukohde(HAKUKOHDE_OID_2, true)
    )

    val tuoreinPeruskoulu = createOpiskeluoikeusWithOppimaara(false, false, SuoritusTila.KESKEYTYNYT, None)
    val opiskeluoikeudet = Seq(tuoreinPeruskoulu)

    val hakemus = BASE_HAKEMUS
    val ohjausparametrit = DEFAULT_OHJAUSPARAMETRIT

    val result = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA,
      result.hakutoiveet(0).harkinnanvaraisuudenSyy,
      "Should return SURE_EI_PAATTOTODISTUSTA for KESKEYTYNYT status"
    )

    // Test for KESKEN with deadline passed
    val tuoreinPeruskoulu2 = createOpiskeluoikeusWithOppimaara(false, false, SuoritusTila.KESKEN, None)
    val opiskeluoikeudet2 = Seq(tuoreinPeruskoulu2)

    // Create ohjausparametrit with deadline in the past
    val pastDate = LocalDate.now().minusDays(10)
    val pastInstant = pastDate.atStartOfDay(ZoneId.systemDefault()).toInstant()

   val ohjausparametritPastDeadline = DEFAULT_OHJAUSPARAMETRIT.copy(suoritustenVahvistuspaiva = Some(DateParam(pastInstant.toEpochMilli)))

    val result2 = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus, opiskeluoikeudet2, ohjausparametritPastDeadline.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA,
      result2.hakutoiveet(0).harkinnanvaraisuudenSyy,
      "Should return SURE_EI_PAATTOTODISTUSTA for KESKEN status when deadline has passed"
    )
  }

  @Test
  def testSyncHarkinnanvaraisuusAtaruUlkomaillaOpiskeltu(): Unit = {
    val hakukohteet = Map(
      HAKUKOHDE_OID_1 -> createHakukohde(HAKUKOHDE_OID_1, true),
      HAKUKOHDE_OID_2 -> createHakukohde(HAKUKOHDE_OID_2, true)
    )

    val opiskeluoikeudet = Seq[Opiskeluoikeus]()
    val ohjausparametrit = DEFAULT_OHJAUSPARAMETRIT

    val hakemus = BASE_HAKEMUS.copy(
      keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusKey -> AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS
      )
    )

    val result = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.ATARU_ULKOMAILLA_OPISKELTU,
      result.hakutoiveet(0).harkinnanvaraisuudenSyy,
      "Should return ATARU_ULKOMAILLA_OPISKELTU when pohjakoulutus is POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS"
    )
  }

  @Test
  def testSyncHarkinnanvaraisuusAtaruEiPaattotodistusta(): Unit = {
    // Test for ATARU_EI_PAATTOTODISTUSTA case
    val hakukohteet = Map(
      HAKUKOHDE_OID_1 -> createHakukohde(HAKUKOHDE_OID_1, true),
      HAKUKOHDE_OID_2 -> createHakukohde(HAKUKOHDE_OID_2, false)
    )

    val opiskeluoikeudet = Seq[Opiskeluoikeus]()
    val ohjausparametrit = DEFAULT_OHJAUSPARAMETRIT

    // Case 1: Explicit EI_PAATTOTODISTUSTA
    val hakemus1 = BASE_HAKEMUS.copy(
      keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusKey -> AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA
      )
    )

    val result1 = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus1, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(HarkinnanvaraisuudenSyy.ATARU_EI_PAATTOTODISTUSTA, result1.hakutoiveet.find(_.hakukohdeOid.equals(HAKUKOHDE_OID_1)).get.harkinnanvaraisuudenSyy)
    Assertions.assertEquals(HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN_HAKUKOHDE, result1.hakutoiveet.find(_.hakukohdeOid.equals(HAKUKOHDE_OID_2)).get.harkinnanvaraisuudenSyy)

    // Case 2: Missing pohjakoulutus_vuosi
    val hakemus2 = BASE_HAKEMUS

    val result2 = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus2, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(HarkinnanvaraisuudenSyy.ATARU_EI_PAATTOTODISTUSTA, result2.hakutoiveet.find(_.hakukohdeOid.equals(HAKUKOHDE_OID_1)).get.harkinnanvaraisuudenSyy)
    Assertions.assertEquals(HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN_HAKUKOHDE, result2.hakutoiveet.find(_.hakukohdeOid.equals(HAKUKOHDE_OID_2)).get.harkinnanvaraisuudenSyy)


  }

  @Test
  def testSyncHarkinnanvaraisuusAtaruYksMatAi(): Unit = {
    // Test for ATARU_YKS_MAT_AI case
    val hakukohteet = Map(
      HAKUKOHDE_OID_1 -> createHakukohde(HAKUKOHDE_OID_1, true),
      HAKUKOHDE_OID_2 -> createHakukohde(HAKUKOHDE_OID_2, true)
    )

    val opiskeluoikeudet = Seq[Opiskeluoikeus]()
    val ohjausparametrit = DEFAULT_OHJAUSPARAMETRIT

    val hakemus = BASE_HAKEMUS.copy(
      keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2015",
        "matematiikka-ja-aidinkieli-yksilollistetty_1" -> "1"
      )
    )

    val result = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.ATARU_YKS_MAT_AI,
      result.hakutoiveet(0).harkinnanvaraisuudenSyy
    )
    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.ATARU_YKS_MAT_AI,
      result.hakutoiveet(1).harkinnanvaraisuudenSyy
    )
  }

  @Test
  def testSyncHarkinnanvaraisuusHakukohteelleHakemukselta(): Unit = {
    // Test for other harkinnanvaraisuus reasons from hakemus
    val hakukohteet = Map(
      HAKUKOHDE_OID_1 -> createHakukohde(HAKUKOHDE_OID_1, true),
      HAKUKOHDE_OID_2 -> createHakukohde(HAKUKOHDE_OID_2, true)
    )

    val opiskeluoikeudet = Seq[Opiskeluoikeus]()
    val ohjausparametrit = DEFAULT_OHJAUSPARAMETRIT

    // Test for ATARU_OPPIMISVAIKEUDET
    val hakemus1 = BASE_HAKEMUS.copy(
      keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017",
        s"${HarkinnanvaraisuusPaattely.ataruHakukohdeHarkinnanvaraisuusPrefix}${HAKUKOHDE_OID_1}" -> "0"
      )
    )

    val result1 = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus1, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.ATARU_OPPIMISVAIKEUDET,
      result1.hakutoiveet(0).harkinnanvaraisuudenSyy,
      "Should return ATARU_OPPIMISVAIKEUDET when value is 0"
    )

    // Test for ATARU_SOSIAALISET_SYYT
    val hakemus2 = BASE_HAKEMUS.copy(
      keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017",
        s"${HarkinnanvaraisuusPaattely.ataruHakukohdeHarkinnanvaraisuusPrefix}${HAKUKOHDE_OID_1}" -> "1"
      )
    )

    val result2 = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus2, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )
    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.ATARU_SOSIAALISET_SYYT,
      result2.hakutoiveet(0).harkinnanvaraisuudenSyy,
      "Should return ATARU_SOSIAALISET_SYYT when value is 1"
    )

    // Test for ATARU_KOULUTODISTUSTEN_VERTAILUVAIKEUDET
    val hakemus3 = BASE_HAKEMUS.copy(
      keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017",
        s"${HarkinnanvaraisuusPaattely.ataruHakukohdeHarkinnanvaraisuusPrefix}${HAKUKOHDE_OID_1}" -> "2"
      )
    )

    val result3 = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus3, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.ATARU_KOULUTODISTUSTEN_VERTAILUVAIKEUDET,
      result3.hakutoiveet(0).harkinnanvaraisuudenSyy,
      "Should return ATARU_KOULUTODISTUSTEN_VERTAILUVAIKEUDET when value is 2"
    )

    // Test for ATARU_RIITTAMATON_TUTKINTOKIELEN_TAITO
    val hakemus4 = BASE_HAKEMUS.copy(
      keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017",
        s"${HarkinnanvaraisuusPaattely.ataruHakukohdeHarkinnanvaraisuusPrefix}${HAKUKOHDE_OID_1}" -> "3"
      )
    )

    val result4 = HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
      hakemus4, opiskeluoikeudet, ohjausparametrit.getVahvistuspaivaLocalDate, hakukohteet
    )

    Assertions.assertEquals(
      HarkinnanvaraisuudenSyy.ATARU_RIITTAMATON_TUTKINTOKIELEN_TAITO,
      result4.hakutoiveet(0).harkinnanvaraisuudenSyy,
      "Should return ATARU_RIITTAMATON_TUTKINTOKIELEN_TAITO when value is 3"
    )
  }

  private def createOpiskeluoikeusWithOppimaara(maYksilollistetty: Boolean,
                                                aiYksilollistetty: Boolean,
                                                tila: SuoritusTila,
                                                vahvistusPaivamaara: Option[LocalDate] = Some(LocalDate.now().minusDays(30))
                                               ): PerusopetuksenOpiskeluoikeus = {
    val matematiikka = PerusopetuksenOppiaine(
      UUID.randomUUID(),
      Kielistetty(Some("matematiikka"), None, None),
      Koodi("MA", "koodisto", None),
      Koodi("8", "arvosanat", None),
      None,
      true,
      Some(maYksilollistetty),
      None)

    val aidinkieli = PerusopetuksenOppiaine(
      UUID.randomUUID(),
      Kielistetty(Some("äidinkieli"), None, None),
      Koodi("AI", "koodisto", None),
      Koodi("8", "arvosanat", None),
      None,
      true,
      Some(aiYksilollistetty),
      None)

    val oppimaara = PerusopetuksenOppimaara(
      UUID.randomUUID(),
      None,
      Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
      None,
      Koodi("arvo", "koodisto", Some(1)),
      tila,
      Koodi("arvo", "koodisto", Some(1)),
      Set.empty,
      None,
      vahvistusPaivamaara.map(_.minusDays(365)),
      vahvistusPaivamaara,
      Set(matematiikka, aidinkieli),
      Set.empty,
      false,
      false)

    PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      Some("1.2.3"),
      "2.3.4",
      Set(oppimaara),
      None,
      tila,
      List.empty)

  }

  private def createOpiskeluoikeusWithOppiaineenOppimaara(aineKoodi: String,
                                                          yksilollistetty: Boolean,
                                                          vahvistusPaivamaara: LocalDate = LocalDate.now().minusDays(15)
                                                         ): PerusopetuksenOpiskeluoikeus = {
    val aine = PerusopetuksenOppiaine(
      UUID.randomUUID(),
      Kielistetty(Some(s"korotus: ${aineKoodi}"), None, None),
      Koodi(aineKoodi, "koodisto", None),
      Koodi("9", "arvosanat", None),
      None,
      true,
      Some(yksilollistetty),
      None
    )

    val oppiaineenSuoritus = PerusopetuksenOppimaaranOppiaineidenSuoritus(
      UUID.randomUUID(),
      None,
      Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
      Koodi("arvo", "koodisto", Some(1)),
      SuoritusTila.VALMIS,
      Koodi("arvo", "koodisto", Some(1)),
      Some(vahvistusPaivamaara),
      Some(LocalDate.now().minusDays(30)),
      Set(aine),
      false
    )

    PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      Some("1.2.3"),
      "2.3.4",
      Set(oppiaineenSuoritus),
      None,
      SuoritusTila.VALMIS,
      List.empty)
  }

  def createHakukohde(oid: String, harkinnanvaraisetSallittu: Boolean) = {
    KoutaHakukohde(
      oid = oid,
      voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita = Some(harkinnanvaraisetSallittu)
    )
  }
}
