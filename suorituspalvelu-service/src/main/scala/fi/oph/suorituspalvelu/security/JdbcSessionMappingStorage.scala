package fi.oph.suorituspalvelu.security

import fi.oph.suorituspalvelu.business.KantaOperaatiot
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import jakarta.servlet.http.HttpSession
import org.apereo.cas.client.session.SessionMappingStorage
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend

class JdbcSessionMappingStorage(sessionRepository: SessionRepository[Session], db: JdbcBackend.JdbcDatabaseDef) extends SessionMappingStorage {

  val LOG = LoggerFactory.getLogger(classOf[JdbcSessionMappingStorage])

  @Override
  def removeSessionByMappingId(mappingId: String): HttpSession = {
    val kantaOperaatiot = new KantaOperaatiot(db)
    val session = kantaOperaatiot.getSessionIdByMappingId(mappingId)
      .map(s => sessionRepository.findById(s))
    session.foreach(s => {
      LOG.info("Removed session " + s.getId + " by mapping " + mappingId)
      sessionRepository.deleteById(s.getId)
    })
    null
  }

  @Override
  def removeBySessionById(sessionId: String) = {
    val kantaOperaatiot = new KantaOperaatiot(db)
    kantaOperaatiot.deleteCasMappingBySessionId(sessionId)
  }

  @Override
  def addSessionById(mappingId: String, session: HttpSession) = {
    val kantaOperaatiot = new KantaOperaatiot(db)
    kantaOperaatiot.addMappingForSessionId(mappingId, session.getId)
  }

}

