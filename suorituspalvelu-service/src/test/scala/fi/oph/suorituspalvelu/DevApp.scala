package fi.oph.suorituspalvelu

import org.testcontainers.containers.PostgreSQLContainer

import scala.jdk.CollectionConverters.SeqHasAsJava

object DevApp {

  private val postgres: PostgreSQLContainer[_] = new PostgreSQLContainer("postgres:15")

  private def startContainers(): Unit =
    postgres.withUsername("app")
    postgres.withPassword("app")
    postgres.setPortBindings(List("55432:5432").asJava)
    postgres.start()

  @main
  def mainMethod(args: String*): Unit =
    main(args.toArray)

  def main(args: Array[String]): Unit =
    if (!sys.env.getOrElse("NO_DB", "false").equalsIgnoreCase("true")) {
      startContainers()
    }

    // virta-integraatio
    System.setProperty("integrations.virta.jarjestelma", "")
    System.setProperty("integrations.virta.tunnus", "")
    System.setProperty("integrations.virta.avain", "salaisuus")
    System.setProperty("integrations.virta.base-url", "http://virtawstesti.csc.fi:8084")

    // cas-configuraatio
    System.setProperty("cas-service.service", sys.env.getOrElse("CAS_SERVICE_URL", "https://localhost:8443"))
    System.setProperty("cas-service.sendRenew", "false")
    System.setProperty("cas-service.key", "suorituspalvelu")

    val virkailijaDomain = sys.env.getOrElse("VIRKAILIJA_DOMAIN", "virkailija.hahtuvaopintopolku.fi")
    val virkailijaCasUrl = s"https://$virkailijaDomain/cas"
    System.setProperty("web.url.cas-login", sys.env.getOrElse("CAS_LOGIN_URL", s"$virkailijaCasUrl/login"))
    System.setProperty("web.url.cas", virkailijaCasUrl)
    System.setProperty("host.virkailija", sys.env.getOrElse("VIRKAILIJA_DOMAIN", "virkailija.hahtuvaopintopolku.fi"))

    App.main(args)
}
