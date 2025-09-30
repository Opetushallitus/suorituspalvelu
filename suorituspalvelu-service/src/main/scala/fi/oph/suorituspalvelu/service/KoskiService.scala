package fi.oph.suorituspalvelu.service

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import com.github.kagkarlsson.scheduler.task.helper.{RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.{FixedDelay, Schedules}
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{KoskiDataForOppija, KoskiIntegration, SyncResultForHenkilo}
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, KoskiClient}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiOppijaFilter, KoskiParser, KoskiToSuoritusConverter}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component
import slick.jdbc.JdbcBackend

import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@Configuration
class KoskiConfiguration {

  @Autowired var koskiService: KoskiService = null

  val KOSKI_POLL_CHANGED_TASK: TaskDescriptor[Instant] = TaskDescriptor.of("koski-poll", classOf[Instant]);

  @Bean
  def koskiPollTask(koskiClient: KoskiClient) = Tasks.recurring(KOSKI_POLL_CHANGED_TASK, Schedules.cron("0 */2 * * * *")).executeStateful((inst, ctx) => {
    val start = Instant.now()
    val prevStart = Option.apply(inst.getData)
    if(prevStart.isDefined) // tyhjä tarkoittaa ettei taskia ajettu koskaan tässä ympäristössä
      koskiService.syncKoskiChangesSince(prevStart.get.minusSeconds(60))

    start
  })
}

@Component
class KoskiService {

  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClientImpl = null

  @Autowired val koskiIntegration: KoskiIntegration = null
  
  @Autowired val koodistoProvider: KoodistoProvider = null

  val LOG = LoggerFactory.getLogger(classOf[KoskiService])

  private val HENKILO_TIMEOUT = 5.minutes

  def syncKoskiChangesSince(since: Instant): Seq[SyncResultForHenkilo] =
    val fetchedAt = Instant.now()
    val tiedot = koskiIntegration.fetchMuuttuneetKoskiTiedotSince(since)
    val filtteroity = tiedot.filter(r => {
      val opiskeluoikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(r.data), koodistoProvider).toSet
      KoskiOppijaFilter.isYsiluokkalainen(opiskeluoikeudet)
    })
    processKoskiDataForOppijat(filtteroity, fetchedAt)

  def syncKoskiForOppijat(personOids: Set[String]): Seq[SyncResultForHenkilo] = {
    val fetchedAt = Instant.now()
    processKoskiDataForOppijat(koskiIntegration.fetchKoskiTiedotForOppijat(personOids), fetchedAt)
  }

  def syncKoskiForHaku(hakuOid: String): Seq[SyncResultForHenkilo] =
    val personOids =
      Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), HENKILO_TIMEOUT)
        .flatMap(_.personOid).toSet
    syncKoskiForOppijat(personOids)

  private def processKoskiDataForOppijat(data: Seq[KoskiDataForOppija], fetchedAt: Instant): Seq[SyncResultForHenkilo] =
    val kantaOperaatiot = KantaOperaatiot(database)

    data.map(oppija => {
      try {
        val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(oppija.oppijaOid, SuoritusJoukko.KOSKI, oppija.data, fetchedAt)
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
    })

}
