package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, NuortenPerusopetuksenOppiaineenOppimaara, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusTila, Telma, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.util.KoodistoProvider

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

  def convertKoskiTila(koodiArvo: String): SuoritusTila =
    koodiArvo match
      case "hyvaksytystisuoritettu"     => SuoritusTila.VALMIS
      case "valmistunut"                => SuoritusTila.VALMIS
      case "loma"                       => SuoritusTila.KESKEN
      case "lasna"                      => SuoritusTila.KESKEN
      case "valiaikaisestikeskeytynyt"  => SuoritusTila.KESKEN
      case "eronnut"                    => SuoritusTila.KESKEYTYNYT
      case "katsotaaneronneeksi"        => SuoritusTila.KESKEYTYNYT
      case "keskeytynyt"                => SuoritusTila.KESKEYTYNYT
      case "peruutettu"                 => SuoritusTila.KESKEYTYNYT
      case "paattynyt"                  => SuoritusTila.KESKEYTYNYT
      case "mitatoity"                  => throw new RuntimeException("Mitätöidyt suoritukset tulee filtteröidä pois ennen tilakonversiota")

  def isMitatoitu(tila: KoskiKoodi): Boolean =
    tila.koodiarvo == "mitatoity"

  def parseTila(opiskeluoikeus: Opiskeluoikeus, suoritus: Option[Suoritus]): Option[KoskiKoodi] =
    if(suoritus.isDefined && suoritus.get.vahvistus.isDefined)
      Some(KoskiKoodi("valmistunut", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None))
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
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l => Laajuus(l.arvo, asKoodiObject(l.yksikkö.get), Option.apply(l.yksikkö.get.nimi), Option.apply(l.yksikkö.get.lyhytNimi.getOrElse(l.yksikkö.get.nimi)))))
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
      arviointiPaiva = arviointi.flatMap(a => a.`päivä`.map(p => LocalDate.parse(p))),
      arvosana = arviointi.map(arviointi => Arvosana(asKoodiObject(arviointi.arvosana), arviointi.arvosana.nimi)),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l => Laajuus(l.arvo, asKoodiObject(l.yksikkö.get), Option.apply(l.yksikkö.get.nimi), Option.apply(l.yksikkö.get.lyhytNimi.getOrElse(l.yksikkö.get.nimi))))),
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
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
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
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
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
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
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
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy())
    )

  def toTuva(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): Tuva =
    Tuva(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
      opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          Kielistetty(
            o.nimi.fi,
            o.nimi.sv,
            o.nimi.en
          ),
          o.oid)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      laajuus = suoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l => Laajuus(l.arvo, asKoodiObject(l.yksikkö.get), Option.apply(l.yksikkö.get.nimi), l.yksikkö.get.lyhytNimi)))
    )

  def toVapaaSivistystyoKoulutus(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus): VapaaSivistystyo =
    VapaaSivistystyo(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
      opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          Kielistetty(
            o.nimi.fi,
            o.nimi.sv,
            o.nimi.en
          ),
          o.oid)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritus.osasuoritukset.map(ost => Laajuus(
        ost.flatMap(os => os.koulutusmoduuli.flatMap(km => km.laajuus.map(l => l.arvo))).sum,
        asKoodiObject(ost.flatMap(os => os.koulutusmoduuli.flatMap(km => km.laajuus.flatMap(l => l.yksikkö))).head),
        ost.flatMap(os => os.koulutusmoduuli.flatMap(km => km.laajuus.flatMap(l => l.yksikkö.map(y => y.nimi)))).headOption,
        ost.flatMap(os => os.koulutusmoduuli.flatMap(km => km.laajuus.flatMap(l => l.yksikkö.flatMap(y => y.lyhytNimi)))).headOption
      )),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy())
    )

  def toPerusopetuksenOppiaine(osaSuoritus: OsaSuoritus, koodistoProvider: KoodistoProvider): Option[PerusopetuksenOppiaine] = {
    if(!KoskiDataFilter.includePerusopetuksenOppiaine(osaSuoritus, koodistoProvider))
      //Käsitellään ainakin toistaiseksi vain sellaiset oppiaineet, joille löytyy arviointi. Halutaanko jatkossa näyttää oppiaineita joilla ei ole?
      None
    else
      val parasArviointi = {
        val arvioinnit = osaSuoritus.arviointi
          .map(arviointi => arviointi
            .filter(arviointi => arviointi.arvosana.koodistoUri == "arviointiasteikkoyleissivistava"))
          .getOrElse(Set.empty)
        valitseParasArviointi(arvioinnit)
      }

      Some(PerusopetuksenOppiaine(
        UUID.randomUUID(),
        osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
        osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
        parasArviointi.map(arviointi => asKoodiObject(arviointi.arvosana)).get, //Yksi arviointi löytyy aina, tai muuten näitä ei edes haluta parsia
        osaSuoritus.koulutusmoduuli.flatMap((k: KoulutusModuuli) => k.kieli.map(kieli => asKoodiObject(kieli))),
        osaSuoritus.koulutusmoduuli.flatMap(k => k.pakollinen).getOrElse(dummy()),
        osaSuoritus.`yksilöllistettyOppimäärä`,
        osaSuoritus.`rajattuOppimäärä`,
      ))
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
      None,
      opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          o.nimi,
          o.oid)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
      parasArviointi.map(arviointi => asKoodiObject(arviointi.arvosana)).get, //Yksi arviointi löytyy aina, tai muuten näitä ei edes haluta parsia
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    )

  def toPerusopetuksenOppimaara(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus, koodistoProvider: KoodistoProvider): PerusopetuksenOppimaara =
    PerusopetuksenOppimaara(
      UUID.randomUUID(),
      None,
      opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          o.nimi,
          o.oid)).getOrElse(dummy()),
      None, // TODO: tämä pitää kaivaa vuosiluokan suoritukselta jossain vaiheessa
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      suoritus.koulusivistyskieli.map(kielet => kielet.map(kieli => asKoodiObject(kieli))).getOrElse(Set.empty),
      None,
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritus.osasuoritukset.map(os => os.flatMap(os => toPerusopetuksenOppiaine(os, koodistoProvider))).getOrElse(Set.empty)
    )

  def toAikuistenPerusopetuksenOppimaara(opiskeluoikeus: Opiskeluoikeus, suoritus: Suoritus, koodistoProvider: KoodistoProvider): PerusopetuksenOppimaara =
    PerusopetuksenOppimaara(
      UUID.randomUUID(),
      None,
      opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          o.nimi,
          o.oid)).getOrElse(dummy()),
      None, // TODO: onko tätä saatavissa?
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      Set.empty,
      None,
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritus.osasuoritukset.map(os => os.flatMap(os => toPerusopetuksenOppiaine(os, koodistoProvider))).getOrElse(Set.empty)
    )

  def toPerusopetuksenVuosiluokka(suoritus: Suoritus): PerusopetuksenVuosiluokka =
    PerusopetuksenVuosiluokka(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
      suoritus.alkamispäivä.map(p => LocalDate.parse(p)),
      suoritus.`jääLuokalle`.getOrElse(false)
    )

  def parseOpiskeluoikeudet(opiskeluoikeudet: Seq[Opiskeluoikeus], koodistoProvider: KoodistoProvider): Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus] = opiskeluoikeudet.flatMap {
    case opiskeluoikeus if isMitatoitu(parseTila(opiskeluoikeus, None).get) => None
    case opiskeluoikeus if opiskeluoikeus.isPerusopetus =>
      Some(PerusopetuksenOpiskeluoikeus(
        UUID.randomUUID(),
        Some(opiskeluoikeus.oid),
        opiskeluoikeus.oppilaitos.get.oid,
        toSuoritukset(Seq(opiskeluoikeus), koodistoProvider),
        opiskeluoikeus.lisätiedot,
        parseTila(opiskeluoikeus, None).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy())))
    case opiskeluoikeus if opiskeluoikeus.isAmmatillinen =>
      Some(AmmatillinenOpiskeluoikeus(
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
        toSuoritukset(Seq(opiskeluoikeus), koodistoProvider),
        opiskeluoikeus.tila))
    case opiskeluoikeus =>
      Some(GeneerinenOpiskeluoikeus(
        UUID.randomUUID(),
        opiskeluoikeus.oid,
        asKoodiObject(opiskeluoikeus.tyyppi),
        opiskeluoikeus.oppilaitos.get.oid,
        toSuoritukset(Seq(opiskeluoikeus), koodistoProvider),
        opiskeluoikeus.tila))
  }

  val SUORITYSTYYPPI_AMMATILLINENTUTKINTO                     = "ammatillinentutkinto"
  val SUORITYSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIMAARA         = "aikuistenperusopetuksenoppimaara"
  val SUORITYSTYYPPI_PERUSOPETUKSENOPPIMAARA                  = "perusopetuksenoppimaara"
  val SUORITYSTYYPPI_PERUSOPETUKSENVUOSILUOKKA                = "perusopetuksenvuosiluokka"
  val SUORITYSTYYPPI_NUORTENPERUSOPETUKSENOPPIAINEENOPPIMAARA = "nuortenperusopetuksenoppiaineenoppimaara"
  val SUORITYSTYYPPI_TELMA                                    = "telma"
  val SUORITYSTYYPPI_TUVAKOULUTUKSENSUORITUS                  = "tuvakoulutuksensuoritus"
  val SUORITYSTYYPPI_VAPAASIVISTYSTYOSUORITUS                 = "vstoppivelvollisillesuunnattukoulutus"

  def toSuoritukset(opiskeluoikeudet: Seq[Opiskeluoikeus], koodistoProvider: KoodistoProvider, allowMissingFieldsForTests: Boolean = false): Set[fi.oph.suorituspalvelu.business.Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      opiskeluoikeudet.flatMap(opiskeluoikeus =>
        opiskeluoikeus.suoritukset.flatMap(suoritus =>
          suoritus.tyyppi.koodiarvo match
            case SUORITYSTYYPPI_AMMATILLINENTUTKINTO              => Some(toAmmatillinenTutkinto(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIMAARA  => Some(toAikuistenPerusopetuksenOppimaara(opiskeluoikeus, suoritus, koodistoProvider))
            case SUORITYSTYYPPI_PERUSOPETUKSENOPPIMAARA           => Some(toPerusopetuksenOppimaara(opiskeluoikeus, suoritus, koodistoProvider))
            case SUORITYSTYYPPI_PERUSOPETUKSENVUOSILUOKKA         => Some(toPerusopetuksenVuosiluokka(suoritus))
            case SUORITYSTYYPPI_NUORTENPERUSOPETUKSENOPPIAINEENOPPIMAARA
              if suoritus.arviointi.exists(_.nonEmpty)            => Some(toNuortenPerusopetuksenOppiaineenOppimaara(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_TELMA                             => Some(toTelma(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_TUVAKOULUTUKSENSUORITUS           => Some(toTuva(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_VAPAASIVISTYSTYOSUORITUS          => Some(toVapaaSivistystyoKoulutus(opiskeluoikeus, suoritus))
            case default => None)).toSet
    finally
      allowMissingFields.set(false)
}
