package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{TELMA, TUVA, VAPAA_SIVISTYSTYO, VUOSILUOKKA_9}
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

import java.time.{Duration, Instant, LocalDate}
import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

@Component
class KoskiService(scheduler: SupaScheduler, kantaOperaatiot: KantaOperaatiot, hakemuspalveluClient: HakemuspalveluClientImpl,
                   tarjontaIntegration: TarjontaIntegration, koskiIntegration: KoskiIntegration, koodistoProvider: KoodistoProvider) {

  val LOG = LoggerFactory.getLogger(classOf[KoskiService])

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  private val HENKILO_TIMEOUT = 5.minutes
  private val HAKEMUKSET_TIMEOUT = 1.minutes

  final val YSILUOKKALAINEN_TAI_LISAPISTEKOULUTUS = Set(VUOSILUOKKA_9, TELMA, TUVA, VAPAA_SIVISTYSTYO)

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

      // Aktiivisen haun loppuminen johtuu ajan kulumisesta eikä KOSKI-tietojen muutoksesta, joten jos halutaan että
      // suoritustietoihin tehdään muutoksia (esim. merkataan viimeisen version voimassaolo päättyneeksi) ne täytyy tehdä
      // muulla mekanismilla (esim. toistuva jobi)
      def hasAktiivinenHaku(oppijaOid: String): Boolean =
        oppijanHaut.get(oppijaOid)
          .exists(haut => haut.exists(haku => aktiivisetHaut.contains(haku)))

      // Huomioita:
      //  - jos päivitetään sen perusteella että KOSKI sanoo että ysiluokkalainen, pitää päivittää myös sillä perusteella
      //    että SUPAn mukaan ysiluokkalainen, muuten tieto siitä ettei olekaan ysiluokkalainen ei päivity
      //  - vaikka teoriassa yksilöinnit voisivat paljastaa että henkilö on jo muussa koulutuksessa, yksilöintitietoja
      //    ei voi käyttää tiedon tallennuksesta päätettäessä koska yksilöinnit voivat muuttua
      //  - tiedot lisäpistekoulutuksista päivitetään kuten ysiluokkalaiset koska näkyvät tarkastusnäkymässä
      def isYsiluokkalainenTaiLisapiste(koskiData: KoskiDataForOppija): Boolean =
        val opiskeluoikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(koskiData.data), koodistoProvider)
        KoskiUtil.onkoJokinLahtokoulu(LocalDate.now, None, Some(YSILUOKKALAINEN_TAI_LISAPISTEKOULUTUS), opiskeluoikeudet.toSet) ||
        KoskiUtil.onkoJokinLahtokoulu(LocalDate.now, None, Some(YSILUOKKALAINEN_TAI_LISAPISTEKOULUTUS), kantaOperaatiot.haeSuoritukset(koskiData.oppijaOid).values.flatten.toSet)
      
      val filtteroity = chunk.filter(r => hasAktiivinenHaku(r.oppijaOid) || isYsiluokkalainenTaiLisapiste(r))
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
    data.map(oppija => {
      try {
        val versio: Option[VersioEntiteetti] = kantaOperaatiot.tallennaJarjestelmaVersio(oppija.oppijaOid, SuoritusJoukko.KOSKI, Seq(oppija.data), fetchedAt)
        versio.foreach(v => {
          LOG.info(s"Versio tallennettu henkilölle ${oppija.oppijaOid}")
          val oikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(oppija.data), koodistoProvider)
          kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(v, oikeudet.toSet, KoskiUtil.getLahtokouluMetadata(oikeudet.toSet))
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
