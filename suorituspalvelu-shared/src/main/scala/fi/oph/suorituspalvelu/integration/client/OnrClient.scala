package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrMasterHenkilo}
import fi.vm.sade.javautils.nio.cas.CasClient
import org.asynchttpclient.RequestBuilder
import org.slf4j.LoggerFactory
import scala.concurrent.Future
import scala.jdk.javaapi.FutureConverters.asScala

//Todo, oma ec?
import scala.concurrent.ExecutionContext.Implicits.global

case class Henkiloviite(henkiloOid: String, masterOid: String) {
  def bothOids: Set[String] = Set(henkiloOid, masterOid)
}

trait OnrClient {
  def getHenkiloviitteetForHenkilot(personOids: Set[String]): Future[Set[Henkiloviite]]
  def getMasterHenkilosForPersonOids(personOids: Set[String]): Future[Map[String, OnrMasterHenkilo]]
  def getAsiointikieli(personOid: String): Future[Option[String]]
  def getPerustiedotByPersonOids(personOids: Set[String]): Future[Seq[OnrHenkiloPerustiedot]]
  def getPerustiedotByHetus(personOids: Set[String]): Future[Seq[OnrHenkiloPerustiedot]]
}

class OnrClientImpl(casClient: CasClient, environmentBaseUrl: String) extends OnrClient {

  val LOG = LoggerFactory.getLogger(classOf[OnrClientImpl]);

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JavaTimeModule())
  mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  val onrBatchSize = 5000

  override def getMasterHenkilosForPersonOids(henkiloOids: Set[String]): Future[Map[String, OnrMasterHenkilo]] = {
    val batches: Seq[(Set[String], Int)] = henkiloOids
      .grouped(onrBatchSize)
      .zipWithIndex
      .toList
    LOG.info(
      s"Haetaan ${henkiloOids.size} henkilöä ${batches.size}:n henkilön osissa"
    )
    batches.foldLeft(Future(Map[String, OnrMasterHenkilo]())) {
      case (result: Future[Map[String, OnrMasterHenkilo]], chunk: (Set[String], Int)) =>
        result.flatMap(rs => {
          LOG.info(
            s"Haetaan ${chunk._1.size}:n henkilön osaa ${chunk._2 + 1 + "/" + batches.size}"
          )
          val chunkResult: Future[Map[String, OnrMasterHenkilo]] = {
            doPost(environmentBaseUrl + "/oppijanumerorekisteri-service/henkilo/masterHenkilosByOidList", chunk._1)
              .map(result => {
                val typeRef = new TypeReference[Map[String, OnrMasterHenkilo]] {}
                mapper.readValue(result, typeRef)
              })
          }
          chunkResult.map(cr => rs ++ cr)
        })
    }
  }

  override def getPerustiedotByHetus(hetus: Set[String]): Future[Seq[OnrHenkiloPerustiedot]] = {
    doPost(environmentBaseUrl + "/oppijanumerorekisteri-service/henkilo/henkiloPerustietosByHenkiloHetuList", hetus)
      .map(result => {
        val typeRef = new TypeReference[Seq[OnrHenkiloPerustiedot]] {}
        mapper.readValue(result, typeRef)
      })
  }

  override def getPerustiedotByPersonOids(personOids: Set[String]): Future[Seq[OnrHenkiloPerustiedot]] = {
    doPost(environmentBaseUrl + "/oppijanumerorekisteri-service/henkilo/henkiloPerustietosByHenkiloOidList", personOids)
      .map(result => {
        val typeRef = new TypeReference[Seq[OnrHenkiloPerustiedot]] {}
        mapper.readValue(result, typeRef)
      })
  }

  override def getHenkiloviitteetForHenkilot(henkiloOids: Set[String]): Future[Set[Henkiloviite]] = {
    val batches: Seq[(Set[String], Int)] = henkiloOids.grouped(onrBatchSize).zipWithIndex.toList

    val allResults: Future[Set[Henkiloviite]] = batches.foldLeft(Future(Set[Henkiloviite]())) {
      case (result: Future[Set[Henkiloviite]], chunk: (Set[String], Int)) =>
        result.flatMap(rs => {
          LOG.info(
            s"Haetaan ${chunk._1.size}:n henkilön osaa ${chunk._2 + 1 + "/" + batches.size}"
          )
          val queryObject: Map[String, Set[String]] = Map("henkiloOids" -> chunk._1)
          val chunkResult: Future[Set[Henkiloviite]] = {
            doPost(environmentBaseUrl + "/oppijanumerorekisteri-service/s2s/duplicateHenkilos", queryObject)
              .map(result => {
                LOG.info(s"Saatiin tulos: $result")
                val typeRef = new TypeReference[List[Henkiloviite]] {}
                val parsed = mapper.readValue(result, typeRef).toSet
                LOG.info(s"Tiedot oppijanumerorekisteristä haettu, erä ${chunk._2 + 1}/${batches.size}, henkiloviitteet: ${parsed.size}")
                parsed
              })
          }
          chunkResult.map(cr => rs ++ cr)
        })
    }
    allResults
  }

  override def getAsiointikieli(personOid: String): Future[Option[String]] = {
    doGet(s"$environmentBaseUrl/oppijanumerorekisteri-service/henkilo/$personOid/asiointiKieli")
  }

  private def doPost(url: String, body: Object): Future[String] = {
    val req = new RequestBuilder()
      .setMethod("POST")
      .setHeader("Content-Type", "application/json")
      .setBody(mapper.writeValueAsString(body))
      .setUrl(url)
      .build()
    try {
      asScala(casClient.execute(req)).map {
        case r if r.getStatusCode == 200 =>
          r.getResponseBody()
        case r =>
          val errorStr = s"Haku oppijanumerorekisteristä epäonnistui: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(
            errorStr
          )
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error(
          s"Haku oppijanumerorekisteristä epäonnistui", e
        )
        Future.failed(e)
    }
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
          val errorStr = s"Haku oppijanumerorekisteristä epäonnistui: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(
            errorStr
          )
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error(
          s"Haku oppijanumerorekisteristä epäonnistui", e
        )
        Future.failed(e)
    }
  }
}
