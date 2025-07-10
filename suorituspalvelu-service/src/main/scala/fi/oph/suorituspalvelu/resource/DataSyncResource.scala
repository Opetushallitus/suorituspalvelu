package fi.oph.suorituspalvelu.resource

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.integration.ytr.YtrIntegration
import fi.oph.suorituspalvelu.integration.{KoskiIntegration, SyncResultForHenkilo}
import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_PATH, DATASYNC_RESPONSE_400_DESCRIPTION, DATASYNC_RESPONSE_403_DESCRIPTION, KOSKI_DATASYNC_HAKU_PATH, KOSKI_DATASYNC_PATH, VIRTA_DATASYNC_HAKU_PATH, VIRTA_DATASYNC_JOBIN_LUONTI_EPAONNISTUI, VIRTA_DATASYNC_PARAM_NAME, VIRTA_DATASYNC_PATH}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.VirtaService
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
import fi.oph.suorituspalvelu.validation.Validator.VALIDATION_OPPIJANUMERO_EI_VALIDI
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.{PathVariable, PostMapping, RequestBody, RequestMapping, RequestParam, RestController}

import java.util.{Optional, UUID}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@RequestMapping(path = Array(""))
@RestController
@Tag(
  name = "Data sync",
  description = "Manuaalinen datan haku lähdejärjestelmistä")
class DataSyncResource {

  val LOG = LoggerFactory.getLogger(classOf[DataSyncResource])

  @Autowired var mapper: ObjectMapper = null

  @Autowired var koskiIntegration: KoskiIntegration = null

  @Autowired var virtaService: VirtaService = null

  @Autowired var ytrIntegration: YtrIntegration = null

  @PostMapping(
    path = Array(KOSKI_DATASYNC_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee tiedot Koskesta",
    description = "Huomioita:\n" +
      "- Huomio 1",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = Array(new Content(schema = new Schema(implementation = classOf[Array[String]])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus tehty, palauttaa VersioEntiteettejä (tulevaisuudessa jotain muuta?)"),
      new ApiResponse(responseCode = "400", description = "Pyyntö on virheellinen"),
      new ApiResponse(responseCode = "403", description = "addme")
    ))
  def paivitaKoskiTiedotHenkiloille(@RequestBody personOids: Array[String], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = KOSKI_DATASYNC_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(KoskiSyncFailureResponse(java.util.List.of("ei oikeuksia")))))
        .flatMap(_ =>
          // validoidaan parametri
          if (personOids.toSet.size > 5000) {
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of("Korkeintaan 5000 henkilöä kerrallaan"))))
          } else {
            val virheet: Set[String] = Validator.validatePersonOids(personOids.toSet)
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(new java.util.ArrayList(virheet.asJava))))
          })
        .map(_ => {
          val user = AuditLog.getUser(request)
          AuditLog.log(user, Map("personOids" -> personOids.mkString("Array(", ", ", ")")), AuditOperation.PaivitaKoskiTiedotHenkiloille, None)
          LOG.info(s"Haetaan Koski-tiedot henkilöille ${personOids.mkString("Array(", ", ", ")")}")
          val result = koskiIntegration.syncKoskiInBatches(personOids.toSet)
          LOG.info(s"Palautetaan rajapintavastaus, $result")
          ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(result.toString())) //Todo, tässä nyt palautellaan vain jotain mitä sattui jäämään käteen. Mitä tietoja oikeasti halutaan palauttaa?
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(KOSKI_DATASYNC_HAKU_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee haun hakijoiden tiedot Koskesta",
    description = "Huomioita:\n" +
      "- Huomio 1",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[String])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus tehty, palauttaa VersioEntiteettejä (tulevaisuudessa jotain muuta?)"),
      new ApiResponse(responseCode = "400", description = "Pyyntö on virheellinen"),
      new ApiResponse(responseCode = "403", description = "addme")
    ))
  def paivitaKoskiTiedotHaulle(@RequestBody hakuOid: Optional[String], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = KOSKI_DATASYNC_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(KoskiSyncFailureResponse(java.util.List.of("ei oikeuksia")))))
        .flatMap(_ =>
          // validoidaan parametri
          val virheet = Validator.validateHakuOid(hakuOid.toScala, true)
          if (virheet.isEmpty)
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
        .map(_ => {
          val user = AuditLog.getUser(request)
          AuditLog.log(user, Map("hakuOid" -> hakuOid.get), AuditOperation.PaivitaKoskiTiedotHaunHakijoille, None)
          LOG.info(s"Haetaan Koski-tiedot haun $hakuOid henkilöille")
          val result: Seq[SyncResultForHenkilo] = koskiIntegration.syncKoskiForHaku(hakuOid.get)
          val responseStr = s"Tallennettiin haulle $hakuOid yhteensä ${result.count(_.versio.isDefined)} versiotietoa. Yhteensä ${result.count(_.exception.isDefined)} henkilön tietojen tallennuksessa oli ongelmia."
          LOG.info(s"Palautetaan rajapintavastaus, $responseStr")
          ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(responseStr))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(VIRTA_DATASYNC_PATH),
    consumes = Array(MediaType.ALL_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää yksittäisen oppijan tiedot Virrasta",
    description = "Huomioita:\n" +
      "- Huomio 1",
    parameters = Array(new Parameter(name = VIRTA_DATASYNC_PARAM_NAME, in = ParameterIn.PATH)),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus käynnistetty, palauttaa job-id:n", content = Array(new Content(schema = new Schema(implementation = classOf[VirtaSyncSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[VirtaSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaVirtaTiedot(@PathVariable(VIRTA_DATASYNC_PARAM_NAME) oppijaNumero: String, @RequestParam(name = "hetu", required = false) hetu: String, request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = VIRTA_DATASYNC_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(VirtaSyncFailureResponse(java.util.List.of("ei oikeuksia")))))
          .flatMap(_ =>
            // validoidaan parametri
            val virheet = Validator.validateOppijanumero(Some(oppijaNumero), true)
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VirtaSyncFailureResponse(java.util.List.of(VALIDATION_OPPIJANUMERO_EI_VALIDI)))))
          .map(_ =>
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(VIRTA_DATASYNC_PARAM_NAME -> oppijaNumero), AuditOperation.PaivitaVirtaTiedot, None)
            LOG.info(s"Haetaan Virta-tiedot henkilölle ${oppijaNumero}")
            val jobId = virtaService.syncVirta(oppijaNumero, Option.apply(hetu))
            LOG.info(s"Palautetaan rajapintavastaus, $jobId")
            ResponseEntity.status(HttpStatus.OK).body(VirtaSyncSuccessResponse(jobId)))
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijan Virta-päivitysjobin luonti epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(VirtaSyncFailureResponse(java.util.List.of(VIRTA_DATASYNC_JOBIN_LUONTI_EPAONNISTUI)))
  }

  @PostMapping(
    path = Array(VIRTA_DATASYNC_HAKU_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee haun hakijoiden tiedot Virrasta",
    description = "Huomioita:\n" +
      "- Huomio 1",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[String])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus tehty, palauttaa VersioEntiteettejä (tulevaisuudessa jotain muuta?)"),
      new ApiResponse(responseCode = "400", description = "Pyyntö on virheellinen"),
      new ApiResponse(responseCode = "403", description = "addme")
    ))
  def paivitaVirtaTiedotHaulle(@RequestBody hakuOid: Optional[String], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = VIRTA_DATASYNC_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(VirtaSyncFailureResponse(java.util.List.of("ei oikeuksia")))))
        .flatMap(_ =>
          // validoidaan parametri
          val virheet = Validator.validateHakuOid(hakuOid.toScala, true)
          if (virheet.isEmpty)
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VirtaSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
        .map(_ => {
          val user = AuditLog.getUser(request)
          AuditLog.log(user, Map("hakuOid" -> hakuOid.get), AuditOperation.PaivitaVirtaTiedotHaunHakijoille, None)
          LOG.info(s"Haetaan Virta-tiedot haun $hakuOid henkilöille")
          val jobId = virtaService.syncVirtaForHaku(hakuOid.get)
          LOG.info(s"Palautetaan rajapintavastaus, $jobId")
          ResponseEntity.status(HttpStatus.OK).body(VirtaSyncSuccessResponse(jobId))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(KOSKI_DATASYNC_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee tiedot Ylioppilastutkintorekisteristä",
    description = "Huomioita:\n" +
      "- Huomio 1",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = Array(new Content(schema = new Schema(implementation = classOf[Array[String]])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus tehty, palauttaa VersioEntiteettejä (tulevaisuudessa jotain muuta?)"),
      new ApiResponse(responseCode = "400", description = "Pyyntö on virheellinen"),
      new ApiResponse(responseCode = "403", description = "addme")
    ))
  def paivitaYtrTiedotHenkiloille(@RequestBody personOids: Array[String], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = KOSKI_DATASYNC_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(YtrSyncFailureResponse(java.util.List.of("ei oikeuksia")))))
        .flatMap(_ =>
          // validoidaan parametri
          if (personOids.toSet.size > 5000) {
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YtrSyncFailureResponse(java.util.List.of("Korkeintaan 5000 henkilöä kerrallaan"))))
          } else {
            val virheet: Set[String] = Validator.validatePersonOids(personOids.toSet)
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YtrSyncFailureResponse(new java.util.ArrayList(virheet.asJava))))
          })
        .map(_ => {
          val user = AuditLog.getUser(request)
          AuditLog.log(user, Map("personOids" -> personOids.mkString("Array(", ", ", ")")), AuditOperation.PaivitaKoskiTiedotHenkiloille, None)
          LOG.info(s"Haetaan Ytr-tiedot henkilöille ${personOids.mkString("Array(", ", ", ")")}")
          val result = ytrIntegration.fetchRawForStudents(personOids.toSet)
          LOG.info(s"Palautetaan rajapintavastaus, $result")
          ResponseEntity.status(HttpStatus.OK).body(YtrSyncSuccessResponse(result.toString())) //Todo, tässä nyt palautellaan vain jotain mitä sattui jäämään käteen. Mitä tietoja oikeasti halutaan palauttaa?
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }
}

