package fi.oph.suorituspalvelu.resource

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.integration.ytr.YtrIntegration
import fi.oph.suorituspalvelu.integration.{KoskiIntegration, SyncResultForHenkilo}
import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_EI_OIKEUKSIA, DATASYNC_JSON_VIRHE, DATASYNC_RESPONSE_400_DESCRIPTION, DATASYNC_RESPONSE_403_DESCRIPTION, KOSKI_DATASYNC_500_VIRHE, KOSKI_DATASYNC_HAKU_PATH, KOSKI_DATASYNC_HENKILOT_LIIKAA, KOSKI_DATASYNC_HENKILOT_MAX_MAARA, KOSKI_DATASYNC_HENKILOT_PATH, KOSKI_DATASYNC_MUUTTUNEET_PATH, VIRTA_DATASYNC_HAKU_PATH, VIRTA_DATASYNC_JOBIN_LUONTI_EPAONNISTUI, VIRTA_DATASYNC_PARAM_NAME, VIRTA_DATASYNC_PATH, YTR_DATASYNC_HAKU_PATH, YTR_DATASYNC_PATH}
import fi.oph.suorituspalvelu.resource.api.{KoskiHaeMuuttuneetJalkeenPayload, KoskiSyncFailureResponse, KoskiSyncSuccessResponse, SyncResponse, VirtaSyncFailureResponse, VirtaSyncSuccessResponse, YtrSyncFailureResponse, YtrSyncSuccessResponse}
import fi.oph.suorituspalvelu.resource.ui.UIVirheet.UI_LUO_SUORITUS_PERUSOPETUS_JSON_VIRHE
import fi.oph.suorituspalvelu.resource.ui.{LuoPerusopetuksenOppimaaraFailureResponse, SyotettyPerusopetuksenOppimaaranSuoritus}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.{KoskiService, VirtaService}
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
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

import java.time.Instant
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

  @Autowired var koskiService: KoskiService = null

  @Autowired var virtaService: VirtaService = null

  @Autowired var ytrIntegration: YtrIntegration = null

  @Autowired var objectMapper: ObjectMapper = null

  @PostMapping(
    path = Array(KOSKI_DATASYNC_HENKILOT_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee yksittäisten hakijoiden tiedot Koskesta",
    description = "Huomioita:\n" +
      "- Huomio 1",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = Array(new Content(schema = new Schema(implementation = classOf[Array[String]])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus tehty, palauttaa VersioEntiteettejä (tulevaisuudessa jotain muuta?)"),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION)
    ))
  def paivitaKoskiTiedotHenkiloille(@RequestBody personOids: Array[String], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = KOSKI_DATASYNC_HENKILOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_EI_OIKEUKSIA)))))
        .flatMap(_ =>
          // validoidaan parametri
          if (personOids.toSet.size > KOSKI_DATASYNC_HENKILOT_MAX_MAARA) {
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(KOSKI_DATASYNC_HENKILOT_LIIKAA))))
          } else {
            val virheet: Set[String] = personOids.map(o => Validator.validateOppijanumero(Some(o), true)).flatten.toSet
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(new java.util.ArrayList(virheet.asJava))))
          })
        .map(_ => {
          try
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("personOids" -> personOids.mkString("Array(", ", ", ")")), AuditOperation.PaivitaKoskiTiedotHenkiloille, None)
            LOG.info(s"Haetaan Koski-tiedot henkilöille ${personOids.mkString("Array(", ", ", ")")}")
            val result = koskiService.syncKoskiForOppijat(personOids.toSet)
            LOG.info(s"Palautetaan rajapintavastaus, $result")
            ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(result.toString())) //Todo, tässä nyt palautellaan vain jotain mitä sattui jäämään käteen. Mitä tietoja oikeasti halutaan palauttaa?
          catch
            case e: Exception =>
              LOG.error(s"KOSKI-tietojen päivitys oppijoille ${personOids.mkString(",")} epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava)))
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
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION)
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
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_EI_OIKEUKSIA)))))
        .flatMap(_ =>
          // validoidaan parametri
          val virheet = Validator.validateHakuOid(hakuOid.toScala, true)
          if (virheet.isEmpty)
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
        .map(_ => {
          try
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("hakuOid" -> hakuOid.get), AuditOperation.PaivitaKoskiTiedotHaunHakijoille, None)
            LOG.info(s"Haetaan Koski-tiedot haun $hakuOid henkilöille")
            val (changed, exceptions) = koskiService.syncKoskiForHaku(hakuOid.get)
              .foldLeft((0, 0))((counts, result) => (counts._1 + { if(result.versio.isDefined) 1 else 0 }, counts._2 + { if(result.exception.isDefined) 1 else 0 }))
            val responseStr = s"Tallennettiin haulle $hakuOid yhteensä ${changed} versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia."
            LOG.info(s"Palautetaan rajapintavastaus, $responseStr")
            ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(responseStr))
          catch
            case e: Exception =>
              LOG.error(s"KOSKI-tietojen päivitys haulle ${hakuOid.get} epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava)))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(KOSKI_DATASYNC_MUUTTUNEET_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee muuttuneet tiedot Koskesta",
    description = "Huomioita:\n" +
      "- Huomio 1",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = Array(new Content(schema = new Schema(implementation = classOf[KoskiHaeMuuttuneetJalkeenPayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus tehty, palauttaa VersioEntiteettejä (tulevaisuudessa jotain muuta?)"),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION)
    ))
  def paivitaKoskiTiedotMuuttuneet(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = KOSKI_DATASYNC_MUUTTUNEET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_EI_OIKEUKSIA)))))
        .flatMap(_ =>
          // deserialisoidaan
          try
            Right(objectMapper.readValue(bytes, classOf[KoskiHaeMuuttuneetJalkeenPayload]).aikaleima)
          catch
            case e: Exception =>
              LOG.error("parametrin deserialisointi epäonnistui")
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
        .flatMap(aikaleima =>
          // validoidaan parametri
          val virheet = Validator.validateMuokattujalkeen(aikaleima.toScala, true)
          if(virheet.isEmpty)
            Right(Instant.parse(aikaleima.get))
          else
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
        .map(timestamp => {
          try
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("timestamp" -> timestamp.toString), AuditOperation.PaivitaMuuttuneetKoskiTiedot, None)
            LOG.info(s"Haetaan ${timestamp} jälkeen muuttuneet Koski-tiedot")
            val (changed, exceptions) = koskiService.syncKoskiChangesSince(timestamp)
              .foldLeft((0, 0))((counts, result) => (counts._1 + { if(result.versio.isDefined) 1 else 0 }, counts._2 + { if(result.exception.isDefined) 1 else 0 }))
            val responseStr = s"Tallennettiin yhteensä ${changed} muuttunutta versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia."
            LOG.info(s"Palautetaan rajapintavastaus, $responseStr")
            ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(responseStr))
          catch
            case e: Exception =>
              LOG.error("Muuttuneiden KOSKI-tietojen haku epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava)))
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
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(VirtaSyncFailureResponse(java.util.List.of(DATASYNC_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            // validoidaan parametri
            val virheet = Validator.validateOppijanumero(Some(oppijaNumero), true)
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VirtaSyncFailureResponse(java.util.List.of(Validator.VALIDATION_OPPIJANUMERO_EI_VALIDI)))))
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
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
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
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(VirtaSyncFailureResponse(java.util.List.of(DATASYNC_EI_OIKEUKSIA)))))
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
    path = Array(YTR_DATASYNC_PATH),
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
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaYtrTiedotHenkiloille(@RequestBody personOids: Array[String], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = YTR_DATASYNC_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(YtrSyncFailureResponse(java.util.List.of(DATASYNC_EI_OIKEUKSIA)))))
        .flatMap(_ =>
          // validoidaan parametri
          if (personOids.toSet.size > 5000) {
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YtrSyncFailureResponse(java.util.List.of("Korkeintaan 5000 henkilöä kerrallaan"))))
          } else {
            val virheet: Set[String] = personOids.map(oid => Validator.validateOppijanumero(Some(oid), true)).flatten.toSet
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YtrSyncFailureResponse(new java.util.ArrayList(virheet.asJava))))
          })
        .map(_ => {
          val user = AuditLog.getUser(request)
          AuditLog.log(user, Map("personOids" -> personOids.mkString("Array(", ", ", ")")), AuditOperation.PaivitaYtrTiedotHenkiloille, None)
          LOG.info(s"Haetaan Ytr-tiedot henkilöille ${personOids.mkString("Array(", ", ", ")")}")
          val result = ytrIntegration.fetchAndPersistStudents(personOids.toSet)
          LOG.info(s"Palautetaan rajapintavastaus, $result")
          ResponseEntity.status(HttpStatus.OK).body(YtrSyncSuccessResponse(result.toString())) //Todo, tässä nyt palautellaan vain jotain mitä sattui jäämään käteen. Mitä tietoja oikeasti halutaan palauttaa?
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(YTR_DATASYNC_HAKU_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee haun hakijoiden tiedot Ylioppilastutkintorekisteristä",
    description = "Huomioita:\n" +
      "- Huomio 1",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[String])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus tehty, palauttaa VersioEntiteettejä (tulevaisuudessa jotain muuta?)"),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaYtrTiedotHaulle(@RequestBody hakuOid: Optional[String], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = YTR_DATASYNC_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(YtrSyncFailureResponse(java.util.List.of(DATASYNC_EI_OIKEUKSIA)))))
        .flatMap(_ =>
          // validoidaan parametri
          val virheet = Validator.validateHakuOid(hakuOid.toScala, true)
          if (virheet.isEmpty)
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YtrSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
        .map(_ => {
          val user = AuditLog.getUser(request)
          AuditLog.log(user, Map("hakuOid" -> hakuOid.get), AuditOperation.PaivitaYtrTiedotHaunHakijoille, None)
          LOG.info(s"Haetaan Ytr-tiedot haun $hakuOid henkilöille")

          val result: Seq[SyncResultForHenkilo] = ytrIntegration.syncYtrForHaku(hakuOid.get)
          val responseStr = s"Tallennettiin haulle $hakuOid yhteensä ${result.size} henkilön ytr-tiedot."
          LOG.info(s"Palautetaan rajapintavastaus, $responseStr")
          ResponseEntity.status(HttpStatus.OK).body(YtrSyncSuccessResponse(responseStr))

        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }
}

