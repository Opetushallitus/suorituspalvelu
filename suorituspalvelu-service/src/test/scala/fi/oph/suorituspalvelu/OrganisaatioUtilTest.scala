package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.integration.client.{HierarkiaOrganisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.util.organisaatio.OrganisaatioUtil
import org.junit.jupiter.api.{Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.Assertions.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class OrganisaatioUtilTest {

  @Test
  def testFlattenEmptyHierarchy(): Unit = {
    val emptyHierarchy = Seq.empty[HierarkiaOrganisaatio]
    val result = OrganisaatioUtil.flattenHierarkia(emptyHierarchy)

    assertTrue(result.isEmpty)
  }

  @Test
  def testFlattenSingleOrganisationWithNoChildren(): Unit = {

    val orgOid = "1.2.246.562.10.57118763123"
    val oppilaitosKoodi = Some("12345")

    val singleOrg = HierarkiaOrganisaatio(
      oid = orgOid,
      nimi = OrganisaatioNimi("Testiorganisaatio", "", ""),
      parentOid = None,
      children = Seq.empty,
      tyypit = Seq.empty,
      oppilaitosKoodi = oppilaitosKoodi
    )

    val result = OrganisaatioUtil.flattenHierarkia(Seq(singleOrg))
    assertEquals(2, result.size)

    val resultOrgWithOid = result(orgOid)
    assertEquals(singleOrg.oid, resultOrgWithOid.oid)
    assertEquals(singleOrg.nimi, resultOrgWithOid.nimi)
    assertEquals(None, resultOrgWithOid.parentOid)
    assertTrue(resultOrgWithOid.allDescendantOids.isEmpty)

    // Myös oppilaitosKoodilla pitäisi löytyä sama organisaatio
    val resultOrgWithKoodi = result(oppilaitosKoodi.get)
    assertEquals(resultOrgWithKoodi, resultOrgWithOid)
  }

  @Test
  def testFlattenSimpleHierarchyWithOneParentAndOneChild(): Unit = {

    val parentOid = "1.2.246.562.10.57118763124"
    val childOid = "1.2.246.562.10.57118763125"

    val childOrg = HierarkiaOrganisaatio(
      oid = childOid,
      nimi = OrganisaatioNimi("Test child organisaatio", "", ""),
      parentOid = Some(parentOid),
      children = Seq.empty,
      tyypit = Seq.empty
    )

    val parentOrg = HierarkiaOrganisaatio(
      oid = parentOid,
      nimi = OrganisaatioNimi("Test parent organisaatio", "", ""),
      parentOid = None,
      children = Seq(childOrg),
      tyypit = Seq.empty
    )

    val result = OrganisaatioUtil.flattenHierarkia(Seq(parentOrg))
    assertEquals(2, result.size)

    // Check parent
    assertTrue(result.contains(parentOid))
    val parent = result(parentOid)
    assertEquals(parentOid, parent.oid)
    assertEquals(OrganisaatioNimi("Test parent organisaatio", "", ""), parent.nimi)
    assertEquals(None, parent.parentOid)
    assertEquals(1, parent.allDescendantOids.size)
    assertTrue(parent.allDescendantOids.contains(childOid))

    // Check child
    assertTrue(result.contains(childOid))
    val child = result(childOid)
    assertEquals(childOid, child.oid)
    assertEquals(OrganisaatioNimi("Test child organisaatio", "", ""), child.nimi)
    assertEquals(Some(parentOid), child.parentOid)
    assertTrue(child.allDescendantOids.isEmpty)
  }

  @Test
  def testFlattenComplexHierarchyWithMultipleLevels(): Unit = {

    val rootOid = "1.2.246.562.10.57118763126"
    val child1Oid = "1.2.246.562.10.57118763127"
    val child2Oid = "1.2.246.562.10.57118763128"
    val grandchild1Oid = "1.2.246.562.10.57118763129"
    val grandchild2Oid = "1.2.246.562.10.57118763130"

    val grandchild1 = HierarkiaOrganisaatio(
      oid = grandchild1Oid,
      nimi = OrganisaatioNimi("Test grandchild organisaatio 1", "", ""),
      parentOid = Some(child1Oid),
      children = Seq.empty,
      tyypit = Seq.empty
    )

    val grandchild2 = HierarkiaOrganisaatio(
      oid = grandchild2Oid,
      nimi = OrganisaatioNimi("Test grandchild organisaatio 2", "", ""),
      parentOid = Some(child1Oid),
      children = Seq.empty,
      tyypit = Seq.empty
    )

    val child1 = HierarkiaOrganisaatio(
      oid = child1Oid,
      nimi = OrganisaatioNimi("Test child organisaatio 1", "", ""),
      parentOid = Some(rootOid),
      children = Seq(grandchild1, grandchild2),
      tyypit = Seq.empty
    )

    val child2 = HierarkiaOrganisaatio(
      oid = child2Oid,
      nimi = OrganisaatioNimi("Test child organisaatio 2", "", ""),
      parentOid = Some(rootOid),
      children = Seq.empty,
      tyypit = Seq.empty
    )

    val root = HierarkiaOrganisaatio(
      oid = rootOid,
      nimi = OrganisaatioNimi("Test root organisaatio", "", ""),
      parentOid = None,
      children = Seq(child1, child2),
      tyypit = Seq.empty
    )

    val result = OrganisaatioUtil.flattenHierarkia(Seq(root))
    assertEquals(5, result.size)

    // Check root
    val rootResult = result(rootOid)
    assertEquals(rootOid, rootResult.oid)
    assertEquals(4, rootResult.allDescendantOids.size)
    assertTrue(rootResult.allDescendantOids.contains(child1Oid))
    assertTrue(rootResult.allDescendantOids.contains(child2Oid))
    assertTrue(rootResult.allDescendantOids.contains(grandchild1Oid))
    assertTrue(rootResult.allDescendantOids.contains(grandchild2Oid))

    // Check child1
    val child1Result = result(child1Oid)
    assertEquals(child1Oid, child1Result.oid)
    assertEquals(2, child1Result.allDescendantOids.size)
    assertTrue(child1Result.allDescendantOids.contains(grandchild1Oid))
    assertTrue(child1Result.allDescendantOids.contains(grandchild2Oid))

    // Check child2
    val child2Result = result(child2Oid)
    assertEquals(child2Oid, child2Result.oid)
    assertTrue(child2Result.allDescendantOids.isEmpty)

    // Check grandchildren
    assertTrue(result.contains(grandchild1Oid))
    assertTrue(result.contains(grandchild2Oid))
    assertTrue(result(grandchild1Oid).allDescendantOids.isEmpty)
    assertTrue(result(grandchild2Oid).allDescendantOids.isEmpty)
  }

  @Test
  def testHandleMultipleRootOrganizations(): Unit = {

    val org1Oid = "1.2.246.562.10.57118763131"
    val org2Oid = "1.2.246.562.10.57118763132"

    val org1 = HierarkiaOrganisaatio(
      oid = org1Oid,
      nimi = OrganisaatioNimi("Test organisaatio 1", "", ""),
      parentOid = None,
      children = Seq.empty,
      tyypit = Seq.empty
    )

    val org2 = HierarkiaOrganisaatio(
      oid = org2Oid,
      nimi = OrganisaatioNimi("Test organisaatio 2", "", ""),
      parentOid = None,
      children = Seq.empty,
      tyypit = Seq.empty
    )

    val result = OrganisaatioUtil.flattenHierarkia(Seq(org1, org2))

    assertEquals(2, result.size)
    assertTrue(result.contains(org1Oid))
    assertTrue(result.contains(org2Oid))
    assertTrue(result(org1Oid).allDescendantOids.isEmpty)
    assertTrue(result(org2Oid).allDescendantOids.isEmpty)
  }

  @Test
  def testIdentifyDescendantOidsAcrossAllLevels(): Unit = {

    val rootOid = "1.2.246.562.10.57118763133"
    val child1Oid = "1.2.246.562.10.57118763134"
    val child2Oid = "1.2.246.562.10.57118763135"
    val grandchild1Oid = "1.2.246.562.10.57118763136"
    val grandchild2Oid = "1.2.246.562.10.57118763137"
    val greatGrandchildOid = "1.2.246.562.10.57118763138"

    val greatGrandchild = HierarkiaOrganisaatio(
      oid = greatGrandchildOid,
      nimi = OrganisaatioNimi("Test great grandchild organisaatio 1", "", ""),
      parentOid = Some(grandchild2Oid),
      children = Seq.empty,
      tyypit = Seq.empty
    )

    val grandchild1 = HierarkiaOrganisaatio(
      oid = grandchild1Oid,
      nimi = OrganisaatioNimi("Test grandchild organisaatio 1", "", ""),
      parentOid = Some(child1Oid),
      children = Seq.empty,
      tyypit = Seq.empty
    )

    val grandchild2 = HierarkiaOrganisaatio(
      oid = grandchild2Oid,
      nimi = OrganisaatioNimi("Test grandchild organisaatio 2", "", ""),
      parentOid = Some(child1Oid),
      children = Seq(greatGrandchild),
      tyypit = Seq.empty
    )

    val child1 = HierarkiaOrganisaatio(
      oid = child1Oid,
      nimi = OrganisaatioNimi("Test child organisaatio 1", "", ""),
      parentOid = Some(rootOid),
      children = Seq(grandchild1, grandchild2),
      tyypit = Seq.empty
    )

    val child2 = HierarkiaOrganisaatio(
      oid = child2Oid,
      nimi = OrganisaatioNimi("Test child organisaatio 2", "", ""),
      parentOid = Some(rootOid),
      children = Seq.empty,
      tyypit = Seq.empty
    )

    val root = HierarkiaOrganisaatio(
      oid = rootOid,
      nimi = OrganisaatioNimi("Test root organisaatio", "", ""),
      parentOid = None,
      children = Seq(child1, child2),
      tyypit = Seq.empty
    )

    val result = OrganisaatioUtil.flattenHierarkia(Seq(root))

    // Check Root
    val rootDescendants = result(rootOid).allDescendantOids
    assertEquals(5, rootDescendants.size)
    val expectedRootDescendants = Set(child1Oid, child2Oid, grandchild1Oid, grandchild2Oid, greatGrandchildOid)
    expectedRootDescendants.foreach(oid =>
      assertTrue(rootDescendants.contains(oid), s"Root should contain descendant $oid")
    )

    // Check Child1
    val child1Descendants = result(child1Oid).allDescendantOids
    assertEquals(3, child1Descendants.size)
    val expectedChild1Descendants = Set(grandchild1Oid, grandchild2Oid, greatGrandchildOid)
    expectedChild1Descendants.foreach(oid =>
      assertTrue(child1Descendants.contains(oid), s"Child1 should contain descendant $oid")
    )

    // Check Grandchild2
    val grandchild2Descendants = result(grandchild2Oid).allDescendantOids
    assertEquals(1, grandchild2Descendants.size)
    assertTrue(grandchild2Descendants.contains(greatGrandchildOid))
  }
}
