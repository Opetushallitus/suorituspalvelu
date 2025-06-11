package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.integration.OnrMasterHenkilo
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.asynchttpclient.RequestBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import scala.concurrent.duration.DurationInt
import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.jdk.javaapi.FutureConverters.asScala

//Todo, oma ec?
import scala.concurrent.ExecutionContext.Implicits.global

case class Henkiloviite(henkiloOid: String, masterOid: String) {
  def bothOids: Set[String] = Set(henkiloOid, masterOid)
}

trait OnrClient {
  def getHenkiloviitteetForHenkilot(personOids: Set[String]): Future[Set[Henkiloviite]]
  def getMasterHenkilosForPersonOids(personOids: Set[String]): Future[Map[String, OnrMasterHenkilo]]
}

class OnrClientImpl(casClient: CasClient, environmentBaseUrl: String) extends OnrClient {

  val LOG = LoggerFactory.getLogger(classOf[OnrClientImpl]);

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  val onrBatchSize = 5000


  override def getMasterHenkilosForPersonOids(henkiloOids: Set[String]): Future[Map[String, OnrMasterHenkilo]] = {
    val batches: Seq[(Set[String], Int)] = henkiloOids
      .grouped(onrBatchSize)
      .zipWithIndex
      .toList
    LOG.info(
      s"fetch Henkilot in ${batches.size} batches for ${henkiloOids.size} henkilos"
    )
    batches.foldLeft(Future(Map[String, OnrMasterHenkilo]())) {
      case (result: Future[Map[String, OnrMasterHenkilo]], chunk: (Set[String], Int)) =>
        result.flatMap(rs => {
          LOG.info(
            s"Querying onr for Henkilo batch: ${chunk._1.size} oids, batch ${chunk._2 + 1 + "/" + batches.size}"
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

  override def getHenkiloviitteetForHenkilot(henkiloOids: Set[String]): Future[Set[Henkiloviite]] = {
    val batches: Seq[(Set[String], Int)] = henkiloOids.grouped(onrBatchSize).zipWithIndex.toList

    val allResults: Seq[Future[Set[Henkiloviite]]] = batches.map((batch: (Set[String], Int)) => {
      LOG.info(s"Haetaan tiedot oppijanumerorekisteristä ${batch._1.size} henkilölle, erä ${batch._2 + 1}/${batches.size}")

      val queryObject: Map[String, Set[String]] = Map("henkiloOids" -> batch._1)
      val batchResult: Future[Set[Henkiloviite]] =
        doPost(environmentBaseUrl + "/oppijanumerorekisteri-service/s2s/duplicateHenkilos", queryObject)
          .map(result => {
            val typeRef = new TypeReference[List[Henkiloviite]] {}
            val parsed = mapper.readValue(result, typeRef).toSet
            LOG.info(s"Tiedot oppijanumerorekisteristä haettu, erä ${batch._2 + 1}/${batches.size}, henkiloviitteet: ${parsed.size}")
            parsed
          })
      batchResult
    })
    Future.sequence(allResults).map(_.flatten.toSet)
  }

  private def doPost(url: String, body: Object): Future[String] = {

    LOG.info(s"fetch, $url")
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
