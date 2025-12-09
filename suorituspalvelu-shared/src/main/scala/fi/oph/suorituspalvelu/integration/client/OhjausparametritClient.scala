package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClientConfig, Dsl, Request, Response}

import java.time.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

case class DateParam(date: Long)

case class Ohjausparametrit(PH_HKP: Option[DateParam] = None, suoritustenVahvistuspaiva: Option[DateParam] = None, valintalaskentapaiva: Option[DateParam] = None)

class OhjausparametritClient(environmentBaseUrl: String) {

  private val client: AsyncHttpClient = asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setMaxRedirects(5).setConnectTimeout(Duration.ofMillis(10 * 1000)).build);

  private val LOG = LoggerFactory.getLogger(classOf[OhjausparametritClient])
  private val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  def haeOhjausparametrit(hakuOid: String): Future[Ohjausparametrit] = {
    val url = environmentBaseUrl + "/ohjausparametrit-service/api/v1/rest/parametri/" + hakuOid
    val request = client
      .prepareGet(url)
      .build()
    executeRequest(request).map(resultStr => {
      val parsedResult: Ohjausparametrit = mapper.readValue(resultStr, classOf[Ohjausparametrit])
      LOG.info(s"Parsitut ohjausparametrit: $parsedResult")
      parsedResult
    })
  }

  def haeKaikkiOhjausparametrit(): Future[Map[String, Ohjausparametrit]] = {
    val url = environmentBaseUrl + "/ohjausparametrit-service/api/v1/rest/parametri/ALL"
    val request = client
      .prepareGet(url)
      .build()
    executeRequest(request).map(resultStr => {
      val typeRef = new TypeReference[Map[String, Ohjausparametrit]] {}
      val parsedResult: Map[String, Ohjausparametrit] = mapper.readValue(resultStr, typeRef)
      LOG.info(s"Parsitut ohjausparametrit: $parsedResult")
      parsedResult
    })
  }

  /**
   * Execute the HTTP request and handle the response asynchronously.
   *
   * @param request The constructed request to execute.
   * @return A `Future[String]` containing the response body.
   */
  private def executeRequest(request: Request): Future[String] = {
    val promise = Promise[String]()
    val listenableFuture = client.executeRequest(request)

    listenableFuture.addListener(() => {
      Try(listenableFuture.get()) match {
        case Success(response) if response.getStatusCode >= 200 && response.getStatusCode < 300 =>
          promise.success(response.getResponseBody)
        case Success(response) =>
          promise.failure(
            new RuntimeException(
              s"HTTP Error: ${response.getStatusCode} - ${response.getResponseBody}"
            )
          )
        case Failure(exception) =>
          promise.failure(exception)
      }
    }, ec.execute(_))

    promise.future
  }
}
