package fi.oph.suorituspalvelu.integration.ytr

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{KoskiIntegration, OnrIntegration, OnrMasterHenkilo, SyncResultForHenkilo}
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, YtlHetuPostData, YtrClient, YtrMassOperationQueryResponse}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired
import slick.jdbc.JdbcBackend

import java.time.LocalDate
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import fi.oph.suorituspalvelu.business.Tietolahde.YTR
import fi.oph.suorituspalvelu.parsing.ytr.YtrParser

import java.util.concurrent.atomic.AtomicInteger

case class Section(sectionId: String, sectionPoints: Option[String])

//Henkilölle ei välttämättä löydy mitään
case class YtrDataForHenkilo(personOid: String, resultJson: Option[String])

class YtrIntegration {

  private val LOG: Logger = LoggerFactory.getLogger(classOf[KoskiIntegration])
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  private val HENKILO_TIMEOUT = 5.minutes

  @Autowired val ytrClient: YtrClient = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClientImpl = null


  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def YTR_BATCH_SIZE = 5000

  def syncYtrForHaku(hakuOid: String): Seq[SyncResultForHenkilo] = {
    val personOids =
      Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), HENKILO_TIMEOUT)
        .flatMap(_.personOid).toSet
    syncYtrInBatches(personOids)
  }

  def syncYtrInBatches(personOids: Set[String]): Seq[SyncResultForHenkilo] = {
    val henkilot: Future[Map[String, OnrMasterHenkilo]] = onrIntegration.getMasterHenkilosForPersonOids(personOids)

    val allResultsF: Future[Seq[SyncResultForHenkilo]] = henkilot.flatMap((henkiloResult: Map[String, OnrMasterHenkilo]) => {
      val personsWithHetu = henkiloResult.values.filter(_.hetu.isDefined)
      val personOidByHetu = personsWithHetu.map(h => (h.hetu.get, h.oidHenkilo)).toMap
      val ytrParams: Iterable[(String, YtlHetuPostData)] = personsWithHetu.map(h => (h.oidHenkilo, YtlHetuPostData(h.hetu.get, Some(h.kaikkiHetut.getOrElse(Seq.empty)))))

      val grouped: Seq[Iterable[(String, YtlHetuPostData)]] = ytrParams.grouped(YTR_BATCH_SIZE).toList
      val started = new AtomicInteger(0)

        val f: Future[Seq[SyncResultForHenkilo]] = Future.sequence(grouped.map((group: Iterable[(String, YtlHetuPostData)]) => {
          LOG.info(s"Synkataan ${group.size} henkilön tiedot Ylioppilastutkintorekisteristä, erä ${started.incrementAndGet()}/${grouped.size}")
          //val personsForBatch = onrIntegration.getMasterHenkilosForPersonOids(group)

          fetchZipForStudents(group.map(_._2).toSeq, personOidByHetu)


        })).map(_.flatten)
      f
    })

    Await.result(allResultsF, 2.hours)
  }

  def persistSingle(ytrResult: YtrDataForHenkilo): SyncResultForHenkilo = {
    LOG.info(s"Persistoidaan Ytr-data henkilölle ${ytrResult.personOid}")

    try {
      val kantaOperaatiot = KantaOperaatiot(database)
      val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(ytrResult.personOid, YTR, ytrResult.resultJson.getOrElse("{}"))
      versio.foreach(v => {
        //Todo, parsitaan ytr-data ja tallennetaan parsitut suoritukset
        LOG.info(s"Versio tallennettu $versio, tallennetaan (leikisti) YTR-suoritukset")
      })
      SyncResultForHenkilo(ytrResult.personOid, versio, None)
    } catch {
      case e: Exception =>
        LOG.error(s"Henkilon ${ytrResult.personOid} VIRTA-tietojen tallentaminen epäonnistui", e)
        SyncResultForHenkilo(ytrResult.personOid, None, Some(e))
    }
  }

  def fetchZipForStudents(hetuPostData: Seq[YtlHetuPostData], personOidByHetu: Map[String, String]): Future[Seq[SyncResultForHenkilo]] = {
    val usePostData: Seq[YtlHetuPostData] = hetuPostData ++ Seq(YtlHetuPostData("170372-9000", None))
    val syncResultF = ytrClient.createYtrMassOperation(usePostData).flatMap(massOp => {
      LOG.info(s"Luotiin massaoperaatio: $massOp, pollataan")
      Thread.sleep(2000)
      pollUntilReady(massOp.uuid).flatMap(finishedQuery => {
        LOG.info(s"Query is now finished, handling result zip.")
        val dataByFileF: Future[Map[String, String]] = ytrClient.fetchAndDecompressZip(massOp.uuid)
        val handled = dataByFileF.map(dataByFile => {
          dataByFile.map((d, f) => {
            val parsed = YtrParser.parseYtrData(f, personOidByHetu).toList
            LOG.info(s"parsed: $parsed")
          })
        })

        val awaited = Await.result(handled, 1.minute)
        LOG.info(s"awaited: $awaited")
        //handleFiles(finishedQuery.files)
        Future.successful(Seq.empty)
      })
    })
    syncResultF
  }


  def pollUntilReady(uuid: String): Future[YtrMassOperationQueryResponse] = {
    ytrClient.pollMassOperation(uuid).flatMap((pollResult: YtrMassOperationQueryResponse) => {
      pollResult match {
        case response if response.finished.isDefined =>
          LOG.info(s"Valmista! $response")
          Future.successful(response)
        case response if response.failure.isDefined =>
          LOG.error(s"Koski failure: $response")
          Future.failed(new RuntimeException("Koski failure!"))
        case response =>
          LOG.info(s"Ei vielä valmista, odotellaan hetki ja pollataan uudestaan $pollResult")
          Thread.sleep(2500) //Todo, fiksumpi odottelumekanismi
          pollUntilReady(uuid)
      }
    })
  }

  def fetchRawForStudents(personOids: Set[String]): Seq[SyncResultForHenkilo] = {
    val useHenkilot = personOids.take(10)
    val henkilot = onrIntegration.getMasterHenkilosForPersonOids(useHenkilot)

    val kantaOperaatiot = KantaOperaatiot(database)

    //Todo, käytetään massahakutoiminnallisuutta vähänkin suuremmille erille (10+? 100+? 500+?)
    val resultF: Future[Seq[SyncResultForHenkilo]] = henkilot.map((henkiloResult: Map[String, OnrMasterHenkilo]) => {
      LOG.info(s"Saatiin oppijanumerorekisteristä ${henkiloResult.size} henkilön tiedot ${useHenkilot.size} haetulle henkilölle")
      val ytrParams = henkiloResult.values.filter(_.hetu.isDefined).map(h => (h.oidHenkilo, YtlHetuPostData(h.hetu.get, Some(h.kaikkiHetut.getOrElse(Seq.empty)))))
      LOG.info(s"Haetuista henkilöistä ${ytrParams.size} henkilölle löytyi hetu, eli haetaan ytr-tiedot")
      val k: Seq[SyncResultForHenkilo] = ytrParams.map(ytrParam => {
        val resultF = ytrClient.fetchOne(ytrParam._2)
        val resultForHenkilo = Await.result(resultF, 1.minute)
        persistSingle(YtrDataForHenkilo(ytrParam._1, resultForHenkilo))
      }).toList
      //Todo, persist jossain välissä
      k
    })
    Await.result(resultF, 15.minutes)
  }

  //Dataa ei välttämättä löydy ytr:stä
  def fetchRawForStudent(ssn: String): Option[String] = {
    val resultF = ytrClient.fetchOne(YtlHetuPostData(ssn, None))
    Await.result(resultF, 1.minute)
  }

}
