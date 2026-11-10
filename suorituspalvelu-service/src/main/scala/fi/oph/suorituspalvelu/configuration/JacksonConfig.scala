package fi.oph.suorituspalvelu.configuration

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class JacksonConfig extends WebMvcConfigurer {

    // Spring Boot 4 käyttää oletuksena Jackson 3:a, asetetaan JSON-konvertteriksi Jackson 2 -konvertteri. Konvertteri
    // asetetaan oletuskonvertterin paikalle, jotta esim. byte- ja String-konvertterit säilyvät sen edellä.
    override def configureMessageConverters(builder: HttpMessageConverters.ServerBuilder): Unit =
        val mapper = ObjectMapper()
        mapper.registerModule(new JavaTimeModule())
        mapper.registerModule(new Jdk8Module())
        mapper.registerModule(DefaultScalaModule)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        builder.withJsonConverter(MappingJackson2HttpMessageConverter(mapper))
}
