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

  /**
   * Henkilö on ysiluokalla jos:
   * - löytyy opiskeluoikeus joka ei ole eronnut-tilassa
   * - ja sen alta löytyy vuosiluokka joka on ysiluokka
   * - ja ei löydy vahvistettua perusopetuksen oppimäärän suoritusta
   *
   * @param opiskeluoikeudet
   * @return
   */
  def isYsiluokkalainen(opiskeluoikeudet: Set[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Boolean = {
    val hasYsiluokka = opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .filter(o => o.tila!=SuoritusTila.KESKEYTYNYT)
      .exists(o => o.suoritukset
          .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
          .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
          .exists(s => s.koodi.arvo == "9"))

    val hasValmisPerusopetus = opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .filter(o => o.tila==SuoritusTila.VALMIS)
      .exists(o => o.suoritukset.exists(s => s.isInstanceOf[PerusopetuksenOppimaara]))

    hasYsiluokka && !hasValmisPerusopetus
  }
  
  def includePerusopetuksenOppiaine(osaSuoritus: KoskiOsaSuoritus, koodistoProvider: KoodistoProvider): Boolean = {
    val oppiaineKoodi = osaSuoritus.koulutusmoduuli.get.tunniste.get.koodiarvo

    val hasArviointi = osaSuoritus.arviointi.isDefined
    val isKoulukohtainen = !koodistoProvider.haeKoodisto(KOODISTO_OPPIAINEET).contains(oppiaineKoodi)
    val aineTiedossa = !"XX".equals(oppiaineKoodi)
    val pakollinen = osaSuoritus.koulutusmoduuli.get.pakollinen.get
    val laajuusYli2vvk = osaSuoritus.koulutusmoduuli.get.laajuus.exists(l => l.arvo > 2)

    hasArviointi && !isKoulukohtainen && aineTiedossa && (pakollinen || laajuusYli2vvk)
  }

/*
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
    "YH"
  )

  def yhteisenAineenArvosanaPuuttuu(opiskeluoikeudet: Set[Opiskeluoikeus]): Boolean =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenOppimaara])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenOppimaara])
        .map(oppimaara => {
          YHTEISET_AINEET.forall(yhteinenAine => oppimaara.aineet.exists(oppimaaranAine => oppimaaranAine.koodi.arvo == yhteinenAine))
        })).isEmpty

  // Vuosiluokan suoritus on kesken ja tiedot tuotu SUPAan tiedetään että henkilö on 7-9. -luokkalainen. Jos valmis (tai ehkä keskeytynyt?) voi olla esim. kk-hakija
  // ylimääräiset ohjausvastuut eivät kuitenkaan haittaa koska opoille näytetään vain kuluva ja seuraava vuosi
  // on mahdollista olla näyttämättä vuosiluokan suorituksia UI:ssa, paitsi 7. tai 8. luokka jos ei ole ysiä
  def getVuosiluokkaOhjausvastuuMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Lahtokoulu] = {
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
        .filter(vuosiluokka => Set("7", "8", "9").contains(vuosiluokka.koodi.arvo))
        .map(vuosiluokka => {
          val aloitusPaiva = vuosiluokka.alkamisPaiva.get
          val valmistumisVuosi = vuosiluokka.vahvistusPaivamaara match
            case Some(pvm) => pvm.getYear                                   // valmistunut
            case None => aloitusPaiva match
              case pvm if pvm.getMonth.getValue < 6 => aloitusPaiva.getYear // todennäköisesti vaihtanut koulua keväällä
              case _ => aloitusPaiva.getYear + 1                            // normaalitapaus
          val arvosanaPuuttuu = if(vuosiluokka.koodi.arvo=="9")
            Some(yhteisenAineenArvosanaPuuttuu(opiskeluoikeudet))
          else
            None                                                            // muilla kuin ysiluokkalaisilla kaikki aineet eivät voikaan olla kasassa
          // TODO: annetaan toistaiseksi kaikille dummy-luokkatieto kunnes saadaan oikea koskesta
          Lahtokoulu(aloitusPaiva, vuosiluokka.vahvistusPaivamaara, o.oppilaitosOid, Some(aloitusPaiva.getYear + 1), Some("9A"), Some(o.tila), arvosanaPuuttuu, LahtokouluTyyppi.valueOf(s"VUOSILUOKKA_${vuosiluokka.koodi.arvo}"))
        }))
  }
*/

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




/*
        .filter(l => Set(7, 8, 9).contains(l.luokkaAste))                   // tämä on defensiivistä, muita luokkia ei pitäisi edes parseroida
        .map(vuosiluokka => {
          val aloitusPaiva = vuosiluokka.aloitusPaivamaara.get
          val valmistumisVuosi = vuosiluokka.vahvistusPaivamaara match
            case Some(pvm) => pvm.getYear                                   // valmistunut
            case None => aloitusPaiva match
              case pvm if pvm.getMonth.getValue < 6 => aloitusPaiva.getYear // todennäköisesti vaihtanut koulua keväällä
              case _ => aloitusPaiva.getYear + 1                            // normaalitapaus
          val arvosanaPuuttuu = if(vuosiluokka.luokkaAste==9)
            Some(yhteisenAineenArvosanaPuuttuu(opiskeluoikeudet))
          else
            None                                                            // muilla kuin ysiluokkalaisilla kaikki aineet eivät voikaan olla kasassa
          // TODO: annetaan toistaiseksi kaikille dummy-luokkatieto kunnes saadaan oikea koskesta

          // Huomioita:
          //  - ohjaustavastuun loppupäivämääränä pitää käyttää vuosiluokan eikä opiskeluoikeuden vahvistuspäivämäärää, tämä koska mukana on myös 7. ja 8.-luokkalaisia
          Ohjausvastuu(aloitusPaiva, vuosiluokka.vahvistusPaivamaara, o.oppilaitosOid, Some(aloitusPaiva.getYear + 1), Some("9A"), Some(o.tila), arvosanaPuuttuu, OhjausvastuuPeruste.valueOf(s"VUOSILUOKKA_${vuosiluokka.luokkaAste}"))
        }))
*/
  }
  
  def getTelmaLahtokouluMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Lahtokoulu] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.Telma])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.Telma])
        .map(telma => {
          val aloitusPaiva = o.jaksot.map(j => j.alku).min
          val valmistumisVuosi = aloitusPaiva.getYear + 1
          Lahtokoulu(aloitusPaiva, telma.vahvistusPaivamaara, o.oppilaitos.oid, Some(aloitusPaiva.getYear + 1), None, Some(telma.supaTila), None, LahtokouluTyyppi.TELMA)
        }))

  def getTuvaLahtokouluMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Lahtokoulu] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[GeneerinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[GeneerinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.Tuva])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.Tuva])
        .map(tuva => {
          val aloitusPaiva = o.jaksot.map(j => j.alku).min
          val valmistumisVuosi = aloitusPaiva.getYear + 1
          Lahtokoulu(aloitusPaiva, tuva.vahvistusPaivamaara, o.oppilaitosOid, Some(aloitusPaiva.getYear + 1), None, Some(tuva.supaTila), None, LahtokouluTyyppi.TUVA)
        }))

  def getVSTLahtokouluMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Lahtokoulu] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[GeneerinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[GeneerinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.VapaaSivistystyo])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.VapaaSivistystyo])
        .map(vst => {
          val aloitusPaiva = o.jaksot.map(j => j.alku).min
          val valmistumisVuosi = aloitusPaiva.getYear + 1
          Lahtokoulu(aloitusPaiva, vst.vahvistusPaivamaara, o.oppilaitosOid, Some(aloitusPaiva.getYear + 1), None, Some(vst.supaTila), None, LahtokouluTyyppi.VAPAA_SIVISTYSTYO)
        }))

  def getLahtokouluMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Seq[Lahtokoulu] =
    // haetaan eri opinnoista seuraavat ohjausvastuut
    Seq(
      getVuosiluokatLahtokouluMetadata(opiskeluoikeudet),
      getTelmaLahtokouluMetadata(opiskeluoikeudet),
      getTuvaLahtokouluMetadata(opiskeluoikeudet),
      getVSTLahtokouluMetadata(opiskeluoikeudet),
    ).flatten.sortBy(ov => ov.suorituksenAlku)
  
  /*
  tätä pitää käyttää lähtökoulun yksikäsitteiseen määrittämiseen, muttei katseluoikeuden määrittämiseen:
    
      val rajatut = ohjausvastuut.zip(ohjausvastuut.tail.map(e => Some(e)) :+ None).map((curr, next) => curr.copy(suorituksenLoppu = {
        (curr.suorituksenLoppu, next.map(n => n.suorituksenAlku)) match
          case (None, None) => None
          case (Some(currLoppu), None) => Some(currLoppu)
          case (None, Some(nextAlku)) => Some(nextAlku)
          case (Some(currLoppu), Some(nextAlku)) => Some(if currLoppu.isBefore(nextAlku) then currLoppu else nextAlku)
      }))
    
   */

  /**
   * Kertoo onko lähettävien katselijalla oikeus nähdä henkilön suoritukset tietyllä ajanhetkellä
   * // TODO: lisää ajanhetki, tätä tarvitaan kun esim. katsotaan onko lähettävällä ollut oikeus nähdä haun leikkuripäivänä
   * 
   * Samaa päättelyä käytetään sekä päätettäessä siitä päivitetäänkö muuttuneet KOSKI-tiedot ysiluokkalaisuuden perusteella
   * sekä siitä saako lähettavien katselija katsoa suoritukset ysiluokkalaisuuden perusteella. Jos tätä halutaan muuttaa
   * niin asiaa kannattaa harkita ainakin kahdesti. Tilanne jossa tiedot näkyvät mutta eivät päivity voi aiheuttaa
   * jonkinmoista sekaannusta.
   * 
   * @param oppilaitosOids
   * @param vuosi
   * @param opiskeluoikeudet
   * @return
   */
  def onkoJokinLahtokoulu(oppilaitosOids: Option[Set[String]], lahtokouluTyypit: Option[Set[LahtokouluTyyppi]], opiskeluoikeudet: Set[Opiskeluoikeus]): Boolean =
    val ohjausvastuut = getLahtokouluMetadata(opiskeluoikeudet)

    ohjausvastuut.exists(o =>
      (oppilaitosOids.isEmpty || oppilaitosOids.exists(_.contains(o.oppilaitosOid))) &&
        (lahtokouluTyypit.isEmpty || lahtokouluTyypit.exists(_.contains(o.suoritusTyyppi))) &&
        //(vuosi.isEmpty || o.valmistumisvuosi.contains(vuosi.get)) &&
        // katseluoikeus on suorituksen päättymisvuotta seuraavan vuoden tammikuun loppuun
        (o.suorituksenLoppu.isEmpty || o.suorituksenLoppu.exists(l => !LocalDate.parse(s"${l.getYear + 1}-01-31").isBefore(LocalDate.now))))

}