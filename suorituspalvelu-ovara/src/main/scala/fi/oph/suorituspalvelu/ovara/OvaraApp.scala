package fi.oph.suorituspalvelu.ovara

import fi.oph.suorituspalvelu.business.{KantaOperaatiot, SiirtotiedostoOperaatio}
import fi.oph.suorituspalvelu.configuration.{DBConfiguration, IntegrationConfiguration}
import fi.oph.suorituspalvelu.service.{MuodostamisTulos, OvaraParams, OvaraService, ValintaDataService}
import org.slf4j.LoggerFactory
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

  private val LOG = LoggerFactory.getLogger(classOf[OvaraRunner])

  @Autowired var ovaraService: OvaraService = _
  @Autowired var kantaOperaatiot: KantaOperaatiot = _

  override def run(args: String*): Unit = {
    val params    = OvaraParams(
      vainAktiiviset = true,
      avainArvot = true,
      harkinnanvaraisuudet = true,
      ensikertalaisuudet = true
    )
    val operaatio: SiirtotiedostoOperaatio = kantaOperaatiot.aloitaSiirtotiedostoOperaatio(params.executionId)
    LOG.info(s"(${params.executionId}) Siirtotiedostonmuodostusoperaatio aloitettu (#${operaatio.id}), ikkuna: ${operaatio.windowStart} – ${operaatio.windowEnd}. $operaatio")
    try {
      val opiskeluoikeudetOnnistuneet = ovaraService.muodostaOpiskeluoikeusSiirtotiedostot(params, operaatio.windowStart, operaatio.windowEnd)

      val paivittaisetTulos = if (operaatio.paivittaiset) {
        LOG.info(s"(${params.executionId}) Muodostetaan paivittaiset siirtotiedostot (valintadata, harkinnanvaraiset, ensikertalaisuudet)")
        ovaraService.muodostaPaivittaisetHauille(params)
      } else MuodostamisTulos(0, Map.empty)

      val errorMessage = if (paivittaisetTulos.epaonnistuneetHaut.nonEmpty)
        Some(s"Epäonnistuneet haut: ${paivittaisetTulos.epaonnistuneetHaut.map((oid, msg) => s"$oid: $msg").mkString(", ")}")
      else None
      //Tulkitaan onnistuneeksi operaatioksi sellainenkin muodostus, jossa joillekin hauille tapahtui virheitä.
      kantaOperaatiot.paataSiirtotiedostoOperaatio(
        operaatio.id,
        success = true,
        errorMessage,
        Map("valintadata" -> paivittaisetTulos.onnistuneet, "opiskeluoikeudet" -> opiskeluoikeudetOnnistuneet)
      )
    } catch {
      case e: Exception =>
        LOG.error(s"(${params.executionId}) Operaatio epäonnistui odottamattomasti", e)
        kantaOperaatiot.paataSiirtotiedostoOperaatio(operaatio.id, false, Some(e.getMessage), Map.empty)
        System.exit(1)
    }
    System.exit(0)
  }
}

object OvaraApp {

  def main(args: Array[String]): Unit =
    SpringApplication.run(classOf[OvaraApp], args*)
}
