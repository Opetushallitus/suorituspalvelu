package fi.oph.suorituspalvelu.resource.api

import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_ESIMERKKI_JOB_ID, ESIMERKKI_AIKALEIMA, ESIMERKKI_HAKU_OID, ESIMERKKI_LUOKKA, ESIMERKKI_OPPIJANUMERO, ESIMERKKI_TULOSTIEDOSTO, KOSKI_DATASYNC_ESIMERKKI_VIRHE, LAHETTAVAT_ESIMERKKI_VIRHE, VIRTA_DATASYNC_ESIMERKKI_VIRHE}
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode

import java.util.{Optional, UUID}
import scala.annotation.meta.field
import scala.beans.BeanProperty

class SyncResponse() {
}

@Schema(name = "VirtaSyncSuccessResponse")
case class VirtaSyncSuccessResponse(
  @(Schema @field)(example = DATASYNC_ESIMERKKI_JOB_ID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty jobId: UUID) extends SyncResponse {}

@Schema(name = "VirtaSyncFailureResponse")
case class VirtaSyncFailureResponse(
  @(Schema @field)(example = "[\"" + VIRTA_DATASYNC_ESIMERKKI_VIRHE + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheet: java.util.List[String]) extends SyncResponse {}

@Schema(name = "KoskiSyncSuccessResponse")
case class KoskiSyncSuccessResponse(
  @(Schema @field)(example = "5", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty onnistuneet: Int,
  @(Schema @field)(example = "0", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheet: Int) extends SyncResponse {}

@Schema(name = "KoskiSyncFailureResponse")
case class KoskiSyncFailureResponse(
  @(Schema @field)(example = "[\"" + KOSKI_DATASYNC_ESIMERKKI_VIRHE + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheet: java.util.List[String]) extends SyncResponse {}

@Schema(name = "YtrSyncSuccessResponse")
case class YtrSyncSuccessResponse(
  @(Schema @field)(example = "5", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty onnistuneet: Int,
  @(Schema @field)(example = "0", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheet: Int) extends SyncResponse {}

@Schema(name = "YtrSyncFailureResponse")
case class YtrSyncFailureResponse(
  @(Schema @field)(example = "[\"" + VIRTA_DATASYNC_ESIMERKKI_VIRHE + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheet: java.util.List[String]) extends SyncResponse {}

case class YTRPaivitaTiedotHaullePayload(
  @(Schema @field)(example = ESIMERKKI_HAKU_OID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakuOid: Optional[String]) {

  def this() = this(Optional.empty())
}

case class YTRPaivitaTiedotHenkilollePayload(
  @(Schema @field)(example = "[\"" + ESIMERKKI_OPPIJANUMERO + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty henkiloOids: Optional[java.util.List[String]]) {

  def this() = this(Optional.empty())
}

case class VirtaPaivitaTiedotHaullePayload(
  @(Schema @field)(example = ESIMERKKI_HAKU_OID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakuOid: Optional[String]) {

  def this() = this(Optional.empty())
}

case class VirtaPaivitaTiedotHenkilollePayload(
  @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty henkiloOid: Optional[String]) {

  def this() = this(Optional.empty())
}

case class KoskiPaivitaTiedotHaullePayload(
  @(Schema @field)(example = ESIMERKKI_HAKU_OID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakuOid: Optional[String]) {

  def this() = this(Optional.empty())
}

case class KoskiPaivitaTiedotHenkiloillePayload(
  @(Schema @field)(example = "[\"" + ESIMERKKI_OPPIJANUMERO + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty henkiloOidit: Optional[java.util.List[String]]) {

  def this() = this(Optional.empty())
}

case class KoskiHaeMuuttuneetJalkeenPayload(
  @(Schema @field)(description = "Haetaan tiedot jotka ovat muuttuneet tämän ajankohdan jälkeen",
    example = ESIMERKKI_AIKALEIMA, requiredMode = RequiredMode.REQUIRED)
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
  @(Schema @field)(example = "[\"" + ESIMERKKI_LUOKKA + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luokat: java.util.List[String]
) extends LahettavatLuokatResponse

case class LahettavatLuokatFailureResponse(
  @(Schema @field)(example = "[\"" + LAHETTAVAT_ESIMERKKI_VIRHE + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheet: java.util.Set[String]
) extends LahettavatLuokatResponse

case class LahettavatHenkilo(
  @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty henkiloOid: String,
  @(Schema @field)(example = "[\"" + ESIMERKKI_LUOKKA + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luokat: java.util.List[String],
)

trait LahettavatHenkilotResponse()

case class LahettavatHenkilotSuccessResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty henkilot: java.util.List[LahettavatHenkilo]
) extends LahettavatHenkilotResponse

case class LahettavatHenkilotFailureResponse(
  @(Schema @field)(example = "[\"" + LAHETTAVAT_ESIMERKKI_VIRHE + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheet: java.util.Set[String]
) extends LahettavatHenkilotResponse
