package fi.oph.suorituspalvelu.integration.ytr

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.integration.{OnrIntegration, OnrMasterHenkilo, SyncResultForHenkilo, Util}
import fi.oph.suorituspalvelu.integration.client.{YtrClient, YtrHetuPostData, YtrMassOperationQueryResponse}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired
import slick.jdbc.JdbcBackend

import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import fi.oph.suorituspalvelu.parsing.ytr.{YtrParser, YtrToSuoritusConverter}
import fi.oph.suorituspalvelu.util.ZipUtil

import java.io.ByteArrayInputStream
import java.time.Instant
import scala.collection.immutable

case class Section(sectionId: String, sectionPoints: Option[String])

//Henkilölle ei välttämättä löydy mitään
case class YtrDataForHenkilo(personOid: String, resultJson: Option[String])

class YtrIntegration {

  private val LOG: Logger = LoggerFactory.getLogger(classOf[YtrIntegration])
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  @Autowired val ytrClient: YtrClient = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  private val pollWaitMillis = 2000

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def YTR_BATCH_SIZE = 5000

  def massFetchForStudents(hetuPostData: Seq[YtrHetuPostData]): Future[Seq[(String, String)]] = {
    ytrClient.createYtrMassOperation(hetuPostData).flatMap(massOp => {
      LOG.info(s"Luotiin massaoperaatio: $massOp, pollataan")
      pollUntilReady(massOp.uuid).flatMap(finishedQuery => {
        LOG.info(s"Massaoperaatio ${massOp.uuid} valmis, haetaan ja käsitellään tulos-zip.")
        ytrClient.fetchYtlMassResult(massOp.uuid).map((result: Option[Array[Byte]]) => {
          LOG.info(s"Haettiin massa-zip, käsitellään. ${massOp.uuid} - ${result.map(_.length).getOrElse(0L)} bytes")
          result.map(bytes => ZipUtil.unzipStreamByFile(new ByteArrayInputStream(bytes))).getOrElse(Map.empty)
        }).map(dataByFile => {
          dataByFile.flatMap((filename, data) => {
            YtrParser.splitAndSanitize(data).toList
          }).toSeq
        })
      })
    })
  }

  def pollUntilReady(uuid: String): Future[YtrMassOperationQueryResponse] = {
    Thread.sleep(pollWaitMillis) //Todo, tarvitaanko fiksumpi odottelumekanismi
    ytrClient.pollMassOperation(uuid).flatMap((pollResult: YtrMassOperationQueryResponse) => {
      pollResult match {
        case response if response.finished.isDefined =>
          LOG.info(s"Valmista! $response")
          Future.successful(response)
        case response if response.failure.isDefined =>
          LOG.error(s"Ytr failure: $response")
          Future.failed(new RuntimeException("Ytr failure!"))
        case response =>
          LOG.info(s"Ei vielä valmista, odotellaan hetki ja pollataan uudestaan $pollResult")
          pollUntilReady(uuid)
      }
    })
  }

  def fetchAndProcessYtrWithSingleApi[R](
                                          personOids: Set[String],
                                          timeout: Duration
                                        ): Iterator[YtrDataForHenkilo] = {
    val resultF = onrIntegration.getMasterHenkilosForPersonOids(personOids)
      .flatMap { henkiloMap =>
        val withHetu = henkiloMap.values.filter(_.hetu.isDefined)

        if (withHetu.isEmpty) {
          LOG.warn(s"None of persons $personOids have hetu, skipping")
          Future.successful(Seq.empty)
        } else {
          val ytrParams = withHetu.map { h => (h.oidHenkilo, YtrHetuPostData(h.hetu.get, Some(h.kaikkiHetut.getOrElse(Seq.empty)))) }.toSeq
          val ytrDataF = ytrParams.map(postData => {
            val personOid = postData._1
            ytrClient.fetchOne(postData._2).map(ytrData => {
              ytrData.map(data => YtrDataForHenkilo(personOid, Some(YtrParser.sanitize(data)))).getOrElse(YtrDataForHenkilo(postData._1, None))
            })
          })
          Future.sequence(ytrDataF)
        }
      }
    Await.result(resultF, timeout).iterator
  }

  def fetchAndProcessStudents(personOids: Set[String]): Iterator[YtrDataForHenkilo] = {
    personOids match {
      case oids if oids.isEmpty => Iterator.empty
      case oids if oids.size < 5 => fetchAndProcessYtrWithSingleApi(oids, 1.minute)
      case oids => processHenkilosInBatches(oids, 4.hours)
    }
  }

  private def processHenkilosInBatches[R](
                                           personOids: Set[String],
                                           timeout: FiniteDuration
                                         ): Iterator[YtrDataForHenkilo] = {

    val henkiloMap = Await.result(onrIntegration.getMasterHenkilosForPersonOids(personOids), timeout)

    val personsWithHetu = henkiloMap.values.filter(_.hetu.isDefined)
    val personOidByHetu = personsWithHetu.map(h => (h.hetu.get, h.oidHenkilo)).toMap
    val ytrParams = personsWithHetu.map { h => (h.oidHenkilo, YtrHetuPostData(h.hetu.get, Some(h.kaikkiHetut.getOrElse(Seq.empty)))) }.toSeq
    val batches = ytrParams.grouped(YTR_BATCH_SIZE).map(_.toSet).toSeq

    Util.toIterator(batches.zipWithIndex.iterator.map((batch, index) => {
      LOG.info(s"Synkataan ${batch.size} henkilön tiedot Ylioppilastutkintorekisteristä, erä ${index + 1}/${batches.size}")
      massFetchForStudents(batch.map(_._2).toSeq).map(fetchResult => fetchResult.map(r => YtrDataForHenkilo(personOidByHetu.getOrElse(r._1, throw new RuntimeException()), Some(r._2))))
    }), 2, timeout).flatten
  }
}