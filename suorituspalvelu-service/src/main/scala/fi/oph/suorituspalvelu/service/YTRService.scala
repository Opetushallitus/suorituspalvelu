package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, ParserVersions, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{SyncResultForHenkilo, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.HakemuspalveluClientImpl
import fi.oph.suorituspalvelu.integration.ytr.{YtrDataForHenkilo, YtrIntegration}
import fi.oph.suorituspalvelu.jobs.{DUMMY_JOB_CTX, SupaJobContext, SupaScheduler}
import fi.oph.suorituspalvelu.parsing.ytr.{YtrParser, YtrToSuoritusConverter}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt
import java.util.UUID
import scala.concurrent.Await
import scala.jdk.CollectionConverters.*

@Service
class YTRService(scheduler: SupaScheduler, hakemuspalveluClient: HakemuspalveluClientImpl, ytrIntegration: YtrIntegration,
                 tarjontaIntegration: TarjontaIntegration, kantaOperaatiot: KantaOperaatiot, @Value("${integrations.ytr.cron}") cron: String) {

  val LOG = LoggerFactory.getLogger(classOf[YTRService])

  final val TIMEOUT = 30.seconds

  private val HENKILO_TIMEOUT = 5.minutes

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def safePersistSingle(ytrResult: YtrDataForHenkilo, fetchedAt: Instant, ctx: SupaJobContext): SyncResultForHenkilo = {
    if(ytrResult.resultJson.isEmpty)
      LOG.info(s"Ytr-dataa ei löytynyt henkilölle ${ytrResult.personOid}")
      SyncResultForHenkilo(ytrResult.personOid, None, None)
    else
      LOG.info(s"Persistoidaan Ytr-data henkilölle ${ytrResult.personOid}")
      try {
        val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(ytrResult.personOid, SuoritusJoukko.YTR, Seq(ytrResult.resultJson.getOrElse("{}")), Seq.empty, fetchedAt)
        versio.foreach(v => {
          LOG.info(s"Versio $versio tallennettu, todo: tallennetaan parsitut YTR-suoritukset")
          val oikeus = YtrToSuoritusConverter.toSuoritus(YtrParser.parseYtrData(ytrResult.resultJson.get))
          kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(v, Set(oikeus), Seq.empty, ParserVersions.YTR)
        })
        SyncResultForHenkilo(ytrResult.personOid, versio, None)
      } catch {
        case e: Exception =>
          LOG.error(s"Henkilon ${ytrResult.personOid} YTR-tietojen tallentaminen epäonnistui", e)
          SyncResultForHenkilo(ytrResult.personOid, None, Some(e))
      }
  }

  def fetchAndPersistStudents(personOids: Set[String], ctx: SupaJobContext = DUMMY_JOB_CTX): Seq[SyncResultForHenkilo] = {
    val fetchedAt = Instant.now()
    ytrIntegration.fetchAndProcessStudents(personOids).map(r => safePersistSingle(r, fetchedAt, ctx)).toSeq
  }

  private val refreshHenkilotJob = scheduler.registerJob("refresh-ytr-for-henkilot", (ctx, oppijaNumerot) => fetchAndPersistStudents(mapper.readValue(oppijaNumerot, classOf[Set[String]]), ctx), Seq(Duration.ofSeconds(30), Duration.ofSeconds(60)))

  def startRefreshForHenkilot(personOids: Set[String]): UUID =
    refreshHenkilotJob.run(mapper.writeValueAsString(personOids))

  def refreshYTRForHaut(ctx: SupaJobContext, hakuOids: Seq[String]): Unit = {
    hakuOids.zipWithIndex.foreach((hakuOid, index) => {
      try
        val personOids = Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), TIMEOUT).flatMap(_.personOid).toSet
        fetchAndPersistStudents(personOids, ctx)
        ctx.updateProgress((index+1).toDouble/hakuOids.size.toDouble)
      catch
        case e: Exception =>
          val message = s"YTR-tietojen päivitys haulle $hakuOid epäonnistui"
          LOG.error(message,  e)
          ctx.reportError(message, Some(e))
    })
  }

  def refreshYTRForAktiivisetHaut(ctx: SupaJobContext): Unit = {
    val paivitettavatHaut = tarjontaIntegration.aktiivisetHaut()
      .filter(haku => !haku.kohdejoukkoKoodiUri.contains("haunkohdejoukko_12"))
    refreshYTRForHaut(ctx, paivitettavatHaut.map(_.oid))
  }

  private val refreshHautJob = scheduler.registerJob("refresh-ytr-for-haut", (ctx, data) => {
    val hakuOids: Seq[String] = mapper.readValue(data, classOf[Seq[String]])
    refreshYTRForHaut(ctx, hakuOids)
  }, Seq.empty)

  def startRefreshYTRForHautJob(hakuOids: Seq[String]): UUID = refreshHautJob.run(mapper.writeValueAsString(hakuOids))  

  private val refreshAktiivisetHautJob = scheduler.registerJob("refresh-ytr-for-aktiiviset-haut", (ctx, data) => refreshYTRForAktiivisetHaut(ctx), Seq.empty)

  def startRefreshYTRForAktiivisetHautJob(): UUID = refreshAktiivisetHautJob.run(null)

  scheduler.scheduleJob("ytr-refresh-aktiiviset", (ctx, data) => {
    refreshYTRForAktiivisetHaut(ctx)
    null
  }, cron)
}