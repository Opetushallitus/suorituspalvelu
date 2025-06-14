package fi.oph.suorituspalvelu.validation

import fi.oph.suorituspalvelu.resource.Suoritus

import java.time.Instant
import java.util.Optional
import scala.util.matching.Regex
import scala.jdk.OptionConverters.*

/**
 * Validoi järjestelmään syötetyn suorituksen kentät. Validaattorin virheilmoitukset eivät saa sisältää sensitiivistä
 * tietoa koska ne menevät mm. lokeille.
 */
object Validator {

  final val VALIDATION_OPPIJANUMERO_TYHJA = "oppijaNumero: Kenttä on pakollinen"
  final val VALIDATION_OPPIJANUMERO_EI_VALIDI = "oppijaNumero: Oppijanumero ei ole validi oppija oid"
  final val VALIDATION_HAKUOID_TYHJA = "hakuOid: Kenttä on pakollinen"
  final val VALIDATION_HAKUOID_EI_VALIDI = "hakuOid ei ole validi: "
  final val VALIDATION_HAKUKOHDEOID_TYHJA = "hakukohdeOid: Kenttä on pakollinen"
  final val VALIDATION_HAKUKOHDEOID_EI_VALIDI = "hakukohdeOid ei ole validi: "
  final val VALIDATION_EI_VALIDIT_OIDIT = "Seuraavat oppijanumerot eivät ole valideja: "

  final val VALIDATION_MUOKATTUJALKEEN_TYHJA          = "muokattuJalkeen: Kenttä on pakollinen"
  final val VALIDATION_MUOKATTUJALKEEN_EI_VALIDI      = "muokattuJalkeen: muokattuJalkeen ei oli validi aikaleima"
  
  
  val oppijaOidPattern: Regex = "^1\\.2\\.246\\.562\\.24\\.\\d+$".r
  val hakuOidPattern: Regex = "^1\\.2\\.246\\.562\\.29\\.\\d+$".r
  val hakukohdeOidPattern: Regex = "^1\\.2\\.246\\.562\\.20\\.\\d+$".r

  def validateOppijanumero(oppijaNumero: Option[String], pakollinen: Boolean): Set[String] =
    if (oppijaNumero.isEmpty || oppijaNumero.get.length == 0)
      if(pakollinen)
        Set(VALIDATION_OPPIJANUMERO_TYHJA)
      else
        Set.empty
    else if(!oppijaOidPattern.matches(oppijaNumero.get))
      Set(VALIDATION_OPPIJANUMERO_EI_VALIDI)
    else
      Set.empty
      
  def validateMuokattujalkeen(aikaleima: Option[String], pakollinen: Boolean): Set[String] =
    if (aikaleima.isEmpty || aikaleima.get.length == 0)
      if(pakollinen)
        Set(VALIDATION_MUOKATTUJALKEEN_TYHJA)
      else
        Set.empty
    else
      try
        Instant.parse(aikaleima.get)
        Set.empty
      catch
        case default => Set(VALIDATION_MUOKATTUJALKEEN_EI_VALIDI)
    
  def validatePersonOids(personOids: Set[String]): Set[String] = {
    if (personOids.isEmpty)
      Set(VALIDATION_OPPIJANUMERO_TYHJA)
    else {
      val nonValid = personOids.filter(!oppijaOidPattern.matches(_))
      if (nonValid.nonEmpty)
        Set(VALIDATION_EI_VALIDIT_OIDIT + nonValid)
      else
        Set.empty
    }
  }

  def validateHakuOid(hakuOid: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (hakuOid.isEmpty || hakuOid.get.isEmpty))
      Set(VALIDATION_HAKUOID_TYHJA)
    else if (hakuOid.isDefined && !hakuOidPattern.matches(hakuOid.get))
      Set(VALIDATION_HAKUOID_EI_VALIDI+hakuOid.get)
    else
      Set.empty
  }

  def validateHakukohdeOid(hakukohdeOid: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (hakukohdeOid.isEmpty || hakukohdeOid.get.isEmpty))
      Set(VALIDATION_HAKUKOHDEOID_TYHJA)
    else if (hakukohdeOid.isDefined && !hakukohdeOidPattern.matches(hakukohdeOid.get))
      Set(VALIDATION_HAKUKOHDEOID_EI_VALIDI+hakukohdeOid.get)
    else
      Set.empty
  }
  
  def validateSuoritus(suoritus: Suoritus, oppijanumeroPakollinen: Boolean): Set[String] =
    Set(
      validateOppijanumero(suoritus.oppijaNumero.toScala, oppijanumeroPakollinen)
    ).flatten
}
