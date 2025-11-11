package fi.oph.suorituspalvelu.ui

import com.scalatsi._
import com.scalatsi.output.{OutputOptions, StyleOptions, WriteTSToFiles}
import java.io.File

object TypeScriptGenerator extends TypeScriptImplicits {
  def main(args: Array[String]): Unit = {
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
      "luoSuoritusDropdownDataFailure" -> luoSuoritusDropdownDataFailureTsType.get,
      "savePerusopetusOppimaaraFailure" -> savePerusopetusOppimaaraFailureResponseTsType.get,
      "deleteSuoritusFailure" -> deleteSuoritusFailureResponseTsType.get,
      "oppijanValintaDataSuccess" -> oppijanValintaDataSuccessResponseTsType.get,
      "oppijanValintaDataFailure" -> oppijanValintaDataFailureResponseTsType.get,
      "saveYliajoBody" -> yliajoTallennusContainerTsType.get,
      "kayttajaSuccessResponse" -> kayttajaSuccessResponseTsType.get,
      "kayttajaFailureResponse" -> kayttajaFailureResponseTsType.get,
      "oppijanHautSuccessResponse" -> oppijanHautSuccessResponseTsType.get,
      "oppijanHautFailureResponse" -> oppijanHautFailureResponseTsType.get
    ))

    println(s"TypeScript interfaces generated")
  }
}