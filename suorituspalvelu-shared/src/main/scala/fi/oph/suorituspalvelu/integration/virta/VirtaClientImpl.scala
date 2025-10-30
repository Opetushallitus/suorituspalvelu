package fi.oph.suorituspalvelu.integration.virta

import org.asynchttpclient.*
import org.asynchttpclient.Dsl.asyncHttpClient
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

case class OppijanumeroTaiHetu(oppijanumero: Option[String], hetu: Option[String])

trait VirtaClient {

  def haeTiedotHetulle(hetu: String): Future[String]
  def haeTiedotOppijanumerolle(oppijanumero: String): Future[String]
}

class VirtaClientImpl(jarjestelma: String, tunnus: String, avain: String, environmentBaseUrl: String) extends VirtaClient {

  private val client: AsyncHttpClient = asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setMaxRedirects(5).setConnectTimeout(Duration.ofMillis(10 * 1000)).build);

  val LOG = LoggerFactory.getLogger(classOf[VirtaClientImpl]);

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  def getSoapOperationEnvelope(oppijanumeroTaiHetu: Either[String, String]): String =
    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
    <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
      <SOAP-ENV:Body>
        <OpiskelijanKaikkiTiedotRequest xmlns="http://tietovaranto.csc.fi/luku">
          <Kutsuja>
            <jarjestelma>{jarjestelma}</jarjestelma>
            <tunnus>{tunnus}</tunnus>
            <avain>{avain}</avain>
          </Kutsuja>
          <Hakuehdot>
            {oppijanumeroTaiHetu.fold(
              oppijanumero => <kansallinenOppijanumero>{oppijanumero}</kansallinenOppijanumero>,
              hetu => <henkilotunnus>{hetu}</henkilotunnus>)}
          </Hakuehdot>
        </OpiskelijanKaikkiTiedotRequest>
      </SOAP-ENV:Body>
    </SOAP-ENV:Envelope>

  def post(url: String, payload: String): Future[String] = {
    val request = client
      .preparePost(url)
      .setHeader("Content-Type", "text/xml")
      .setBody(payload)
      .build()
    executeRequest(request)
  }

  def haeTiedotOppijanumerolle(oppijanumero: String): Future[String] = {
    post(environmentBaseUrl + "/luku/OpiskelijanTiedot", this.getSoapOperationEnvelope(Left(oppijanumero))).map(result => result)
  }

  def haeTiedotHetulle(hetu: String): Future[String] = {
    post(environmentBaseUrl + "/luku/OpiskelijanTiedot", this.getSoapOperationEnvelope(Right(hetu))).map(result => result)
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
