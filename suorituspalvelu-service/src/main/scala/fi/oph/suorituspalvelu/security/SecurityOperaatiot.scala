package fi.oph.suorituspalvelu.security

import org.springframework.security.core.context.SecurityContextHolder

import scala.jdk.CollectionConverters.*

//organisaatioOids on tyhjä rekisterinpitäjille (suorituskykysyyt + tarpeeton),
// muille sisältää kaikki oikeudelliset organisaatiot sekä näiden aliorganisaatiot.
case class VirkailijaAuthorization(username: String, onRekisterinpitaja: Boolean, organisaatioOids: Set[String])

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
    val supaOikeudet = getOikeudet().filter(_.startsWith("ROLE_APP_SUORITUSREKISTERI_CRUD_"))
    getOrganisaatioOidsFromRoolit(supaOikeudet)
  }

  def getAuthorization(): VirkailijaAuthorization = {
    val onRekisterinpitaja = this.onRekisterinpitaja()
    val organisaatioOids = if (!onRekisterinpitaja) getOrganisaatiot() else Set.empty
    VirkailijaAuthorization(getUserOid(), onRekisterinpitaja, organisaatioOids)
  }

}
