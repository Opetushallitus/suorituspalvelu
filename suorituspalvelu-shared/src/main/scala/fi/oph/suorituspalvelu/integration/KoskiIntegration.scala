package fi.oph.suorituspalvelu.integration

import com.fasterxml.jackson.core.JsonToken
import fi.oph.suorituspalvelu.integration.client.{AtaruHenkiloSearchParams, HakemuspalveluClientImpl, KoskiClient, KoskiMassaluovutusQueryParams, KoskiMassaluovutusQueryResponse}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.KoskiIntegration.splitKoskiDataByOppija
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter, Opiskeluoikeus}
import slick.jdbc.JdbcBackend

import java.io.{ByteArrayInputStream, InputStream}
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

case class SplitattavaKoskiData(oppijaOid: String, opiskeluoikeudet: Seq[Map[String, Any]])

case class KoskiDataForOppija(oppijaOid: String, data: String)

object KoskiIntegration {

  val MAPPER: ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    mapper
  }

  def splitKoskiDataByOppija(input: InputStream): Iterator[(String, String)] =
    val jsonParser = MAPPER.getFactory().createParser(input)
    jsonParser.nextToken()

    Iterator.continually({
        val token = jsonParser.nextToken()
        if(token != JsonToken.END_ARRAY)
          Some(jsonParser.readValueAs(classOf[SplitattavaKoskiData]))
        else
          None})
      .takeWhile(data => data.isDefined)
      .map(data => {
        (data.get.oppijaOid, MAPPER.writeValueAsString(data.get.opiskeluoikeudet))
      })
}

class KoskiIntegration {

  private val LOG: Logger = LoggerFactory.getLogger(classOf[KoskiIntegration])
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  @Autowired val koskiClient: KoskiClient = null

  private val KOSKI_BATCH_SIZE = 1000

  def fetchMuuttuneetKoskiTiedotSince(timestamp: Instant): SaferIterator[KoskiDataForOppija] = {
    new SaferIterator(fetchKoskiBatch(KoskiMassaluovutusQueryParams.forTimestamp(timestamp)))
  }

  def fetchKoskiTiedotForOppijat(personOids: Set[String]): SaferIterator[KoskiDataForOppija] = {
    val size = if(personOids.isEmpty) 0 else Math.ceil(personOids.size.asInstanceOf[Double]/KOSKI_BATCH_SIZE.asInstanceOf[Double]).asInstanceOf[Int]
    val grouped = personOids.grouped(KOSKI_BATCH_SIZE)
    val started = new AtomicInteger(0)

    new SaferIterator(grouped.flatMap(group => {
      LOG.info(s"Synkataan ${group.size} henkilön tiedot Koskesta, erä ${started.incrementAndGet()}/$size")
      fetchKoskiBatch(KoskiMassaluovutusQueryParams.forOids(group))
    }))
  }

  def retryKoskiResultFile(fileUrl: String): SaferIterator[KoskiDataForOppija] = {
    new SaferIterator(Await.result(handleFile(fileUrl, 3), 2.hours))
  }

  private def fetchKoskiBatch(query: KoskiMassaluovutusQueryParams): Iterator[KoskiDataForOppija] = {
    val syncResultF = koskiClient.createMassaluovutusQuery(query).flatMap(res => {
      LOG.info(s"Käsitellään KOSKI-massaluovutushaun $query tulokset osoitteesta ${res.resultsUrl.get}")
      pollUntilReadyWithRetries(res.resultsUrl.get, 3).map(finishedQuery => {
        LOG.info(s"Haku valmis, käsitellään ${finishedQuery.files.size} KOSKI-tulostiedostoa.")
        Util.toIterator(finishedQuery.files.iterator.map(f => handleFile(f, 3)), 3, 1.minute).flatten
      })
    })
    Await.result(syncResultF, 2.hours)
  }

  def pollUntilReadyWithRetries(pollUrl: String, retries: Int): Future[KoskiMassaluovutusQueryResponse] = {
    pollUntilReady(pollUrl).recoverWith({
      case e: Exception =>
        if(retries > 0)
          pollUntilReadyWithRetries(pollUrl, retries - 1)
        else
          LOG.error(s"Virhe KOSKI-pollauksessa: $pollUrl", e)
          Future.failed(e)
    })
  }

  private def pollUntilReady(pollUrl: String): Future[KoskiMassaluovutusQueryResponse] = {
    koskiClient.pollQuery(pollUrl).flatMap((pollResult: KoskiMassaluovutusQueryResponse) => {
      pollResult match {
        case response if response.isComplete() =>
          LOG.info(s"KOSKI-massaluovutushaku valmistui: ${response.getTruncatedLoggable()}")
          Future.successful(response)
        case response if response.isFailed() =>
          LOG.error(s"KOSKI-massaluovutushaussa virhe Kosken päässä: ${response.error.getOrElse("")}, koko vastaus ${response.getTruncatedLoggable()}")
          Future.failed(new RuntimeException("Koski failure!"))
        case response =>
          LOG.info(s"KOSKI-massaluovutushaun tulokset eivät vielä valmiit, odotellaan hetki ja pollataan uudestaan ${pollResult.getTruncatedLoggable()}")
          Thread.sleep(2500) //Todo, fiksumpi odottelumekanismi
          pollUntilReady(pollUrl)
      }
    })
  }

  private def handleFile(fileUrl: String, retries: Int): Future[Iterator[KoskiDataForOppija]] =
    LOG.info(s"Käsitellään KOSKI-massaluovutushaun tulostiedosto: $fileUrl")
    koskiClient.getWithBasicAuth(fileUrl, followRedirects = true).map(fileResult => {
      LOG.info(s"Saatiin haettua KOSKI-massaluovutushaun tiedosto $fileUrl onnistuneesti")
      val inputStream = new ByteArrayInputStream(fileResult.getBytes("UTF-8"))
      val splitted = splitKoskiDataByOppija(inputStream)
      splitted.map(henkilonTiedot => {
        KoskiDataForOppija(henkilonTiedot._1, henkilonTiedot._2)
      })
    }).recoverWith({
      case e: Exception =>
        if(retries > 0)
          handleFile(fileUrl, retries - 1)
        else
          LOG.error(s"Virhe KOSKI-tulostiedoston $fileUrl käsittelyssä", e)
          Future.failed(e)
    })
}
