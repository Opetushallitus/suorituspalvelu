package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_RESPONSE_403_DESCRIPTION, OVARA_409_VIRHE, OVARA_500_VIRHE, OVARA_OPISKELUOIKEUDET_PATH, OVARA_PAIVITTAISET_PATH}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.{OvaraParams, OvaraService}
import fi.oph.suorituspalvelu.util.LogContext
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.{PostMapping, RequestMapping, RequestParam, RestController}

import java.time.Instant
import java.util.Optional
import java.util.concurrent.{CompletableFuture, ExecutorService}
import java.util.concurrent.atomic.AtomicReference
import scala.jdk.OptionConverters.*

@RequestMapping(path = Array(""))
@RestController
@Tag(
  name = "Ovara-siirtotiedostot",
  description = "Rajapinnat Ovara-järjestelmää varten tuotettavien siirtotiedostojen muodostamiseen. " +
    "Siirtotiedostot sisältävät avain-arvo-pareja ja harkinnanvaraisuustietoja aktiivisten toisen asteen hakujen " +
    "hakijoista. Vain rekisterinpitäjällä on pääsy näihin rajapintoihin.")
class OvaraResource {

  val LOG = LoggerFactory.getLogger(classOf[OvaraResource])

  @Autowired var ovaraService: OvaraService = null
  @Autowired var ovaraExecutor: ExecutorService = null

  private val runningExecutionId = new AtomicReference[String](null)

  private def claimOrConflict(executionId: String): Either[ResponseEntity[String], String] =
    if runningExecutionId.compareAndSet(null, executionId) then Right(executionId)
    else Left(ResponseEntity.status(HttpStatus.CONFLICT)
      .body(s"$OVARA_409_VIRHE (käynnissä: ${runningExecutionId.get})"))

  private def submitJob(executionId: String, job: => Unit): Unit =
    try
      CompletableFuture
        .runAsync(() => {
          try job
          catch case e: Exception => LOG.error(s"($executionId) Ovara-ajo epäonnistui", e)
        }, ovaraExecutor)
        .whenComplete((_, _) => runningExecutionId.set(null))
    catch
      case e: Exception =>
        runningExecutionId.set(null)
        throw e

  @PostMapping(
    path = Array(OVARA_PAIVITTAISET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Muodostaa päivittäiset siirtotiedostot Ovaraa varten",
    description = "Hakee kaikkien aktiivisten toisen asteen hakujen hakijat ja muodostaa heistä avain-arvo- ja " +
      "harkinnanvaraisuussiirtotiedostot Ovara-järjestelmää varten. Ajo käynnistetään taustalle ja voi kestää useita minuutteja.",
    responses = Array(
      new ApiResponse(responseCode = "202", description = "Siirtotiedostojen muodostus käynnistetty, palauttaa ajon tunnisteen",
        content = Array(new Content(schema = new Schema(implementation = classOf[String])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION,
        content = Array(new Content(schema = new Schema(implementation = classOf[Void])))),
      new ApiResponse(responseCode = "409", description = OVARA_409_VIRHE,
        content = Array(new Content(schema = new Schema(implementation = classOf[String])))),
      new ApiResponse(responseCode = "500", description = OVARA_500_VIRHE,
        content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def muodostaPaivittaiset(
    @RequestParam(required = false) @Parameter(description = "Ajon tunniste, generoidaan automaattisesti jos ei annettu") executionId: Optional[String],
    @RequestParam(required = false, defaultValue = "true") @Parameter(description = "Käsitellään vain aktiiviset haut") vainAktiiviset: Boolean,
    request: HttpServletRequest
  ): ResponseEntity[String] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = OVARA_PAIVITTAISET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            if (securityOperaatiot.onRekisterinpitaja()) Right(None)
            else Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
          .flatMap(_ => {
            val params = OvaraParams(
              executionId    = executionId.toScala.getOrElse(java.util.UUID.randomUUID().toString),
              vainAktiiviset = vainAktiiviset
            )
            claimOrConflict(params.executionId).map(eid => {
              AuditLog.log(AuditLog.getUser(request), Map(
                "vainAktiiviset" -> vainAktiiviset.toString
              ), AuditOperation.MuodostaPaivittaisetOvara, None)
              LOG.info(s"Käynnistetään päivittäiset Ovara-siirtotiedostot $params")
              submitJob(eid, ovaraService.muodostaPaivittaisetHauille(params))
              ResponseEntity.status(HttpStatus.ACCEPTED).body(eid)
            })
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[String]])
    catch
      case e: Exception =>
        LOG.error("Päivittäisten Ovara-siirtotiedostojen käynnistys epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
  }

  @PostMapping(
    path = Array(OVARA_OPISKELUOIKEUDET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Muodostaa opiskeluoikeussiirtotiedostot Ovaraa varten annetulle aikavälille",
    description = "Hakee annetulla aikavälillä muuttuneet opiskeluoikeudet ja muodostaa niistä siirtotiedostot Ovara-järjestelmää varten. Ajo käynnistetään taustalle.",
    responses = Array(
      new ApiResponse(responseCode = "202", description = "Siirtotiedostojen muodostus käynnistetty, palauttaa ajon tunnisteen",
        content = Array(new Content(schema = new Schema(implementation = classOf[String])))),
      new ApiResponse(responseCode = "400", description = "Puuttuva tai virheellinen aikavälipäärametri",
        content = Array(new Content(schema = new Schema(implementation = classOf[Void])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION,
        content = Array(new Content(schema = new Schema(implementation = classOf[Void])))),
      new ApiResponse(responseCode = "409", description = OVARA_409_VIRHE,
        content = Array(new Content(schema = new Schema(implementation = classOf[String])))),
      new ApiResponse(responseCode = "500", description = OVARA_500_VIRHE,
        content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def muodostaOpiskeluoikeussiirtotiedostot(
    @RequestParam @Parameter(description = "Aikavälin alku (ISO-8601, esim. 2026-01-01T00:00:00Z)") windowStart: String,
    @RequestParam @Parameter(description = "Aikavälin loppu (ISO-8601, esim. 2026-06-01T00:00:00Z)") windowEnd: String,
    @RequestParam(required = false) @Parameter(description = "Ajon tunniste, generoidaan automaattisesti jos ei annettu") executionId: Optional[String],
    request: HttpServletRequest
  ): ResponseEntity[String] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = OVARA_OPISKELUOIKEUDET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            if (securityOperaatiot.onRekisterinpitaja()) Right(None)
            else Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build()))
          .flatMap(_ =>
            try Right((Instant.parse(windowStart), Instant.parse(windowEnd)))
            catch case _: Exception => Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).build()))
          .flatMap((start, end) => {
            val params = OvaraParams(
              executionId = executionId.toScala.getOrElse(java.util.UUID.randomUUID().toString)
            )
            claimOrConflict(params.executionId).map(eid => {
              AuditLog.log(AuditLog.getUser(request), Map(
                "windowStart" -> windowStart,
                "windowEnd"   -> windowEnd
              ), AuditOperation.MuodostaOpiskeluoikeussiirtotiedostotOvara, None)
              LOG.info(s"Käynnistetään opiskeluoikeus-siirtotiedostot aikavälille $windowStart – $windowEnd ($eid)")
              submitJob(eid, ovaraService.muodostaOpiskeluoikeusSiirtotiedostot(params, start, end))
              ResponseEntity.status(HttpStatus.ACCEPTED).body(eid)
            })
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[String]])
    catch
      case e: Exception =>
        LOG.error("Ovara opiskeluoikeussiirtotiedostojen käynnistys epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
  }
}
