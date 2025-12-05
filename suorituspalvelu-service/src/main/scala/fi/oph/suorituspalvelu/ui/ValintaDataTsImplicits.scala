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
  implicit val yliajoTsType: TSType[Yliajo] = TSType.fromCaseClass
  implicit val optionalYliajoListTsType: TSType[Optional[java.util.List[Yliajo]]] = TSType.sameAs[Optional[java.util.List[Yliajo]], Option[Seq[Yliajo]]]
  implicit val yliajoTallennusContainerTsType: TSType[YliajoTallennusContainer] = TSType.fromCaseClass
  implicit val oppijanHautSuccessResponseTsType: TSType[OppijanHautSuccessResponse] = TSType.fromCaseClass
  implicit val oppijanHautFailureResponseTsType: TSType[OppijanHautFailureResponse] = TSType.fromCaseClass
}

