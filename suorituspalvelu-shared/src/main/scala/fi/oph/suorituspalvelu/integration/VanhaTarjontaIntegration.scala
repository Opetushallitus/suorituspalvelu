package fi.oph.suorituspalvelu.integration

import fi.oph.suorituspalvelu.integration.client.{VanhaTarjontaHaku, VanhaTarjontaHakukohde, VanhaTarjontaClient}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import com.github.benmanes.caffeine.cache.{Caffeine, LoadingCache}

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

class VanhaTarjontaIntegration {

  @Autowired val vanhaTarjontaClient: VanhaTarjontaClient = null
  val LOG = LoggerFactory.getLogger(classOf[VanhaTarjontaIntegration])

  private val TIMEOUT = 120.seconds

  private val allHautCache: LoadingCache[String, Map[String, VanhaTarjontaHaku]] = Caffeine.newBuilder()
    .expireAfterWrite(java.time.Duration.ofHours(6))
    .refreshAfterWrite(java.time.Duration.ofHours(3))
    .build[String, Map[String, VanhaTarjontaHaku]](_ => {
      Await.result(vanhaTarjontaClient.haeHaut(), TIMEOUT).map(h => h.oid -> h).toMap
    })

  private val hakukohdeCache: LoadingCache[String, VanhaTarjontaHakukohde] = Caffeine.newBuilder()
    .expireAfterWrite(java.time.Duration.ofHours(6))
    .refreshAfterWrite(java.time.Duration.ofHours(3))
    .build[String, VanhaTarjontaHakukohde](hakukohdeOid => {
      Await.result(vanhaTarjontaClient.haeHakukohde(hakukohdeOid), TIMEOUT)
    })

  def getHaku(hakuOid: String): Option[VanhaTarjontaHaku] = {
    allHautCache.get("haut").get(hakuOid)
  }

  def getHakukohde(hakukohdeOid: String): VanhaTarjontaHakukohde = {
    hakukohdeCache.get(hakukohdeOid)
  }
}
