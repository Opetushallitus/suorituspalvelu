package fi.oph.suorituspalvelu.resource.ui

import fi.oph.suorituspalvelu.resource.ApiConstants.{ESIMERKKI_AIKALEIMA, ESIMERKKI_ETUNIMET, ESIMERKKI_HAKU_OID, ESIMERKKI_HETU, ESIMERKKI_LUOKKA, ESIMERKKI_OPPIAINEKOODI, ESIMERKKI_OPPIJANUMERO, ESIMERKKI_OPPILAITOS_NIMI, ESIMERKKI_OPPILAITOS_OID, ESIMERKKI_PERUSOPETUKSEN_OPPIAINEEN_ARVOSANA, ESIMERKKI_SUKUNIMI, ESIMERKKI_SUORITUSKIELI, ESIMERKKI_SYNTYMAIKA, ESIMERKKI_TILA, ESIMERKKI_VALMISTUMISPAIVA, ESIMERKKI_VIERAS_KIELI_KIELIKOODI, ESIMERKKI_YKSILOLLISTAMINEN, ESIMERKKI_YLIAJO_ARVO, ESIMERKKI_YLIAJO_AVAIN, ESIMERKKI_YLIAJO_SELITE, ESIMERKKI_YLIAJO_VIRKAILIJA}
import fi.oph.suorituspalvelu.resource.*
import fi.oph.suorituspalvelu.resource.ui.UIVirheet.{UI_HAKU_JOKO_HAKUSANA_TAI_OPPILAITOS, UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT, UI_LUO_SUORITUS_OPPIAINE_TALLENNUS_EPAONNISTUI, UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA, UI_LUO_SUORITUS_VAIHTOEHDOT_HAKU_EPAONNISTUI, UI_OPPIJAN_HAUT_HAKU_EPAONNISTUI, UI_POISTA_SUORITUS_SUORITUSTA_EI_LOYTYNYT, UI_POISTA_YLIAJO_EPAONNISTUI, UI_RAJAIMEN_TIEDOT_HAKU_EPAONNISTUI, UI_TALLENNA_YLIAJO_OPPIJALLE_TUNTEMATON_OPPIJA, UI_TIEDOT_HAKU_EPAONNISTUI}
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode

import java.time.{Instant, LocalDate}
import java.util.{Optional, UUID}
import scala.annotation.meta.field
import scala.beans.BeanProperty

trait KayttajaResponse()

case class KayttajaSuccessResponse(
  @(Schema @field)(example = "fi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty asiointiKieli: String,
  @(Schema @field)(example = "false", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty isRekisterinpitaja: Boolean,
  @(Schema @field)(example = "false", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty isOrganisaationKatselija: Boolean
) extends KayttajaResponse

case class KayttajaFailureResponse(
  @(Schema @field)(example = UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT)
  @BeanProperty virheet: java.util.Set[String]
) extends KayttajaResponse

case class OppilaitosNimi(
  @(Schema @field)(example = ESIMERKKI_OPPILAITOS_NIMI, requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = ESIMERKKI_OPPILAITOS_NIMI, requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = ESIMERKKI_OPPILAITOS_NIMI, requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class Oppilaitos(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: OppilaitosNimi,
  @(Schema @field)(example = ESIMERKKI_OPPILAITOS_OID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

trait OppilaitosResponse()

case class OppilaitosSuccessResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitokset: java.util.List[Oppilaitos]
) extends OppilaitosResponse

case class OppilaitosFailureResponse(
  @(Schema @field)(example = UI_RAJAIMEN_TIEDOT_HAKU_EPAONNISTUI)
  @BeanProperty virheet: java.util.Set[String]
) extends OppilaitosResponse

trait VuodetResponse()

case class VuodetSuccessResponse(
  @(Schema @field)(example = "2025", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty vuodet: java.util.List[String]
) extends VuodetResponse

case class VuodetFailureResponse(
  @(Schema @field)(example = UI_RAJAIMEN_TIEDOT_HAKU_EPAONNISTUI)
  @BeanProperty virheet: java.util.Set[String]
) extends VuodetResponse

trait LuokatResponse()

case class LuokatSuccessResponse(
  @(Schema @field)(example = "9A", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luokat: java.util.List[String]
) extends LuokatResponse

case class LuokatFailureResponse(
  @(Schema @field)(example = UI_RAJAIMEN_TIEDOT_HAKU_EPAONNISTUI)
  @BeanProperty virheet: java.util.Set[String]
) extends LuokatResponse

trait OppijanHakuResponse()

case class Oppija(
  @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppijaNumero: String,
  @(Schema @field)(example = ESIMERKKI_HETU)
  @BeanProperty hetu: Optional[String],
  @(Schema @field)(example = ESIMERKKI_ETUNIMET, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty etunimet: Optional[String],
  @(Schema @field)(example = ESIMERKKI_SUKUNIMI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty sukunimi: Optional[String]
)

case class OppijanHakuSuccessResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppijat: java.util.List[Oppija]
) extends OppijanHakuResponse

case class OppijanHakuFailureResponse(
  @(Schema @field)(example = UI_HAKU_JOKO_HAKUSANA_TAI_OPPILAITOS)
  @BeanProperty virheet: java.util.Set[String]
) extends OppijanHakuResponse

trait OppijanHautResponse()

case class HakuNimi(
  @(Schema @field)(example = "Esimerkki haku", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Exempel ansökning", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Example application", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String]
)

case class Haku(
  @(Schema @field)(example = ESIMERKKI_HAKU_OID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakuOid: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: HakuNimi
)

case class OppijanHautSuccessResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty haut: java.util.List[Haku]
) extends OppijanHautResponse

case class OppijanHautFailureResponse(
  @(Schema @field)(example = UI_OPPIJAN_HAUT_HAKU_EPAONNISTUI)
  @BeanProperty virheet: java.util.Set[String]
) extends OppijanHautResponse

enum OpiskeluoikeusTila:
  case VOIMASSA
  case EI_VOIMASSA
  case PAATTYNYT

enum SuoritusTila:
  case VALMIS
  case KESKEN
  case KESKEYTYNYT

enum SuoritusTapa:
  case NAYTTOTUTKINTO

case class OOOppilaitosNimi(
  @(Schema @field)(example = "Tampereen yliopisto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Tampereen yliopisto sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Tampereen yliopisto en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String]
)

case class OOOppilaitos(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: OOOppilaitosNimi,
  @(Schema @field)(example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class UIOpiskeluoikeusNimi(
  @(Schema @field)(example = "Kasvatustieteen maisteri", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Kasvatustieteen maisteri sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Kasvatustieteen maisteri en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String]
)

case class UIOpiskeluoikeusVirtaTila(
  @(Schema @field)(example = "optio", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "option", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "option", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String]
)

case class UIOpiskeluoikeus(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: UIOpiskeluoikeusNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: OOOppilaitos,
  @(Schema @field)(example = "2020-01-01", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty voimassaolonAlku: LocalDate,
  @(Schema @field)(example = "2024-12-31", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty voimassaolonLoppu: LocalDate,
  @(Schema @field)(example = "VOIMASSA", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty supaTila: OpiskeluoikeusTila,
  @(Schema @field)(example = "optio", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virtaTila: UIOpiskeluoikeusVirtaTila
)

case class KKOppilaitosNimi(
  @(Schema @field)(example = "Tampereen yliopisto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Tampereen yliopisto sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Tampereen yliopisto en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class KKOppilaitos(
  @(Schema @field)(description = "Oppilaitoksen nimi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: KKOppilaitosNimi,
  @(Schema @field)(description = "Oppilaitoksen tunniste", example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class KKSuoritusNimi(
  @(Schema @field)(example = "Kasvatustieteen maisteri", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Kasvatustieteen maisteri sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Kasvatustieteen maisteri en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class KKSuoritus(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(description = "Tutkinnon nimi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: KKSuoritusNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: KKOppilaitos,
  @(Schema @field)(description = "Tutkinnon tila", example = "KESKEN", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
)

case class YOKoeNimi(
  @(Schema @field)(example = "Biologia", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Biologi", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Biology", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String]
)

case class YOKoe(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: YOKoeNimi,
  @(Schema @field)(example = "E", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: String,
  @(Schema @field)(example = "23", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yhteispistemaara: Optional[Int],
  @(Schema @field)(example = "2019-06-01", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tutkintokerta: LocalDate
)

case class YOOppilaitosNimi(
  @(Schema @field)(example = "Kalevan lukio", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Kalevan lukio sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Kalevan lukio en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String]
)

case class YOOppilaitos(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: YOOppilaitosNimi,
  @(Schema @field)(example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class YOTutkintoNimi(
  @(Schema @field)(example = "Ylioppilastutkinto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Ylioppilastutkinto sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Ylioppilastutkinto en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class YOTutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: YOTutkintoNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yoKokeet: java.util.List[YOKoe]
)

case class LukionOppiaineNimi(
  @(Schema @field)(example = "Äidinkieli ja kirjallisuus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Äidinkieli ja kirjallisuus sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Äidinkieli ja kirjallisuus en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class LukionOppiaine(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: LukionOppiaineNimi,
)

case class LukionOppimaaraNimi(
  @(Schema @field)(example = "Lukion oppimäärä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Lukion oppimäärä sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Lukion oppimäärä en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class LukionOppimaara(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: LukionOppimaaraNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[LukionOppiaine]
)

case class LukionOppiaineenOppimaaraNimi(
  @(Schema @field)(example = "Lukion oppiaineen oppimäärä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Lukion oppiaineen oppimäärä sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Lukion oppiaineen oppimäärä en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class LukionOppiaineenOppimaara(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: LukionOppiaineenOppimaaraNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[LukionOppiaine]
)

case class DIATutkintoNimi(
  @(Schema @field)(example = "DIA-tutkinto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "DIA-tutkinto sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "DIA-tutkinto en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class DIATutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: DIATutkintoNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String
)

case class DIAOppiaineNimi(
  @(Schema @field)(example = "Historia", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Historia sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Historia en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class DIAOppiaine(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: DIAOppiaineNimi,
  @(Schema @field)(description = "Oppiaineen laajuus (vuosiviikkotuntia)", example = "3", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Int,
  @(Schema @field)(example = "8.5", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty keskiarvo: BigDecimal
)

case class DIAVastaavuusTodistusNimi(
  @(Schema @field)(example = "DIA vastaavuustodistus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "DIA vastaavuustodistus sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "DIA vastaavuustodistus en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class DIAVastaavuusTodistus(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: DIAVastaavuusTodistusNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty kieletKirjallisuusTaide: java.util.List[DIAOppiaine],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty matematiikkaLuonnontieteet: java.util.List[DIAOppiaine]
)

case class EBOppiaineNimiUI(
  @(Schema @field)(example = "Matematiikka", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Matematiikka sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Mathematics", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class EBOppiaineOsasuoritusUI(
  @(Schema @field)(example = "Final", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty koodi: String, //final, written, oral
  @(Schema @field)(example = "8.67", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: BigDecimal
)

case class EBOppiaineUI(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: EBOppiaineNimiUI,
  @(Schema @field)(example = "englanti", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(description = "Oppiaineen laajuus (vuosiviikkotuntia)", example = "3", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: BigDecimal,
  @(Schema @field)(requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty written: Optional[EBOppiaineOsasuoritusUI],
  @(Schema @field)(requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty oral: Optional[EBOppiaineOsasuoritusUI],
  @(Schema @field)(requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty `final`: Optional[EBOppiaineOsasuoritusUI]
)

case class EBTutkintoNimi(
  @(Schema @field)(example = "EB-tutkinto (European Baccalaureate)", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "EB-tutkinto (European Baccalaureate) sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "EB-tutkinto (European Baccalaureate) en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class EBTutkintoUI(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: EBTutkintoNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[EBOppiaineUI]
)

case class IBSuoritusNimi(
  @(Schema @field)(example = "Mathematical studies fi", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Mathematical studies sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Mathematical studies", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class IBSuoritus(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: IBSuoritusNimi,
  @(Schema @field)(description = "Oppiaineen laajuus (kurssia)", example = "3", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Int,
  @(Schema @field)(example = "7")
  @BeanProperty predictedGrade: Option[Int],
  @(Schema @field)(example = "7", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: Int,
)

case class IBOppiaineNimi(
  @(Schema @field)(example = "Experimental sciences fi", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Experimental sciences sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Experimental sciences", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class IBOppiaine(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: IBOppiaineNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suoritukset: java.util.List[IBSuoritus]
)

case class IBTutkintoNimi(
  @(Schema @field)(example = "EB-tutkinto (International Baccalaureate)", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "EB-tutkinto (International Baccalaureate) sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "EB-tutkinto (International Baccalaureate) en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class IBTutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: IBTutkintoNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[IBOppiaine]
)

case class PreIBNimi(
  @(Schema @field)(example = "Pre-IB", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Pre-IB sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Pre-IB en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class PreIB(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: PreIBNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String
)

case class YTOOsaAlueNimi(
  @(Schema @field)(example = "Viestintä ja vuorovaikutus äidinkielellä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Kommunikation och interaktion på modersmålet", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Communication and interaction in mother tongue", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String]
)

case class YTOOsaAlue(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: YTOOsaAlueNimi,
  @(Schema @field)(description = "Tutkinnon osa-alueen laajuus (osaamispistettä)", example = "11", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Optional[BigDecimal],
  @(Schema @field)(example = "5", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: Optional[String]
)

case class YTONimi(
  @(Schema @field)(example = "Viestintä- ja vuorovaikutusosaaminen", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Kunnande i kommunikation och interaktion", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Communication and interaction competence", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String]
)

case class YTOArvosana(
  @(Schema @field)(example = "Hyväksytty", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Godkänd", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Pass", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String]
)

case class YTO(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: YTONimi,
  @(Schema @field)(description = "Tutkinnon osan laajuus (osaamispistettä)", example = "11", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Optional[BigDecimal],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: Optional[YTOArvosana],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty osaAlueet: java.util.List[YTOOsaAlue]
)

case class AmmatillisenTutkinnonOsaAlueNimi(
  @(Schema @field)(example = "Toimiminen ajoneuvoalan liiketoimintaympäristössä 1", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Arbete i en affärsmiljö inom fordonsbranschen 1", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class AmmatillisenTutkinnonOsaAlue(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: AmmatillisenTutkinnonOsaAlueNimi,
  @(Schema @field)(description = "Tutkinnon osa-alueen laajuus (osaamispistettä)", example = "11", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Optional[BigDecimal],
  @(Schema @field)(example = "5", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: Optional[String]
)

case class AmmatillisenTutkinnonOsaNimi(
  @(Schema @field)(example = "Toimiminen ajoneuvoalan liiketoimintaympäristössä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Arbete i en affärsmiljö inom fordonsbranschen", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class AmmatillisenTutkinnonOsa(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: AmmatillisenTutkinnonOsaNimi,
  @(Schema @field)(description = "Tutkinnon osan laajuus (osaamispistettä)", example = "11", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Optional[BigDecimal],
  @(Schema @field)(example = "4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: Optional[String],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty osaAlueet: java.util.List[AmmatillisenTutkinnonOsaAlue]
)

case class AmmatillinenOppilaitosNimi(
  @(Schema @field)(example = "Stadin ammattiopisto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Stadin ammattiopisto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Stadin ammattiopisto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class AmmatillinenOppilaitos(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: AmmatillinenOppilaitosNimi,
  @(Schema @field)(example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class AmmatillinentutkintoNimi(
  @(Schema @field)(example = "Ajoneuvoalan perustutkinto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Grundexamen inom fordonsbranschen", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Vocational qualification in the Vehicle Sector", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class Ammatillinentutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: AmmatillinentutkintoNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(example = "4.34", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty painotettuKeskiarvo: Optional[BigDecimal],
  @(Schema @field)(example = "4.34", requiredMode = RequiredMode.REQUIRED)
/*
  // TODO: tämä on kälikuvissa muttei ole lähdedatassa tarjolla
  @BeanProperty ammatillisetYtotKeskiarvo: BigDecimal,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
 */
  @BeanProperty ytot: java.util.List[YTO],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty ammatillisenTutkinnonOsat: java.util.List[AmmatillisenTutkinnonOsa],
  @(Schema @field)(example = "Näyttötutkinto")
  @BeanProperty suoritustapa: Optional[SuoritusTapa]
)

case class AmmattitutkintoNimi(
  @(Schema @field)(example = "Hieronnan ammattitutkinto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Yrkesexamen i massage", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Further vocational qualification in Massage", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class Ammattitutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: AmmattitutkintoNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2017-06-01")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2017-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
)

case class ErikoisammattitutkintoNimi(
  @(Schema @field)(example = "Talous- ja henkilöstöhallinnon erikoisammattitutkinto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Specialyrkesexamen i ekonomi- och personalförvaltning", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Specialist vocational qualification in Business Administration, Financing, Accounting and HR Management", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class Erikoisammattitutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: ErikoisammattitutkintoNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2017-06-01")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2017-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
)

case class TelmaNimi(
  @(Schema @field)(example = "Työhön ja itsenäiseen elämään valmentava koulutus (TELMA)", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Utbildning som handleder för arbete och ett självständigt liv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Preparatory education for work and independent living", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class Telma(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: TelmaNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2017-06-01")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2017-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String
)

case class OsittainenAmmatillinentutkintoNimi(
  @(Schema @field)(example = "Kasvatus- ja ohjausalan perustutkinto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Kasvatus- ja ohjausalan perustutkinto sv", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Kasvatus- ja ohjausalan perustutkinto en", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class OsittainenAmmatillinenTutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: OsittainenAmmatillinentutkintoNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty ammatillisenTutkinnonOsat: java.util.List[AmmatillisenTutkinnonOsa]
)

case class TuvaNimi(
  @(Schema @field)(example = "Tutkintokoulutukseen valmentava koulutus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Utbildning som handleder för examensutbildning", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Preparatory education for an upper secondary qualification", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class TuvaLaajuusYksikko(
  @(Schema @field)(example = "vk", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "v", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "w", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class TuvaLaajuus(
  @(Schema @field)(example = "38", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: BigDecimal,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yksikko: TuvaLaajuusYksikko,
)

case class Tuva(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: TuvaNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "KESKEYTYNYT", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Optional[TuvaLaajuus],
  // TODO: jos halutaan näyttää suorituskieli niin tarvitaan tieto Kosken massaluovutusrajapinnasta
)

case class VapaaSivistystyoOppilaitosNimi(
  @(Schema @field)(example = "Lahden kansanopisto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Lahden kansanopisto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Lahden kansanopisto", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class VapaaSivistystyoOppilaitos(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: VapaaSivistystyoOppilaitosNimi,
  @(Schema @field)(example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class VapaaSivistystyoKoulutusNimi(
  @(Schema @field)(example = "Vapaan sivistystyön koulutus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Fritt bildningsarbete", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Liberal adult education", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class VapaaSivistystyoLaajuusYksikko(
  @(Schema @field)(example = "op", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "sp", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "ECTS cr", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String]
)

case class VapaaSivistystyoLaajuus(
  @(Schema @field)(example = "38", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: BigDecimal,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yksikko: VapaaSivistystyoLaajuusYksikko,
)

case class VapaaSivistystyoKoulutus(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: VapaaSivistystyoKoulutusNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: VapaaSivistystyoOppilaitos,
  @(Schema @field)(example = "KESKEYTYNYT", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Optional[VapaaSivistystyoLaajuus]
)

case class PKOppilaitosNimi(
  @(Schema @field)(example = "Keltinmäen koulu", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Keltinmäen koulu", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Keltinmäen koulu", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class PKOppilaitos(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: PKOppilaitosNimi,
  @(Schema @field)(example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class PerusopetuksenOppiaineNimi(
  @(Schema @field)(example = "matematiikka", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "matematik", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Mathematics", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class PerusopetuksenOppiaineUI(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(example = ESIMERKKI_OPPIAINEKOODI, description="koskioppiaineetyleissivistävä-koodiston koodi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty koodi: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: PerusopetuksenOppiaineNimi,
  @(Schema @field)(example = ESIMERKKI_VIERAS_KIELI_KIELIKOODI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty kieli: Optional[String],
  @(Schema @field)(example = ESIMERKKI_PERUSOPETUKSEN_OPPIAINEEN_ARVOSANA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: String,
  @(Schema @field)(example = "true", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty valinnainen: Boolean,
)

case class PerusopetuksenOppimaaraNimi(
  @(Schema @field)(example = "Perusopetuksen oppimäärä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Grundläggande utbildningens lärokurs", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Basic education syllabus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class YksilollistamisNimi(
  @(Schema @field)(example = "Perusopetuksen osittain yksilöllistetty oppimäärä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Delvis individualiserad lärokurs inom den grundläggande utbildningen", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Partially individualized basic education syllabus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class Yksilollistaminen(
  @(Schema @field)(example = "2", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty arvo: Int,
  @(Schema @field)(requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty nimi: YksilollistamisNimi
)

case class PerusopetuksenOppimaaraUI(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty versioTunniste: Optional[UUID],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: PerusopetuksenOppimaaraNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: PKOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(example = "9A", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luokka: Optional[String],
  @(Schema @field)(example = "false", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yksilollistaminen: Optional[Yksilollistaminen],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[PerusopetuksenOppiaineUI],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty syotetty: Boolean,
)

case class AikuistenPerusopetuksenOppimaaraNimi(
  @(Schema @field)(example = "Aikuisten perusopetuksen oppimäärä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Lärokurs i den grundläggande utbildningen för vuxna", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Preparatory instruction and lower secondary education for adults syllabus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class AikuistenPerusopetuksenOppimaara(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: AikuistenPerusopetuksenOppimaaraNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: PKOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[PerusopetuksenOppiaineUI]
)

case class PerusopetuksenOppimaara78LuokkalaisetNimi(
  @(Schema @field)(example = "Perusopetuksen oppimäärä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Grundläggande utbildningens lärokurs", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Basic education syllabus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class PerusopetuksenOppimaara78Luokkalaiset(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: PerusopetuksenOppimaara78LuokkalaisetNimi,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: PKOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty koulusivistyskieli: String,
  @(Schema @field)(example = "9A", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luokka: String,
  @(Schema @field)(example = "false", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yksilollistetty: Boolean
)

case class NuortenPerusopetuksenOppiaineenOppimaaraNimi(
  @(Schema @field)(example = "Nuorten perusopetuksen oppiaineen oppimäärä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Lärokurs i ett läroämne i grundläggande utbildning", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Basic education for youth subject syllabus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

//Sama luokka käytössä sekä Koskesta tulevalle että käsin syötetylle tiedolle. Koskesta tulevalla tiedolla oppiaineita on aina yksi per entiteetti.
// Käsin syötetylle tiedolle kaikki käsin syötetyt oppiaineet ovat saman entiteetin sisällä.
case class PerusopetuksenOppiaineenOppimaaratUI(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: UUID,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty versioTunniste: Optional[UUID],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: PKOppilaitos,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: PerusopetuksenOppiaineenOppimaaraNimi,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: SuoritusTila,
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty aloituspaiva: Optional[LocalDate],
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[PerusopetuksenOppiaineUI],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty syotetty: Boolean
)

case class PerusopetuksenOppiaineenOppimaaraNimi(
  @(Schema @field)(example = "Perusopetuksen oppiaineen oppimäärä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Lärokurs i ett läroämne i grundläggande utbildning", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Basic education subject syllabus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class OppijanTiedotRequest(
  @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tunniste: Optional[String]
)

trait OppijanTiedotResponse()

case class OppijanTiedotSuccessResponse(
  @(Schema @field)(example = ESIMERKKI_ETUNIMET, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty etunimet: Optional[String],
  @(Schema @field)(example = ESIMERKKI_SUKUNIMI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty sukunimi: Optional[String],
  @(Schema @field)(example = ESIMERKKI_HETU, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty henkiloTunnus: Optional[String],
  @(Schema @field)(example = ESIMERKKI_SYNTYMAIKA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty syntymaAika: Optional[LocalDate],
  @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppijaNumero: String,
  @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty henkiloOID: String,
  @BeanProperty opiskeluoikeudet: java.util.List[UIOpiskeluoikeus],
  @BeanProperty kkTutkinnot: java.util.List[KKSuoritus],
  @BeanProperty yoTutkinnot: java.util.List[YOTutkinto],
  @BeanProperty lukionOppimaara: Optional[LukionOppimaara],
  @BeanProperty lukionOppiaineenOppimaarat: java.util.List[LukionOppiaineenOppimaara],
  @BeanProperty diaTutkinto: Optional[DIATutkinto],
  @BeanProperty diaVastaavuusTodistus: Optional[DIAVastaavuusTodistus],
  @BeanProperty ebTutkinto: Optional[EBTutkintoUI],
  @BeanProperty ibTutkinto: Optional[IBTutkinto],
  @BeanProperty preIB: Optional[PreIB],
  @BeanProperty ammatillisetPerusTutkinnot: java.util.List[Ammatillinentutkinto],
  @BeanProperty ammattitutkinnot: java.util.List[Ammattitutkinto],
  @BeanProperty erikoisammattitutkinnot: java.util.List[Erikoisammattitutkinto],
  @BeanProperty telmat: java.util.List[Telma],
  @BeanProperty tuvat: java.util.List[Tuva],
  @BeanProperty vapaaSivistystyoKoulutukset: java.util.List[VapaaSivistystyoKoulutus],
  @BeanProperty perusopetuksenOppimaarat: java.util.List[PerusopetuksenOppimaaraUI],
  @BeanProperty perusopetuksenOppimaara78Luokkalaiset: Optional[PerusopetuksenOppimaara78Luokkalaiset],
  @BeanProperty perusopetuksenOppiaineenOppimaarat: java.util.List[PerusopetuksenOppiaineenOppimaaratUI]
) extends OppijanTiedotResponse

case class OppijanTiedotFailureResponse(
  @(Schema @field)(example = UI_TIEDOT_HAKU_EPAONNISTUI)
  @BeanProperty virheet: java.util.Set[String],
) extends OppijanTiedotResponse

case class AvainArvoYliajoUI(@BeanProperty avain: String,
                             @BeanProperty arvo: Optional[String],
                             @BeanProperty henkiloOid: String,
                             @BeanProperty hakuOid: String,
                             @BeanProperty virkailijaOid: String,
                             @BeanProperty selite: String)

case class AvainArvoMetadataUI(@BeanProperty selitteet: java.util.List[String],
                               @BeanProperty arvoEnnenYliajoa: Optional[String],
                               @BeanProperty yliajo: Optional[AvainArvoYliajoUI],
                               @BeanProperty arvoOnHakemukselta: Boolean) //Hakemukselta tulleita arvoja ei voi muokata Supassa, ja ne halutaan varmaan useimmiten piilottaa kälissä.


case class AvainArvoContainerUI(@BeanProperty avain: String,
                                @BeanProperty arvo: String,
                                @BeanProperty metadata: AvainArvoMetadataUI)

case class OppijanValintaDataSuccessResponse(
                                         @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
                                         @BeanProperty henkiloOID: String,
                                         @BeanProperty hakuOID: String,
                                         //@BeanProperty leikkuriPaiva: LocalDate, lisätään tänne kun saadaan tieto kaivettua ohjausparametreista ja käyttöön
                                         //@BeanProperty laskennanAlkaminen: LocalDate, lisätään tänne kun saadaan tieto kaivettua jostain ja käyttöön
                                         @BeanProperty avainArvot: java.util.List[AvainArvoContainerUI]
                                       ) extends OppijanTiedotResponse

case class OppijanValintaDataFailureResponse(
                                         @(Schema @field)(example = UI_TIEDOT_HAKU_EPAONNISTUI)
                                         @BeanProperty virheAvaimet: java.util.Set[String],
                                       ) extends OppijanTiedotResponse

trait YliajonMuutosHistoriaResponse()

case class YliajonMuutosUI(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: Optional[String],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luotu: Instant,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virkailija: Optional[String],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty selite: String
)

case class YliajonMuutosHistoriaSuccessResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty muutokset: java.util.List[YliajonMuutosUI],
) extends YliajonMuutosHistoriaResponse

case class YliajonMuutosHistoriaFailureResponse(
  @(Schema @field)(example = UI_TIEDOT_HAKU_EPAONNISTUI)
  @BeanProperty virheAvaimet: java.util.Set[String],
) extends YliajonMuutosHistoriaResponse


case class SyotettavaSuoritusTyyppiVaihtoehtoNimi(
  @(Schema @field)(example = "Nuorten perusopetuksen oppiaineen oppimäärä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Lärokurs i ett läroämne i grundläggande utbildning", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Basic education for youth subject syllabus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class SyotettavaSuoritusTilaVaihtoehtoNimi(
  @(Schema @field)(example = "Suoritus valmis", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Prestationen slutförd", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class SyotettavaSuoritusTilaVaihtoehto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: SyotettavaSuoritusTilaVaihtoehtoNimi,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: String
)

case class SyotettavaSuoritusTyyppiVaihtoehto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: SyotettavaSuoritusTyyppiVaihtoehtoNimi,
  @(Schema @field)(example = "OPPIAINEEN_OPPIMAARA", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: String
)

case class SyotettavaOppiaineVaihtoehtoNimi(
  @(Schema @field)(example = "A1-kieli", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "A1-språk", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "A1-language", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class SyotettavaOppiaineVaihtoehto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: SyotettavaOppiaineVaihtoehtoNimi,
  @(Schema @field)(example = "A1", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: String,
  @(Schema @field)(example = "true", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty isKieli: Boolean,
  @(Schema @field)(example = "false", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty isAidinkieli: Boolean
)

case class SyotettavaSuoritusKieliVaihtoehtoNimi(
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "finska", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Finnish", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class SyotettavaSuoritusKieliVaihtoehto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: SyotettavaSuoritusKieliVaihtoehtoNimi,
  @(Schema @field)(example = "FI", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: String
)

case class SyotettavaAidinkielenOppimaaraVaihtoehtoNimi(
  @(Schema @field)(example = "Suomen kieli ja kirjallisuus", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Finska och litteratur", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "Finnish language and literature", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class SyotettavaAidinkielenOppimaaraVaihtoehto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: SyotettavaAidinkielenOppimaaraVaihtoehtoNimi,
  @(Schema @field)(example = "AI1", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: String
)

case class SyotettavaVierasKieliVaihtoehtoNimi(
  @(Schema @field)(example = "saksa", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "tyska", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "German", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class SyotettavaVierasKieliVaihtoehto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: SyotettavaVierasKieliVaihtoehtoNimi,
  @(Schema @field)(example = "DE", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: String
)

case class SyotettavaYksilollistamisVaihtoehtoNimi(
  @(Schema @field)(example = "Perusopetuksen oppimäärä", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty fi: Optional[String],
  @(Schema @field)(example = "Den grundläggande utbildningens lärokurs", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty sv: Optional[String],
  @(Schema @field)(example = "", requiredMode = RequiredMode.NOT_REQUIRED)
  @BeanProperty en: Optional[String],
)

case class SyotettavaYksilollistamisVaihtoehto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: SyotettavaYksilollistamisVaihtoehtoNimi,
  @(Schema @field)(example = "1", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: Int
)

trait LuoSuoritusOppilaitoksetResponse()

case class LuoSuoritusOppilaitoksetSuccessResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitokset: java.util.List[Oppilaitos],
) extends LuoSuoritusOppilaitoksetResponse

case class LuoSuoritusOppilaitoksetFailureResponse(
  @(Schema @field)(example = UI_LUO_SUORITUS_VAIHTOEHDOT_HAKU_EPAONNISTUI)
  @BeanProperty virheet: java.util.Set[String]
) extends LuoSuoritusOppilaitoksetResponse

case class SyotettavaArvosanaVaihtoehto(
  @(Schema @field)(example = "10", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: Int
)

trait LuoSuoritusDropdownDataResponse()

case class LuoSuoritusDropdownDataSuccessResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suoritusTilat: java.util.List[SyotettavaSuoritusTilaVaihtoehto],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suoritusTyypit: java.util.List[SyotettavaSuoritusTyyppiVaihtoehto],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[SyotettavaOppiaineVaihtoehto],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suoritusKielet: java.util.List[SyotettavaSuoritusKieliVaihtoehto],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty aidinkielenOppimaarat: java.util.List[SyotettavaAidinkielenOppimaaraVaihtoehto],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty vieraatKielet: java.util.List[SyotettavaVierasKieliVaihtoehto],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yksilollistaminen: java.util.List[SyotettavaYksilollistamisVaihtoehto],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosanat: java.util.List[SyotettavaArvosanaVaihtoehto],
) extends LuoSuoritusDropdownDataResponse

case class LuoSuoritusDropdownDataFailureResponse(
  @(Schema @field)(example = UI_LUO_SUORITUS_VAIHTOEHDOT_HAKU_EPAONNISTUI)
  @BeanProperty virheet: java.util.Set[String]
) extends LuoSuoritusDropdownDataResponse

trait LuoPerusopetuksenOppimaaraResponse()

case class LuoPerusopetuksenOppimaaraSuccessResponse() extends LuoPerusopetuksenOppimaaraResponse

case class LuoPerusopetuksenOppimaaraFailureResponseOppiaineVirhe(
  @(Schema @field)(example = ESIMERKKI_OPPIAINEKOODI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineKoodiArvo: String,
  @(Schema @field)(example = UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheAvaimet : java.util.Set[String],
)

case class LuoPerusopetuksenOppimaaraFailureResponse(
  @(Schema @field)(example = UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yleisetVirheAvaimet: java.util.Set[String],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineKohtaisetVirheet: java.util.List[LuoPerusopetuksenOppimaaraFailureResponseOppiaineVirhe]
) extends LuoPerusopetuksenOppimaaraResponse

case class SyotettyPerusopetuksenOppiaine(
  @(Schema @field)(example = ESIMERKKI_OPPIAINEKOODI, description="koskioppiaineetyleissivistävä-koodiston koodi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty koodi: Optional[String],
  @(Schema @field)(example = ESIMERKKI_VIERAS_KIELI_KIELIKOODI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty kieli: Optional[String], //Vieraan kielen tai äidinkielen kieli
  @(Schema @field)(example = ESIMERKKI_PERUSOPETUKSEN_OPPIAINEEN_ARVOSANA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: Optional[Int],
  @(Schema @field)(example = "true", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty valinnainen: Optional[Boolean]
)

case class SyotettyPerusopetuksenOppimaaranSuoritus(
  @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO)
  @BeanProperty oppijaOid: Optional[String],
  @(Schema @field)(example = ESIMERKKI_OPPILAITOS_OID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitosOid: Optional[String],
  @(Schema @field)(example = ESIMERKKI_TILA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: Optional[String],
  @(Schema @field)(example = ESIMERKKI_VALMISTUMISPAIVA)
  @BeanProperty valmistumispaiva: Optional[String], // tämä on merkkijono tarkoituksella, näin päästään tarvittaessa validaattoriin asti ja saadaan ymmärrettävä virhe
  @(Schema @field)(example = ESIMERKKI_LUOKKA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luokka: Optional[String],
  @(Schema @field)(example = ESIMERKKI_SUORITUSKIELI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: Optional[String],
  @(Schema @field)(example = ESIMERKKI_YKSILOLLISTAMINEN, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yksilollistetty: Optional[Int],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: Optional[java.util.List[SyotettyPerusopetuksenOppiaine]]
)

trait LuoPerusopetuksenOppiaineenOppimaaraResponse()

case class LuoPerusopetuksenOppiaineenOppimaaraSuccessResponse() extends LuoPerusopetuksenOppiaineenOppimaaraResponse

case class LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(
  @(Schema @field)(example = UI_LUO_SUORITUS_OPPIAINE_TALLENNUS_EPAONNISTUI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheAvaimet: java.util.Set[String],
) extends LuoPerusopetuksenOppiaineenOppimaaraResponse

case class SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer(
  @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO)
  @BeanProperty oppijaOid: Optional[String],
  @(Schema @field)(example = ESIMERKKI_OPPILAITOS_OID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitosOid: Optional[String],
  @(Schema @field)(example = ESIMERKKI_VALMISTUMISPAIVA)
  @BeanProperty valmistumispaiva: Optional[String], // tämä on merkkijono tarkoituksella, näin päästään tarvittaessa validaattoriin asti ja saadaan ymmärrettävä virhe
  @(Schema @field)(example = ESIMERKKI_SUORITUSKIELI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: Optional[String],
  @(Schema @field)(example = ESIMERKKI_YKSILOLLISTAMINEN, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yksilollistetty: Optional[Int],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[SyotettyPerusopetuksenOppiaine]
)

trait PoistaSuoritusResponse()

case class PoistaSuoritusSuccessResponse() extends PoistaSuoritusResponse

case class PoistaSuoritusFailureResponse(
  @(Schema @field)(example = UI_POISTA_SUORITUS_SUORITUSTA_EI_LOYTYNYT, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheAvaimet: java.util.Set[String],
) extends PoistaSuoritusResponse

trait PoistaYliajotResponse()

case class PoistaYliajoSuccessResponse() extends PoistaYliajotResponse

case class PoistaYliajoFailureResponse(
  @(Schema @field)(example = UI_POISTA_YLIAJO_EPAONNISTUI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheAvaimet: java.util.Set[String],
) extends PoistaYliajotResponse

trait TallennaYliajotOppijalleResponse()

case class TallennaYliajotOppijalleSuccessResponse() extends TallennaYliajotOppijalleResponse

case class TallennaYliajotOppijalleFailureResponse(
  @(Schema @field)(example = UI_TALLENNA_YLIAJO_OPPIJALLE_TUNTEMATON_OPPIJA, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty virheAvaimet: java.util.Set[String]
) extends TallennaYliajotOppijalleResponse

case class Yliajo(
  @(Schema @field)(example = ESIMERKKI_YLIAJO_AVAIN, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty avain: Optional[String],
  @(Schema @field)(example = ESIMERKKI_YLIAJO_ARVO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvo: Optional[String],
  @(Schema @field)(example = ESIMERKKI_YLIAJO_SELITE)
  @BeanProperty selite: Optional[String])

case class YliajoTallennusContainer(
  @(Schema @field)(example = ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty henkiloOid: Optional[String], //Oppija, jonka arvoihin yliajo vaikuttaa
  @(Schema @field)(example = ESIMERKKI_HAKU_OID, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakuOid: Optional[String],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yliajot: Optional[java.util.List[Yliajo]])
