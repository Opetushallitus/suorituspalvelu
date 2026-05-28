package fi.oph.suorituspalvelu.util.organisaatio

import fi.oph.suorituspalvelu.integration.client.{HierarkiaOrganisaatio, Organisaatio}

object OrganisaatioUtil {

  val varhaiskasvatuksenOrganisaatiotyypit = Set("organisaatiotyyppi_07", "organisaatiotyyppi_08")
  val AKTIIVINEN = "AKTIIVINEN"

  private def isAktiivinen(org: HierarkiaOrganisaatio): Boolean =
    org.status.contains(AKTIIVINEN)

  def filterAndFlattenHierarkia(hierarkia: Seq[HierarkiaOrganisaatio]): Map[String, Organisaatio] = {
    //Säästetään vain sellaiset organisaatiot, joilla on vähintään yksi muu kuin varhaiskasvatuksen organisaatiotyyppi
    def filterOutVarhaiskasvatus(hierarkia: Seq[HierarkiaOrganisaatio]): Seq[HierarkiaOrganisaatio] = {
      hierarkia.collect {
        case org if org.organisaatiotyypit.toSet.exists(!varhaiskasvatuksenOrganisaatiotyypit.contains(_)) =>
          // Keep this org but also filter its children recursively
          org.copy(children = filterOutVarhaiskasvatus(org.children))
      }
    }

    // Lakkautetut jätetään pois jälkeläisistä, jotta esim. käyttöoikeuksien laajennus ei ulotu lakkautettuihin
    // aliorganisaatioihin. Itse lakkautetut organisaatiot säilyvät lookup-taulussa; suodatus näkyvyydestä
    // tapahtuu OrganisaatioProviderissa.
    def collectDescendantOids(org: HierarkiaOrganisaatio): Seq[String] = {
      org.children.flatMap {
        case child if isAktiivinen(child) => child.oid +: collectDescendantOids(child)
        case _ => Seq.empty
      }
    }

    def flatten(orgs: Seq[HierarkiaOrganisaatio]): Map[String, Organisaatio] = {
      orgs.foldLeft(Map.empty[String, Organisaatio]) { (acc, org) =>
        val descendantOids = collectDescendantOids(org)
        val o = Organisaatio(
          oid = org.oid,
          nimi = org.nimi,
          parentOid = org.parentOid,
          allDescendantOids = descendantOids,
          tyypit = org.organisaatiotyypit,
          oppilaitosTyyppi = org.oppilaitostyyppi,
          status = org.status
        )
        val childrenMap = flatten(org.children)

        val withOidItem = acc + (org.oid -> o)

        org.oppilaitosKoodi match {
          case Some(oppilaitosKoodi) => withOidItem + (oppilaitosKoodi -> o) ++ childrenMap
          case _ => withOidItem ++ childrenMap
        }
      }
    }
    val withoutVarhaiskasvatus = filterOutVarhaiskasvatus(hierarkia)
    flatten(withoutVarhaiskasvatus)
  }
}
