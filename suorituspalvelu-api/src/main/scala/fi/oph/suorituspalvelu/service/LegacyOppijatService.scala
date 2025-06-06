package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenTutkinto, KantaOperaatiot, NuortenPerusopetuksenOppiaineenOppimaara, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, Telma, YOOpiskeluoikeus}
import fi.oph.suorituspalvelu.integration.client.{AtaruHenkiloSearchParams, HakemuspalveluClient}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import slick.jdbc.JdbcBackend

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Komot {
  val perusopetus = "1.2.246.562.13.62959769647"
  val perusopetuksenOppiaineenOppimaara = "TODO perusopetuksenOppiaineenOppimäärä"
  val ammatillinen = "TODO ammatillinen komo oid"
  val yoTutkinto = "1.2.246.562.5.2013061010184237348007"
  val telma = "telma"
}

case class LegacySuoritus(suoritusKieli: String, komo: String)

case class LegacySuoritusJaArvosanat(suoritus: LegacySuoritus)

case class LegacyOppija(oppijanumero: String, suoritukset: List[LegacySuoritusJaArvosanat])

@Component
class LegacyOppijatService {

  final val TIMEOUT = 30.seconds

  val LOG = LoggerFactory.getLogger(classOf[LegacyOppijatService])

  @Autowired val hakemuspalveluClient: HakemuspalveluClient = null

  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  def getOppijat(hakuOid: String, hakukohdeOid: Option[String]): Seq[LegacyOppija] = {
    val kantaOperaatiot = KantaOperaatiot(database)
    val hakijat = Await.result(hakemuspalveluClient.getHaunHakijat(AtaruHenkiloSearchParams(hakukohdeOid.map(oid => List(oid)), Some(hakuOid), None)), TIMEOUT)
    hakijat
      .filter(h => h.personOid.isDefined)
      .map(h => {
        val suoritukset = kantaOperaatiot.haeSuoritukset(h.personOid.get)
          .values
          .flatten
          .map(oo => oo match {
            case oo: PerusopetuksenOpiskeluoikeus => Set(
              oo.suoritukset
                .filter(s => s.isInstanceOf[PerusopetuksenOppimaara])
                .map(s => s.asInstanceOf[PerusopetuksenOppimaara])
                .map(poo => Some(LegacySuoritusJaArvosanat(LegacySuoritus(poo.suoritusKieli.arvo, Komot.perusopetus)))),
              oo.suoritukset
                .filter(s => s.isInstanceOf[NuortenPerusopetuksenOppiaineenOppimaara])
                .map(s => s.asInstanceOf[NuortenPerusopetuksenOppiaineenOppimaara])
                .map(poo => Some(LegacySuoritusJaArvosanat(LegacySuoritus(poo.suoritusKieli.arvo, Komot.perusopetuksenOppiaineenOppimaara)))),
            ).flatten
            case oo: AmmatillinenOpiskeluoikeus => Set(
              oo.suoritukset
                .filter(s => s.isInstanceOf[AmmatillinenTutkinto])
                .map(s => s.asInstanceOf[AmmatillinenTutkinto])
                .map(at => Some(LegacySuoritusJaArvosanat(LegacySuoritus(at.suoritusKieli.arvo, Komot.ammatillinen)))),
              oo.suoritukset
                .filter(s => s.isInstanceOf[Telma])
                .map(s => s.asInstanceOf[Telma])
                .map(t => Some(LegacySuoritusJaArvosanat(LegacySuoritus(t.suoritusKieli.arvo, Komot.telma)))),
            ).flatten
            case oo: YOOpiskeluoikeus => Set(Some(LegacySuoritusJaArvosanat(LegacySuoritus(oo.yoTutkinto.suoritusKieli.arvo, Komot.yoTutkinto))))
            case default => None
          })
          .flatten
          .flatten
          .toList
        
        LegacyOppija(h.personOid.get, suoritukset)
      })
  }
}
