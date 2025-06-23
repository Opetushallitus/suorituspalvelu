package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_RESPONSE_400_DESCRIPTION, DATASYNC_RESPONSE_403_DESCRIPTION, EXAMPLE_OPPIJANUMERO, UI_SUORITUKSET_400_DESCRIPTION, UI_SUORITUKSET_403_DESCRIPTION, UI_SUORITUKSET_OPPIJANUMERO_PARAM_NAME, UI_SUORITUKSET_PATH}
import fi.oph.suorituspalvelu.resource.ui.{OppijanTiedot, OppijatFailureResponse}
import fi.oph.suorituspalvelu.service.UIService
import fi.oph.suorituspalvelu.validation.Validator
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.*

import java.time.LocalDate
import java.util.Optional
import scala.annotation.meta.field
import scala.beans.BeanProperty

@RequestMapping(path = Array(UI_SUORITUKSET_PATH))
@RestController
@Tag("UI")
class UIResource {

  val LOG = LoggerFactory.getLogger(classOf[UIResource]);

  @Autowired val uiService: UIService = null

  @GetMapping(
    path = Array(""),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa yksittäisen oppijan tiedot käyttöliittymälle",
    description = "Huomioita:\n" +
      "- Huomio 1",
    parameters = Array(new Parameter(name = UI_SUORITUKSET_OPPIJANUMERO_PARAM_NAME, in = ParameterIn.PATH)),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää yksittäisen oppijan tiedot", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanTiedot])))),
      new ApiResponse(responseCode = "400", description = UI_SUORITUKSET_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijatFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_SUORITUKSET_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
  ))
  def haeSuoritukset(@PathVariable(UI_SUORITUKSET_OPPIJANUMERO_PARAM_NAME)  @Parameter(description = "oppijan oid", example = EXAMPLE_OPPIJANUMERO, required = true) oppijaNumero: String): ResponseEntity[_] = {
    val oppijanTiedot = uiService.getOppijanTiedot(oppijaNumero)
    if(oppijanTiedot.isEmpty)
      ResponseEntity.status(HttpStatus.NOT_FOUND).body("")
    else
      ResponseEntity.status(HttpStatus.OK).body(oppijanTiedot)
  }
}