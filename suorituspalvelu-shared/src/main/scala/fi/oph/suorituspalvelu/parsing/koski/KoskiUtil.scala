package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{TELMA, VAPAA_SIVISTYSTYO, VUOSILUOKKA_7}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, GeneerinenOpiskeluoikeus, Lahtokoulu, LahtokouluTyyppi, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusTila}
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.slf4j.LoggerFactory

import java.time.{Instant, LocalDate}

val NOT_DEFINED_PLACEHOLDER = "_"

case class Ohjattavuus(oppilaitosOid: String, vahvistusVuosi: Option[Int], luokka: Option[String]) {

  def this(str: String) = this(str.split(":")(0), {
    str.split(":")(1) match
      case NOT_DEFINED_PLACEHOLDER => None
      case vuosi => Some(vuosi.toInt)
  }, {
    str.split(":")(2) match
      case NOT_DEFINED_PLACEHOLDER => None
      case luokka => Some(luokka)
  })

  override def toString(): String = s"$oppilaitosOid:${vahvistusVuosi.getOrElse(NOT_DEFINED_PLACEHOLDER)}:${luokka.getOrElse(NOT_DEFINED_PLACEHOLDER)}"
}

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

  // Vuosiluokan suoritus on kesken ja tiedot tuotu SUPAan tiedetään että henkilö on 7-9. -luokkalainen. Jos valmis (tai ehkä keskeytynyt?) voi olla esim. kk-hakija
  // ylimääräiset lähtökoulut eivät kuitenkaan haittaa koska opoille näytetään vain kuluva ja seuraava vuosi
  def getVuosiluokatLahtokouluMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Lahtokoulu] = {
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenOppimaara])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenOppimaara])
        .flatMap(s => s.lahtokoulut))
  }

  def getTelmaLahtokouluMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Lahtokoulu] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.Telma])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.Telma])
        .map(telma => telma.lahtokoulu))

  def getTuvaLahtokouluMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Lahtokoulu] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[GeneerinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[GeneerinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.Tuva])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.Tuva])
        .map(tuva => tuva.lahtokoulu))

  def getVSTLahtokouluMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Lahtokoulu] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[GeneerinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[GeneerinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.VapaaSivistystyo])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.VapaaSivistystyo])
        .map(vst => vst.lahtokoulu))

  def getLahtokouluMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Seq[Lahtokoulu] =
    // haetaan eri opinnoista seuraavat ohjausvastuut
    Seq(
      getVuosiluokatLahtokouluMetadata(opiskeluoikeudet),
      getTelmaLahtokouluMetadata(opiskeluoikeudet),
      getTuvaLahtokouluMetadata(opiskeluoikeudet),
      getVSTLahtokouluMetadata(opiskeluoikeudet),
    ).flatten.sortBy(ov => ov.suorituksenAlku)

  def haeViimeisinLahtokoulu(ajanhetki: LocalDate, opiskeluoikeudet: Set[Opiskeluoikeus]): Option[String] =
    None
    /* tätä pitää käyttää hakemuksen lähtökoulun yksikäsitteiseen määrittämiseen, muttei katseluoikeuden määrittämiseen:
        val rajatut = ohjausvastuut.zip(ohjausvastuut.tail.map(e => Some(e)) :+ None).map((curr, next) => curr.copy(suorituksenLoppu = {
          (curr.suorituksenLoppu, next.map(n => n.suorituksenAlku)) match
            case (None, None) => None
            case (Some(currLoppu), None) => Some(currLoppu)
            case (None, Some(nextAlku)) => Some(nextAlku)
            case (Some(currLoppu), Some(nextAlku)) => Some(if currLoppu.isBefore(nextAlku) then currLoppu else nextAlku)
        }))
     */

  /**
   * Kertoo löytyykö suorituksista kriteerit täyttäviä lähtökouluja. Tätä tietoa käytetään ratkaisemaan:
   *  - Onko lähettävän katselijalla oikeus nähdä henkilön suoritukset SUPAssa
   *  - Onko lähettävän katselijalla oikeus nähdä henkilön hakemukset Hakemuspalvelussa
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