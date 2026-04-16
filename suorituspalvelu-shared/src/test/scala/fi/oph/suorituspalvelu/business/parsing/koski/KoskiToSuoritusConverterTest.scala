package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.VUOSILUOKKA_9
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, GeneerinenOpiskeluoikeus, KantaOperaatiot, Lahtokoulu, LahtokouluTyyppi, Opiskeluoikeus, OpiskeluoikeusJakso, PerusopetuksenOpiskeluoikeus, PerusopetuksenYksilollistaminen, PoistettuOpiskeluoikeus, SuoritusTila}
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiArviointi, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiKoulutusModuuli, KoskiLaajuus, KoskiLisatiedot, KoskiOpiskeluoikeus, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila, KoskiOpiskeluoikeusTyyppi, KoskiOppilaitos, KoskiOsaSuoritus, KoskiParser, KoskiSuoritus, KoskiSuoritusTyyppi, KoskiToSuoritusConverter}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

import java.time.LocalDate
import fi.oph.suorituspalvelu.business.TestDataUtil.{mkJakso, mkOpiskeluoikeusWithTila, mkVuosiluokkaSuoritus}

@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiToSuoritusConverterTest {

  val DUMMY_KOODISTOPROVIDER: KoodistoProvider = koodisto => Map().empty

  def getOikeudetByType(oikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): (Seq[PerusopetuksenOpiskeluoikeus], Seq[AmmatillinenOpiskeluoikeus], Seq[GeneerinenOpiskeluoikeus], Seq[PoistettuOpiskeluoikeus]) = {
    val perusopetukset = oikeudet.collect { case po: PerusopetuksenOpiskeluoikeus => po }
    val ammatilliset = oikeudet.collect { case am: AmmatillinenOpiskeluoikeus => am }
    val geneeriset = oikeudet.collect { case g: GeneerinenOpiskeluoikeus => g }
    val poistetut = oikeudet.collect { case g: PoistettuOpiskeluoikeus => g }
    (perusopetukset, ammatilliset, geneeriset, poistetut)
  }

  @Test def testKoskiParsingAndConversion1(): Unit =
    val fileName = "/1_2_246_562_24_40483869857.json"
    val splitData = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream(fileName))
    val oikeudet = splitData.flatMap(henkilo => {
      henkilo.opiskeluoikeudet.flatMap {
        case Right(oo) =>
          val koskiOpiskeluoikeus = KoskiParser.parseKoskiData(oo.data)
          KoskiToSuoritusConverter.parseOpiskeluoikeudet(Seq(koskiOpiskeluoikeus), DUMMY_KOODISTOPROVIDER)
        case Left(exception) => Assertions.fail(exception)
      }
    }).toSeq

    Assertions.assertEquals(7, oikeudet.size) // kolme mitätöityä opiskeluoikeutta

    val (perusopetukset, ammatilliset, geneeriset, poistetut) = getOikeudetByType(oikeudet)
    Assertions.assertEquals(1, perusopetukset.size)
    Assertions.assertEquals(3, ammatilliset.size)
    Assertions.assertEquals(0, geneeriset.size)
    Assertions.assertEquals(3, poistetut.size)


  @Test def testKoskiParsingAndConversion2(): Unit =
    val fileName = "/1_2_246_562_24_30563266636.json"
    val splitData = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream(fileName))
    val oikeudet = splitData.flatMap(henkilo => {
      henkilo.opiskeluoikeudet.flatMap {
        case Right(oo) =>
          val koskiOpiskeluoikeus = KoskiParser.parseKoskiData(oo.data)
          KoskiToSuoritusConverter.parseOpiskeluoikeudet(Seq(koskiOpiskeluoikeus), DUMMY_KOODISTOPROVIDER)
        case Left(exception) => Assertions.fail(exception)
      }
    }).toSeq

    Assertions.assertEquals(8, oikeudet.size) // kuusi mitätöityä opiskeluoikeutta

    val (perusopetukset, ammatilliset, geneeriset, poistetut) = getOikeudetByType(oikeudet)
    Assertions.assertEquals(1, perusopetukset.size)
    Assertions.assertEquals(0, ammatilliset.size)
    Assertions.assertEquals(1, geneeriset.size)
    Assertions.assertEquals(6, poistetut.size)

  @Test def testKoskiParsingForPerusopetuksenOpiskeluoikeudenSuoritukset(): Unit = {
    val fileName = "/oo_1.2.246.562.15.94501385358.json"
    val splitData = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream(fileName)).toList
    val oikeudet = splitData.flatMap(henkilo => {
      henkilo.opiskeluoikeudet.flatMap {
        case Right(oo) =>
          val koskiOpiskeluoikeus = KoskiParser.parseKoskiData(oo.data)
          KoskiToSuoritusConverter.parseOpiskeluoikeudet(Seq(koskiOpiskeluoikeus), DUMMY_KOODISTOPROVIDER)
        case Left(exception) => Assertions.fail(exception)
      }
     })

    // Tarkistetaan että löytyy yksi perusopetuksen opiskeluoikeusopiskeluoikeus, joka sisältää neljä suoritusta
    // (perusopetuksen oppimäärän ja kolme vuosiluokkaa). Tästä seuraa yksi parseroitu suoritus koska vuosiluokista
    // tarvittavat tiedot (lähtökoulu) yhdistetään oppimäärän parseroituun suoritukseen
    Assertions.assertEquals(oikeudet.size, 1)
    Assertions.assertEquals(oikeudet.head.asInstanceOf[PerusopetuksenOpiskeluoikeus].suoritukset.size, 1)
  }

  @Test def testKoskiParsingForAmmatillistenOpiskeluoikeuksienSuoritukset(): Unit = {
    val fileName = "/1_2_246_562_24_56916824272.json"
    val splitData = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream(fileName)).toList
    val oikeudet = splitData.flatMap(henkilo => {
      henkilo.opiskeluoikeudet.flatMap {
        case Right(oo) =>
          val koskiOpiskeluoikeus = KoskiParser.parseKoskiData(oo.data)
          KoskiToSuoritusConverter.parseOpiskeluoikeudet(Seq(koskiOpiskeluoikeus), DUMMY_KOODISTOPROVIDER)
            .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
            .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
        case Left(exception) => Assertions.fail(exception)
      }
    })

    //Pitäisi syntyä kolme ammatillista opiskeluoikeutta, joista kahdella on yksi suoritus ja yhdellä ei suorituksia.
    Assertions.assertEquals(oikeudet.size, 3)
    Assertions.assertEquals(1, oikeudet.find(o => o.oid == "1.2.246.562.15.79299730741").get.suoritukset.size)
    Assertions.assertEquals(1, oikeudet.find(o => o.oid == "1.2.246.562.15.24748024759").get.suoritukset.size)
    Assertions.assertEquals(0, oikeudet.find(o => o.oid == "1.2.246.562.15.54761186631").get.suoritukset.size)
  }

  @Test
  def testGetYksilollistaminen(): Unit = {
    val baseSuoritus = KoskiSuoritus(
      null, null, null, null, null, null,
      osasuoritukset = Some(Set.empty), null, null, null, null, null, null, null
    )

    val baseOikeus =  KoskiOpiskeluoikeus(
      null, null, null, null, null, lisätiedot = None, None
    )

    def createOsaSuoritus(aine: String, yksilollistetty: Boolean, rajattu: Boolean): KoskiOsaSuoritus = {
      KoskiOsaSuoritus(
        null, koulutusmoduuli = Some(KoskiKoulutusModuuli(tunniste = Some(KoskiKoodi(aine, "oppiaineet", null, null, null)), null, null, null, null, null, null)), null,
        predictedArviointi = None,
        `yksilöllistettyOppimäärä` = if (yksilollistetty) Some(true) else None,
        `rajattuOppimäärä` = if (rajattu) Some(true) else None,
        suorituskieli = None, null, null, None
      )
    }

    // Ei erityisiä aineita
    val suoritusVainTavallisia = baseSuoritus.copy(osasuoritukset = Some(Set(createOsaSuoritus("HI", false, false))))
    Assertions.assertEquals(None, KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, suoritusVainTavallisia))

    // Ei osasuorituksia
    Assertions.assertEquals(None, KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus))

    // Toiminta-alueittain
    val opiskeluoikeusToimintaAlueittain = baseOikeus.copy(lisätiedot = Some(KoskiLisatiedot(erityisenTuenPäätökset = Some(List(KoskiErityisenTuenPaatos(Some(true)))), vuosiluokkiinSitoutumatonOpetus = None, kotiopetusjaksot = None)))
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY), KoskiToSuoritusConverter.getYksilollistaminen(opiskeluoikeusToimintaAlueittain, baseSuoritus))

    // Vain yksilöllistettyjä, osittain (1 <= 3/2)
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("BI", false, false), createOsaSuoritus("MA", true, false), createOsaSuoritus("LI", false, false))))))

    // Vain yksilöllistettyjä, pääosin (2 > 3/2)
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("LI", true, false), createOsaSuoritus("AI", true, false), createOsaSuoritus("A1", false, false))))))

    // Vain rajattuja, osittain (1 <= 3/2)
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_RAJATTU),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("GE", false, false), createOsaSuoritus("BI", false, true), createOsaSuoritus("HI", false, false))))))

    // Vain rajattuja, pääosin (2 > 3/2)
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_RAJATTU),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("KE", false, true), createOsaSuoritus("B1", false, true), createOsaSuoritus("A1", false, false))))))

    // Sekä yksilöllistettyjä että rajattuja, yksilöllistettyjä enemmän, osittain (yht 2+1 <= 6/2)
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("HI", false, true), createOsaSuoritus("MA", true, false), createOsaSuoritus("FY", true, false),
        createOsaSuoritus("GE", false, false), createOsaSuoritus("BI", false, false), createOsaSuoritus("LI", false, false))))))

    // Sekä yksilöllistettyjä että rajattuja, yksilöllistettyjä enemmän, pääosin (yht 2+1 > 4/2)
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("HI", false, true), createOsaSuoritus("MA", true, false), createOsaSuoritus("GE", false, false), createOsaSuoritus("FY", true, false))))))

    // Sekä yksilöllistettyjä että rajattuja, rajattuja enemmän, osittain (yht 1+2 <= 6/2)
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_RAJATTU),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("HI", false, true), createOsaSuoritus("MA", false, true), createOsaSuoritus("FY", true, false),
        createOsaSuoritus("GE", false, false), createOsaSuoritus("BI", false, false), createOsaSuoritus("LI", false, false))))))

    // Sekä yksilöllistettyjä että rajattuja, rajattuja enemmän, pääosin (yht 1+2 > 4/2)
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_RAJATTU),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("HI", false, true), createOsaSuoritus("MA", false, true), createOsaSuoritus("GE", false, false), createOsaSuoritus("FY", true, false))))))

    // Yhtä paljon yksilöllistettyjä ja rajattuja, osittain (yht 1+1 <= 4/2) => rajattu
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_RAJATTU),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("HI", false, true), createOsaSuoritus("FY", true, false),
        createOsaSuoritus("GE", false, false), createOsaSuoritus("BI", false, false))))))

    // Yhtä paljon yksilöllistettyjä ja rajattuja, pääosin (yht 1+1 > 3/2) => rajattu
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_RAJATTU),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("HI", false, true), createOsaSuoritus("MA", false, false), createOsaSuoritus("FY", true, false))))))

    // Rajatapaus: tasan puolet erityisiä => osittain (2+0 <= 4/2)
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY),
      KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus.copy(osasuoritukset = Some(Set(
        createOsaSuoritus("HI", true, false), createOsaSuoritus("MA", true, false), createOsaSuoritus("LI", false, false), createOsaSuoritus("GE", false, false))))))
  }

  @Test def testParseKeskeytyminen(): Unit = {
    // Keskeytynyt opiskeluoikeus palauttaa keskeytymishetken
    val opiskeluoikeusEronnut = KoskiOpiskeluoikeus(
      "1.2.3",
      None,
      Some(KoskiOpiskeluoikeusTyyppi("arvo", "koodisto", None)),
      Some(KoskiOpiskeluoikeusTila(List(
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2024-01-01"),
          KoskiKoodi("lasna", "koodisto", None, Kielistetty(None, None, None), None)
        ),
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2024-06-15"),
          KoskiKoodi("eronnut", "koodisto", None, Kielistetty(None, None, None), None)
        )))),
      Some(Set.empty),
      None,
      None
    )
    Assertions.assertEquals(Some(LocalDate.parse("2024-06-15")), KoskiToSuoritusConverter.parseKeskeytyminen(opiskeluoikeusEronnut))

    // Ei keskeytynyt palauttaa None
    val opiskeluoikeusValmis = KoskiOpiskeluoikeus(
      "1.2.3",
      None,
      Some(KoskiOpiskeluoikeusTyyppi("arvo", "koodisto", None)),
      Some(KoskiOpiskeluoikeusTila(List(
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2024-01-01"),
          KoskiKoodi("lasna", "koodisto", None, Kielistetty(None, None, None), None)
        ),
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2024-06-15"),
          KoskiKoodi("valmistunut", "koodisto", None, Kielistetty(None, None, None), None)
        )))),
      Some(Set.empty),
      None,
      None
    )
    Assertions.assertEquals(None, KoskiToSuoritusConverter.parseKeskeytyminen(opiskeluoikeusValmis))

    // Jos ei tilaa palautuu None
    val opiskeluoikeusNoTila = KoskiOpiskeluoikeus(
      "1.2.3",
      None,
      Some(KoskiOpiskeluoikeusTyyppi("arvo", "koodisto", None)),
      None,
      Some(Set.empty),
      None,
      None
    )
    Assertions.assertEquals(None, KoskiToSuoritusConverter.parseKeskeytyminen(opiskeluoikeusNoTila))
  }

  @Test def testIsMitatoity(): Unit = {
    val opiskeluoikeus = KoskiOpiskeluoikeus(
      "1.2.3",
      None,
      Some(KoskiOpiskeluoikeusTyyppi("arvo", "koodisto", None)),
      Some(KoskiOpiskeluoikeusTila(List(
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2025-01-01"),
          KoskiKoodi("mitatoity", "koodisto", None, Kielistetty(None, None, None), None)
        ),
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2025-05-05"),
          KoskiKoodi("läsnä", "koodisto", None, Kielistetty(None, None, None), None)
        )))),
      Some(Set.empty),
      None,
      None
    )

    // Opiskeluoikeus on mitätöity jos se on milloinkaan ollut mitätöity. Tämä johtuu siitä että
    // KOSKI-datassa on opiskeluoikeuksia jotka on laitettu alkamaan tulevaisuudessa ja sitten mitätöity
    // nykyhetkeen.
    Assertions.assertTrue(KoskiToSuoritusConverter.isMitatoitu(opiskeluoikeus))
  }

  @Test
  def testGetLisapistekoulutusYhteenlaskettuLaajuusEiOsasuorituksia(): Unit = {
    val suoritus = KoskiSuoritus(
      tyyppi = KoskiSuoritusTyyppi("tuvakoulutuksensuoritus", "suorituksentyyppi", Kielistetty(Some("Tuva-koulutus"), None, None)),
      koulutusmoduuli = None,
      suorituskieli = None,
      koulusivistyskieli = None,
      alkamispäivä = None,
      vahvistus = None,
      osasuoritukset = None,
      arviointi = None,
      keskiarvo = None,
      korotettuKeskiarvo = None,
      korotettuOpiskeluoikeusOid = None,
      suoritustapa = None,
      luokka = None,
      jääLuokalle = None
    )

    val result = KoskiToSuoritusConverter.getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, false)

    Assertions.assertEquals(None, result)
  }

  @Test
  def testGetLisapistekoulutusYhteenlaskettuLaajuusTyhjatOsasuoritukset(): Unit = {
    val suoritus = KoskiSuoritus(
      tyyppi = KoskiSuoritusTyyppi("tuvakoulutuksensuoritus", "suorituksentyyppi", Kielistetty(Some("Tuva-koulutus"), None, None)),
      koulutusmoduuli = None,
      suorituskieli = None,
      koulusivistyskieli = None,
      alkamispäivä = None,
      vahvistus = None,
      osasuoritukset = Some(Set.empty),
      arviointi = None,
      keskiarvo = None,
      korotettuKeskiarvo = None,
      korotettuOpiskeluoikeusOid = None,
      suoritustapa = None,
      luokka = None,
      jääLuokalle = None
    )

    val result = KoskiToSuoritusConverter.getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, false)

    Assertions.assertEquals(None, result)
  }

  @Test
  def testGetLisapistekoulutusYhteenlaskettuLaajuusEiLaajuuttaOsasuorituksella(): Unit = {
    val osasuoritus = KoskiOsaSuoritus(
      tyyppi = KoskiSuoritusTyyppi("osasuoritus", "suorituksentyyppi", Kielistetty(Some("Osasuoritus"), None, None)),
      koulutusmoduuli = Some(KoskiKoulutusModuuli(
        tunniste = Some(KoskiKoodi(
          koodiarvo = "123",
          koodistoUri = "osasuorituksentyyppi",
          koodistoVersio = Some(1),
          nimi = Kielistetty(Some("Osasuoritus"), None, None),
          lyhytNimi = None
        )),
        laajuus = None,
        kieli = None,
        pakollinen = None,
        koulutustyyppi = None,
        osaAlue = None,
        ryhmä = None
      )),
      arviointi = None,
      predictedArviointi = None,
      yksilöllistettyOppimäärä = None,
      rajattuOppimäärä = None,
      suorituskieli = None,
      vastaavuustodistuksenTiedot = None,
      osasuoritukset = None,
      korotettu = None
    )

    val suoritus = KoskiSuoritus(
      tyyppi = KoskiSuoritusTyyppi("tuvakoulutuksensuoritus", "suorituksentyyppi", Kielistetty(Some("Tuva-koulutus"), None, None)),
      koulutusmoduuli = None,
      suorituskieli = None,
      koulusivistyskieli = None,
      alkamispäivä = None,
      vahvistus = None,
      osasuoritukset = Some(Set(osasuoritus)),
      arviointi = None,
      keskiarvo = None,
      korotettuKeskiarvo = None,
      korotettuOpiskeluoikeusOid = None,
      suoritustapa = None,
      luokka = None,
      jääLuokalle = None
    )

    val result = KoskiToSuoritusConverter.getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, false)

    Assertions.assertEquals(None, result)
  }

  @Test
  def testGetLisapistekoulutusYhteenlaskettuLaajuusPuuttuvaLaajuudenYksikkoOsasuorituksella(): Unit = {
    val osasuoritus = KoskiOsaSuoritus(
      tyyppi = KoskiSuoritusTyyppi("osasuoritus", "suorituksentyyppi", Kielistetty(Some("Osasuoritus"), None, None)),
      koulutusmoduuli = Some(KoskiKoulutusModuuli(
        tunniste = Some(KoskiKoodi(
          koodiarvo = "123",
          koodistoUri = "osasuorituksentyyppi",
          koodistoVersio = Some(1),
          nimi = Kielistetty(Some("Osasuoritus"), None, None),
          lyhytNimi = None
        )),
        laajuus = Some(KoskiLaajuus(
          arvo = 10,
          yksikkö = None
        )),
        kieli = None,
        pakollinen = None,
        koulutustyyppi = None,
        osaAlue = None,
        ryhmä = None
      )),
      arviointi = None,
      predictedArviointi = None,
      yksilöllistettyOppimäärä = None,
      rajattuOppimäärä = None,
      suorituskieli = None,
      vastaavuustodistuksenTiedot = None,
      osasuoritukset = None,
      korotettu = None
    )

    val suoritus = KoskiSuoritus(
      tyyppi = KoskiSuoritusTyyppi("tuvakoulutuksensuoritus", "suorituksentyyppi", Kielistetty(Some("Tuva-koulutus"), None, None)),
      koulutusmoduuli = None,
      suorituskieli = None,
      koulusivistyskieli = None,
      alkamispäivä = None,
      vahvistus = None,
      osasuoritukset = Some(Set(osasuoritus)),
      arviointi = None,
      keskiarvo = None,
      korotettuKeskiarvo = None,
      korotettuOpiskeluoikeusOid = None,
      suoritustapa = None,
      luokka = None,
      jääLuokalle = None,
    )
    val result = KoskiToSuoritusConverter.getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, false)

    Assertions.assertEquals(None, result)
  }

  @Test
  def testGetLisapistekoulutusYhteenlaskettuLaajuusKaikilleOsasuorituksilleMyosEiHyvaksytyt(): Unit = {
    val yksikko = KoskiKoodi(
      koodiarvo = "op",
      koodistoUri = "opintojenlaajuusyksikko",
      koodistoVersio = Some(1),
      nimi = Kielistetty(Some("opintopistettä"), None, None),
      lyhytNimi = None
    )

    val osasuoritus1 = KoskiOsaSuoritus(
      tyyppi = KoskiSuoritusTyyppi("osasuoritus", "suorituksentyyppi", Kielistetty(Some("Osasuoritus 1"), None, None)),
      koulutusmoduuli = Some(KoskiKoulutusModuuli(
        tunniste = Some(KoskiKoodi(
          koodiarvo = "123",
          koodistoUri = "osasuorituksentyyppi",
          koodistoVersio = Some(1),
          nimi = Kielistetty(Some("Osasuoritus 1"), None, None),
          lyhytNimi = None
        )),
        laajuus = Some(KoskiLaajuus(
          arvo = 5,
          yksikkö = Some(yksikko)
        )),
        kieli = None,
        pakollinen = None,
        koulutustyyppi = None,
        osaAlue = None,
        ryhmä = None
      )),
      arviointi = None,
      predictedArviointi = None,
      yksilöllistettyOppimäärä = None,
      rajattuOppimäärä = None,
      suorituskieli = None,
      vastaavuustodistuksenTiedot = None,
      osasuoritukset = None,
      korotettu = None
    )

    val osasuoritus2 = KoskiOsaSuoritus(
      tyyppi = KoskiSuoritusTyyppi("osasuoritus", "suorituksentyyppi", Kielistetty(Some("Osasuoritus 2"), None, None)),
      koulutusmoduuli = Some(KoskiKoulutusModuuli(
        tunniste = Some(KoskiKoodi(
          koodiarvo = "456",
          koodistoUri = "osasuorituksentyyppi",
          koodistoVersio = Some(1),
          nimi = Kielistetty(Some("Osasuoritus 2"), None, None),
          lyhytNimi = None
        )),
        laajuus = Some(KoskiLaajuus(
          arvo = 10,
          yksikkö = Some(yksikko)
        )),
        kieli = None,
        pakollinen = None,
        koulutustyyppi = None,
        osaAlue = None,
        ryhmä = None
      )),
      arviointi = None,
      predictedArviointi = None,
      yksilöllistettyOppimäärä = None,
      rajattuOppimäärä = None,
      suorituskieli = None,
      vastaavuustodistuksenTiedot = None,
      osasuoritukset = None,
      korotettu = None
    )

    val suoritus = KoskiSuoritus(
      tyyppi = KoskiSuoritusTyyppi("tuvakoulutuksensuoritus", "suorituksentyyppi", Kielistetty(Some("Tuva-koulutus"), None, None)),
      koulutusmoduuli = None,
      suorituskieli = None,
      koulusivistyskieli = None,
      alkamispäivä = None,
      vahvistus = None,
      osasuoritukset = Some(Set(osasuoritus1, osasuoritus2)),
      arviointi = None,
      keskiarvo = None,
      korotettuKeskiarvo = None,
      korotettuOpiskeluoikeusOid = None,
      suoritustapa = None,
      luokka = None,
      jääLuokalle = None
    )

    val result = KoskiToSuoritusConverter.getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, false)

    Assertions.assertTrue(result.isDefined)
    Assertions.assertEquals(15, result.get.arvo.intValue)
    Assertions.assertEquals("op", result.get.yksikko.arvo)
    Assertions.assertEquals("opintojenlaajuusyksikko", result.get.yksikko.koodisto)
  }

  @Test
  def testGetLisapistekoulutusYhteenlaskettuLaajuusVainHyvaksytyilleOsasuorituksille(): Unit = {
    val yksikko = KoskiKoodi(
      koodiarvo = "op",
      koodistoUri = "opintojenlaajuusyksikko",
      koodistoVersio = Some(1),
      nimi = Kielistetty(Some("opintopistettä"), None, None),
      lyhytNimi = None
    )

    // Approved osasuoritus
    val osasuoritus1 = KoskiOsaSuoritus(
      tyyppi = KoskiSuoritusTyyppi("osasuoritus", "suorituksentyyppi", Kielistetty(Some("Osasuoritus 1"), None, None)),
      koulutusmoduuli = Some(KoskiKoulutusModuuli(
        tunniste = Some(KoskiKoodi(
          koodiarvo = "123",
          koodistoUri = "osasuorituksentyyppi",
          koodistoVersio = Some(1),
          nimi = Kielistetty(Some("Osasuoritus 1"), None, None),
          lyhytNimi = None
        )),
        laajuus = Some(KoskiLaajuus(
          arvo = 5,
          yksikkö = Some(yksikko)
        )),
        kieli = None,
        pakollinen = None,
        koulutustyyppi = None,
        osaAlue = None,
        ryhmä = None
      )),
      arviointi = Some(Set(
        KoskiArviointi(
          arvosana = KoskiKoodi("Hyväksytty", "arviointiasteikkoammatillinenhyvaksyttyhylatty", Some(1), Kielistetty(Some("Hyväksytty"), None, None), None),
          hyväksytty = true,
          päivä = None
        )
      )),
      predictedArviointi = None,
      yksilöllistettyOppimäärä = None,
      rajattuOppimäärä = None,
      suorituskieli = None,
      vastaavuustodistuksenTiedot = None,
      osasuoritukset = None,
      korotettu = None
    )

    // Non-approved osasuoritus
    val osasuoritus2 = KoskiOsaSuoritus(
      tyyppi = KoskiSuoritusTyyppi("osasuoritus", "suorituksentyyppi", Kielistetty(Some("Osasuoritus 2"), None, None)),
      koulutusmoduuli = Some(KoskiKoulutusModuuli(
        tunniste = Some(KoskiKoodi(
          koodiarvo = "456",
          koodistoUri = "osasuorituksentyyppi",
          koodistoVersio = Some(1),
          nimi = Kielistetty(Some("Osasuoritus 2"), None, None),
          lyhytNimi = None
        )),
        laajuus = Some(KoskiLaajuus(
          arvo = 10,
          yksikkö = Some(yksikko)
        )),
        kieli = None,
        pakollinen = None,
        koulutustyyppi = None,
        osaAlue = None,
        ryhmä = None
      )),
      arviointi = Some(Set(
        KoskiArviointi(
          arvosana = KoskiKoodi("Hylätty", "arviointiasteikkoammatillinenhyvaksyttyhylatty", Some(1), Kielistetty(Some("Hylätty"), None, None), None),
          hyväksytty = false,
          päivä = None
        )
      )),
      predictedArviointi = None,
      yksilöllistettyOppimäärä = None,
      rajattuOppimäärä = None,
      suorituskieli = None,
      vastaavuustodistuksenTiedot = None,
      osasuoritukset = None,
      korotettu = None
    )

    val suoritus = KoskiSuoritus(
      tyyppi = KoskiSuoritusTyyppi("tuvakoulutuksensuoritus", "suorituksentyyppi", Kielistetty(Some("Tuva-koulutus"), None, None)),
      koulutusmoduuli = None,
      suorituskieli = None,
      koulusivistyskieli = None,
      alkamispäivä = None,
      vahvistus = None,
      osasuoritukset = Some(Set(osasuoritus1, osasuoritus2)),
      arviointi = None,
      keskiarvo = None,
      korotettuKeskiarvo = None,
      korotettuOpiskeluoikeusOid = None,
      suoritustapa = None,
      luokka = None,
      jääLuokalle = None
    )

    val result = KoskiToSuoritusConverter.getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, true)

    Assertions.assertTrue(result.isDefined)
    Assertions.assertEquals(5, result.get.arvo.intValue) // Vain osasuoritus laajuudella 5 huomioitu. Osasuoritus laajuudella 10 jäi pois.
    Assertions.assertEquals("op", result.get.yksikko.arvo)
    Assertions.assertEquals("opintojenlaajuusyksikko", result.get.yksikko.koodisto)
  }

  @Test
  def testGetLisapistekoulutusYhteenlaskettuLaajuusTyhjäArviointiJoukko(): Unit = {
    // Arrange
    val yksikko = KoskiKoodi(
      koodiarvo = "op",
      koodistoUri = "opintojenlaajuusyksikko",
      koodistoVersio = Some(1),
      nimi = Kielistetty(Some("opintopistettä"), None, None),
      lyhytNimi = None
    )

    // Osasuoritus with empty evaluation set
    val osasuoritus = KoskiOsaSuoritus(
      tyyppi = KoskiSuoritusTyyppi("osasuoritus", "suorituksentyyppi", Kielistetty(Some("Osasuoritus"), None, None)),
      koulutusmoduuli = Some(KoskiKoulutusModuuli(
        tunniste = Some(KoskiKoodi(
          koodiarvo = "123",
          koodistoUri = "osasuorituksentyyppi",
          koodistoVersio = Some(1),
          nimi = Kielistetty(Some("Osasuoritus"), None, None),
          lyhytNimi = None
        )),
        laajuus = Some(KoskiLaajuus(
          arvo = 5,
          yksikkö = Some(yksikko)
        )),
        kieli = None,
        pakollinen = None,
        koulutustyyppi = None,
        osaAlue = None,
        `ryhmä` = None
      )),
      arviointi = Some(Set.empty),
      predictedArviointi = None,
      yksilöllistettyOppimäärä = None,
      rajattuOppimäärä = None,
      suorituskieli = None,
      vastaavuustodistuksenTiedot = None,
      osasuoritukset = None,
      korotettu = None
    )

    val suoritus = KoskiSuoritus(
      tyyppi = KoskiSuoritusTyyppi("tuvakoulutuksensuoritus", "suorituksentyyppi", Kielistetty(Some("Tuva-koulutus"), None, None)),
      koulutusmoduuli = None,
      suorituskieli = None,
      koulusivistyskieli = None,
      alkamispäivä = None,
      vahvistus = None,
      osasuoritukset = Some(Set(osasuoritus)),
      arviointi = None,
      keskiarvo = None,
      korotettuKeskiarvo = None,
      korotettuOpiskeluoikeusOid = None,
      suoritustapa = None,
      luokka = None,
      jääLuokalle = None
    )

    // Should include the osasuoritus when vainHyvaksytytArvioinnit is false
    val resultFalse = KoskiToSuoritusConverter.getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, false)
    Assertions.assertTrue(resultFalse.isDefined)
    Assertions.assertEquals(5, resultFalse.get.arvo.intValue)

    // Should not include the osasuoritus when vainHyvaksytytArvioinnit is true
    val resultTrue = KoskiToSuoritusConverter.getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, true)
    Assertions.assertTrue(resultTrue.isDefined)
    Assertions.assertEquals(0, resultTrue.get.arvo.intValue)
  }

  // --- parseLasnaolot tests ---

  @Test def testParseLasnaolotMultipleLasnaWithGap(): Unit = {
    val oo = mkOpiskeluoikeusWithTila(
      mkJakso("2020-01-01", "lasna"),
      mkJakso("2020-03-01", "eronnut"),
      // ------------------------------ ennen tätä leikkautuu pois (ennen aloituspäivämäärää)
      mkJakso("2024-01-01", "lasna"), // osa leikautuu pois (ennen aloituspäivämäärää)
      mkJakso("2024-03-01", "eronnut"),
      mkJakso("2024-05-01", "lasna"),
      mkJakso("2024-08-01", "eronnut"), // osa leikkautuu pois (vahvistuspäivämäärän jälkeen)
      // ------------------------------ tämän jälkeen leikkautuu pois (vahvistuspäivämäärän jälkeen)
      mkJakso("2025-05-01", "lasna"),
      mkJakso("2025-08-01", "valmistunut")
    )
    val result = KoskiToSuoritusConverter.parseLasnaolot(oo, Some(LocalDate.parse("2024-02-01")), Some(LocalDate.parse("2024-07-01")))
    Assertions.assertEquals(List(
      (LocalDate.parse("2024-02-01"), Some(LocalDate.parse("2024-03-01"))),
      (LocalDate.parse("2024-05-01"), Some(LocalDate.parse("2024-07-01")))
    ), result)
  }

  // --- getPerusopetuksenLahtokoulut tests ---

  private def mkOpiskeluoikeusForLahtokoulut(suoritukset: Set[KoskiSuoritus], jaksot: KoskiOpiskeluoikeusJakso*): KoskiOpiskeluoikeus =
    KoskiOpiskeluoikeus(
      "1.2.246.562.15.123",
      Some(KoskiOppilaitos(Kielistetty(Some("Testikoulu"), None, None), "1.2.246.562.10.999")),
      None,
      Some(KoskiOpiskeluoikeusTila(jaksot.toList)),
      Some(suoritukset),
      None,
      None
    )

  @Test def testGetPerusopetuksenLahtokoulutNoMatchingLuokkaAste(): Unit = {
    val suoritus = mkVuosiluokkaSuoritus("8", alkamispaiva = Some("2024-01-01"))
    val oo = mkOpiskeluoikeusForLahtokoulut(
      Set(suoritus),
      mkJakso("2024-01-01", "lasna")
    )
    val result = KoskiToSuoritusConverter.getPerusopetuksenLahtokoulut(oo, "9", false, None, DUMMY_KOODISTOPROVIDER)
    Assertions.assertEquals(List.empty, result)
  }

  @Test def testGetPerusopetuksenLahtokoulutMissingAlkamispaiva(): Unit = {
    val suoritus = mkVuosiluokkaSuoritus("9", alkamispaiva = None)
    val oo = mkOpiskeluoikeusForLahtokoulut(
      Set(suoritus),
      mkJakso("2024-01-01", "lasna")
    )
    val result = KoskiToSuoritusConverter.getPerusopetuksenLahtokoulut(oo, "9", false, None, DUMMY_KOODISTOPROVIDER)
    Assertions.assertEquals(List.empty, result)
  }

  @Test def testGetPerusopetuksenLahtokoulutSingleMatchSinglePeriod(): Unit = {
    val suoritus = mkVuosiluokkaSuoritus("9", alkamispaiva = Some("2024-08-01"), vahvistuspaiva = Some("2025-06-01"), luokka = Some("9B"))
    val oo = mkOpiskeluoikeusForLahtokoulut(
      Set(suoritus),
      mkJakso("2024-01-01", "lasna"),
      mkJakso("2025-06-01", "valmistunut")
    )
    Assertions.assertEquals(
      List(Lahtokoulu(LocalDate.parse("2024-08-01"), Some(LocalDate.parse("2025-06-01")), oo.oppilaitos.get.oid, Some(2025), "9B", Some(VALMIS), Some(true), VUOSILUOKKA_9)),
      KoskiToSuoritusConverter.getPerusopetuksenLahtokoulut(oo, "9", true, None, DUMMY_KOODISTOPROVIDER))
  }

  @Test def testGetPerusopetuksenLahtokoulutInterruptedLasnaKesken(): Unit = {
    val suoritus = mkVuosiluokkaSuoritus("9", alkamispaiva = Some("2024-01-01"), luokka = Some("9A"))
    val oo = mkOpiskeluoikeusForLahtokoulut(
      Set(suoritus),
      mkJakso("2024-01-01", "lasna"),
      mkJakso("2024-03-01", "eronnut"),
      mkJakso("2024-05-01", "lasna")
    )

    Assertions.assertEquals(List(
      Lahtokoulu(LocalDate.parse("2024-05-01"), None, oo.oppilaitos.get.oid, Some(2025), "9A", Some(KESKEN), Some(true), VUOSILUOKKA_9),
      Lahtokoulu(LocalDate.parse("2024-01-01"), Some(LocalDate.parse("2024-03-01")), oo.oppilaitos.get.oid, Some(2025), "9A", Some(KESKEN), Some(true), VUOSILUOKKA_9)),
      KoskiToSuoritusConverter.getPerusopetuksenLahtokoulut(oo, "9", true, None, DUMMY_KOODISTOPROVIDER))
  }

  @Test def testGetPerusopetuksenLahtokoulutInterruptedLasnaValmistunut(): Unit = {
    val suoritus = mkVuosiluokkaSuoritus("9", alkamispaiva = Some("2024-01-01"), vahvistuspaiva = Some("2024-08-01"), luokka = Some("9A"))
    val oo = mkOpiskeluoikeusForLahtokoulut(
      Set(suoritus),
      mkJakso("2024-01-01", "lasna"),
      mkJakso("2024-03-01", "eronnut"),
      mkJakso("2024-05-01", "lasna"),
      mkJakso("2024-08-01", "valmistunut")
    )

    Assertions.assertEquals(List(
      Lahtokoulu(LocalDate.parse("2024-05-01"), Some(LocalDate.parse("2024-08-01")), oo.oppilaitos.get.oid, Some(2024), "9A", Some(VALMIS), Some(true), VUOSILUOKKA_9),
      Lahtokoulu(LocalDate.parse("2024-01-01"), Some(LocalDate.parse("2024-03-01")), oo.oppilaitos.get.oid, Some(2024), "9A", Some(VALMIS), Some(true), VUOSILUOKKA_9)),
      KoskiToSuoritusConverter.getPerusopetuksenLahtokoulut(oo, "9", true, None, DUMMY_KOODISTOPROVIDER))
  }
}
