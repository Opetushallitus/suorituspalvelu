package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus}
import fi.oph.suorituspalvelu.ui.{AikuistenPerusopetuksenOppimaara, AmmatillinenOppilaitos, AmmatillinenTutkinto, AmmatillisenTutkinnonOsa, Ammattitutkinto, DIATutkinto, DIAVastaavuusTodistus, EBOppiaine, EBSuoritus, EBTutkinto, Erikoisammattitutkinto, Hakukohde, IBOppiaine, IBSuoritus, IBTutkinto, KKOppilaitos, KKSuoritus, LukionOppiaine, LukionOppiaineenOppimaara, LukionOppimaara, NuortenPerusopetuksenOppiaineenOppimaara, OOOppilaitos, Oppiaine, OppijanTiedot, OppimaaranOppiaine, PKOppilaitos, PerusopetuksenOppiaine, PerusopetuksenOppiaineenOppimaara, PerusopetuksenOppimaara, PerusopetuksenOppimaara78Luokkalaiset, PreIB, Telma, Tuva, UIOpiskeluoikeus, VapaanSivistysTyonKoulutus, YOKoe, YOOppilaitos, YOTutkinto, YTO}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.time.LocalDate
import java.util.Optional
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@Component
class UIService {

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

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
      tila = "KESKEN",
      valmistumispaiva = Optional.empty(),
      hakukohde = Hakukohde(
        nimi = "Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)"
      ),
    ))

  def getYOTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): YOTutkinto =
    YOTutkinto(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2019-06-01")),
      suorituskieli = "suomi",
      yoKokeet = java.util.List.of(YOKoe(
        aine = "Matematiikka",
        taso = "Lyhyt oppimäärä (MA)",
        arvosana = "E",
        yhteispistemaara = 23,
        tutkintokerta = LocalDate.parse("2019-06-01")
      ))
    )

  def getLukionOppimaara(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[LukionOppimaara] =
    Some(LukionOppimaara(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(LukionOppiaine(
        nimi = "Äidinkieli ja kirjallisuus"
      ))
    ))

  def getLukionOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[LukionOppiaineenOppimaara] =
    List(LukionOppiaineenOppimaara(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(LukionOppiaine(
        nimi = "Äidinkieli ja kirjallisuus"
      ))
    ))

  def getDiaTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[DIATutkinto] =
    Some(DIATutkinto(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
    ))

  def getDiaVastaavuusTodistus(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[DIAVastaavuusTodistus] =
    Some(DIAVastaavuusTodistus(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      kieletKirjallisuusTaide = java.util.List.of(Oppiaine(
        nimi = "Historia",
        laajuus = 3,
        keskiarvo = 8.5
      )),
      matematiikkaLuonnontieteet = java.util.List.of(Oppiaine(
        nimi = "Matematiikka",
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
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(EBOppiaine(
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
      ))
    ))

  def getIBTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[IBTutkinto] =
    Some(IBTutkinto(
      oppilaitos = YOOppilaitos(
        nimi = "Ylioppilastutkintolautakunta",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(IBOppiaine(
        nimi = "Experimental sciences",
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
      tila = "VALMIS",
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
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
      suorituskieli = "suomi",
      painotettuKeskiarvo = 4.34,
      ammatillisetYtotKeskiarvo = 4.34,
      ytot = Optional.of(java.util.List.of(YTO(
        nimi = "Viestintä- ja vuorovaikutusosaaminen",
        laajuus = 11,
        tila = "Hyväksytty"
      ))),
      ammatillisenTutkinnonOsat = java.util.List.of(AmmatillisenTutkinnonOsa(
        nimi = "Audiovisuaalisen kulttuurin perusteet",
        laajuus = 11,
        arvosana = 4,
      )),
      suoritustapa = Optional.of("Näyttötutkinto")
    ))

  def getAmmattitutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Ammattitutkinto] =
    List(Ammattitutkinto(
      nimi = "Maanmittausalan ammattitutkinto",
      oppilaitos = AmmatillinenOppilaitos(
        nimi = "Hämeen ammatti-instituutti, Lepaa",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
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
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      suorituskieli = "suomi"
    ))

  def getTelmat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Telma] =
    List(Telma(
      oppilaitos = AmmatillinenOppilaitos(
        nimi = "Hämeen ammatti-instituutti, Lepaa",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
      suorituskieli = "suomi"
    ))

  def getTuvat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Tuva] =
    List(Tuva(
      oppilaitos = AmmatillinenOppilaitos(
        nimi = "Hämeen ammatti-instituutti, Lepaa",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
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
      tila = "KESKEYTYNYT",
      valmistumispaiva = Optional.empty(),
      suorituskieli = "suomi",
      laajuus = 38))

  def getPerusopetuksenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[PerusopetuksenOppimaara] =
    List(PerusopetuksenOppimaara(
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      luokka = "9A",
      yksilollistetty = false,
      oppiaineet = java.util.List.of(PerusopetuksenOppiaine(
        nimi = "matematiikka",
        arvosana = 9,
        valinnainen = "S",
      ))
    ))

  def getPerusopetuksenOppimaarat78Luokkalaiset(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[PerusopetuksenOppimaara78Luokkalaiset] =
    Some(PerusopetuksenOppimaara78Luokkalaiset(
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
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
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(OppimaaranOppiaine(
        nimi = "matematiikka",
        arvosana = 9
      ))
    ))

  def getPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[PerusopetuksenOppiaineenOppimaara] =
    List(PerusopetuksenOppiaineenOppimaara(
      oppilaitos = PKOppilaitos(
        nimi = "Keltinmäen koulu",
        oid = "1.2.3.4"
      ),
      tila = "VALMIS",
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
      tila = "VALMIS",
      valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
      suorituskieli = "suomi",
      oppiaineet = java.util.List.of(PerusopetuksenOppiaine(
        nimi = "matematiikka",
        arvosana = 9,
        valinnainen = "S"
      ))
    ))

  def getOppijanTiedot(oppijaNumero: String): Option[OppijanTiedot] = {
    val opiskeluoikeudet = kantaOperaatiot.haeSuoritukset(oppijaNumero).values.flatten.toSet

    Some(OppijanTiedot(
      oppijaNumero =                              oppijaNumero,
      opiskeluoikeudet =                          getOpiskeluoikeudet(opiskeluoikeudet).asJava,
      kkTutkinnot =                               getKKTutkinnot(opiskeluoikeudet).asJava,
      yoTutkinto =                                Optional.of(getYOTutkinto(opiskeluoikeudet)),
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
