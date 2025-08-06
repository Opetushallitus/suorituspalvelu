package fi.oph.suorituspalvelu

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, Koodi, Oppilaitos, YOOpiskeluoikeus, YOTutkinto}
import fi.oph.suorituspalvelu.business.Tietolahde.{KOSKI, YTR}
import fi.oph.suorituspalvelu.integration.virta.VirtaClient
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.parsing.koski.KoskiToSuoritusConverter.SUORITYSTYYPPI_AMMATILLINENTUTKINTO
import fi.oph.suorituspalvelu.resource.ApiConstants.{LEGACY_SUORITUKSET_HAKU_EPAONNISTUI, LEGACY_SUORITUKSET_HENKILO_PARAM_NAME, LEGACY_SUORITUKSET_JOKO_OID_TAI_PVM_PAKOLLINEN, LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME}
import fi.oph.suorituspalvelu.resource.{ApiConstants, LegacyAmmatillinenTaiYOSuoritus, LegacyMuuttunutSuoritus, LegacySuorituksetFailureResponse, VirtaSyncFailureResponse, VirtaSyncSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.validation.Validator
import fi.oph.suorituspalvelu.validation.Validator.{VALIDATION_MUOKATTUJALKEEN_EI_VALIDI, VALIDATION_OPPIJANUMERO_EI_VALIDI}
import org.junit.jupiter.api.*
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.system.{CapturedOutput, OutputCaptureRule}
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.{Instant, LocalDate}
import java.util.{Optional, UUID}

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

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(LEGACY_SUORITUKSET_JOKO_OID_TAI_PVM_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LegacySuorituksetFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetBothParameters(): Unit =
    // vain toinen parametreista saa olla määritelty
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_SUORITUKSET_PATH + "?" +
        LEGACY_SUORITUKSET_HENKILO_PARAM_NAME + "=1.2.3" + "&" + LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME + "=aikaleima"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(LEGACY_SUORITUKSET_JOKO_OID_TAI_PVM_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LegacySuorituksetFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetMalformedOid(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_SUORITUKSET_PATH + "?" + LEGACY_SUORITUKSET_HENKILO_PARAM_NAME + "=tämä ei ole validi oid"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(VALIDATION_OPPIJANUMERO_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LegacySuorituksetFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetMalformedTimestamp(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_SUORITUKSET_PATH + "?" + LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME + "=tämä ei ole validi timestamp"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(VALIDATION_MUOKATTUJALKEEN_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LegacySuorituksetFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetHenkilolleAllowed(capturedOutput: CapturedOutput): Unit =
    val tutkintoKoodi = "123456"

    // tallennetaan ammatillinen- ja yo-tutkinto
    val koskiVersio = kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJA_OID, KOSKI, "{\"testi\": \"suorituksetHenkilölle\"}")
    val ammatillinenTutkinto = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi(tutkintoKoodi, "koulutus", Some(1)), Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Koodi("valmistunut", "jokutila", Some(1)), Some(LocalDate.now()), Some(LocalDate.now()), None, Koodi("tapa", "suoritustapa", Some(1)), Koodi("kieli", "suorituskieli", Some(1)), Set.empty)
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(koskiVersio.get, Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Set(ammatillinenTutkinto), None)), Set.empty)

    val ytrVersio = kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJA_OID, YTR, "{\"testi\": \"suorituksetHenkilölle\"}")
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(ytrVersio.get, Set(YOOpiskeluoikeus(UUID.randomUUID(), YOTutkinto(UUID.randomUUID(), Koodi("fi", "kieli", Some(1))))), Set.empty)

    // haetaan tutkinnot legacy-rajapinnasta
    val result = mvc.perform(MockMvcRequestBuilders.get(getHenkiloPath(OPPIJA_OID)))
      .andExpect(status().isOk).andReturn()
    val legacySuorituksetResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), new TypeReference[Seq[LegacyAmmatillinenTaiYOSuoritus]] {})

    // varmistetaan että tulokset täsmäävät
    Assertions.assertEquals(Seq(
      LegacyAmmatillinenTaiYOSuoritus(OPPIJA_OID, SUORITYSTYYPPI_AMMATILLINENTUTKINTO, Optional.of(tutkintoKoodi), "VALMIS"),
      LegacyAmmatillinenTaiYOSuoritus(OPPIJA_OID, "yotutkinto", Optional.empty, "VALMIS")
    ), legacySuorituksetResponse)

    // ja että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeYoTaiAmmatillinenTutkintoTiedot.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.LEGACY_SUORITUKSET_HENKILO_PARAM_NAME -> OPPIJA_OID), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetMuuttuneetAllowed(): Unit =
    val aikaleima = Instant.now()
    
    // tallennetaan suoritus
    val koskiVersio = kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJA_OID, KOSKI, "{\"testi\": \"muuttuneetSuoritukset\"}")
    val ammatillinenTutkinto = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi("123456", "koulutus", Some(1)), Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Koodi("valmistunut", "jokutila", Some(1)), Some(LocalDate.now()), Some(LocalDate.now()), None, Koodi("tapa", "suoritustapa", Some(1)), Koodi("kieli", "suorituskieli", Some(1)), Set.empty)
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(koskiVersio.get, Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), OPPIJA_OID, Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Set(ammatillinenTutkinto), None)), Set.empty)

    // haetaan muuttuneet legacy-rajapinnasta
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_SUORITUKSET_PATH + "?" + LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME + "=" + aikaleima.toString))
      .andExpect(status().isOk).andReturn()
    val legacySuorituksetResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), new TypeReference[Seq[LegacyMuuttunutSuoritus]] {})

    // varmistetaan että tulokset täsmäävät
    Assertions.assertEquals(Seq(LegacyMuuttunutSuoritus(OPPIJA_OID)), legacySuorituksetResponse)

    // ja että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeKoskiTaiYTRMuuttuneet.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME -> aikaleima.toString), auditLogEntry.target)

}
