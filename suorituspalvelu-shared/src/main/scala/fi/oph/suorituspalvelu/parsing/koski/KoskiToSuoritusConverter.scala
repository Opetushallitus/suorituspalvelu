package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{AIKUISTEN_PERUSOPETUS, TELMA, TUVA, VAPAA_SIVISTYSTYO, VUOSILUOKKA_9}
import fi.oph.suorituspalvelu.business.SuoritusTila.KESKEYTYNYT
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, AmmattiTutkinto, Arvosana, EBArvosana, EBLaajuus, EBOppiaine, EBOppiaineenOsasuoritus, EBTutkinto, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, Lahtokoulu, LahtokouluTyyppi, LukionOppimaara, Opiskeluoikeus, OpiskeluoikeusJakso, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, PerusopetuksenYksilollistaminen, SuoritusTila, Telma, TelmaArviointi, TelmaOsasuoritus, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.parsing.koski
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

  def asKoodiObject(tunniste: KoskiVersioituTunniste): Koodi =
    Koodi(tunniste.koodiarvo, tunniste.koodistoUri, tunniste.koodistoVersio)

  def asKoodi(tunniste: KoskiVersioituTunniste): String =
    tunniste.koodistoUri + "_" + tunniste.koodiarvo + "#" + tunniste.koodistoVersio

  def asKoodisto(tunniste: KoskiVersioituTunniste): String =
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

  def convertOpiskeluoikeusJaksot(jaksot: Seq[KoskiOpiskeluoikeusJakso]): List[OpiskeluoikeusJakso] = {
    jaksot.map(j => OpiskeluoikeusJakso(j.alku, convertKoskiTila(j.tila.koodiarvo)))
      .sortBy(_.alku)
      .toList
  }

  def isMitatoitu(opiskeluoikeus: KoskiOpiskeluoikeus): Boolean =
    // Opiskeluoikeus on mitätöity jos se on milloinkaan ollut mitätöity. Tämä johtuu siitä että
    // KOSKI-datassa on opiskeluoikeuksia jotka on laitettu alkamaan tulevaisuudessa ja sitten mitätöity
    // nykyhetkeen.
    opiskeluoikeus.tila.exists(tila => tila.opiskeluoikeusjaksot.exists(jakso => jakso.tila.koodiarvo=="mitatoity"))

  def parseTila(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: Option[KoskiSuoritus]): Option[KoskiKoodi] =
    if(suoritus.isDefined && suoritus.get.vahvistus.isDefined)
      Some(KoskiKoodi("valmistunut", "koskiopiskeluoikeudentila", Some(1), Kielistetty(None, None, None), None))
    else
      opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.tila).last)

  def parseAloitus(opiskeluoikeus: KoskiOpiskeluoikeus): Option[LocalDate] =
      opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.alku).head)

  def parseKeskeytyminen(opiskeluoikeus: KoskiOpiskeluoikeus): Option[LocalDate] = {
    val uusinJakso = opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.maxBy(_.alku))
    uusinJakso.flatMap(jakso => convertKoskiTila(jakso.tila.koodiarvo) match {
      case KESKEYTYNYT => Some(jakso.alku)
      case default => None
    })
  }

  def valitseParasArviointi(arvioinnit: Set[KoskiArviointi]): Option[KoskiArviointi] = {
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
              val parasArviointi: Option[KoskiArviointi] = {
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
              val parasArviointi: Option[KoskiArviointi] = {
                if (numeeriset.nonEmpty) Some(numeeriset.maxBy(arviointi => arviointi.arvosana.koodiarvo.toInt))
                else {
                  arvioinnit.find(_.arvosana.koodiarvo.equals("Hyväksytty"))
                    .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("Hylätty")))
                }
              }
              parasArviointi
            case "arviointiasteikkoammatillinenhyvaksyttyhylatty" =>
              val parasArviointi: Option[KoskiArviointi] = {
                  arvioinnit.find(_.arvosana.koodiarvo.equals("Hyväksytty"))
                    .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("Hylätty")))
              }
              parasArviointi
            case "arviointiasteikkoeuropeanschoolofhelsinkifinalmark" =>
              if (arvioinnit.nonEmpty)
                Some(arvioinnit.maxBy(arviointi => arviointi.arvosana.koodiarvo.toDouble))
              else None
            case _ =>
              ???
          }
      }
      parasArviointi
    }
  }

  def toAmmattillisenTutkinnonOsaAlue(osaSuoritus: KoskiOsaSuoritus): AmmatillisenTutkinnonOsaAlue = {
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

  def toAmmatillisenTutkinnonOsa(osaSuoritus: KoskiOsaSuoritus): AmmatillisenTutkinnonOsa = {
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

  def toAmmatillinenPerustutkinto(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): AmmatillinenPerustutkinto =
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

  def toAmmattiTutkinto(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): AmmattiTutkinto =
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

  def toErikoisAmmattiTutkinto(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): ErikoisAmmattiTutkinto =
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

  def toAmmatillinenTutkinto(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): fi.oph.suorituspalvelu.business.Suoritus = {
    suoritus.koulutusmoduuli.get.koulutustyyppi.get.koodiarvo match
      case "1" => toAmmatillinenPerustutkinto(opiskeluoikeus, suoritus)
      case "26" => toAmmatillinenPerustutkinto(opiskeluoikeus, suoritus)
      case "11" => toAmmattiTutkinto(opiskeluoikeus, suoritus)
      case "12" => toErikoisAmmattiTutkinto(opiskeluoikeus, suoritus)
  }

  //Vahvistuspäivän vuosi, tai kuluva vuosi jos ei vahvistettu
  //Suunniteltu käyttöön suoritustyypeille Tuva, Telma, Opistovuosi
  def getLisapistekoulutusSuoritusvuosi(suoritus: KoskiSuoritus): Int = {
    suoritus.vahvistus.map(_.`päivä`).map(p => LocalDate.parse(p).getYear)
      .getOrElse(LocalDate.now.getYear)
  }

  def getLisapistekoulutusYhteenlaskettuLaajuus(suoritus: KoskiSuoritus, vainHyvaksytytArvioinnit: Boolean): Option[Laajuus] = {
    suoritus.osasuoritukset.flatMap(ost => {
      val laajuudenYksikot =
        ost.flatMap(os => os.koulutusmoduuli.flatMap(km => km.laajuus.flatMap(l => l.yksikkö)))
      //Oletus, että kaikkien osasuoritusten laajuuksien yksiköt ovat samat.
      laajuudenYksikot.headOption.map(ly => {
        val osasuoritustenLaajuudet: Set[BigDecimal] =
          ost.filter(os => !vainHyvaksytytArvioinnit || os.arviointi.exists(arviointi => arviointi.exists(_.hyväksytty)))
            .flatMap(os => os.koulutusmoduuli.flatMap(km => km.laajuus.map(l => l.arvo)))
        Laajuus(
          osasuoritustenLaajuudet.sum,
          asKoodiObject(ly),
          Some(ly.nimi),
          ly.lyhytNimi
        )
      })
    })
  }

  def toTelma(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): Telma = {
    val aloitusPaivamaara = parseAloitus(opiskeluoikeus).get
    val vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        Kielistetty(
          o.nimi.fi,
          o.nimi.sv,
          o.nimi.en
        ),
        o.oid)).getOrElse(dummy())
    val tila = opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.tila).last)
    val supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy())

    Telma(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      oppilaitos,
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      supaTila,
      aloitusPaivamaara,
      vahvistusPaivamaara,
      getLisapistekoulutusSuoritusvuosi(suoritus),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, true),
      Lahtokoulu(aloitusPaivamaara, vahvistusPaivamaara.orElse(parseKeskeytyminen(opiskeluoikeus)), oppilaitos.oid, Some(aloitusPaivamaara.getYear + 1), TELMA.defaultLuokka.get, Some(supaTila), None, TELMA)
    )
  }

  def toTuva(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): Tuva =
    val aloitusPaivamaara = parseAloitus(opiskeluoikeus).get
    val vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        Kielistetty(
          o.nimi.fi,
          o.nimi.sv,
          o.nimi.en
        ),
        o.oid)).getOrElse(dummy())
    val tila = opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.tila).last)
    val supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy())

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
      aloitusPaivamaara,
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      laajuus = suoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l =>
        Laajuus(
          l.arvo,
          asKoodiObject(l.yksikkö.get),
          Option.apply(l.yksikkö.get.nimi),
          l.yksikkö.get.lyhytNimi))),
      Lahtokoulu(aloitusPaivamaara, vahvistusPaivamaara.orElse(parseKeskeytyminen(opiskeluoikeus)), oppilaitos.oid, Some(aloitusPaivamaara.getYear + 1), TUVA.defaultLuokka.get, Some(supaTila), None, TUVA)
    )

  def toVapaaSivistystyoKoulutus(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): VapaaSivistystyo =
    val aloitusPaivamaara = parseAloitus(opiskeluoikeus).get
    val vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        Kielistetty(
          o.nimi.fi,
          o.nimi.sv,
          o.nimi.en
        ),
        o.oid)).getOrElse(dummy())
    val tila = opiskeluoikeus.tila.map(tila => tila.opiskeluoikeusjaksot.sortBy(jakso => jakso.alku).map(jakso => jakso.tila).last)
    val supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy())

    VapaaSivistystyo(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
      oppilaitos,
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      supaTila,
      aloitusPaivamaara,
      vahvistusPaivamaara,
      getLisapistekoulutusSuoritusvuosi(suoritus),
      //Huom. Tässä ei ole filtteröintiä hyväksytyn arvioinnin perusteella vrt. Telma, koska arviointeja ei ole.
      getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, false),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      Lahtokoulu(aloitusPaivamaara, vahvistusPaivamaara.orElse(parseKeskeytyminen(opiskeluoikeus)), oppilaitos.oid, Some(aloitusPaivamaara.getYear + 1), VAPAA_SIVISTYSTYO.defaultLuokka.get, Some(supaTila), None, VAPAA_SIVISTYSTYO)
    )

  def toPerusopetuksenOppiaine(osaSuoritus: KoskiOsaSuoritus, koodistoProvider: KoodistoProvider): Option[PerusopetuksenOppiaine] = {
    if(!KoskiUtil.includePerusopetuksenOppiaine(osaSuoritus, koodistoProvider))
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
        osaSuoritus.koulutusmoduuli.flatMap((k: KoskiKoulutusModuuli) => k.kieli.map(kieli => asKoodiObject(kieli))),
        osaSuoritus.koulutusmoduuli.flatMap(k => k.pakollinen).getOrElse(dummy()),
        osaSuoritus.`yksilöllistettyOppimäärä`,
        osaSuoritus.`rajattuOppimäärä`,
      ))
  }

  def toPerusopetuksenOppiaineenOppimaara(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): PerusopetuksenOppimaaranOppiaineidenSuoritus = {
    val parasArviointi: Option[KoskiArviointi] = {
      val arvioinnit = suoritus.arviointi
        .map(arviointi => arviointi
          .filter(arviointi => arviointi.arvosana.koodistoUri == "arviointiasteikkoyleissivistava"))
        .getOrElse(Set.empty)
      valitseParasArviointi(arvioinnit)
    }

    PerusopetuksenOppimaaranOppiaineidenSuoritus(
      tunniste = UUID.randomUUID(),
      versioTunniste = None,
      oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          o.nimi,
          o.oid)).getOrElse(dummy()),
      koskiTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
      suoritusKieli = suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      aloitusPaivamaara = parseAloitus(opiskeluoikeus),
      vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      aineet = Set(
        PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
          koodi = suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
          arvosana = parasArviointi.map(arviointi => asKoodiObject(arviointi.arvosana)).get, //Yksi arviointi löytyy aina, tai muuten näitä ei edes haluta parsia
          kieli = suoritus.koulutusmoduuli.flatMap(km => km.kieli.map(kieli => asKoodiObject(kieli))),
          pakollinen = suoritus.koulutusmoduuli.flatMap(km => km.pakollinen).getOrElse(dummy()),
          yksilollistetty = None, //Näitä tietoja ei tule Koskesta oppiaineen oppimäärän suoritukselle
          rajattu = None //Näitä tietoja ei tule Koskesta oppiaineen oppimäärän suoritukselle
        )
      ),
      syotetty = false
    )
  }

  //Tämän tuottamat numeeriset arvot ovat käytännössä koodiston 2asteenpohjakoulutus2021 arvoja.
  def getYksilollistaminen(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): Option[PerusopetuksenYksilollistaminen] = {
    val yksilollistettyja = suoritus.osasuoritukset.getOrElse(Set.empty).count(_.`yksilöllistettyOppimäärä`.exists(_.equals(true)))
    val rajattuja = suoritus.osasuoritukset.getOrElse(Set.empty).count(_.`rajattuOppimäärä`.exists(_.equals(true)))
    val yhteensa = suoritus.osasuoritukset.getOrElse(Set.empty).size
    val opiskeleeToimintaAlueittain =
      opiskeluoikeus
        .lisätiedot
        .flatMap(_.erityisenTuenPäätökset).getOrElse(List.empty)
        .exists(_.opiskeleeToimintaAlueittain.exists(_.equals(true)))

    (yksilollistettyja, rajattuja, yhteensa, opiskeleeToimintaAlueittain) match {
      case (yks, raj, yhteensa, _) if yks >= 1 && yks >= raj =>
        if (yks > yhteensa / 2)
          Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY)
        else
          Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY)
      case (yks, raj, yhteensa, _) if raj >= 1 =>
        if (raj > yhteensa / 2)
          Some(PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_RAJATTU)
        else
          Some(PerusopetuksenYksilollistaminen.OSITTAIN_RAJATTU)
      case (_, _, _, true) => Some(PerusopetuksenYksilollistaminen.TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY)
      case _ => None
    }
  }

  def toPerusopetuksenOppimaara(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus, koodistoProvider: KoodistoProvider): PerusopetuksenOppimaara = {
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        o.nimi,
        o.oid)).getOrElse(dummy())

    val supatila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo))
    val aineet = suoritus.osasuoritukset.map(os => os.flatMap(os => toPerusopetuksenOppiaine(os, koodistoProvider))).getOrElse(Set.empty)
    PerusopetuksenOppimaara(
      tunniste = UUID.randomUUID(),
      versioTunniste = None,
      oppilaitos = oppilaitos,
      luokka = None, // Tämä tallennetaan perusopetuksen oppimäärälle vain syötetyille suorituksille. KOSKI-suorituksille tieto löytyy vuosiluokan suoritukselta
      koskiTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
      suoritusKieli = suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      koulusivistyskieli = suoritus.koulusivistyskieli.map(kielet => kielet.map(kieli => asKoodiObject(kieli))).getOrElse(Set.empty),
      yksilollistaminen = getYksilollistaminen(opiskeluoikeus, suoritus),
      aloitusPaivamaara = parseAloitus(opiskeluoikeus),
      vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      aineet = aineet,
      lahtokoulut = opiskeluoikeus.suoritukset
        .filter(s => s.tyyppi.koodiarvo == SUORITYSTYYPPI_PERUSOPETUKSENVUOSILUOKKA).flatMap(s => {
          val luokkaAste = s.koulutusmoduuli.flatMap(m => m.tunniste.map(t => t.koodiarvo)).getOrElse(dummy())
          val luokka = s.luokka.getOrElse(dummy())
          s.alkamispäivä match
            // Luodaan lähtökoulu vain jos alkamispäivä määritelty. KOSKI-tiimin mukaan alkamispäivät määritelty ainakin
            // viimeisen kuuden vuoden ajalta.
            case Some(pvm) if Set("7", "8", "9").contains(luokkaAste)=>
              val alkamispaiva = LocalDate.parse(pvm)
              val vahvistuspaiva = s.vahvistus.map(v => LocalDate.parse(v.`päivä`))
              Some(Lahtokoulu(alkamispaiva, vahvistuspaiva.orElse(parseKeskeytyminen(opiskeluoikeus)), oppilaitos.oid, Some(alkamispaiva.getYear + 1), luokka, supatila, Some(yhteisenAineenArvosanaPuuttuu(aineet)), LahtokouluTyyppi.valueOf(s"VUOSILUOKKA_$luokkaAste")))
            case default => None
        }),
      syotetty = false,
      vuosiluokkiinSitoutumatonOpetus = opiskeluoikeus.lisätiedot.exists(_.vuosiluokkiinSitoutumatonOpetus.exists(_.equals(true)))
    )
  }

  val YHTEISET_AINEET = List(
    "AI",
    "A1",
    "A2",
    "B1",
    "MA",
    "BI",
    "GE",
    "FY",
    "KE",
    "HI",
    "YH",
    "LI",
    "TE",
    "MU",
    "KU",
    "KS",
    "KO"
  )

  val KATSOMUSAINEET = List(
    "ET",
    "KT"
  )

  // Muista kuin katsomuaineista pitää olla kaikki, ja katsomusaineista jompi kumpi
  def yhteisenAineenArvosanaPuuttuu(aineet: Set[PerusopetuksenOppiaine]): Boolean =
    !YHTEISET_AINEET.forall(yhteinenAine => aineet.exists(oppimaaranAine => oppimaaranAine.koodi.arvo == yhteinenAine)) ||
      !KATSOMUSAINEET.exists(yhteinenAine => aineet.exists(oppimaaranAine => oppimaaranAine.koodi.arvo == yhteinenAine))

  def toAikuistenPerusopetuksenOppimaara(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus, koodistoProvider: KoodistoProvider): PerusopetuksenOppimaara = {
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        o.nimi,
        o.oid)).getOrElse(dummy())

    val aloitus = parseAloitus(opiskeluoikeus)
    val vahvistus = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    val supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo))
    val aineet = suoritus.osasuoritukset.map(os => os.flatMap(os => toPerusopetuksenOppiaine(os, koodistoProvider))).getOrElse(Set.empty)

    PerusopetuksenOppimaara(
     tunniste = UUID.randomUUID(),
      versioTunniste = None,
      oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          o.nimi,
          o.oid)).getOrElse(dummy()),
      luokka = None, // TODO: onko tätä saatavissa?
      koskiTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      supaTila = supaTila.getOrElse(dummy()),
      suoritusKieli = suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      koulusivistyskieli = Set.empty,
      yksilollistaminen = getYksilollistaminen(opiskeluoikeus, suoritus),
      aloitusPaivamaara = aloitus,
      vahvistusPaivamaara = vahvistus,
      aineet = aineet,
      lahtokoulut = Set(Lahtokoulu(aloitus.get, vahvistus, oppilaitos.oid, aloitus.map(_.getYear + 1), AIKUISTEN_PERUSOPETUS.defaultLuokka.get, supaTila, Some(yhteisenAineenArvosanaPuuttuu(aineet)), AIKUISTEN_PERUSOPETUS)),
      syotetty = false,
      vuosiluokkiinSitoutumatonOpetus = opiskeluoikeus.lisätiedot.exists(_.vuosiluokkiinSitoutumatonOpetus.exists(_.equals(true)))
    )
  }

  def toEbOppiaineenOsasuoritus(osaSuoritus: KoskiOsaSuoritus): EBOppiaineenOsasuoritus = {
    //Voiko eb-alaosasuorituksella olla useita arviointeja? Jos voi, voiko arvioinneilla olla erilaisia koodistoja? Käytetäänkö aina koodistoa arviointiasteikkoeuropeanschoolofhelsinkifinalmark?
    val parasArviointi: Option[KoskiArviointi] = {
      val arvioinnit = osaSuoritus.arviointi
        .map(arviointi => arviointi
          .filter(arviointi => arviointi.arvosana.koodistoUri == "arviointiasteikkoeuropeanschoolofhelsinkifinalmark"))
        .getOrElse(Set.empty)
      valitseParasArviointi(arvioinnit)
    }

    EBOppiaineenOsasuoritus(
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      arvosana = parasArviointi.map(pa => EBArvosana(asKoodiObject(pa.arvosana), pa.hyväksytty)).getOrElse(dummy()),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l => Laajuus(l.arvo, asKoodiObject(l.yksikkö.get), Option.apply(l.yksikkö.get.nimi), Option.apply(l.yksikkö.get.lyhytNimi.getOrElse(l.yksikkö.get.nimi)))))
    )
  }

  def toEbOppiaine(osaSuoritus: KoskiOsaSuoritus): EBOppiaine = {
    val arviointi = {
      val arvioinnit = osaSuoritus.arviointi
        .map(arviointi => arviointi
          .filter(arviointi => arviointi.arvosana.koodistoUri == "arviointiasteikkoammatillinen15"))
        .getOrElse(Set.empty)
      valitseParasArviointi(arvioinnit)
    }

    EBOppiaine(
      tunniste = UUID.randomUUID(),
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l => EBLaajuus(l.arvo, asKoodiObject(l.yksikkö.get)))),
      suorituskieli = osaSuoritus.suorituskieli.map(suoritusKieli => asKoodiObject(suoritusKieli)).getOrElse(Koodi("EN", "kieli", Some(1))), //Fixme: massaluovutusrajapinnasta ei vielä tule eb-suorituskieltä, joten fallback.
      osasuoritukset = osaSuoritus.osasuoritukset.map((osaSuoritukset: Set[KoskiOsaSuoritus]) => {
        //Käsitellään vain sellaiset osasuoritukset, joilla on ainakin yksi arviointi.
        val osaSuorituksetJoillaArviointi = osaSuoritukset.filter(o => o.arviointi.exists(_.nonEmpty))
        osaSuorituksetJoillaArviointi.map(osaSuoritus => toEbOppiaineenOsasuoritus(osaSuoritus))
      }).getOrElse(Set.empty))
  }

  def toEbTutkinto(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): EBTutkinto = {
    EBTutkinto(
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
      suoritus.osasuoritukset.map(oss => oss.map(o => toEbOppiaine(o))).getOrElse(Set.empty))
  }

  def toLukionOppimaara(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): LukionOppimaara = {
    LukionOppimaara(
      tunniste = UUID.randomUUID(),
      oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          Kielistetty(
            o.nimi.fi,
            o.nimi.sv,
            o.nimi.en,
          ),
          o.oid)).getOrElse(dummy()),
      koskiTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
      vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritusKieli = None, // Ei saatavilla Koskesta lukion oppimäärälle
      koulusivistyskieli = suoritus.koulusivistyskieli.map(kielet => kielet.map(kieli => asKoodiObject(kieli))).getOrElse(Set.empty)
    )
  }

  def parseOpiskeluoikeudet(opiskeluoikeudet: Seq[KoskiOpiskeluoikeus], koodistoProvider: KoodistoProvider): Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus] =
    opiskeluoikeudet.flatMap {
      case opiskeluoikeus if isMitatoitu(opiskeluoikeus) => None
      case opiskeluoikeus if opiskeluoikeus.isPerusopetus =>
        Some(PerusopetuksenOpiskeluoikeus(
          UUID.randomUUID(),
          Some(opiskeluoikeus.oid),
          opiskeluoikeus.oppilaitos.get.oid,
          toSuoritukset(Seq(opiskeluoikeus), koodistoProvider),
          opiskeluoikeus.lisätiedot,
          parseTila(opiskeluoikeus, None).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
          opiskeluoikeus.tila.map(t => convertOpiskeluoikeusJaksot(t.opiskeluoikeusjaksot)).getOrElse(dummy())))
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
          opiskeluoikeus.tila,
          opiskeluoikeus.tila.map(t => convertOpiskeluoikeusJaksot(t.opiskeluoikeusjaksot)).getOrElse(dummy())))
      case opiskeluoikeus =>
        Some(GeneerinenOpiskeluoikeus(
          UUID.randomUUID(),
          opiskeluoikeus.oid,
          asKoodiObject(opiskeluoikeus.tyyppi),
          opiskeluoikeus.oppilaitos.get.oid,
          toSuoritukset(Seq(opiskeluoikeus), koodistoProvider),
          opiskeluoikeus.tila,
          opiskeluoikeus.tila.map(t => convertOpiskeluoikeusJaksot(t.opiskeluoikeusjaksot)).getOrElse(dummy())))
  }

  val SUORITYSTYYPPI_AMMATILLINENTUTKINTO                       = "ammatillinentutkinto"
  val SUORITYSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIMAARA           = "aikuistenperusopetuksenoppimaara"
  val SUORITYSTYYPPI_PERUSOPETUKSENOPPIMAARA                    = "perusopetuksenoppimaara"
  val SUORITYSTYYPPI_PERUSOPETUKSENVUOSILUOKKA                  = "perusopetuksenvuosiluokka"
  val SUORITYSTYYPPI_NUORTENPERUSOPETUKSENOPPIAINEENOPPIMAARA   = "nuortenperusopetuksenoppiaineenoppimaara"
  val SUORITUSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIAINEENOPPIMAARA = "aikuistenperusopetuksenoppiaineenoppimaara"
  val SUORITYSTYYPPI_TELMA                                      = "telma"
  val SUORITYSTYYPPI_TUVAKOULUTUKSENSUORITUS                    = "tuvakoulutuksensuoritus"
  val SUORITYSTYYPPI_VAPAASIVISTYSTYOSUORITUS                   = "vstoppivelvollisillesuunnattukoulutus"
  val SUORITYSTYYPPI_EB                                         = "ebtutkinto"
  val SUORITYSTYYPPI_LUKIONOPPIMAARA                            = "lukionoppimaara"

  def toSuoritukset(opiskeluoikeudet: Seq[KoskiOpiskeluoikeus], koodistoProvider: KoodistoProvider, allowMissingFieldsForTests: Boolean = false): Set[fi.oph.suorituspalvelu.business.Suoritus] = {
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      opiskeluoikeudet.flatMap(opiskeluoikeus =>
        opiskeluoikeus.suoritukset.flatMap(suoritus =>
          suoritus.tyyppi.koodiarvo match
            case SUORITYSTYYPPI_AMMATILLINENTUTKINTO              => Some(toAmmatillinenTutkinto(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIMAARA  => Some(toAikuistenPerusopetuksenOppimaara(opiskeluoikeus, suoritus, koodistoProvider))
            case SUORITYSTYYPPI_PERUSOPETUKSENOPPIMAARA           => Some(toPerusopetuksenOppimaara(opiskeluoikeus, suoritus, koodistoProvider))
            case SUORITYSTYYPPI_PERUSOPETUKSENVUOSILUOKKA         => None // vuosiluokkien tiedot käsitellään osana perusopetuksen oppimäärää
            case SUORITYSTYYPPI_NUORTENPERUSOPETUKSENOPPIAINEENOPPIMAARA
              if suoritus.arviointi.exists(_.nonEmpty)            => Some(toPerusopetuksenOppiaineenOppimaara(opiskeluoikeus, suoritus))
            case SUORITUSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIAINEENOPPIMAARA
              if suoritus.arviointi.exists(_.nonEmpty)            => Some(toPerusopetuksenOppiaineenOppimaara(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_TELMA                             => Some(toTelma(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_TUVAKOULUTUKSENSUORITUS           => Some(toTuva(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_VAPAASIVISTYSTYOSUORITUS          => Some(toVapaaSivistystyoKoulutus(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_EB                                => Some(toEbTutkinto(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_LUKIONOPPIMAARA                   => Some(toLukionOppimaara(opiskeluoikeus, suoritus))
            case default => None)).toSet
    finally
      allowMissingFields.set(false)
  }
}
