package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{KoskiDataForOppija, KoskiIntegration, SaferIterator, SyncResultForHenkilo, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, KoskiClient}
import fi.oph.suorituspalvelu.jobs.{DUMMY_JOB_CTX, SupaJobContext, SupaScheduler}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter, KoskiUtil}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component
import slick.jdbc.JdbcBackend

import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

@Component
class KoskiService(scheduler: SupaScheduler, database: JdbcBackend.JdbcDatabaseDef, hakemuspalveluClient: HakemuspalveluClientImpl,
                   tarjontaIntegration: TarjontaIntegration, koskiIntegration: KoskiIntegration, koodistoProvider: KoodistoProvider) {

  val LOG = LoggerFactory.getLogger(classOf[KoskiService])

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  private val HENKILO_TIMEOUT = 5.minutes
  private val HAKEMUKSET_TIMEOUT = 1.minutes

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  scheduler.scheduleJob("koski-poll-muuttuneet", (ctx, data) => {
    val start = Instant.now()
    val prevStart = Option.apply(data).map(Instant.parse(_))
    if (prevStart.isDefined) { // tyhjä tarkoittaa ettei taskia ajettu koskaan tässä ympäristössä
      try
        refreshKoskiChangesSince(ctx, prevStart.get.minusSeconds(60))
      catch
        case e: Exception => LOG.error("Muuttuneiden KOSKI-tietojen pollaus epäonnistui", e)
    }
    start.toString
  }, "0 */2 * * * *")

  def refreshKoskiChangesSince(ctx: SupaJobContext, since: Instant): SaferIterator[SyncResultForHenkilo] =
    val fetchedAt = Instant.now()
    val tiedot = koskiIntegration.fetchMuuttuneetKoskiTiedotSince(since)

    tiedot.grouped(100).flatMap(chunk => {
      // haetaan relevantit aktiiviset haut
      val oppijaOids = chunk.map(_.oppijaOid)
      val oppijanHaut = Await.result(hakemuspalveluClient.getHenkilonHaut(oppijaOids), HAKEMUKSET_TIMEOUT)
      val aktiivisetHaut = oppijanHaut.values.flatten.toSet
        .filter(h => tarjontaIntegration.tarkistaHaunAktiivisuus(h))

      def hasAktiivinenHaku(oppijaOid: String): Boolean =
        oppijanHaut.get(oppijaOid)
          .exists(haut => haut.exists(haku => aktiivisetHaut.contains(haku)))

      def isOhjattava(koskiData: String): Boolean =
        val opiskeluoikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(koskiData), koodistoProvider)
        KoskiUtil.isOhjattava(opiskeluoikeudet)

      val filtteroity = chunk.filter(r => hasAktiivinenHaku(r.oppijaOid) || isOhjattava(r.data))
      processKoskiDataForOppijat(ctx, new SaferIterator(filtteroity.iterator), fetchedAt)
    })

  private val refreshKoskiChangesSinceJob = scheduler.registerJob("refresh-koski-changes-since", (ctx, alkaen) => {
    val (changed, exceptions) = refreshKoskiChangesSince(ctx, Instant.parse(alkaen))
      .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0)}))
  }, Seq.empty)

  def startRefreshForKoskiChangesSince(alkaen: Instant): UUID = refreshKoskiChangesSinceJob.run(alkaen.toString)

  def syncKoskiForHenkilot(personOids: Set[String], ctx: SupaJobContext = DUMMY_JOB_CTX): SaferIterator[SyncResultForHenkilo] = {
    val fetchedAt = Instant.now()
    processKoskiDataForOppijat(ctx, koskiIntegration.fetchKoskiTiedotForOppijat(personOids), fetchedAt)
  }

  private val refreshHenkilotJob = scheduler.registerJob("refresh-koski-for-henkilot", (ctx, oppijaNumerot) => syncKoskiForHenkilot(mapper.readValue(oppijaNumerot, classOf[Set[String]]), ctx), Seq(Duration.ofSeconds(30), Duration.ofSeconds(60)))

  def startRefreshForHenkilot(personOids: Set[String]): UUID = refreshHenkilotJob.run(mapper.writeValueAsString(personOids))

  def refreshKoskiForHaku(hakuOid: String, ctx: SupaJobContext): SaferIterator[SyncResultForHenkilo] =
    val personOids =
      Await.result(hakemuspalveluClient.getHaunHakijat(hakuOid), HENKILO_TIMEOUT)
        .flatMap(_.personOid).toSet
    syncKoskiForHenkilot(personOids, ctx)

  private val refreshHakuJob = scheduler.registerJob("refresh-koski-for-haku", (ctx, hakuOid) => {
    val (changed, exceptions) = refreshKoskiForHaku(hakuOid, ctx)
      .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0)}))
  }, Seq.empty)

  def startRefreshKoskiForHaku(hakuOid: String): UUID = refreshHakuJob.run(hakuOid)

  def retryKoskiResultFiles(fileUrls: Seq[String]): SaferIterator[SyncResultForHenkilo] =
    val fetchedAt = Instant.now()
    new SaferIterator(fileUrls.iterator).flatMap(fileUrl => processKoskiDataForOppijat(DUMMY_JOB_CTX, koskiIntegration.retryKoskiResultFile(fileUrl), fetchedAt))

  private def processKoskiDataForOppijat(ctx: SupaJobContext, data: SaferIterator[KoskiDataForOppija], fetchedAt: Instant): SaferIterator[SyncResultForHenkilo] =
    val kantaOperaatiot = KantaOperaatiot(database)

    data.map(oppija => {
      try {
        val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(oppija.oppijaOid, SuoritusJoukko.KOSKI, Seq(oppija.data), fetchedAt)
        versio.foreach(v => {
          LOG.info(s"Versio tallennettu henkilölle ${oppija.oppijaOid}")
          val oikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(oppija.data), koodistoProvider)
          kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(v, oikeudet.toSet, KoskiUtil.getTallennettavaMetadata(oikeudet))
        })
        SyncResultForHenkilo(oppija.oppijaOid, versio, None)
      } catch {
        case e: Exception =>
          val message = s"Henkilon ${oppija.oppijaOid} Koski-tietojen tallentaminen epäonnistui" 
          LOG.error(message, e)
          ctx.reportError(message, Some(e))
          SyncResultForHenkilo(oppija.oppijaOid, None, Some(e))
      }
    })
}
