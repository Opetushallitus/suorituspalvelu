package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenTutkinto, KantaOperaatiot, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, YOOpiskeluoikeus}
import fi.oph.suorituspalvelu.integration.client.{AtaruHenkiloSearchParams, HakemuspalveluClient}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import slick.jdbc.JdbcBackend

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

case class LegacySuoritus(suoritusKieli: String)

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
            case oo: PerusopetuksenOpiskeluoikeus => oo.suoritukset
              .filter(s => s.isInstanceOf[PerusopetuksenOppimaara])
              .map(s => s.asInstanceOf[PerusopetuksenOppimaara])
              .map(poo => Some(LegacySuoritusJaArvosanat(LegacySuoritus("kieli"))))
            case oo: AmmatillinenOpiskeluoikeus => oo.suoritukset
              .filter(s => s.isInstanceOf[AmmatillinenTutkinto])
              .map(s => s.asInstanceOf[AmmatillinenTutkinto])
              .map(at => Some(LegacySuoritusJaArvosanat(LegacySuoritus(at.suoritusKieli.arvo))))
            case oo: YOOpiskeluoikeus => Set(Some(LegacySuoritusJaArvosanat(LegacySuoritus(oo.yoTutkinto.suoritusKieli.arvo))))
            case default => None
          })
          .flatten
          .flatten
          .toList
        
        LegacyOppija(h.personOid.get, suoritukset)
      })
  }
}
