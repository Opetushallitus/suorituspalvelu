package fi.oph.suorituspalvelu.ui

import com.scalatsi.*
import fi.oph.suorituspalvelu.resource.ui.*

import java.util.Optional

trait SuorituksetTsImplicits extends SharedTsImplicits {
  implicit val optionalYoTutkintoTSType: TSType[Optional[YOTutkinto]] = TSType.sameAs[Optional[YOTutkinto], Option[YOTutkinto]]
  implicit val optionalLukionOppimaara: TSType[Optional[LukionOppimaaraUI]] = TSType.sameAs[Optional[LukionOppimaaraUI], Option[LukionOppimaaraUI]]
  implicit val optionalDIATutkinto: TSType[Optional[DIATutkintoUI]] = TSType.sameAs[Optional[DIATutkintoUI], Option[DIATutkintoUI]]
  implicit val optionalEBSuoritus: TSType[Optional[EBOppiaineOsasuoritusUI]] = TSType.sameAs[Optional[EBOppiaineOsasuoritusUI], Option[EBOppiaineOsasuoritusUI]]
  implicit val optionalEBTutkinto: TSType[Optional[EBTutkintoUI]] = TSType.sameAs[Optional[EBTutkintoUI], Option[EBTutkintoUI]]
  implicit val optionalIBTutkinto: TSType[Optional[IBTutkintoUI]] = TSType.sameAs[Optional[IBTutkintoUI], Option[IBTutkintoUI]]
  implicit val optionalSuoritusTapa: TSType[Optional[SuoritusTapaUI]] = TSType.sameAs[Optional[SuoritusTapaUI], Option[SuoritusTapaUI]]
  implicit val optionalYTOArvosana: TSType[Optional[YTOArvosana]] = TSType.sameAs[Optional[YTOArvosana], Option[YTOArvosana]]
  implicit val optionalYksilollistaminen: TSType[Optional[Yksilollistaminen]] = TSType.sameAs[Optional[Yksilollistaminen], Option[Yksilollistaminen]]
  implicit val optionalKorotus: TSType[Optional[Korotus]] = TSType.sameAs[Optional[Korotus], Option[Korotus]]
  implicit val ammatillisenTutkinnonOsaAlueTSType: TSType[AmmatillisenTutkinnonOsaAlue] = TSType.fromCaseClass
  implicit val ammatillisenTutkinnonOsaTSType: TSType[AmmatillisenTutkinnonOsa] = TSType.fromCaseClass
  implicit val perusopetuksenOppiaineTSType: TSType[PerusopetuksenOppiaineUI] = TSType.fromCaseClass
  implicit val optionalPerusopetuksenOppimaara78LuokkalaisetTSType: TSType[Optional[PerusopetuksenOppimaara78Luokkalaiset]] = TSType.sameAs[Optional[PerusopetuksenOppimaara78Luokkalaiset], Option[PerusopetuksenOppimaara78Luokkalaiset]]
  implicit val perusopetuksenOppiaineenOppimaaraTSType: TSType[PerusopetuksenOppiaineenOppimaaratUI] = TSType.fromCaseClass
  implicit val perusopetuksenOppimaaraTSType: TSType[PerusopetuksenOppimaaraUI] = TSType.fromCaseClass
  implicit val lukionOppiaineenOppimaaraTSType: TSType[LukionOppiaineenOppimaara] = TSType.fromCaseClass
  implicit val optionalTuvaLaajuus: TSType[Optional[TuvaLaajuus]] = TSType.sameAs[Optional[TuvaLaajuus], Option[TuvaLaajuus]]
  implicit val optionalVSTLaajuus: TSType[Optional[VapaaSivistystyoLaajuus]] = TSType.sameAs[Optional[VapaaSivistystyoLaajuus], Option[VapaaSivistystyoLaajuus]]
  implicit val tuvaTSType: TSType[TuvaUI] = TSType.fromCaseClass
  implicit val vapaaSivistysTyoKoulutusTSType: TSType[VapaaSivistystyoKoulutus] = TSType.fromCaseClass
  implicit val telmaTSType: TSType[Telma] = TSType.fromCaseClass
  implicit val ammattitutkintoTSType: TSType[Ammattitutkinto] = TSType.fromCaseClass
  implicit val erikoisAmmattitutkintoTSType: TSType[Erikoisammattitutkinto] = TSType.fromCaseClass
  implicit val ytoTSType: TSType[YTO] = TSType.fromCaseClass
  implicit val ammatillinenTutkintoTSType: TSType[Ammatillinentutkinto] = TSType.fromCaseClass
  implicit val osittainenAmmatillinenTutkintoTSType: TSType[OsittainenAmmatillinenTutkintoUI] = TSType.fromCaseClass
  implicit val optionalKkSuoritusNimiTsType: TSType[Optional[KKSuoritusNimiUI]] = TSType.sameAs[Optional[KKSuoritusNimiUI], Option[KKSuoritusNimiUI]]
  implicit val vastaanottoHakukohdeOppilaitosTSType: TSType[VastaanottoHakukohdeOppilaitos] = TSType.fromCaseClass
  implicit val vastaanottoTSType: TSType[VastaanottoUI] = TSType.fromCaseClass
  implicit val vanhaVastaanottoTSType: TSType[VanhaVastaanottoUI] = TSType.fromCaseClass

  // Rekursiivinen tyyppi
  implicit val kkOpintojaksoTsType: TSType[KKOpintojaksoUI] = {
    implicit val recursiveRef: TSType[KKOpintojaksoUI] = TSType.external[KKOpintojaksoUI]("IKKOpintojaksoUI")
    TSType.fromCaseClass[KKOpintojaksoUI]
  }
  implicit val kkSuoritusTSType: TSType[KKSuoritusUI] = TSType.fromCaseClass
  implicit val oppijanTiedotSuccessTSType: TSType[OppijanTiedotSuccessResponse] = TSType.fromCaseClass
  implicit val oppijanTiedotFailureTSType: TSType[OppijanTiedotFailureResponse] = TSType.fromCaseClass
  implicit val oppijanVastaanototSuccessTSType: TSType[OppijanVastaanototSuccessResponse] = TSType.fromCaseClass
  implicit val oppijanVastaanototFailureTSType: TSType[OppijanVastaanototFailureResponse] = TSType.fromCaseClass
}
