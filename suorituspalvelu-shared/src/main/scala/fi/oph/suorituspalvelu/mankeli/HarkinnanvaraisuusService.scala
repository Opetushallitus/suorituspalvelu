package fi.oph.suorituspalvelu.mankeli

import org.springframework.beans.factory.annotation.Autowired
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, SuoritusTila}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, HakemuspalveluClient, KoutaHakukohde, Ohjausparametrit}
import org.springframework.stereotype.Component

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

object HarkinnanvaraisuusPaattely {

  //Tämän jälkeen suoritettuja ma/ai yksilöllistämisiä ei enää huomioida harkinnanvaraisuuspäättelyssä.
  // Oppiaineen oppimäärän suoritukset (korotukset) kuitenkin huomioidaan myös tämän jälkeen.
  val YKS_MAT_AI_SUORITUS_ENNEN_DATE = LocalDate.parse("2025-08-01")

  val ataruMatematiikkaJaAidinkieliYksilollistettyQuestions = Set("matematiikka-ja-aidinkieli-yksilollistetty_1", "matematiikka-ja-aidinkieli-yksilollistetty_2")
  val ataruHakukohdeHarkinnanvaraisuusPrefix = "harkinnanvaraisuus-reason_"

  def hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Boolean = {
    val perusopetusOOs = opiskeluoikeudet.collect { case o: PerusopetuksenOpiskeluoikeus => o }

    val huomioitavatValmiitOppimaarat = perusopetusOOs
      .flatMap(_.suoritukset).collect { case om: PerusopetuksenOppimaara => om }
      .filter(_.supaTila.equals(SuoritusTila.VALMIS))
      .filter(_.vahvistusPaivamaara.exists(!_.isAfter(vahvistettuViimeistaan)))
      .filter(_.vahvistusPaivamaara.exists(!_.isAfter(YKS_MAT_AI_SUORITUS_ENNEN_DATE)))
    val huomioitavatOppiaineenOppimaarat = perusopetusOOs
      .flatMap(_.suoritukset).collect { case oom: PerusopetuksenOppimaaranOppiaineidenSuoritus => oom }
      .filter(_.supaTila.equals(SuoritusTila.VALMIS))
      .filter(_.vahvistusPaivamaara.exists(vahvistusPvm => vahvistusPvm.isBefore(vahvistettuViimeistaan) || vahvistusPvm.isEqual(vahvistettuViimeistaan)))

    val hasYksilollistettyMA = huomioitavatValmiitOppimaarat.flatMap(_.aineet).exists(aine => aine.pakollinen && aine.koodi.arvo.equals("MA") && aine.yksilollistetty.exists(_.equals(true)))
    val hasYksilollistettyAI = huomioitavatValmiitOppimaarat.flatMap(_.aineet).exists(aine => aine.pakollinen && aine.koodi.arvo.equals("AI") && aine.yksilollistetty.exists(_.equals(true)))
    val oppimaarallaYksMatAi = hasYksilollistettyMA && hasYksilollistettyAI
    val hasKumoavaKorotus = huomioitavatOppiaineenOppimaarat.flatMap(_.aineet).exists(aine => {
      aine.pakollinen && Set("AI", "MA").contains(aine.koodi.arvo) && !aine.yksilollistetty.exists(_.equals(true))
    })

    oppimaarallaYksMatAi && !hasKumoavaKorotus
  }


  def syncHarkinnanvaraisuusForHakemus(hakemus: AtaruValintalaskentaHakemus,
                                       opiskeluoikeudet: Seq[Opiskeluoikeus],
                                       vahvistettuViimeistaan: LocalDate,
                                       hakukohteet: Map[String, KoutaHakukohde]): HakemuksenHarkinnanvaraisuus = {

    val tuoreinPeruskoulusuoritus = AvainArvoConverter.etsiViimeisinPeruskoulu(hakemus.personOid, opiskeluoikeudet)

    val deadlineOhitettu = LocalDate.now().isAfter(vahvistettuViimeistaan)
    val pohjakoulutusHakemukselta = hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusKey).flatMap(v => Option.apply(v))

    val isSupaYksMatAi = hasYksilollistettyMatematiikkaJaAidinkieli(opiskeluoikeudet, vahvistettuViimeistaan)
    val isAtaruIlmoitettuYksMatAi = hakemus.keyValues.filter(kv => ataruMatematiikkaJaAidinkieliYksilollistettyQuestions.contains(kv._1)).values.exists(_.equals("1"))

    val hakutoiveet = hakemus.hakutoiveet.map(hakutoive => {
      val hakukohteenHarkinnanvaraisuusHakemukselta: Option[String] = hakemus.keyValues.get(ataruHakukohdeHarkinnanvaraisuusPrefix + hakutoive.hakukohdeOid).flatMap(v => Option.apply(v))
      val ilmoitettuVanhaPeruskoulu = hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusVuosiKey).flatMap(v => Option.apply(v)).map(_.toInt).exists(_ <= 2017)

      val hakukohde = hakukohteet.getOrElse(hakutoive.hakukohdeOid, throw new RuntimeException(s"Hakukohdetta ${hakutoive.hakukohdeOid} ei löytynyt!"))
      val hakukohdeSalliiHarkinnanvaraisuuden = hakukohde.voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita.exists(_.equals(true))
      val syy = (hakukohdeSalliiHarkinnanvaraisuuden, tuoreinPeruskoulusuoritus, pohjakoulutusHakemukselta, hakukohteenHarkinnanvaraisuusHakemukselta) match {
        case (false, _, _, _) => HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN_HAKUKOHDE
        case (true, Some(tuoreinPeruskoulu), _, _) if tuoreinPeruskoulu.supaTila.equals(SuoritusTila.KESKEYTYNYT) =>
          HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA
        case (true, Some(tuoreinPeruskoulu), _, _) if tuoreinPeruskoulu.supaTila.equals(SuoritusTila.KESKEN) && deadlineOhitettu =>
          HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA
        case (true, Some(tuoreinPeruskoulu), _, _) if tuoreinPeruskoulu.supaTila.equals(SuoritusTila.VALMIS) && isSupaYksMatAi =>
          HarkinnanvaraisuudenSyy.SURE_YKS_MAT_AI
        case (true, None, Some(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS), _) => HarkinnanvaraisuudenSyy.ATARU_ULKOMAILLA_OPISKELTU
        case (true, None, Some(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA), _) => HarkinnanvaraisuudenSyy.ATARU_EI_PAATTOTODISTUSTA
        case (true, None, _, _) if !ilmoitettuVanhaPeruskoulu => HarkinnanvaraisuudenSyy.ATARU_EI_PAATTOTODISTUSTA
        case (true, None, _, _) if ilmoitettuVanhaPeruskoulu && isAtaruIlmoitettuYksMatAi => HarkinnanvaraisuudenSyy.ATARU_YKS_MAT_AI
        case (true, None, _, Some(harkinnanvaraisuusHakukohteelleHakemukselta)) =>
          harkinnanvaraisuusHakukohteelleHakemukselta match {
            case "0" => HarkinnanvaraisuudenSyy.ATARU_OPPIMISVAIKEUDET
            case "1" => HarkinnanvaraisuudenSyy.ATARU_SOSIAALISET_SYYT
            case "2" => HarkinnanvaraisuudenSyy.ATARU_KOULUTODISTUSTEN_VERTAILUVAIKEUDET
            case "3" => HarkinnanvaraisuudenSyy.ATARU_RIITTAMATON_TUTKINTOKIELEN_TAITO
            case default => throw new RuntimeException(s"Tuntematon arvo hakemukselta hakukohteen ${hakutoive.hakukohdeOid} harkinnanvaraisuudeksi: $default")
          }
        case _ => HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN
      }
      HakutoiveenHarkinnanvaraisuus(hakutoive.hakukohdeOid, syy)
    })
    HakemuksenHarkinnanvaraisuus(hakemus.hakemusOid, hakemus.personOid, hakutoiveet)
  }
}

@Component
class HarkinnanvaraisuusService {

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClient = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired val tarjontaIntegration: TarjontaIntegration = null

  def haeSupaTiedot(personOid: String): Seq[Opiskeluoikeus] = {
    val allOidsForPerson = Await.result(onrIntegration.getAliasesForPersonOids(Set(personOid)), 10.seconds).allOids
    allOidsForPerson.flatMap(oid => kantaOperaatiot.haeSuoritukset(oid).values.flatten).toSeq
  }

  def getHakemuksenHarkinnanvaraisuudet(hakemus: AtaruValintalaskentaHakemus, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate) = {
    val hakemuksenHakukohteetMap = hakemus.hakutoiveet
      .map(hakutoive => tarjontaIntegration.getHakukohde(hakutoive.hakukohdeOid))
      .map(koutaHakukohde => (koutaHakukohde.oid, koutaHakukohde)).toMap
    HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(hakemus, opiskeluoikeudet, vahvistettuViimeistaan, hakemuksenHakukohteetMap)
  }

  def getHakemustenHarkinnanvaraisuudet(hakemusOids: Set[String]): Set[HakemuksenHarkinnanvaraisuus] = {
    if (hakemusOids.isEmpty) {
      Set.empty
    } else {
      val hakemuksetF: Future[Seq[AtaruValintalaskentaHakemus]] = hakemuspalveluClient.getValintalaskentaHakemukset(None, true, hakemusOids)
      val hakemukset = Await.result(hakemuksetF, 2.minutes)

      val hakuOidsToHakemukset = hakemukset.groupBy(_.hakuOid)

      hakuOidsToHakemukset.flatMap { case (hakuOid, hakemuksetForHaku) =>
        val ohjausparametrit = tarjontaIntegration.getOhjausparametrit(hakuOid)
        val vahvistusPaiva = ohjausparametrit.getVahvistuspaivaLocalDate

        //Haetaan valmiiksi kaikkien hakemusten hakutoiveita vastaavat hakukohteet
        val hakukohteetMap =
          hakemuksetForHaku.flatMap(_.hakutoiveet.map(_.hakukohdeOid)).toSet
            .map(oid => tarjontaIntegration.getHakukohde(oid))
            .map(hakukohde => (hakukohde.oid, hakukohde)).toMap

        hakemuksetForHaku.map { hakemus =>
          val opiskeluoikeudet = haeSupaTiedot(hakemus.personOid)
          HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(
            hakemus,
            opiskeluoikeudet,
            vahvistusPaiva,
            hakukohteetMap
          )
        }
      }.toSet
    }
  }


  def getHakemuksenHarkinnanvaraisuudet(hakemusOid: String): HakemuksenHarkinnanvaraisuus = {
    val hakemusF: Future[Seq[AtaruValintalaskentaHakemus]] = hakemuspalveluClient.getValintalaskentaHakemukset(None, true, Set(hakemusOid))
    val hakemus = Await.result(hakemusF, 20.seconds).headOption.getOrElse(throw new RuntimeException(s"Hakemusta $hakemusOid ei löytynyt!"))

    val hakemuksenHakukohteetMap = hakemus.hakutoiveet
      .map(hakutoive => tarjontaIntegration.getHakukohde(hakutoive.hakukohdeOid))
      .map(koutaHakukohde => (koutaHakukohde.oid, koutaHakukohde)).toMap
    val ohjausparametrit = tarjontaIntegration.getOhjausparametrit(hakemus.hakuOid)
    val suorituksetKannasta = haeSupaTiedot(hakemus.personOid)

    HarkinnanvaraisuusPaattely.syncHarkinnanvaraisuusForHakemus(hakemus, suorituksetKannasta, ohjausparametrit.getVahvistuspaivaLocalDate, hakemuksenHakukohteetMap)
  }
}
