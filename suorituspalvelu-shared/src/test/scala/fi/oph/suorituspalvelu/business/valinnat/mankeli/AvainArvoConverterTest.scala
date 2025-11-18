package fi.oph.suorituspalvelu.business.valinnat.mankeli

import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.{Koodisto, KoutaHaku, KoutaHakuaika}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, GeneerinenOpiskeluoikeus, KantaOperaatiot, Koodi, Laajuus, Opiskeluoikeus, Oppilaitos, PerusopetuksenOppiaine, PerusopetuksenOppimaara, SuoritusTila, Telma, VapaaSivistystyo}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoContainer, AvainArvoConverter}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiParser, KoskiToSuoritusConverter}
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

  @Test def testAvainArvoConverterForPeruskouluKeys(): Unit = {
    val fileName = "/1_2_246_562_98_69863082363.json"
    val splitData = KoskiIntegration.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName)).toList
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oos: Seq[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet, DUMMY_KOODISTOPROVIDER)

      Assertions.assertEquals(1, oos.size)

      val leikkuri = java.time.Instant.ofEpochMilli(System.currentTimeMillis()).atZone(java.time.ZoneId.systemDefault()).toLocalDate
      val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", oos, leikkuri, Some(DEFAULT_KOUTA_HAKU))

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
      val leikkuri = java.time.Instant.ofEpochMilli(System.currentTimeMillis()).atZone(java.time.ZoneId.systemDefault()).toLocalDate
      val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", oos, leikkuri, Some(DEFAULT_KOUTA_HAKU))
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
    val oppimaara = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "1.2.3"), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, Some(LocalDate.parse("2025-06-06")), Some(LocalDate.parse("2025-06-06")), aineet)

    val ka: Set[AvainArvoContainer] = AvainArvoConverter.korkeimmatPerusopetuksenArvosanatAineittain(Some(oppimaara), Seq.empty)
    val korkeimmatArvosanat = ka.map(aa => (aa.avain, aa.arvo)).toMap

    val tavoiteArvosanat = Map("A1" -> "10", "BI" -> "8", "KO" -> "S", "LI" -> "9")
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

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuri, Some(DEFAULT_KOUTA_HAKU))

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

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuri, Some(DEFAULT_KOUTA_HAKU))

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.yoSuoritettuKey))

  }

  @Test def testAmmArvoMyohassaValmistunut(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val leikkuripaivanJalkeenValmistunutTutkinto = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi("123456", "koulutus", Some(1)), Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"),
      Koodi("valmistunut", "jokutila", Some(1)), SuoritusTila.VALMIS, Some(LocalDate.parse("2021-01-01")), Some(LocalDate.parse("2024-04-03")), None, Koodi("tapa", "suoritustapa", Some(1)), Koodi("kieli", "suorituskieli", Some(1)), Set.empty)
    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(leikkuripaivanJalkeenValmistunutTutkinto), None))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, Some(DEFAULT_KOUTA_HAKU))

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.ammSuoritettuKey))
  }

  @Test def testAmmArvoAjoissaValmistunut(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val ajoissaValmistunut = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi("123456", "koulutus", Some(1)), Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"),
      Koodi("valmistunut", "jokutila", Some(1)), SuoritusTila.VALMIS, Some(LocalDate.parse("2021-01-01")), Some(LocalDate.parse("2023-04-03")), None, Koodi("tapa", "suoritustapa", Some(1)), Koodi("kieli", "suorituskieli", Some(1)), Set.empty)
    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(ajoissaValmistunut), None))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, Some(DEFAULT_KOUTA_HAKU))

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
      Some(LocalDate.parse("2021-01-01")),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Koodi("FI", "kieli", Some(1)),
      Some (Laajuus(26, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None))
    )

    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Oppilaitos(Kielistetty(None, None, None), ""),
      Set(telmaTutkinto),
      None
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, Some(DEFAULT_KOUTA_HAKU))

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
      Some(LocalDate.parse("2021-01-01")),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Koodi("FI", "kieli", Some(1)),
      Some(Laajuus(24, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None))
    )

    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Oppilaitos(Kielistetty(None, None, None), ""),
      Set(telmaTutkinto),
      None
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, Some(DEFAULT_KOUTA_HAKU))

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
      Some(LocalDate.parse("2021-01-01")),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Koodi("FI", "kieli", Some(1)),
      Some(Laajuus(26, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None))
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
      None
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, Some(haku))

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
      Some(LocalDate.parse("2021-01-01")),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Some(Laajuus(28, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
      Koodi("FI", "kieli", Some(1))
    )

    val oikeudet = Seq(GeneerinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Koodi("", "", None),
      "oppilaitosOid",
      Set(opistovuosiKoulutus),
      None
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, Some(DEFAULT_KOUTA_HAKU))
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
      Some(LocalDate.parse("2021-01-01")),
      Some(LocalDate.parse("2022-05-15")),
      suoritusVuosi,
      Some(Laajuus(22, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
      Koodi("FI", "kieli", Some(1))
    )

    val oikeudet = Seq(GeneerinenOpiskeluoikeus(
      UUID.randomUUID(),
      "1.2.3",
      Koodi("", "", None),
      "oppilaitosOid",
      Set(opistovuosiKoulutus),
      None
    ))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva, Some(DEFAULT_KOUTA_HAKU))

    Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritettuKey))

    //Jos Opistovuotta ei ole suoritettu riittävässä laajuudessa, suoritusvuodelle ei saa tulla avain-arvoa.
    Assertions.assertEquals(None, converterResult.getAvainArvoMap().get(AvainArvoConstants.opistovuosiSuoritusvuosiKey))
  }
}
