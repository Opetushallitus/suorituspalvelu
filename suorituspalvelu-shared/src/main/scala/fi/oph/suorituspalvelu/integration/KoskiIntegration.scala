package fi.oph.suorituspalvelu.integration

import fi.oph.suorituspalvelu.integration.client.KoskiClient
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, VersioEntiteetti}
import fi.oph.suorituspalvelu.business.Tietolahde.KOSKI
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter}
import slick.jdbc.JdbcBackend

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class KoskiIntegration {

  private val LOG: Logger = LoggerFactory.getLogger(classOf[KoskiIntegration])
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  @Autowired val client: KoskiClient = null

  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def syncKoski(personOids: Set[String]): Seq[Option[VersioEntiteetti]] = {
    LOG.info(s"Synkataan Koski-data $personOids")
    val query = KoskiMassaluovutusQueryParams.forOids(personOids)

    val syncResultF = client.createMassaluovutusQuery(query).flatMap(res => {
      pollUntilReady(res.resultsUrl.get).flatMap(finishedQuery => {
        LOG.info(s"Query is now finished, handling files.")
        handleFiles(finishedQuery.files)
      })
    })
    Await.result(syncResultF, 5.minutes)
  }


  def pollUntilReady(pollUrl: String): Future[KoskiMassaluovutusQueryResponse] = {
    client.pollQuery(pollUrl).flatMap((pollResult: KoskiMassaluovutusQueryResponse) => {
      pollResult match {
        case response if response.isComplete() =>
          LOG.info(s"Valmista! $response")
          Future.successful(response)
        case response if response.isFailed() =>
          LOG.error(s"Koski failure: $response")
          Future.failed(new RuntimeException("Koski failure!"))
        case response =>
          LOG.info(s"Ei vielä valmista, odotellaan hetki ja pollataan uudestaan $pollResult")
          Thread.sleep(1000) //Todo, fiksumpi odottelumekanismi
          pollUntilReady(pollUrl)
      }
    })
  }
  
  def handleFiles(fileUrls: Seq[String]): Future[Seq[Option[VersioEntiteetti]]] = {
    LOG.info(s"Käsitellään ${fileUrls.size} Koski-tiedostoa.")
    val handled = new AtomicInteger(0)

    val kantaOperaatiot = KantaOperaatiot(database)

    val futures = fileUrls.map(fileUrl => {
      LOG.info(s"Käsitellään tiedosto ${handled.incrementAndGet()}/${fileUrls.size}: $fileUrl")
      client.getWithBasicAuth(fileUrl, followRedirects = true).flatMap(fileResult => {
        LOG.info(s"Saatiin haettua tiedosto $fileUrl onnistuneesti")
        val inputStream = new ByteArrayInputStream(fileResult.getBytes("UTF-8"))
        val splitted = KoskiParser.splitKoskiDataByOppija(inputStream).toList
        LOG.info(s"Saatiin tulokset tiedostolle $fileUrl: käsitellään yhteensä ${splitted.size} henkilön Koski-tiedot.")
        val kantaResults = splitted.map(henkilonTiedot => {
          LOG.info(s"Tallennetaan henkilön ${henkilonTiedot._1} Koski-tiedot")
          val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(henkilonTiedot._1, KOSKI, henkilonTiedot._2)
          versio.foreach(v => {
            LOG.info(s"Versio tallennettu henkilölle ${henkilonTiedot._1}")
            val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(henkilonTiedot._2)
            val suoritukset = KoskiToSuoritusConverter.toSuoritus(koskiOpiskeluoikeudet).toSet
            LOG.info(s"Tallennetaan henkilön ${henkilonTiedot._1} suoritukset. Versio $v, suoritukset $suoritukset")
            kantaOperaatiot.tallennaSuoritukset(v, suoritukset)
          })
          versio
        })
        LOG.info(s"Valmista! $kantaResults")
        Future.successful(kantaResults)
      })
    })
    //Todo, miten käsitellään osittaiset onnistumiset? Halutaanko retryjä?
    Future.sequence(futures).map(_.flatten) //Todo, rajoitetaanko rinnakkaisuutta jotenkin?
  }
}
