package fi.oph.suorituspalvelu.service

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.suorituspalvelu.integration.virta.VirtaClient
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import fi.oph.suorituspalvelu.service.VirtaService.VIRTA_REFRESH_TASK
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component

import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object VirtaService {
  val VIRTA_REFRESH_TASK: TaskDescriptor[String] = TaskDescriptor.of("virta-refresh", classOf[String]);
}

@Configuration
class VirtaRefresh {

  final val TIMEOUT = 30.seconds

  @Bean
  def virtaRefreshTask(virtaClient: VirtaClient) = Tasks.oneTime(VIRTA_REFRESH_TASK).execute((instance, ctx) => {
    val virtaXMLs = Await.result(virtaClient.haeKaikkiTiedot(instance.getData, None), TIMEOUT)
    val parseroidut = virtaXMLs.map(r => VirtaParser.parseVirtaData(new ByteArrayInputStream(r.getBytes)))
    val konvertoidut = parseroidut.map(p => VirtaToSuoritusConverter.toSuoritukset(p).toSet).flatten

    // TODO: tallennus tapahtuu tässä
  })
}

@Component
class VirtaService {

  @Autowired val scheduler: Scheduler = null

  def syncVirta(oppijaNumero: String): UUID =
    val taskId = UUID.randomUUID();
    this.scheduler.schedule(VIRTA_REFRESH_TASK.instance(taskId.toString).data(oppijaNumero).scheduledTo(Instant.now()))
    taskId

}
