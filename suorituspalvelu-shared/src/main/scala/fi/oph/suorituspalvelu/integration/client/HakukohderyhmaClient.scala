package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.vm.sade.javautils.nio.cas.CasClient
import org.asynchttpclient.RequestBuilder
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.jdk.javaapi.FutureConverters.asScala
import scala.concurrent.ExecutionContext.Implicits.global

case class HakukohteenHakukohderyhmat(oid: String, hakukohderyhmat: List[String])

class HakukohderyhmaClient(casClient: CasClient, environmentBaseUrl: String) {

  private val LOG = LoggerFactory.getLogger(classOf[HakukohderyhmaClient])

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def fetchHakukohderyhmat(hakukohdeOids: Set[String]): Future[List[HakukohteenHakukohderyhmat]] = {
    val url = environmentBaseUrl + "/hakukohderyhmapalvelu/api/hakukohderyhma/search/by-hakukohteet"
    val req = new RequestBuilder()
      .setMethod("POST")
      .setHeader("Content-Type", "application/json")
      .setBody(mapper.writeValueAsString(hakukohdeOids))
      .setUrl(url)
      .build()
    try {
      asScala(casClient.execute(req)).map {
        case r if r.getStatusCode == 200 =>
          val typeRef = new TypeReference[List[HakukohteenHakukohderyhmat]] {}
          mapper.readValue(r.getResponseBody(), typeRef)
        case r =>
          val errorStr = s"Haku hakukohderyhmapalvelusta epäonnistui: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(errorStr)
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error("Haku hakukohderyhmapalvelusta epäonnistui", e)
        Future.failed(e)
    }
  }
}
