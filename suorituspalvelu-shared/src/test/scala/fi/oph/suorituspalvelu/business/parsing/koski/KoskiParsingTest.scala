package fi.oph.suorituspalvelu.business.parsing

import fi.oph.suorituspalvelu.business.{AmmatillinenTutkinto, Koodi, PerusopetuksenOppimaara, Suoritus}
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
        val suoritukset = KoskiToSuoritusConverter.toSuoritukset(koskiOpiskeluoikeudet)
      })
    })

  @Test def testIsYTO(): Unit =
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106727"))
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106728"))
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106729"))

    Assertions.assertFalse(KoskiToSuoritusConverter.isYTO("123456"))
    Assertions.assertFalse(KoskiToSuoritusConverter.isYTO("234567"))


  private def getFirstSuoritusFromJson(data: String): Suoritus =
    val splitData = KoskiParser.splitKoskiDataByOppija(new ByteArrayInputStream(data.getBytes))
    splitData.map((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      KoskiToSuoritusConverter.toSuoritukset(koskiOpiskeluoikeudet, true)
    }).next().head

  @Test def testAmmatillisenTutkinnonTila(): Unit =
    // ammatillisen tutkinnon tila on ajallisesti viimeisen opiskeluoikeusjakson tila (vaikka alku olisi tulevaisuudessa)
    val tutkinto = getFirstSuoritusFromJson("""
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
        |""".stripMargin).asInstanceOf[AmmatillinenTutkinto]
    Assertions.assertEquals(Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), tutkinto.tila)

  @Test def testAmmatillisenTutkinnonKentat(): Unit =
    val tutkinto = getFirstSuoritusFromJson("""
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
        |            "vahvistus": {
        |               "päivä": "2023-03-15"
        |            },
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
        |""".stripMargin).asInstanceOf[AmmatillinenTutkinto]

    Assertions.assertEquals(Koodi("351301", "koulutus", Some(12)), tutkinto.tyyppi)
    Assertions.assertEquals("Ajoneuvoalan perustutkinto", tutkinto.nimi)
    Assertions.assertEquals(Some(BigDecimal.valueOf(1.0)), tutkinto.keskiarvo)
    Assertions.assertEquals(Some(LocalDate.parse("2023-03-15")), tutkinto.vahvistusPaivamaara)
    Assertions.assertEquals(Koodi("reformi", "ammatillisentutkinnonsuoritustapa", Some(1)), tutkinto.suoritustapa)

  @Test def testAmmatillisenTutkinnonOsasuoritukset(): Unit =
    val tutkinto = getFirstSuoritusFromJson("""
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
        |""".stripMargin).asInstanceOf[AmmatillinenTutkinto]

    val osaSuoritus = tutkinto.osat.head
    Assertions.assertEquals(Koodi("106727", "tutkinnonosat", Some(2)), osaSuoritus.koodi)
    Assertions.assertEquals("Viestintä- ja vuorovaikutusosaaminen", osaSuoritus.nimi)
    Assertions.assertEquals(true, osaSuoritus.yto) // koodi 106727 kuuluu yleisiin tutkinnon osiin
    Assertions.assertEquals(Some(Koodi("Hyväksytty", "arviointiasteikkoammatillinen15", Some(1))), osaSuoritus.arvosana)
    Assertions.assertEquals(Some(20), osaSuoritus.laajuus)
    Assertions.assertEquals(Some(Koodi("6", "opintojenlaajuusyksikko", Some(1))), osaSuoritus.laajuusKoodi)

  @Test def testAmmatillisenTutkinnonOsaAlueet(): Unit =
    val tutkinto = getFirstSuoritusFromJson("""
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
        |""".stripMargin).asInstanceOf[AmmatillinenTutkinto]

    val osaAlue = tutkinto.osat.head.osaAlueet.head
    Assertions.assertEquals(Koodi("VVAI22", "ammatillisenoppiaineet", Some(1)), osaAlue.koodi)
    Assertions.assertEquals("Viestintä ja vuorovaikutus äidinkielellä", osaAlue.nimi)
    Assertions.assertEquals(Some(Koodi("1", "arviointiasteikkoammatillinen15", Some(1))), osaAlue.arvosana)
    Assertions.assertEquals(Some(4), osaAlue.laajuus)
    Assertions.assertEquals(Some(Koodi("6", "opintojenlaajuusyksikko", Some(1))), osaAlue.laajuusKoodi)

  @Test def testPerusopetuksenOppimaarat(): Unit =
    val oppimaara = getFirstSuoritusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.32727448402"
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2024-06-01",
        |              "tila": {
        |                "koodiarvo": "valmistunut",
        |                "koodistoUri": "koskiopiskeluoikeudentila",
        |                "koodistoVersio": 1
        |              }
        |            }
        |          ]
        |        },
        |        "suoritukset": [
        |          {
        |            "tyyppi": {
        |              "koodiarvo": "perusopetuksenoppimaara",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            },
        |            "vahvistus": {
        |              "päivä": "2024-06-01"
        |            },
        |            "koulusivistyskieli": [
        |              {
        |                "koodiarvo": "FI",
        |                "nimi": {
        |                  "fi": "suomi"
        |                },
        |                "koodistoUri": "kieli"
        |              }
        |            ]
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[PerusopetuksenOppimaara]

    Assertions.assertEquals("1.2.246.562.10.32727448402", oppimaara.organisaatioOid)
    Assertions.assertEquals(Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), oppimaara.tila)
    Assertions.assertEquals(Some(LocalDate.parse("2024-06-01")), oppimaara.vahvistusPaivamaara)
    Assertions.assertEquals(Set(Koodi("FI", "kieli", None)), oppimaara.koulusivistyskieli);
}
