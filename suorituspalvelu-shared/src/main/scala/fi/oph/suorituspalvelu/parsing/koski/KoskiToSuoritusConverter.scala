package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.{AmmatillinenTutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, Arvosana, NuortenPerusopetuksenOppiaineenOppimaara, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, Telma, Tuva}

import java.time.LocalDate

/**
 * Muuntaa Kosken suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object KoskiToSuoritusConverter {

  def asKoodi(tunniste: VersioituTunniste): String =
    tunniste.koodistoUri + "_" + tunniste.koodiarvo + "#" + tunniste.koodistoVersio

  def asKoodisto(tunniste: VersioituTunniste): String =
    tunniste.koodistoUri + "#" + tunniste.koodistoVersio

  def isYTO(osaSuoritus: OsaSuoritus): Boolean =
    osaSuoritus.koulutusmoduuli.tunniste.koodiarvo match
      case "106727" => true // viestintä- ja vuorovaikutusosaaminen
      case "106728" => true // matemaattis-luonnontieteellinen osaaminen
      case "106729" => true // yhteiskunta- ja työelämäosaaminen
      case default => false

  def toAmmattillisenTutkinnonOsaAlue(osaSuoritus: OsaSuoritus): AmmatillisenTutkinnonOsaAlue =
    val arviointi = {
      val arvioinnit = osaSuoritus.arviointi
        .map(arviointi => arviointi
          .filter(arviointi => arviointi.arvosana.koodistoUri == "arviointiasteikkoammatillinen15"))
        .getOrElse(Set.empty)
      if (arvioinnit.size > 1)
        throw new RuntimeException("liikaa arvosanoja")
      arvioinnit.headOption
    }

    AmmatillisenTutkinnonOsaAlue(
      osaSuoritus.koulutusmoduuli.tunniste.nimi.fi,
      osaSuoritus.koulutusmoduuli.tunniste.koodiarvo,
      asKoodisto(osaSuoritus.koulutusmoduuli.tunniste),
      arviointi.map(arviointi => arviointi.arvosana.nimi.fi),
      arviointi.map(arviointi => asKoodisto(arviointi.arvosana)),
      osaSuoritus.koulutusmoduuli.laajuus.arvo,
      asKoodi(osaSuoritus.koulutusmoduuli.laajuus.yksikkö)
    )

  def toAmmatillisenTutkinnonOsa(osaSuoritus: OsaSuoritus): AmmatillisenTutkinnonOsa =
    val arviointi = {
      val arvioinnit = osaSuoritus.arviointi
        .map(arviointi => arviointi
          .filter(arviointi => arviointi.arvosana.koodistoUri == "arviointiasteikkoammatillinen15"))
        .getOrElse(Set.empty)
      if (arvioinnit.size > 1)
        throw new RuntimeException("liikaa arvosanoja")
      arvioinnit.headOption
    }

    AmmatillisenTutkinnonOsa(
      osaSuoritus.koulutusmoduuli.tunniste.nimi.fi,
      osaSuoritus.koulutusmoduuli.tunniste.koodiarvo,
      asKoodisto(osaSuoritus.koulutusmoduuli.tunniste),
      isYTO(osaSuoritus),
      arviointi.map(arviointi => arviointi.arvosana.nimi.fi),
      arviointi.map(arviointi => asKoodisto(arviointi.arvosana)),
      osaSuoritus.koulutusmoduuli.laajuus.arvo,
      asKoodi(osaSuoritus.koulutusmoduuli.laajuus.yksikkö),
      osaSuoritus.osasuoritukset.map(osaSuoritukset => osaSuoritukset.map(osaSuoritus => toAmmattillisenTutkinnonOsaAlue(osaSuoritus))).getOrElse(Set.empty)
    )

  def toAmmatillinenTutkinto(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): AmmatillinenTutkinto =
    val tila = {
      opiskeluoikeus.tila.opiskeluoikeusjaksot.map(jakso => jakso.tila).last
    }

    AmmatillinenTutkinto(
      suoritus.koulutusmoduuli.tunniste.nimi.fi,
      suoritus.koulutusmoduuli.tunniste.koodiarvo,
      asKoodisto(suoritus.koulutusmoduuli.tunniste),
      tila.koodiarvo,
      asKoodisto(tila),
      suoritus.vahvistuspäivä.map(p => LocalDate.parse(p)),
      suoritus.keskiarvo,
      suoritus.suoritustapa.map(suoritusTapa => suoritusTapa.koodiarvo).get,
      suoritus.suoritustapa.map(suoritusTapa => asKoodisto(suoritusTapa)).get,
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
    opiskeluoikeudet.map(opiskeluoikeus => opiskeluoikeus.suoritukset.map(suoritus =>
      suoritus match
        case suoritus if suoritus.tyyppi.koodiarvo=="ammatillinentutkinto" => Some(toAmmatillinenTutkinto(opiskeluoikeus, suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="aikuistenperusopetuksenoppimaara" => Some(toAikuistenPerusopetuksenOppimaara(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="perusopetuksenoppimaara" => Some(toPerusopetuksenOppimaara(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="perusopetuksenvuosiluokka" => Some(toPerusopetuksenVuosiluokka(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="nuortenperusopetuksenoppiaineenoppimaara" => Some(toNuortenPerusopetuksenOppiaineenOppimaara(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="telma" => Some(toTelma(suoritus))
        case suoritus if suoritus.tyyppi.koodiarvo=="tuvakoulutuksensuoritus" => Some(toTuva(suoritus))
        case default => None
    ).flatten).flatten
}
