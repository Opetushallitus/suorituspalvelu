package fi.oph.suorituspalvelu.business

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiKotiopetusjakso, KoskiLisatiedot, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila, KoskiParser, KoskiToSuoritusConverter}
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.{AfterAll, AfterEach, Assertions, BeforeAll, BeforeEach, Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.testcontainers.containers.PostgreSQLContainer
import slick.jdbc.JdbcBackend.Database
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory
import slick.jdbc.PostgresProfile.api.*

import java.time.{Instant, LocalDate}
import scala.concurrent.duration.{DurationInt, pairIntToDuration}
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random
import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{TUVA, VUOSILUOKKA_9}
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
            DROP TABLE lahtokoulut;
            DROP TABLE versiot;
            DROP TABLE flyway_schema_history;
            DROP TABLE henkilot;
            DROP TABLE yliajot;
          """), 5.seconds)

  /**
   * Testataan että versio tallentuu ja luetaan oikein.
   */
  @Test def testVersioRoundtrip(): Unit =
    val HENKILONUMERO = "1.2.3"

    // tallennetaan versio
    val data = "{\"attr\": \"value\"}"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq(data), Seq.empty, Instant.now()).get

    // data palautuu
    Assertions.assertEquals(Seq(data), this.kantaOperaatiot.haeData(versio)._2)

  /**
   * Testataan että json-datan muuttuessa henkilölle tallennetaan uusi versio.
   */
  @Test def testUusiVersioLuodaanKunJsonDataMuuttuu(): Unit =
    val HENKILONUMERO = "1.2.3"

    // tallennetaan versio
    Assertions.assertTrue(this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Seq.empty, Instant.now()).isDefined)
    Assertions.assertTrue(this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value2\"}"), Seq.empty, Instant.now()).isDefined)

  /**
   * Testataan että jos henkiölle tallennetaan uudestaan viimeisin json-data niin uutta versiota ei luoda.
   */
  @Test def testUuttaVersiotaEiLuodaKunJsonDataEiMuutu(): Unit =
    val HENKILONUMERO = "1.2.3"

    // tallennetaan versio
    val originalData = "{\"attr\": \"value\", \"arr\": [1, 2]}"
    val originalVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq(originalData), Seq.empty, Instant.now())
    Assertions.assertTrue(originalVersio.isDefined)

    // yritetään tallentaa uusi versio samalla datalla
    val duplicateData = "{\"arr\": [2, 1], \"attr\": \"value\"}"
    val duplicateVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq(duplicateData), Seq.empty, Instant.now())
    Assertions.assertTrue(duplicateVersio.isEmpty)

  /**
   * Testataan että xml-datan muuttuessa henkilölle tallennetaan uusi versio.
   */
  @Test def testUusiVersioLuodaanKunXmlDataMuuttuu(): Unit =
    val HENKILONUMERO = "1.2.3"

    // tallennetaan versio
    Assertions.assertTrue(this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.VIRTA, Seq.empty, Seq("<ulompi><sisempi>arvo1</sisempi><sisempi attr=\"arvo\">arvo2</sisempi></ulompi>"), Instant.now()).isDefined)
    Assertions.assertTrue(this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.VIRTA, Seq.empty, Seq("<ulompi><sisempi>arvo1</sisempi><sisempi attr=\"muuttunut\">arvo2</sisempi></ulompi>"), Instant.now()).isDefined)

  /**
   * Testataan että jos henkilölle tallennetaan uudestaan viimeisin xml-data niin uutta versiota ei luoda.
   */
  @Test def testUuttaVersiotaEiLuodaKunXmlDataEiMuutu(): Unit =
    val HENKILONUMERO = "1.2.3"

    // tallennetaan versio
    val originalData = "<ulompi><sisempi>arvo1</sisempi><sisempi>arvo2</sisempi></ulompi>"
    val originalVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.VIRTA, Seq.empty, Seq(originalData), Instant.now())
    Assertions.assertTrue(originalVersio.isDefined)

    // yritetään tallentaa uusi versio samalla datalla (vaikka tagien järjestys eri)
    val duplicateData = "<ulompi><sisempi>arvo2</sisempi><sisempi>arvo1</sisempi></ulompi>"
    val duplicateVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.VIRTA, Seq.empty, Seq(duplicateData), Instant.now())
    Assertions.assertTrue(duplicateVersio.isEmpty)

  /**
   * Testataan että jos henkilölle tallennetaan uudestaan viimeisin json-data niin uutta versiota ei luoda.
   */
  @Test def testUuttaVersiotaEiLuodaKunUudempiVersioTallennettu(): Unit =
    val HENKILONUMERO = "1.2.3"

    // tallennetaan versio
    val originalData = "{\"attr\": \"value\", \"arr\": [1, 2]}"
    val originalVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq(originalData), Seq.empty, Instant.now())
    Assertions.assertTrue(originalVersio.isDefined)

    // yritetään tallentaa uusi versio samalla datalla
    val staleData = "{\"arr\": [2, 1], \"attr\": \"value\"}"
    val staleVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq(staleData), Seq.empty, Instant.now().minusSeconds(10))
    Assertions.assertTrue(staleVersio.isEmpty)

  /**
   * Testataan että kun samalla henkilöllä tallennetaan versioita rinnakkain syntyy katkeamaton voimassaolohistoria
   */
  @Test def testVoimassaoloPerakkain(): Unit =
    val HENKILONUMERO = "1.2.3"

    // tallennetaan rinnakkain suuri joukko versioita
    val tallennusOperaatiot = Range(0, 500).map(i => () => {
      Thread.sleep((Math.random()*50).asInstanceOf[Int])
      this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value" + i + "\"}"), Seq.empty, Instant.now())
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
    val vanhaVersio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Seq.empty, Instant.now()).get
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(vanhaVersio1, Set.empty, Seq.empty, ParserVersions.KOSKI)

    // tallennetaan aikaleima ja todetaan ettei ole versioita ennen sitä
    val alkaen = Instant.now
    Assertions.assertEquals(Seq.empty, this.kantaOperaatiot.haeUusimmatMuuttuneetVersiot(alkaen))

    // tallennetaan ja otetaan käyttöön versio aikaleiman jälkeen
    val uusiVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value2\"}"), Seq.empty, Instant.now()).get
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(uusiVersio, Set.empty, Seq.empty, ParserVersions.KOSKI)

    // tallennetaan (muttei oteta käyttöön) vielä uudempi versio
    val eiKaytossaVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value3\"}"), Seq.empty, Instant.now()).get

    // palautuu aikaleiman jälkeen tallennettu ja käyttöönotettu versio
    Assertions.assertEquals(Seq(uusiVersio.tunniste), this.kantaOperaatiot.haeUusimmatMuuttuneetVersiot(alkaen).map(v => v.tunniste))

  @Test def testHaeVersiot(): Unit =
    val koskiVersio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Seq.empty, Instant.now()).get
    val koskiVersio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio("2.3.4", SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Seq.empty, Instant.now()).get
    val ytrVersio = this.kantaOperaatiot.tallennaJarjestelmaVersio("1.2.3", SuoritusJoukko.YTR, Seq("{\"attr\": \"value1\"}"), Seq.empty, Instant.now()).get

    Assertions.assertEquals(Set(koskiVersio1, koskiVersio2), this.kantaOperaatiot.haeVersiot(SuoritusJoukko.KOSKI).toSet)

  @Test def testParserVersioRoundtrip(): Unit =
    val HENKILONUMERO = "1.2.246.562.24.99977766655"
    val PARSER_VERSIO = 42

    // tallennetaan versio - parserVersio pitäisi olla None
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Seq.empty, Instant.now()).get
    Assertions.assertEquals(None, versio.parserVersio)

    // haetaan versio ja tarkistetaan että parserVersio on edelleen None
    val haettuVersioEnnen = this.kantaOperaatiot.haeHenkilonVersiot(HENKILONUMERO).head
    Assertions.assertEquals(None, haettuVersioEnnen.parserVersio)

    // tallennetaan opiskeluoikeudet parserVersiolla
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set.empty, Seq.empty, PARSER_VERSIO)

    // haetaan versio ja tarkistetaan että parserVersio on nyt asetettu
    val haettuVersioJalkeen = this.kantaOperaatiot.haeHenkilonVersiot(HENKILONUMERO).head
    Assertions.assertEquals(Some(PARSER_VERSIO), haettuVersioJalkeen.parserVersio)

    // tarkistetaan myös haeVersiot-metodin kautta
    val haetutVersiot = this.kantaOperaatiot.haeVersiot(SuoritusJoukko.KOSKI).filter(_.henkiloOid == HENKILONUMERO)
    Assertions.assertEquals(1, haetutVersiot.size)
    Assertions.assertEquals(Some(PARSER_VERSIO), haetutVersiot.head.parserVersio)

  /**
   * Testataan että minimaalinen suoritus tallentuu ja luetaan oikein.
   */
  @Test def testSuoritusRoundtrip(): Unit =
    val HENKILONUMERO = "2.3.4"

    // tallennetaan versio ja suoritukset
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now()).get
    val suoritukset = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "3.4.5"), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("FI", "kielivalikoima", None)), true, None, None)), Set.empty, false, false)
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("4.5.6"), "opiskeluoikeusoid1", Set(suoritukset), None, VALMIS, List.empty)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus), Seq.empty, ParserVersions.KOSKI)

    // suoritus palautuu kun haetaan henkilönumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(HENKILONUMERO)
    Assertions.assertEquals(Map(versio.copy(parserVersio = Some(ParserVersions.KOSKI)) -> Set(opiskeluoikeus)), haetutSuoritusEntiteetit)

  /**
   * Testataan että vanhat suoritukset poistetaan kun uudet suoritukset tallennetaan, ts. ei synny suoritusten
   * unionia.
   */
  @Test def testVanhatSuorituksetPoistetaanUusienTieltä(): Unit =
    val HENKILONUMERO1 = "1.2.246.562.24.99988877766"
    val HENKILONUMERO2 = "1.2.246.562.24.88877766655"
    val OPPILAITOSOID1 = "1.2.246.562.10.00000000123"
    val OPPILAITOSOID2 = "1.2.246.562.10.00000000234"
    val OPISKELUOIKEUSOID1 = "1.2.246.562.15.12345678901"
    val OPISKELUOIKEUSOID2 = "1.2.246.562.15.09876543210"

    // tallennetaan versio ja suoritukset henkilölle 1
    val versio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO1, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now()).get
    val suoritus1 = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID1), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("FI", "kielivalikoima", None)), true, None, None)), Set.empty, false, false)
    val opiskeluoikeus1 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(OPISKELUOIKEUSOID1), OPPILAITOSOID1, Set(suoritus1), None, VALMIS, List.empty)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio1, Set(opiskeluoikeus1), Seq.empty, ParserVersions.KOSKI)

    // tallennetaan versio henkilölle 2
    val versio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO2, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now()).get

    // tallennetaan suoritukset kerran ja sitten toisen kerran henkilölle 2
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio2, Set(opiskeluoikeus1), Seq.empty, ParserVersions.KOSKI)
    val suoritus2 = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID2), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None)), Set.empty, false, false)
    val opiskeluoikeus2 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(OPISKELUOIKEUSOID2), OPPILAITOSOID2, Set(suoritus2), None, VALMIS, List.empty)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio2, Set(opiskeluoikeus2), Seq.empty, ParserVersions.KOSKI)

    // henkilön 2 uudet suoritukset palautuvat kun haetaan henkilönumerolla
    val haetutSuoritusEntiteetit2 = this.kantaOperaatiot.haeSuoritukset(HENKILONUMERO2)
    Assertions.assertEquals(Map(versio2.copy(parserVersio = Some(ParserVersions.KOSKI)) -> Set(opiskeluoikeus2)), haetutSuoritusEntiteetit2)

    // henkilön 1 suoritukset ennallaan
    val haetutSuoritusEntiteetit1 = this.kantaOperaatiot.haeSuoritukset(HENKILONUMERO1)
    Assertions.assertEquals(Map(versio1.copy(parserVersio = Some(ParserVersions.KOSKI)) -> Set(opiskeluoikeus1)), haetutSuoritusEntiteetit1)


  /**
   * Testataan että henkilön osittainen KOSKI-suoritussetti säilyy muuttumattomana kun se tallennetaan ja luetaan
   */
  @Test def testAitoKoskiDataSuorituksetOsajoukkoRoundtrip(): Unit = {
    Seq(
      "/1_2_246_562_24_40483869857b.json"
    ).foreach(fileName => {
      val splitData = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream(fileName))
      val suoritukset = splitData.foreach((henkiloOid, data) => {
        val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloOid, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now()).get

        val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
        val oo: Set[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet, koodisto => Map.empty).toSet
        this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, oo, Seq.empty, ParserVersions.KOSKI)

        val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(henkiloOid)

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
      val splitData = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream(fileName))
      val suoritukset = splitData.foreach((henkiloOid, data) => {

        val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloOid, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Seq.empty, Instant.now()).get

        val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
        val oo: Set[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet, koodisto => Map.empty).toSet
        this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, oo, Seq.empty, ParserVersions.KOSKI)

        val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(henkiloOid)

        Assertions.assertEquals(Map(versio.copy(parserVersio = Some(ParserVersions.KOSKI)) -> oo), haetutSuoritukset);
      })
    })
  }

  /**
   * Testataan että suorituksia haettaessa palautetaan viimeisin versio jonka data on parseroitu onnistuneesti.
   */
  @Test def testPalautetaanViimeisinParseroituVersio(): Unit =
    val HENKILONUMERO = "2.3.4"

    // tallenetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Seq.empty, Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value2\"}"), Seq.empty, Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value3\"}"), Seq.empty, Instant.now()).get

    // tallennetaan versio ja suoritukset
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value4\"}"), Seq.empty, Instant.now()).get
    val suoritus = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), "3.4.5"), None, Koodi("arvo", "koodisto",  Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("FI", "kielivalikoima", None)), true, None, None)), Set.empty, false, false)
    val lisatiedot = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain = Some(true)))), None, None)
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("opiskeluoikeusOid"), "oppilaitosOid", Set(suoritus), Some(lisatiedot), VALMIS, List.empty)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus), Seq.empty, ParserVersions.KOSKI)

    // tallennetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value5\"}"), Seq.empty, Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value6\"}"), Seq.empty, Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value7\"}"), Seq.empty, Instant.now()).get

    // versio jotka suoritukset purettu palautuu suorituksineen kun haetaan henkilönumerolla
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(HENKILONUMERO)
    Assertions.assertEquals(Map(versio.copy(parserVersio = Some(ParserVersions.KOSKI)) -> Set(opiskeluoikeus)), haetutSuoritusEntiteetit)

  /**
   * Testataan että suorituksia haettaessa ei palauteta mitään jos ei ole versioita joiden data on parseroitu
   * onnistuneesti.
   */
  @Test def testEiPalautetaVersioitaJosEiParseroituja(): Unit =
    val HENKILONUMERO = "2.3.4"

    // tallenetaan uusia versioita ilman että tallennetaan suorituksia
    this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Seq.empty, Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value2\"}"), Seq.empty, Instant.now()).get
    this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value3\"}"), Seq.empty, Instant.now()).get

    // koska ei ole parseroituja versioita ei palaudu mitään
    val haetutSuoritusEntiteetit = this.kantaOperaatiot.haeSuoritukset(HENKILONUMERO)
    Assertions.assertEquals(Map.empty, haetutSuoritusEntiteetit)

  /**
   * Testataan (hyvin karkealla tavalla) suoritusten tallennuksen ja haun suorituskykyä.
   */
  @Test def testExampleDataSuorituksetRoundtripPerformance(): Unit =
    val HENKILONUMERO = "2.3.4."
    val iterations = 100

    val startSave = Instant.now()
    val data = KoskiIntegration.splitKoskiDataByHenkilo(this.getClass.getResourceAsStream("/1_2_246_562_24_40483869857.json")).iterator.next()._2
    (1 to iterations).foreach(i => {
      val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO + i, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Seq.empty, Instant.now()).get
      val oo = KoskiToSuoritusConverter.parseOpiskeluoikeudet(KoskiParser.parseKoskiData(data), koodisto => Map.empty)
      this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, oo.toSet, Seq.empty, ParserVersions.KOSKI)
    })
    val saveDuration = Instant.now().toEpochMilli - startSave.toEpochMilli
    Assertions.assertTrue(saveDuration< 50 * iterations);

    val readStart = Instant.now()
    (1 to iterations).foreach(i => {
      val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(HENKILONUMERO + i)
    })
    val readDuration = Instant.now().toEpochMilli - readStart.toEpochMilli
    Assertions.assertTrue(readDuration < 10 * iterations);

  @Test def testPerusopetuksenOpiskeluoikeusEqualityAfterPersisting(): Unit =
    val HENKILONUMERO1 = "1.2.246.562.24.99988877766"
    val HENKILONUMERO2 = "1.2.246.562.24.88877766655"
    val OPPILAITOSOID1 = "1.2.246.562.10.00000000123"
    val OPPILAITOSOID2 = "1.2.246.562.10.00000000234"
    val OPISKELUOIKEUSOID1 = "1.2.246.562.15.12345678901"
    val OPISKELUOIKEUSOID2 = "1.2.246.562.15.09876543210"

    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO1, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Seq.empty, Instant.now()).get

    val suoritus1 = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID1), None, Koodi("arvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("FI", "kielivalikoima", None)), true, None, None)), Set.empty, false, false)
    val luokkasuoritus1 = PerusopetuksenVuosiluokka(UUID.randomUUID(), Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID1), Kielistetty(Some("vuosiluokka"), None, None), Koodi("arvo1", "koodisto", Some(1)), Some(LocalDate.parse("2024-08-01")), Some(LocalDate.parse("2025-08-01")), false)
    val lisatiedot = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(Some(true)))), Some(false), Some(List(KoskiKotiopetusjakso("2023-08-24", Some("2024-01-22")))))
    val tilat = KoskiOpiskeluoikeusTila(List(KoskiOpiskeluoikeusJakso(LocalDate.parse("2024-06-03"), KoskiKoodi("opiskelu", "tilakoodisto", Some(6), Kielistetty(None, None, None), None)), KoskiOpiskeluoikeusJakso(LocalDate.parse("2024-11-09"), KoskiKoodi("joulunvietto", "tilakoodisto", Some(6), Kielistetty(None, None, None), None))))
    val opiskeluoikeus1 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(OPISKELUOIKEUSOID1), OPPILAITOSOID1, Set(suoritus1, luokkasuoritus1), Some(lisatiedot), KESKEN, List.empty)

    val suoritus2 = PerusopetuksenOppimaara(UUID.randomUUID(), None, Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID2), None, Koodi("toinenarvo", "koodisto", Some(1)), SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)), Set.empty, None, None, None, Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None), Koodi("arvo", "koodisto", None), Koodi("10", "koodisto", None), Some(Koodi("EN", "kielivalikoima", None)), true, None, None)), Set.empty, false, false)
    val luokkasuoritus2 = PerusopetuksenVuosiluokka(UUID.randomUUID(), Oppilaitos(Kielistetty(None, None, None), OPPILAITOSOID2), Kielistetty(Some("vuosiluokka2"), None, None), Koodi("arvo2", "koodisto", Some(1)), Some(LocalDate.parse("2023-08-01")), Some(LocalDate.parse("2024-08-01")), false)
    val lisatiedot2 = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(Some(false)))), Some(true), None)
    val tilat2 = KoskiOpiskeluoikeusTila(List(KoskiOpiskeluoikeusJakso(LocalDate.parse("2022-07-02"), KoskiKoodi("hengailu", "tilakoodisto", Some(6), Kielistetty(None, None, None), None)), KoskiOpiskeluoikeusJakso(LocalDate.parse("2022-10-09"), KoskiKoodi("juhannusvalmistelut", "tilakoodisto", Some(6), Kielistetty(None, None, None), None))))
    val opiskeluoikeus2 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some(OPISKELUOIKEUSOID2), OPPILAITOSOID2, Set(suoritus2, luokkasuoritus2), Some(lisatiedot2), KESKEN, List.empty)

    // tallennetaan molemmat versioon liittyvät opiskeluoikeudet
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus1, opiskeluoikeus2), Seq.empty, ParserVersions.KOSKI)

    // henkilön 1 suoritukset ennallaan
    val haetutSuoritusEntiteetit1 = this.kantaOperaatiot.haeSuoritukset(HENKILONUMERO1)
    Assertions.assertEquals(Map(versio.copy(parserVersio = Some(ParserVersions.KOSKI)) -> Set(opiskeluoikeus1, opiskeluoikeus2)), haetutSuoritusEntiteetit1)

  @Test def testAmmatillinenOpiskeluoikeusEqualityAfterPersisting(): Unit =
    val HENKILONUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO1, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Seq.empty, Instant.now()).get
    val oppilaitos = Oppilaitos(Kielistetty(Some("Nimi suomi"), Some("Nimi Ruotsi"), Some("Nimi englanti")), "1.2.246.562.10.95136889433")
    val suoritus = TestDataUtil.getTestAmmatillinenTutkinto(oppilaitos = oppilaitos)
    val tilat = KoskiOpiskeluoikeusTila(List(KoskiOpiskeluoikeusJakso(LocalDate.parse("2024-06-03"), KoskiKoodi("opiskelu", "tilakoodisto", Some(6), Kielistetty(None, None, None), None)), KoskiOpiskeluoikeusJakso(LocalDate.parse("2024-11-09"), KoskiKoodi("joulunvietto", "tilakoodisto", Some(6), Kielistetty(None, None, None), None))))
    val opiskeluoikeus = AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "opiskeluoikeusOid", oppilaitos, Set(suoritus), Some(tilat), List.empty)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus), Seq.empty, ParserVersions.KOSKI)

    val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(HENKILONUMERO1)
    Assertions.assertEquals(Map(versio.copy(parserVersio = Some(ParserVersions.KOSKI)) -> Set(opiskeluoikeus)), haetutSuoritukset)

  @Test def testGeneerinenOpiskeluoikeusEqualityAfterPersisting(): Unit =
    val HENKILONUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO1, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value\"}"), Seq.empty, Instant.now()).get
    val suoritus = Tuva(UUID.randomUUID(), Kielistetty(Some("Nimi Suomi"), None, None), Koodi("arvo", "koodisto", None), Oppilaitos(Kielistetty(Some("Nimi suomi"), None, None), "1.2.246.562.10.95136889433"), Koodi("lasna", "koskiopiskeluoikeudentila", Some(1)), SuoritusTila.KESKEN, LocalDate.parse("2025-03-20"), Some(LocalDate.parse("2025-03-20")), None,
      Lahtokoulu(LocalDate.parse("2025-03-20"), Some(LocalDate.parse("2025-03-20")), "1.2.246.562.10.95136889433", Some(2025), None, Some(SuoritusTila.KESKEN), None, TUVA))
    val tilat = KoskiOpiskeluoikeusTila(List(KoskiOpiskeluoikeusJakso(LocalDate.parse("2023-05-03"), KoskiKoodi("opiskelu", "tilakoodisto", Some(2), Kielistetty(None, None, None), None)), KoskiOpiskeluoikeusJakso(LocalDate.parse("2025-10-09"), KoskiKoodi("lasna", "tilakoodisto", Some(6), Kielistetty(None, None, None), None))))
    val opiskeluoikeus = GeneerinenOpiskeluoikeus(UUID.randomUUID(), "opiskeluoikeusOid", Koodi("arvo", "koodisto", None), "oppilaitosOid", Set(suoritus), Some(tilat), List.empty)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus), Seq.empty, ParserVersions.KOSKI)

    val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(HENKILONUMERO1)
    Assertions.assertEquals(Map(versio.copy(parserVersio = Some(ParserVersions.KOSKI)) -> Set(opiskeluoikeus)), haetutSuoritukset)

  @Test def testYTRRoundTrip(): Unit =
    val HENKILONUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO1, SuoritusJoukko.YTR, Seq("{}"), Seq.empty, Instant.now()).get
    val opiskeluoikeus = YOOpiskeluoikeus(UUID.randomUUID(), YOTutkinto(UUID.randomUUID(), Koodi("fi", "kieli", Some(1)), SuoritusTila.KESKEN, None, Set.empty))
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set(opiskeluoikeus), Seq.empty, ParserVersions.YTR)

    val haetutSuoritukset = this.kantaOperaatiot.haeSuoritukset(HENKILONUMERO1)
    Assertions.assertEquals(Map(versio.copy(parserVersio = Some(ParserVersions.YTR)) -> Set(opiskeluoikeus)), haetutSuoritukset)

  @Test def testHaeHenkilonVersiot(): Unit =
    val HENKILONUMERO1 = "1.2.246.562.24.00000000123"
    val HENKILONUMERO2 = "1.2.246.562.24.00000000234"
    val HENKILONUMEROJOLLAEIDATAA = "1.2.246.562.24.00000000987"

    val henkilon1Versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO1, SuoritusJoukko.YTR, Seq("{\"attr\": \"value1\"}"), Seq.empty, Instant.now()).get
    val henkilon1Versio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO1, SuoritusJoukko.YTR, Seq("{\"attr\": \"value2\"}"), Seq.empty, Instant.now()).get
    val henkilon2Versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO2, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value1\"}"), Seq.empty, Instant.now()).get

    val henkilon1Versiot = this.kantaOperaatiot.haeHenkilonVersiot(HENKILONUMERO1)
    Assertions.assertTrue(henkilon1Versiot.exists(v => v.tunniste == henkilon1Versio.tunniste))
    Assertions.assertTrue(henkilon1Versiot.exists(v => v.tunniste == henkilon1Versio2.tunniste))
    Assertions.assertEquals(henkilon1Versiot.size, 2)

    val henkilon2Versiot = this.kantaOperaatiot.haeHenkilonVersiot(HENKILONUMERO2)
    Assertions.assertTrue(henkilon2Versiot.exists(_.tunniste == henkilon2Versio.tunniste))
    Assertions.assertEquals(henkilon2Versiot.size, 1)

    val henkilonJollaEiDataaVersiot = this.kantaOperaatiot.haeHenkilonVersiot(HENKILONUMEROJOLLAEIDATAA)
    Assertions.assertTrue(henkilonJollaEiDataaVersiot.isEmpty)

  @Test def testTallennaVersio(): Unit =
    val HENKILONUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO1, SuoritusJoukko.YTR, Seq.empty, Seq.empty, Instant.now())

    val haettuVersio = this.kantaOperaatiot.haeVersio(versio.get.tunniste)
    Assertions.assertEquals(versio, haettuVersio)

  @Test def testPaataversionVoimassaolo(): Unit =
    val HENKILONUMERO1 = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO1, SuoritusJoukko.YTR, Seq.empty, Seq.empty, Instant.now())

    Thread.sleep(100)

    Assertions.assertTrue(this.kantaOperaatiot.paataVersionVoimassaolo(versio.get.tunniste))
    val loppu = this.kantaOperaatiot.haeVersio(versio.get.tunniste).get.loppu
    Assertions.assertTrue(loppu.isDefined)
    Assertions.assertTrue(loppu.get.isBefore(Instant.now()))
    Assertions.assertTrue(loppu.get.isAfter(Instant.now().minusMillis(1000)))

  @Test def testPaataversionVoimassaoloEiVaikutustaJoPaatettyyn(): Unit =
    val HENKILONUMERO = "1.2.246.562.24.99988877766"
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(HENKILONUMERO, SuoritusJoukko.YTR, Seq.empty, Seq.empty, Instant.now())

    Thread.sleep(100)

    Assertions.assertTrue(this.kantaOperaatiot.paataVersionVoimassaolo(versio.get.tunniste))
    val loppu1 = this.kantaOperaatiot.haeVersio(versio.get.tunniste).get.loppu

    Assertions.assertFalse(this.kantaOperaatiot.paataVersionVoimassaolo(versio.get.tunniste))
    val loppu2 = this.kantaOperaatiot.haeVersio(versio.get.tunniste).get.loppu

    Assertions.assertEquals(loppu1, loppu2)

  @Test def testHaeOhjattavatOppijat(): Unit =
    val henkiloNumero = "1.2.246.562.24.99988877766"
    val oppilaitosOid = "1.2.246.562.10.95136889433"
    val valmistumisVuosi = 2025
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloNumero, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now())

    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, Set.empty, Seq(
      Lahtokoulu(LocalDate.parse(s"${valmistumisVuosi-1}-08-18"), None, oppilaitosOid, Some(valmistumisVuosi), Some("9A"), Some(SuoritusTila.KESKEN), None, LahtokouluTyyppi.VUOSILUOKKA_9)
    ), 1);

    // henkilö näkyy koska suoritus kesken
    Assertions.assertEquals(Set((henkiloNumero, Some("9A"))), this.kantaOperaatiot.haeLahtokoulunOppilaat(Some(LocalDate.now), oppilaitosOid, valmistumisVuosi, None, false, false, Set(VUOSILUOKKA_9)))

  @Test def testHaeOhjattavatOppijatOppilaitosSiirto(): Unit =
    val henkiloNumero = "1.2.246.562.24.99988877766"
    val vanhaOppilaitosOid = "1.2.246.562.10.95136889433"
    val uusiOppilaitosOid = "1.2.246.562.10.95136889434"
    val valmistumisVuosi = 2025

    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloNumero, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now())
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, Set.empty, Seq(
      // ensimmäinen suoritus päättyy koulun vaihtoon
      Lahtokoulu(LocalDate.parse("2024-08-18"), Some(LocalDate.parse("2024-10-01")), vanhaOppilaitosOid, Some(valmistumisVuosi), Some("9A"), Some(SuoritusTila.KESKEYTYNYT), None, LahtokouluTyyppi.VUOSILUOKKA_9),
      // jälkimmäinen suoritus alkaa kun ensimmäinen loppuu
      Lahtokoulu(LocalDate.parse("2024-10-01"), None, uusiOppilaitosOid, Some(valmistumisVuosi), Some("9B"), Some(SuoritusTila.KESKEN), None, LahtokouluTyyppi.VUOSILUOKKA_9)
    ), 1)

    // henkilö näkyy joulukuussa edelleen molempien koulujen oppilaslistalla koska opolla oikeus tarkastaa tilanne seuraavan tammikuun loppuun
    Assertions.assertEquals(Set((henkiloNumero, Some("9A"))), this.kantaOperaatiot.haeLahtokoulunOppilaat(Some(LocalDate.parse("2024-12-01")), vanhaOppilaitosOid, valmistumisVuosi, None, false, false, Set(VUOSILUOKKA_9)))
    // ja luonnollisesti näkyy myös uuden koulun listalla
    Assertions.assertEquals(Set((henkiloNumero, Some("9B"))), this.kantaOperaatiot.haeLahtokoulunOppilaat(Some(LocalDate.parse("2024-12-01")), uusiOppilaitosOid, valmistumisVuosi, None, false, false, Set(VUOSILUOKKA_9)))

  @Test def testHaeOhjattavatOppijatSeuraavaHelmikuu(): Unit =
    val henkiloNumero = "1.2.246.562.24.99988877766"
    val vanhaOppilaitosOid = "1.2.246.562.10.95136889433"
    val uusiOppilaitosOid = "1.2.246.562.10.95136889434"
    val valmistumisVuosi = 2025

    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloNumero, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now())
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, Set.empty, Seq(
      // ensimmäinen suoritus päättyy koulun vaihtoon
      Lahtokoulu(LocalDate.parse("2024-08-18"), Some(LocalDate.parse("2024-10-01")), vanhaOppilaitosOid, Some(valmistumisVuosi), Some("9A"), Some(SuoritusTila.KESKEYTYNYT), None, LahtokouluTyyppi.VUOSILUOKKA_9),
      // jälkimmäinen suoritus alkaa kun ensimmäinen loppuu
      Lahtokoulu(LocalDate.parse("2024-10-01"), None, uusiOppilaitosOid, Some(valmistumisVuosi), Some("9B"), Some(SuoritusTila.KESKEN), None, LahtokouluTyyppi.VUOSILUOKKA_9)
    ), 1)

    // helmikuussa henkilö ei enää näy vanhan koulun listalla
    Assertions.assertEquals(Set.empty, this.kantaOperaatiot.haeLahtokoulunOppilaat(Some(LocalDate.parse("2025-02-01")), vanhaOppilaitosOid, valmistumisVuosi, None, false, false, Set(VUOSILUOKKA_9)))
    // mutta uudella listalla näkyy
    Assertions.assertEquals(Set((henkiloNumero, Some("9B"))), this.kantaOperaatiot.haeLahtokoulunOppilaat(Some(LocalDate.parse("2025-02-01")), uusiOppilaitosOid, valmistumisVuosi, None, false, false, Set(VUOSILUOKKA_9)))

  @Test def testHaeOhjattavatOppijatEiPvmRajausta(): Unit =
    val henkiloNumero = "1.2.246.562.24.99988877766"
    val vanhaOppilaitosOid = "1.2.246.562.10.95136889433"
    val uusiOppilaitosOid = "1.2.246.562.10.95136889434"
    val valmistumisVuosi = 2025

    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloNumero, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now())
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, Set.empty, Seq(
      // ensimmäinen suoritus päättyy koulun vaihtoon
      Lahtokoulu(LocalDate.parse("2024-08-18"), Some(LocalDate.parse("2024-10-01")), vanhaOppilaitosOid, Some(valmistumisVuosi), Some("9A"), Some(SuoritusTila.KESKEYTYNYT), None, LahtokouluTyyppi.VUOSILUOKKA_9),
      // jälkimmäinen suoritus alkaa kun ensimmäinen loppuu
      Lahtokoulu(LocalDate.parse("2024-10-01"), None, uusiOppilaitosOid, Some(valmistumisVuosi), Some("9B"), Some(SuoritusTila.KESKEN), None, LahtokouluTyyppi.VUOSILUOKKA_9)
    ), 1)

    // henkilö näkyy molemmilla listoilla koska ei tarkastella tiettyä ajanhetkeä, ja henkilö on ollut kummankin koulun tietyn valmistumisvuoden oppilas
    Assertions.assertEquals(Set((henkiloNumero, Some("9A"))), this.kantaOperaatiot.haeLahtokoulunOppilaat(None, vanhaOppilaitosOid, valmistumisVuosi, None, false, false, Set(VUOSILUOKKA_9)))
    Assertions.assertEquals(Set((henkiloNumero, Some("9B"))), this.kantaOperaatiot.haeLahtokoulunOppilaat(None, uusiOppilaitosOid, valmistumisVuosi, None, false, false, Set(VUOSILUOKKA_9)))

  @Test def haeLahtokoulutRoundTrip(): Unit = {
    val henkiloNumero = "1.2.246.562.24.99988877766"
    val oppilaitosOid = "1.2.246.562.10.95136889433"
    val valmistumisVuosi = 2025

    // tallennetaan versiot ja lähtökoulut
    val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloNumero, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now())
    val lahtokoulu = Lahtokoulu(LocalDate.parse("2024-08-18"), Some(LocalDate.parse("2024-10-01")), "1.2.246.562.10.95136889433", Some(2025), Some("9A"), Some(SuoritusTila.KESKEYTYNYT), None, LahtokouluTyyppi.VUOSILUOKKA_9)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, Set.empty, Seq(lahtokoulu), ParserVersions.KOSKI)

    // luettu vastaa tallennettua
    Assertions.assertEquals(Set(lahtokoulu), kantaOperaatiot.haeLahtokoulut(Set(henkiloNumero)))
  }

  @Test def haeLahtokoulutPerHenkilö(): Unit = {
    val henkiloNumero1 = "1.2.246.562.24.99988877766"
    val henkiloNumero2 = "1.2.246.562.24.99988877767"
    val oppilaitosOid1 = "1.2.246.562.10.95136889433"
    val uusiOppilaitosOid = "1.2.246.562.10.95136889434"
    val valmistumisVuosi = 2025

    // tallennetaan versiot ja lähtökoulut
    val versio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloNumero1, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now())
    val versio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloNumero2, SuoritusJoukko.KOSKI, Seq.empty, Seq.empty, Instant.now())
    val lahtokoulu1 = Lahtokoulu(LocalDate.parse("2024-08-18"), Some(LocalDate.parse("2024-10-01")), "1.2.246.562.10.95136889433", Some(2025), Some("9A"), Some(SuoritusTila.KESKEYTYNYT), None, LahtokouluTyyppi.VUOSILUOKKA_9)
    val lahtokoulu2 = Lahtokoulu(LocalDate.parse("2024-10-01"), None, "1.2.246.562.10.95136889434", Some(2025), Some("9B"), Some(SuoritusTila.KESKEN), None, LahtokouluTyyppi.VUOSILUOKKA_9)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio1.get, Set.empty, Seq(lahtokoulu1), ParserVersions.KOSKI)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio2.get, Set.empty, Seq(lahtokoulu2), ParserVersions.KOSKI)

    // luettu vastaa tallennettua
    Assertions.assertEquals(Set(lahtokoulu1), kantaOperaatiot.haeLahtokoulut(Set(henkiloNumero1)))
    Assertions.assertEquals(Set(lahtokoulu2), kantaOperaatiot.haeLahtokoulut(Set(henkiloNumero2)))
    Assertions.assertEquals(Set(lahtokoulu1, lahtokoulu2), kantaOperaatiot.haeLahtokoulut(Set(henkiloNumero1, henkiloNumero2)))
  }

  @Test def haeLahtokoulutVainViimeisimmaltaVersiolta(): Unit = {
    val henkiloNumero = "1.2.246.562.24.99988877766"
    val oppilaitosOid1 = "1.2.246.562.10.95136889433"
    val uusiOppilaitos2 = "1.2.246.562.10.95136889434"
    val valmistumisVuosi = 2025

    // tallennetaan ensimmäinen versio, lähtökoulu täsmää
    val versio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloNumero, SuoritusJoukko.KOSKI, Seq("{\"avain\": \"arvo1\"}"), Seq.empty, Instant.now())
    val lahtokoulu1 = Lahtokoulu(LocalDate.parse("2024-08-18"), Some(LocalDate.parse("2024-10-01")), "1.2.246.562.10.95136889433", Some(2025), Some("9A"), Some(SuoritusTila.KESKEYTYNYT), None, LahtokouluTyyppi.VUOSILUOKKA_9)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio1.get, Set.empty, Seq(lahtokoulu1), ParserVersions.KOSKI)
    Assertions.assertEquals(Set(lahtokoulu1), kantaOperaatiot.haeLahtokoulut(Set(henkiloNumero)))

    // tallennetaan toinen versio, lähtökoulu muuttuu
    val versio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio(henkiloNumero, SuoritusJoukko.KOSKI, Seq("{\"avain\": \"arvo2\"}"), Seq.empty, Instant.now())
    val lahtokoulu2 = Lahtokoulu(LocalDate.parse("2024-10-01"), None, "1.2.246.562.10.95136889434", Some(2025), Some("9B"), Some(SuoritusTila.KESKEN), None, LahtokouluTyyppi.VUOSILUOKKA_9)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio2.get, Set.empty, Seq(lahtokoulu2), ParserVersions.KOSKI)
    Assertions.assertEquals(Set(lahtokoulu2), kantaOperaatiot.haeLahtokoulut(Set(henkiloNumero)))

    // vanhan version parserointi uudelleen ei muuta lähtökouluja
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio1.get, Set.empty, Seq(lahtokoulu1), ParserVersions.KOSKI)
    Assertions.assertEquals(Set(lahtokoulu2), kantaOperaatiot.haeLahtokoulut(Set(henkiloNumero)))
  }

  @Test def testYliajoRoundtrip(): Unit = {
    val personOid = "1.2.246.562.24.12345678901"
    val hakuOid = "1.2.246.562.29.98765432109"
    val virkailijaOid = "1.2.246.562.24.11223344556"
    val luotu = Instant.now

    val avainArvoYliajo = AvainArvoYliajo(
      avain = "perusopetuksen_kieli",
      arvo = Some("FI"),
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Testiyliajon selite"
    )

    //Save and fetch
    this.kantaOperaatiot.tallennaYliajot(Seq(avainArvoYliajo))
    val haetutYliajot = this.kantaOperaatiot.haeHenkilonYliajot(personOid, hakuOid)

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
      arvo = Some("2022"),
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Ensimmäinen yliajo"
    )

    // Save first override
    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo1))

    // Verify it's saved correctly
    val haetutYliajot1 = this.kantaOperaatiot.haeHenkilonYliajot(personOid, hakuOid)
    Assertions.assertEquals(1, haetutYliajot1.size)
    Assertions.assertEquals(Some("2022"), haetutYliajot1.head.arvo)

    // Create second override for the same key
    val yliajo2 = AvainArvoYliajo(
      avain = avain,
      arvo = Some("2023"),
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Toinen yliajo"
    )

    // Save second override
    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo2))

    // Verify only the second override is active
    val haetutYliajot2 = this.kantaOperaatiot.haeHenkilonYliajot(personOid, hakuOid)
    Assertions.assertEquals(1, haetutYliajot2.size)
    Assertions.assertEquals(Some("2023"), haetutYliajot2.head.arvo)
    Assertions.assertEquals("Toinen yliajo", haetutYliajot2.head.selite)
  }

  @Test def testMultipleYliajosForDifferentKeys(): Unit = {
    val personOid = "1.2.246.562.24.12345678901"
    val hakuOid = "1.2.246.562.29.98765432109"
    val virkailijaOid = "1.2.246.562.24.11223344556"

    // Create overrides for different keys
    val yliajo1 = AvainArvoYliajo(
      avain = "perusopetuksen_kieli",
      arvo = Some("SV"),
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Kieliyliajo"
    )

    val yliajo2 = AvainArvoYliajo(
      avain = "perustutkinto_suoritettu",
      arvo = Some("true"),
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Suoritusyliajo"
    )

    // Save both overrides
    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo1, yliajo2))

    val haetutYliajot = this.kantaOperaatiot.haeHenkilonYliajot(personOid, hakuOid)
    Assertions.assertEquals(2, haetutYliajot.size)

    val yliajoMap = haetutYliajot.map(y => y.avain -> y).toMap

    //Verify avain, arvo and selite for both overrides
    Assertions.assertTrue(yliajoMap.contains("perusopetuksen_kieli"))
    Assertions.assertEquals(Some("SV"), yliajoMap("perusopetuksen_kieli").arvo)
    Assertions.assertEquals("Kieliyliajo", yliajoMap("perusopetuksen_kieli").selite)

    Assertions.assertTrue(yliajoMap.contains("perustutkinto_suoritettu"))
    Assertions.assertEquals(Some("true"), yliajoMap("perustutkinto_suoritettu").arvo)
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
      arvo = Some("FI"),
      henkiloOid = personOid1,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Henkilö 1 yliajo"
    )

    val yliajo2 = AvainArvoYliajo(
      avain = avain,
      arvo = Some("SV"),
      henkiloOid = personOid2,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = "Henkilö 2 yliajo"
    )

    // Save both overrides
    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo1, yliajo2))

    // Fetch overrides for person 1
    val haetutYliajot1 = this.kantaOperaatiot.haeHenkilonYliajot(personOid1, hakuOid)
    Assertions.assertEquals(1, haetutYliajot1.size)
    Assertions.assertEquals(Some("FI"), haetutYliajot1.head.arvo)
    Assertions.assertEquals("Henkilö 1 yliajo", haetutYliajot1.head.selite)

    // Fetch overrides for person 2
    val haetutYliajot2 = this.kantaOperaatiot.haeHenkilonYliajot(personOid2, hakuOid)
    Assertions.assertEquals(1, haetutYliajot2.size)
    Assertions.assertEquals(Some("SV"), haetutYliajot2.head.arvo)
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
      arvo = Some("FI"),
      henkiloOid = personOid,
      hakuOid = hakuOid1,
      virkailijaOid = virkailijaOid,
      selite = "Haku 1 yliajo"
    )

    val yliajo2 = AvainArvoYliajo(
      avain = avain,
      arvo = Some("SV"),
      henkiloOid = personOid,
      hakuOid = hakuOid2,
      virkailijaOid = virkailijaOid,
      selite = "Haku 2 yliajo"
    )

    // Save both overrides
    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo1, yliajo2))

    // Check haku 1
    val haetutYliajot1 = this.kantaOperaatiot.haeHenkilonYliajot(personOid, hakuOid1)
    Assertions.assertEquals(1, haetutYliajot1.size)
    Assertions.assertEquals(Some("FI"), haetutYliajot1.head.arvo)
    Assertions.assertEquals("Haku 1 yliajo", haetutYliajot1.head.selite)

    // Check haku 2
    val haetutYliajot2 = this.kantaOperaatiot.haeHenkilonYliajot(personOid, hakuOid2)
    Assertions.assertEquals(1, haetutYliajot2.size)
    Assertions.assertEquals(Some("SV"), haetutYliajot2.head.arvo)
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
      arvo = Some("FI"),
      henkiloOid = personOid,
      hakuOid = hakuOid1,
      virkailijaOid = virkailijaOid,
      selite = "Haku 1 yliajo"
    )

    val yliajo2 = AvainArvoYliajo(
      avain = avain,
      arvo = Some("SV"),
      henkiloOid = personOid,
      hakuOid = hakuOid2,
      virkailijaOid = virkailijaOid,
      selite = "Haku 2 yliajo"
    )

    this.kantaOperaatiot.tallennaYliajot(Seq(yliajo1, yliajo2))

    // Tarkistetaan haun 1 yliajo
    val haetutYliajot1 = this.kantaOperaatiot.haeHenkilonYliajot(personOid, hakuOid1)
    Assertions.assertEquals(1, haetutYliajot1.size)
    Assertions.assertEquals(Some("FI"), haetutYliajot1.head.arvo)
    Assertions.assertEquals("Haku 1 yliajo", haetutYliajot1.head.selite)

    // Tarkistetaan haun 2 yliajo
    val haetutYliajot2 = this.kantaOperaatiot.haeHenkilonYliajot(personOid, hakuOid2)
    Assertions.assertEquals(1, haetutYliajot2.size)
    Assertions.assertEquals(Some("SV"), haetutYliajot2.head.arvo)
    Assertions.assertEquals("Haku 2 yliajo", haetutYliajot2.head.selite)

    // Poistetaan yliajo haulta 2
    this.kantaOperaatiot.poistaYliajo(personOid, hakuOid2, avain, virkailijaOid, "ei huvita enää yliajaa")

    // Tarkistetaan että haun 1 yliajo edelleen voimassa
    val haetutYliajot1After = this.kantaOperaatiot.haeHenkilonYliajot(personOid, hakuOid1)
    Assertions.assertEquals(1, haetutYliajot1After.size)
    Assertions.assertEquals(Some("FI"), haetutYliajot1After.head.arvo)
    Assertions.assertEquals("Haku 1 yliajo", haetutYliajot1After.head.selite)

    // Tarkistetaan että haulle 2 ei enää yliajoa
    val haetutYliajot2After = this.kantaOperaatiot.haeHenkilonYliajot(personOid, hakuOid2)
    Assertions.assertEquals(None, haetutYliajot2After.head.arvo)
  }

  @Test def testHaeYliajoMuutokset(): Unit = {
    val personOid = "1.2.246.562.24.12345678901"
    val hakuOid = "1.2.246.562.29.98765432109"
    val virkailijaOid = "1.2.246.562.24.11223344556"
    val avain = "perusopetuksen_kieli"
    val luomisHetki = Instant.ofEpochMilli((Instant.now.minusSeconds(1).toEpochMilli/1000)*1000)
    val poistoHetki = Instant.ofEpochMilli((Instant.now.toEpochMilli/1000)*1000)
    val luomisselite = "haluan yliajaa"
    val poistoselite = "ei huvita enää yliajaa"

    this.kantaOperaatiot.tallennaYliajot(Seq(AvainArvoYliajo(
      avain = avain,
      arvo = Some("FI"),
      henkiloOid = personOid,
      hakuOid = hakuOid,
      virkailijaOid = virkailijaOid,
      selite = luomisselite
    )), luomisHetki)
    this.kantaOperaatiot.poistaYliajo(personOid, hakuOid, avain, virkailijaOid, poistoselite, poistoHetki)

    Assertions.assertEquals(Seq(
      AvainArvoYliajoMuutos(None, poistoHetki, virkailijaOid, poistoselite),
      AvainArvoYliajoMuutos(Some("FI"), luomisHetki, virkailijaOid, luomisselite)
    ), this.kantaOperaatiot.haeYliajoMuutokset(personOid, hakuOid, avain))
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

  @Test
  def testHaeSuorituksetAjanhetkella(): Unit = {
    val personOid = "2.3.4"
    val oppilaitosOid = "1.2.246.562.10.12345678900"

    // tallennetaan versio ja suoritukset
    val versio1 = this.kantaOperaatiot.tallennaJarjestelmaVersio(personOid, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value111\"}"), Seq.empty, Instant.now()).get
    val suoritus1 = PerusopetuksenOppimaara(
      UUID.randomUUID(), None,
      Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
      None, Koodi("arvo", "koodisto", Some(1)),
      SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)),
      Set.empty, None, None, None,
      Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None),
        Koodi("AI", "koodisto", None), Koodi("8", "koodisto", None),
        Some(Koodi("FI", "kielivalikoima", None)), true, None, None)),
      Set.empty,
      false,
      false
    )

    val lisatiedot1 = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain = Some(true)))), None, None)
    val opiskeluoikeus1 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("opiskeluoikeusOid"), oppilaitosOid, Set(suoritus1), Some(lisatiedot1), VALMIS, List.empty)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio1, Set(opiskeluoikeus1), Seq.empty, ParserVersions.KOSKI)

    val ekaVersioTallennettuna = Instant.now

    Thread.sleep(100)

    // tallennetaan toinen versio ja suoritukset
    val versio2 = this.kantaOperaatiot.tallennaJarjestelmaVersio(personOid, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value222\"}"), Seq.empty, Instant.now()).get
    val suoritus2 = PerusopetuksenOppimaara(
      UUID.randomUUID(), None,
      Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
      None, Koodi("arvo", "koodisto", Some(1)),
      SuoritusTila.KESKEN, Koodi("arvo", "koodisto", Some(1)),
      Set.empty, None, None, None,
      Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None),
        Koodi("AI", "koodisto", None), Koodi("8", "koodisto", None),
        Some(Koodi("FI", "kielivalikoima", None)), true, None, None),
        PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("matematiikka"), None, None),
          Koodi("MA", "koodisto", None), Koodi("7", "koodisto", None),
          None, true, None, None)),
      Set.empty,
      false,
      false
    )
    val lisatiedot2 = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain = Some(true)))), None, None)
    val opiskeluoikeus2 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("opiskeluoikeusOid"), oppilaitosOid, Set(suoritus2), Some(lisatiedot2), VALMIS, List.empty)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio2, Set(opiskeluoikeus2), Seq.empty, ParserVersions.KOSKI)

    val tokaVersioTallennettuna = Instant.now

    Thread.sleep(100)


    // tallennetaan kolmas versio ja suoritukset
    val versio3 = this.kantaOperaatiot.tallennaJarjestelmaVersio(personOid, SuoritusJoukko.KOSKI, Seq("{\"attr\": \"value333\"}"), Seq.empty, Instant.now()).get
    val suoritus3 = PerusopetuksenOppimaara(
      UUID.randomUUID(), None,
      Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
      None, Koodi("arvo", "koodisto", Some(1)),
      SuoritusTila.VALMIS, Koodi("arvo", "koodisto", Some(1)),
      Set.empty, None, None, None,
      Set(PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("äidinkieli"), None, None),
        Koodi("AI", "koodisto", None), Koodi("8", "koodisto", None),
        Some(Koodi("FI", "kielivalikoima", None)), true, None, None),
        PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("matematiikka"), None, None),
          Koodi("MA", "koodisto", None), Koodi("7", "koodisto", None),
          None, true, None, None),
        PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(Some("englanti"), None, None),
          Koodi("EN", "koodisto", None), Koodi("9", "koodisto", None),
          Some(Koodi("EN", "kielivalikoima", None)), true, None, None)),
      Set.empty,
      false,
      false
    )
    val lisatiedot3 = KoskiLisatiedot(Some(List(KoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain = Some(true)))), None, None)
    val opiskeluoikeus3 = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("opiskeluoikeusOid"), oppilaitosOid, Set(suoritus3), Some(lisatiedot3), VALMIS, List.empty)
    this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio3, Set(opiskeluoikeus3), Seq.empty, ParserVersions.KOSKI)

    val kolmasVersioTallennettuna = Instant.now

    Thread.sleep(100)

    //Haetaan uusin sekä voimassaolleet versiot kolmelta ajanhetkeltä
    val uusin = this.kantaOperaatiot.haeSuoritukset(personOid)
    val eka = this.kantaOperaatiot.haeSuorituksetAjanhetkella(personOid, ekaVersioTallennettuna)
    val toka = this.kantaOperaatiot.haeSuorituksetAjanhetkella(personOid, tokaVersioTallennettuna)
    val kolmas = this.kantaOperaatiot.haeSuorituksetAjanhetkella(personOid, kolmasVersioTallennettuna)

    Assertions.assertNotEquals(eka.head._1.tunniste, toka.head._1.tunniste)
    Assertions.assertNotEquals(toka.head._1.tunniste, kolmas.head._1.tunniste)

    // Tarkistetaan että uusin versio == kolmasVersio
    Assertions.assertEquals(1, uusin.size)
    Assertions.assertTrue(uusin.head._1.tunniste.equals(versio3.tunniste))
    Assertions.assertEquals(Set(opiskeluoikeus3), uusin.head._2)

    // Tarkistetaan että ensimmäisellä ajanhetkellä tallennettuna ollut versio == versio1
    Assertions.assertEquals(1, eka.size)
    println(s"eka: ${eka.head._1}")
    println(s"v1: ${versio1}")
    Assertions.assertTrue(eka.head._1.tunniste.equals(versio1.tunniste))
    Assertions.assertEquals(Set(opiskeluoikeus1), eka.head._2)

    // Tarkistetaan että toisella ajanhetkellä tallennettuna ollut versio == versio2
    Assertions.assertEquals(1, toka.size)
    Assertions.assertTrue(toka.head._1.tunniste.equals(versio2.tunniste))
    Assertions.assertEquals(Set(opiskeluoikeus2), toka.head._2)

    // Tarkistetaan että kolmannella ajanhetkellä tallennettuna ollut versio == versio3
    Assertions.assertEquals(1, kolmas.size)
    Assertions.assertTrue(kolmas.head._1.tunniste.equals(versio3.tunniste))
    Assertions.assertEquals(Set(opiskeluoikeus3), kolmas.head._2)

    // Varmistetaan että oppiaineiden määrät täsmäävät versioittain
    Assertions.assertEquals(1,
      eka.head._2
        .collect { case oo: PerusopetuksenOpiskeluoikeus => oo }
        .flatMap(o => o.suoritukset)
        .collect { case s: PerusopetuksenOppimaara => s }
        .flatMap(_.aineet)
        .size)

    Assertions.assertEquals(2,
      toka.head._2
        .collect { case oo: PerusopetuksenOpiskeluoikeus => oo }
        .flatMap(o => o.suoritukset)
        .collect { case s: PerusopetuksenOppimaara => s }
        .flatMap(_.aineet)
        .size)

    Assertions.assertEquals(3,
      kolmas.head._2
        .collect { case oo: PerusopetuksenOpiskeluoikeus => oo }
        .flatMap(o => o.suoritukset)
        .collect { case s: PerusopetuksenOppimaara => s }
        .flatMap(_.aineet)
        .size)
  }
}
