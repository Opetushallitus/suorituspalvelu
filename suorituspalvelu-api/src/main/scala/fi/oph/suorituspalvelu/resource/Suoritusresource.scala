package fi.oph.suorituspalvelu.resource

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Koodi, PerusopetuksenOppiaine, PerusopetuksenOppimaara}
import fi.oph.suorituspalvelu.business.Tietolahde.VIRKAILIJA
import fi.oph.suorituspalvelu.resource.ApiConstants.{SUORITUKSEN_LUONTI_EPAONNISTUI, SUORITUS_PATH, SUORITUS_RESPONSE_403_DESCRIPTION, VIRHEELLINEN_SUORITUS_JSON_VIRHE}
import fi.oph.suorituspalvelu.util.{LogContext, SecurityUtil}
import fi.oph.suorituspalvelu.validation.SuoritusValidator
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation}
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RequestMapping, RestController}
import slick.jdbc.JdbcBackend

import scala.jdk.CollectionConverters.SeqHasAsJava

@RequestMapping(path = Array(""))
@RestController
@Tag(
  name= "Suoritukset",
  description = "Suoritustiedot")
class Suoritusresource {

  val LOG = LoggerFactory.getLogger(classOf[Suoritusresource])

  @Autowired var mapper: ObjectMapper = null
  
  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  @PostMapping(
    path = Array(SUORITUS_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Luo oppijalle uuden suorituksen",
    description = "Huomioita:\n" +
      "- Huomio 1",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = Array(new Content(schema = new Schema(implementation = classOf[Suoritus])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Suoritus vastaanotettu, palauttaa tunnisteen", content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = "Pyyntö on virheellinen", content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusFailureResponse])))),
      new ApiResponse(responseCode = "403", description = SUORITUS_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def lisaaSuoritus(@RequestBody suoritusBytes: Array[Byte], request: HttpServletRequest): ResponseEntity[LuoSuoritusResponse] =
    LogContext(path = SUORITUS_PATH, identiteetti = SecurityUtil.getIdentiteetti())(() =>
      try
        Right(None)
          .flatMap(_ =>
            if (!SecurityUtil.onOikeus())
              LOG.warn("Ei oikeutta tallentaa suoritusta")
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
            else
              Right(None))
          .flatMap(_ =>
            // deserialisoidaan
            try
              Right(mapper.readValue(suoritusBytes, classOf[Suoritus]))
            catch
              case e: Exception =>
                LOG.warn("suorituksen deserialisointi epäonnistui")
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoSuoritusFailureResponse(java.util.List.of(VIRHEELLINEN_SUORITUS_JSON_VIRHE)))))
          .flatMap(suoritus =>
            val validointiVirheet = SuoritusValidator.validateSuoritus(suoritus)
            if (!validointiVirheet.isEmpty)
              LOG.warn("Suorituksessa on validointivirheitä: " + validointiVirheet.mkString(","))
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoSuoritusFailureResponse(validointiVirheet.toSeq.asJava)))
            else
              Right(suoritus))
          .map(suoritus =>
            val kantaOperaatiot = KantaOperaatiot(database)
            val versio = kantaOperaatiot.tallennaJarjestelmaVersio(suoritus.oppijaNumero.get, VIRKAILIJA, "{}").get
            kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set.empty, Set(PerusopetuksenOppimaara("3.4.5", Koodi("arvo", "koodisto", 1), None, Set(PerusopetuksenOppiaine(suoritus.suoritus.get, "koodi", "10")))))
            LogContext(oppijaNumero = suoritus.oppijaNumero.get())(() =>
              LOG.info("Tallennettu suoritus")
              val user = AuditLog.getUser(request)
              AuditLog.logCreate(user, Map("oppijaNumero" -> suoritus.oppijaNumero.get()), AuditOperation.LuoSuoritus, suoritus)
              ResponseEntity.status(HttpStatus.OK).body(LuoSuoritusSuccessResponse(""))))
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuoSuoritusResponse]]
      catch
        case e: Exception =>
          LOG.error("Suorituksen lisääminen epäonnistui", e)
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoSuoritusFailureResponse(Seq(SUORITUKSEN_LUONTI_EPAONNISTUI).asJava)))
}
