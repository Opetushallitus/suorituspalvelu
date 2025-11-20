package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.{FailureHandler, TaskDescriptor}
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, SyncResultForHenkilo, TarjontaIntegration, Util}
import fi.oph.suorituspalvelu.integration.client.HakemuspalveluClientImpl
import fi.oph.suorituspalvelu.integration.virta.VirtaClient
import fi.oph.suorituspalvelu.jobs.{SupaJobContext, SupaScheduler}
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaSuoritukset, VirtaToSuoritusConverter}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component

import java.io.ByteArrayInputStream
import java.time.Duration.ofSeconds
import java.time.Instant
import java.util.UUID
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import fi.oph.suorituspalvelu.service.VirtaService.LOG
import org.springframework.beans.factory.InitializingBean
import slick.jdbc.JdbcBackend

import java.util.concurrent.Executors
import scala.collection.immutable
import scala.jdk.CollectionConverters.*

object VirtaService {

  val LOG = LoggerFactory.getLogger(classOf[VirtaService])
}

object VirtaUtil {
  val replacementHetu = "010190-937W"

  //For now, we don't really need/want to store such information.
  def replaceHetusWithPlaceholder(xml: String): String = {
    val start = "<virta:Henkilotunnus>"
    val end = "</virta:Henkilotunnus>"
    val replacement = start + replacementHetu + end
    val pattern = s"(?<=$start).*?(?=$end)".r
    pattern.replaceAllIn(xml, replacement)
  }
}

@Component
class VirtaService(scheduler: SupaScheduler, database: JdbcBackend.JdbcDatabaseDef, tarjontaIntegration: TarjontaIntegration,
                   onrIntegration: OnrIntegration, virtaClient: VirtaClient, hakemuspalveluClient: HakemuspalveluClientImpl,
                   @Value("${integrations.virta.cron}") cron: String) {

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  final val TIMEOUT = 30.seconds

  final val VIRTA_CONCURRENCY = 3

  def persist(oppijaNumero: String, virtaXmls: Seq[String], fetchedAt: Instant): Option[VersioEntiteetti] = {
    val hetulessXmls = virtaXmls.map(xml => VirtaUtil.replaceHetusWithPlaceholder(xml))
    LOG.info(s"Persistoidaan Virta-data henkilölle $oppijaNumero")

    val kantaOperaatiot = KantaOperaatiot(database)
    val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.VIRTA, hetulessXmls, fetchedAt)

    versio.foreach(v => {
      LOG.info(s"Versio tallennettu $versio, tallennetaan VIRTA-suoritukset")
      val parseroidut = hetulessXmls.map(xml => VirtaParser.parseVirtaData(new ByteArrayInputStream(xml.getBytes)))
      val konvertoidut = parseroidut.flatMap(p => VirtaToSuoritusConverter.toOpiskeluoikeudet(p))
      kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(v, konvertoidut.toSet)
      LOG.info(s"Päivitettiin Virta-tiedot oppijanumerolle $oppijaNumero, yhteensä ${konvertoidut.size} suoritusta.")
    })

    versio
  }

  def refreshVirtaForPersonOids(ctx: SupaJobContext, personOids: Set[String]): Seq[SyncResultForHenkilo] = {
    val masterHenkilot = Await.result(onrIntegration.getMasterHenkilosForPersonOids(personOids), TIMEOUT).values.toSet
    val masterOids = masterHenkilot.map(_.oidHenkilo)
    val duplikaatit = Await.result(onrIntegration.getAliasesForPersonOids(masterHenkilot.map(_.oidHenkilo)), TIMEOUT).allOids
      .filter(oid => !masterOids.contains(oid))

    // konstruoidaan lista (henkiloOid, Set[hetu]) tupleja joille sitten suoritetaan Virtahaku, muiden kuin masterhenkilöiden
    // hetut ovat tyhjä joukko
    val virtaHaut = masterHenkilot.map(h => (h.oidHenkilo, h.combinedHetut)) ++ duplikaatit.map(a => (a, Set.empty.asInstanceOf[Set[String]]))
    val tulokset = Util.toIterator(virtaHaut.iterator.map((oppijaNumero, hetut) => {
      Future.sequence(Seq(
          Seq(virtaClient.haeTiedotOppijanumerolle(oppijaNumero)),
          hetut.map(hetu => virtaClient.haeTiedotHetulle(hetu))
        ).flatten)
        .map(xmls => SyncResultForHenkilo(oppijaNumero, persist(oppijaNumero, xmls, Instant.now()), None))
        .recover {
          case e: Exception =>
            val message = s"Henkilon $oppijaNumero VIRTA-tietojen päivittäminen epäonnistui"
            LOG.error(message, e)
            ctx.reportError(message, Some(e))
            SyncResultForHenkilo(oppijaNumero, None, Some(e))
          case t: Throwable => throw t
        }
    }), VIRTA_CONCURRENCY, TIMEOUT).toList

    val succeeded = tulokset.filter(_.exception.isEmpty)
    LOG.info(s"Synkattiin onnistuneesti ${succeeded.size} personOidia (sisältäen aliakset) VIRTA-tietojen synkronoinnissa.")
    val failed = tulokset.filter(_.exception.isDefined)
    LOG.error(s"Failed to sync ${failed.size} henkiloita VIRTA-tietojen synkronoinnissa")
    failed.foreach(r => LOG.error(s"Failed to sync ${r.henkiloOid} with exception ${r.exception.get.getMessage}"))

    tulokset
  }

  private val refreshOppijaJob = scheduler.registerJob("refresh-virta-for-oppija", (ctx, oppijaNumero) => refreshVirtaForPersonOids(ctx, Set(oppijaNumero)), Seq.empty)

  def syncVirtaForHenkilo(henkiloNumero: String): UUID = refreshOppijaJob.run(henkiloNumero)

  private val refreshHautJob = scheduler.registerJob("refresh-virta-for-haut", (ctx, data) => {
    val hakuOids: Seq[String] = mapper.readValue(data, classOf[Seq[String]])
    hakuOids.zipWithIndex.foreach((hakuOid, index) => {
      try
        val henkiloNumerot = Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), TIMEOUT).flatMap(_.personOid).toSet
        refreshVirtaForPersonOids(ctx, henkiloNumerot)
      catch
        case e: Exception => LOG.error(s"Haun $hakuOid tietojen päivittäminen VIRTA-järjestelmästä epäonnistui", e)
      ctx.updateProgress((index+1)/hakuOids.size)
    })
  }, Seq.empty)

  def syncVirtaForHaut(hakuOids: Seq[String]): UUID = refreshHautJob.run(mapper.writeValueAsString(hakuOids))

  private def refreshVirtaForAktiivisetHaut(ctx: SupaJobContext): Unit =
    val paivitettavatHaut = tarjontaIntegration.aktiivisetHaut()
      .filter(haku => !haku.kohdejoukkoKoodiUri.contains("haunkohdejoukko_12"))
      .map(_.oid)

    paivitettavatHaut.zipWithIndex.foreach((hakuOid, index) => {
      try
        val henkiloNumerot = Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), TIMEOUT).flatMap(_.personOid).toSet
        refreshVirtaForPersonOids(ctx, henkiloNumerot)
      catch
        case e: Exception => LOG.error(s"Haun $hakuOid tietojen päivittäminen VIRTA-järjestelmästä epäonnistui", e)
      ctx.updateProgress((index+1)/paivitettavatHaut.size)
    })

  private val refreshAktiivisetHautJob = scheduler.registerJob("refresh-virta-for-aktiiviset-haut", (ctx, data) => refreshVirtaForAktiivisetHaut(ctx), Seq.empty)

  def syncVirtaForAktiivisetHaut(): UUID = refreshAktiivisetHautJob.run("")

  scheduler.scheduleJob("virta-refresh-aktiiviset", (ctx, data) => {
    refreshVirtaForAktiivisetHaut(ctx)
    null
  }, cron)

}
