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
import scala.concurrent.{Await, ExecutionContext, Future}
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

  /**
   * Testataan että versio tallentuu ja luetaan oikein.
   */
  @Test def testVersioRoundtrip(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    val data = "{\"attr\": \"value\"}"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, data).get

    // data palautuu
    Assertions.assertEquals(data, this.kantaOperaatiot.haeData(versio)._2)

  /**
   * Testataan että json-datan muuttuessa oppijalle tallennetaan uusi versio.
   */
  @Test def testUusiVersioLuodaanKunJsonDataMuuttuu(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    Assertions.assertTrue(this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value1\"}").isDefined)
    Assertions.assertTrue(this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value2\"}").isDefined)

  /**
   * Testataan että jos oppijalle tallennetaan uudestaan viimeisin json-data niin uutta versiota ei luoda.
   */
  @Test def testUuttaVersiotaEiLuodaKunJsonDataEiMuutu(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    val originalData = "{\"attr\": \"value\", \"arr\": [1, 2]}"
    val originalVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, originalData)
    Assertions.assertTrue(originalVersio.isDefined)

    // yritetään tallentaa uusi versio samalla datalla
    val duplicateData = "{\"arr\": [2, 1], \"attr\": \"value\"}"
    val duplicateVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, duplicateData)
    Assertions.assertTrue(duplicateVersio.isEmpty)

  /**
   * Testataan että kun samalla oppijalla tallennetaan versioita rinnakkain syntyy katkeamaton voimassaolohistoria
   */
  @Test def testVoimassaoloPerakkain(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan rinnakkain suuri joukko versioita
    val tallennusOperaatiot = Range(0, 500).map(i => () => {
      Thread.sleep((Math.random()*50).asInstanceOf[Int])
      this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value" + i + "\"}")
    })

    val versiot = Await.result(Future.sequence(tallennusOperaatiot.map(op => Future {op ()})), 20.seconds)
      .map(o => Some(this.kantaOperaatiot.haeData(o.get)._1))
      .sortBy(v => v.get.alku)

    // testataan että versioista muodostuu katkeamaton jatkumo ja viimeisin versio voimassa
    (Seq(None) ++ versiot).zip(versiot ++ Seq(None)).foreach(pair => {
      val (earlier, later) = pair
      (earlier, later) match
        case (Some(earlier), Some(later)) => Assertions.assertEquals(earlier.loppu.get, later.alku)
        case (Some(earlier), None) => Assertions.assertTrue(earlier.loppu.isEmpty)
        case (None, Some(later)) => // ensimmäinen versio
        case (None, None) => Assertions.fail()
    })

  /**
   * Testataan että minimaalinen suoritus tallentuu ja luetaan oikein.
   */
  @Test def testSuoritusRoundtrip(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallennetaan versio ja suoritukset
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value\"}").get
    val suoritukset = PerusopetuksenOppimaara(None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))
    this.kantaOperaatiot.tallennaSuoritukset(versio, Set(suoritukset))

    // suoritus palautuu kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)
    Assertions.assertEquals(Map(versio -> Set(suoritukset)), haetutSuoritusEntiteetit)

  /**
   * Testataan että vanhat suoritukset poistetaan kun uudet suoritukset tallennetaan, ts. ei synny suoritusten
   * unionia.
   */
  @Test def testVanhatSuorituksetPoistetaanUusienTieltä(): Unit =
    val OPPIJANUMERO1 = "2.3.4"
    val OPPIJANUMERO2 = "3.4.5"

    // tallennetaan versio ja suoritukset oppijalle 1
    val versio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, KOSKI, "{\"attr\": \"value\"}").get
    val suoritukset1 = PerusopetuksenOppimaara(None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))
    this.kantaOperaatiot.tallennaSuoritukset(versio1, Set(suoritukset1))

    // tallennetaan versio oppijalle 2
    val versio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO2, KOSKI, "{\"attr\": \"value\"}").get

    // tallennetaan suoritukset kerran ja sitten toisen kerran oppijalle 2
    this.kantaOperaatiot.tallennaSuoritukset(versio2, Set(PerusopetuksenOppimaara(None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))))
    val uudetSuoritukset2 = PerusopetuksenOppimaara(None, Set(PerusopetuksenOppiaine("englanti", "koodi", "10")))
    this.kantaOperaatiot.tallennaSuoritukset(versio2, Set(uudetSuoritukset2))

    // oppijan 2 uudet suoritukset palautuvat kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit2 = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO2)
    Assertions.assertEquals(Map(versio2 -> Set(uudetSuoritukset2)), haetutSuoritusEntiteetit2)

    // oppijan 1 suoritukset ennallaan
    val haetutSuoritusEntiteetit1 = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio1 -> Set(suoritukset1)), haetutSuoritusEntiteetit1)


  /**
   * Testataan että suoritukset tallentuvat ja luetaan oikein oikealla KOSKI-datalla. Tämän testin tulisi kattaa kaikki
   * erityyppiset KOSKI-suoritukset.
   */
  @Test def testAitoKoskiDataSuorituksetRoundtrip(): Unit =
    Seq(
      "/1_2_246_562_24_40483869857.json",
      "/1_2_246_562_24_30563266636.json"
    ).foreach(fileName => {
      val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName))
      val suoritukset = splitData.foreach((oppijaOid, data) => {
        val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(oppijaOid, KOSKI, "{\"attr\": \"value\"}").get

        val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
        val suoritukset = KoskiToSuoritusConverter.toSuoritus(koskiOpiskeluoikeudet).toSet
        this.kantaOperaatiot.tallennaSuoritukset(versio, suoritukset)

        val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(oppijaOid)

        Assertions.assertEquals(Map(versio -> suoritukset), haetutSuoritukset);
      })
    })

  /**
   * Testataan että suorituksia haettaessa palautetaan viimeisin versio jonka data on parseroitu onnistuneesti.
   */
  @Test def testPalautetaanViimeisinParseroituVersio(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallenetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value1\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value2\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value3\"}").get

    // tallennetaan versio ja suoritukset
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value4\"}").get
    val suoritukset = PerusopetuksenOppimaara(None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))
    this.kantaOperaatiot.tallennaSuoritukset(versio, Set(suoritukset))

    // tallennetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value5\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value6\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value7\"}").get

    // versio jotka suoritukset purettu palautuu suorituksineen kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)
    Assertions.assertEquals(Map(versio -> Set(suoritukset)), haetutSuoritusEntiteetit)

  /**
   * Testataan että suorituksia haettaessa ei palauteta mitään jos ei ole versioita joiden data on parseroitu
   * onnistuneesti.
   */
  @Test def testEiPalautetaVersioitaJosEiParseroituja(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallenetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value1\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value2\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value3\"}").get

    // koska ei ole parseroituja versioita ei palaudu mitään
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)
    Assertions.assertEquals(Map.empty, haetutSuoritusEntiteetit)

  /**
   * Testataan (hyvin karkealla tavalla) suoritusten tallennuksen ja haun suorituskykyä.
   */
  @Test def testExampleDataSuorituksetRoundtripPerformance(): Unit =
    val OPPIJANUMERO = "2.3.4."
    val iterations = 100

    val startSave = Instant.now()
    val data = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream("/1_2_246_562_24_40483869857.json")).iterator.next()._2
    (1 to iterations).foreach(i => {
      val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO + i, KOSKI, "{\"attr\": \"value\"}").get
      this.kantaOperaatiot.tallennaSuoritukset(versio, KoskiToSuoritusConverter.toSuoritus(KoskiParser.parseKoskiData(data)).toSet)
    })
    val saveDuration = Instant.now().toEpochMilli - startSave.toEpochMilli
    Assertions.assertTrue(saveDuration< 50 * iterations);

    val readStart = Instant.now()
    (1 to iterations).foreach(i => {
      val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO + i)
    })
    val readDuration = Instant.now().toEpochMilli - readStart.toEpochMilli
    Assertions.assertTrue(readDuration < 10 * iterations);
}
