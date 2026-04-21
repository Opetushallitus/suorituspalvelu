package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_RESPONSE_403_DESCRIPTION, OVARA_500_VIRHE, OVARA_PAIVITTAISET_PATH}
import fi.oph.suorituspalvelu.resource.api.SyncResponse
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

import java.util.Optional
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

  @PostMapping(
    path = Array(OVARA_PAIVITTAISET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Muodostaa päivittäiset siirtotiedostot Ovaraa varten",
    description = "Hakee kaikkien aktiivisten toisen asteen hakujen hakijat ja muodostaa heistä avain-arvo- ja " +
      "harkinnanvaraisuussiirtotiedostot Ovara-järjestelmää varten. Ajo voi kestää useita minuutteja.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Siirtotiedostot muodostettu onnistuneesti",
        content = Array(new Content(schema = new Schema(implementation = classOf[Void])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION,
        content = Array(new Content(schema = new Schema(implementation = classOf[Void])))),
      new ApiResponse(responseCode = "500", description = OVARA_500_VIRHE,
        content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def muodostaPaivittaiset(
    @RequestParam(required = false) @Parameter(description = "Ajon tunniste, generoidaan automaattisesti jos ei annettu") executionId: Optional[String],
    @RequestParam(required = false, defaultValue = "true") @Parameter(description = "Käsitellään vain aktiiviset haut") vainAktiiviset: Boolean,
    @RequestParam(required = false, defaultValue = "true") @Parameter(description = "Muodostetaan avain-arvotiedostot") avainArvot: Boolean,
    @RequestParam(required = false, defaultValue = "true") @Parameter(description = "Muodostetaan harkinnanvaraisuustiedostot") harkinnanvaraisuudet: Boolean,
    @RequestParam(required = false, defaultValue = "false") @Parameter(description = "Muodostetaan ensikertalaisuustiedostot") ensikertalaisuudet: Boolean,
    request: HttpServletRequest
  ): ResponseEntity[SyncResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = OVARA_PAIVITTAISET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .map(_ => {
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(
              "vainAktiiviset"     -> vainAktiiviset.toString,
              "avainArvot"         -> avainArvot.toString,
              "harkinnanvaraisuudet" -> harkinnanvaraisuudet.toString,
              "ensikertalaisuudet" -> ensikertalaisuudet.toString
            ), AuditOperation.MuodostaPaivittaisetOvara, None)
            val params = OvaraParams(
              executionId        = executionId.toScala.getOrElse(java.util.UUID.randomUUID().toString),
              vainAktiiviset     = vainAktiiviset,
              avainArvot         = avainArvot,
              harkinnanvaraisuudet = harkinnanvaraisuudet,
              ensikertalaisuudet = ensikertalaisuudet
            )
            LOG.info(s"Muodostetaan päivittäiset Ovara-siirtotiedostot $params")
            ovaraService.muodostaPaivittaisetHauille(params)
            ResponseEntity.status(HttpStatus.OK).build()
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Ovara-siirtotiedostojen muodostus epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
  }
}
