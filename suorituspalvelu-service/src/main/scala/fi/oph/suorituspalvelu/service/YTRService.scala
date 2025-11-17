package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.integration.TarjontaIntegration
import fi.oph.suorituspalvelu.integration.client.HakemuspalveluClientImpl
import fi.oph.suorituspalvelu.integration.ytr.YtrIntegration
import fi.oph.suorituspalvelu.jobs.SupaScheduler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import scala.concurrent.duration.DurationInt
import java.util.UUID
import scala.concurrent.Await
import scala.jdk.CollectionConverters.*

@Service
class YTRService(scheduler: SupaScheduler, hakemuspalveluClient: HakemuspalveluClientImpl, ytrIntegration: YtrIntegration, tarjontaIntegration: TarjontaIntegration) {

  val LOG = LoggerFactory.getLogger(classOf[YTRService])

  final val TIMEOUT = 30.seconds

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def refreshYTRForHaut(hakuOids: Seq[String]): Unit =
    hakuOids.foreach(hakuOid => {
      try
        val personOids = Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), TIMEOUT).flatMap(_.personOid).toSet
        ytrIntegration.fetchAndPersistStudents(personOids)
      catch
        case e: Exception => LOG.error(s"YTR-tietojen päivitys haulle $hakuOid epäonnistui",  e)
    })

  def refreshYTRForAktiivisetHaut(): Unit =
    val paivitettavatHaut = tarjontaIntegration.aktiivisetHaut()
      .filter(haku => !haku.kohdejoukkoKoodiUri.contains("12"))
    refreshYTRForHaut(paivitettavatHaut.map(_.oid))

  private val refreshHautJob = scheduler.registerJob("refresh-ytr-for-haut", (ctx, data) => {
    val hakuOids: Seq[String] = mapper.readValue(data, classOf[Seq[String]])
    refreshYTRForHaut(hakuOids)
  }, Seq.empty)

  def startRefreshYTRForHautJob(hakuOids: Seq[String]): UUID = refreshHautJob.run(mapper.writeValueAsString(hakuOids))  

  private val refreshAktiivisetHautJob = scheduler.registerJob("refresh-ytr-for-aktiiviset-haut", (ctx, data) => {
    refreshYTRForAktiivisetHaut()
  }, Seq.empty)

  def startRefreshYTRForAktiivisetHautJob(): UUID = refreshAktiivisetHautJob.run(null)


}