package fi.oph.suorituspalvelu.integration.virta

import org.asynchttpclient.*
import org.asynchttpclient.Dsl.asyncHttpClient


import java.time.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

//Todo, erotetaan hetu oppijanumerosta kun on selvää miten tiedot tallennetaan. Haetaan ja tallennetaan toistaiseksi tiedot vain oppijanumerolle.
case class VirtaResultForHenkilo(oppijanumeroTaiHetu: String, resultXml: String)

trait VirtaClient {

  def haeKaikkiTiedot(oppijanumero: String, hetu: Option[String]): Future[Seq[VirtaResultForHenkilo]]
}

class VirtaClientImpl(jarjestelma: String, tunnus: String, avain: String, environmentBaseUrl: String) extends VirtaClient {

  private val client: AsyncHttpClient = asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setMaxRedirects(5).setConnectTimeout(Duration.ofMillis(10 * 1000)).build);

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

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

  def haeTiedotOppijanumerolle(oppijanumero: String): Future[VirtaResultForHenkilo] = {
    post(environmentBaseUrl + "/luku/OpiskelijanTiedot", this.getSoapOperationEnvelope(Left(oppijanumero))).map(result => VirtaResultForHenkilo(oppijanumero, result))
  }

  def haeTiedotHetulle(hetu: String): Future[VirtaResultForHenkilo] = {
    post(environmentBaseUrl + "/luku/OpiskelijanTiedot", this.getSoapOperationEnvelope(Right(hetu))).map(result => VirtaResultForHenkilo(hetu, result))
  }

  def haeKaikkiTiedot(oppijanumero: String, hetu: Option[String]): Future[Seq[VirtaResultForHenkilo]] = {
    val futures =
      Seq(
        Some(haeTiedotOppijanumerolle(oppijanumero))
        //hetu.map(haeTiedotHetulle) todo ei haeta eikä tallenneta tietoja hetuille toistaiseksi.
      ).filter(_.isDefined).flatten
    Future.sequence(futures)
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
