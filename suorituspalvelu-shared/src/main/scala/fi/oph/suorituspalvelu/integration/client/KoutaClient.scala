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

case class KoutaHakukohde(oid: String,
                          organisaatioOid: String,
                          nimi: Map[String, String],
                          voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita: Option[Boolean])

case class KoutaHaku(oid: String,
                     tila: String,
                     nimi: Map[String, String],
                     hakutapaKoodiUri: String,
                     kohdejoukkoKoodiUri: Option[String],
                     hakuajat: List[KoutaHakuaika],
                     kohdejoukonTarkenneKoodiUri: Option[String],
                     hakuvuosi: Option[Int]) {

  val toisenAsteenUrit = Set(
    "haunkohdejoukko_11",
    "haunkohdejoukko_20",
    "haunkohdejoukko_21",
    "haunkohdejoukko_22",
    "haunkohdejoukko_23",
    "haunkohdejoukko_24"
  )

  val toisenAsteenYhteishakuUri = "haunkohdejoukko_11"

  def isToisenAsteenHaku() = {
    val kohdejoukkoPrefix = kohdejoukkoKoodiUri.flatMap(_.split("#").headOption).getOrElse("")
    toisenAsteenUrit.contains(kohdejoukkoPrefix)
  }

  def isToisenAsteenYhteisHaku() = {
    val kohdejoukkoPrefix = kohdejoukkoKoodiUri.flatMap(_.split("#").headOption).getOrElse("")
    kohdejoukkoPrefix.equals(toisenAsteenYhteishakuUri)
  }
}


class KoutaClient(casClient: CasClient, environmentBaseUrl: String) {

  private val LOG = LoggerFactory.getLogger(classOf[KoutaClient])

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def fetchHakukohde(hakukohdeOid: String): Future[KoutaHakukohde] = {
    val url = environmentBaseUrl + "/kouta-internal/hakukohde/" + hakukohdeOid
    val hakukohdeTulosF: Future[Option[KoutaHakukohde]] = doGet(url).map(resultOpt => resultOpt.map(result => {
      mapper.readValue(result, classOf[KoutaHakukohde])
    }))
    hakukohdeTulosF.map(_.getOrElse(throw new RuntimeException(s"Hakukohdetta $hakukohdeOid ei löytynyt!")))
  }

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
