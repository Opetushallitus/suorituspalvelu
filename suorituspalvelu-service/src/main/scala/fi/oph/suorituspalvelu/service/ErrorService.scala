package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.email.EmailService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

trait ErrorService {
  def reportErrors(jobName: String, errors: Seq[(String, Option[Exception])]): Unit
}

@Component
class SupaErrorService(emailService: EmailService) extends ErrorService {

  private val LOG = LoggerFactory.getLogger(classOf[SupaErrorService])

  private val EMAIL_ENABLED_JOBS = Set("ytr-refresh-aktiiviset", "virta-refresh-aktiiviset")

  def reportErrors(jobName: String, errors: Seq[(String, Option[Exception])]): Unit = {
    if errors.isEmpty then return

    LOG.warn(s"Tausta-ajo $jobName päättyi ${errors.size} virheeseen")

    if !EMAIL_ENABLED_JOBS.contains(jobName) then return

    val htmlContent = s"<p>Tausta-ajo <strong>$jobName</strong> päättyi ${errors.size} virheeseen.</p>"
    val exceptionMessages: Seq[String] = errors.map(_._1)

    try {
      emailService.sendErrorSummaryEmail(
        subject = s"Suorituspalvelu: $jobName - ${errors.size} virhettä",
        htmlContent = htmlContent,
        exceptionMessages = exceptionMessages
      )
    } catch {
      case e: Exception =>
        LOG.error(s"Virheraportin sähköpostin lähetys epäonnistui jobille $jobName", e)
    }
  }
}
