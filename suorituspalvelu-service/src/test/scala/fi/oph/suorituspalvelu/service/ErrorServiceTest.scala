package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.email.GreenMailHelper
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{AfterAll, Assertions, BeforeAll, Test, TestInstance}
import org.slf4j.LoggerFactory

@TestInstance(Lifecycle.PER_CLASS)
class ErrorServiceTest {

  private val LOG = LoggerFactory.getLogger(classOf[ErrorServiceTest])

  private val greenmail = GreenMailHelper()

  @BeforeAll def setup(): Unit =
    greenmail.start()

  @AfterAll def teardown(): Unit =
    greenmail.stop()

  @Test def testReportErrorsSendsEmailForEnabledJob(): Unit =
    val errorService = SupaErrorService(greenmail.createEmailService())

    val errors = Seq(
      ("YTR-tietojen päivitys haulle 1.2.3 epäonnistui", Some(RuntimeException("Connection timeout"))),
      ("YTR-tietojen päivitys haulle 4.5.6 epäonnistui", Some(RuntimeException("Internal server error")))
    )

    errorService.reportErrors("ytr-refresh-aktiiviset", errors)

    val messages = greenmail.getMessages()
    LOG.info(s"GreenMail-viestit: $messages")
    Assertions.assertTrue(messages.contains("ytr-refresh-aktiiviset"), s"Jobin nimi puuttuu: $messages")
    Assertions.assertTrue(messages.contains("1.2.3"), s"Ensimmäinen virheviesti puuttuu: $messages")
    Assertions.assertTrue(messages.contains("4.5.6"), s"Toinen virheviesti puuttuu: $messages")

  @Test def testReportErrorsDoesNotSendEmailForDisabledJob(): Unit =
    val errorService = SupaErrorService(greenmail.createEmailService())

    val errors = Seq(
      ("Jokin virhe", Some(RuntimeException("Virhe")))
    )

    errorService.reportErrors("refresh-koski-for-haku", errors)

  @Test def testReportErrorsDoesNothingForEmptyErrors(): Unit =
    val errorService = SupaErrorService(greenmail.createEmailService())
    errorService.reportErrors("ytr-refresh-aktiiviset", Seq.empty)
}
