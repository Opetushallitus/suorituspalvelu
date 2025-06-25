package fi.oph.suorituspalvelu.configuration

import ch.qos.logback.access.tomcat.LogbackValve
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.{Bean, Configuration, Profile}

import java.io.IOException
import java.nio.file.{Files, Paths, StandardCopyOption}

@Configuration
@Profile(Array("default"))
class AccessLogConfiguration {

  val CONFIG_FILE = "logback-access.xml"

  @Bean
  def containerCustomizer(@Value("${logback.access:}") path: String): WebServerFactoryCustomizer[TomcatServletWebServerFactory] =
    container =>
      if (container.isInstanceOf[TomcatServletWebServerFactory])
        container.addContextCustomizers(context =>
          try
            // LogbackValve suostuu lukemaan konfiguraation vain classpathilta tai tomcat.base/tomcat.home -hakemistoista, joten kopioidaan
            Files.copy(Paths.get(path), Paths.get(context.getCatalinaBase.getPath + "/" + CONFIG_FILE), StandardCopyOption.REPLACE_EXISTING)
          catch
            case e: IOException => throw new RuntimeException(e)

          val logbackValve = new LogbackValve()
          logbackValve.setFilename(CONFIG_FILE)
          logbackValve.setAsyncSupported(true)
          context.getPipeline().addValve(logbackValve)
        );
  
}
