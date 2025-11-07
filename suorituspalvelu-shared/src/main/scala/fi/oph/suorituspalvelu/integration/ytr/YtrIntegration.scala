package fi.oph.suorituspalvelu.integration.ytr

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, OnrMasterHenkilo, SyncResultForHenkilo}
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, YtrClient, YtrHetuPostData, YtrMassOperationQueryResponse}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired
import slick.jdbc.JdbcBackend

import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}
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

  private val HENKILO_TIMEOUT = 5.minutes

  @Autowired val ytrClient: YtrClient = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClientImpl = null


  private val pollWaitMillis = 2000

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def YTR_BATCH_SIZE = 5000

  def syncYtrForHaku(hakuOid: String): Seq[SyncResultForHenkilo] = {
    val personOids =
      Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), HENKILO_TIMEOUT)
        .flatMap(_.personOid).toSet
    val fetchedAt = Instant.now()
    val syncResult = fetchAndProcessStudents(personOids, ytrDataForBatch => safePersistBatch(ytrDataForBatch, fetchedAt))
    LOG.info(s"Ytr-sync haulle $hakuOid valmis. Tallennettiin yhteensä ${syncResult.size} henkilön tiedot.")
    syncResult
  }

  def safePersistBatch(ytrResult: Seq[YtrDataForHenkilo], fetchedAt: Instant): Seq[SyncResultForHenkilo] = {
    ytrResult.map(r => safePersistSingle(r, fetchedAt))
  }

  def safePersistSingle(ytrResult: YtrDataForHenkilo, fetchedAt: Instant): SyncResultForHenkilo = {
    LOG.info(s"Persistoidaan Ytr-data henkilölle ${ytrResult.personOid}: ${ytrResult.resultJson.getOrElse("no data")}")
    try {
      val kantaOperaatiot = KantaOperaatiot(database)
      val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(ytrResult.personOid, SuoritusJoukko.YTR, Seq(ytrResult.resultJson.getOrElse("{}")), fetchedAt)
      versio.foreach(v => {
        LOG.info(s"Versio $versio tallennettu, todo: tallennetaan parsitut YTR-suoritukset")
        val oikeus = YtrToSuoritusConverter.toSuoritus(YtrParser.parseYtrData(ytrResult.resultJson.get))
        kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(v, Set(oikeus))
      })
      SyncResultForHenkilo(ytrResult.personOid, versio, None)
    } catch {
      case e: Exception =>
        LOG.error(s"Henkilon ${ytrResult.personOid} YTR-tietojen tallentaminen epäonnistui", e)
        SyncResultForHenkilo(ytrResult.personOid, None, Some(e))
    }
  }

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
                                          processFunction: Seq[YtrDataForHenkilo] => Seq[R],
                                          timeout: Duration
                                        ): Seq[R] = {
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
          Future.sequence(ytrDataF).map(processFunction)
        }
      }
    Await.result(resultF, timeout)
  }

  def fetchAndPersistStudents(personOids: Set[String]): Seq[SyncResultForHenkilo] = {
    val fetchedAt = Instant.now()
    fetchAndProcessStudents(personOids, ytrDataForBatch => safePersistBatch(ytrDataForBatch, fetchedAt))
  }

  def fetchAndProcessStudents[R](personOids: Set[String], processFunction: Seq[YtrDataForHenkilo] => Seq[R]): Seq[R] = {
    personOids match {
      case oids if oids.isEmpty => Seq.empty
      case oids if oids.size < 5 => fetchAndProcessYtrWithSingleApi(oids, processFunction, 1.minute)
      case oids => fetchAndProcessYtrInBatchesWithMassaApi(oids, processFunction, 4.hours)
    }
  }

  def fetchAndProcessYtrInBatchesWithMassaApi[R](
                                      personOids: Set[String],
                                      processFunction: Seq[YtrDataForHenkilo] => Seq[R],
                                      timeout: Duration
                                    ): Seq[R] = {
    val syncResultsF: Future[Seq[R]] =
      onrIntegration.getMasterHenkilosForPersonOids(personOids)
        .flatMap { henkiloMap => processHenkilosInBatches(henkiloMap, processFunction)
        }

    Await.result(syncResultsF, timeout)
  }

  private def processHenkilosInBatches[R](
                                           henkiloMap: Map[String, OnrMasterHenkilo],
                                           processFunction: Seq[YtrDataForHenkilo] => Seq[R]
                                         ): Future[Seq[R]] = {
    val personsWithHetu = henkiloMap.values.filter(_.hetu.isDefined)
    val personOidByHetu = personsWithHetu.map(h => (h.hetu.get, h.oidHenkilo)).toMap
    val ytrParams = personsWithHetu.map { h => (h.oidHenkilo, YtrHetuPostData(h.hetu.get, Some(h.kaikkiHetut.getOrElse(Seq.empty)))) }.toSeq
    val batches = ytrParams.grouped(YTR_BATCH_SIZE).map(_.toSet).toSeq.zipWithIndex

    processBatchesSequentially(batches, personOidByHetu, processFunction)
  }

  private def processBatchesSequentially[R](
                                             batches: Seq[(Set[(String, YtrHetuPostData)], Int)],
                                             personOidByHetu: Map[String, String],
                                             processFunction: Seq[YtrDataForHenkilo] => Seq[R]
                                           ): Future[Seq[R]] = {
    batches.foldLeft(Future(Seq.empty[R])) {
      case (accResultF, (batchData, batchIndex)) =>
        accResultF.flatMap { accResults =>
          LOG.info(s"Synkataan ${batchData.size} henkilön tiedot Ylioppilastutkintorekisteristä, erä ${batchIndex+1}/${batches.size}")
          val batchResultF = massFetchForStudents(batchData.map(_._2).toSeq)
            .map(fetchResult => processFunction(fetchResult.map(r => YtrDataForHenkilo(personOidByHetu.getOrElse(r._1, throw new RuntimeException()), Some(r._2)))))
          batchResultF.map(batchResults => accResults ++ batchResults)
        }
    }
  }
}
