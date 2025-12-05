package fi.oph.suorituspalvelu.business.parsing.ytr

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koe, Koodi, Laajuus, PerusopetuksenOppimaaranOppiaineidenSuoritus, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, Suoritus, SuoritusTila, Telma, Tuva, VapaaSivistystyo, YOOpiskeluoikeus, YOTutkinto}
import fi.oph.suorituspalvelu.parsing.koski.{Arviointi, Kielistetty, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiLisatiedot, KoskiParser, KoskiToSuoritusConverter, Kotiopetusjakso, OpiskeluoikeusJakso, OpiskeluoikeusTila}
import fi.oph.suorituspalvelu.parsing.ytr.{YtrParser, YtrToSuoritusConverter}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

import java.io.ByteArrayInputStream
import java.time.LocalDate

@Test
@TestInstance(Lifecycle.PER_CLASS)
class YtrParsingTest {

  @Test def testRemoveSsn(): Unit =
    val tutkinto =
      """
        |{
        |  "ssn": "000000-000A",
        |  "exams": [],
        |  "language": "fi"
        |}
        |""".stripMargin

    val sanitized = YtrParser.splitAndSanitize(tutkinto).next()._2
    Assertions.assertEquals(None, YtrParser.parseYtrData(sanitized).ssn)  
  
  @Test def testParseYlioppilastutkinto(): Unit =
    val tutkinto =
      """
        |{
        |  "ssn": "000000-000A",
        |  "graduationPeriod": "2013K",
        |  "graduationDate": "2013-06-01",
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
    
    Assertions.assertEquals(YOOpiskeluoikeus(
      parsed.tunniste,
      YOTutkinto(
        parsed.yoTutkinto.tunniste,
        Koodi("FI", "kieli", Some(1)),
        SuoritusTila.VALMIS,
        Some(LocalDate.parse("2013-06-01")),
        Set(Koe(
          parsed.yoTutkinto.aineet.head.tunniste,
          Koodi("EA", YtrToSuoritusConverter.YO_KOKEET_KOODISTO, Some(1)),
          LocalDate.parse("2012-06-01"),
          Koodi("M", YtrToSuoritusConverter.YO_ARVOSANAT_KOODISTO, Some(1)),
          Some(236)
        ))
      )
    ), parsed)
}
