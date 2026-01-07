package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.*
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Lahtokoulu, Opiskeluoikeus, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.client.{AtaruPermissionRequest, AtaruPermissionResponse, HakemuspalveluClientImpl, KoutaHaku}
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration, OnrMasterHenkilo}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiUtil, NOT_DEFINED_PLACEHOLDER}
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.security.{SecurityConstants, SecurityOperaatiot, VirkailijaAuthorization}
import fi.oph.suorituspalvelu.ui.EntityToUIConverter
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}
import fi.oph.suorituspalvelu.validation.Validator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.time.{Instant, LocalDate}
import java.util.Optional
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@Component
class LahtokoulutService {

  val LOG = LoggerFactory.getLogger(classOf[UIService])

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val onrIntegration: OnrIntegration = null
  
  val ONR_TIMEOUT = 10.seconds
  
  def haeLuokat(oppilaitosOid: String, valmistumisVuosi: Int): Set[String] = {
    kantaOperaatiot.haeLuokat(oppilaitosOid, valmistumisVuosi)
  }

  def haeOhjattavatJaLuokat(oppilaitosOid: String, vuosi: Int): Set[(String, String)] = {
    kantaOperaatiot.haeHenkilotJaLuokat(oppilaitosOid, vuosi).map((henkilo, luokka) => henkilo -> luokka)
  }
  
  def haeLahtokoulut(henkiloOid: String): Seq[Lahtokoulu] = {
    val r = onrIntegration.getAliasesForPersonOids(Set(henkiloOid))
      .map(_.allOidsByQueriedOids(henkiloOid))
      .map(aliakset => {
        val suoritukset = aliakset.map(alias => this.kantaOperaatiot.haeSuoritukset(alias)).flatMap(_.values).flatten
        KoskiUtil.haeLahtokoulut(suoritukset)
      })

    Await.result(r, 30.seconds)
  }
}