package fi.oph.suorituspalvelu.resource.api

import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_ESIMERKKI_JOB_ID, DATASYNC_JOBIEN_TIETOJEN_HAKU_EPAONNISTUI, ESIMERKKI_AIKALEIMA, ESIMERKKI_HAKEMUS_OID, ESIMERKKI_HAKUKOHDE_OID, ESIMERKKI_HAKU_OID, ESIMERKKI_JOB_NIMI, ESIMERKKI_LUOKKA, ESIMERKKI_OPPIJANUMERO, ESIMERKKI_TULOSTIEDOSTO, KOSKI_DATASYNC_ESIMERKKI_VIRHE, LAHETTAVAT_ESIMERKKI_VIRHE, VIRTA_DATASYNC_ESIMERKKI_VIRHE}
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode

import java.time.Instant
import java.util.{Optional, UUID}
import scala.annotation.meta.field
import scala.beans.BeanProperty

@Schema(name = "SyncJob")
case class SyncJob(
  @(Schema @field)(example = DATASYNC_ESIMERKKI_JOB_ID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty id: UUID,
  @(Schema @field)(example = ESIMERKKI_JOB_NIMI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty name: String,
  @(Schema @field)(example = "55", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty completion: Int,
  @(Schema @field)(example = "2025-11-18T11:16:01.327249Z", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty lastUpdated: Instant
) extends SyncResponse {}

class SyncResponse() {
}

@Schema(name = "SyncJobStatusResponse")
case class SyncJobStatusResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty jobs: java.util.List[SyncJob]) extends SyncResponse {}

@Schema(name = "SyncJobFailureResponse")
case class SyncJobFailureResponse(
  @(Schema @field)(example = "[\"" + DATASYNC_JOBIEN_TIETOJEN_HAKU_EPAONNISTUI + "\"]", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheet: java.util.List[String]) extends SyncResponse {}

@Schema(name = "SyncSuccessJobResponse")
case class SyncSuccessJobResponse(
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
  @(Schema @field)(example = ESIMERKKI_LUOKKA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luokka: String,
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

case class ValintalaskentaDataPayload(
  @(Schema @field)(example = ESIMERKKI_HAKU_OID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakuOid: Optional[String],
  @(Schema @field)(example = ESIMERKKI_HAKUKOHDE_OID, requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty hakukohdeOid: Optional[String],
  @(Schema @field)(example = "[]", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty hakemusOids: java.util.List[String]
)

trait ValintalaskentaDataResponse

case class ValintalaskentaDataSuccessResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty valintaHakemukset: java.util.List[ValintalaskentaApiHakemus]
) extends ValintalaskentaDataResponse

case class ValintalaskentaDataFailureResponse(
  @(Schema @field)(example = LAHETTAVAT_ESIMERKKI_VIRHE)
  @BeanProperty virheet: java.util.List[String]
) extends ValintalaskentaDataResponse

case class ValintalaskentaApiAvainArvo(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty avain: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: String
)

case class ValintalaskentaApiAvainMetatiedotDTO()

case class ValintalaskentaApiHakutoive(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakuoid: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String, //hakukohdeOid
  //Tämä tieto löytyy Valintalaskennan HakukohdeDTO-luokasta, mutta Koostepalvelu ei ole sitä asettanut.
  //Luultavasti ei tarpeellinen, mutta varmuudeksi huomina tässä.
  //@(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  //@BeanProperty tarjoajaoid: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty prioriteetti: Int,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakukohdeRyhmatOids: java.util.List[String],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty harkinnanvaraisuus: Boolean = false //Onkohan tämä tarpeellinen? Tarkistetaan, kun muuten laitetaan harkinnanvaraisuusasiat kuntoon.
)

case class ValintalaskentaApiHakemus(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakuoid: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakemusoid: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakukohteet: java.util.List[ValintalaskentaApiHakutoive],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakijaOid: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty etunimi: String = "", //Todo, voiko pudottaa pois?
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty sukunimi: String = "", //Todo, voiko pudottaa pois?
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty koskiOpiskeluoikeudetJson: String, //Tässä vaiheessa lähinnä placeholder. Ensivaiheessa haetaan erikseen Koostepalvelussa/Valintalaskennassa. Tulevaisuudessa kuitenkin voidaan toimittaa suoraan Supasta.
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty avaimet: java.util.List[ValintalaskentaApiAvainArvo],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty avainMetatiedotDTO: java.util.List[ValintalaskentaApiAvainMetatiedotDTO] //Lisätään nämä myöhemmässä vaiheessa, tai yhdistetään avain-arvoihin (vaatii muutoksia valintaperusteisiin jos yhdistetään)
)
