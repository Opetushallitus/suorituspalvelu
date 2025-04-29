package fi.oph.suorituspalvelu.business

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import fi.oph.suorituspalvelu.business.Tietolahde.KOSKI
import fi.oph.suorituspalvelu.parsing.koski.KoskiParser
import fi.oph.suorituspalvelu.parsing.koski.KoskiToSuoritusConverter
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.{AfterAll, AfterEach, Assertions, BeforeAll, BeforeEach, Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.testcontainers.containers.PostgreSQLContainer
import slick.jdbc.JdbcBackend.Database
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory
import slick.jdbc.PostgresProfile.api.*

import java.time.Instant
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
            DROP TABLE perusopetuksen_vuosiluokat;
            DROP TABLE nuorten_perusopetuksen_oppiaineen_oppimaarat;
            DROP TABLE perusopetuksen_oppiaineet;
            DROP TABLE perusopetuksen_oppimaarat;
            DROP TABLE ammatillisen_tutkinnon_osaalueet;
            DROP TABLE ammatillisen_tutkinnon_osat;
            DROP TABLE ammatilliset_tutkinnot;
            DROP TABLE tuvat;
            DROP TABLE telmat;
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
    val suoritukset = PerusopetuksenOppimaara(None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))
    this.kantaOperaatiot.tallennaSuoritukset(versio, Set(suoritukset))

    // suoritus palautuu kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)
    Assertions.assertEquals(Map(versio -> Set(suoritukset)), haetutSuoritusEntiteetit)

  @Test def testVanhatSuorituksetPoistetaan(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallennetaan versio
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value\"}").get

    // tallennetaan suoritukset kerran ja sitten toisen kerran
    this.kantaOperaatiot.tallennaSuoritukset(versio, Set(PerusopetuksenOppimaara(None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))))
    val uudetSuoritukset = PerusopetuksenOppimaara(None, Set(PerusopetuksenOppiaine("englanti", "koodi", "10")))
    this.kantaOperaatiot.tallennaSuoritukset(versio, Set(uudetSuoritukset))

    // uudet suoritukset palautuvat kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)
    Assertions.assertEquals(Map(versio -> Set(uudetSuoritukset)), haetutSuoritusEntiteetit)

  @Test def testExampleDataSuorituksetRoundtrip(): Unit =
    Seq("/1_2_246_562_24_40483869857.json", "/1_2_246_562_24_30563266636.json").foreach(fileName => {
      val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName))
      val suoritukset = splitData.map((oppijaOid, data) => {
        val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(oppijaOid, KOSKI, "{\"attr\": \"value\"}").get

        val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
        val suoritukset = KoskiToSuoritusConverter.toSuoritus(koskiOpiskeluoikeudet).toSet
        this.kantaOperaatiot.tallennaSuoritukset(versio, suoritukset)

        val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(oppijaOid)

        Assertions.assertEquals(Map(versio -> suoritukset), haetutSuoritukset);
      }).toSeq
    })

  @Test def testUseVersion(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallenetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value1\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value2\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value3\"}").get

    // tallennetaan versio ja suoritukset
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value4\"}").get
    val suoritukset = PerusopetuksenOppimaara(None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))
    this.kantaOperaatiot.tallennaSuoritukset(versio, Set(suoritukset))

    // tallenetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value5\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value6\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value7\"}").get

    // versio jotka suoritukset purettu palautuu suorituksineen kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)
    Assertions.assertEquals(Map(versio -> Set(suoritukset)), haetutSuoritusEntiteetit)

  @Test def testExampleDataSuorituksetRoundtripPerformance(): Unit =
    val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream("/1_2_246_562_24_40483869857.json"))
    val suoritukset = splitData.map((oppijaOid, data) => (oppijaOid, KoskiToSuoritusConverter.toSuoritus(KoskiParser.parseKoskiData(data)).toSet)).toSeq.head
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(suoritukset._1, KOSKI, "{\"attr\": \"value\"}").get
    this.kantaOperaatiot.tallennaSuoritukset(versio, suoritukset._2)

    val start = Instant.now()

    (1 to 100).foreach(i => {
      val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(suoritukset._1)
    })

    val duration = Instant.now().toEpochMilli - start.toEpochMilli
    LOG.info("Duration: " + duration + "ms")

}
