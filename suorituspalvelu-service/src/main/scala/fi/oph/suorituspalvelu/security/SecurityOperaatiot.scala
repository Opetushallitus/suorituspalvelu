package fi.oph.suorituspalvelu.security

import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import org.springframework.security.core.context.SecurityContextHolder

import scala.jdk.CollectionConverters.*

//organisaatioOids on tyhjä rekisterinpitäjille (suorituskykysyyt + tarpeeton),
// muille sisältää kaikki oikeudelliset organisaatiot sekä näiden aliorganisaatiot.
case class VirkailijaAuthorization(username: String, onRekisterinpitaja: Boolean, oikeudellisetOrganisaatiot: Set[String])

class SecurityOperaatiot(
                          getKayttajanOikeudet: () => Set[String] = () => SecurityContextHolder.getContext.getAuthentication.getAuthorities.asScala.map(a => a.getAuthority).toSet,
                          getUserOid: () => String = () => SecurityContextHolder.getContext.getAuthentication.getName())  {

  private lazy val kayttajanOikeudet = getKayttajanOikeudet()

  val identiteetti = getUserOid()

  def getIdentiteetti(): String =
    identiteetti

  def onRekisterinpitaja(): Boolean =
    kayttajanOikeudet.contains(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL)

  def onPalveluKayttaja(): Boolean =
    kayttajanOikeudet.contains(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT)

  //Tarkastusnäkymä
  def onOrganisaationKatselija(): Boolean =
    kayttajanOikeudet.contains(SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA)

  def onHakeneidenKatselija(): Boolean =
    kayttajanOikeudet.contains(SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA)

  def onValintaKayttaja(): Boolean = {
    kayttajanOikeudet.exists(
      Seq(
        SecurityConstants.SECURITY_ROOLI_SUPA_VALINTAKAYTTAJA_CRUD,
        SecurityConstants.SECURITY_ROOLI_SUPA_VALINTAKAYTTAJA_READ_UPDATE
      ).contains(_)
    )
  }

  def onUIKayttaja(): Boolean =
    onRekisterinpitaja() || onOrganisaationKatselija() || onHakeneidenKatselija()

  private def getOidsFromRoolit(roolit: Set[String], oidPrefix: String): Set[String] =
    roolit.filter(_.contains(oidPrefix))
      .map(_.split("_").last)

  //Filtteröidään oikeuksista sellaiset OIDit, joihin löytyy suoraan haettu oikeus
  private def getOidsOikeuksille(tarvittavatRoolit: Set[String], oidPrefix: String): Set[String] = {
    val riittavatOikeudet = getKayttajanOikeudet().filter(oikeus => tarvittavatRoolit.exists(rooli => oikeus.startsWith(rooli)))
    getOidsFromRoolit(riittavatOikeudet, oidPrefix)
  }

  def getAuthorization(tarvittavatRoolit: Set[String], organisaatioProvider: OrganisaatioProvider, myosHakukohderyhmat: Boolean = false): VirkailijaAuthorization = {
    val rekPit = onRekisterinpitaja()
    val organisaatiotOikeuksista = if (!rekPit) getOidsOikeuksille(tarvittavatRoolit, "1.2.246.562.10.") else Set.empty
    //Käsitellään suorat oikeudet niin, että käyttäjällä on samat oikeudet myös näiden aliorganisaatioille
    val aliorganisaatiot = organisaatiotOikeuksista.flatMap(o => organisaatioProvider.haeOrganisaationTiedot(o).map(_.allDescendantOids).getOrElse(Set.empty))
    val hakukohderyhmatOikeuksista = if (!rekPit && myosHakukohderyhmat) getOidsOikeuksille(tarvittavatRoolit, "1.2.246.562.28.") else Set.empty
    VirkailijaAuthorization(getUserOid(), rekPit, organisaatiotOikeuksista ++ aliorganisaatiot ++ hakukohderyhmatOikeuksista)
  }

}
