package fi.oph.suorituspalvelu.integration

import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, AtaruHenkiloSearchParams, HakemuspalveluClientImpl, KoskiClient}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, PerusopetuksenOpiskeluoikeus, Suoritus, VersioEntiteetti}
import fi.oph.suorituspalvelu.business.Tietolahde.KOSKI
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter, Opiskeluoikeus}
import slick.jdbc.JdbcBackend

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class KoskiIntegration {

  private val LOG: Logger = LoggerFactory.getLogger(classOf[KoskiIntegration])
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  @Autowired val koskiClient: KoskiClient = null
  
  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClientImpl = null
  
  
  
  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  
  val KOSKI_BATCH_SIZE = 5000
  
  def syncKoskiForHaku(hakuOid: String): Seq[Option[VersioEntiteetti]] = {
    val personOids =
      Await.result(hakemuspalveluClient.getHaunHakijat(AtaruHenkiloSearchParams(hakukohdeOids = None, hakuOid = Some(hakuOid))), 1.minute)
        .flatMap(_.personOid).toSet

    //Todo, mietitään näille hyvä rinnakkaisuus. Ehkä paria kyselyä kannattaa ajella rinnakkain?
    val grouped = personOids.grouped(KOSKI_BATCH_SIZE).toList
    val started = new AtomicInteger(0)
    grouped.flatMap(group =>
      LOG.info(s"Synkataan ${group.size} henkilön tiedot Koskesta, erä ${started.incrementAndGet()}/${grouped.size}")
      syncKoski(group))

  }
  
  
  def syncKoski(personOids: Set[String]): Seq[Option[VersioEntiteetti]] = {
    LOG.info(s"Synkataan Koski-data ${personOids.size} henkilölle")
    val query = KoskiMassaluovutusQueryParams.forOids(personOids)

    val syncResultF = koskiClient.createMassaluovutusQuery(query).flatMap(res => {
      pollUntilReady(res.resultsUrl.get).flatMap(finishedQuery => {
        LOG.info(s"Query is now finished, handling files.")
        handleFiles(finishedQuery.files)
      })
    })
    Await.result(syncResultF, 5.minutes)
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
  
  def handleFiles(fileUrls: Seq[String]): Future[Seq[Option[VersioEntiteetti]]] = {
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
        val kantaResults = splitted.map(henkilonTiedot => {
          LOG.info(s"Tallennetaan henkilön ${henkilonTiedot._1} Koski-tiedot")
          val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(henkilonTiedot._1, KOSKI, henkilonTiedot._2)
          versio.foreach(v => {
            LOG.info(s"Versio tallennettu henkilölle ${henkilonTiedot._1}")
            val oikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(henkilonTiedot._2))
            LOG.info(s"Henkilölle ${henkilonTiedot._1} yhteensä ${oikeudet.size} opiskeluoikeutta.")
            kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(v, oikeudet.toSet, Set.empty)
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
