package fi.oph.suorituspalvelu.business.parsing

import fi.oph.suorituspalvelu.business.{AmmatillinenTutkinto, Koodi}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

import java.io.ByteArrayInputStream
import java.time.LocalDate

object KoskiParsing {
}

@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiParsingTest {

  @Test def testKoskiParsingAndConversion(): Unit =
    Seq(
      "/1_2_246_562_24_40483869857.json",
      "/1_2_246_562_24_30563266636.json"
    ).foreach(fileName => {
      val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName))
      splitData.foreach((oppijaOid, data) => {
        val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
        val suoritukset = KoskiToSuoritusConverter.toSuoritus(koskiOpiskeluoikeudet)
      })
    })

  @Test def testIsYTO(): Unit =
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106727"))
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106728"))
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106729"))

    Assertions.assertFalse(KoskiToSuoritusConverter.isYTO("123456"))
    Assertions.assertFalse(KoskiToSuoritusConverter.isYTO("234567"))


  private def getFirstAmmatillinenTutkintoFromJson(data: String): AmmatillinenTutkinto =
    val splitData = KoskiParser.splitKoskiDataByOppija(new ByteArrayInputStream(data.getBytes))
    splitData.map((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      KoskiToSuoritusConverter.toSuoritus(koskiOpiskeluoikeudet, true)
    }).next().head.asInstanceOf[AmmatillinenTutkinto]

  @Test def testAmmatillisenTutkinnonTila(): Unit =
    // ammatillisen tutkinnon tila on ajallisesti viimeisen opiskeluoikeusjakson tila (vaikka alku olisi tulevaisuudessa)
    val tutkinto = getFirstAmmatillinenTutkintoFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.40483869857",
        |    "opiskeluoikeudet": [
        |      {
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2023-07-18",
        |              "tila": {
        |                "koodiarvo": "valmistunut",
        |                "koodistoUri": "koskiopiskeluoikeudentila",
        |                "koodistoVersio": 1
        |
        |              }
        |            },
        |            {
        |              "alku": "2023-07-13",
        |              "tila": {
        |                "koodiarvo": "lasna"
        |              }
        |            }
        |          ]
        |        },
        |        "suoritukset": [
        |          {
        |            "tyyppi": {
        |              "koodiarvo": "ammatillinentutkinto"
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin)
    Assertions.assertEquals(Koodi("valmistunut", "koskiopiskeluoikeudentila", 1), tutkinto.tila)

  @Test def testAmmatillisenTutkinnonKentat(): Unit =
    val tutkinto = getFirstAmmatillinenTutkintoFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.40483869857",
        |    "opiskeluoikeudet": [
        |      {
        |        "suoritukset": [
        |          {
        |            "tyyppi": {
        |              "koodiarvo": "ammatillinentutkinto"
        |            },
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "351301",
        |                "nimi": {
        |                  "fi": "Ajoneuvoalan perustutkinto"
        |                },
        |                "koodistoUri": "koulutus",
        |                "koodistoVersio": 12
        |              }
        |            },
        |            "keskiarvo": 1.0,
        |            "vahvistuspäivä": "2023-03-15",
        |            "suoritustapa": {
        |              "koodiarvo": "reformi",
        |              "koodistoUri": "ammatillisentutkinnonsuoritustapa",
        |              "koodistoVersio": 1
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin)

    Assertions.assertEquals(Koodi("351301", "koulutus", 12), tutkinto.tyyppi)
    Assertions.assertEquals("Ajoneuvoalan perustutkinto", tutkinto.nimi)
    Assertions.assertEquals(Some(BigDecimal.valueOf(1.0)), tutkinto.keskiarvo)
    Assertions.assertEquals(Some(LocalDate.parse("2023-03-15")), tutkinto.vahvistusPaivamaara)
    Assertions.assertEquals(Koodi("reformi", "ammatillisentutkinnonsuoritustapa", 1), tutkinto.suoritustapa)

  @Test def testAmmatillisenTutkinnonOsasuoritukset(): Unit =
    val tutkinto = getFirstAmmatillinenTutkintoFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.40483869857",
        |    "opiskeluoikeudet": [
        |      {
        |        "suoritukset": [
        |          {
        |            "tyyppi": {
        |              "koodiarvo": "ammatillinentutkinto"
        |            },
        |            "osasuoritukset": [
        |              {
        |                "koulutusmoduuli": {
        |                  "tunniste": {
        |                    "koodiarvo": "106727",
        |                    "nimi": {
        |                      "fi": "Viestintä- ja vuorovaikutusosaaminen"
        |                    },
        |                    "koodistoUri": "tutkinnonosat",
        |                    "koodistoVersio": 2
        |                  },
        |                  "laajuus": {
        |                    "arvo": 20.0,
        |                    "yksikkö": {
        |                      "koodiarvo": "6",
        |                      "koodistoUri": "opintojenlaajuusyksikko",
        |                      "koodistoVersio": 1
        |                    }
        |
        |                  }
        |                },
        |                "arviointi": [
        |                  {
        |                    "arvosana": {
        |                      "koodiarvo": "Hyväksytty",
        |                      "nimi": {
        |                        "fi": "Hyväksytty"
        |                      },
        |                      "koodistoUri": "arviointiasteikkoammatillinen15",
        |                      "koodistoVersio": 1
        |                    }
        |                  }
        |                ]
        |              }
        |            ]
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin)

    val osaSuoritus = tutkinto.osat.head
    Assertions.assertEquals(Koodi("106727", "tutkinnonosat", 2), osaSuoritus.koodi)
    Assertions.assertEquals("Viestintä- ja vuorovaikutusosaaminen", osaSuoritus.nimi)
    Assertions.assertEquals(true, osaSuoritus.yto) // koodi 106727 kuuluu yleisiin tutkinnon osiin
    Assertions.assertEquals(Some(Koodi("Hyväksytty", "arviointiasteikkoammatillinen15", 1)), osaSuoritus.arvosana)
    Assertions.assertEquals(20, osaSuoritus.laajuus)
    Assertions.assertEquals(Koodi("6", "opintojenlaajuusyksikko", 1), osaSuoritus.laajuusKoodi)

  @Test def testAmmatillisenTutkinnonOsaAlueet(): Unit =
    val tutkinto = getFirstAmmatillinenTutkintoFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.40483869857",
        |    "opiskeluoikeudet": [
        |      {
        |        "suoritukset": [
        |          {
        |            "tyyppi": {
        |              "koodiarvo": "ammatillinentutkinto"
        |            },
        |            "osasuoritukset": [
        |              {
        |                "osasuoritukset": [
        |                  {
        |                    "koulutusmoduuli": {
        |                      "tunniste": {
        |                        "koodiarvo": "VVAI22",
        |                        "nimi": {
        |                          "fi": "Viestintä ja vuorovaikutus äidinkielellä"
        |                        },
        |                        "koodistoUri": "ammatillisenoppiaineet",
        |                        "koodistoVersio": 1
        |                      },
        |                      "laajuus": {
        |                        "arvo": 4.0,
        |                        "yksikkö": {
        |                          "koodiarvo": "6",
        |                          "koodistoUri": "opintojenlaajuusyksikko",
        |                          "koodistoVersio": 1
        |                        }
        |                      }
        |                    },
        |                    "arviointi": [
        |                      {
        |                        "arvosana": {
        |                          "koodiarvo": "1",
        |                          "nimi": {
        |                            "fi": "1"
        |                          },
        |                          "koodistoUri": "arviointiasteikkoammatillinen15",
        |                          "koodistoVersio": 1
        |                        }
        |                      }
        |                    ]
        |                  }
        |                ]
        |              }
        |            ]
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin)

    val osaAlue = tutkinto.osat.head.osaAlueet.head
    Assertions.assertEquals(Koodi("VVAI22", "ammatillisenoppiaineet", 1), osaAlue.koodi)
    Assertions.assertEquals("Viestintä ja vuorovaikutus äidinkielellä", osaAlue.nimi)
    Assertions.assertEquals(Some(Koodi("1", "arviointiasteikkoammatillinen15", 1)), osaAlue.arvosana)
    Assertions.assertEquals(4, osaAlue.laajuus)
    Assertions.assertEquals(Koodi("6", "opintojenlaajuusyksikko", 1), osaAlue.laajuusKoodi)

}
