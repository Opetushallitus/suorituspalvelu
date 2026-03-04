package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, EBOppiaine, EBTutkinto, GeneerinenOpiskeluoikeus, KKOpintosuoritus, KKOpiskeluoikeus, KKSynteettinenOpiskeluoikeus, KKSynteettinenSuoritus, KKTutkinto, Koodi, LukionOppimaara, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, PerusopetuksenYksilollistaminen, Suoritus, YOOpiskeluoikeus}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.service.UIService.{EXAMPLE_OPPIJA_OID, KOODISTO_POHJAKOULUTUS, KOODISTO_SUORITUSKIELET}
import fi.oph.suorituspalvelu.service.{UIService, ValintaData}
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}

import java.time.LocalDate
import java.util.Optional
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.reflect.ClassTag

object EntityToUIConverter {
  val KOULUTUS_KOODISTO = "koulutus"
  val VIRTA_OO_TILA_KOODISTO = "virtaopiskeluoikeudentila"
  val VIRTA_OPISKELUOIKEUDEN_TYYPPI_KOODISTO = "virtaopiskeluoikeudentyyppi"

  private def getKoodiNimi[N <: NimiBase](
    koodiArvo: Option[String],
    koodisto: String,
    koodistoProvider: KoodistoProvider)(implicit ct: ClassTag[N]): Optional[N] =
    koodiArvo.flatMap(arvo => koodistoProvider.haeKoodisto(koodisto).get(arvo).map(k =>
      val fi = k.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(_.nimi).toJava
      val sv = k.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(_.nimi).toJava
      val en = k.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(_.nimi).toJava
      ct.runtimeClass.getDeclaredConstructors()(0).newInstance(fi, sv, en).asInstanceOf[N]
    )).toJava

  private def getKKOppilaitos(myontaja: String, organisaatioProvider: OrganisaatioProvider) = {
    val oppilaitosTiedot = organisaatioProvider.haeOrganisaationTiedot(myontaja)
    KKOppilaitosUI(
      oppilaitosTiedot.map(org =>
        KKOppilaitosNimiUI(
          fi = Optional.of(org.nimi.fi),
          sv = Optional.of(org.nimi.sv),
          en = Optional.of(org.nimi.en),
        )).getOrElse(KKOppilaitosNimiUI(
        fi = Optional.of(myontaja),
        sv = Optional.of(myontaja),
        en = Optional.of(myontaja)
      )),
      oid = oppilaitosTiedot.map(org => org.oid).getOrElse("")
    )
  }

  private def getTutkintotaso(oo: KKOpiskeluoikeus): Optional[String] = oo.tyyppiKoodi match {
    case "1" | "2"  => Optional.of("alempi")
    case "3" | "4" => Optional.of("ylempi")
    case "7" => Optional.of("tohtori")
    case _ => Optional.empty
  }

  private def getSektori(myontaja: String, organisaatioProvider: OrganisaatioProvider): Optional[String] =
    organisaatioProvider.haeOrganisaationTiedot(myontaja)
      .flatMap(org => {
        org.oppilaitosTyyppi.map(_.split("#").head) match {
          case Some("oppilaitostyyppi_41") => Some("amk")
          case Some("oppilaitostyyppi_42") | Some("oppilaitostyyppi_66") => Some("yo")
          case _ => None
        }
      }).toJava

  def getOpiskeluoikeudet(opiskeluoikeudet: Set[Opiskeluoikeus], organisaatioProvider: OrganisaatioProvider, koodistoProvider: KoodistoProvider): List[OpiskeluoikeusUI] =
    opiskeluoikeudet
      .collect{ case oo: KKOpiskeluoikeus if oo.isTutkintoonJohtava => oo }
      .map(o => OpiskeluoikeusUI(
        tunniste = o.tunniste,
        nimi = getKoodiNimi[OpiskeluoikeusNimiUI](o.koulutusKoodi, KOULUTUS_KOODISTO, koodistoProvider)
          .orElse(
            getKoodiNimi[OpiskeluoikeusNimiUI](Some(o.tyyppiKoodi), VIRTA_OPISKELUOIKEUDEN_TYYPPI_KOODISTO, koodistoProvider)
              .orElse(OpiskeluoikeusNimiUI(Optional.empty(), Optional.empty(), Optional.empty()))
          ),
        oppilaitos = getKKOppilaitos(o.myontaja, organisaatioProvider),
        voimassaolonAlku = o.alkuPvm,
        voimassaolonLoppu = o.loppuPvm,
        supaTila = OpiskeluoikeusTilaUI.valueOf(o.supaTila.toString),
        virtaTila = getKoodiNimi[OpiskeluoikeusVirtaTilaUI](Some(o.virtaTila.arvo), VIRTA_OO_TILA_KOODISTO, koodistoProvider).orElse(
          OpiskeluoikeusVirtaTilaUI(Optional.empty, Optional.empty, Optional.empty
        )),
        tutkintotaso = getTutkintotaso(o),
        sektori = getSektori(o.myontaja, organisaatioProvider)
      )).toList

  private def createVirtaOpintojaksoHierarkia(
    virtaSuoritukset: Seq[Suoritus],
  ): java.util.List[KKOpintojaksoUI] = {
    virtaSuoritukset
      .flatMap {
        case virtaSuoritus: KKOpintosuoritus =>
          Some(KKOpintojaksoUI(
            tunniste = virtaSuoritus.tunniste,
            nimi = KKOpintojaksoNimiUI(
              fi = virtaSuoritus.nimi.flatMap(_.fi).toJava,
              sv = virtaSuoritus.nimi.flatMap(_.sv).toJava,
              en = virtaSuoritus.nimi.flatMap(_.en).toJava,
            ),
            laajuus = virtaSuoritus.opintoPisteet,
            arvosana = virtaSuoritus.arvosana.toJava,
            opintojaksot = createVirtaOpintojaksoHierarkia(virtaSuoritus.suoritukset)
          ))
        case _ => None
      }.toList.asJava
  }

  private def convertKKSuoritusTila(opiskeluoikeus: KKOpiskeluoikeus): SuoritusTilaUI =
    opiskeluoikeus.virtaTila.arvo match
      case "1" => SuoritusTilaUI.KESKEN // aktiivinen
      case "2" => SuoritusTilaUI.KESKEN // optio
      case "3" => SuoritusTilaUI.VALMIS // valmistunut
      case "4" => SuoritusTilaUI.KESKEYTYNYT // passivoitu
      case "5" => SuoritusTilaUI.KESKEYTYNYT // luopunut
      case "6" => SuoritusTilaUI.KESKEYTYNYT // päättynyt

  private def getKKOpiskeluoikeusTyyppiNimi(opiskeluoikeus: Option[KKOpiskeluoikeus], koodistoProvider: KoodistoProvider): Option[KKSuoritusNimiUI] =
    getKoodiNimi[KKSuoritusNimiUI](opiskeluoikeus.map(_.tyyppiKoodi), VIRTA_OPISKELUOIKEUDEN_TYYPPI_KOODISTO, koodistoProvider).toScala

  private def getSuorituskieliFromKoodi(
    kielikoodi: Option[String],
    koodistoProvider: KoodistoProvider
  ): Optional[SuorituskieliUI] =
    getKoodiNimi[SuorituskieliUI](kielikoodi.map(_.toUpperCase), KOODISTO_SUORITUSKIELET, koodistoProvider)

  private def kielistettyToKKSuoritusNimi(kielistetty: Kielistetty): KKSuoritusNimiUI = {
    KKSuoritusNimiUI(
      fi = kielistetty.fi.toJava,
      sv = kielistetty.sv.toJava,
      en = kielistetty.en.toJava
    )
  }

  private def getKKSuoritusNimi(
    suoritus: Option[Suoritus],
    opiskeluoikeus: Option[KKOpiskeluoikeus] = None,
    koodistoProviderOption: Option[KoodistoProvider] = None
  ): Optional[KKSuoritusNimiUI] = {
    (suoritus match {
      case Some(tutkinto: KKTutkinto) =>
        koodistoProviderOption.flatMap(koodistoProvider =>
          getKoodiNimi[KKSuoritusNimiUI](tutkinto.koulutusKoodi, KOULUTUS_KOODISTO, koodistoProvider).toScala
          .orElse(getKKOpiskeluoikeusTyyppiNimi(opiskeluoikeus, koodistoProvider)))
      case Some(opintosuoritus: KKOpintosuoritus) =>
        opintosuoritus.nimi.map(kielistettyToKKSuoritusNimi)
          .orElse(koodistoProviderOption.flatMap(getKKOpiskeluoikeusTyyppiNimi(opiskeluoikeus, _)))
      case Some(synteettinenSuoritus: KKSynteettinenSuoritus) =>
        synteettinenSuoritus.nimi.map(kielistettyToKKSuoritusNimi)
          .orElse(koodistoProviderOption.flatMap(getKKOpiskeluoikeusTyyppiNimi(opiskeluoikeus, _)))
      case _ => None
    }).toJava
  }

  private def convertSyntheticKkOpiskeluoikeusSuoritukset(
    oo: KKSynteettinenOpiskeluoikeus,
    organisaatioProvider: OrganisaatioProvider,
    koodistoProvider: KoodistoProvider
  ): Seq[KKSuoritusUI] = {
    oo.suoritukset.toSeq match {
      case Seq(suoritus: KKOpintosuoritus) => Seq(KKSuoritusUI(
        tunniste = suoritus.tunniste,
        nimi = getKKSuoritusNimi(Some(suoritus)),
        virtaNimi = suoritus.nimi.map(kielistettyToKKSuoritusNimi).toJava,
        oppilaitos = getKKOppilaitos(suoritus.myontaja, organisaatioProvider),
        tila = SuoritusTilaUI.VALMIS,
        aloituspaiva = Optional.empty(),
        valmistumispaiva = Optional.empty(),
        suorituskieli = getSuorituskieliFromKoodi(Some(suoritus.kieli), koodistoProvider),
        opintojaksot = createVirtaOpintojaksoHierarkia(suoritus.suoritukset.toSeq),
        isTutkintoonJohtava = false,
        tutkintotaso = Optional.empty,
        sektori = getSektori(suoritus.myontaja, organisaatioProvider)
      ))
      case suoritukset => Seq(KKSuoritusUI(
        tunniste = oo.tunniste,
        nimi = Optional.of(KKSuoritusNimiUI(
          fi = Optional.of(s"${suoritukset.size} opintojaksoa"),
          sv = Optional.of(s"${suoritukset.size} studieavsnitt"),
          en = Optional.of(s"${suoritukset.size} study modules"),
        )),
        virtaNimi = Optional.empty,
        oppilaitos = getKKOppilaitos(oo.myontaja, organisaatioProvider),
        tila = SuoritusTilaUI.VALMIS,
        aloituspaiva = Optional.empty,
        valmistumispaiva = Optional.empty,
        suorituskieli = Optional.empty,
        opintojaksot = createVirtaOpintojaksoHierarkia(suoritukset),
        isTutkintoonJohtava = false,
        tutkintotaso = Optional.empty,
        sektori = getSektori(oo.myontaja, organisaatioProvider)
      ))
    }
  }

  private def convertNormalKkOpiskeluoikeusSuoritukset(
    oo: KKOpiskeluoikeus,
    organisaatioProvider: OrganisaatioProvider,
    koodistoProvider: KoodistoProvider
  ): Seq[KKSuoritusUI] = oo.suoritukset.toSeq.flatMap {
    case tutkinto: KKTutkinto =>
      Some(KKSuoritusUI(
        tunniste = tutkinto.tunniste,
        nimi = getKKSuoritusNimi(Some(tutkinto), Some(oo), Some(koodistoProvider)),
        virtaNimi = tutkinto.nimi.map(kielistettyToKKSuoritusNimi).toJava,
        oppilaitos = getKKOppilaitos(tutkinto.myontaja, organisaatioProvider),
        tila = SuoritusTilaUI.valueOf(tutkinto.supaTila.toString),
        aloituspaiva = tutkinto.aloitusPvm.toJava,
        valmistumispaiva = tutkinto.suoritusPvm.toJava,
        opintojaksot = createVirtaOpintojaksoHierarkia(tutkinto.suoritukset.toSeq),
        suorituskieli = getSuorituskieliFromKoodi(tutkinto.kieli, koodistoProvider),
        isTutkintoonJohtava = oo.isTutkintoonJohtava,
        tutkintotaso = getTutkintotaso(oo),
        sektori = getSektori(tutkinto.myontaja, organisaatioProvider),
      ))
    case suoritus: KKOpintosuoritus =>
      Some(KKSuoritusUI(
        tunniste = suoritus.tunniste,
        nimi = getKKSuoritusNimi(Some(suoritus), Some(oo), Some(koodistoProvider)),
        virtaNimi = suoritus.nimi.map(kielistettyToKKSuoritusNimi).toJava,
        oppilaitos = getKKOppilaitos(oo.myontaja, organisaatioProvider),
        tila = SuoritusTilaUI.valueOf(suoritus.supaTila.toString),
        aloituspaiva = Optional.of(oo.alkuPvm),
        valmistumispaiva = suoritus.suoritusPvm.toJava,
        opintojaksot = createVirtaOpintojaksoHierarkia(suoritus.suoritukset.toSeq),
        suorituskieli = getSuorituskieliFromKoodi(Some(suoritus.kieli), koodistoProvider),
        isTutkintoonJohtava = oo.isTutkintoonJohtava,
        tutkintotaso = getTutkintotaso(oo),
        sektori = getSektori(suoritus.myontaja, organisaatioProvider),
      ))
    case suoritus: KKSynteettinenSuoritus =>
      Some(KKSuoritusUI(
        tunniste = suoritus.tunniste,
        nimi = getKKSuoritusNimi(Some(suoritus), Some(oo), Some(koodistoProvider)),
        virtaNimi = suoritus.nimi.map(kielistettyToKKSuoritusNimi).toJava,
        oppilaitos = getKKOppilaitos(oo.myontaja, organisaatioProvider),
        tila = SuoritusTilaUI.valueOf(suoritus.supaTila.toString),
        aloituspaiva = Optional.of(oo.alkuPvm),
        valmistumispaiva = suoritus.suoritusPvm.toJava,
        opintojaksot = createVirtaOpintojaksoHierarkia(suoritus.suoritukset.toSeq),
        suorituskieli = Optional.empty,
        isTutkintoonJohtava = oo.isTutkintoonJohtava,
        tutkintotaso = getTutkintotaso(oo),
        sektori = getSektori(suoritus.myontaja, organisaatioProvider),
      ))
    case _ => None
  }

  def getKKTutkinnot(
    opiskeluoikeudet: Set[Opiskeluoikeus],
    organisaatioProvider: OrganisaatioProvider,
    koodistoProvider: KoodistoProvider
  ): List[KKSuoritusUI] = {
    opiskeluoikeudet.flatMap({
      case synteettinenOpiskeluoikeus: KKSynteettinenOpiskeluoikeus =>
        convertSyntheticKkOpiskeluoikeusSuoritukset(synteettinenOpiskeluoikeus, organisaatioProvider, koodistoProvider)
      case opiskeluoikeus: KKOpiskeluoikeus =>
        convertNormalKkOpiskeluoikeusSuoritukset(opiskeluoikeus, organisaatioProvider, koodistoProvider)
      case _ => Seq.empty
    }).toList
  }

  def getYOTutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus], koodistoProvider: KoodistoProvider): List[YOTutkinto] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[YOOpiskeluoikeus])
      .map(o => o.asInstanceOf[YOOpiskeluoikeus])
      .flatMap(o => o.yoTutkinto.map(t => Seq(t)).getOrElse(Seq.empty))
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.YOTutkinto])
      .map(t => {
        YOTutkinto(
          tunniste = t.tunniste,
          nimi = YOTutkintoNimi(
            fi = Optional.of("Ylioppilastutkinto"),
            sv = Optional.of("Studentexamen"),
            en = Optional.of("Matriculation Examination")
          ),
          oppilaitos = YOOppilaitos(
            nimi = YOOppilaitosNimi(
              fi = Optional.of("Ylioppilastutkintolautakunta"),
              sv = Optional.of("Studentexamensnämnden"),
              en = Optional.of("The Matriculation Examination Board")
            ),
            oid = UIService.YTL_ORGANISAATIO_OID
          ),
          tila = SuoritusTilaUI.valueOf(t.supaTila.toString),
          valmistumispaiva = t.valmistumisPaiva.toJava,
          suorituskieli = t.suoritusKieli.arvo,
          yoKokeet = t.aineet.map(a => YOKoe(
            tunniste = a.tunniste,
            nimi = koodistoProvider.haeKoodisto(UIService.KOODISTO_YOKOKEET).get(a.koodi.arvo).map(k =>
              YOKoeNimi(
                fi = k.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(_.nimi).toJava,
                sv = k.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(_.nimi).toJava,
                en = k.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(_.nimi).toJava,
              )
            ).getOrElse(YOKoeNimi(Optional.empty(), Optional.empty(), Optional.empty())),
            arvosana = a.arvosana.arvo,
            yhteispistemaara = a.pisteet.toJava,
            tutkintokerta = a.tutkintoKerta
          )).toList.asJava
        )
      }).toList

  def getLukionOppimaara(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[LukionOppimaaraUI] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[GeneerinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[GeneerinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset)
      .filter(s => s.isInstanceOf[LukionOppimaara])
      .map(s => s.asInstanceOf[LukionOppimaara])
      .map(lukionOppimaara =>
        LukionOppimaaraUI(
          tunniste = lukionOppimaara.tunniste,
          nimi = LukionOppimaaraNimi(
            fi = Optional.of("Lukion oppimäärä"),
            sv = Optional.of("Gymnasiets lärokurs"),
            en = Optional.of("Upper secondary school syllabus")
          ),
          oppilaitos = YOOppilaitos(
            nimi = YOOppilaitosNimi(
              fi = lukionOppimaara.oppilaitos.nimi.fi.toJava,
              sv = lukionOppimaara.oppilaitos.nimi.sv.toJava,
              en = lukionOppimaara.oppilaitos.nimi.en.toJava
            ),
            oid = lukionOppimaara.oppilaitos.oid
          ),
          tila = SuoritusTilaUI.valueOf(lukionOppimaara.supaTila.toString),
          aloituspaiva = lukionOppimaara.aloitusPaivamaara.toJava,
          valmistumispaiva = lukionOppimaara.vahvistusPaivamaara.toJava,
          suorituskieli = lukionOppimaara.suoritusKieli.map(_.arvo).getOrElse("")
        )
      ).headOption

  def getLukionOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[LukionOppiaineenOppimaara] =
    List.empty[LukionOppiaineenOppimaara]

  def getDiaTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[DIATutkinto] = None

  def getDiaVastaavuusTodistus(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[DIAVastaavuusTodistus] =
    None

  def getEBTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[EBTutkintoUI] = {
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[GeneerinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[GeneerinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset)
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.EBTutkinto])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.EBTutkinto])
      .map((ebTutkinto: EBTutkinto) =>
        val oppiaineet = ebTutkinto.osasuoritukset.map((o: EBOppiaine) => {
          EBOppiaineUI(
            tunniste = o.tunniste,
            nimi = EBOppiaineNimiUI(
              fi = o.nimi.fi.toJava,
              sv = o.nimi.sv.toJava,
              en = o.nimi.en.toJava
            ),
            suorituskieli = o.suorituskieli.arvo,
            laajuus = o.laajuus.map(l => l.arvo).getOrElse(BigDecimal(0)),
            written = o.osasuoritukset.find(os => os.koodi.arvo.equals("Written"))
              .map(osw => EBOppiaineOsasuoritusUI(osw.koodi.arvo, BigDecimal.apply(osw.arvosana.arvosana.arvo))).toJava,
            oral = o.osasuoritukset.find(os => os.koodi.arvo.equals("Oral"))
              .map(osw => EBOppiaineOsasuoritusUI(osw.koodi.arvo, BigDecimal.apply(osw.arvosana.arvosana.arvo))).toJava,
            `final` = o.osasuoritukset.find(os => os.koodi.arvo.equals("Final"))
            .map(osw => EBOppiaineOsasuoritusUI(osw.koodi.arvo, BigDecimal.apply(osw.arvosana.arvosana.arvo))).toJava

          )
        })
        EBTutkintoUI(
          tunniste = ebTutkinto.tunniste,
          nimi = EBTutkintoNimi(
            fi = ebTutkinto.nimi.fi.toJava,
            sv = ebTutkinto.nimi.sv.toJava,
            en = ebTutkinto.nimi.en.toJava
          ),
          oppilaitos =
            YOOppilaitos(
              nimi = YOOppilaitosNimi(
                fi = ebTutkinto.oppilaitos.nimi.fi.toJava,
                sv = ebTutkinto.oppilaitos.nimi.sv.toJava,
                en = ebTutkinto.oppilaitos.nimi.en.toJava
              ),
              oid = ebTutkinto.oppilaitos.oid
            ),
          tila = SuoritusTilaUI.valueOf(ebTutkinto.supaTila.toString),
          aloituspaiva = ebTutkinto.aloitusPaivamaara.toJava,
          valmistumispaiva = ebTutkinto.vahvistusPaivamaara.toJava,
          suorituskieli = "EN",
          oppiaineet = oppiaineet.toList.asJava
        )
      ).headOption
  }


  def getIBTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[IBTutkinto] =
    None

  def getPreIB(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[PreIB] =
    None

  def getAmmatillisetPerusTutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Ammatillinentutkinto] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset)
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.AmmatillinenPerustutkinto])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.AmmatillinenPerustutkinto])
      .map(t => {
        // Jos koskesta ei tule arvosanoja, kyseessä näyttötutkinto
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
          tila = SuoritusTilaUI.valueOf(t.supaTila.toString),
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
          suoritustapa = if (nayttoTutkinto) Optional.of(SuoritusTapaUI.NAYTTOTUTKINTO) else Optional.empty()
        )
      }).toList

  def getAmmattitutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Ammattitutkinto] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset)
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
          tila = SuoritusTilaUI.valueOf(t.supaTila.toString),
          aloituspaiva = t.aloitusPaivamaara.toJava,
          valmistumispaiva = t.vahvistusPaivamaara.toJava,
          suorituskieli = t.suoritusKieli.arvo
        )
      }).toList

  def getErikoisAmmattitutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Erikoisammattitutkinto] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset)
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
          tila = SuoritusTilaUI.valueOf(t.supaTila.toString),
          aloituspaiva = t.aloitusPaivamaara.toJava,
          valmistumispaiva = t.vahvistusPaivamaara.toJava,
          suorituskieli = t.suoritusKieli.arvo,
        )
      }).toList

  def getTelmat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[Telma] = {
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset)
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
          tila = SuoritusTilaUI.valueOf(t.supaTila.toString),
          aloituspaiva = Optional.of(t.aloitusPaivamaara),
          valmistumispaiva = t.vahvistusPaivamaara.toJava,
          suorituskieli = t.suoritusKieli.arvo,
        )
      }).toList
    }

    def getTuvat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[TuvaUI] =
      opiskeluoikeudet
        .collect { case o: GeneerinenOpiskeluoikeus => o}
        .flatMap(o => o.suoritukset)
        .collect { case s: fi.oph.suorituspalvelu.business.Tuva => s }
        .map(t => {
          TuvaUI(
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
            tila = SuoritusTilaUI.valueOf(t.supaTila.toString),
            aloituspaiva = Optional.of(t.aloitusPaivamaara),
            valmistumispaiva = t.vahvistusPaivamaara.toJava,
            laajuus = t.hyvaksyttyLaajuus.map(l => TuvaLaajuus(l.arvo, TuvaLaajuusYksikko(
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
        .flatMap(o => o.suoritukset)
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
            tila = SuoritusTilaUI.valueOf(t.supaTila.toString),
            aloituspaiva = Optional.of(t.aloitusPaivamaara),
            valmistumispaiva = t.vahvistusPaivamaara.toJava,
            laajuus = t.hyvaksyttyLaajuus.map(l => VapaaSivistystyoLaajuus(l.arvo, VapaaSivistystyoLaajuusYksikko(
              l.lyhytNimi.get.fi.toJava,
              l.lyhytNimi.get.sv.toJava,
              l.lyhytNimi.get.en.toJava
            ))).toJava,
            suorituskieli = t.suoritusKieli.arvo
          )
        }).toList

  def getPerusopetuksenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus], koodistoProvider: KoodistoProvider): List[PerusopetuksenOppimaaraUI] =
    def getVieraanKielenNimi(kieli: Option[Koodi], asiointiKieli: String): Option[String] =
      kieli.flatMap(kieli => koodistoProvider.haeKoodisto(UIService.KOODISTO_KIELIVALIKOIMA).get(kieli.arvo).flatMap(koodi => koodi.metadata.find(m => m.kieli.equalsIgnoreCase(asiointiKieli)).map(m => m.nimi)))

    opiskeluoikeudet
      .collect { case oo: PerusopetuksenOpiskeluoikeus => oo }
      .flatMap(_.suoritukset)
      .collect { case s: PerusopetuksenOppimaara => s }
       .map(om => {
        PerusopetuksenOppimaaraUI(
          versioTunniste = om.versioTunniste.toJava,
          tunniste = om.tunniste,
          nimi = PerusopetuksenOppimaaraNimi(
            fi = Optional.of("Perusopetuksen oppimäärä"),
            sv = Optional.of("Grundläggande utbildningens lärokurs"),
            en = Optional.of("Basic education syllabus")
          ),
          oppilaitos = PKOppilaitos(
            nimi = PKOppilaitosNimi(
              fi = om.oppilaitos.nimi.fi.toJava,
              sv = om.oppilaitos.nimi.sv.toJava,
              en = om.oppilaitos.nimi.en.toJava
            ),
            oid = om.oppilaitos.oid
          ),
          tila = SuoritusTilaUI.valueOf(om.supaTila.toString),
          aloituspaiva = om.aloitusPaivamaara.toJava,
          valmistumispaiva = om.vahvistusPaivamaara.toJava,
          suorituskieli = om.suoritusKieli.arvo,
          luokka = om.luokka.toJava,
          yksilollistaminen = om.yksilollistaminen.map((y: PerusopetuksenYksilollistaminen) => Yksilollistaminen(
            PerusopetuksenYksilollistaminen.toIntValue(y),
            getKoodiNimi[YksilollistamisNimi](Some(PerusopetuksenYksilollistaminen.toIntValue(y).toString), KOODISTO_POHJAKOULUTUS, koodistoProvider).get
          )).toJava,
          // halutaan näyttää vain valmistuneen suorituksen oppiaineet
          oppiaineet = (if (om.supaTila == fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS) om.aineet else Set.empty).map(a => PerusopetuksenOppiaineUI(
            tunniste = a.tunniste,
            koodi = a.koodi.arvo,
            nimi = PerusopetuksenOppiaineNimi(
              fi = a.nimi.fi.map(n => n + getVieraanKielenNimi(a.kieli, "fi").map(k => ", " + k).getOrElse("")).toJava,
              sv = a.nimi.sv.map(n => n + getVieraanKielenNimi(a.kieli, "sv").map(k => ", " + k).getOrElse("")).toJava,
              en = a.nimi.en.map(n => n + getVieraanKielenNimi(a.kieli, "en").map(k => ", " + k).getOrElse("")).toJava,
            ),
            kieli = a.kieli.map(k => k.arvo).toJava,
            arvosana = a.arvosana.arvo,
            valinnainen = !a.pakollinen,
          )).toList.asJava,
          syotetty = om.syotetty
        )
      }).toList

  def getPerusopetuksenOppimaarat78Luokkalaiset(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[PerusopetuksenOppimaara78Luokkalaiset] =
    None

  def getPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[PerusopetuksenOppiaineenOppimaaratUI] = {
    opiskeluoikeudet
      .collect { case oo: PerusopetuksenOpiskeluoikeus => oo }
      .flatMap(_.suoritukset)
      .collect { case s: PerusopetuksenOppimaaranOppiaineidenSuoritus => s }
      .map(oppiaineidenSuoritus => {
        val oppiaineet: Set[PerusopetuksenOppiaineUI] =
          oppiaineidenSuoritus.aineet.map(oppiaine => {
            PerusopetuksenOppiaineUI(
              tunniste = oppiaine.tunniste,
              koodi = oppiaine.koodi.arvo,
              nimi = PerusopetuksenOppiaineNimi(
                oppiaine.nimi.fi.toJava,
                oppiaine.nimi.sv.toJava,
                oppiaine.nimi.en.toJava
              ),
              kieli = java.util.Optional.ofNullable(oppiaine.kieli.map(_.arvo).orNull),
              arvosana = oppiaine.arvosana.arvo,
              valinnainen = !oppiaine.pakollinen
            )
          })
        PerusopetuksenOppiaineenOppimaaratUI(
          tunniste = oppiaineidenSuoritus.tunniste,
          versioTunniste = oppiaineidenSuoritus.versioTunniste.toJava,
          oppilaitos = PKOppilaitos(
            PKOppilaitosNimi(
              oppiaineidenSuoritus.oppilaitos.nimi.fi.toJava,
              oppiaineidenSuoritus.oppilaitos.nimi.sv.toJava,
              oppiaineidenSuoritus.oppilaitos.nimi.en.toJava,
            ),
            oppiaineidenSuoritus.oppilaitos.oid
          ),
          nimi = PerusopetuksenOppiaineenOppimaaraNimi(
            fi = Optional.of("Perusopetuksen oppiaineen oppimäärä"),
            sv = Optional.of("Lärokurs i ett läroämne i grundläggande utbildning"),
            en = Optional.of("Basic education subject syllabus")
          ),
          tila = SuoritusTilaUI.valueOf(oppiaineidenSuoritus.supaTila.toString),
          aloituspaiva = java.util.Optional.ofNullable(oppiaineidenSuoritus.aloitusPaivamaara.orNull),
          valmistumispaiva = java.util.Optional.ofNullable(oppiaineidenSuoritus.vahvistusPaivamaara.orNull),
          suorituskieli = oppiaineidenSuoritus.suoritusKieli.arvo,
          oppiaineet = oppiaineet.toList.asJava,
          syotetty = oppiaineidenSuoritus.syotetty
        )
      }).toList
  }

  def getOppijanTiedot(etunimet: Option[String], sukunimi: Option[String], hetu: Option[String], oppijaNumero: String, henkiloOid: String, syntymaAika: Option[LocalDate], opiskeluoikeudet: Set[Opiskeluoikeus], organisaatioProvider: OrganisaatioProvider, koodistoProvider: KoodistoProvider): OppijanTiedotSuccessResponse = {
    if(EXAMPLE_OPPIJA_OID.equals(oppijaNumero))
      MockEntityToUIConverter.getOppijanTiedot()
    else
      OppijanTiedotSuccessResponse(
        etunimet =                                  etunimet.toJava,
        sukunimi =                                  sukunimi.toJava,
        henkiloTunnus =                             hetu.toJava,
        syntymaAika =                               syntymaAika.toJava,
        oppijaNumero =                              oppijaNumero,
        henkiloOID =                                henkiloOid,
        opiskeluoikeudet =                          getOpiskeluoikeudet(opiskeluoikeudet, organisaatioProvider, koodistoProvider).asJava,
        kkTutkinnot =                               getKKTutkinnot(opiskeluoikeudet, organisaatioProvider, koodistoProvider).asJava,
        yoTutkinnot =                               getYOTutkinnot(opiskeluoikeudet, koodistoProvider).asJava,
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
        perusopetuksenOppimaarat =                  getPerusopetuksenOppimaarat(opiskeluoikeudet, koodistoProvider).asJava,
        perusopetuksenOppimaara78Luokkalaiset =     getPerusopetuksenOppimaarat78Luokkalaiset(opiskeluoikeudet).toJava,
        perusopetuksenOppiaineenOppimaarat =        getPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet).asJava
      )
  }

  def getOppijanValintaDataForUI(oppijaNumero: String, hakuOid: String, valintaData: ValintaData): OppijanValintaDataSuccessResponse = {
    OppijanValintaDataSuccessResponse(
      oppijaNumero,
      hakuOid,
      valintaData.kaikkiAvainArvotFull().map(aac => {
        AvainArvoContainerUI(
          aac.avain,
          aac.arvo,
          AvainArvoMetadataUI(
            aac.metadata.selitteet.toList.asJava,
            aac.metadata.arvoEnnenYliajoa.toJava,
            aac.metadata.yliajo.map(y => {
              AvainArvoYliajoUI(
                avain = y.avain,
                arvo = y.arvo.toJava,
                henkiloOid = y.henkiloOid,
                hakuOid = y.hakuOid,
                virkailijaOid = y.virkailijaOid,
                selite = y.selite
              )
            }).toJava,
            aac.metadata.arvoOnHakemukselta)
        )
      }).toList.asJava
    )
  }
}
