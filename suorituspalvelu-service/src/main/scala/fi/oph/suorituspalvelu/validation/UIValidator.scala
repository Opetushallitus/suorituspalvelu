package fi.oph.suorituspalvelu.validation

import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, HarkinnanvaraisuudenSyy}
import fi.oph.suorituspalvelu.resource.ui.{SuoritusTila, SyotettyPerusopetuksenOppiaine, SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer, SyotettyPerusopetuksenOppimaaranSuoritus, YliajoTallennusContainer}
import fi.oph.suorituspalvelu.service.UIService.*
import fi.oph.suorituspalvelu.util.KoodistoProvider
import fi.oph.suorituspalvelu.validation.Validator.{hakuOidPattern, hetuPattern, oppijaOidPattern, oppilaitosOidPattern}

import java.time.{Instant, LocalDate}
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
  final val VALIDATION_HAKUOID_TYHJA              = "backend-virhe.hakuoid.tyhja"
  final val VALIDATION_HAKUOID_EI_VALIDI          = "backend-virhe.hakuoid.ei_validi"
  final val VALIDATION_TUNNISTE_TYHJA             = "backend-virhe.tunniste.tyhja"
  final val VALIDATION_TUNNISTE_EI_VALIDI         = "backend-virhe.tunniste.ei_validi"
  final val VALIDATION_VUOSI_TYHJA                = "backend-virhe.vuosi.tyhja"
  final val VALIDATION_VUOSI_EI_VALIDI            = "backend-virhe.vuosi.ei_validi"
  final val VALIDATION_LUOKKA_TYHJA               = "backend-virhe.luokka.tyhja"
  final val VALIDATION_LUOKKA_EI_VALIDI           = "backend-virhe.luokka.ei_validi"
  final val VALIDATION_TILA_TYHJA                 = "backend-virhe.tila.tyhja"
  final val VALIDATION_TILA_EI_VALIDI             = "backend-virhe.tila.ei_validi"
  final val VALIDATION_VALMISTUMISPAIVA_TYHJA     = "backend-virhe.valmistumispaiva.tyhja"
  final val VALIDATION_VALMISTUMISPAIVA_EI_VALIDI = "backend-virhe.valmistumispaiva.ei_validi"
  final val VALIDATION_AIKALEIMA_TYHJA            = "backend-virhe.ajanhetki.tyhja"
  final val VALIDATION_AIKALEIMA_EI_VALIDI        = "backend-virhe.ajanhetki.ei_validi"
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
  final val VALIDATION_AVAIN_EI_SALLITTU          = "backend-virhe.avain.ei_sallittu"
  final val VALIDATION_ARVO_TYHJA                 = "backend-virhe.arvo.tyhja"
  final val VALIDATION_ARVO_EI_VALIDI             = "backend-virhe.arvo.ei_validi"
  final val VALIDATION_SELITE_TYHJA               = "backend-virhe.selite.tyhja"
  final val VALIDATION_SELITE_EI_VALIDI           = "backend-virhe.selite.ei_validi"
  final val VALIDATION_HAKEMUSOID_TYHJA           = "backend-virhe.hakemusoid.tyhja"
  final val VALIDATION_HAKEMUSOID_EI_VALIDI       = "backend-virhe.hakemusoid.ei_validi"
  final val VALIDATION_HAKUKOHDEOID_TYHJA         = "backend-virhe.hakukohdeoid.tyhja"
  final val VALIDATION_HAKUKOHDEOID_EI_VALIDI     = "backend-virhe.hakukohdeoid.ei_validi"
  final val VALIDATION_HARKINNANVARAISUUDEN_SYY_TYHJA = "backend-virhe.harkinnanvaraisuuden_syy.tyhja"
  final val VALIDATION_HARKINNANVARAISUUDEN_SYY_EI_VALIDI = "backend-virhe.harkinnanvaraisuuden_syy.ei_validi"

  val oppilaitosOidPattern: Regex = "^1\\.2\\.246\\.562\\.10\\.\\d+$".r
  val hakuOidPattern: Regex = "^1\\.2\\.246\\.562\\.29\\.\\d+$".r
  val hakemusOidPattern: Regex = "^1\\.2\\.246\\.562\\.11\\.\\d+$".r
  val hakukohdeOidPattern: Regex = "^1\\.2\\.246\\.562\\.20\\.\\d+$".r

  //Yliajojen avamissa ja arvoissa vain kirjaimia, numeroita ja alaviivoja.
  val avainArvoStringPattern: Regex = "^[a-zA-Z0-9_]*$".r

  val yliajoSeliteStringPattern: Regex = "^[a-zA-ZåäöÅÄÖ\\d _.,:]*$".r

  val vuosiPattern: Regex = "^20[0-9][0-9]$".r
  val luokkaPattern: Regex = "^[a-zA-ZåäöÅÄÖ\\d \\-_]+$".r

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
    if (oppijaNumero.isEmpty || oppijaNumero.get.isEmpty)
      if(pakollinen)
        Set(VALIDATION_OPPIJANUMERO_TYHJA)
      else
        Set.empty
    else if(!Validator.oppijaOidPattern.matches(oppijaNumero.get))
      Set(VALIDATION_OPPIJANUMERO_EI_VALIDI)
    else
      Set.empty

  def validateAikaleima(aikaleima: Option[String], pakollinen: Boolean): Set[String] = {
    if (aikaleima.isEmpty || aikaleima.get.isEmpty)
      if(pakollinen)
        Set(VALIDATION_AIKALEIMA_TYHJA)
      else
        Set.empty
    else
      try
        Instant.parse(aikaleima.get)
        Set.empty
      catch
        case default => Set(VALIDATION_AIKALEIMA_EI_VALIDI)
  }

  def validateTunniste(tunniste: Option[String]): Set[String] = {
    if (tunniste.isEmpty || tunniste.get.isEmpty)
      Set(VALIDATION_TUNNISTE_TYHJA)
    else if (!hetuPattern.matches(tunniste.get) && !oppijaOidPattern.matches(tunniste.get))
      Set(VALIDATION_TUNNISTE_EI_VALIDI)
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

  def validatePerusopetuksenOppiaineenOppimaarat(suoritus: SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer, koodistoProvider: KoodistoProvider): Set[String] = {
    Set(
      validateOppijanumero(suoritus.oppijaOid.toScala, true),
      validateOppilaitosOid(suoritus.oppilaitosOid.toScala, true),
      validateValmistumisPaiva(suoritus.valmistumispaiva.toScala),
      validatePerusopetuksenOppimaaranSuorituskieli(suoritus.suorituskieli.toScala),
      validatePerusopetuksenOppimaaranYksilollistaminen(suoritus.yksilollistetty.toScala),
      suoritus.oppiaineet.asScala.flatMap(o => validatePerusopetuksenOppimaaranOppiaine(Some(o), koodistoProvider))
    ).flatten
  }

  def validateSelite(selite: Option[String], pakollinen: Boolean): Set[String] = {
    if (selite.isEmpty || selite.exists(_.isEmpty))
      if (pakollinen)
        Set(VALIDATION_SELITE_TYHJA)
      else
        Set.empty
    else if (!yliajoSeliteStringPattern.matches(selite.get))
      Set(VALIDATION_SELITE_EI_VALIDI)
    else
      Set.empty
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
    else if (!avainArvoStringPattern.matches(avain.get)) {
      Set(VALIDATION_AVAIN_EI_VALIDI)
    } else {
      //Vain sellaisia arvoja voi yliajaa, joita AvainArvoConverter tuottaa
      if (!AvainArvoConstants.avainToAvaimenSeliteMap.keySet.contains(avain.get))
        Set(VALIDATION_AVAIN_EI_SALLITTU)
      else
        Set.empty
    }
  }

  def validateHakuOid(hakuOid: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (hakuOid.isEmpty || hakuOid.get.isEmpty))
      Set(VALIDATION_HAKUOID_TYHJA)
    else if (hakuOid.isDefined && !hakuOidPattern.matches(hakuOid.get))
      Set(VALIDATION_HAKUOID_EI_VALIDI)
    else
      Set.empty
  }

  def validateYliajot(container: YliajoTallennusContainer): Set[String] = {
    val yliajot = container.yliajot.toScala.map(_.asScala).getOrElse(List.empty)
    val avainErrors = yliajot.flatMap(y => validateAvain(y.avain.toScala, true))
    val arvoErrors = yliajot.flatMap(y => validateArvo(y.arvo.toScala, true))
    val seliteErrors = yliajot.flatMap(y => validateSelite(y.selite.toScala, false))
    val containerErrors = Set(
      validateOppijanumero(container.henkiloOid.toScala, true),
      validateHakuOid(container.hakuOid.toScala, true)
    ).flatten
    containerErrors ++ avainErrors ++ arvoErrors
  }

  def validateHakemusOid(hakemusOid: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (hakemusOid.isEmpty || hakemusOid.exists(_.isEmpty)))
      Set(VALIDATION_HAKEMUSOID_TYHJA)
    else if (hakemusOid.isDefined && !hakemusOidPattern.matches(hakemusOid.get))
      Set(VALIDATION_HAKEMUSOID_EI_VALIDI)
    else
      Set.empty
  }

  def validateHakukohdeOid(hakukohdeOid: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (hakukohdeOid.isEmpty || hakukohdeOid.exists(_.isEmpty)))
      Set(VALIDATION_HAKUKOHDEOID_TYHJA)
    else if (hakukohdeOid.isDefined && !hakukohdeOidPattern.matches(hakukohdeOid.get))
      Set(VALIDATION_HAKUKOHDEOID_EI_VALIDI)
    else
      Set.empty
  }

  def validateHarkinnanvaraisuudenSyy(syy: Option[String], pakollinen: Boolean): Set[String] = {
    if (pakollinen && (syy.isEmpty || syy.exists(_.isEmpty)))
      Set(VALIDATION_HARKINNANVARAISUUDEN_SYY_TYHJA)
    else if (syy.isDefined && !HarkinnanvaraisuudenSyy.values.map(_.toString).contains(syy.get))
      Set(VALIDATION_HARKINNANVARAISUUDEN_SYY_EI_VALIDI)
    else
      Set.empty
  }

  def validateHarkinnanvaraisuusYliajot(container: fi.oph.suorituspalvelu.resource.ui.HarkinnanvaraisuusYliajoTallennusContainer): Set[String] = {
    val yliajot = container.yliajot.toScala.map(_.asScala).getOrElse(List.empty)
    val hakemusOidErrors = yliajot.flatMap(y => validateHakemusOid(y.hakemusOid.toScala, true))
    val hakukohdeOidErrors = yliajot.flatMap(y => validateHakukohdeOid(y.hakukohdeOid.toScala, true))
    val harkinnanvaraisuudenSyyErrors = yliajot.flatMap(y => validateHarkinnanvaraisuudenSyy(y.harkinnanvaraisuudenSyy.toScala, true))
    val seliteErrors = yliajot.flatMap(y => validateSelite(y.selite.toScala, true))
    (hakemusOidErrors ++ hakukohdeOidErrors ++ harkinnanvaraisuudenSyyErrors ++ seliteErrors).toSet
  }
}
