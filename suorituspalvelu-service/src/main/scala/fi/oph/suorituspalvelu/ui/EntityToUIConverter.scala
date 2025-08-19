package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, GeneerinenOpiskeluoikeus, Koodi, Opiskeluoikeus}
import fi.oph.suorituspalvelu.resource.ApiConstants.EXAMPLE_SYNTYMAIKA
import fi.oph.suorituspalvelu.resource.ui.SuoritusTapa.NAYTTOTUTKINTO
import fi.oph.suorituspalvelu.resource.ui.Tila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.resource.ui.{AikuistenPerusopetuksenOppimaara, AikuistenPerusopetuksenOppimaaraNimi, AmmatillinenOppilaitos, AmmatillinenOppilaitosNimi, Ammatillinentutkinto, AmmatillinentutkintoNimi, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, AmmatillisenTutkinnonOsaAlueNimi, AmmatillisenTutkinnonOsaNimi, Ammattitutkinto, AmmattitutkintoNimi, DIAOppiaine, DIAOppiaineNimi, DIATutkinto, DIATutkintoNimi, DIAVastaavuusTodistus, DIAVastaavuusTodistusNimi, EBOppiaine, EBOppiaineNimi, EBSuoritus, EBTutkinto, EBTutkintoNimi, Erikoisammattitutkinto, ErikoisammattitutkintoNimi, Hakukohde, HakukohdeNimi, IBOppiaine, IBOppiaineNimi, IBSuoritus, IBSuoritusNimi, IBTutkinto, IBTutkintoNimi, KKOppilaitos, KKOppilaitosNimi, KKSuoritus, KKSuoritusNimi, LukionOppiaine, LukionOppiaineNimi, LukionOppiaineenOppimaara, LukionOppiaineenOppimaaraNimi, LukionOppimaara, LukionOppimaaraNimi, NuortenPerusopetuksenOppiaineenOppimaara, NuortenPerusopetuksenOppiaineenOppimaaraNimi, OOOppilaitos, OOOppilaitosNimi, OppijanTiedotSuccessResponse, OppimaaranOppiaine, OppimaaranOppiaineNimi, PKOppilaitos, PKOppilaitosNimi, PerusopetuksenOppiaine, PerusopetuksenOppiaineNimi, PerusopetuksenOppiaineenOppimaara, PerusopetuksenOppiaineenOppimaaraNimi, PerusopetuksenOppimaara, PerusopetuksenOppimaara78Luokkalaiset, PerusopetuksenOppimaara78LuokkalaisetNimi, PerusopetuksenOppimaaraNimi, PreIB, PreIBNimi, Telma, TelmaNimi, Tila, Tuva, TuvaLaajuus, TuvaLaajuusYksikko, TuvaNimi, UIOpiskeluoikeus, UIOpiskeluoikeusNimi, VapaaSivistystyoKoulutus, VapaaSivistystyoKoulutusNimi, VapaaSivistystyoLaajuus, VapaaSivistystyoLaajuusYksikko, VapaaSivistystyoOppilaitos, VapaaSivistystyoOppilaitosNimi, YOKoe, YOOppilaitos, YOOppilaitosNimi, YOTutkinto, YOTutkintoNimi, YTO, YTOArvosana, YTONimi, YTOOsaAlue, YTOOsaAlueNimi}
import fi.oph.suorituspalvelu.ui.UIService.{EXAMPLE_HETU, EXAMPLE_NIMI, EXAMPLE_OPPIJA_OID}

import java.time.LocalDate
import java.util.{Optional, UUID}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

object EntityToUIConverter {

  def convertTila(koodi: Koodi): Tila =
    koodi.arvo match
      case "hyvaksytystisuoritettu"     => Tila.VALMIS
      case "valmistunut"                => Tila.VALMIS
      case "loma"                       => Tila.KESKEN
      case "lasna"                      => Tila.KESKEN
      case "eronnut"                    => Tila.KESKEYTYNYT
      case "katsotaaneronneeksi"        => Tila.KESKEYTYNYT
      case "keskeytynyt"                => Tila.KESKEYTYNYT
      case "valiaikaisestikeskeytynyt"  => Tila.KESKEYTYNYT
      case "mitatoity"                  => Tila.MITATOITY
      case "peruutettu"                 => Tila.PERUUTETTU
      case "paattynyt"                  => Tila.PAATTYNYT

  def getOpiskeluoikeudet(opiskeluoikeudet: Set[Opiskeluoikeus]): List[UIOpiskeluoikeus] =
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
        voimassaolonLoppu = LocalDate.parse("2025-12-11")
      ))

  def getKKTutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[KKSuoritus] =
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
      valmistumispaiva = Optional.empty(),
      hakukohde = Hakukohde(
        nimi = HakukohdeNimi(
          fi = Optional.of("Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)"),
          sv = Optional.of("Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v) sv"),
          en = Optional.of("Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v) en")
        )
      ),
    ))

  def getYOTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[YOTutkinto] =
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

  def getLukionOppimaara(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[LukionOppimaara] =
    Some(LukionOppimaara(
      tunniste = UUID.randomUUID(),
      nimi = LukionOppimaaraNimi(
        fi = Optional.of("Lukion oppimaara"),
        sv = Optional.of("Lukion oppimaara sv"),
        en = Optional.of("Lukion oppimaara en")
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

  def getLukionOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[LukionOppiaineenOppimaara] =
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

  def getDiaTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[DIATutkinto] =
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

  def getDiaVastaavuusTodistus(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[DIAVastaavuusTodistus] =
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

  def getEBTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[EBTutkinto] =
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

  def getIBTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[IBTutkinto] =
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

  def getPreIB(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[PreIB] =
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

  def getAmmatillisetPerusTutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Ammatillinentutkinto] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.suoritukset)
      .flatten
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.AmmatillinenPerustutkinto])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.AmmatillinenPerustutkinto])
      .map(t => {
        // Jos koskesta ei tule arvosanoja, kyseessä näyttötutkinto
        // 
        val nayttoTutkinto = !t.osat.exists(osa => osa.arvosana.isDefined)
        Ammatillinentutkinto(
          t.tunniste,
          nimi = AmmatillinentutkintoNimi(
            t.nimi.fi.toJava,
            t.nimi.sv.toJava,
            t.nimi.en.toJava
          ),
          oppilaitos = AmmatillinenOppilaitos(
            AmmatillinenOppilaitosNimi(
              t.oppilaitos.nimi.fi.toJava,
              t.oppilaitos.nimi.sv.toJava,
              t.oppilaitos.nimi.en.toJava
            ),
            t.oppilaitos.oid,
          ),
          tila = convertTila(t.tila),
          aloituspaiva = t.aloitusPaivamaara.toJava,
          valmistumispaiva = t.vahvistusPaivamaara.toJava,
          suorituskieli = t.suoritusKieli.arvo,
          painotettuKeskiarvo = t.keskiarvo.toJava,
          ytot = t.osat
            .filter(o => o.yto)
            .map(o => YTO(
              o.tunniste,
              nimi = YTONimi(
                fi = o.nimi.fi.toJava,
                sv = o.nimi.sv.toJava,
                en = o.nimi.en.toJava
              ),
              vahvistuspaiva = o.arviointiPaiva.toJava,
              laajuus = o.laajuus.map(l => l.arvo).toJava,
              arvosana = o.arvosana.map(a => YTOArvosana(
                a.nimi.fi.toJava,
                a.nimi.sv.toJava,
                a.nimi.en.toJava
              )).toJava,
              osaAlueet = o.osaAlueet.map(oa => YTOOsaAlue(
                nimi = YTOOsaAlueNimi(
                  fi = oa.nimi.fi.toJava,
                  sv = oa.nimi.sv.toJava,
                  en = oa.nimi.en.toJava
                ),
                laajuus = oa.laajuus.map(l => l.arvo).toJava,
                arvosana = oa.arvosana.map(a => a.arvo).toJava
              )).toList.asJava
            )).toList.asJava,
          ammatillisenTutkinnonOsat = if(t.suoritustapa.arvo!="ops") java.util.List.of() else t.osat
            .filter(o => !o.yto)
            .map(o => AmmatillisenTutkinnonOsa(
              o.tunniste,
              nimi = AmmatillisenTutkinnonOsaNimi(
                o.nimi.fi.toJava,
                o.nimi.sv.toJava,
                o.nimi.en.toJava
              ),
              laajuus = o.laajuus.map(l => l.arvo).toJava,
              arvosana = o.arvosana.map(a => a.koodi.arvo).toJava,
              osaAlueet = o.osaAlueet.map(oa => AmmatillisenTutkinnonOsaAlue(
                nimi = AmmatillisenTutkinnonOsaAlueNimi(
                  fi = oa.nimi.fi.toJava,
                  sv = oa.nimi.sv.toJava,
                  en = oa.nimi.en.toJava
                ),
                laajuus = oa.laajuus.map(l => l.arvo).toJava,
                arvosana = oa.arvosana.map(a => a.arvo).toJava
              )).toList.asJava
            )).toList.asJava,
          suoritustapa = if (nayttoTutkinto) Optional.of(NAYTTOTUTKINTO) else Optional.empty()
        )
      }).toList

  def getAmmattitutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Ammattitutkinto] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.suoritukset)
      .flatten
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.AmmattiTutkinto])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.AmmattiTutkinto])
      .map(t => {
        Ammattitutkinto(
          t.tunniste,
          nimi = AmmattitutkintoNimi(
            t.nimi.fi.toJava,
            t.nimi.sv.toJava,
            t.nimi.en.toJava
          ),
          oppilaitos = AmmatillinenOppilaitos(
            AmmatillinenOppilaitosNimi(
              t.oppilaitos.nimi.fi.toJava,
              t.oppilaitos.nimi.sv.toJava,
              t.oppilaitos.nimi.en.toJava
            ),
            t.oppilaitos.oid,
          ),
          tila = convertTila(t.tila),
          aloituspaiva = t.aloitusPaivamaara.toJava,
          valmistumispaiva = t.vahvistusPaivamaara.toJava,
          suorituskieli = t.suoritusKieli.arvo
        )
      }).toList

  def getErikoisAmmattitutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Erikoisammattitutkinto] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.suoritukset)
      .flatten
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.ErikoisAmmattiTutkinto])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.ErikoisAmmattiTutkinto])
      .map(t => {
        Erikoisammattitutkinto(
          t.tunniste,
          nimi = ErikoisammattitutkintoNimi(
            t.nimi.fi.toJava,
            t.nimi.sv.toJava,
            t.nimi.en.toJava
          ),
          oppilaitos = AmmatillinenOppilaitos(
            AmmatillinenOppilaitosNimi(
              t.oppilaitos.nimi.fi.toJava,
              t.oppilaitos.nimi.sv.toJava,
              t.oppilaitos.nimi.en.toJava
            ),
            t.oppilaitos.oid,
          ),
          tila = convertTila(t.tila),
          aloituspaiva = t.aloitusPaivamaara.toJava,
          valmistumispaiva = t.vahvistusPaivamaara.toJava,
          suorituskieli = t.suoritusKieli.arvo,
        )
      }).toList

  def getTelmat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Telma] = {
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.suoritukset)
      .flatten
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.Telma])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.Telma])
      .map(t => {
        Telma(
          t.tunniste,
          nimi = TelmaNimi(
            t.nimi.fi.toJava,
            t.nimi.sv.toJava,
            t.nimi.en.toJava
          ),
          oppilaitos = AmmatillinenOppilaitos(
            AmmatillinenOppilaitosNimi(
              t.oppilaitos.nimi.fi.toJava,
              t.oppilaitos.nimi.sv.toJava,
              t.oppilaitos.nimi.en.toJava
            ),
            t.oppilaitos.oid,
          ),
          tila = convertTila(t.tila),
          aloituspaiva = t.aloitusPaivamaara.toJava,
          valmistumispaiva = t.vahvistusPaivamaara.toJava,
          suorituskieli = t.suoritusKieli.arvo,
        )
      }).toList
    }

    def getTuvat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Tuva] =
      opiskeluoikeudet
        .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
        .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
        .map(o => o.suoritukset)
        .flatten
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.Tuva])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.Tuva])
        .map(t => {
          Tuva(
            t.tunniste,
            nimi = TuvaNimi(
              t.nimi.fi.toJava,
              t.nimi.sv.toJava,
              t.nimi.en.toJava
            ),
            oppilaitos = AmmatillinenOppilaitos(
              AmmatillinenOppilaitosNimi(
                t.oppilaitos.nimi.fi.toJava,
                t.oppilaitos.nimi.sv.toJava,
                t.oppilaitos.nimi.en.toJava
              ),
              t.oppilaitos.oid,
            ),
            tila = convertTila(t.tila),
            aloituspaiva = t.aloitusPaivamaara.toJava,
            valmistumispaiva = t.vahvistusPaivamaara.toJava,
            laajuus = t.laajuus.map(l => TuvaLaajuus(l.arvo, TuvaLaajuusYksikko(
              l.lyhytNimi.get.fi.toJava,
              l.lyhytNimi.get.sv.toJava,
              l.lyhytNimi.get.en.toJava
            ))).toJava,
          )
        }).toList

    def getVapaaSivistystyoKoulutukset(opiskeluoikeudet: Set[Opiskeluoikeus]): List[VapaaSivistystyoKoulutus] =
      opiskeluoikeudet
        .filter(o => o.isInstanceOf[GeneerinenOpiskeluoikeus])
        .map(o => o.asInstanceOf[GeneerinenOpiskeluoikeus])
        .map(o => o.suoritukset)
        .flatten
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.VapaaSivistystyo])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.VapaaSivistystyo])
        .map(t => {
          VapaaSivistystyoKoulutus(
            t.tunniste,
            nimi = VapaaSivistystyoKoulutusNimi(
              t.nimi.fi.toJava,
              t.nimi.sv.toJava,
              t.nimi.en.toJava
            ),
            oppilaitos = VapaaSivistystyoOppilaitos(
              VapaaSivistystyoOppilaitosNimi(
                t.oppilaitos.nimi.fi.toJava,
                t.oppilaitos.nimi.sv.toJava,
                t.oppilaitos.nimi.en.toJava
              ),
              t.oppilaitos.oid,
            ),
            tila = convertTila(t.tila),
            aloituspaiva = t.aloitusPaivamaara.toJava,
            valmistumispaiva = t.vahvistusPaivamaara.toJava,
            laajuus = t.laajuus.map(l => VapaaSivistystyoLaajuus(l.arvo, VapaaSivistystyoLaajuusYksikko(
              l.lyhytNimi.get.fi.toJava,
              l.lyhytNimi.get.sv.toJava,
              l.lyhytNimi.get.en.toJava
            ))).toJava,
            suorituskieli = t.suoritusKieli.arvo
          )
        }).toList

  def getPerusopetuksenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[PerusopetuksenOppimaara] =
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

  def getPerusopetuksenOppimaarat78Luokkalaiset(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[PerusopetuksenOppimaara78Luokkalaiset] =
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

  def getNuortenPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[NuortenPerusopetuksenOppiaineenOppimaara] =
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

  def getPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[PerusopetuksenOppiaineenOppimaara] =
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

  def getAikuistenPerusopetuksetOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[AikuistenPerusopetuksenOppimaara] =
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

  def getOppijanTiedot(oppijaNumero: String, opiskeluoikeudet: Set[Opiskeluoikeus]): Option[OppijanTiedotSuccessResponse] = {
    if(opiskeluoikeudet.isEmpty && !EXAMPLE_OPPIJA_OID.equals(oppijaNumero))
      None
    else if(EXAMPLE_OPPIJA_OID.equals(oppijaNumero))
      MockEntityToUIConverter.getOppijanTiedot()
    else
      Some(OppijanTiedotSuccessResponse(
        // TODO: oppijan tietojen osalta pitää päättää haetaanko reaaliaikaisesti ONR:stä vai miten toimitaan
        nimi =                                      EXAMPLE_NIMI,
        henkiloTunnus =                             EXAMPLE_HETU,
        syntymaAika =                               LocalDate.parse(EXAMPLE_SYNTYMAIKA),
        oppijaNumero =                              oppijaNumero,
        henkiloOID =                                oppijaNumero,
        opiskeluoikeudet =                          getOpiskeluoikeudet(opiskeluoikeudet).asJava,
        kkTutkinnot =                               getKKTutkinnot(opiskeluoikeudet).asJava,
        yoTutkinto =                                getYOTutkinto(opiskeluoikeudet).toJava,
        lukionOppimaara =                           getLukionOppimaara(opiskeluoikeudet).toJava,
        lukionOppiaineenOppimaarat =                getLukionOppiaineenOppimaarat(opiskeluoikeudet).asJava,
        diaTutkinto =                               getDiaTutkinto(opiskeluoikeudet).toJava,
        diaVastaavuusTodistus =                     getDiaVastaavuusTodistus(opiskeluoikeudet).toJava,
        ebTutkinto =                                getEBTutkinto(opiskeluoikeudet).toJava,
        ibTutkinto =                                getIBTutkinto(opiskeluoikeudet).toJava,
        preIB =                                     getPreIB(opiskeluoikeudet).toJava,
        ammatillisetPerusTutkinnot =                getAmmatillisetPerusTutkinnot(opiskeluoikeudet).asJava,
        ammattitutkinnot =                          getAmmattitutkinnot(opiskeluoikeudet).asJava,
        erikoisammattitutkinnot =                   getErikoisAmmattitutkinnot(opiskeluoikeudet).asJava,
        telmat =                                    getTelmat(opiskeluoikeudet).asJava,
        tuvat =                                     getTuvat(opiskeluoikeudet).asJava,
        vapaaSivistystyoKoulutukset =               getVapaaSivistystyoKoulutukset(opiskeluoikeudet).asJava,
        perusopetuksenOppimaarat =                  getPerusopetuksenOppimaarat(opiskeluoikeudet).asJava,
        perusopetuksenOppimaara78Luokkalaiset =     getPerusopetuksenOppimaarat78Luokkalaiset(opiskeluoikeudet).toJava,
        nuortenPerusopetuksenOppiaineenOppimaarat = getNuortenPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet).asJava,
        perusopetuksenOppiaineenOppimaarat =        getPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet).asJava,
        aikuistenPerusopetuksenOppimaarat =         getAikuistenPerusopetuksetOppimaarat(opiskeluoikeudet).asJava
      ))
  }
}
