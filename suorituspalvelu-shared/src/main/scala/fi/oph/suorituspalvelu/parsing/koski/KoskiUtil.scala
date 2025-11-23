package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.{Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusTila}
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil.OHJATTAVA_METADATA_AVAIN
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

  def isOhjattava(opiskeluoikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Boolean =
    getOhjattavuudet(opiskeluoikeudet).exists(t =>
      t.vahvistusVuosi match {
        case None => true                                                                                 // ohjattavuuden perusteena oleva suoritus kesken => täytyy ohjata
        case Some(vuosi) if vuosi == LocalDate.now().getYear && LocalDate.now().getMonthValue < 9 => true // tämän vuoden valmistuneita seurataan elokuun loppuun
        case default => false                                                                             // suoritus valmistunut aikaisempina vuosina, ei tarvitse ohjausta
      }
    )

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

  def includePerusopetuksenOppiaine(osaSuoritus: OsaSuoritus, koodistoProvider: KoodistoProvider): Boolean = {
    val oppiaineKoodi = osaSuoritus.koulutusmoduuli.get.tunniste.get.koodiarvo

    val hasArviointi = osaSuoritus.arviointi.isDefined
    val isKoulukohtainen = !koodistoProvider.haeKoodisto(KOODISTO_OPPIAINEET).contains(oppiaineKoodi)
    val aineTiedossa = !"XX".equals(oppiaineKoodi)
    val pakollinen = osaSuoritus.koulutusmoduuli.get.pakollinen.get
    val laajuusYli2vvk = osaSuoritus.koulutusmoduuli.get.laajuus.exists(l => l.arvo > 2)

    hasArviointi && !isKoulukohtainen && aineTiedossa && (pakollinen || laajuusYli2vvk)
  }
}