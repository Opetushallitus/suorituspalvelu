package fi.oph.suorituspalvelu.resource.ui

import fi.oph.suorituspalvelu.resource.ApiConstants.{EXAMPLE_OPPIJANUMERO, UI_HAKU_ESIMERKKI_HETU, UI_HAKU_ESIMERKKI_NIMI, UI_HAKU_ESIMERKKI_OPPIJANUMERO, UI_HAKU_ESIMERKKI_OPPILAITOS_NIMI, UI_HAKU_ESIMERKKI_OPPILAITOS_OID, UI_HAKU_ESIMERKKI_VIRHE, UI_TIEDOT_ESIMERKKI_VIRHE}
import fi.oph.suorituspalvelu.resource.*
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode

import java.time.LocalDate
import java.util.Optional
import scala.annotation.meta.field
import scala.beans.BeanProperty

case class Oppilaitos(
  @(Schema @field)(example = UI_HAKU_ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = UI_HAKU_ESIMERKKI_OPPILAITOS_NIMI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

trait OppilaitosResponse()

case class OppilaitosSuccessResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitokset: java.util.List[Oppilaitos]
) extends OppilaitosResponse

case class OppilaitosFailureResponse(
  @(Schema @field)(example = UI_HAKU_ESIMERKKI_VIRHE)
  @BeanProperty virheet: java.util.Set[String]
) extends OppilaitosResponse

trait OppijanHakuResponse()

case class Oppija(
  @(Schema @field)(example = UI_HAKU_ESIMERKKI_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppijaNumero: String,
  @(Schema @field)(example = UI_HAKU_ESIMERKKI_HETU)
  @BeanProperty hetu: Optional[String],
  @(Schema @field)(example = UI_HAKU_ESIMERKKI_NIMI, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String
)

case class OppijanHakuSuccessResponse(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppijat: java.util.List[Oppija]
) extends OppijanHakuResponse

case class OppijanHakuFailureResponse(
  @(Schema @field)(example = UI_HAKU_ESIMERKKI_VIRHE)
  @BeanProperty virheet: java.util.Set[String]
) extends OppijanHakuResponse

enum Tila:
  case VALMIS
  case KESKEN
  case KESKEYTYNYT

enum YTOTila:
  case HYVAKSYTTY

case class OOOppilaitos(
  @(Schema @field)(example = "Tampereen yliopisto", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty nimi: String,
  @(Schema @field)(example = "1.2.3.4", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oid: String
)

case class UIOpiskeluoikeus(
  @(Schema @field)(example = "Kasvatustieteen maisteri", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tutkinto: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: OOOppilaitos,
  @(Schema @field)(example = "2020-01-01", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty voimassaolonAlku: LocalDate,
  @(Schema @field)(example = "2024-12-31", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty voimassaolonLoppu: LocalDate
)

// TODO: ei ole mitenkään selvää miten kouta-tiedon voi yhdistää järkevästi kk-suorituksiin, käytävä läpi
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @(Schema @field)(example = "HYVAKSYTTY", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: YTOTila
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
  @BeanProperty tila: Tila,
  @(Schema @field)(example = "2024-12-31")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(example = "4.34", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty painotettuKeskiarvo: BigDecimal,
  @(Schema @field)(example = "4.34", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty ammatillisetYtotKeskiarvo: BigDecimal,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty ytot: java.util.List[YTO],
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
  @(Schema @field)(example = "2017-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
)

case class Telma(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: AmmatillinenOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty arvosana: Optional[Int],
  @(Schema @field)(example = "S", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty valinnainen: Optional[String],
)

case class PerusopetuksenOppimaara(
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppilaitos: PKOppilaitos,
  @(Schema @field)(example = "VALMIS", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
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
  @BeanProperty tila: Tila,
  @(Schema @field)(example = "2016-06-01")
  @BeanProperty valmistumispaiva: Optional[LocalDate],
  @(Schema @field)(example = "suomi", requiredMode = RequiredMode.REQUIRED)
  @BeanProperty suorituskieli: String,
  @(Schema @field)(requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppiaineet: java.util.List[OppimaaranOppiaine]
)

trait OppijanTiedotResponse()

case class OppijanTiedotSuccessResponse(
  @(Schema @field)(example = EXAMPLE_OPPIJANUMERO, requiredMode = RequiredMode.REQUIRED)
  @BeanProperty oppijaNumero: String,
  @BeanProperty opiskeluoikeudet: java.util.List[UIOpiskeluoikeus],
  @BeanProperty kkTutkinnot: java.util.List[KKSuoritus],
  @BeanProperty yoTutkinto: Optional[YOTutkinto],
  @BeanProperty lukionOppimaara: Optional[LukionOppimaara],
  @BeanProperty lukionOppiaineenOppimaarat: java.util.List[LukionOppiaineenOppimaara],
  @BeanProperty diaTutkinto: Optional[DIATutkinto],
  @BeanProperty diaVastaavuusTodistus: Optional[DIAVastaavuusTodistus],
  @BeanProperty ebTutkinto: Optional[EBTutkinto],
  @BeanProperty ibTutkinto: Optional[IBTutkinto],
  @BeanProperty preIB: Optional[PreIB],
  @BeanProperty ammatillisetTutkinnot: java.util.List[AmmatillinenTutkinto],
  @BeanProperty ammattitutkinnot: java.util.List[Ammattitutkinto],
  @BeanProperty erikoisammattitutkinnot: java.util.List[Erikoisammattitutkinto],
  @BeanProperty telmat: java.util.List[Telma],
  @BeanProperty tuvat: java.util.List[Tuva],
  @BeanProperty vapaanSivistystyonKoulutukset: java.util.List[VapaanSivistysTyonKoulutus],
  @BeanProperty perusopetuksenOppimaarat: java.util.List[PerusopetuksenOppimaara],
  @BeanProperty perusopetuksenOppimaara78Luokkalaiset: Optional[PerusopetuksenOppimaara78Luokkalaiset],
  @BeanProperty nuortenPerusopetuksenOppiaineenOppimaarat: java.util.List[NuortenPerusopetuksenOppiaineenOppimaara],
  @BeanProperty perusopetuksenOppiaineenOppimaarat: java.util.List[PerusopetuksenOppiaineenOppimaara],
  @BeanProperty aikuistenPerusopetuksenOppimaarat: java.util.List[AikuistenPerusopetuksenOppimaara]
) extends OppijanTiedotResponse

case class OppijanTiedotFailureResponse(
  @(Schema @field)(example = UI_TIEDOT_ESIMERKKI_VIRHE)
  @BeanProperty virheet: java.util.Set[String]
) extends OppijanTiedotResponse

