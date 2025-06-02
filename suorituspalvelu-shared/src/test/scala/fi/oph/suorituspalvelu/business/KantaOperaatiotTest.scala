package fi.oph.suorituspalvelu.business

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import fi.oph.suorituspalvelu.business.Tietolahde.{KOSKI, YTR}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiErityisenTuenPaatos, KoskiLisatiedot, KoskiParser, KoskiToSuoritusConverter, Kotiopetusjakso, OpiskeluoikeusJakso, OpiskeluoikeusJaksoTila, OpiskeluoikeusTila}
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.{AfterAll, AfterEach, Assertions, BeforeAll, BeforeEach, Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.testcontainers.containers.PostgreSQLContainer
import slick.jdbc.JdbcBackend.Database
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory
import slick.jdbc.PostgresProfile.api.*

import java.time.{Instant, LocalDate}
import scala.concurrent.duration.DurationInt
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random
import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.business.parsing.koski.TestDataUtil

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
            DROP TABLE scheduled_tasks;
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
            DROP TABLE perusopetuksen_opiskeluoikeudet;
            DROP TABLE ammatilliset_opiskeluoikeudet;
            DROP TABLE geneeriset_opiskeluoikeudet;
            DROP TABLE yotutkinnot;
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

  @Test def testHaeUusimmatMuuttuneetVersiot(): Unit =
    // tallennetaan ja otetaan käyttöön versio ennen aikaleimaa
    val vanhaVersio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", KOSKI, "{\"attr\": \"value1\"}").get
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(vanhaVersio1, Set.empty, Set.empty)

    // tallennetaan aikaleima ja todetaan ettei ole versioita ennen sitä
    val alkaen = Instant.now
    Assertions.assertEquals(Seq.empty, this.kantaOperaatiot.haeUusimmatMuuttuneetVersiot(alkaen))

    // tallennetaan ja otetaan käyttöön versio aikaleiman jälkeen
    val uusiVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", KOSKI, "{\"attr\": \"value2\"}").get
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(uusiVersio, Set.empty, Set.empty)

    // tallennetaan (muttei oteta käyttöön) vielä uudempi versio
    val eiKaytossaVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", KOSKI, "{\"attr\": \"value3\"}").get

    // palautuu aikaleiman jälkeen tallennettu ja käyttöönotettu versio
    Assertions.assertEquals(Seq(uusiVersio.tunniste), this.kantaOperaatiot.haeUusimmatMuuttuneetVersiot(alkaen).map(v => v.tunniste))

  /**
   * Testataan että minimaalinen suoritus tallentuu ja luetaan oikein.
   */
  @Test def testSuoritusRoundtrip(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallennetaan versio ja suoritukset
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value\"}").get
    val suoritukset = PerusopetuksenOppimaara("3.4.5", Koodi("arvo", "koodisto", Some(1)), None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus("4.5.6", "opiskeluoikeusoid1", Seq(suoritukset), None, None)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus), Set.empty)

    // suoritus palautuu kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)
    Assertions.assertEquals(Map(versio -> Set(opiskeluoikeus)), haetutSuoritusEntiteetit)

  /**
   * Testataan että vanhat suoritukset poistetaan kun uudet suoritukset tallennetaan, ts. ei synny suoritusten
   * unionia.
   */
  @Test def testVanhatSuorituksetPoistetaanUusienTieltä(): Unit =
    val OPPIJANUMERO1 = "1.2.246.562.24.99988877766"
    val OPPIJANUMERO2 = "1.2.246.562.24.88877766655"
    val OPPILAITOSOID1 = "1.2.246.562.10.00000000123"
    val OPPILAITOSOID2 = "1.2.246.562.10.00000000234"
    val OPISKELUOIKEUSOID1 = "1.2.246.562.15.12345678901"
    val OPISKELUOIKEUSOID2 = "1.2.246.562.15.09876543210"

    // tallennetaan versio ja suoritukset oppijalle 1
    val versio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, KOSKI, "{\"attr\": \"value\"}").get
    val suoritus1 = PerusopetuksenOppimaara(OPPILAITOSOID1, Koodi("arvo", "koodisto", Some(1)), None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))
    val opiskeluoikeus1 = PerusopetuksenOpiskeluoikeus(OPISKELUOIKEUSOID1, OPPILAITOSOID1, Seq(suoritus1), None, None)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio1, Set(opiskeluoikeus1), Set.empty)

    // tallennetaan versio oppijalle 2
    val versio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO2, KOSKI, "{\"attr\": \"value\"}").get

    // tallennetaan suoritukset kerran ja sitten toisen kerran oppijalle 2
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio2, Set(opiskeluoikeus1), Set.empty)
    val suoritus2 = PerusopetuksenOppimaara(OPPILAITOSOID2, Koodi("arvo", "koodisto", Some(1)), None, Set(PerusopetuksenOppiaine("englanti", "koodi", "10")))
    val opiskeluoikeus2 = PerusopetuksenOpiskeluoikeus(OPISKELUOIKEUSOID2, OPPILAITOSOID2, Seq(suoritus2), None, None)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio2, Set(opiskeluoikeus2), Set.empty)

    // oppijan 2 uudet suoritukset palautuvat kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit2 = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO2)
    Assertions.assertEquals(Map(versio2 -> Set(opiskeluoikeus2)), haetutSuoritusEntiteetit2)

    // oppijan 1 suoritukset ennallaan
    val haetutSuoritusEntiteetit1 = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio1 -> Set(opiskeluoikeus1)), haetutSuoritusEntiteetit1)


  @Test def testAitoKoskiDataSuorituksetOsajoukkoRoundtrip(): Unit = {
    Seq(
      "/1_2_246_562_24_40483869857b.json"
    ).foreach(fileName => {
      val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName))
      val suoritukset = splitData.foreach((oppijaOid, data) => {
        val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(oppijaOid, KOSKI, "{\"attr\": \"value\"}").get

        val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
        val oo: Set[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet).toSet
        this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, oo, Set.empty)

        val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(oppijaOid)

        Assertions.assertEquals(oo, haetutSuoritukset.head._2);
      })
    })
  }

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
        val oo: Set[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet).toSet
        this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, oo, Set.empty)

        val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(oppijaOid)

        Assertions.assertEquals(Map(versio -> oo), haetutSuoritukset);
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
    val suoritus = PerusopetuksenOppimaara("3.4.5", Koodi("arvo", "koodisto",  Some(1)), None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))
    val lisatiedot = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain = Some(true)))), None, None)
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus("opiskeluoikeusOid", "oppilaitosOid", Seq(suoritus), Some(lisatiedot), None)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus), Set.empty)

    // tallennetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value5\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value6\"}").get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, KOSKI, "{\"attr\": \"value7\"}").get

    // versio jotka suoritukset purettu palautuu suorituksineen kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO)
    Assertions.assertEquals(Map(versio -> Set(opiskeluoikeus)), haetutSuoritusEntiteetit)

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
      val oo = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(data))
      this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, oo.toSet, Set.empty)
    })
    val saveDuration = Instant.now().toEpochMilli - startSave.toEpochMilli
    Assertions.assertTrue(saveDuration< 50 * iterations);

    val readStart = Instant.now()
    (1 to iterations).foreach(i => {
      val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO + i)
    })
    val readDuration = Instant.now().toEpochMilli - readStart.toEpochMilli
    Assertions.assertTrue(readDuration < 10 * iterations);

  @Test def testPerusopetuksenOpiskeluoikeusEqualityAfterPersisting(): Unit =
    val OPPIJANUMERO1 = "1.2.246.562.24.99988877766"
    val OPPIJANUMERO2 = "1.2.246.562.24.88877766655"
    val OPPILAITOSOID1 = "1.2.246.562.10.00000000123"
    val OPPILAITOSOID2 = "1.2.246.562.10.00000000234"
    val OPISKELUOIKEUSOID1 = "1.2.246.562.15.12345678901"
    val OPISKELUOIKEUSOID2 = "1.2.246.562.15.09876543210"

    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, KOSKI, "{\"attr\": \"value\"}").get

    val suoritus1 = PerusopetuksenOppimaara(OPPILAITOSOID1, Koodi("arvo", "koodisto", Some(1)), None, Set(PerusopetuksenOppiaine("äidinkieli", "koodi", "10")))
    val luokkasuoritus1 = PerusopetuksenVuosiluokka("vuosiluokka", "koodi", Some(LocalDate.parse("2024-08-01")))
    val lisatiedot = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(Some(true)))), Some(false), Some(List(Kotiopetusjakso("2023-08-24", Some("2024-01-22")))))
    val tilat = OpiskeluoikeusTila(List(OpiskeluoikeusJakso(LocalDate.parse("2024-06-03"), OpiskeluoikeusJaksoTila("opiskelu", "tilakoodisto", Some(6))), OpiskeluoikeusJakso(LocalDate.parse("2024-11-09"), OpiskeluoikeusJaksoTila("joulunvietto", "tilakoodisto", Some(6)))))
    val opiskeluoikeus1 = PerusopetuksenOpiskeluoikeus(OPISKELUOIKEUSOID1, OPPILAITOSOID1, Seq(suoritus1, luokkasuoritus1), Some(lisatiedot), Some(tilat))

    val suoritus2 = PerusopetuksenOppimaara(OPPILAITOSOID2, Koodi("toinenarvo", "koodisto", Some(1)), None, Set(PerusopetuksenOppiaine("englanti", "koodi", "10")))
    val luokkasuoritus2 = PerusopetuksenVuosiluokka("vuosiluokka2", "koodi2", Some(LocalDate.parse("2023-08-01")))
    val lisatiedot2 = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(Some(false)))), Some(true), None)
    val tilat2 = OpiskeluoikeusTila(List(OpiskeluoikeusJakso(LocalDate.parse("2022-07-02"), OpiskeluoikeusJaksoTila("hengailu", "tilakoodisto", Some(6))), OpiskeluoikeusJakso(LocalDate.parse("2022-10-09"), OpiskeluoikeusJaksoTila("juhannusvalmistelut", "tilakoodisto", Some(6)))))
    val opiskeluoikeus2 = PerusopetuksenOpiskeluoikeus(OPISKELUOIKEUSOID2, OPPILAITOSOID2, Seq(suoritus2, luokkasuoritus2), Some(lisatiedot2), Some(tilat2))

    // tallennetaan molemmat versioon liittyvät opiskeluoikeudet
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus1, opiskeluoikeus2), Set.empty)

    // oppijan 1 suoritukset ennallaan
    val haetutSuoritusEntiteetit1 = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio -> Set(opiskeluoikeus1, opiskeluoikeus2)), haetutSuoritusEntiteetit1)

  @Test def testAmmatillinenOpiskeluoikeusEqualityAfterPersisting(): Unit =
    val OPPIJANUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, KOSKI, "{\"attr\": \"value\"}").get
    val suoritus = TestDataUtil.getTestAmmatillinenTutkinto()
    val tilat = OpiskeluoikeusTila(List(OpiskeluoikeusJakso(LocalDate.parse("2024-06-03"), OpiskeluoikeusJaksoTila("opiskelu", "tilakoodisto", Some(6))), OpiskeluoikeusJakso(LocalDate.parse("2024-11-09"), OpiskeluoikeusJaksoTila("joulunvietto", "tilakoodisto", Some(6)))))
    val opiskeluoikeus = AmmatillinenOpiskeluoikeus("opiskeluoikeusOid", "oppilaitosOid", Seq(suoritus), Some(tilat))
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus), Set.empty)

    val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio -> Set(opiskeluoikeus)), haetutSuoritukset)

  @Test def testGeneerinenOpiskeluoikeusEqualityAfterPersisting(): Unit =
    val OPPIJANUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, KOSKI, "{\"attr\": \"value\"}").get
    val suoritus = Tuva("koodi", Some(LocalDate.parse("2025-03-20")))
    val tilat = OpiskeluoikeusTila(List(OpiskeluoikeusJakso(LocalDate.parse("2023-05-03"), OpiskeluoikeusJaksoTila("opiskelu", "tilakoodisto", Some(2))), OpiskeluoikeusJakso(LocalDate.parse("2025-10-09"), OpiskeluoikeusJaksoTila("lasna", "tilakoodisto", Some(6)))))
    val opiskeluoikeus = GeneerinenOpiskeluoikeus("opiskeluoikeusOid", "oppilaitosOid", "testityyppi", Seq(suoritus), Some(tilat))
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus), Set.empty)

    val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio -> Set(opiskeluoikeus)), haetutSuoritukset)

  @Test def testYTRRoundTrip(): Unit =
    val OPPIJANUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, YTR, "{}").get
    val opiskeluoikeus = YOOpiskeluoikeus(YOTutkinto())
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus), Set.empty)

    val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio -> Set(opiskeluoikeus)), haetutSuoritukset)

}
