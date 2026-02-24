package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, GeneerinenOpiskeluoikeus, KantaOperaatiot, Opiskeluoikeus, SuoritusTila, YOOpiskeluoikeus}
import fi.oph.suorituspalvelu.integration.OnrIntegration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

object AutomaattinenHakukelpoisuus {

  private val LOG = LoggerFactory.getLogger(AutomaattinenHakukelpoisuus.getClass)

  //Todo, add IB & DIA
  def getAutomaattinenHakukelpoisuus(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): Boolean = {
    val hasYo =
      opiskeluoikeudet.collect { case o: YOOpiskeluoikeus => o }
        .flatMap(_.yoTutkinto)
        .exists(yo => yo.valmistumisPaiva.isDefined && yo.supaTila.equals(SuoritusTila.VALMIS))
    val hasEb =
      opiskeluoikeudet.collect { case o: GeneerinenOpiskeluoikeus => o }
        .flatMap(_.suoritukset)
        .collect { case s: fi.oph.suorituspalvelu.business.EBTutkinto => s }
        .exists(eb => eb.vahvistusPaivamaara.isDefined && eb.supaTila.equals(SuoritusTila.VALMIS))
    val hasAmmatillinenPerustutkinto =
      opiskeluoikeudet.collect { case o: AmmatillinenOpiskeluoikeus => o }
        .flatMap(_.suoritukset)
        .collect { case s: fi.oph.suorituspalvelu.business.AmmatillinenPerustutkinto => s }
        .exists(amm => amm.vahvistusPaivamaara.isDefined && amm.supaTila.equals(SuoritusTila.VALMIS))
    val hasAmmatillinenTutkinto =
      opiskeluoikeudet.collect { case o: AmmatillinenOpiskeluoikeus => o }
        .flatMap(_.suoritukset)
        .collect { case s: fi.oph.suorituspalvelu.business.AmmattiTutkinto => s }
        .exists(amm => amm.vahvistusPaivamaara.isDefined && amm.supaTila.equals(SuoritusTila.VALMIS))
    val hasAmmatillinenErikoisTutkinto =
      opiskeluoikeudet.collect { case o: AmmatillinenOpiskeluoikeus => o }
        .flatMap(_.suoritukset)
        .collect { case s: fi.oph.suorituspalvelu.business.ErikoisAmmattiTutkinto => s }
        .exists(amm => amm.vahvistusPaivamaara.isDefined && amm.supaTila.equals(SuoritusTila.VALMIS))

    val automaattisestiHakukelpoinen = hasYo || hasEb || hasAmmatillinenPerustutkinto || hasAmmatillinenTutkinto || hasAmmatillinenErikoisTutkinto
    LOG.info(s"Henkilön $personOid automaattinen hakukelpoisuus: $automaattisestiHakukelpoinen " +
      s"(yo - $hasYo, eb - $hasEb, amm-perus - $hasAmmatillinenPerustutkinto, " +
      s"amm-tutkinto - $hasAmmatillinenTutkinto, amm-erikois - $hasAmmatillinenErikoisTutkinto)")
    automaattisestiHakukelpoinen
  }
}

@Component
class HakukelpoisuusService {
  private val LOG = LoggerFactory.getLogger(classOf[HakukelpoisuusService])

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val onrIntegration: OnrIntegration = null

  def haeSupaTiedot(personOid: String): Seq[Opiskeluoikeus] = {
    val allOidsForPerson = Await.result(onrIntegration.getAliasesForPersonOids(Set(personOid)), 10.seconds).allOids
    allOidsForPerson.flatMap(oid => kantaOperaatiot.haeSuoritukset(oid).values.flatten).toSeq
  }

  def getAutomaattinenHakukelpoisuus(personOid: String): Boolean = {
    try {
      val opiskeluoikeudet = haeSupaTiedot(personOid)
      AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(personOid, opiskeluoikeudet)
    } catch {
      case t: Throwable =>
        LOG.error(s"Virhe pääteltäessä automaattista hakukelpoisuutta henkilölle $personOid:", t)
        false
    }
  }

  def getHenkiloidenHakukelpoisuudet(henkiloOids: Set[String]): Map[String, Boolean] = {
    henkiloOids.map(oid => oid -> getAutomaattinenHakukelpoisuus(oid)).toMap
  }

}
