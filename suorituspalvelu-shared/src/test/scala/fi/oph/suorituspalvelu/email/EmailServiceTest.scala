package fi.oph.suorituspalvelu.email

import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{AfterAll, Assertions, BeforeAll, Test, TestInstance}
import org.slf4j.LoggerFactory

@TestInstance(Lifecycle.PER_CLASS)
class EmailServiceTest {

  private val LOG = LoggerFactory.getLogger(classOf[EmailServiceTest])

  private val greenmail = GreenMailHelper()

  @BeforeAll def setup(): Unit =
    greenmail.start()

  @AfterAll def teardown(): Unit =
    greenmail.stop()

  @Test def testSendErrorEmail(): Unit =
    val emailService = greenmail.createEmailService()

    val testException = RuntimeException("Testivirhe")
    emailService.sendErrorEmail(
      subject = "Virhe suorituspalvelussa",
      htmlContent = "<p>Jokin meni pieleen</p>",
      exception = testException
    )

    val messages = greenmail.getMessages()
    LOG.info(s"GreenMail-viestit: $messages")
    Assertions.assertTrue(messages.contains("Virhe suorituspalvelussa"), s"Otsikko puuttuu: $messages")
    Assertions.assertTrue(messages.contains("Testivirhe"), s"Exception-viesti puuttuu: $messages")
    Assertions.assertTrue(messages.contains("Jokin meni pieleen"), s"HTML-sisältö puuttuu: $messages")

  @Test def testSendingDisabled(): Unit =
    val emailService = greenmail.createEmailService(sendingEnabled = false)

    emailService.sendErrorEmail(
      subject = "Ei pitäisi lähteä",
      htmlContent = "<p>Testi</p>",
      exception = RuntimeException("Testi")
    )

  @Test def testEmptyRecipients(): Unit =
    val emailService = greenmail.createEmailService(recipientsList = "")

    emailService.sendErrorEmail(
      subject = "Ei vastaanottajia",
      htmlContent = "<p>Testi</p>",
      exception = RuntimeException("Testi")
    )
}
