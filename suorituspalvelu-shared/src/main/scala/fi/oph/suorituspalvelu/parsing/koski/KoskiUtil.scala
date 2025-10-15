package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.{Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka}
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.util.KoodistoProvider

case class PerusopetuksenSuoritusOrganisaatio(oppilaitosOid: String, vuosi: Option[Integer], luokka: Option[String])

case class PeruskoulunSuoritusMetadataArvo(oppilaitosOid: String, vuosi: Integer) {

  def this(str: String) = this(str.split(":")(0), str.split(":")(1).toInt)

  override def toString(): String = s"$oppilaitosOid:$vuosi"
}

case class PeruskoulunLuokkaMetadataArvo(oppilaitosOid: String, luokka: String) {

  def this(str: String) = this(str.split(":")(0), str.split(":")(1))

  override def toString(): String = s"$oppilaitosOid:$luokka"
}

object KoskiUtil {

  val KOODISTO_OPPIAINEET = "koskioppiaineetyleissivistava"

  val PERUSKOULU_OPPIMAARA_VUOSI_AVAIN = "PK_OPPIMAARA_OPPILAITOS_VUOSI"
  val PERUSKOULU_OPPIMAARA_LUOKKA_AVAIN = "PK_OPPILAITOS_LUOKKA"
  val PERUSKOULU_YSI_AVAIN = "PK_YSI_OPPILAITOS"

  def perusopetuksenSuoritusOrganisaatiot(opiskeluoikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Set[PerusopetuksenSuoritusOrganisaatio] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .filter(o => o.tila == VALMIS).flatMap(o => o.suoritukset)
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenOppimaara])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenOppimaara])
      .map(s => PerusopetuksenSuoritusOrganisaatio(s.oppilaitos.oid, Some(s.vahvistusPaivamaara.get.getYear), None))
      .toSet

  def ysiluokatOrganisaatiot(opiskeluoikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Set[PerusopetuksenSuoritusOrganisaatio] =
    opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .filter(o => o.tila != KESKEYTYNYT)
      .flatMap(o => o.suoritukset)
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
      .filter(s => {
        s.koodi.arvo == "9"
      })
      // Annetaan toistaiseksi kaikille dummy-luokkatieto kunnes saadaan oikea koskesta
      .map(s => PerusopetuksenSuoritusOrganisaatio(s.oppilaitos.oid, s.vahvistusPaivamaara.map(_.getYear), Some("9A")))
      .toSet

  /**
   * Henkilö on ysiluokalla jos:
   * - löytyy opiskeluoikeus joka ei ole eronnut-tilassa
   * - ja sen alta löytyy vuosiluokka joka on ysiluokka
   * - ja ei löydy vahvistettua perusopetuksen oppimäärän suoritusta
   *
   * @param opiskeluoikeudet
   * @return
   */
  def isYsiluokkalainen(opiskeluoikeudet: Seq[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Boolean =
    val valmisPerusopetus = perusopetuksenSuoritusOrganisaatiot(opiskeluoikeudet).nonEmpty
    val eiEronnutYsiluokkalainen = ysiluokatOrganisaatiot(opiskeluoikeudet).nonEmpty

    !valmisPerusopetus && eiEronnutYsiluokkalainen

  def includePerusopetuksenOppiaine(osaSuoritus: OsaSuoritus, koodistoProvider: KoodistoProvider): Boolean = {
    val oppiaineKoodi = osaSuoritus.koulutusmoduuli.get.tunniste.get.koodiarvo

    val hasArviointi = osaSuoritus.arviointi.isDefined
    val isKoulukohtainen = !koodistoProvider.haeKoodisto(KOODISTO_OPPIAINEET).contains(oppiaineKoodi)
    val aineTiedossa = !"XX".equals(oppiaineKoodi)
    val pakollinen = osaSuoritus.koulutusmoduuli.get.pakollinen.get
    val laajuusYli2vvk = osaSuoritus.koulutusmoduuli.get.laajuus.exists(l => l.arvo > 2)

    hasArviointi && !isKoulukohtainen && aineTiedossa && (pakollinen || laajuusYli2vvk)
  }

  def getPeruskouluHakuMetadata(oppilaitosOid: String, vuosi: Int, luokka: Option[String]): Map[String, Set[String]] =
    Seq(
      Some(PERUSKOULU_OPPIMAARA_VUOSI_AVAIN -> Set(PeruskoulunSuoritusMetadataArvo(oppilaitosOid, vuosi).toString)),
      Some(PERUSKOULU_YSI_AVAIN -> Set(oppilaitosOid)),
      luokka.map(l => PERUSKOULU_OPPIMAARA_LUOKKA_AVAIN -> Set(PeruskoulunLuokkaMetadataArvo(oppilaitosOid, l).toString))
    ).flatten.toMap

  def getPeruskoulunSuoritusMetadata(opiskeluoikeudet: Seq[Opiskeluoikeus]): Map[String, Set[String]] = {
    Set(
      PERUSKOULU_OPPIMAARA_VUOSI_AVAIN -> perusopetuksenSuoritusOrganisaatiot(opiskeluoikeudet).map(o => PeruskoulunSuoritusMetadataArvo(o.oppilaitosOid, o.vuosi.get).toString),
      PERUSKOULU_YSI_AVAIN -> ysiluokatOrganisaatiot(opiskeluoikeudet).filter(_.vuosi.isEmpty).map(_.oppilaitosOid),
      PERUSKOULU_OPPIMAARA_LUOKKA_AVAIN -> ysiluokatOrganisaatiot(opiskeluoikeudet).map(o => PeruskoulunLuokkaMetadataArvo(o.oppilaitosOid, o.luokka.get).toString),
    ).toMap
  }

}