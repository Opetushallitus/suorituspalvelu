package fi.oph.suorituspalvelu.integration.ytr

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.integration.client.{YtrClient, YtrHetuPostData, YtrMassOperationQueryResponse}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, Util}
import fi.oph.suorituspalvelu.parsing.ytr.YtrParser
import fi.oph.suorituspalvelu.util.ZipUtil
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import slick.jdbc.JdbcBackend

import java.io.ByteArrayInputStream
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}
import fi.oph.suorituspalvelu.VirtualThreadExecutionContext.executor

case class Section(sectionId: String, sectionPoints: Option[String])

//Henkilölle ei välttämättä löydy mitään
case class YtrDataForHenkilo(personOid: String, resultJson: Option[String])

class YtrPollFailed(message: String) extends RuntimeException(message)

enum YtrFetchMode:
  case SingleApi, BatchApi

class YtrIntegration {

  private val LOG: Logger = LoggerFactory.getLogger(classOf[YtrIntegration])

  @Autowired val ytrClient: YtrClient = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  // Sallitaan viiveiden muuttaminen konfiguraatiolla, jotta integraatiotesteissä ei tarvitse odotella

  // Kuinka kauan odotellaan ennen ensimmäistä pollausta?
  @Value("${ytr.poll.startDelayMillis:2000}")
  private val pollStartDelayMillis: Long = 2000

  // Ensimmäisen uudelleenyrityksen (eksponentiaalinen) viive tai pollausten välinen viive
  @Value("${ytr.retry.delayMillis:5000}")
  private val defaultRetryDelayMillis: Long = 5000

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def YTR_BATCH_SIZE = 5000

  def massFetchForStudents(hetuPostData: Seq[YtrHetuPostData]): Future[Seq[(String, String)]] = {
    Util.retryWithBackoff(
      operation = ytrClient.createYtrMassOperation(hetuPostData),
      retries = 1,
      retryDelayMillis = defaultRetryDelayMillis,
      failMessage = s"YTR-massaoperaation käynnistäminen epäonnistui",
    ).flatMap(massOp => {
      LOG.info(s"Luotiin YTR-massaoperaatio: ${massOp.uuid}, pollataan")
      pollUntilReady(massOp.uuid).flatMap(finishedQuery => {
        LOG.info(s"Massaoperaatio ${massOp.uuid} valmis, haetaan ja käsitellään tulos-zip.")
        Util.retryWithBackoff(
          operation = ytrClient.fetchYtlMassResult(massOp.uuid),
          retries = 1,
          retryDelayMillis = defaultRetryDelayMillis,
          failMessage = s"YTR-massaoperaation ${massOp.uuid} tulosten hakeminen epäonnistui"
        ).map((result: Option[Array[Byte]]) => {
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

  def pollYtrMassOperation(uuid: String, pollWaitMillis: Long): Future[YtrMassOperationQueryResponse] = {
    ytrClient.pollMassOperation(uuid).flatMap {
      response =>
        if (response.finished.isDefined) {
          Future.successful(response)
        } else if (response.failure.isDefined) {
          Future.failed(new RuntimeException(response.toString))
        } else {
          LOG.info(s"YTR-Massaoperaatio $uuid ei vielä valmis, odotellaan ja pollataan uudestaan")
          Util.sleepAsync(pollWaitMillis).flatMap(_ => pollYtrMassOperation(uuid, pollWaitMillis))
        }
    }
  }

  def pollUntilReady(
    uuid: String,
    retries: Int = 5,
    retryDelayMillis: Long = defaultRetryDelayMillis
  ): Future[YtrMassOperationQueryResponse] = {
    Util.retryWithBackoff(
      operation = pollYtrMassOperation(uuid, retryDelayMillis),
      retries = retries,
      retryDelayMillis = retryDelayMillis,
      startDelayMillis = pollStartDelayMillis,
      failMessage = s"YTR-massaoperaatio $uuid epäonnistui"
    ).recoverWith {
      case e: RuntimeException =>
        Future.failed(new YtrPollFailed(e.getMessage))
    }
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
          val ytrParams = withHetu.map { h =>
            (h.oidHenkilo, YtrHetuPostData(h.hetu.get, Some(h.kaikkiHetut.getOrElse(Seq.empty))))
          }.toSeq
          val ytrDataF = ytrParams.map(postData => {
            val personOid = postData._1
            Util.retryWithBackoff(
              operation = ytrClient.fetchOne(postData._2),
              retries = 1,
              retryDelayMillis = defaultRetryDelayMillis
            ).map(ytrData => {
              ytrData.map(data => YtrDataForHenkilo(personOid, Some(YtrParser.sanitize(data)))).getOrElse(
                YtrDataForHenkilo(postData._1, None)
              )
            })
          })
          Future.sequence(ytrDataF)
        }
      }
    Await.result(resultF, timeout).iterator
  }

  def fetchAndProcessStudents(personOids: Set[String], mode: YtrFetchMode): Iterator[YtrDataForHenkilo] = {
    personOids match {
      case oids if oids.isEmpty => Iterator.empty
      case oids => mode match {
          case YtrFetchMode.SingleApi => fetchAndProcessYtrWithSingleApi(oids, 30.minutes)
          case YtrFetchMode.BatchApi => processHenkilosInBatches(oids, 4.hours)
        }
    }
  }

  private def processHenkilosInBatches[R](
    personOids: Set[String],
    timeout: FiniteDuration
  ): Iterator[YtrDataForHenkilo] = {

    val henkiloMap = Await.result(onrIntegration.getMasterHenkilosForPersonOids(personOids), timeout)

    val personsWithHetu = henkiloMap.values.filter(_.hetu.isDefined)
    val personOidByHetu = personsWithHetu.flatMap { h =>
      (h.kaikkiHetut.getOrElse(Seq.empty) :+ h.hetu.get).map(hetu => (hetu, h.oidHenkilo))
    }.toMap
    val ytrParams = personsWithHetu.map { h =>
      (h.oidHenkilo, YtrHetuPostData(h.hetu.get, Some(h.kaikkiHetut.getOrElse(Seq.empty))))
    }.toSeq
    val batches = ytrParams.grouped(YTR_BATCH_SIZE).map(_.toSet).toSeq

    Util.toIterator(
      batches.zipWithIndex.iterator.map((batch, index) => {
        LOG.info(
          s"Synkataan ${batch.size} henkilön tiedot YTR:stä, erä ${index + 1}/${batches.size}"
        )
        massFetchForStudents(batch.map(_._2).toSeq).map(fetchResult =>
          fetchResult.map(r =>
            YtrDataForHenkilo(personOidByHetu.getOrElse(r._1, throw new RuntimeException()), Some(r._2))
          )
        )
      }),
      2,
      timeout
    ).flatten
  }
}
