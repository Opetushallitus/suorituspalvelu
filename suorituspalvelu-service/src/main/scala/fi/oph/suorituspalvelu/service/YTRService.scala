package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{SyncResultForHenkilo, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.HakemuspalveluClientImpl
import fi.oph.suorituspalvelu.integration.ytr.{YtrDataForHenkilo, YtrIntegration}
import fi.oph.suorituspalvelu.jobs.{SupaJobContext, SupaScheduler}
import fi.oph.suorituspalvelu.parsing.ytr.{YtrParser, YtrToSuoritusConverter}
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import java.time.Instant
import scala.concurrent.duration.DurationInt
import java.util.UUID
import scala.concurrent.Await
import scala.jdk.CollectionConverters.*

@Service
class YTRService(scheduler: SupaScheduler, hakemuspalveluClient: HakemuspalveluClientImpl, ytrIntegration: YtrIntegration, tarjontaIntegration: TarjontaIntegration, kantaOperaatiot: KantaOperaatiot) {

  val LOG = LoggerFactory.getLogger(classOf[YTRService])

  final val TIMEOUT = 30.seconds

  private val HENKILO_TIMEOUT = 5.minutes

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def syncYtrForHaku(hakuOid: String): Seq[SyncResultForHenkilo] = {
    val personOids =
      Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), HENKILO_TIMEOUT)
        .flatMap(_.personOid).toSet
    val fetchedAt = Instant.now()
    val syncResult = ytrIntegration.fetchAndProcessStudents(personOids, ytrDataForBatch => safePersistBatch(ytrDataForBatch, fetchedAt))
    LOG.info(s"Ytr-sync haulle $hakuOid valmis. Tallennettiin yhteensä ${syncResult.size} henkilön tiedot.")
    syncResult
  }

  def safePersistBatch(ytrResult: Seq[YtrDataForHenkilo], fetchedAt: Instant): Seq[SyncResultForHenkilo] = {
    ytrResult.map(r => safePersistSingle(r, fetchedAt))
  }

  def safePersistSingle(ytrResult: YtrDataForHenkilo, fetchedAt: Instant): SyncResultForHenkilo = {
    if(ytrResult.resultJson.isEmpty)
      LOG.info(s"Ytr-dataa ei löytynyt henkilölle ${ytrResult.personOid}")
      SyncResultForHenkilo(ytrResult.personOid, None, None)
    else
      LOG.info(s"Persistoidaan Ytr-data henkilölle ${ytrResult.personOid}: ${ytrResult.resultJson.getOrElse("no data")}")
      try {
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

  def fetchAndPersistStudents(personOids: Set[String]): Seq[SyncResultForHenkilo] = {
    val fetchedAt = Instant.now()
    ytrIntegration.fetchAndProcessStudents(personOids, ytrDataForBatch => safePersistBatch(ytrDataForBatch, fetchedAt))
  }

  def refreshYTRForHaut(ctx: SupaJobContext, hakuOids: Seq[String]): Unit = {
    hakuOids.zipWithIndex.foreach((hakuOid, index) => {
      try
        val personOids = Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), TIMEOUT).flatMap(_.personOid).toSet
        fetchAndPersistStudents(personOids)
      catch
        case e: Exception => LOG.error(s"YTR-tietojen päivitys haulle $hakuOid epäonnistui",  e)
      ctx.updateProgress((index+1).toDouble/hakuOids.size.toDouble)
    })
  }

  private val refreshHautJob = scheduler.registerJob("refresh-ytr-for-haut", (ctx, data) => {
    val hakuOids: Seq[String] = mapper.readValue(data, classOf[Seq[String]])
    refreshYTRForHaut(ctx, hakuOids)
  }, Seq.empty)

  def startRefreshYTRForHautJob(hakuOids: Seq[String]): UUID = refreshHautJob.run(mapper.writeValueAsString(hakuOids))  

  private val refreshAktiivisetHautJob = scheduler.registerJob("refresh-ytr-for-aktiiviset-haut", (ctx, data) => {
    val paivitettavatHaut = tarjontaIntegration.aktiivisetHaut()
      .filter(haku => !haku.kohdejoukkoKoodiUri.contains("12"))
    refreshYTRForHaut(ctx, paivitettavatHaut.map(_.oid))
  }, Seq.empty)

  def startRefreshYTRForAktiivisetHautJob(): UUID = refreshAktiivisetHautJob.run(null)


}