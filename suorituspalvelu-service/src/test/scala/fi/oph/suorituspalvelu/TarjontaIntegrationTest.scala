package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.integration.TarjontaIntegration
import fi.oph.suorituspalvelu.integration.client.{KoutaHaku, KoutaHakuaika, Ohjausparametrit, DateParam}
import org.junit.jupiter.api.{Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.Assertions.*

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

@Test
@TestInstance(Lifecycle.PER_CLASS)
class TarjontaIntegrationTest {

  val tarjontaIntegration = new TarjontaIntegration

  val now = System.currentTimeMillis()
  val pastTime: Long = now - (1000 * 60 * 60 * 24) // One day ago
  val futureTime: Long = now + (1000 * 60 * 60 * 24) // One day in future

  val pastTimeStr = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(pastTime),
      ZoneId.of("Europe/Helsinki"))
    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  val futureTimeStr = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(futureTime),
      ZoneId.of("Europe/Helsinki"))
    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  def createHaku(hakuOid: String, hakuaikaAlkaa: String, hakuaikaPaattyy: Option[String] = None): KoutaHaku = {
    KoutaHaku(
      oid = hakuOid,
      tila = "julkaistu",
      nimi = Map("fi" -> s"Testi haku $hakuOid"),
      hakutapaKoodiUri = "hakutapa_01",
      kohdejoukkoKoodiUri = Some("kohdejoukko_01"),
      hakuajat = List(KoutaHakuaika(alkaa = hakuaikaAlkaa, paattyy = hakuaikaPaattyy)),
      kohdejoukonTarkenneKoodiUri = None
    )
  }

  @Test
  def testHakuOnAktiivinenWhenHakuaikaStartedAndHakukierrosNotEnded(): Unit = {
    val hakuOid = "1.2.246.562.29.01000000000000012345"
    val haku = createHaku(hakuOid, pastTimeStr)
    val ohjausparametrit = Ohjausparametrit(Some(DateParam(futureTime)))

    val result = tarjontaIntegration.hakuOnAktiivinen(haku, ohjausparametrit)
    assertTrue(result, "Haku should be active when hakuaika has started and hakukierros has not ended")
  }

  @Test
  def testHakuOnAktiivinenWhenHakuaikaHasNotStarted(): Unit = {
    val hakuOid = "1.2.246.562.29.01000000000000054321"
    val haku = createHaku(hakuOid, futureTimeStr)
    val ohjausparametrit = Ohjausparametrit(Some(DateParam(futureTime)))

    val result = tarjontaIntegration.hakuOnAktiivinen(haku, ohjausparametrit)
    assertFalse(result, "Haku should not be active when hakuaika has not started.")
  }

  @Test
  def testHakuOnAktiivinenWhenHakuaikaHasStartedAndHakukierrosHasEnded(): Unit = {
    val hakuOid = "1.2.246.562.29.01000000000000022222"
    val haku = createHaku(hakuOid, pastTimeStr)
    val ohjausparametrit = Ohjausparametrit(Some(DateParam(pastTime)))

    val result = tarjontaIntegration.hakuOnAktiivinen(haku, ohjausparametrit)
    assertFalse(result, "Haku should not be active when hakuaika has started but hakukierros has ended")
  }

  @Test
  def testHakuOnAktiivinenWithMultipleHakuaikaPeriods(): Unit = {
    val hakuOid = "1.2.246.562.29.01000000000000044444"
    val multiPeriodHaku = KoutaHaku(
      oid = hakuOid,
      tila = "julkaistu",
      nimi = Map("fi" -> s"Testi haku $hakuOid"),
      hakutapaKoodiUri = "hakutapa_01",
      kohdejoukkoKoodiUri = Some("kohdejoukko_01"),
      hakuajat = List(
        KoutaHakuaika(alkaa = futureTimeStr, paattyy = None),
        KoutaHakuaika(alkaa = pastTimeStr, paattyy = None)
      ),
      kohdejoukonTarkenneKoodiUri = None
    )
    val ohjausparametrit = Ohjausparametrit(Some(DateParam(futureTime)))

    val result = tarjontaIntegration.hakuOnAktiivinen(multiPeriodHaku, ohjausparametrit)
    assertTrue(result, "Haku should be active when at least one hakuaika period has started and hakukierros has not ended")
  }
}

