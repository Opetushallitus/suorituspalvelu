package fi.oph.suorituspalvelu.business.valinnat.mankeli

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{TELMA, TUVA, VAPAA_SIVISTYSTYO}
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, Hakutoive, Koodisto, KoutaHaku}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, DIAArvosana, DIALaajuus, DIAOppiaine, DIAOppiaineenKoesuoritus, DIATutkinto, DIAVastaavuustodistuksenTiedot, GeneerinenOpiskeluoikeus, Koodi, Laajuus, Lahtokoulu, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, PerusopetuksenYksilollistaminen, SuoritusTila, Telma, TestDataUtil, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoContainer, AvainArvoConverter, HakemuksenHarkinnanvaraisuus, HakemusConverter, HakutoiveenHarkinnanvaraisuus, HarkinnanvaraisuudenSyy}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiLisatiedot, KoskiParser, KoskiToSuoritusConverter}
import fi.oph.suorituspalvelu.parsing.ytr.{YtrParser, YtrToSuoritusConverter}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

import java.time.LocalDate
import java.util.UUID

@Test
@TestInstance(Lifecycle.PER_CLASS)
class AvainArvoConverterTest {

  val DEFAULT_OPPIAINEKOODI = fi.oph.suorituspalvelu.integration.client.Koodi("", Koodisto(""), List.empty)
  val DUMMY_KOODISTOPROVIDER: KoodistoProvider = koodisto => Map(
    "HI" -> DEFAULT_OPPIAINEKOODI, "KO" -> DEFAULT_OPPIAINEKOODI, "BI" -> DEFAULT_OPPIAINEKOODI,
    "B1" -> DEFAULT_OPPIAINEKOODI, "LI" -> DEFAULT_OPPIAINEKOODI,
    "YH" -> DEFAULT_OPPIAINEKOODI, "KU" -> DEFAULT_OPPIAINEKOODI, "GE" -> DEFAULT_OPPIAINEKOODI,
    "TH" -> DEFAULT_OPPIAINEKOODI, "MA" -> DEFAULT_OPPIAINEKOODI, "B2" -> DEFAULT_OPPIAINEKOODI,
    "TE" -> DEFAULT_OPPIAINEKOODI, "KT" -> DEFAULT_OPPIAINEKOODI, "FY" -> DEFAULT_OPPIAINEKOODI,
    "AI" -> DEFAULT_OPPIAINEKOODI, "MU" -> DEFAULT_OPPIAINEKOODI, "A1" -> DEFAULT_OPPIAINEKOODI,
    "KE" -> DEFAULT_OPPIAINEKOODI)

  val DEFAULT_LEIKKURIPVM = LocalDate.parse("2050-06-01")

  val DEFAULT_KOUTA_HAKU = KoutaHaku(
    oid = "1.2.246.562.29.01000000000000012345",
    tila = "julkaistu",
    nimi = Map("fi" -> s"Testi haku 1.2.246.562.29.01000000000000012345"),
    hakutapaKoodiUri = "hakutapa_01",
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_11#1"),
    hakuajat = List.empty,
    kohdejoukonTarkenneKoodiUri = None,
    hakuvuosi = Some(2022)
  )

  val BASE_HAKEMUS = AtaruValintalaskentaHakemus(
    hakemusOid = "1.2.246.562.11.00000000000000006321",
    personOid = "1.2.246.562.24.21250967211",
    hakuOid = "1.2.246.562.29.01000000000000013275",
    asiointikieli = "fi",
    hakutoiveet = List(
      Hakutoive(
        processingState = "unprocessed",
        eligibilityState = "eligible",
        paymentObligation = "not-obligated",
        kkApplicationPaymentObligation = "unreviewed",
        hakukohdeOid = "1.2.246.562.20.00000000000000000001",
        languageRequirement = "unreviewed",
        degreeRequirement = "unreviewed",
        harkinnanvaraisuus = None
      ),
      Hakutoive(
        processingState = "unprocessed",
        eligibilityState = "eligible",
        paymentObligation = "not-obligated",
        kkApplicationPaymentObligation = "unreviewed",
        hakukohdeOid = "1.2.246.562.20.00000000000000000002",
        languageRequirement = "unreviewed",
        degreeRequirement = "unreviewed",
        harkinnanvaraisuus = Some("harkinnanvaraisesti_hyvaksyttavissa")
      )
    ),
    maksuvelvollisuus = Map.empty,
    keyValues = Map.empty,
    korkeakoulututkintoVuosi = None
  )

  private def aine(koodi: String, arvosana: String, kieli: Option[String] = None): PerusopetuksenOppiaine =
    PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(None, None, None),
      Koodi(koodi, "koodisto", None), Koodi(arvosana, "koodisto", None),
      kieli.map(k => Koodi(k, "kielivalikoima", None)), true, None, None)

  private def paasuoritusOpiskeluoikeus(aineet: Set[PerusopetuksenOppiaine]): PerusopetuksenOpiskeluoikeus = {
    val oppimaara = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
      None, Koodi("FI", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("FI", "koodisto", Some(1)),
      Set.empty, None, Some(LocalDate.parse("2025-05-30")), Some(LocalDate.parse("2025-05-30")),
      aineet.toSeq, List.empty, false, false, None)
    PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.09876543210"),
      "1.2.246.562.10.09876543211", Set(oppimaara), None, SuoritusTila.VALMIS, List.empty)
  }

  private def korotusOpiskeluoikeus(aineet: Set[PerusopetuksenOppiaine]): PerusopetuksenOpiskeluoikeus = {
    val suoritus = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None,
      Oppilaitos(Kielistetty(None, None, None), "1.2.3"), Koodi("arvo", "koodisto", Some(1)),
      SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)),
      Some(LocalDate.parse("2025-06-08")), Some(LocalDate.parse("2025-06-08")), aineet, false)
    PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.09876543210"),
      "1.2.246.562.10.09876543211", Set(suoritus), None, SuoritusTila.VALMIS, List.empty)
  }

  private def convertArvot(opiskeluoikeudet: Seq[Opiskeluoikeus]): Map[String, String] =
    AvainArvoConverter.convertOpiskeluoikeudet("1.2.3", Some(BASE_HAKEMUS),
      opiskeluoikeudet, Seq.empty, LocalDate.now(), DEFAULT_KOUTA_HAKU, None, Map.empty).getAvainArvoMap()

  // Rakentaa hakemuksen annetuista avain-arvoista lisäten automaattisesti pohjakoulutus_vuosi=2016,
  // jotta hakemukselta tulevat arvosanat huomioidaan. Palauttaa kaikki päätellyt avain-arvot selitteineen.
  private def hakemukseltaConvertTulos(keyValues: Map[String, String], opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq.empty): Set[AvainArvoContainer] = {
    val hakemus = BASE_HAKEMUS.copy(keyValues = keyValues + (AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2016"))
    AvainArvoConverter.convertOpiskeluoikeudet("1.2.3", Some(hakemus), opiskeluoikeudet, Seq.empty, LocalDate.now(), DEFAULT_KOUTA_HAKU, None, Map.empty).paatellytArvot
  }

  @Test def testAvainArvoConverterForPeruskouluKeys(): Unit = {
    val fileName = "/1_2_246_562_98_69863082363.json"
    val splitData = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream(fileName)).toList
    val opiskeluoikeudet = splitData.flatMap(henkilo => {
      henkilo.opiskeluoikeudet.flatMap {
        case Right(oo) =>
          val koskiOpiskeluoikeus = KoskiParser.parseKoskiData(oo.data)
          KoskiToSuoritusConverter.parseOpiskeluoikeudet(Seq(koskiOpiskeluoikeus), DUMMY_KOODISTOPROVIDER)
        case Left(exception) => Assertions.fail(exception)
      }
    })

    Assertions.assertEquals(1, opiskeluoikeudet.size)

    val leikkuri = LocalDate.parse("2025-05-31")
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", None, opiskeluoikeudet, Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)

    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(Some("2025"), converterResult.getAvainArvoMap().get(AvainArvoConstants.peruskouluSuoritusvuosiKey))
    Assertions.assertEquals(Some("2025"), converterResult.getAvainArvoMap().get(AvainArvoConstants.peruskouluPaattotodistusvuosiKey))
    Assertions.assertEquals(Some("FI"), converterResult.getAvainArvoMap().get(AvainArvoConstants.perusopetuksenKieliKey))
    Assertions.assertEquals(Some("2"), converterResult.getAvainArvoMap().get(AvainArvoConstants.pkSuorituslukukausiKey))
  }

  @Test def testAvainArvoConverterForPeruskouluArvosanatJaKielet(): Unit = {
    val fileName = "/1_2_246_562_98_69863082363.json"
    val splitData = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream(fileName)).toList
    val opiskeluoikeudet = splitData.flatMap(henkilo => {
      henkilo.opiskeluoikeudet.flatMap {
        case Right(oo) =>
          val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(oo.data)
          KoskiToSuoritusConverter.parseOpiskeluoikeudet(Seq(koskiOpiskeluoikeudet), DUMMY_KOODISTOPROVIDER)
        case Left(exception) => Assertions.fail(exception)
      }
    })

    Assertions.assertEquals(1, opiskeluoikeudet.size)
    val leikkuri = LocalDate.now
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", None, opiskeluoikeudet, Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pakollisetTavoiteArvosanat = Map("HI" -> "8", "BI" -> "9", "B1" -> "8", "LI" -> "9",
      "YH" -> "10", "KU" -> "8", "GE" -> "9", "MA" -> "9", "B2" -> "9", "TE" -> "8",
      "KT" -> "10", "FY" -> "9", "AI" -> "9", "MU" -> "7", "A1" -> "8", "KE" -> "7")
    val tavoiteKielet = Map("B1" -> "SV", "A1" -> "EN", "B2" -> "DE", "AI" -> "FI")

    pakollisetTavoiteArvosanat.foreach { case (aine, arvosana) =>
      val prefix = AvainArvoConstants.peruskouluAineenArvosanaPrefix
      Assertions.assertEquals(Some(arvosana), converterResult.getAvainArvoMap().get(prefix + aine))
    }

    tavoiteKielet.foreach { case (aine, kieli) =>
      val postfix = AvainArvoConstants.peruskouluAineenKieliOppiainePostfix
      val prefix = AvainArvoConstants.peruskouluAineenArvosanaPrefix
      val kieliAvain = prefix + aine + postfix
      Assertions.assertEquals(Some(kieli), converterResult.getAvainArvoMap().get(kieliAvain))
    }

    //Pitäisi löytyä yksi tarpeeksi laaja valinnainen arvosana
    Assertions.assertEquals(Some("10"), converterResult.getAvainArvoMap().get("PK_LI_VAL1"))

    // KIELITIETO-arvojen tulee sisältää muuntamattomat kielikoodit
    val tavoiteKieliTiedot = Map("B1" -> "SV", "A1" -> "EN", "B2" -> "DE", "AI" -> "AI1")
    tavoiteKieliTiedot.foreach { case (aine, kieliTieto) =>
      val prefix = AvainArvoConstants.peruskouluAineenArvosanaPrefix
      val postfix = AvainArvoConstants.peruskouluAineenKieliTietoPostfix
      val kieliTietoAvain = prefix + aine + postfix

      Assertions.assertEquals(Some(kieliTieto), converterResult.getAvainArvoMap().get(kieliTietoAvain), s"KIELITIETO for $aine should be $kieliTieto")
    }
  }

  @Test def testAidinkieliKielikoodiMuunnos(): Unit = {
    val aineet = List(
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("Suomen kieli ja kirjallisuus"), None, None), Koodi("AI", "koodisto", None), Koodi("9", "koodisto", None), Some(Koodi("AI1", "oppiaineaidinkielijakirjallisuus", None)), true, None, None),
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("Ruotsin kieli ja kirjallisuus"), None, None), Koodi("AI", "koodisto", None), Koodi("8", "koodisto", None), Some(Koodi("AI2", "oppiaineaidinkielijakirjallisuus", None)), true, None, None),
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("Suomi toisena kielenä"), None, None), Koodi("AI", "koodisto", None), Koodi("7", "koodisto", None), Some(Koodi("AI7", "oppiaineaidinkielijakirjallisuus", None)), true, None, None),
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("Muu äidinkieli"), None, None), Koodi("AI", "koodisto", None), Koodi("6", "koodisto", None), Some(Koodi("AIAI", "oppiaineaidinkielijakirjallisuus", None)), true, None, None),
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("A1", "koodisto", None), Koodi("8", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None)
    )

    val avainArvot = aineet.flatMap(aine => AvainArvoConverter.perusopetuksenPakollisetOppiaineetJaKieletToAvainArvot("1.2.3.4.5", Seq(aine)).toList)
    val resultMap = avainArvot.map(aa => (aa.avain, aa.arvo)).toMap

    // AI-kielikoodit muunnetaan OPPIAINE-arvoissa
    Assertions.assertTrue(resultMap.values.toSet.intersect(Set("FI", "SV", "FI_2", "XX")).nonEmpty, "AI kieli codes should be mapped to standardized codes")

    // Muiden kuin AI-kielten pitää säilyä muuttumattomina
    Assertions.assertEquals("EN", resultMap("PK_A1_OPPIAINE"), "A1 language should remain EN")

    // KIELITIETO sisältää raa'at kielikoodit
    val kieliTietoArvot = avainArvot.filter(_.avain.endsWith("_KIELITIETO")).map(aa => (aa.avain, aa.arvo))
    val kieliTietoMap = kieliTietoArvot.groupBy(_._1).map { case (k, v) => (k, v.map(_._2)) }
    Assertions.assertTrue(kieliTietoMap("PK_AI_KIELITIETO").contains("AI1"), "PK_AI_KIELITIETO should contain raw AI1")
    Assertions.assertTrue(kieliTietoMap("PK_AI_KIELITIETO").contains("AI2"), "PK_AI_KIELITIETO should contain raw AI2")
    Assertions.assertTrue(kieliTietoMap("PK_AI_KIELITIETO").contains("AI7"), "PK_AI_KIELITIETO should contain raw AI7")
    Assertions.assertTrue(kieliTietoMap("PK_AI_KIELITIETO").contains("AIAI"), "PK_AI_KIELITIETO should contain raw AIAI")
    Assertions.assertEquals(List("EN"), kieliTietoMap("PK_A1_KIELITIETO"), "PK_A1_KIELITIETO should contain raw EN")
  }

  @Test def testAidinkieliKielikoodiMuunnosKaikkiArvot(): Unit = {
    val expectedMappings = Map(
      "AI1"  -> "FI",
      "AI2"  -> "SV",
      "AI3"  -> "SE",
      "AI4"  -> "RI",
      "AI5"  -> "VK",
      "AI6"  -> "XX",
      "AI7"  -> "FI_2",
      "AI8"  -> "SV_2",
      "AI9"  -> "FI_SE",
      "AI10" -> "XX",
      "AI11" -> "FI_VK",
      "AI12" -> "SV_VK",
      "AIAI" -> "XX"
    )

    expectedMappings.foreach { case (input, expected) =>
      Assertions.assertEquals(expected, AvainArvoConverter.convertAidinkieliKielikoodi(input), s"$input should map to $expected")
    }

    // Tuntematon koodi säilyy muuttumattomana
    Assertions.assertEquals("UNKNOWN", AvainArvoConverter.convertAidinkieliKielikoodi("UNKNOWN"), "Unknown code should pass through as-is")
  }

  @Test def testETRemappedToKT(): Unit = {
    val aineet = Seq(
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("Elämänkatsomustieto"), None, None),
        Koodi("ET", "koodisto", None), Koodi("8", "koodisto", None),
        None, true, None, None)
    )
    val avainArvot = AvainArvoConverter.perusopetuksenPakollisetOppiaineetJaKieletToAvainArvot("1.2.3.4.5", aineet)
    val resultMap = avainArvot.map(aa => (aa.avain, aa.arvo)).toMap

    Assertions.assertEquals("8", resultMap("PK_KT"))
    Assertions.assertFalse(resultMap.contains("PK_ET"), "PK_ET should not exist")
  }

  @Test def testETAndKTConflictLogsErrorAndRemaps(): Unit = {
    val aineet = Seq(
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("ET"), None, None),
        Koodi("ET", "koodisto", None), Koodi("8", "koodisto", None), None, true, None, None),
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("KT"), None, None),
        Koodi("KT", "koodisto", None), Koodi("9", "koodisto", None), None, true, None, None)
    )
    val avainArvot = AvainArvoConverter.perusopetuksenPakollisetOppiaineetJaKieletToAvainArvot("1.2.3.4.5", aineet)
    Assertions.assertTrue(avainArvot.nonEmpty, "Should return results despite conflict")
  }

  @Test def testKieltenNumerointi(): Unit = {
    val a1Suomi = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("suomi A1"), None, None),    Koodi("A1", "koodisto", None), Koodi("8", "koodisto", None), Some(Koodi("FI", "kielivalikoima", None)), true, None, None)
    val a1Englanti = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti A1"), None, None), Koodi("A1", "koodisto", None), Koodi("9", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None)
    val a1Ruotsi = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("ruotsi A1"), None, None),   Koodi("A1", "koodisto", None), Koodi("7", "koodisto", None), Some(Koodi("SV", "kielivalikoima", None)), true, None, None)
    val b1Ruotsi = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("ruotsi B1"), None, None),   Koodi("B1", "koodisto", None), Koodi("6", "koodisto", None), Some(Koodi("SV", "kielivalikoima", None)), true, None, None)
    val b1Englanti = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti B1"), None, None), Koodi("B1", "koodisto", None), Koodi("5", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None)
    val korotusA1Suomi = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("suomi A1 korotus"), None, None), Koodi("A1", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("FI", "kielivalikoima", None)), true, None, None)

    // Syötejärjestys määrää numeroinnin: a1Suomi=ensimmäinen(PK_A1), a1Englanti=toinen(PK_A12), a1Ruotsi=kolmas(PK_A13)
    val aineetJaKorotukset = Seq(
      (a1Suomi, Seq(korotusA1Suomi)),  // korotus nostaa arvosanan 10:ksi
      (a1Englanti, Seq.empty),
      (a1Ruotsi, Seq.empty),
      (b1Ruotsi, Seq.empty),
      (b1Englanti, Seq.empty)
    )

    val result    = AvainArvoConverter.pakollisetJaKieletToContainers(aineetJaKorotukset)
    val resultMap = result.map(aa => aa.avain -> aa.arvo).toMap

    Assertions.assertEquals("10", resultMap("PK_A1"),         "First A1 grade should be korotus 10")
    Assertions.assertEquals("FI", resultMap("PK_A1_OPPIAINE"))
    Assertions.assertEquals("9",  resultMap("PK_A12"),        "Second A1 -> PK_A12")
    Assertions.assertEquals("EN", resultMap("PK_A12_OPPIAINE"))
    Assertions.assertEquals("7",  resultMap("PK_A13"),        "Third A1 -> PK_A13")
    Assertions.assertEquals("SV", resultMap("PK_A13_OPPIAINE"))
    Assertions.assertEquals("6",  resultMap("PK_B1"),         "First B1 -> PK_B1")
    Assertions.assertEquals("SV", resultMap("PK_B1_OPPIAINE"))
    Assertions.assertEquals("5",  resultMap("PK_B12"),        "Second B1 -> PK_B12")
    Assertions.assertEquals("EN", resultMap("PK_B12_OPPIAINE"))
  }

  //Useita AI-oppiaineita: eri äidinkielen oppimäärät, (esim. AI1 ja AI7) pitäisi numeroida kuten A- ja B-kielet.
  @Test def testMultipleAidinkieliArvosanatAreNumbered(): Unit = {
    val aiSuomi = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(None, None, None),
      Koodi("AI", "koodisto", None), Koodi("9", "koodisto", None),
      Some(Koodi("AI1", "oppiaineaidinkielijakirjallisuus", None)), true, None, None)
    val aiSuomiToisenaKielena = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(None, None, None),
      Koodi("AI", "koodisto", None), Koodi("8", "koodisto", None),
      Some(Koodi("AI7", "oppiaineaidinkielijakirjallisuus", None)), true, None, None)

    val aineetJaKorotukset = Seq(
      (aiSuomi, Seq.empty[PerusopetuksenOppiaine]),
      (aiSuomiToisenaKielena, Seq.empty[PerusopetuksenOppiaine])
    )

    val result    = AvainArvoConverter.pakollisetJaKieletToContainers(aineetJaKorotukset)
    val resultMap = result.map(aa => aa.avain -> aa.arvo).toMap

    // AI-aineet pitäisi numeroida kuten A-kielet: PK_AI (AI1/suomi) ja PK_AI2 (AI2/ruotsi)
    Assertions.assertEquals("9", resultMap(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"),  "First AI (suomi) -> PK_AI")
    Assertions.assertEquals("FI", resultMap(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI" + AvainArvoConstants.peruskouluAineenKieliOppiainePostfix), "Second AI (suomi) -> PK_AI_OPPIAINE")
    Assertions.assertEquals("AI1", resultMap(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI" + AvainArvoConstants.peruskouluAineenKieliTietoPostfix), "Second AI (suomi) -> PK_AI_OPPIAINE")
    Assertions.assertEquals("8", resultMap(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI2"), "Second AI (ruotsi) -> PK_AI2")
    Assertions.assertEquals("FI_2", resultMap(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI2" + AvainArvoConstants.peruskouluAineenKieliOppiainePostfix), "Second AI (ruotsi) -> PK_AI2_OPPIAINE")
    Assertions.assertEquals("AI7", resultMap(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI2" + AvainArvoConstants.peruskouluAineenKieliTietoPostfix), "Second AI (ruotsi) -> PK_AI2_OPPIAINE")
  }

  @Test def testKorkeimmatArvosanat(): Unit = {
    val aineet = Seq(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti, hankala savotta"), None, None), Koodi("A1", "koodisto", None), Koodi("8", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("A1", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia, suoritus"), None, None), Koodi("BI", "koodisto", None), Koodi("S", "koodisto", None), None, true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia"), None, None), Koodi("BI", "koodisto", None), Koodi("8", "koodisto", None), None, true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("kotitalous"), None, None), Koodi("KO", "koodisto", None), Koodi("S", "koodisto", None), None, true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("kotitalous, osallistuminen"), None, None), Koodi("BI", "koodisto", None), Koodi("O", "koodisto", None), None, true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("liikunta"), None, None), Koodi("LI", "koodisto", None), Koodi("9", "koodisto", None), None, true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("liikunta, toinen"), None, None), Koodi("LI", "koodisto", None), Koodi("7", "koodisto", None), None, true, None, None))
    val oppimaara = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, Some(LocalDate.parse("2025-05-30")), Some(LocalDate.parse("2025-05-30")), aineet, List.empty, false, false, None)
    val baseOpiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.09876543210"), "1.2.246.562.10.09876543211", Set(oppimaara), None, SuoritusTila.VALMIS, List.empty)

    val korotus1Biologia = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia"), None, None), Koodi("BI", "koodisto", None), Koodi("9", "koodisto", None), None, true, None, None)
    val korotus1Suoritus = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Some(LocalDate.parse("2025-06-08")), Some(LocalDate.parse("2025-06-08")), Set(korotus1Biologia), false)
    val korotus1Opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.09876543210"), "1.2.246.562.10.09876543211", Set(korotus1Suoritus), None, SuoritusTila.VALMIS, List.empty)

    val korotus2Liikunta = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("liikunta"), None, None), Koodi("LI", "koodisto", None), Koodi("10", "koodisto", None), None, true, None, None)
    val korotus2Suoritus = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Some(LocalDate.parse("2025-06-08")), Some(LocalDate.parse("2025-06-08")), Set(korotus2Liikunta), false)
    val korotus2Opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.09876543210"), "1.2.246.562.10.09876543211", Set(korotus2Suoritus), None, SuoritusTila.VALMIS, List.empty)

    val leikkuriPaiva = LocalDate.now()
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.3", Some(BASE_HAKEMUS), Seq(baseOpiskeluoikeus, korotus1Opiskeluoikeus, korotus2Opiskeluoikeus), Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val avainArvoMap = converterResult.getAvainArvoMap()
    println(s"korkeimmatArvosanat: $avainArvoMap")

    //Erityisesti paras kotitalouden arvosana "S" ei saa olla mukana, koska ei ole numeerinen.
    Assertions.assertFalse(avainArvoMap.contains(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "KO"))

    val tavoiteArvosanat = Map("A1" -> "10", "BI" -> "9", "LI" -> "10")
    tavoiteArvosanat.foreach { case (aine, arvosana) =>
      val prefix = AvainArvoConstants.peruskouluAineenArvosanaPrefix
      Assertions.assertEquals(Some(arvosana), avainArvoMap.get(prefix + aine))
    }
  }

  @Test def testOrpoaOppiaineenOppimaaraaEiKuuluHuomioida(): Unit = {
    val aineet = Seq(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia"), None, None), Koodi("BI", "koodisto", None), Koodi("8", "koodisto", None), None, true, None, None))
    val baseOppimaara = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, Some(LocalDate.parse("2025-05-30")), Some(LocalDate.parse("2025-05-30")), aineet, List.empty, false, false, None)
    val baseOpiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.09876543210"), "1.2.246.562.10.09876543211", Set(baseOppimaara), None, SuoritusTila.VALMIS, List.empty)

    val korotus1Biologia = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia"), None, None), Koodi("BI", "koodisto", None), Koodi("9", "koodisto", None), None, true, None, None)
    val korotus1Suoritus = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Some(LocalDate.parse("2025-06-08")), Some(LocalDate.parse("2025-06-08")), Set(korotus1Biologia), false)
    val korotus1Opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.09876543210"), "1.2.246.562.10.09876543211", Set(korotus1Suoritus), None, SuoritusTila.VALMIS, List.empty)
    val orpoOppiaineMaantieto = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("maantieto"), None, None), Koodi("GE", "koodisto", None), Koodi("10", "koodisto", None), None, true, None, None)
    val orpoOppiaineMaantietoSuoritus = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Some(LocalDate.parse("2025-06-08")), Some(LocalDate.parse("2025-06-08")), Set(orpoOppiaineMaantieto), false)
    val orpoOppiaineMaantietoOpiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.09876543210"), "1.2.246.562.10.09876543211", Set(orpoOppiaineMaantietoSuoritus), None, SuoritusTila.VALMIS, List.empty)

    val leikkuriPaiva = LocalDate.now()
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.3", Some(BASE_HAKEMUS), Seq(baseOpiskeluoikeus, korotus1Opiskeluoikeus, orpoOppiaineMaantietoOpiskeluoikeus), Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val avainArvoMap = converterResult.getAvainArvoMap()
    println(s"korkeimmatArvosanat: $avainArvoMap")

    //Tarkistetaan, että biologian arvosana löytyy ja korotus huomioitu
    Assertions.assertEquals(Some("9"), avainArvoMap.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "BI"))
    //Tarkistetaan, että "orpoa" maantietoa ei löydy
    Assertions.assertFalse(avainArvoMap.contains(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "GE"))
  }

  // Paikat järjestetään (koodi.arvo, kieli.arvo) mukaan ennen numerointia: EN < FI < SV aakkosellisesti.

  @Test def testKaksiEriA1KieltaNumeroituvat(): Unit = {
    // A1-EN ja A1-FI pääsuorituksella → EN järjestyy ensin (indeksi 0 = PK_A1), FI toiseksi (indeksi 1 = PK_A12)
    val arvot = convertArvot(Seq(paasuoritusOpiskeluoikeus(Set(aine("A1", "8", Some("EN")), aine("A1", "9", Some("FI"))))))

    Assertions.assertEquals(Some("8"),  arvot.get("PK_A1"))
    Assertions.assertEquals(Some("EN"), arvot.get("PK_A1_OPPIAINE"))
    Assertions.assertEquals(Some("9"),  arvot.get("PK_A12"))
    Assertions.assertEquals(Some("FI"), arvot.get("PK_A12_OPPIAINE"))
    Assertions.assertFalse(arvot.contains("PK_A13"), "Ei kolmatta A1-kieltä")
  }

  @Test def testKolmeEriA1KieltaNumeroituvat(): Unit = {
    // Kolme A1-kieltä → EN=PK_A1, FI=PK_A12, SV=PK_A13
    val arvot = convertArvot(Seq(paasuoritusOpiskeluoikeus(
      Set(aine("A1", "8", Some("EN")), aine("A1", "9", Some("FI")), aine("A1", "7", Some("SV"))))))

    Assertions.assertEquals(Some("8"),  arvot.get("PK_A1"))
    Assertions.assertEquals(Some("EN"), arvot.get("PK_A1_OPPIAINE"))
    Assertions.assertEquals(Some("9"),  arvot.get("PK_A12"))
    Assertions.assertEquals(Some("FI"), arvot.get("PK_A12_OPPIAINE"))
    Assertions.assertEquals(Some("7"),  arvot.get("PK_A13"))
    Assertions.assertEquals(Some("SV"), arvot.get("PK_A13_OPPIAINE"))
  }

  @Test def testA1KorotusOppiaineenOppimaaraltaKorottaaArvosanaa(): Unit = {
    // A1-EN-"8" pääsuorituksella, A1-EN-"10" korotus oppiaineen oppimäärältä → PK_A1="10"
    val arvot = convertArvot(Seq(
      paasuoritusOpiskeluoikeus(Set(aine("A1", "8", Some("EN")))),
      korotusOpiskeluoikeus(Set(aine("A1", "10", Some("EN"))))
    ))

    Assertions.assertEquals(Some("10"), arvot.get("PK_A1"))
    Assertions.assertEquals(Some("EN"), arvot.get("PK_A1_OPPIAINE"))
    Assertions.assertFalse(arvot.contains("PK_A12"), "Korotus ei luo uutta numeroa")
  }

  @Test def testA2KorotusOppiaineenOppimaaraltaKorottaaA1ta(): Unit = {
    // A2-EN oppiaineen oppimäärältä toimii A1-EN:n korotuksena (sama kieli, mikä tahansa A-kielen laajuus)
    val arvot = convertArvot(Seq(
      paasuoritusOpiskeluoikeus(Set(aine("A1", "8", Some("EN")))),
      korotusOpiskeluoikeus(Set(aine("A2", "10", Some("EN"))))
    ))

    Assertions.assertEquals(Some("10"), arvot.get("PK_A1"))
    Assertions.assertFalse(arvot.contains("PK_A12"), "A2-korotus ei luo uutta A1-numeroa")
  }

  @Test def testA1KorotusOppiaineenOppimaaraltaKorottaaA2ta(): Unit = {
    // A1-EN oppiaineen oppimäärältä toimii A2-EN:n korotuksena (sama kieli, mikä tahansa A-kielen laajuus)
    val arvot = convertArvot(Seq(
      paasuoritusOpiskeluoikeus(Set(aine("A2", "8", Some("EN")))),
      korotusOpiskeluoikeus(Set(aine("A1", "10", Some("EN"))))
    ))

    Assertions.assertEquals(Some("10"), arvot.get("PK_A2"))
    Assertions.assertFalse(arvot.contains("PK_A12"), "A2-korotus ei luo uutta A1-numeroa")
  }

  @Test def testA1KorotusEiKorotaEriKielta(): Unit = {
    // A1-EN ja A1-FI pääsuorituksella, vain A1-EN korotus oppiaineen oppimäärältä
    // A1-FI:n pitää säilyttää alkuperäinen arvosanansa
    val arvot = convertArvot(Seq(
      paasuoritusOpiskeluoikeus(Set(aine("A1", "8", Some("EN")), aine("A1", "7", Some("FI")))),
      korotusOpiskeluoikeus(Set(aine("A1", "10", Some("EN"))))
    ))

    Assertions.assertEquals(Some("10"), arvot.get("PK_A1"),  "EN saa korotuksen")
    Assertions.assertEquals(Some("7"),  arvot.get("PK_A12"), "FI ei saa EN:n korotusta")
  }

  @Test def testB1KorotusVainSamallakielella(): Unit = {
    // B1-EN ja B1-SV pääsuorituksella, vain B1-SV korotus oppiaineen oppimäärältä
    // EN < SV aakkosellisesti → PK_B1=EN, PK_B12=SV
    val arvot = convertArvot(Seq(
      paasuoritusOpiskeluoikeus(Set(aine("B1", "8", Some("EN")), aine("B1", "7", Some("SV")))),
      korotusOpiskeluoikeus(Set(aine("B1", "10", Some("SV"))))
    ))

    Assertions.assertEquals(Some("8"),  arvot.get("PK_B1"))
    Assertions.assertEquals(Some("EN"), arvot.get("PK_B1_OPPIAINE"))
    Assertions.assertEquals(Some("10"), arvot.get("PK_B12"), "SV saa korotuksen")
    Assertions.assertEquals(Some("SV"), arvot.get("PK_B12_OPPIAINE"))
  }

  @Test def testB1KorotusEriKieltaEiKorota(): Unit = {
    // B1-SV pääsuorituksella, B1-EN korotus oppiaineen oppimäärältä
    // B-kielen korotus huomioidaan vain samalle kielelle ja laajuudelle → B1-EN ei korota B1-SV:tä
    val arvot = convertArvot(Seq(
      paasuoritusOpiskeluoikeus(Set(aine("B1", "7", Some("SV")))),
      korotusOpiskeluoikeus(Set(aine("B1", "10", Some("EN"))))
    ))

    Assertions.assertEquals(Some("7"),  arvot.get("PK_B1"))
    Assertions.assertEquals(Some("SV"), arvot.get("PK_B1_OPPIAINE"))
    Assertions.assertFalse(arvot.contains("PK_B12"), "B1-EN korotus ilman pohjasuoritusta ei luo uutta B1-numeroa")
  }

  @Test def testAidinkielenKorotusVainSamallakielella(): Unit = {
    // AI-AI1 ja AI-AI7 pääsuorituksella, vain AI-AI1 korotus oppiaineen oppimäärältä
    val arvot = convertArvot(Seq(
      paasuoritusOpiskeluoikeus(Set(aine("AI", "7", Some("AI1")), aine("AI", "9", Some("AI7")))),
      korotusOpiskeluoikeus(Set(aine("AI", "8", Some("AI1")), aine("AI", "10", Some("AI6"))) //AI6-korotusta ei huomioida, koska eri kieli
    )))

    Assertions.assertEquals(Some("8"), arvot.get("PK_AI")) //Sisältä korotuksen
    Assertions.assertEquals(Some("FI"), arvot.get("PK_AI_OPPIAINE"))
    Assertions.assertEquals(Some("AI1"), arvot.get("PK_AI_KIELITIETO"))
    Assertions.assertEquals(Some("9"), arvot.get("PK_AI2")) //Ei korotusta
    Assertions.assertEquals(Some("FI_2"), arvot.get("PK_AI2_OPPIAINE"))
    Assertions.assertEquals(Some("AI7"), arvot.get("PK_AI2_KIELITIETO"))
  }

  @Test def testYoArvoEnnenLeikkuripaivaa(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val tutkinto =
      """
        |{
        |  "ssn": "000000-000A",
        |  "graduationPeriod": "2013K",
        |  "graduationDate": "2021-12-01",
        |  "exams": [
        |    {
        |      "period": "2012K",
        |      "examId": "EA",
        |      "grade": "M",
        |      "points": 236
        |    }
        |  ],
        |  "language": "fi"
        |}
        |""".stripMargin

    val parsed = YtrToSuoritusConverter.toSuoritus(YtrParser.parseYtrData(tutkinto))
    val oikeudet = Seq(parsed)

    val leikkuri = LocalDate.parse("2023-05-29")

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)

    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.yoSuoritettuKey))
    Assertions.assertEquals(Some("2021"), converterResult.getAvainArvoMap().get(AvainArvoConstants.yoSuoritusvuosiKey))
    Assertions.assertEquals(Some("1"), converterResult.getAvainArvoMap().get(AvainArvoConstants.yoSuorituslukukausiKey))
    Assertions.assertEquals(Some("FI"), converterResult.getAvainArvoMap().get(AvainArvoConstants.yoTutkintoKieliKey))
  }

  @Test def testYoArvoLeikkuripaivanJalkeen(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val tutkinto =
      """
        |{
        |  "ssn": "000000-000A",
        |  "graduationPeriod": "2013K",
        |  "graduationDate": "2024-06-01",
        |  "exams": [
        |    {
        |      "period": "2012K",
        |      "examId": "EA",
        |      "grade": "M",
        |      "points": 236
        |    }
        |  ],
        |  "language": "fi"
        |}
        |""".stripMargin

    val parsed = YtrToSuoritusConverter.toSuoritus(YtrParser.parseYtrData(tutkinto))
    val oikeudet = Seq(parsed)

    val leikkuri = LocalDate.parse("2023-05-15")

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.yoSuoritettuKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.yoSuoritusvuosiKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.yoSuorituslukukausiKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.yoTutkintoKieliKey))

  }

  @Test def testAmmArvoMyohassaValmistunut(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val leikkuripaivanJalkeenValmistunutTutkinto = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi("123456", "koulutus", Some(1)), Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"),
      Koodi("valmistunut", "jokutila", Some(1)), SuoritusTila.VALMIS, Some(LocalDate.parse("2021-01-01")), Some(LocalDate.parse("2024-04-03")), None, Koodi("tapa", "suoritustapa", Some(1)), Koodi("kieli", "suorituskieli", Some(1)), Seq.empty)
    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(leikkuripaivanJalkeenValmistunutTutkinto), None, List.empty))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.ammSuoritettuKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.ammTutkintoKieliKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.ammSuoritusvuosiKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.ammSuorituslukukausiKey))
  }

  @Test def testAmmArvoAjoissaValmistunut(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val ajoissaValmistunut = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi("123456", "koulutus", Some(1)), Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"),
      Koodi("valmistunut", "jokutila", Some(1)), SuoritusTila.VALMIS, Some(LocalDate.parse("2021-01-01")), Some(LocalDate.parse("2023-04-03")), None, Koodi("tapa", "suoritustapa", Some(1)), Koodi("FI", "suorituskieli", Some(1)), Seq.empty)
    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(ajoissaValmistunut), None, List.empty))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.ammSuoritettuKey))
    Assertions.assertEquals(Some("FI"), converterResult.getAvainArvoMap().get(AvainArvoConstants.ammTutkintoKieliKey))
    Assertions.assertEquals(Some("2023"), converterResult.getAvainArvoMap().get(AvainArvoConstants.ammSuoritusvuosiKey))
    Assertions.assertEquals(Some("2"), converterResult.getAvainArvoMap().get(AvainArvoConstants.ammSuorituslukukausiKey))
  }

  private def diaKoesuoritus(arvosana: String): DIAOppiaineenKoesuoritus =
    DIAOppiaineenKoesuoritus(
      nimi = Kielistetty(Some("koe"), None, None),
      koodi = Koodi("kirjallinenkoe", "diaoppiaineenkoe", Some(1)),
      arvosana = DIAArvosana(Koodi(arvosana, "arviointiasteikkodiatutkinto", Some(1)), true),
      laajuus = None
    )

  private def diaOppiaine(
    koodiArvo: String,
    laajuus: Option[BigDecimal],
    kirjallinenKoe: Option[DIAOppiaineenKoesuoritus] = None,
    suullinenKoe: Option[DIAOppiaineenKoesuoritus] = None,
    vastaavuustodistuksenTiedot: Option[DIAVastaavuustodistuksenTiedot] = None,
    kieli: Option[Koodi] = None): DIAOppiaine =
    DIAOppiaine(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some(koodiArvo), None, None),
      koodi = Koodi(koodiArvo, "diaoppiaineet", Some(1)),
      laajuus = laajuus.map(l => DIALaajuus(l, Koodi("4", "opintojenlaajuusyksikko", Some(1)))),
      osaAlue = None,
      kieli = kieli,
      vastaavuustodistuksenTiedot = vastaavuustodistuksenTiedot,
      kirjallinenKoe = kirjallinenKoe,
      suullinenKoe = suullinenKoe
    )

  private def diaAidinkieliKoodi(kieliArvo: String): Koodi = Koodi(kieliArvo, "oppiainediaaidinkieli", Some(1))

  private def diaOpiskeluoikeus(supaTila: SuoritusTila, vahvistusPaivamaara: Option[LocalDate], oppiaineet: Seq[DIAOppiaine] = Seq.empty): GeneerinenOpiskeluoikeus = {
    val diaTutkinto = DIATutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("DIA-tutkinto"), None, None),
      Koodi("301104", "koulutus", Some(1)),
      Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"),
      Koodi("DE", "kieli", None),
      Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)),
      supaTila,
      Some(LocalDate.parse("2021-01-01")),
      vahvistusPaivamaara,
      oppiaineet
    )
    GeneerinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Koodi("diatutkinto", "opiskeluoikeudentyyppi", None), "1.2.3.4", Set(diaTutkinto), None, List.empty)
  }

  @Test def testDiaArvoValmisAjoissaVahvistettu(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03"))))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritettuKey))
    Assertions.assertEquals(Some("2023"), converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritusvuosiKey))
  }

  @Test def testDiaArvoValmisMyohassaVahvistettu(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2024-04-03"))))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritettuKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritusvuosiKey))
    val diaSelite = converterResult.paatellytArvot.find(_.avain == AvainArvoConstants.diaSuoritettuKey).get.selitteet.head
    Assertions.assertTrue(diaSelite.contains("leikkuripäivän"), s"Odotettiin myöhästymisestä kertovaa selitettä, saatiin: $diaSelite")
  }

  @Test def testDiaArvoValmisIlmanVahvistuspaivaa(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, None))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritettuKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritusvuosiKey))
  }

  @Test def testDiaArvoKesken(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.KESKEN, Some(LocalDate.parse("2023-04-03"))))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritettuKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritusvuosiKey))
  }

  @Test def testDiaArvoEiTutkintoa(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, Seq.empty, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritettuKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritusvuosiKey))
  }

  @Test def testDiaOppiaineLaajuus(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oppiaineet = Seq(
      diaOppiaine("FIN", Some(BigDecimal(4))),
      diaOppiaine("MATH", Some(BigDecimal("3.5")))
    )
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("4"), converterResult.getAvainArvoMap().get("DIA_FIN_LAAJUUS"))
    Assertions.assertEquals(Some("3.5"), converterResult.getAvainArvoMap().get("DIA_MATH_LAAJUUS"))
  }

  @Test def testDiaOppiaineIlmanLaajuutta(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oppiaineet = Seq(diaOppiaine("FIN", None))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get("DIA_FIN_LAAJUUS"))
  }

  @Test def testDiaOppiaineKirjallinen(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oppiaineet = Seq(diaOppiaine("FIN", None, kirjallinenKoe = Some(diaKoesuoritus("9"))))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("9"), converterResult.getAvainArvoMap().get("DIA_FIN_KIRJALLINEN"))
  }

  @Test def testDiaOppiaineIlmanKirjallista(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oppiaineet = Seq(diaOppiaine("FIN", None))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get("DIA_FIN_KIRJALLINEN"))
  }

  @Test def testDiaOppiaineSuullinen(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oppiaineet = Seq(diaOppiaine("FIN", None, suullinenKoe = Some(diaKoesuoritus("8"))))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("8"), converterResult.getAvainArvoMap().get("DIA_FIN_SUULLINEN"))
  }

  @Test def testDiaOppiaineIlmanSuullista(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oppiaineet = Seq(diaOppiaine("FIN", None))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get("DIA_FIN_SUULLINEN"))
  }

  @Test def testDiaOppiaineVastaavuus(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val vastaavuus = DIAVastaavuustodistuksenTiedot(BigDecimal("8.5"), DIALaajuus(BigDecimal(4), Koodi("4", "opintojenlaajuusyksikko", Some(1))))
    val oppiaineet = Seq(diaOppiaine("FIN", None, vastaavuustodistuksenTiedot = Some(vastaavuus)))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("8.5"), converterResult.getAvainArvoMap().get("DIA_FIN_VASTAAVUUS"))
  }

  @Test def testDiaOppiaineIlmanVastaavuutta(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oppiaineet = Seq(diaOppiaine("FIN", None))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get("DIA_FIN_VASTAAVUUS"))
  }

  // Äidinkieli (koodi "AI") tuottaa kielikohtaiset avaimet (SUOMI/SAKSA), ei geneerisiä DIA_AI_* -avaimia.
  @Test def testDiaAidinkieliSuomi(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val vastaavuus = DIAVastaavuustodistuksenTiedot(BigDecimal("8.5"), DIALaajuus(BigDecimal(4), Koodi("4", "opintojenlaajuusyksikko", Some(1))))
    val aidinkieli = diaOppiaine("AI", Some(BigDecimal(4)),
      kirjallinenKoe = Some(diaKoesuoritus("9")),
      suullinenKoe = Some(diaKoesuoritus("8")),
      vastaavuustodistuksenTiedot = Some(vastaavuus),
      kieli = Some(diaAidinkieliKoodi("FI")))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), Seq(aidinkieli)))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val avaimet = converterResult.getAvainArvoMap()
    Assertions.assertEquals(Some("4"), avaimet.get("DIA_AIDINKIELI_LAAJUUS_SUOMI"))
    Assertions.assertEquals(Some("9"), avaimet.get("DIA_AIDINKIELI_KIRJALLINEN_SUOMI"))
    Assertions.assertEquals(Some("8"), avaimet.get("DIA_AIDINKIELI_SUULLINEN_SUOMI"))
    Assertions.assertEquals(Some("8.5"), avaimet.get("DIA_AIDINKIELI_VASTAAVUUS_SUOMI"))
    // Geneerisiä DIA_AI_* -avaimia ei tuoteta äidinkielelle.
    Assertions.assertEquals(None, avaimet.get("DIA_AI_LAAJUUS"))
    Assertions.assertEquals(None, avaimet.get("DIA_AI_KIRJALLINEN"))
    Assertions.assertEquals(None, avaimet.get("DIA_AI_VASTAAVUUS"))
  }

  @Test def testDiaAidinkieliSaksa(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val vastaavuus = DIAVastaavuustodistuksenTiedot(BigDecimal("7.0"), DIALaajuus(BigDecimal(4), Koodi("4", "opintojenlaajuusyksikko", Some(1))))
    val aidinkieli = diaOppiaine("AI", Some(BigDecimal(10)),
      kirjallinenKoe = Some(diaKoesuoritus("5")),
      suullinenKoe = Some(diaKoesuoritus("6")),
      vastaavuustodistuksenTiedot = Some(vastaavuus),
      kieli = Some(diaAidinkieliKoodi("DE")))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), Seq(aidinkieli)))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val avaimet = converterResult.getAvainArvoMap()
    Assertions.assertEquals(Some("10"), avaimet.get("DIA_AIDINKIELI_LAAJUUS_SAKSA"))
    Assertions.assertEquals(Some("5"), avaimet.get("DIA_AIDINKIELI_KIRJALLINEN_SAKSA"))
    Assertions.assertEquals(Some("6"), avaimet.get("DIA_AIDINKIELI_SUULLINEN_SAKSA"))
    Assertions.assertEquals(Some("7.0"), avaimet.get("DIA_AIDINKIELI_VASTAAVUUS_SAKSA"))
    Assertions.assertEquals(None, avaimet.get("DIA_AI_LAAJUUS"))
  }

  // Ydintapaus: äidinkieli sekä suomeksi että saksaksi (molemmat koodi "AI") -> ei avainten törmäystä.
  @Test def testDiaAidinkieliSuomiJaSaksa(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val suomi = diaOppiaine("AI", Some(BigDecimal(4)),
      kirjallinenKoe = Some(diaKoesuoritus("9")),
      kieli = Some(diaAidinkieliKoodi("FI")))
    val saksa = diaOppiaine("AI", Some(BigDecimal(10)),
      kirjallinenKoe = Some(diaKoesuoritus("5")),
      kieli = Some(diaAidinkieliKoodi("DE")))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), Seq(suomi, saksa)))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val avaimet = converterResult.getAvainArvoMap()
    Assertions.assertEquals(Some("4"), avaimet.get("DIA_AIDINKIELI_LAAJUUS_SUOMI"))
    Assertions.assertEquals(Some("9"), avaimet.get("DIA_AIDINKIELI_KIRJALLINEN_SUOMI"))
    Assertions.assertEquals(Some("10"), avaimet.get("DIA_AIDINKIELI_LAAJUUS_SAKSA"))
    Assertions.assertEquals(Some("5"), avaimet.get("DIA_AIDINKIELI_KIRJALLINEN_SAKSA"))
    Assertions.assertEquals(None, avaimet.get("DIA_AI_LAAJUUS"))
  }

  // Tuntematon kieli (esim. S2) tai puuttuva kieli -> äidinkielelle ei tuoteta avaimia.
  @Test def testDiaAidinkieliTuntematonKieliEiTuotaAvaimia(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val s2 = diaOppiaine("AI", Some(BigDecimal(4)),
      kirjallinenKoe = Some(diaKoesuoritus("9")),
      kieli = Some(diaAidinkieliKoodi("S2")))
    val ilmanKielta = diaOppiaine("AI", Some(BigDecimal(5)),
      kirjallinenKoe = Some(diaKoesuoritus("7")),
      kieli = None)
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), Seq(s2, ilmanKielta)))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val avaimet = converterResult.getAvainArvoMap()
    Assertions.assertTrue(avaimet.keys.forall(avain => !avain.startsWith("DIA_AIDINKIELI") && !avain.startsWith("DIA_VASTAAVUUS") && avain != "DIA_AI_LAAJUUS"),
      s"Tuntemattomalle äidinkielelle ei pitäisi tuottaa avaimia, saatiin: ${avaimet.keys.filter(_.startsWith("DIA_"))}")
  }

  // Kielioppiaineet (koodi yksittäinen iso kirjain + numerot, esim. A1, B2) saavat _KIELI-infixin:
  // DIA_<KOODI>_KIELI_<POSTFIX>.
  @Test def testDiaKieliOppiaineLaajuusSaaKieliInfixin(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oppiaineet = Seq(
      diaOppiaine("A1", Some(BigDecimal(4))),
      diaOppiaine("B2", Some(BigDecimal("3.5")))
    )
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val avaimet = converterResult.getAvainArvoMap()
    Assertions.assertEquals(Some("4"), avaimet.get("DIA_A1_KIELI_LAAJUUS"))
    Assertions.assertEquals(Some("3.5"), avaimet.get("DIA_B2_KIELI_LAAJUUS"))
    // Ilman infixiä olevaa avainta ei tuoteta kielioppiaineelle.
    Assertions.assertEquals(None, avaimet.get("DIA_A1_LAAJUUS"))
  }

  // Yksittäinen iso kirjain ilman numeroa (esim. A, B) tulkitaan myös kieleksi.
  @Test def testDiaKieliOppiaineYksittainenKirjain(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val vastaavuus = DIAVastaavuustodistuksenTiedot(BigDecimal("8.5"), DIALaajuus(BigDecimal(4), Koodi("4", "opintojenlaajuusyksikko", Some(1))))
    val oppiaineet = Seq(diaOppiaine("A", None,
      kirjallinenKoe = Some(diaKoesuoritus("9")),
      suullinenKoe = Some(diaKoesuoritus("8")),
      vastaavuustodistuksenTiedot = Some(vastaavuus)))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val avaimet = converterResult.getAvainArvoMap()
    Assertions.assertEquals(Some("9"), avaimet.get("DIA_A_KIELI_KIRJALLINEN"))
    Assertions.assertEquals(Some("8"), avaimet.get("DIA_A_KIELI_SUULLINEN"))
    Assertions.assertEquals(Some("8.5"), avaimet.get("DIA_A_KIELI_VASTAAVUUS"))
    Assertions.assertEquals(None, avaimet.get("DIA_A_KIRJALLINEN"))
  }

  // Monikirjaiminen koodi (esim. BI) ei ole kieli, joten _KIELI-infixiä ei lisätä.
  @Test def testDiaEiKieliOppiaineEiSaaKieliInfixia(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oppiaineet = Seq(diaOppiaine("BI", Some(BigDecimal(4))))
    val oikeudet = Seq(diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), oppiaineet))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val avaimet = converterResult.getAvainArvoMap()
    Assertions.assertEquals(Some("4"), avaimet.get("DIA_BI_LAAJUUS"))
    Assertions.assertEquals(None, avaimet.get("DIA_BI_KIELI_LAAJUUS"))
  }

  @Test def testDiaUseampiValmisTutkintoHeittaaPoikkeuksen(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val oikeudet = Seq(
      diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03"))),
      diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")))
    )

    Assertions.assertThrows(classOf[RuntimeException], () =>
      AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty))
  }

  // Keskeytyneitä DIA-tutkintoja voi olla useita; ne eivät estä laskentaa eivätkä vaikuta avaimiin,
  // vaan kaikki avaimet lasketaan ainoasta valmiista tutkinnosta.
  @Test def testDiaKeskeytynytEiEstaValmiinKasittelya(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val keskeytynyt = diaOpiskeluoikeus(SuoritusTila.KESKEYTYNYT, Some(LocalDate.parse("2020-04-03")), Seq(diaOppiaine("MATH", Some(BigDecimal(9)))))
    val valmis = diaOpiskeluoikeus(SuoritusTila.VALMIS, Some(LocalDate.parse("2023-04-03")), Seq(diaOppiaine("FIN", Some(BigDecimal(4)))))
    val oikeudet = Seq(keskeytynyt, valmis)

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritettuKey))
    Assertions.assertEquals(Some("2023"), converterResult.getAvainArvoMap().get(AvainArvoConstants.diaSuoritusvuosiKey))
    // Avaimet tulevat valmiista tutkinnosta, ei keskeytyneestä.
    Assertions.assertEquals(Some("4"), converterResult.getAvainArvoMap().get("DIA_FIN_LAAJUUS"))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get("DIA_MATH_LAAJUUS"))
  }

  @Test def testTelmaRiittavaLaajuus(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val suoritusVuosi = 2022
    val telmaTutkinto = Telma(
      UUID.randomUUID(),
      Kielistetty(Some("Työhön ja itsenäiseen elämään valmentava koulutus"), None, None),
      Koodi("999903", "koulutus", Some(1)),
      Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
      Koodi("valmistunut", "suorituksentila", Some(1)),
      SuoritusTila.VALMIS,
      LocalDate.parse("2021-01-01"),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Koodi("FI", "kieli", Some(1)),
      Some (Laajuus(26, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
      List(Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), TELMA.defaultLuokka.get, VALMIS, None, TELMA))
    )

    val telmaOikeus = AmmatillinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Oppilaitos(Kielistetty(None, None, None), ""),
      Set(telmaTutkinto),
      None,
      List.empty
    )
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> "1",
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020" //Ei merkitystä, koska 2017 jälkeen
    ))

    val suoritettuPerusopetus = getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2021-05-15"))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(suoritettuPerusopetus, telmaOikeus), Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), converterResult.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritettuKey))
    Assertions.assertEquals(Some(suoritusVuosi.toString), converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritusvuosiKey))

    //Lisäpistekoulutuksia ei huomioida, jos pohjakoulutus on POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS tai POHJAKOULUTUS_EI_PAATTOTODISTUSTA
    val converterResultWithoutPerusopetus = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(telmaOikeus), Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA), converterResultWithoutPerusopetus.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    Assertions.assertEquals(Some("false"), converterResultWithoutPerusopetus.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritettuKey))
    Assertions.assertEquals(None, converterResultWithoutPerusopetus.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritusvuosiKey))

  }

  @Test def testTelmaRiittamatonLaajuus(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val suoritusVuosi = 2022

    val telmaTutkinto = Telma(
      UUID.randomUUID(),
      Kielistetty(Some("Työhön ja itsenäiseen elämään valmentava koulutus"), None, None),
      Koodi("999903", "koulutus", Some(1)),
      Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
      Koodi("valmistunut", "suorituksentila", Some(1)),
      SuoritusTila.VALMIS,
      LocalDate.parse("2021-01-01"),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Koodi("FI", "kieli", Some(1)),
      Some(Laajuus(24, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
      List(Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), TELMA.defaultLuokka.get, VALMIS, None, TELMA))
    )

    val telmaOikeus = AmmatillinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Oppilaitos(Kielistetty(None, None, None), ""),
      Set(telmaTutkinto),
      None,
      List.empty
    )
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> "0",
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020"
    ))

    val suoritettuPerusopetus = getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2021-05-15"))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(telmaOikeus, suoritettuPerusopetus), Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritettuKey))

    //Jos Telmaa ei ole suoritettu riittävässä laajuudessa, suoritusvuodelle ei saa tulla avain-arvoa.
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritusvuosiKey))
  }

  @Test def testTelmaLiianVanhaSuoritus(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2024-05-15")
    val suoritusVuosi = 2022 //Tämä on liian vanha, koska haun hakuVuosi on 2024.

    val telmaTutkinto = Telma(
      UUID.randomUUID(),
      Kielistetty(Some("Työhön ja itsenäiseen elämään valmentava koulutus"), None, None),
      Koodi("999903", "koulutus", Some(1)),
      Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
      Koodi("valmistunut", "suorituksentila", Some(1)),
      SuoritusTila.VALMIS,
      LocalDate.parse("2021-01-01"),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Koodi("FI", "kieli", Some(1)),
      Some(Laajuus(26, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
      List(Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), TELMA.defaultLuokka.get, VALMIS, None, TELMA))
    )

    val haku = KoutaHaku(
      oid = "1.2.246.562.29.01000000000000012345",
      tila = "julkaistu",
      nimi = Map("fi" -> s"Testi haku 1.2.246.562.29.01000000000000012345"),
      hakutapaKoodiUri = "hakutapa_01",
      kohdejoukkoKoodiUri = Some("haunkohdejoukko_11#1"),
      hakuajat = List.empty,
      kohdejoukonTarkenneKoodiUri = None,
      hakuvuosi = Some(2024)
    )

    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Oppilaitos(Kielistetty(None, None, None), ""),
      Set(telmaTutkinto),
      None,
      List.empty
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, haku, None, Map.empty)

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritettuKey))

    //Jos Telmaa ei ole suoritettu hakuvuonna tai sitä edeltävänä vuonna, suoritusvuodelle ei saa tulla avain-arvoa.
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritusvuosiKey))
  }

  @Test def testTuvaRiittavaLaajuus(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val suoritusVuosi = 2022
    val tuvaTutkinto = Tuva(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkintokoulutukseen valmentava koulutus"), None, None),
      Koodi("999904", "koulutus", Some(1)),
      Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
      Koodi("valmistunut", "suorituksentila", Some(1)),
      SuoritusTila.VALMIS,
      LocalDate.parse("2021-01-01"),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Some(Laajuus(38, Koodi("4", "opintojenlaajusyksikkö", Some(1)), None, None)),
      List(Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), "tuva", VALMIS, None, TUVA))
    )

    val tuvaOikeus = GeneerinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Koodi("", "", None),
      "2.3.4.5",
      Set(tuvaTutkinto),
      None,
      List.empty
    )
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> "1",
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020" //Ei huomioida, koska 2017 jälkeen
    ))

    val suoritettuPerusopetus = getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2021-05-15"))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(tuvaOikeus, suoritettuPerusopetus), Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), converterResult.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritettuKey))
    Assertions.assertEquals(Some(suoritusVuosi.toString), converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritusvuosiKey))

    //Lisäpistekoulutuksia ei huomioida, jos pohjakoulutus on POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS tai POHJAKOULUTUS_EI_PAATTOTODISTUSTA
    val converterResultWithoutPerusopetus = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(tuvaOikeus), Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA), converterResultWithoutPerusopetus.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    Assertions.assertEquals(Some("false"), converterResultWithoutPerusopetus.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritettuKey))
    Assertions.assertEquals(None, converterResultWithoutPerusopetus.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritusvuosiKey))

  }

  @Test def testTuvaRiittamatonLaajuus(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val suoritusVuosi = 2022

    val tuvaTutkinto = Tuva(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkintokoulutukseen valmentava koulutus"), None, None),
      Koodi("999904", "koulutus", Some(1)),
      Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
      Koodi("valmistunut", "suorituksentila", Some(1)),
      SuoritusTila.VALMIS,
      LocalDate.parse("2021-01-01"),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      None,
      List(Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), "tuva", VALMIS, None, TUVA))
    )

    val oikeudet = Seq(GeneerinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Koodi("", "", None),
      "2.3.4.5",
      Set(tuvaTutkinto),
      None,
      List.empty
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritettuKey))

    //Jos Tuvaa ei ole suoritettu hakuvuonna tai sitä edeltävänä vuonna, suoritusvuodelle ei saa tulla avain-arvoa.
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritusvuosiKey))
  }

  @Test def testTuvaLiianVanhaSuoritus(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2024-05-15")
    val suoritusVuosi = 2022 // Liian vanha, koska hakuvuosi 2024

    val tuvaTutkinto = Tuva(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkintokoulutukseen valmentava koulutus"), None, None),
      Koodi("999904", "koulutus", Some(1)),
      Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
      Koodi("valmistunut", "suorituksentila", Some(1)),
      SuoritusTila.VALMIS,
      LocalDate.parse("2021-01-01"),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Some(Laajuus(22, Koodi("4", "opintojenlaajusyksikkö", Some(1)), None, None)),
      List(Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), "tuva", VALMIS, None, TUVA))
    )

    val haku = KoutaHaku(
      oid = "1.2.246.562.29.01000000000000012345",
      tila = "julkaistu",
      nimi = Map("fi" -> s"Testi haku 1.2.246.562.29.01000000000000012345"),
      hakutapaKoodiUri = "hakutapa_01",
      kohdejoukkoKoodiUri = Some("haunkohdejoukko_11#1"),
      hakuajat = List.empty,
      kohdejoukonTarkenneKoodiUri = None,
      hakuvuosi = Some(2024)
    )

    val oikeudet = Seq(GeneerinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Koodi("", "", None),
      "2.3.4.5",
      Set(tuvaTutkinto),
      None,
      List.empty
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, oikeudet, Seq.empty, leikkuriPaiva, haku, None, Map.empty)

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritettuKey))

    //Jos Tuvaa ei ole suoritettu hakuvuonna tai sitä edeltävänä vuonna, suoritusvuodelle ei saa tulla avain-arvoa.
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritusvuosiKey))
  }

  @Test def testOpistovuosiRiittavaLaajuus(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val suoritusVuosi = 2022
    val opistovuosiKoulutus = VapaaSivistystyo(
      UUID.randomUUID(),
      Kielistetty(Some("Kansanopiston opistovuosi"), None, None),
      Koodi("999901", "koulutus", Some(1)),
      Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
      Koodi("valmistunut", "suorituksentila", Some(1)),
      SuoritusTila.VALMIS,
      LocalDate.parse("2021-01-01"),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Some(Laajuus(28, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
      Koodi("FI", "kieli", Some(1)),
      List(Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), VAPAA_SIVISTYSTYO.defaultLuokka.get, SuoritusTila.VALMIS, None, VAPAA_SIVISTYSTYO))
    )

    val opistovuosiOikeus = GeneerinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Koodi("", "", None),
      "oppilaitosOid",
      Set(opistovuosiKoulutus),
      None,
      List.empty
    )
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> "0",
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020"
    ))

    val suoritettuPerusopetus = getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2021-05-15"))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(opistovuosiOikeus, suoritettuPerusopetus), Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), converterResult.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritettuKey))
    Assertions.assertEquals(Some(suoritusVuosi.toString), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritusvuosiKey))

    //Lisäpistekoulutuksia ei huomioida, jos pohjakoulutus on POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS tai POHJAKOULUTUS_EI_PAATTOTODISTUSTA
    val converterResultWithoutPerusopetus = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(opistovuosiOikeus), Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS), converterResultWithoutPerusopetus.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    Assertions.assertEquals(Some("false"), converterResultWithoutPerusopetus.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritettuKey))
    Assertions.assertEquals(None, converterResultWithoutPerusopetus.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritusvuosiKey))
  }

  @Test def testOpistovuosiRiittamatonLaajuus(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")

    val suoritusVuosi = 2022

    val opistovuosiKoulutus = VapaaSivistystyo(
      UUID.randomUUID(),
      Kielistetty(Some("Kansanopiston opistovuosi"), None, None),
      Koodi("999901", "koulutus", Some(1)),
      Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
      Koodi("valmistunut", "suorituksentila", Some(1)),
      SuoritusTila.VALMIS,
      LocalDate.parse("2021-01-01"),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Some(Laajuus(22, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
      Koodi("FI", "kieli", Some(1)),
      List(Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), VAPAA_SIVISTYSTYO.defaultLuokka.get, SuoritusTila.VALMIS, None, VAPAA_SIVISTYSTYO))
    )

    val opistovuosiOikeus = GeneerinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Koodi("", "", None),
      "oppilaitosOid",
      Set(opistovuosiKoulutus),
      None,
      List.empty
    )
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> "0",
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020"
    ))

    val suoritettuPerusopetus = getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2021-05-15"))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(opistovuosiOikeus, suoritettuPerusopetus), Seq.empty, leikkuriPaiva, DEFAULT_KOUTA_HAKU, None, Map.empty)

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritettuKey))

    //Jos Opistovuotta ei ole suoritettu riittävässä laajuudessa, suoritusvuodelle ei saa tulla avain-arvoa.
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritusvuosiKey))
  }

  @Test def testAvainArvoConverterForToisenAsteenPohjakoulutusLoytyySupasta(): Unit = {
    val opiskeluoikeusOid = "1.2.246.562.15.09876543210"
    val oppilaitosOid = "1.2.246.562.10.00000000234"
    val personOid = "1.2.246.562.98.69863082363"

    val oppiaineet = Seq(
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("historia"), None, None),
        Koodi("HI", "koodisto", None), Koodi("8", "koodisto", None),
        None, true, None, None),
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia"), None, None),
        Koodi("BI", "koodisto", None), Koodi("9", "koodisto", None),
        None, true, None, None))
    val perusopetuksenOppimaaraValmis = PerusopetuksenOppimaara(
      UUID.randomUUID(),
      None,
      Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
      None,
      Koodi("toinenarvo", "koodisto", Some(1)),
      SuoritusTila.VALMIS,
      Koodi("FI", "kielikoodisto", Some(1)),
      Set.empty,
      yksilollistaminen = Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY),
      None,
      vahvistusPaivamaara = Some(LocalDate.parse("2025-05-30")),
      oppiaineet,
      List.empty,
      false,
      vuosiluokkiinSitoutumatonOpetus = false,
      luokkaAste = Some(9))

    val lisatiedot = KoskiLisatiedot(None, Some(true), None)
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(perusopetuksenOppimaaraValmis), Some(lisatiedot), SuoritusTila.VALMIS, List.empty)

    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> "0",
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020"
    ))

    val leikkuri = LocalDate.now
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(opiskeluoikeus), Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)

    //Suoritukselta poimittu yksilöllistämistieto välittyy
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU_OSITTAIN_YKSILOLLISTETTY), converterResult.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))

    Assertions.assertEquals(Some("FI"), converterResult.getAvainArvoMap().get(AvainArvoConstants.perusopetuksenKieliKey))
    Assertions.assertEquals(Some("2025"), converterResult.getAvainArvoMap().get(AvainArvoConstants.peruskouluSuoritusvuosiKey))
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.peruskouluSuoritettuKey))
  }

  //Testataan seuraavat tapaukset:
  //Perusopetuksen oppimäärän suoritus kesken ja:
  //-vuosiluokkiinSitoutumatonOpetus true, arvosanoissa nelosia
  //-vuosiluokkiinSitoutumatonOpetus false, arvosanoissa nelosia
  //-vuosiluokkiinSitoutumatonOpetus true, arvosanoissa ei nelosia
  //-vuosiluokkiinSitoutumatonOpetus false, arvosanoissa ei nelosia
  @Test def testAvainArvoConverterForToisenAsteenPohjakoulutusArvosanoissaNelosiaSuoritusKesken(): Unit = {
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> "6",
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020"
    ))

    val opiskeluoikeusOid = "1.2.246.562.15.09876543210"
    val oppilaitosOid = "1.2.246.562.10.00000000234"
    val oppiaineetArvosanoissaNelosia = Seq(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("arvo", "koodisto", None), Koodi("4", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None))
    val oppiaineetArvosanoissaEiNelosia = Seq(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("arvo", "koodisto", None), Koodi("8", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None))
    val perusopetuksenOppimaaraBase = PerusopetuksenOppimaara(
      UUID.randomUUID(),
      None,
      Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
      None,
      Koodi("toinenarvo", "koodisto", Some(1)),
      SuoritusTila.KESKEN,
      Koodi("FI", "kielikoodisto", Some(1)),
      Set.empty,
      None,
      None,
      None,
      oppiaineetArvosanoissaNelosia,
      List.empty,
      false,
      vuosiluokkiinSitoutumatonOpetus = false,
      luokkaAste = None)

    val leikkuriDeadlineOhitettu =
      java.time.Instant.ofEpochMilli(System.currentTimeMillis())
        .atZone(java.time.ZoneId.systemDefault())
        .minusDays(1)
        .toLocalDate

    val lisatiedot = KoskiLisatiedot(None, Some(true), None)
    val opiskeluoikeusVSOPNelosia = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(perusopetuksenOppimaaraBase.copy(aineet = oppiaineetArvosanoissaNelosia, vuosiluokkiinSitoutumatonOpetus = true)), Some(lisatiedot), SuoritusTila.KESKEN, List.empty)
    val opiskeluoikeusVSOPEiNelosia = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(perusopetuksenOppimaaraBase.copy(aineet = oppiaineetArvosanoissaEiNelosia, vuosiluokkiinSitoutumatonOpetus = true)), Some(lisatiedot), SuoritusTila.KESKEN, List.empty)
    val opiskeluoikeusNelosia = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(perusopetuksenOppimaaraBase.copy(aineet = oppiaineetArvosanoissaNelosia, vuosiluokkiinSitoutumatonOpetus = false)), Some(lisatiedot), SuoritusTila.KESKEN, List.empty)
    val opiskeluoikeusEiNelosia = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(perusopetuksenOppimaaraBase.copy(aineet = oppiaineetArvosanoissaEiNelosia, vuosiluokkiinSitoutumatonOpetus = false)), Some(lisatiedot), SuoritusTila.KESKEN, List.empty)

    //Ehdot-override perustuu leikkurihetken opiskeluoikeuksiin, joten samat opiskeluoikeudet
    //syötetään sekä nykyisinä että leikkurihetken opiskeluoikeuksina.
    val converterResultVSOPNelosia = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusVSOPNelosia), Seq(opiskeluoikeusVSOPNelosia), leikkuriDeadlineOhitettu, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val converterResult2VSOPEiNelosia = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusVSOPEiNelosia), Seq(opiskeluoikeusVSOPEiNelosia), leikkuriDeadlineOhitettu, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val converterResult3Nelosia = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusNelosia), Seq(opiskeluoikeusNelosia), leikkuriDeadlineOhitettu, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val converterResult4EiNelosia = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusEiNelosia), Seq(opiskeluoikeusEiNelosia), leikkuriDeadlineOhitettu, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA), converterResultVSOPNelosia.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA), converterResult2VSOPEiNelosia.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), converterResult3Nelosia.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA), converterResult4EiNelosia.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
  }

  @Test def testAvainArvoConverterForToisenAsteenPohjakoulutusUskotaanHakemukseenUlkomainen(): Unit = {
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS,
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020"
    ))
    val leikkuri = LocalDate.now
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq.empty, Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS), converterResult.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
  }

  @Test def testAvainArvoConverterForToisenAsteenPohjakoulutusUskotaanHakemukseen2017TaiEnnen(): Unit = {
    Seq("1", "2", "3", "6", "8", "9").foreach(hakemuksenPohjakoulutus => {
      val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusKey -> hakemuksenPohjakoulutus,
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017"
      ))
      val leikkuri = LocalDate.now
      val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq.empty, Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)

      Assertions.assertEquals(Some(hakemuksenPohjakoulutus), converterResult.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    })
  }

  @Test def testArvosanatHakemukseltaJaYksiKorotusSupasta(): Unit = {
    val keyValues = Map(
      "arvosana-KO_group0" -> "arvosana-KO-7",
      "oppiaine-valinnainen-kieli_group0" -> "oppiaine-valinnainen-kieli-b2",
      "arvosana-valinnainen-kieli_group0" -> "arvosana-valinnainen-kieli-6",
      //"arvosana-FY_group0" -> "arvosana-FY-6", huom. tämä arvosana poistettu hakemuksen arvoista - testataan, että "orpo" Supasta löytynyt korotus ei päädy avain-arvoihin.
      "oppimaara-kieli-valinnainen-kieli_group0" -> "DE",
      "oppimaara-kieli-A1_group0" -> "EN",
      "arvosana-KE_group0" -> "arvosana-KE-7", //Tämä arvosana yliajetaan korotuksella
      "arvosana-TE_group0" -> "arvosana-TE-7",
      "arvosana-A1_group0" -> "arvosana-A1-8",
      "oppimaara-a-valinnainen-kieli_group0" -> "",
      "arvosana-KU_group0" -> "arvosana-KU-8",
      "arvosana-HI_group0" -> "arvosana-HI-6",
      "arvosana-KU_group1" -> "arvosana-KU-9",
      "oppimaara-a_group0" -> "suomi-aidinkielena",
      "arvosana-KT_group0" -> "arvosana-KT-7",
      "arvosana-KA_group0" -> "arvosana-KA-7",
      "arvosana-TY_group0" -> "arvosana-TY-7",
      "arvosana-KS_group1" -> "arvosana-KS-hyvaksytty",
      "arvosana-MU_group0" -> "arvosana-MU-7",
      "arvosana-valinnainen-kieli_group1" -> "",
      "arvosana-GE_group0" -> "arvosana-GE-8",
      "oppimaara-a-valinnainen-kieli_group1" -> "",
      "arvosana-TT_group0" -> "arvosana-TT-7",
      "oppiaine-valinnainen-kieli_group1" -> "",
      "arvosana-KS_group0" -> "arvosana-KS-7",
      "arvosana-YH_group0" -> "arvosana-YH-8",
      "arvosana-LI_group0" -> "arvosana-LI-8",
      "arvosana-A_group0" -> "arvosana-A-8",
      "oppimaara-kieli-B1_group0" -> "SV",
      "arvosana-KO_group1" -> "arvosana-KO-hyvaksytty",
      "arvosana-BI_group0" -> "arvosana-BI-7",
      "arvosana-B1_group0" -> "arvosana-B1-6",
      "arvosana-MA_group0" -> "arvosana-MA-7",
      "arvosana-KA_group1" -> "arvosana-KA-hyvaksytty",
      "oppimaara-kieli-valinnainen-kieli_group1" -> "",
      "pohjakoulutus_vuosi" -> "2016" //Tällä vuodella on merkitystä jotta hakemuksen arvosanat huomioidaan.
    )
    val hakemus = BASE_HAKEMUS.copy(keyValues = keyValues)

    //Yhdelle hakemuksen arvosanoista löytyy korotus, muille ei
    val korotus1Kemia = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("kemia"), None, None), Koodi("KE", "koodisto", None), Koodi("9", "koodisto", None), None, true, None, None)
    val korotus1Suoritus = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Some(LocalDate.parse("2025-06-08")), Some(LocalDate.parse("2025-06-08")), Set(korotus1Kemia), false)
    val korotus1Opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.09876543210"), "1.2.246.562.10.09876543211", Set(korotus1Suoritus), None, SuoritusTila.VALMIS, List.empty)

    val orpoOppiaineFysiikka = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("maantieto"), None, None), Koodi("FY", "koodisto", None), Koodi("9", "koodisto", None), None, true, None, None)
    val orpoOppiaineFysiikkaSuoritus = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Some(LocalDate.parse("2025-06-08")), Some(LocalDate.parse("2025-06-08")), Set(orpoOppiaineFysiikka), false)
    val orpoOppiaineFysiikkaOpiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.09876543210"), "1.2.246.562.10.09876543211", Set(orpoOppiaineFysiikkaSuoritus), None, SuoritusTila.VALMIS, List.empty)

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.3", Some(hakemus), Seq(korotus1Opiskeluoikeus, orpoOppiaineFysiikkaOpiskeluoikeus), Seq.empty, LocalDate.now(), DEFAULT_KOUTA_HAKU, None, Map.empty)
    val resultMap = converterResult.getAvainArvoMap()
    println(s"resultMap: $resultMap")

    //Tarkistetaan, että arvosanat vastaavat oletettuja
    val tavoiteArvosanat = Map(
      "PK_LI" -> "8",
      "PK_MU" -> "7",
      "PK_MA" -> "7",
      "PK_KO" -> "7",
      "PK_AI" -> "8",
      "PK_GE" -> "8",
      "PK_B1_OPPIAINE" -> "SV",
      "PK_KU_VAL1" -> "9",
      "PK_B1" -> "6",
      "PK_TT" -> "7",
      "PK_KU" -> "8",
      "PK_YH" -> "8",
      "PK_KS" -> "7",
      //"PK_FY" -> "6",
      "PK_A1_OPPIAINE" -> "EN",
      "PK_TE" -> "7",
      "PK_TY" -> "7",
      "PK_B2" -> "6",
      "PK_KT" -> "7",
      "PK_KE" -> "9", //Tämä arvosana on korotettu oppiaineen oppimäärän suorituksella, olisi muuten hakemuksen perusteella 7
      "PK_AI_OPPIAINE" -> "FI",
      "PK_HI" -> "6",
      "PK_BI" -> "7",
      "PK_B2_OPPIAINE" -> "DE",
      "PK_A1" -> "8",
      "PK_KA" -> "7"
    )

    tavoiteArvosanat.foreach { case (key, value) =>
      Assertions.assertEquals(
        value,
        resultMap(key)
      )
    }

    Assertions.assertFalse(resultMap.contains("PK_FY"))
  }

  @Test def testSupanKorotusVoittaaHakemuksenAineenSeliteOnSupa(): Unit = {
    // Hakemuksella KE=7, Supassa korotus KE=9 → korotus voittaa, selite on Supa
    val tulos = hakemukseltaConvertTulos(
      Map("arvosana-KE_group0" -> "arvosana-KE-7"),
      Seq(korotusOpiskeluoikeus(Set(aine("KE", "9"))))
    )
    val ke = tulos.find(_.avain == "PK_KE").get
    Assertions.assertEquals("9", ke.arvo)
    Assertions.assertEquals(AvainArvoConstants.arvosananLahdeSeliteSupa, ke.selitteet.head)
  }

  @Test def testHakemuksenAineVoittaaHeikoimmanSupanKorotuksenSeliteOnHakemus(): Unit = {
    // Hakemuksella KE=9, Supassa korotusyritys KE=7 → hakemus voittaa, selite on hakemus
    val tulos = hakemukseltaConvertTulos(
      Map("arvosana-KE_group0" -> "arvosana-KE-9"),
      Seq(korotusOpiskeluoikeus(Set(aine("KE", "7"))))
    )
    val ke = tulos.find(_.avain == "PK_KE").get
    Assertions.assertEquals("9", ke.arvo)
    Assertions.assertEquals(AvainArvoConstants.arvosananLahdeSeliteHakemus, ke.selitteet.head)
  }

  //Huom. Tässä testissä vain yksi A1-kieli tulee "pakollisen" A1-kielen kautta, toinen on valinnainen.
  //Tapaus, jossa pakollisia A1-kieliä on useampia, vain yksi välittyy avain-arvoihin ja sitä kautta valinnoille.
  @Test def testHakemuksenNumeroituA1Kieli(): Unit = {
    // Pakollinen A1-EN hakemukselta sekä valinnainen A1-FI: EN järjestyy ensin (PK_A1), FI toiseksi (PK_A12)
    val tulos = hakemukseltaConvertTulos(Map(
      "arvosana-A1_group0"                      -> "arvosana-A1-8",
      "oppimaara-kieli-A1_group0"               -> "EN",
      "oppiaine-valinnainen-kieli_group0"        -> "oppiaine-valinnainen-kieli-a1",
      "oppimaara-kieli-valinnainen-kieli_group0" -> "FI",
      "arvosana-valinnainen-kieli_group0"        -> "arvosana-valinnainen-kieli-9"
    ))
    val arvot = tulos.map(c => c.avain -> c.arvo).toMap
    Assertions.assertEquals(Some("8"),  arvot.get("PK_A1"))
    Assertions.assertEquals(Some("EN"), arvot.get("PK_A1_OPPIAINE"))
    Assertions.assertEquals(Some("9"),  arvot.get("PK_A12"))
    Assertions.assertEquals(Some("FI"), arvot.get("PK_A12_OPPIAINE"))
    Assertions.assertFalse(arvot.contains("PK_A13"), "Ei kolmatta A1-kieltä")
  }

  @Test def testA2KorotusSupastaKorottaaHakemuksenA1ta(): Unit = {
    // A1-EN hakemukselta, A2-EN korotus Supasta → A-kieli-logiikka: A2 voi korottaa samaa A1-kieltä
    val tulos = hakemukseltaConvertTulos(
      Map("arvosana-A1_group0" -> "arvosana-A1-8", "oppimaara-kieli-A1_group0" -> "EN"),
      Seq(korotusOpiskeluoikeus(Set(aine("A2", "10", Some("EN")))))
    )
    val arvot = tulos.map(c => c.avain -> c.arvo).toMap
    Assertions.assertEquals(Some("10"), arvot.get("PK_A1"))
    Assertions.assertEquals(Some("EN"), arvot.get("PK_A1_OPPIAINE"))
    Assertions.assertFalse(arvot.contains("PK_A2"),  "A2-korotus ei luo uutta PK_A2-avainta")
    Assertions.assertFalse(arvot.contains("PK_A12"), "A2-korotus ei luo uutta A1-numeroa")
  }

  @Test def testB1KorotusSupastaEriKielellaeEiKorotaHakemuksenB1ta(): Unit = {
    // B1-SV hakemukselta, B1-EN korotus Supasta → B-kieli-logiikka: eri kieli ei korota
    val tulos = hakemukseltaConvertTulos(
      Map("arvosana-B1_group0" -> "arvosana-B1-7", "oppimaara-kieli-B1_group0" -> "SV"),
      Seq(korotusOpiskeluoikeus(Set(aine("B1", "10", Some("EN")))))
    )
    val arvot = tulos.map(c => c.avain -> c.arvo).toMap
    Assertions.assertEquals(Some("7"),  arvot.get("PK_B1"))
    Assertions.assertEquals(Some("SV"), arvot.get("PK_B1_OPPIAINE"))
    Assertions.assertFalse(arvot.contains("PK_B12"), "B1-EN korotus ei luo uutta B1-numeroa")
  }

  @Test def testHakemuksenAidinkieliKielitieto(): Unit = {
    // suomi-aidinkielena → PK_AI_OPPIAINE="FI", PK_AI_KIELITIETO="AI1" (ei "FI")
    val tulos = hakemukseltaConvertTulos(Map(
      "arvosana-A_group0"  -> "arvosana-A-9",
      "oppimaara-a_group0" -> "suomi-aidinkielena"
    ))
    val avainArvoMap = tulos.map(c => c.avain -> c.arvo).toMap
    Assertions.assertEquals("FI",  avainArvoMap("PK_AI_OPPIAINE"),   "PK_AI_OPPIAINE should be FI")
    Assertions.assertEquals("AI1", avainArvoMap("PK_AI_KIELITIETO"), "PK_AI_KIELITIETO should be raw AI1")
  }

  @Test def peruskouluPaattotodistusVuosiHakemukselta(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2016"
    ))
    val leikkuri = LocalDate.now
    val converterResultWithRekisteriPeruskoulu = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq.empty, Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)

    Assertions.assertEquals(Some("2016"), converterResultWithRekisteriPeruskoulu.getAvainArvoMap().get(AvainArvoConstants.peruskouluPaattotodistusvuosiKey))
  }

  @Test def testAvainArvoConverterHakemuksenArvosanojaEiHuomioidaJosRekisteristaLoytyyPerusopetus(): Unit = {
    val opiskeluoikeusOid = "1.2.246.562.15.09876543210"
    val oppilaitosOid = "1.2.246.562.10.00000000234"
    val personOid = "1.2.246.562.98.69863082363"

    val oppiaineet = Seq(
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("historia"), None, None),
        Koodi("HI", "koodisto", None), Koodi("8", "koodisto", None),
        None, true, None, None),
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("kemia"), None, None),
        Koodi("KE", "koodisto", None), Koodi("9", "koodisto", None),
        None, true, None, None))
    val perusopetuksenOppimaaraValmis = PerusopetuksenOppimaara(
      UUID.randomUUID(),
      None,
      Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
      None,
      Koodi("toinenarvo", "koodisto", Some(1)),
      SuoritusTila.VALMIS,
      Koodi("FI", "kielikoodisto", Some(1)),
      Set.empty,
      yksilollistaminen = Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY),
      None,
      vahvistusPaivamaara = Some(LocalDate.parse("2025-05-30")),
      oppiaineet,
      List.empty,
      false,
      vuosiluokkiinSitoutumatonOpetus = false,
      luokkaAste = None)

    val lisatiedot = KoskiLisatiedot(None, Some(true), None)
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(perusopetuksenOppimaaraValmis), Some(lisatiedot), SuoritusTila.VALMIS, List.empty)

    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2016",
      "arvosana-BI_group0" -> "arvosana-BI-7",
      "arvosana-MA_group0" -> "arvosana-MA-7",
    ))

    val leikkuri = LocalDate.now
    val converterResultWithRekisteriPeruskoulu = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(opiskeluoikeus), Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    println(s"converterResultWithRekisteriPeruskoulu: $converterResultWithRekisteriPeruskoulu")
    //Tarkistetaan, että perusopetuksen opiskeluoikeudesta poimitut arvosanat ovat mukana, ja hakemuksen arvosanoja ei ole
    val avainArvoKE = converterResultWithRekisteriPeruskoulu.paatellytArvot.find(_.avain.equals("PK_KE")) //Supa
    val avainArvoHI = converterResultWithRekisteriPeruskoulu.paatellytArvot.find(_.avain.equals("PK_HI")) //Supa
    val avainArvoBI = converterResultWithRekisteriPeruskoulu.paatellytArvot.find(_.avain.equals("PK_BI")) //Hakemus, ei pitäisi löytyä
    val avainArvoMA = converterResultWithRekisteriPeruskoulu.paatellytArvot.find(_.avain.equals("PK_MA")) //Hakemus, ei pitäisi löytyä
    Assertions.assertEquals("9", avainArvoKE.get.arvo)
    Assertions.assertEquals("8", avainArvoHI.get.arvo)
    Assertions.assertEquals(AvainArvoConstants.arvosananLahdeSeliteSupa, avainArvoKE.get.selitteet.head)
    Assertions.assertEquals(AvainArvoConstants.arvosananLahdeSeliteSupa, avainArvoHI.get.selitteet.head)
    Assertions.assertEquals(None, avainArvoBI)
    Assertions.assertEquals(None, avainArvoMA)

    val converterResultWithoutRekisteriPeruskoulu = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq.empty, Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)

    //Tarkistetaan, että hakemuksella ilmoitetut arvosanat ovat mukana
    val avainArvoBIHakemus = converterResultWithoutRekisteriPeruskoulu.paatellytArvot.find(_.avain.equals("PK_BI"))
    val avainArvoMAHakemus = converterResultWithoutRekisteriPeruskoulu.paatellytArvot.find(_.avain.equals("PK_MA"))
    Assertions.assertEquals("7", avainArvoBIHakemus.get.arvo)
    Assertions.assertEquals("7", avainArvoMAHakemus.get.arvo)
    Assertions.assertEquals(AvainArvoConstants.arvosananLahdeSeliteHakemus, avainArvoBIHakemus.get.selitteet.head)
    Assertions.assertEquals(AvainArvoConstants.arvosananLahdeSeliteHakemus, avainArvoMAHakemus.get.selitteet.head)

  }

  @Test def testYksMatAi(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"


    val leikkuri = LocalDate.now

    //Case 1: Yksi hakemuksen hakutoiveista harkinnanvarainen syyllä SURE_YKS_MAT_AI
    val harkYksMatAi = BASE_HAKEMUS.hakutoiveet.map(ht => {
      HakutoiveenHarkinnanvaraisuus(ht.hakukohdeOid, ht.hakukohdeOid match {
        case "1.2.246.562.20.00000000000000000001" => HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN_HAKUKOHDE
        case "1.2.246.562.20.00000000000000000002" => HarkinnanvaraisuudenSyy.SURE_YKS_MAT_AI
      })
    })
    val hakemuksenHarkinnanvaraisuusYksMatAi = HakemuksenHarkinnanvaraisuus(hakemusOid = BASE_HAKEMUS.hakemusOid, henkiloOid = BASE_HAKEMUS.personOid, harkYksMatAi)
    val converterResultWithYksMatAi = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, Seq.empty, Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, Some(hakemuksenHarkinnanvaraisuusYksMatAi), Map.empty)
    Assertions.assertEquals("true", converterResultWithYksMatAi.getAvainArvoMap()(AvainArvoConstants.yksMatAiKey))

    //Case 2: Mikään hakemuksen hakutoiveista ei harkinnanvarainen syyllä SURE_YKS_MAT_AI tai ATARU_YKS_MAT_AI
    val harkWithoutYksMatAi = BASE_HAKEMUS.hakutoiveet.map(ht => {
      HakutoiveenHarkinnanvaraisuus(ht.hakukohdeOid, ht.hakukohdeOid match {
        case "1.2.246.562.20.00000000000000000001" => HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN_HAKUKOHDE
        case "1.2.246.562.20.00000000000000000002" => HarkinnanvaraisuudenSyy.ATARU_ULKOMAILLA_OPISKELTU
      })
    })
    val hakemuksenHarkinnanvaraisuusWithoutYksMatAi = HakemuksenHarkinnanvaraisuus(hakemusOid = BASE_HAKEMUS.hakemusOid, henkiloOid = BASE_HAKEMUS.personOid, harkWithoutYksMatAi)
    val converterResultWithoutYksMatAi = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, Seq.empty, Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU, Some(hakemuksenHarkinnanvaraisuusWithoutYksMatAi), Map.empty)
    Assertions.assertEquals("false", converterResultWithoutYksMatAi.getAvainArvoMap()(AvainArvoConstants.yksMatAiKey))

  }

  // Testataan, että ennen leikkuripäivämäärää vain ysiluokkalaisten (luokkaAste = Some(9)) perusopetuksen oppimäärä on kelpaava.
  // luokkaAste = Some(8) tai None eivät ole kelpaavia, vaikka suoritus olisi valmis.
  @Test def testAvainArvoConverterForToisenAsteenPohjakoulutusLuokkaAsteEnnenLeikkuria(): Unit = {
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> "6",
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020"
    ))

    val opiskeluoikeusOid = "1.2.246.562.15.09876543210"
    val oppilaitosOid = "1.2.246.562.10.00000000234"
    val oppiaineet = Seq(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("historia"), None, None), Koodi("HI", "koodisto", None), Koodi("8", "koodisto", None), None, true, None, None))

    val perusopetuksenOppimaaraBase = PerusopetuksenOppimaara(
      UUID.randomUUID(),
      None,
      Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
      None,
      Koodi("toinenarvo", "koodisto", Some(1)),
      SuoritusTila.VALMIS,
      Koodi("FI", "kielikoodisto", Some(1)),
      Set.empty,
      None,
      None,
      vahvistusPaivamaara = Some(LocalDate.parse("2025-05-30")),
      oppiaineet,
      List.empty,
      false,
      vuosiluokkiinSitoutumatonOpetus = false,
      luokkaAste = None)

    val leikkuriDeadlineEiOhitettu =
      java.time.Instant.ofEpochMilli(System.currentTimeMillis())
        .atZone(java.time.ZoneId.systemDefault())
        .plusDays(30)
        .toLocalDate

    val lisatiedot = KoskiLisatiedot(None, Some(true), None)

    // luokkaAste = Some(9), valmis, ennen deadlinea → kelpaava → POHJAKOULUTUS_PERUSKOULU
    val oppimaaraLuokka9 = perusopetuksenOppimaaraBase.copy(luokkaAste = Some(9))
    val opiskeluoikeusLuokka9 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(oppimaaraLuokka9), Some(lisatiedot), SuoritusTila.VALMIS, List.empty)
    val resultLuokka9 = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusLuokka9), Seq.empty, leikkuriDeadlineEiOhitettu, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), resultLuokka9.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))

    // luokkaAste = Some(8), valmis, ennen deadlinea → EI kelpaava → hakemukselta → POHJAKOULUTUS_EI_PAATTOTODISTUSTA
    val oppimaaraLuokka8 = perusopetuksenOppimaaraBase.copy(luokkaAste = Some(8))
    val opiskeluoikeusLuokka8 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(oppimaaraLuokka8), Some(lisatiedot), SuoritusTila.VALMIS, List.empty)
    val resultLuokka8 = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusLuokka8), Seq.empty, leikkuriDeadlineEiOhitettu, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA), resultLuokka8.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))

    // luokkaAste = None, valmis, ennen deadlinea → EI kelpaava → hakemukselta → POHJAKOULUTUS_EI_PAATTOTODISTUSTA
    val oppimaaraEiLuokkaa = perusopetuksenOppimaaraBase.copy(luokkaAste = None)
    val opiskeluoikeusEiLuokkaa = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(oppimaaraEiLuokkaa), Some(lisatiedot), SuoritusTila.VALMIS, List.empty)
    val resultEiLuokkaa = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusEiLuokkaa), Seq.empty, leikkuriDeadlineEiOhitettu, DEFAULT_KOUTA_HAKU, None, Map.empty)
    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), resultEiLuokkaa.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
  }

  private def buildEhdotTestOpiskeluoikeus(
                                            vahvistusPaivamaara: Option[LocalDate],
                                            arvosanat: Seq[(String, String, Boolean)],
                                            vuosiluokkiinSitoutumatonOpetus: Boolean = false,
                                            yksilollistaminen: Option[PerusopetuksenYksilollistaminen] = Some(PerusopetuksenYksilollistaminen.EI_YKSILOLLISTETTY),
                                            suoritusKieli: String = "FI",
                                            jaaLuokalle: Option[Boolean] = None
                                          ): PerusopetuksenOpiskeluoikeus = {
    val opiskeluoikeusOid = "1.2.246.562.15.09876543211"
    val oppilaitosOid = "1.2.246.562.10.00000000235"
    val aineet = arvosanat.map { case (koodi, arvosana, pakollinen) =>
      PerusopetuksenOppiaine(UUID.randomUUID(),
        Kielistetty(Some(koodi), None, None),
        Koodi(koodi, "koodisto", None),
        Koodi(arvosana, "koodisto", None),
        None, pakollinen, None, None)
    }.toSeq
    val oppimaara = PerusopetuksenOppimaara(
      UUID.randomUUID(), None,
      Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
      None,
      Koodi("toinenarvo", "koodisto", Some(1)),
      if (vahvistusPaivamaara.isDefined) SuoritusTila.VALMIS else SuoritusTila.KESKEN,
      Koodi(suoritusKieli, "kielikoodisto", Some(1)),
      Set.empty,
      yksilollistaminen = yksilollistaminen,
      None,
      vahvistusPaivamaara = vahvistusPaivamaara,
      aineet,
      List.empty,
      false,
      vuosiluokkiinSitoutumatonOpetus = vuosiluokkiinSitoutumatonOpetus,
      luokkaAste = None,
      jaaLuokalle = jaaLuokalle)
    val lisatiedot = KoskiLisatiedot(None, Some(true), None)
    PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid,
      Set(oppimaara), Some(lisatiedot),
      if (vahvistusPaivamaara.isDefined) SuoritusTila.VALMIS else SuoritusTila.KESKEN,
      List.empty)
  }

  //Ehdot-hakija: nykyinen oppimäärä vahvistamatta + leikkurihetkellä pakollisessa nelonen, ei VSOP + ikkuna auki
  //→ arvosanat nykyisistä opiskeluoikeuksista (kuten muillakin hakijoilla), meta-avaimet johdetaan leikkuripäivästä.
  @Test def testEhdotOverrideWhenEhdotPending(): Unit = {
    val personOid = "1.2.246.562.24.00000000111"
    val leikkuri = LocalDate.now().plusDays(7)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true), ("BI", "7", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true), ("BI", "7", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("true"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(Some(leikkuri.getYear.toString), map.get(AvainArvoConstants.peruskouluSuoritusvuosiKey))
    Assertions.assertEquals(Some("FI"), map.get(AvainArvoConstants.perusopetuksenKieliKey))
    Assertions.assertEquals(Some("4"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(Some("8"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
    Assertions.assertEquals(Some("7"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "BI"))
  }

  //Ehdot-override: ei-numeeriset arvosanat (esim. "S") eivät saa päätyä tulokseen.
  @Test def testEhdotOverrideNonNumericArvosanaExcluded(): Unit = {
    val personOid = "1.2.246.562.24.00000000119"
    val leikkuri = LocalDate.now().minusDays(1)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "8", true), ("AI", "8", true), ("KE", "S", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true), ("KE", "S", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("true"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    // Ei-numeerinen "S" ei saa päätyä tulokseen
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "KE"))
  }

  //Ehdot pending + ikkuna kiinni (today < leikkuri - 2 vko) → override ei laukea, ei arvosanoja.
  @Test def testEhdotNoOverrideWhenWindowClosed(): Unit = {
    val personOid = "1.2.246.562.24.00000000112"
    val leikkuri = LocalDate.now().plusDays(30)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("false"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
  }

  //Leikkurihetkellä ei nelosia pakollisissa aineissa → override ei laukea, vaikka ikkuna auki.
  @Test def testEhdotNoOverrideWhenNoFoursAtLeikkurihetki(): Unit = {
    val personOid = "1.2.246.562.24.00000000113"
    val leikkuri = LocalDate.now().plusDays(7)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "7", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "7", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("false"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
  }

  //Ehdot-hakija valinnaisen A2-kielen nelosen perusteella: nykyinen oppimäärä vahvistamatta + leikkurihetkellä A2:ssa nelonen
  //(pakolliset kunnossa), ei VSOP + ikkuna auki → ehdot-override laukeaa.
  @Test def testEhdotOverrideWhenOptionalA2HasFour(): Unit = {
    val personOid = "1.2.246.562.24.00000000119"
    val leikkuri = LocalDate.now().plusDays(7)
    val arvosanat = Seq(("MA", "7", true), ("AI", "8", true), ("A2", "4", false))
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(vahvistusPaivamaara = None, arvosanat = arvosanat))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(vahvistusPaivamaara = None, arvosanat = arvosanat))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("true"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(Some("7"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(Some("4"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "A2" + AvainArvoConstants.peruskouluAineValinnainenPostfix + "1"))
  }

  //Leikkurihetkellä nelosia mutta vuosiluokkiinSitoutumatonOpetus = true → override ei laukea.
  @Test def testEhdotNoOverrideWhenVsopAtLeikkurihetki(): Unit = {
    val personOid = "1.2.246.562.24.00000000114"
    val leikkuri = LocalDate.now().plusDays(7)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      vuosiluokkiinSitoutumatonOpetus = true
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      vuosiluokkiinSitoutumatonOpetus = true
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("false"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
  }

  //Leikkurihetkellä jaaLuokalle = true (luokalle jäänyt) → override ei laukea, vaikka muut ehdot täyttyisivät.
  @Test def testEhdotNoOverrideWhenJaaLuokalleAtLeikkurihetki(): Unit = {
    val personOid = "1.2.246.562.24.00000000120"
    val leikkuri = LocalDate.now().plusDays(7)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      jaaLuokalle = Some(true)
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      jaaLuokalle = Some(true)
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("false"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
  }

  //jaaLuokalle = Some(false) ei estä override-laukeamista (käyttäytyy kuten None).
  @Test def testEhdotOverrideWhenJaaLuokalleFalse(): Unit = {
    val personOid = "1.2.246.562.24.00000000121"
    val leikkuri = LocalDate.now().plusDays(7)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      jaaLuokalle = Some(false)
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      jaaLuokalle = Some(false)
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("true"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(Some("4"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(Some("8"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
  }

  //Nykyinen oppimäärä vahvistettu ajoissa (ei ehtoja ajantasaisesti) → normaali haara, ei ehdot-overridea.
  @Test def testEhdotOverrideBypassedWhenCurrentConfirmedOnTime(): Unit = {
    val personOid = "1.2.246.562.24.00000000115"
    val leikkuri = LocalDate.now().plusDays(7)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = Some(LocalDate.now().minusDays(1)),
      arvosanat = Seq(("MA", "9", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("true"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(Some("9"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(Some("8"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
  }

  //Ehdot leikkurihetkellä + nykyinen oppimäärä vahvistamatta + ikkuna auki →
  //pohjakoulutus päätellään leikkurihetken oppimäärältä dedikoidulla ehdot-selitteellä.
  @Test def testEhdotOverrideForPohjakoulutusWhenEhdotPending(): Unit = {
    val personOid = "1.2.246.562.24.00000000211"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), pohjakoulutus.map(_.arvo))
    Assertions.assertEquals(
      Some(Seq("Hakijalla oli ehdot leikkurihetkellä (pakollisessa aineessa nelonen, oppimäärä vahvistamatta), joten pohjakoulutus päätellään perusopetuksen oppimäärältä vaikka sitä ei ole vahvistettu ajoissa.")),
      pohjakoulutus.map(_.selitteet))
  }

  //Ehdot leikkurihetkellä mutta ikkuna kiinni → override ei laukea, normaali haara.
  @Test def testEhdotOverrideForPohjakoulutusNoOverrideWhenWindowClosed(): Unit = {
    val personOid = "1.2.246.562.24.00000000212"
    val leikkuri = LocalDate.now().plusDays(30)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.forall(!_.contains("ehdot"))),
      s"Pohjakoulutuksen selite sisältää 'ehdot' vaikka ei pitäisi: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Leikkurihetkellä ei nelosia pakollisissa aineissa → override ei laukea.
  @Test def testEhdotOverrideForPohjakoulutusNoOverrideWhenNoFoursAtLeikkurihetki(): Unit = {
    val personOid = "1.2.246.562.24.00000000213"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "7", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "7", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.forall(!_.contains("ehdot"))),
      s"Pohjakoulutuksen selite sisältää 'ehdot' vaikka ei pitäisi: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Leikkurihetkellä VSOP → override ei laukea vaikka nelosia olisi.
  @Test def testEhdotOverrideForPohjakoulutusNoOverrideWhenVsopAtLeikkurihetki(): Unit = {
    val personOid = "1.2.246.562.24.00000000214"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      vuosiluokkiinSitoutumatonOpetus = true
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      vuosiluokkiinSitoutumatonOpetus = true
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.forall(!_.contains("ehdot"))),
      s"Pohjakoulutuksen selite sisältää 'ehdot' vaikka ei pitäisi: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Nykyinen oppimäärä vahvistettu ajoissa (vahvistusPäivä <= deadline) → override ei laukea.
  @Test def testEhdotOverrideForPohjakoulutusBypassedWhenCurrentConfirmedOnTime(): Unit = {
    val personOid = "1.2.246.562.24.00000000215"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = Some(LocalDate.now().minusDays(1)),
      arvosanat = Seq(("MA", "9", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), pohjakoulutus.map(_.arvo))
    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.forall(!_.contains("ehdot"))),
      s"Pohjakoulutuksen selite sisältää 'ehdot' vaikka nykyinen vahvistettu ajoissa: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Nykyinen oppimäärä vahvistettu VASTA leikkurin jälkeen + ehdot leikkurihetkellä + ikkuna auki →
  //override laukeaa, koska "vahvistettu ajoissa" määritellään suhteessa deadlineen, ei wall-clock nyt-hetkeen.
  @Test def testEhdotOverrideForPohjakoulutusFiresWhenCurrentConfirmedAfterDeadline(): Unit = {
    val personOid = "1.2.246.562.24.00000000216"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = Some(leikkuri.plusDays(3)),
      arvosanat = Seq(("MA", "9", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), pohjakoulutus.map(_.arvo))
    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.exists(_.contains("ehdot"))),
      s"Pohjakoulutuksen selite ei sisällä 'ehdot' vaikka nykyinen vahvistettu vasta leikkurin jälkeen: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Ehdot-haarassa yksilöllistäminen välittyy NYKYISELTÄ oppimäärältä pohjakoulutuskoodiin (osittain → "2").
  //Leikkurihetken arvo (None) ei vaikuta, koska arvosanat ja oppimäärän muut tiedot luetaan nykyisestä snapshotista.
  @Test def testEhdotOverrideForPohjakoulutusYksilollistettyOsittain(): Unit = {
    val personOid = "1.2.246.562.24.00000000221"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      yksilollistaminen = Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY)
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      yksilollistaminen = Some(PerusopetuksenYksilollistaminen.EI_YKSILOLLISTETTY)
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU_OSITTAIN_YKSILOLLISTETTY), pohjakoulutus.map(_.arvo))
    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.exists(_.contains("ehdot"))),
      s"Pohjakoulutuksen selite ei sisällä 'ehdot': ${pohjakoulutus.map(_.selitteet)}")
  }

  //Ehdot-haarassa yksilöllistäminen välittyy NYKYISELTÄ oppimäärältä pohjakoulutuskoodiin (pääosin/kokonaan → "6").
  @Test def testEhdotOverrideForPohjakoulutusYksilollistettyKokonaan(): Unit = {
    val personOid = "1.2.246.562.24.00000000222"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      yksilollistaminen = Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY)
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      yksilollistaminen = Some(PerusopetuksenYksilollistaminen.EI_YKSILOLLISTETTY)
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU_PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY), pohjakoulutus.map(_.arvo))
    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.exists(_.contains("ehdot"))),
      s"Pohjakoulutuksen selite ei sisällä 'ehdot': ${pohjakoulutus.map(_.selitteet)}")
  }

  //Ehdot-override voittaa hakemuksella ilmoitetun ulkomaisen pohjakoulutuksen match-järjestyksessä.
  @Test def testEhdotOverrideVoittaaUlkomaisenHakemuksenPohjakoulutuksen(): Unit = {
    val personOid = "1.2.246.562.24.00000000231"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS,
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020"
    ))
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), pohjakoulutus.map(_.arvo))
    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.exists(_.contains("ehdot"))),
      s"Pohjakoulutuksen selite ei sisällä 'ehdot': ${pohjakoulutus.map(_.selitteet)}")
  }

  //Ehdot-override voittaa hakemuksella ilmoitetun 2017-pohjakoulutuksen match-järjestyksessä.
  @Test def testEhdotOverrideVoittaa2017HakemuksenPohjakoulutuksen(): Unit = {
    val personOid = "1.2.246.562.24.00000000232"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU_PAAOSIN_TAI_KOKONAAN_RAJOITETTU,
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017"
    ))
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), pohjakoulutus.map(_.arvo))
    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.exists(_.contains("ehdot"))),
      s"Pohjakoulutuksen selite ei sisällä 'ehdot': ${pohjakoulutus.map(_.selitteet)}")
  }

  //Leikkurihetkellä ei yhtään opiskeluoikeutta → override ei laukea vaikka muut ehdot täyttyisivät.
  @Test def testEhdotOverrideForPohjakoulutusNoOverrideWhenLeikkurihetkiEmpty(): Unit = {
    val personOid = "1.2.246.562.24.00000000241"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq.empty[Opiskeluoikeus]

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.forall(!_.contains("ehdot"))),
      s"Pohjakoulutuksen selite sisältää 'ehdot' vaikka leikkurihetki on tyhjä: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Leikkurihetkellä vain muu kuin PerusopetuksenOpiskeluoikeus → override ei laukea.
  @Test def testEhdotOverrideForPohjakoulutusNoOverrideWhenLeikkurihetkiHasNoPeruskoulu(): Unit = {
    val personOid = "1.2.246.562.24.00000000242"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))
    val muuOpiskeluoikeus = GeneerinenOpiskeluoikeus(
      UUID.randomUUID(), "1.2.246.562.15.00000000999",
      Koodi("lukiokoulutus", "koodisto", Some(1)),
      "1.2.246.562.10.00000000235",
      Set.empty, None, List.empty
    )
    val leikkurihetkella: Seq[Opiskeluoikeus] = Seq(muuOpiskeluoikeus)

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.forall(!_.contains("ehdot"))),
      s"Pohjakoulutuksen selite sisältää 'ehdot' vaikka leikkurihetkellä ei ole peruskoulua: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Leikkurihetkellä nelonen vain valinnaisessa aineessa (pakolliset OK) → override ei laukea.
  @Test def testEhdotOverrideForPohjakoulutusNoOverrideWhenOnlyValinnainenHasFour(): Unit = {
    val personOid = "1.2.246.562.24.00000000243"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "7", true), ("AI", "8", true), ("LI", "4", false))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "7", true), ("AI", "8", true), ("LI", "4", false))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.forall(!_.contains("ehdot"))),
      s"Pohjakoulutuksen selite sisältää 'ehdot' vaikka vain valinnaisessa on nelonen: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Nykyinen vahvistettu päivälleen deadlinena (boundary: vahvistusPäivä == deadline → ajoissa) → override ei laukea.
  @Test def testEhdotOverrideForPohjakoulutusBypassedWhenCurrentConfirmedExactlyOnDeadline(): Unit = {
    val personOid = "1.2.246.562.24.00000000251"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = Some(leikkuri),
      arvosanat = Seq(("MA", "9", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), pohjakoulutus.map(_.arvo))
    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.forall(!_.contains("ehdot"))),
      s"Pohjakoulutuksen selite sisältää 'ehdot' vaikka nykyinen vahvistettu tasan deadlinena: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Ikkunan boundary: today == deadline - 14 vrk → ikkuna juuri-juuri auki, override laukeaa.
  @Test def testEhdotOverrideForPohjakoulutusFiresOnWindowBoundary(): Unit = {
    val personOid = "1.2.246.562.24.00000000261"
    val leikkuri = LocalDate.now().plusWeeks(2)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), pohjakoulutus.map(_.arvo))
    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.exists(_.contains("ehdot"))),
      s"Pohjakoulutuksen selite ei sisällä 'ehdot' vaikka today == deadline - 14 vrk: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Deadline menneisyydessä → ikkuna pysyy auki (now on aina deadline - 14 vrk jälkeen), override laukeaa.
  @Test def testEhdotOverrideForPohjakoulutusFiresWhenDeadlineInPast(): Unit = {
    val personOid = "1.2.246.562.24.00000000262"
    val leikkuri = LocalDate.now().minusDays(5)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_PERUSKOULU), pohjakoulutus.map(_.arvo))
    Assertions.assertTrue(pohjakoulutus.exists(_.selitteet.exists(_.contains("ehdot"))),
      s"Pohjakoulutuksen selite ei sisällä 'ehdot' vaikka deadline on menneisyydessä: ${pohjakoulutus.map(_.selitteet)}")
  }

  //Nykyinen oppimäärä vahvistettu VASTA leikkurin jälkeen + ehdot leikkurihetkellä + ikkuna auki →
  //peruskoulu-avaimet PK_TILA = true ja meta-avaimet leikkuripäivältä, mutta arvosanat tulevat NYKYISESTÄ snapshotista
  //(korotukset huomioidaan). Symmetriatesti toisenAsteenPohjakoulutus-haaran testille
  //testEhdotOverrideForPohjakoulutusFiresWhenCurrentConfirmedAfterDeadline.
  @Test def testEhdotOverrideForPeruskouluFiresWhenCurrentConfirmedAfterDeadline(): Unit = {
    val personOid = "1.2.246.562.24.00000000281"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = Some(leikkuri.plusDays(3)),
      arvosanat = Seq(("MA", "9", true), ("AI", "9", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("true"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(Some(leikkuri.getYear.toString), map.get(AvainArvoConstants.peruskouluSuoritusvuosiKey))
    //Arvosanat tulevat nykyisestä snapshotista, joten myöhässä vahvistetut korotukset huomioidaan.
    Assertions.assertEquals(Some("9"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(Some("9"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
  }

  //Ehdot-haarassa perusopetuksen_kieli luetaan nykyisestä oppimäärältä (samoin kuin arvosanat).
  //Tämä pitää haarat keskenään johdonmukaisina: kaikki arvot (kieli mukaan lukien) tulevat samasta lähteestä.
  @Test def testEhdotOverrideForPeruskouluSuoritusKieliFromCurrent(): Unit = {
    val personOid = "1.2.246.562.24.00000000282"
    val leikkuri = LocalDate.now().plusDays(7)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      suoritusKieli = "SV"
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true)),
      suoritusKieli = "FI"
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("SV"), map.get(AvainArvoConstants.perusopetuksenKieliKey))
  }

  //Kesken + pakollinen nelonen + ei-VSOP + deadline ohitettu ilman ehdot-overridea → EI_PAATTOTODISTUSTA.
  //Lukitsee commitin 14be04b5 käyttäytymismuutoksen: onKelpaavaOppimaara ei enää hyväksy kesken-suoritusta
  //pelkän pakollisen nelosen perusteella deadline-ohitettu-haarassa.
  @Test def testOnKelpaavaOppimaaraEiEnaaHyvaksyKeskenNelonenIlmanEhtoOverridea(): Unit = {
    val personOid = "1.2.246.562.24.00000000271"
    val leikkuri = LocalDate.now().minusDays(1)
    val hakemus = BASE_HAKEMUS.copy(keyValues = Map.empty)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq.empty[Opiskeluoikeus]

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val pohjakoulutus = result.paatellytArvot.find(_.avain == AvainArvoConstants.pohjakoulutusToinenAste)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA), pohjakoulutus.map(_.arvo))
  }

  //Ehdot leikkurihetkellä + nykyinen oppimäärä KOROTETTU ja vahvistettu vasta leikkurin jälkeen →
  //arvosanat tulevat nykyisestä snapshotista, suoritusvuosi pysyy leikkuripäivän vuotena.
  @Test def testEhdotOverrideUsesCurrentGradesWhenImproved(): Unit = {
    val personOid = "1.2.246.562.24.00000000291"
    val leikkuri = LocalDate.now().plusDays(7)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = Some(leikkuri.plusDays(3)),
      arvosanat = Seq(("MA", "7", true), ("AI", "8", true), ("BI", "7", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true), ("BI", "7", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("true"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(Some(leikkuri.getYear.toString), map.get(AvainArvoConstants.peruskouluSuoritusvuosiKey))
    Assertions.assertEquals(Some("7"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(Some("8"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
  }

  //Ehdot leikkurihetkellä + nykyinen oppimäärä korotettu mutta vielä vahvistamatta →
  //arvosanat silti nykyisestä snapshotista, override aktiivinen vahvistuspäivän puutteesta huolimatta.
  @Test def testEhdotOverrideUsesCurrentGradesWhenImprovedButStillUnconfirmed(): Unit = {
    val personOid = "1.2.246.562.24.00000000292"
    val leikkuri = LocalDate.now().plusDays(7)
    val nykyiset = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "7", true), ("AI", "8", true))
    ))
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("true"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(Some("7"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(Some("8"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
  }

  //Ehdot leikkurihetkellä + nykyiseen snapshotiin lisätty oppiaineen oppimäärä (korotus), jota ei ollut leikkurihetkellä →
  //korotus huomioidaan ehdot-haarassa, koska kaikki arvosanat luetaan nykyisestä snapshotista.
  @Test def testEhdotOverrideUsesCurrentSnapshotKorotukset(): Unit = {
    val personOid = "1.2.246.562.24.00000000293"
    val leikkuri = LocalDate.now().plusDays(7)
    val perusopetus = buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("BI", "7", true))
    )
    val korotusBiologia = PerusopetuksenOppiaine(UUID.randomUUID(),
      Kielistetty(Some("biologia"), None, None),
      Koodi("BI", "koodisto", None),
      Koodi("9", "koodisto", None),
      None, true, None, None)
    val korotusSuoritus = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None,
      Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
      Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN,
      Koodi("arvo", "koodisto", Some(1)),
      Some(leikkuri.plusDays(2)), Some(leikkuri.plusDays(2)),
      Set(korotusBiologia), false)
    val korotusOpiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(),
      Some("1.2.246.562.15.09876543212"), "1.2.246.562.10.00000000235",
      Set(korotusSuoritus), None, SuoritusTila.VALMIS, List.empty)

    val nykyiset: Seq[Opiskeluoikeus] = Seq(perusopetus, korotusOpiskeluoikeus)
    val leikkurihetkella: Seq[Opiskeluoikeus] = Seq(perusopetus)

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("true"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(Some("4"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    //Korotus näkyy ainoastaan jos korotuksen sisältävä oppiaineen oppimäärä luetaan nykyisestä snapshotista.
    Assertions.assertEquals(Some("9"), map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "BI"))
  }

  //Ehdot leikkurihetkellä + nykyisestä snapshotista perusopetuksen oppimäärä on hävinnyt (esim. data-virhe) →
  //ehdot-haara ei laukea (vaatii Some(po) nykyisestä), peruskouluSuoritettuKey on false eikä arvosanoja muodosteta.
  @Test def testEhdotOverrideNoCurrentOppimaaraReturnsFalse(): Unit = {
    val personOid = "1.2.246.562.24.00000000294"
    val leikkuri = LocalDate.now().plusDays(7)
    val nykyiset: Seq[Opiskeluoikeus] = Seq.empty
    val leikkurihetkella = Seq(buildEhdotTestOpiskeluoikeus(
      vahvistusPaivamaara = None,
      arvosanat = Seq(("MA", "4", true), ("AI", "8", true))
    ))

    val result = AvainArvoConverter.convertOpiskeluoikeudet(personOid, None, nykyiset, leikkurihetkella, leikkuri, DEFAULT_KOUTA_HAKU, None, Map.empty)
    val map = result.getAvainArvoMap()

    Assertions.assertEquals(Some("false"), map.get(AvainArvoConstants.peruskouluSuoritettuKey))
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "MA"))
    Assertions.assertEquals(None, map.get(AvainArvoConstants.peruskouluAineenArvosanaPrefix + "AI"))
  }

  private def getToisenAsteenPeruskoulutusOpiskeluoikeus(vahvistusPaiva: LocalDate = LocalDate.parse("2025-05-30")) = {
    val opiskeluoikeusOid = "1.2.246.562.15.09876543210"
    val oppilaitosOid = "1.2.246.562.10.00000000234"
    val personOid = "1.2.246.562.98.69863082363"

    val oppiaineet = Seq(
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("historia"), None, None),
        Koodi("HI", "koodisto", None), Koodi("8", "koodisto", None),
        None, true, None, None),
      PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia"), None, None),
        Koodi("BI", "koodisto", None), Koodi("9", "koodisto", None),
        None, true, None, None))
    val perusopetuksenOppimaaraValmis = PerusopetuksenOppimaara(
      UUID.randomUUID(),
      None,
      Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
      None,
      Koodi("toinenarvo", "koodisto", Some(1)),
      SuoritusTila.VALMIS,
      Koodi("FI", "kielikoodisto", Some(1)),
      Set.empty,
      yksilollistaminen = Some(PerusopetuksenYksilollistaminen.EI_YKSILOLLISTETTY),
      None,
      vahvistusPaivamaara = Some(vahvistusPaiva),
      oppiaineet,
      List.empty,
      false,
      vuosiluokkiinSitoutumatonOpetus = false,
      luokkaAste = None)

    val lisatiedot = KoskiLisatiedot(None, Some(true), None)
    PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(perusopetuksenOppimaaraValmis), Some(lisatiedot), SuoritusTila.VALMIS, List.empty)
  }

  @Test def testTuvaYhteislaajuusEiRiita(): Unit = {
    val opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq(
      getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2018-06-01")),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestTuva(laajuusArvo = Some(8), suoritusVuosi = 2021))),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestTuva(laajuusArvo = Some(5), suoritusVuosi = 2022))),
    )

    // Yhteislaajuus 5 + 8 = 13 < 19
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(BASE_HAKEMUS.personOid, Some(BASE_HAKEMUS), opiskeluoikeudet, Seq.empty, DEFAULT_LEIKKURIPVM, DEFAULT_KOUTA_HAKU.copy(hakuvuosi = Some(2022)), None, Map.empty)
    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritettuKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritusvuosiKey))
  }

  @Test def testTuvaYhteislaajuusTuoreinLiianVanha(): Unit = {
    val opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq(
      getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2018-06-01")),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestTuva(laajuusArvo = Some(10), suoritusVuosi = 2020))),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestTuva(laajuusArvo = Some(15), suoritusVuosi = 2022))),
    )

    // Yhteislaajuus 15 + 10 = 25 >= 19, mutta tuorein vuosi 2022 < vuosiVahintaan 2023
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(BASE_HAKEMUS.personOid, Some(BASE_HAKEMUS), opiskeluoikeudet, Seq.empty, DEFAULT_LEIKKURIPVM, DEFAULT_KOUTA_HAKU.copy(hakuvuosi = Some(2024)), None, Map.empty)
    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritettuKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritusvuosiKey))
  }

  @Test def testTuvaKynnyksenYlittamisvuosi(): Unit = {
    val opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq(
      getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2018-06-01")),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestTuva(laajuusArvo = Some(12), suoritusVuosi = 2019))),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestTuva(laajuusArvo = Some(8), suoritusVuosi = 2021))),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestTuva(laajuusArvo = Some(5), suoritusVuosi = 2022))),
    )

    // Yhteislaajuus 12 + 8 + 5 = 25 >= 19. Kynnys ylittyy vuonna 2021 (12 + 8 = 20 >= 19).
    // Tuorein vuosi 2022 >= 2021 (vuosiVahintaan). Suoritusvuosi = 2021 (kynnyksen ylittämisvuosi, ei tuorein).
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(BASE_HAKEMUS.personOid, Some(BASE_HAKEMUS), opiskeluoikeudet, Seq.empty, DEFAULT_LEIKKURIPVM, DEFAULT_KOUTA_HAKU.copy(hakuvuosi = Some(2022)), None, Map.empty)
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritettuKey))
    Assertions.assertEquals(Some("2021"), converterResult.getAvainArvoMap().get(AvainArvoConstants.tuvaSuoritusvuosiKey))
  }

  @Test def testTelmaYhteislaajuusRiittava(): Unit = {
    val opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq(
      getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2018-06-01")),
      TestDataUtil.getTestAmmatillinenOpiskeluoikeus(Set(TestDataUtil.getTestTelma(laajuusArvo = Some(13), suoritusVuosi = 2022))),
      TestDataUtil.getTestAmmatillinenOpiskeluoikeus(Set(TestDataUtil.getTestTelma(laajuusArvo = Some(14), suoritusVuosi = 2021))),
    )

    // Yhteislaajuus 13 + 14 = 27 >= 25
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(BASE_HAKEMUS.personOid, Some(BASE_HAKEMUS), opiskeluoikeudet, Seq.empty, DEFAULT_LEIKKURIPVM, DEFAULT_KOUTA_HAKU.copy(hakuvuosi = Some(2022)), None, Map.empty)
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritettuKey))
    Assertions.assertEquals(Some("2022"), converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritusvuosiKey))
  }

  @Test def testVSTYhteislaajuusEiRiita(): Unit = {
    val opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq(
      getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2018-06-01")),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestVapaaSivistystyo(laajuusArvo = Some(10), suoritusVuosi = 2021))),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestVapaaSivistystyo(laajuusArvo = Some(8), suoritusVuosi = 2022))),
    )

    // Yhteislaajuus 8 + 10 = 18 < 26.5
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(BASE_HAKEMUS.personOid, Some(BASE_HAKEMUS), opiskeluoikeudet, Seq.empty, DEFAULT_LEIKKURIPVM, DEFAULT_KOUTA_HAKU.copy(hakuvuosi = Some(2022)), None, Map.empty)
    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritettuKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritusvuosiKey))
  }

  @Test def testVSTYhteislaajuusTuoreinLiianVanha(): Unit = {
    // Perusopetus ja kolme perättäistä VST:tä
    val opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq(
      getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2018-06-01")),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestVapaaSivistystyo(laajuusArvo = Some(14), suoritusVuosi = 2019))),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestVapaaSivistystyo(laajuusArvo = Some(14), suoritusVuosi = 2021)))
    )

    // Yhteislaajuus 14 + 14 = 28 >= 26.5, mutta kynnyksenYlittamisvuosi 2021 < vuosiVahintaan 2022
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(BASE_HAKEMUS.personOid, Some(BASE_HAKEMUS), opiskeluoikeudet, Seq.empty, DEFAULT_LEIKKURIPVM, DEFAULT_KOUTA_HAKU.copy(hakuvuosi = Some(2023)), None, Map.empty)
    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritettuKey))
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritusvuosiKey))
  }

  @Test def testVSTKynnyksenYlittamisvuosi(): Unit = {
    // Perusopetus ja kolme perättäistä VST:tä
    val opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq(
      getToisenAsteenPeruskoulutusOpiskeluoikeus(LocalDate.parse("2018-06-01")),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestVapaaSivistystyo(laajuusArvo = Some(14), suoritusVuosi = 2019))),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestVapaaSivistystyo(laajuusArvo = Some(14), suoritusVuosi = 2021))),
      TestDataUtil.getTestGeneerinenOpiskeluoikeus(Set(TestDataUtil.getTestVapaaSivistystyo(laajuusArvo = Some(5), suoritusVuosi = 2022))),
    )

    // Yhteislaajuus 14 + 14 + 5 = 33 >= 26.5. Kynnys ylittyy vuonna 2021 (14 + 14 = 28 >= 26.5).
    // Suoritusvuosi = 2021 (kynnyksen ylittämisvuosi, ei tuorein).
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(BASE_HAKEMUS.personOid, Some(BASE_HAKEMUS), opiskeluoikeudet, Seq.empty, DEFAULT_LEIKKURIPVM, DEFAULT_KOUTA_HAKU.copy(hakuvuosi = Some(2022)), None, Map.empty)
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritettuKey))
    Assertions.assertEquals(Some("2021"), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritusvuosiKey))
  }
}
