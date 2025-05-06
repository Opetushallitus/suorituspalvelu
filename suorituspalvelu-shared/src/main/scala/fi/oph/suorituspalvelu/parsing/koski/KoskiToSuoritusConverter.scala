package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.{AmmatillinenTutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, Arvosana, Koodi, NuortenPerusopetuksenOppiaineenOppimaara, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, Telma, Tuva}

import java.time.LocalDate

/**
 * Muuntaa Kosken suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object KoskiToSuoritusConverter {

  val allowMissingFields = new ThreadLocal[Boolean]

  def dummy[A](): A =
    if(allowMissingFields.get())
      null.asInstanceOf[A]
    else
      throw new RuntimeException("Dummies not allowed")

  def asKoodiObject(tunniste: VersioituTunniste): Koodi =
    Koodi(tunniste.koodiarvo, tunniste.koodistoUri, tunniste.koodistoVersio)

  def asKoodi(tunniste: VersioituTunniste): String =
    tunniste.koodistoUri + "_" + tunniste.koodiarvo + "#" + tunniste.koodistoVersio

  def asKoodisto(tunniste: VersioituTunniste): String =
    tunniste.koodistoUri + "#" + tunniste.koodistoVersio

  def isYTO(koodiarvo: String): Boolean =
    koodiarvo match
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
      osaSuoritus.koulutusmoduuli.map(k => k.tunniste.nimi.fi).getOrElse(dummy()),
      osaSuoritus.koulutusmoduuli.map(k => asKoodiObject(k.tunniste)).getOrElse(dummy()),
      arviointi.map(arviointi => asKoodiObject(arviointi.arvosana)),
      osaSuoritus.koulutusmoduuli.map(k => k.laajuus.arvo).getOrElse(dummy()),
      osaSuoritus.koulutusmoduuli.map(k => k.laajuus.yksikkö.map(y => asKoodiObject(y)).getOrElse(dummy())).getOrElse(dummy())
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
      osaSuoritus.koulutusmoduuli.map(k => k.tunniste.nimi.fi).getOrElse(dummy()),
      osaSuoritus.koulutusmoduuli.map(k => asKoodiObject(k.tunniste)).getOrElse(dummy()),
      osaSuoritus.koulutusmoduuli.map(k => isYTO(k.tunniste.koodiarvo)).getOrElse(false),
      arviointi.map(arviointi => asKoodiObject(arviointi.arvosana)),
      osaSuoritus.koulutusmoduuli.map(k => k.laajuus.arvo).getOrElse(dummy()),
      osaSuoritus.koulutusmoduuli.map(k => k.laajuus.yksikkö.map(y => asKoodiObject(y)).getOrElse(dummy())).getOrElse(dummy()),
      osaSuoritus.osasuoritukset.map(osaSuoritukset => osaSuoritukset.map(osaSuoritus => toAmmattillisenTutkinnonOsaAlue(osaSuoritus))).getOrElse(Set.empty)
    )

  def toAmmatillinenTutkinto(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): AmmatillinenTutkinto =
    val tila = {
      opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.tila).last)
    }

    AmmatillinenTutkinto(
      suoritus.koulutusmoduuli.map(km => km.tunniste.nimi.fi).getOrElse(dummy()),
      suoritus.koulutusmoduuli.map(km => asKoodiObject(km.tunniste)).getOrElse(dummy()),
      tila.map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      suoritus.vahvistuspäivä.map(p => LocalDate.parse(p)),
      suoritus.keskiarvo,
      suoritus.suoritustapa.map(suoritusTapa => asKoodiObject(suoritusTapa)).getOrElse(dummy()),
      suoritus.osasuoritukset.map(os => os.map(os => toAmmatillisenTutkinnonOsa(os))).getOrElse(Set.empty)
    )

  def toPerusopetuksenOppiaine(osaSuoritus: OsaSuoritus): PerusopetuksenOppiaine =
    PerusopetuksenOppiaine(
      osaSuoritus.koulutusmoduuli.map(k => k.tunniste.nimi.fi).getOrElse(dummy()),
      osaSuoritus.koulutusmoduuli.map(k => asKoodi(k.tunniste)).getOrElse(dummy()),
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
      suoritus.koulutusmoduuli.map(km => km.tunniste.nimi.fi).get,
      suoritus.koulutusmoduuli.map(km => asKoodi(km.tunniste)).get,
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
      suoritus.koulutusmoduuli.map(km => km.tunniste.nimi.fi).get,
      suoritus.koulutusmoduuli.map(km => asKoodi(km.tunniste)).get
    )

  def toTelma(suoritus: Suoritus): Telma =
    Telma(
      suoritus.koulutusmoduuli.map(km => asKoodi(km.tunniste)).get
    )

  def toTuva(suoritus: Suoritus): Tuva =
    Tuva(
      suoritus.koulutusmoduuli.map(km => asKoodi(km.tunniste)).get,
      suoritus.vahvistuspäivä.map(p => LocalDate.parse(p))
    )

  def toSuoritus(opiskeluoikeudet: Seq[Opiskeluoikeus], allowMissingFieldsForTests: Boolean = false): Seq[fi.oph.suorituspalvelu.business.Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      opiskeluoikeudet.flatMap(opiskeluoikeus => opiskeluoikeus.suoritukset.flatMap(suoritus =>
        suoritus match
          case suoritus if suoritus.tyyppi.koodiarvo == "ammatillinentutkinto" => Some(toAmmatillinenTutkinto(opiskeluoikeus, suoritus))
          case suoritus if suoritus.tyyppi.koodiarvo == "aikuistenperusopetuksenoppimaara" => Some(toAikuistenPerusopetuksenOppimaara(suoritus))
          case suoritus if suoritus.tyyppi.koodiarvo == "perusopetuksenoppimaara" => Some(toPerusopetuksenOppimaara(suoritus))
          case suoritus if suoritus.tyyppi.koodiarvo == "perusopetuksenvuosiluokka" => Some(toPerusopetuksenVuosiluokka(suoritus))
          case suoritus if suoritus.tyyppi.koodiarvo == "nuortenperusopetuksenoppiaineenoppimaara" => Some(toNuortenPerusopetuksenOppiaineenOppimaara(suoritus))
          case suoritus if suoritus.tyyppi.koodiarvo == "telma" => Some(toTelma(suoritus))
          case suoritus if suoritus.tyyppi.koodiarvo == "tuvakoulutuksensuoritus" => Some(toTuva(suoritus))
          case default => None))
    finally
      allowMissingFields.set(false)
}
