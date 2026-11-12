package fi.oph.suorituspalvelu.security

import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.cas.ServiceProperties
import org.springframework.security.cas.authentication.ServiceAuthenticationDetails
import org.springframework.security.cas.web.CasAuthenticationFilter
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetailsSource
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetails

import java.io.IOException

/**
 * Spring Security 7 -yhteensopiva versio java-utilsin (opintopolku-cas-servlet-filter) OpintopolkuCasAuthenticationFilterista,
 * joka on käännetty Spring Security 6:ta vasten eikä toimi Security 7:n kanssa (ServiceAuthenticationDetails on siirtynyt
 * pakettiin org.springframework.security.cas.authentication).
 *
 * Tiketti luetaan http-parametrin lisäksi myös CasSecurityTicket-headerista.
 */
class OpintopolkuCasAuthenticationFilter(serviceProperties: ServiceProperties) extends CasAuthenticationFilter {

  setServiceProperties(serviceProperties)
  setAuthenticationDetailsSource(new OpintopolkuServiceAuthenticationDetailsSource(serviceProperties))

  override protected def obtainArtifact(request: HttpServletRequest): String =
    // ticket-parametrin lisäksi autentikoidaan myös CasSecurityTicket-headerissa oleva ticket
    val casTicketHeader = request.getHeader(OpintopolkuCasAuthenticationFilter.CAS_SECURITY_TICKET)
    if (casTicketHeader != null)
      // jos ko tiketillä ollaan jo autentikoiduttu sessio, ei tehdä sitä enää
      if (casTicketHeader.equals(getSessionTicket))
        logger.debug("ticket already authenticated in session: " + casTicketHeader)
        null
      else
        casTicketHeader
    // getParameter -kutsu saattaa hajottaa tietyt post-requestit, siksi ticket-parametrin validointi skipataan,
    // jos post-request ja headerissa ei tikettiä
    else if ("POST".equals(request.getMethod))
      logger.debug("skipping cas obtainArtifact because post and already authenticated")
      null
    else
      super.obtainArtifact(request)

  override protected def requiresAuthentication(request: HttpServletRequest, response: HttpServletResponse): Boolean =
    // kirjaudutaan uudelleen jos tiketti vaihtuu kesken session
    val sessionTicket = getSessionTicket
    if (sessionTicket != null)
      val requestTicket = obtainArtifact(request)
      if (requestTicket != null && !requestTicket.equals(sessionTicket))
        logger.warn("clear authentication because ticket changed, requestTicket: " + requestTicket + ", sessionTicket: " + sessionTicket)
        SecurityContextHolder.clearContext()
    super.requiresAuthentication(request, response)

  // cas-tiketti on tallessa authentication.credentials-kentässä
  private def getSessionTicket: AnyRef =
    Option(SecurityContextHolder.getContext.getAuthentication).map(_.getCredentials).orNull

  override def attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse): Authentication =
    try
      super.attemptAuthentication(request, response)
    catch
      case e: RuntimeException =>
        e.getCause match
          case cause: IOException if cause.getMessage != null && cause.getMessage.contains("412") && cause.getMessage.contains("proxyValidate") =>
            throw new BadCredentialsException("Possible error with auth system or infra.. check: 1) configs, urls, ports, 2) caller ticket not expired, 3) cas logs for req ticket: " + obtainArtifact(request), e)
          case _ => throw e
}

object OpintopolkuCasAuthenticationFilter {
  val CAS_SECURITY_TICKET = "CasSecurityTicket"
}

class OpintopolkuServiceAuthenticationDetailsSource(serviceProperties: ServiceProperties) extends ServiceAuthenticationDetailsSource(serviceProperties) {
  override def buildDetails(request: HttpServletRequest): ServiceAuthenticationDetails =
    new OpintopolkuAuthenticationDetails(request, serviceProperties.getService)
}

class OpintopolkuAuthenticationDetails(request: HttpServletRequest, serviceUrl: String) extends WebAuthenticationDetails(request) with ServiceAuthenticationDetails {
  override def getServiceUrl: String = serviceUrl
}
