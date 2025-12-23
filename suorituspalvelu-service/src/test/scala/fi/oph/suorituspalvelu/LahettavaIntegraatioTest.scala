package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.VUOSILUOKKA_9
import fi.oph.suorituspalvelu.business.SuoritusJoukko.KOSKI
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, Koodi, Lahtokoulu, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusJoukko, SuoritusTila}
import fi.oph.suorituspalvelu.integration.client.*
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiUtil}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.api.{LahettavatHenkilo, LahettavatHenkilotSuccessResponse, LahettavatLuokatFailureResponse, LahettavatLuokatSuccessResponse}
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.service.UIService
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import fi.oph.suorituspalvelu.validation.{UIValidator, Validator}
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.context.bean.`override`.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.{Instant, LocalDate}
import java.util
import java.util.{Optional, UUID}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

/**
 * Lähettävät-apin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta.
 */
class LahettavaIntegraatioTest extends BaseIntegraatioTesti {

  @MockitoBean
  val onrIntegration: OnrIntegration = null

  @MockitoBean
  val organisaatioProvider: OrganisaatioProvider = null

  @MockitoBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  val OPPILAITOS_OID = "1.2.246.562.10.52320123196"

  /*
   * Integraatiotestit luokkien haulle
   */

  @WithAnonymousUser
  @Test def testHaeLuokatAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_LUOKAT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "2025"), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeLuokatEiOikeuksia(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_LUOKAT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "2025"), ""))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeLuokatInvalidParams(): Unit =
    // haetaan virheellisillä parametreilla
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_LUOKAT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, "Tämä ei ole validi oid")
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "Tämä ei ole validi vuosi"), ""))
      .andExpect(status().isBadRequest).andReturn()

    // virhe on kuten pitää
    Assertions.assertEquals(LahettavatLuokatFailureResponse(java.util.Set.of(
        Validator.VALIDATION_OPPILAITOSOID_EI_VALIDI + "Tämä ei ole validi oid",
        Validator.VALIDATION_VUOSI_EI_VALIDI + "Tämä ei ole validi vuosi")
      ),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LahettavatLuokatFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeLuokatAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21583363331"
    val valmistumisvuosi = 2025

    // tallennetaan valmis perusopetuksen oppimäärä ja vuosiluokka
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      OPPILAITOS_OID,
      Set(
        PerusopetuksenOppimaara(
          UUID.randomUUID(),
          None,
          fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), OPPILAITOS_OID),
          None,
          Koodi("", "", None),
          VALMIS,
          Koodi("", "", None),
          Set.empty,
          None,
          None,
          Some(LocalDate.parse(s"$valmistumisvuosi-06-01")),
          Set.empty,
          Set(Lahtokoulu(LocalDate.parse(s"${valmistumisvuosi-1}-08-01"), Some(LocalDate.parse(s"$valmistumisvuosi-06-01")), OPPILAITOS_OID, Some(valmistumisvuosi), Some("9A"), Some(VALMIS), None, VUOSILUOKKA_9)),
          false
        )
      ),
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet))

    // haetaan luokat
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_LUOKAT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, valmistumisvuosi.toString), ""))
      .andExpect(status().isOk).andReturn()

    // vastaa (toistaiseksi hardkoodattua) luokkaa
    Assertions.assertEquals(LahettavatLuokatSuccessResponse(List("9A").asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LahettavatLuokatSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeLuokatLahettava.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_NAME -> OPPILAITOS_OID,
      ApiConstants.LAHETTAVAT_VUOSI_PARAM_NAME -> valmistumisvuosi.toString
    ), auditLogEntry.target)

  /*
   * Integraatiotestit luokkien haulle
   */

  @WithAnonymousUser
  @Test def testHaeHenkilotAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_HENKILOT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "2025"), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeHenkilotEiOikeuksia(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_HENKILOT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "2025"), ""))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeHenkilotInvalidParams(): Unit =
    // haetaan virheellisillä parametreilla
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_HENKILOT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, "Tämä ei ole validi oid")
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "Tämä ei ole validi vuosi"), ""))
      .andExpect(status().isBadRequest).andReturn()

    // virhe on kuten pitää
    Assertions.assertEquals(LahettavatLuokatFailureResponse(java.util.Set.of(
      Validator.VALIDATION_OPPILAITOSOID_EI_VALIDI + "Tämä ei ole validi oid",
      Validator.VALIDATION_VUOSI_EI_VALIDI + "Tämä ei ole validi vuosi")
    ),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LahettavatLuokatFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeHenkilotAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21583363331"
    val valmistumisVuosi = 2025

    // tallennetaan valmis perusopetuksen oppimäärä ja vuosiluokka
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      OPPILAITOS_OID,
      Set(
        PerusopetuksenOppimaara(
          UUID.randomUUID(),
          None,
          fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), OPPILAITOS_OID),
          None,
          Koodi("", "", None),
          VALMIS,
          Koodi("", "", None),
          Set.empty,
          None,
          None,
          Some(LocalDate.parse(s"$valmistumisVuosi-08-18")),
          Set.empty,
          Set(Lahtokoulu(LocalDate.parse(s"${valmistumisVuosi-1}-08-18"), Some(LocalDate.parse(s"$valmistumisVuosi-06-01")), OPPILAITOS_OID, Some(valmistumisVuosi), Some("9A"), Some(VALMIS), None, VUOSILUOKKA_9)),
          false
        )
      ),
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet))

    // haetaan luokat
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_HENKILOT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, valmistumisVuosi.toString), ""))
      .andExpect(status().isOk).andReturn()

    // henkilön luokka vastaa (toistaiseksi hardkoodattua) luokkaa
    Assertions.assertEquals(LahettavatHenkilotSuccessResponse(List(LahettavatHenkilo(oppijaNumero, "9A")).asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LahettavatHenkilotSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeHenkilotLahettava.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_NAME -> OPPILAITOS_OID,
      ApiConstants.LAHETTAVAT_VUOSI_PARAM_NAME -> valmistumisVuosi.toString
    ), auditLogEntry.target)

}
