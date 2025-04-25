package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.{AmmatillinenTutkinto, AmmatillisenTutkinnonOsa, Arvosana, PerusopetuksenOppiaine, NuortenPerusopetuksenOppiaineenOppimaara, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, Telma, Tuva}

import java.time.LocalDate

/**
 * Muuntaa Kosken suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object KoskiToSuoritusConverter {

  def asKoodi(tunniste: VersioituTunniste): String =
    tunniste.koodistoUri + ":" + tunniste.koodistoVersio + ":" + tunniste.koodiarvo

  def toAmmatillisenTutkinnonOsa(osaSuoritus: OsaSuoritus): AmmatillisenTutkinnonOsa =
    AmmatillisenTutkinnonOsa(
      osaSuoritus.koulutusmoduuli.tunniste.nimi.fi,
      asKoodi(osaSuoritus.koulutusmoduuli.tunniste),
      {
        val arvosanat = osaSuoritus.arviointi
          .map(arviointi => arviointi
            .filter(arviointi => arviointi.arvosana.koodistoUri=="arviointiasteikkoammatillinen15")
            .map(arviointi => arviointi.arvosana.nimi.fi))
          .getOrElse(Set.empty)
        if(arvosanat.size>1)
          throw new RuntimeException("liikaa arvosanoja")
        arvosanat.headOption
      }
    )

  def toAmmatillinenTutkinto(suoritus: Suoritus): AmmatillinenTutkinto =
    AmmatillinenTutkinto(
      suoritus.koulutusmoduuli.tunniste.nimi.fi,
      asKoodi(suoritus.koulutusmoduuli.tunniste),
      suoritus.vahvistuspäivä.map(p => LocalDate.parse(p)),
      suoritus.osasuoritukset.map(os => os.map(os => toAmmatillisenTutkinnonOsa(os))).getOrElse(Set.empty)
    )

  def toPerusopetuksenOppiaine(osaSuoritus: OsaSuoritus): PerusopetuksenOppiaine =
    PerusopetuksenOppiaine(
      osaSuoritus.koulutusmoduuli.tunniste.nimi.fi,
      asKoodi(osaSuoritus.koulutusmoduuli.tunniste),
      {
        val arvosanat = osaSuoritus.arviointi
          .map(arviointi => arviointi
            .filter(arviointi => arviointi.arvosana.koodistoUri=="arviointiasteikkoyleissivistava")
            .map(arviointi => arviointi.arvosana.koodiarvo))
          .getOrElse(Set.empty)
        if(arvosanat.size>1)
          throw new RuntimeException("liikaa arvosanoja")
        arvosanat.head
      }
    )

  def toNuortenPerusopetuksenOppiaineenOppimaara(suoritus: Suoritus): NuortenPerusopetuksenOppiaineenOppimaara =
    NuortenPerusopetuksenOppiaineenOppimaara(
      suoritus.koulutusmoduuli.tunniste.nimi.fi,
      asKoodi(suoritus.koulutusmoduuli.tunniste),
      {
        val arvosanat = suoritus.arviointi
          .map(arviointi => arviointi
            .filter(arviointi => arviointi.arvosana.koodistoUri=="arviointiasteikkoyleissivistava")
            .map(arviointi => arviointi.arvosana.koodiarvo))
          .getOrElse(Set.empty)
        if(arvosanat.size>1)
          throw new RuntimeException("liikaa arvosanoja")
        arvosanat.head
      }
    )

  def toPerusopetuksenOppimaara(suoritus: Suoritus): PerusopetuksenOppimaara =
    PerusopetuksenOppimaara(
      suoritus.vahvistuspäivä.map(p => LocalDate.parse(p)),
      suoritus.osasuoritukset.map(os => os.map(os => toPerusopetuksenOppiaine(os))).getOrElse(Set.empty)
    )

  def toAikuistenPerusopetuksenOppimaara(suoritus: Suoritus): PerusopetuksenOppimaara =
    PerusopetuksenOppimaara(
      suoritus.vahvistuspäivä.map(p => LocalDate.parse(p)),
      suoritus.osasuoritukset.map(os => os.map(os => toPerusopetuksenOppiaine(os))).getOrElse(Set.empty)
    )

  def toPerusopetuksenVuosiluokka(suoritus: Suoritus): PerusopetuksenVuosiluokka =
    PerusopetuksenVuosiluokka(
      suoritus.koulutusmoduuli.tunniste.nimi.fi,
      asKoodi(suoritus.koulutusmoduuli.tunniste)
    )

  def toTelma(suoritus: Suoritus): Telma =
    Telma(
      asKoodi(suoritus.koulutusmoduuli.tunniste)
    )

  def toTuva(suoritus: Suoritus): Tuva =
    Tuva(
      asKoodi(suoritus.koulutusmoduuli.tunniste),
      suoritus.vahvistuspäivä.map(p => LocalDate.parse(p))
    )

  def toSuoritus(opiskeluoikeudet: Seq[Opiskeluoikeus]): Seq[fi.oph.suorituspalvelu.business.Suoritus] =
    opiskeluoikeudet.map(oo => oo.suoritukset.map(suoritus =>
      suoritus match
        case suoritus if suoritus.tyyppi.koodiarvo=="ammatillinentutkinto" => Some(toAmmatillinenTutkinto(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="aikuistenperusopetuksenoppimaara" => Some(toAikuistenPerusopetuksenOppimaara(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="perusopetuksenoppimaara" => Some(toPerusopetuksenOppimaara(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="perusopetuksenvuosiluokka" => Some(toPerusopetuksenVuosiluokka(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="nuortenperusopetuksenoppiaineenoppimaara" => Some(toNuortenPerusopetuksenOppiaineenOppimaara(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="telma" => Some(toTelma(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="tuvakoulutuksensuoritus" => Some(toTuva(suoritus))
        case default => None
    ).flatten).flatten
}
