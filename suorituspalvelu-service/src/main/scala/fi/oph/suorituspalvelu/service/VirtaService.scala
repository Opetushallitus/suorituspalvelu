package fi.oph.suorituspalvelu.service

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.{FailureHandler, TaskDescriptor}
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, SyncResultForHenkilo}
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, HakemuspalveluClientImpl}
import fi.oph.suorituspalvelu.integration.virta.{VirtaClient, VirtaResultForHenkilo}
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

  final val TIMEOUT = 30.seconds

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  def safeFetchAndPersistPersonOid(personOid: String): Future[SyncResultForHenkilo] = {
    try {
      virtaClient.haeTiedotOppijanumerolle(personOid).map(persist)
    } catch {
      case exception: Exception =>
        LOG.error(s"Jotain meni pieleen VIRTA-tietojen käsittelyssä henkilölle $personOid", exception)
        Future.successful(SyncResultForHenkilo(personOid, None, Some(exception)))
    }
  }

  def safeFetchAndPersistHetu(hetu: String): Future[SyncResultForHenkilo] = {
    try {
      virtaClient.haeTiedotHetulle(hetu).map(persist)
    } catch {
      case exception: Exception =>
        LOG.error(s"Jotain meni pieleen VIRTA-tietojen käsittelyssä henkilölle (hetu xxxxxx-xxxx)", exception)
        Future.successful(SyncResultForHenkilo(hetu, None, Some(exception)))
    }
  }

  def persist(virtaResult: VirtaResultForHenkilo): SyncResultForHenkilo = {
    val hetulessXml = VirtaUtil.replaceHetusWithPlaceholder(virtaResult.resultXml)
    LOG.info(s"Persistoidaan Virta-data henkilölle ${virtaResult.oppijanumeroTaiHetu}")

    val kantaResult: SyncResultForHenkilo =
      try {
        val kantaOperaatiot = KantaOperaatiot(database)
        val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(virtaResult.oppijanumeroTaiHetu, SuoritusJoukko.VIRTA, hetulessXml)

        versio.foreach(v => {
          LOG.info(s"Versio tallennettu $versio, tallennetaan VIRTA-suoritukset")
          val versionParseroidut: VirtaSuoritukset = VirtaParser.parseVirtaData(new ByteArrayInputStream(hetulessXml.getBytes))
          val konvertoidut: Seq[Opiskeluoikeus] = VirtaToSuoritusConverter.toOpiskeluoikeudet(versionParseroidut)
          kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(v, konvertoidut.toSet, Set.empty)
          LOG.info(s"Päivitettiin Virta-tiedot oppijanumerolle ${virtaResult.oppijanumeroTaiHetu}, yhteensä ${konvertoidut.size} suoritusta.")
        })
        SyncResultForHenkilo(virtaResult.oppijanumeroTaiHetu, versio, None)
      } catch {
        case e: Exception =>
          LOG.error(s"Henkilon ${virtaResult.oppijanumeroTaiHetu} VIRTA-tietojen tallentaminen epäonnistui", e)
          SyncResultForHenkilo(virtaResult.oppijanumeroTaiHetu, None, Some(e))
      }

    kantaResult
  }

  @Bean
  def virtaRefreshTaskForHaku(virtaClient: VirtaClient) = Tasks.oneTime(VIRTA_REFRESH_TASK_FOR_HAKU)
    .onFailure(new FailureHandler.MaxRetriesFailureHandler(1, new FailureHandler.ExponentialBackoffFailureHandler(ofSeconds(30), 2)))
    .execute((instance, ctx) => {
      val hakuOid: String = instance.getData

      //Hetut voisi ottaa joko hakemuksilta tai erikseen haettavilta onr-henkilöiltä. Onr-henkilöt lienevät vähintään yhtä hyvä ja mahdollisesti parempi lähde?
      val hakemustenHenkilot: Future[Seq[AtaruHakemuksenHenkilotiedot]] = hakemuspalveluClient.getHaunHakijat(hakuOid)
      hakemustenHenkilot.flatMap((hakemustenHenkilotResult: Seq[AtaruHakemuksenHenkilotiedot]) => {
        val personOids = hakemustenHenkilotResult.flatMap(_.personOid).toSet
        LOG.info(s"Saatiin ${hakemustenHenkilotResult.size} hakemuksen tiedot haulle $hakuOid")
        val aliakset = onrIntegration.getAliasesForPersonOids(personOids)
        //val masterHenkilot = onrIntegration.getMasterHenkilosForPersonOids(personOids)

        //Haetaan Virrasta yksitellen tiedot kaikkien hakijoiden kaikille aliaksille
        val resultsForOids: Future[Seq[SyncResultForHenkilo]] = aliakset.flatMap(aliasResult => {
          LOG.info(s"Saatiin oppijanumerorekisteristä yhteensä ${aliasResult.allOids.size} oppijanumeroa ja aliasta hakemuksilta poimituille ${personOids.size} henkilöOidille")
          val synced = new AtomicInteger(0)
          aliasResult.allOids.foldLeft(Future.successful(Seq.empty[SyncResultForHenkilo])) {
            case (result: Future[Seq[SyncResultForHenkilo]], personOid: String) =>
              result.flatMap((rs: Seq[SyncResultForHenkilo]) => {
                LOG.info(
                  s"Syncing VIRTA for person: $personOid, progress ${synced.incrementAndGet()}/${aliasResult.allOids.size}"
                )
                safeFetchAndPersistPersonOid(personOid).map(cr => rs :+ cr)
              })
          }
        })

        //Ei persistoida toistaiseksi tietoja hetuille
        /*
        val hetuF: Future[Seq[SyncResultForHenkilo]] = masterHenkilot.flatMap(masterHenkilotResult => {
          val synced = new AtomicInteger(0)
          val kaikkiHetut = masterHenkilotResult.flatMap(_._2.combinedHetut).toSet
          kaikkiHetut.foldLeft(Future.successful(Seq.empty[SyncResultForHenkilo])) {
            case (result: Future[Seq[SyncResultForHenkilo]], hetu: String) =>
              result.flatMap((rs: Seq[SyncResultForHenkilo]) => {
                LOG.info(
                  s"Syncing VIRTA for person with hetu (xxxxxx-xxxx), progress ${synced.incrementAndGet()}/${kaikkiHetut.size}"
                )
                val syncResultF: Future[SyncResultForHenkilo] = virtaClient.haeTiedotHetulle(hetu).map(persist)
                syncResultF.map(cr => rs :+ cr)
              })
        }
        })
        */

        val fullResults = Future.sequence(Seq(resultsForOids))
        val succeeded = fullResults.map(_.flatten.filter(_.exception.isEmpty))
        succeeded.map(results => {
          LOG.info(s"Synkattiin onnistuneesti ${results.size} personOidia (sisältäen aliakset) VIRTA-tietojen synkronoinnissa.")
        })
        val failed = fullResults.map(_.flatten.filter(_.exception.isDefined))
        failed.map(failedResults => {
          LOG.error(s"Failed to sync ${failedResults.size} henkiloita VIRTA-tietojen synkronoinnissa")
          failedResults.foreach(r => LOG.error(s"Failed to sync ${r.henkiloOid} with exception ${r.exception.get.getMessage}"))
        })
      })
    })

  @Bean
  def virtaRefreshTask(virtaClient: VirtaClient) = Tasks.oneTime(VIRTA_REFRESH_TASK)
    .onFailure(new FailureHandler.MaxRetriesFailureHandler(6, new FailureHandler.ExponentialBackoffFailureHandler(ofSeconds(1), 2)))
    .execute((instance, ctx) => {
    val oppijaNumero = instance.getData.split(":").head
    val hetu = instance.getData.split(":").tail.headOption.getOrElse("")
    try {
      val kantaOperaatiot = KantaOperaatiot(database)
      val virtaResults: Seq[VirtaResultForHenkilo] = Await.result(virtaClient.haeKaikkiTiedot(oppijaNumero, {
        if (hetu.isBlank) None else Some(hetu)
      }), TIMEOUT)
      virtaResults.map(persist)
    } catch {
      case e: Exception => LOG.error(s"Virhe päivettäessä Virta-tietoja oppijanumerolle ${oppijaNumero}", e)
    }
  })
}

@Component
class VirtaService {

  @Autowired val scheduler: Scheduler = null

  def syncVirta(oppijaNumero: String, hetu: Option[String]): UUID = {
    val taskId = UUID.randomUUID();
    this.scheduler.schedule(VIRTA_REFRESH_TASK.instance(taskId.toString).data(oppijaNumero + ":" + hetu.getOrElse("")).scheduledTo(Instant.now()))
    taskId
  }

  def syncVirtaForHaku(hakuOid: String): UUID = {
    val taskId = UUID.randomUUID();
    this.scheduler.schedule(VIRTA_REFRESH_TASK_FOR_HAKU.instance(taskId.toString).data(hakuOid).scheduledTo(Instant.now()))
    taskId
  }
}
