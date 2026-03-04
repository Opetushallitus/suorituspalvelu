package fi.oph.suorituspalvelu.util.organisaatio

import fi.oph.suorituspalvelu.integration.client.{Organisaatio, HierarkiaOrganisaatio}

object OrganisaatioUtil {

  val varhaiskasvatuksenOrganisaatiotyypit = Set("organisaatiotyyppi_07", "organisaatiotyyppi_08")

  def filterAndFlattenHierarkia(hierarkia: Seq[HierarkiaOrganisaatio]): Map[String, Organisaatio] = {
    //Säästetään vain sellaiset organisaatiot, joilla on vähintään yksi muu kuin varhaiskasvatuksen organisaatiotyyppi
    def filterOutVarhaiskasvatus(hierarkia: Seq[HierarkiaOrganisaatio]): Seq[HierarkiaOrganisaatio] = {
      hierarkia.collect {
        case org if org.organisaatiotyypit.toSet.exists(!varhaiskasvatuksenOrganisaatiotyypit.contains(_)) =>
          // Keep this org but also filter its children recursively
          org.copy(children = filterOutVarhaiskasvatus(org.children))
      }
    }

    def collectDescendantOids(org: HierarkiaOrganisaatio): Seq[String] = {
      val childrenOids = org.children.flatMap(child => child.oid +: collectDescendantOids(child))
      childrenOids
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
          oppilaitosTyyppi = org.oppilaitostyyppi
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
