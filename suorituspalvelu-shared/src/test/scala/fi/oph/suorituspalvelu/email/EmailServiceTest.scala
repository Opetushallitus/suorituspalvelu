package fi.oph.suorituspalvelu.email

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
class EmailServiceTest {

  private val LOG = LoggerFactory.getLogger(classOf[EmailServiceTest])

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

  @Test def testSendErrorEmail(): Unit =
    val emailService = EmailService(
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

    val testException = RuntimeException("Testivirhe")
    emailService.sendErrorEmail(
      subject = "Virhe suorituspalvelussa",
      htmlContent = "<p>Jokin meni pieleen</p>",
      exception = testException
    )

    val messages = getMessages(RECIPIENT)
    LOG.info(s"GreenMail-viestit: $messages")
    Assertions.assertTrue(messages.contains("Virhe suorituspalvelussa"), s"Otsikko puuttuu: $messages")
    Assertions.assertTrue(messages.contains("Testivirhe"), s"Exception-viesti puuttuu: $messages")
    Assertions.assertTrue(messages.contains("Jokin meni pieleen"), s"HTML-sisältö puuttuu: $messages")

  @Test def testSendingDisabled(): Unit =
    val emailService = EmailService(
      sendingEnabled = false,
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

    emailService.sendErrorEmail(
      subject = "Ei pitäisi lähteä",
      htmlContent = "<p>Testi</p>",
      exception = RuntimeException("Testi")
    )

  @Test def testEmptyRecipients(): Unit =
    val emailService = EmailService(
      sendingEnabled = true,
      senderAddress = SENDER,
      smtpHost = greenmail.getHost,
      smtpPort = greenmail.getMappedPort(SMTP_PORT),
      smtpUsername = "",
      smtpPassword = "",
      useAuthentication = false,
      useTLS = false,
      recipientsList = "",
      senderName = "Suorituspalvelu"
    )

    emailService.sendErrorEmail(
      subject = "Ei vastaanottajia",
      htmlContent = "<p>Testi</p>",
      exception = RuntimeException("Testi")
    )

  private def getMessages(recipient: String): String =
    val apiUrl = s"http://${greenmail.getHost}:${greenmail.getMappedPort(API_PORT)}/api/user/$recipient/messages"
    val request = HttpRequest.newBuilder()
      .uri(URI.create(apiUrl))
      .GET()
      .build()
    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body()
}
