package fi.oph.suorituspalvelu

import com.nimbusds.jose.util.StandardCharset
import fi.oph.suorituspalvelu.business.Tietolahde.KOSKI
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenTutkinto, Koodi}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.resource.ui.{Oppija, OppijanHakuFailureResponse, OppijanHakuSuccessResponse, OppijanTiedotFailureResponse, OppijanTiedotSuccessResponse, Oppilaitos, OppilaitosSuccessResponse}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.service.UIService
import fi.oph.suorituspalvelu.validation.Validator
import org.junit.jupiter.api.*
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.time.LocalDate
import java.util.Optional

/**
 * UI-apin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta.
 */
class UIResourceIntegraatioTest extends BaseIntegraatioTesti {

  @WithAnonymousUser
  @Test def testHaeOppilaitoksetAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOKSET_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeOppilaitoksetNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOKSET_PATH, ""))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppilaitoksetAllowed(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOKSET_PATH, ""))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(OppilaitosSuccessResponse(java.util.List.of(Oppilaitos(UIService.EXAMPLE_OPPILAITOS_NIMI, UIService.EXAMPLE_OPPILAITOS_OID))),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[OppilaitosSuccessResponse]))

  /*
   * Integraatiotestit oppijalistauksen haulle
   */

  @WithAnonymousUser
  @Test def testHaeOppijatAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeOppijatNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH, ""))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatHakuKriteereitaEiMaaritelty(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(ApiConstants.UI_HAKU_KRITEERI_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatVainOppilaitusMaaritelty(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?oppilaitos={oppilaitos}", "1.2.246.562.10.56753942459"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(ApiConstants.UI_HAKU_OPPIJA_TAI_VUOSI_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatVainVuosiMaaritelty(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?vuosi={vuosi}", "2025"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(ApiConstants.UI_HAKU_OPPILAITOS_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatVainLuokkaMaaritelty(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?luokka={luokka}", "9B"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(ApiConstants.UI_HAKU_VUOSI_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatMalformedParameters(): Unit =
    // kaikki validoidut parametrit määritelty mutta mikään ei validi
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?oppilaitos={oppilaitos}&luokka={luokka}&vuosi={vuosi}", "ei validi oppilaitos", "ei validi luokka", "ei validi vuosi"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(
      java.util.Set.of(
        Validator.VALIDATION_OPPILAITOSOID_EI_VALIDI + "ei validi oppilaitos",
        Validator.VALIDATION_LUOKKA_EI_VALIDI + "ei validi luokka",
        Validator.VALIDATION_VUOSI_EI_VALIDI + "ei validi vuosi"
      )),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatAllowed(): Unit =
    val hakuKriteeri = "Olli"
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?oppija={oppija}", hakuKriteeri))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of(Oppija(UIService.EXAMPLE_OPPIJA_OID, Optional.of(UIService.EXAMPLE_HETU), UIService.EXAMPLE_NIMI))),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[OppijanHakuSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_HAKU_OPPIJA_PARAM_NAME -> hakuKriteeri,
      ApiConstants.UI_HAKU_OPPILAITOS_PARAM_NAME -> null,
      ApiConstants.UI_HAKU_VUOSI_PARAM_NAME -> null,
      ApiConstants.UI_HAKU_LUOKKA_PARAM_NAME -> null,
    ), auditLogEntry.target)

  /*
   * Integraatiotestit oppijan tietojen haulle
   */

  @WithAnonymousUser
  @Test def testHaeOppijanTiedotAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_TIEDOT_PATH.replace(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER, "1.2.3.4"), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeOppijanTiedotNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_TIEDOT_PATH.replace(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER, "1.2.3.4"), ""))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotMalformedOid(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
      .get(ApiConstants.UI_TIEDOT_PATH.replace(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER, "tämä ei ole validi oid"), ""))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanTiedotFailureResponse(java.util.Set.of(Validator.VALIDATION_OPPIJANUMERO_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[OppijanTiedotFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotNotFound(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967216"

    // suoritetaan kutsu ja parseroidaan vastaus
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_TIEDOT_PATH.replace(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER, oppijaNumero), ""))
      .andExpect(status.isGone)
      .andReturn()

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaTiedotUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967215"
    val tutkintoKoodi = "123456"
    val suoritusKieli = Koodi("fi", "kieli", Some(1))

    // tallennetaan tutkinnot
    val koskiVersio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, KOSKI, "{\"testi\": \"suorituksetHenkilölle\"}")
    val ammatillinenTutkinto = AmmatillinenTutkinto(Kielistetty(Some("diplomi"), None, None), Koodi(tutkintoKoodi, "koulutus", Some(1)), Koodi("valmistunut", "jokutila", Some(1)), Some(LocalDate.now()), None, Koodi("tapa", "suoritustapa", Some(1)), suoritusKieli, Set.empty)
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(koskiVersio.get, Set(
      AmmatillinenOpiskeluoikeus("1.2.3", "2.3.4", Set(ammatillinenTutkinto), None),
    ), Set.empty)

    // suoritetaan kutsu ja parseroidaan vastaus
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_TIEDOT_PATH.replace(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER, oppijaNumero), ""))
      .andExpect(status().isOk)
      .andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[OppijanTiedotSuccessResponse])

    // TODO: validoidaan vastauksen sisältö kun liitetty oikeisiin suorituksiin

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaTiedotUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)

}
