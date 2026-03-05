package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.*
import fi.oph.suorituspalvelu.business.KantaOperaatiot
import fi.oph.suorituspalvelu.integration.OnrIntegration
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil
import fi.oph.suorituspalvelu.resource.api.LahtokouluAuthorization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

@Component
class LahtokoulutService {

  val LOG = LoggerFactory.getLogger(classOf[LahtokoulutService])

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val onrIntegration: OnrIntegration = null

  val ONR_TIMEOUT = 10.seconds
  
  def haeLuokat(oppilaitosOid: String, valmistumisVuosi: Int): Set[String] = {
    kantaOperaatiot.haeLuokat(None, oppilaitosOid, valmistumisVuosi, None)
  }

  def haeOhjattavatJaLuokat(oppilaitosOid: String, vuosi: Int): Set[(String, String)] = {
    kantaOperaatiot.haeHenkilotJaLuokat(oppilaitosOid, vuosi, None)
  }

  def haeLahtokouluAuthorizations(henkiloOid: String): Seq[LahtokouluAuthorization] = {
    val r = onrIntegration.getAliasesForPersonOids(Set(henkiloOid))
      .map(_.allOidsByQueriedOids(henkiloOid))
      .map(aliakset => {
        val lahtokoulut = this.kantaOperaatiot.haeLahtokoulut(aliakset)
        KoskiUtil.luoLahtokouluAuthorizations(lahtokoulut.toSeq)
      })

    Await.result(r, 30.seconds)
  }
}