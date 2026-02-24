package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.asynchttpclient.{Dsl, RequestBuilder}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.jdk.javaapi.FutureConverters.asScala
import scala.concurrent.ExecutionContext.Implicits.global

case class VanhaTarjontaHakukohde(
  oid: String,
  hakukohteenNimet: Map[String, String],
  tarjoajaNimet: Map[String, String],
  tarjoajaOids: List[String],
  hakuOid: String
)

case class VanhaTarjontaHakukohdeResult(result: VanhaTarjontaHakukohde)

case class VanhaTarjontaHaku(oid: String, nimi: Map[String, String])

case class VanhaTarjontaHakuResult(result: List[VanhaTarjontaHaku])

class VanhaTarjontaClient(environmentBaseUrl: String) {

  val LOG = LoggerFactory.getLogger(classOf[VanhaTarjontaClient])

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.registerModule(DefaultScalaModule)

  val asyncHttpClient = Dsl.asyncHttpClient()

  def haeHakukohde(hakukohdeOid: String): Future[VanhaTarjontaHakukohde] =
    fetch(environmentBaseUrl + s"/tarjonta-service/rest/v1/hakukohde/$hakukohdeOid")
      .map(data => mapper.readValue(data, classOf[VanhaTarjontaHakukohdeResult]).result)

  def haeHaut(): Future[List[VanhaTarjontaHaku]] =
    fetch(environmentBaseUrl + "/tarjonta-service/rest/v1/haku/find?addHakukohdes=false")
      .map(data => mapper.readValue(data, classOf[VanhaTarjontaHakuResult]).result)

  private def fetch(url: String): Future[String] =
    LOG.info(s"fetch, $url")
    val req = new RequestBuilder()
      .setMethod("GET")
      .setHeader("Content-Type", "application/json")
      .setUrl(url)
      .build()
    try {
      asScala(asyncHttpClient.executeRequest(req).toCompletableFuture).map {
        case r if r.getStatusCode == 200 =>
          r.getResponseBody()
        case r =>
          val errorStr = s"Failed to fetch data from tarjonta-service: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(
            errorStr
          )
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error(
          s"Failed to fetch data from tarjonta-service", e
        )
        Future.failed(e)
    }

}
