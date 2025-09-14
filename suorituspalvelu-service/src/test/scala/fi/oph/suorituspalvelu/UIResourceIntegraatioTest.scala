package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, Koodi, SuoritusJoukko}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.resource.ui.{KayttajaFailureResponse, KayttajaSuccessResponse, LuoPerusopetuksenOppimaaraFailureResponse, Oppija, OppijanHakuFailureResponse, OppijanHakuSuccessResponse, OppijanTiedotFailureResponse, OppijanTiedotSuccessResponse, Oppilaitos, OppilaitosNimi, OppilaitosSuccessResponse, PoistaSuoritusFailureResponse, SuoritusTila, SyotettyPerusopetuksenOppiaine, SyotettyPerusopetuksenOppimaaranSuoritus, UIVirheet}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.ui.UIService
import fi.oph.suorituspalvelu.validation.UIValidator
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.LocalDate
import java.util
import java.util.{Optional, UUID}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

/**
 * UI-apin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta.
 */
class UIResourceIntegraatioTest extends BaseIntegraatioTesti {

  @MockBean
  val onrIntegration: OnrIntegration = null

  @WithAnonymousUser
  @Test def testHaeKayttajanTiedotAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_KAYTTAJAN_TIEDOT_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeKayttajanTiedotNotFound(): Unit =
    // mockataan onr-vastaus
    Mockito.when(onrIntegration.getAsiointikieli("kayttaja")).thenReturn(Future.successful(None))

    // haetaan käyttäjän tiedot
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_KAYTTAJAN_TIEDOT_PATH, ""))
      .andExpect(status().isNotFound)
      .andReturn()

    // virhe on kuten pitää
    Assertions.assertEquals(KayttajaFailureResponse(java.util.Set.of(UIVirheet.UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KayttajaFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeKayttajanTiedotAllowed(): Unit =
    // mockataan onr-vastaus
    Mockito.when(onrIntegration.getAsiointikieli("kayttaja")).thenReturn(Future.successful(Some("fi")))

    // haetaan käyttäjän tiedot
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_KAYTTAJAN_TIEDOT_PATH, ""))
      .andExpect(status().isOk)
      .andReturn()

    // asiointikieli on "fi"
    Assertions.assertEquals(KayttajaSuccessResponse("fi"),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KayttajaSuccessResponse]))

  /*
   * Integraatiotestit oppilaitoslistauksen haulle
   */

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

    Assertions.assertEquals(OppilaitosSuccessResponse(java.util.List.of(Oppilaitos(OppilaitosNimi(Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI), Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI), Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI)), UIService.EXAMPLE_OPPILAITOS_OID))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppilaitosSuccessResponse]))

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

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_HAKU_KRITEERI_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatVainOppilaitusMaaritelty(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?oppilaitos={oppilaitos}", "1.2.246.562.10.56753942459"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_HAKU_OPPIJA_TAI_VUOSI_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatVainVuosiMaaritelty(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?vuosi={vuosi}", "2025"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_HAKU_OPPILAITOS_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatVainLuokkaMaaritelty(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?luokka={luokka}", "9B"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_HAKU_VUOSI_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatMalformedParameters(): Unit =
    // kaikki validoidut parametrit määritelty mutta mikään ei validi
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?oppilaitos={oppilaitos}&luokka={luokka}&vuosi={vuosi}", "ei validi oppilaitos", "ei validi luokka", "ei validi vuosi"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(
      java.util.Set.of(
        UIValidator.VALIDATION_OPPILAITOSOID_EI_VALIDI,
        UIValidator.VALIDATION_LUOKKA_EI_VALIDI,
        UIValidator.VALIDATION_VUOSI_EI_VALIDI
      )),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatAllowed(): Unit =
    val hakuKriteeri = "Olli"
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?oppija={oppija}", hakuKriteeri))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of(Oppija(UIService.EXAMPLE_OPPIJA_OID, Optional.of(UIService.EXAMPLE_HETU), UIService.EXAMPLE_NIMI))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

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

    Assertions.assertEquals(OppijanTiedotFailureResponse(java.util.Set.of(UIValidator.VALIDATION_OPPIJANUMERO_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanTiedotFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotNotFound(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967216"

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero))).thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaNumero -> Set(oppijaNumero)))))

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
    val koskiVersio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.KOSKI, "{\"testi\": \"suorituksetHenkilölle\"}")
    val ammatillinenTutkinto = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi(tutkintoKoodi, "koulutus", Some(1)), fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Koodi("valmistunut", "jokutila", Some(1)), fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS, Some(LocalDate.now()), Some(LocalDate.now()), None, Koodi("tapa", "suoritustapa", Some(1)), suoritusKieli, Set.empty)
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(koskiVersio.get, Set(
      AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Set(ammatillinenTutkinto), None),
    ), Set.empty)

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero))).thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaNumero -> Set(oppijaNumero)))))

    // suoritetaan kutsu ja parseroidaan vastaus
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_TIEDOT_PATH.replace(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER, oppijaNumero), ""))
      .andExpect(status().isOk)
      .andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanTiedotSuccessResponse])

    // TODO: validoidaan vastauksen sisältö kun liitetty oikeisiin suorituksiin

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaTiedotUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)

  /*
   * Integraatiotestit peruskoulun oppimäärän suorituksen tallennukselle
   */

  def getSyotettyPeruskoulunOppimaaranSuoritus(): SyotettyPerusopetuksenOppimaaranSuoritus =
    SyotettyPerusopetuksenOppimaaranSuoritus(
      Optional.of("1.2.246.562.24.21250967214"),
      Optional.of(UIService.EXAMPLE_OPPILAITOS_OID),
      Optional.of(LocalDate.now().toString),
      Optional.of("FI"),
      Optional.of(1),
      Optional.of(List(SyotettyPerusopetuksenOppiaine(
        Optional.of("MA"),
        Optional.empty(),
        Optional.empty(),
        Optional.of(9),
        Optional.of(false)
      )).asJava))

  @WithAnonymousUser
  @Test def testTallennaPeruskoulunOppimaaranSuoritusAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPeruskoulunOppimaaranSuoritus())))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testTallennaPeruskoulunOppimaaranSuoritusNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPeruskoulunOppimaaranSuoritus())))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPeruskoulunOppimaaranSuoritusInvalidJson(): Unit =
    // ei validi json ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("tämä ei ole validia jsonia"))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UIVirheet.UI_LUO_SUORITUS_PERUSOPETUS_JSON_VIRHE), List.empty.asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuoPerusopetuksenOppimaaraFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPeruskoulunOppimaaranSuoritusInvalidSuoritus(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPeruskoulunOppimaaranSuoritus().copy(oppijaOid = Optional.of("tämä ei ole validi oid")))))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UIValidator.VALIDATION_OPPIJANUMERO_EI_VALIDI), List.empty.asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuoPerusopetuksenOppimaaraFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPeruskoulunOppimaaranSuoritusOppijaNotFound(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967214"

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(false))

    // tuntematon henkilöoid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPeruskoulunOppimaaranSuoritus().copy(oppijaOid = Optional.of(oppijaNumero)))))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UIVirheet.UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA), List.empty.asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuoPerusopetuksenOppimaaraFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPeruskoulunOppimaaranSuoritusOppijaAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967213"

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(true))

    // validin suorituksen tallentaminen tunnetulle henkilölle ok
    val suoritusPayload = objectMapper.writeValueAsString(getSyotettyPeruskoulunOppimaaranSuoritus().copy(oppijaOid = Optional.of(oppijaNumero)))
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(suoritusPayload))
      .andExpect(status().isOk)
      .andReturn()

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.TallennaPeruskoulunOppimaaranSuoritus.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "oppijaNumero" -> oppijaNumero,
    ), auditLogEntry.target)
    Assertions.assertEquals(List(objectMapper.readValue(suoritusPayload, classOf[Map[Any, Any]])), auditLogEntry.changes)

    // ja suoritus tallentuu kantaan
    val suoritukset = kantaOperaatiot.haeSuoritukset(oppijaNumero).values.flatten.toSet
    Assertions.assertEquals(1, suoritukset.size)

  /*
   * Integraatiotestit peruskoulun oppimäärän suorituksen poistolle
   */

  // TODO: tässähän poistetaan versioita eikä oppijanumeroa!!!!

  @WithAnonymousUser
  @Test def testPoistaPeruskoulunOppimaaranSuoritusAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, UUID.randomUUID().toString), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testPoistaPeruskoulunOppimaaranSuoritusNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, UUID.randomUUID().toString), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testPoistaPeruskoulunOppimaaranSuoritusSuoritusNotFound(): Unit =
    val tunniste = UUID.randomUUID().toString

    // versio joka ei olemassa aiheuttaa virheen
    val result = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, tunniste), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isGone)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(PoistaSuoritusFailureResponse(java.util.Set.of(UIVirheet.UI_POISTA_SUORITUS_SUORITUSTA_EI_LOYTYNYT)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[PoistaSuoritusFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testPoistaPeruskoulunOppimaaranSuoritusSuoritusEiVoimassa(): Unit =
    // tallennetaan versio ja päätetään voimassaolo
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.21250967212", SuoritusJoukko.PERUSOPETUS, "{}")
    kantaOperaatiot.paataVersionVoimassaolo(versio.get.tunniste)

    // versio joka jo poistettu aiheuttaa virheen
    val result = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, versio.get.tunniste.toString), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(PoistaSuoritusFailureResponse(java.util.Set.of(UIVirheet.UI_POISTA_SUORITUS_SUORITUS_EI_VOIMASSA)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[PoistaSuoritusFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testPoistaPeruskoulunOppimaaranSuoritusSuoritusEiPoistettavissa(): Unit =
    // tallennetaan versio ja päätetään voimassaolo
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.21250967212", SuoritusJoukko.KOSKI, "{}")

    // versio joka ei poistettavissa aiheuttaa virheen
    val result = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, versio.get.tunniste.toString), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(PoistaSuoritusFailureResponse(java.util.Set.of(UIVirheet.UI_POISTA_SUORITUS_SUORITUSTA_EI_POISTETTAVISSA)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[PoistaSuoritusFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testPoistaPeruskoulunOppimaaranSuoritusSuoritusAllowed(): Unit =
    // tallennetaan versio
    val oppijaNumero = "1.2.246.562.24.21250967211"
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.PERUSOPETUS, "{}")

    // poistetaan versio
    val result = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, versio.get.tunniste.toString), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.PoistaPeruskoulunOppimaaranSuoritus.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "versioTunniste" -> versio.get.tunniste.toString,
    ), auditLogEntry.target)

    // ja suoritus tallentuu kantaan
    val suoritukset = kantaOperaatiot.haeSuoritukset(oppijaNumero).values.flatten.toSet
    Assertions.assertEquals(0, suoritukset.size)

}
