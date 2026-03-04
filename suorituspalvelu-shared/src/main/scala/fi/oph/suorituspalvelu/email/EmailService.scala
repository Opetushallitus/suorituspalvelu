package fi.oph.suorituspalvelu.email

import jakarta.mail.internet.{InternetAddress, MimeMessage}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.{JavaMailSender, JavaMailSenderImpl, MimeMessageHelper}
import org.springframework.stereotype.Component

import java.io.{PrintWriter, StringWriter}
import java.util.Properties
import scala.io.Source

@Component
class EmailService(@Value("${email.enabled:false}") sendingEnabled: Boolean,
                   @Value("${email.sender}") senderAddress: String,
                   @Value("${email.smtp.host:invalid.domain}") smtpHost: String,
                   @Value("${email.smtp.port:25}") smtpPort: Int,
                   @Value("${email.smtp.username:}") smtpUsername: String,
                   @Value("${email.smtp.password:}") smtpPassword: String,
                   @Value("${email.smtp.use_authentication:false}") useAuthentication: Boolean,
                   @Value("${email.smtp.use_tls:false}") useTLS: Boolean,
                   @Value("${email.recipients:}") recipientsList: String,
                   @Value("${email.sender-name:Suorituspalvelu}") senderName: String) {

  private val LOG = LoggerFactory.getLogger(classOf[EmailService])

  private val recipients: Array[String] =
    recipientsList.split(",").map(_.trim).filter(_.nonEmpty)

  private val mailSender: JavaMailSender = {
    val sender = JavaMailSenderImpl()
    sender.setHost(smtpHost)
    sender.setPort(smtpPort)
    if useAuthentication then
      sender.setUsername(smtpUsername)
      sender.setPassword(smtpPassword)
    val props = Properties()
    props.put("mail.smtp.auth", useAuthentication.toString)
    props.put("mail.smtp.starttls.enable", useTLS.toString)
    props.put("mail.smtp.connectiontimeout", "10000")
    props.put("mail.smtp.timeout", "10000")
    sender.setJavaMailProperties(props)
    sender
  }

  private val errorEmailTemplate: String = {
    val source = Source.fromResource("email/error-email.html")
    try source.mkString finally source.close()
  }

  def sendErrorEmail(subject: String, htmlContent: String, exception: Throwable): Unit = {
    if !sendingEnabled then
      LOG.info(s"Sähköpostin lähetys ei käytössä, ei lähetetä viestiä: $subject")
      return

    if recipients.isEmpty then
      LOG.warn(s"Vastaanottajia ei ole määritelty, ei lähetetä viestiä: $subject")
      return

    val stackTraceStr = {
      val sw = StringWriter()
      exception.printStackTrace(PrintWriter(sw))
      sw.toString
    }

    val html = errorEmailTemplate
      .replace("{{CONTENT}}", htmlContent)
      .replace("{{EXCEPTION_MESSAGE}}", escapeHtml(exception.getMessage))
      .replace("{{STACK_TRACE}}", escapeHtml(stackTraceStr))

    try {
      val message: MimeMessage = mailSender.createMimeMessage()
      val helper = MimeMessageHelper(message, false, "UTF-8")
      helper.setFrom(InternetAddress(senderAddress, senderName))
      helper.setTo(recipients)
      helper.setSubject(subject)
      helper.setText(html, true)
      mailSender.send(message)
      LOG.info(s"Sähköposti lähetetty vastaanottajille: ${recipients.mkString(", ")}")
    } catch {
      case e: Exception =>
        LOG.error(s"Sähköpostin lähetys epäonnistui vastaanottajille: ${recipients.mkString(", ")}", e)
        throw e
    }
  }

  private def escapeHtml(text: String): String =
    if text == null then ""
    else text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
