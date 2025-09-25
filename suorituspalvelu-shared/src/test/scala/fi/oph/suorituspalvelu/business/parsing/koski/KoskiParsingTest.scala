package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, NuortenPerusopetuksenOppiaineenOppimaara, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, Suoritus, Telma, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.parsing.koski.{Arviointi, Kielistetty, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiLisatiedot, KoskiParser, KoskiToSuoritusConverter, Kotiopetusjakso, OpiskeluoikeusJakso, OpiskeluoikeusTila}
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
        val koskiOpiskeluoikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(data))
      })
    })

  @Test def testIsYTO(): Unit =
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106727"))
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106728"))
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106729"))

    Assertions.assertFalse(KoskiToSuoritusConverter.isYTO("123456"))
    Assertions.assertFalse(KoskiToSuoritusConverter.isYTO("234567"))

  private def getFirstOpiskeluoikeusFromJson(data: String): Option[Opiskeluoikeus] =
    val splitData = KoskiParser.splitKoskiDataByOppija(new ByteArrayInputStream(data.getBytes))
    splitData.map((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet)
    }).next().headOption

  private def getFirstSuoritusFromJson(data: String): Suoritus =
    val splitData = KoskiParser.splitKoskiDataByOppija(new ByteArrayInputStream(data.getBytes))
    splitData.map((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      KoskiToSuoritusConverter.toSuoritukset(koskiOpiskeluoikeudet, true)
    }).next().head

  @Test def testAmmatillisetOpiskeluoikeudet(): Unit =
    val opiskeluoikeus = getFirstOpiskeluoikeusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "tyyppi": {
        |          "koodiarvo": "ammatillinenkoulutus",
        |          "koodistoUri": "opiskeluoikeudentyyppi",
        |          "koodistoVersio": 1
        |        },
        |        "oid": "1.2.246.562.15.24186343661",
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.41945921983",
        |          "nimi": {
        |            "fi": "Stadin ammattiopisto",
        |            "sv": "Stadin ammattiopisto",
        |            "en": "Stadin ammattiopisto"
        |          }
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2022-06-06",
        |              "tila": {
        |                "koodiarvo": "lasna",
        |                "koodistoUri": "koskiopiskeluoikeudentila",
        |                "koodistoVersio": 1,
        |                "nimi": {}
        |              }
        |            }
        |          ]
        |        },
        |        "suoritukset": [
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).get.asInstanceOf[AmmatillinenOpiskeluoikeus]

    Assertions.assertEquals("1.2.246.562.15.24186343661", opiskeluoikeus.oid)
    Assertions.assertEquals("1.2.246.562.10.41945921983", opiskeluoikeus.oppilaitos.oid)
    Assertions.assertEquals(Some(OpiskeluoikeusTila(List(OpiskeluoikeusJakso(
      LocalDate.parse("2022-06-06"), KoskiKoodi("lasna", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None))))), opiskeluoikeus.tila)

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
        |            },
        |            "koulutusmoduuli": {
        |              "koulutustyyppi": {
        |                "koodiarvo": "1"
        |              }
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[AmmatillinenPerustutkinto]
    Assertions.assertEquals(Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), tutkinto.koskiTila)

  @Test def testAmmatillisenTutkinnonKentat(): Unit =
    val tutkinto = getFirstSuoritusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.40483869857",
        |    "opiskeluoikeudet": [
        |      {
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.41945921983",
        |          "nimi": {
        |            "fi": "Stadin ammattiopisto",
        |            "sv": "Stadin ammattiopisto sv",
        |            "en": "Stadin ammattiopisto en"
        |          }
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2022-06-06"
        |            }
        |          ]
        |        },
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
        |              },
        |              "koulutustyyppi": {
        |                "koodiarvo": "1",
        |                "koodistoUri": "koulutustyyppi",
        |                "koodistoVersio": 2
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
        |            },
        |            "suorituskieli": {
        |              "koodiarvo": "FI",
        |              "koodistoUri": "kieli",
        |              "koodistoVersio": 1
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[AmmatillinenPerustutkinto]

    Assertions.assertNotNull(tutkinto.tunniste)
    Assertions.assertEquals(Koodi("351301", "koulutus", Some(12)), tutkinto.koodi)
    Assertions.assertEquals(Kielistetty(Some("Ajoneuvoalan perustutkinto"), None, None), tutkinto.nimi)
    Assertions.assertEquals(Some(BigDecimal.valueOf(1.0)), tutkinto.keskiarvo)
    Assertions.assertEquals(Some(LocalDate.parse("2022-06-06")), tutkinto.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2023-03-15")), tutkinto.vahvistusPaivamaara)
    Assertions.assertEquals(Koodi("reformi", "ammatillisentutkinnonsuoritustapa", Some(1)), tutkinto.suoritustapa)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), tutkinto.suoritusKieli)
    Assertions.assertEquals(Oppilaitos(Kielistetty(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en")), "1.2.246.562.10.41945921983"), tutkinto.oppilaitos)

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
        |            "koulutusmoduuli": {
        |              "koulutustyyppi": {
        |                "koodiarvo": "1"
        |              }
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
        |                    },
        |                    "päivä": "2023-03-15"
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
        |""".stripMargin).asInstanceOf[AmmatillinenPerustutkinto]

    val osaSuoritus = tutkinto.osat.head
    Assertions.assertNotNull(osaSuoritus.tunniste)
    Assertions.assertEquals(Koodi("106727", "tutkinnonosat", Some(2)), osaSuoritus.koodi)
    Assertions.assertEquals(Kielistetty(Some("Viestintä- ja vuorovaikutusosaaminen"), None, None), osaSuoritus.nimi)
    Assertions.assertEquals(true, osaSuoritus.yto) // koodi 106727 kuuluu yleisiin tutkinnon osiin
    Assertions.assertEquals(Some(Arvosana(Koodi("Hyväksytty", "arviointiasteikkoammatillinen15", Some(1)), Kielistetty(Some("Hyväksytty"), None, None))), osaSuoritus.arvosana)
    Assertions.assertEquals(Some(Laajuus(20, Koodi("6", "opintojenlaajuusyksikko", Some(1)), None, None)), osaSuoritus.laajuus)

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
        |            "koulutusmoduuli": {
        |              "koulutustyyppi": {
        |                "koodiarvo": "1"
        |              }
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
        |                      },
        |                      {
        |                        "arvosana": {
        |                          "koodiarvo": "3",
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
        |""".stripMargin).asInstanceOf[AmmatillinenPerustutkinto]

    val osaAlue = tutkinto.osat.head.osaAlueet.head
    Assertions.assertNotNull(osaAlue.tunniste)
    Assertions.assertEquals(Koodi("VVAI22", "ammatillisenoppiaineet", Some(1)), osaAlue.koodi)
    Assertions.assertEquals(Kielistetty(Some("Viestintä ja vuorovaikutus äidinkielellä"), None, None), osaAlue.nimi)
    Assertions.assertEquals(Some(Koodi("3", "arviointiasteikkoammatillinen15", Some(1))), osaAlue.arvosana)
    Assertions.assertEquals(Some(Laajuus(4, Koodi("6", "opintojenlaajuusyksikko", Some(1)), None, None)), osaAlue.laajuus)

  @Test def testAmmattiTutkinto(): Unit =
    val tutkinto = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.35986177022",
        |    "opiskeluoikeudet": [
        |      {
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.41945921983",
        |          "nimi": {
        |            "fi": "Pirkanmaan urheiluhierojakoulu"
        |          }
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2022-06-06"
        |            }
        |          ]
        |        },
        |        "suoritukset": [
        |          {
        |            "tyyppi": {
        |              "koodiarvo": "ammatillinentutkinto"
        |            },
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "437109",
        |                "nimi": {
        |                  "fi": "Hieronnan ammattitutkinto"
        |                },
        |                "koodistoUri": "koulutus",
        |                "koodistoVersio": 12
        |              },
        |              "koulutustyyppi": {
        |                "koodiarvo": "11",
        |                "koodistoUri": "koulutustyyppi",
        |                "koodistoVersio": 2
        |              }
        |            },
        |            "vahvistus": {
        |               "päivä": "2023-03-15"
        |            },
        |            "suorituskieli": {
        |              "koodiarvo": "FI",
        |              "koodistoUri": "kieli",
        |              "koodistoVersio": 1
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[AmmattiTutkinto]

    Assertions.assertNotNull(tutkinto.tunniste)
    Assertions.assertEquals(Koodi("437109", "koulutus", Some(12)), tutkinto.koodi)
    Assertions.assertEquals(Kielistetty(Some("Hieronnan ammattitutkinto"), None, None), tutkinto.nimi)
    Assertions.assertEquals(Oppilaitos(Kielistetty(Some("Pirkanmaan urheiluhierojakoulu"), None, None), "1.2.246.562.10.41945921983"), tutkinto.oppilaitos)
    Assertions.assertEquals(Some(LocalDate.parse("2022-06-06")), tutkinto.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2023-03-15")), tutkinto.vahvistusPaivamaara)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), tutkinto.suoritusKieli)

  @Test def testErikoisAmmattiTutkinto(): Unit =
    val tutkinto = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.35986177022",
        |    "opiskeluoikeudet": [
        |      {
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.54019331674",
        |          "nimi": {
        |            "fi": "HAUS kehittämiskeskus Oy"
        |          }
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2022-06-06"
        |            }
        |          ]
        |        },
        |        "suoritukset": [
        |          {
        |            "tyyppi": {
        |              "koodiarvo": "ammatillinentutkinto"
        |            },
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "437109",
        |                "nimi": {
        |                  "fi": "Talous- ja henkilöstöhallinnon erikoisammattitutkinto"
        |                },
        |                "koodistoUri": "koulutus",
        |                "koodistoVersio": 12
        |              },
        |              "koulutustyyppi": {
        |                "koodiarvo": "12",
        |                "koodistoUri": "koulutustyyppi",
        |                "koodistoVersio": 2
        |              }
        |            },
        |            "vahvistus": {
        |               "päivä": "2023-03-15"
        |            },
        |            "suorituskieli": {
        |              "koodiarvo": "FI",
        |              "koodistoUri": "kieli",
        |              "koodistoVersio": 1
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[ErikoisAmmattiTutkinto]

    Assertions.assertNotNull(tutkinto.tunniste)
    Assertions.assertEquals(Koodi("437109", "koulutus", Some(12)), tutkinto.koodi)
    Assertions.assertEquals(Kielistetty(Some("Talous- ja henkilöstöhallinnon erikoisammattitutkinto"), None, None), tutkinto.nimi)
    Assertions.assertEquals(Oppilaitos(Kielistetty(Some("HAUS kehittämiskeskus Oy"), None, None), "1.2.246.562.10.54019331674"), tutkinto.oppilaitos)
    Assertions.assertEquals(Some(LocalDate.parse("2022-06-06")), tutkinto.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2023-03-15")), tutkinto.vahvistusPaivamaara)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), tutkinto.suoritusKieli)
  
  @Test def testTelma(): Unit =
    val telma = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.21583363224",
        |    "opiskeluoikeudet": [
        |      {
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2022-06-06",
        |              "tila": {
        |                "koodiarvo": "mitatoity"
        |              }
        |            }
        |          ]
        |        },
        |        "suoritukset": [
        |          {
        |            "tyyppi": {
        |              "koodiarvo": "telma",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            },
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "999903",
        |                "nimi": {
        |                  "fi": "Työhön ja itsenäiseen elämään valmentava koulutus (TELMA)"
        |                },
        |                "koodistoUri": "koulutus",
        |                "koodistoVersio": 12
        |              }
        |            },
        |            "vahvistus": {
        |               "päivä": "2023-03-15"
        |            },
        |            "suorituskieli": {
        |              "koodiarvo": "FI",
        |              "koodistoUri": "kieli",
        |              "koodistoVersio": 1
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[Telma]

    Assertions.assertNotNull(telma.tunniste)
    Assertions.assertEquals(Koodi("999903", "koulutus", Some(12)), telma.koodi)
    Assertions.assertEquals(Kielistetty(Some("Työhön ja itsenäiseen elämään valmentava koulutus (TELMA)"), None, None), telma.nimi)
    Assertions.assertEquals(Some(LocalDate.parse("2022-06-06")), telma.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2023-03-15")), telma.vahvistusPaivamaara)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), telma.suoritusKieli)

  @Test def testPerusopetuksenOpiskeluoikeudet(): Unit =
    val opiskeluoikeus = getFirstOpiskeluoikeusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "tyyppi": {
        |          "koodiarvo": "perusopetus",
        |          "koodistoUri": "opiskeluoikeudentyyppi",
        |          "koodistoVersio": 1
        |        },
        |        "oid": "1.2.246.562.15.77661702355",
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.19666365424"
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2022-05-01",
        |              "tila": {
        |                "koodiarvo": "lasna",
        |                "koodistoUri": "koskiopiskeluoikeudentila",
        |                "koodistoVersio": 1,
        |                "nimi": {}
        |              }
        |            }
        |          ]
        |        },
        |        "suoritukset": [
        |        ],
        |        "lisätiedot": {
        |          "erityisenTuenPäätökset": [
        |            {
        |              "opiskeleeToimintaAlueittain": true
        |            }
        |          ],
        |          "kotiopetusjaksot": [
        |            {
        |              "alku": "2021-08-24",
        |              "loppu": "2022-01-23"
        |            }
        |          ],
        |          "vuosiluokkiinSitoutumatonOpetus": false
        |        }
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).get.asInstanceOf[PerusopetuksenOpiskeluoikeus]

    Assertions.assertEquals(Some("1.2.246.562.15.77661702355"), opiskeluoikeus.oid)
    Assertions.assertEquals("1.2.246.562.10.19666365424", opiskeluoikeus.oppilaitosOid)
    Assertions.assertEquals(Some(OpiskeluoikeusTila(List(OpiskeluoikeusJakso(
      LocalDate.parse("2022-05-01"), KoskiKoodi("lasna", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None))))), opiskeluoikeus.tila)
    Assertions.assertEquals(Some(KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(Some(true)))), Some(false),
      Some(List(Kotiopetusjakso("2021-08-24", Some("2022-01-23")))))), opiskeluoikeus.lisatiedot)

  @Test def testPerusopetuksenOppimaarat(): Unit =
    val oppimaara = getFirstSuoritusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.32727448402",
        |          "nimi": {
        |            "fi": "Hatsalan klassillinen koulu"
        |          }
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
        |            "suorituskieli": {
        |              "koodiarvo": "FI",
        |              "koodistoUri": "kieli",
        |              "koodistoVersio": 1
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

    Assertions.assertNotNull(oppimaara.tunniste)
    Assertions.assertTrue(oppimaara.versioTunniste.isEmpty)
    Assertions.assertEquals(Oppilaitos(Kielistetty(Some("Hatsalan klassillinen koulu"), None, None), "1.2.246.562.10.32727448402"), oppimaara.oppilaitos)
    Assertions.assertTrue(oppimaara.yksilollistaminen.isEmpty)
    Assertions.assertEquals(Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), oppimaara.koskiTila)
    Assertions.assertEquals(Some(LocalDate.parse("2024-06-01")), oppimaara.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2024-06-01")), oppimaara.vahvistusPaivamaara)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), oppimaara.suoritusKieli)
    Assertions.assertEquals(Set(Koodi("FI", "kieli", None)), oppimaara.koulusivistyskieli)

  @Test def testPerusopetuksenOppimaaranOppiaineet(): Unit =
    val oppimaara = getFirstSuoritusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.32727448402",
        |          "nimi": {
        |            "fi": "Hatsalan klassillinen koulu"
        |          }
        |        },
        |        "suoritukset": [
        |          {
        |            "tyyppi": {
        |              "koodiarvo": "perusopetuksenoppimaara",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            },
        |            "osasuoritukset": [
        |              {
        |                "koulutusmoduuli": {
        |                  "tunniste": {
        |                    "koodiarvo": "AI",
        |                    "nimi": {
        |                      "fi": "Äidinkieli ja kirjallisuus"
        |                    },
        |                    "lyhytNimi": {
        |                      "fi": "Äidinkieli ja kirjallisuus",
        |                      "sv": "Modersmålet och litteratur"
        |                    },
        |                    "koodistoUri": "koskioppiaineetyleissivistava",
        |                    "koodistoVersio": 1
        |                  },
        |                  "pakollinen": true
        |                },
        |                "arviointi": [
        |                  {
        |                    "arvosana": {
        |                      "koodiarvo": "10",
        |                      "koodistoUri": "arviointiasteikkoyleissivistava",
        |                      "koodistoVersio": 1
        |                    }
        |                  }
        |                ],
        |                "yksilöllistettyOppimäärä": true,
        |                "rajattuOppimäärä": true
        |              }
        |            ]
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[PerusopetuksenOppimaara]

    val oppiaine = oppimaara.aineet.head
    Assertions.assertNotNull(oppiaine.tunniste)
    Assertions.assertEquals(Kielistetty(Some("Äidinkieli ja kirjallisuus"), None, None), oppiaine.nimi)
    Assertions.assertEquals(Koodi("AI", "koskioppiaineetyleissivistava", Some(1)), oppiaine.koodi)
    Assertions.assertEquals(Koodi("10", "arviointiasteikkoyleissivistava", Some(1)), oppiaine.arvosana)
    Assertions.assertEquals(true, oppiaine.pakollinen)
    Assertions.assertEquals(Some(true), oppiaine.yksilollistetty)
    Assertions.assertEquals(Some(true), oppiaine.rajattu)

  @Test def testPerusopetuksenVuosiluokat(): Unit =
    val vuosiluokka = getFirstSuoritusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.21583363224",
        |    "opiskeluoikeudet": [
        |      {
        |        "suoritukset": [
        |          {
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "7",
        |                "nimi": {
        |                  "fi": "7. vuosiluokka"
        |                },
        |                "koodistoUri": "perusopetuksenluokkaaste",
        |                "koodistoVersio": 1
        |              }
        |            },
        |            "luokka": "7A",
        |            "alkamispäivä": "2020-08-15",
        |            "vahvistus": {
        |              "päivä": "2021-06-01"
        |            },
        |            "jääLuokalle": true,
        |            "osasuoritukset": [
        |            ],
        |            "tyyppi": {
        |              "koodiarvo": "perusopetuksenvuosiluokka",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[PerusopetuksenVuosiluokka]

    Assertions.assertNotNull(vuosiluokka.tunniste)
    Assertions.assertEquals(Kielistetty(Some("7. vuosiluokka"), None, None), vuosiluokka.nimi)
    Assertions.assertEquals(Koodi("7", "perusopetuksenluokkaaste", Some(1)), vuosiluokka.koodi)
    Assertions.assertEquals(Some(LocalDate.parse("2020-08-15")), vuosiluokka.alkamisPaiva)
    Assertions.assertEquals(true, vuosiluokka.jaaLuokalle)

  @Test def testNuortenPerusopetuksenOppiaineenOppimaara(): Unit =
    val oppimaara = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.21583363224",
        |    "opiskeluoikeudet": [
        |      {
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2022-05-01"
        |            }
        |          ]
        |        },
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.42923230215",
        |          "nimi": {
        |            "fi": "oppilaitos"
        |          }
        |        },
        |        "suoritukset": [
        |          {
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "MA",
        |                "nimi": {
        |                  "fi": "Matematiikka"
        |                },
        |                "koodistoUri": "koskioppiaineetyleissivistava",
        |                "koodistoVersio": 1
        |              }
        |            },
        |            "arviointi": [
        |              {
        |                "arvosana": {
        |                  "koodiarvo": "8",
        |                  "koodistoUri": "arviointiasteikkoyleissivistava",
        |                  "koodistoVersio": 1
        |                }
        |              }
        |            ],
        |            "vahvistus": {
        |              "päivä": "2022-05-18"
        |            },
        |            "suorituskieli": {
        |              "koodiarvo": "FI",
        |              "koodistoUri": "kieli",
        |              "koodistoVersio": 1
        |            },
        |            "tyyppi": {
        |              "koodiarvo": "nuortenperusopetuksenoppiaineenoppimaara",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[NuortenPerusopetuksenOppiaineenOppimaara]

    Assertions.assertNotNull(oppimaara.tunniste)
    Assertions.assertTrue(oppimaara.versioTunniste.isEmpty)
    Assertions.assertEquals(Oppilaitos(Kielistetty(Some("oppilaitos"), None, None), "1.2.246.562.10.42923230215"), oppimaara.oppilaitos)
    Assertions.assertEquals(Kielistetty(Some("Matematiikka"), None, None), oppimaara.nimi)
    Assertions.assertEquals(Koodi("MA", "koskioppiaineetyleissivistava", Some(1)), oppimaara.koodi)
    Assertions.assertEquals(Koodi("8", "arviointiasteikkoyleissivistava", Some(1)), oppimaara.arvosana)
    Assertions.assertEquals(Some(LocalDate.parse("2022-05-01")), oppimaara.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2022-05-18")), oppimaara.vahvistusPaivamaara)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), oppimaara.suoritusKieli)

  @Test def testAikuistenPerusopetuksenOpiskeluoikeudet(): Unit =
    val opiskeluoikeus = getFirstOpiskeluoikeusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "tyyppi": {
        |          "koodiarvo": "aikuistenperusopetus",
        |          "koodistoUri": "opiskeluoikeudentyyppi",
        |          "koodistoVersio": 1
        |        },
        |        "oid": "1.2.246.562.15.50478693398",
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.42923230215"
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2021-04-16",
        |              "tila": {
        |                "koodiarvo": "lasna",
        |                "koodistoUri": "koskiopiskeluoikeudentila",
        |                "koodistoVersio": 1,
        |                "nimi": {}
        |              }
        |            },
        |            {
        |              "alku": "2022-04-16",
        |              "tila": {
        |                "koodiarvo": "valmistunut",
        |                "koodistoUri": "koskiopiskeluoikeudentila",
        |                "koodistoVersio": 1,
        |                "nimi": {}
        |              }
        |            }
        |          ]
        |        },
        |        "suoritukset": [
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).get.asInstanceOf[PerusopetuksenOpiskeluoikeus]

    Assertions.assertNotNull(opiskeluoikeus.tunniste)
    Assertions.assertEquals(Some("1.2.246.562.15.50478693398"), opiskeluoikeus.oid)
    Assertions.assertEquals("1.2.246.562.10.42923230215", opiskeluoikeus.oppilaitosOid)
    Assertions.assertEquals(Some(OpiskeluoikeusTila(List(
      OpiskeluoikeusJakso(LocalDate.parse("2021-04-16"), KoskiKoodi("lasna", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None)),
      OpiskeluoikeusJakso(LocalDate.parse("2022-04-16"), KoskiKoodi("valmistunut", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None))
    ))), opiskeluoikeus.tila)
    Assertions.assertEquals(None, opiskeluoikeus.lisatiedot)

  @Test def testAikuistenPerusopetuksenOppimaarat(): Unit =
    val oppimaara = getFirstSuoritusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.32727448402",
        |          "nimi": {
        |            "fi": "Hatsalan klassillinen koulu"
        |          }
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
        |              "koodiarvo": "aikuistenperusopetuksenoppimaara",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            },
        |            "vahvistus": {
        |              "päivä": "2022-04-16"
        |            },
        |            "suorituskieli": {
        |              "koodiarvo": "FI",
        |              "koodistoUri": "kieli",
        |              "koodistoVersio": 1
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[PerusopetuksenOppimaara]

    Assertions.assertEquals(Oppilaitos(Kielistetty(Some("Hatsalan klassillinen koulu"), None, None), "1.2.246.562.10.32727448402"), oppimaara.oppilaitos)
    Assertions.assertEquals(Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), oppimaara.koskiTila)
    Assertions.assertEquals(Some(LocalDate.parse("2024-06-01")), oppimaara.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2022-04-16")), oppimaara.vahvistusPaivamaara)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), oppimaara.suoritusKieli)

  @Test def testTuvaOpiskeluoikeus(): Unit =
    val opiskeluoikeus = getFirstOpiskeluoikeusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.21583363224",
        |    "opiskeluoikeudet": [
        |      {
        |        "tyyppi": {
        |          "koodiarvo": "tuva",
        |          "koodistoUri": "opiskeluoikeudentyyppi",
        |          "koodistoVersio": 1
        |        },
        |        "oid": "1.2.246.562.15.30994048939",
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.41945921983"
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2025-04-16",
        |              "tila": {
        |                "koodiarvo": "valmistunut",
        |                "koodistoUri": "koskiopiskeluoikeudentila",
        |                "koodistoVersio": 1,
        |                "nimi": {}
        |              }
        |            }
        |          ]
        |        },
        |        "suoritukset": []
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).get.asInstanceOf[GeneerinenOpiskeluoikeus]

    Assertions.assertNotNull(opiskeluoikeus.tunniste)
    Assertions.assertEquals(Koodi("tuva", "opiskeluoikeudentyyppi", Some(1)), opiskeluoikeus.tyyppi)
    Assertions.assertEquals("1.2.246.562.15.30994048939", opiskeluoikeus.oid)
    Assertions.assertEquals(Some(OpiskeluoikeusTila(List(OpiskeluoikeusJakso(LocalDate.parse("2025-04-16"), KoskiKoodi("valmistunut", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None))))), opiskeluoikeus.tila)
    Assertions.assertEquals("1.2.246.562.10.41945921983", opiskeluoikeus.oppilaitosOid)

  @Test def testTuvaSuoritus(): Unit =
    val tuva = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.21583363224",
        |    "opiskeluoikeudet": [
        |      {
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.41945921983",
        |          "nimi": {
        |            "fi": "Stadin ammattiopisto"
        |          }
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2022-08-01",
        |              "tila": {
        |                "koodiarvo": "lasna",
        |                "koodistoUri": "koskiopiskeluoikeudentila",
        |                "koodistoVersio": 1
        |              }
        |            }
        |          ]
        |        },
        |        "suoritukset": [
        |          {
        |            "tyyppi": {
        |              "koodiarvo": "tuvakoulutuksensuoritus",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            },
        |            "vahvistus": {
        |              "päivä": "2025-04-16"
        |            },
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "999908",
        |                "nimi": {
        |                  "fi": "Tutkintokoulutukseen valmentava koulutuksen suoritus"
        |                },
        |                "koodistoUri": "koulutus",
        |                "koodistoVersio": 12
        |              },
        |              "laajuus": {
        |                "arvo": 30.0,
        |                "yksikkö": {
        |                  "koodiarvo": "8",
        |                  "nimi": {
        |                    "fi": "viikkoa"
        |                  },
        |                  "koodistoUri": "opintojenlaajuusyksikko",
        |                  "koodistoVersio": 1
        |                }
        |              }
        |            }
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[Tuva]

    Assertions.assertNotNull(tuva.tunniste)
    Assertions.assertEquals(Some("Tutkintokoulutukseen valmentava koulutuksen suoritus"), tuva.nimi.fi)
    Assertions.assertEquals(Some("Stadin ammattiopisto"), tuva.oppilaitos.nimi.fi)
    Assertions.assertEquals(Koodi("999908", "koulutus", Some(12)), tuva.koodi)
    Assertions.assertEquals(Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), tuva.koskiTila)
    Assertions.assertEquals(Some(LocalDate.parse("2022-08-01")), tuva.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2025-04-16")), tuva.vahvistusPaivamaara)
    Assertions.assertEquals(Some(Laajuus(30, Koodi("8", "opintojenlaajuusyksikko", Some(1)), Some(Kielistetty(Some("viikkoa"), None, None)), None)), tuva.laajuus)

  @Test def testVapaaSivistysTyoOpiskeluoikeus(): Unit =
    val opiskeluoikeus = getFirstOpiskeluoikeusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.75291104630",
        |    "opiskeluoikeudet": [
        |      {
        |        "tyyppi": {
        |          "koodiarvo": "vapaansivistystyonkoulutus",
        |          "koodistoUri": "opiskeluoikeudentyyppi",
        |          "koodistoVersio": 1
        |        },
        |        "oid": "1.2.246.562.15.87456579967",
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.63029756333"
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2024-05-25",
        |              "tila": {
        |                "koodiarvo": "valmistunut",
        |                "koodistoUri": "koskiopiskeluoikeudentila",
        |                "koodistoVersio": 1,
        |                "nimi": {}
        |              }
        |            }
        |          ]
        |        },
        |        "suoritukset": []
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).get.asInstanceOf[GeneerinenOpiskeluoikeus]

    Assertions.assertNotNull(opiskeluoikeus.tunniste)
    Assertions.assertEquals(Koodi("vapaansivistystyonkoulutus", "opiskeluoikeudentyyppi", Some(1)), opiskeluoikeus.tyyppi)
    Assertions.assertEquals("1.2.246.562.15.87456579967", opiskeluoikeus.oid)
    Assertions.assertEquals(Some(OpiskeluoikeusTila(List(OpiskeluoikeusJakso(LocalDate.parse("2024-05-25"), KoskiKoodi("valmistunut", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None))))), opiskeluoikeus.tila)
    Assertions.assertEquals("1.2.246.562.10.63029756333", opiskeluoikeus.oppilaitosOid)

  @Test def testVapaaSivistysTyoSuoritus(): Unit =
    val vst = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.75291104630",
        |    "opiskeluoikeudet": [
        |      {
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.63029756333",
        |          "nimi": {
        |            "fi": "Lahden kansanopisto"
        |          }
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2024-05-25",
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
        |              "koodiarvo": "vstoppivelvollisillesuunnattukoulutus",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            },
        |            "vahvistus": {
        |              "päivä": "2025-04-16"
        |            },
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "999909",
        |                "nimi": {
        |                  "fi": "Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille"
        |                },
        |                "koodistoUri": "koulutus",
        |                "koodistoVersio": 12
        |              }
        |            },
        |            "suorituskieli": {
        |              "koodiarvo": "FI",
        |              "koodistoUri": "kieli",
        |              "koodistoVersio": 1
        |            },
        |            "osasuoritukset": [
        |              {
        |                "tyyppi": {
        |                  "koodiarvo": "vstosaamiskokonaisuus",
        |                  "nimi": {
        |                    "fi": "Oppivelvollisille suunnattu vapaan sivistystyön osaamiskokonaisuus",
        |                    "sv": "Kompetenshelhet inom fritt bildningsarbete som riktar sig till läropliktiga",
        |                    "en": "Oppivelvollisille suunnattu vapaan sivistystyön osaamiskokonaisuus"
        |                  },
        |                  "koodistoUri": "suorituksentyyppi",
        |                  "koodistoVersio": 1
        |                },
        |                "koulutusmoduuli": {
        |                  "laajuus": {
        |                    "arvo": 4.5,
        |                    "yksikkö": {
        |                      "koodiarvo": "2",
        |                      "nimi": {
        |                        "fi": "opintopistettä"
        |                      },
        |                      "lyhytNimi": {
        |                        "fi": "op"
        |                      },
        |                      "koodistoUri": "opintojenlaajuusyksikko",
        |                      "koodistoVersio": 1
        |                    }
        |                  }
        |                }
        |              }
        |            ]
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[VapaaSivistystyo]

    Assertions.assertNotNull(vst.tunniste)
    Assertions.assertEquals(Some("Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille"), vst.nimi.fi)
    Assertions.assertEquals(Some("Lahden kansanopisto"), vst.oppilaitos.nimi.fi)
    Assertions.assertEquals(Koodi("999909", "koulutus", Some(12)), vst.koodi)
    Assertions.assertEquals(Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), vst.koskiTila)
    Assertions.assertEquals(Some(LocalDate.parse("2024-05-25")), vst.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2025-04-16")), vst.vahvistusPaivamaara)
    Assertions.assertEquals(Some(Laajuus(4.5, Koodi("2", "opintojenlaajuusyksikko", Some(1)), Some(Kielistetty(Some("opintopistettä"), None, None)), Some(Kielistetty(Some("op"), None, None)))), vst.laajuus)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), vst.suoritusKieli)

  @Test def testMitatoidutOpiskeluoikeudetFiltteroidaan(): Unit =
    val opiskeluoikeus = getFirstOpiskeluoikeusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.75291104630",
        |    "opiskeluoikeudet": [
        |      {
        |        "tyyppi": {
        |          "koodiarvo": "vapaansivistystyonkoulutus",
        |          "koodistoUri": "opiskeluoikeudentyyppi",
        |          "koodistoVersio": 1
        |        },
        |        "oid": "1.2.246.562.15.87456579967",
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.63029756333"
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2024-05-25",
        |              "tila": {
        |                "koodiarvo": "mitatoity",
        |                "koodistoUri": "koskiopiskeluoikeudentila",
        |                "koodistoVersio": 1,
        |                "nimi": {}
        |              }
        |            }
        |          ]
        |        },
        |        "suoritukset": []
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin)
    
    Assertions.assertTrue(opiskeluoikeus.isEmpty)

  @Test def testParasArviointiAmmatillinen(): Unit = {
    val arvioinnit = Set(
      Arviointi(KoskiKoodi("4", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true),
      Arviointi(KoskiKoodi("2", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true),
      Arviointi(KoskiKoodi("Hyväksytty", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true)
    )
     Assertions.assertEquals(
       Some(Arviointi(KoskiKoodi("4", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true)),
       KoskiToSuoritusConverter.valitseParasArviointi(arvioinnit)
     )
  }

  @Test def testParasArviointiAmmatillinenSanalliset(): Unit = {
    val arvioinnit = Set(
      Arviointi(KoskiKoodi("Hylätty", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, false),
      Arviointi(KoskiKoodi("Hyväksytty", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true)
    )
    Assertions.assertEquals(
      Some(Arviointi(KoskiKoodi("Hyväksytty", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true)),
      KoskiToSuoritusConverter.valitseParasArviointi(arvioinnit)
    )
  }

  @Test def testParasArviointiPerusopetus(): Unit = {
    val arvioinnit = Set(
      Arviointi(KoskiKoodi("8", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true),
      Arviointi(KoskiKoodi("7", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true),
      Arviointi(KoskiKoodi("S", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true)
    )
    Assertions.assertEquals(
      Some(Arviointi(KoskiKoodi("8", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true)),
      KoskiToSuoritusConverter.valitseParasArviointi(arvioinnit)
    )
  }

  @Test def testParasArviointiPerusopetusSanalliset(): Unit = {
    val arvioinnit = Set(
      Arviointi(KoskiKoodi("O", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true),
      Arviointi(KoskiKoodi("S", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true)
    )
    Assertions.assertEquals(
      Some(Arviointi(KoskiKoodi("S", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true)),
      KoskiToSuoritusConverter.valitseParasArviointi(arvioinnit)
    )
  }
}
