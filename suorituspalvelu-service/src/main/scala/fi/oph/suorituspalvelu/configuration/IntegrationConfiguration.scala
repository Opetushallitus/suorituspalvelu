package fi.oph.suorituspalvelu.configuration

import scala.concurrent.duration.DurationInt
import com.github.benmanes.caffeine.cache.{Caffeine, LoadingCache}
import fi.oph.suorituspalvelu.integration.TarjontaIntegration.KOUTA_OID_LENGTH
import fi.oph.suorituspalvelu.integration.{KoskiIntegration, OnrIntegrationImpl, TarjontaIntegration, VanhaTarjontaIntegration}
import fi.oph.suorituspalvelu.integration.virta.VirtaClientImpl
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, Koodi, KoodistoClient, KoskiClient, KoutaClient, OhjausparametritClient, OnrClientImpl, Organisaatio, OrganisaatioClient, VTSClient, VanhaTarjontaClient, YtrClient}
import fi.oph.suorituspalvelu.util.{HakuProvider, HakukohdeProvider, KoodistoProvider, OrganisaatioProvider}
import fi.oph.suorituspalvelu.integration.ytr.YtrIntegration
import fi.oph.suorituspalvelu.util.organisaatio.OrganisaatioUtil
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.{Bean, Configuration}

import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
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
  def getYtrIntegration(): YtrIntegration =
    new YtrIntegration

  @Bean
  def getKoutaIntegration(): TarjontaIntegration =
    new TarjontaIntegration

  @Bean
  def getVanhaTarjontaClient(@Value("${integrations.koski.base-url}") envBaseUrl: String): VanhaTarjontaClient =
    new VanhaTarjontaClient(envBaseUrl)

  @Bean
  def getVanhaTarjontaIntegration(): VanhaTarjontaIntegration =
    new VanhaTarjontaIntegration

  @Bean
  def getKoskiClient(@Value("${integrations.koski.username}") user: String,
                     @Value("${integrations.koski.password}") password: String,
                     @Value("${integrations.koski.base-url}") envBaseUrl: String): KoskiClient =
    new KoskiClient(user, password, envBaseUrl)

  @Bean
  def getOhjausparametritClient(@Value("${integrations.koski.base-url}") envBaseUrl: String): OhjausparametritClient =
    new OhjausparametritClient(envBaseUrl)

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

  @Bean
  def getKoutaClient(@Value("${integrations.koski.username}") user: String,
                     @Value("${integrations.koski.password}") password: String,
                     @Value("${integrations.koski.base-url}") envBaseUrl: String): KoutaClient = {

    val CALLER_ID = "1.2.246.562.10.00000000001.suorituspalvelu"
    val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
      user,
      password,
      envBaseUrl + "/cas",
      envBaseUrl + "/kouta-internal",
      CALLER_ID,
      CALLER_ID,
      "/auth/login")
      .setJsessionName("session").build

    val casClient: CasClient = CasClientBuilder.build(casConfig)

    new KoutaClient(casClient, envBaseUrl)
  }

  @Bean
  def getVTSClient(@Value("${integrations.koski.username}") user: String,
                   @Value("${integrations.koski.password}") password: String,
                   @Value("${integrations.koski.base-url}") envBaseUrl: String): VTSClient = {

    val CALLER_ID = "1.2.246.562.10.00000000001.suorituspalvelu"
    val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
      user,
      password,
      envBaseUrl + "/cas",
      envBaseUrl + "/valinta-tulos-service",
      CALLER_ID,
      CALLER_ID,
      "/auth/login")
      .setJsessionName("session").build

    val casClient: CasClient = CasClientBuilder.build(casConfig)

    new VTSClient(casClient, envBaseUrl)
  }

  private val ORGANISAATIO_TIMEOUT = 30.seconds

  @Bean
  def getOrganisaatioProvider(@Value("${integrations.koski.base-url}") envBaseUrl: String): OrganisaatioProvider = {
    new OrganisaatioProvider {
      val organisaatioClient = new OrganisaatioClient(envBaseUrl)
      private val ORGANISAATIO_TIMEOUT = 120.seconds

      private val hierarkiaCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(6))
        .refreshAfterWrite(Duration.ofHours(3))
        .build[String, Map[String, Organisaatio]](_ => {
          val tuoreHierarkia = Await.result(organisaatioClient.haeHierarkia(), ORGANISAATIO_TIMEOUT)
          OrganisaatioUtil.filterAndFlattenHierarkia(tuoreHierarkia)
        })

      override def orgLookupTable(): Map[String, Organisaatio] =
        hierarkiaCache.get("hierarkia")
    }
  }

  @Bean
  def getKoodistoProvider(@Value("${integrations.koski.base-url}") envBaseUrl: String): KoodistoProvider = {
    val KOODISTO_TIMEOUT = 120.seconds
    val koodistoClient = KoodistoClient(envBaseUrl)
    val cache = Caffeine.newBuilder()
      .maximumSize(10000)
      .expireAfterWrite(Duration.ofHours(24))
      .refreshAfterWrite(Duration.ofHours(12))
      .build(koodisto => Await.result(koodistoClient.haeKoodisto(koodisto.toString), KOODISTO_TIMEOUT))

    (koodisto: String) =>
      cache.get(koodisto)
  }

  @Bean
  def getHakuProvider(tarjontaIntegration: TarjontaIntegration, vanhaTarjontaIntegration: VanhaTarjontaIntegration): HakuProvider =
    hakuOid =>
      if (hakuOid.length == KOUTA_OID_LENGTH) tarjontaIntegration.getHaku(hakuOid)
      else vanhaTarjontaIntegration.getHaku(hakuOid)

  @Bean
  def getHakukohdeProvider(tarjontaIntegration: TarjontaIntegration, vanhaTarjontaIntegration: VanhaTarjontaIntegration): HakukohdeProvider =
    hakukohdeOid =>
      if (hakukohdeOid.length == KOUTA_OID_LENGTH) Some(tarjontaIntegration.getHakukohde(hakukohdeOid))
      else Some(vanhaTarjontaIntegration.getHakukohde(hakukohdeOid))


}
