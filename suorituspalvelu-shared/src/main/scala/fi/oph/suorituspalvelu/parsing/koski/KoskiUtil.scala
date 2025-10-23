package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.{Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusTila}
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil.{PK_OPPIMAARA_OPPILAITOS_VUOSI_AVAIN, PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA_AVAIN}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.slf4j.LoggerFactory

import java.time.{Instant, LocalDate}

case class PKOppimaaraOppilaitosVuosiMetadataArvo(oppilaitosOid: String, vuosi: Option[Int]) {

  def this(str: String) = this(str.split(":")(0), {
    str.split(":")(1) match
      case "KESKEN" => None
      case vuosi => Some(vuosi.toInt)
  })

  override def toString(): String = s"$oppilaitosOid:${if(vuosi.isDefined) vuosi.get else "KESKEN"}"
}

case class PKOppimaaraOppilaitosVuosiLuokkaMetadataArvo(oppilaitosOid: String, vuosi: Option[Int], luokka: String) {

  def this(str: String) = this(str.split(":")(0), {
    str.split(":")(1) match
      case "KESKEN" => None
      case vuosi => Some(vuosi.toInt)
  }, {
    val parts = str.split(":")
    val prefix = s"${parts(0)}:${parts(1)}:"
    str.substring(prefix.length)
  })

  override def toString(): String = s"$oppilaitosOid:${if(vuosi.isDefined) vuosi.get else "KESKEN"}:$luokka"
}

case class OponSeurattavaPerusopetuksenTila(oppilaitosOid: String, vahvistusVuosi: Option[Int], luokka: Option[String]) {

  def toMetadata(): Map[String, Set[String]] = {
    Seq(
      Some(PK_OPPIMAARA_OPPILAITOS_VUOSI_AVAIN -> Set(PKOppimaaraOppilaitosVuosiMetadataArvo(oppilaitosOid, vahvistusVuosi).toString())),
      luokka.map(l => PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA_AVAIN -> Set(PKOppimaaraOppilaitosVuosiLuokkaMetadataArvo(oppilaitosOid, vahvistusVuosi, l).toString()))
    ).flatten.toMap
  }
}

object KoskiUtil {

  val KOODISTO_OPPIAINEET = "koskioppiaineetyleissivistava"

  val PK_OPPIMAARA_OPPILAITOS_VUOSI_AVAIN         = "PK_OPPIMAARA_OPPILAITOS_VUOSI"
  val PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA_AVAIN  = "PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA"

  val PK_OPPILAITOS_KEYS = Set(
    PK_OPPIMAARA_OPPILAITOS_VUOSI_AVAIN,
    PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA_AVAIN
  )

  def extractLuokat(oppilaitosOid: String, metadata: Map[String, Set[String]]): Set[String] =
    metadata.get(PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA_AVAIN)
      .map(arvot => arvot
        .map(arvo => new PKOppimaaraOppilaitosVuosiLuokkaMetadataArvo(arvo))
        .filter(arvo => arvo.oppilaitosOid == oppilaitosOid)
        .map(arvo => arvo.luokka)).getOrElse(Set.empty)

  def hasOrganisaatioPKMetadata(organisaatioOid: String, metadata: Map[String, Set[String]]): Boolean =
    metadata
      .filter((key, values) => PK_OPPILAITOS_KEYS.contains(key))
      .exists((key, values) => values.exists(value => value.startsWith(organisaatioOid)))

  def getOponSeurattavatPerusopetuksenTilat(opiskeluoikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Set[OponSeurattavaPerusopetuksenTila] = {
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
              case VALMIS => Some(OponSeurattavaPerusopetuksenTila(oppimaara.oppilaitos.oid, Some(oppimaara.vahvistusPaivamaara.get.getYear), None))
              case KESKEN => None // eivät ysillä
              case KESKEYTYNYT => None
          case (Some(ysiluokka), None) =>
            o.tila match
              case KESKEYTYNYT => None
              case default => Some(OponSeurattavaPerusopetuksenTila(ysiluokka.oppilaitos.oid, None, Some("9A")))
          case (Some(ysiluokka), Some(oppimaara)) =>
            o.tila match
              case VALMIS => Some(OponSeurattavaPerusopetuksenTila(o.oppilaitosOid, Some(oppimaara.vahvistusPaivamaara.get.getYear), Some("9A")))
              case KESKEN => Some(OponSeurattavaPerusopetuksenTila(o.oppilaitosOid, None, Some("9A")))
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

  def getPeruskoulunOppimaaraHakuMetadata(oppilaitosOid: String, vuosi: Int, luokka: Option[String]): Seq[Map[String, Set[String]]] = {
    (vuosi, luokka) match
      case (vuosi, None) if LocalDate.now().getYear==vuosi =>
        // jos tämä vuosi mutta luokkaa ei määritelty, niin pitää olla joko kyseisen oppilaitoksen valmis perusopetuksen suoritus
        // tältä vuodelta, tai oppilaitoksen (ei valmis) ysiluokkalainen
        Seq(
          OponSeurattavaPerusopetuksenTila(oppilaitosOid, Some(vuosi), None).toMetadata(),
          OponSeurattavaPerusopetuksenTila(oppilaitosOid, None, None).toMetadata()
        )
      case (vuosi, Some(luokka)) if LocalDate.now().getYear==vuosi =>
        // jos tämä vuosi ja luokka määritelty, pitää olla joko kyseisen oppilaitoksen ja luokan valmis tai
        // kesken olevan ysi
        Seq(
          OponSeurattavaPerusopetuksenTila(oppilaitosOid, Some(vuosi), Some(luokka)).toMetadata(),
          OponSeurattavaPerusopetuksenTila(oppilaitosOid, None, Some(luokka)).toMetadata()
        )
      case (vuosi, None) =>
        // jos aikaisempi vuosi ja luokkaa ei määritelty, pitää olla kyseisen oppilaitoksen valmis perusopetuksen suoritus
        Seq(OponSeurattavaPerusopetuksenTila(oppilaitosOid, Some(vuosi), None).toMetadata())
      case (vuosi, Some(luokka)) =>
        // jos aikaisempi vuosi ja luokka määritelty, pitää olla kyseisen oppilaitoksen ja luokan valmis perusopetuksen suoritus
        Seq(OponSeurattavaPerusopetuksenTila(oppilaitosOid, Some(vuosi), Some(luokka)).toMetadata())
  }

  def getMetadata(opiskeluoikeudet: Seq[Opiskeluoikeus]): Map[String, Set[String]] = {
    // TODO: kunnon merge
    getOponSeurattavatPerusopetuksenTilat(opiskeluoikeudet).flatMap(_.toMetadata()).toMap
  }

}