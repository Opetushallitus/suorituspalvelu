package fi.oph.suorituspalvelu.service

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.{FailureHandler, TaskDescriptor}
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.oph.suorituspalvelu.business.Tietolahde.VIRTA
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, Suoritus, Tietolahde, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.virta.VirtaClient
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaSuoritukset, VirtaToSuoritusConverter}
import fi.oph.suorituspalvelu.service.VirtaService.VIRTA_REFRESH_TASK
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component

import java.io.ByteArrayInputStream
import java.time.Duration.ofSeconds
import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import fi.oph.suorituspalvelu.service.VirtaService.LOG
import slick.jdbc.JdbcBackend

object VirtaService {

  val LOG = LoggerFactory.getLogger(classOf[VirtaService])

  val VIRTA_REFRESH_TASK: TaskDescriptor[String] = TaskDescriptor.of("virta-refresh", classOf[String]);
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

  final val TIMEOUT = 30.seconds

  @Bean
  def virtaRefreshTask(virtaClient: VirtaClient) = Tasks.oneTime(VIRTA_REFRESH_TASK)
    .onFailure(new FailureHandler.MaxRetriesFailureHandler(6, new FailureHandler.ExponentialBackoffFailureHandler(ofSeconds(1), 2)))
    .execute((instance, ctx) => {
    val oppijaNumero = instance.getData.split(":").head
    val hetu = instance.getData.split(":").tail.headOption.getOrElse("")
    try {
      val kantaOperaatiot = KantaOperaatiot(database)
      val virtaResults = Await.result(virtaClient.haeKaikkiTiedot(oppijaNumero, {
        if (hetu.isBlank) None else Some(hetu)
      }), TIMEOUT)
      val versiot: Seq[Option[VersioEntiteetti]] = virtaResults.map(virtaResult => {
        val hetulessXml = VirtaUtil.replaceHetusWithPlaceholder(virtaResult.resultXml)

        //Todo, lisätään jossain vaiheessa versiotauluun oma sarake hetulle, tai kehitetään muu ratkaisu tietojen tallennukseen hetun alle.
        // Tiedot pitäisi periaatteessa tallentaa nimenomaan hetun alle, koska hetuun liittyvälle oppijanumerolle saattaisi palautua Virrasta eri tiedot.
        kantaOperaatiot.tallennaJarjestelmaVersio(virtaResult.oppijanumeroTaiHetu, VIRTA, hetulessXml)
      })

      versiot.filter(_.isDefined).flatten.foreach((versio: VersioEntiteetti) => {
        LOG.info(s"Versio tallennettu $versio, tallennetaan VIRTA-suoritukset")
        val versionParseroidut: VirtaSuoritukset = virtaResults.find(_.oppijanumeroTaiHetu == versio.oppijaNumero)
          .map(r => VirtaParser.parseVirtaData(new ByteArrayInputStream(r.resultXml.getBytes))).get
        val konvertoidut: Seq[Opiskeluoikeus] = VirtaToSuoritusConverter.toOpiskeluoikeudet(versionParseroidut)
        val foo = kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, konvertoidut.toSet, Set.empty)
        LOG.info(s"Päivitettiin Virta-tiedot oppijanumerolle ${oppijaNumero}, yhteensä ${konvertoidut.size} suoritusta.")
      })
    } catch {
      case e: Exception => LOG.error(s"Virhe päivettäessä Virta-tietoja oppijanumerolle ${oppijaNumero}", e)
    }
  })
}

@Component
class VirtaService {

  @Autowired val scheduler: Scheduler = null

  def syncVirta(oppijaNumero: String, hetu: Option[String]): UUID =
    val taskId = UUID.randomUUID();
    this.scheduler.schedule(VIRTA_REFRESH_TASK.instance(taskId.toString).data(oppijaNumero + ":" + hetu.getOrElse("")).scheduledTo(Instant.now()))
    taskId

}
