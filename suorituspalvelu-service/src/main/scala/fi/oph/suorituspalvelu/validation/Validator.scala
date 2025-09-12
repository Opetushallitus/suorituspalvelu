package fi.oph.suorituspalvelu.validation

import fi.oph.suorituspalvelu.resource.ui.{SyotettyPeruskoulunOppiaine, SyotettyPeruskoulunOppimaaranSuoritus}
import fi.oph.suorituspalvelu.ui.UIService.{KOODISTO_KIELIVALIKOIMA, KOODISTO_OPPIAINE_AIDINKIELI_JA_KIRJALLISUUS, SYOTETTAVAT_OPPIAINEET, SYOTETYN_OPPIMAARAN_KIELIAINEKOODIT, SYOTETYN_OPPIMAARAN_SUORITUSKIELET, SYOTETYN_OPPIMAARAN_YKSILOLLISTAMINEN}
import fi.oph.suorituspalvelu.util.KoodistoProvider

import java.time.{Instant, LocalDate}
import java.util.Optional
import scala.util.matching.Regex
import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*

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
  final val VALIDATION_VALMISTUMISPAIVA_TYHJA     = "valmistumisPaiva: Kenttä on pakollinen"
  final val VALIDATION_VALMISTUMISPAIVA_EI_VALIDI = "valmistumisPaiva: valmistumisPaiva ei ole validi päivämäärä: "
  final val VALIDATION_YKSILOLLISTETTY_TYHJA      = "yksilollistetty: Kenttä on pakollinen"
  final val VALIDATION_YKSILOLLISTETTY_EI_VALIDI  = "yksilollistetty: Kenttä ei ole sallittu 2asteenpohjakoulutus2021-koodiston koodi: "
  final val VALIDATION_SUORITUSKIELI_TYHJA        = "suoritusKieli: Kenttä on pakollinen"
  final val VALIDATION_SUORITUSKIELI_EI_VALIDI    = "suoritusKieli: arvo ei ole validi syötettävän oppimäärän kielikoodi: "
  final val VALIDATION_OPPIAINEET_TYHJA           = "oppiaineet: Kenttä on pakollinen"
  final val VALIDATION_OPPIAINEET_KOODI_TYHJA     = "oppiaineet: Osalla oppiaineista koodi on tyhjä"
  final val VALIDATION_KOODI_TYHJA                = "Koodi on pakollinen"
  final val VALIDATION_KOODI_EI_VALIDI            = "Koodi ei ole validi syotettävän oppimäärän oppiainekoodi: "
  final val VALIDATION_KIELI_MAARITELTY           = "Kieli on sallittu kenttä vain kieliaineissa"
  final val VALIDATION_KIELI_EI_MAARITELTY        = "Kieli on pakollinen kenttä kieliaineissa"
  final val VALIDATION_KIELI_INVALID              = "Kieli ei ole validi kielivalikoima-koodiston arvo"
  final val VALIDATION_VALINNAINEN_EI_MAARITELTY  = "Valinnaisuus-kenttä on pakollinen"
  final val VALIDATION_AI_OPPIMAARA_MAARITELTY    = "Äidinkielen oppimäärä on sallittu vain äidinkielessä"
  final val VALIDATION_AI_OPPIMAARA_EI_MAARITELTY = "Äidinkielen oppimäärä on pakollinen äidinkielelle"
  final val VALIDATION_AI_OPPIMAARA_EI_VALIDI     = "Äidinkielen oppimäärä ei ole validi oppiaineaidinkielijakirjallisuus-koodiston koodi"
  final val VALIDATION_ARVOSANA_TYHJA             = "Arvosana on pakollinen"
  final val VALIDATION_ARVOSANA_EI_VALIDI         = "Arvosana ei ole validi numeerinen arviointi (4-10)"

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

  def validateValmistumisPaiva(valmistumisPaiva: Option[String]): Set[String] = {
    if (valmistumisPaiva.isEmpty || valmistumisPaiva.get.isEmpty)
      Set(VALIDATION_VALMISTUMISPAIVA_TYHJA)
    else
      try
        LocalDate.parse(valmistumisPaiva.get)
        Set.empty
      catch
        case default => Set(VALIDATION_VALMISTUMISPAIVA_EI_VALIDI + valmistumisPaiva.get)
  }

  def validatePeruskoulunOppimaaranSuorituskieli(suorituskieli: Option[String]): Set[String] = {
    if(suorituskieli.isEmpty || suorituskieli.get.isEmpty)
      Set(VALIDATION_SUORITUSKIELI_TYHJA)
    else if(!SYOTETYN_OPPIMAARAN_SUORITUSKIELET.contains(suorituskieli.get))
      Set(VALIDATION_SUORITUSKIELI_EI_VALIDI + suorituskieli.get)
    else
      Set.empty
  }

  def validatePeruskoulunOppimaaranYksilollistaminen(yksilollistetty: Option[Int]): Set[String] = {
    if(yksilollistetty.isEmpty)
      Set(VALIDATION_YKSILOLLISTETTY_TYHJA)
    else if(!SYOTETYN_OPPIMAARAN_YKSILOLLISTAMINEN.contains(yksilollistetty.get))
      Set(VALIDATION_YKSILOLLISTETTY_EI_VALIDI + yksilollistetty.get)
    else
      Set.empty
  }

  def validatePeruskoulunOppimaaranOppiaineet(oppiaineet: Option[List[SyotettyPeruskoulunOppiaine]]): Set[String] = {
    if (oppiaineet.isEmpty)
      Set(VALIDATION_OPPIAINEET_TYHJA)
    else if (oppiaineet.map(oat => oat.exists(oa => oa.koodi.isEmpty)).getOrElse(false))
      Set(VALIDATION_OPPIAINEET_KOODI_TYHJA)
    else
      Set.empty
  }

  def validatePeruskoulunOppimaaranYleisetKentat(suoritus: SyotettyPeruskoulunOppimaaranSuoritus, koodistoProvider: KoodistoProvider): Set[String] = {
    Set(
      validateOppijanumero(suoritus.oppijaOid.toScala, true),
      validateOppilaitosOid(suoritus.oppilaitosOid.toScala, true),
      validateValmistumisPaiva(suoritus.valmistumispaiva.toScala),
      validatePeruskoulunOppimaaranSuorituskieli(suoritus.suorituskieli.toScala),
      validatePeruskoulunOppimaaranYksilollistaminen(suoritus.yksilollistetty.toScala),
      validatePeruskoulunOppimaaranOppiaineet(suoritus.oppiaineet.toScala.map(oat => oat.asScala.toList))
    ).flatten
  }

  def validatePeruskoulunOppimaaranOppiaineenArvosana(arvosana: Option[Int]): Set[String] = {
    if(arvosana.isEmpty)
      Set(VALIDATION_ARVOSANA_TYHJA)
    else if(arvosana.get>10 || arvosana.get<4)
      Set(VALIDATION_ARVOSANA_EI_VALIDI)
    else
      Set.empty
  }

  def validatePeruskoulunOppimaaranOppiaineenKieli(oppiaine: SyotettyPeruskoulunOppiaine, koodistoProvider: KoodistoProvider): Set[String] = {
    if(oppiaine.kieli.isPresent)
      if(oppiaine.koodi.isEmpty || !SYOTETYN_OPPIMAARAN_KIELIAINEKOODIT.contains(oppiaine.koodi.get()))
        Set(VALIDATION_KIELI_MAARITELTY)
      else if(koodistoProvider.haeKoodisto(KOODISTO_KIELIVALIKOIMA).get(oppiaine.kieli.get()).isEmpty)
        Set(VALIDATION_KIELI_INVALID)
      else
        Set.empty
    else
      if(oppiaine.koodi.isPresent && SYOTETYN_OPPIMAARAN_KIELIAINEKOODIT.contains(oppiaine.koodi.get()))
        Set(VALIDATION_KIELI_EI_MAARITELTY)
      else
        Set.empty
  }

  def validatePeruskoulunOppimaaranOppiaineenValinnainen(valinnainen: Option[Boolean]): Set[String] = {
    if(valinnainen.isEmpty)
      Set(VALIDATION_VALINNAINEN_EI_MAARITELTY)
    else
      Set.empty
  }

  def validatePeruskoulunOppimaaranOppiaineenAidinkielenOppimaara(oppiaine: SyotettyPeruskoulunOppiaine, koodistoProvider: KoodistoProvider): Set[String] = {
    if(oppiaine.aidinkielenOppimaara.isPresent)
      if(oppiaine.koodi.isEmpty || !"AI".equals(oppiaine.koodi.get()))
        Set(VALIDATION_AI_OPPIMAARA_MAARITELTY)
      else if(koodistoProvider.haeKoodisto(KOODISTO_OPPIAINE_AIDINKIELI_JA_KIRJALLISUUS).get(oppiaine.aidinkielenOppimaara.get()).isEmpty)
        Set(VALIDATION_AI_OPPIMAARA_EI_VALIDI)
      else
        Set.empty
    else
      if(oppiaine.koodi.isPresent && "AI".equals(oppiaine.koodi.get()))
        Set(VALIDATION_AI_OPPIMAARA_EI_MAARITELTY)
      else
        Set.empty
  }

  def validatePeruskoulunOppimaaranOppiaine(oppiaine: SyotettyPeruskoulunOppiaine, koodistoProvider: KoodistoProvider): Set[String] = {
    Set(
      validatePeruskoulunOppimaaranOppiaineenArvosana(oppiaine.arvosana.toScala),
      validatePeruskoulunOppimaaranOppiaineenKieli(oppiaine, koodistoProvider),
      validatePeruskoulunOppimaaranOppiaineenValinnainen(oppiaine.valinnainen.toScala),
      validatePeruskoulunOppimaaranOppiaineenAidinkielenOppimaara(oppiaine, koodistoProvider)
    ).flatten
  }

  def validatePeruskoulunOppimaaranYksittaisetOppiaineet(oppiaineet: Optional[java.util.List[SyotettyPeruskoulunOppiaine]], koodistoProvider: KoodistoProvider): Map[String, Set[String]] = {
    if(oppiaineet.isEmpty)
      Map.empty
    else
      oppiaineet.get().asScala
        .filter(oa => oa.koodi.toScala.isDefined)
        .map(oa => oa.koodi.get() -> validatePeruskoulunOppimaaranOppiaine(oa, koodistoProvider))
        .filter(oa => oa._2.nonEmpty)
        .toMap
  }


}
