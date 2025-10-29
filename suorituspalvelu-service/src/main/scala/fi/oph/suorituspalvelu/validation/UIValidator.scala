package fi.oph.suorituspalvelu.validation

import fi.oph.suorituspalvelu.resource.ui.{SuoritusTila, SyotettyPerusopetuksenOppiaine, SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, SyotettyPerusopetuksenOppimaaranSuoritus, YliajoTallennusContainer}
import fi.oph.suorituspalvelu.ui.UIService.*
import fi.oph.suorituspalvelu.util.KoodistoProvider
import fi.oph.suorituspalvelu.validation.Validator.{hetuPattern, oppijaOidPattern, validateHakuOid}

import java.time.LocalDate
import java.util.{Optional, UUID}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.util.matching.Regex

/**
 * Validoi järjestelmään syötetyn suorituksen kentät. Validaattorin virheilmoitukset eivät saa sisältää sensitiivistä
 * tietoa koska ne menevät mm. lokeille.
 */
object UIValidator {

  final val VALIDATION_OPPIJANUMERO_TYHJA         = "backend-virhe.oppijanumero.tyhja"
  final val VALIDATION_OPPIJANUMERO_EI_VALIDI     = "backend-virhe.oppijanumero.ei_validi"
  final val VALIDATION_OPPILAITOSOID_TYHJA        = "backend-virhe.oppilaitosoid.tyhja"
  final val VALIDATION_OPPILAITOSOID_EI_VALIDI    = "backend-virhe.oppilaitosoid.ei_validi"
  final val VALIDATION_HAKUSANA_TYHJA             = "backend-virhe.hakusana.tyhja"
  final val VALIDATION_HAKUSANA_EI_VALIDI         = "backend-virhe.hakusana.ei_validi"
  final val VALIDATION_VUOSI_TYHJA                = "backend-virhe.vuosi.tyhja"
  final val VALIDATION_VUOSI_EI_VALIDI            = "backend-virhe.vuosi.ei_validi"
  final val VALIDATION_LUOKKA_TYHJA               = "backend-virhe.luokka.tyhja"
  final val VALIDATION_LUOKKA_EI_VALIDI           = "backend-virhe.luokka.ei_validi"
  final val VALIDATION_TILA_TYHJA                 = "backend-virhe.tila.tyhja"
  final val VALIDATION_TILA_EI_VALIDI             = "backend-virhe.tila.ei_validi"
  final val VALIDATION_VALMISTUMISPAIVA_TYHJA     = "backend-virhe.valmistumispaiva.tyhja"
  final val VALIDATION_VALMISTUMISPAIVA_EI_VALIDI = "backend-virhe.valmistumispaiva.ei_validi"
  final val VALIDATION_YKSILOLLISTETTY_TYHJA      = "backend-virhe.yksilollistetty.tyhja"
  final val VALIDATION_YKSILOLLISTETTY_EI_VALIDI  = "backend-virhe.yksilollistetty.ei_validi"
  final val VALIDATION_SUORITUSKIELI_TYHJA        = "backend-virhe.suorituskieli.tyhja"
  final val VALIDATION_SUORITUSKIELI_EI_VALIDI    = "backend-virhe.suorituskieli.ei_validi"
  final val VALIDATION_OPPIAINE_TYHJA             = "backend-virhe.oppiaine.tyhja"
  final val VALIDATION_OPPIAINEET_TYHJA           = "backend-virhe.oppiaineet.tyhja"
  final val VALIDATION_OPPIAINE_KOODI_TYHJA       = "backend-virhe.oppiaine.koodi_tyhja"
  final val VALIDATION_OPPIAINEET_KOODI_TYHJA     = "backend-virhe.oppiaineet.koodi_tyhja"
  final val VALIDATION_KOODI_EI_VALIDI            = "backend-virhe.oppiaine.koodi.ei_validi"
  final val VALIDATION_KIELI_MAARITELTY           = "backend-virhe.oppiaine.kieli.maaritelty"
  final val VALIDATION_KIELI_EI_MAARITELTY        = "backend-virhe.oppiaine.kieli.ei_maaritelty"
  final val VALIDATION_KIELI_EI_VALIDI            = "backend-virhe.oppiaine.kieli.ei_validi"
  final val VALIDATION_VALINNAINEN_EI_MAARITELTY  = "backend-virhe.oppiaine.valinnaisuus.ei_maaritelty"
  final val VALIDATION_AI_OPPIMAARA_EI_VALIDI     = "backend-virhe.oppiaine.ai_oppimaara.ei_validi"
  final val VALIDATION_ARVOSANA_TYHJA             = "backend-virhe.oppiaine.arvosana.tyhja"
  final val VALIDATION_ARVOSANA_EI_VALIDI         = "backend-virhe.oppiaine.arvosana.ei_validi"
  final val VALIDATION_VERSIOTUNNISTE_TYHJA       = "backend-virhe.versiotunniste.tyhja"
  final val VALIDATION_VERSIOTUNNISTE_EI_VALIDI   = "backend-virhe.versiotunniste.ei_validi"
  final val VALIDATION_AVAIN_TYHJA                = "backend-virhe.avain.tyhja"
  final val VALIDATION_AVAIN_EI_VALIDI            = "backend-virhe.avain.ei_validi"
  final val VALIDATION_ARVO_TYHJA                 = "backend-virhe.arvo.tyhja"
  final val VALIDATION_ARVO_EI_VALIDI             = "backend-virhe.arvo.ei_validi"
  final val VALIDATION_HAKUOID_TYHJA              = "backend-virhe.hakuoid.tyhja"
  final val VALIDATION_HAKUOID_EI_VALIDI          = "backend-virhe.hakuoid.ei_validi"

  val oppilaitosOidPattern: Regex = "^1\\.2\\.246\\.562\\.10\\.\\d+$".r
  val hakuOidPattern: Regex = "^1\\.2\\.246\\.562\\.29\\.\\d+$".r

  //Yliajojen avamissa ja arvoissa vain kirjaimia, numeroita ja alaviivoja.
  val avainArvoStringPattern: Regex = "^[a-zA-Z0-9_]*$".r

  val vuosiPattern: Regex = "^20[0-9][0-9]$".r
  val luokkaPattern: Regex = "^[0-9][A-Z]$".r

  def validateVersioTunniste(tunniste: Option[String]): Set[String] =
    if (tunniste.isEmpty)
      Set(VALIDATION_VERSIOTUNNISTE_TYHJA)
    else
      try
        UUID.fromString(tunniste.get)
        Set.empty
      catch
        case default => Set(VALIDATION_VERSIOTUNNISTE_EI_VALIDI)

  def validateOppijanumero(oppijaNumero: Option[String], pakollinen: Boolean): Set[String] =
    if (oppijaNumero.isEmpty || oppijaNumero.get.length == 0)
      if(pakollinen)
        Set(VALIDATION_OPPIJANUMERO_TYHJA)
      else
        Set.empty
    else if(!Validator.oppijaOidPattern.matches(oppijaNumero.get))
      Set(VALIDATION_OPPIJANUMERO_EI_VALIDI)
    else
      Set.empty

  //Tuetaan tässä vaiheessa hetuja ja oppijanumeroita
  def validateHakusana(hakusana: Option[String]): Set[String] = {
    if (hakusana.isEmpty || hakusana.get.isEmpty)
      Set(VALIDATION_HAKUSANA_TYHJA)
    else if (!hetuPattern.matches(hakusana.get) && !oppijaOidPattern.matches(hakusana.get))
      Set(VALIDATION_HAKUSANA_EI_VALIDI)
    else
      Set.empty
  }

  def validateOppilaitosOid(oppilaitosOid: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (oppilaitosOid.isEmpty || oppilaitosOid.get.isEmpty))
      Set(VALIDATION_OPPILAITOSOID_TYHJA)
    else if (oppilaitosOid.isDefined && !oppilaitosOidPattern.matches(oppilaitosOid.get))
      Set(VALIDATION_OPPILAITOSOID_EI_VALIDI)
    else
      Set.empty
  }

  def validateVuosi(vuosi: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (vuosi.isEmpty || vuosi.get.isEmpty))
      Set(VALIDATION_VUOSI_TYHJA)
    else if (vuosi.isDefined && !vuosiPattern.matches(vuosi.get))
      Set(VALIDATION_VUOSI_EI_VALIDI)
    else
      Set.empty
  }

  def validateLuokka(luokka: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (luokka.isEmpty || luokka.get.isEmpty))
      Set(VALIDATION_LUOKKA_TYHJA)
    else if (luokka.isDefined && !luokkaPattern.matches(luokka.get))
      Set(VALIDATION_LUOKKA_EI_VALIDI)
    else
      Set.empty
  }

  def validateTila(tila: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (tila.isEmpty || tila.get.isEmpty))
      Set(VALIDATION_TILA_TYHJA)
    else
      try
        SuoritusTila.valueOf(tila.get)
        Set.empty
      catch
        case default => Set(VALIDATION_TILA_EI_VALIDI)
  }

  def validateValmistumisPaiva(valmistumisPaiva: Option[String]): Set[String] = {
    if (valmistumisPaiva.isEmpty || valmistumisPaiva.get.isEmpty)
      Set(VALIDATION_VALMISTUMISPAIVA_TYHJA)
    else
      try
        LocalDate.parse(valmistumisPaiva.get)
        Set.empty
      catch
        case default => Set(VALIDATION_VALMISTUMISPAIVA_EI_VALIDI)
  }

  def validatePerusopetuksenOppimaaranSuorituskieli(suorituskieli: Option[String]): Set[String] = {
    if(suorituskieli.isEmpty || suorituskieli.get.isEmpty)
      Set(VALIDATION_SUORITUSKIELI_TYHJA)
    else if(!SYOTETYN_OPPIMAARAN_SUORITUSKIELET.contains(suorituskieli.get))
      Set(VALIDATION_SUORITUSKIELI_EI_VALIDI)
    else
      Set.empty
  }

  def validatePerusopetuksenOppimaaranYksilollistaminen(yksilollistetty: Option[Int]): Set[String] = {
    if(yksilollistetty.isEmpty)
      Set(VALIDATION_YKSILOLLISTETTY_TYHJA)
    else if(!SYOTETYN_OPPIMAARAN_YKSILOLLISTAMINEN.contains(yksilollistetty.get))
      Set(VALIDATION_YKSILOLLISTETTY_EI_VALIDI)
    else
      Set.empty
  }

  def validatePerusopetuksenOppimaaranOppiaineet(oppiaineet: Option[List[SyotettyPerusopetuksenOppiaine]]): Set[String] = {
    if (oppiaineet.isEmpty)
      Set(VALIDATION_OPPIAINEET_TYHJA)
    else if (oppiaineet.map(oat => oat.exists(oa => oa.koodi.isEmpty)).getOrElse(false))
      Set(VALIDATION_OPPIAINEET_KOODI_TYHJA)
    else
      Set.empty
  }

  def validatePerusopetuksenOppimaaranYleisetKentat(suoritus: SyotettyPerusopetuksenOppimaaranSuoritus, koodistoProvider: KoodistoProvider): Set[String] = {
    Set(
      validateOppijanumero(suoritus.oppijaOid.toScala, true),
      validateOppilaitosOid(suoritus.oppilaitosOid.toScala, true),
      validateLuokka(suoritus.luokka.toScala, true),
      validateTila(suoritus.tila.toScala, true),
      validateValmistumisPaiva(suoritus.valmistumispaiva.toScala),
      validatePerusopetuksenOppimaaranSuorituskieli(suoritus.suorituskieli.toScala),
      validatePerusopetuksenOppimaaranYksilollistaminen(suoritus.yksilollistetty.toScala),
      validatePerusopetuksenOppimaaranOppiaineet(suoritus.oppiaineet.toScala.map(oat => oat.asScala.toList))
    ).flatten
  }

  def validatePerusopetuksenOppimaaranOppiaineenKoodi(oppiaineKoodi: Option[String]): Set[String] = {
    if(oppiaineKoodi.isEmpty)
      Set(VALIDATION_OPPIAINE_KOODI_TYHJA)
    else if(!SYOTETTAVAT_OPPIAINEET.contains(oppiaineKoodi.get))
      Set(VALIDATION_KOODI_EI_VALIDI)
    else
      Set.empty
  }

  def validatePerusopetuksenOppimaaranOppiaineenArvosana(arvosana: Option[Int]): Set[String] = {
    if(arvosana.isEmpty)
      Set(VALIDATION_ARVOSANA_TYHJA)
    else if(arvosana.get>10 || arvosana.get<4)
      Set(VALIDATION_ARVOSANA_EI_VALIDI)
    else
      Set.empty
  }

  def validatePerusopetuksenOppimaaranOppiaineenKieli(oppiaine: SyotettyPerusopetuksenOppiaine, koodistoProvider: KoodistoProvider): Set[String] = {
    if(oppiaine.kieli.isPresent)
      if(oppiaine.koodi.isEmpty || (!SYOTETYN_OPPIMAARAN_KIELIAINEKOODIT.contains(oppiaine.koodi.get()) && !"AI".equals(oppiaine.koodi.get())))
        Set(VALIDATION_KIELI_MAARITELTY)
      else if(SYOTETYN_OPPIMAARAN_KIELIAINEKOODIT.contains(oppiaine.koodi.get()) && koodistoProvider.haeKoodisto(KOODISTO_KIELIVALIKOIMA).get(oppiaine.kieli.get()).isEmpty)
        Set(VALIDATION_KIELI_EI_VALIDI)
      else if("AI".equals(oppiaine.koodi.get()) && koodistoProvider.haeKoodisto(KOODISTO_OPPIAINE_AIDINKIELI_JA_KIRJALLISUUS).get(oppiaine.kieli.get()).isEmpty)
        Set(VALIDATION_AI_OPPIMAARA_EI_VALIDI)
      else
        Set.empty
    else
      if(oppiaine.koodi.isPresent && (SYOTETYN_OPPIMAARAN_KIELIAINEKOODIT.contains(oppiaine.koodi.get()) || "AI".equals(oppiaine.koodi.get())))
        Set(VALIDATION_KIELI_EI_MAARITELTY)
      else
        Set.empty
  }

  def validatePerusopetuksenOppimaaranOppiaineenValinnainen(valinnainen: Option[Boolean]): Set[String] = {
    if(valinnainen.isEmpty)
      Set(VALIDATION_VALINNAINEN_EI_MAARITELTY)
    else
      Set.empty
  }

  def validatePerusopetuksenOppimaaranOppiaine(oppiaine: Option[SyotettyPerusopetuksenOppiaine], koodistoProvider: KoodistoProvider): Set[String] = {
    if(oppiaine.isEmpty)
      Set(VALIDATION_OPPIAINE_TYHJA)
    else
      Set(
        validatePerusopetuksenOppimaaranOppiaineenKoodi(oppiaine.get.koodi.toScala),
        validatePerusopetuksenOppimaaranOppiaineenArvosana(oppiaine.get.arvosana.toScala),
        validatePerusopetuksenOppimaaranOppiaineenKieli(oppiaine.get, koodistoProvider),
        validatePerusopetuksenOppimaaranOppiaineenValinnainen(oppiaine.get.valinnainen.toScala),
      ).flatten
  }

  def validatePerusopetuksenOppimaaranYksittaisetOppiaineet(oppiaineet: Optional[java.util.List[SyotettyPerusopetuksenOppiaine]], koodistoProvider: KoodistoProvider): Map[String, Set[String]] = {
    if(oppiaineet.isEmpty)
      Map.empty
    else
      oppiaineet.get().asScala
        .filter(oa => oa.koodi.toScala.isDefined)
        .map(oa => oa.koodi.get() -> validatePerusopetuksenOppimaaranOppiaine(Some(oa), koodistoProvider))
        .filter(oa => oa._2.nonEmpty)
        .toMap
  }

  def validatePerusopetuksenOppiaineenOppimaara(suoritus: SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, koodistoProvider: KoodistoProvider): Set[String] = {
    Set(
      validateOppijanumero(suoritus.oppijaOid.toScala, true),
      validateOppilaitosOid(suoritus.oppilaitosOid.toScala, true),
      validateValmistumisPaiva(suoritus.valmistumispaiva.toScala),
      validatePerusopetuksenOppimaaranSuorituskieli(suoritus.suorituskieli.toScala),
      validatePerusopetuksenOppimaaranYksilollistaminen(suoritus.yksilollistetty.toScala),
      validatePerusopetuksenOppimaaranOppiaine(suoritus.oppiaine.toScala, koodistoProvider)
    ).flatten
  }

  def validateArvo(arvo: Option[String], pakollinen: Boolean): Set[String] = {
    if (arvo.isEmpty || arvo.exists(_.isEmpty))
      if (pakollinen)
        Set(VALIDATION_ARVO_TYHJA)
      else
        Set.empty
    else if (!avainArvoStringPattern.matches(arvo.get))
      Set(VALIDATION_ARVO_EI_VALIDI)
    else
      Set.empty
  }

  def validateAvain(avain: Option[String], pakollinen: Boolean): Set[String] = {
    if (avain.isEmpty || avain.exists(_.isEmpty))
      if (pakollinen)
        Set(VALIDATION_AVAIN_TYHJA)
      else
        Set.empty
    else if (!avainArvoStringPattern.matches(avain.get))
      Set(VALIDATION_AVAIN_EI_VALIDI)
    else
      Set.empty
  }

  def validateHakuOid(hakuOid: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (hakuOid.isEmpty || hakuOid.get.isEmpty))
      Set(VALIDATION_HAKUOID_TYHJA)
    else if (hakuOid.isDefined && !hakuOidPattern.matches(hakuOid.get))
      Set(VALIDATION_HAKUOID_EI_VALIDI + hakuOid.get)
    else
      Set.empty
  }

  def validateYliajot(container: YliajoTallennusContainer): Set[String] = {
    val yliajot = container.yliajot.toScala.map(_.asScala).getOrElse(List.empty)
    val avainErrors = yliajot.flatMap(y => validateAvain(y.avain.toScala, true))
    val arvoErrors = yliajot.flatMap(y => validateArvo(y.arvo.toScala, false)) //Todo, onko hyödyllistä voida tallentaa tyhjä arvo?
    val containerErrors = Set(
      validateOppijanumero(container.henkiloOid.toScala, true),
      validateOppijanumero(container.virkailijaOid.toScala, true),
      validateHakuOid(container.hakuOid.toScala, true)
    ).flatten
    containerErrors ++ avainErrors ++ arvoErrors
  }
}
