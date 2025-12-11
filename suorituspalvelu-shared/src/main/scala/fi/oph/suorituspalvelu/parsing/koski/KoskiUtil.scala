package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, GeneerinenOpiskeluoikeus, Ohjausvastuu, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusTila}
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
//import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil.OHJATTAVA_METADATA_AVAIN
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

  /*
  val OHJATTAVA_METADATA_AVAIN  = "OHJATTAVA_OPPILAITOS_VUOSI_LUOKKA"
  
  def extractLuokat(oppilaitosOid: String, metadata: Map[String, Set[String]]): Set[String] =
    metadata.get(OHJATTAVA_METADATA_AVAIN)
      .map(arvot => arvot
        .map(arvo => new Ohjattavuus(arvo))
        .filter(arvo => arvo.oppilaitosOid == oppilaitosOid && arvo.luokka.isDefined)
        .map(arvo => arvo.luokka.get)).getOrElse(Set.empty)

  
  /**
   * Henkilö on ysiluokalla jos:
   * - löytyy opiskeluoikeus joka ei ole eronnut-tilassa
   * - ja sen alta löytyy vuosiluokka joka on ysiluokka
   * - ja ei löydy vahvistettua perusopetuksen oppimäärän suoritusta
   *
   * @param opiskeluoikeudet
   */
  def getOhjattavuudet(opiskeluoikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Set[Ohjattavuus] = {
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => {
        val ysiluokat = o.suoritukset
          .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
          .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
          .filter(s => {
            s.koodi.arvo == "9"
          })
        if(ysiluokat.size>1)
          throw new RuntimeException(s"Opiskeluoikeudessa ${o.oid} on useita ysiluokkia")

        val perusopetuksenOppimaarat = o.suoritukset
          .filter(s => s.isInstanceOf[PerusopetuksenOppimaara])
          .map(s => s.asInstanceOf[PerusopetuksenOppimaara])
        if(perusopetuksenOppimaarat.size>1)
          throw new RuntimeException(s"Opiskeluoikeudessa ${o.oid} on useita perusopetuksen oppimääriä")

        // TODO: annetaan toistaiseksi kaikille dummy-luokkatieto kunnes saadaan oikea koskesta
        (ysiluokat.headOption, perusopetuksenOppimaarat.headOption) match
          case (None, None) => None
          case (None, Some(oppimaara)) =>
            o.tila match
              case VALMIS => Some(Set(Ohjattavuus(oppimaara.oppilaitos.oid, Some(oppimaara.vahvistusPaivamaara.get.getYear), None)))
              case KESKEN => None // eivät ysillä
              case KESKEYTYNYT => None
          case (Some(ysiluokka), None) =>
            o.tila match
              case KESKEYTYNYT => None
              case default => Some(Set(
                Ohjattavuus(ysiluokka.oppilaitos.oid, None, Some("9A")),
                Ohjattavuus(ysiluokka.oppilaitos.oid, None, None)
              ))
          case (Some(ysiluokka), Some(oppimaara)) =>
            o.tila match
              case VALMIS => Some(Set(
                Ohjattavuus(o.oppilaitosOid, Some(oppimaara.vahvistusPaivamaara.get.getYear), Some("9A")),
                Ohjattavuus(o.oppilaitosOid, Some(oppimaara.vahvistusPaivamaara.get.getYear), None)
              ))
              case KESKEN => Some(Set(
                Ohjattavuus(o.oppilaitosOid, None, Some("9A")),
                Ohjattavuus(o.oppilaitosOid, None, None)
              ))
              case KESKEYTYNYT => None
      }).toSet.flatten.flatten
  }

  def isOrWasOrganisaationOhjattava(organisaatioOid: String, opiskeluoikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Boolean =
    getOhjattavuudet(opiskeluoikeudet).exists(t => t.oppilaitosOid==organisaatioOid)

  def getOhjattavienHakuMetadata(oppilaitosOid: String, vuosi: Int, luokka: Option[String], suoritusKesken: Boolean, arvosanaPuuttuu: Boolean): Seq[Map[String, Set[String]]] = {
    (vuosi, luokka) match
      case (vuosi, None) if LocalDate.now().getYear==vuosi =>
        // jos tämä vuosi mutta luokkaa ei määritelty, niin pitää olla joko kyseisen oppilaitoksen valmis perusopetuksen suoritus
        // tältä vuodelta, tai oppilaitoksen (ei valmis) ysiluokkalainen
        Seq(
          Map(OHJATTAVA_METADATA_AVAIN -> Set(Ohjattavuus(oppilaitosOid, Some(vuosi), None).toString())),
          Map(OHJATTAVA_METADATA_AVAIN -> Set(Ohjattavuus(oppilaitosOid, None, None).toString()))
        )
      case (vuosi, Some(luokka)) if LocalDate.now().getYear==vuosi =>
        // jos tämä vuosi ja luokka määritelty, pitää olla joko kyseisen oppilaitoksen ja luokan valmis tai
        // kesken olevan ysi
        Seq(
          Map(OHJATTAVA_METADATA_AVAIN -> Set(Ohjattavuus(oppilaitosOid, Some(vuosi), Some(luokka)).toString())),
          Map(OHJATTAVA_METADATA_AVAIN -> Set(Ohjattavuus(oppilaitosOid, None, Some(luokka)).toString()))
        )
      case (vuosi, None) =>
        // jos aikaisempi vuosi ja luokkaa ei määritelty, pitää olla kyseisen oppilaitoksen valmis perusopetuksen suoritus
        Seq(Map(OHJATTAVA_METADATA_AVAIN -> Set(Ohjattavuus(oppilaitosOid, Some(vuosi), None).toString()())))
      case (vuosi, Some(luokka)) =>
        // jos aikaisempi vuosi ja luokka määritelty, pitää olla kyseisen oppilaitoksen ja luokan valmis perusopetuksen suoritus
        Seq(Map(OHJATTAVA_METADATA_AVAIN -> Set(Ohjattavuus(oppilaitosOid, Some(vuosi), Some(luokka)).toString())))
  }

  def getTallennettavaMetadata(opiskeluoikeudet: Seq[Opiskeluoikeus]): Map[String, Set[String]] = {
    Map(OHJATTAVA_METADATA_AVAIN -> getOhjattavuudet(opiskeluoikeudet).map(s => s.toString()))
  }
*/

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

  def yhteisenAineenArvosanaPuuttuu(perusopetuksenOppimaara: PerusopetuksenOppimaara): Boolean =
    YHTEISET_AINEET.exists(yhteinenAine => !perusopetuksenOppimaara.aineet.exists(oppimaaranAine => oppimaaranAine.koodi.arvo==yhteinenAine))

  def getPerusopetuksenOhjausvastuuMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Ohjausvastuu] = {
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .flatMap(o => {
        val ysiluokat = o.suoritukset
          .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
          .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
          .filter(s => {
            s.koodi.arvo == "9"
          })
          .filter(s => !s.jaaLuokalle)
        if (ysiluokat.size > 1)
          throw new RuntimeException(s"Opiskeluoikeudessa ${o.oid} on useita ysiluokkia")

        val perusopetuksenOppimaarat = o.suoritukset
          .filter(s => s.isInstanceOf[PerusopetuksenOppimaara])
          .map(s => s.asInstanceOf[PerusopetuksenOppimaara])
        if (perusopetuksenOppimaarat.size > 1)
          throw new RuntimeException(s"Opiskeluoikeudessa ${o.oid} on useita perusopetuksen oppimääriä")

        // TODO: jotenkin pitää saada tieto hakemuksesta. Tätä kautta tulevat 7. ja 8. -luokkalaiset ja kotiopetuslaiset
        // TODO: annetaan toistaiseksi kaikille dummy-luokkatieto kunnes saadaan oikea koskesta
        (ysiluokat.headOption, perusopetuksenOppimaarat.headOption) match
          case (None, None) => None
          case (None, Some(oppimaara)) =>
            o.tila match
              case VALMIS => Some(Ohjausvastuu(oppimaara.vahvistusPaivamaara.get, None, o.oppilaitosOid, Some(oppimaara.vahvistusPaivamaara.get.getYear), None, Some(SuoritusTila.VALMIS), Some(yhteisenAineenArvosanaPuuttuu(oppimaara)), false))
              case default => None // eivät ysillä
          case (Some(ysiluokka), None) =>
            Some(Ohjausvastuu(ysiluokka.alkamisPaiva.get, None, o.oppilaitosOid, Some(ysiluokka.alkamisPaiva.get.getYear + 1), Some("9A"), Some(o.tila), Some(true), false))
          case (Some(ysiluokka), Some(oppimaara)) =>
            Some(Ohjausvastuu(ysiluokka.alkamisPaiva.get, None, o.oppilaitosOid, Some(ysiluokka.alkamisPaiva.get.getYear + 1), Some("9A"), Some(o.tila), Some(yhteisenAineenArvosanaPuuttuu(oppimaara)), false))
      })
  }

  def getTelmaOhjausvastuuMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Ohjausvastuu] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.Telma])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.Telma])
        .map(telma => {
          val aloitusPaiva = o.jaksot.map(j => j.alku).min
          val valmistumisVuosi = aloitusPaiva.getYear + 1
          Ohjausvastuu(aloitusPaiva, None, o.oppilaitos.oid, Some(aloitusPaiva.getYear + 1), None, Some(telma.supaTila), None, false)
        }))

  def getTuvaOhjausvastuuMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Ohjausvastuu] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[GeneerinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[GeneerinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.Tuva])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.Tuva])
        .map(tuva => {
          val aloitusPaiva = o.jaksot.map(j => j.alku).min
          val valmistumisVuosi = aloitusPaiva.getYear + 1
          Ohjausvastuu(aloitusPaiva, None, o.oppilaitosOid, Some(aloitusPaiva.getYear + 1), None, Some(tuva.supaTila), None, false)
        }))

  def getVSTOhjausvastuuMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Ohjausvastuu] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[GeneerinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[GeneerinenOpiskeluoikeus])
      .flatMap(o => o.suoritukset
        .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.VapaaSivistystyo])
        .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.VapaaSivistystyo])
        .map(vst => {
          val aloitusPaiva = o.jaksot.map(j => j.alku).min
          val valmistumisVuosi = aloitusPaiva.getYear + 1
          Ohjausvastuu(aloitusPaiva, None, o.oppilaitosOid, Some(aloitusPaiva.getYear + 1), None, Some(vst.supaTila), None, false)
        }))

  // Ammatillisen koulutuksen ohjausvastuut tallennetaan jotta ne katkaisevat aikaisemman perusopetukseen
  // tai lisäpistekoulutukseen perustuvan ohjausvastuun
  def getAmmatillinenOhjausvastuuMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Ohjausvastuu] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .filter(o => !o.suoritukset.exists(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.Telma]))
      .map(o => Ohjausvastuu(o.jaksot.map(j => j.alku).min, None, o.oppilaitos.oid, None, None, None, None, true))

  // Lukiokoulutuksen ohjausvastuut tallennetaan jotta ne katkaisevat aikaisemman perusopetukseen
  // tai lisäpistekoulutukseen perustuvan ohjausvastuun
  def getLukioOhjausvastuuMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Set[Ohjausvastuu] =
    // TODO: Tarvitaan Koskesta lukion opiskeluoikeus
    Set.empty
    
  def getOhjausvastuuMetadata(opiskeluoikeudet: Set[Opiskeluoikeus]): Seq[Ohjausvastuu] =
    // haetaan eri opinnoista seuraavat ohjausvastuut
    Seq(
      getPerusopetuksenOhjausvastuuMetadata(opiskeluoikeudet),
      getTelmaOhjausvastuuMetadata(opiskeluoikeudet),
      getTuvaOhjausvastuuMetadata(opiskeluoikeudet),
      getVSTOhjausvastuuMetadata(opiskeluoikeudet),
      getAmmatillinenOhjausvastuuMetadata(opiskeluoikeudet),
      getLukioOhjausvastuuMetadata(opiskeluoikeudet)
    ).flatten.sortBy(ov => ov.alku)
  
  def onkoOhjattava(oppilaitosOids: Set[String], vuosi: Option[Int], opiskeluoikeudet: Set[Opiskeluoikeus]): Boolean =
    val ohjausvastuut = getOhjausvastuuMetadata(opiskeluoikeudet)
  
    val rajatut = ohjausvastuut.zip(ohjausvastuut.tail.map(e => Some(e)) ++ None).map((curr, next) => curr.copy(loppu = {
      (curr.loppu, next.map(n => n.alku)) match
        case (None, None) => None
        case (Some(currLoppu), None) => Some(currLoppu)
        case (None, Some(nextAlku)) => Some(nextAlku)
        case (Some(currLoppu), Some(nextAlku)) => Some(if currLoppu.isBefore(nextAlku) then currLoppu else nextAlku)
    }))

    rajatut.exists(o => oppilaitosOids.contains(o.oppilaitosOid) && (vuosi.isEmpty || o.valmistumisvuosi.contains(vuosi.get)) && o.loppu.exists(!_.isBefore(LocalDate.now)))
}