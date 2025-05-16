package fi.oph.suorituspalvelu.validation

import fi.oph.suorituspalvelu.resource.Suoritus

import java.util.Optional
import scala.util.matching.Regex

import scala.jdk.OptionConverters.*

/**
 * Validoi järjestelmään syötetyn suorituksen kentät. Validaattorin virheilmoitukset eivät saa sisältää sensitiivistä
 * tietoa koska ne menevät mm. lokeille.
 */
object Validator {

  final val VALIDATION_OPPIJANUMERO_TYHJA             = "oppijaNumero: Kenttä on pakollinen"
  final val VALIDATION_OPPIJANUMERO_EI_VALIDI         = "oppijaNumero: Oppijanumero ei oli validi oppija oid"

  val oppijaOidPattern: Regex = "^1\\.2\\.246\\.562\\.24\\.\\d+$".r

  def validateOppijanumero(oppijaNumero: Option[String]): Set[String] =
    if (oppijaNumero.isEmpty || oppijaNumero.get.length == 0)
      Set(VALIDATION_OPPIJANUMERO_TYHJA)
    else if(!oppijaOidPattern.matches(oppijaNumero.get))
      Set(VALIDATION_OPPIJANUMERO_EI_VALIDI)
    else
      Set.empty

  def validateSuoritus(suoritus: Suoritus): Set[String] =
    Set(
      validateOppijanumero(suoritus.oppijaNumero.toScala)
    ).flatten
}
