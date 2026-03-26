package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Lahdejarjestelma, ParserVersions, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{SyncResultForHenkilo, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.HakemuspalveluClientImpl
import fi.oph.suorituspalvelu.integration.ytr.{YtrDataForHenkilo, YtrFetchMode, YtrIntegration, YtrPollFailed}
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
      LOG.info(s"(job id: ${ctx.getJobId}) YTR-tietoja ei löytynyt henkilölle ${ytrResult.personOid}")
      SyncResultForHenkilo(ytrResult.personOid, None, None)
    else
      LOG.info(s"(job id: ${ctx.getJobId}) Tallennetaan YTR-tiedot henkilölle ${ytrResult.personOid}")
      try {
        val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(ytrResult.personOid, Lahdejarjestelma.YTR, Seq(ytrResult.resultJson.getOrElse("{}")), Seq.empty, fetchedAt, Lahdejarjestelma.defaultLahdeTunniste(Lahdejarjestelma.YTR), None)
        versio.foreach(v => {
          LOG.info(s"(job id: ${ctx.getJobId}) Tallennettiin versio: $v")
          val oikeus = YtrToSuoritusConverter.toSuoritus(YtrParser.parseYtrData(ytrResult.resultJson.get))
          kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(v, Set(oikeus), Seq.empty, ParserVersions.YTR)
          LOG.info(s"(job id: ${ctx.getJobId}) Tallennettiin YTR-tiedot henkilölle ${ytrResult.personOid}")
        })
        SyncResultForHenkilo(ytrResult.personOid, versio, None)
      } catch {
        case e: Exception =>
          LOG.error(s"(job id: ${ctx.getJobId}) Henkilon ${ytrResult.personOid} YTR-tietojen tallentaminen epäonnistui", e)
          SyncResultForHenkilo(ytrResult.personOid, None, Some(e))
      }
  }

  def fetchAndPersistStudents(personOids: Set[String], mode: YtrFetchMode, ctx: SupaJobContext = DUMMY_JOB_CTX): Seq[SyncResultForHenkilo] = {
    val fetchedAt = Instant.now()
    ytrIntegration.fetchAndProcessStudents(personOids, mode).map(r => safePersistSingle(r, fetchedAt, ctx)).toSeq
  }

  private val refreshHenkilotJob = scheduler.registerJob("refresh-ytr-for-henkilot", (ctx, oppijaNumerot) => fetchAndPersistStudents(mapper.readValue(oppijaNumerot, classOf[Set[String]]), YtrFetchMode.SingleApi, ctx), Seq(Duration.ofSeconds(30), Duration.ofSeconds(60)))

  def startRefreshForHenkilot(personOids: Set[String]): UUID =
    refreshHenkilotJob.run(mapper.writeValueAsString(personOids))

  private def refreshYTRForHaut(ctx: SupaJobContext, hakuOids: Seq[String]): Unit = {
    hakuOids.zipWithIndex.foreach((hakuOid, index) => {
      try
        val personOids = Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), TIMEOUT).flatMap(_.personOid).toSet
        LOG.info(s"(job id ${ctx.getJobId}) Haetaan YTR-tiedot haun $hakuOid hakijoille (${personOids.size} henkilöä)")
        fetchAndPersistStudents(personOids, YtrFetchMode.BatchApi, ctx)
        LOG.info(s"(job id ${ctx.getJobId}) YTR-tietojen hakeminen haun $hakuOid hakijoille onnistui.")
        ctx.updateProgress((index+1).toDouble/hakuOids.size.toDouble)
      catch
        case e: YtrPollFailed =>
          val message = s"(job id ${ctx.getJobId}) YTR-tietojen päivitys haulle $hakuOid epäonnistui, massaoperaation tilaa ei saatu selville. Lopetetaan koko YTR-ajo koska seuraavan haun muodostuksen aloittaminen ei toimisi koska edellisen muodostus on yhä kesken."
          LOG.error(message, e)
          throw e
        case e: Exception =>
          val message = s"YTR-tietojen päivitys haulle $hakuOid epäonnistui"
          LOG.error(s"(job id ${ctx.getJobId}) $message", e)
          ctx.reportError(message, Some(e))
    })
  }

  private def refreshYTRForHautJob(ctx: SupaJobContext, data: String): Unit = {
    val hakuOids: Seq[String] = mapper.readValue(data, classOf[Seq[String]])
    LOG.info(s"(job id ${ctx.getJobId}) Aloitetaan YTR-tietojen päivitys valituille hauille (${hakuOids.size} kpl)")
    refreshYTRForHaut(ctx, hakuOids)
    LOG.info(s"(job id ${ctx.getJobId}) YTR-tietojen päivitys valituille hauille (${hakuOids.size} kpl) on valmis")
  }

  private val refreshHautJobHandle = scheduler.registerJob("refresh-ytr-for-haut", refreshYTRForHautJob, Seq.empty)

  def startRefreshYTRForHautJob(hakuOids: Seq[String]): UUID = refreshHautJobHandle.run(mapper.writeValueAsString(hakuOids))

  private def refreshYTRForAktiivisetHautJob(ctx: SupaJobContext, data: String): Unit = {
    val paivitettavatHaut = tarjontaIntegration.aktiivisetHaut()
    LOG.info(s"(job id ${ctx.getJobId}) Aloitetaan YTR-tietojen päivitys aktiivisille hauille (${paivitettavatHaut.size} kpl)")
    refreshYTRForHaut(ctx, paivitettavatHaut.map(_.oid))
    LOG.info(s"(job id ${ctx.getJobId}) YTR-tietojen päivitys aktiivisille hauille (${paivitettavatHaut.size} kpl) on valmis")
  }

  private val refreshAktiivisetHautJobHandle = scheduler.registerJob("refresh-ytr-for-aktiiviset-haut", refreshYTRForAktiivisetHautJob, Seq.empty)

  def startRefreshYTRForAktiivisetHautJob(): UUID = refreshAktiivisetHautJobHandle.run(null)

  scheduler.scheduleJob("ytr-refresh-aktiiviset", (ctx, data) => {
    refreshYTRForAktiivisetHautJob(ctx, data)
    null
  }, cron)
}