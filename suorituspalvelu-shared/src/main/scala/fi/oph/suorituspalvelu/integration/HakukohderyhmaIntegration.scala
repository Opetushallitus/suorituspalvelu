package fi.oph.suorituspalvelu.integration

import com.github.benmanes.caffeine.cache.{Caffeine, LoadingCache}
import fi.oph.suorituspalvelu.integration.client.HakukohderyhmaClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class HakukohderyhmaIntegration {

  @Autowired val tarjontaIntegration: TarjontaIntegration = null
  @Autowired val hakukohderyhmaClient: HakukohderyhmaClient = null

  private val LOG = LoggerFactory.getLogger(classOf[HakukohderyhmaIntegration])
  private val TIMEOUT = 120.seconds

  private val hakukohderyhmatByHakuCache: LoadingCache[String, Map[String, Set[String]]] = Caffeine.newBuilder()
    .expireAfterWrite(java.time.Duration.ofHours(6))
    .refreshAfterWrite(java.time.Duration.ofHours(3))
    .build[String, Map[String, Set[String]]](hakuOid => {
      val hakukohdeOids = tarjontaIntegration.getHakukohteetForHaku(hakuOid).map(_.oid).toSet
      if (hakukohdeOids.isEmpty) {
        LOG.info(s"Haulle $hakuOid ei löytynyt hakukohteita, palautetaan tyhjä lista")
        Map.empty
      } else {
        Await.result(hakukohderyhmaClient.fetchHakukohderyhmat(hakukohdeOids), TIMEOUT)
          .map(h => h.oid -> h.hakukohderyhmat.toSet)
          .toMap
      }
    })

  def getHakukohderyhmatForHaku(hakuOid: String): Map[String, Set[String]] = {
    hakukohderyhmatByHakuCache.get(hakuOid)
  }
}
