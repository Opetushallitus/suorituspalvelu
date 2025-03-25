package fi.oph.suorituspalvelu.business

import slick.jdbc.JdbcBackend
import slick.jdbc.PostgresProfile.api.*

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.slf4j.LoggerFactory

import java.util.UUID

class KantaOperaatiot(db: JdbcBackend.JdbcDatabaseDef) {

  final val DB_TIMEOUT = 30.seconds
  val LOG = LoggerFactory.getLogger(classOf[KantaOperaatiot])

  def getUUID(): UUID =
    UUID.randomUUID()

  def tallennaSuoritus(oppijaNumero: String): Suoritus =
    val tunniste = getUUID()
    val lahetysInsertAction =
      sqlu"""INSERT INTO suoritukset(tunniste, oppijanumero) VALUES(${tunniste.toString}::uuid, ${oppijaNumero})"""

    Await.result(db.run(DBIO.sequence(Seq(lahetysInsertAction)).transactionally), DB_TIMEOUT)
    Suoritus(tunniste, oppijaNumero)

  def haeSuoritukset(oppijaNumero: String): Seq[Suoritus] =
    Await.result(db.run(
        sql"""
          SELECT tunniste, oppijanumero
          FROM suoritukset
          WHERE oppijanumero=${oppijaNumero}
       """.as[(String, String)]), DB_TIMEOUT)
      .map((tunniste, oppijaNumero) =>
        Suoritus(UUID.fromString(tunniste), oppijaNumero))
}
