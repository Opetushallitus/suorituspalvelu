package fi.oph.suorituspalvelu.business

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiLisatiedot, KoskiParser, KoskiToSuoritusConverter, Kotiopetusjakso, OpiskeluoikeusJakso, OpiskeluoikeusTila}
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
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.parsing.koski.TestDataUtil
import fi.oph.suorituspalvelu.integration.KoskiIntegration

import java.util.UUID

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
            --DROP FUNCTION get_tyyppi(text, text);
            DROP TABLE cas_client_session;
            DROP TABLE spring_session_attributes;
            DROP TABLE spring_session;
            DROP TABLE task_status;
            DROP TABLE scheduled_tasks;
            DROP TABLE opiskeluoikeudet;
            DROP TABLE metadata_arvot;
            DROP TABLE versiot;
            DROP TABLE flyway_schema_history;
            DROP TABLE oppijat;
            DROP TABLE yliajot;
          """), 5.seconds)

  /**
   * Testataan että versio tallentuu ja luetaan oikein.
   */
  @Test def testVersioRoundtrip(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    val data = "{\"attr\": \"value\"}"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq(data), Instant.now()).get

    // data palautuu
    Assertions.assertEquals(Seq(data), this.kantaOperaatiot.haeData(versio)._2)

  /**
   * Testataan että json-datan muuttuessa oppijalle tallennetaan uusi versio.
   */
  @Test def testUusiVersioLuodaanKunJsonDataMuuttuu(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    Assertions.assertTrue(this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Instant.now()).isDefined)
    Assertions.assertTrue(this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value2\"}"), Instant.now()).isDefined)

  /**
   * Testataan että jos oppijalle tallennetaan uudestaan viimeisin json-data niin uutta versiota ei luoda.
   */
  @Test def testUuttaVersiotaEiLuodaKunJsonDataEiMuutu(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    val originalData = "{\"attr\": \"value\", \"arr\": [1, 2]}"
    val originalVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq(originalData), Instant.now())
    Assertions.assertTrue(originalVersio.isDefined)

    // yritetään tallentaa uusi versio samalla datalla
    val duplicateData = "{\"arr\": [2, 1], \"attr\": \"value\"}"
    val duplicateVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq(duplicateData), Instant.now())
    Assertions.assertTrue(duplicateVersio.isEmpty)

  /**
   * Testataan että jos oppijalle tallennetaan uudestaan viimeisin json-data niin uutta versiota ei luoda.
   */
  @Test def testUuttaVersiotaEiLuodaKunUudempiVersioTallennettu(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan versio
    val originalData = "{\"attr\": \"value\", \"arr\": [1, 2]}"
    val originalVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq(originalData), Instant.now())
    Assertions.assertTrue(originalVersio.isDefined)

    // yritetään tallentaa uusi versio samalla datalla
    val staleData = "{\"arr\": [2, 1], \"attr\": \"value\"}"
    val staleVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq(staleData), Instant.now().minusSeconds(10))
    Assertions.assertTrue(staleVersio.isEmpty)

  /**
   * Testataan että kun samalla oppijalla tallennetaan versioita rinnakkain syntyy katkeamaton voimassaolohistoria
   */
  @Test def testVoimassaoloPerakkain(): Unit =
    val OPPIJANUMERO = "1.2.3"

    // tallennetaan rinnakkain suuri joukko versioita
    val tallennusOperaatiot = Range(0, 500).map(i => () => {
      Thread.sleep((Math.random()*50).asInstanceOf[Int])
      this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value" + i + "\"}"), Instant.now())
    })

    val versiot = Await.result(Future.sequence(tallennusOperaatiot.map(op => Future {op ()})), 20.seconds)
      .filter(o => o.isDefined) // versiota ei välttämättä tallenneta jos uudempi jos tallennettu
      .map(o => Some(this.kantaOperaatiot.haeData(o.get)._1))
      .sortBy(v => v.get.alku)

    // ainakin 1/5 yrityksistä pitäisi onnistua (eli uusin versio), muuten jotain pahasti pielessä
    Assertions.assertTrue(versiot.size>=100)

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
    val vanhaVersio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Instant.now()).get
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(vanhaVersio1, Set.empty)

    // tallennetaan aikaleima ja todetaan ettei ole versioita ennen sitä
    val alkaen = Instant.now
    Assertions.assertEquals(Seq.empty, this.kantaOperaatiot.haeUusimmatMuuttuneetVersiot(alkaen))

    // tallennetaan ja otetaan käyttöön versio aikaleiman jälkeen
    val uusiVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value2\"}"), Instant.now()).get
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(uusiVersio, Set.empty)

    // tallennetaan (muttei oteta käyttöön) vielä uudempi versio
    val eiKaytossaVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value3\"}"), Instant.now()).get

    // palautuu aikaleiman jälkeen tallennettu ja käyttöönotettu versio
    Assertions.assertEquals(Seq(uusiVersio.tunniste), this.kantaOperaatiot.haeUusimmatMuuttuneetVersiot(alkaen).map(v => v.tunniste))

  /**
   * Testataan että minimaalinen suoritus tallentuu ja luetaan oikein.
   */
  @Test def testSuoritusRoundtrip(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallennetaan versio ja suoritukset
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq.empty, Instant.now()).get
    val suoritukset = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "3.4.5"), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("FI", "kielivalikoima", None)), true, None, None)))
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("4.5.6"), "opiskeluoikeusoid1", Set(suoritukset), None, VALMIS)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus))

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
    val versio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, SuoritusJoukko.KOSKI, Seq.empty, Instant.now()).get
    val suoritus1 = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID1), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("FI", "kielivalikoima", None)), true, None, None)))
    val opiskeluoikeus1 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(OPISKELUOIKEUSOID1), OPPILAITOSOID1, Set(suoritus1), None, VALMIS)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio1, Set(opiskeluoikeus1))

    // tallennetaan versio oppijalle 2
    val versio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO2, SuoritusJoukko.KOSKI, Seq.empty, Instant.now()).get

    // tallennetaan suoritukset kerran ja sitten toisen kerran oppijalle 2
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio2, Set(opiskeluoikeus1))
    val suoritus2 = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID2), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None)))
    val opiskeluoikeus2 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(OPISKELUOIKEUSOID2), OPPILAITOSOID2, Set(suoritus2), None, VALMIS)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio2, Set(opiskeluoikeus2))

    // oppijan 2 uudet suoritukset palautuvat kun haetaan oppijanumerolla
    val haetutSuoritusEntiteetit2 = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO2)
    Assertions.assertEquals(Map(versio2 -> Set(opiskeluoikeus2)), haetutSuoritusEntiteetit2)

    // oppijan 1 suoritukset ennallaan
    val haetutSuoritusEntiteetit1 = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio1 -> Set(opiskeluoikeus1)), haetutSuoritusEntiteetit1)


  /**
   * Testataan että oppijan osittainen KOSKI-suoritussetti säilyy muuttumattomana kun se tallennetaan ja luetaan
   */
  @Test def testAitoKoskiDataSuorituksetOsajoukkoRoundtrip(): Unit = {
    Seq(
      "/1_2_246_562_24_40483869857b.json"
    ).foreach(fileName => {
      val splitData = KoskiIntegration.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName))
      val suoritukset = splitData.foreach((oppijaOid, data) => {
        val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(oppijaOid, SuoritusJoukko.KOSKI, Seq.empty, Instant.now()).get

        val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
        val oo: Set[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet, koodisto => Map.empty).toSet
        this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, oo)

        val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(oppijaOid)

        Assertions.assertEquals(oo, haetutSuoritukset.head._2);
      })
    })
  }

  /**
   * Testataan että suoritukset tallentuvat ja luetaan oikein oikealla KOSKI-datalla. Tämän testin tulisi kattaa kaikki
   * erityyppiset KOSKI-suoritukset.
   */
  @Test def testAitoKoskiDataSuorituksetRoundtrip(): Unit = {
    Seq(
      "/1_2_246_562_24_40483869857.json",
      "/1_2_246_562_24_30563266636.json",
      "/1_2_246_562_15_94501385358.json"
    ).foreach(fileName => {
      val splitData = KoskiIntegration.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName))
      val suoritukset = splitData.foreach((oppijaOid, data) => {

        val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(oppijaOid, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Instant.now()).get

        val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
        val oo: Set[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet, koodisto => Map.empty).toSet
        this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, oo)

        val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(oppijaOid)

        Assertions.assertEquals(Map(versio -> oo), haetutSuoritukset);
      })
    })
  }

  /**
   * Testataan että suorituksia haettaessa palautetaan viimeisin versio jonka data on parseroitu onnistuneesti.
   */
  @Test def testPalautetaanViimeisinParseroituVersio(): Unit =
    val OPPIJANUMERO = "2.3.4"

    // tallenetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value2\"}"), Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value3\"}"), Instant.now()).get

    // tallennetaan versio ja suoritukset
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value4\"}"), Instant.now()).get
    val suoritus = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "3.4.5"), None, Koodi("arvo", "koodisto",  Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("FI", "kielivalikoima", None)), true, None, None)))
    val lisatiedot = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain = Some(true)))), None, None)
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("opiskeluoikeusOid"), "oppilaitosOid", Set(suoritus), Some(lisatiedot), VALMIS)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus))

    // tallennetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value5\"}"), Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value6\"}"), Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value7\"}"), Instant.now()).get

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
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value2\"}"), Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value3\"}"), Instant.now()).get

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
    val data = KoskiIntegration.splitKoskiDataByOppija(this.getClass.getResourceAsStream("/1_2_246_562_24_40483869857.json")).iterator.next()._2
    (1 to iterations).foreach(i => {
      val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO + i, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Instant.now()).get
      val oo = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(data), koodisto => Map.empty)
      this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, oo.toSet)
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

    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Instant.now()).get

    val suoritus1 = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID1), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("FI", "kielivalikoima", None)), true, None, None)))
    val luokkasuoritus1 = PerusopetuksenVuosiluokka(UUID.randomUUID(), Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID1), Kielistetty(Some("vuosiluokka"), None, None), Koodi("arvo1", "koodisto", Some(1)), Some(LocalDate.parse("2024-08-01")), Some(LocalDate.parse("2025-08-01")), false)
    val lisatiedot = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(Some(true)))), Some(false), Some(List(Kotiopetusjakso("2023-08-24", Some("2024-01-22")))))
    val tilat = OpiskeluoikeusTila(List(OpiskeluoikeusJakso(LocalDate.parse("2024-06-03"), KoskiKoodi("opiskelu", "tilakoodisto", Some(6), Kielistetty(None, None, None), None)), OpiskeluoikeusJakso(LocalDate.parse("2024-11-09"), KoskiKoodi("joulunvietto", "tilakoodisto", Some(6), Kielistetty(None, None, None), None))))
    val opiskeluoikeus1 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(OPISKELUOIKEUSOID1), OPPILAITOSOID1, Set(suoritus1, luokkasuoritus1), Some(lisatiedot), KESKEN)

    val suoritus2 = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID2), None, Koodi("toinenarvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None)))
    val luokkasuoritus2 = PerusopetuksenVuosiluokka(UUID.randomUUID(), Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID2), Kielistetty(Some("vuosiluokka2"), None, None), Koodi("arvo2", "koodisto", Some(1)), Some(LocalDate.parse("2023-08-01")), Some(LocalDate.parse("2024-08-01")), false)
    val lisatiedot2 = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(Some(false)))), Some(true), None)
    val tilat2 = OpiskeluoikeusTila(List(OpiskeluoikeusJakso(LocalDate.parse("2022-07-02"), KoskiKoodi("hengailu", "tilakoodisto", Some(6), Kielistetty(None, None, None), None)), OpiskeluoikeusJakso(LocalDate.parse("2022-10-09"), KoskiKoodi("juhannusvalmistelut", "tilakoodisto", Some(6), Kielistetty(None, None, None), None))))
    val opiskeluoikeus2 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(OPISKELUOIKEUSOID2), OPPILAITOSOID2, Set(suoritus2, luokkasuoritus2), Some(lisatiedot2), KESKEN)

    // tallennetaan molemmat versioon liittyvät opiskeluoikeudet
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus1, opiskeluoikeus2))

    // oppijan 1 suoritukset ennallaan
    val haetutSuoritusEntiteetit1 = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio -> Set(opiskeluoikeus1, opiskeluoikeus2)), haetutSuoritusEntiteetit1)

  @Test def testAmmatillinenOpiskeluoikeusEqualityAfterPersisting(): Unit =
    val OPPIJANUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Instant.now()).get
    val oppilaitos = Oppilaitos(Kielistetty(Some("Nimi suomi"), Some("Nimi Ruotsi"), Some("Nimi englanti")), "1.2.246.562.10.95136889433")
    val suoritus = TestDataUtil.getTestAmmatillinenTutkinto(oppilaitos = oppilaitos)
    val tilat = OpiskeluoikeusTila(List(OpiskeluoikeusJakso(LocalDate.parse("2024-06-03"), KoskiKoodi("opiskelu", "tilakoodisto", Some(6), Kielistetty(None, None, None), None)), OpiskeluoikeusJakso(LocalDate.parse("2024-11-09"), KoskiKoodi("joulunvietto", "tilakoodisto", Some(6), Kielistetty(None, None, None), None))))
    val opiskeluoikeus = AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "opiskeluoikeusOid", oppilaitos, Set(suoritus), Some(tilat))
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus))

    val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio -> Set(opiskeluoikeus)), haetutSuoritukset)

  @Test def testGeneerinenOpiskeluoikeusEqualityAfterPersisting(): Unit =
    val OPPIJANUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Instant.now()).get
    val suoritus = Tuva(UUID.randomUUID(), Kielistetty(Some("Nimi Suomi"), None, None), Koodi("arvo", "koodisto", None), Oppilaitos(Kielistetty(Some("Nimi suomi"), None, None), "1.2.246.562.10.95136889433"), Koodi("lasna", "koskiopiskeluoikeudentila", Some(1)), SuoritusTila.KESKEN, Some(LocalDate.parse("2025-03-20")), Some(LocalDate.parse("2025-03-20")), None)
    val tilat = OpiskeluoikeusTila(List(OpiskeluoikeusJakso(LocalDate.parse("2023-05-03"), KoskiKoodi("opiskelu", "tilakoodisto", Some(2), Kielistetty(None, None, None), None)), OpiskeluoikeusJakso(LocalDate.parse("2025-10-09"), KoskiKoodi("lasna", "tilakoodisto", Some(6), Kielistetty(None, None, None), None))))
    val opiskeluoikeus = GeneerinenOpiskeluoikeus(UUID.randomUUID(), "opiskeluoikeusOid", Koodi("arvo", "koodisto", None), "oppilaitosOid", Set(suoritus), Some(tilat))
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus))

    val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio -> Set(opiskeluoikeus)), haetutSuoritukset)

  @Test def testYTRRoundTrip(): Unit =
    val OPPIJANUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, SuoritusJoukko.YTR, Seq("{}"), Instant.now()).get
    val opiskeluoikeus = YOOpiskeluoikeus(UUID.randomUUID(), YOTutkinto(UUID.randomUUID(), Koodi("fi", "kieli", Some(1)), SuoritusTila.KESKEN, None, Set.empty))
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus))

    val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(OPPIJANUMERO1)
    Assertions.assertEquals(Map(versio -> Set(opiskeluoikeus)), haetutSuoritukset)

  @Test def testHaeOppijanVersiot(): Unit =
    val oppijanumero1 = "1.2.246.562.24.00000000123"
    val oppijanumero2 = "1.2.246.562.24.00000000234"
    val oppijanumeroJollaEiDataa = "1.2.246.562.24.00000000987"

    val oppijan1Versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(oppijanumero1, SuoritusJoukko.YTR, Seq("{\"attr\": \"value1\"}"), Instant.now()).get
    val oppijan1Versio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio(oppijanumero1, SuoritusJoukko.YTR, Seq("{\"attr\": \"value2\"}"), Instant.now()).get
    val oppijan2Versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(oppijanumero2, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Instant.now()).get

    val oppijan1Versiot = this.kantaOperaatiot.haeOppijanVersiot(oppijanumero1)
    Assertions.assertTrue(oppijan1Versiot.exists(v => v.tunniste == oppijan1Versio.tunniste))
    Assertions.assertTrue(oppijan1Versiot.exists(v => v.tunniste == oppijan1Versio2.tunniste))
    Assertions.assertEquals(oppijan1Versiot.size, 2)

    val oppijan2Versiot = this.kantaOperaatiot.haeOppijanVersiot(oppijanumero2)
    Assertions.assertTrue(oppijan2Versiot.exists(_.tunniste == oppijan2Versio.tunniste))
    Assertions.assertEquals(oppijan2Versiot.size, 1)

    val oppijanJollaEiDataaVersiot = this.kantaOperaatiot.haeOppijanVersiot(oppijanumeroJollaEiDataa)
    Assertions.assertTrue(oppijanJollaEiDataaVersiot.isEmpty)

  @Test def testTallennaVersio(): Unit =
    val OPPIJANUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, SuoritusJoukko.YTR, Seq("{}"), Instant.now())

    val haettuVersio = this.kantaOperaatiot.haeVersio(versio.get.tunniste)
    Assertions.assertEquals(versio, haettuVersio)

  @Test def testPaataversionVoimassaolo(): Unit =
    val OPPIJANUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO1, SuoritusJoukko.YTR, Seq("{}"), Instant.now())

    Thread.sleep(100)

    Assertions.assertTrue(this.kantaOperaatiot.paataVersionVoimassaolo(versio.get.tunniste))
    val loppu = this.kantaOperaatiot.haeVersio(versio.get.tunniste).get.loppu
    Assertions.assertTrue(loppu.isDefined)
    Assertions.assertTrue(loppu.get.isBefore(Instant.now()))
    Assertions.assertTrue(loppu.get.isAfter(Instant.now().minusMillis(1000)))

  @Test def testPaataversionVoimassaoloEiVaikutustaJoPaatettyyn(): Unit =
    val OPPIJANUMERO = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJANUMERO, SuoritusJoukko.YTR, Seq("{}"), Instant.now())

    Thread.sleep(100)

    Assertions.assertTrue(this.kantaOperaatiot.paataVersionVoimassaolo(versio.get.tunniste))
    val loppu1 = this.kantaOperaatiot.haeVersio(versio.get.tunniste).get.loppu

    Assertions.assertFalse(this.kantaOperaatiot.paataVersionVoimassaolo(versio.get.tunniste))
    val loppu2 = this.kantaOperaatiot.haeVersio(versio.get.tunniste).get.loppu

    Assertions.assertEquals(loppu1, loppu2)

  @Test def testHaeMetadatalla(): Unit =
    // tallennetaan versiot
    val versio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.99988877767", SuoritusJoukko.KOSKI, Seq("{}"), Instant.now()).get
    val versio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.99988877768", SuoritusJoukko.KOSKI, Seq("{}"), Instant.now()).get
    val versio3 = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.99988877769", SuoritusJoukko.KOSKI, Seq("{}"), Instant.now()).get

    // ja opiskeluoikeudet metadatalla
    // versiolla 1 molemmat haetut avaimet
    val opiskeluoikeus1 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("4.5.6"), "dummy oid", Set.empty, None, VALMIS)
    val metadata1 = Map(
      "haettuAvain1" -> Set("haettuArvo1", "muuArvo1"),
      "haettuAvain2" -> Set("haettuArvo2", "muuArvo2")
    )
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio1, Set(opiskeluoikeus1), metadata1)

    // versiolla 2 molemmat haetut avaimet mutta ei voimassa
    this.kantaOperaatiot.paataVersionVoimassaolo(versio2.tunniste)
    val opiskeluoikeus2 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("4.5.6"), "dummy oid", Set.empty, None, VALMIS)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio2, Set(opiskeluoikeus1), Map(
      "haettuAvain1" -> Set("haettuArvo1", "muuArvo1"),
      "haettuAvain2" -> Set("haettuArvo2", "muuArvo2")
    ))

    // ja versiolla 2 vain ensimmäinen
    val opiskeluoikeus3 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("4.5.6"), "dummy oid", Set.empty, None, VALMIS)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio3, Set(opiskeluoikeus2), Map(
      "haettuAvain1" -> Set("haettuArvo1", "muuArvo3"),
      "haettuAvain2" -> Set("muuArvo4", "muuArvo5")
    ))

    // joten palautuu vain versio 1
    Assertions.assertEquals(Seq((versio1, metadata1)), this.kantaOperaatiot.haeVersiotJaMetadata(Map(
      "haettuAvain1" -> Set("haettuArvo1"),
      "haettuAvain2" -> Set("haettuArvo2")
    ), Instant.now()))

  @Test def testHaeMetadaArvot(): Unit =
    // tallennetaan versiot ja opiskeluoikeudet metadatalla
    val versio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.99988877767", SuoritusJoukko.KOSKI, Seq("{}"), Instant.now()).get
    val arvot = Set("arvo1", "arvo2", "arvo3")
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio1, Set.empty, Map(
      "avain1" -> arvot,
      "avain2" -> Set("muu1", "muu2")
    ))

    Assertions.assertEquals(arvot, this.kantaOperaatiot.haeMetadataAvaimenArvot("avain1"));

  @Test def testHaeMetadaArvotPrefixilla(): Unit =
    // tallennetaan versiot ja opiskeluoikeudet metadatalla
    val versio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.99988877767", SuoritusJoukko.KOSKI, Seq("{}"), Instant.now()).get
    val haetutArvot = Set("prefiksi:arvo1", "prefiksi:arvo2")
    val muutArvot = Set("arvo3", "arvo4")
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio1, Set.empty, Map(
      "avain1" -> Set(haetutArvot, muutArvot).flatten,
      "avain2" -> Set("prefiksi:arvo3", "muu2")
    ))

    Assertions.assertEquals(haetutArvot, this.kantaOperaatiot.haeMetadataAvaimenArvot("avain1", Some("prefiksi:")));

  @Test def testYliajoRoundtrip(): Unit = {
    val personOid = "1.2.246.562.24.12345678901"
    val hakuOid = "1.2.246.562.29.98765432109"
    val virkailijaOid = "1.2.246.562.24.11223344556"

    val avainArvoYliajo = AvainArvoYliajo(
      avain = "perusopetuksen_kieli",
      arvo = "FI",
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Testiyliajon selite"
    )

    //Save and fetch
    this.kantaOperaatiot.tallennaYliajot(Seq(avainArvoYliajo))
    val haetutYliajot = this.kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid)

    // Verify results
    Assertions.assertEquals(1, haetutYliajot.size)
    Assertions.assertEquals(avainArvoYliajo.avain, haetutYliajot.head.avain)
    Assertions.assertEquals(avainArvoYliajo.arvo, haetutYliajot.head.arvo)
    Assertions.assertEquals(avainArvoYliajo.henkiloOid, haetutYliajot.head.henkiloOid)
    Assertions.assertEquals(avainArvoYliajo.hakuOid, haetutYliajot.head.hakuOid)
    Assertions.assertEquals(avainArvoYliajo.virkailijaOid, haetutYliajot.head.virkailijaOid)
    Assertions.assertEquals(avainArvoYliajo.selite, haetutYliajot.head.selite)
  }

  @Test def testYliajoVersiointi(): Unit = {
    val personOid = "1.2.246.562.24.12345678901"
    val hakuOid = "1.2.246.562.29.98765432109"
    val virkailijaOid = "1.2.246.562.24.11223344556"
    val avain = "peruskoulu_suoritusvuosi"

    //Create first override
    val yliajo1 = AvainArvoYliajo(
      avain = avain,
      arvo = "2022",
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Ensimmäinen yliajo"
    )

    // Save first override
    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo1))

    // Verify it's saved correctly
    val haetutYliajot1 = this.kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid)
    Assertions.assertEquals(1, haetutYliajot1.size)
    Assertions.assertEquals("2022", haetutYliajot1.head.arvo)

    // Create second override for the same key
    val yliajo2 = AvainArvoYliajo(
      avain = avain,
      arvo = "2023",
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Toinen yliajo"
    )

    // Save second override
    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo2))

    // Verify only the second override is active
    val haetutYliajot2 = this.kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid)
    Assertions.assertEquals(1, haetutYliajot2.size)
    Assertions.assertEquals("2023", haetutYliajot2.head.arvo)
    Assertions.assertEquals("Toinen yliajo", haetutYliajot2.head.selite)
  }

  @Test def testMultipleYliajosForDifferentKeys(): Unit = {
    val personOid = "1.2.246.562.24.12345678901"
    val hakuOid = "1.2.246.562.29.98765432109"
    val virkailijaOid = "1.2.246.562.24.11223344556"

    // Create overrides for different keys
    val yliajo1 = AvainArvoYliajo(
      avain = "perusopetuksen_kieli",
      arvo = "SV",
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Kieliyliajo"
    )

    val yliajo2 = AvainArvoYliajo(
      avain = "perustutkinto_suoritettu",
      arvo = "true",
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Suoritusyliajo"
    )

    // Save both overrides
    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo1, yliajo2))

    val haetutYliajot = this.kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid)
    Assertions.assertEquals(2, haetutYliajot.size)

    val yliajoMap = haetutYliajot.map(y => y.avain -> y).toMap

    //Verify avain, arvo and selite for both overrides
    Assertions.assertTrue(yliajoMap.contains("perusopetuksen_kieli"))
    Assertions.assertEquals("SV", yliajoMap("perusopetuksen_kieli").arvo)
    Assertions.assertEquals("Kieliyliajo", yliajoMap("perusopetuksen_kieli").selite)

    Assertions.assertTrue(yliajoMap.contains("perustutkinto_suoritettu"))
    Assertions.assertEquals("true", yliajoMap("perustutkinto_suoritettu").arvo)
    Assertions.assertEquals("Suoritusyliajo", yliajoMap("perustutkinto_suoritettu").selite)
  }

  @Test def testYliajotForDifferentPersons(): Unit = {
    val personOid1 = "1.2.246.562.24.12345678901"
    val personOid2 = "1.2.246.562.24.98765432109"
    val hakuOid = "1.2.246.562.29.98765432109"
    val virkailijaOid = "1.2.246.562.24.11223344556"
    val avain = "perusopetuksen_kieli"

    // Create overrides for different persons
    val yliajo1 = AvainArvoYliajo(
      avain = avain,
      arvo = "FI",
      henkiloOid = personOid1,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Henkilö 1 yliajo"
    )

    val yliajo2 = AvainArvoYliajo(
      avain = avain,
      arvo = "SV",
      henkiloOid = personOid2,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Henkilö 2 yliajo"
    )

    // Save both overrides
    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo1, yliajo2))

    // Fetch overrides for person 1
    val haetutYliajot1 = this.kantaOperaatiot.haeOppijanYliajot(personOid1, hakuOid)
    Assertions.assertEquals(1, haetutYliajot1.size)
    Assertions.assertEquals("FI", haetutYliajot1.head.arvo)
    Assertions.assertEquals("Henkilö 1 yliajo", haetutYliajot1.head.selite)

    // Fetch overrides for person 2
    val haetutYliajot2 = this.kantaOperaatiot.haeOppijanYliajot(personOid2, hakuOid)
    Assertions.assertEquals(1, haetutYliajot2.size)
    Assertions.assertEquals("SV", haetutYliajot2.head.arvo)
    Assertions.assertEquals("Henkilö 2 yliajo", haetutYliajot2.head.selite)
  }

  @Test def testYliajotForDifferentHakus(): Unit = {
    val personOid = "1.2.246.562.24.12345678901"
    val hakuOid1 = "1.2.246.562.29.11111111111"
    val hakuOid2 = "1.2.246.562.29.22222222222"
    val virkailijaOid = "1.2.246.562.24.11223344556"
    val avain = "perusopetuksen_kieli"

    val yliajo1 = AvainArvoYliajo(
      avain = avain,
      arvo = "FI",
      henkiloOid = personOid,
      hakuOid = hakuOid1,
      virkailijaOid = virkailijaOid,
      selite = "Haku 1 yliajo"
    )

    val yliajo2 = AvainArvoYliajo(
      avain = avain,
      arvo = "SV",
      henkiloOid = personOid,
      hakuOid = hakuOid2,
      virkailijaOid = virkailijaOid,
      selite = "Haku 2 yliajo"
    )

    // Save both overrides
    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo1, yliajo2))

    // Check haku 1
    val haetutYliajot1 = this.kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid1)
    Assertions.assertEquals(1, haetutYliajot1.size)
    Assertions.assertEquals("FI", haetutYliajot1.head.arvo)
    Assertions.assertEquals("Haku 1 yliajo", haetutYliajot1.head.selite)

    // Check haku 2
    val haetutYliajot2 = this.kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid2)
    Assertions.assertEquals(1, haetutYliajot2.size)
    Assertions.assertEquals("SV", haetutYliajot2.head.arvo)
    Assertions.assertEquals("Haku 2 yliajo", haetutYliajot2.head.selite)
  }

  @Test def testYliajonPoisto(): Unit = {
    val personOid = "1.2.246.562.24.12345678901"
    val hakuOid1 = "1.2.246.562.29.11111111111"
    val hakuOid2 = "1.2.246.562.29.22222222222"
    val virkailijaOid = "1.2.246.562.24.11223344556"
    val avain = "perusopetuksen_kieli"

    val yliajo1 = AvainArvoYliajo(
      avain = avain,
      arvo = "FI",
      henkiloOid = personOid,
      hakuOid = hakuOid1,
      virkailijaOid = virkailijaOid,
      selite = "Haku 1 yliajo"
    )

    val yliajo2 = AvainArvoYliajo(
      avain = avain,
      arvo = "SV",
      henkiloOid = personOid,
      hakuOid = hakuOid2,
      virkailijaOid = virkailijaOid,
      selite = "Haku 2 yliajo"
    )

    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo1, yliajo2))

    // Tarkistetaan haun 1 yliajo
    val haetutYliajot1 = this.kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid1)
    Assertions.assertEquals(1, haetutYliajot1.size)
    Assertions.assertEquals("FI", haetutYliajot1.head.arvo)
    Assertions.assertEquals("Haku 1 yliajo", haetutYliajot1.head.selite)

    // Tarkistetaan haun 2 yliajo
    val haetutYliajot2 = this.kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid2)
    Assertions.assertEquals(1, haetutYliajot2.size)
    Assertions.assertEquals("SV", haetutYliajot2.head.arvo)
    Assertions.assertEquals("Haku 2 yliajo", haetutYliajot2.head.selite)

    //Poistetaan yliajo haulta 2
    this.kantaOperaatiot.poistaYliajo(personOid, hakuOid2, avain)

    // Tarkistetaan että haun 1 yliajo edelleen voimassa
    val haetutYliajot1After = this.kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid1)
    Assertions.assertEquals(1, haetutYliajot1After.size)
    Assertions.assertEquals("FI", haetutYliajot1After.head.arvo)
    Assertions.assertEquals("Haku 1 yliajo", haetutYliajot1After.head.selite)

    // Tarkistetaan että haulle 2 ei enää yliajoa
    val haetutYliajot2After = this.kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid2)
    Assertions.assertEquals(0, haetutYliajot2After.size)
  }

  @Test def testCasMapping(): Unit = {
    val CAS_SESSION_ID = "cas-session-id"
    val SPRING_SESSION_ID = "tomcat-session-id-000000000000000000"

    // kannan foreign key vaatii että lisätään mock spring-sessio
    Await.result(this.database.run(sqlu"""INSERT INTO spring_session (primary_id, session_id, creation_time, last_access_time, max_inactive_interval, expiry_time, principal_name) VALUES ('primary_id', $SPRING_SESSION_ID, 0, 0, 0, 0, 'principal')"""), 5.seconds)

    // kun lisätään mappaus cas-sessioon niin mappays löytyy
    this.kantaOperaatiot.addMappingForSessionId(CAS_SESSION_ID, SPRING_SESSION_ID);
    Assertions.assertEquals(Some(SPRING_SESSION_ID), this.kantaOperaatiot.getSessionIdByMappingId(CAS_SESSION_ID))

    // ja kun mappaus poistetaan sitä ei enää löydy
    this.kantaOperaatiot.deleteCasMappingBySessionId(SPRING_SESSION_ID)
    Assertions.assertEquals(None, this.kantaOperaatiot.getSessionIdByMappingId(CAS_SESSION_ID))
  }

  @Test def testJobProgress(): Unit = {
    val haettuTaskName = "test-task"
    val haettuTaskId = UUID.randomUUID()
    val muuTaskName = "muu-task"
    val muuTaskId = UUID.randomUUID()
    val lastUpdated = Instant.ofEpochMilli((Instant.now.toEpochMilli/1000)*1000)

    this.kantaOperaatiot.updateJobStatus(haettuTaskId, haettuTaskName, 0.5, lastUpdated)
    this.kantaOperaatiot.updateJobStatus(muuTaskId, muuTaskName, 0.5, lastUpdated)

    // kun haetaan nimellä saadaan vain haettut jobi
    Assertions.assertEquals(List(Job(haettuTaskId, haettuTaskName, 0.5, lastUpdated)), this.kantaOperaatiot.getLastJobStatuses(Some(haettuTaskName), None, 10))

    // kun haetaan tunnisteella saadaan vain haettu jobi
    Assertions.assertEquals(List(Job(haettuTaskId, haettuTaskName, 0.5, lastUpdated)), this.kantaOperaatiot.getLastJobStatuses(None, Some(haettuTaskId), 10))

    // kun haetaan nimellä ja tunnisteella saadaan vain haettu jobi
    Assertions.assertEquals(List(Job(haettuTaskId, haettuTaskName, 0.5, lastUpdated)), this.kantaOperaatiot.getLastJobStatuses(Some(haettuTaskName), Some(haettuTaskId), 10))

    // kun haetaan ilman parametrejä saadaan kaikki jobit
    Assertions.assertEquals(List(Job(haettuTaskId, haettuTaskName, 0.5, lastUpdated), Job(muuTaskId, muuTaskName, 0.5, lastUpdated)), this.kantaOperaatiot.getLastJobStatuses(None, None, 10))

  }
}
