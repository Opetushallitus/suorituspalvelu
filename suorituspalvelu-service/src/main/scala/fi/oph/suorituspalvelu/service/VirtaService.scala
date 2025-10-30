package fi.oph.suorituspalvelu.service

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.{FailureHandler, TaskDescriptor}
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, SaferIterator, SyncResultForHenkilo, Util}
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, HakemuspalveluClientImpl}
import fi.oph.suorituspalvelu.integration.virta.VirtaClient
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaSuoritukset, VirtaToSuoritusConverter}
import fi.oph.suorituspalvelu.service.VirtaService.{VIRTA_REFRESH_TASK, VIRTA_REFRESH_TASK_FOR_HAKU}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component

import java.io.ByteArrayInputStream
import java.time.Duration.ofSeconds
import java.time.Instant
import java.util.UUID
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import fi.oph.suorituspalvelu.service.VirtaService.LOG
import slick.jdbc.JdbcBackend

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.immutable

object VirtaService {

  val LOG = LoggerFactory.getLogger(classOf[VirtaService])

  val VIRTA_REFRESH_TASK: TaskDescriptor[String] = TaskDescriptor.of("virta-refresh", classOf[String]);
  val VIRTA_REFRESH_TASK_FOR_HAKU: TaskDescriptor[String] = TaskDescriptor.of("virta-refresh-for-haku", classOf[String]);
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

@Configuration
class VirtaRefresh {


  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  @Autowired var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired val virtaClient: VirtaClient = null

  final val VIRTA_CONCURRENCY = 3

  final val TIMEOUT = 30.seconds

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

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

  def refreshVirtaForPersonOids(personOids: Set[String]): Seq[SyncResultForHenkilo] = {
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
            LOG.error(s"Henkilon $oppijaNumero VIRTA-tietojen päivittäminen epäonnistui", e)
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

  @Bean
  def virtaRefreshTaskForHaku(virtaClient: VirtaClient) = Tasks.oneTime(VIRTA_REFRESH_TASK_FOR_HAKU)
    .onFailure(new FailureHandler.MaxRetriesFailureHandler(1, new FailureHandler.ExponentialBackoffFailureHandler(ofSeconds(30), 2)))
    .execute((instance, ctx) => {
      val hakuOid: String = instance.getData
      val personOids = Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), TIMEOUT).flatMap(_.personOid).toSet
      refreshVirtaForPersonOids(personOids)
    })

  @Bean
  def virtaRefreshTask(virtaClient: VirtaClient) = Tasks.oneTime(VIRTA_REFRESH_TASK)
    .onFailure(new FailureHandler.MaxRetriesFailureHandler(6, new FailureHandler.ExponentialBackoffFailureHandler(ofSeconds(1), 2)))
    .execute((instance, ctx) => {
    val oppijaNumero = instance.getData
    refreshVirtaForPersonOids(Set(oppijaNumero))
  })
}

@Component
class VirtaService {

  @Autowired val scheduler: Scheduler = null

  def syncVirta(oppijaNumero: String): UUID = {
    val taskId = UUID.randomUUID();
    this.scheduler.schedule(VIRTA_REFRESH_TASK.instance(taskId.toString).data(oppijaNumero).scheduledTo(Instant.now()))
    taskId
  }

  def syncVirtaForHaku(hakuOid: String): UUID = {
    val taskId = UUID.randomUUID();
    this.scheduler.schedule(VIRTA_REFRESH_TASK_FOR_HAKU.instance(taskId.toString).data(hakuOid).scheduledTo(Instant.now()))
    taskId
  }
}
