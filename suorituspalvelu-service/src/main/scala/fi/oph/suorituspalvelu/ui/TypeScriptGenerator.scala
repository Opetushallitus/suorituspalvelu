package fi.oph.suorituspalvelu.ui

import com.scalatsi.*
import com.scalatsi.TypescriptType.TSUnion
import com.scalatsi.output.{OutputOptions, StyleOptions, WriteTSToFiles}
import fi.oph.suorituspalvelu.resource.ui._

import java.io.File
import java.time.LocalDate
import java.util.{Optional, UUID}

object TypeScriptGenerator extends App {

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

  // Korjataan polku, jotta toimii riippumatta siitä kutsutaanko suorituspalvelu-servicen vai juuren kontekstista
  val prefix = System.getProperty("user.dir").replace("/suorituspalvelu-service", "")
  val outputFile = new File(s"${prefix}/suorituspalvelu-ui/src/types/backend.ts")
  // Kirjoitetaan TS-tyypit tiedostoon
  WriteTSToFiles.generate(OutputOptions(
    outputFile,
    StyleOptions(semicolons = true),
    Some("/* Scala-koodista automaattisesti generoituja tyyppejä (kts. TypeScriptGenerator.scala). Älä muokkaa käsin! */"))
  )(Map(
    "oppijanTiedotSuccess" -> oppijanTiedotSuccessTSType.get,
    "oppijanTiedotFailure" -> oppijanTiedotFailureTSType.get,
    "oppijaHakuSuccess" -> oppijatSuccessTSType.get,
    "oppijaHakuFailure" -> oppijatFailureTSType.get,
    "oppilaitosSuccess" -> oppilaitosSuccessTSType.get,
    "oppilaitosFailure" -> oppilaitosFailureTSType.get,
    "luoSuoritusDropdownDataSuccess" -> luoSuoritusDropdownDataSuccessTsType.get,
    "luoSuoritusDropdownDataFailure" -> luoSuoritusDropdownDataFailureTsType.get
  ))

  println(s"TypeScript interfaces generated")
}