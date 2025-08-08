package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, NuortenPerusopetuksenOppiaineenOppimaara, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, TallennettavaEntiteetti, Telma, Tuva}

import java.time.LocalDate
import java.util.UUID

/**
 * Muuntaa Kosken suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object KoskiToSuoritusConverter {

  val allowMissingFields = new ThreadLocal[Boolean]

  def dummy[A](): A =
    if (allowMissingFields.get())
      null.asInstanceOf[A]
    else
      throw new RuntimeException("Dummies not allowed")

  def asKoodiObject(tunniste: VersioituTunniste): Koodi =
    Koodi(tunniste.koodiarvo, tunniste.koodistoUri, tunniste.koodistoVersio)

  def asKoodi(tunniste: VersioituTunniste): String =
    tunniste.koodistoUri + "_" + tunniste.koodiarvo + "#" + tunniste.koodistoVersio

  def asKoodisto(tunniste: VersioituTunniste): String =
    tunniste.koodistoUri + "#" + tunniste.koodistoVersio

  def isYTO(koodiarvo: String): Boolean = {
    koodiarvo match
      case "106727" => true // viestintä- ja vuorovaikutusosaaminen
      case "106728" => true // matemaattis-luonnontieteellinen osaaminen
      case "106729" => true // yhteiskunta- ja työelämäosaaminen
      case default => false
  }

  def parseTila(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): Option[VersioituTunniste] =
    if(suoritus.vahvistus.isDefined)
      Some(OpiskeluoikeusJaksoTila("valmistunut", "koskiopiskeluoikeudentila", Some(1)))
    else
      opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.tila).last)

  def parseAloitus(opiskeluoikeus: Opiskeluoikeus): Option[LocalDate] =
      opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.alku).head)

  def valitseParasArviointi(arvioinnit: Set[Arviointi]): Option[Arviointi] = {
    if (arvioinnit.size <= 1) arvioinnit.headOption
    else {
      val asteikot: Set[String] = arvioinnit.map(arviointi => arviointi.arvosana.koodistoUri)
      val parasArviointi = asteikot match {
        case asteikot if asteikot.size > 1 => throw new RuntimeException(s"Arvioinnit sisältävät useita asteikkoja: $arvioinnit")
        case asteikot if asteikot.isEmpty => throw new RuntimeException(s"Arvioinneilta puuttuu asteikko: $arvioinnit")
        case asteikot =>
          asteikot.head match {
            case "arviointiasteikkoyleissivistava" =>
              val numeeriset = arvioinnit.filter(arv => Set("10", "9", "8", "7", "6", "5", "4").contains(arv.arvosana.koodiarvo))
              val parasArviointi: Option[Arviointi] = {
                if (numeeriset.nonEmpty) Some(numeeriset.maxBy(arviointi => arviointi.arvosana.koodiarvo.toInt))
                else {
                  arvioinnit.find(_.arvosana.koodiarvo.equals("S"))
                    .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("O")))
                    .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("H")))
                }
              }
              parasArviointi
            case "arviointiasteikkoammatillinen15" =>
              val numeeriset = arvioinnit.filter(arv => Set("1", "2", "3", "4", "5").contains(arv.arvosana.koodiarvo))
              val parasArviointi: Option[Arviointi] = {
                if (numeeriset.nonEmpty) Some(numeeriset.maxBy(arviointi => arviointi.arvosana.koodiarvo.toInt))
                else {
                  arvioinnit.find(_.arvosana.koodiarvo.equals("Hyväksytty"))
                    .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("Hylätty")))
                }
              }
              parasArviointi
            case _ =>
              ???
          }
      }
      parasArviointi
    }
  }

  def toAmmattillisenTutkinnonOsaAlue(osaSuoritus: OsaSuoritus): AmmatillisenTutkinnonOsaAlue = {
    val arviointi = {
      val arvioinnit = osaSuoritus.arviointi
        .map(arviointi => arviointi
          .filter(arviointi => arviointi.arvosana.koodistoUri == "arviointiasteikkoammatillinen15"))
        .getOrElse(Set.empty)
      valitseParasArviointi(arvioinnit)
    }

    AmmatillisenTutkinnonOsaAlue(
      UUID.randomUUID(),
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
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
      valitseParasArviointi(arvioinnit)
    }

    AmmatillisenTutkinnonOsa(
      UUID.randomUUID(),
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      yto = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => isYTO(t.koodiarvo))).getOrElse(false),
      arvosana = arviointi.map(arviointi => asKoodiObject(arviointi.arvosana)),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(_.arvo)),
      laajuusKoodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.flatMap(_.yksikkö).map(y => asKoodiObject(y))),
      osaAlueet = osaSuoritus.osasuoritukset.map(osaSuoritukset => osaSuoritukset.map(osaSuoritus => toAmmattillisenTutkinnonOsaAlue(osaSuoritus))).getOrElse(Set.empty)
    )
  }

  def toAmmatillinenPerustutkinto(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): AmmatillinenPerustutkinto =
    AmmatillinenPerustutkinto(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          Kielistetty(
            o.nimi.fi,
            o.nimi.sv,
            o.nimi.en
          ),
          o.oid)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, suoritus).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritus.keskiarvo,
      suoritus.suoritustapa.map(suoritusTapa => asKoodiObject(suoritusTapa)).getOrElse(dummy()),
      suoritus.suorituskieli.map(suoritusKieli => asKoodiObject(suoritusKieli)).getOrElse(dummy()),
      suoritus.osasuoritukset.map(os => os.map(os => toAmmatillisenTutkinnonOsa(os))).getOrElse(Set.empty)
    )

  def toAmmattiTutkinto(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): AmmattiTutkinto =
    AmmattiTutkinto(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          Kielistetty(
            o.nimi.fi,
            o.nimi.sv,
            o.nimi.en,
          ),
          o.oid)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, suoritus).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritus.suoritustapa.map(suoritusTapa => asKoodiObject(suoritusTapa)).getOrElse(dummy()),
      suoritus.suorituskieli.map(suoritusKieli => asKoodiObject(suoritusKieli)).getOrElse(dummy())
    )

  def toErikoisAmmattiTutkinto(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): ErikoisAmmattiTutkinto =
    ErikoisAmmattiTutkinto(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          Kielistetty(
            o.nimi.fi,
            o.nimi.sv,
            o.nimi.en,
          ),
          o.oid)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, suoritus).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritus.suorituskieli.map(suoritusKieli => asKoodiObject(suoritusKieli)).getOrElse(dummy())
    )
  
  def toAmmatillinenTutkinto(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): fi.oph.suorituspalvelu.business.Suoritus = {
    suoritus.koulutusmoduuli.get.koulutustyyppi.get.koodiarvo match
      case "1" => toAmmatillinenPerustutkinto(opiskeluoikeus, suoritus)
      case "26" => toAmmatillinenPerustutkinto(opiskeluoikeus, suoritus)
      case "11" => toAmmattiTutkinto(opiskeluoikeus, suoritus)
      case "12" => toErikoisAmmattiTutkinto(opiskeluoikeus, suoritus)
  }

  def toTelma(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): Telma =
    val tila = opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.tila).last)

    Telma(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          Kielistetty(
            o.nimi.fi,
            o.nimi.sv,
            o.nimi.en
          ),
          o.oid)).getOrElse(dummy()),
      tila.map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy())
    )

  def toPerusopetuksenOppiaine(osaSuoritus: OsaSuoritus): PerusopetuksenOppiaine = {
    val parasArviointi = {
      val arvioinnit = osaSuoritus.arviointi
        .map(arviointi => arviointi
          .filter(arviointi => arviointi.arvosana.koodistoUri == "arviointiasteikkoyleissivistava"))
        .getOrElse(Set.empty)
      valitseParasArviointi(arvioinnit)
    }

    PerusopetuksenOppiaine(
      UUID.randomUUID(),
      osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      parasArviointi.map(arviointi => asKoodiObject(arviointi.arvosana)).get, //Yksi arviointi löytyy aina, tai muuten näitä ei edes haluta parsia
      osaSuoritus.koulutusmoduuli.flatMap((k: KoulutusModuuli) => k.kieli.map(kieli => asKoodiObject(kieli)))
    )
  }

  def toNuortenPerusopetuksenOppiaineenOppimaara(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): NuortenPerusopetuksenOppiaineenOppimaara =
    val parasArviointi = {
      val arvioinnit = suoritus.arviointi
        .map(arviointi => arviointi
          .filter(arviointi => arviointi.arvosana.koodistoUri == "arviointiasteikkoyleissivistava"))
        .getOrElse(Set.empty)
      valitseParasArviointi(arvioinnit)
    }

    NuortenPerusopetuksenOppiaineenOppimaara(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
      parasArviointi.map(arviointi => asKoodiObject(arviointi.arvosana)).get, //Yksi arviointi löytyy aina, tai muuten näitä ei edes haluta parsia
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    )

  def toPerusopetuksenOppimaara(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): PerusopetuksenOppimaara =
    PerusopetuksenOppimaara(
      UUID.randomUUID(),
      opiskeluoikeus.oppilaitos.get.oid,
      parseTila(opiskeluoikeus, suoritus).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      suoritus.koulusivistyskieli.map(kielet => kielet.map(kieli => asKoodiObject(kieli))).getOrElse(Set.empty),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      //Käsitellään ainakin toistaiseksi vain sellaiset osasuoritukset, joille löytyy arviointi. Halutaanko jatkossa näyttää osasuorituksia joilla ei ole?
      suoritus.osasuoritukset.map(os => os.filter(_.arviointi.nonEmpty).map(os => toPerusopetuksenOppiaine(os))).getOrElse(Set.empty)
    )

  def toAikuistenPerusopetuksenOppimaara(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): PerusopetuksenOppimaara =
    PerusopetuksenOppimaara(
      UUID.randomUUID(),
      opiskeluoikeus.oppilaitos.get.oid,
      parseTila(opiskeluoikeus, suoritus).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      Set.empty,
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      //Käsitellään ainakin toistaiseksi vain sellaiset osasuoritukset, joille löytyy arviointi. Halutaanko jatkossa näyttää osasuorituksia joilla ei ole?
      suoritus.osasuoritukset.map(os => os.filter(_.arviointi.nonEmpty).map(os => toPerusopetuksenOppiaine(os))).getOrElse(Set.empty)
    )

  def toPerusopetuksenVuosiluokka(suoritus: Suoritus): PerusopetuksenVuosiluokka =
    PerusopetuksenVuosiluokka(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
      suoritus.alkamispäivä.map(p => LocalDate.parse(p)),
      suoritus.`jääLuokalle`.getOrElse(false)
    )

  def toTuva(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): Tuva =
    Tuva(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    )

  def parseOpiskeluoikeudet(opiskeluoikeudet: Seq[Opiskeluoikeus]): Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus] = {
    opiskeluoikeudet.map {
      case opiskeluoikeus if opiskeluoikeus.isPerusopetus =>
        PerusopetuksenOpiskeluoikeus(
          UUID.randomUUID(),
          opiskeluoikeus.oid,
          opiskeluoikeus.oppilaitos.get.oid,
          toSuoritukset(Seq(opiskeluoikeus)),
          opiskeluoikeus.lisätiedot,
          opiskeluoikeus.tila)
      case opiskeluoikeus if opiskeluoikeus.isAmmatillinen =>
        AmmatillinenOpiskeluoikeus(
          UUID.randomUUID(),
          opiskeluoikeus.oid,
          opiskeluoikeus.oppilaitos.map(o =>
            fi.oph.suorituspalvelu.business.Oppilaitos(
              Kielistetty(
                o.nimi.fi,
                o.nimi.sv,
                o.nimi.en,
              ),
              o.oid)).getOrElse(dummy()),
          toSuoritukset(Seq(opiskeluoikeus)),
          opiskeluoikeus.tila)
      case opiskeluoikeus =>
        GeneerinenOpiskeluoikeus(
          UUID.randomUUID(),
          opiskeluoikeus.oid,
          asKoodiObject(opiskeluoikeus.tyyppi),
          opiskeluoikeus.oppilaitos.get.oid,
          toSuoritukset(Seq(opiskeluoikeus)),
          opiskeluoikeus.tila)
    }
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
            case suoritus if suoritus.tyyppi.koodiarvo == SUORITYSTYYPPI_NUORTENPERUSOPETUKSENOPPIAINEENOPPIMAARA && suoritus.arviointi.exists(_.nonEmpty) => Some(toNuortenPerusopetuksenOppiaineenOppimaara(opiskeluoikeus, suoritus))
            case suoritus if suoritus.tyyppi.koodiarvo == SUORITYSTYYPPI_TELMA => Some(toTelma(opiskeluoikeus, suoritus))
            case suoritus if suoritus.tyyppi.koodiarvo == SUORITYSTYYPPI_TUVAKOULUTUKSENSUORITUS => Some(toTuva(opiskeluoikeus, suoritus))
            case default => None)).toSet
    finally
      allowMissingFields.set(false)
}
