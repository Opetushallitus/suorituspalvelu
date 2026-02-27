package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, ParserVersions, Lahdejarjestelma, VersioEntiteetti}
import fi.oph.suorituspalvelu.jobs.SupaScheduler
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
  final val TIMEOUT = 30.seconds

  private val HENKILO_TIMEOUT = 5.minutes
  
  private val reparseKoskiJob = scheduler.registerJob("reparse-koski-data", (ctx, dryRun) => {
    LOG.info(s"Uudelleenparseroidaan KOSKI-data, job id: ${ctx.getJobId}")
    val versiot = kantaOperaatiot.haeVersiot(Lahdejarjestelma.KOSKI)
    versiot.zipWithIndex.foreach((versio, idx) => {
        try
          if(idx % PROGRESS_UPDATE_INTERVAL == 0) ctx.updateProgress(idx.toDouble/versiot.size.toDouble)
          val (_, data, _) = kantaOperaatiot.haeData(versio)
          val parsed = data.map(d => KoskiParser.parseKoskiData(d))
          val converted = KoskiToSuoritusConverter.parseOpiskeluoikeudet(parsed, koodistoProvider).toSet
          if(!dryRun.toBoolean) kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, converted, KoskiUtil.getLahtokouluMetadata(converted), ParserVersions.KOSKI)
        catch
          case e: Exception => LOG.error(s"Virhe henkilön ${versio.henkiloOid} KOSKI-version ${versio.tunniste.toString} uudelleenparseroinnissa, job-id: ${ctx.getJobId}", e)
      })
    LOG.info(s"KOSKI-datan uudelleenparserpointi valmis, job id: ${ctx.getJobId}")
  }, Seq.empty)

  def reparseKoski(dryRun: Boolean): UUID = reparseKoskiJob.run(dryRun.toString)

  private val reparseVirtaJob = scheduler.registerJob("reparse-virta-data", (ctx, dryRun) => {
    LOG.info(s"Uudelleenparseroidaan VIRTA-data, job id: ${ctx.getJobId}")
    val versiot = kantaOperaatiot.haeVersiot(Lahdejarjestelma.VIRTA)
    versiot.zipWithIndex.foreach((versio, idx) => {
        try
          if(idx % PROGRESS_UPDATE_INTERVAL == 0) ctx.updateProgress(idx.toDouble/versiot.size.toDouble)
          val (_, _, data) = kantaOperaatiot.haeData(versio)
          val virtaOpiskelijat = data.flatMap(VirtaParser.parseVirtaOpiskelijat)
          val converted: Set[Opiskeluoikeus] = VirtaToSuoritusConverter.toOpiskeluoikeudet(virtaOpiskelijat).toSet
          if(!dryRun.toBoolean) kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, converted, KoskiUtil.getLahtokouluMetadata(converted), ParserVersions.VIRTA)
        catch
          case e: Exception => LOG.error(s"Virhe henkilön ${versio.henkiloOid} VIRTA-version ${versio.tunniste.toString} uudelleenparseroinnissa, job-id: ${ctx.getJobId}", e)
      })
    LOG.info(s"VIRTA-datan uudelleenparserpointi valmis, job id: ${ctx.getJobId}")
  }, Seq.empty)

  def reparseVirta(dryRun: Boolean): UUID = reparseVirtaJob.run(dryRun.toString)

  private val reparseYTRJob = scheduler.registerJob("reparse-ytr-data", (ctx, dryRun) => {
    LOG.info(s"Uudelleenparseroidaan YTR-data, job id: ${ctx.getJobId}")
    val versiot = kantaOperaatiot.haeVersiot(Lahdejarjestelma.YTR)
    versiot.zipWithIndex.foreach((versio, idx) => {
        try
          if(idx % PROGRESS_UPDATE_INTERVAL == 0) ctx.updateProgress(idx.toDouble/versiot.size.toDouble)
          val (_, data, _) = kantaOperaatiot.haeData(versio)
          val parsed = data.map(d => YtrParser.parseYtrData(d))
          val converted: Set[Opiskeluoikeus] = parsed.map(s => YtrToSuoritusConverter.toSuoritus(s)).toSet
          if(!dryRun.toBoolean) kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, converted, KoskiUtil.getLahtokouluMetadata(converted), ParserVersions.YTR)
        catch
          case e: Exception => LOG.error(s"Virhe henkilön ${versio.henkiloOid} YTR-version ${versio.tunniste.toString} uudelleenparseroinnissa, job-id: ${ctx.getJobId}", e)
      })
    LOG.info(s"YTR-datan uudelleenparserpointi valmis, job id: ${ctx.getJobId}")
  }, Seq.empty)

  def reparseYTR(dryRun: Boolean): UUID = reparseYTRJob.run(dryRun.toString)

  private val reparsePerusopetuksenOppimaaratJob = scheduler.registerJob("reparse-perusopetus-data", (ctx, dryRun) => {
    LOG.info(s"Uudelleenparseroidaan syötetyt perusopetuksen oppimäärät, job id: ${ctx.getJobId}")
    val versiot = kantaOperaatiot.haeVersiot(Lahdejarjestelma.SYOTETTY_PERUSOPETUS)
    versiot.zipWithIndex.foreach((versio, idx) => {
        try
          if(idx % PROGRESS_UPDATE_INTERVAL == 0) ctx.updateProgress(idx.toDouble/versiot.size.toDouble)
          val (_, data, _) = kantaOperaatiot.haeData(versio)
          val parsed = data.map(d => objectMapper.readValue(d, classOf[SyotettyPerusopetuksenOppimaaranSuoritus]))
          val converted: Set[Opiskeluoikeus] = parsed.map(p => VirkailijaToSuoritusConverter.toPerusopetuksenOppimaara(versio.tunniste, p, koodistoProvider, organisaatioProvider)).toSet
          if(!dryRun.toBoolean) kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, converted, KoskiUtil.getLahtokouluMetadata(converted), ParserVersions.SYOTETTY_PERUSOPETUS)
        catch
          case e: Exception => LOG.error(s"Virhe henkilön ${versio.henkiloOid} käsin syötetyn version ${versio.tunniste.toString} uudelleenparseroinnissa, job-id: ${ctx.getJobId}", e)
      })
    LOG.info(s"Syotettyjen perusopetuksen oppimäärien uudelleenparserpointi valmis, job id: ${ctx.getJobId}")
  }, Seq.empty)

  def reparsePerusopetuksenOppimaarat(dryRun: Boolean): UUID = reparsePerusopetuksenOppimaaratJob.run(dryRun.toString)

  private val reparsePerusopetuksenOppiaineenOppimaaratJob = scheduler.registerJob("reparse-perusopetus-oppiaineet-data", (ctx, dryRun) => {
    LOG.info(s"Uudelleenparseroidaan syötetyt perusopetuksen oppimäärän oppiaineet, job id: ${ctx.getJobId}")
    val versiot = kantaOperaatiot.haeVersiot(Lahdejarjestelma.SYOTETYT_OPPIAINEET)
    versiot.zipWithIndex.foreach((versio, idx) => {
        try
          if(idx % PROGRESS_UPDATE_INTERVAL == 0) ctx.updateProgress(idx.toDouble/versiot.size.toDouble)
          val (_, data, _) = kantaOperaatiot.haeData(versio)
          val parsed = data.map(d => objectMapper.readValue(d, classOf[SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer]))
          val converted: Set[Opiskeluoikeus] = parsed.map(p => VirkailijaToSuoritusConverter.toPerusopetuksenOppiaineenOppimaara(versio.tunniste, p, koodistoProvider, organisaatioProvider)).toSet
          if(!dryRun.toBoolean) kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, converted, KoskiUtil.getLahtokouluMetadata(converted), ParserVersions.SYOTETYT_OPPIAINEET)
        catch
          case e: Exception => LOG.error(s"Virhe henkilön ${versio.henkiloOid} käsin syötetyn version ${versio.tunniste.toString} uudelleenparseroinnissa, job-id: ${ctx.getJobId}", e)
      })
    LOG.info(s"Syotettyjen perusopetuksen oppiaineen oppimäärien uudelleenparserpointi valmis, job id: ${ctx.getJobId}")
  }, Seq.empty)

  def reparsePerusopetuksenOppiaineenOppimaarat(dryRun: Boolean): UUID = reparsePerusopetuksenOppiaineenOppimaaratJob.run(dryRun.toString)
  
}