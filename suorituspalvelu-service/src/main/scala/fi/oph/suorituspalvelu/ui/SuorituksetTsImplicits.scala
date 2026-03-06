package fi.oph.suorituspalvelu.ui

import com.scalatsi.*
import fi.oph.suorituspalvelu.resource.ui.*

import java.util.Optional

trait SuorituksetTsImplicits extends SharedTsImplicits {
  // YO types
  implicit val yoKoeTSType: TSType[YOKoe] = TSType.fromCaseClass
  implicit val yoOppilaitosTSType: TSType[YOOppilaitos] = TSType.fromCaseClass
  implicit val yoTutkintoTSType: TSType[YOTutkinto] = TSType.fromCaseClass

  // Lukio types
  implicit val lukionOppimaaraTSType: TSType[LukionOppimaaraUI] = TSType.fromCaseClass
  implicit val lukionOppiaineenOppimaaraTSType: TSType[LukionOppiaineenOppimaara] = TSType.fromCaseClass

  // DIA types
  implicit val diaTutkintoTSType: TSType[DIATutkintoUI] = TSType.fromCaseClass
  implicit val diaVastaavuusTodistusTSType: TSType[DIAVastaavuusTodistusUI] = TSType.fromCaseClass

  // EB types - define osasuuritus + its Optional before oppiaine (oppiaine has Optional[osasuuritus] fields)
  implicit val ebOppiaineOsasuoritusTSType: TSType[EBOppiaineOsasuoritusUI] = TSType.fromCaseClass
  implicit val optionalEBOppiaineOsasuoritus: TSType[Optional[EBOppiaineOsasuoritusUI]] = TSType.sameAs[Optional[EBOppiaineOsasuoritusUI], Option[EBOppiaineOsasuoritusUI]]
  implicit val ebOppiaineUITSType: TSType[EBOppiaineUI] = TSType.fromCaseClass
  implicit val ebTutkintoTSType: TSType[EBTutkintoUI] = TSType.fromCaseClass

  // IB types - define Optional[SuorituskieliUI] before IBTutkintoUI (which has that field)
  implicit val ibSuoritusTSType: TSType[IBSuoritusUI] = TSType.fromCaseClass
  implicit val ibOppiaineTSType: TSType[IBOppiaineUI] = TSType.fromCaseClass
  implicit val optionalSuorituskieliTSType: TSType[Optional[SuorituskieliUI]] = TSType.sameAs[Optional[SuorituskieliUI], Option[SuorituskieliUI]]
  implicit val ibTutkintoTSType: TSType[IBTutkintoUI] = TSType.fromCaseClass

  // Perusopetus types - define Optional[Yksilollistaminen] before PerusopetuksenOppimaaraUI (which has that field)
  implicit val yksilollistaminenTSType: TSType[Yksilollistaminen] = TSType.fromCaseClass
  implicit val optionalYksilollistaminen: TSType[Optional[Yksilollistaminen]] = TSType.sameAs[Optional[Yksilollistaminen], Option[Yksilollistaminen]]
  implicit val perusopetuksenOppimaara78LuokkalaisetTSType: TSType[PerusopetuksenOppimaara78Luokkalaiset] = TSType.fromCaseClass
  implicit val perusopetuksenOppiaineTSType: TSType[PerusopetuksenOppiaineUI] = TSType.fromCaseClass
  implicit val perusopetuksenOppiaineenOppimaaraTSType: TSType[PerusopetuksenOppiaineenOppimaaratUI] = TSType.fromCaseClass
  implicit val perusopetuksenOppimaaraTSType: TSType[PerusopetuksenOppimaaraUI] = TSType.fromCaseClass

  // TUVA types - define Optional[TuvaLaajuus] before TuvaUI (which has that field)
  implicit val tuvaLaajuusTSType: TSType[TuvaLaajuus] = TSType.fromCaseClass
  implicit val optionalTuvaLaajuus: TSType[Optional[TuvaLaajuus]] = TSType.sameAs[Optional[TuvaLaajuus], Option[TuvaLaajuus]]
  implicit val tuvaTSType: TSType[TuvaUI] = TSType.fromCaseClass

  // VST types - define Optional[VapaaSivistystyoLaajuus] before VapaaSivistystyoKoulutus (which has that field)
  implicit val vapaaSivistystyoLaajuusTSType: TSType[VapaaSivistystyoLaajuus] = TSType.fromCaseClass
  implicit val optionalVSTLaajuus: TSType[Optional[VapaaSivistystyoLaajuus]] = TSType.sameAs[Optional[VapaaSivistystyoLaajuus], Option[VapaaSivistystyoLaajuus]]
  implicit val vapaaSivistysTyoKoulutusTSType: TSType[VapaaSivistystyoKoulutus] = TSType.fromCaseClass

  // Ammatillinen types - define Optional[SuoritusTapaUI] before Ammatillinentutkinto (which has that field)
  implicit val ammatillisenTutkinnonOsaTSType: TSType[AmmatillisenTutkinnonOsa] = TSType.fromCaseClass
  implicit val ytoArvosanaTSType: TSType[YTOArvosana] = TSType.fromCaseClass
  implicit val optionalYTOArvosana: TSType[Optional[YTOArvosana]] = TSType.sameAs[Optional[YTOArvosana], Option[YTOArvosana]]
  implicit val ytoTSType: TSType[YTO] = TSType.fromCaseClass
  implicit val optionalSuoritusTapa: TSType[Optional[SuoritusTapaUI]] = TSType.sameAs[Optional[SuoritusTapaUI], Option[SuoritusTapaUI]]
  implicit val ammatillinenTutkintoTSType: TSType[Ammatillinentutkinto] = TSType.fromCaseClass
  implicit val ammattitutkintoTSType: TSType[Ammattitutkinto] = TSType.fromCaseClass
  implicit val erikoisAmmattitutkintoTSType: TSType[Erikoisammattitutkinto] = TSType.fromCaseClass
  implicit val telmaTSType: TSType[Telma] = TSType.fromCaseClass

  // KK types - Optional[KKSuoritusNimiUI] and Optional[SuorituskieliUI] must come before KKSuoritusUI
  implicit val opiskeluoikeusUITSType: TSType[OpiskeluoikeusUI] = TSType.fromCaseClass
  implicit val kkSuoritusNimiTSType: TSType[KKSuoritusNimiUI] = TSType.fromCaseClass
  implicit val optionalKkSuoritusNimiTsType: TSType[Optional[KKSuoritusNimiUI]] = TSType.sameAs[Optional[KKSuoritusNimiUI], Option[KKSuoritusNimiUI]]
  // Rekursiivinen tyyppi
  implicit val kkOpintojaksoTsType: TSType[KKOpintojaksoUI] = {
    implicit val recursiveRef: TSType[KKOpintojaksoUI] = TSType.external[KKOpintojaksoUI]("IKKOpintojaksoUI")
    TSType.fromCaseClass[KKOpintojaksoUI]
  }
  implicit val kkSuoritusTSType: TSType[KKSuoritusUI] = TSType.fromCaseClass

  // Vastaanotto types
  implicit val vastaanottoHakukohdeOppilaitosTSType: TSType[VastaanottoHakukohdeOppilaitos] = TSType.fromCaseClass
  implicit val vastaanottoTSType: TSType[VastaanottoUI] = TSType.fromCaseClass
  implicit val vanhaVastaanottoTSType: TSType[VanhaVastaanottoUI] = TSType.fromCaseClass

  // Optional wrappers needed by Response types below
  implicit val optionalYoTutkintoTSType: TSType[Optional[YOTutkinto]] = TSType.sameAs[Optional[YOTutkinto], Option[YOTutkinto]]
  implicit val optionalLukionOppimaara: TSType[Optional[LukionOppimaaraUI]] = TSType.sameAs[Optional[LukionOppimaaraUI], Option[LukionOppimaaraUI]]
  implicit val optionalDIATutkinto: TSType[Optional[DIATutkintoUI]] = TSType.sameAs[Optional[DIATutkintoUI], Option[DIATutkintoUI]]
  implicit val optionalDIAVastaavuusTodistus: TSType[Optional[DIAVastaavuusTodistusUI]] = TSType.sameAs[Optional[DIAVastaavuusTodistusUI], Option[DIAVastaavuusTodistusUI]]
  implicit val optionalEBTutkinto: TSType[Optional[EBTutkintoUI]] = TSType.sameAs[Optional[EBTutkintoUI], Option[EBTutkintoUI]]
  implicit val optionalIBSuoritus: TSType[Optional[IBSuoritusUI]] = TSType.sameAs[Optional[IBSuoritusUI], Option[IBSuoritusUI]]
  implicit val optionalIBOppiaine: TSType[Optional[IBOppiaineUI]] = TSType.sameAs[Optional[IBOppiaineUI], Option[IBOppiaineUI]]
  implicit val optionalIBTutkinto: TSType[Optional[IBTutkintoUI]] = TSType.sameAs[Optional[IBTutkintoUI], Option[IBTutkintoUI]]
  implicit val optionalPerusopetuksenOppimaara78LuokkalaisetTSType: TSType[Optional[PerusopetuksenOppimaara78Luokkalaiset]] = TSType.sameAs[Optional[PerusopetuksenOppimaara78Luokkalaiset], Option[PerusopetuksenOppimaara78Luokkalaiset]]

  // Response types
  implicit val oppijanTiedotSuccessTSType: TSType[OppijanTiedotSuccessResponse] = TSType.fromCaseClass
  implicit val oppijanTiedotFailureTSType: TSType[OppijanTiedotFailureResponse] = TSType.fromCaseClass
  implicit val oppijanVastaanototSuccessTSType: TSType[OppijanVastaanototSuccessResponse] = TSType.fromCaseClass
  implicit val oppijanVastaanototFailureTSType: TSType[OppijanVastaanototFailureResponse] = TSType.fromCaseClass
}
