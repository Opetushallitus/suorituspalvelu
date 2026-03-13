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

  private val EMAIL_ENABLED_JOBS = Set("refresh-ytr-for-aktiiviset-haut", "refresh-virta-for-aktiiviset-haut", "refresh-ytr-for-haut", "refresh-virta-for-haut")

  def reportErrors(jobName: String, errors: Seq[(String, Option[Exception])]): Unit = {
    if (!errors.isEmpty) {
      LOG.warn(s"Tausta-ajo $jobName päättyi ${errors.size} virheeseen")
      if (EMAIL_ENABLED_JOBS.contains(jobName)) {
        try {
          val errorMessages: Seq[String] = errors.map((message, exception) =>
            s"$message (${exception.map(e => s"${e.getClass.getSimpleName}: ${e.getMessage}").getOrElse("Ei poikkeusta")})")
          emailService.sendErrorSummaryEmail(
            jobName = jobName,
            errorMessages = errorMessages
          )
        } catch {
          case e: Exception =>
            LOG.error(s"Virheraportin sähköpostin lähetys epäonnistui jobille $jobName", e)
        }
      }
    }
  }
}
