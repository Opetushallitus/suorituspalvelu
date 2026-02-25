package fi.oph.suorituspalvelu.resource

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.resource.ApiConstants.*
import fi.oph.suorituspalvelu.resource.api.{AutomaattinenHakukelpoisuusFailureResponse, AutomaattinenHakukelpoisuusResponse, AutomaattinenHakukelpoisuusSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.HakukelpoisuusService
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
  name = "Automaattisen hakukelpoisuuden rajapinnat",
  description = "Rajapinnat jotka palauttavat automaattiseen hakukelpoisuuteen liittyviä tietoja")
class AutomaattinenHakukelpoisuusResource {

  val LOG = LoggerFactory.getLogger(classOf[AutomaattinenHakukelpoisuusResource])

  val objectMapper: ObjectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  objectMapper.registerModule(Jdk8Module())
  objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
  objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
  objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)

  @Autowired val hakukelpoisuusService: HakukelpoisuusService = null

  @GetMapping(
    path = Array(VALINNAT_HAKUKELPOISUUS_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa tiedot hakijoiden automaattisesta hakukelpoisuudesta.",
    description = "Henkilö on automaattisesti hakukelpoinen, jos tällä on valmis yo-, kvyo- tai ammatillinen (perus, amm, erikois) tutkinto.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa tiedon henkilön automaattisesta hakukelpoisuudesta.", content = Array(new Content(schema = new Schema(implementation = classOf[AutomaattinenHakukelpoisuusSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = AUTOM_HAKUKELPOISUUS_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[AutomaattinenHakukelpoisuusFailureResponse])))),
      new ApiResponse(responseCode = "403", description = AUTOM_HAKUKELPOISUUS_RESPONSE_403_DESCRIPTION)
    ))
  def haeHenkiloidenAutomaattinenHakukelpoisuus(@PathVariable(HAKUKELPOISUUS_HENKILO_PARAM_NAME)  @Parameter(description = "henkilön tunniste", example = ESIMERKKI_OPPIJANUMERO, required = true) henkiloOid: Optional[String], request: HttpServletRequest): ResponseEntity[AutomaattinenHakukelpoisuusResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = VALINNAT_HAKUKELPOISUUS_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(AutomaattinenHakukelpoisuusFailureResponse(java.util.List.of(AUTOM_HAKUKELPOISUUS_EI_OIKEUKSIA)))))
        .flatMap(payload =>
          // validoidaan parametrit
          val virheet = Validator.validateHenkiloOid(henkiloOid.toScala, true)
          if (virheet.nonEmpty) {
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AutomaattinenHakukelpoisuusFailureResponse(new java.util.ArrayList(virheet.asJava))))
          } else {
            Right(henkiloOid.get())
          })
        .map(henkiloOid => {
          try {
            val user = AuditLog.getUser(request)
            AuditLog.log(
              user,
              Map("henkiloOid" -> henkiloOid),
              AuditOperation.HaeHenkiloidenAutomaattisetHakukelpoisuudet,
              None
            )
            LOG.info(s"Haetaan automaattiset hakukelpoisuudet henkilölle $henkiloOid")
            val result = hakukelpoisuusService.paatteleAutomaattinenHakukelpoisuus(henkiloOid)

            ResponseEntity.status(HttpStatus.OK).body(AutomaattinenHakukelpoisuusSuccessResponse(henkiloOid, result))
          } catch {
            case e: Exception =>
              LOG.error(s"Automaattisten hakukelpoisuuksien hakeminen henkilölle $henkiloOid epäonnistui: ", e)
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AutomaattinenHakukelpoisuusFailureResponse(Seq(AUTOM_HAKUKELPOISUUS_500_VIRHE).asJava))
          }
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[AutomaattinenHakukelpoisuusResponse]])
  }
}

