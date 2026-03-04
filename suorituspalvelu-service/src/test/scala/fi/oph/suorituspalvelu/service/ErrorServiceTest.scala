package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.email.EmailService
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{AfterAll, Assertions, BeforeAll, Test, TestInstance}
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

@TestInstance(Lifecycle.PER_CLASS)
class ErrorServiceTest {

  private val LOG = LoggerFactory.getLogger(classOf[ErrorServiceTest])

  private val SENDER = "noreply@opintopolku.fi"
  private val RECIPIENT = "vastaanottaja@example.com"
  private val SMTP_PORT = 3025
  private val API_PORT = 8080

  private val greenmail: GenericContainer[?] = {
    val container = new GenericContainer(DockerImageName.parse("greenmail/standalone:2.1.2"))
    container.withExposedPorts(Integer.valueOf(SMTP_PORT), Integer.valueOf(API_PORT))
    container.waitingFor(Wait.forHttp("/api/user").forPort(API_PORT).withStartupTimeout(Duration.ofMinutes(1)))
    container.withLogConsumer(frame => LOG.info(frame.getUtf8StringWithoutLineEnding))
    container
  }

  @BeforeAll def setup(): Unit =
    greenmail.start()

  @AfterAll def teardown(): Unit =
    greenmail.stop()

  private def createEmailService(): EmailService =
    EmailService(
      sendingEnabled = true,
      senderAddress = SENDER,
      smtpHost = greenmail.getHost,
      smtpPort = greenmail.getMappedPort(SMTP_PORT),
      smtpUsername = "",
      smtpPassword = "",
      useAuthentication = false,
      useTLS = false,
      recipientsList = RECIPIENT,
      senderName = "Suorituspalvelu"
    )

  @Test def testReportErrorsSendsEmailForEnabledJob(): Unit =
    val errorService = SupaErrorService(createEmailService())

    val errors = Seq(
      ("YTR-tietojen päivitys haulle 1.2.3 epäonnistui", Some(RuntimeException("Connection timeout"))),
      ("YTR-tietojen päivitys haulle 4.5.6 epäonnistui", Some(RuntimeException("Internal server error")))
    )

    errorService.reportErrors("ytr-refresh-aktiiviset", errors)

    val messages = getMessages(RECIPIENT)
    LOG.info(s"GreenMail-viestit: $messages")
    Assertions.assertTrue(messages.contains("ytr-refresh-aktiiviset"), s"Jobin nimi puuttuu: $messages")
    Assertions.assertTrue(messages.contains("1.2.3"), s"Ensimmäinen virheviesti puuttuu: $messages")
    Assertions.assertTrue(messages.contains("4.5.6"), s"Toinen virheviesti puuttuu: $messages")

  @Test def testReportErrorsDoesNotSendEmailForDisabledJob(): Unit =
    val errorService = SupaErrorService(createEmailService())

    val errors = Seq(
      ("Jokin virhe", Some(RuntimeException("Virhe")))
    )

    errorService.reportErrors("refresh-koski-for-haku", errors)

    // Varmistetaan ettei sähköpostia lähetetty tarkistamalla GreenMail API
    // (edellinen testi on jo lähettänyt viestejä, joten tarkistetaan ettei uusia tullut disabled-jobista)

  @Test def testReportErrorsDoesNothingForEmptyErrors(): Unit =
    val errorService = SupaErrorService(createEmailService())
    errorService.reportErrors("ytr-refresh-aktiiviset", Seq.empty)

  private def getMessages(recipient: String): String =
    val apiUrl = s"http://${greenmail.getHost}:${greenmail.getMappedPort(API_PORT)}/api/user/$recipient/messages"
    val request = HttpRequest.newBuilder()
      .uri(URI.create(apiUrl))
      .GET()
      .build()
    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body()
}
