package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.KantaOperaatiot.KantaEntiteetit.{AMMATILLINEN_OPISKELUOIKEUS, GENEERINEN_OPISKELUOIKEUS, PERUSOPETUKSEN_OPISKELUOIKEUS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, GeneerinenOpiskeluoikeus, KantaOperaatiot, Opiskeluoikeus, OpiskeluoikeusJakso, PerusopetuksenOpiskeluoikeus, PerusopetuksenYksilollistaminen}
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiKoulutusModuuli, KoskiLisatiedot, KoskiOpiskeluoikeus, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila, KoskiOpiskeluoikeusTyyppi, KoskiOsaSuoritus, KoskiParser, KoskiSuoritus, KoskiToSuoritusConverter}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

import java.time.LocalDate

@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiToSuoritusConverterTest {

  val DUMMY_KOODISTOPROVIDER: KoodistoProvider = koodisto => Map().empty

  def getOikeudetByType(oikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): (Seq[PerusopetuksenOpiskeluoikeus], Seq[AmmatillinenOpiskeluoikeus], Seq[GeneerinenOpiskeluoikeus]) = {
    val perusopetukset = oikeudet.collect { case po: PerusopetuksenOpiskeluoikeus => po }
    val ammatilliset = oikeudet.collect { case am: AmmatillinenOpiskeluoikeus => am }
    val geneeriset = oikeudet.collect { case g: GeneerinenOpiskeluoikeus => g }
    (perusopetukset, ammatilliset, geneeriset)
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

    Assertions.assertEquals(4, oikeudet.size) // kolme mitätöityä opiskeluoikeutta

    val (perusopetukset, ammatilliset, geneeriset) = getOikeudetByType(oikeudet)
    Assertions.assertEquals(1, perusopetukset.size)
    Assertions.assertEquals(3, ammatilliset.size)
    Assertions.assertEquals(0, geneeriset.size)


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

    Assertions.assertEquals(2, oikeudet.size) // kuusi mitätöityä opiskeluoikeutta

    val (perusopetukset, ammatilliset, geneeriset) = getOikeudetByType(oikeudet)
    Assertions.assertEquals(1, perusopetukset.size)
    Assertions.assertEquals(0, ammatilliset.size)
    Assertions.assertEquals(1, geneeriset.size)


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
      osasuoritukset = Some(Set.empty), null, null, null, null, null
    )

    val baseOikeus =  KoskiOpiskeluoikeus(
      null, null, null, null, null, lisätiedot = None
    )

    def createOsaSuoritus(aine: String, yksilollistetty: Boolean, rajattu: Boolean): KoskiOsaSuoritus = {
      KoskiOsaSuoritus(
        null, koulutusmoduuli = Some(KoskiKoulutusModuuli(tunniste = Some(KoskiKoodi(aine, "oppiaineet", null, null, null)), null, null, null, null)), null,
        `yksilöllistettyOppimäärä` = if (yksilollistetty) Some(true) else None,
        `rajattuOppimäärä` = if (rajattu) Some(true) else None,
        null, null
      )
    }

    // Case 1: Ei yksilöllistettyjä tai rajattuja
    val suoritusVainTavallisia = baseSuoritus.copy(osasuoritukset = Some(Set(createOsaSuoritus("HI", false, false))))
    Assertions.assertEquals(None, KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, suoritusVainTavallisia))

    // Case 2: Alle puolet yksilöllistettyjä
    val suoritusAllePuoletYks = baseSuoritus.copy(osasuoritukset = Some(Set(createOsaSuoritus("BI", false, false), createOsaSuoritus("MA", true, false), createOsaSuoritus("LI", false, false))))
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY), KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, suoritusAllePuoletYks))

    // Case 3: Tasan puolet suorituksista yksilöllistettyjä
    val suoritusPuoletYks = baseSuoritus.copy(osasuoritukset = Some(Set(createOsaSuoritus("HI", true, false), createOsaSuoritus("MA", true, false), createOsaSuoritus("LI", false, false), createOsaSuoritus("GE", false, false))))
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY), KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, suoritusPuoletYks))

    // Case 4: Yli puolet yksilöllistettyjä
    val suoritusYliPuoletYks = baseSuoritus.copy(osasuoritukset = Some(Set(createOsaSuoritus("LI", true, false), createOsaSuoritus("AI", true, false), createOsaSuoritus("A1", false, false))))
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY), KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, suoritusYliPuoletYks))

    // Case 5: Alle puolet rajattuja
    val suoritusAllePuoletRajattu = baseSuoritus.copy(osasuoritukset = Some(Set(createOsaSuoritus("GE", false, false), createOsaSuoritus("BI", false, true), createOsaSuoritus("HI",false, false))))
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_RAJATTU), KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, suoritusAllePuoletRajattu))

    // Case 6: Puolet suorituksista rajattuja
    val suoritusPuoletRajattu = baseSuoritus.copy(osasuoritukset = Some(Set(createOsaSuoritus("AI", false, true), createOsaSuoritus("MA", false, true), createOsaSuoritus("GE", false, false), createOsaSuoritus("LI", false, false))))
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_RAJATTU), KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, suoritusPuoletRajattu))

    // Case 7: Yli puolet rajattuja
    val suoritusYliPuoletRajattu = baseSuoritus.copy(osasuoritukset = Some(Set(createOsaSuoritus("LI", false, true), createOsaSuoritus("B2", false, true), createOsaSuoritus("A1", false, false))))
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_RAJATTU),    KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, suoritusYliPuoletRajattu))

    // Case 8: Sekä yksilöllistettyjä että rajattuja, mutta yksilöllistettyjä on enemmän
    val suoritusEnemmanYksilollistettyja = baseSuoritus.copy(osasuoritukset = Some(Set(createOsaSuoritus("HI", false, true), createOsaSuoritus("MA", true, false), createOsaSuoritus("GE", false, false), createOsaSuoritus("FY", true, false))))
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY), KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, suoritusEnemmanYksilollistettyja))

    // Case 9: Sekä yhtä paljon yksilöllistettyjä ja rajattuja
    val suoritusYhtaPaljonYksRaj = baseSuoritus.copy(osasuoritukset = Some(Set(createOsaSuoritus("HI", false, true), createOsaSuoritus("MA", false, false), createOsaSuoritus("FY", true, false))))
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY), KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, suoritusYhtaPaljonYksRaj))

    // Case 10: Toiminta-alueittain
    val opiskeluoikeusToimintaAlueittain = baseOikeus.copy(lisätiedot = Some(KoskiLisatiedot(erityisenTuenPäätökset = Some(List(KoskiErityisenTuenPaatos(Some(true)))), vuosiluokkiinSitoutumatonOpetus = None, kotiopetusjaksot = None)))
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY), KoskiToSuoritusConverter.getYksilollistaminen(opiskeluoikeusToimintaAlueittain, baseSuoritus))

    // Case 11: Ei osasuorituksia
    Assertions.assertEquals(None, KoskiToSuoritusConverter.getYksilollistaminen(baseOikeus, baseSuoritus))
  }

  @Test def testParseKeskeytyminen(): Unit = {
    // Keskeytynyt opiskeluoikeus palauttaa keskeytymishetken
    val opiskeluoikeusEronnut = KoskiOpiskeluoikeus(
      "1.2.3",
      None,
      KoskiOpiskeluoikeusTyyppi("arvo", "koodisto", None),
      Some(KoskiOpiskeluoikeusTila(List(
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2024-01-01"),
          KoskiKoodi("lasna", "koodisto", None, Kielistetty(None, None, None), None)
        ),
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2024-06-15"),
          KoskiKoodi("eronnut", "koodisto", None, Kielistetty(None, None, None), None)
        )))),
      Set.empty,
      None
    )
    Assertions.assertEquals(Some(LocalDate.parse("2024-06-15")), KoskiToSuoritusConverter.parseKeskeytyminen(opiskeluoikeusEronnut))

    // Ei keskeytynyt palauttaa None
    val opiskeluoikeusValmis = KoskiOpiskeluoikeus(
      "1.2.3",
      None,
      KoskiOpiskeluoikeusTyyppi("arvo", "koodisto", None),
      Some(KoskiOpiskeluoikeusTila(List(
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2024-01-01"),
          KoskiKoodi("lasna", "koodisto", None, Kielistetty(None, None, None), None)
        ),
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2024-06-15"),
          KoskiKoodi("valmistunut", "koodisto", None, Kielistetty(None, None, None), None)
        )))),
      Set.empty,
      None
    )
    Assertions.assertEquals(None, KoskiToSuoritusConverter.parseKeskeytyminen(opiskeluoikeusValmis))

    // Jos ei tilaa palautuu None
    val opiskeluoikeusNoTila = KoskiOpiskeluoikeus(
      "1.2.3",
      None,
      KoskiOpiskeluoikeusTyyppi("arvo", "koodisto", None),
      None,
      Set.empty,
      None
    )
    Assertions.assertEquals(None, KoskiToSuoritusConverter.parseKeskeytyminen(opiskeluoikeusNoTila))
  }

  @Test def testIsMitatoity(): Unit = {
    val opiskeluoikeus = KoskiOpiskeluoikeus(
      "1.2.3",
      None,
      KoskiOpiskeluoikeusTyyppi("arvo", "koodisto", None),
      Some(KoskiOpiskeluoikeusTila(List(
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2025-01-01"),
          KoskiKoodi("mitatoity", "koodisto", None, Kielistetty(None, None, None), None)
        ),
        KoskiOpiskeluoikeusJakso(
          LocalDate.parse("2025-05-05"),
          KoskiKoodi("läsnä", "koodisto", None, Kielistetty(None, None, None), None)
        )))),
      Set.empty,
      None
    )

    // Opiskeluoikeus on mitätöity jos se on milloinkaan ollut mitätöity. Tämä johtuu siitä että
    // KOSKI-datassa on opiskeluoikeuksia jotka on laitettu alkamaan tulevaisuudessa ja sitten mitätöity
    // nykyhetkeen.
    Assertions.assertTrue(KoskiToSuoritusConverter.isMitatoitu(opiskeluoikeus))
  }
}
