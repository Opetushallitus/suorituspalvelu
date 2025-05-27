package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.asynchttpclient.RequestBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.jdk.javaapi.FutureConverters.asScala

//Todo, oma ec?
import scala.concurrent.ExecutionContext.Implicits.global

case class AtaruHenkiloSearchParams(hakukohdeOids: Option[List[String]], hakuOid: Option[String])


case class AtaruHakemuksenHenkilotiedot(oid: String, //hakemuksen oid
                                        personOid: Option[String],
                                        ssn: Option[String])

case class AtaruResponseHenkilot(applications: List[AtaruHakemuksenHenkilotiedot],
                                 offset: Option[String])

case class CasParams(user: String, password: String, casUrl: String, envBaseUrl: String)

trait HakemuspalveluClient {
  def getHaunHakijat(params: AtaruHenkiloSearchParams): Future[Seq[AtaruHakemuksenHenkilotiedot]]
}

class HakemuspalveluClientImpl(casClient: CasClient) extends HakemuspalveluClient {


  val LOG = LoggerFactory.getLogger(classOf[HakemuspalveluClientImpl]);

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)


  override def getHaunHakijat(params: AtaruHenkiloSearchParams): Future[Seq[AtaruHakemuksenHenkilotiedot]] = {
    fetch("https://virkailija.testiopintopolku.fi/lomake-editori/api/external/suoritusrekisteri/henkilot", params)
      .map(data => {
        //Todo handle offset/paging! Currently the ataru api external/suoritusrekisteri/henkilot
        // returns the first 200k hakemukses, which is of course enough for most cases/hakus.
        val parsed: AtaruResponseHenkilot = mapper.readValue[AtaruResponseHenkilot](data, classOf[AtaruResponseHenkilot])
        LOG.info(s"Offset: ${parsed.offset}")
        LOG.info(s"Saatiin hakemuspalvelusta ${parsed.applications.size} hakemuksen henkilötiedot parametreille $params")
        parsed.applications
      })
  }

  private def fetch(url: String, body: AtaruHenkiloSearchParams): Future[String] = {
    val bodyMap = Map("hakuOid" -> body.hakuOid.get)
    LOG.info(s"fetch, $url $body ${mapper.writeValueAsString(body)} ${mapper.writeValueAsString(bodyMap)}")
    val req = new RequestBuilder()
      .setMethod("POST")
      .setHeader("Content-Type", "application/json")
      .setBody(mapper.writeValueAsString(bodyMap))
      .setUrl(url)
      .build()
    try {
      asScala(casClient.execute(req)).map {
        case r if r.getStatusCode == 200 =>
          r.getResponseBody()
        case r =>
          val errorStr = s"Failed to fetch data from hakemuspalvelu: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(
            errorStr
          )
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error(
          s"Failed to fetch data from hakemuspalvelu", e
        )
        Future.failed(e)
    }
  }
}
