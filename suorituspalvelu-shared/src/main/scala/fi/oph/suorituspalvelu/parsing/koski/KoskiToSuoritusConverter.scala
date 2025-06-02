package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenTutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, Arvosana, GeneerinenOpiskeluoikeus, Koodi, NuortenPerusopetuksenOppiaineenOppimaara, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, TallennettavaEntiteetti, Telma, Tuva}

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

  def toAmmattillisenTutkinnonOsaAlue(osaSuoritus: OsaSuoritus): AmmatillisenTutkinnonOsaAlue = {
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
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.nimi.fi).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.map(k => asKoodiObject(k.tunniste)).getOrElse(dummy()),
      arvosana = arviointi.map(arviointi => asKoodiObject(arviointi.arvosana)),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(_.arvo)),
      laajuusKoodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(_.yksikkö).map(y => asKoodiObject(y.get)))
    )
  }

  def toAmmatillisenTutkinnonOsa(osaSuoritus: OsaSuoritus): AmmatillisenTutkinnonOsa = {
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
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.nimi.fi).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.map((k: KoulutusModuuli) => asKoodiObject(k.tunniste)).getOrElse(dummy()),
      yto = osaSuoritus.koulutusmoduuli.exists(k => isYTO(k.tunniste.koodiarvo)),
      arvosana = arviointi.map(arviointi => asKoodiObject(arviointi.arvosana)),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(_.arvo)),
      laajuusKoodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.flatMap(_.yksikkö).map(y => asKoodiObject(y))),
      osaAlueet = osaSuoritus.osasuoritukset.map(osaSuoritukset => osaSuoritukset.map(osaSuoritus => toAmmattillisenTutkinnonOsaAlue(osaSuoritus))).getOrElse(Set.empty)
    )
  }

  def toAmmatillinenTutkinto(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): AmmatillinenTutkinto =
    val tila = opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.tila).last)

    AmmatillinenTutkinto(
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.nimi.fi).getOrElse(dummy()),
      suoritus.koulutusmoduuli.map(km => asKoodiObject(km.tunniste)).getOrElse(dummy()),
      tila.map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      suoritus.vahvistuspäivä.map(p => LocalDate.parse(p)),
      suoritus.keskiarvo,
      suoritus.suoritustapa.map(suoritusTapa => asKoodiObject(suoritusTapa)).getOrElse(dummy()),
      suoritus.osasuoritukset.map(os => os.map(os => toAmmatillisenTutkinnonOsa(os))).getOrElse(Set.empty)
    )

  def toPerusopetuksenOppiaine(osaSuoritus: OsaSuoritus): PerusopetuksenOppiaine =
    PerusopetuksenOppiaine(
      osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.nimi.fi).getOrElse(dummy()),
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
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.nimi.fi).get,
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

  def toPerusopetuksenOppimaara(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): PerusopetuksenOppimaara =
    val tila = opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.tila).last)
    PerusopetuksenOppimaara(
      opiskeluoikeus.oppilaitos.oid,
      tila.map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      suoritus.koulusivistyskieli.map(kielet => kielet.map(kieli => asKoodiObject(kieli))).getOrElse(Set.empty),
      suoritus.vahvistuspäivä.map(p => LocalDate.parse(p)),
      //Käsitellään ainakin toistaiseksi vain sellaiset osasuoritukset, joille löytyy arviointi. Halutaanko jatkossa näyttää osasuorituksia joilla ei ole?
      suoritus.osasuoritukset.map(os => os.filter(_.arviointi.nonEmpty).map(os => toPerusopetuksenOppiaine(os))).getOrElse(Set.empty)
    )

  def toAikuistenPerusopetuksenOppimaara(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): PerusopetuksenOppimaara =
    val tila = opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.tila).last)
     PerusopetuksenOppimaara(
      opiskeluoikeus.oppilaitos.oid,
      tila.map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      Set.empty,
      suoritus.vahvistuspäivä.map(p => LocalDate.parse(p)),
      //Käsitellään ainakin toistaiseksi vain sellaiset osasuoritukset, joille löytyy arviointi. Halutaanko jatkossa näyttää osasuorituksia joilla ei ole?
      suoritus.osasuoritukset.map(os => os.filter(_.arviointi.nonEmpty).map(os => toPerusopetuksenOppiaine(os))).getOrElse(Set.empty)
    )

  def toPerusopetuksenVuosiluokka(suoritus: Suoritus): PerusopetuksenVuosiluokka =
    PerusopetuksenVuosiluokka(
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.nimi.fi).get,
      suoritus.koulutusmoduuli.map(km => asKoodi(km.tunniste)).get,
      suoritus.alkamispäivä.map(p => LocalDate.parse(p)),
      suoritus.`jääLuokalle`.getOrElse(false)
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

  def parseOpiskeluoikeudet(opiskeluoikeudet: Seq[Opiskeluoikeus]): Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus] = {
    opiskeluoikeudet.map {
      case opiskeluoikeus if opiskeluoikeus.isPerusopetus =>
        PerusopetuksenOpiskeluoikeus(
          opiskeluoikeus.oid,
          opiskeluoikeus.oppilaitos.oid,
          toSuoritukset(Seq(opiskeluoikeus)),
          opiskeluoikeus.lisätiedot,
          opiskeluoikeus.tila)
      case opiskeluoikeus if opiskeluoikeus.isAmmatillinen =>
        AmmatillinenOpiskeluoikeus(
          opiskeluoikeus.oid,
          opiskeluoikeus.oppilaitos.oid,
          toSuoritukset(Seq(opiskeluoikeus)),
          opiskeluoikeus.tila)
      case opiskeluoikeus =>
        GeneerinenOpiskeluoikeus(
          opiskeluoikeus.oid,
          opiskeluoikeus.tyyppi.koodiarvo,
          opiskeluoikeus.oppilaitos.oid,
          toSuoritukset(Seq(opiskeluoikeus)),
          opiskeluoikeus.tila)
    }
  }

  def toPerusopetuksenOpiskeluoikeus(opiskeluoikeus: Opiskeluoikeus): fi.oph.suorituspalvelu.business.Opiskeluoikeus = {
    val convertedSuoritukset: Set[business.Suoritus] = toSuoritukset(Seq(opiskeluoikeus))
    PerusopetuksenOpiskeluoikeus(
      opiskeluoikeus.oppilaitos.oid,
      opiskeluoikeus.oid,
      convertedSuoritukset,
      opiskeluoikeus.lisätiedot,
      opiskeluoikeus.tila)
  }

  val SUORITYSTYYPPI_AMMATILLINENTUTKINTO                     = "ammatillinentutkinto"
  val SUORITYSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIMAARA         = "aikuistenperusopetuksenoppimaara"
  val SUORITYSTYYPPI_PERUSOPETUKSENOPPIMAARA                  = "perusopetuksenoppimaara"
  val SUORITYSTYYPPI_PERUSOPETUKSENVUOSILUOKKA                = "perusopetuksenvuosiluokka"
  val SUORITYSTYYPPI_NUORTENPERUSOPETUKSENOPPIAINEENOPPIMAARA = "nuortenperusopetuksenoppiaineenoppimaara"
  val SUORITYSTYYPPI_TELMA                                    = "telma"
  val SUORITYSTYYPPI_TUVAKOULUTUKSENSUORITUS                  = "tuvakoulutuksensuoritus"

  def toSuoritukset(opiskeluoikeudet: Seq[Opiskeluoikeus], allowMissingFieldsForTests: Boolean = false): Set[fi.oph.suorituspalvelu.business.Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      opiskeluoikeudet.flatMap(opiskeluoikeus =>
        opiskeluoikeus.suoritukset.flatMap(suoritus =>
          suoritus match
            case suoritus if suoritus.tyyppi.koodiarvo == SUORITYSTYYPPI_AMMATILLINENTUTKINTO => Some(toAmmatillinenTutkinto(opiskeluoikeus, suoritus))
            case suoritus if suoritus.tyyppi.koodiarvo == SUORITYSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIMAARA => Some(toAikuistenPerusopetuksenOppimaara(opiskeluoikeus, suoritus))
            case suoritus if suoritus.tyyppi.koodiarvo == SUORITYSTYYPPI_PERUSOPETUKSENOPPIMAARA => Some(toPerusopetuksenOppimaara(opiskeluoikeus, suoritus))
            case suoritus if suoritus.tyyppi.koodiarvo == SUORITYSTYYPPI_PERUSOPETUKSENVUOSILUOKKA => Some(toPerusopetuksenVuosiluokka(suoritus))
            case suoritus if suoritus.tyyppi.koodiarvo == SUORITYSTYYPPI_NUORTENPERUSOPETUKSENOPPIAINEENOPPIMAARA => Some(toNuortenPerusopetuksenOppiaineenOppimaara(suoritus))
            case suoritus if suoritus.tyyppi.koodiarvo == SUORITYSTYYPPI_TELMA => Some(toTelma(suoritus))
            case suoritus if suoritus.tyyppi.koodiarvo == SUORITYSTYYPPI_TUVAKOULUTUKSENSUORITUS => Some(toTuva(suoritus))
            case default => None)).toSet
    finally
      allowMissingFields.set(false)
}
