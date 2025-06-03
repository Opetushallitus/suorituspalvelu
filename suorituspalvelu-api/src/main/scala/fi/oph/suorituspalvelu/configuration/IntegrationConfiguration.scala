package fi.oph.suorituspalvelu.configuration

import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.virta.VirtaClientImpl
import fi.oph.suorituspalvelu.integration.client.{HakemuspalveluClientImpl, KoskiClient}
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.{Bean, Configuration}

@Configuration
class IntegrationConfiguration {

  @Bean
  def getKoskiIntegration(): KoskiIntegration =
    new KoskiIntegration

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

    new HakemuspalveluClientImpl(casClient)
  }
}
