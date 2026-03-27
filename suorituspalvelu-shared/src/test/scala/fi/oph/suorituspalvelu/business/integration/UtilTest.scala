package fi.oph.suorituspalvelu.business.integration

import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.Util
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoContainer, AvainArvoConverter}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiParser, KoskiToSuoritusConverter}
import fi.oph.suorituspalvelu.parsing.ytr.{YtrParser, YtrToSuoritusConverter}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

import java.time.LocalDate
import java.util
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class UtilTest {

  // --- retryWithBackoff tests ---

  @Test def testRetryWithBackoff_succedsOnFirstAttempt(): Unit = {
    val result = Await.result(
      Util.retryWithBackoff(Future.successful(42), retries = 3, retryDelayMillis = 10),
      5.seconds
    )
    Assertions.assertEquals(42, result)
  }

  @Test def testRetryWithBackoff_retriesAndSucceeds(): Unit = {
    val attempts = new AtomicInteger(0)
    def operation = Future {
      val attempt = attempts.incrementAndGet()
      if (attempt < 3) throw new RuntimeException(s"Fail attempt $attempt")
      "success"
    }

    val result = Await.result(
      Util.retryWithBackoff(operation, retries = 5, retryDelayMillis = 10),
      5.seconds
    )
    Assertions.assertEquals("success", result)
    Assertions.assertEquals(3, attempts.get())
  }

  @Test def testRetryWithBackoff_failsAfterAllRetriesExhausted(): Unit = {
    val attempts = new AtomicInteger(0)
    def operation = Future {
      attempts.incrementAndGet()
      throw new RuntimeException("always fails")
    }

    val exception = Assertions.assertThrows(classOf[RuntimeException], () => {
      Await.result(
        Util.retryWithBackoff(operation, retries = 2, retryDelayMillis = 10),
        5.seconds
      )
    })
    Assertions.assertEquals("always fails", exception.getMessage)
    // 1 initial attempt + 2 retries = 3 total
    Assertions.assertEquals(3, attempts.get())
  }

  @Test def testRetryWithBackoff_zeroRetriesFailsImmediately(): Unit = {
    val attempts = new AtomicInteger(0)
    def operation = Future {
      attempts.incrementAndGet()
      throw new RuntimeException("no retries")
    }

    val exception = Assertions.assertThrows(classOf[RuntimeException], () => {
      Await.result(
        Util.retryWithBackoff(operation, retries = 0, retryDelayMillis = 10),
        5.seconds
      )
    })
    Assertions.assertEquals("no retries", exception.getMessage)
    Assertions.assertEquals(1, attempts.get())
  }

  @Test def testRetryWithBackoff_failsOnLastRetryThenSucceeds(): Unit = {
    val attempts = new AtomicInteger(0)
    def operation = Future {
      val attempt = attempts.incrementAndGet()
      if (attempt <= 3) throw new RuntimeException(s"Fail attempt $attempt")
      "recovered"
    }

    // 3 retries means: 1 initial + 3 retries = up to 4 attempts; fails on 1,2,3 and succeeds on 4
    val result = Await.result(
      Util.retryWithBackoff(operation, retries = 3, retryDelayMillis = 10),
      5.seconds
    )
    Assertions.assertEquals("recovered", result)
    Assertions.assertEquals(4, attempts.get())
  }

  @Test def testRetryWithBackoff_preservesExceptionFromLastAttempt(): Unit = {
    val attempts = new AtomicInteger(0)
    def operation = Future {
      val attempt = attempts.incrementAndGet()
      throw new RuntimeException(s"error $attempt")
    }

    val exception = Assertions.assertThrows(classOf[RuntimeException], () => {
      Await.result(
        Util.retryWithBackoff(operation, retries = 2, retryDelayMillis = 10),
        5.seconds
      )
    })
    // The exception from the last attempt (attempt 3) should be propagated
    Assertions.assertEquals("error 3", exception.getMessage)
  }

  @Test def testRetryWithBackoff_initialDelayIsApplied(): Unit = {
    val start = System.currentTimeMillis()
    val result = Await.result(
      Util.retryWithBackoff(Future.successful("ok"), retries = 0, retryDelayMillis = 10, initialDelayMillis = 200),
      5.seconds
    )
    val elapsed = System.currentTimeMillis() - start
    Assertions.assertEquals("ok", result)
    Assertions.assertTrue(elapsed >= 150, s"Expected at least 150ms elapsed due to initialDelay, got ${elapsed}ms")
  }

  @Test def testRetryWithBackoff_exponentialBackoffIncreasesDelay(): Unit = {
    val timestamps = new java.util.concurrent.ConcurrentLinkedQueue[Long]()
    val attempts = new AtomicInteger(0)
    def operation = Future {
      timestamps.add(System.currentTimeMillis())
      val attempt = attempts.incrementAndGet()
      if (attempt <= 3) throw new RuntimeException(s"Fail $attempt")
      "done"
    }

    Await.result(
      Util.retryWithBackoff(operation, retries = 3, retryDelayMillis = 100),
      10.seconds
    )

    val times = timestamps.asScala.toList
    Assertions.assertEquals(4, times.size)
    // Delays should be approximately: 100ms, 200ms, 400ms
    // Verify that each successive delay is roughly double the previous one
    val delays = times.zip(times.tail).map((a, b) => b - a)
    // delay(0) ~ 100ms, delay(1) ~ 200ms, delay(2) ~ 400ms
    Assertions.assertTrue(delays(0) >= 80, s"First delay ${delays(0)}ms should be >= 80ms")
    Assertions.assertTrue(delays(1) >= 160, s"Second delay ${delays(1)}ms should be >= 160ms")
    Assertions.assertTrue(delays(2) >= 320, s"Third delay ${delays(2)}ms should be >= 320ms")
  }

  // --- toIterator tests ---

  @Test def testToIterator(): Unit = {
    val order: java.util.List[String] = util.ArrayList[String]()

    val iterators: Iterator[Future[Iterator[Int]]] = (1 to 10).iterator.map(i => {
      order.add(s"iterator$i")
      Future.successful(Seq(i).iterator)
    }).iterator

    val result = Util.toIterator(iterators, 3, 1.minute).flatten
    result.foreach(i => order.add(s"$i"))

    Assertions.assertEquals(List(
      "iterator1",
      "iterator2",
      "iterator3",
      "1",
      "iterator4",
      "2",
      "iterator5",
      "3",
      "iterator6",
      "4",
      "iterator7",
      "5",
      "iterator8",
      "6",
      "iterator9",
      "7",
      "iterator10",
      "8",
      "9",
      "10",
    ), order.asScala.toList)
  }
}
