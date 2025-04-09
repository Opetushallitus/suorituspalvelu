package fi.oph.suorituspalvelu

import com.nimbusds.jose.util.StandardCharset
import fi.oph.suorituspalvelu.resource.{ApiConstants, LuoSuoritusFailureResponse, LuoSuoritusSuccessResponse, Suoritus}
import fi.oph.suorituspalvelu.validation.SuoritusValidator
import org.junit.jupiter.api.*
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.util.Optional

/**
 * Suoritusapin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta.
 */
class IntegraatioTest extends BaseIntegraatioTesti {

  def getSuoritus(): Suoritus =
    Suoritus(Optional.of("1.2.3"), Optional.of("Kurssi 1"))

  /**
   * Testataan healthcheck-toiminnallisuus
   */
  @WithAnonymousUser
  @Test def testHealthCheckAnonymous(): Unit =
    // tuntematon käyttäjä blokataan
    mvc.perform(MockMvcRequestBuilders
      .get(ApiConstants.HEALTHCHECK_PATH))
      .andExpect(status().isOk())
      .andExpect(MockMvcResultMatchers.content().string("OK"));

  @WithMockUser(value = "kayttaja")
  @Test def testHealthCheckOk(): Unit =
    // healthcheck palauttaa aina ok
    mvc.perform(MockMvcRequestBuilders
      .get(ApiConstants.HEALTHCHECK_PATH))
      .andExpect(status().isOk())
      .andExpect(MockMvcResultMatchers.content().string("OK"));

  /**
   * Testataan lähetyksen luonti
   */
  @WithAnonymousUser
  @Test def testLuoLahetysAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.SUORITUS_PATH, getSuoritus()))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testLuoSuoritusAllowed(): Unit =
    val suoritus = getSuoritus()
    val result = mvc.perform(jsonPost(ApiConstants.SUORITUS_PATH, suoritus))
      .andExpect(status().isOk()).andReturn()

    val luoSuoritusResponse = objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[LuoSuoritusSuccessResponse])

    // varmistetaan että kentät tulevat kantaan oikein
    val tallennettuSuoritus = kantaOperaatiot.haeSuoritus(luoSuoritusResponse.tunniste)
    val entiteetti = fi.oph.suorituspalvelu.business.Suoritus(
      luoSuoritusResponse.tunniste,
      suoritus.oppijaNumero.get
    )
    Assertions.assertEquals(Some(entiteetti), tallennettuSuoritus)


  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testLuoSuoritusMalformedJson(): Unit =
    // ei validi json ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.SUORITUS_PATH, "tämä ei ole lähetys-json-objekti"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LuoSuoritusFailureResponse(java.util.List.of(ApiConstants.VIRHEELLINEN_SUORITUS_JSON_VIRHE)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[LuoSuoritusFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testLuoLahetysInvalidRequest(): Unit =
    // tyhjä otsikko ei (esimerkiksi) ole sallittu, muuten validointi testataan yksikkötesteillä
    val result = mvc.perform(jsonPost(ApiConstants.SUORITUS_PATH, Suoritus(Optional.of(""), Optional.of(""))))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LuoSuoritusFailureResponse(java.util.List.of(SuoritusValidator.VALIDATION_OPPIJANUMERO_TYHJA)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[LuoSuoritusFailureResponse]))
}
