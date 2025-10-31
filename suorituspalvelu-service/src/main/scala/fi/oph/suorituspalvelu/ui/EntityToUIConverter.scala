package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, GeneerinenOpiskeluoikeus, Koodi, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, VirtaOpiskeluoikeus, YOOpiskeluoikeus}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoContainer, AvainArvoConverterResults}
import fi.oph.suorituspalvelu.resource.ApiConstants.ESIMERKKI_SYNTYMAIKA
import fi.oph.suorituspalvelu.resource.ui.SuoritusTapa.NAYTTOTUTKINTO
import fi.oph.suorituspalvelu.resource.ui.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.resource.ui.{AikuistenPerusopetuksenOppimaara, AikuistenPerusopetuksenOppimaaraNimi, AmmatillinenOppilaitos, AmmatillinenOppilaitosNimi, Ammatillinentutkinto, AmmatillinentutkintoNimi, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, AmmatillisenTutkinnonOsaAlueNimi, AmmatillisenTutkinnonOsaNimi, Ammattitutkinto, AmmattitutkintoNimi, AvainArvoContainerUI, AvainArvoMetadataUI, AvainArvoYliajoUI, DIAOppiaine, DIAOppiaineNimi, DIATutkinto, DIATutkintoNimi, DIAVastaavuusTodistus, DIAVastaavuusTodistusNimi, EBOppiaine, EBOppiaineNimi, EBSuoritus, EBTutkinto, EBTutkintoNimi, Erikoisammattitutkinto, ErikoisammattitutkintoNimi, IBOppiaine, IBOppiaineNimi, IBSuoritus, IBSuoritusNimi, IBTutkinto, IBTutkintoNimi, KKOppilaitos, KKOppilaitosNimi, KKSuoritus, KKSuoritusNimi, LukionOppiaine, LukionOppiaineNimi, LukionOppiaineenOppimaara, LukionOppiaineenOppimaaraNimi, LukionOppimaara, LukionOppimaaraNimi, NuortenPerusopetuksenOppiaineenOppimaara, NuortenPerusopetuksenOppiaineenOppimaaraNimi, OOOppilaitos, OOOppilaitosNimi, OpiskeluoikeusTila, OppijanTiedotSuccessResponse, OppijanValintaDataSuccessResponse, PKOppilaitos, PKOppilaitosNimi, PerusopetuksenOppiaine, PerusopetuksenOppiaineNimi, PerusopetuksenOppiaineenOppimaara, PerusopetuksenOppiaineenOppimaaraNimi, PerusopetuksenOppimaara, PerusopetuksenOppimaara78Luokkalaiset, PerusopetuksenOppimaara78LuokkalaisetNimi, PerusopetuksenOppimaaraNimi, PreIB, PreIBNimi, SuoritusTila, Telma, TelmaNimi, Tuva, TuvaLaajuus, TuvaLaajuusYksikko, TuvaNimi, UIOpiskeluoikeus, UIOpiskeluoikeusNimi, UIOpiskeluoikeusVirtaTila, VapaaSivistystyoKoulutus, VapaaSivistystyoKoulutusNimi, VapaaSivistystyoLaajuus, VapaaSivistystyoLaajuusYksikko, VapaaSivistystyoOppilaitos, VapaaSivistystyoOppilaitosNimi, YOKoe, YOKoeNimi, YOOppilaitos, YOOppilaitosNimi, YOTutkinto, YOTutkintoNimi, YTO, YTOArvosana, YTONimi, YTOOsaAlue, YTOOsaAlueNimi, Yksilollistaminen, YksilollistamisNimi}
import fi.oph.suorituspalvelu.service.{UIService, ValintaData}
import fi.oph.suorituspalvelu.service.UIService.{EXAMPLE_HETU, EXAMPLE_NIMI, EXAMPLE_OPPIJA_OID}
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}

import java.time.LocalDate
import java.util.{Optional, UUID}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

object EntityToUIConverter {

  val KOULUTUS_KOODISTO = "koulutus"
  val VIRTA_OO_TILA_KOODISTO = "virtaopiskeluoikeudentila"

  def getOpiskeluoikeudet(opiskeluoikeudet: Set[Opiskeluoikeus], organisaatioProvider: OrganisaatioProvider, koodistoProvider: KoodistoProvider): List[UIOpiskeluoikeus] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[VirtaOpiskeluoikeus])
      .map(o => o.asInstanceOf[VirtaOpiskeluoikeus])
      .map(o => UIOpiskeluoikeus(
        o.tunniste,
        nimi = {
          val koulutus = koodistoProvider.haeKoodisto(KOULUTUS_KOODISTO).get(o.koulutusKoodi)
          UIOpiskeluoikeusNimi(
            fi = koulutus.flatMap(k => k.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(_.nimi)).toJava,
            sv = koulutus.flatMap(k => k.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(_.nimi)).toJava,
            en = koulutus.flatMap(k => k.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(_.nimi)).toJava,
          )
        },
        oppilaitos = {
          val organisaatio = organisaatioProvider.haeOrganisaationTiedot(o.myontaja)
          OOOppilaitos(
            nimi = OOOppilaitosNimi(
              fi = organisaatio.map(o => o.nimi.fi).toJava,
              sv = organisaatio.map(o => o.nimi.sv).toJava,
              en = organisaatio.map(o => o.nimi.en).toJava,
            ),
            oid = o.myontaja
          )
        },
        o.alkuPvm,
        o.loppuPvm,
        OpiskeluoikeusTila.valueOf(o.supaTila.toString),
        {
          val tilaKoodi = koodistoProvider.haeKoodisto(VIRTA_OO_TILA_KOODISTO).get(o.virtaTila.arvo)
          UIOpiskeluoikeusVirtaTila(
            tilaKoodi.flatMap(k => k.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(_.nimi)).toJava,
            tilaKoodi.flatMap(k => k.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(_.nimi)).toJava,
            tilaKoodi.flatMap(k => k.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(_.nimi)).toJava,
          )
        }
      )).toList

  def getKKTutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus], organisaatioProvider: OrganisaatioProvider): List[KKSuoritus] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[VirtaOpiskeluoikeus])
      .map(o => o.asInstanceOf[VirtaOpiskeluoikeus])
      .flatMap(o => o.suoritukset)
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.VirtaTutkinto])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.VirtaTutkinto])
      .map(t => {
        KKSuoritus(
          tunniste = t.tunniste,
          nimi = KKSuoritusNimi(
            fi = t.nimiFi.toJava,
            sv = t.nimiSv.toJava,
            en = t.nimiEn.toJava,
          ),
          oppilaitos = {
            val organisaatio = organisaatioProvider.haeOrganisaationTiedot(t.myontaja)
            KKOppilaitos(
              nimi = KKOppilaitosNimi(
                fi = organisaatio.map(o => o.nimi.fi).toJava,
                sv = organisaatio.map(o => o.nimi.sv).toJava,
                en = organisaatio.map(o => o.nimi.en).toJava,
              ),
              oid = t.myontaja
            )
          },
          tila = VALMIS,
          aloituspaiva = Optional.of(t.aloitusPvm),
          valmistumispaiva = Optional.of(t.suoritusPvm)
        )
      }).toList

  def getYOTutkinnot(opiskeluoikeudet: Set[Opiskeluoikeus], koodistoProvider: KoodistoProvider): List[YOTutkinto] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[YOOpiskeluoikeus])
      .map(o => o.asInstanceOf[YOOpiskeluoikeus])
      .flatMap(o => Seq(o.yoTutkinto))
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
          tila = VALMIS,
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

  def getLukionOppimaara(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[LukionOppimaara] =
    None

  def getLukionOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[LukionOppiaineenOppimaara] =
    List.empty[LukionOppiaineenOppimaara]

  def getDiaTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[DIATutkinto] = None

  def getDiaVastaavuusTodistus(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[DIAVastaavuusTodistus] =
    None

  def getEBTutkinto(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[EBTutkinto] =
    None

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
          tila = SuoritusTila.valueOf(t.supaTila.toString),
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
          suoritustapa = if (nayttoTutkinto) Optional.of(NAYTTOTUTKINTO) else Optional.empty()
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
          tila = SuoritusTila.valueOf(t.supaTila.toString),
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
          tila = SuoritusTila.valueOf(t.supaTila.toString),
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
          tila = SuoritusTila.valueOf(t.supaTila.toString),
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
        .flatMap(o => o.suoritukset)
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
            tila = SuoritusTila.valueOf(t.supaTila.toString),
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
            tila = SuoritusTila.valueOf(t.supaTila.toString),
            aloituspaiva = t.aloitusPaivamaara.toJava,
            valmistumispaiva = t.vahvistusPaivamaara.toJava,
            laajuus = t.hyvaksyttyLaajuus.map(l => VapaaSivistystyoLaajuus(l.arvo, VapaaSivistystyoLaajuusYksikko(
              l.lyhytNimi.get.fi.toJava,
              l.lyhytNimi.get.sv.toJava,
              l.lyhytNimi.get.en.toJava
            ))).toJava,
            suorituskieli = t.suoritusKieli.arvo
          )
        }).toList

  def getPerusopetuksenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus], koodistoProvider: KoodistoProvider): List[PerusopetuksenOppimaara] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .flatMap(o => o.suoritukset)
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenOppimaara])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenOppimaara])
      .map(om => {
        PerusopetuksenOppimaara(
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
          tila = SuoritusTila.valueOf(om.supaTila.toString),
          aloituspaiva = om.aloitusPaivamaara.toJava,
          valmistumispaiva = om.vahvistusPaivamaara.toJava,
          suorituskieli = om.suoritusKieli.arvo,
          luokka = om.luokka.toJava,
          yksilollistaminen = om.yksilollistaminen.map(y => Yksilollistaminen(
            y,
            koodistoProvider.haeKoodisto(UIService.KOODISTO_POHJAKOULUTUS).get(y.toString).map(ya => YksilollistamisNimi(
              ya.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(_.nimi).toJava,
              ya.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(_.nimi).toJava,
              ya.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(_.nimi).toJava
            )).get
          )).toJava,
          oppiaineet = om.aineet.map(a => PerusopetuksenOppiaine(
            tunniste = a.tunniste,
            koodi = a.koodi.arvo,
            nimi = PerusopetuksenOppiaineNimi(
              fi = a.nimi.fi.toJava,
              sv = a.nimi.sv.toJava,
              en = a.nimi.en.toJava
            ),
            kieli = a.kieli.map(k => k.arvo).toJava,
            arvosana = a.arvosana.arvo,
            valinnainen = !a.pakollinen,
          )).toList.asJava,
          syotetty = om.yksilollistaminen.isDefined // KOSKI-tiedoissa yksilöllistäminen on oppiainetasolla
        )
      }).toList

  def getPerusopetuksenOppimaarat78Luokkalaiset(opiskeluoikeudet: Set[Opiskeluoikeus]): Option[PerusopetuksenOppimaara78Luokkalaiset] =
    None

  def getNuortenPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[NuortenPerusopetuksenOppiaineenOppimaara] =
    List.empty[NuortenPerusopetuksenOppiaineenOppimaara]

  def getPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[PerusopetuksenOppiaineenOppimaara] =
    List.empty[PerusopetuksenOppiaineenOppimaara]

  def getAikuistenPerusopetuksetOppimaarat(opiskeluoikeudet: Set[Opiskeluoikeus]): List[AikuistenPerusopetuksenOppimaara] =
    List.empty[AikuistenPerusopetuksenOppimaara]

  def getOppijanTiedot(oppijaNumero: String, opiskeluoikeudet: Set[Opiskeluoikeus], organisaatioProvider: OrganisaatioProvider, koodistoProvider: KoodistoProvider): Option[OppijanTiedotSuccessResponse] = {
    if(opiskeluoikeudet.isEmpty && !EXAMPLE_OPPIJA_OID.equals(oppijaNumero))
      None
    else if(EXAMPLE_OPPIJA_OID.equals(oppijaNumero))
      MockEntityToUIConverter.getOppijanTiedot()
    else
      Some(OppijanTiedotSuccessResponse(
        // TODO: oppijan tietojen osalta pitää päättää haetaanko reaaliaikaisesti ONR:stä vai miten toimitaan
        nimi =                                      EXAMPLE_NIMI,
        henkiloTunnus =                             EXAMPLE_HETU,
        syntymaAika =                               LocalDate.parse(ESIMERKKI_SYNTYMAIKA),
        oppijaNumero =                              oppijaNumero,
        henkiloOID =                                oppijaNumero,
        opiskeluoikeudet =                          getOpiskeluoikeudet(opiskeluoikeudet, organisaatioProvider, koodistoProvider).asJava,
        kkTutkinnot =                               getKKTutkinnot(opiskeluoikeudet, organisaatioProvider).asJava,
        yoTutkinnot =                                getYOTutkinnot(opiskeluoikeudet, koodistoProvider).asJava,
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
        nuortenPerusopetuksenOppiaineenOppimaarat = getNuortenPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet).asJava,
        perusopetuksenOppiaineenOppimaarat =        getPerusopetuksenOppiaineenOppimaarat(opiskeluoikeudet).asJava,
        aikuistenPerusopetuksenOppimaarat =         getAikuistenPerusopetuksetOppimaarat(opiskeluoikeudet).asJava
      ))
  }

  def getOppijanValintaDataForUI(oppijaNumero: String, hakuOid: Option[String], valintaData: ValintaData): OppijanValintaDataSuccessResponse = {
    OppijanValintaDataSuccessResponse(
      oppijaNumero,
      hakuOid.toJava,
      valintaData.avainArvot.map(aac => {
        AvainArvoContainerUI(
          aac.avain,
          aac.arvo,
          AvainArvoMetadataUI(
            aac.metadata.selitteet.toList.asJava,
            aac.metadata.duplikaatti,
            aac.metadata.arvoEnnenYliajoa.toJava,
            aac.metadata.yliajo.map(y => {
              AvainArvoYliajoUI(
                avain = y.avain,
                arvo = y.arvo,
                henkiloOid = y.henkiloOid,
                hakuOid = y.hakuOid,
                virkailijaOid = y.virkailijaOid,
                selite = y.selite
              )
            }).toJava)
        )
      }).toList.asJava
    )
  }
}
