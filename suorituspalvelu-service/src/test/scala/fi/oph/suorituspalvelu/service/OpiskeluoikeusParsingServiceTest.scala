package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.BaseIntegraatioTesti
import fi.oph.suorituspalvelu.business.{ParserVersions, SuoritusJoukko, VersioEntiteetti}
import org.junit.jupiter.api.{Assertions, Test}
import org.springframework.beans.factory.annotation.Autowired
import slick.jdbc.PostgresProfile.api.*

import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class OpiskeluoikeusParsingServiceTest extends BaseIntegraatioTesti {

  @Autowired var opiskeluoikeusParsingService: OpiskeluoikeusParsingService = null

  val OPPIJA_OID = "1.2.246.562.24.12345678901"

  // Yksinkertainen KOSKI-data testejä varten
  val KOSKI_JSON = "[]"

  /**
   * Kun parserVersio on None, palvelu parseroi ja tallentaa tuloksen.
   */
  @Test def testParseWhenNoVersionStored(): Unit =
    // Tallennetaan versio ilman parserointia
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJA_OID, SuoritusJoukko.KOSKI, Seq(KOSKI_JSON), Seq.empty, Instant.now(), "KOSKI", None).get

    // Varmistetaan että parserVersio on None
    val versioBeforeParse = kantaOperaatiot.haeVersio(versio.tunniste)
    Assertions.assertTrue(versioBeforeParse.get.parserVersio.isEmpty, "parserVersio pitäisi olla None ennen parserausta")

    // Kutsutaan palvelua joka parsii on-demand
    val result = opiskeluoikeusParsingService.haeSuoritukset(OPPIJA_OID)

    // Varmistetaan että parserVersio on nyt tallennettu
    val versioAfterParse = kantaOperaatiot.haeVersio(versio.tunniste)
    Assertions.assertTrue(versioAfterParse.get.parserVersio.isDefined, "parserVersio pitäisi olla tallennettu parseroinnin jälkeen")
    Assertions.assertEquals(ParserVersions.KOSKI, versioAfterParse.get.parserVersio.get)

  /**
   * Kun tallennettu parserVersio on vanhempi kuin nykyinen, palvelu parseroi uudelleen ja tallentaa.
   */
  @Test def testReparseWhenOlderVersionStored(): Unit =
    // Tallennetaan versio vanhalla parserVersiolla
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJA_OID, SuoritusJoukko.KOSKI, Seq(KOSKI_JSON), Seq.empty, Instant.now(), "KOSKI", None).get
    val oldParserVersion = ParserVersions.KOSKI - 1

    // Päivitetään parserVersio vanhaksi suoraan kantaan
    Await.result(database.run(
      sqlu"""UPDATE versiot SET parser_versio = $oldParserVersion WHERE tunniste = ${versio.tunniste.toString}::uuid"""
    ), 5.seconds)

    // Varmistetaan että vanha versio on tallennettu
    val versioBeforeParse = kantaOperaatiot.haeVersio(versio.tunniste)
    Assertions.assertEquals(Some(oldParserVersion), versioBeforeParse.get.parserVersio)

    // Kutsutaan palvelua joka parsii uudelleen
    val result = opiskeluoikeusParsingService.haeSuoritukset(OPPIJA_OID)

    // Varmistetaan että parserVersio on päivitetty uuteen
    val versioAfterParse = kantaOperaatiot.haeVersio(versio.tunniste)
    Assertions.assertEquals(Some(ParserVersions.KOSKI), versioAfterParse.get.parserVersio)

  /**
   * Kun tallennettu parserVersio on uudempi kuin nykyinen (deployment-tilanne),
   * palvelu parsii mutta EI tallenna tulosta.
   */
  @Test def testParseWithoutStoreWhenNewerVersionStored(): Unit =
    // Tallennetaan versio uudemmalla parserVersiolla (simuloi deployment-tilannetta)
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJA_OID, SuoritusJoukko.KOSKI, Seq(KOSKI_JSON), Seq.empty, Instant.now(), "KOSKI", None).get
    val newerParserVersion = ParserVersions.KOSKI + 1

    // Päivitetään parserVersio uudemmaksi suoraan kantaan
    Await.result(database.run(
      sqlu"""UPDATE versiot SET parser_versio = $newerParserVersion WHERE tunniste = ${versio.tunniste.toString}::uuid"""
    ), 5.seconds)

    // Varmistetaan että uudempi versio on tallennettu
    val versioBeforeParse = kantaOperaatiot.haeVersio(versio.tunniste)
    Assertions.assertEquals(Some(newerParserVersion), versioBeforeParse.get.parserVersio)

    // Kutsutaan palvelua
    val result = opiskeluoikeusParsingService.haeSuoritukset(OPPIJA_OID)

    // Varmistetaan että parserVersio EI muuttunut (uudempaa ei ylikirjoiteta)
    val versioAfterParse = kantaOperaatiot.haeVersio(versio.tunniste)
    Assertions.assertEquals(Some(newerParserVersion), versioAfterParse.get.parserVersio,
      "Uudempaa parserVersiota ei saa ylikirjoittaa")

  /**
   * Kun tallennettu parserVersio vastaa nykyistä, käytetään tallennettuja opiskeluoikeuksia.
   */
  @Test def testUseStoredDataWhenVersionsMatch(): Unit =
    // Tallennetaan ja parseroidaan versio normaalisti
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJA_OID, SuoritusJoukko.KOSKI, Seq(KOSKI_JSON), Seq.empty, Instant.now(), "KOSKI", None).get
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, Set.empty, Seq.empty, ParserVersions.KOSKI)

    // Varmistetaan että nykyinen versio on tallennettu
    val versioBeforeFetch = kantaOperaatiot.haeVersio(versio.tunniste)
    Assertions.assertEquals(Some(ParserVersions.KOSKI), versioBeforeFetch.get.parserVersio)

    // Kutsutaan palvelua
    val result = opiskeluoikeusParsingService.haeSuoritukset(OPPIJA_OID)

    // Varmistetaan että versio ei muuttunut
    val versioAfterFetch = kantaOperaatiot.haeVersio(versio.tunniste)
    Assertions.assertEquals(Some(ParserVersions.KOSKI), versioAfterFetch.get.parserVersio)

    // Palvelu palautti tuloksen
    Assertions.assertEquals(1, result.size)
}
