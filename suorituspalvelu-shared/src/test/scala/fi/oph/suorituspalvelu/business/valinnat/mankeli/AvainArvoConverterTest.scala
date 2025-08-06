package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Koodi, Opiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoConverter}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiParser, KoskiToSuoritusConverter}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

import java.time.LocalDate
import java.util.UUID

@Test
@TestInstance(Lifecycle.PER_CLASS)
class AvainArvoConverterTest {

  @Test def testAvainArvoConverterForPeruskouluKeys(): Unit = {
    val fileName = "/1_2_246_562_98_69863082363.json"
    val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName)).toList
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oos: Seq[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet)

      Assertions.assertEquals(1, oos.size)

      val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", oos)

      AvainArvoConstants.perusopetuksenKieliKeys.foreach(key => {
        Assertions.assertEquals(Some("FI"), converterResult.keyValues.get(key))
      })
      AvainArvoConstants.peruskouluSuoritusvuosiKeys.foreach(key => {
        Assertions.assertEquals(Some("2025"), converterResult.keyValues.get(key))
      })
      AvainArvoConstants.peruskouluSuoritettuKeys.foreach(key => {
        Assertions.assertEquals(Some("true"), converterResult.keyValues.get(AvainArvoConstants.peruskouluSuoritettuKeys.head))
      })
    })
  }

  @Test def testAvainArvoConverterForPeruskouluArvosanatJaKielet(): Unit = {
    val fileName = "/1_2_246_562_98_69863082363.json"
    val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName)).toList
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oos: Seq[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet)

      Assertions.assertEquals(1, oos.size)

      val converterResult = AvainArvoConverter.convertOpiskeluoikeudet("1.2.246.562.98.69863082363", oos)

      val tavoiteArvosanat = Map("HI" -> "8", "KO" -> "8", "BI" -> "9", "B1" -> "8", "AOM" -> "8", "LI" -> "9",
        "YH" -> "10", "KU" -> "8", "GE" -> "9", "TH" -> "9", "MA" -> "9", "B2" -> "9", "TE" -> "8",
        "KT" -> "10", "FY" -> "9", "AI" -> "9", "MU" -> "7", "A1" -> "8", "KE" -> "7")
      val tavoiteKielet = Map("B1" -> "SV", "A1" -> "EN", "B2" -> "DE")

      tavoiteArvosanat.foreach((aine, arvosana) => {
        AvainArvoConstants.peruskouluAineenArvosanaPrefixes.foreach(prefix => {
          Assertions.assertEquals(Some(arvosana), converterResult.keyValues.get(prefix + aine))
        })
      })

      tavoiteKielet.foreach((aine, kieli) => {
        AvainArvoConstants.peruskouluAineenKieliPostfixes.foreach(postfix => {
          AvainArvoConstants.peruskouluAineenArvosanaPrefixes.foreach(prefix => {
            Assertions.assertEquals(Some(kieli), converterResult.keyValues.get(prefix + aine + postfix))
          })
        })
      })
    })
  }

  @Test def testKorkeimmatArvosanat(): Unit = {
    val aineet = Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti, hankala savotta"), None, None), Koodi("A1", "koodisto", None), Koodi("8", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None))),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("A1", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None))),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia, suoritus"), None, None), Koodi("BI", "koodisto", None), Koodi("S", "koodisto", None), None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("biologia"), None, None), Koodi("BI", "koodisto", None), Koodi("8", "koodisto", None), None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("kotitalous"), None, None), Koodi("KO", "koodisto", None), Koodi("S", "koodisto", None), None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("kotitalous, osallistuminen"), None, None), Koodi("BI", "koodisto", None), Koodi("O", "koodisto", None), None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("liikunta"), None, None), Koodi("LI", "koodisto", None), Koodi("9", "koodisto", None), None),
                     PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("liikunta, toinen"), None, None), Koodi("LI", "koodisto", None), Koodi("7", "koodisto", None), None))
    val oppimaara = PerusopetuksenOppimaara(UUID.randomUUID(), "1.2.3", Koodi("arvo", "koodisto", Some(1)), Koodi("arvo", "koodisto", Some(1)), Set.empty, Some(LocalDate.parse("2025-06-06")), Some(LocalDate.parse("2025-06-06")), aineet)

    val korkeimmatArvosanat: Map[String, String] = AvainArvoConverter.korkeimmatPerusopetuksenArvosanatAineittain(Some(oppimaara), Seq.empty).toMap

    val tavoiteArvosanat = Map("A1" -> "10", "BI" -> "8", "KO" -> "S", "LI" -> "9")
    tavoiteArvosanat.foreach((aine, arvosana) => {
      AvainArvoConstants.peruskouluAineenArvosanaPrefixes.foreach(prefix => {
        Assertions.assertEquals(Some(arvosana), korkeimmatArvosanat.get(prefix + aine))
      })
    })
  }
}
