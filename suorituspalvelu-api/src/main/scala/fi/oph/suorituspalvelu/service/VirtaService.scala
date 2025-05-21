package fi.oph.suorituspalvelu.service

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.{FailureHandler, TaskDescriptor}
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.suorituspalvelu.integration.virta.VirtaClient
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import fi.oph.suorituspalvelu.service.VirtaService.VIRTA_REFRESH_TASK
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component

import java.io.ByteArrayInputStream
import java.time.Duration.ofSeconds
import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import fi.oph.suorituspalvelu.service.VirtaService.LOG

object VirtaService {

  val LOG = LoggerFactory.getLogger(classOf[VirtaService])

  val VIRTA_REFRESH_TASK: TaskDescriptor[String] = TaskDescriptor.of("virta-refresh", classOf[String]);
}

@Configuration
class VirtaRefresh {

  final val TIMEOUT = 30.seconds

  @Bean
  def virtaRefreshTask(virtaClient: VirtaClient) = Tasks.oneTime(VIRTA_REFRESH_TASK)
    .onFailure(new FailureHandler.MaxRetriesFailureHandler(6, new FailureHandler.ExponentialBackoffFailureHandler(ofSeconds(1), 2)))
    .execute((instance, ctx) => {
    val oppijaNumero = instance.getData.split(":").head
    val hetu = instance.getData.split(":").tail.head
    try
      val virtaXMLs = Await.result(virtaClient.haeKaikkiTiedot(oppijaNumero, { if(hetu.isBlank) None else Some(hetu)}), TIMEOUT)
      val parseroidut = virtaXMLs.map(r => VirtaParser.parseVirtaData(new ByteArrayInputStream(r.getBytes)))
      val konvertoidut = parseroidut.map(p => VirtaToSuoritusConverter.toSuoritukset(p).toSet).flatten

      // TODO: tallennus tapahtuu tässä

      LOG.info(s"Päivitettiin Virta-tiedot oppijanumerolle ${oppijaNumero}, yhteensä ${konvertoidut.size} suoritusta.")
    catch
      case e: Exception => LOG.error(s"Virhe päivettäessä Virta-tietoja oppijanumerolle ${oppijaNumero}", e)
  })
}

@Component
class VirtaService {

  @Autowired val scheduler: Scheduler = null

  def syncVirta(oppijaNumero: String, hetu: Option[String]): UUID =
    val taskId = UUID.randomUUID();
    this.scheduler.schedule(VIRTA_REFRESH_TASK.instance(taskId.toString).data(oppijaNumero + ":" + hetu.getOrElse("")).scheduledTo(Instant.now()))
    taskId

}
