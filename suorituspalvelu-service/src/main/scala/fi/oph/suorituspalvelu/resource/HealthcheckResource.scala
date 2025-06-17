package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.HEALTHCHECK_PATH
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.*

@RequestMapping(path = Array(HEALTHCHECK_PATH))
@RestController
@Tag("Healthcheck")
class HealthcheckResource {

  val LOG = LoggerFactory.getLogger(classOf[HealthcheckResource]);

  @GetMapping(path = Array(""))
  def healthcheck(): ResponseEntity[String] =
    LOG.info("healthcheck")
    ResponseEntity.status(HttpStatus.OK).body("OK")
}