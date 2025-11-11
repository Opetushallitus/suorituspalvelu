package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.{FailureHandler, TaskDescriptor}
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.suorituspalvelu.integration.TarjontaIntegration
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, YtrClient}
import fi.oph.suorituspalvelu.integration.ytr.YtrIntegration
import fi.oph.suorituspalvelu.service.YTRService.{YTR_REFRESH_TASK_FOR_HAUT}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Service

import java.time.Duration.ofSeconds
import scala.concurrent.duration.DurationInt
import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.jdk.CollectionConverters.*

object YTRService {

  val YTR_REFRESH_TASK_FOR_HAUT: TaskDescriptor[String] = TaskDescriptor.of("ytr-refresh-haut", classOf[String]);
}

@Configuration
class YTRRefresh {

  @Autowired var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @Autowired val ytrIntegration: YtrIntegration = null

  final val TIMEOUT = 30.seconds

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  @Bean
  def ytrRefreshTaskForHaut(virtaClient: YtrClient) = Tasks.oneTime(YTR_REFRESH_TASK_FOR_HAUT)
    .onFailure(new FailureHandler.MaxRetriesFailureHandler(1, new FailureHandler.ExponentialBackoffFailureHandler(ofSeconds(30), 2)))
    .execute((instance, ctx) => {
      val hakuOids: Seq[String] = mapper.readValue(instance.getData, classOf[Seq[String]])
      hakuOids.foreach(hakuOid => {
          val personOids = Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), TIMEOUT).flatMap(_.personOid).toSet
        ytrIntegration.fetchAndPersistStudents(personOids)
      })
    })
}

@Service
class YTRService {

  @Autowired val scheduler: Scheduler = null

  @Autowired val tarjontaIntegration: TarjontaIntegration = null

  @Autowired val mapper: ObjectMapper = null

  def syncYTRForAktiivisetHaut(): UUID = {
    val paivitettavatHaut = tarjontaIntegration.aktiivisetHaut()
      .filter(haku => !haku.kohdejoukkoKoodiUri.contains("11"))

    val taskId = UUID.randomUUID();
    this.scheduler.schedule(YTR_REFRESH_TASK_FOR_HAUT.instance(taskId.toString).data(mapper.writeValueAsString(paivitettavatHaut.asJava)).scheduledTo(Instant.now()))
    taskId
  }
}