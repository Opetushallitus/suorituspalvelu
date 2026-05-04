package fi.oph.suorituspalvelu.ovara

import fi.oph.suorituspalvelu.configuration.{DBConfiguration, IntegrationConfiguration}
import fi.oph.suorituspalvelu.service.{OvaraParams, OvaraService, ValintaDataService}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.{CommandLineRunner, SpringApplication}
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.{SecurityAutoConfiguration, SecurityFilterAutoConfiguration}
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.context.annotation.{ComponentScan, Import}
import org.springframework.stereotype.Component

// Scan only packages that OvaraApp needs. Explicit package list avoids web-only
// configuration classes (Tomcat, WebMvcConfigurer, etc.) in the service module.
// Integration beans come from OvaraIntegrationConfiguration (ovara-specific subset).
@SpringBootApplication(exclude = Array(
  classOf[SecurityAutoConfiguration],
  classOf[SecurityFilterAutoConfiguration],
  classOf[WebMvcAutoConfiguration],
  classOf[ErrorMvcAutoConfiguration]
))
@ComponentScan(basePackages = Array(
  "fi.oph.suorituspalvelu.mankeli",
  "fi.oph.suorituspalvelu.parsing",
  "fi.oph.suorituspalvelu.ovara"
))
@Import(Array(
  classOf[IntegrationConfiguration],
  classOf[DBConfiguration],
  classOf[OvaraService],
  classOf[ValintaDataService]
))
class OvaraApp

@Component
class OvaraRunner extends CommandLineRunner {

  @Autowired var ovaraService: OvaraService = _

  override def run(args: String*): Unit =
    ovaraService.muodostaPaivittaisetHauille(OvaraParams())
}

object OvaraApp {

  def main(args: Array[String]): Unit =
    SpringApplication.run(classOf[OvaraApp], args*)
}
