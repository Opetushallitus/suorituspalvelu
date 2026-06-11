package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.VUOSILUOKKA_9
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, Lahtokoulu, LahtokouluTyyppi, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, Suoritus, SuoritusTila, Telma, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiArviointi, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiKotiopetusjakso, KoskiKoulutusModuuli, KoskiLaajuus, KoskiLisatiedot, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila, KoskiOsaSuoritus, KoskiParser, KoskiToSuoritusConverter, KoskiUtil}
import fi.oph.suorituspalvelu.resource.api.LahtokouluAuthorization
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.{Optional, UUID}
import scala.jdk.OptionConverters.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiUtilTest {

  @Test def testIsYsiluokkalainenEiSuoritusta(): Unit =
    Assertions.assertFalse(KoskiUtil.onkoJokinLahtokoulu(LocalDate.now, None, Some(Set(LahtokouluTyyppi.VUOSILUOKKA_9)), Set.empty))

  @Test def testIsYsiluokkalainenTrue(): Unit =
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = None,
      oppilaitosOid = "1.2.3",
      suoritukset = Set(
        PerusopetuksenOppimaara(
          tunniste = UUID.randomUUID(),
          versioTunniste = None,
          oppilaitos = Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
          luokka = None,
          luokkaAste = None,
          koskiTila = null,
          supaTila = SuoritusTila.KESKEN,
          suoritusKieli = null,
          koulusivistyskieli = Set.empty,
          yksilollistaminen = None,
          aloitusPaivamaara = None,
          vahvistusPaivamaara = Some(LocalDate.now()),
          aineet = Seq.empty,
          lahtokoulut = List(Lahtokoulu(LocalDate.now().minusDays(1), Some(LocalDate.now()), "1.2.3", Some(LocalDate.now.getYear), "9A", SuoritusTila.KESKEN, None, VUOSILUOKKA_9)),
          syotetty = false,
          vuosiluokkiinSitoutumatonOpetus = false
        )
      ),
      lisatiedot = None,
      tila = KESKEN,
      jaksot = List.empty
    )
    Assertions.assertTrue(KoskiUtil.onkoJokinLahtokoulu(LocalDate.now, None, Some(Set(VUOSILUOKKA_9)), Set(opiskeluoikeus)))

  @Test def testIsYsiluokkalainenValmisPerusopetus(): Unit =
    val vahvistusPaivamaara = LocalDate.parse("2025-06-01")
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = None,
      oppilaitosOid = "1.2.3",
      suoritukset = Set(
        PerusopetuksenOppimaara(
          tunniste = UUID.randomUUID(),
          versioTunniste = None,
          oppilaitos = Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
          luokka = None,
          luokkaAste = None,
          koskiTila = null,
          supaTila = SuoritusTila.VALMIS,
          suoritusKieli = null,
          koulusivistyskieli = Set.empty,
          yksilollistaminen = None,
          aloitusPaivamaara = Some(LocalDate.now()),
          vahvistusPaivamaara = Some(LocalDate.now()),
          aineet = Seq.empty,
          lahtokoulut = List(Lahtokoulu(LocalDate.now(), Some(LocalDate.now()), "1.2.3", Some(LocalDate.now.getYear), "9A", SuoritusTila.VALMIS, None, VUOSILUOKKA_9)),
          syotetty = false,
          vuosiluokkiinSitoutumatonOpetus = false
        ),
      ),
      lisatiedot = None,
      tila = SuoritusTila.VALMIS,
      jaksot = List.empty
    )
    Assertions.assertTrue(KoskiUtil.onkoJokinLahtokoulu(LocalDate.now, None, Some(Set(VUOSILUOKKA_9)), Set(opiskeluoikeus)))

  @Test def testLuoLahtokouluAuthorizations(): Unit =
    val lahtokoulu1 = Lahtokoulu(LocalDate.parse("2024-08-18"), Some(LocalDate.parse("2025-03-31")), "ensimmäinen", Some(LocalDate.now.getYear), "9A", SuoritusTila.KESKEYTYNYT, None, VUOSILUOKKA_9)
    val lahtokoulu2 = Lahtokoulu(LocalDate.parse("2025-01-01"), Some(LocalDate.parse("2025-05-31")), "toinen", Some(LocalDate.now.getYear), "9A", SuoritusTila.KESKEN, None, VUOSILUOKKA_9)

    Assertions.assertEquals(Seq(
      // ensimmäisen autorisaation loppu on toisen alku koska menevät limittäin
      LahtokouluAuthorization(lahtokoulu1.oppilaitosOid, lahtokoulu1.suorituksenAlku, Optional.of(lahtokoulu2.suorituksenAlku), "9A", lahtokoulu1.suoritusTyyppi.toString),
      // toisen autorisaation loppu on seuraavan vuoden tammikuun loppu (ei-inklusiivinen)
      LahtokouluAuthorization(lahtokoulu2.oppilaitosOid, lahtokoulu2.suorituksenAlku, Optional.of(LocalDate.parse("2026-02-01")), "9A", lahtokoulu2.suoritusTyyppi.toString)
    ), KoskiUtil.luoLahtokouluAuthorizations(Seq(
      // varmistetaan ettei lähdedatan järjestys merkitse
      lahtokoulu2,
      lahtokoulu1
    )))

  private val tunnetutOppiaineet: Set[String] = Set("AI", "MA", "HI", "LI", "A1", "A2", "B1", "B2")

  private val oppiaineKoodistoProvider: KoodistoProvider = (koodisto: String) =>
    if (koodisto == KoskiUtil.KOODISTO_OPPIAINEET)
      tunnetutOppiaineet.map(k => k -> null.asInstanceOf[fi.oph.suorituspalvelu.integration.client.Koodi]).toMap
    else Map.empty

  private def mkOsa(koodi: String, pakollinen: Boolean, laajuus: Option[BigDecimal], hasArviointi: Boolean, koodistoUri: String = "koskioppiaineetyleissivistava"): KoskiOsaSuoritus =
    KoskiOsaSuoritus(
      tyyppi = null,
      koulutusmoduuli = Some(KoskiKoulutusModuuli(
        tunniste = Some(KoskiKoodi(koodi, koodistoUri, None, Kielistetty(None, None, None), None)),
        koulutustyyppi = None,
        laajuus = laajuus.map(arvo => KoskiLaajuus(arvo, None)),
        kieli = None,
        pakollinen = Some(pakollinen),
        osaAlue = None,
        `ryhmä` = None,
        taso = None)),
      arviointi = if (hasArviointi) Some(Set(KoskiArviointi(KoskiKoodi("8", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true))) else None,
      predictedArviointi = None,
      `yksilöllistettyOppimäärä` = None,
      `rajattuOppimäärä` = None,
      suorituskieli = None,
      vastaavuustodistuksenTiedot = None,
      osasuoritukset = None,
      korotettu = None
    )

  @Test def testIncludePerusopetuksenOppiaineEiArviointia(): Unit =
    Assertions.assertFalse(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("AI", pakollinen = true, laajuus = Some(BigDecimal(4)), hasArviointi = false),
      Set("AI"),
      oppiaineKoodistoProvider))

  @Test def testIncludePerusopetuksenOppiaineKoulukohtainen(): Unit =
    // koodi ei kuulu standardikoodistoon -> ei mukaan
    Assertions.assertFalse(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("KOULUKOHTAINEN_X", pakollinen = true, laajuus = Some(BigDecimal(3)), hasArviointi = true),
      Set("KOULUKOHTAINEN_X"),
      oppiaineKoodistoProvider))

  @Test def testIncludePerusopetuksenOppiainePaikallinenValinnainen(): Unit =
    // paikallinen valinnainen: koodiarvo löytyy koodistosta mutta koodistoUri on paikallinen -> ei mukaan
    Assertions.assertFalse(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("MA", pakollinen = false, laajuus = Some(BigDecimal(3)), hasArviointi = true, koodistoUri = "painotus"),
      Set("MA"),
      oppiaineKoodistoProvider))

  @Test def testIncludePerusopetuksenOppiaineXXEiTiedossa(): Unit =
    Assertions.assertFalse(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("XX", pakollinen = true, laajuus = Some(BigDecimal(3)), hasArviointi = true),
      Set("XX"),
      // "XX" sisällytetään tilapäisesti koodistoon jotta isKoulukohtainen-tarkistus ei mätsää
      (koodisto: String) =>
        if (koodisto == KoskiUtil.KOODISTO_OPPIAINEET)
          (tunnetutOppiaineet + "XX").map(k => k -> null.asInstanceOf[fi.oph.suorituspalvelu.integration.client.Koodi]).toMap
        else Map.empty))

  @Test def testIncludePerusopetuksenOppiainePakollinenAinaMukaan(): Unit =
    // Pakollinen mukaan vaikka laajuutta ei ole määritelty
    Assertions.assertTrue(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("AI", pakollinen = true, laajuus = None, hasArviointi = true),
      Set("AI"),
      oppiaineKoodistoProvider))

  @Test def testIncludePerusopetuksenOppiaineValinnainenA2MukaanIlmanVastinetta(): Unit =
    // A2-kieli sisällytetään vaikka olisi valinnainen ja ilman pakollista vastinetta
    Assertions.assertTrue(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("A2", pakollinen = false, laajuus = None, hasArviointi = true),
      Set.empty,
      oppiaineKoodistoProvider))

  @Test def testIncludePerusopetuksenOppiaineValinnainenB2MukaanIlmanVastinetta(): Unit =
    Assertions.assertTrue(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("B2", pakollinen = false, laajuus = None, hasArviointi = true),
      Set.empty,
      oppiaineKoodistoProvider))

  @Test def testIncludePerusopetuksenOppiaineValinnainenVastineLaajuusTasanKaksi(): Unit =
    // Raja-arvo: laajuus tasan 2 vvk -> mukaan kun pakollinen vastine löytyy
    Assertions.assertTrue(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("MA", pakollinen = false, laajuus = Some(BigDecimal(2)), hasArviointi = true),
      Set("MA"),
      oppiaineKoodistoProvider))

  @Test def testIncludePerusopetuksenOppiaineValinnainenVastineLaajuusAlle2(): Unit =
    // laajuus < 2 -> ei mukaan vaikka pakollinen vastine löytyy
    Assertions.assertFalse(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("MA", pakollinen = false, laajuus = Some(BigDecimal("1.5")), hasArviointi = true),
      Set("MA"),
      oppiaineKoodistoProvider))

  @Test def testIncludePerusopetuksenOppiaineValinnainenEiVastinettaLaajuusRiittava(): Unit =
    // Laajuus riittävä mutta pakollinen vastine puuttuu -> ei mukaan (regressio commitista 3f71a8f7)
    Assertions.assertFalse(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("LI", pakollinen = false, laajuus = Some(BigDecimal(3)), hasArviointi = true),
      Set("AI", "MA"),
      oppiaineKoodistoProvider))

  @Test def testIncludePerusopetuksenOppiaineValinnainenEiVastinettaEiLaajuutta(): Unit =
    Assertions.assertFalse(KoskiUtil.includePerusopetuksenOppiaine(
      mkOsa("HI", pakollinen = false, laajuus = None, hasArviointi = true),
      Set.empty,
      oppiaineKoodistoProvider))

  private val KAIKKI_YHTEISET = List("AI", "A1", "B1", "MA", "BI", "GE", "FY", "KE", "HI", "YH", "LI", "TE", "MU", "KU", "KS", "KO")

  private def aineet(koodit: String*): Seq[PerusopetuksenOppiaine] =
    koodit.map(k => PerusopetuksenOppiaine(
      UUID.randomUUID(),
      Kielistetty(None, None, None),
      Koodi(k, "koskioppiaineetyleissivistava", Some(1)),
      Koodi("9", "arviointiasteikkoyleissivistava", Some(1)),
      None,
      pakollinen = true,
      yksilollistetty = None,
      rajattu = None
    )).toSeq

  @Test def testYhteisenAineenArvosanaPuuttuuKaikkiAineetJaET(): Unit =
    Assertions.assertFalse(KoskiUtil.yhteisenAineenArvosanaPuuttuu(aineet(KAIKKI_YHTEISET :+ "ET" *)))

  @Test def testYhteisenAineenArvosanaPuuttuuKaikkiAineetJaKT(): Unit =
    Assertions.assertFalse(KoskiUtil.yhteisenAineenArvosanaPuuttuu(aineet(KAIKKI_YHTEISET :+ "KT" *)))

  @Test def testYhteisenAineenArvosanaPuuttuuKaikkiAineetJaMolemmatKatsomusaineet(): Unit =
    Assertions.assertFalse(KoskiUtil.yhteisenAineenArvosanaPuuttuu(aineet(KAIKKI_YHTEISET ++ List("ET", "KT") *)))

  @Test def testYhteisenAineenArvosanaPuuttuuYksiYhteinenPuuttuu(): Unit =
    val ilmanMatikkaa = KAIKKI_YHTEISET.filterNot(_ == "MA")
    Assertions.assertTrue(KoskiUtil.yhteisenAineenArvosanaPuuttuu(aineet(ilmanMatikkaa :+ "ET" *)))

  @Test def testYhteisenAineenArvosanaPuuttuuKatsomusainePuuttuu(): Unit =
    Assertions.assertTrue(KoskiUtil.yhteisenAineenArvosanaPuuttuu(aineet(KAIKKI_YHTEISET *)))

  @Test def testYhteisenAineenArvosanaPuuttuuTyhja(): Unit =
    Assertions.assertTrue(KoskiUtil.yhteisenAineenArvosanaPuuttuu(Seq.empty))

  @Test def testYhteisenAineenArvosanaPuuttuuVainKatsomusaine(): Unit =
    Assertions.assertTrue(KoskiUtil.yhteisenAineenArvosanaPuuttuu(aineet("ET")))

  @Test def testYhteisenAineenArvosanaPuuttuuYlimaaraisetEivatHaittaa(): Unit =
    Assertions.assertFalse(KoskiUtil.yhteisenAineenArvosanaPuuttuu(aineet(KAIKKI_YHTEISET ++ List("ET", "A2", "FI") *)))
}
