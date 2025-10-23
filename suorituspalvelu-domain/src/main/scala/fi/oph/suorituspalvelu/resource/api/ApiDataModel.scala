package fi.oph.suorituspalvelu.resource.api

import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_ESIMERKKI_JOB_ID, ESIMERKKI_AIKALEIMA, ESIMERKKI_LUOKKA, ESIMERKKI_OPPIJANUMERO, ESIMERKKI_TULOSTIEDOSTO, LAHETTAVAT_ESIMERKKI_VIRHE, VIRTA_DATASYNC_ESIMERKKI_VIRHE}
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode

import java.util.{Optional, UUID}
import scala.annotation.meta.field
import scala.beans.BeanProperty

class SyncResponse() {
}

@Schema(name = "VirtaSyncSuccessResponse")
case class VirtaSyncSuccessResponse(
  @(Schema @field)(example = DATASYNC_ESIMERKKI_JOB_ID)
  @BeanProperty jobId: UUID) extends SyncResponse {}

@Schema(name = "VirtaSyncFailureResponse")
case class VirtaSyncFailureResponse(
  @(Schema @field)(example = VIRTA_DATASYNC_ESIMERKKI_VIRHE)
  @BeanProperty virheet: java.util.List[String]) extends SyncResponse {}

@Schema(name = "KoskiSyncSuccessResponse")
case class KoskiSyncSuccessResponse(
  @(Schema @field)(example = "")
  @BeanProperty responseStr: String) extends SyncResponse {}

@Schema(name = "KoskiSyncFailureResponse")
case class KoskiSyncFailureResponse(
  @(Schema @field)(example = VIRTA_DATASYNC_ESIMERKKI_VIRHE)
  @BeanProperty virheet: java.util.List[String]) extends SyncResponse {}

@Schema(name = "YtrSyncSuccessResponse")
case class YtrSyncSuccessResponse(
  @(Schema @field)(example = "")
  @BeanProperty responseStr: String) extends SyncResponse {}

@Schema(name = "YtrSyncFailureResponse")
case class YtrSyncFailureResponse(
  @(Schema @field)(example = VIRTA_DATASYNC_ESIMERKKI_VIRHE)
  @BeanProperty virheet: java.util.List[String]) extends SyncResponse {}

case class KoskiHaeMuuttuneetJalkeenPayload(
  @(Schema @field)(example = ESIMERKKI_AIKALEIMA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty aikaleima: Optional[String]) {

  def this() = this(Optional.empty())
}

case class KoskiRetryPayload(
  @(Schema @field)(example = "[\"" + ESIMERKKI_TULOSTIEDOSTO + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tiedostot: Optional[java.util.List[String]]) {

  def this() = this(Optional.empty())
}

trait LahettavatLuokatResponse()

case class LahettavatLuokatSuccessResponse(
  @(Schema @field)(example = "9A", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luokat: java.util.List[String]
) extends LahettavatLuokatResponse

case class LahettavatLuokatFailureResponse(
  @(Schema @field)(example = LAHETTAVAT_ESIMERKKI_VIRHE)
  @BeanProperty virheet: java.util.Set[String]
) extends LahettavatLuokatResponse

case class LahettavatHenkilo(
  @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty henkiloOid: String,
  @(Schema @field)(example = ESIMERKKI_LUOKKA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luokat: java.util.List[String],
)

trait LahettavatHenkilotResponse()

case class LahettavatHenkilotSuccessResponse(
  @(Schema @field)(example = "9A", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty henkilot: java.util.List[LahettavatHenkilo]
) extends LahettavatHenkilotResponse

case class LahettavatHenkilotFailureResponse(
  @(Schema @field)(example = LAHETTAVAT_ESIMERKKI_VIRHE)
  @BeanProperty virheet: java.util.Set[String]
) extends LahettavatHenkilotResponse
