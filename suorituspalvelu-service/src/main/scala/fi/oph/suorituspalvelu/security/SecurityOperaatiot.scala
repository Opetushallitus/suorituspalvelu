package fi.oph.suorituspalvelu.security

import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import org.springframework.security.core.context.SecurityContextHolder

import scala.jdk.CollectionConverters.*

//organisaatioOids on tyhjä rekisterinpitäjille (suorituskykysyyt + tarpeeton),
// muille sisältää kaikki oikeudelliset organisaatiot sekä näiden aliorganisaatiot.
case class VirkailijaAuthorization(username: String, onRekisterinpitaja: Boolean, oikeudellisetOrganisaatiot: Set[String], aliOrganisaatiot: Set[String]) {
  def getOrgsForAuth(): Set[String] = {
    oikeudellisetOrganisaatiot ++ aliOrganisaatiot
  }
}

class SecurityOperaatiot(
                          getOikeudet: () => Set[String] = () => SecurityContextHolder.getContext.getAuthentication.getAuthorities.asScala.map(a => a.getAuthority).toSet,
                          getUserOid: () => String = () => SecurityContextHolder.getContext.getAuthentication.getName())  {

  private lazy val kayttajanOikeudet = getOikeudet()

  val identiteetti = getUserOid()

  def getIdentiteetti(): String =
    identiteetti

  def onRekisterinpitaja(): Boolean =
    kayttajanOikeudet.contains(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL)

  //Todo, otetaanko huomioon hakukohderyhmäkohtaiset oikeudet? (eri solmuluokka)
  private def getOrganisaatioOidsFromRoolit(roolit: Set[String]): Set[String] = {
    roolit.filter(rooli => rooli.contains("1.2.246.562.10."))
      .map(rooliWithOrg => rooliWithOrg.split("_").last)
  }

  //Filtteröidään käyttäjäoikeuksista sellaiset organisaatiot, joihin käyttäjällä on oikeus
  def getOrganisaatiot() = {
    //Todo, tarkat oikeudet kuntoon. Nyt varsinaisia Supa-oikeuksia ei edes ole. Pitää myös miettiä, mitä oikeuksia mihinkin operaatioon oikeasti halutaan tarkistella.
    val supaOikeudet = getOikeudet().filter(oikeus => oikeus.startsWith("ROLE_APP_SUORITUSREKISTERI_CRUD_") || oikeus.startsWith("ROLE_APP_SUORITUSREKISTERI_READ_"))
    getOrganisaatioOidsFromRoolit(supaOikeudet)
  }

  def getAuthorization(organisaatioProvider: OrganisaatioProvider): VirkailijaAuthorization = {
    val rekPit = onRekisterinpitaja()
    val organisaatiotOikeuksista = if (!rekPit) getOrganisaatiot() else Set.empty
    val aliorganisaatiot = organisaatiotOikeuksista.flatMap(o => organisaatioProvider.haeOrganisaationTiedot(o).map(_.allDescendantOids).getOrElse(Set.empty))
    VirkailijaAuthorization(getUserOid(), rekPit, organisaatiotOikeuksista, aliorganisaatiot)
  }

}
