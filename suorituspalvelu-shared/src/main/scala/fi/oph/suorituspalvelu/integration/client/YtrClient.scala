package fi.oph.suorituspalvelu.integration.client


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.util.ZipUtil
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClientConfig, Dsl, Request, Response}
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import java.time.Duration



case class YtlHetuPostData(ssn: String,
                           previousSsns: Option[Seq[String]])

case class YtrMassOperationQueryResponse(created: String, name: String, finished: Option[String], failure: Option[String], status: Option[String])

case class YtrMassOperationCreateResponse(operationUuid: String)

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

  val LOG = LoggerFactory.getLogger(classOf[KoskiClient])

  case class YtrMassOperation(uuid: String)
  def createYtrMassOperation(data: Seq[YtlHetuPostData]): Future[YtrMassOperation] = {
    LOG.info(s"Haetaan massahakuna yhteensä ${data.size} henkilön tiedot")
    val url = baseUrl + "/api/oph-transfer/bulk"
    //val url = "https://registry.integration.yo-test.ylioppilastutkinto.fi:28090/api/oph-transfer/bulk"

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
    //val url = "https://registry.integration.yo-test.ylioppilastutkinto.fi:28090/api/oph-transfer/status/" + uuid
    getWithBasicAuthAsByteArray(url).map((rawResult: Option[Array[Byte]]) =>
      val str = rawResult.map(r => new String(r, "UTF-8")).getOrElse("")
      LOG.info(s"Saatiin pollausvastaus: $str")
      mapper.readValue[YtrMassOperationQueryResponse](str, classOf[YtrMassOperationQueryResponse]))
  }

  def fetchOne(data: YtlHetuPostData): Future[Option[String]] = {
    val url = baseUrl + "/api/oph-transfer/student"
    //val url = "https://registry.integration.yo-test.ylioppilastutkinto.fi:28090/api/oph-transfer/student"

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

  def fetchAndDecompressZip(uuid: String): Future[Map[String, String]] = {
    val url = baseUrl + "/api/oph-transfer/bulk/" + uuid
    getWithBasicAuthAsByteArray(url).map((result: Option[Array[Byte]]) => {
      LOG.info(s"Haettiin massa-zip, käsitellään. $uuid - ${result.map(_.length).getOrElse(0L)} bytes")
      result.map(bytes => ZipUtil.unzipStreamByFile(new ByteArrayInputStream(bytes))).getOrElse(Map.empty)
    })
  }

  private def encodeBasicAuth(username: String, password: String) = {
    //LOG.info(s"Basic auth username: $username, password: $password")
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

  // Execute request and return byte array response
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

  def postWithBasicAuth(url: String, payload: Object): Future[Option[String]] = {
    val payloadString = mapper.writeValueAsString(payload)
    LOG.info(s"Payload string: $payloadString")
    val request = client
      .preparePost(url)
      .setHeader("Authorization", encodeBasicAuth(username, password))
      .setHeader("Content-Type", "application/json")
      .setBody(payloadString)
      .build()
    //LOG.info(s"About to execute request $request, data ${request.getStringData}")
    //Todo, voidaan varmaan käyttää kaikkeen ByteArrayn palauttavaa executea
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
