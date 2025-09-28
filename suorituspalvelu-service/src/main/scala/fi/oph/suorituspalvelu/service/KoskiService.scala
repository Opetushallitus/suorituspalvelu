package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{KantaOperaatiot, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{KoskiDataForOppija, KoskiIntegration, SyncResultForHenkilo}
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, KoskiClient}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import slick.jdbc.JdbcBackend

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@Component
class KoskiService {

  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClientImpl = null

  @Autowired val koskiIntegration: KoskiIntegration = null
  
  @Autowired val koodistoProvider: KoodistoProvider = null

  private val HENKILO_TIMEOUT = 5.minutes

  private val LOG: Logger = LoggerFactory.getLogger(classOf[KoskiService])
  
  def syncKoskiForOppijat(personOids: Set[String]): Seq[SyncResultForHenkilo] =
    val kantaOperaatiot = KantaOperaatiot(database)
    
    koskiIntegration.fetchKoskiTiedotForOppijat(personOids).map(oppija => {
      try {
        val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(oppija.oppijaOid, SuoritusJoukko.KOSKI, oppija.data)
        versio.foreach(v => {
          LOG.info(s"Versio tallennettu henkilölle ${oppija.oppijaOid}")
          val oikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(oppija.data), koodistoProvider)
          kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(v, oikeudet.toSet)
        })
        SyncResultForHenkilo(oppija.oppijaOid, versio, None)
      } catch {
        case e: Exception =>
          LOG.error(s"Henkilon ${oppija.oppijaOid} Koski-tietojen tallentaminen epäonnistui", e)
          SyncResultForHenkilo(oppija.oppijaOid, None, Some(e))
      }
    }).toSeq
  
  def syncKoskiForHaku(hakuOid: String): Seq[SyncResultForHenkilo] = {
    val personOids =
      Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), HENKILO_TIMEOUT)
        .flatMap(_.personOid).toSet
    syncKoskiForOppijat(personOids)
  }

}
