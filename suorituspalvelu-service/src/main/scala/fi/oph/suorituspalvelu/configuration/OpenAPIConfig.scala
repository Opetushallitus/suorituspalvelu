package fi.oph.suorituspalvelu.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.{Bean, Configuration, Profile}
import org.springframework.beans.factory.annotation.Value

@Configuration
@Profile(Array("default"))
class OpenAPIConfig {

  @Bean def openApi(@Value("${host.virkailija}") host: String): OpenAPI =
    new OpenAPI().addServersItem(new Server().url(s"https://${host}/suorituspalvelu"))

}