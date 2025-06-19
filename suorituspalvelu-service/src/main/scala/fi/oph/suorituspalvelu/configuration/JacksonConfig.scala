package fi.oph.suorituspalvelu.configuration

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

import java.util
import scala.jdk.CollectionConverters.*

@Configuration
class JacksonConfig extends WebMvcConfigurer {

    override def extendMessageConverters(converters: util.List[HttpMessageConverter[_]]): Unit =
        val jacksonConverter = converters.asScala.find(c => c.isInstanceOf[MappingJackson2HttpMessageConverter]).get
        val index = converters.indexOf(jacksonConverter)

        val mapper = ObjectMapper()
        mapper.registerModule(new JavaTimeModule())
        mapper.registerModule(new Jdk8Module())
        mapper.registerModule(DefaultScalaModule)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        converters.set(index, MappingJackson2HttpMessageConverter(mapper))
}