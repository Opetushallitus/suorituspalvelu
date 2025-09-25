package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.vm.sade.javautils.nio.cas.CasClient
import org.asynchttpclient.RequestBuilder
import org.slf4j.LoggerFactory

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import scala.jdk.javaapi.FutureConverters.asScala
import scala.concurrent.ExecutionContext.Implicits.global

//aikaleimat muodossa "2022-02-22T08:00:00"
case class KoutaHakuaika(alkaa: String, paattyy: Option[String])

case class KoutaHaku(oid: String,
                     tila: String,
                     nimi: Map[String, String],
                     hakutapaKoodiUri: String,
                     kohdejoukkoKoodiUri: Option[String],
                     hakuajat: List[KoutaHakuaika],
                     kohdejoukonTarkenneKoodiUri: Option[String])


class KoutaClient(casClient: CasClient, environmentBaseUrl: String) {

  private val LOG = LoggerFactory.getLogger(classOf[KoutaClient])

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def fetchHaut(): Future[Map[String, KoutaHaku]] = {
    val url = environmentBaseUrl + "/kouta-internal/haku/search"
    val hakuTulos: Future[Seq[KoutaHaku]] = doGet(url).map(resultOpt => {
      resultOpt.map(result => {
        val typeRef = new TypeReference[List[KoutaHaku]] {}
        mapper.readValue(result, typeRef)
      }).getOrElse(List.empty)
    })
    hakuTulos.map(ht => ht.map(h => h.oid -> h).toMap)
  }


  private def doGet(url: String): Future[Option[String]] = {

    LOG.info(s"haetaan, $url")
    val req = new RequestBuilder()
      .setMethod("GET")
      .setUrl(url)
      .build()
    try {
      asScala(casClient.execute(req)).map {
        case r if r.getStatusCode == 200 =>
          Some(r.getResponseBody())
        case r if r.getStatusCode == 404 =>
          None
        case r =>
          val errorStr = s"Haku Ohjausparametreista epäonnistui: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(
            errorStr
          )
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error(
          s"Haku Ohjausparametreista epäonnistui", e
        )
        Future.failed(e)
    }
  }
}
