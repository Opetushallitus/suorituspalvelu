package fi.oph.suorituspalvelu.ui

import com.scalatsi.*
import com.scalatsi.TypescriptType.TSUnion
import com.scalatsi.output.{OutputOptions, StyleOptions, WriteTSToFiles}
import fi.oph.suorituspalvelu.resource.ui.{AikuistenPerusopetuksenOppimaara, AmmatillinenTutkinto, AmmatillisenTutkinnonOsa, DIATutkinto, DIAVastaavuusTodistus, EBSuoritus, EBTutkinto, IBTutkinto, LukionOppimaara, NuortenPerusopetuksenOppiaineenOppimaara, OppijanHakuFailureResponse, OppijanHakuSuccessResponse, OppijanTiedotFailureResponse, OppijanTiedotSuccessResponse, OppilaitosFailureResponse, OppilaitosSuccessResponse, OppimaaranOppiaine, PerusopetuksenOppiaine, PerusopetuksenOppiaineenOppimaara, PerusopetuksenOppimaara, PerusopetuksenOppimaara78Luokkalaiset, PreIB, Tila, VapaanSivistysTyonKoulutus, YOTutkinto, YTOTila}

import java.io.File
import java.time.LocalDate
import java.util.Optional

object TypeScriptGenerator extends App {

  // Scala-TSI ei osaa oletuksena muuntaa Scala-enumeja oikein
  implicit val suoritusTilaTSType: TSType[Tila] =
    TSType.alias("SuoritusTila", TSUnion(Tila.values.map(v => TypescriptType.TSLiteralString(v.toString))))
  implicit val ytoTilaTSType: TSType[YTOTila] =
    TSType.alias("YTOTila", TSUnion(YTOTila.values.map(v => TypescriptType.TSLiteralString(v.toString))))

  implicit val date: TSType[LocalDate] = TSType.external("Date")
  implicit val optionalDate: TSType[Optional[LocalDate]] = TSType.sameAs[Optional[LocalDate], Option[LocalDate]]
  implicit val optionalString: TSType[Optional[String]] = TSType.sameAs[Optional[String], Option[String]]
  implicit val optionalInt: TSType[Optional[Int]] = TSType.sameAs[Optional[Int], Option[Int]]
  implicit val optionalYoTutkintoTSType: TSType[Optional[YOTutkinto]] = TSType.sameAs[Optional[YOTutkinto], Option[YOTutkinto]]
  implicit val optionalLukionOppimaara: TSType[Optional[LukionOppimaara]] = TSType.sameAs[Optional[LukionOppimaara], Option[LukionOppimaara]]
  implicit val optionalDIATutkinto: TSType[Optional[DIATutkinto]] = TSType.sameAs[Optional[DIATutkinto], Option[DIATutkinto]]
  implicit val optionalDIAVastaavuusTodistus: TSType[Optional[DIAVastaavuusTodistus]] = TSType.sameAs[Optional[DIAVastaavuusTodistus], Option[DIAVastaavuusTodistus]]
  implicit val optionalEBSuoritus: TSType[Optional[EBSuoritus]] = TSType.sameAs[Optional[EBSuoritus], Option[EBSuoritus]]
  implicit val optionalEBTutkinto: TSType[Optional[EBTutkinto]] = TSType.sameAs[Optional[EBTutkinto], Option[EBTutkinto]]
  implicit val optionalIBTutkinto: TSType[Optional[IBTutkinto]] = TSType.sameAs[Optional[IBTutkinto], Option[IBTutkinto]]
  implicit val optionalPreIB: TSType[Optional[PreIB]] = TSType.sameAs[Optional[PreIB], Option[PreIB]]
  implicit val ammatillisenTutkinnonOsaTSType: TSType[AmmatillisenTutkinnonOsa] = TSType.fromCaseClass
  implicit val perusopetuksenOppiaineTSType: TSType[PerusopetuksenOppiaine] = TSType.fromCaseClass
  implicit val optionalPerusopetuksenOppimaara78LuokkalaisetTSType: TSType[Optional[PerusopetuksenOppimaara78Luokkalaiset]] = TSType.sameAs[Optional[PerusopetuksenOppimaara78Luokkalaiset], Option[PerusopetuksenOppimaara78Luokkalaiset]]
  implicit val oppimaaranOppiaineTSType: TSType[OppimaaranOppiaine] = TSType.fromCaseClass
  implicit val perusopetuksenOppiaineenOppimaaraTSType: TSType[PerusopetuksenOppiaineenOppimaara] = TSType.fromCaseClass
  implicit val aikuistenPerusopetuksenOppimaaraTSType: TSType[AikuistenPerusopetuksenOppimaara] = TSType.fromCaseClass
  implicit val vapaanSivistysTyonKoulutusTSType: TSType[VapaanSivistysTyonKoulutus] = TSType.fromCaseClass
  implicit val perusopetuksenOppimaaraTSType: TSType[PerusopetuksenOppimaara] = TSType.fromCaseClass
  implicit val nuortenPerusopetuksenOppiaineenOppimaaraTSType: TSType[NuortenPerusopetuksenOppiaineenOppimaara] = TSType.fromCaseClass
  implicit val ammatillinenTutkintoTSType: TSType[AmmatillinenTutkinto] = TSType.fromCaseClass
  implicit val oppijanTiedotSuccessTSType: TSType[OppijanTiedotSuccessResponse] = TSType.fromCaseClass
  implicit val oppijanTiedotFailureTSType: TSType[OppijanTiedotFailureResponse] = TSType.fromCaseClass
  implicit val oppijatSuccessTSType: TSType[OppijanHakuSuccessResponse] = TSType.fromCaseClass
  implicit val oppijatFailureTSType: TSType[OppijanHakuFailureResponse] = TSType.fromCaseClass
  implicit val oppilaitosSuccessTSType: TSType[OppilaitosSuccessResponse] = TSType.fromCaseClass
  implicit val oppilaitosFailureTSType: TSType[OppilaitosFailureResponse] = TSType.fromCaseClass

  // Kirjoitetaan TS-tyypit tiedostoon, polku kannattaa muuttaa sopivammaksi kun fronttityö etenee
  val outputDir = new File("target/generated-sources/typescript/Interface.ts")
  WriteTSToFiles.generate(OutputOptions(
    outputDir,
    StyleOptions(semicolons = true),
    Some("/* Generated using Scala-TSI (https://github.com/scala-tsi/scala-tsi) */"))
  )(Map(
    "oppijanTiedotSuccess" -> oppijanTiedotSuccessTSType.get,
    "oppijanTiedotFailure" -> oppijanTiedotFailureTSType.get,
    "oppijaHakuSuccess" -> oppijatSuccessTSType.get,
    "oppijaHakuFailure" -> oppijatFailureTSType.get,
    "oppilaitosSuccess" -> oppilaitosSuccessTSType.get,
    "oppilaitosFailure" -> oppilaitosFailureTSType.get,
  ))

  println(s"TypeScript interfaces generated")
}