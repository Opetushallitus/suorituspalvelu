package fi.oph.suorituspalvelu.ui

import com.scalatsi._
import com.scalatsi.TypescriptType.TSUnion
import fi.oph.suorituspalvelu.resource.ui._
import java.time.LocalDate
import java.util.{Optional, UUID}

trait SharedTsImplicits {
  // Scala-TSI ei osaa oletuksena muuntaa Scala-enumeja oikein
  implicit val suoritusTilaTSType: TSType[SuoritusTilaUI] =
    TSType.alias("SuoritusTila", TSUnion(SuoritusTilaUI.values.map(v => TypescriptType.TSLiteralString(v.toString))))
  implicit val suoritusTapaTSType: TSType[SuoritusTapaUI] =
    TSType.alias("Suoritustapa", TSUnion(SuoritusTapaUI.values.map(v => TypescriptType.TSLiteralString(v.toString))))
  implicit val opiskeluoikeusTilaTSType: TSType[OpiskeluoikeusTilaUI] =
    TSType.alias("OpiskeluoikeusTila", TSUnion(OpiskeluoikeusTilaUI.values.map(v => TypescriptType.TSLiteralString(v.toString))))

  // Päivämäärät palautuu rajapinnoista aikaleima-merkkijonoina, joten asetetaan päivämäärät string-tyyppisiksi
  implicit val date: TSType[LocalDate] = TSType.sameAs[LocalDate, String]
  implicit val optionalDate: TSType[Optional[LocalDate]] = TSType.sameAs[Optional[LocalDate], Option[LocalDate]]
  implicit val optionalString: TSType[Optional[String]] = TSType.sameAs[Optional[String], Option[String]]
  implicit val optionalInt: TSType[Optional[Int]] = TSType.sameAs[Optional[Int], Option[Int]]
  implicit val optionalBoolean: TSType[Optional[Boolean]] = TSType.sameAs[Optional[Boolean], Option[Boolean]]
  implicit val optionalBigDecimal: TSType[Optional[BigDecimal]] = TSType.sameAs[Optional[BigDecimal], Option[BigDecimal]]
  implicit val optionalUUID: TSType[Optional[UUID]] = TSType.sameAs[Optional[UUID], Option[UUID]]
  implicit val kayttajaSuccessResponseTsType: TSType[KayttajaSuccessResponse] = TSType.fromCaseClass
  implicit val kayttajaFailureResponseTsType: TSType[KayttajaFailureResponse] = TSType.fromCaseClass
}

