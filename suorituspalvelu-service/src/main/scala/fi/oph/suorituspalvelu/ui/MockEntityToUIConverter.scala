package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, Koodi, Opiskeluoikeus}
import fi.oph.suorituspalvelu.resource.ApiConstants.EXAMPLE_SYNTYMAIKA
import fi.oph.suorituspalvelu.resource.ui.SuoritusTapa.NAYTTOTUTKINTO
import fi.oph.suorituspalvelu.resource.ui.Tila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.ui.UIService.{EXAMPLE_HETU, EXAMPLE_NIMI, EXAMPLE_OPPIJA_OID}

import java.time.LocalDate
import java.util.{Optional, UUID}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

object MockEntityToUIConverter {

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

  def getOpiskeluoikeudet(): List[UIOpiskeluoikeus] =
    List(
      UIOpiskeluoikeus(
        tunniste = UUID.randomUUID(),
        tutkinto = "Kasvatust. maist., kasvatustiede",
        oppilaitos = OOOppilaitos(
          nimi = "Tampereen yliopisto",
          oid = "1.2.3.4"
        ),
        voimassaolonAlku = LocalDate.parse("2001-08-01"),
        voimassaolonLoppu = LocalDate.parse("2025-12-11")
      ))

  def getKKTutkinnot(): List[KKSuoritus] =
    List(KKSuoritus(
      tunniste = UUID.randomUUID(),
      tutkinto = "Kasvatust. maist., kasvatustiede",
      oppilaitos = KKOppilaitos(
        nimi = "Tampereen yliopisto",
        oid = "1.2.3.4"
      ),
      tila = KESKEN,
      aloituspaiva = Optional.of(LocalDate.parse("2025-12-11")),
      valmistumispaiva = Optional.empty(),
      hakukohde = Hakukohde(
        nimi = "Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)"
      ),
    ))

  def getYOTutkinto(): Option[YOTutkinto] =
    Some(YOTutkinto(
      tunniste = UUID.randomUUID(),
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
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
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(LukionOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Äidinkieli ja kirjallisuus"
      ),LukionOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Uskonto/Elämänkatsomustieto"
      ))
    ))

  def getLukionOppiaineenOppimaarat(): List[LukionOppiaineenOppimaara] =
    List(LukionOppiaineenOppimaara(
      tunniste = UUID.randomUUID(),
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(LukionOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Äidinkieli ja kirjallisuus, suomi äidinkielenä"
      ), LukionOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Matematiikka, lyhyt oppimäärä, valinnainen"
      ))
    ))

  def getDiaTutkinto(): Option[DIATutkinto] =
    Some(DIATutkinto(
      tunniste = UUID.randomUUID(),
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
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
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      kieletKirjallisuusTaide = java.util.List.of(Oppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "A-kieli, englanti",
        laajuus = 3,
        keskiarvo = 8.5
      ), Oppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Historia",
        laajuus = 2,
        keskiarvo = 8.5
      )),
      matematiikkaLuonnontieteet = java.util.List.of(Oppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Matematiikka",
        laajuus = 3,
        keskiarvo = 6
      ), Oppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Kuvataide",
        laajuus = 3,
        keskiarvo = 8.5
      ))
    ))

  def getEBTutkinto(): Option[EBTutkinto] =
    Some(EBTutkinto(
      tunniste = UUID.randomUUID(),
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(EBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Mathematics",
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
        nimi = "First language, ranska",
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
        nimi = "Second language, saksa",
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
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(IBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Studies in language and literature",
        suoritukset = java.util.List.of(IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = "Language A: literature, suomi",
          laajuus = 9,
          predictedGrade = Some(7),
          arvosana = 7
        ), IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = "Language A: language and literature, englanti",
          laajuus = 6,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ), IBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Individuals and societies",
        suoritukset = java.util.List.of(IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = "History",
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ), IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = "Psychology",
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ), IBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Experimental sciences",
        suoritukset = java.util.List.of(IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = "Biology",
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ), IBOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Mathematics",
        suoritukset = java.util.List.of(IBSuoritus(
          tunniste = UUID.randomUUID(),
          nimi = "Mathematical studies",
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ))
    ))

  def getPreIB(): Option[PreIB] =
    Some(PreIB(
      tunniste = UUID.randomUUID(),
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
    ))

  def getAmmatillisetPerusTutkinnot(): List[AmmatillinenTutkinto] =
    List(
      AmmatillinenTutkinto(
        tunniste = UUID.randomUUID(),
        nimi = AmmatillisenTutkinnonNimi(
          fi = Optional.of("Puutarha-alan perustutkinto"),
          sv = Optional.of("Puutarha-alan perustutkinto sv"),
          en = Optional.of("Puutarha-alan perustutkinto en")
        ),
        oppilaitos = AmmatillinenOppilaitos(
          nimi = AmmatillisenOppilaitoksenNimi(
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
            sv = Optional.of("Viestintä- ja vuorovaikutusosaaminen sv"),
            en = Optional.of("Viestintä- ja vuorovaikutusosaaminen en"),
          ),
          laajuus = Optional.of(11),
          arvosana = Optional.of("HYVAKSYTTY")
        ), YTO(
          tunniste = UUID.randomUUID(),
          nimi = YTONimi(
            fi = Optional.of("Matemaattis-luonnontiellinen osaaminen"),
            sv = Optional.of("Matemaattis-luonnontiellinen osaaminen sv"),
            en = Optional.of("Matemaattis-luonnontiellinen osaaminen en")
          ),
          laajuus = Optional.of(11),
          arvosana = Optional.of("HYVAKSYTTY")
        ), YTO(
          tunniste = UUID.randomUUID(),
          nimi = YTONimi(
            fi = Optional.of("Yhteiskunta- ja työelämäosaaminen"),
            sv = Optional.of("Yhteiskunta- ja työelämäosaaminen sv"),
            en = Optional.of("Yhteiskunta- ja työelämäosaaminen en")
          ),
          laajuus = Optional.of(11),
          arvosana = Optional.of("HYVAKSYTTY")
        )),
        ammatillisenTutkinnonOsat = java.util.List.of(
          AmmatillisenTutkinnonOsa(
            tunniste = UUID.randomUUID(),
            nimi = AmmatillisenTutkinnonOsanNimi(
              fi = Optional.of("Audiovisuaalisen kulttuurin perusteet"),
              sv = Optional.of("Audiovisuaalisen kulttuurin perusteet sv"),
              en = Optional.of("Audiovisuaalisen kulttuurin perusteet en")
            ),
            laajuus = Optional.of(11),
            arvosana = Optional.of("4"),
          ),
          AmmatillisenTutkinnonOsa(
            tunniste = UUID.randomUUID(),
            nimi = AmmatillisenTutkinnonOsanNimi(
              fi = Optional.of("Äänimaailman suunnittelu"),
              sv = Optional.of("Äänimaailman suunnittelu sv"),
              en = Optional.of("Äänimaailman suunnittelu en")
            ),
            laajuus = Optional.of(11),
            arvosana = Optional.of("4"),
          )
        ),
        suoritustapa = Optional.empty()
      ), AmmatillinenTutkinto(
        tunniste = UUID.randomUUID(),
        nimi = AmmatillisenTutkinnonNimi(
          fi = Optional.of("Hevostalouden perustutkinto"),
          sv = Optional.of("Hevostalouden perustutkinto sv"),
          en = Optional.of("Hevostalouden perustutkinto en")
        ),
        oppilaitos = AmmatillinenOppilaitos(
          nimi = AmmatillisenOppilaitoksenNimi(
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
      nimi = AmmattitutkinnonNimi(
        fi = Optional.of("Maanmittausalan ammattitutkinto"),
        sv = Optional.of("Maanmittausalan ammattitutkinto sv"),
        en = Optional.of("Maanmittausalan ammattitutkinto en")
      ),
      oppilaitos = AmmatillinenOppilaitos(
        nimi = AmmatillisenOppilaitoksenNimi(
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
      nimi = ErikoisammattitutkinnonNimi(
        fi = Optional.of("Talous- ja henkilöstöalan erikoisammattitutkinto"),
        sv = Optional.of("Talous- ja henkilöstöalan erikoisammattitutkinto sv"),
        en = Optional.of("Talous- ja henkilöstöalan erikoisammattitutkinto en")
      ),
      oppilaitos = AmmatillinenOppilaitos(
        nimi = AmmatillisenOppilaitoksenNimi(
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
        nimi = AmmatillisenOppilaitoksenNimi(
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
      oppilaitos = AmmatillinenOppilaitos(
        AmmatillisenOppilaitoksenNimi(
          fi = Optional.of("Hämeen ammatti-instituutti, Lepaa"),
          sv = Optional.empty(),
          en = Optional.empty()
        ),
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2023-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      suorituskieli = "suomi",
      laajuus = 38
    ))

  def getVapaanSivistystyonKoulutukset(): List[VapaanSivistysTyonKoulutus] =
    List(VapaanSivistysTyonKoulutus(
      tunniste = UUID.randomUUID(),
      nimi = "Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille",
      oppilaitos = AmmatillinenOppilaitos(
        AmmatillisenOppilaitoksenNimi(
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
      laajuus = 38))

  def getPerusopetuksenOppimaarat(): List[PerusopetuksenOppimaara] =
    List(PerusopetuksenOppimaara(
      tunniste = UUID.randomUUID(),
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
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
          nimi = "Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "A1-kieli, englanti",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "B1-kieli, ruotsi",
          arvosana = Optional.of(9),
          valinnainen = Optional.of("S"),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "B2-kieli, saksa",
          arvosana = Optional.empty(),
          valinnainen = Optional.of("S"),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Matematiikka",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Biologia",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Maantieto",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Fysiikka",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Kemia",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Terveystieto",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Uskonto tai elämänkatsomustieto",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        )
      )
    ))

  def getPerusopetuksenOppimaarat78Luokkalaiset(): Option[PerusopetuksenOppimaara78Luokkalaiset] =
    Some(PerusopetuksenOppimaara78Luokkalaiset(
      tunniste = UUID.randomUUID(),
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
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
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2015-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(OppimaaranOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Biologia",
        arvosana = 9
      ),OppimaaranOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "Historia",
        arvosana = 8
      ))
    ))

  def getPerusopetuksenOppiaineenOppimaarat(): List[PerusopetuksenOppiaineenOppimaara] =
    List(PerusopetuksenOppiaineenOppimaara(
      tunniste = UUID.randomUUID(),
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2015-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(OppimaaranOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = "matematiikka",
        arvosana = 9
      ))
    ))

  def getAikuistenPerusopetuksetOppimaarat(): List[AikuistenPerusopetuksenOppimaara] =
    List(AikuistenPerusopetuksenOppimaara(
      tunniste = UUID.randomUUID(),
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      aloituspaiva = Optional.of(LocalDate.parse("2015-12-31")),
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(
        PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "A1-kieli, englanti",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "B1-kieli, ruotsi",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "B2-kieli, saksa",
          arvosana = Optional.of(9),
          valinnainen = Optional.empty(),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Matematiikka",
          arvosana = Optional.empty(),
          valinnainen = Optional.of("10"),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Biologia",
          arvosana = Optional.empty(),
          valinnainen = Optional.of("9"),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Maantieto",
          arvosana = Optional.empty(),
          valinnainen = Optional.of("8"),
        ), PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = "Fysiikka",
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
      vapaanSivistystyonKoulutukset =             getVapaanSivistystyonKoulutukset().asJava,
      perusopetuksenOppimaarat =                  getPerusopetuksenOppimaarat().asJava,
      perusopetuksenOppimaara78Luokkalaiset =     getPerusopetuksenOppimaarat78Luokkalaiset().toJava,
      nuortenPerusopetuksenOppiaineenOppimaarat = getNuortenPerusopetuksenOppiaineenOppimaarat().asJava,
      perusopetuksenOppiaineenOppimaarat =        getPerusopetuksenOppiaineenOppimaarat().asJava,
      aikuistenPerusopetuksenOppimaarat =         getAikuistenPerusopetuksetOppimaarat().asJava
    ))
  }
}
