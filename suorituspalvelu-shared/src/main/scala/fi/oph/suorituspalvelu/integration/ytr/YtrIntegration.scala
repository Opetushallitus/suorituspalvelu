package fi.oph.suorituspalvelu.integration.ytr

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, OnrMasterHenkilo, SyncResultForHenkilo}
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, YtrHetuPostData, YtrClient, YtrMassOperationQueryResponse}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired
import slick.jdbc.JdbcBackend

import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import fi.oph.suorituspalvelu.business.Tietolahde.YTR
import fi.oph.suorituspalvelu.parsing.ytr.YtrParser
import fi.oph.suorituspalvelu.util.ZipUtil

import java.io.ByteArrayInputStream
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
    val syncResult = syncYtrInBatches(personOids)
    LOG.info(s"Ytr-sync haulle $hakuOid valmis. Tallennettiin yhteensä ${syncResult.size} henkilön tiedot.")
    syncResult
  }

  def syncYtrInBatches(personOids: Set[String]): Seq[SyncResultForHenkilo] = {
    val henkilot: Future[Map[String, OnrMasterHenkilo]] = onrIntegration.getMasterHenkilosForPersonOids(personOids)

    val allResultsF: Future[Seq[SyncResultForHenkilo]] = henkilot.flatMap(henkiloResult => {
      val personsWithHetu = henkiloResult.values.filter(_.hetu.isDefined)
      val personOidByHetu = personsWithHetu.map(h => (h.hetu.get, h.oidHenkilo)).toMap
      val ytrParams: Iterable[(String, YtrHetuPostData)] = personsWithHetu.map(h => (h.oidHenkilo, YtrHetuPostData(h.hetu.get, Some(h.kaikkiHetut.getOrElse(Seq.empty))))).toSeq

      val grouped: Seq[(Set[(String, YtrHetuPostData)], Int)] = ytrParams.grouped(YTR_BATCH_SIZE).map(_.toSet).toSeq.zipWithIndex

      grouped.foldLeft(Future(Seq[SyncResultForHenkilo]())) {
        case (result: Future[Seq[SyncResultForHenkilo]], group: (Set[(String, YtrHetuPostData)], Int)) =>
          result.flatMap(rs => {
            LOG.info(s"Synkataan ${group._1.size} henkilön tiedot Ylioppilastutkintorekisteristä, erä ${group._2 + 1 + "/" + grouped.size}")
            val chunkResult: Future[Seq[SyncResultForHenkilo]] = {
              massFetchForStudents(group._1.map(_._2).toSeq, personOidByHetu).map(fetchResult => {
                fetchResult.map(r => persistSingle(r))
              })
            }
            chunkResult.map(cr => rs ++ cr)
          })
      }
    })

    Await.result(allResultsF, 4.hours)
  }

  def persistSingle(ytrResult: YtrDataForHenkilo): SyncResultForHenkilo = {
    LOG.info(s"Persistoidaan Ytr-data henkilölle ${ytrResult.personOid}: ${ytrResult.resultJson.getOrElse("no data")}")
    try {
      val kantaOperaatiot = KantaOperaatiot(database)
      val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(ytrResult.personOid, YTR, ytrResult.resultJson.getOrElse("{}"))
      versio.foreach(v => {
        //Todo, parsitaan ytr-data ja tallennetaan parsitut suoritukset
        LOG.info(s"Versio $versio tallennettu, todo: tallennetaan parsitut YTR-suoritukset")
      })
      SyncResultForHenkilo(ytrResult.personOid, versio, None)
    } catch {
      case e: Exception =>
        LOG.error(s"Henkilon ${ytrResult.personOid} VIRTA-tietojen tallentaminen epäonnistui", e)
        SyncResultForHenkilo(ytrResult.personOid, None, Some(e))
    }
  }

  def massFetchForStudents(hetuPostData: Seq[YtrHetuPostData], personOidByHetu: Map[String, String]): Future[Seq[YtrDataForHenkilo]] = {
    ytrClient.createYtrMassOperation(hetuPostData).flatMap(massOp => {
      LOG.info(s"Luotiin massaoperaatio: $massOp, pollataan")
      pollUntilReady(massOp.uuid).flatMap(finishedQuery => {
        LOG.info(s"Massaoperaatio ${massOp.uuid} valmis, haetaan ja käsitellään tulos-zip.")
        ytrClient.getWithBasicAuthAsByteArray(massOp.uuid).map((result: Option[Array[Byte]]) => {
          LOG.info(s"Haettiin massa-zip, käsitellään. ${massOp.uuid} - ${result.map(_.length).getOrElse(0L)} bytes")
          result.map(bytes => ZipUtil.unzipStreamByFile(new ByteArrayInputStream(bytes))).getOrElse(Map.empty)
        }).map(dataByFile => {
          dataByFile.flatMap((filename, data) => {
            //LOG.info(s"Handling file: $filename, data $data")
            YtrParser.parseYtrMassData(data, personOidByHetu).toList
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

  def fetchRawForStudents(personOids: Set[String]): Seq[SyncResultForHenkilo] = {
    if (personOids.size > 5) {
      syncYtrInBatches(personOids)
    } else {
      val useHenkilot = personOids.take(10)
      val henkilot = onrIntegration.getMasterHenkilosForPersonOids(useHenkilot)

      val resultF: Future[Seq[SyncResultForHenkilo]] = henkilot.map((henkiloResult: Map[String, OnrMasterHenkilo]) => {
        LOG.info(s"Saatiin oppijanumerorekisteristä ${henkiloResult.size} henkilön tiedot ${useHenkilot.size} haetulle henkilölle")
        val ytrParams = henkiloResult.values.filter(_.hetu.isDefined).map(h => (h.oidHenkilo, YtrHetuPostData(h.hetu.get, Some(h.kaikkiHetut.getOrElse(Seq.empty)))))
        LOG.info(s"Haetuista henkilöistä ${ytrParams.size} henkilölle löytyi hetu. Haetaan näille ytr-tiedot.")
        ytrParams.map(ytrParam => {
          LOG.info(s"Haetaan $ytrParam")
          val resultF = ytrClient.fetchOne(ytrParam._2)
          val resultForHenkilo = Await.result(resultF, 1.minute)
          val parsed = resultForHenkilo.map(r => YtrParser.parseSingleAndRemoveHetu(r, ytrParam._1)).getOrElse(YtrDataForHenkilo(ytrParam._1, None))
          persistSingle(parsed)
        }).toList
      })
      Await.result(resultF, 15.minutes)
    }
  }
}
