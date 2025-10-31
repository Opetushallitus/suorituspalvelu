package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.HEALTHCHECK_PATH
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.*

@RequestMapping(path = Array(HEALTHCHECK_PATH))
@RestController
@Tag(
  name = "Healthcheck",
  description = "Rajapinta sovelluksen tilan monitorointiin.")
class HealthcheckResource {

  val LOG = LoggerFactory.getLogger(classOf[HealthcheckResource]);

  @GetMapping(path = Array(""))
  @Operation(
    summary = "Healthcheck-endpoint",
    description = "Palauttaa OK jos sovellus on toiminnassa.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa OK jos sovellus on toiminnassa.",
        content = Array(new Content(schema = new Schema(implementation = classOf[String])))),
    ))
  def healthcheck(): ResponseEntity[String] =
    LOG.info("healthcheck")
    ResponseEntity.status(HttpStatus.OK).body("OK")
}