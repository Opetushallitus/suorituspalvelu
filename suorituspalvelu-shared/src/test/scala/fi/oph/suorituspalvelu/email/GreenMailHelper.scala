package fi.oph.suorituspalvelu.email

import org.slf4j.{Logger, LoggerFactory}
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

class GreenMailHelper {

  private val LOG: Logger = LoggerFactory.getLogger(classOf[GreenMailHelper])

  val SENDER = "noreply@opintopolku.fi"
  val RECIPIENT = "vastaanottaja@example.com"

  private val SMTP_PORT = 3025
  private val API_PORT = 8080

  val container: GenericContainer[?] = {
    val c = new GenericContainer(DockerImageName.parse("greenmail/standalone:2.1.2"))
    c.withExposedPorts(Integer.valueOf(SMTP_PORT), Integer.valueOf(API_PORT))
    c.waitingFor(Wait.forHttp("/api/user").forPort(API_PORT).withStartupTimeout(Duration.ofMinutes(1)))
    c.withLogConsumer(frame => LOG.info(frame.getUtf8StringWithoutLineEnding))
    c
  }

  def start(): Unit = container.start()

  def stop(): Unit = container.stop()

  def createEmailService(recipientsList: String = RECIPIENT, sendingEnabled: Boolean = true): EmailService =
    EmailService(
      sendingEnabled = sendingEnabled,
      senderAddress = SENDER,
      smtpHost = container.getHost,
      smtpPort = container.getMappedPort(SMTP_PORT),
      smtpUsername = "",
      smtpPassword = "",
      useAuthentication = false,
      useTLS = false,
      recipientsList = recipientsList,
      senderName = "Suorituspalvelu"
    )

  def getMessages(recipient: String = RECIPIENT): String =
    val apiUrl = s"http://${container.getHost}:${container.getMappedPort(API_PORT)}/api/user/$recipient/messages"
    val request = HttpRequest.newBuilder()
      .uri(URI.create(apiUrl))
      .GET()
      .build()
    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body()
}
