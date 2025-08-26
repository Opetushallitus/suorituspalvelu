package fi.oph.suorituspalvelu.integration.client


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClientConfig, Dsl, Request, Response}
import org.slf4j.LoggerFactory

import java.util.Base64
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import java.time.Duration



case class YtrHetuPostData(ssn: String,
                           previousSsns: Option[Seq[String]])

case class YtrMassOperationQueryResponse(created: String, name: String, finished: Option[String], failure: Option[String], status: Option[String])

case class YtrMassOperationCreateResponse(operationUuid: String)

case class YtrMassOperation(uuid: String)

class YtrClient(username: String, password: String, baseUrl: String) {

  val CALLER_ID = "1.2.246.562.10.00000000001.suorituspalvelu"

  val config = new DefaultAsyncHttpClientConfig.Builder()
    .setMaxRedirects(5)
    .setConnectTimeout(Duration.ofMillis(10 * 1000))
    .build

  private val client: AsyncHttpClient = asyncHttpClient(config);

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val LOG = LoggerFactory.getLogger(classOf[YtrClient])

  def createYtrMassOperation(data: Seq[YtrHetuPostData]): Future[YtrMassOperation] = {
    LOG.info(s"Haetaan massahakuna yhteensä ${data.size} henkilön tiedot")
    val url = baseUrl + "/api/oph-transfer/bulk"
    postWithBasicAuth(url, data).map(result => {
      if (result.isEmpty) {
        throw new RuntimeException(s"Massahaku ${data.size} henkilölle epäonnistui")
      } else {
        LOG.info(s"Saatiin vastaus ${result}")
        val response = mapper.readValue[YtrMassOperationCreateResponse](result.get, classOf[YtrMassOperationCreateResponse])
        YtrMassOperation(response.operationUuid)
      }
    })
  }

  def pollMassOperation(uuid: String): Future[YtrMassOperationQueryResponse] = {
    val url = baseUrl + "/api/oph-transfer/status/" + uuid
    getWithBasicAuthAsByteArray(url).map((rawResult: Option[Array[Byte]]) =>
      val str = rawResult.map(r => new String(r, "UTF-8")).getOrElse("")
      LOG.info(s"Saatiin pollausvastaus: $str")
      mapper.readValue[YtrMassOperationQueryResponse](str, classOf[YtrMassOperationQueryResponse]))
  }

  def fetchOne(data: YtrHetuPostData): Future[Option[String]] = {
    val url = baseUrl + "/api/oph-transfer/student"
    postWithBasicAuth(url, data).map(result => {
      if (result.isEmpty) {
        LOG.info(s"Ei löytynyt ytr-tietoja parametreille $data")
        result
      } else {
        LOG.info(s"Saatiin vastaus ytr parametreille $data: ${result}")
        result
      }
    })
  }

  private def encodeBasicAuth(username: String, password: String) = {
    "Basic " + Base64.getEncoder.encodeToString((username + ":" + password).getBytes)
  }

  def getWithBasicAuthAsByteArray(url: String): Future[Option[Array[Byte]]] = {
    val request = client
      .prepareGet(url)
      .setHeader("Authorization", encodeBasicAuth(username, password))
      .setHeader("Content-Type", "application/json")
      .build()
    LOG.info(s"About to execute request $request")
    executeRequestAsByteArray(request)
  }

  def postWithBasicAuth(url: String, payload: Object): Future[Option[String]] = {
    val payloadString = mapper.writeValueAsString(payload)
    val request = client
      .preparePost(url)
      .setHeader("Authorization", encodeBasicAuth(username, password))
      .setHeader("Content-Type", "application/json")
      .setBody(payloadString)
      .build()
    executeRequestAsByteArray(request).map(result => result.map(r => new String(r, "UTF-8")))
  }

  private def executeRequestAsByteArray(request: Request): Future[Option[Array[Byte]]] = {
    val promise = Promise[Option[Array[Byte]]]()
    val listenableFuture = client.executeRequest(request)

    listenableFuture.addListener(() => {
      Try(listenableFuture.get()) match {
        case Success(response) if response.getStatusCode >= 200 && response.getStatusCode < 300 =>
          promise.success(Some(response.getResponseBodyAsBytes))
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
