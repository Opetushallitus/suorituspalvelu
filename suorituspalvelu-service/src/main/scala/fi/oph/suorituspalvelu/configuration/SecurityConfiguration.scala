package fi.oph.suorituspalvelu.configuration

import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.vm.sade.java_utils.security.OpintopolkuCasAuthenticationFilter
import fi.vm.sade.javautils.kayttooikeusclient.OphUserDetailsServiceImpl
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import jakarta.servlet.{FilterChain, ServletRequest, ServletResponse}
import org.apereo.cas.client.validation.{Cas20ProxyTicketValidator, TicketValidator}
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.cas.ServiceProperties
import org.springframework.security.cas.authentication.CasAuthenticationProvider
import org.springframework.security.cas.web.{CasAuthenticationEntryPoint, CasAuthenticationFilter}
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession
import org.springframework.session.web.http.{CookieSerializer, DefaultCookieSerializer}
import jakarta.servlet.Filter
import org.springframework.http.HttpMethod
import org.springframework.security.web.access.intercept.AuthorizationFilter

/**
 *
 */
@Configuration
@EnableWebSecurity
@EnableJdbcHttpSession
class SecurityConfiguration {
  private val FRONTEND_ROUTES: Map[String, String] = Map(
    "/" -> "/index.html",
  )

  @Bean
  def frontendResourceFilter: Filter = (request: ServletRequest, response: ServletResponse, chain: FilterChain) => {
    val req = request.asInstanceOf[HttpServletRequest]
    val res = response.asInstanceOf[HttpServletResponse]
    val contextPath = req.getContextPath
    val path = req.getRequestURI.stripPrefix(contextPath)
    val queryString = Option(req.getQueryString).map(q => s"?$q").getOrElse("")

    val isForwarded = req.getAttribute("custom.forwarded") != null
    if (!isForwarded && FRONTEND_ROUTES.flatMap(_.toList).toSet.contains(path) && queryString.matches("[?&]continue")) {
      // Poistetaan index.html osoitteesta
      val newPath = path.stripSuffix("/index.html")
      // Poistetaan "continue" query-parametri
      res.sendRedirect(contextPath + newPath + queryString.replaceAll("[?&]continue", ""))
    } else {
      FRONTEND_ROUTES.get(path) match {
        case Some(route) => {
          // Lisätään attribuutti, jotta voidaan välttää uudelleenohjauslooppi
          request.setAttribute("custom.forwarded", true)
          // Ohjataan frontend-pyyntö html-tiedostoon
          request.getRequestDispatcher(route).forward(request, response)
        }
        case None => chain.doFilter(request, response)
      }
    }
  }

  @Bean
  def casLoginFilterChain(http: HttpSecurity, casAuthenticationEntryPoint: CasAuthenticationEntryPoint, authenticationFilter: CasAuthenticationFilter): SecurityFilterChain =
    http
      .authorizeHttpRequests(requests => requests
        .requestMatchers(HttpMethod.GET, ApiConstants.HEALTHCHECK_PATH, "/static/**", "/actuator/health", "/openapi/v3/api-docs/**")
        .permitAll()
        .anyRequest
        .fullyAuthenticated)
      .csrf(c => c.disable())
      .exceptionHandling(c => c.authenticationEntryPoint(casAuthenticationEntryPoint))
      .addFilter(authenticationFilter)
      /* Tehdään ohjaukset käyttöliittymään vasta koko CAS-autentikaation (ja mahdollisen login-uudellenohjauksen) jälkeen.
       * Huom! classOf[CasAuthenticationFilter] ei toimi integraatiotesteissä, koska silloin frontendResourceFilter
       * ajetaan ennen kuin koko CAS-autentikaatiota on tehty, ja koska MockMvc ei aja forwardointeja
       * filter chainin läpi.
       */
      .addFilterAfter(frontendResourceFilter, classOf[AuthorizationFilter])
      .build()

  @Bean
  def cookieSerializer(): CookieSerializer =
    val serializer = new DefaultCookieSerializer()
    serializer.setCookieName("JSESSIONID")
    serializer

  @Bean
  def serviceProperties(@Value("${cas-service.service}") service: String, @Value("${cas-service.sendRenew}") sendRenew: Boolean): ServiceProperties =
    val serviceProperties = new ServiceProperties()
    serviceProperties.setService(service + ApiConstants.CAS_TICKET_VALIDATION_PATH)
    serviceProperties.setSendRenew(sendRenew)
    serviceProperties.setAuthenticateAllArtifacts(true)
    serviceProperties

  //
  // CAS authentication provider (authentication manager)
  //
  @Bean
  def casAuthenticationProvider(serviceProperties: ServiceProperties, ticketValidator: TicketValidator, environment: Environment, @Value("${cas-service.key}") key: String): CasAuthenticationProvider =
    val host = environment.getProperty("host.alb", environment.getRequiredProperty("host.virkailija"))
    val casAuthenticationProvider = new CasAuthenticationProvider()
    casAuthenticationProvider.setAuthenticationUserDetailsService(new OphUserDetailsServiceImpl())
    casAuthenticationProvider.setServiceProperties(serviceProperties)
    casAuthenticationProvider.setTicketValidator(ticketValidator)
    casAuthenticationProvider.setKey(key)
    casAuthenticationProvider

  @Bean
  def ticketValidator(environment: Environment): TicketValidator =
    val ticketValidator = new Cas20ProxyTicketValidator(environment.getRequiredProperty("web.url.cas"))
    ticketValidator.setAcceptAnyProxy(true)
    ticketValidator

  //
  // CAS filter
  //
  @Bean
  def casAuthenticationFilter(authenticationManager: AuthenticationManager, serviceProperties: ServiceProperties): CasAuthenticationFilter =
    val casAuthenticationFilter = new OpintopolkuCasAuthenticationFilter(serviceProperties)
    casAuthenticationFilter.setAuthenticationManager(authenticationManager)
    casAuthenticationFilter.setFilterProcessesUrl(ApiConstants.CAS_TICKET_VALIDATION_PATH)
    casAuthenticationFilter

  //
  // CAS entry point
  //
  @Bean def casAuthenticationEntryPoint(environment: Environment, serviceProperties: ServiceProperties): CasAuthenticationEntryPoint =
    val casAuthenticationEntryPoint = new CasAuthenticationEntryPoint()
    casAuthenticationEntryPoint.setLoginUrl(environment.getRequiredProperty("web.url.cas-login"))
    casAuthenticationEntryPoint.setServiceProperties(serviceProperties)
    casAuthenticationEntryPoint

  @Bean
  def authenticationManager(http: HttpSecurity, casAuthenticationProvider: CasAuthenticationProvider): AuthenticationManager =
    http.getSharedObject(classOf[AuthenticationManagerBuilder])
      .authenticationProvider(casAuthenticationProvider)
      .build()

}
