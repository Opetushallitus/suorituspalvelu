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
    val result = OrganisaatioUtil.filterAndFlattenHierarkia(emptyHierarchy)

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
      organisaatiotyypit = Seq("organisaatiotyyppi_01"),
      oppilaitosKoodi = oppilaitosKoodi
    )

    val result = OrganisaatioUtil.filterAndFlattenHierarkia(Seq(singleOrg))
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
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val parentOrg = HierarkiaOrganisaatio(
      oid = parentOid,
      nimi = OrganisaatioNimi("Test parent organisaatio", "", ""),
      parentOid = None,
      children = Seq(childOrg),
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val result = OrganisaatioUtil.filterAndFlattenHierarkia(Seq(parentOrg))
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
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val grandchild2 = HierarkiaOrganisaatio(
      oid = grandchild2Oid,
      nimi = OrganisaatioNimi("Test grandchild organisaatio 2", "", ""),
      parentOid = Some(child1Oid),
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val child1 = HierarkiaOrganisaatio(
      oid = child1Oid,
      nimi = OrganisaatioNimi("Test child organisaatio 1", "", ""),
      parentOid = Some(rootOid),
      children = Seq(grandchild1, grandchild2),
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val child2 = HierarkiaOrganisaatio(
      oid = child2Oid,
      nimi = OrganisaatioNimi("Test child organisaatio 2", "", ""),
      parentOid = Some(rootOid),
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val root = HierarkiaOrganisaatio(
      oid = rootOid,
      nimi = OrganisaatioNimi("Test root organisaatio", "", ""),
      parentOid = None,
      children = Seq(child1, child2),
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val result = OrganisaatioUtil.filterAndFlattenHierarkia(Seq(root))
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
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val org2 = HierarkiaOrganisaatio(
      oid = org2Oid,
      nimi = OrganisaatioNimi("Test organisaatio 2", "", ""),
      parentOid = None,
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val result = OrganisaatioUtil.filterAndFlattenHierarkia(Seq(org1, org2))

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
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val grandchild1 = HierarkiaOrganisaatio(
      oid = grandchild1Oid,
      nimi = OrganisaatioNimi("Test grandchild organisaatio 1", "", ""),
      parentOid = Some(child1Oid),
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val grandchild2 = HierarkiaOrganisaatio(
      oid = grandchild2Oid,
      nimi = OrganisaatioNimi("Test grandchild organisaatio 2", "", ""),
      parentOid = Some(child1Oid),
      children = Seq(greatGrandchild),
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val child1 = HierarkiaOrganisaatio(
      oid = child1Oid,
      nimi = OrganisaatioNimi("Test child organisaatio 1", "", ""),
      parentOid = Some(rootOid),
      children = Seq(grandchild1, grandchild2),
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val child2 = HierarkiaOrganisaatio(
      oid = child2Oid,
      nimi = OrganisaatioNimi("Test child organisaatio 2", "", ""),
      parentOid = Some(rootOid),
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val root = HierarkiaOrganisaatio(
      oid = rootOid,
      nimi = OrganisaatioNimi("Test root organisaatio", "", ""),
      parentOid = None,
      children = Seq(child1, child2),
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val result = OrganisaatioUtil.filterAndFlattenHierarkia(Seq(root))

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

  @Test
  def testFilterOutVarhaiskasvatusOrganization(): Unit = {
    val orgOid = "1.2.246.562.10.57118763139"

    // Create an organization that only has varhaiskasvatus organization types
    val varhaiskasvatusOrg = HierarkiaOrganisaatio(
      oid = orgOid,
      nimi = OrganisaatioNimi("Varhaiskasvatus organisaatio", "", ""),
      parentOid = None,
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_07")
    )

    // The organization should be filtered out
    val result = OrganisaatioUtil.filterAndFlattenHierarkia(Seq(varhaiskasvatusOrg))
    assertTrue(result.isEmpty, "Organization with only varhaiskasvatus type should be filtered out")
  }

  @Test
  def testKeepOrganizationWithMixedTypes(): Unit = {
    val orgOid = "1.2.246.562.10.57118763140"

    // Create an organization that has both varhaiskasvatus and other types
    val mixedTypesOrg = HierarkiaOrganisaatio(
      oid = orgOid,
      nimi = OrganisaatioNimi("Mixed types organisaatio", "", ""),
      parentOid = None,
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_07", "organisaatiotyyppi_01")
    )

    // The organization should be kept because it has at least one non-varhaiskasvatus type
    val result = OrganisaatioUtil.filterAndFlattenHierarkia(Seq(mixedTypesOrg))
    assertEquals(1, result.size)
    assertTrue(result.contains(orgOid))
  }

  @Test
  def testHierarchyWithVarhaiskasvatusFiltering(): Unit = {
    val rootOid = "1.2.246.562.10.57118763141"
    val regularChildOid = "1.2.246.562.10.57118763142"
    val varhaiskasvatusChildOid = "1.2.246.562.10.57118763143"

    // Create a varhaiskasvatus child organization
    val varhaiskasvatusChild = HierarkiaOrganisaatio(
      oid = varhaiskasvatusChildOid,
      nimi = OrganisaatioNimi("Varhaiskasvatus child", "", ""),
      parentOid = Some(rootOid),
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_07")
    )

    // Create a regular child organization
    val regularChild = HierarkiaOrganisaatio(
      oid = regularChildOid,
      nimi = OrganisaatioNimi("Regular child", "", ""),
      parentOid = Some(rootOid),
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    // Create a root organization with both types of children
    val root = HierarkiaOrganisaatio(
      oid = rootOid,
      nimi = OrganisaatioNimi("Root org", "", ""),
      parentOid = None,
      children = Seq(regularChild, varhaiskasvatusChild),
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val result = OrganisaatioUtil.filterAndFlattenHierarkia(Seq(root))

    // Should include root and regular child, but not varhaiskasvatus child
    assertEquals(2, result.size)
    assertTrue(result.contains(rootOid))
    assertTrue(result.contains(regularChildOid))
    assertFalse(result.contains(varhaiskasvatusChildOid))

    // Check descendants - should only include the regular child
    val rootResult = result(rootOid)
    assertEquals(1, rootResult.allDescendantOids.size)
    assertTrue(rootResult.allDescendantOids.contains(regularChildOid))
    assertFalse(rootResult.allDescendantOids.contains(varhaiskasvatusChildOid))
  }

  @Test
  def testBothVarhaiskasvatusTypesTogether(): Unit = {
    val orgOid = "1.2.246.562.10.57118763144"

    // Create an organization with both varhaiskasvatus types but no other types
    val bothVarhaiskasvatusTypesOrg = HierarkiaOrganisaatio(
      oid = orgOid,
      nimi = OrganisaatioNimi("Both varhaiskasvatus types", "", ""),
      parentOid = None,
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_07", "organisaatiotyyppi_08")
    )

    // The organization should be filtered out as it only has varhaiskasvatus types
    val result = OrganisaatioUtil.filterAndFlattenHierarkia(Seq(bothVarhaiskasvatusTypesOrg))
    assertTrue(result.isEmpty)
  }

  @Test
  def testComplexHierarchyWithVarhaiskasvatusFiltering(): Unit = {
    val rootOid = "1.2.246.562.10.57118763145"
    val regularBranchOid = "1.2.246.562.10.57118763146"
    val varhaiskasvatusBranchOid = "1.2.246.562.10.57118763147"
    val varhaiskasvatusBranchRegularGrandchildOid = "1.2.246.562.10.57118763148"
    val mixedTypeGrandchildOid = "1.2.246.562.10.57118763149"

    // Create a varhaiskasvatus branch regular grandchild, this should get filtered out
    val varhaiskasvatusBranchRegularGrandchild = HierarkiaOrganisaatio(
      oid = varhaiskasvatusBranchRegularGrandchildOid,
      nimi = OrganisaatioNimi("Regular grandchild", "", ""),
      parentOid = Some(regularBranchOid),
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    // Create a varhaiskasvatus-only branch, this should get filtered out
    val varhaiskasvatusBranch = HierarkiaOrganisaatio(
      oid = varhaiskasvatusBranchOid,
      nimi = OrganisaatioNimi("Varhaiskasvatus branch", "", ""),
      parentOid = Some(rootOid),
      children = Seq(varhaiskasvatusBranchRegularGrandchild),
      organisaatiotyypit = Seq("organisaatiotyyppi_08")
    )

    // Create a mixed-type grandchild (has both varhaiskasvatus and regular types)
    val mixedTypeGrandchild = HierarkiaOrganisaatio(
      oid = mixedTypeGrandchildOid,
      nimi = OrganisaatioNimi("Mixed-type grandchild", "", ""),
      parentOid = Some(varhaiskasvatusBranchOid),
      children = Seq.empty,
      organisaatiotyypit = Seq("organisaatiotyyppi_07", "organisaatiotyyppi_01")
    )

    // Create a regular branch
    val regularBranch = HierarkiaOrganisaatio(
      oid = regularBranchOid,
      nimi = OrganisaatioNimi("Regular branch", "", ""),
      parentOid = Some(rootOid),
      children = Seq(mixedTypeGrandchild),
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    // Create a root with both branches
    val root = HierarkiaOrganisaatio(
      oid = rootOid,
      nimi = OrganisaatioNimi("Complex root", "", ""),
      parentOid = None,
      children = Seq(regularBranch, varhaiskasvatusBranch),
      organisaatiotyypit = Seq("organisaatiotyyppi_01")
    )

    val result = OrganisaatioUtil.filterAndFlattenHierarkia(Seq(root))

    // Should include root, regular branch, and mixed-type grandchild
    // But should NOT include the varhaiskasvatus-only branch or its children
    assertEquals(3, result.size)
    assertTrue(result.contains(rootOid))
    assertTrue(result.contains(regularBranchOid))
    assertTrue(result.contains(mixedTypeGrandchildOid))
    assertFalse(result.contains(varhaiskasvatusBranchOid))
    assertFalse(result.contains(varhaiskasvatusBranchRegularGrandchildOid))

    // Check descendants
    val rootDescendants = result(rootOid).allDescendantOids
    assertEquals(2, rootDescendants.size)
    assertTrue(rootDescendants.contains(regularBranchOid))
    assertTrue(rootDescendants.contains(mixedTypeGrandchildOid))
    assertFalse(rootDescendants.contains(varhaiskasvatusBranchRegularGrandchildOid))
    assertFalse(rootDescendants.contains(varhaiskasvatusBranchOid))
  }

}
