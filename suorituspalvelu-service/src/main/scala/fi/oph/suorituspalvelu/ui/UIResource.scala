package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_RESPONSE_400_DESCRIPTION, DATASYNC_RESPONSE_403_DESCRIPTION, EXAMPLE_OPPIJANUMERO, UI_SUORITUKSET_400_DESCRIPTION, UI_SUORITUKSET_403_DESCRIPTION, UI_SUORITUKSET_OPPIJANUMERO_PARAM_NAME, UI_SUORITUKSET_PATH}
import fi.oph.suorituspalvelu.resource.{VirtaSyncFailureResponse, VirtaSyncSuccessResponse}
import fi.oph.suorituspalvelu.validation.Validator
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.*

import java.time.LocalDate
import java.util.Optional
import scala.annotation.meta.field
import scala.beans.BeanProperty

case class OOOppilaitos(
  @(Schema @field)(example = "Tampereen yliopisto", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class Opiskeluoikeus(
  @(Schema @field)(example = "Kasvatustieteen maisteri", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tutkinto: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: OOOppilaitos,
  @(Schema @field)(example = "2020-01-01", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty voimassaolonAlku: LocalDate,
  @(Schema @field)(example = "2024-12-31", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty voimassaolonLoppu: LocalDate
)

// ???? mistä tämä tulee?
case class Hakukohde(
  @(Schema @field)(example = "Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String
)

case class KKOppilaitos(
  @(Schema @field)(description = "Oppilaitoksen nimi", example = "Tampereen yliopisto", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(description = "Oppilaitoksen tunniste", example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class KKSuoritus(
  @(Schema @field)(description = "Tutkinnon nimi", example = "Kasvatustieteen maisteri", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tutkinto: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: KKOppilaitos,
  @(Schema @field)(description = "Tutkinnon tila", example = "KESKEN", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty hakukohde: Hakukohde,
)

case class YOKoe(
  @(Schema @field)(description = "Aine", example = "Matematiikka", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty aine: String,
  @(Schema @field)(example = "Lyhyt oppimäärä (MA)", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty taso: String,
  @(Schema @field)(example = "E", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: String,
  @(Schema @field)(example = "23", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yhteispistemaara: Int,
  @(Schema @field)(example = "2019-06-01", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tutkintokerta: LocalDate
)

case class YOOppilaitos(
  @(Schema @field)(example = "Kalevan lukio", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class YOTutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yoKokeet: java.util.List[YOKoe]
)

case class LukionOppiaine(
  @(Schema @field)(example = "Äidinkieli ja kirjallisuus", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
)

case class LukionOppimaara(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[LukionOppiaine]
)

case class LukionOppiaineenOppimaara(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[LukionOppiaine]
)

case class DIATutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String
)

case class Oppiaine(
  @(Schema @field)(example = "Historia", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "3", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Int,
  @(Schema @field)(example = "8.5", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty keskiarvo: BigDecimal
)

case class DIAVastaavuusTodistus(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty kieletKirjallisuusTaide: java.util.List[Oppiaine],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty matematiikkaLuonnontieteet: java.util.List[Oppiaine]
)

case class EBSuoritus(
  @(Schema @field)(example = "8.67", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: BigDecimal
)

case class EBOppiaine(
  @(Schema @field)(example = "Mathematics", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "englanti", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(example = "3", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Int,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty written: EBSuoritus,
  @BeanProperty oral: Optional[EBSuoritus],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty `final`: EBSuoritus
)

case class EBTutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[EBOppiaine]
)

case class IBSuoritus(
  @(Schema @field)(example = "Mathematical studies", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "3", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Int,
  @(Schema @field)(example = "7")
  @BeanProperty predictedGrade: Option[Int],
  @(Schema @field)(example = "7", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: Int,
)

case class IBOppiaine(
  @(Schema @field)(example = "Experimental sciences", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suoritukset: java.util.List[IBSuoritus]
)

case class IBTutkinto(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[IBOppiaine]
)

case class PreIB(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: YOOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String
)

case class YTO(
  @(Schema @field)(example = "Viestintä- ja vuorovaikutusosaaminen", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "11", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Int,
  @(Schema @field)(example = "Hyväksytty", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String
)

case class AmmatillisenTutkinnonOsa(
  @(Schema @field)(example = "Audiovisuaalisen kulttuurin perusteet", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "11", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Int,
  @(Schema @field)(example = "4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: Int
)

case class AmmatillinenOppilaitos(
  @(Schema @field)(example = "Hämeen ammatti-instituutti, Lepaa", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)


case class AmmatillinenTutkinto(
  @(Schema @field)(example = "Puutarha-alan perustutkinto", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(example = "4.34", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty painotettuKeskiarvo: BigDecimal,
  @(Schema @field)(example = "4.34", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty ammatillisetYtotKeskiarvo: BigDecimal,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty ytot: Optional[java.util.List[YTO]],
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty ammatillisenTutkinnonOsat: java.util.List[AmmatillisenTutkinnonOsa],
  @(Schema @field)(example = "Näyttötutkinto")
  @BeanProperty suoritustapa: Optional[String]
)

case class Ammattitutkinto(
  @(Schema @field)(example = "Maanmittausalan ammattitutkinto", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2017-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
)

case class Erikoisammattitutkinto(
  @(Schema @field)(example = "Talous- ja henkilöstöalan erikoisammattitutkinto", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2017-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
)

case class Telma(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2017-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String
)

case class OsittainenAmmatillinenTutkinto(
  @(Schema @field)(example = "Kasvatus- ja ohjausalan perustutkinto", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty ammatillisenTutkinnonOsat: java.util.List[AmmatillisenTutkinnonOsa]
)

case class Tuva(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "KESKEYTYNYT", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(description = "Suoritettujen opintojen laajuus viikkoina", example = "38", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Int
)

case class VapaanSivistysTyonKoulutus(
  @(Schema @field)(example = "Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "KESKEYTYNYT", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(description = "Suoritettujen opintojen laajuus viikkoina", example = "38", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty laajuus: Int
)

case class PKOppilaitos(
  @(Schema @field)(example = "Keltinmäen koulu", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class PerusopetuksenOppiaine(
  @(Schema @field)(example = "matematiikka", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "9", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: Int,
  @(Schema @field)(example = "S", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty valinnainen: String,
)

case class PerusopetuksenOppimaara(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: PKOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(example = "9A", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty luokka: String,
  @(Schema @field)(example = "false", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty yksilollistetty: Boolean,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[PerusopetuksenOppiaine]
)

case class AikuistenPerusopetuksenOppimaara(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: PKOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[PerusopetuksenOppiaine]
)

case class PerusopetuksenOppimaara78Luokkalaiset(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: PKOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
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

case class OppimaaranOppiaine(
  @(Schema @field)(example = "matematiikka", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "9", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty arvosana: Int
)

case class NuortenPerusopetuksenOppiaineenOppimaara(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: PKOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[OppimaaranOppiaine]
)

case class PerusopetuksenOppiaineenOppimaara(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: PKOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: String,
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[OppimaaranOppiaine]
)

case class OppijanTiedot(
  @(Schema @field)(example = EXAMPLE_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppijaNumero: String,
  @BeanProperty opiskeluoikeudet: Optional[java.util.List[Opiskeluoikeus]],
  @BeanProperty kkTutkinnot: Optional[java.util.List[KKSuoritus]],
  @BeanProperty yoTutkinto: Optional[YOTutkinto],
  @BeanProperty lukionOppimaara: Optional[LukionOppimaara],
  @BeanProperty lukionOppiaineenOppimaarat: Optional[java.util.List[LukionOppiaineenOppimaara]],
  @BeanProperty diaTutkinto: Optional[DIATutkinto],
  @BeanProperty diaVastaavuusTodistus: Optional[DIAVastaavuusTodistus],
  @BeanProperty ebTutkinto: Optional[EBTutkinto],
  @BeanProperty ibTutkinto: Optional[IBTutkinto],
  @BeanProperty preIB: Optional[PreIB],
  @BeanProperty ammatillisetTutkinnot: Optional[java.util.List[AmmatillinenTutkinto]],
  @BeanProperty ammattitutkinnot: Optional[java.util.List[Ammattitutkinto]],
  @BeanProperty erikoisammattitutkinnot: Optional[java.util.List[Erikoisammattitutkinto]],
  @BeanProperty telmat: Optional[java.util.List[Telma]],
  @BeanProperty tuvat: Optional[java.util.List[Tuva]],
  @BeanProperty vapaanSivistystyonKoulutukset: Optional[java.util.List[VapaanSivistysTyonKoulutus]],
  @BeanProperty perusopetuksenOppimaarat: Optional[java.util.List[PerusopetuksenOppimaara]],
  @BeanProperty perusopetuksenOppimaara78Luokkalaiset: Optional[PerusopetuksenOppimaara78Luokkalaiset],
  @BeanProperty nuortenPerusopetuksenOppiaineenOppimaarat: Optional[java.util.List[NuortenPerusopetuksenOppiaineenOppimaara]],
  @BeanProperty perusopetuksenOppiaineenOppimaarat: Optional[java.util.List[PerusopetuksenOppiaineenOppimaara]],
  @BeanProperty aikuistenPerusopetuksenOppimaarat: Optional[java.util.List[AikuistenPerusopetuksenOppimaara]]
)

@Schema(name = "OppijatFailureResponse")
case class SuorituksetFailureResponse(
  @(Schema @field)(example = Validator.VALIDATION_OPPIJANUMERO_EI_VALIDI)
  @BeanProperty virheet: java.util.Set[String]
)

@RequestMapping(path = Array(UI_SUORITUKSET_PATH))
@RestController
@Tag("UI")
class UIResource {

  val LOG = LoggerFactory.getLogger(classOf[UIResource]);

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
      new ApiResponse(responseCode = "400", description = UI_SUORITUKSET_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[SuorituksetFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_SUORITUKSET_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
  ))
  def haeSuoritukset(@PathVariable(UI_SUORITUKSET_OPPIJANUMERO_PARAM_NAME)  @Parameter(description = "oppijan oid", example = EXAMPLE_OPPIJANUMERO, required = true) oppijaNumero: String): ResponseEntity[_] =
    ResponseEntity.status(HttpStatus.OK).body(OppijanTiedot(
      oppijaNumero = "1.2.3.4",
      opiskeluoikeudet = Optional.of(java.util.List.of(Opiskeluoikeus(
        tutkinto = "Kasvatust. maist., kasvatustiede",
        oppilaitos = OOOppilaitos(
          nimi = "Tampereen yliopisto",
          oid = "1.2.3.4"
        ),
        voimassaolonAlku = LocalDate.parse("2001-08-01"),
        voimassaolonLoppu = LocalDate.parse("2025-12-11")
      ))),
      kkTutkinnot = Optional.of(java.util.List.of(KKSuoritus(
          tutkinto = "Kasvatust. maist., kasvatustiede",
          oppilaitos = KKOppilaitos(
            nimi = "Tampereen yliopisto",
            oid = "1.2.3.4"
          ),
        tila = "KESKEN",
        valmistumispaiva = Optional.empty(),
        hakukohde = Hakukohde(
          nimi = "Maisterihaku, luokanopettaja (opetus suomeksi), kasvatustieteiden maisteriohjelma, kasvatustieteen maisteri (2v)"
        ),
      ))),
      yoTutkinto = Optional.of(YOTutkinto(
        oppilaitos = YOOppilaitos(
          nimi = "Ylioppilastutkintolautakunta",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2019-06-01")),
        suorituskieli = "suomi",
        yoKokeet = java.util.List.of(YOKoe(
          aine = "Matematiikka",
          taso= "Lyhyt oppimäärä (MA)",
          arvosana = "E",
          yhteispistemaara = 23,
          tutkintokerta = LocalDate.parse("2019-06-01")
        ))
      )),
      lukionOppimaara = Optional.of(LukionOppimaara(
        oppilaitos = YOOppilaitos(
          nimi = "Ylioppilastutkintolautakunta",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
        suorituskieli = "suomi",
        oppiaineet = java.util.List.of(LukionOppiaine(
          nimi = "Äidinkieli ja kirjallisuus"
        ))
      )),
      lukionOppiaineenOppimaarat = Optional.of(java.util.List.of(LukionOppiaineenOppimaara(
        oppilaitos = YOOppilaitos(
          nimi = "Ylioppilastutkintolautakunta",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
        suorituskieli = "suomi",
        oppiaineet = java.util.List.of(LukionOppiaine(
          nimi = "Äidinkieli ja kirjallisuus"
        ))
      ))),
      diaTutkinto = Optional.of(DIATutkinto(
        oppilaitos = YOOppilaitos(
          nimi = "Ylioppilastutkintolautakunta",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
        suorituskieli = "suomi",
      )),
      diaVastaavuusTodistus = Optional.of(DIAVastaavuusTodistus(
        oppilaitos = YOOppilaitos(
          nimi = "Ylioppilastutkintolautakunta",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
        suorituskieli = "suomi",
        kieletKirjallisuusTaide = java.util.List.of(Oppiaine(
          nimi = "Historia",
          laajuus = 3,
          keskiarvo = 8.5
        )),
        matematiikkaLuonnontieteet = java.util.List.of(Oppiaine(
          nimi = "Matematiikka",
          laajuus = 3,
          keskiarvo = 8.5
        ))
      )),
      ebTutkinto = Optional.of(EBTutkinto(
        oppilaitos = YOOppilaitos(
          nimi = "Ylioppilastutkintolautakunta",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
        suorituskieli = "suomi",
        oppiaineet = java.util.List.of(EBOppiaine(
          nimi = "First language, ranska",
          suorituskieli = "englanti",
          laajuus = 3,
          written = EBSuoritus(
            arvosana = 8.67,
          ),
          oral = Optional.of(EBSuoritus(
            arvosana = 8.67,
          )),
          `final` = EBSuoritus(
            arvosana = 8.67,
          )
        ))
      )),
      ibTutkinto = Optional.of(IBTutkinto(
        oppilaitos = YOOppilaitos(
          nimi = "Ylioppilastutkintolautakunta",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
        suorituskieli = "suomi",
        oppiaineet = java.util.List.of(IBOppiaine(
          nimi = "Experimental sciences",
          suoritukset = java.util.List.of(IBSuoritus(
            nimi = "Mathematical studies", 
            laajuus = 3,
            predictedGrade = Some(7),
            arvosana = 7
          ))
        ))
      )),
      preIB = Optional.of(PreIB(
        oppilaitos = YOOppilaitos(
          nimi = "Ylioppilastutkintolautakunta",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
        suorituskieli = "suomi",
      )),
      ammatillisetTutkinnot = Optional.of(java.util.List.of(AmmatillinenTutkinto(
        nimi = "Puutarha-alan perustutkinto",
        oppilaitos = AmmatillinenOppilaitos(
          nimi = "Hämeen ammatti-instituutti, Lepaa",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2024-12-31")),
        suorituskieli = "suomi",
        painotettuKeskiarvo = 4.34,
        ammatillisetYtotKeskiarvo = 4.34,
        ytot = Optional.of(java.util.List.of(YTO(
          nimi = "Viestintä- ja vuorovaikutusosaaminen",
          laajuus = 11,
          tila = "Hyväksytty"
        ))),
        ammatillisenTutkinnonOsat = java.util.List.of(AmmatillisenTutkinnonOsa(
          nimi = "Audiovisuaalisen kulttuurin perusteet",
          laajuus = 11,
          arvosana = 4,
        )),
        suoritustapa = Optional.of("Näyttötutkinto")
      ))),
      ammattitutkinnot = Optional.of(java.util.List.of(Ammattitutkinto(
        nimi = "Maanmittausalan ammattitutkinto",
        oppilaitos = AmmatillinenOppilaitos(
          nimi = "Hämeen ammatti-instituutti, Lepaa",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
        suorituskieli = "suomi"
      ))),
      erikoisammattitutkinnot = Optional.of(java.util.List.of(Erikoisammattitutkinto(
        nimi = "Talous- ja henkilöstöalan erikoisammattitutkinto",
        oppilaitos = AmmatillinenOppilaitos(
          nimi = "Hämeen ammatti-instituutti, Lepaa",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
        suorituskieli = "suomi"
      ))),
      telmat = Optional.of(java.util.List.of(Telma(
        oppilaitos = AmmatillinenOppilaitos(
          nimi = "Hämeen ammatti-instituutti, Lepaa",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
        suorituskieli = "suomi"
      ))),
      tuvat = Optional.of(java.util.List.of(Tuva(
        oppilaitos = AmmatillinenOppilaitos(
          nimi = "Hämeen ammatti-instituutti, Lepaa",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2017-06-01")),
        suorituskieli = "suomi",
        laajuus = 38
      ))),
      vapaanSivistystyonKoulutukset = Optional.of(java.util.List.of(VapaanSivistysTyonKoulutus(
        nimi = "Kansanopistojen vapaan sivistystyön koulutus oppivelvollisille",
        oppilaitos = AmmatillinenOppilaitos(
          nimi = "Hämeen ammatti-instituutti, Lepaa",
          oid = "1.2.3.4"
        ),
        tila = "KESKEYTYNYT",
        valmistumispaiva = Optional.empty(),
        suorituskieli = "suomi",
        laajuus = 38))),
      perusopetuksenOppimaarat = Optional.of(java.util.List.of(PerusopetuksenOppimaara(
        oppilaitos = PKOppilaitos(
          nimi = "Keltinmäen koulu",
          oid = "1.2.3.4"
        ), 
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
        suorituskieli = "suomi",
        luokka = "9A",
        yksilollistetty = false,
        oppiaineet = java.util.List.of(PerusopetuksenOppiaine(
          nimi = "matematiikka",
          arvosana = 9,
          valinnainen = "S",
        ))
      ))),
      perusopetuksenOppimaara78Luokkalaiset = Optional.of(PerusopetuksenOppimaara78Luokkalaiset(
        oppilaitos = PKOppilaitos(
          nimi = "Keltinmäen koulu",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
        suorituskieli = "suomi",
        koulusivistyskieli = "suomi",
        luokka = "9A",
        yksilollistetty = false
      )),
      nuortenPerusopetuksenOppiaineenOppimaarat = Optional.of(java.util.List.of(NuortenPerusopetuksenOppiaineenOppimaara(
        oppilaitos = PKOppilaitos(
          nimi = "Keltinmäen koulu",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
        suorituskieli = "suomi",
        oppiaineet = java.util.List.of(OppimaaranOppiaine(
          nimi = "matematiikka",
          arvosana = 9
        ))
      ))),
      perusopetuksenOppiaineenOppimaarat = Optional.of(java.util.List.of(PerusopetuksenOppiaineenOppimaara(
        oppilaitos = PKOppilaitos(
          nimi = "Keltinmäen koulu",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
        suorituskieli = "suomi",
        oppiaineet = java.util.List.of(OppimaaranOppiaine(
          nimi = "matematiikka",
          arvosana = 9
        ))
      ))),
      aikuistenPerusopetuksenOppimaarat = Optional.of(java.util.List.of(AikuistenPerusopetuksenOppimaara(
        oppilaitos = PKOppilaitos(
          nimi = "Keltinmäen koulu",
          oid = "1.2.3.4"
        ),
        tila = "VALMIS",
        valmistumispaiva = Optional.of(LocalDate.parse("2016-06-01")),
        suorituskieli = "suomi",
        oppiaineet = java.util.List.of(PerusopetuksenOppiaine(
          nimi = "matematiikka",
          arvosana = 9,
          valinnainen = "S"
        ))
      )))
    ))
}