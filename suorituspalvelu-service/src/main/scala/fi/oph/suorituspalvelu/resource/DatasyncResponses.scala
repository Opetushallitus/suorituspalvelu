package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_ESIMERKKI_JOB_ID, DATASYNC_VIRTA_ESIMERKKI_VIRHE}
import io.swagger.v3.oas.annotations.media.Schema

import java.util
import java.util.UUID
import scala.annotation.meta.field
import scala.beans.BeanProperty

object SyncResponse {

}

class SyncResponse() {
}

@Schema(name = "VirtaSyncSuccessResponse")
case class VirtaSyncSuccessResponse(
                                     @(Schema@field)(example = DATASYNC_ESIMERKKI_JOB_ID)
                                     @BeanProperty jobId: UUID) extends SyncResponse {}

@Schema(name = "VirtaSyncFailureResponse")
case class VirtaSyncFailureResponse(
                                     @(Schema@field)(example = DATASYNC_VIRTA_ESIMERKKI_VIRHE)
                                     @BeanProperty virheet: java.util.List[String]) extends SyncResponse {}

@Schema(name = "KoskiSyncSuccessResponse")
case class KoskiSyncSuccessResponse(
                                     @(Schema @field)(example = "")
                                     @BeanProperty responseStr: String) extends SyncResponse {}

@Schema(name = "KoskiSyncFailureResponse")
case class KoskiSyncFailureResponse(
                                     @(Schema@field)(example = DATASYNC_VIRTA_ESIMERKKI_VIRHE)
                                     @BeanProperty virheet: java.util.List[String]) extends SyncResponse {}

@Schema(name = "YtrSyncSuccessResponse")
case class YtrSyncSuccessResponse(
                                     @(Schema @field)(example = "")
                                     @BeanProperty responseStr: String) extends SyncResponse {}

@Schema(name = "YtrSyncFailureResponse")
case class YtrSyncFailureResponse(
                                     @(Schema@field)(example = DATASYNC_VIRTA_ESIMERKKI_VIRHE)
                                     @BeanProperty virheet: java.util.List[String]) extends SyncResponse {}
