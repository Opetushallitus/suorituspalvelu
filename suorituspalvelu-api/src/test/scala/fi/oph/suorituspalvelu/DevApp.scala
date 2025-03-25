package fi.oph.suorituspalvelu

import org.testcontainers.containers.PostgreSQLContainer
import scala.jdk.CollectionConverters.SeqHasAsJava

object DevApp {

  private val postgres: PostgreSQLContainer[_] = new PostgreSQLContainer("postgres:15").withCommand("postgres")

  private def startContainers(): Unit =
    postgres.withUsername("app")
    postgres.withPassword("app")
    postgres.setPortBindings(List("55432:5432").asJava)
    postgres.start()

  @main
  def mainMethod(args: String*): Unit =
    main(args.toArray)

  def main(args: Array[String]): Unit =
    startContainers()

    // cas-configuraatio
    System.setProperty("cas-service.service", "https://localhost:8443")
    System.setProperty("cas-service.sendRenew", "false")
    System.setProperty("cas-service.key", "suorituspalvelu")
    System.setProperty("web.url.cas", "https://virkailija.hahtuvaopintopolku.fi/cas")
    System.setProperty("host.virkailija", "virkailija.hahtuvaopintopolku.fi")

    App.main(args)
}
