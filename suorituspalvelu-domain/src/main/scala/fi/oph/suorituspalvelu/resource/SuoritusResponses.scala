package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.{ESIMERKKI_SUORITUSTUNNISTE, EXAMPLE_OPPIJANUMERO_VALIDOINTIVIRHE}
import io.swagger.v3.oas.annotations.media.Schema

import java.util.UUID
import scala.annotation.meta.field
import scala.beans.BeanProperty

class LuoSuoritusResponse {}

@Schema(name = "LuoSuoritusSuccessResponse")
case class LuoSuoritusSuccessResponse(
                                          @(Schema@field)(example = ESIMERKKI_SUORITUSTUNNISTE)
                                          @BeanProperty tunniste: String) extends LuoSuoritusResponse {

  /**
   * Tyhjä konstruktori Jacksonia varten
   */
  def this() = {
    this(null)
  }
}

@Schema(name = "LuoSuoritusFailureResponse")
case class LuoSuoritusFailureResponse(
                                          @(Schema @field)(example = EXAMPLE_OPPIJANUMERO_VALIDOINTIVIRHE)
                                          @BeanProperty validointiVirheet: java.util.List[String]) extends LuoSuoritusResponse {

  def this() =
    this(null)
}

