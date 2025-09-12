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
  val oppilaitosOidPattern: Regex = "^1\\.2\\.246\\.562\\.10\\.\\d+$".r

  val vuosiPattern: Regex = "^20[0-9][0-9]$".r
  val luokkaPattern: Regex = "^[0-9][A-Z]$".r

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

  def validateOppilaitosOid(oppilaitosOid: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (oppilaitosOid.isEmpty || oppilaitosOid.get.isEmpty))
      Set(VALIDATION_OPPILAITOSOID_TYHJA)
    else if (oppilaitosOid.isDefined && !oppilaitosOidPattern.matches(oppilaitosOid.get))
      Set(VALIDATION_OPPILAITOSOID_EI_VALIDI+oppilaitosOid.get)
    else
      Set.empty
  }

  def validateVuosi(vuosi: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (vuosi.isEmpty || vuosi.get.isEmpty))
      Set(VALIDATION_VUOSI_TYHJA)
    else if (vuosi.isDefined && !vuosiPattern.matches(vuosi.get))
      Set(VALIDATION_VUOSI_EI_VALIDI+vuosi.get)
    else
      Set.empty
  }

  def validateLuokka(luokka: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (luokka.isEmpty || luokka.get.isEmpty))
      Set(VALIDATION_LUOKKA_TYHJA)
    else if (luokka.isDefined && !luokkaPattern.matches(luokka.get))
      Set(VALIDATION_LUOKKA_EI_VALIDI+luokka.get)
    else
      Set.empty
  }
}
