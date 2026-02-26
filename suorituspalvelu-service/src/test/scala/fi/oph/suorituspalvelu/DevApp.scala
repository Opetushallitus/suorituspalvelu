package fi.oph.suorituspalvelu

import org.testcontainers.postgresql.PostgreSQLContainer

import scala.jdk.CollectionConverters.SeqHasAsJava

object DevApp {

  private val postgres: PostgreSQLContainer = new PostgreSQLContainer("postgres:15")

  private def startContainers(): Unit =
    postgres.withUsername("app")
    postgres.withPassword("app")
    postgres.setPortBindings(List("55432:5432").asJava)
    postgres.start()

  @main
  def mainMethod(args: String*): Unit =
    main(args.toArray)

  def main(args: Array[String]): Unit =
    // virta-integraatio
    System.setProperty("integrations.virta.jarjestelma", "")
    System.setProperty("integrations.virta.tunnus", "")
    System.setProperty("integrations.virta.avain", "salaisuus")
    System.setProperty("integrations.virta.base-url", "http://virtawstesti.csc.fi:8084")

    // cas-configuraatio
    System.setProperty("cas-service.sendRenew", "false")
    System.setProperty("cas-service.key", "suorituspalvelu")
    System.setProperty("cas-service.service", "https://localhost:8443/suorituspalvelu")

    // Ylikirjoitukset ympäristömuuttujien perusteella (lokaalia docker-ajoa varten käyttölittymän kanssa)
    val casUsernameOverride = sys.env.getOrElse("CAS_USERNAME", "")
    val casPasswordOverride = sys.env.getOrElse("CAS_PASSWORD", "")
    if (casUsernameOverride.nonEmpty && casPasswordOverride.nonEmpty) {
      System.setProperty("integrations.koski.username", casUsernameOverride)
      System.setProperty("integrations.koski.password", casPasswordOverride)
    }

    val casServiceUrlOverride = sys.env.getOrElse("CAS_SERVICE_URL", "")
    if (casServiceUrlOverride.nonEmpty) {
      System.setProperty("cas-service.service", casServiceUrlOverride)
    }

    val virkailijaDomainOverride = sys.env.getOrElse("VIRKAILIJA_DOMAIN", "")
    if (virkailijaDomainOverride.nonEmpty) {
      val virkailijaBaseUrl = s"https://$virkailijaDomainOverride"
      val virkailijaCasUrl = s"$virkailijaBaseUrl/cas"
      System.setProperty("web.url.cas-login", sys.env.getOrElse("CAS_LOGIN_URL", s"$virkailijaCasUrl/login"))
      System.setProperty("web.url.cas", virkailijaCasUrl)
      System.setProperty("host.virkailija", virkailijaDomainOverride)
      System.setProperty("integrations.koski.base-url", virkailijaBaseUrl)
    } else {
      val virkailijaDomain = "virkailija.testiopintopolku.fi"
      System.setProperty("host.virkailija", virkailijaDomain)
      System.setProperty("web.url.cas", s"https://$virkailijaDomain/cas")
      System.setProperty("web.url.cas-login", s"https://$virkailijaDomain/cas/login")
    }

    // Docker-lokaaliajo käynnistää myös tietokannan, joten ei käynnistetä sitä tässä
    if (!sys.env.getOrElse("NO_DB", "false").equalsIgnoreCase("true")) {
      startContainers()
    }

    App.main(args)
}
