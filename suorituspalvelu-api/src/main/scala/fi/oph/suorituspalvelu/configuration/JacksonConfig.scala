package fi.oph.suorituspalvelu.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

import java.util

@Configuration
class JacksonConfig extends WebMvcConfigurer {

    override def configureMessageConverters(converters: util.List[HttpMessageConverter[_]]): Unit =
        val mapper = ObjectMapper()
        mapper.registerModule(new JavaTimeModule())
        mapper.registerModule(DefaultScalaModule)
        converters.add(MappingJackson2HttpMessageConverter(mapper))
}