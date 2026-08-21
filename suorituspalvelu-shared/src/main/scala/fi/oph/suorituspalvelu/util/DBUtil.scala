package fi.oph.suorituspalvelu.util

import slick.jdbc.JdbcBackend.JdbcDatabaseDef

import scala.concurrent.Await
import slick.jdbc.PostgresProfile.api.*

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, DurationInt}

object DBUtil {

  private final val TIMEOUT_FOR_INFINITE: Duration = 300.seconds

  extension (database: JdbcDatabaseDef) {
    def runBlocking[R](operations: DBIO[R], timeout: Duration): R = {
      if (timeout.isFinite) {
        Await.result(
          database.run(
            operations.withStatementParameters(statementInit = st => st.setQueryTimeout(timeout.toSeconds.toInt))
          ),
          timeout + Duration(1, TimeUnit.SECONDS)
        )
      } else {
        Await.result(
          database.run(operations),
          TIMEOUT_FOR_INFINITE
        )
      }
    }
  }
}
