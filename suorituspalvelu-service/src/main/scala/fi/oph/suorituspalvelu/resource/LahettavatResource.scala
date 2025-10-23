package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.*
import fi.oph.suorituspalvelu.resource.api.{LahettavatLuokatFailureResponse, LahettavatLuokatResponse, LahettavatLuokatSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.ui.UIService
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.*

import java.util.Optional
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@RequestMapping(path = Array(""))
@RestController
@Tag(
  name = "Organisaation rajaimet",
  description = "Rajapinnat joiden avulla voidaan rajata 2. asteen hakijoihin kohdistuvia hakuja")
class LahettavatResource {

  val LOG = LoggerFactory.getLogger(classOf[LahettavatResource])

  @Autowired var uiService: UIService = null

  @GetMapping(
    path = Array(LAHETTAVAT_LUOKAT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee listan oppilaitoksen luokista joilla on mahdollisia 2. asteen hakijoita",
    description = "Lista sisältää haetun oppilaitoksen luokat joilla on mahdollisia 2. asteen hakijoita",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa listan oppilaitoksen luokista", content = Array(new Content(schema = new Schema(implementation = classOf[LahettavatLuokatSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = LAHETTAVAT_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LahettavatLuokatFailureResponse])))),
      new ApiResponse(responseCode = "403", description = LAHETTAVAT_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeLuokat(@PathVariable(name = LAHETTAVAT_OPPILAITOSOID_PARAM_NAME, required = false) @Parameter(description = "oppilaitoksen oid", example = ESIMERKKI_OPPILAITOS_OID) oppilaitosOid: Optional[String],
                @PathVariable(name = LAHETTAVAT_VUOSI_PARAM_NAME, required = false) @Parameter(description = "vuosi", example = ESIMERKKI_VUOSI) vuosi: Optional[String],
                request: HttpServletRequest): ResponseEntity[LahettavatLuokatResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = LAHETTAVAT_LUOKAT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onPalveluKayttaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(LahettavatLuokatFailureResponse(java.util.Set.of(LAHETTAVAT_EI_OIKEUKSIA)))))
        .flatMap(_ =>
          // validoidaan parametri
          val virheet: Set[String] = Set(
            Validator.validateOppilaitosOid(oppilaitosOid.toScala, true),
            Validator.validateVuosi(vuosi.toScala, true)
          ).flatten
          if (virheet.isEmpty)
            Right((oppilaitosOid.get, vuosi.get.toInt))
          else
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LahettavatLuokatFailureResponse(virheet.asJava))))
        .map((oppilaitosOid, vuosi) => {
          try
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(LAHETTAVAT_OPPILAITOSOID_PARAM_NAME -> oppilaitosOid, LAHETTAVAT_VUOSI_PARAM_NAME -> vuosi.toString), AuditOperation.HaeLuokatLahettava, None)
            LOG.info(s"Haetaan 2. asteen mahdollisten hakijoiden luokat oppilaitokselle $oppilaitosOid")
            val luokat = uiService.haeLuokat(oppilaitosOid, vuosi)
            ResponseEntity.status(HttpStatus.OK).body(LahettavatLuokatSuccessResponse(luokat.toList.asJava))
          catch
            case e: Exception =>
              LOG.error(s"Luokkien haku oppilaitokselle $oppilaitosOid epäonnistui", e)
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LahettavatLuokatFailureResponse(Set(LAHETTAVAT_500_VIRHE).asJava))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[LahettavatLuokatResponse]])
  }
}

