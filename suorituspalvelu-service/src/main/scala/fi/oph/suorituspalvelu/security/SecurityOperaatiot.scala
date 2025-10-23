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
    kayttajanOikeudet.contains(SecurityConstants.SECURITY_ROOLI_OPH_PALVELUKAYTTAJA)

  def onOrganisaationKatselija(): Boolean =
    kayttajanOikeudet.contains(SecurityConstants.SECURITY_ROOLI_ORGANISAATION_KATSELIJA)

  //Todo, otetaanko huomioon hakukohderyhmäkohtaiset oikeudet? (eri solmuluokka)
  private def getOrganisaatioOidsFromRoolit(roolit: Set[String]): Set[String] = {
    roolit.filter(rooli => rooli.contains("1.2.246.562.10."))
      .map(rooliWithOrg => rooliWithOrg.split("_").last)
  }

  //Filtteröidään käyttäjäoikeuksista sellaiset organisaatiot, joihin käyttäjällä on oikeus
  def getOrganisaatiotOikeuksille(tarvittavatRoolit: Set[String]) = {
    //Todo, tarkat oikeudet kuntoon. Nyt varsinaisia Supa-oikeuksia ei edes ole. Pitää myös miettiä, mitä oikeuksia mihinkin operaatioon oikeasti halutaan tarkistella.
    val riittavatOikeudet = getKayttajanOikeudet().filter(oikeus => tarvittavatRoolit.exists(rooli => oikeus.startsWith(rooli)))
    getOrganisaatioOidsFromRoolit(riittavatOikeudet)
  }

  def getAuthorization(tarvittavatRoolit: Set[String], organisaatioProvider: OrganisaatioProvider): VirkailijaAuthorization = {
    val rekPit = onRekisterinpitaja()
    val organisaatiotOikeuksista = if (!rekPit) getOrganisaatiotOikeuksille(tarvittavatRoolit) else Set.empty
    val aliorganisaatiot = organisaatiotOikeuksista.flatMap(o => organisaatioProvider.haeOrganisaationTiedot(o).map(_.allDescendantOids).getOrElse(Set.empty))
    VirkailijaAuthorization(getUserOid(), rekPit, organisaatiotOikeuksista ++ aliorganisaatiot)
  }

}
