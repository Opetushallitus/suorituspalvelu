package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.integration.client.{Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.{Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle

@Test
@TestInstance(Lifecycle.PER_CLASS)
class OrganisaatioProviderTest {

  private val aktiivisenOppilaitoksenOid = "1.2.246.562.10.10000000001"
  private val lakkautetunOppilaitoksenOid = "1.2.246.562.10.10000000002"
  private val aktiivinen = Organisaatio(aktiivisenOppilaitoksenOid, OrganisaatioNimi("Aktiivinen", "", ""), None, Seq.empty, Seq("organisaatiotyyppi_02"), status = Some("AKTIIVINEN"))
  private val lakkautettu = Organisaatio(lakkautetunOppilaitoksenOid, OrganisaatioNimi("Lakkautettu", "", ""), None, Seq.empty, Seq("organisaatiotyyppi_02"), status = Some("PASSIIVINEN"))

  private def providerWith(orgs: Map[String, Organisaatio]): OrganisaatioProvider =
    new OrganisaatioProvider {
      override def orgLookupTable(): Map[String, Organisaatio] = orgs
    }

  @Test
  def haeOrganisaationTiedotPalauttaaOletuksenaMyosLakkautetun(): Unit = {
    val provider = providerWith(Map(lakkautetunOppilaitoksenOid -> lakkautettu))
    assertEquals(Some(lakkautettu), provider.haeOrganisaationTiedot(lakkautetunOppilaitoksenOid))
  }

  @Test
  def haeOrganisaationTiedotSuodattaaLakkautetunKunPyydetaan(): Unit = {
    val provider = providerWith(Map(lakkautetunOppilaitoksenOid -> lakkautettu))
    assertEquals(None, provider.haeOrganisaationTiedot(lakkautetunOppilaitoksenOid, mukaanLukienLakkautetut = false))
  }

  @Test
  def haeKaikkiOrganisaatiotOletuksenaSuodattaaLakkautetut(): Unit = {
    val provider = providerWith(Map(
      aktiivisenOppilaitoksenOid -> aktiivinen,
      lakkautetunOppilaitoksenOid -> lakkautettu
    ))
    val result = provider.haeKaikkiOrganisaatiot()
    assertEquals(Set(aktiivisenOppilaitoksenOid), result.keySet)
  }

  @Test
  def haeKaikkiOrganisaatiotPalauttaaLakkautetutKunPyydetaan(): Unit = {
    val provider = providerWith(Map(
      aktiivisenOppilaitoksenOid -> aktiivinen,
      lakkautetunOppilaitoksenOid -> lakkautettu
    ))
    val result = provider.haeKaikkiOrganisaatiot(mukaanLukienLakkautetut = true)
    assertEquals(Set(aktiivisenOppilaitoksenOid, lakkautetunOppilaitoksenOid), result.keySet)
  }

  @Test
  def haeKaikkiOrganisaatiotSuodattaaPuuttuvanStatuksen(): Unit = {
    val ilmanStatusta = Organisaatio(aktiivisenOppilaitoksenOid, OrganisaatioNimi("Statukseton", "", ""), None, Seq.empty, Seq("organisaatiotyyppi_02"))
    val provider = providerWith(Map(aktiivisenOppilaitoksenOid -> ilmanStatusta))
    assertTrue(provider.haeKaikkiOrganisaatiot().isEmpty)
    assertEquals(Set(aktiivisenOppilaitoksenOid), provider.haeKaikkiOrganisaatiot(mukaanLukienLakkautetut = true).keySet)
  }

}
