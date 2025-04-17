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
            DROP TABLE oppijat;
            DROP TYPE lahde;
          """), 5.seconds)

  @Test def testVersioRoundtrip(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    val data = "{\"attr\": \"value\"}"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, data).get

    // data palautuu
    Assertions.assertEquals(data, this.kantaOperaatiot.haeData(versio))

  @Test def testNewVersionCreated(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    Assertions.assertTrue(this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value1\"}").isDefined)
    Assertions.assertTrue(this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value2\"}").isDefined)


  @Test def testNoDuplicateVersionsCreatedForKoski(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    val originalData = "{\"attr\": \"value\", \"arr\": [1, 2]}"
    val originalVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, originalData)
    Assertions.assertTrue(originalVersio.isDefined)

    // yritetään tallentaa uusi versio samalla datalla
    val duplicateData = "{\"arr\": [2, 1], \"attr\": \"value\"}"
    val duplicateVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, duplicateData)
    Assertions.assertTrue(duplicateVersio.isEmpty)

  @Test def testSuoritusRoundtrip(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallennetaan versio ja suoritukset
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value\"}").get
    val tallennetutSuoritusEntiteetit = Map(versio -> Seq(this.kantaOperaatiot.tallennaSuoritukset(versio, GenericSuoritus("peruskoulu", Seq(GenericSuoritus("äidinkieli", Seq.empty))))))

    // suoritus palautuu kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)
    Assertions.assertEquals(tallennetutSuoritusEntiteetit, haetutSuoritusEntiteetit)

  @Test def testVanhatSuorituksetPoistetaan(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallennetaan versio
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value\"}").get

    // tallennetaan suoritukset kerran ja sitten toisen kerran
    val vanhentuvatSuoritusEntiteetit = Map(versio -> Seq(this.kantaOperaatiot.tallennaSuoritukset(versio, GenericSuoritus("peruskoulu", Seq(GenericSuoritus("äidinkieli", Seq.empty))))))
    val tallennetutSuoritusEntiteetit = Map(versio -> Seq(this.kantaOperaatiot.tallennaSuoritukset(versio, GenericSuoritus("ammattikoulu", Seq(GenericSuoritus("englanti", Seq.empty))))))

    // uudet suoritukset palautuvat kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)
    Assertions.assertEquals(tallennetutSuoritusEntiteetit, haetutSuoritusEntiteetit)
}
