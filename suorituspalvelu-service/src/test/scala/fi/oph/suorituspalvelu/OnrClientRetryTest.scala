package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.integration.NonRetriableException
import fi.oph.suorituspalvelu.integration.client.OnrClientImpl
import fi.vm.sade.javautils.nio.cas.CasClient
import org.asynchttpclient.{Request, Response}
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{BeforeEach, Test, TestInstance}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*

import java.util.concurrent.CompletableFuture
import scala.concurrent.Await
import scala.concurrent.duration.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class OnrClientRetryTest {

  private var mockCasClient: CasClient = _
  private var client: OnrClientImpl = _

  @BeforeEach
  def setup(): Unit = {
    mockCasClient = mock(classOf[CasClient])
    client = new OnrClientImpl(mockCasClient, "http://localhost", onrRetries = 2, onrRetryDelayMillis = 10)
  }

  private def successResponse(body: String): CompletableFuture[Response] = {
    val r = mock(classOf[Response])
    when(r.getStatusCode).thenReturn(200)
    when(r.getResponseBody()).thenReturn(body)
    CompletableFuture.completedFuture(r)
  }

  private def errorResponse(status: Int): CompletableFuture[Response] = {
    val r = mock(classOf[Response])
    when(r.getStatusCode).thenReturn(status)
    when(r.getStatusText).thenReturn("Error")
    when(r.getResponseBody()).thenReturn("")
    CompletableFuture.completedFuture(r)
  }

  private def connectionError: CompletableFuture[Response] =
    CompletableFuture.failedFuture(new RuntimeException("Connection refused"))

  // --- doPost (getMasterHenkilosForPersonOids) ---

  @Test
  def testPostRetriesOnConnectionFailureThenSucceeds(): Unit = {
    when(mockCasClient.execute(any(classOf[Request])))
      .thenReturn(connectionError)
      .thenReturn(successResponse("{}"))

    val result = Await.result(client.getMasterHenkilosForPersonOids(Set("1.2.3")), 10.seconds)
    assertEquals(Map.empty, result)
    verify(mockCasClient, times(2)).execute(any(classOf[Request]))
  }

  @Test
  def testPostRetries5xxThenSucceeds(): Unit = {
    val failFuture = errorResponse(503)
    val okFuture = successResponse("{}")
    when(mockCasClient.execute(any(classOf[Request])))
      .thenReturn(failFuture)
      .thenReturn(okFuture)

    val result = Await.result(client.getMasterHenkilosForPersonOids(Set("1.2.3")), 10.seconds)
    assertEquals(Map.empty, result)
    verify(mockCasClient, times(2)).execute(any(classOf[Request]))
  }

  @Test
  def testPostDoesNotRetry4xxClientError(): Unit = {
    val badRequestFuture = errorResponse(400)
    when(mockCasClient.execute(any(classOf[Request]))).thenReturn(badRequestFuture)

    assertThrows(classOf[NonRetriableException], () =>
      Await.result(client.getMasterHenkilosForPersonOids(Set("1.2.3")), 5.seconds)
    )
    // 4xx ei retriata — vain yksi kutsu
    verify(mockCasClient, times(1)).execute(any(classOf[Request]))
  }

  @Test
  def testPostExhaustsAllRetries(): Unit = {
    when(mockCasClient.execute(any(classOf[Request]))).thenReturn(connectionError)

    assertThrows(classOf[RuntimeException], () =>
      Await.result(client.getMasterHenkilosForPersonOids(Set("1.2.3")), 10.seconds)
    )
    // 1 initial attempt + 2 retries = 3 total
    verify(mockCasClient, times(3)).execute(any(classOf[Request]))
  }

  @Test
  def testPostSucceedsOnFirstAttemptWithNoRetries(): Unit = {
    val okFuture = successResponse("{}")
    when(mockCasClient.execute(any(classOf[Request]))).thenReturn(okFuture)

    Await.result(client.getMasterHenkilosForPersonOids(Set("1.2.3")), 5.seconds)
    verify(mockCasClient, times(1)).execute(any(classOf[Request]))
  }

  // --- doGet (getAsiointikieli) ---

  @Test
  def testGetRetriesOnConnectionFailureThenSucceeds(): Unit = {
    when(mockCasClient.execute(any(classOf[Request])))
      .thenReturn(connectionError)
      .thenReturn(successResponse("""{"kieliKoodi":"fi"}"""))

    val result = Await.result(client.getAsiointikieli("1.2.3"), 10.seconds)
    assertTrue(result.isDefined)
    verify(mockCasClient, times(2)).execute(any(classOf[Request]))
  }

  @Test
  def testGetExhaustsAllRetries(): Unit = {
    when(mockCasClient.execute(any(classOf[Request]))).thenReturn(connectionError)

    assertThrows(classOf[RuntimeException], () =>
      Await.result(client.getAsiointikieli("1.2.3"), 10.seconds)
    )
    verify(mockCasClient, times(3)).execute(any(classOf[Request]))
  }

  @Test
  def testGetReturnsNoneFor404WithoutRetrying(): Unit = {
    val notFoundFuture = errorResponse(404)
    when(mockCasClient.execute(any(classOf[Request]))).thenReturn(notFoundFuture)

    val result = Await.result(client.getAsiointikieli("1.2.3"), 5.seconds)
    assertFalse(result.isDefined)
    // 404 on validi vastaus, ei retriata
    verify(mockCasClient, times(1)).execute(any(classOf[Request]))
  }

  @Test
  def testGetDoesNotRetry4xxClientError(): Unit = {
    val forbiddenFuture = errorResponse(403)
    when(mockCasClient.execute(any(classOf[Request]))).thenReturn(forbiddenFuture)

    assertThrows(classOf[NonRetriableException], () =>
      Await.result(client.getAsiointikieli("1.2.3"), 5.seconds)
    )
    // 4xx ei retriata — vain yksi kutsu
    verify(mockCasClient, times(1)).execute(any(classOf[Request]))
  }
}
