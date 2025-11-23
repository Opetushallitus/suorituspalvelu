package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.{Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusTila}
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil.OHJATTAVA_METADATA_AVAIN
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.slf4j.LoggerFactory

import java.time.{Instant, LocalDate}

val NOT_DEFINED_PLACEHOLDER = "_"

case class OhjattavaMetadataArvo(oppilaitosOid: String, vuosi: Option[Int], luokka: Option[String]) {

  def this(str: String) = this(str.split(":")(0), {
    str.split(":")(1) match
      case NOT_DEFINED_PLACEHOLDER => None
      case vuosi => Some(vuosi.toInt)
  }, {
    str.split(":")(2) match
      case NOT_DEFINED_PLACEHOLDER => None
      case luokka => Some(luokka)
  })

  override def toString(): String = s"$oppilaitosOid:${vuosi.getOrElse(NOT_DEFINED_PLACEHOLDER)}:${luokka.getOrElse(NOT_DEFINED_PLACEHOLDER)}"
}

case class OponSeurattavaSuoritus(oppilaitosOid: String, vahvistusVuosi: Option[Int], luokka: Option[String]) {

  def asMetadata(): Map[String, Set[String]] = {
    Map(OHJATTAVA_METADATA_AVAIN -> Set(
      OhjattavaMetadataArvo(oppilaitosOid, vahvistusVuosi, None).toString(),
      OhjattavaMetadataArvo(oppilaitosOid, vahvistusVuosi, luokka).toString()
    ))
  }
}

object KoskiUtil {

  val KOODISTO_OPPIAINEET = "koskioppiaineetyleissivistava"

  val OHJATTAVA_METADATA_AVAIN  = "OHJATTAVA_OPPILAITOS_VUOSI_LUOKKA"
  
  def extractLuokat(oppilaitosOid: String, metadata: Map[String, Set[String]]): Set[String] =
    metadata.get(OHJATTAVA_METADATA_AVAIN)
      .map(arvot => arvot
        .map(arvo => new OhjattavaMetadataArvo(arvo))
        .filter(arvo => arvo.oppilaitosOid == oppilaitosOid && arvo.luokka.isDefined)
        .map(arvo => arvo.luokka.get)).getOrElse(Set.empty)

  def hasOrganisaatioPKMetadata(organisaatioOid: String, metadata: Map[String, Set[String]]): Boolean =
    metadata
      .filter((key, values) => OHJATTAVA_METADATA_AVAIN.equals(key))
      .exists((key, values) => values.exists(value => value.startsWith(organisaatioOid)))

  def getOponSeurattavatPerusopetuksenTilat(opiskeluoikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Set[OponSeurattavaSuoritus] = {
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
              case VALMIS => Some(OponSeurattavaSuoritus(oppimaara.oppilaitos.oid, Some(oppimaara.vahvistusPaivamaara.get.getYear), None))
              case KESKEN => None // eivät ysillä
              case KESKEYTYNYT => None
          case (Some(ysiluokka), None) =>
            o.tila match
              case KESKEYTYNYT => None
              case default => Some(OponSeurattavaSuoritus(ysiluokka.oppilaitos.oid, None, Some("9A")))
          case (Some(ysiluokka), Some(oppimaara)) =>
            o.tila match
              case VALMIS => Some(OponSeurattavaSuoritus(o.oppilaitosOid, Some(oppimaara.vahvistusPaivamaara.get.getYear), Some("9A")))
              case KESKEN => Some(OponSeurattavaSuoritus(o.oppilaitosOid, None, Some("9A")))
              case KESKEYTYNYT => None
      }).toSet.flatten
  }

  /**
   * Henkilö on ysiluokalla jos:
   * - löytyy opiskeluoikeus joka ei ole eronnut-tilassa
   * - ja sen alta löytyy vuosiluokka joka on ysiluokka
   * - ja ei löydy vahvistettua perusopetuksen oppimäärän suoritusta
   *
   * @param opiskeluoikeudet
   * @return
   */
  def isOponSeurattava(opiskeluoikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Boolean =
    getOponSeurattavatPerusopetuksenTilat(opiskeluoikeudet).exists(t =>
      t.vahvistusVuosi match {
        case None => true
        // tämän vuoden valmistuneita seurataan elokuun loppuun
        case Some(vuosi) if vuosi == LocalDate.now().getYear && LocalDate.now().getMonthValue < 9 => true
        case default => false
      }
    )

  def includePerusopetuksenOppiaine(osaSuoritus: OsaSuoritus, koodistoProvider: KoodistoProvider): Boolean = {
    val oppiaineKoodi = osaSuoritus.koulutusmoduuli.get.tunniste.get.koodiarvo

    val hasArviointi = osaSuoritus.arviointi.isDefined
    val isKoulukohtainen = !koodistoProvider.haeKoodisto(KOODISTO_OPPIAINEET).contains(oppiaineKoodi)
    val aineTiedossa = !"XX".equals(oppiaineKoodi)
    val pakollinen = osaSuoritus.koulutusmoduuli.get.pakollinen.get
    val laajuusYli2vvk = osaSuoritus.koulutusmoduuli.get.laajuus.exists(l => l.arvo > 2)

    hasArviointi && !isKoulukohtainen && aineTiedossa && (pakollinen || laajuusYli2vvk)
  }

  def getPeruskoulunOppimaaraHakuMetadata(oppilaitosOid: String, vuosi: Int, luokka: Option[String], suoritusKesken: Boolean, arvosanaPuuttuu: Boolean): Seq[Map[String, Set[String]]] = {
    (vuosi, luokka) match
      case (vuosi, None) if LocalDate.now().getYear==vuosi =>
        // jos tämä vuosi mutta luokkaa ei määritelty, niin pitää olla joko kyseisen oppilaitoksen valmis perusopetuksen suoritus
        // tältä vuodelta, tai oppilaitoksen (ei valmis) ysiluokkalainen
        Seq(
          OponSeurattavaSuoritus(oppilaitosOid, Some(vuosi), None).asMetadata(),
          OponSeurattavaSuoritus(oppilaitosOid, None, None).asMetadata()
        )
      case (vuosi, Some(luokka)) if LocalDate.now().getYear==vuosi =>
        // jos tämä vuosi ja luokka määritelty, pitää olla joko kyseisen oppilaitoksen ja luokan valmis tai
        // kesken olevan ysi
        Seq(
          OponSeurattavaSuoritus(oppilaitosOid, Some(vuosi), Some(luokka)).asMetadata(),
          OponSeurattavaSuoritus(oppilaitosOid, None, Some(luokka)).asMetadata()
        )
      case (vuosi, None) =>
        // jos aikaisempi vuosi ja luokkaa ei määritelty, pitää olla kyseisen oppilaitoksen valmis perusopetuksen suoritus
        Seq(OponSeurattavaSuoritus(oppilaitosOid, Some(vuosi), None).asMetadata())
      case (vuosi, Some(luokka)) =>
        // jos aikaisempi vuosi ja luokka määritelty, pitää olla kyseisen oppilaitoksen ja luokan valmis perusopetuksen suoritus
        Seq(OponSeurattavaSuoritus(oppilaitosOid, Some(vuosi), Some(luokka)).asMetadata())
  }

  def getMetadata(opiskeluoikeudet: Seq[Opiskeluoikeus]): Map[String, Set[String]] = {
    // TODO: kunnon merge
    getOponSeurattavatPerusopetuksenTilat(opiskeluoikeudet).flatMap(_.asMetadata()).toMap
  }

}