package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus}
import fi.oph.suorituspalvelu.resource.ui.{AikuistenPerusopetuksenOppimaara, AmmatillinenOppilaitos, AmmatillinenTutkinto, AmmatillisenTutkinnonOsa, Ammattitutkinto, DIATutkinto, DIAVastaavuusTodistus, EBOppiaine, EBSuoritus, EBTutkinto, Erikoisammattitutkinto, Hakukohde, IBOppiaine, IBSuoritus, IBTutkinto, KKOppilaitos, KKSuoritus, LukionOppiaine, LukionOppiaineenOppimaara, LukionOppimaara, NuortenPerusopetuksenOppiaineenOppimaara, OOOppilaitos, Oppiaine, Oppija, OppijanTiedotSuccessResponse, Oppilaitos, OppimaaranOppiaine, PKOppilaitos, PerusopetuksenOppiaine, PerusopetuksenOppiaineenOppimaara, PerusopetuksenOppimaara, PerusopetuksenOppimaara78Luokkalaiset, PreIB, Telma, Tuva, UIOpiskeluoikeus, VapaanSivistysTyonKoulutus, YOKoe, YOOppilaitos, YOTutkinto, YTO, YTOTila}
import fi.oph.suorituspalvelu.resource.ui.Tila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.resource.ui.YTOTila.HYVAKSYTTY
import fi.oph.suorituspalvelu.service.UIService.{EXAMPLE_HETU, EXAMPLE_NIMI, EXAMPLE_OPPIJA_OID, EXAMPLE_OPPILAITOS_NIMI, EXAMPLE_OPPILAITOS_OID}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.time.LocalDate
import java.util.Optional
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

object UIService {
  val EXAMPLE_OPPIJA_OID = "1.2.246.562.24.40483869857"
  val EXAMPLE_HETU = "010296-1230"
  val EXAMPLE_NIMI = "Olli Oppija"

  val EXAMPLE_OPPILAITOS_OID = "1.2.246.562.10.56753942459"
  val EXAMPLE_OPPILAITOS_NIMI = "Esimerkki oppilaitos"
}

@Component
class UIService {

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  def haeOppilaitokset(): Set[Oppilaitos] =
    Set(Oppilaitos(
      nimi = EXAMPLE_OPPILAITOS_NIMI,
      oid = EXAMPLE_OPPILAITOS_OID
    ))

  def haeOppijat(oppija: Option[String], oppilaitos: Option[String], vuosi: Option[String], luokka: Option[String]): Set[Oppija] =
    // TODO: implementaatiohuomio. Todennäköisesti halutaan purkaa olennainen tieto (oppilaitos, vuosi, luokka) erilliseen sopivasti GIN-indeksoituun tauluun KOSKI-hakujen yhteydessä josta sitten haetaan tässä
    Set(Oppija(
      EXAMPLE_OPPIJA_OID,
      Optional.of(EXAMPLE_HETU),
      EXAMPLE_NIMI
    )).filter(o => {
      oppija.isDefined && (o.oppijaNumero == oppija.get || o.hetu.get() == oppija.get || o.nimi.contains(oppija.get))
    })

  def getOpiskeluoikeudet(opiskeluoikeudet: Set[Opiskeluoikeus]): List[UIOpiskeluoikeus] =
    List(
      UIOpiskeluoikeus(
        tutkinto = "Kasvatust. maist., kasvatustiede",
        oppilaitos = OOOppilaitos(
          nimi = "Tampereen yliopisto",
          oid = "1.2.3.4"
        ),
        voimassaolonAlku = LocalDate.parse("2001-08-01"),
        voimassaolonLoppu = LocalDate.parse("2025-12-11")
      ))

  def getKKTutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[KKSuoritus] =
    List(KKSuoritus(
      tutkinto = "Kasvatust. maist., kasvatustiede",
      oppilaitos = KKOppilaitos(
        nimi = "Tampereen yliopisto",
        oid = "1.2.3.4"
      ),
      tila = KESKEN,
      valmistumispaiva = Optional.empty(),
      hakukohde = Hakukohde(
        nimi = "Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)"
      ),
    ))

  def getYOTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[YOTutkinto] =
    Some(YOTutkinto(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2019-06-01")),
      suorituskieli = "suomi",
      yoKokeet = java.util.List.of(YOKoe(
        aine = "Psykologia",
        taso = "Ainemuotoinen reaali",
        arvosana = "E",
        yhteispistemaara = 28,
        tutkintokerta = LocalDate.parse("2018-12-21")
      ), YOKoe(
        aine = "Englanti",
        taso = "Pitkä oppimäärä (KIELI)",
        arvosana = "E",
        yhteispistemaara = 259,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ), YOKoe(
        aine = "Matematiikka",
        taso = "Lyhyt oppimäärä (MA)",
        arvosana = "C",
        yhteispistemaara = 23,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ), YOKoe(
        aine = "Suomi",
        taso = "Äidinkieli",
        arvosana = "C",
        yhteispistemaara = 49,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ), YOKoe(
        aine = "Historia",
        taso = "Ainemuotoinen reaali",
        arvosana = "M",
        yhteispistemaara = 25,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ), YOKoe(
        aine = "Yhteiskuntaoppi",
        taso = "Ainemuotoinen reaali",
        arvosana = "E",
        yhteispistemaara = 32,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ))
    ))

  def getLukionOppimaara(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[LukionOppimaara] =
    Some(LukionOppimaara(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(LukionOppiaine(
        nimi = "Äidinkieli ja kirjallisuus"
      ),LukionOppiaine(
        nimi = "Uskonto/Elämänkatsomustieto"
      ))
    ))

  def getLukionOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[LukionOppiaineenOppimaara] =
    List(LukionOppiaineenOppimaara(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(LukionOppiaine(
        nimi = "Äidinkieli ja kirjallisuus, suomi äidinkielenä"
      ), LukionOppiaine(
        nimi = "Matematiikka, lyhyt oppimäärä, valinnainen"
      ))
    ))

  def getDiaTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[DIATutkinto] =
    Some(DIATutkinto(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
    ))

  def getDiaVastaavuusTodistus(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[DIAVastaavuusTodistus] =
    Some(DIAVastaavuusTodistus(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      kieletKirjallisuusTaide = java.util.List.of(Oppiaine(
        nimi = "A-kieli, englanti",
        laajuus = 3,
        keskiarvo = 8.5
      ), Oppiaine(
        nimi = "Historia",
        laajuus = 2,
        keskiarvo = 8.5
      )),
      matematiikkaLuonnontieteet = java.util.List.of(Oppiaine(
        nimi = "Matematiikka",
        laajuus = 3,
        keskiarvo = 6
      ), Oppiaine(
        nimi = "Kuvataide",
        laajuus = 3,
        keskiarvo = 8.5
      ))
    ))

  def getEBTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[EBTutkinto] =
    Some(EBTutkinto(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(EBOppiaine(
        nimi = "Mathematics",
        suorituskieli = "englanti",
        laajuus = 4,
        written = EBSuoritus(
          arvosana = 8.67,
        ),
        oral = Optional.empty(),
        `final` = EBSuoritus(
          arvosana = 8.67,
        )
      ), EBOppiaine(
        nimi = "First language, ranska",
        suorituskieli = "englanti",
        laajuus = 3,
        written = EBSuoritus(
          arvosana = 8.67,
        ),
        oral = Optional.of(EBSuoritus(
          arvosana = 8.67,
        )),
        `final` = EBSuoritus(
          arvosana = 8.67,
        )
      ), EBOppiaine(
        nimi = "Second language, saksa",
        suorituskieli = "englanti",
        laajuus = 3,
        written = EBSuoritus(
          arvosana = 8.67,
        ),
        oral = Optional.of(EBSuoritus(
          arvosana = 8.67,
        )),
        `final` = EBSuoritus(
          arvosana = 8.67,
        )
      ))
    ))

  def getIBTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[IBTutkinto] =
    Some(IBTutkinto(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(IBOppiaine(
        nimi = "Studies in language and literature",
        suoritukset = java.util.List.of(IBSuoritus(
          nimi = "Language A: literature, suomi",
          laajuus = 9,
          predictedGrade = Some(7),
          arvosana = 7
        ), IBSuoritus(
          nimi = "Language A: language and literature, englanti",
          laajuus = 6,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ), IBOppiaine(
        nimi = "Individuals and societies",
        suoritukset = java.util.List.of(IBSuoritus(
          nimi = "History",
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ), IBSuoritus(
          nimi = "Psychology",
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ), IBOppiaine(
        nimi = "Experimental sciences",
        suoritukset = java.util.List.of(IBSuoritus(
          nimi = "Biology",
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ), IBOppiaine(
        nimi = "Mathematics",
        suoritukset = java.util.List.of(IBSuoritus(
          nimi = "Mathematical studies",
          laajuus = 3,
          predictedGrade = Some(7),
          arvosana = 7
        ))
      ))
    ))

  def getPreIB(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[PreIB] =
    Some(PreIB(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
    ))

  def getAmmatillisetTutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[AmmatillinenTutkinto] =
    List(AmmatillinenTutkinto(
      nimi = "Puutarha-alan perustutkinto",
      oppilaitos = AmmatillinenOppilaitos(
        nimi = "Hämeen ammatti-instituutti, Lepaa",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      painotettuKeskiarvo = 4.34,
      ammatillisetYtotKeskiarvo = 4.34,
      ytot = java.util.List.of(YTO(
        nimi = "Viestintä- ja vuorovaikutusosaaminen",
        laajuus = 11,
        tila = HYVAKSYTTY
      ), YTO(
        nimi = "Matemaattis-luonnontiellinen osaaminen",
        laajuus = 11,
        tila = HYVAKSYTTY
      ), YTO(
        nimi = "Yhteiskunta- ja työelämäosaaminen",
        laajuus = 11,
        tila = HYVAKSYTTY
      )),
      ammatillisenTutkinnonOsat = java.util.List.of(AmmatillisenTutkinnonOsa(
        nimi = "Audiovisuaalisen kulttuurin perusteet",
        laajuus = 11,
        arvosana = 4,
      ), AmmatillisenTutkinnonOsa(
        nimi = "Äänimaailman suunnittelu",
        laajuus = 11,
        arvosana = 4,
      )),
      suoritustapa = Optional.empty()
    ), AmmatillinenTutkinto(
      nimi = "Hevostalouden perustutkinto",
      oppilaitos = AmmatillinenOppilaitos(
        nimi = "Hämeen ammatti-instituutti, Lepaa",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      painotettuKeskiarvo = 4.34,
      ammatillisetYtotKeskiarvo = 4.34,
      ytot = java.util.List.of(),
      ammatillisenTutkinnonOsat = java.util.List.of(),
      suoritustapa = Optional.of("Näyttötutkinto")
    ))

  def getAmmattitutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Ammattitutkinto] =
    List(Ammattitutkinto(
      nimi = "Maanmittausalan ammattitutkinto",
      oppilaitos = AmmatillinenOppilaitos(
        nimi = "Hämeen ammatti-instituutti, Lepaa",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      suorituskieli = "suomi"
    ))

  def getErikoisAmmattitutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Erikoisammattitutkinto] =
    List(Erikoisammattitutkinto(
      nimi = "Talous- ja henkilöstöalan erikoisammattitutkinto",
      oppilaitos = AmmatillinenOppilaitos(
        nimi = "Hämeen ammatti-instituutti, Lepaa",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      suorituskieli = "suomi"
    ))

  def getTelmat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Telma] =
    List(Telma(
      oppilaitos = AmmatillinenOppilaitos(
        nimi = "Hämeen ammatti-instituutti, Lepaa",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      suorituskieli = "suomi"
    ))

  def getTuvat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Tuva] =
    List(Tuva(
      oppilaitos = AmmatillinenOppilaitos(
        nimi = "Hämeen ammatti-instituutti, Lepaa",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      suorituskieli = "suomi",
      laajuus = 38
    ))

  def getVapaanSivistystyonKoulutukset(opiskeluoikeudet: Set[Opiskeluoikeus]): List[VapaanSivistysTyonKoulutus] =
    List(VapaanSivistysTyonKoulutus(
      nimi = "Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille",
      oppilaitos = AmmatillinenOppilaitos(
        nimi = "Hämeen ammatti-instituutti, Lepaa",
        oid = "1.2.3.4"
      ),
      tila = KESKEYTYNYT,
      valmistumispaiva = Optional.empty(),
      suorituskieli = "suomi",
      laajuus = 38))

  def getPerusopetuksenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[PerusopetuksenOppimaara] =
    List(PerusopetuksenOppimaara(
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      luokka = "9A",
      yksilollistetty = false,
      oppiaineet = java.util.List.of(PerusopetuksenOppiaine(
        nimi = "Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "A1-kieli, englanti",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "B1-kieli, ruotsi",
        arvosana = Optional.of(9),
        valinnainen = Optional.of("S"),
      ), PerusopetuksenOppiaine(
        nimi = "B2-kieli, saksa",
        arvosana = Optional.empty(),
        valinnainen = Optional.of("S"),
      ), PerusopetuksenOppiaine(
        nimi = "Matematiikka",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "Biologia",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "Maantieto",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "Fysiikka",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "Kemia",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "Terveystieto",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "Uskonto tai elämänkatsomustieto",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ))
    ))

  def getPerusopetuksenOppimaarat78Luokkalaiset(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[PerusopetuksenOppimaara78Luokkalaiset] =
    Some(PerusopetuksenOppimaara78Luokkalaiset(
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      koulusivistyskieli = "suomi",
      luokka = "9A",
      yksilollistetty = false
    ))

  def getNuortenPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[NuortenPerusopetuksenOppiaineenOppimaara] =
    List(NuortenPerusopetuksenOppiaineenOppimaara(
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(OppimaaranOppiaine(
        nimi = "Biologia",
        arvosana = 9
      ),OppimaaranOppiaine(
        nimi = "Historia",
        arvosana = 8
      ))
    ))

  def getPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[PerusopetuksenOppiaineenOppimaara] =
    List(PerusopetuksenOppiaineenOppimaara(
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(OppimaaranOppiaine(
        nimi = "matematiikka",
        arvosana = 9
      ))
    ))

  def getAikuistenPerusopetuksetOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[AikuistenPerusopetuksenOppimaara] =
    List(AikuistenPerusopetuksenOppimaara(
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = VALMIS,
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(PerusopetuksenOppiaine(
        nimi = "Äidinkieli ja kirjallisuus, suomen kieli ja kirjallisuus",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "A1-kieli, englanti",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "B1-kieli, ruotsi",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "B2-kieli, saksa",
        arvosana = Optional.of(9),
        valinnainen = Optional.empty(),
      ), PerusopetuksenOppiaine(
        nimi = "Matematiikka",
        arvosana = Optional.empty(),
        valinnainen = Optional.of("10"),
      ), PerusopetuksenOppiaine(
        nimi = "Biologia",
        arvosana = Optional.empty(),
        valinnainen = Optional.of("9"),
      ), PerusopetuksenOppiaine(
        nimi = "Maantieto",
        arvosana = Optional.empty(),
        valinnainen = Optional.of("8"),
      ), PerusopetuksenOppiaine(
        nimi = "Fysiikka",
        arvosana = Optional.empty(),
        valinnainen = Optional.of("9"),
      ))
    ))

  def getOppijanTiedot(oppijaNumero: String): Option[OppijanTiedotSuccessResponse] = {
    val opiskeluoikeudet = kantaOperaatiot.haeSuoritukset(oppijaNumero).values.flatten.toSet

    if(opiskeluoikeudet.isEmpty && !EXAMPLE_OPPIJA_OID.equals(oppijaNumero))
      None
    else
      Some(OppijanTiedotSuccessResponse(
        oppijaNumero =                              oppijaNumero,
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
        ammatillisetTutkinnot =                     getAmmatillisetTutkinnot(opiskeluoikeudet).asJava,
        ammattitutkinnot =                          getAmmattitutkinnot(opiskeluoikeudet).asJava,
        erikoisammattitutkinnot =                   getErikoisAmmattitutkinnot(opiskeluoikeudet).asJava,
        telmat =                                    getTelmat(opiskeluoikeudet).asJava,
        tuvat =                                     getTuvat(opiskeluoikeudet).asJava,
        vapaanSivistystyonKoulutukset =             getVapaanSivistystyonKoulutukset(opiskeluoikeudet).asJava,
        perusopetuksenOppimaarat =                  getPerusopetuksenOppimaarat(opiskeluoikeudet).asJava,
        perusopetuksenOppimaara78Luokkalaiset =     getPerusopetuksenOppimaarat78Luokkalaiset(opiskeluoikeudet).toJava,
        nuortenPerusopetuksenOppiaineenOppimaarat = getNuortenPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet).asJava,
        perusopetuksenOppiaineenOppimaarat =        getPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet).asJava,
        aikuistenPerusopetuksenOppimaarat =         getAikuistenPerusopetuksetOppimaarat(opiskeluoikeudet).asJava
      ))
  }
}
