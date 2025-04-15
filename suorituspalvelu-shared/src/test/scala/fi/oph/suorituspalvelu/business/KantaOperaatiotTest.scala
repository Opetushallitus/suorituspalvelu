package fi.oph.suorituspalvelu.business

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import fi.oph.suorituspalvelu.business.Tietolahde.KOSKI
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.{AfterAll, AfterEach, Assertions, BeforeAll, BeforeEach, Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.testcontainers.containers.PostgreSQLContainer
import slick.jdbc.JdbcBackend.Database
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory
import slick.jdbc.PostgresProfile.api.*

import scala.concurrent.duration.DurationInt
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Random

@TestInstance(Lifecycle.PER_CLASS)
class KantaOperaatiotTest {

  class OphPostgresContainer(dockerImageName: String) extends PostgreSQLContainer[OphPostgresContainer](dockerImageName) {}

  val DATABASE_NAME = "suorituspalvelu"

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(64))

  val LOG = LoggerFactory.getLogger(classOf[KantaOperaatiotTest])

  var postgres: PostgreSQLContainer[_] = new PostgreSQLContainer("postgres:15")
    postgres.withDatabaseName(DATABASE_NAME)
    postgres.withUsername("app")
    postgres.withPassword("app")
    postgres.withLogConsumer(frame => LOG.info(frame.getUtf8StringWithoutLineEnding))

  private def getDatasource() =
    val ds: PGSimpleDataSource = new PGSimpleDataSource()
    ds.setServerNames(Array("localhost"))
    ds.setDatabaseName(DATABASE_NAME)
    ds.setPortNumbers(Array(postgres.getMappedPort(5432)))
    ds.setUser("app")
    ds.setPassword("app")
    ds

  private def getHikariDatasource() =
    val config = new HikariConfig()
    config.setMaximumPoolSize(64)
    config.setDataSource(getDatasource())
    new HikariDataSource(config)

  val rand = Random
  var kantaOperaatiot: KantaOperaatiot = null
  var database: Database = null

  @BeforeAll def setup(): Unit =
    postgres.start()
    database = Database.forDataSource(getHikariDatasource(), None)
    kantaOperaatiot = KantaOperaatiot(database)

  @AfterAll def teardown(): Unit =
    postgres.stop()

  @BeforeEach def setupTest(): Unit =
    val flyway = Flyway.configure()
      .dataSource(getDatasource())
      .outOfOrder(true)
      .load()
    try
      val result = flyway.migrate()
      LOG.info(result.migrationsExecuted + "")
    catch
      case e: Throwable =>
        LOG.error(e.getMessage)

  @AfterEach def teardownTest(): Unit =
    Await.result(database.run(
      sqlu"""
            DROP TABLE spring_session_attributes;
            DROP TABLE spring_session;
            DROP TABLE opiskeluoikeudet;
            DROP TABLE suoritukset;
            DROP TABLE versiot;
            DROP TABLE flyway_schema_history;
            DROP TYPE lahde;
          """), 5.seconds)

  @Test def testVersioRoundtrip(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    val versio = this.kantaOperaatiot.tallennaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value\"}", None)

    // suoritus palautuu kun haetaan oppijanumerolla
    //Assertions.assertTrue(this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO).filter(s => s.tunniste.equals(suoritus.tunniste)).nonEmpty)

  @Test def testSuoritusRoundtrip(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallennetaan versio
    val versio = this.kantaOperaatiot.tallennaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value\"}", None)

    // tallennetaan suoritus
    val tallennetutSuoritusEntiteetit = Map(versio -> Seq(this.kantaOperaatiot.tallennaSuoritukset(versio, Suoritus("peruskoulu", Seq(Suoritus("äidinkieli", Seq.empty))))))
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)

    // suoritus palautuu kun haetaan oppijanumerolla
    Assertions.assertEquals(tallennetutSuoritusEntiteetit, haetutSuoritusEntiteetit)
}
