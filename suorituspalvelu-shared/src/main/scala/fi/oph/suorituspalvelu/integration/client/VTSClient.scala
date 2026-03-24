package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.vm.sade.javautils.nio.cas.CasClient
import org.asynchttpclient.RequestBuilder
import org.slf4j.LoggerFactory

import com.fasterxml.jackson.core.`type`.TypeReference

import scala.concurrent.Future
import scala.jdk.javaapi.FutureConverters.asScala
import scala.concurrent.ExecutionContext.Implicits.global

case class OpintopolkuVastaanotto(personOid: String, hakuOid: String, hakukohdeOid: String, vastaanottoAction: String, vastaanottoaika: String)

case class VanhaVastaanotto(personOid: String, hakukohde: String, vastaanottoaika: String)

case class Vastaanotot(opintopolku: Seq[OpintopolkuVastaanotto], vanhat: Seq[VanhaVastaanotto])

case class Ensikertalaisuus(personOid: String, paattyi: Option[String])

class VTSClient(casClient: CasClient, environmentBaseUrl: String) {

  private val LOG = LoggerFactory.getLogger(classOf[VTSClient])

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def fetchVastaanotot(henkiloOid: String): Future[Option[Vastaanotot]] = {
    val url = environmentBaseUrl + s"/valinta-tulos-service/ensikertalaisuus/$henkiloOid/historia"
    doGet(url).map(resultOpt => resultOpt.map(result => {
      mapper.readValue(result, classOf[Vastaanotot])
    }))
  }

  def fetchEnsikertalaisuudet(henkiloOids: Seq[String]): Future[Seq[Ensikertalaisuus]] = {
    val url = environmentBaseUrl + "/valinta-tulos-service/ensikertalaisuus?koulutuksenAlkamiskausi=2014S"
    doPost(url, henkiloOids).map(result => {
      val typeRef = new TypeReference[Seq[Ensikertalaisuus]] {}
      mapper.readValue(result, typeRef)
    })
  }

  private def doPost(url: String, body: Object): Future[String] = {
    LOG.debug(s"haetaan POST, $url")
    val req = new RequestBuilder()
      .setMethod("POST")
      .setHeader("Content-Type", "application/json")
      .setBody(mapper.writeValueAsString(body))
      .setUrl(url)
      .build()
    try {
      asScala(casClient.execute(req)).map {
        case r if r.getStatusCode == 200 =>
          r.getResponseBody()
        case r =>
          val errorStr = s"Haku valinta-tulos-servicestä epäonnistui: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(
            errorStr
          )
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error(
          s"Haku valinta-tulos-servicestä epäonnistui", e
        )
        Future.failed(e)
    }
  }

  private def doGet(url: String): Future[Option[String]] = {

    LOG.debug(s"haetaan, $url")
    val req = new RequestBuilder()
      .setMethod("GET")
      .setUrl(url)
      .build()
    try {
      asScala(casClient.execute(req)).map {
        case r if r.getStatusCode == 200 =>
          Some(r.getResponseBody())
        case r if r.getStatusCode == 404 =>
          None
        case r =>
          val errorStr = s"Haku valinta-tulos-servicestä epäonnistui: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(
            errorStr
          )
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error(
          s"Haku valinta-tulos-servicestä epäonnistui", e
        )
        Future.failed(e)
    }
  }
}
