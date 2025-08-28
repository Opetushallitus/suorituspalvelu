package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, Koodi, Opiskeluoikeus}
import fi.oph.suorituspalvelu.resource.ApiConstants.EXAMPLE_SYNTYMAIKA
import fi.oph.suorituspalvelu.resource.ui.SuoritusTapa.NAYTTOTUTKINTO
import fi.oph.suorituspalvelu.resource.ui.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.ui.UIService.{EXAMPLE_HETU, EXAMPLE_NIMI, EXAMPLE_OPPIJA_OID}

import java.time.LocalDate
import java.util.{Optional, UUID}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

object MockEntityToUIConverter {

  def getOpiskeluoikeudet(): List[UIOpiskeluoikeus] =
    List(
      UIOpiskeluoikeus(
        tunniste = UUID.randomUUID(),
        nimi = UIOpiskeluoikeusNimi(
          fi = Optional.of("Kasvatust. maist., kasvatustiede"),
          sv = Optional.of("Kasvatust. maist., kasvatustiede sv"),
          en = Optional.of("Kasvatust. maist., kasvatustiede en")
        ),
        oppilaitos = OOOppilaitos(
          nimi = OOOppilaitosNimi(
            fi = Optional.of("Tampereen yliopisto"),
            sv = Optional.of("Tampereen yliopisto sv"),
            en = Optional.of("Tampereen yliopisto en")
          ),
          oid = "1.2.3.4"
        ),
        voimassaolonAlku = LocalDate.parse("2001-08-01"),
        voimassaolonLoppu = LocalDate.parse("2025-12-11"),
        OpiskeluoikeusTila.VOIMASSA,
        UIOpiskeluoikeusVirtaTila(
          Optional.of("aktiivinen"),
          Optional.of("aktiv"),
          Optional.of("active")
        )
      ))

  def getKKTutkinnot(): List[KKSuoritus] =
    List(KKSuoritus(
      tunniste = UUID.randomUUID(),
      nimi = KKSuoritusNimi(
        fi = Optional.of("Kasvatust. maist., kasvatustiede"),
        sv = Optional.of("Kasvatust. maist., kasvatustiede sv"),
        en = Optional.of("Kasvatust. maist., kasvatustiede en")
      ),
      oppilaitos = KKOppilaitos(
        nimi = KKOppilaitosNimi(
          fi = Optional.of("Tampereen yliopisto"),
          sv = Optional.of("Tampereen yliopisto sv"),
          en = Optional.of("Tampereen yliopisto en")
        ),
        oid = "1.2.3.4"
      ),
      tila = KESKEN,
      aloituspaiva = Optional.of(LocalDate.parse("2025-12-11")),
      valmistumispaiva = Optional.empty()
    ))

  def getYOTutkinto(): Option[YOTutkinto] =
    Some(YOTutkinto(
      tunniste = UUID.randomUUID(),
      nimi = YOTutkintoNimi(
        fi = Optional.of("Ylioppilastutkinto"),
        sv = Optional.of("Ylioppilastutkinto sv"),
        en = Optional.of("Ylioppilastutkinto en")
      ),
      oppilaitos = YOOppilaitos(
        nimi = YOOppilaitosNimi(
          fi = Optional.of("Ylioppilastutkintolautakunta"),
          sv = Optional.of("Ylioppilastutkintolautakunta sv"),
          en = Optional.of("Ylioppilastutkintolautakunta en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2019-06-01")),
      suorituskieli = "suomi",
      yoKokeet = java.util.List.of(YOKoe(
        tunniste = UUID.randomUUID(),
        aine = "Psykologia",
        taso = "Ainemuotoinen reaali",
        arvosana = "E",
        yhteispistemaara = 28,
        tutkintokerta = LocalDate.parse("2018-12-21")
      ), YOKoe(
        tunniste = UUID.randomUUID(),
        aine = "Englanti",
        taso = "Pitkä oppimäärä (KIELI)",
        arvosana = "E",
        yhteispistemaara = 259,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ), YOKoe(
        tunniste = UUID.randomUUID(),
        aine = "Matematiikka",
        taso = "Lyhyt oppimäärä (MA)",
        arvosana = "C",
        yhteispistemaara = 23,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ), YOKoe(
        tunniste = UUID.randomUUID(),
        aine = "Suomi",
        taso = "Äidinkieli",
        arvosana = "C",
        yhteispistemaara = 49,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ), YOKoe(
        tunniste = UUID.randomUUID(),
        aine = "Historia",
        taso = "Ainemuotoinen reaali",
        arvosana = "M",
        yhteispistemaara = 25,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ), YOKoe(
        tunniste = UUID.randomUUID(),
        aine = "Yhteiskuntaoppi",
        taso = "Ainemuotoinen reaali",
        arvosana = "E",
        yhteispistemaara = 32,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ))
    ))

  def getLukionOppimaara(): Option[LukionOppimaara] =
    Some(LukionOppimaara(
      tunniste = UUID.randomUUID(),
      nimi = LukionOppimaaraNimi(
        fi = Optional.of("Lukion oppimäärä"),
        sv = Optional.of("Lukion oppimäärä sv"),
        en = Optional.of("Lukion oppimäärä en")
      ),
      oppilaitos = YOOppilaitos(
        nimi = YOOppilaitosNimi(
          fi = Optional.of("Ylioppilastutkintolautakunta"),
          sv = Optional.of("Ylioppilastutkintolautakunta sv"),
          en = Optional.of("Ylioppilastutkintolautakunta en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(LukionOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = LukionOppiaineNimi(
          fi = Optional.of("Äidinkieli ja kirjallisuus"),
          sv = Optional.of("Äidinkieli ja kirjallisuus sv"),
          en = Optional.of("Äidinkieli ja kirjallisuus en")
        )
      ),LukionOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = LukionOppiaineNimi(
          fi = Optional.of("Uskonto/Elämänkatsomustieto"),
          sv = Optional.of("Uskonto/Elämänkatsomustieto sv"),
          en = Optional.of("Uskonto/Elämänkatsomustieto en")
        )
      ))
    ))

  def getLukionOppiaineenOppimaarat(): List[LukionOppiaineenOppimaara] =
    List(LukionOppiaineenOppimaara(
      tunniste = UUID.randomUUID(),
      nimi = LukionOppiaineenOppimaaraNimi(
        fi = Optional.of("Lukion oppiaineen oppimäärä"),
        sv = Optional.of("Lukion oppiaineen oppimäärä sv"),
        en = Optional.of("Lukion oppiaineen oppimäärä en")
      ),
      oppilaitos = YOOppilaitos(
        nimi = YOOppilaitosNimi(
          fi = Optional.of("Ylioppilastutkintolautakunta"),
          sv = Optional.of("Ylioppilastutkintolautakunta sv"),
          en = Optional.of("Ylioppilastutkintolautakunta en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(LukionOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = LukionOppiaineNimi(
          fi = Optional.of("Äidinkieli ja kirjallisuus, suomi äidinkielenä"),
          sv = Optional.of("Äidinkieli ja kirjallisuus, suomi äidinkielenä sv"),
          en = Optional.of("Äidinkieli ja kirjallisuus, suomi äidinkielenä en")
        )
      ), LukionOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = LukionOppiaineNimi(
          fi = Optional.of("Matematiikka, lyhyt oppimäärä, valinnainen"),
          sv = Optional.of("Matematiikka, lyhyt oppimäärä, valinnainen sv"),
          en = Optional.of("Matematiikka, lyhyt oppimäärä, valinnainen en")
        )
      ))
    ))

  def getDiaTutkinto(): Option[DIATutkinto] =
    Some(DIATutkinto(
      tunniste = UUID.randomUUID(),
      nimi = DIATutkintoNimi(
        fi = Optional.of("DIA-tutkinto"),
        sv = Optional.of("DIA-tutkinto sv"),
        en = Optional.of("DIA-tutkinto en")
      ),
      oppilaitos = YOOppilaitos(
        nimi = YOOppilaitosNimi(
          fi = Optional.of("Ylioppilastutkintolautakunta"),
          sv = Optional.of("Ylioppilastutkintolautakunta sv"),
          en = Optional.of("Ylioppilastutkintolautakunta en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
    ))

  def getDiaVastaavuusTodistus(): Option[DIAVastaavuusTodistus] =
    Some(DIAVastaavuusTodistus(
      tunniste = UUID.randomUUID(),
      nimi = DIAVastaavuusTodistusNimi(
        fi = Optional.of("DIA-vastaavuustodistus"),
        sv = Optional.of("DIA-vastaavuustodistus sv"),
        en = Optional.of("DIA-vastaavuustodistus en")
      ),
      oppilaitos = YOOppilaitos(
        nimi = YOOppilaitosNimi(
          fi = Optional.of("Ylioppilastutkintolautakunta"),
          sv = Optional.of("Ylioppilastutkintolautakunta sv"),
          en = Optional.of("Ylioppilastutkintolautakunta en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      kieletKirjallisuusTaide = java.util.List.of(DIAOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = DIAOppiaineNimi(
          fi = Optional.of("A-kieli, englanti"),
          sv = Optional.of("A-kieli, englanti sv"),
          en = Optional.of("A-kieli, englanti en")
        ),
        laajuus = 3,
        keskiarvo = 8.5
      ), DIAOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = DIAOppiaineNimi(
          fi = Optional.of("Historia"),
          sv = Optional.of("Historia sv"),
          en = Optional.of("Historia en")
        ),
        laajuus = 2,
        keskiarvo = 8.5
      )),
      matematiikkaLuonnontieteet = java.util.List.of(DIAOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = DIAOppiaineNimi(
          fi = Optional.of("Matematiikka"),
          sv = Optional.of("Matematiikka sv"),
          en = Optional.of("Matematiikka en")
        ),
        laajuus = 3,
        keskiarvo = 6
      ), DIAOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = DIAOppiaineNimi(
          fi = Optional.of("Kuvataide"),
          sv = Optional.of("Kuvataide sv"),
          en = Optional.of("Kuvataide en")
        ),
        laajuus = 3,
        keskiarvo = 8.5
      ))
    ))

  def getEBTutkinto(): Option[EBTutkinto] =
    Some(EBTutkinto(
      tunniste = UUID.randomUUID(),
      nimi = EBTutkintoNimi(
        fi = Optional.of("EB-tutkinto"),
        sv = Optional.of("EB-tutkinto sv"),
        en = Optional.of("EB-tutkinto en")
      ),
      oppilaitos = YOOppilaitos(
        nimi = YOOppilaitosNimi(
          fi = Optional.of("Ylioppilastutkintolautakunta"),
          sv = Optional.of("Ylioppilastutkintolautakunta sv"),
          en = Optional.of("Ylioppilastutkintolautakunta en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(EBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = EBOppiaineNimi(
          fi = Optional.of("Mathematics fi"),
          sv = Optional.of("Mathematics sv"),
          en = Optional.of("Mathematics")
        ),
        suorituskieli = "englanti",
        laajuus = 4,
        written = EBSuoritus(
          tunniste = UUID.randomUUID(),
          arvosana = 8.67,
        ),
        oral = Optional.empty(),
        `final` = EBSuoritus(
          tunniste = UUID.randomUUID(),
          arvosana = 8.67,
        )
      ), EBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = EBOppiaineNimi(
          fi = Optional.of("First language, ranska fi"),
          sv = Optional.of("First language, ranska sv"),
          en = Optional.of("First language, ranska en")
        ),
        suorituskieli = "englanti",
        laajuus = 3,
        written = EBSuoritus(
          tunniste = UUID.randomUUID(),
          arvosana = 8.67,
        ),
        oral = Optional.of(EBSuoritus(
          tunniste = UUID.randomUUID(),
          arvosana = 8.67,
        )),
        `final` = EBSuoritus(
          tunniste = UUID.randomUUID(),
          arvosana = 8.67,
        )
      ), EBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = EBOppiaineNimi(
          fi = Optional.of("Second language, saksa fi"),
          sv = Optional.of("Second language, saksa sv"),
          en = Optional.of("Second language, saksa en")
        ),
        suorituskieli = "englanti",
        laajuus = 3,
        written = EBSuoritus(
          tunniste = UUID.randomUUID(),
          arvosana = 8.67,
        ),
        oral = Optional.of(EBSuoritus(
          tunniste = UUID.randomUUID(),
          arvosana = 8.67,
        )),
        `final` = EBSuoritus(
          tunniste = UUID.randomUUID(),
          arvosana = 8.67,
        )
      ))
    ))

  def getIBTutkinto(): Option[IBTutkinto] =
    Some(IBTutkinto(
      tunniste = UUID.randomUUID(),
      nimi = IBTutkintoNimi(
        fi = Optional.of("IB-tutkinto"),
        sv = Optional.of("IB-tutkinto sv"),
        en = Optional.of("IB-tutkinto en")
      ),
      oppilaitos = YOOppilaitos(
        nimi = YOOppilaitosNimi(
          fi = Optional.of("Ylioppilastutkintolautakunta"),
          sv = Optional.of("Ylioppilastutkintolautakunta sv"),
          en = Optional.of("Ylioppilastutkintolautakunta en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(IBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = IBOppiaineNimi(
          fi = Optional.of("Studies in language and literature fi"),
          sv = Optional.of("Studies in language and literature sv"),
          en = Optional.of("Studies in language and literature en")
        ),
        suoritukset = java.util.List.of(IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = IBSuoritusNimi(
            fi = Optional.of("Language A: literature, suomi fi"),
            sv = Optional.of("Language A: literature, suomi sv"),
            en = Optional.of("Language A: literature, suomi en")
          ),
          laajuus = 9,
          predictedGrade = Some(7),
          arvosana = 7
        ), IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = IBSuoritusNimi(
            fi = Optional.of("Language A: language and literature, englanti fi"),
            sv = Optional.of("Language A: language and literature, englanti sv"),
            en = Optional.of("Language A: language and literature, englanti en")
          ),
          laajuus = 6,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ), IBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = IBOppiaineNimi(
          fi = Optional.of("Individuals and societies fi"),
          sv = Optional.of("Individuals and societies sv"),
          en = Optional.of("Individuals and societies en")
        ),
        suoritukset = java.util.List.of(IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = IBSuoritusNimi(
            fi = Optional.of("History fi"),
            sv = Optional.of("History sv"),
            en = Optional.of("History en")
          ),
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ), IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = IBSuoritusNimi(
            fi = Optional.of("Psychology fi"),
            sv = Optional.of("Psychology sv"),
            en = Optional.of("Psychology en")
          ),
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ), IBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = IBOppiaineNimi(
          fi = Optional.of("Experimental sciences fi"),
          sv = Optional.of("Experimental sciences sv"),
          en = Optional.of("Experimental sciences en")
        ),
        suoritukset = java.util.List.of(IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = IBSuoritusNimi(
            fi = Optional.of("Biology fi"),
            sv = Optional.of("Biology sv"),
            en = Optional.of("Biology en")
          ),
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ), IBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = IBOppiaineNimi(
          fi = Optional.of("Mathematics fi"),
          sv = Optional.of("Mathematics sv"),
          en = Optional.of("Mathematics en")
        ),
        suoritukset = java.util.List.of(IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = IBSuoritusNimi(
            fi = Optional.of("Mathematical studies fi"),
            sv = Optional.of("Mathematical studies sv"),
            en = Optional.of("Mathematical studies en")
          ),
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ))
    ))

  def getPreIB(): Option[PreIB] =
    Some(PreIB(
      tunniste = UUID.randomUUID(),
      nimi = PreIBNimi(
        fi = Optional.of("Pre-IB"),
        sv = Optional.of("Pre-IB sv"),
        en = Optional.of("Pre-IB en")
      ),
      oppilaitos = YOOppilaitos(
        nimi = YOOppilaitosNimi(
          fi = Optional.of("Ylioppilastutkintolautakunta"),
          sv = Optional.of("Ylioppilastutkintolautakunta sv"),
          en = Optional.of("Ylioppilastutkintolautakunta en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
    ))

  def getAmmatillisetPerusTutkinnot(): List[Ammatillinentutkinto] =
    List(
      Ammatillinentutkinto(
        tunniste = UUID.randomUUID(),
        nimi = AmmatillinentutkintoNimi(
          fi = Optional.of("Puutarha-alan perustutkinto"),
          sv = Optional.of("Puutarha-alan perustutkinto sv"),
          en = Optional.of("Puutarha-alan perustutkinto en")
        ),
        oppilaitos = AmmatillinenOppilaitos(
          nimi = AmmatillinenOppilaitosNimi(
            fi = Optional.of("Hämeen ammatti-instituutti, Lepaa"),
            sv = Optional.of("Hämeen ammatti-instituutti, Lepaa sv"),
            en = Optional.of("Hämeen ammatti-instituutti, Lepaa en")
          ),
          oid = "1.2.3.4"
        ),
        tila = VALMIS,
        aloituspaiva = Optional.of(LocalDate.parse("2024-12-31")),
        valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
        suorituskieli = "suomi",
        painotettuKeskiarvo = Optional.of(4.34),
        ytot = java.util.List.of(YTO(
          tunniste = UUID.randomUUID(),
          nimi = YTONimi(
            fi = Optional.of("Viestintä- ja vuorovaikutusosaaminen"),
            sv = Optional.of("Kunnande i kommunikation och interaktion"),
            en = Optional.of("Communication and interaction competence"),
          ),
          laajuus = Optional.of(11),
          arvosana = Optional.of(
            YTOArvosana(
              fi = Optional.of("Hyväksytty"),
              sv = Optional.of("Godkänd"),
              en = Optional.of("Pass")
            )
          ),
          java.util.List.of(
            YTOOsaAlue(
              nimi = YTOOsaAlueNimi(
                fi = Optional.of("Viestintä ja vuorovaikutus äidinkielellä"),
                sv = Optional.of("Kommunikation och interaktion på modersmålet"),
                en = Optional.of("Communication and interaction in mother tongue")
              ),
              laajuus = Optional.of(4),
              arvosana = Optional.of("1")
            ),
            YTOOsaAlue(
              nimi = YTOOsaAlueNimi(
                fi = Optional.of("Viestintä ja vuorovaikutus toisella kotimaisella kielellä"),
                sv = Optional.of("Kommunikation och interaktion på det andra inhemska språket"),
                en = Optional.of("Communication and interaction in the second national language")
              ),
              laajuus = Optional.of(1),
              arvosana = Optional.of("1")
            ),
            YTOOsaAlue(
              nimi = YTOOsaAlueNimi(
                fi = Optional.of("Toiminta digitaalisessa ympäristössä"),
                sv = Optional.of("Verksamhet i den digitala miljön"),
                en = Optional.of("Operating in a digital environment")
              ),
              laajuus = Optional.of(1),
              arvosana = Optional.of("1")
            )
          )
        ), YTO(
          tunniste = UUID.randomUUID(),
          nimi = YTONimi(
            fi = Optional.of("Matemaattis-luonnontieteellinen osaaminen"),
            sv = Optional.of("Matemaattis-luonnontieteellinen osaaminen sv"),
            en = Optional.of("Matemaattis-luonnontieteellinen osaaminen en")
          ),
          laajuus = Optional.of(11),
          arvosana = Optional.of(
            YTOArvosana(
              fi = Optional.of("Hyväksytty"),
              sv = Optional.of("Godkänd"),
              en = Optional.of("Pass")
            )
          ),
          java.util.List.of()
        ), YTO(
          tunniste = UUID.randomUUID(),
          nimi = YTONimi(
            fi = Optional.of("Yhteiskunta- ja työelämäosaaminen"),
            sv = Optional.of("Yhteiskunta- ja työelämäosaaminen sv"),
            en = Optional.of("Yhteiskunta- ja työelämäosaaminen en")
          ),
          laajuus = Optional.of(11),
          arvosana = Optional.of(
            YTOArvosana(
              fi = Optional.of("Hyväksytty"),
              sv = Optional.of("Godkänd"),
              en = Optional.of("Pass")
            )
          ),
          java.util.List.of()
        )),
        ammatillisenTutkinnonOsat = java.util.List.of(
          AmmatillisenTutkinnonOsa(
            tunniste = UUID.randomUUID(),
            nimi = AmmatillisenTutkinnonOsaNimi(
              fi = Optional.of("Audiovisuaalisen kulttuurin perusteet"),
              sv = Optional.of("Audiovisuaalisen kulttuurin perusteet sv"),
              en = Optional.of("Audiovisuaalisen kulttuurin perusteet en")
            ),
            laajuus = Optional.of(11),
            arvosana = Optional.of("4"),
            osaAlueet = java.util.List.of(
              AmmatillisenTutkinnonOsaAlue(
                nimi = AmmatillisenTutkinnonOsaAlueNimi(
                  fi = Optional.of("Audiovisuaalisen kulttuurin perusteet 1"),
                  sv = Optional.of("Audiovisuaalisen kulttuurin perusteet 1 sv"),
                  en = Optional.of("Audiovisuaalisen kulttuurin perusteet 1 en")
                ),
                laajuus = Optional.of(2),
                arvosana = Optional.of("1")
              ),
              AmmatillisenTutkinnonOsaAlue(
                nimi = AmmatillisenTutkinnonOsaAlueNimi(
                  fi = Optional.of("Audiovisuaalisen kulttuurin perusteet 2"),
                  sv = Optional.of("Audiovisuaalisen kulttuurin perusteet 2 sv"),
                  en = Optional.of("Audiovisuaalisen kulttuurin perusteet 2 en")
                ),
                laajuus = Optional.of(3),
                arvosana = Optional.of("1")
              )
            )
          ),
          AmmatillisenTutkinnonOsa(
            tunniste = UUID.randomUUID(),
            nimi = AmmatillisenTutkinnonOsaNimi(
              fi = Optional.of("Äänimaailman suunnittelu"),
              sv = Optional.of("Äänimaailman suunnittelu sv"),
              en = Optional.of("Äänimaailman suunnittelu en")
            ),
            laajuus = Optional.of(11),
            arvosana = Optional.of("4"),
            osaAlueet = java.util.List.of()
          )
        ),
        suoritustapa = Optional.empty()
      ), Ammatillinentutkinto(
        tunniste = UUID.randomUUID(),
        nimi = AmmatillinentutkintoNimi(
          fi = Optional.of("Hevostalouden perustutkinto"),
          sv = Optional.of("Hevostalouden perustutkinto sv"),
          en = Optional.of("Hevostalouden perustutkinto en")
        ),
        oppilaitos = AmmatillinenOppilaitos(
          nimi = AmmatillinenOppilaitosNimi(
            fi = Optional.of("Hämeen ammatti-instituutti, Lepaa"),
            sv = Optional.of("Hämeen ammatti-instituutti, Lepaa sv"),
            en = Optional.of("Hämeen ammatti-instituutti, Lepaa en")
          ),
          oid = "1.2.3.4"
        ),
        tila = VALMIS,
        aloituspaiva = Optional.of(LocalDate.parse("2024-12-31")),
        valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
        suorituskieli = "suomi",
        painotettuKeskiarvo = Optional.of(4.34),
        ytot = java.util.List.of(),
        ammatillisenTutkinnonOsat = java.util.List.of(),
        suoritustapa = Optional.of(SuoritusTapa.NAYTTOTUTKINTO)
      )
    )

  def getAmmattitutkinnot(): List[Ammattitutkinto] =
    List(Ammattitutkinto(
      tunniste = UUID.randomUUID(),
      nimi = AmmattitutkintoNimi(
        fi = Optional.of("Maanmittausalan ammattitutkinto"),
        sv = Optional.of("Maanmittausalan ammattitutkinto sv"),
        en = Optional.of("Maanmittausalan ammattitutkinto en")
      ),
      oppilaitos = AmmatillinenOppilaitos(
        nimi = AmmatillinenOppilaitosNimi(
          fi = Optional.of("Hämeen ammatti-instituutti, Lepaa"),
          sv = Optional.of("Hämeen ammatti-instituutti, Lepaa sv"),
          en = Optional.of("Hämeen ammatti-instituutti, Lepaa en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2017-06-01")),
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      suorituskieli = "suomi"
    ))

  def getErikoisAmmattitutkinnot(): List[Erikoisammattitutkinto] =
    List(Erikoisammattitutkinto(
      tunniste = UUID.randomUUID(),
      nimi = ErikoisammattitutkintoNimi(
        fi = Optional.of("Talous- ja henkilöstöalan erikoisammattitutkinto"),
        sv = Optional.of("Talous- ja henkilöstöalan erikoisammattitutkinto sv"),
        en = Optional.of("Talous- ja henkilöstöalan erikoisammattitutkinto en")
      ),
      oppilaitos = AmmatillinenOppilaitos(
        nimi = AmmatillinenOppilaitosNimi(
          fi = Optional.of("Hämeen ammatti-instituutti, Lepaa"),
          sv = Optional.of("Hämeen ammatti-instituutti, Lepaa sv"),
          en = Optional.of("Hämeen ammatti-instituutti, Lepaa en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2017-06-01")),
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      suorituskieli = "suomi"
    ))

  def getTelmat(): List[Telma] =
    List(Telma(
      nimi = TelmaNimi(
        fi = Optional.of("Työhön ja itsenäiseen elämään valmentava koulutus"),
        sv = Optional.of("Utbildning som handleder för arbete och ett självständigt liv"),
        en = Optional.of("Preparatory education for work and independent living")
      ),
      tunniste = UUID.randomUUID(),
      oppilaitos = AmmatillinenOppilaitos(
        nimi = AmmatillinenOppilaitosNimi(
          fi = Optional.of("Hämeen ammatti-instituutti, Lepaa"),
          sv = Optional.of("Hämeen ammatti-instituutti, Lepaa sv"),
          en = Optional.of("Hämeen ammatti-instituutti, Lepaa en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2017-06-01")),
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      suorituskieli = "suomi"
    ))

  def getTuvat(): List[Tuva] =
    List(Tuva(
      tunniste = UUID.randomUUID(),
      nimi = TuvaNimi(
        fi = Optional.of("Tutkintokoulutukseen valmentava koulutus"),
        sv = Optional.of("Utbildning som handleder för examensutbildning"),
        en = Optional.of("Preparatory education for an upper secondary qualification")
      ),
      oppilaitos = AmmatillinenOppilaitos(
        nimi = AmmatillinenOppilaitosNimi(
          fi = Optional.of("Hämeen ammatti-instituutti, Lepaa"),
          sv = Optional.empty(),
          en = Optional.empty()
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      laajuus = Optional.of(TuvaLaajuus(38, TuvaLaajuusYksikko(Optional.of("vk"), Optional.empty(), Optional.empty())))
    ))

  def getVapaaSivistystyoKoulutukset(): List[VapaaSivistystyoKoulutus] =
    List(VapaaSivistystyoKoulutus(
      tunniste = UUID.randomUUID(),
      nimi = VapaaSivistystyoKoulutusNimi(
        fi = Optional.of("Vapaan sivistystyön koulutus"),
        sv = Optional.of("Fritt bildningsarbete"),
        en = Optional.of("Liberal adult education")
      ),
      oppilaitos = VapaaSivistystyoOppilaitos(
        nimi = VapaaSivistystyoOppilaitosNimi(
          fi = Optional.of("Hämeen ammatti-instituutti, Lepaa"),
          sv = Optional.empty(),
          en = Optional.empty()
        ),
        oid = "1.2.3.4"
      ),
      tila = KESKEYTYNYT,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.empty(),
      suorituskieli = "suomi",
      laajuus = Optional.of(VapaaSivistystyoLaajuus(38, VapaaSivistystyoLaajuusYksikko(Optional.of("op"), Optional.empty(), Optional.empty())))
    ))

  def getPerusopetuksenOppimaarat(): List[PerusopetuksenOppimaara] =
    List(PerusopetuksenOppimaara(
      tunniste = UUID.randomUUID(),
      nimi = PerusopetuksenOppimaaraNimi(
        fi = Optional.of("Perusopetuksen oppimäärä"),
        sv = Optional.of("Grundläggande utbildningens lärokurs"),
        en = Optional.of("Basic education syllabus")
      ),
      oppilaitos = PKOppilaitos(
        nimi = PKOppilaitosNimi(
          fi = Optional.of("Keltinmäen koulu"),
          sv = Optional.of("Keltinmäen koulu sv"),
          en = Optional.of("Keltinmäen koulu en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2015-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      luokka = "9A",
      yksilollistetty = false,
      oppiaineet = java.util.List.of(
        PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus"),
            sv = Optional.of("Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus sv"),
            en = Optional.of("Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("A1-kieli, englanti"),
            sv = Optional.of("A1-kieli, englanti sv"),
            en = Optional.of("A1-kieli, englanti en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("B1-kieli, ruotsi"),
            sv = Optional.of("B1-kieli, ruotsi sv"),
            en = Optional.of("B1-kieli, ruotsi en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.of("S"),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("B2-kieli, saksa"),
            sv = Optional.of("B2-kieli, saksa sv"),
            en = Optional.of("B2-kieli, saksa en")
          ),
          arvosana = Optional.empty(),
          valinnainen = Optional.of("S"),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Matematiikka"),
            sv = Optional.of("Matematiikka sv"),
            en = Optional.of("Matematiikka en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Biologia"),
            sv = Optional.of("Biologia sv"),
            en = Optional.of("Biologia en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Maantieto"),
            sv = Optional.of("Maantieto sv"),
            en = Optional.of("Maantieto en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Fysiikka"),
            sv = Optional.of("Fysiikka sv"),
            en = Optional.of("Fysiikka en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Kemia"),
            sv = Optional.of("Kemia sv"),
            en = Optional.of("Fysiikka en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Terveystieto"),
            sv = Optional.of("Terveystieto sv"),
            en = Optional.of("Terveystieto en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Uskonto tai elämänkatsomustieto"),
            sv = Optional.of("Uskonto tai elämänkatsomustieto sv"),
            en = Optional.of("Uskonto tai elämänkatsomustieto en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        )
      )
    ))

  def getPerusopetuksenOppimaarat78Luokkalaiset(): Option[PerusopetuksenOppimaara78Luokkalaiset] =
    Some(PerusopetuksenOppimaara78Luokkalaiset(
      tunniste = UUID.randomUUID(),
      nimi = PerusopetuksenOppimaara78LuokkalaisetNimi(
        fi = Optional.of("Perusopetuksen oppimäärä"),
        sv = Optional.of("Grundläggande utbildningens lärokurs"),
        en = Optional.of("Basic education syllabus")
      ),
      oppilaitos = PKOppilaitos(
        nimi = PKOppilaitosNimi(
          fi = Optional.of("Keltinmäen koulu"),
          sv = Optional.of("Keltinmäen koulu sv"),
          en = Optional.of("Keltinmäen koulu en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2015-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      koulusivistyskieli = "suomi",
      luokka = "9A",
      yksilollistetty = false
    ))

  def getNuortenPerusopetuksenOppiaineenOppimaarat(): List[NuortenPerusopetuksenOppiaineenOppimaara] =
    List(NuortenPerusopetuksenOppiaineenOppimaara(
      tunniste = UUID.randomUUID(),
      nimi = NuortenPerusopetuksenOppiaineenOppimaaraNimi(
        fi = Optional.of("Nuorten perusopetuksen oppiaineen oppimäärä"),
        sv = Optional.of("Lärokurs i ett läroämne i grundläggande utbildning sv"),
        en = Optional.of("Basic education for youth subject syllabus en")
      ),
      oppilaitos = PKOppilaitos(
        nimi = PKOppilaitosNimi(
          fi = Optional.of("Keltinmäen koulu"),
          sv = Optional.of("Keltinmäen koulu sv"),
          en = Optional.of("Keltinmäen koulu en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2015-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(OppimaaranOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = OppimaaranOppiaineNimi(
          fi = Optional.of("Biologia"),
          sv = Optional.of("Biologia sv"),
          en = Optional.of("Biologia en")
        ),
        arvosana = 9
      ),OppimaaranOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = OppimaaranOppiaineNimi(
          fi = Optional.of("Historia"),
          sv = Optional.of("Historia sv"),
          en = Optional.of("Historia en")
        ),
        arvosana = 8
      ))
    ))

  def getPerusopetuksenOppiaineenOppimaarat(): List[PerusopetuksenOppiaineenOppimaara] =
    List(PerusopetuksenOppiaineenOppimaara(
      tunniste = UUID.randomUUID(),
      nimi = PerusopetuksenOppiaineenOppimaaraNimi(
        fi = Optional.of("Perusopetuksen oppiaineen oppimäärä"),
        sv = Optional.of("Lärokurs i ett läroämne i grundläggande utbildning"),
        en = Optional.of("Basic education subject syllabus")
      ),
      oppilaitos = PKOppilaitos(
        nimi = PKOppilaitosNimi(
          fi = Optional.of("Keltinmäen koulu"),
          sv = Optional.of("Keltinmäen koulu sv"),
          en = Optional.of("Keltinmäen koulu en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2015-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(OppimaaranOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = OppimaaranOppiaineNimi(
          fi = Optional.of("matematiikka"),
          sv = Optional.of("matematiikka sv"),
          en = Optional.of("matematiikka en")
        ),
        arvosana = 9
      ))
    ))

  def getAikuistenPerusopetuksetOppimaarat(): List[AikuistenPerusopetuksenOppimaara] =
    List(AikuistenPerusopetuksenOppimaara(
      tunniste = UUID.randomUUID(),
      nimi = AikuistenPerusopetuksenOppimaaraNimi(
        fi = Optional.of("Aikuisten perusopetuksen oppimäärä"),
        sv = Optional.of("Lärokurs i den grundläggande utbildningen för vuxna"),
        en = Optional.of("Preparatory instruction and lower secondary education for adults syllabus")
      ),
      oppilaitos = PKOppilaitos(
        nimi = PKOppilaitosNimi(
          fi = Optional.of("Keltinmäen koulu"),
          sv = Optional.of("Keltinmäen koulu sv"),
          en = Optional.of("Keltinmäen koulu en")
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2015-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(
        PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus"),
            sv = Optional.of("Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus sv"),
            en = Optional.of("Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("A1-kieli, englanti"),
            sv = Optional.of("A1-kieli, englanti sv"),
            en = Optional.of("A1-kieli, englanti en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("B1-kieli, ruotsi"),
            sv = Optional.of("B1-kieli, ruotsi sv"),
            en = Optional.of("B1-kieli, ruotsi en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("B2-kieli, saksa"),
            sv = Optional.of("B2-kieli, saksa sv"),
            en = Optional.of("B2-kieli, saksa en")
          ),
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Matematiikka"),
            sv = Optional.of("Matematiikka sv"),
            en = Optional.of("Matematiikka en")
          ),
          arvosana = Optional.empty(),
          valinnainen = Optional.of("10"),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Biologia"),
            sv = Optional.of("Biologia sv"),
            en = Optional.of("Biologia en")
          ),
          arvosana = Optional.empty(),
          valinnainen = Optional.of("9"),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Maantieto"),
            sv = Optional.of("Maantieto sv"),
            en = Optional.of("Maantieto en")
          ),
          arvosana = Optional.empty(),
          valinnainen = Optional.of("8"),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = PerusopetuksenOppiaineNimi(
            fi = Optional.of("Fysiikka"),
            sv = Optional.of("Fysiikka sv"),
            en = Optional.of("Fysiikka en")
          ),
          arvosana = Optional.empty(),
          valinnainen = Optional.of("9"),
        )
      )
    ))

  def getOppijanTiedot(): Option[OppijanTiedotSuccessResponse] = {
    Some(OppijanTiedotSuccessResponse(
      // TODO: oppijan tietojen osalta pitää päättää haetaanko reaaliaikaisesti ONR:stä vai miten toimitaan
      nimi =                                      EXAMPLE_NIMI,
      henkiloTunnus =                             EXAMPLE_HETU,
      syntymaAika =                               LocalDate.parse(EXAMPLE_SYNTYMAIKA),
      oppijaNumero =                              EXAMPLE_OPPIJA_OID,
      henkiloOID =                                EXAMPLE_OPPIJA_OID,
      opiskeluoikeudet =                          getOpiskeluoikeudet().asJava,
      kkTutkinnot =                               getKKTutkinnot().asJava,
      yoTutkinto =                                getYOTutkinto().toJava,
      lukionOppimaara =                           getLukionOppimaara().toJava,
      lukionOppiaineenOppimaarat =                getLukionOppiaineenOppimaarat().asJava,
      diaTutkinto =                               getDiaTutkinto().toJava,
      diaVastaavuusTodistus =                     getDiaVastaavuusTodistus().toJava,
      ebTutkinto =                                getEBTutkinto().toJava,
      ibTutkinto =                                getIBTutkinto().toJava,
      preIB =                                     getPreIB().toJava,
      ammatillisetPerusTutkinnot =                getAmmatillisetPerusTutkinnot().asJava,
      ammattitutkinnot =                          getAmmattitutkinnot().asJava,
      erikoisammattitutkinnot =                   getErikoisAmmattitutkinnot().asJava,
      telmat =                                    getTelmat().asJava,
      tuvat =                                     getTuvat().asJava,
      vapaaSivistystyoKoulutukset =               getVapaaSivistystyoKoulutukset().asJava,
      perusopetuksenOppimaarat =                  getPerusopetuksenOppimaarat().asJava,
      perusopetuksenOppimaara78Luokkalaiset =     getPerusopetuksenOppimaarat78Luokkalaiset().toJava,
      nuortenPerusopetuksenOppiaineenOppimaarat = getNuortenPerusopetuksenOppiaineenOppimaarat().asJava,
      perusopetuksenOppiaineenOppimaarat =        getPerusopetuksenOppiaineenOppimaarat().asJava,
      aikuistenPerusopetuksenOppimaarat =         getAikuistenPerusopetuksetOppimaarat().asJava
    ))
  }
}
