package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.vm.sade.javautils.nio.cas.CasClient
import org.asynchttpclient.{Dsl, RequestBuilder}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.jdk.javaapi.FutureConverters.asScala
import scala.concurrent.ExecutionContext.Implicits.global

case class Koodisto(koodistoUri: String)

case class KoodiMetadata(kieli: String, nimi: String)

case class Koodi(koodiArvo: String, koodisto: Koodisto, metadata: List[KoodiMetadata])

class KoodistoClient(environmentBaseUrl: String) {

  val LOG = LoggerFactory.getLogger(classOf[HakemuspalveluClientImpl]);

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.registerModule(DefaultScalaModule)

  val asyncHttpClient = Dsl.asyncHttpClient()

  def haeKoodisto(koodisto: String): Future[Map[String, Koodi]] =
    fetch(environmentBaseUrl + s"/koodisto-service/rest/json/${koodisto}/koodi")
      .map(data => mapper.readValue(data, classOf[Array[Koodi]])).map(koodit => koodit.map(k => k.koodiArvo -> k).toMap)

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
          val errorStr = s"Failed to fetch data from koodistopalvelu: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(
            errorStr
          )
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error(
          s"Failed to fetch data from koodistopalvelu", e
        )
        Future.failed(e)
    }

}
