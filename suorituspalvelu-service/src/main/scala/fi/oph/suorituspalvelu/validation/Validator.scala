package fi.oph.suorituspalvelu.validation

import java.time.Instant
import scala.util.matching.Regex

/**
 * Validoi järjestelmään syötetyn suorituksen kentät. Validaattorin virheilmoitukset eivät saa sisältää sensitiivistä
 * tietoa koska ne menevät mm. lokeille.
 */
object Validator {

  final val VALIDATION_OPPIJANUMERO_TYHJA         = "oppijaNumero: Kenttä on pakollinen"
  final val VALIDATION_OPPIJANUMERO_EI_VALIDI     = "oppijaNumero: Oppijanumero ei ole validi oppija oid"
  final val VALIDATION_HAKUOID_TYHJA              = "hakuOid: Kenttä on pakollinen"
  final val VALIDATION_HAKUOID_EI_VALIDI          = "hakuOid ei ole validi: "
  final val VALIDATION_HAKUKOHDEOID_TYHJA         = "hakukohdeOid: Kenttä on pakollinen"
  final val VALIDATION_HAKUKOHDEOID_EI_VALIDI     = "hakukohdeOid ei ole validi: "
  final val VALIDATION_OPPILAITOSOID_TYHJA        = "oppilaitosOid: Kenttä on pakollinen"
  final val VALIDATION_OPPILAITOSOID_EI_VALIDI    = "oppilaitosOid ei ole validi: "
  final val VALIDATION_VUOSI_TYHJA                = "vuosi: Kenttä on pakollinen"
  final val VALIDATION_VUOSI_EI_VALIDI            = "vuosi ei ole validi: "
  final val VALIDATION_LUOKKA_TYHJA               = "luokka: Kenttä on pakollinen"
  final val VALIDATION_LUOKKA_EI_VALIDI           = "luokka ei ole validi: "
  final val VALIDATION_EI_VALIDIT_OIDIT           = "Seuraavat oppijanumerot eivät ole valideja: "
  final val VALIDATION_MUOKATTUJALKEEN_TYHJA      = "muokattuJalkeen: Kenttä on pakollinen"
  final val VALIDATION_MUOKATTUJALKEEN_EI_VALIDI  = "muokattuJalkeen: muokattuJalkeen ei ole validi aikaleima"

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
}
