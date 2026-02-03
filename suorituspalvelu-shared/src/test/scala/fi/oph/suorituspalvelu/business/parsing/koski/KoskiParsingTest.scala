package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{TELMA, TUVA, VAPAA_SIVISTYSTYO, VUOSILUOKKA_9}
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, Arvosana, EBArvosana, EBLaajuus, EBTutkinto, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, Lahtokoulu, LukionOppimaara, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, PerusopetuksenYksilollistaminen, PoistettuOpiskeluoikeus, Suoritus, SuoritusTila, Telma, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.parsing.koski
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiArviointi, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiKotiopetusjakso, KoskiLisatiedot, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila, KoskiParser, KoskiToSuoritusConverter}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

import java.io.ByteArrayInputStream
import java.time.LocalDate

@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiParsingTest {

  val DUMMY_KOODISTOPROVIDER: KoodistoProvider = koodisto => Map("AI" -> fi.oph.suorituspalvelu.integration.client.Koodi("", Koodisto(""), List.empty))

  @Test def testKoskiParsingAndConversion(): Unit =
    Seq(
      "/1_2_246_562_24_40483869857.json",
      "/1_2_246_562_24_30563266636.json"
    ).foreach(fileName => {
      val splitData = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream(fileName))
      splitData.foreach(henkilo => {
        henkilo.opiskeluoikeudet.foreach {
          case Right(opiskeluoikeus) => val koskiOpiskeluoikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(Seq(KoskiParser.parseKoskiData(opiskeluoikeus.data)), DUMMY_KOODISTOPROVIDER)
          case Left(exception) => Assertions.fail(exception)
        }
      })
    })

  @Test def testIsYTO(): Unit =
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106727"))
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106728"))
    Assertions.assertTrue(KoskiToSuoritusConverter.isYTO("106729"))

    Assertions.assertFalse(KoskiToSuoritusConverter.isYTO("123456"))
    Assertions.assertFalse(KoskiToSuoritusConverter.isYTO("234567"))

  private def getFirstOpiskeluoikeusFromJson(data: String): Option[Opiskeluoikeus] =
    val splitData = KoskiIntegration.splitKoskiDataByHenkilo(new ByteArrayInputStream(data.getBytes))
    splitData.flatMap(henkilo => {
      henkilo.opiskeluoikeudet.map {
        case Right(opiskeluoikeus) =>
          val koskiOpiskeluoikeus = KoskiParser.parseKoskiData(opiskeluoikeus.data)
          KoskiToSuoritusConverter.parseOpiskeluoikeudet(Seq(koskiOpiskeluoikeus), DUMMY_KOODISTOPROVIDER)
        case Left(exception) => Assertions.fail(exception)
      }
    }).next().headOption

  private def getFirstSuoritusFromJson(data: String): Suoritus =
    val splitData = KoskiIntegration.splitKoskiDataByHenkilo(new ByteArrayInputStream(data.getBytes))
    splitData.flatMap(henkilo => {
      henkilo.opiskeluoikeudet.map {
        case Right(opiskeluoikeus) =>
          val koskiOpiskeluoikeus = KoskiParser.parseKoskiData(opiskeluoikeus.data)
          KoskiToSuoritusConverter.toSuoritukset(Seq(koskiOpiskeluoikeus), DUMMY_KOODISTOPROVIDER, true)
        case Left(exception) => Assertions.fail(exception)
      }
    }).next().head

  @Test def testAmmatillisetOpiskeluoikeudet(): Unit =
    val opiskeluoikeus = getFirstOpiskeluoikeusFromJson(
      """
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
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
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
    Assertions.assertEquals(Some(KoskiOpiskeluoikeusTila(List(KoskiOpiskeluoikeusJakso(
      LocalDate.parse("2022-06-06"), KoskiKoodi("lasna", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None))))), opiskeluoikeus.tila)

  @Test def testPoistettuOpiskeluoikeus(): Unit =
    val opiskeluoikeus = getFirstOpiskeluoikeusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "oid": "1.2.246.562.15.24186343661",
        |        "versionumero": 5,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "poistettu": true
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).get.asInstanceOf[PoistettuOpiskeluoikeus]

    Assertions.assertEquals("1.2.246.562.15.24186343661", opiskeluoikeus.oid)

  @Test def testAmmatillisenTutkinnonTila(): Unit =
    // ammatillisen tutkinnon tila on ajallisesti viimeisen opiskeluoikeusjakson tila (vaikka alku olisi tulevaisuudessa)
    val tutkinto = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.40483869857",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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
    val tutkinto = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.40483869857",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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
    val tutkinto = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.40483869857",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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
    val tutkinto = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.40483869857",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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

  @Test def testTelma(): Unit = {
    val telma = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.21583363224",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.54019331674",
        |          "nimi": {
        |            "fi": "HAUS kehittämiskeskus Oy"
        |          }
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2022-06-06",
        |              "tila": {
        |                "koodiarvo": "lasna"
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
        |            },
        |            "osasuoritukset": []
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
    Assertions.assertEquals(LocalDate.parse("2022-06-06"), telma.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2023-03-15")), telma.vahvistusPaivamaara)
    Assertions.assertEquals(2023, telma.suoritusVuosi)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), telma.suoritusKieli)
    Assertions.assertEquals(Lahtokoulu(LocalDate.parse("2022-06-06"), Some(LocalDate.parse("2023-03-15")), "1.2.246.562.10.54019331674", Some(2023), TELMA.defaultLuokka.get, Some(VALMIS), None, TELMA), telma.lahtokoulu)
  }

  @Test def testTelmaOsasuoritukset(): Unit = {
    val fileName = "/telmaosasuoritukset.json"
    val splitData = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream(fileName)).toList
    val oikeudet: Seq[AmmatillinenOpiskeluoikeus] = splitData.flatMap(henkilo => {
      henkilo.opiskeluoikeudet.flatMap {
        case Right(opiskeluoikeus) =>
          val koskiOpiskeluoikeus = KoskiParser.parseKoskiData(opiskeluoikeus.data)
          KoskiToSuoritusConverter.parseOpiskeluoikeudet(Seq(koskiOpiskeluoikeus), DUMMY_KOODISTOPROVIDER)
            .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
            .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
        case Left(exception) => Assertions.fail(exception)
      }
    })

    val telmaSuoritus = oikeudet.head.suoritukset.head.asInstanceOf[Telma]
    val telmaLaajuus = telmaSuoritus.hyvaksyttyLaajuus.map(_.arvo)

    Assertions.assertEquals(oikeudet.size, 1)
    Assertions.assertEquals(oikeudet.head.suoritukset.size, 1)
    Assertions.assertEquals(telmaLaajuus.get, 60)
  }

  @Test def testPerusopetuksenOpiskeluoikeudet(): Unit =
    val opiskeluoikeus = getFirstOpiskeluoikeusFromJson(
      """
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
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
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
    Assertions.assertEquals(SuoritusTila.KESKEN, opiskeluoikeus.tila)
    Assertions.assertEquals(Some(KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(Some(true)))), Some(false),
      Some(List(KoskiKotiopetusjakso("2021-08-24", Some("2022-01-23")))))), opiskeluoikeus.lisatiedot)

  @Test def testPerusopetuksenOppimaarat(): Unit =
    val oppimaara = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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

  @Test def testPerusopetuksenOppimaaranOppiaineet(): Unit = {
    val oppimaara = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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
    Assertions.assertEquals(Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY), oppimaara.yksilollistaminen)
  }

  @Test def testPerusopetuksenOppimaaranLahtokoulut(): Unit =
    val oppimaara = getFirstSuoritusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.32727448402",
        |          "nimi": {
        |            "fi": "Hatsalan klassillinen koulu"
        |          }
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2021-06-02",
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
        |            "osasuoritukset": []
        |          },
        |          {
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "9",
        |                "nimi": {
        |                  "fi": "9. vuosiluokka"
        |                },
        |                "koodistoUri": "perusopetuksenluokkaaste",
        |                "koodistoVersio": 1
        |              }
        |            },
        |            "luokka": "9G",
        |            "alkamispäivä": "2020-08-15",
        |            "vahvistus": {
        |              "päivä": "2021-06-01"
        |            },
        |            "tyyppi": {
        |              "koodiarvo": "perusopetuksenvuosiluokka",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            },
        |            "osasuoritukset": []
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[PerusopetuksenOppimaara]

    Assertions.assertEquals(Set(Lahtokoulu(LocalDate.parse("2020-08-15"), Some(LocalDate.parse("2021-06-01")), "1.2.246.562.10.32727448402", Some(2021), "9G", Some(VALMIS), Some(true), VUOSILUOKKA_9)), oppimaara.lahtokoulut)

  @Test def testPerusopetuksenOppimaaranLahtokoulutEiAlkamispaivaa(): Unit =
    val oppimaara = getFirstSuoritusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.32727448402",
        |          "nimi": {
        |            "fi": "Hatsalan klassillinen koulu"
        |          }
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2021-06-02",
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
        |            "osasuoritukset": []
        |          },
        |          {
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "9",
        |                "nimi": {
        |                  "fi": "9. vuosiluokka"
        |                },
        |                "koodistoUri": "perusopetuksenluokkaaste",
        |                "koodistoVersio": 1
        |              }
        |            },
        |            "luokka": "9D",
        |            "vahvistus": {
        |              "päivä": "2021-06-01"
        |            },
        |            "tyyppi": {
        |              "koodiarvo": "perusopetuksenvuosiluokka",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            },
        |            "osasuoritukset": []
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[PerusopetuksenOppimaara]

    Assertions.assertEquals(Set.empty, oppimaara.lahtokoulut)

  @Test def testNuortenPerusopetuksenOppiaineenOppimaara(): Unit =
    val oppimaara = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.21583363224",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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
        |""".stripMargin).asInstanceOf[PerusopetuksenOppimaaranOppiaineidenSuoritus]

    Assertions.assertNotNull(oppimaara.tunniste)
    Assertions.assertTrue(oppimaara.versioTunniste.isEmpty)
    Assertions.assertEquals(Oppilaitos(Kielistetty(Some("oppilaitos"), None, None), "1.2.246.562.10.42923230215"), oppimaara.oppilaitos)
    Assertions.assertEquals(Kielistetty(Some("Matematiikka"), None, None), oppimaara.aineet.head.nimi)
    Assertions.assertEquals(Koodi("MA", "koskioppiaineetyleissivistava", Some(1)), oppimaara.aineet.head.koodi)
    Assertions.assertEquals(Koodi("8", "arviointiasteikkoyleissivistava", Some(1)), oppimaara.aineet.head.arvosana)
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
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
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
    Assertions.assertEquals(SuoritusTila.VALMIS, opiskeluoikeus.tila)
    Assertions.assertEquals(None, opiskeluoikeus.lisatiedot)

  @Test def testAikuistenPerusopetuksenOppimaarat(): Unit =
    val oppimaara = getFirstSuoritusFromJson("""
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.30563266636",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
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
    Assertions.assertEquals(Some(KoskiOpiskeluoikeusTila(List(KoskiOpiskeluoikeusJakso(LocalDate.parse("2025-04-16"), KoskiKoodi("valmistunut", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None))))), opiskeluoikeus.tila)
    Assertions.assertEquals("1.2.246.562.10.41945921983", opiskeluoikeus.oppilaitosOid)

  @Test def testTuvaSuoritus(): Unit =
    val tuva = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.21583363224",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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
        |              "päivä": "2023-04-16"
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
    Assertions.assertEquals(LocalDate.parse("2022-08-01"), tuva.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2023-04-16")), tuva.vahvistusPaivamaara)
    Assertions.assertEquals(Some(Laajuus(30, Koodi("8", "opintojenlaajuusyksikko", Some(1)), Some(Kielistetty(Some("viikkoa"), None, None)), None)), tuva.laajuus)
    Assertions.assertEquals(Lahtokoulu(LocalDate.parse("2022-08-01"), Some(LocalDate.parse("2023-04-16")), "1.2.246.562.10.41945921983", Some(2023), TUVA.defaultLuokka.get, Some(VALMIS), None, TUVA), tuva.lahtokoulu)


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
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
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
    Assertions.assertEquals(Some(KoskiOpiskeluoikeusTila(List(KoskiOpiskeluoikeusJakso(LocalDate.parse("2024-05-25"), KoskiKoodi("valmistunut", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None))))), opiskeluoikeus.tila)
    Assertions.assertEquals("1.2.246.562.10.63029756333", opiskeluoikeus.oppilaitosOid)

  @Test def testVapaaSivistysTyoSuoritus(): Unit =
    val vst = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.75291104630",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
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
    Assertions.assertEquals(LocalDate.parse("2024-05-25"), vst.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2025-04-16")), vst.vahvistusPaivamaara)
    Assertions.assertEquals(
      Some(Laajuus(4.5, Koodi("2", "opintojenlaajuusyksikko", Some(1)),
      Some(Kielistetty(Some("opintopistettä"), None, None)), Some(Kielistetty(Some("op"), None, None)))), vst.hyvaksyttyLaajuus)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), vst.suoritusKieli)
    Assertions.assertEquals(Lahtokoulu(LocalDate.parse("2024-05-25"), Some(LocalDate.parse("2025-04-16")), "1.2.246.562.10.63029756333", Some(2025), VAPAA_SIVISTYSTYO.defaultLuokka.get, Some(VALMIS), None, VAPAA_SIVISTYSTYO), vst.lahtokoulu)

  @Test def testMitatoituOpiskeluoikeusPalautetaanPoistettunaOpiskeluoikeutena(): Unit =
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
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
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
        |""".stripMargin).get.asInstanceOf[PoistettuOpiskeluoikeus]

    Assertions.assertEquals("1.2.246.562.15.87456579967", opiskeluoikeus.oid)

  @Test def testParasArviointiAmmatillinen(): Unit = {
    val arvioinnit = Set(
      KoskiArviointi(KoskiKoodi("4", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true),
      KoskiArviointi(KoskiKoodi("2", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true),
      KoskiArviointi(KoskiKoodi("Hyväksytty", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true)
    )
     Assertions.assertEquals(
       Some(KoskiArviointi(KoskiKoodi("4", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true)),
       KoskiToSuoritusConverter.valitseParasArviointi(arvioinnit)
     )
  }

  @Test def testParasArviointiAmmatillinenSanalliset(): Unit = {
    val arvioinnit = Set(
      KoskiArviointi(KoskiKoodi("Hylätty", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, false),
      KoskiArviointi(KoskiKoodi("Hyväksytty", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true)
    )
    Assertions.assertEquals(
      Some(KoskiArviointi(KoskiKoodi("Hyväksytty", "arviointiasteikkoammatillinen15", None, Kielistetty(None, None, None), None), None, true)),
      KoskiToSuoritusConverter.valitseParasArviointi(arvioinnit)
    )
  }

  @Test def testParasArviointiPerusopetus(): Unit = {
    val arvioinnit = Set(
      KoskiArviointi(KoskiKoodi("8", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true),
      KoskiArviointi(KoskiKoodi("7", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true),
      KoskiArviointi(KoskiKoodi("S", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true)
    )
    Assertions.assertEquals(
      Some(KoskiArviointi(KoskiKoodi("8", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true)),
      KoskiToSuoritusConverter.valitseParasArviointi(arvioinnit)
    )
  }

  @Test def testParasArviointiPerusopetusSanalliset(): Unit = {
    val arvioinnit = Set(
      KoskiArviointi(KoskiKoodi("O", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true),
      KoskiArviointi(KoskiKoodi("S", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true)
    )
    Assertions.assertEquals(
      Some(KoskiArviointi(KoskiKoodi("S", "arviointiasteikkoyleissivistava", None, Kielistetty(None, None, None), None), None, true)),
      KoskiToSuoritusConverter.valitseParasArviointi(arvioinnit)
    )
  }

  @Test def testEBTutkinto(): Unit = {
    val tutkinto = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.35986177022",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 127,
        |        "aikaleima": "2024-09-12T15:12:40.365225",
        |        "oid": "1.2.246.562.15.50478693398",
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.73383452575",
        |          "nimi": {
        |            "fi": "European School of Helsinki",
        |            "sv": "Europaskolan i Helsingfors",
        |            "en": "European School of Helsinki"
        |          }
        |        },
        |        "tila": {
        |          "opiskeluoikeusjaksot": [
        |            {
        |              "alku": "2022-08-15",
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
        |              "koodiarvo": "ebtutkinto",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            },
        |            "koulutusmoduuli": {
        |              "tunniste": {
        |                "koodiarvo": "301103",
        |                "nimi": {
        |                  "fi": "Eurooppalainen ylioppilastutkinto (EB)",
        |                  "sv": "Europeisk studentexamen (EB)",
        |                  "en": "European Baccalaureate (EB)"
        |                },
        |                "koodistoUri": "koulutus",
        |                "koodistoVersio": 12
        |              }
        |            },
        |            "vahvistus": {
        |               "päivä": "2023-06-30"
        |            },
        |            "osasuoritukset": [
        |              {
        |                "koulutusmoduuli": {
        |                  "tunniste": {
        |                    "koodiarvo": "L1",
        |                    "nimi": {
        |                      "fi": "Ensimmäinen kieli (L1)",
        |                      "sv": "Första språket (L1)",
        |                      "en": "First Language (L1)"
        |                    },
        |                    "koodistoUri": "eboppiaineet",
        |                    "koodistoVersio": 1
        |                  },
        |                  "laajuus": {
        |                    "arvo": 4.0,
        |                    "yksikkö": {
        |                      "koodiarvo": "4",
        |                      "nimi": {
        |                        "fi": "periodia"
        |                      },
        |                      "koodistoUri": "opintojenlaajuusyksikko",
        |                      "koodistoVersio": 1
        |                    }
        |                  }
        |                },
        |                "suorituskieli": {
        |                  "koodiarvo": "FI",
        |                  "koodistoUri": "kieli",
        |                  "koodistoVersio": 1
        |                },
        |                "osasuoritukset": [
        |                  {
        |                    "koulutusmoduuli": {
        |                      "tunniste": {
        |                        "koodiarvo": "Written",
        |                        "nimi": {
        |                          "fi": "Kirjallinen koe",
        |                          "sv": "Skriftligt förhör",
        |                          "en": "Written examination"
        |                        },
        |                        "koodistoUri": "ebtutkinnonoppiaineenkomponentti",
        |                        "koodistoVersio": 1
        |                      }
        |                    },
        |                    "arviointi": [
        |                      {
        |                        "arvosana": {
        |                          "koodiarvo": "9.0",
        |                          "nimi": {
        |                            "fi": "9,0"
        |                          },
        |                          "koodistoUri": "arviointiasteikkoeuropeanschoolofhelsinkifinalmark",
        |                          "koodistoVersio": 1
        |                        },
        |                        "hyväksytty": true
        |                      }
        |                    ]
        |                  },
        |                  {
        |                    "koulutusmoduuli": {
        |                      "tunniste": {
        |                        "koodiarvo": "Oral",
        |                        "nimi": {
        |                          "fi": "Suullinen koe",
        |                          "sv": "Muntligt förhör",
        |                          "en": "Oral examination"
        |                        },
        |                        "koodistoUri": "ebtutkinnonoppiaineenkomponentti",
        |                        "koodistoVersio": 1
        |                      }
        |                    },
        |                    "arviointi": [
        |                      {
        |                        "arvosana": {
        |                          "koodiarvo": "8.5",
        |                          "nimi": {
        |                            "fi": "8,5"
        |                          },
        |                          "koodistoUri": "arviointiasteikkoeuropeanschoolofhelsinkifinalmark",
        |                          "koodistoVersio": 1
        |                        },
        |                        "hyväksytty": true
        |                      }
        |                    ]
        |                  },
        |                  {
        |                    "koulutusmoduuli": {
        |                      "tunniste": {
        |                        "koodiarvo": "Final",
        |                        "nimi": {
        |                          "fi": "Lopullinen arvosana",
        |                          "sv": "Slutligt vitsord",
        |                          "en": "Final mark"
        |                        },
        |                        "koodistoUri": "ebtutkinnonoppiaineenkomponentti",
        |                        "koodistoVersio": 1
        |                      }
        |                    },
        |                    "arviointi": [
        |                      {
        |                        "arvosana": {
        |                          "koodiarvo": "9.0",
        |                          "nimi": {
        |                            "fi": "9,0"
        |                          },
        |                          "koodistoUri": "arviointiasteikkoeuropeanschoolofhelsinkifinalmark",
        |                          "koodistoVersio": 1
        |                        },
        |                        "hyväksytty": true
        |                      }
        |                    ]
        |                  }
        |                ]
        |              },
        |              {
        |                "koulutusmoduuli": {
        |                  "tunniste": {
        |                    "koodiarvo": "L2",
        |                    "nimi": {
        |                      "fi": "Toinen kieli (L2)",
        |                      "sv": "Andra språket (L2)",
        |                      "en": "Second Language (L2)"
        |                    },
        |                    "koodistoUri": "eboppiaineet",
        |                    "koodistoVersio": 1
        |                  },
        |                  "laajuus": {
        |                    "arvo": 3.0,
        |                    "yksikkö": {
        |                      "koodiarvo": "4",
        |                      "nimi": {
        |                        "fi": "periodia"
        |                      },
        |                      "koodistoUri": "opintojenlaajuusyksikko",
        |                      "koodistoVersio": 1
        |                    }
        |                  }
        |                },
        |                "suorituskieli": {
        |                  "koodiarvo": "EN",
        |                  "koodistoUri": "kieli",
        |                  "koodistoVersio": 1
        |                },
        |                "osasuoritukset": [
        |                  {
        |                    "koulutusmoduuli": {
        |                      "tunniste": {
        |                        "koodiarvo": "Final",
        |                        "nimi": {
        |                          "fi": "Lopullinen arvosana",
        |                          "sv": "Slutligt vitsord",
        |                          "en": "Final mark"
        |                        },
        |                        "koodistoUri": "ebtutkinnonoppiaineenkomponentti",
        |                        "koodistoVersio": 1
        |                      }
        |                    },
        |                    "arviointi": [
        |                      {
        |                        "arvosana": {
        |                          "koodiarvo": "8.0",
        |                          "nimi": {
        |                            "fi": "8,0"
        |                          },
        |                          "koodistoUri": "arviointiasteikkoeuropeanschoolofhelsinkifinalmark",
        |                          "koodistoVersio": 1
        |                        },
        |                        "hyväksytty": true
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
        |""".stripMargin).asInstanceOf[EBTutkinto]

    // Test basic tutkinto properties
    Assertions.assertNotNull(tutkinto.tunniste)
    Assertions.assertEquals(Koodi("301103", "koulutus", Some(12)), tutkinto.koodi)
    Assertions.assertEquals(Kielistetty(Some("Eurooppalainen ylioppilastutkinto (EB)"), Some("Europeisk studentexamen (EB)"), Some("European Baccalaureate (EB)")), tutkinto.nimi)
    Assertions.assertEquals(Oppilaitos(Kielistetty(Some("European School of Helsinki"), Some("Europaskolan i Helsingfors"), Some("European School of Helsinki")), "1.2.246.562.10.73383452575"), tutkinto.oppilaitos)
    Assertions.assertEquals(Some(LocalDate.parse("2022-08-15")), tutkinto.aloitusPaivamaara)
    Assertions.assertEquals(Some(LocalDate.parse("2023-06-30")), tutkinto.vahvistusPaivamaara)

    Assertions.assertEquals(2, tutkinto.osasuoritukset.size)

    // Test L1 oppiaine
    val l1 = tutkinto.osasuoritukset.find(_.koodi.arvo == "L1").get
    Assertions.assertEquals(Koodi("L1", "eboppiaineet", Some(1)), l1.koodi)
    Assertions.assertEquals(Kielistetty(Some("Ensimmäinen kieli (L1)"), Some("Första språket (L1)"), Some("First Language (L1)")), l1.nimi)
    Assertions.assertEquals(Some(EBLaajuus(4.0, Koodi("4", "opintojenlaajuusyksikko", Some(1)))), l1.laajuus)
    Assertions.assertEquals(Koodi("FI", "kieli", Some(1)), l1.suorituskieli)

    // Test L1 osasuoritukset
    Assertions.assertEquals(3, l1.osasuoritukset.size)

    // Test Written osasuoritus
    val writtenKoe = l1.osasuoritukset.find(_.koodi.arvo == "Written").get
    Assertions.assertEquals(Koodi("Written", "ebtutkinnonoppiaineenkomponentti", Some(1)), writtenKoe.koodi)
    Assertions.assertEquals(Kielistetty(Some("Kirjallinen koe"), Some("Skriftligt förhör"), Some("Written examination")), writtenKoe.nimi)
    Assertions.assertEquals(EBArvosana(Koodi("9.0", "arviointiasteikkoeuropeanschoolofhelsinkifinalmark", Some(1)), true), writtenKoe.arvosana)

    // Test Oral osasuoritus
    val oralKoe = l1.osasuoritukset.find(_.koodi.arvo == "Oral").get
    Assertions.assertEquals(Koodi("Oral", "ebtutkinnonoppiaineenkomponentti", Some(1)), oralKoe.koodi)
    Assertions.assertEquals(Kielistetty(Some("Suullinen koe"), Some("Muntligt förhör"), Some("Oral examination")), oralKoe.nimi)
    Assertions.assertEquals(EBArvosana(Koodi("8.5", "arviointiasteikkoeuropeanschoolofhelsinkifinalmark", Some(1)), true), oralKoe.arvosana)

    // Test Final osasuoritus
    val finalKoeL1 = l1.osasuoritukset.find(_.koodi.arvo == "Final").get
    Assertions.assertEquals(Koodi("Final", "ebtutkinnonoppiaineenkomponentti", Some(1)), finalKoeL1.koodi)
    Assertions.assertEquals(Kielistetty(Some("Lopullinen arvosana"), Some("Slutligt vitsord"), Some("Final mark")), finalKoeL1.nimi)
    Assertions.assertEquals(EBArvosana(Koodi("9.0", "arviointiasteikkoeuropeanschoolofhelsinkifinalmark", Some(1)), true), finalKoeL1.arvosana)

    // Test L2 oppiaine
    val l2 = tutkinto.osasuoritukset.find(_.koodi.arvo == "L2").get
    Assertions.assertEquals(Koodi("L2", "eboppiaineet", Some(1)), l2.koodi)
    Assertions.assertEquals(Kielistetty(Some("Toinen kieli (L2)"), Some("Andra språket (L2)"), Some("Second Language (L2)")), l2.nimi)
    Assertions.assertEquals(Some(EBLaajuus(3.0, Koodi("4", "opintojenlaajuusyksikko", Some(1)))), l2.laajuus)
    Assertions.assertEquals(Koodi("EN", "kieli", Some(1)), l2.suorituskieli)

    // Test L2 osasuoritukset
    Assertions.assertEquals(1, l2.osasuoritukset.size)

    // Test Final osasuoritus for L2
    val finalKoeL2 = l2.osasuoritukset.head
    Assertions.assertEquals(Koodi("Final", "ebtutkinnonoppiaineenkomponentti", Some(1)), finalKoeL2.koodi)
    Assertions.assertEquals(Kielistetty(Some("Lopullinen arvosana"), Some("Slutligt vitsord"), Some("Final mark")), finalKoeL2.nimi)
    Assertions.assertEquals(EBArvosana(Koodi("8.0", "arviointiasteikkoeuropeanschoolofhelsinkifinalmark", Some(1)), true), finalKoeL2.arvosana)

  }

  @Test def testLukionOppimaara(): Unit =
    val lukionOppimaara = getFirstSuoritusFromJson(
      """
        |[
        |  {
        |    "oppijaOid": "1.2.246.562.24.24698338212",
        |    "opiskeluoikeudet": [
        |      {
        |        "versionumero": 299,
        |        "aikaleima": "2025-11-25T08:57:51.633360",
        |        "oid": "1.2.246.562.15.99636591200",
        |        "tyyppi": {
        |          "koodiarvo": "lukiokoulutus",
        |          "koodistoUri": "opiskeluoikeudentyyppi",
        |          "koodistoVersio": 1
        |        },
        |        "oppilaitos": {
        |          "oid": "1.2.246.562.10.57118763579",
        |          "nimi": {
        |            "fi": "Testin lukio",
        |            "sv": "Test gymnasium",
        |            "en": "Test high school"
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
        |            },
        |            {
        |              "alku": "2025-05-31",
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
        |              "koodiarvo": "lukionoppimaara",
        |              "koodistoUri": "suorituksentyyppi",
        |              "koodistoVersio": 1
        |            },
        |            "vahvistus": {
        |              "päivä": "2025-05-31"
        |            },
        |            "koulusivistyskieli": [
        |              {
        |                "koodiarvo": "FI",
        |                "koodistoUri": "kieli"
        |              }
        |            ]
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin).asInstanceOf[LukionOppimaara]

    Assertions.assertEquals("1.2.246.562.10.57118763579", lukionOppimaara.oppilaitos.oid)
    Assertions.assertEquals(Kielistetty(Some("Testin lukio"), Some("Test gymnasium"), Some("Test high school")), lukionOppimaara.oppilaitos.nimi)
    Assertions.assertEquals(Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), lukionOppimaara.koskiTila)
    Assertions.assertEquals(SuoritusTila.VALMIS, lukionOppimaara.supaTila)
    Assertions.assertEquals(Some(LocalDate.parse("2025-05-31")), lukionOppimaara.vahvistusPaivamaara)
    Assertions.assertEquals(None, lukionOppimaara.suoritusKieli)
    Assertions.assertEquals(Set(Koodi("FI", "kieli", None)), lukionOppimaara.koulusivistyskieli)


}
