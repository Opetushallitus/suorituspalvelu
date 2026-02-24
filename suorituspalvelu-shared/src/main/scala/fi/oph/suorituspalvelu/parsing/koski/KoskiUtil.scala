package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, GeneerinenOpiskeluoikeus, Lahtokoulu, LahtokouluTyyppi, Opiskeluoikeus, PerusopetukseenValmistavaOpetus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, SuoritusTila, Telma, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.resource.api.LahtokouluAuthorization
import fi.oph.suorituspalvelu.util.KoodistoProvider

import java.time.LocalDate
import scala.jdk.OptionConverters.*

val NOT_DEFINED_PLACEHOLDER = "_"

object KoskiUtil {

  val KOODISTO_OPPIAINEET = "koskioppiaineetyleissivistava"

  def includePerusopetuksenOppiaine(osaSuoritus: KoskiOsaSuoritus, koodistoProvider: KoodistoProvider): Boolean = {
    val oppiaineKoodi = osaSuoritus.koulutusmoduuli.get.tunniste.get.koodiarvo

    val hasArviointi = osaSuoritus.arviointi.isDefined
    val isKoulukohtainen = !koodistoProvider.haeKoodisto(KOODISTO_OPPIAINEET).contains(oppiaineKoodi)
    val aineTiedossa = !"XX".equals(oppiaineKoodi)
    val pakollinen = osaSuoritus.koulutusmoduuli.get.pakollinen.get
    val laajuusYli2vvk = osaSuoritus.koulutusmoduuli.get.laajuus.exists(l => l.arvo > 2)

    hasArviointi && !isKoulukohtainen && aineTiedossa && (pakollinen || laajuusYli2vvk)
  }

  def getLahtokouluMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Seq[Lahtokoulu] =
    opiskeluoikeudet.collect {
      case oo: PerusopetuksenOpiskeluoikeus => oo.suoritukset.collect {
        // jos vuosiluokan suoritus on kesken ja tiedot tuotu SUPAan tiedetään että henkilö on 7-9. -luokkalainen. Jos valmis (tai ehkä keskeytynyt?) voi olla esim. kk-hakija
        // ylimääräiset lähtökoulut eivät kuitenkaan haittaa koska opoille näytetään vain kuluva ja seuraava vuosi
        case s: PerusopetuksenOppimaara => s.lahtokoulut
      }.flatten
      case oo: AmmatillinenOpiskeluoikeus => oo.suoritukset.collect {
        case s: Telma => s.lahtokoulu
      }
      case oo: GeneerinenOpiskeluoikeus => oo.suoritukset.collect {
        case s: Tuva => s.lahtokoulu
        case s: VapaaSivistystyo => s.lahtokoulu
        case s: PerusopetukseenValmistavaOpetus => s.lahtokoulu
      }
    }.flatten.toSeq.sortBy(ov => ov.suorituksenAlku)

  /**
   * Luo lähtökoulutietojen perusteella ajallisen jatkumon oppilaitoksista joiden (tietyillä) rooleilla on oikeus
   * tarkastella henkilön tietoja niin että oikeus on yhdellä oppilaitoksella kerrallaan. Listaa luodaan seuraavasti:
   * - oikeus alkaa relevantin suorituksen alkupäivästä
   * - oikeus päättyy siihen päivään (ei-inklusiivinen) kun ajallisesti seuraava oikeus alkaa
   * - jos ajallisesti seuraavaa oikeutta ei ole, oikeus päättyy seuraavan vuoden tammikuun loppuun
   * - jos suoritus on kesken päättymispäivää ei ole määritelty
   *
   * @param lahtokoulut lähtökoulutiedot joiden perusteella oikeudet luodaan
   * @return
   */
  def luoLahtokouluAuthorizations(lahtokoulut: Seq[Lahtokoulu]): Seq[LahtokouluAuthorization] =
    if(lahtokoulut.isEmpty)
      Seq.empty
    else {
      val aikajarjestetyt = lahtokoulut.sortBy(_.suorituksenAlku)
      aikajarjestetyt.zip(aikajarjestetyt.tail.map(e => Some(e)) :+ None).map((curr, next) => {
        val loppuPaivamaara =
          (curr.suorituksenLoppu, next.map(n => n.suorituksenAlku)) match
            case (None, None) => None
            case (Some(currLoppu), None) => Some(LocalDate.parse(s"${currLoppu.getYear + 1}-02-01")) // loppupäivä ei-inklusiivinen
            case (_, Some(nextAlku)) => Some(nextAlku)
        LahtokouluAuthorization(curr.oppilaitosOid, curr.suorituksenAlku, loppuPaivamaara.toJava, curr.luokka, curr.suoritusTyyppi.toString)
      })
    }

  /**
   * Kertoo löytyykö suorituksista kriteerit täyttäviä lähtökouluja. Tätä tietoa käytetään ratkaisemaan:
   *  - Onko lähettävän katselijalla oikeus nähdä henkilön suoritukset SUPAssa
   *  - Pitääkö Muuttuneet KOSKI-tiedot päivittää SUPAan (lähtökohta on että jos henkilön tiedot näkyvät tarkastus-
   *    näkymässä niin pitää päivittää). Samaa päättelyä käytetään siis siihen saako suoritukset nähdä SUPAssa ja
   *    päivitetäänkö suoritustieto. Jos tätä halutaan muuttaa niin asiaa kannattaa harkita ainakin kahdesti. Tilanne
   *    jossa tiedot näkyvät mutta eivät päivity voi aiheuttaa jonkinmoista sekaannusta.
   *
   * @param ajanhetki         Ajanhetki jolloin tarkastelu suoritetaan, ts. onko tällä ajanhetkellä kriteerit (oppilaitos, tyyppi)
   *                          täyttäviä lähtökouluja (eri asia kuin suoritustietojen ajanhetki). Periaatteena on että
   *                          lähtökoulu on voimassa vielä suorituksen valmistumis- tai keskeytymispäivästä seuraavan vuoden
   *                          tammikuun loppuun.
   * @param oppilaitosOids    Jos tämä määritelty, haetaan vain määriteltyjä oppilaitoksia
   * @param lahtokouluTyypit  Jos tämä määritelty, haetaan vain tietyn tyyppisiä lähtökouluja
   * @param opiskeluoikeudet  Suoritustiedot joista lähtökouluja haetaan
   *
   * @return Löytyykö annetuista suoritustiedoista jokin kriteerit täyttävä lähtökoulu annetulla ajanhetkellä
   */
  def onkoJokinLahtokoulu(ajanhetki: LocalDate, oppilaitosOids: Option[Set[String]], lahtokouluTyypit: Option[Set[LahtokouluTyyppi]], opiskeluoikeudet: Set[Opiskeluoikeus]): Boolean =
    val ohjausvastuut = getLahtokouluMetadata(opiskeluoikeudet)

    ohjausvastuut.exists(o =>
      (oppilaitosOids.isEmpty || oppilaitosOids.exists(_.contains(o.oppilaitosOid))) &&
        (lahtokouluTyypit.isEmpty || lahtokouluTyypit.exists(_.contains(o.suoritusTyyppi))) &&
        // katseluoikeus on suorituksen päättymisvuotta seuraavan vuoden tammikuun loppuun
        (o.suorituksenLoppu.isEmpty || o.suorituksenLoppu.exists(l => !LocalDate.parse(s"${l.getYear + 1}-01-31").isBefore(LocalDate.now))))

}