package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, ParserVersions, Lahdejarjestelma, VersioEntiteetti}
import fi.oph.suorituspalvelu.jobs.{SupaJobContext, SupaScheduler}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter, KoskiUtil}
import fi.oph.suorituspalvelu.parsing.virkailija.VirkailijaToSuoritusConverter
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import fi.oph.suorituspalvelu.parsing.ytr.{YtrParser, YtrToSuoritusConverter}
import fi.oph.suorituspalvelu.resource.ui.{SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer, SyotettyPerusopetuksenOppimaaranSuoritus}
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import java.time.{Duration, Instant}
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

@Service
class ReparseService(scheduler: SupaScheduler, kantaOperaatiot: KantaOperaatiot, koodistoProvider: KoodistoProvider, organisaatioProvider: OrganisaatioProvider, objectMapper: ObjectMapper) {

  val LOG = LoggerFactory.getLogger(classOf[ReparseService])

  final val PROGRESS_UPDATE_INTERVAL = 100
  final val VERSIO_ERAKOKO = 500
  final val TIMEOUT = 30.seconds

  private val HENKILO_TIMEOUT = 5.minutes

  /**
   * Käy läpi lähdejärjestelmän versiot ja parseroi ne uudelleen. Versiot haetaan erissä, koska isoilla
   * lähdejärjestelmillä (esim. VIRTA) koko joukon hakeminen yhdellä kyselyllä ei mahdu kannan timeoutin sisään.
   *
   * @param parseroi versiosta opiskeluoikeudet muodostava funktio
   */
  private def reparseVersiot(ctx: SupaJobContext, lahdejarjestelma: Lahdejarjestelma, parserVersio: Int, dryRun: String)(parseroi: VersioEntiteetti => Set[Opiskeluoikeus]): Unit = {
    val versioidenMaara = kantaOperaatiot.haeVersioidenMaara(lahdejarjestelma)
    LOG.info(s"Uudelleenparseroidaan ${lahdejarjestelma.nimi}-data ($versioidenMaara versiota), job id: ${ctx.getJobId}")
    var kasitelty = 0
    var era = kantaOperaatiot.haeVersiot(lahdejarjestelma, None, VERSIO_ERAKOKO)
    while(era.nonEmpty) {
      era.foreach(versio => {
        try
          val converted = parseroi(versio)
          if(!dryRun.toBoolean) kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, converted, KoskiUtil.getLahtokouluMetadata(converted), parserVersio)
        catch
          case e: Exception => LOG.error(s"Virhe henkilön ${versio.henkiloOid} ${lahdejarjestelma.nimi}-version ${versio.tunniste.toString} uudelleenparseroinnissa, job-id: ${ctx.getJobId}", e)
        kasitelty = kasitelty + 1
        if(kasitelty % PROGRESS_UPDATE_INTERVAL == 0) ctx.updateProgress(kasitelty.toDouble/Math.max(versioidenMaara, kasitelty).toDouble)
      })
      // jos erä jäi vajaaksi ei versioita ole enää jäljellä
      era = if(era.size < VERSIO_ERAKOKO) Seq.empty else kantaOperaatiot.haeVersiot(lahdejarjestelma, Some(era.last.tunniste), VERSIO_ERAKOKO)
    }
    LOG.info(s"${lahdejarjestelma.nimi}-datan uudelleenparserointi valmis, käsiteltiin $kasitelty versiota, job id: ${ctx.getJobId}")
  }

  private val reparseKoskiJob = scheduler.registerJob("reparse-koski-data", (ctx, dryRun) => {
    reparseVersiot(ctx, Lahdejarjestelma.KOSKI, ParserVersions.KOSKI, dryRun)(versio => {
      val data = kantaOperaatiot.haeJsonData(versio)
      val parsed = data.map(d => KoskiParser.parseKoskiData(d))
      KoskiToSuoritusConverter.parseOpiskeluoikeudet(parsed, koodistoProvider).toSet
    })
  }, Seq.empty)

  def reparseKoski(dryRun: Boolean): UUID = reparseKoskiJob.run(dryRun.toString)

  private val reparseVirtaJob = scheduler.registerJob("reparse-virta-data", (ctx, dryRun) => {
    reparseVersiot(ctx, Lahdejarjestelma.VIRTA, ParserVersions.VIRTA, dryRun)(versio => {
      val data = kantaOperaatiot.haeXmlData(versio)
      val virtaOpiskelijat = data.flatMap(VirtaParser.parseVirtaOpiskelijat)
      VirtaToSuoritusConverter.toOpiskeluoikeudet(virtaOpiskelijat).toSet
    })
  }, Seq.empty)

  def reparseVirta(dryRun: Boolean): UUID = reparseVirtaJob.run(dryRun.toString)

  private val reparseYTRJob = scheduler.registerJob("reparse-ytr-data", (ctx, dryRun) => {
    reparseVersiot(ctx, Lahdejarjestelma.YTR, ParserVersions.YTR, dryRun)(versio => {
      val data = kantaOperaatiot.haeJsonData(versio)
      val parsed = data.map(d => YtrParser.parseYtrData(d))
      parsed.map(s => YtrToSuoritusConverter.toSuoritus(s)).toSet
    })
  }, Seq.empty)

  def reparseYTR(dryRun: Boolean): UUID = reparseYTRJob.run(dryRun.toString)

  private val reparsePerusopetuksenOppimaaratJob = scheduler.registerJob("reparse-perusopetus-data", (ctx, dryRun) => {
    reparseVersiot(ctx, Lahdejarjestelma.SYOTETTY_PERUSOPETUS, ParserVersions.SYOTETTY_PERUSOPETUS, dryRun)(versio => {
      val data = kantaOperaatiot.haeJsonData(versio)
      val parsed = data.map(d => objectMapper.readValue(d, classOf[SyotettyPerusopetuksenOppimaaranSuoritus]))
      parsed.map(p => VirkailijaToSuoritusConverter.toPerusopetuksenOppimaara(versio.tunniste, p, koodistoProvider, organisaatioProvider)).toSet
    })
  }, Seq.empty)

  def reparsePerusopetuksenOppimaarat(dryRun: Boolean): UUID = reparsePerusopetuksenOppimaaratJob.run(dryRun.toString)

  private val reparsePerusopetuksenOppiaineenOppimaaratJob = scheduler.registerJob("reparse-perusopetus-oppiaineet-data", (ctx, dryRun) => {
    reparseVersiot(ctx, Lahdejarjestelma.SYOTETYT_OPPIAINEET, ParserVersions.SYOTETYT_OPPIAINEET, dryRun)(versio => {
      val data = kantaOperaatiot.haeJsonData(versio)
      val parsed = data.map(d => objectMapper.readValue(d, classOf[SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer]))
      parsed.map(p => VirkailijaToSuoritusConverter.toPerusopetuksenOppiaineenOppimaara(versio.tunniste, p, koodistoProvider, organisaatioProvider)).toSet
    })
  }, Seq.empty)

  def reparsePerusopetuksenOppiaineenOppimaarat(dryRun: Boolean): UUID = reparsePerusopetuksenOppiaineenOppimaaratJob.run(dryRun.toString)
  
}