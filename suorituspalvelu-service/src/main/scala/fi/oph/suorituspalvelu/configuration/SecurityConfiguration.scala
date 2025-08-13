package fi.oph.suorituspalvelu.configuration

import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.vm.sade.java_utils.security.OpintopolkuCasAuthenticationFilter
import fi.vm.sade.javautils.kayttooikeusclient.OphUserDetailsServiceImpl
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.{FilterChain, ServletRequest, ServletResponse}
import org.apereo.cas.client.validation.{Cas20ProxyTicketValidator, TicketValidator}
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.core.annotation.Order
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
import jakarta.servlet.FilterConfig
import org.springframework.http.HttpMethod

/**
 *
 */
@Configuration
@EnableWebSecurity
@EnableJdbcHttpSession
class SecurityConfiguration {

  @Bean
  def spaResourceFilter: Filter = new Filter {
    override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
      val path = request.asInstanceOf[HttpServletRequest].getServletPath
      val isApiResource = path.startsWith("/error") || path.startsWith("/api") || path == "/actuator/health" || path.startsWith("/openapi/v3/api-docs")
      val isStaticResource = path.matches(".*\\.(js|css|ico|html|json|png|svg)") || path.startsWith("/static")
      if (isApiResource || isStaticResource) {
        chain.doFilter(request, response)
      }
      else {
        // SPA resource
        request.getRequestDispatcher("/index.html").forward(request, response)
      }
    }
    override def init(filterConfig: FilterConfig): Unit = {}
    override def destroy(): Unit = {}
  }

  @Bean
  @Order(2)
  def spaFilterChain(http: HttpSecurity, casAuthenticationEntryPoint: CasAuthenticationEntryPoint, authenticationFilter: CasAuthenticationFilter): SecurityFilterChain =
    http
      .addFilterBefore(spaResourceFilter, classOf[CasAuthenticationFilter])
      .build()

  @Bean
  @Order(1)
  def casLoginFilterChain(http: HttpSecurity, casAuthenticationEntryPoint: CasAuthenticationEntryPoint, authenticationFilter: CasAuthenticationFilter): SecurityFilterChain =
    http
      .securityMatcher("/api/**")
      .authorizeHttpRequests(requests => requests
        .requestMatchers(HttpMethod.GET, ApiConstants.HEALTHCHECK_PATH)
        .permitAll()
        .anyRequest
        .fullyAuthenticated)
      .csrf(c => c.disable())
      .exceptionHandling(c => c.authenticationEntryPoint(casAuthenticationEntryPoint))
      .addFilter(authenticationFilter)
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
