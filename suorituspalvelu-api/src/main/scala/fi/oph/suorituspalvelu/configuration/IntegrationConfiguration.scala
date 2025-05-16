package fi.oph.suorituspalvelu.configuration

import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.KoskiClient
import fi.oph.suorituspalvelu.integration.virta.VirtaClientImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.{Bean, Configuration, Profile}

import java.time.Duration

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

}
