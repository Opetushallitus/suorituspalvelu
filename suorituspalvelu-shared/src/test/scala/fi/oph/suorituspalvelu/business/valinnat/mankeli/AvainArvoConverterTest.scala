package fi.oph.suorituspalvelu.business.valinnat.mankeli

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{TELMA, VAPAA_SIVISTYSTYO}
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, Hakutoive, Koodisto, KoutaHaku}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, Lahtokoulu, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, PerusopetuksenYksilollistaminen, SuoritusTila, Telma, VapaaSivistystyo}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoContainer, AvainArvoConverter, HakemusConverter}
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
    "B1" -> DEFAULT_OPPIAINEKOODI, "AOM" -> DEFAULT_OPPIAINEKOODI, "LI" -> DEFAULT_OPPIAINEKOODI,
    "YH" -> DEFAULT_OPPIAINEKOODI, "KU" -> DEFAULT_OPPIAINEKOODI, "GE" -> DEFAULT_OPPIAINEKOODI,
    "TH" -> DEFAULT_OPPIAINEKOODI, "MA" -> DEFAULT_OPPIAINEKOODI, "B2" -> DEFAULT_OPPIAINEKOODI,
    "TE" -> DEFAULT_OPPIAINEKOODI, "KT" -> DEFAULT_OPPIAINEKOODI, "FY" -> DEFAULT_OPPIAINEKOODI,
    "AI" -> DEFAULT_OPPIAINEKOODI, "MU" -> DEFAULT_OPPIAINEKOODI, "A1" -> DEFAULT_OPPIAINEKOODI,
    "KE" -> DEFAULT_OPPIAINEKOODI)

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
    keyValues = Map.empty
  )

  @Test def testAvainArvoConverterForPeruskouluKeys(): Unit = {
    val fileName = "/1_2_246_562_98_69863082363.json"
    val splitData = KoskiIntegration.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName)).toList
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oos: Seq[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet, DUMMY_KOODISTOPROVIDER)

      Assertions.assertEquals(1, oos.size)

      val leikkuri = LocalDate.now
      val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", oos, leikkuri, DEFAULT_KOUTA_HAKU)

      Assertions.assertEquals(Some("FI"), converterResult.getAvainArvoMap().get(AvainArvoConstants.perusopetuksenKieliKey))
      Assertions.assertEquals(Some("2025"), converterResult.getAvainArvoMap().get(AvainArvoConstants.peruskouluSuoritusvuosiKey))
      Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.peruskouluSuoritettuKey))

    })
  }

  @Test def testAvainArvoConverterForPeruskouluArvosanatJaKielet(): Unit = {
    val fileName = "/1_2_246_562_98_69863082363.json"
    val splitData = KoskiIntegration.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName)).toList
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oos: Seq[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet, DUMMY_KOODISTOPROVIDER)

      Assertions.assertEquals(1, oos.size)
      val leikkuri = LocalDate.now
      val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", oos, leikkuri, DEFAULT_KOUTA_HAKU)
      val tavoiteArvosanat = Map("HI" -> "8", "BI" -> "9", "B1" -> "8", "AOM" -> "8", "LI" -> "9",
        "YH" -> "10", "KU" -> "8", "GE" -> "9", "MA" -> "9", "B2" -> "9", "TE" -> "8",
        "KT" -> "10", "FY" -> "9", "AI" -> "9", "MU" -> "7", "A1" -> "8", "KE" -> "7")
      val tavoiteKielet = Map("B1" -> "SV", "A1" -> "EN", "B2" -> "DE")

      tavoiteArvosanat.foreach { case (aine, arvosana) =>
        val prefix = AvainArvoConstants.peruskouluAineenArvosanaPrefix
        Assertions.assertEquals(Some(arvosana), converterResult.getAvainArvoMap().get(prefix + aine))
      }

      tavoiteKielet.foreach { case (aine, kieli) =>
        val postfix = AvainArvoConstants.peruskouluAineenKieliPostfix
        val prefix = AvainArvoConstants.peruskouluAineenArvosanaPrefix
        val kieliAvain = prefix + aine + postfix

        Assertions.assertEquals(Some(kieli), converterResult.getAvainArvoMap().get(kieliAvain))
      }
    })
  }

  @Test def testKorkeimmatArvosanat(): Unit = {
    val aineet = Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti, hankala savotta"), None, None), Koodi("A1", "koodisto", None), Koodi("8", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("A1", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia, suoritus"), None, None), Koodi("BI", "koodisto", None), Koodi("S", "koodisto", None), None, true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia"), None, None), Koodi("BI", "koodisto", None), Koodi("8", "koodisto", None), None, true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("kotitalous"), None, None), Koodi("KO", "koodisto", None), Koodi("S", "koodisto", None), None, true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("kotitalous, osallistuminen"), None, None), Koodi("BI", "koodisto", None), Koodi("O", "koodisto", None), None, true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("liikunta"), None, None), Koodi("LI", "koodisto", None), Koodi("9", "koodisto", None), None, true, None, None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("liikunta, toinen"), None, None), Koodi("LI", "koodisto", None), Koodi("7", "koodisto", None), None, true, None, None))
    val oppimaara = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, Some(LocalDate.parse("2025-05-30")), Some(LocalDate.parse("2025-05-30")), aineet, Set.empty, false, false)

    val korotus1Biologia = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia"), None, None), Koodi("BI", "koodisto", None), Koodi("9", "koodisto", None), None, true, None, None)
    val korotus2Liikunta = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("liikunta"), None, None), Koodi("LI", "koodisto", None), Koodi("10", "koodisto", None), None, true, None, None)

    PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, Some(LocalDate.parse("2025-06-06")), Some(LocalDate.parse("2025-06-06")), aineet, Set.empty, false, false)

    val oppiaineenOppimaara1 = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Some(LocalDate.parse("2025-06-07")), Some(LocalDate.parse("2025-06-07")), Set(korotus1Biologia), false)
    val oppiaineenOppimaara2 = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Some(LocalDate.parse("2025-06-08")), Some(LocalDate.parse("2025-06-08")), Set(korotus2Liikunta), false)

    val oppiaineet = oppimaara.aineet ++ oppiaineenOppimaara1.oppiaineet ++ oppiaineenOppimaara2.oppiaineet
    val avainArvot = AvainArvoConverter.perusopetuksenOppiaineetToAvainArvot(oppiaineet)
    val ka: Set[AvainArvoContainer] = AvainArvoConverter.valitseKorkeimmatPerusopetuksenArvosanatAineittain(avainArvot)
    val korkeimmatArvosanat = ka.map(aa => (aa.avain, aa.arvo)).toMap

    val tavoiteArvosanat = Map("A1" -> "10", "BI" -> "9", "KO" -> "S", "LI" -> "10")
    tavoiteArvosanat.foreach { case (aine, arvosana) =>
      val prefix = AvainArvoConstants.peruskouluAineenArvosanaPrefix
      Assertions.assertEquals(Some(arvosana), korkeimmatArvosanat.get(prefix + aine))
    }
  }

  @Test def testYoArvoEnnenLeikkuripaivaa(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"
    val tutkinto =
      """
        |{
        |  "ssn": "000000-000A",
        |  "graduationPeriod": "2013K",
        |  "graduationDate": "2021-06-01",
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

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuri, DEFAULT_KOUTA_HAKU)

    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.yoSuoritettuKey))
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

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuri, DEFAULT_KOUTA_HAKU)

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.yoSuoritettuKey))

  }

  @Test def testAmmArvoMyohassaValmistunut(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val leikkuripaivanJalkeenValmistunutTutkinto = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi("123456", "koulutus", Some(1)), Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"),
      Koodi("valmistunut", "jokutila", Some(1)), SuoritusTila.VALMIS, Some(LocalDate.parse("2021-01-01")), Some(LocalDate.parse("2024-04-03")), None, Koodi("tapa", "suoritustapa", Some(1)), Koodi("kieli", "suorituskieli", Some(1)), Set.empty)
    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(leikkuripaivanJalkeenValmistunutTutkinto), None, List.empty))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, DEFAULT_KOUTA_HAKU)

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.ammSuoritettuKey))
  }

  @Test def testAmmArvoAjoissaValmistunut(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val ajoissaValmistunut = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi("123456", "koulutus", Some(1)), Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"),
      Koodi("valmistunut", "jokutila", Some(1)), SuoritusTila.VALMIS, Some(LocalDate.parse("2021-01-01")), Some(LocalDate.parse("2023-04-03")), None, Koodi("tapa", "suoritustapa", Some(1)), Koodi("kieli", "suorituskieli", Some(1)), Set.empty)
    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(ajoissaValmistunut), None, List.empty))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, DEFAULT_KOUTA_HAKU)

    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.ammSuoritettuKey))
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
      Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), None, Some(VALMIS), None, TELMA)
    )

    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Oppilaitos(Kielistetty(None, None, None), ""),
      Set(telmaTutkinto),
      None,
      List.empty
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, DEFAULT_KOUTA_HAKU)

    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritettuKey))
    Assertions.assertEquals(Some(suoritusVuosi.toString), converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritusvuosiKey))

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
      Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), None, Some(VALMIS), None, TELMA)
    )

    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Oppilaitos(Kielistetty(None, None, None), ""),
      Set(telmaTutkinto),
      None,
      List.empty
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, DEFAULT_KOUTA_HAKU)

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
      Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), None, Some(VALMIS), None, TELMA)
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

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, haku)

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritettuKey))

    //Jos Telmaa ei ole suoritettu hakuvuonna tai sitä edeltävänä vuonna, suoritusvuodelle ei saa tulla avain-arvoa.
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.telmaSuoritusvuosiKey))
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
      Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), None, Some(SuoritusTila.VALMIS), None, VAPAA_SIVISTYSTYO)
    )

    val oikeudet = Seq(GeneerinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Koodi("", "", None),
      "oppilaitosOid",
      Set(opistovuosiKoulutus),
      None,
      List.empty
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, DEFAULT_KOUTA_HAKU)
    Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritettuKey))

    Assertions.assertEquals(Some(suoritusVuosi.toString), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritusvuosiKey))

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
      Lahtokoulu(LocalDate.parse("2021-01-01"), Some(LocalDate.parse("2022-05-15")), "1.2.3.4", Some(2022), None, Some(SuoritusTila.VALMIS), None, VAPAA_SIVISTYSTYO)
    )

    val oikeudet = Seq(GeneerinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Koodi("", "", None),
      "oppilaitosOid",
      Set(opistovuosiKoulutus),
      None,
      List.empty
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, DEFAULT_KOUTA_HAKU)

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritettuKey))

    //Jos Opistovuotta ei ole suoritettu riittävässä laajuudessa, suoritusvuodelle ei saa tulla avain-arvoa.
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritusvuosiKey))
  }

  @Test def testAvainArvoConverterForToisenAsteenPohjakoulutusLoytyySupasta(): Unit = {
    val opiskeluoikeusOid = "1.2.246.562.15.09876543210"
    val oppilaitosOid = "1.2.246.562.10.00000000234"
    val personOid = "1.2.246.562.98.69863082363"

    val oppiaineet = Set(
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
      Set.empty,
      false,
      vuosiluokkiinSitoutumatonOpetus = false)

    val lisatiedot = KoskiLisatiedot(None, Some(true), None)
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(perusopetuksenOppimaaraValmis), Some(lisatiedot), SuoritusTila.VALMIS, List.empty)

    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusKey -> "0",
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2020"
    ))

    val leikkuri = LocalDate.now
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(opiskeluoikeus), leikkuri, DEFAULT_KOUTA_HAKU)

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
    val oppiaineetArvosanoissaNelosia = Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("arvo", "koodisto", None), Koodi("4", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None))
    val oppiaineetArvosanoissaEiNelosia = Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("arvo", "koodisto", None), Koodi("8", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None))
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
      Set.empty,
      false,
      vuosiluokkiinSitoutumatonOpetus = false)

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

    val converterResultVSOPNelosia = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusVSOPNelosia), leikkuriDeadlineOhitettu, DEFAULT_KOUTA_HAKU)
    val converterResult2VSOPEiNelosia = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusVSOPEiNelosia), leikkuriDeadlineOhitettu, DEFAULT_KOUTA_HAKU)
    val converterResult3Nelosia = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusNelosia), leikkuriDeadlineOhitettu, DEFAULT_KOUTA_HAKU)
    val converterResult4EiNelosia = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq(opiskeluoikeusEiNelosia), leikkuriDeadlineOhitettu, DEFAULT_KOUTA_HAKU)
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
    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU)

    Assertions.assertEquals(Some(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS), converterResult.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
  }

  @Test def testAvainArvoConverterForToisenAsteenPohjakoulutusUskotaanHakemukseen2017TaiEnnen(): Unit = {
    Seq("1", "2", "3", "6", "8", "9").foreach(hakemuksenPohjakoulutus => {
      val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusKey -> hakemuksenPohjakoulutus,
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017"
      ))
      val leikkuri = LocalDate.now
      val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", Some(hakemus), Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU)

      Assertions.assertEquals(Some(hakemuksenPohjakoulutus), converterResult.getAvainArvoMap().get(AvainArvoConstants.pohjakoulutusToinenAste))
    })
  }

  @Test def testArvosanatHakemukseltaJaYksiKorotusSupasta(): Unit = {
    val keyValues = Map(
      "arvosana-KO_group0" -> "arvosana-KO-7",
      "oppiaine-valinnainen-kieli_group0" -> "oppiaine-valinnainen-kieli-b2",
      "arvosana-valinnainen-kieli_group0" -> "arvosana-valinnainen-kieli-6",
      "arvosana-FY_group0" -> "arvosana-FY-6",
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
      "pohjakoulutus_vuosi" -> "2016",
      "oppimaara-kieli-B1_group0" -> "SV",
      "arvosana-KO_group1" -> "arvosana-KO-hyvaksytty",
      "arvosana-BI_group0" -> "arvosana-BI-7",
      "arvosana-B1_group0" -> "arvosana-B1-6",
      "arvosana-MA_group0" -> "arvosana-MA-7",
      "arvosana-KA_group1" -> "arvosana-KA-hyvaksytty",
      "oppimaara-kieli-valinnainen-kieli_group1" -> ""
    )
    val hakemus = BASE_HAKEMUS.copy(keyValues = keyValues)

    //Yhdelle hakemuksen arvosanoista löytyy korotus, muille ei
    val korotus1Kemia = PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("kemia"), None, None), Koodi("KE", "koodisto", None), Koodi("9", "koodisto", None), None, true, None, None)

    val arvosanatHakemukselta = HakemusConverter.convertArvosanatHakemukselta(hakemus)
    val arvosanatSupasta = AvainArvoConverter.perusopetuksenOppiaineetToAvainArvot(Set(korotus1Kemia))

    val korkeimmat = AvainArvoConverter.valitseKorkeimmatPerusopetuksenArvosanatAineittain(arvosanatHakemukselta ++ arvosanatSupasta)

    //Tarkistetaan, että arvosanat vastaavat oletettuja
    val resultMap = korkeimmat.map(aa => aa.avain -> aa.arvo).toMap

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
      "PK_FY" -> "6",
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

  }

  @Test def testAvainArvoConverterHakemuksenArvosanojaEiHuomioidaJosRekisteristaLoytyyPerusopetus(): Unit = {
    val opiskeluoikeusOid = "1.2.246.562.15.09876543210"
    val oppilaitosOid = "1.2.246.562.10.00000000234"
    val personOid = "1.2.246.562.98.69863082363"

    val oppiaineet = Set(
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
      Set.empty,
      false,
      vuosiluokkiinSitoutumatonOpetus = false)

    val lisatiedot = KoskiLisatiedot(None, Some(true), None)
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(opiskeluoikeusOid), oppilaitosOid, Set(perusopetuksenOppimaaraValmis), Some(lisatiedot), SuoritusTila.VALMIS, List.empty)

    val hakemus = BASE_HAKEMUS.copy(keyValues = Map(
      AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2016",
      "arvosana-BI_group0" -> "arvosana-BI-7",
      "arvosana-MA_group0" -> "arvosana-MA-7",
    ))

    val leikkuri = LocalDate.now
    val converterResultWithRekisteriPeruskoulu = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq(opiskeluoikeus), leikkuri, DEFAULT_KOUTA_HAKU)
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

    val converterResultWithoutRekisteriPeruskoulu = AvainArvoConverter.convertOpiskeluoikeudet(personOid, Some(hakemus), Seq.empty, leikkuri, DEFAULT_KOUTA_HAKU)

    //Tarkistetaan, että hakemuksella ilmoitetut arvosanat ovat mukana
    val avainArvoBIHakemus = converterResultWithoutRekisteriPeruskoulu.paatellytArvot.find(_.avain.equals("PK_BI"))
    val avainArvoMAHakemus = converterResultWithoutRekisteriPeruskoulu.paatellytArvot.find(_.avain.equals("PK_MA"))
    Assertions.assertEquals("7", avainArvoBIHakemus.get.arvo)
    Assertions.assertEquals("7", avainArvoMAHakemus.get.arvo)
    Assertions.assertEquals(AvainArvoConstants.arvosananLahdeSeliteHakemus, avainArvoBIHakemus.get.selitteet.head)
    Assertions.assertEquals(AvainArvoConstants.arvosananLahdeSeliteHakemus, avainArvoMAHakemus.get.selitteet.head)

  }
}
