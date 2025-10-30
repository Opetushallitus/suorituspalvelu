package fi.oph.suorituspalvelu.ui

import com.scalatsi._
import fi.oph.suorituspalvelu.resource.ui._
import java.util.Optional

trait ValintaDataTsImplicits extends SharedTsImplicits {
  implicit val avainArvoYliajoTsType: TSType[AvainArvoYliajoUI] = TSType.fromCaseClass
  implicit val optionalAvainArvoYliajoTsType: TSType[Optional[AvainArvoYliajoUI]] = TSType.sameAs[Optional[AvainArvoYliajoUI], Option[AvainArvoYliajoUI]]
  implicit val avainArvoMetadataTsType: TSType[AvainArvoMetadataUI] = TSType.fromCaseClass
  implicit val avainArvoContainerTsType: TSType[AvainArvoContainerUI] = TSType.fromCaseClass
  implicit val oppijanValintaDataSuccessResponseTsType: TSType[OppijanValintaDataSuccessResponse] = TSType.fromCaseClass
  implicit val oppijanValintaDataFailureResponseTsType: TSType[OppijanValintaDataFailureResponse] = TSType.fromCaseClass
}

