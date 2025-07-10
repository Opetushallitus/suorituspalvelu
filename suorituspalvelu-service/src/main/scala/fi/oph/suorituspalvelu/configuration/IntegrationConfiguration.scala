package fi.oph.suorituspalvelu.configuration

import scala.concurrent.duration.DurationInt
import com.github.benmanes.caffeine.cache.Caffeine
import fi.oph.suorituspalvelu.integration.{KoskiIntegration, OnrIntegrationImpl}
import fi.oph.suorituspalvelu.integration.virta.VirtaClientImpl
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, KoodistoClient, KoskiClient, Koodi, OnrClientImpl, Organisaatio, OrganisaatioClient, YtrClient}
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

  @Bean
  def getYtrClient(@Value("${integrations.ytr.username}") user: String,
                   @Value("${integrations.ytr.password}") password: String,
                   @Value("${integrations.ytr.base-url}") envBaseUrl: String): YtrClient =
    new YtrClient(user, password, envBaseUrl)

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

  @Bean
  def getKoodistoProvider(@Value("${integrations.koski.base-url}") envBaseUrl: String): KoodistoProvider = {
    val KOODISTO_TIMEOUT = 120.seconds
    val koodistoClient = KoodistoClient(envBaseUrl)

    (koodisto: String) =>
      val cache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofHours(24))
        .refreshAfterWrite(Duration.ofHours(12))
        .build(koodisto => Await.result(koodistoClient.haeKoodisto(koodisto.toString), KOODISTO_TIMEOUT))

      cache.get(koodisto)
  }
}

trait OrganisaatioProvider {
  def haeOrganisaationTiedot(koodiArvo: String): Organisaatio
}

trait KoodistoProvider {
  def haeKoodisto(koodisto: String): Map[String, Koodi]
}
