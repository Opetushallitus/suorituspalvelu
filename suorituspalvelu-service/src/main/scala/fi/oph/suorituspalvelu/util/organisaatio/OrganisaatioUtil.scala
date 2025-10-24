package fi.oph.suorituspalvelu.util.organisaatio

import fi.oph.suorituspalvelu.integration.client.{Organisaatio, HierarkiaOrganisaatio}

object OrganisaatioUtil {

  def flattenHierarkia(hierarkia: Seq[HierarkiaOrganisaatio]): Map[String, Organisaatio] = {
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
          tyypit = org.tyypit
        )
        val childrenMap = flatten(org.children)
        acc + (org.oid -> o) ++ childrenMap
      }
    }
    flatten(hierarkia)
  }
}
