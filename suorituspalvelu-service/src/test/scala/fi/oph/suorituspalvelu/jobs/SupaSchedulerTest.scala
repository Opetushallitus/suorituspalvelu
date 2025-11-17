package fi.oph.suorituspalvelu.jobs

import fi.oph.suorituspalvelu.BaseIntegraatioTesti
import org.junit.jupiter.api.{Assertions, Test}

import java.time.Duration
import java.util.UUID

class SupaSchedulerTest extends BaseIntegraatioTesti {

  @Test def testRunBasicJob(): Unit =
    // luodaan scheduler
    val scheduler = SupaScheduler(1, Duration.ofMillis(100), this.datasource, this.kantaOperaatiot)
    val payload = "payload"
    var result: String = null

    // rekisteröidään jobi, joka tallentaa payload-arvon result-muuttujaan
    val testJob = scheduler.registerJob("testjob", (ctx, data) => {
      result = data
    }, Seq.empty)

    // pyöräytetään scheduleria
    scheduler.start()
    waitUntilReady(testJob.run(payload))
    scheduler.stop()

    // jobi ajanut
    Assertions.assertEquals(payload, result)


  @Test def testJobRetries(): Unit =
    // luodaan scheduler
    val scheduler = SupaScheduler(1, Duration.ofMillis(100), this.datasource, this.kantaOperaatiot)
    var failures: Int = 0

    // rekisteröidään jobi joka menee läpi toisella retryllä
    val testJob = scheduler.registerJob("testjob", (ctx, data) => {
      failures += 1
      if(failures<2) throw new RuntimeException()
    }, Seq(Duration.ofMillis(10), Duration.ofMillis(10)))

    // pyöräytetään scheduleria
    scheduler.start()
    waitUntilReady(testJob.run(null))
    scheduler.stop()

    // jobi feilannut kaksi kertaa
    Assertions.assertEquals(2, failures)

  @Test def testScheduledJob(): Unit =
    // luodaan scheduler
    val scheduler = SupaScheduler(1, Duration.ofMillis(100), this.datasource, this.kantaOperaatiot)
    var counter: Int = 0

    // määritellään jobi joka ajaa joka sekunti ja lisää counterin arvoa yhdellä
    scheduler.scheduleJob("testjob", (ctx, data) => {
      counter = Option.apply(data).map(d => d.toInt + 1).getOrElse(0)
      counter.toString
    }, "* * * * * *")

    // pyöräytetään scheduleria ja odotetaan että jobi ajanut kaksi kertaa
    scheduler.start()
    def waitUntilReady(retries: Int = 20): Unit =
      if(retries == 0) Assertions.fail("Jobi ei valmistunut")
      if(counter!=2) {
        Thread.sleep(200)
        waitUntilReady(retries - 1)
      }
    waitUntilReady()
    scheduler.stop()

  
}
