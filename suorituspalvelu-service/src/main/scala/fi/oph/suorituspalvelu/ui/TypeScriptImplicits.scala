package fi.oph.suorituspalvelu.ui

import com.scalatsi._
import fi.oph.suorituspalvelu.resource.ui._
import java.util.Optional

trait TypeScriptImplicits extends SharedTsImplicits with ValintaDataTsImplicits {
  implicit val optionalYoTutkintoTSType: TSType[Optional[YOTutkinto]] = TSType.sameAs[Optional[YOTutkinto], Option[YOTutkinto]]
  implicit val optionalLukionOppimaara: TSType[Optional[LukionOppimaara]] = TSType.sameAs[Optional[LukionOppimaara], Option[LukionOppimaara]]
  implicit val optionalDIATutkinto: TSType[Optional[DIATutkinto]] = TSType.sameAs[Optional[DIATutkinto], Option[DIATutkinto]]
  implicit val optionalDIAVastaavuusTodistus: TSType[Optional[DIAVastaavuusTodistus]] = TSType.sameAs[Optional[DIAVastaavuusTodistus], Option[DIAVastaavuusTodistus]]
  implicit val optionalEBSuoritus: TSType[Optional[EBSuoritus]] = TSType.sameAs[Optional[EBSuoritus], Option[EBSuoritus]]
  implicit val optionalEBTutkinto: TSType[Optional[EBTutkinto]] = TSType.sameAs[Optional[EBTutkinto], Option[EBTutkinto]]
  implicit val optionalIBTutkinto: TSType[Optional[IBTutkinto]] = TSType.sameAs[Optional[IBTutkinto], Option[IBTutkinto]]
  implicit val optionalPreIB: TSType[Optional[PreIB]] = TSType.sameAs[Optional[PreIB], Option[PreIB]]
  implicit val optionalSuoritusTapa: TSType[Optional[SuoritusTapa]] = TSType.sameAs[Optional[SuoritusTapa], Option[SuoritusTapa]]
  implicit val optionalYTOArvosana: TSType[Optional[YTOArvosana]] = TSType.sameAs[Optional[YTOArvosana], Option[YTOArvosana]]
  implicit val optionalYksilollistaminen: TSType[Optional[Yksilollistaminen]] = TSType.sameAs[Optional[Yksilollistaminen], Option[Yksilollistaminen]]
  implicit val ammatillisenTutkinnonOsaTSType: TSType[AmmatillisenTutkinnonOsa] = TSType.fromCaseClass
  implicit val perusopetuksenOppiaineTSType: TSType[PerusopetuksenOppiaine] = TSType.fromCaseClass
  implicit val optionalPerusopetuksenOppimaara78LuokkalaisetTSType: TSType[Optional[PerusopetuksenOppimaara78Luokkalaiset]] = TSType.sameAs[Optional[PerusopetuksenOppimaara78Luokkalaiset], Option[PerusopetuksenOppimaara78Luokkalaiset]]
  implicit val perusopetuksenOppiaineenOppimaaraTSType: TSType[PerusopetuksenOppiaineenOppimaara] = TSType.fromCaseClass
  implicit val aikuistenPerusopetuksenOppimaaraTSType: TSType[AikuistenPerusopetuksenOppimaara] = TSType.fromCaseClass
  implicit val perusopetuksenOppimaaraTSType: TSType[PerusopetuksenOppimaara] = TSType.fromCaseClass
  implicit val nuortenPerusopetuksenOppiaineenOppimaaraTSType: TSType[NuortenPerusopetuksenOppiaineenOppimaara] = TSType.fromCaseClass
  implicit val lukionOppiaineenOppimaaraTSType: TSType[LukionOppiaineenOppimaara] = TSType.fromCaseClass
  implicit val optionalTuvaLaajuus: TSType[Optional[TuvaLaajuus]] = TSType.sameAs[Optional[TuvaLaajuus], Option[TuvaLaajuus]]
  implicit val optionalVSTLaajuus: TSType[Optional[VapaaSivistystyoLaajuus]] = TSType.sameAs[Optional[VapaaSivistystyoLaajuus], Option[VapaaSivistystyoLaajuus]]
  implicit val tuvaTSType: TSType[Tuva] = TSType.fromCaseClass
  implicit val vapaaSivistysTyoKoulutusTSType: TSType[VapaaSivistystyoKoulutus] = TSType.fromCaseClass
  implicit val telmaTSType: TSType[Telma] = TSType.fromCaseClass
  implicit val ammattitutkintoTSType: TSType[Ammattitutkinto] = TSType.fromCaseClass
  implicit val erikoisAmmattitutkintoTSType: TSType[Erikoisammattitutkinto] = TSType.fromCaseClass
  implicit val ytoTSType: TSType[YTO] = TSType.fromCaseClass
  implicit val ammatillinenTutkintoTSType: TSType[Ammatillinentutkinto] = TSType.fromCaseClass
  implicit val oppijanTiedotSuccessTSType: TSType[OppijanTiedotSuccessResponse] = TSType.fromCaseClass
  implicit val oppijanTiedotFailureTSType: TSType[OppijanTiedotFailureResponse] = TSType.fromCaseClass
  implicit val oppijatSuccessTSType: TSType[OppijanHakuSuccessResponse] = TSType.fromCaseClass
  implicit val oppijatFailureTSType: TSType[OppijanHakuFailureResponse] = TSType.fromCaseClass
  implicit val oppilaitosSuccessTSType: TSType[OppilaitosSuccessResponse] = TSType.fromCaseClass
  implicit val oppilaitosFailureTSType: TSType[OppilaitosFailureResponse] = TSType.fromCaseClass
  implicit val luoSuoritusDropdownDataSuccessTsType: TSType[LuoSuoritusDropdownDataSuccessResponse] = TSType.fromCaseClass
  implicit val luoSuoritusDropdownDataFailureTsType: TSType[LuoSuoritusDropdownDataFailureResponse] = TSType.fromCaseClass
  implicit val savePerusopetusOppimaaraFailureResponseTsType: TSType[LuoPerusopetuksenOppimaaraFailureResponse] = TSType.fromCaseClass
  implicit val deleteSuoritusFailureResponseTsType: TSType[PoistaSuoritusFailureResponse] = TSType.fromCaseClass
  implicit val kayttajaSuccessResponseTsType: TSType[KayttajaSuccessResponse] = TSType.fromCaseClass
  implicit val kayttajaFailureResponseTsType: TSType[KayttajaFailureResponse] = TSType.fromCaseClass
}

