package fi.oph.suorituspalvelu.configuration

import fi.oph.suorituspalvelu.business.KantaOperaatiot
import fi.oph.suorituspalvelu.jobs.SupaScheduler
import fi.oph.suorituspalvelu.service.ErrorService
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.{Bean, Configuration, Profile}
import org.springframework.context.event.{ContextClosedEvent, EventListener}

import java.time.Duration
import javax.sql.DataSource

@Configuration
class JobConfiguration {

  @Bean def getScheduler(dataSource: DataSource, kantaOperaatiot: KantaOperaatiot, errorService: ErrorService): SupaScheduler =
    new SupaScheduler(5, Duration.ofSeconds(2), dataSource, kantaOperaatiot, errorService)

}

@Configuration
class StartScheduler {

  @Autowired var scheduler: SupaScheduler = _
  
  @EventListener(Array(classOf[ApplicationReadyEvent])) def onApplicationReady(event: ApplicationReadyEvent): Unit =
    scheduler.start()

  @EventListener(Array(classOf[ContextClosedEvent])) def onContextClosed(event: ContextClosedEvent): Unit =
    scheduler.stop()
}