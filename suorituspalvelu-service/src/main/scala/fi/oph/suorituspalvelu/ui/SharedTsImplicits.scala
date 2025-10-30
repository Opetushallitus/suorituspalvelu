package fi.oph.suorituspalvelu.ui

import com.scalatsi._
import com.scalatsi.TypescriptType.TSUnion
import fi.oph.suorituspalvelu.resource.ui._
import java.time.LocalDate
import java.util.{Optional, UUID}

trait SharedTsImplicits {
  // Scala-TSI ei osaa oletuksena muuntaa Scala-enumeja oikein
  implicit val suoritusTilaTSType: TSType[SuoritusTila] =
    TSType.alias("SuoritusTila", TSUnion(SuoritusTila.values.map(v => TypescriptType.TSLiteralString(v.toString))))
  implicit val suoritusTapaTSType: TSType[SuoritusTapa] =
    TSType.alias("Suoritustapa", TSUnion(SuoritusTapa.values.map(v => TypescriptType.TSLiteralString(v.toString))))
  implicit val opiskeluoikeusTilaTSType: TSType[OpiskeluoikeusTila] =
    TSType.alias("OpiskeluoikeusTila", TSUnion(OpiskeluoikeusTila.values.map(v => TypescriptType.TSLiteralString(v.toString))))

  // Päivämäärät palautuu rajapinnoista aikaleima-merkkijonoina, joten asetetaan päivämäärät string-tyyppisiksi
  implicit val date: TSType[LocalDate] = TSType.sameAs[LocalDate, String]
  implicit val optionalDate: TSType[Optional[LocalDate]] = TSType.sameAs[Optional[LocalDate], Option[LocalDate]]
  implicit val optionalString: TSType[Optional[String]] = TSType.sameAs[Optional[String], Option[String]]
  implicit val optionalInt: TSType[Optional[Int]] = TSType.sameAs[Optional[Int], Option[Int]]
  implicit val optionalBoolean: TSType[Optional[Boolean]] = TSType.sameAs[Optional[Boolean], Option[Boolean]]
  implicit val optionalBigDecimal: TSType[Optional[BigDecimal]] = TSType.sameAs[Optional[BigDecimal], Option[BigDecimal]]
  implicit val optionalUUID: TSType[Optional[UUID]] = TSType.sameAs[Optional[UUID], Option[UUID]]
}

