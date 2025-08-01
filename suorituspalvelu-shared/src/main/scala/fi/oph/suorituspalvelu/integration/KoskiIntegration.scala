package fi.oph.suorituspalvelu.integration

import fi.oph.suorituspalvelu.integration.client.{AtaruHenkiloSearchParams, HakemuspalveluClientImpl, KoskiClient}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, VersioEntiteetti}
import fi.oph.suorituspalvelu.business.Tietolahde.KOSKI
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter}
import slick.jdbc.JdbcBackend

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

case class SyncResultForHenkilo(henkiloOid: String, versio: Option[VersioEntiteetti], exception: Option[Exception])

class KoskiIntegration {

  private val LOG: Logger = LoggerFactory.getLogger(classOf[KoskiIntegration])
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  @Autowired val koskiClient: KoskiClient = null

  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClientImpl = null

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  private val KOSKI_BATCH_SIZE = 5000
  private val HENKILO_TIMEOUT = 5.minutes


  def syncKoskiInBatches(personOids: Set[String]): Seq[SyncResultForHenkilo] = {
    val grouped = personOids.grouped(KOSKI_BATCH_SIZE).toList
    val started = new AtomicInteger(0)

    grouped.flatMap(group => {
      LOG.info(s"Synkataan ${group.size} henkilön tiedot Koskesta, erä ${started.incrementAndGet()}/${grouped.size}")
      syncKoski(group)
    })
  }

  def syncKoskiForHaku(hakuOid: String): Seq[SyncResultForHenkilo] = {
    val personOids =
      Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), HENKILO_TIMEOUT)
        .flatMap(_.personOid).toSet
    syncKoskiInBatches(personOids)
  }

  def syncKoski(personOids: Set[String]): Seq[SyncResultForHenkilo] = {
    LOG.info(s"Synkataan Koski-data ${personOids.size} henkilölle")
    val query = KoskiMassaluovutusQueryParams.forOids(personOids)

    val syncResultF = koskiClient.createMassaluovutusQuery(query).flatMap(res => {
      pollUntilReady(res.resultsUrl.get).flatMap(finishedQuery => {
        LOG.info(s"Query is now finished, handling files.")
        handleFiles(finishedQuery.files)
      })
    })
    Await.result(syncResultF, 2.hours)
  }


  def pollUntilReady(pollUrl: String): Future[KoskiMassaluovutusQueryResponse] = {
    koskiClient.pollQuery(pollUrl).flatMap((pollResult: KoskiMassaluovutusQueryResponse) => {
      pollResult match {
        case response if response.isComplete() =>
          LOG.info(s"Valmista! ${response.getTruncatedLoggable()}")
          Future.successful(response)
        case response if response.isFailed() =>
          LOG.error(s"Koski failure: ${response.getTruncatedLoggable()}")
          Future.failed(new RuntimeException("Koski failure!"))
        case response =>
          LOG.info(s"Ei vielä valmista, odotellaan hetki ja pollataan uudestaan ${pollResult.getTruncatedLoggable()}")
          Thread.sleep(2500) //Todo, fiksumpi odottelumekanismi
          pollUntilReady(pollUrl)
      }
    })
  }

  def handleFiles(fileUrls: Seq[String]): Future[Seq[SyncResultForHenkilo]] = {
    LOG.info(s"Käsitellään ${fileUrls.size} Koski-tiedostoa.")
    val handled = new AtomicInteger(0)

    val kantaOperaatiot = KantaOperaatiot(database)

    val futures = fileUrls.map(fileUrl => {
      LOG.info(s"Käsitellään tiedosto ${handled.incrementAndGet()}/${fileUrls.size}: $fileUrl")
      koskiClient.getWithBasicAuth(fileUrl, followRedirects = true).flatMap(fileResult => {
        LOG.info(s"Saatiin haettua tiedosto $fileUrl onnistuneesti")
        val inputStream = new ByteArrayInputStream(fileResult.getBytes("UTF-8"))
        val splitted = KoskiParser.splitKoskiDataByOppija(inputStream).toList
        LOG.info(s"Saatiin tulokset tiedostolle $fileUrl: käsitellään yhteensä ${splitted.size} henkilön Koski-tiedot.")
        val kantaResults: Seq[SyncResultForHenkilo] = splitted.map(henkilonTiedot => {
          try {
            val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(henkilonTiedot._1, KOSKI, henkilonTiedot._2)
            versio.foreach(v => {
              LOG.info(s"Versio tallennettu henkilölle ${henkilonTiedot._1}")
              val oikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(henkilonTiedot._2))
              kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(v, oikeudet.toSet, Set.empty)
            })
            SyncResultForHenkilo(henkilonTiedot._1, versio, None)
          } catch {
            case e: Exception =>
              LOG.error(s"Henkilon ${henkilonTiedot._1} Koski-tietojen tallentaminen epäonnistui", e)
              SyncResultForHenkilo(henkilonTiedot._1, None, Some(e))
          }
        })
        LOG.info(s"Valmista! $kantaResults")
        Future.successful(kantaResults)
      })
    })
    Future.sequence(futures).map(_.flatten) //Todo, rajoitetaanko rinnakkaisuutta jotenkin?
  }
}
