package fi.oph.suorituspalvelu.validation

import fi.oph.suorituspalvelu.resource.Suoritus

import java.util.Optional

/**
 * Validoi järjestelmään syötetyn suorituksen kentät. Validaattorin virheilmoitukset eivät saa sisältää sensitiivistä
 * tietoa koska ne menevät mm. lokeille.
 */
object SuoritusValidator {

  final val VALIDATION_OPPIJANUMERO_TYHJA             = "oppijaNumero: Kenttä on pakollinen"

  def validateOppijanumero(oppijaNumero: Optional[String]): Set[String] =
    if (oppijaNumero.isEmpty || oppijaNumero.get.length == 0)
      Set(VALIDATION_OPPIJANUMERO_TYHJA)
    else
      Set.empty

  def validateSuoritus(suoritus: Suoritus): Set[String] =
    Set(
      validateOppijanumero(suoritus.oppijaNumero)
    ).flatten
}
