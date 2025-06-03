package fi.oph.suorituspalvelu.integration.client
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.integration.{KoskiMassaluovutusQueryParams, KoskiMassaluovutusQueryResponse}
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClientConfig, Dsl, Realm, Request, Response}
import org.slf4j.LoggerFactory

import java.util.Base64
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import java.time.Duration

class KoskiClient(username: String, password: String, environmentBaseUrl: String) {

  private val client: AsyncHttpClient = asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setMaxRedirects(5).setConnectTimeout(Duration.ofMillis(10 * 1000)).build);

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val LOG = LoggerFactory.getLogger(classOf[KoskiClient])

  def pollQuery(url: String): Future[KoskiMassaluovutusQueryResponse] = {
    getWithBasicAuth(url).map(rawResult => mapper.readValue[KoskiMassaluovutusQueryResponse](rawResult, classOf[KoskiMassaluovutusQueryResponse]))
  }

  def getWithBasicAuth(url: String, followRedirects: Boolean = false): Future[String] = {
    LOG.debug(s"Get with basic auth, url: $url")
    val realm = new Realm.Builder(username, password)
      .setUsePreemptiveAuth(false)
      .setScheme(Realm.AuthScheme.NTLM)
      .build
    val request = client
      .prepareGet(url)
      .setRealm(realm)
      .setHeader("Authorization", encodeBasicAuth(username, password))
      .setHeader("Content-Type", "application/json")
      .setFollowRedirect(true)
      .build()
    executeRequest(request)
  }

  def postWithBasicAuth(url: String, payload: Object): Future[String] = {
    val payloadString = mapper.writeValueAsString(payload)
    val realm = new Realm.Builder(username, password)
      .setUsePreemptiveAuth(false)
      .setScheme(Realm.AuthScheme.NTLM)
      .build
    val request = client
      .preparePost(url)
      .setRealm(realm)
      .setHeader("Authorization", encodeBasicAuth(username, password))
      .setHeader("Content-Type", "application/json")
      .setBody(payloadString)
      .build()
    //LOG.info(s"About to execute request $request")
    executeRequest(request)
  }

  def createMassaluovutusQuery(params: KoskiMassaluovutusQueryParams): Future[KoskiMassaluovutusQueryResponse] = {
    postWithBasicAuth(environmentBaseUrl+"/koski/api/massaluovutus", params).map(result =>
      val parsed: KoskiMassaluovutusQueryResponse = mapper.readValue[KoskiMassaluovutusQueryResponse](result, classOf[KoskiMassaluovutusQueryResponse])
      LOG.info(s"Saatiin vastaus massaluovutusrajapinnalta: ${parsed.getTruncatedLoggable()}")
      parsed)}

  private def encodeBasicAuth(username: String, password: String) = {
    "Basic " + Base64.getEncoder.encodeToString((username + ":" + password).getBytes)
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

  def close(): Unit = client.close()
}