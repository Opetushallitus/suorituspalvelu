package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_ESIMERKKI_JOB_ID, DATASYNC_VIRTA_ESIMERKKI_VIRHE}
import io.swagger.v3.oas.annotations.media.Schema

import java.util
import java.util.UUID
import scala.annotation.meta.field
import scala.beans.BeanProperty

object VirtaSyncResponse {

}

class VirtaSyncResponse() {
}

@Schema(name = "VirtaSyncSuccessResponse")
case class VirtaSyncSuccessResponse(
                                     @(Schema@field)(example = DATASYNC_ESIMERKKI_JOB_ID)
                                     @BeanProperty jobId: UUID) extends VirtaSyncResponse {}

@Schema(name = "VirtaSyncFailureResponse")
case class VirtaSyncFailureResponse(
                                     @(Schema@field)(example = DATASYNC_VIRTA_ESIMERKKI_VIRHE)
                                     @BeanProperty virheet: Seq[String]) extends VirtaSyncResponse {}
