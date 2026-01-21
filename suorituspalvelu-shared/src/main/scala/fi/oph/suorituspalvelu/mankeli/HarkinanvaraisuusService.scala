package fi.oph.suorituspalvelu.mankeli

import org.springframework.beans.factory.annotation.Autowired
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, SuoritusTila}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, HakemuspalveluClient, KoutaHakukohde, Ohjausparametrit}

import java.time.{Instant, LocalDate, ZoneId}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

enum HarkinnanvaraisuudenSyy {
  case SURE_YKS_MAT_AI
  case SURE_EI_PAATTOTODISTUSTA
  case ATARU_YKS_MAT_AI
  case ATARU_ULKOMAILLA_OPISKELTU
  case ATARU_EI_PAATTOTODISTUSTA
  case ATARU_SOSIAALISET_SYYT
  case ATARU_OPPIMISVAIKEUDET
  case ATARU_KOULUTODISTUSTEN_VERTAILUVAIKEUDET
  case ATARU_RIITTAMATON_TUTKINTOKIELEN_TAITO
  case EI_HARKINNANVARAINEN
  case EI_HARKINNANVARAINEN_HAKUKOHDE
}

case class HakutoiveenHarkinnanvaraisuus(hakukohdeOid: String,
                                         harkinnanvaraisuudenSyy: HarkinnanvaraisuudenSyy)

case class HakemuksenHarkinnanvaraisuus(hakemusOid: String,
                                        henkiloOid: String,
                                        hakutoiveet: List[HakutoiveenHarkinnanvaraisuus])

object HarkinnanvaraisuusDeducer {

  //Tämän jälkeen suoritettuja ma/ai yksilöllistämisiä ei enää huomioida harkinnanvaraisuuspäättelyssä.
  // Oppiaineen oppimäärän suoritukset (korotukset) kuitenkin huomioidaan myös tämän jälkeen.
  val YKS_MAT_AI_SUORITUS_ENNEN_DATE = LocalDate.parse("2025-08-01")

  val ataruMatematiikkaJaAidinkieliYksilollistettyQuestions = Set("matematiikka-ja-aidinkieli-yksilollistetty_1", "matematiikka-ja-aidinkieli-yksilollistetty_2")
  val ataruHakukohdeHarkinnanvaraisuusPrefix = "harkinnanvaraisuus-reason_"
  val ataruHakemusPohjakoulutus = "base-education-2nd"

  def hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate) = {
    val perusopetusOOs = opiskeluoikeudet.collect { case o: PerusopetuksenOpiskeluoikeus => o }

    val huomioitavatValmiitOppimaarat = perusopetusOOs
      .flatMap(_.suoritukset).collect { case om: PerusopetuksenOppimaara => om }
      .filter(_.supaTila.equals(SuoritusTila.VALMIS))
      .filter(_.vahvistusPaivamaara.exists(vahvistusPvm => vahvistusPvm.isBefore(vahvistettuViimeistaan) || vahvistusPvm.isEqual(vahvistettuViimeistaan)))
      .filter(_.vahvistusPaivamaara.exists(vahvistusPvm => vahvistusPvm.isBefore(YKS_MAT_AI_SUORITUS_ENNEN_DATE) || vahvistusPvm.isEqual(YKS_MAT_AI_SUORITUS_ENNEN_DATE)))
    val huomioitavatOppiaineenOppimaarat = perusopetusOOs
      .flatMap(_.suoritukset).collect { case oom: PerusopetuksenOppimaaranOppiaineidenSuoritus => oom }
      .filter(_.supaTila.equals(SuoritusTila.VALMIS))
      .filter(_.vahvistusPaivamaara.exists(vahvistusPvm => vahvistusPvm.isBefore(vahvistettuViimeistaan) || vahvistusPvm.isEqual(vahvistettuViimeistaan)))

    val hasYksilollistettyMA = huomioitavatValmiitOppimaarat.flatMap(_.aineet).exists(aine => aine.pakollinen && aine.koodi.arvo.equals("MA") && aine.yksilollistetty.exists(_.equals(true)))
    val hasYksilollistettyAI = huomioitavatValmiitOppimaarat.flatMap(_.aineet).exists(aine => aine.pakollinen && aine.koodi.arvo.equals("AI") && aine.yksilollistetty.exists(_.equals(true)))
    val oppimaarallaYksMatAi = hasYksilollistettyMA && hasYksilollistettyAI
    val hasKumoavaKorotus = huomioitavatOppiaineenOppimaarat.flatMap(_.oppiaineet).exists(aine => {
      aine.pakollinen && Set("AI", "MA").contains(aine.koodi.arvo) && !aine.yksilollistetty.exists(_.equals(true))
    })

    oppimaarallaYksMatAi && !hasKumoavaKorotus
  }


  def syncHarkinnanvaraisuusForHakemus(hakemus: AtaruValintalaskentaHakemus,
                                       opiskeluoikeudet: Seq[Opiskeluoikeus],
                                       ohjausparametrit: Ohjausparametrit,
                                       hakukohteet: Map[String, KoutaHakukohde]): HakemuksenHarkinnanvaraisuus = {

    val vahvistettuViimeistaan = ohjausparametrit.suoritustenVahvistuspaiva
      .map(svp => Instant.ofEpochMilli(svp.date)
        .atZone(ZoneId.of("Europe/Helsinki"))
        .toLocalDate).getOrElse(LocalDate.now())
    val tuoreinPeruskoulusuoritus = AvainArvoConverter.etsiViimeisinPeruskoulu(hakemus.personOid, opiskeluoikeudet)
    val isSupaYksMatAi = hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet, vahvistettuViimeistaan)
    val deadlineOhitettu = LocalDate.now().isAfter(vahvistettuViimeistaan)
    val pohjakoulutusHakemukselta = hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusKey)

    val isAtaruIlmoitettuYksMatAi = hakemus.keyValues.filter(kv => ataruMatematiikkaJaAidinkieliYksilollistettyQuestions.contains(kv._1)).values.exists(_.equals("1"))

    val tpk: Option[PerusopetuksenOppimaara] = tuoreinPeruskoulusuoritus._1
    val hakutoiveet = hakemus.hakutoiveet.map(hakutoive => {
      val hakukohteenHarkinnanvaraisuusHakemukselta: Option[String] = hakemus.keyValues.get(ataruHakukohdeHarkinnanvaraisuusPrefix + hakutoive.hakukohdeOid)
      val ilmoitettuVanhaPeruskoulu = hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusVuosiKey).map(_.toInt).exists(_ <= 2017)

      val hakukohde = hakukohteet(hakutoive.hakukohdeOid)
      val hakukohdeSalliiHarkinnanvaraisuuden = hakukohde.voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita.exists(_.equals(true))
      val syy = (hakukohdeSalliiHarkinnanvaraisuuden, tpk, pohjakoulutusHakemukselta, hakukohteenHarkinnanvaraisuusHakemukselta) match {
        case (false, _, _, _) => HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN_HAKUKOHDE
        case (true, Some(tuoreinPeruskoulu), _, _) if tuoreinPeruskoulu.supaTila.equals(SuoritusTila.KESKEYTYNYT) =>
          HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA
        case (true, Some(tuoreinPeruskoulu), _, _) if tuoreinPeruskoulu.supaTila.equals(SuoritusTila.KESKEN) && deadlineOhitettu =>
          HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA
        case (true, Some(tuoreinPeruskoulu), _, _) if tuoreinPeruskoulu.supaTila.equals(SuoritusTila.VALMIS) && isSupaYksMatAi =>
          HarkinnanvaraisuudenSyy.SURE_YKS_MAT_AI
        case (true, _, Some(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS), _) => HarkinnanvaraisuudenSyy.ATARU_ULKOMAILLA_OPISKELTU
        case (true, _, Some(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA), _) => HarkinnanvaraisuudenSyy.ATARU_EI_PAATTOTODISTUSTA
        case (true, _, _, _) if !ilmoitettuVanhaPeruskoulu => HarkinnanvaraisuudenSyy.ATARU_EI_PAATTOTODISTUSTA
        case (true, _, _, _) if ilmoitettuVanhaPeruskoulu && isAtaruIlmoitettuYksMatAi => HarkinnanvaraisuudenSyy.ATARU_YKS_MAT_AI
        case (true, _, _, Some(harkinnanvaraisuusHakukohteelleHakemukselta)) =>
          harkinnanvaraisuusHakukohteelleHakemukselta match {
            case "0" => HarkinnanvaraisuudenSyy.ATARU_OPPIMISVAIKEUDET
            case "1" => HarkinnanvaraisuudenSyy.ATARU_SOSIAALISET_SYYT
            case "2" => HarkinnanvaraisuudenSyy.ATARU_KOULUTODISTUSTEN_VERTAILUVAIKEUDET
            case "3" => HarkinnanvaraisuudenSyy.ATARU_RIITTAMATON_TUTKINTOKIELEN_TAITO
          }
        case _ => HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN
      }
      HakutoiveenHarkinnanvaraisuus(hakutoive.hakukohdeOid, syy)
    })
    HakemuksenHarkinnanvaraisuus(hakemus.hakemusOid, hakemus.personOid, hakutoiveet)
  }
}

class HarkinnanvaraisuusService {

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClient = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired val tarjontaIntegration: TarjontaIntegration = null

  def haeSupaTiedot(personOid: String): Seq[Opiskeluoikeus] = {
    val allOidsForPerson = Await.result(onrIntegration.getAliasesForPersonOids(Set(personOid)), 10.seconds).allOids
    allOidsForPerson.flatMap(oid => kantaOperaatiot.haeSuoritukset(oid).values.flatten).toSeq
  }

  def getHakemuksenHarkinnanvaraisuudet(hakemusOid: String) = {
    val hakemusF: Future[Seq[AtaruValintalaskentaHakemus]] = hakemuspalveluClient.getValintalaskentaHakemukset(None, true, Set(hakemusOid))
    val hakemus = Await.result(hakemusF, 20.seconds).headOption.getOrElse(throw new RuntimeException(s"Hakemusta $hakemusOid ei löytynyt!"))

    val hakemuksenHakukohteetMap = hakemus.hakutoiveet
      .map(hakutoive => tarjontaIntegration.getHakukohde(hakutoive.hakukohdeOid))
      .map(koutaHakukohde => (koutaHakukohde.oid, koutaHakukohde)).toMap
    val ohjausparametrit = tarjontaIntegration.getOhjausparametrit(hakemus.hakuOid)
    val suorituksetKannasta = kantaOperaatiot.haeSuoritukset(hakemusOid).flatMap(_._2).toSeq

    val hark = HarkinnanvaraisuusDeducer.syncHarkinnanvaraisuusForHakemus(hakemus, suorituksetKannasta, ohjausparametrit, hakemuksenHakukohteetMap)
  }
}
