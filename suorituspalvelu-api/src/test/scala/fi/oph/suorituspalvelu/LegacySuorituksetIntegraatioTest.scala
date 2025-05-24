package fi.oph.suorituspalvelu

import com.fasterxml.jackson.core.`type`.TypeReference
import com.nimbusds.jose.util.StandardCharset
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenTutkinto, Koodi}
import fi.oph.suorituspalvelu.business.Tietolahde.KOSKI
import fi.oph.suorituspalvelu.integration.virta.VirtaClient
import fi.oph.suorituspalvelu.parsing.koski.KoskiToSuoritusConverter.SUORITYSTYYPPI_AMMATILLINENTUTKINTO
import fi.oph.suorituspalvelu.resource.ApiConstants.{JOKO_OID_TAI_PVM_PITAA_OLLA_ANNETTU, LEGACY_SUORITUKSET_HENKILO_PARAM_NAME, LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME}
import fi.oph.suorituspalvelu.resource.{ApiConstants, LegacySuorituksetFailureResponse, LegacySuoritus, VirtaSyncFailureResponse, VirtaSyncSuccessResponse}
import fi.oph.suorituspalvelu.security.SecurityConstants
import fi.oph.suorituspalvelu.validation.Validator
import fi.oph.suorituspalvelu.validation.Validator.{VALIDATION_MUOKATTUJALKEEN_EI_VALIDI, VALIDATION_OPPIJANUMERO_EI_VALIDI}
import org.junit.jupiter.api.*
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.time.LocalDate

/**
 * Legacy-suoritusapin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta.
 */
class LegacySuorituksetIntegraatioTest extends BaseIntegraatioTesti {

  val OPPIJA_OID = "1.2.246.562.24.40483869857"

  def getHenkiloPath(oid: String = OPPIJA_OID): String =
    ApiConstants.LEGACY_SUORITUKSET_PATH + "?" + LEGACY_SUORITUKSET_HENKILO_PARAM_NAME + "=" + oid

  @WithAnonymousUser
  @Test def testLegacySuorituksetAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders.get(getHenkiloPath()))
      .andExpect(status().is3xxRedirection)

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testLegacySuorituksetNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders.get(getHenkiloPath()))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetMissingParameters(): Unit =
    // jompi kumpi parametreista pitää olla määritelty
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_SUORITUKSET_PATH))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(JOKO_OID_TAI_PVM_PITAA_OLLA_ANNETTU)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[LegacySuorituksetFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetBothParameters(): Unit =
    // vain toinen parametreista saa olla määritelty
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_SUORITUKSET_PATH + "?" +
        LEGACY_SUORITUKSET_HENKILO_PARAM_NAME + "=1.2.3" + "&" + LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME + "=aikaleima"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(JOKO_OID_TAI_PVM_PITAA_OLLA_ANNETTU)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[LegacySuorituksetFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetMalformedOid(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_SUORITUKSET_PATH + "?" + LEGACY_SUORITUKSET_HENKILO_PARAM_NAME + "=tämä ei ole validi oid"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(VALIDATION_OPPIJANUMERO_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[LegacySuorituksetFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetMalformedTimestamp(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_SUORITUKSET_PATH + "?" + LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME + "=tämä ei ole validi timestamp"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(VALIDATION_MUOKATTUJALKEEN_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[LegacySuorituksetFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetAllowed(): Unit =
    val tutkintoKoodi = "123456"
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJA_OID, KOSKI, "{}")
    val ammatillinenTutkinto = AmmatillinenTutkinto("diplomi", Koodi(tutkintoKoodi, "koulutus", 1), Koodi("valmistunut", "jokutila", 1), Some(LocalDate.now()), None, Koodi("tapa", "suoritustapa", 1), Set.empty)
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, Set(AmmatillinenOpiskeluoikeus("1.2.3", "2.3.4", Seq(ammatillinenTutkinto), None)), Set.empty)

    val result = mvc.perform(MockMvcRequestBuilders.get(getHenkiloPath(OPPIJA_OID)))
      .andExpect(status().isOk).andReturn()

    val legacySuorituksetResponse = objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), new TypeReference[Seq[LegacySuoritus]] {})

    Assertions.assertEquals(Seq(ammatillinenTutkinto).map(t => LegacySuoritus(OPPIJA_OID, SUORITYSTYYPPI_AMMATILLINENTUTKINTO, tutkintoKoodi, "VALMIS")), legacySuorituksetResponse)
}
