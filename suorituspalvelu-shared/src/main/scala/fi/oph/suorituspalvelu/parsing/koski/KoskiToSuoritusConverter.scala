package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{AIKUISTEN_PERUSOPETUS, PERUSOPETUKSEEN_VALMISTAVA_OPETUS, TELMA, TUVA, VAPAA_SIVISTYSTYO, VUOSILUOKKA_9}
import fi.oph.suorituspalvelu.business.SuoritusTila.KESKEYTYNYT
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmatillinenTutkintoOsittainen, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, AmmattiTutkinto, Arvosana, DIAArvosana, DIALaajuus, DIAOppiaine, DIAOppiaineenKoesuoritus, DIATutkinto, DIAVastaavuustodistuksenTiedot, EBArvosana, EBLaajuus, EBOppiaine, EBOppiaineenOsasuoritus, EBTutkinto, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, IBArvosana, IBLaajuus, IBOppiaineRyhma, IBOppiaineSuoritus, IBTutkinto, Koodi, Korotus, Laajuus, Lahtokoulu, LahtokouluTyyppi, LukionOppimaara, Opiskeluoikeus, OpiskeluoikeusJakso, PerusopetukseenValmistavaOpetus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, PerusopetuksenYksilollistaminen, PoistettuOpiskeluoikeus, SuoritusTila, Telma, TelmaArviointi, TelmaOsasuoritus, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.parsing.koski
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil.isPakollinenJaArviointiPuuttuu
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.slf4j.LoggerFactory

import java.time.LocalDate
import java.util.UUID
import scala.math.Ordering.Implicits.infixOrderingOps

/**
 * Muuntaa Kosken suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object KoskiToSuoritusConverter {

  private val LOG = LoggerFactory.getLogger(KoskiToSuoritusConverter.getClass)

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
      case "101053" => true // viestintä- ja vuorovaikutusosaaminen
      case "400012" => true // viestintä- ja vuorovaikutusosaaminen
      case "106728" => true // matemaattis-luonnontieteellinen osaaminen
      case "101054" => true // matemaattis-luonnontieteellinen osaaminen
      case "400013" => true // matemaattis-luonnontieteellinen osaaminen
      case "106729" => true // yhteiskunta- ja työelämäosaaminen
      case "101055" => true // yhteiskunta- ja työelämäosaaminen
      case "400014" => true // yhteiskunta- ja työelämäosaaminen
      case default => false
  }

  def convertKoskiTila(koodiArvo: String): SuoritusTila =
    koodiArvo match
      case "hyvaksytystisuoritettu"     => SuoritusTila.VALMIS
      case "valmistunut"                => SuoritusTila.VALMIS
      case "loma"                       => SuoritusTila.KESKEN
      case "lasna"                      => SuoritusTila.KESKEN
      case "valiaikaisestikeskeytynyt"  => SuoritusTila.KESKEYTYNYT
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

  def parseLasnaolot(opiskeluoikeus: KoskiOpiskeluoikeus, aloitusPvm: Option[LocalDate], vahvistusPvm: Option[LocalDate]): List[(LocalDate, Option[LocalDate])] = {
    if (opiskeluoikeus.tila.isEmpty)
      List.empty
    else
      val jaksot = convertOpiskeluoikeusJaksot(opiskeluoikeus.tila.get.opiskeluoikeusjaksot)
      jaksot.zip(jaksot.tail.map(e => Some(e)) :+ None).flatMap((curr, next) => {
        if (curr.tila == SuoritusTila.KESKEN) {
          (aloitusPvm, vahvistusPvm, next) match
            case (Some(aloitusPvm), _, Some(next)) if !aloitusPvm.isBefore(next.alku) => None
            case (_, Some(vahvistusPvm), _) if !vahvistusPvm.isAfter(curr.alku) => None
            case (Some(aloitusPvm), None, _) => Some((aloitusPvm.max(curr.alku), next.map(_.alku)))
            case (None, Some(vahvistusPvm), _) => Some(curr.alku, next.map(n => vahvistusPvm.min(n.alku)).orElse(Some(vahvistusPvm)))
            case (Some(aloitusPvm), Some(vahvistusPvm), _) => Some((aloitusPvm.max(curr.alku), next.map(n => vahvistusPvm.min(n.alku)).orElse(Some(vahvistusPvm))))
            case (None, None, _) => Some(curr.alku, next.map(_.alku))
        } else
          None
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
              numeeriset.maxByOption(_.arvosana.koodiarvo.toInt)
                .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("S")))
                .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("O")))
                .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("H")))
            case "arviointiasteikkoeuropeanschoolofhelsinkifinalmark" =>
              arvioinnit.maxByOption(_.arvosana.koodiarvo.toDouble)
            case "arviointiasteikkodiatutkinto" =>
              //Koodistossa on S, muut numeerisia
              val numeeriset = arvioinnit.filter(arv => !arv.arvosana.koodiarvo.equals("S"))
              numeeriset.maxByOption(_.arvosana.koodiarvo.toInt)
                .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("S")))
            case "arviointiasteikkoib" =>
              val numeeriset = arvioinnit.filter(arv => Set("7", "6", "5", "4", "3", "2", "1").contains(arv.arvosana.koodiarvo))
              numeeriset.maxByOption(_.arvosana.koodiarvo.toInt)
                .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("S")))
                .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("O")))
                .orElse(arvioinnit.find(_.arvosana.koodiarvo.equals("F")))
            case _ =>
              ???
          }
      }
      parasArviointi
    }
  }

  private def AMMATILLINEN_ARVIOINTIASTEIKKO_15 = "arviointiasteikkoammatillinen15"
  private def AMMATILLINEN_ARVIOINTIASTEIKKO_13 = "arviointiasteikkoammatillinent1k3"

  private def AMMATILLISET_ARVIOINTIASTEIKKO_KOODISTOT = Set(
    AMMATILLINEN_ARVIOINTIASTEIKKO_13,
    AMMATILLINEN_ARVIOINTIASTEIKKO_15,
    "arviointiasteikkoammatillinenhyvaksyttyhylatty"
  )

  // Ammatillisilla osasuorituksilla voi olla arviointeja eri asteikoilla. Valitaan ensisijaisesti numeerinen, siten hyväksytty ja hylätty
  def valitseParasAmmatillinenArviointi(
    arvioinnit: Option[Set[KoskiArviointi]],
  ): Option[KoskiArviointi] = {
    val validitArvioinnit = arvioinnit.getOrElse(Set.empty)
      .filter(arviointi => AMMATILLISET_ARVIOINTIASTEIKKO_KOODISTOT.contains(arviointi.arvosana.koodistoUri))
    val asteikot = validitArvioinnit.map(_.arvosana.koodistoUri)

    if (asteikot.contains(AMMATILLINEN_ARVIOINTIASTEIKKO_13) && asteikot.contains(AMMATILLINEN_ARVIOINTIASTEIKKO_15)) {
      throw RuntimeException(s"Ammatillisella osasuorituksella on arviointeja useilla numeerisilla asteikoilla: ${asteikot.mkString(", ")}.")
    }

    validitArvioinnit.maxByOption(arviointi => {
      val koodiarvo = arviointi.arvosana.koodiarvo
      if (koodiarvo.matches("\\d+")) koodiarvo.toDouble
      // arviointiasteikkoammatillinent1k3 sisältää arvon "0", joka tarkoittaa "hylätty".
      // Valitaan ennemmin "Hyväksytty" muista koodistoista, jos löytyy
      else if (koodiarvo.equals("Hyväksytty")) 0.5
      else -1
    })
  }

  def toAmmattillisenTutkinnonOsaAlue(osaSuoritus: KoskiOsaSuoritus): AmmatillisenTutkinnonOsaAlue = {
    val arviointi = valitseParasAmmatillinenArviointi(osaSuoritus.arviointi)

    AmmatillisenTutkinnonOsaAlue(
      UUID.randomUUID(),
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      arvosana = arviointi.map(arviointi => asKoodiObject(arviointi.arvosana)),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l => Laajuus(l.arvo, asKoodiObject(l.yksikkö.get), Option.apply(l.yksikkö.get.nimi), Option.apply(l.yksikkö.get.lyhytNimi.getOrElse(l.yksikkö.get.nimi))))),
      korotettu = osaSuoritus.korotettu.map(k => Korotus.valueOf(k.koodiarvo.toUpperCase))
    )
  }

  def toAmmatillisenTutkinnonOsa(osaSuoritus: KoskiOsaSuoritus): AmmatillisenTutkinnonOsa = {
    val arviointi = valitseParasAmmatillinenArviointi(osaSuoritus.arviointi)

    AmmatillisenTutkinnonOsa(
      UUID.randomUUID(),
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      yto = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => isYTO(t.koodiarvo))).getOrElse(false),
      arviointiPaiva = arviointi.flatMap(a => a.`päivä`.map(p => LocalDate.parse(p))),
      arvosana = arviointi.map(arviointi => Arvosana(asKoodiObject(arviointi.arvosana), arviointi.arvosana.nimi)),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l => Laajuus(l.arvo, asKoodiObject(l.yksikkö.get), Option.apply(l.yksikkö.get.nimi), Option.apply(l.yksikkö.get.lyhytNimi.getOrElse(l.yksikkö.get.nimi))))),
      osaAlueet = osaSuoritus.osasuoritukset.map(osaSuoritukset => osaSuoritukset.map(osaSuoritus => toAmmattillisenTutkinnonOsaAlue(osaSuoritus))).getOrElse(Set.empty),
      korotettu = osaSuoritus.korotettu.map(k => Korotus.valueOf(k.koodiarvo.toUpperCase))
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

  def toAmmatillinenTutkintoOsittainen(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): AmmatillinenTutkintoOsittainen =
    AmmatillinenTutkintoOsittainen(
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
      suoritus.korotettuKeskiarvo,
      suoritus.korotettuOpiskeluoikeusOid,
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
    val aloitusPaivamaara = parseAloitus(opiskeluoikeus)
    val vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        Kielistetty(
          o.nimi.fi,
          o.nimi.sv,
          o.nimi.en
        ),
        o.oid)).getOrElse(dummy())
    val supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy())
    val valmistumisVuosi = if vahvistusPaivamaara.isDefined then vahvistusPaivamaara.map(_.getYear) else aloitusPaivamaara.map(_.getYear + 1)

    Telma(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      oppilaitos,
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      supaTila,
      aloitusPaivamaara.get,
      vahvistusPaivamaara,
      getLisapistekoulutusSuoritusvuosi(suoritus),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, true),
      parseLasnaolot(opiskeluoikeus, None, vahvistusPaivamaara).map(l =>
        Lahtokoulu(l._1, l._2, oppilaitos.oid, valmistumisVuosi, TELMA.defaultLuokka.get, Some(supaTila), None, TELMA))
    )
  }

  def toTuva(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): Tuva =
    val aloitusPaivamaara = parseAloitus(opiskeluoikeus)
    val vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        Kielistetty(
          o.nimi.fi,
          o.nimi.sv,
          o.nimi.en
        ),
        o.oid)).getOrElse(dummy())
    val supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy())
    val valmistumisVuosi = if vahvistusPaivamaara.isDefined then vahvistusPaivamaara.map(_.getYear) else aloitusPaivamaara.map(_.getYear + 1)

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
      aloitusPaivamaara.get,
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      getLisapistekoulutusSuoritusvuosi(suoritus),
      getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, true),
      parseLasnaolot(opiskeluoikeus, None, vahvistusPaivamaara).map(l =>
        Lahtokoulu(l._1, l._2, oppilaitos.oid, valmistumisVuosi, TUVA.defaultLuokka.get, Some(supaTila), None, TUVA))
    )

  def toVapaaSivistystyoKoulutus(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): VapaaSivistystyo =
    val aloitusPaivamaara = parseAloitus(opiskeluoikeus)
    val vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        Kielistetty(
          o.nimi.fi,
          o.nimi.sv,
          o.nimi.en
        ),
        o.oid)).getOrElse(dummy())
    val supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy())
    val valmistumisVuosi = if vahvistusPaivamaara.isDefined then vahvistusPaivamaara.map(_.getYear) else aloitusPaivamaara.map(_.getYear + 1)

    VapaaSivistystyo(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
      oppilaitos,
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      supaTila,
      aloitusPaivamaara.get,
      vahvistusPaivamaara,
      getLisapistekoulutusSuoritusvuosi(suoritus),
      //Huom. Tässä ei ole filtteröintiä hyväksytyn arvioinnin perusteella vrt. Telma, koska arviointeja ei ole.
      getLisapistekoulutusYhteenlaskettuLaajuus(suoritus, false),
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      parseLasnaolot(opiskeluoikeus, None, vahvistusPaivamaara).map(l =>
        Lahtokoulu(l._1, l._2, oppilaitos.oid, valmistumisVuosi, VAPAA_SIVISTYSTYO.defaultLuokka.get, Some(supaTila), None, VAPAA_SIVISTYSTYO))
    )

  def toPerusopetuksenOppiaine(osaSuoritus: KoskiOsaSuoritus, koodistoProvider: KoodistoProvider): Option[PerusopetuksenOppiaine] = {
    if(!KoskiUtil.includePerusopetuksenOppiaine(osaSuoritus, koodistoProvider))
      //Käsitellään ainakin toistaiseksi vain sellaiset oppiaineet, joille löytyy arviointi. Halutaanko jatkossa näyttää oppiaineita joilla ei ole?
      None
    else
      val parasArviointi = {
        val arvioinnit = osaSuoritus.arviointi.getOrElse(Set.empty)
          .filter(_.arvosana.koodistoUri == "arviointiasteikkoyleissivistava")
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
      val arvioinnit = suoritus.arviointi.getOrElse(Set.empty)
        .filter(_.arvosana.koodistoUri == "arviointiasteikkoyleissivistava")
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
    val yhteisetAineetJaKielet = suoritus.osasuoritukset.getOrElse(Set.empty)
      .filter(os => os.koulutusmoduuli.exists(km => km.pakollinen.exists(p => p) || km.tunniste.map(_.koodiarvo).exists(Set("A2", "B2").contains)))
    val yksilollistettyja = yhteisetAineetJaKielet.count(_.`yksilöllistettyOppimäärä`.exists(_.equals(true)))
    val rajattuja = yhteisetAineetJaKielet.count(_.`rajattuOppimäärä`.exists(_.equals(true)))
    val yhteensa = yhteisetAineetJaKielet.size
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

  //Kotiopetus-opiskeluoikeudeksi tulkitaan kaksi eri tapausta:
  // -Opiskeluoikeus, jolla on kotiopetusjakso jolla ei ole loppupäivää.
  // -Opiskeluoikeus, jolla on kotiopetusjakso jolla on loppupäivä, mutta ei 9. vuosiluokan luokkasuoritusta.
  def isKotiopetus(opiskeluoikeus: KoskiOpiskeluoikeus): Boolean = {
    def hasYsiluokka = opiskeluoikeus.suoritukset.get.filter(
      s => s.tyyppi.koodiarvo == SUORITUSTYYPPI_PERUSOPETUKSENVUOSILUOKKA
    ).exists(s => s.koulutusmoduuli.flatMap(m => m.tunniste.map(t => t.koodiarvo)).exists("9".equals))
    val loytyyJaksoIlmanLoppua = opiskeluoikeus.lisätiedot.flatMap(_.kotiopetusjaksot).getOrElse(List.empty).exists(_.loppu.isEmpty)
    val loytyyJaksoLopulla = opiskeluoikeus.lisätiedot.flatMap(_.kotiopetusjaksot).getOrElse(List.empty).exists(_.loppu.isDefined)
    loytyyJaksoIlmanLoppua || (loytyyJaksoLopulla && !hasYsiluokka)
  }

  def isYlaAste(opiskeluoikeus: KoskiOpiskeluoikeus): Boolean = {
    opiskeluoikeus.suoritukset.get
      .filter(s => s.tyyppi.koodiarvo == SUORITUSTYYPPI_PERUSOPETUKSENVUOSILUOKKA)
      .exists(s => s.koulutusmoduuli.flatMap(m => m.tunniste.map(t => t.koodiarvo)).exists(Set("7", "8", "9").contains))
  }

  def getPerusopetuksenLahtokoulut(opiskeluoikeus: KoskiOpiskeluoikeus, haluttuLuokkaAste: String, yhteisenAineenArvosanaPuuttuu: Option[Boolean], seuraavanAsteenAlkamispaiva: Option[LocalDate], koodistoProvider: KoodistoProvider): List[Lahtokoulu] =
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        o.nimi,
        o.oid)).getOrElse(dummy())

    opiskeluoikeus.suoritukset.get
      .filter(s => s.tyyppi.koodiarvo == SUORITUSTYYPPI_PERUSOPETUKSENVUOSILUOKKA).flatMap(s => {
        val luokkaAste = s.koulutusmoduuli.flatMap(m => m.tunniste.map(t => t.koodiarvo)).getOrElse(dummy())
        s.alkamispäivä match
          // Luodaan lähtökoulu vain jos alkamispäivä määritelty. KOSKI-tiimin mukaan alkamispäivät määritelty ainakin
          // viimeisen kuuden vuoden ajalta.
          case Some(pvm) if luokkaAste==haluttuLuokkaAste =>
            val luokka = s.luokka.getOrElse(dummy())
            val supatila = parseTila(opiskeluoikeus, Some(s)).map(tila => convertKoskiTila(tila.koodiarvo))
            val alkamispaiva = LocalDate.parse(pvm)
            val loppuPaivamaara = (s.vahvistus.map(v => LocalDate.parse(v.`päivä`)), seuraavanAsteenAlkamispaiva) match
              case (None, None) => None
              case (Some(vahvistusPaivamaara), None) => Some(vahvistusPaivamaara)
              case (None, Some(seuraavanAsteenAlkamispaiva)) => Some(seuraavanAsteenAlkamispaiva)
              case (Some(vahvistusPaivamaara), Some(seuraavanAsteenAlkamispaiva)) => Some(vahvistusPaivamaara.min(seuraavanAsteenAlkamispaiva))
            val valmistumisVuosi = if loppuPaivamaara.isDefined then loppuPaivamaara.map(_.getYear) else Some(alkamispaiva.getYear + 1)
            Some(parseLasnaolot(opiskeluoikeus, Some(alkamispaiva), loppuPaivamaara).map(l => {
              Lahtokoulu(l._1, l._2, oppilaitos.oid, valmistumisVuosi, luokka, supatila, yhteisenAineenArvosanaPuuttuu, LahtokouluTyyppi.valueOf(s"VUOSILUOKKA_$luokkaAste"))
            }))
          case default => None
      })
      .flatten
      .toList
      .sortBy(_.suorituksenAlku)
      .reverse

  def toPerusopetuksenOppimaara(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus, koodistoProvider: KoodistoProvider): Option[PerusopetuksenOppimaara] = {
    if (isKotiopetus(opiskeluoikeus)) {
      LOG.info(s"Ei muodosteta oppimäärää Koski-opiskeluoikeudelle ${opiskeluoikeus.oid}, koska kyse on kotiopetuslaisesta.")
      None
    } else if (!isYlaAste(opiskeluoikeus)) {
      LOG.info(s"Ei muodosteta oppimäärää Koski-opiskeluoikeudelle ${opiskeluoikeus.oid}, koska luokkien 7-9 suorituksia ei löytynyt.")
      None
    } else {
      val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          o.nimi,
          o.oid)).getOrElse(dummy())

      val supatila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo))
      val aineet = suoritus.osasuoritukset.map(os => os.flatMap(os => toPerusopetuksenOppiaine(os, koodistoProvider))).getOrElse(Set.empty)
      val arvosanaPuuttuu = suoritus.osasuoritukset.exists(os => os.exists(os => isPakollinenJaArviointiPuuttuu(os)))

      val lahtokoulut9 = getPerusopetuksenLahtokoulut(opiskeluoikeus, "9", Some(arvosanaPuuttuu), None, koodistoProvider)
      val lahtokoulut8 = getPerusopetuksenLahtokoulut(opiskeluoikeus, "8", None, lahtokoulut9.map(_.suorituksenAlku).minOption, koodistoProvider)
      val lahtokoulut7 = getPerusopetuksenLahtokoulut(opiskeluoikeus, "7", None, (lahtokoulut9 ++ lahtokoulut8).map(_.suorituksenAlku).minOption, koodistoProvider)
      val lahtokoulut = lahtokoulut9 ++ lahtokoulut8 ++ lahtokoulut7

      val maxLuokkaAste = lahtokoulut
        .map(_.suoritusTyyppi)
        .collect {
          case LahtokouluTyyppi.VUOSILUOKKA_7 => 7
          case LahtokouluTyyppi.VUOSILUOKKA_8 => 8
          case LahtokouluTyyppi.VUOSILUOKKA_9 => 9
        }
        .maxOption

      Some(PerusopetuksenOppimaara(
        tunniste = UUID.randomUUID(),
        versioTunniste = None,
        oppilaitos = oppilaitos,
        luokka = lahtokoulut.maxByOption(_.suorituksenAlku).map(_.luokka),
        koskiTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
        supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
        suoritusKieli = suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
        koulusivistyskieli = suoritus.koulusivistyskieli.map(kielet => kielet.map(kieli => asKoodiObject(kieli))).getOrElse(Set.empty),
        yksilollistaminen = getYksilollistaminen(opiskeluoikeus, suoritus),
        aloitusPaivamaara = parseAloitus(opiskeluoikeus),
        vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
        aineet = aineet,
        lahtokoulut = lahtokoulut,
        syotetty = false,
        vuosiluokkiinSitoutumatonOpetus = opiskeluoikeus.lisätiedot.exists(_.vuosiluokkiinSitoutumatonOpetus.exists(_.equals(true))),
        luokkaAste = maxLuokkaAste
      ))
    }
  }

  def toAikuistenPerusopetuksenOppimaara(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus, koodistoProvider: KoodistoProvider): PerusopetuksenOppimaara = {
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        o.nimi,
        o.oid)).getOrElse(dummy())

    val aloitusPaivamaara = parseAloitus(opiskeluoikeus)
    val vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    val supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo))
    val aineet = suoritus.osasuoritukset.map(os => os.flatMap(os => toPerusopetuksenOppiaine(os, koodistoProvider))).getOrElse(Set.empty)
    val valmistumisVuosi = if vahvistusPaivamaara.isDefined then vahvistusPaivamaara.map(_.getYear) else aloitusPaivamaara.map(_.getYear + 1)
    val yhteisenAineenArvosanaPuuttuu = suoritus.osasuoritukset.exists(os => os.exists(os => isPakollinenJaArviointiPuuttuu(os)))

    PerusopetuksenOppimaara(
     tunniste = UUID.randomUUID(),
      versioTunniste = None,
      oppilaitos = oppilaitos,
      luokka = None, // TODO: onko tätä saatavissa?
      koskiTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      supaTila = supaTila.getOrElse(dummy()),
      suoritusKieli = suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      koulusivistyskieli = Set.empty,
      yksilollistaminen = getYksilollistaminen(opiskeluoikeus, suoritus),
      aloitusPaivamaara = aloitusPaivamaara,
      vahvistusPaivamaara = vahvistusPaivamaara,
      aineet = aineet,
      lahtokoulut =
        parseLasnaolot(opiskeluoikeus, None, vahvistusPaivamaara).map(l =>
          Lahtokoulu(l._1, l._2, oppilaitos.oid, valmistumisVuosi, AIKUISTEN_PERUSOPETUS.defaultLuokka.get, supaTila, Some(yhteisenAineenArvosanaPuuttuu), AIKUISTEN_PERUSOPETUS)),
      syotetty = false,
      vuosiluokkiinSitoutumatonOpetus = opiskeluoikeus.lisätiedot.exists(_.vuosiluokkiinSitoutumatonOpetus.exists(_.equals(true))),
      luokkaAste = None
    )
  }

  def toPerusopetukseenValmistavaOpetus(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): PerusopetukseenValmistavaOpetus = {
    val oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
      fi.oph.suorituspalvelu.business.Oppilaitos(
        o.nimi,
        o.oid)).getOrElse(dummy())

    val aloitusPaivamaara = parseAloitus(opiskeluoikeus)
    val vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    val supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo))
    val valmistumisVuosi = if vahvistusPaivamaara.isDefined then vahvistusPaivamaara.map(_.getYear) else aloitusPaivamaara.map(_.getYear + 1)

    PerusopetukseenValmistavaOpetus(lahtokoulut =
        parseLasnaolot(opiskeluoikeus, None, vahvistusPaivamaara).map(l =>
          Lahtokoulu(l._1, l._2, oppilaitos.oid, valmistumisVuosi, PERUSOPETUKSEEN_VALMISTAVA_OPETUS.defaultLuokka.get, supaTila, None, PERUSOPETUKSEEN_VALMISTAVA_OPETUS))
    )
  }

  def toEbOppiaineenOsasuoritus(osaSuoritus: KoskiOsaSuoritus): EBOppiaineenOsasuoritus = {
    //Voiko eb-alaosasuorituksella olla useita arviointeja? Jos voi, voiko arvioinneilla olla erilaisia koodistoja? Käytetäänkö aina koodistoa arviointiasteikkoeuropeanschoolofhelsinkifinalmark?
    val parasArviointi: Option[KoskiArviointi] = {
      val arvioinnit = osaSuoritus.arviointi.getOrElse(Set.empty)
        .filter(_.arvosana.koodistoUri == "arviointiasteikkoeuropeanschoolofhelsinkifinalmark")
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
    EBOppiaine(
      tunniste = UUID.randomUUID(),
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l => EBLaajuus(l.arvo, asKoodiObject(l.yksikkö.get)))),
      suorituskieli = osaSuoritus.suorituskieli.map(suoritusKieli => asKoodiObject(suoritusKieli)),
      osasuoritukset = osaSuoritus.osasuoritukset.map((osaSuoritukset: Set[KoskiOsaSuoritus]) => {
        //Käsitellään vain sellaiset osasuoritukset, joilla on ainakin yksi arviointi.
        val osaSuorituksetJoillaArviointi = osaSuoritukset.filter(o => o.arviointi.exists(_.nonEmpty))
        osaSuorituksetJoillaArviointi.map(osaSuoritus => toEbOppiaineenOsasuoritus(osaSuoritus))
      }).getOrElse(Set.empty))
  }

  def toDIAOppiaineenKoeSuoritus(osaSuoritus: KoskiOsaSuoritus): DIAOppiaineenKoesuoritus = {
    //Voiko dia-alaosasuorituksella olla useita arviointeja? Jos voi, voiko arvioinneilla olla erilaisia koodistoja? Käytetäänkö aina koodistoa arviointiasteikkodiatutkinto?
    val parasArviointi: Option[KoskiArviointi] = {
      val arvioinnit = osaSuoritus.arviointi.getOrElse(Set.empty)
        .filter(_.arvosana.koodistoUri == "arviointiasteikkodiatutkinto")
      valitseParasArviointi(arvioinnit)
    }

    DIAOppiaineenKoesuoritus(
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      arvosana = parasArviointi.map(pa => DIAArvosana(asKoodiObject(pa.arvosana), pa.hyväksytty)).getOrElse(dummy()),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l => DIALaajuus(l.arvo, asKoodiObject(l.yksikkö.get))))
    )
  }

  def toDiaOppiaine(osaSuoritus: KoskiOsaSuoritus): DIAOppiaine = {
    val vastaavuustodistuksenTiedot = osaSuoritus.vastaavuustodistuksenTiedot
      .map(vtt =>
        DIAVastaavuustodistuksenTiedot(
          vtt.keskiarvo,
          DIALaajuus(
            vtt.lukioOpintojenLaajuus.arvo,
            asKoodiObject(vtt.lukioOpintojenLaajuus.yksikkö.get))))

    val kirjallinen = osaSuoritus.osasuoritukset.getOrElse(Set.empty).find(o => o.koulutusmoduuli.flatMap(_.tunniste).exists(t => t.koodiarvo == "kirjallinenkoe"))
    val suullinen = osaSuoritus.osasuoritukset.getOrElse(Set.empty).find(o => o.koulutusmoduuli.flatMap(_.tunniste).exists(t => t.koodiarvo == "suullinenkoe"))
    DIAOppiaine(
      tunniste = UUID.randomUUID(),
      nimi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      koodi = osaSuoritus.koulutusmoduuli.flatMap(k => k.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      laajuus = osaSuoritus.koulutusmoduuli.flatMap(k => k.laajuus.map(l => DIALaajuus(l.arvo, asKoodiObject(l.yksikkö.get)))),
      kieli = osaSuoritus.koulutusmoduuli.flatMap(km => km.kieli.map(k => asKoodiObject(k))),
      osaAlue = osaSuoritus.koulutusmoduuli.flatMap(_.osaAlue).map(o => asKoodiObject(o)),
      vastaavuustodistuksenTiedot = vastaavuustodistuksenTiedot,
      kirjallinenKoe = kirjallinen.map(toDIAOppiaineenKoeSuoritus),
      suullinenKoe = suullinen.map(toDIAOppiaineenKoeSuoritus)
    )
  }

  def toDiaTutkinto(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): DIATutkinto = {
    DIATutkinto(
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
      suorituskieli = suoritus.suorituskieli.map(suoritusKieli => asKoodiObject(suoritusKieli)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritus.osasuoritukset.map(oss => oss.map(o => toDiaOppiaine(o))).getOrElse(Set.empty).filter(o => o.kirjallinenKoe.isDefined || o.suullinenKoe.isDefined || o.vastaavuustodistuksenTiedot.isDefined),
    )
  }

  def toIbOppiaine(osaSuoritus: KoskiOsaSuoritus): IBOppiaineSuoritus = {
    val parasPredictedArviointi: Option[KoskiArviointi] = valitseParasArviointi(
      osaSuoritus.predictedArviointi.getOrElse(Set.empty).filter(_.arvosana.koodistoUri == "arviointiasteikkoib")
    )
    val koulutusmoduuli = osaSuoritus.koulutusmoduuli

    IBOppiaineSuoritus(
      tunniste = UUID.randomUUID(),
      nimi = koulutusmoduuli.flatMap(_.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      koodi = koulutusmoduuli.flatMap(_.tunniste.map(t => asKoodiObject(t))).getOrElse(dummy()),
      ryhma = koulutusmoduuli.flatMap(k => k.`ryhmä`.map(r => IBOppiaineRyhma(r.nimi, asKoodiObject(r)))).getOrElse(dummy()),
      predictedArvosana = parasPredictedArviointi.map(pa => IBArvosana(asKoodiObject(pa.arvosana), pa.hyväksytty)),
      laajuus = koulutusmoduuli.flatMap(_.laajuus.map(l => IBLaajuus(l.arvo, asKoodiObject(l.yksikkö.get)))),
      suorituskieli = osaSuoritus.suorituskieli.map(asKoodiObject)
    )
  }

  def toIbTutkinto(opiskeluoikeus: KoskiOpiskeluoikeus, suoritus: KoskiSuoritus): IBTutkinto = {
    IBTutkinto(
      tunniste = UUID.randomUUID(),
      nimi = suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(_.nimi)).getOrElse(dummy()),
      koodi = suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(asKoodiObject(_))).getOrElse(dummy()),
      oppilaitos = opiskeluoikeus.oppilaitos.map(o =>
        fi.oph.suorituspalvelu.business.Oppilaitos(
          Kielistetty(
            o.nimi.fi,
            o.nimi.sv,
            o.nimi.en,
          ),
          o.oid)).getOrElse(dummy()),
      suorituskieli = suoritus.suorituskieli.map(asKoodiObject),
      koskiTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => asKoodiObject(tila)).getOrElse(dummy()),
      supaTila = parseTila(opiskeluoikeus, Some(suoritus)).map(tila => convertKoskiTila(tila.koodiarvo)).getOrElse(dummy()),
      aloitusPaivamaara = parseAloitus(opiskeluoikeus),
      vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      osasuoritukset = suoritus.osasuoritukset.map(_.map(o => toIbOppiaine(o))).getOrElse(Set.empty))
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
      aloitusPaivamaara = parseAloitus(opiskeluoikeus),
      vahvistusPaivamaara = suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`)),
      suoritusKieli = None, // Ei saatavilla Koskesta lukion oppimäärälle
      koulusivistyskieli = suoritus.koulusivistyskieli.map(kielet => kielet.map(kieli => asKoodiObject(kieli))).getOrElse(Set.empty)
    )
  }

  def parseOpiskeluoikeudet(opiskeluoikeudet: Seq[KoskiOpiskeluoikeus], koodistoProvider: KoodistoProvider): Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus] =
    opiskeluoikeudet.flatMap {
      case opiskeluoikeus if isMitatoitu(opiskeluoikeus) => Some(PoistettuOpiskeluoikeus(opiskeluoikeus.oid))
      case opiskeluoikeus if opiskeluoikeus.poistettu.contains(true) => Some(PoistettuOpiskeluoikeus(opiskeluoikeus.oid))
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
          asKoodiObject(opiskeluoikeus.tyyppi.get),
          opiskeluoikeus.oppilaitos.get.oid,
          toSuoritukset(Seq(opiskeluoikeus), koodistoProvider),
          opiskeluoikeus.tila,
          opiskeluoikeus.tila.map(t => convertOpiskeluoikeusJaksot(t.opiskeluoikeusjaksot)).getOrElse(dummy())))
  }

  val SUORITUSTYYPPI_AMMATILLINENTUTKINTO                       = "ammatillinentutkinto"
  val SUORITUSTYYPPI_AMMATILLINENTUTKINTOOSITTAINEN             = "ammatillinentutkintoosittainen"
  val SUORITUSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIMAARA           = "aikuistenperusopetuksenoppimaara"
  val SUORITUSTYYPPI_PERUSOPETUKSENOPPIMAARA                    = "perusopetuksenoppimaara"
  val SUORITUSTYYPPI_PERUSOPETUKSENVUOSILUOKKA                  = "perusopetuksenvuosiluokka"
  val SUORITUSTYYPPI_NUORTENPERUSOPETUKSENOPPIAINEENOPPIMAARA   = "nuortenperusopetuksenoppiaineenoppimaara"
  val SUORITUSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIAINEENOPPIMAARA = "perusopetuksenoppiaineenoppimaara"
  val SUORITUSTYYPPI_PERUSOPETUKSEENVALMISTAVAOPETUS            = "perusopetukseenvalmistavaopetus"
  val SUORITYSTYYPPI_TELMA                                      = "telma"
  val SUORITYSTYYPPI_TUVAKOULUTUKSENSUORITUS                    = "tuvakoulutuksensuoritus"
  val SUORITYSTYYPPI_VAPAASIVISTYSTYOSUORITUS                   = "vstoppivelvollisillesuunnattukoulutus"
  val SUORITYSTYYPPI_EB                                         = "ebtutkinto"
  val SUORITYSTYYPPI_DIA                                        = "diatutkintovaihe"
  val SUORITUSTYYPPI_IB                                         = "ibtutkinto"
  val SUORITYSTYYPPI_LUKIONOPPIMAARA                            = "lukionoppimaara"

  def toSuoritukset(opiskeluoikeudet: Seq[KoskiOpiskeluoikeus], koodistoProvider: KoodistoProvider, allowMissingFieldsForTests: Boolean = false): Set[fi.oph.suorituspalvelu.business.Suoritus] = {
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      opiskeluoikeudet.flatMap(opiskeluoikeus =>
        opiskeluoikeus.suoritukset.get.flatMap(suoritus =>
          suoritus.tyyppi.koodiarvo match
            case SUORITUSTYYPPI_AMMATILLINENTUTKINTO              => Some(toAmmatillinenTutkinto(opiskeluoikeus, suoritus))
            case SUORITUSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIMAARA  => Some(toAikuistenPerusopetuksenOppimaara(opiskeluoikeus, suoritus, koodistoProvider))
            case SUORITUSTYYPPI_AMMATILLINENTUTKINTOOSITTAINEN    => Some(toAmmatillinenTutkintoOsittainen(opiskeluoikeus, suoritus))
            case SUORITUSTYYPPI_PERUSOPETUKSENOPPIMAARA           => toPerusopetuksenOppimaara(opiskeluoikeus, suoritus, koodistoProvider)
            case SUORITUSTYYPPI_PERUSOPETUKSENVUOSILUOKKA         => None // vuosiluokkien tiedot käsitellään osana perusopetuksen oppimäärää
            case SUORITUSTYYPPI_NUORTENPERUSOPETUKSENOPPIAINEENOPPIMAARA
              if suoritus.arviointi.exists(_.nonEmpty)            => Some(toPerusopetuksenOppiaineenOppimaara(opiskeluoikeus, suoritus))
            case SUORITUSTYYPPI_AIKUISTENPERUSOPETUKSENOPPIAINEENOPPIMAARA
              if suoritus.arviointi.exists(_.nonEmpty)            => Some(toPerusopetuksenOppiaineenOppimaara(opiskeluoikeus, suoritus))
            case SUORITUSTYYPPI_PERUSOPETUKSEENVALMISTAVAOPETUS   => Some(toPerusopetukseenValmistavaOpetus(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_TELMA                             => Some(toTelma(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_TUVAKOULUTUKSENSUORITUS           => Some(toTuva(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_VAPAASIVISTYSTYOSUORITUS          => Some(toVapaaSivistystyoKoulutus(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_EB                                => Some(toEbTutkinto(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_DIA                               => Some(toDiaTutkinto(opiskeluoikeus, suoritus))
            case SUORITUSTYYPPI_IB                                => Some(toIbTutkinto(opiskeluoikeus, suoritus))
            case SUORITYSTYYPPI_LUKIONOPPIMAARA                   => Some(toLukionOppimaara(opiskeluoikeus, suoritus))
            case default => None)).toSet
    finally
      allowMissingFields.set(false)
  }
}
