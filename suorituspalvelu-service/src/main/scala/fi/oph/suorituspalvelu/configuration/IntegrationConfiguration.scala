package fi.oph.suorituspalvelu.configuration

import scala.concurrent.duration.DurationInt
import com.github.benmanes.caffeine.cache.Caffeine
import fi.oph.suorituspalvelu.integration.{KoskiIntegration, OnrIntegrationImpl}
import fi.oph.suorituspalvelu.integration.virta.VirtaClientImpl
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, KoodistoClient, KoskiClient, Koodi, OnrClientImpl, Organisaatio, OrganisaatioClient}
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.{Bean, Configuration}

import java.time.Duration
import scala.concurrent.{Await, Future}

@Configuration
class IntegrationConfiguration {

  @Bean
  def getKoskiIntegration(): KoskiIntegration =
    new KoskiIntegration

  @Bean
  def getOnrIntegration(): OnrIntegrationImpl =
    new OnrIntegrationImpl

  @Bean
  def getKoskiClient(@Value("${integrations.koski.username}") user: String,
                     @Value("${integrations.koski.password}") password: String,
                     @Value("${integrations.koski.base-url}") envBaseUrl: String): KoskiClient =
    new KoskiClient(user, password, envBaseUrl)

  @Bean
  def getVirtaClient(@Value("${integrations.virta.jarjestelma}") jarjestelma: String,
                     @Value("${integrations.virta.tunnus}") tunnus: String,
                     @Value("${integrations.virta.avain}") avain: String,
                     @Value("${integrations.virta.base-url}") environmentBaseUrl: String): VirtaClientImpl =
    new VirtaClientImpl(jarjestelma, tunnus, avain, environmentBaseUrl)

  //Todo, näille konffeille vaikka uusi cas-ryhmä application.ymliin, kannattanee käyttää samoja tunnuksia kaikissa cas-käyttöisissä integraatioissa
  @Bean
  def getHakemuspalveluClient(@Value("${integrations.koski.username}") user: String,
                              @Value("${integrations.koski.password}") password: String,
                              @Value("${integrations.koski.base-url}") envBaseUrl: String,
                              @Value("${integrations.koski.base-url}") casUrl: String): HakemuspalveluClientImpl = {

    val CALLER_ID = "1.2.246.562.10.00000000001.suorituspalvelu"
    val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
      user,
      password,
      envBaseUrl + "/cas",
      envBaseUrl + "/lomake-editori",
      CALLER_ID,
      CALLER_ID,
      "/auth/cas")
      .setJsessionName("ring-session").build

    val casClient: CasClient = CasClientBuilder.build(casConfig)

    new HakemuspalveluClientImpl(casClient, envBaseUrl)
  }

  @Bean
  def getOnrClient(@Value("${integrations.koski.username}") user: String,
                   @Value("${integrations.koski.password}") password: String,
                   @Value("${integrations.koski.base-url}") envBaseUrl: String,
                   @Value("${integrations.koski.base-url}") casUrl: String): OnrClientImpl = {

    val CALLER_ID = "1.2.246.562.10.00000000001.suorituspalvelu"
    val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
      user,
      password,
      envBaseUrl + "/cas",
      envBaseUrl + "/oppijanumerorekisteri-service",
      CALLER_ID,
      CALLER_ID,
      "/j_spring_cas_security_check")
      .setJsessionName("JSESSIONID").build

    val casClient: CasClient = CasClientBuilder.build(casConfig)

    new OnrClientImpl(casClient, envBaseUrl)
  }

  private val ORGANISAATIO_TIMEOUT = 30.seconds

  @Bean
  def getOrganisaatioProvider(@Value("${integrations.koski.base-url}") envBaseUrl: String): OrganisaatioProvider = {
    new OrganisaatioProvider {

      val organisaatioClient = OrganisaatioClient(envBaseUrl)

      val cache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofHours(12))
        .build(koodiArvo => Await.result(organisaatioClient.haeOrganisaationTiedot(koodiArvo.toString), ORGANISAATIO_TIMEOUT))

      override def haeOrganisaationTiedot(koodiArvo: String): Organisaatio = cache.get(koodiArvo)
    }
  }

  private val KOODISTO_TIMEOUT = 120.seconds

  @Bean
  def getKoulutusProvider(@Value("${integrations.koski.base-url}") envBaseUrl: String): KoulutusProvider = {
    new KoulutusProvider {

      val koodistoClient = KoodistoClient(envBaseUrl)

      val cache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofHours(24))
        .refreshAfterWrite(Duration.ofHours(12))
        .build(koodiArvo => Await.result(koodistoClient.haeKoodisto("koulutus"), KOODISTO_TIMEOUT))

      override def haeKoulutus(koodiArvo: String): Option[Koodi] = cache.get(koodiArvo).get(koodiArvo)
    }
  }

}

trait OrganisaatioProvider {
  def haeOrganisaationTiedot(koodiArvo: String): Organisaatio
}

trait KoulutusProvider {
  def haeKoulutus(koodiArvo: String): Option[Koodi]
}
