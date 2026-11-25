package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.*
import fi.oph.suorituspalvelu.business.KantaOperaatiot
import fi.oph.suorituspalvelu.integration.OnrIntegration
import fi.oph.suorituspalvelu.integration.client.RetryConfig
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil
import fi.oph.suorituspalvelu.resource.api.LahtokouluAuthorization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import java.time.LocalDate

@Component
class LahtokoulutService {

  val LOG = LoggerFactory.getLogger(classOf[LahtokoulutService])

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val onrIntegration: OnrIntegration = null

  implicit val onrRetryConfig: RetryConfig = RetryConfig(retries = 1)

  def haeLuokat(oppilaitosOid: String, valmistumisVuosi: Int, nykyhetki: LocalDate = LocalDate.now()): Set[String] = {
    kantaOperaatiot.haeLuokat(nykyhetki, false, oppilaitosOid, valmistumisVuosi, None)
  }

  def haeOhjattavatJaLuokat(oppilaitosOid: String, vuosi: Int, nykyhetki: LocalDate = LocalDate.now()): Set[(String, String)] = {
    val henkilotJaLuokat = kantaOperaatiot.haeHenkilotJaLuokat(nykyhetki, false, oppilaitosOid, vuosi, None)
    if(henkilotJaLuokat.isEmpty)
      Set.empty
    else
      val r = onrIntegration.getAliasesForPersonOids(henkilotJaLuokat.map(_._1))
        .map(aliakset => {
          henkilotJaLuokat.flatMap((h, l) => aliakset.allOidsByQueriedOids(h).map(oid => (oid, l)))
        })
      Await.result(r, 65.seconds)
  }

  def haeLahtokouluAuthorizations(henkiloOid: String): Seq[LahtokouluAuthorization] = {
    val r = onrIntegration.getAliasesForPersonOids(Set(henkiloOid))
      .map(_.allOidsByQueriedOids(henkiloOid))
      .map(aliakset => {
        val lahtokoulut = this.kantaOperaatiot.haeLahtokoulut(aliakset)
        KoskiUtil.luoLahtokouluAuthorizations(lahtokoulut.toSeq)
      })

    Await.result(r, 65.seconds)
  }
}