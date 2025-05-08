package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.util.{LogContext, SecurityUtil}
import io.swagger.v3.oas.annotations.media.Content as content
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.beans.factory.annotation.Autowired
import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_PATH}
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RequestMapping, RestController}
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}

@RequestMapping(path = Array("dataSync"))
@RestController
@Tag(
  name = "Data sync",
  description = "Manuaalinen datan haku lähdejärjestelmistä")
class DataSyncResource {

  val LOG = LoggerFactory.getLogger(classOf[DataSyncResource])

  @Autowired var mapper: ObjectMapper = null

  @Autowired var koskiIntegration: KoskiIntegration = null

  @PostMapping(
    path = Array(""),
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
  def haeKoskiTiedot(@RequestBody personOids: Array[String], request: HttpServletRequest): ResponseEntity[String] = {
    LogContext(path = DATASYNC_PATH, identiteetti = SecurityUtil.getIdentiteetti())(() =>
      if (SecurityUtil.onRekisterinpitaja()) {
        LOG.info(s"Haetaan Koski-tiedot henkilöille ${personOids.mkString("Array(", ", ", ")")}")
        val result = koskiIntegration.syncKoski(personOids.toSet)
        LOG.info(s"Palautetaan rajapintavastaus, $result")
        ResponseEntity.status(HttpStatus.OK).body(result.toString())//Todo, tässä nyt palautellaan vain jotain mitä sattui jäämään käteen. Mitä tietoja oikeasti halutaan palauttaa?
      } else {
        ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ei oikeuksia")
      }
    )
  }
}

