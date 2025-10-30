package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.integration.{OnrIntegrationImpl, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.integration.client.{Henkiloviite, OnrClientImpl}
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{BeforeEach, Test, TestInstance}
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}


@Test
@TestInstance(Lifecycle.PER_CLASS)
class OnrIntegrationTest {
  private var onrIntegration: OnrIntegrationImpl = _

  @MockBean
  private var mockOnrClient: OnrClientImpl = _

  @Autowired
  var onrIntegrationImpl: OnrIntegrationImpl = null

  @BeforeEach
  def setup(): Unit = {
    MockitoAnnotations.openMocks(this)
    mockOnrClient = mock(classOf[OnrClientImpl])
    onrIntegration = new OnrIntegrationImpl {
      override val onrClient: OnrClientImpl = mockOnrClient
    }
  }

  @Test
  def testGetAliasesForPersonOidsWithNoDuplicates(): Unit = {
    val personOid1 = "1.2.246.562.24.00000000987"
    val personOid2 = "1.2.246.562.24.00000000123"
    val personOids = Set(personOid1, personOid2)
    val emptyViitteet = Set.empty[Henkiloviite]

    when(mockOnrClient.getHenkiloviitteetForHenkilot(personOids))
      .thenReturn(Future.successful(emptyViitteet))

    val result = Await.result(onrIntegration.getAliasesForPersonOids(personOids), 5.seconds)

    val expected = PersonOidsWithAliases(Map(
      personOid1 -> Set(personOid1),
      personOid2 -> Set(personOid2)
    ))

    assertEquals(expected, result)
  }

  @Test
  def testGetAliasesForPersonOidsWithDuplicates(): Unit = {
    val personOid1 = "1.2.246.562.24.00000000987"
    val aliasForPersonOid1 = "1.2.246.562.24.00000000888"
    val personOids = Set(personOid1)
    val viitteet = Set(
      Henkiloviite(aliasForPersonOid1, personOid1),
    )

    when(mockOnrClient.getHenkiloviitteetForHenkilot(personOids))
      .thenReturn(Future.successful(viitteet))

    val result = Await.result(onrIntegration.getAliasesForPersonOids(personOids), 5.seconds)

    val expected = PersonOidsWithAliases(Map(
      personOid1 -> Set(personOid1, aliasForPersonOid1)
    ))

    assertEquals(expected, result)
  }

  @Test
  def testGetAliasesForPersonOidsWithMultipleDuplicates(): Unit = {
    val personOid1 = "1.2.246.562.24.00000000987"
    val personOid2 = "1.2.246.562.24.00000000123"
    val personOid3 = "1.2.246.562.24.00000000456"
    val aliasForPersonOid1 = "1.2.246.562.24.00000000888"
    val alias1ForPersonOid2 = "1.2.246.562.24.00000000444"
    val alias2ForPersonOid2 = "1.2.246.562.24.00000000333"

    val personOids = Set(personOid1, personOid2, personOid3)
    val viitteet = Set(
      Henkiloviite(aliasForPersonOid1, personOid1),
      Henkiloviite(alias1ForPersonOid2, personOid2),
      Henkiloviite(alias2ForPersonOid2, personOid2)
    )

    when(mockOnrClient.getHenkiloviitteetForHenkilot(personOids))
      .thenReturn(Future.successful(viitteet))

    val result = Await.result(onrIntegration.getAliasesForPersonOids(personOids), 2.seconds)

    val expected = PersonOidsWithAliases(Map(
      personOid2 -> Set(personOid2, alias1ForPersonOid2, alias2ForPersonOid2),
      personOid1 -> Set(personOid1, aliasForPersonOid1),
      personOid3 -> Set(personOid3)
    ))

    assertEquals(expected, result)
  }


  @Test
  def testGetAliasesForDuplikaattiSaranaCase(): Unit = {
    val queriedOid = "1.2.246.562.24.00000000987" //duplikaatti 1
    val masterOid = "1.2.246.562.24.00000000123"
    val saranaAlias = "1.2.246.562.24.00000000456" //duplikaatti 2

    //Testi tapaukselle, jossa kysytään onr:sta duplikaatin oidilla
    // ja löytyy kaksi linkitystä: duplikaatti 1 <-> masterOid ja lisäksi duplikaatti 2 <-> masterOid
    val viitteet = Set(
      Henkiloviite(queriedOid, masterOid),
      Henkiloviite(saranaAlias, masterOid)
    )

    val personOids = Set(queriedOid)
    when(mockOnrClient.getHenkiloviitteetForHenkilot(personOids))
      .thenReturn(Future.successful(viitteet))

    val result = Await.result(onrIntegration.getAliasesForPersonOids(personOids), 2.seconds)

    val expected = PersonOidsWithAliases(Map(
      queriedOid -> Set(queriedOid, masterOid, saranaAlias)
    ))

    assertEquals(expected, result)
  }

  @Test
  def testGetAliasesForPersonOidsWithFailure(): Unit = {
    val personOids = Set("1.2.3")
    val exception = new RuntimeException("Test error")

    when(mockOnrClient.getHenkiloviitteetForHenkilot(personOids))
      .thenReturn(Future.failed(exception))

    val thrown = assertThrows(classOf[RuntimeException], () =>
      Await.result(onrIntegration.getAliasesForPersonOids(personOids), 5.seconds)
    )

    assertEquals("Test error", thrown.getMessage)
  }
}
