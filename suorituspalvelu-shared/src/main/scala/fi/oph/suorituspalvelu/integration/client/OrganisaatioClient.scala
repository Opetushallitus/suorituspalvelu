package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.vm.sade.javautils.nio.cas.CasClient
import org.asynchttpclient.{Dsl, RequestBuilder}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.jdk.javaapi.FutureConverters.asScala
import scala.concurrent.ExecutionContext.Implicits.global

case class OrganisaatioNimi(fi: String, sv: String, en: String)

case class Organisaatio(oid: String, nimi: OrganisaatioNimi)

// TODO: tämä pitää evaluoida uudelleen kun ruvetaan miettimään käyttöoikeuksia. Jos organisaatiohierarkia haetaan
//  kokonaisuudessaan niin tämä haku voi olla turha
class OrganisaatioClient(environmentBaseUrl: String) {

  val LOG = LoggerFactory.getLogger(classOf[HakemuspalveluClientImpl]);

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.registerModule(DefaultScalaModule)

  val asyncHttpClient = Dsl.asyncHttpClient()

  def haeOrganisaationTiedot(koodiArvo: String): Future[Option[Organisaatio]] =
    fetch(environmentBaseUrl + s"/organisaatio-service/api/${koodiArvo}?includeImage=false")
      .map(data => data.map(data => mapper.readValue(data, classOf[Organisaatio])))

  private def fetch(url: String): Future[Option[String]] =
    LOG.info(s"fetch, $url")
    val req = new RequestBuilder()
      .setMethod("GET")
      .setHeader("Content-Type", "application/json")
      .setUrl(url)
      .build()
    try {
      asScala(asyncHttpClient.executeRequest(req).toCompletableFuture).map {
        case r if r.getStatusCode == 200 =>
          Some(r.getResponseBody())
        case r if r.getStatusCode == 404 =>
          LOG.warn(s"haettiin tuntematonta organisaatiota osoitteesta $url")
          None
        case r =>
          val errorStr = s"Failed to fetch data from organisaatiopalvelu: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(
            errorStr
          )
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error(
          s"Failed to fetch data from organisaatiopalvelu", e
        )
        Future.failed(e)
    }

}
