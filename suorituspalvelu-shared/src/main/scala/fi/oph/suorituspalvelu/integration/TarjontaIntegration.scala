package fi.oph.suorituspalvelu.integration

import fi.oph.suorituspalvelu.integration.client.{KoutaClient, KoutaHaku, Ohjausparametrit, OhjausparametritClient}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import com.github.benmanes.caffeine.cache.{Caffeine, LoadingCache}

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

class TarjontaIntegration {

  @Autowired val ohjausparametritClient: OhjausparametritClient = null
  @Autowired val koutaClient: KoutaClient = null
  val LOG = LoggerFactory.getLogger(classOf[TarjontaIntegration])

  val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  val helsinkiZone = ZoneId.of("Europe/Helsinki")

  private val HAUT_TIMEOUT = 120.seconds
  private val OHJAUSPARAMETRIT_TIMEOUT = 30.seconds

  private val allHautCache: LoadingCache[String, Map[String, KoutaHaku]] = Caffeine.newBuilder()
    .expireAfterWrite(java.time.Duration.ofHours(6))
    .refreshAfterWrite(java.time.Duration.ofHours(3))
    .build[String, Map[String, KoutaHaku]](_ => {
      Await.result(koutaClient.fetchHaut(), HAUT_TIMEOUT)
    })

  private val ohjausparametritCache: LoadingCache[String, Ohjausparametrit] = Caffeine.newBuilder()
    .expireAfterWrite(java.time.Duration.ofHours(6))
    .refreshAfterWrite(java.time.Duration.ofHours(3))
    .build[String, Ohjausparametrit](hakuOid => {
      Await.result(ohjausparametritClient.haeOhjausparametrit(hakuOid), OHJAUSPARAMETRIT_TIMEOUT)
    })

  def getHaku(hakuOid: String): Option[KoutaHaku] = {
    allHautCache.get("haut").get(hakuOid)
  }

  def getOhjausparametrit(hakuOid: String): Ohjausparametrit = {
    ohjausparametritCache.get(hakuOid)
  }

  def aktiivisetHaut(): Seq[KoutaHaku] = {
    val haut = allHautCache.get("haut")
    val ohjausparametrit = Await.result(ohjausparametritClient.haeKaikkiOhjausparametrit(), OHJAUSPARAMETRIT_TIMEOUT)

    haut.values.filter(h => hakuOnAktiivinen(h, ohjausparametrit(h.oid))).toSeq
  }

  def tarkistaHaunAktiivisuus(hakuOid: String): Boolean = {
    val ohjausparametrit: Ohjausparametrit = getOhjausparametrit(hakuOid)
    val haku: Option[KoutaHaku] = getHaku(hakuOid)
    haku.exists(h => hakuOnAktiivinen(h, ohjausparametrit))
  }

  //Haku on aktiivinen, jos sen ensimmäisen hakuajan alkuhetki on menneisyydessä
  // ja sen ohjausparametri PH_HKP = hakukierroksen päättymishetki on tulevaisuudessa.
  def hakuOnAktiivinen(haku: KoutaHaku, ohjausparametrit: Ohjausparametrit): Boolean = {
    val now = System.currentTimeMillis()
    val jokuHakuajoistaOnAlkanut = haku.hakuajat.map(_.alkaa).exists(alkuDateTimeStr => {
      LocalDateTime.parse(alkuDateTimeStr, formatter).atZone(helsinkiZone).toInstant.toEpochMilli < now
    })
    val hakukierrosPaattynyt = ohjausparametrit.PH_HKP.exists(_.date < now)

    jokuHakuajoistaOnAlkanut && !hakukierrosPaattynyt
  }
}
