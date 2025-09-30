package fi.oph.suorituspalvelu.business.valinnat.mankeli

import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.util.KoodistoProvider
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, KantaOperaatiot, Koodi, Opiskeluoikeus, Oppilaitos, PerusopetuksenOppiaine, PerusopetuksenOppimaara, SuoritusTila}
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

  @Test def testAvainArvoConverterForPeruskouluKeys(): Unit = {
    val fileName = "/1_2_246_562_98_69863082363.json"
    val splitData = KoskiIntegration.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName)).toList
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oos: Seq[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet, DUMMY_KOODISTOPROVIDER)

      Assertions.assertEquals(1, oos.size)

      val leikkuri = java.time.Instant.ofEpochMilli(System.currentTimeMillis()).atZone(java.time.ZoneId.systemDefault()).toLocalDate
      val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", oos, leikkuri)

      AvainArvoConstants.perusopetuksenKieliKeys.foreach(key => {
        Assertions.assertEquals(Some("FI"), converterResult.getAvainArvoMap().get(key))
      })
      AvainArvoConstants.peruskouluSuoritusvuosiKeys.foreach(key => {
        Assertions.assertEquals(Some("2025"), converterResult.getAvainArvoMap().get(key))
      })
      AvainArvoConstants.peruskouluSuoritettuKeys.foreach(key => {
        Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(AvainArvoConstants.peruskouluSuoritettuKeys.head))
      })
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
      val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", oos, leikkuri)

      val tavoiteArvosanat = Map("HI" -> "8", "BI" -> "9", "B1" -> "8", "AOM" -> "8", "LI" -> "9",
        "YH" -> "10", "KU" -> "8", "GE" -> "9", "MA" -> "9", "B2" -> "9", "TE" -> "8",
        "KT" -> "10", "FY" -> "9", "AI" -> "9", "MU" -> "7", "A1" -> "8", "KE" -> "7")
      val tavoiteKielet = Map("B1" -> "SV", "A1" -> "EN", "B2" -> "DE")

      tavoiteArvosanat.foreach((aine, arvosana) => {
        AvainArvoConstants.peruskouluAineenArvosanaPrefixes.foreach(prefix => {
          Assertions.assertEquals(Some(arvosana), converterResult.getAvainArvoMap().get(prefix + aine))
        })
      })

      tavoiteKielet.foreach((aine, kieli) => {
        AvainArvoConstants.peruskouluAineenKieliPostfixes.foreach(postfix => {
          AvainArvoConstants.peruskouluAineenArvosanaPrefixes.foreach(prefix => {
            Assertions.assertEquals(Some(kieli), converterResult.getAvainArvoMap().get(prefix + aine + postfix))
          })
        })
      })
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
    tavoiteArvosanat.foreach((aine, arvosana) => {
      AvainArvoConstants.peruskouluAineenArvosanaPrefixes.foreach(prefix => {
        Assertions.assertEquals(Some(arvosana), korkeimmatArvosanat.get(prefix + aine))
      })
    })
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

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuri)

    AvainArvoConstants.yoSuoritettuKeys.foreach(key => {
      Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(key))
    })
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

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuri)

    AvainArvoConstants.yoSuoritettuKeys.foreach(key => {
      Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(key))
    })
  }

  @Test def testAmmArvoMyohassaValmistunut(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val leikkuripaivanJalkeenValmistunutTutkinto = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi("123456", "koulutus", Some(1)), Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"),
      Koodi("valmistunut", "jokutila", Some(1)), SuoritusTila.VALMIS, Some(LocalDate.parse("2021-01-01")), Some(LocalDate.parse("2024-04-03")), None, Koodi("tapa", "suoritustapa", Some(1)), Koodi("kieli", "suorituskieli", Some(1)), Set.empty)
    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(leikkuripaivanJalkeenValmistunutTutkinto), None))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva)

    AvainArvoConstants.ammSuoritettuKeys.foreach(key => {
      Assertions.assertEquals(Some("false"), converterResult.getAvainArvoMap().get(key))
    })
  }

  @Test def testAmmArvoAjoissaValmistunut(): Unit = {
    val personOid = "1.2.246.562.98.69863082363"

    val leikkuriPaiva = LocalDate.parse("2023-05-15")
    val ajoissaValmistunut = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi("123456", "koulutus", Some(1)), Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"),
      Koodi("valmistunut", "jokutila", Some(1)), SuoritusTila.VALMIS, Some(LocalDate.parse("2021-01-01")), Some(LocalDate.parse("2023-04-03")), None, Koodi("tapa", "suoritustapa", Some(1)), Koodi("kieli", "suorituskieli", Some(1)), Set.empty)
    val oikeudet = Seq(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(ajoissaValmistunut), None))

    val converterResult = AvainArvoConverter.convertOpiskeluoikeudet(personOid, oikeudet, leikkuriPaiva)

    AvainArvoConstants.ammSuoritettuKeys.foreach(key => {
      Assertions.assertEquals(Some("true"), converterResult.getAvainArvoMap().get(key))
    })
  }

}
