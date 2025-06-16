package fi.oph.suorituspalvelu.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.gson.{Gson, JsonArray, JsonParser}
import fi.vm.sade.auditlog.*
import fi.vm.sade.javautils.http.HttpServletRequestUtils
import jakarta.servlet.http.HttpServletRequest
import org.ietf.jgss.Oid
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

import java.net.InetAddress

class AuditLog {}

object AuditLog {

  val LOG = LoggerFactory.getLogger(classOf[AuditLog])

  val mapper = {
    // luodaan objectmapper jonka pitäisi pystyä serialisoimaan "kaikki mahdollinen"
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JavaTimeModule())
    mapper.registerModule(new Jdk8Module())
    mapper
  }

  lazy val audit = {
    new Audit(entry => LOG.info(entry), "suorituspalvelu", ApplicationType.VIRKAILIJA)
  }

  def logRead(kohde: String, tunniste: String, operaatio: AuditOperation, request: HttpServletRequest): Unit =
    val target = new Target.Builder().setField(kohde, tunniste).build()
    audit.log(getUser(request), operaatio, target, Changes.EMPTY)

  def logCreate(user: User, targetFields: Map[String, String], operaatio: AuditOperation, entity: Option[Any]): Unit =
    val target = new Target.Builder()
    for ((key, value) <- targetFields)
      target.setField(key, value)
    // Tämä kludge on lisätty koska audit-lokirjaston gson-konfiguraatio ei kykene serialisoimaan esim. java.time.Instant-luokkia
    // (eikä paljon muutakaan), mutta kirjaston metodit haluavat kuitenkin parametreina gson-objekteja.
    // Tällä tavoin audit lokille voi antaa suoraan entiteetin ja kaikki kentät tallennetaan.
    val elements: JsonArray = new JsonArray()
    if(entity.isDefined)
      elements.add(JsonParser.parseString(mapper.writeValueAsString(entity.get)))
    audit.log(user, operaatio, target.build(), elements)

  def logChanges(user: User, targetFields: Map[String, String], operaatio: AuditOperation, changes: Changes): Unit =
    val target = new Target.Builder()
    for ((key, value) <- targetFields)
      target.setField(key, value)
    audit.log(user, operaatio, target.build(), changes)

  def getUser(request: HttpServletRequest): User =
    val userOid = getCurrentPersonOid()
    val ip = getInetAddress(request)
    new User(userOid, ip, request.getSession(false).getId(), Option(request.getHeader("User-Agent")).getOrElse("Tuntematon user agent"))

  def getCurrentPersonOid(): Oid =
    val authentication: Authentication = SecurityContextHolder.getContext().getAuthentication()
    if (authentication != null)
      try
        new Oid(authentication.getName())
      catch
        case e: Exception => LOG.error(s"Käyttäjän oidin luonti epäonnistui: ${authentication.getName}")
    null

  def getInetAddress(request: HttpServletRequest): InetAddress =
    InetAddress.getByName(HttpServletRequestUtils.getRemoteAddress(request))
}

