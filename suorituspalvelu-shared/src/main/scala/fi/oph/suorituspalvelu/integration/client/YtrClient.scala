package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.integration.ytr.Student
import fi.oph.suorituspalvelu.integration.{KoskiMassaluovutusQueryParams, KoskiMassaluovutusQueryResponse}
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClientConfig, Dsl, Realm, Request, Response}
import org.slf4j.LoggerFactory

import java.util.Base64
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import java.time.Duration


case class YtlHetuPostData(ssn: String,
                           previousSsns: Option[Seq[String]])

class YtrClient(username: String, password: String, environmentBaseUrl: String) {

  private val client: AsyncHttpClient = asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setMaxRedirects(5).setConnectTimeout(Duration.ofMillis(10 * 1000)).build);

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val LOG = LoggerFactory.getLogger(classOf[KoskiClient])

  def fetchOne(hetu: YtlHetuPostData): Future[Option[String]] = {
    val base = "https://registry.integration.yo-test.ylioppilastutkinto.fi"
    val url = base + "/api/oph-transfer/student"

    val resultF: Future[Option[String]] = postWithBasicAuth(url, hetu).map(result => {
      if (result.isEmpty) {
        LOG.info(s"Ei löytynyt ytr-tietoja parametreille $hetu")
        result
      } else {
        LOG.info(s"Saatiin vastaus ytr: ${result}")
        //val parsittu = mapper.readValue[Student](result.get, classOf[Student])
        //LOG.info(s"Parsittiin ytr: ${parsittu}")
        //Some(parsittu)
        result
      }
    })

    resultF
  }

  private def encodeBasicAuth(username: String, password: String) = {
    "Basic " + Base64.getEncoder.encodeToString((username + ":" + password).getBytes)
  }

  def postWithBasicAuth(url: String, payload: Object): Future[Option[String]] = {
    val payloadString = mapper.writeValueAsString(payload)
    val realm = new Realm.Builder(username, password)
      .setUsePreemptiveAuth(true)
      //.setScheme(Realm.AuthScheme.NTLM)
      .build
    val request = client
      .preparePost(url)
      .setRealm(realm)
      .setHeader("Authorization", encodeBasicAuth(username, password))
      .setHeader("Content-Type", "application/json")
      .setBody(payloadString)
      .build()
    LOG.info(s"About to execute request $request")
    executeRequest(request)
  }


  /**
   * Execute the HTTP request and handle the response asynchronously.
   *
   * @param request The constructed request to execute.
   * @return A `Future[String]` containing the response body.
   */
  private def executeRequest(request: Request): Future[Option[String]] = {
    val promise = Promise[Option[String]]()
    val listenableFuture = client.executeRequest(request)

    listenableFuture.addListener(() => {
      Try(listenableFuture.get()) match {
        case Success(response) if response.getStatusCode >= 200 && response.getStatusCode < 300 =>
          promise.success(Some(response.getResponseBody))
        case Success(response) if response.getStatusCode == 404 => promise.success(None)
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
