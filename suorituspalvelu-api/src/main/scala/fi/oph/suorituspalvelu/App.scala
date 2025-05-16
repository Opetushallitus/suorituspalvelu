package fi.oph.suorituspalvelu

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication
@EnableWebMvc
class App {

}

object App {
  @main
  def mainMethod(args: String*): Unit =
    main(args.toArray)

  def main(args: Array[String]): Unit =
    // swagger
    System.setProperty("springdoc.api-docs.path", "/openapi/v3/api-docs")
    System.setProperty("springdoc.swagger-ui.path", "/static/swagger-ui/index.html")
    System.setProperty("springdoc.swagger-ui.tagsSorter", "alpha")

    SpringApplication.run(classOf[App], args:_*)
}
