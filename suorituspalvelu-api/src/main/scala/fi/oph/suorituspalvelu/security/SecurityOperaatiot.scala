package fi.oph.suorituspalvelu.security

import org.springframework.security.core.context.SecurityContextHolder

import scala.jdk.CollectionConverters.*

class SecurityOperaatiot(
  getOikeudet: () => Set[String] = () => SecurityContextHolder.getContext.getAuthentication.getAuthorities.asScala.map(a => a.getAuthority).toSet,
  getUsername: () => String = () => SecurityContextHolder.getContext.getAuthentication.getName())  {

  private lazy val kayttajanOikeudet = getOikeudet()

  val identiteetti = getUsername()

  def getIdentiteetti(): String =
    identiteetti

  def onRekisterinpitaja(): Boolean =
    kayttajanOikeudet.contains(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL)

}
