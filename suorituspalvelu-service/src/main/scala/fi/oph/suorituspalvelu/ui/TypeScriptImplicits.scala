package fi.oph.suorituspalvelu.ui
import com.scalatsi._
import fi.oph.suorituspalvelu.resource.ui._

trait TypeScriptImplicits extends SuorituksetTsImplicits with ValintaDataTsImplicits {
  implicit val oppijatSuccessTSType: TSType[OppijanHakuSuccessResponse] = TSType.fromCaseClass
  implicit val oppijatFailureTSType: TSType[OppijanHakuFailureResponse] = TSType.fromCaseClass
  implicit val oppilaitosSuccessTSType: TSType[OppilaitosSuccessResponse] = TSType.fromCaseClass
  implicit val oppilaitosFailureTSType: TSType[OppilaitosFailureResponse] = TSType.fromCaseClass
  implicit val vuodetSuccessResponse: TSType[VuodetSuccessResponse] = TSType.fromCaseClass
  implicit val vuodetFailureResponse: TSType[VuodetFailureResponse] = TSType.fromCaseClass
  implicit val luokatSuccessResponse: TSType[LuokatSuccessResponse] = TSType.fromCaseClass
  implicit val luokatFailureResponse: TSType[LuokatFailureResponse] = TSType.fromCaseClass
  implicit val luoSuoritusDropdownDataSuccessTsType: TSType[LuoSuoritusDropdownDataSuccessResponse] = TSType.fromCaseClass
  implicit val luoSuoritusDropdownDataFailureTsType: TSType[LuoSuoritusDropdownDataFailureResponse] = TSType.fromCaseClass
  implicit val savePerusopetusOppimaaraFailureResponseTsType: TSType[LuoPerusopetuksenOppimaaraFailureResponse] = TSType.fromCaseClass
  implicit val deleteSuoritusFailureResponseTsType: TSType[PoistaSuoritusFailureResponse] = TSType.fromCaseClass
}

