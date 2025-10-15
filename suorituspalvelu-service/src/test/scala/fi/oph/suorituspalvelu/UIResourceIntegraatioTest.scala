package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.SuoritusJoukko.KOSKI
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, Koodi, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusJoukko, SuoritusTila}
import fi.oph.suorituspalvelu.integration.client.{AtaruPermissionRequest, AtaruPermissionResponse, HakemuspalveluClientImpl, Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiUtil}
import fi.oph.suorituspalvelu.resource.ui.{KayttajaFailureResponse, KayttajaSuccessResponse, LuoPerusopetuksenOppiaineenOppimaaraFailureResponse, LuoPerusopetuksenOppimaaraFailureResponse, Oppija, OppijanHakuFailureResponse, OppijanHakuSuccessResponse, OppijanTiedotFailureResponse, OppijanTiedotSuccessResponse, Oppilaitos, OppilaitosNimi, OppilaitosSuccessResponse, PoistaSuoritusFailureResponse, SuoritusTila, SyotettyPerusopetuksenOppiaine, SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, SyotettyPerusopetuksenOppimaaranSuoritus, UIVirheet}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.ui.UIService
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
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
import java.time.{Instant, LocalDate}
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

  @MockBean
  val organisaatioProvider: OrganisaatioProvider = null

  @MockBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

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

  @WithMockUser(value = "kayttaja", authorities = Array("ROLE_APP_SUORITUSREKISTERI_READ_1.2.246.562.10.52320123196"))
  @Test def testHaeOppilaitoksetAllowed(): Unit =
    val oppilaitosOid = "1.2.246.562.10.52320123196"

    // mockataan organisaatiopalvelun vastaus
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi(UIService.EXAMPLE_OPPILAITOS_NIMI, UIService.EXAMPLE_OPPILAITOS_NIMI, UIService.EXAMPLE_OPPILAITOS_NIMI), None, Seq.empty)
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid)).thenReturn(Some(organisaatio))

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOKSET_PATH, ""))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(OppilaitosSuccessResponse(java.util.List.of(Oppilaitos(OppilaitosNimi(Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI), Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI), Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI)), oppilaitosOid))),
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
    val hakusanaOppijanumero = "1.2.246.562.24.21583363331"
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?hakusana={hakusana}", hakusanaOppijanumero))
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

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_HAKU_VUOSI_PAKOLLINEN)),
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

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_HAKU_VUOSI_PAKOLLINEN_JOS_LUOKKA)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatHakusanaJaOppilaitosMaaritelty(): Unit =
    // haku tehdään joko hetulla/oppijaoidilla tai oppilaitoksella
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?hakusana={hakusana}&oppilaitos={oppilaitos}", "1.2.246.562.24.21583363331", "1.2.246.562.10.56753942459"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_HAKU_JOKO_HAKUSANA_TAI_OPPILAITOS)),
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
    val hakusanaOppijanumero = "1.2.246.562.24.21583363332"
    val onrPerustiedot = OnrHenkiloPerustiedot(oidHenkilo = hakusanaOppijanumero,
      etunimet = "Teppo Hemmo",
      sukunimi = "Testinen")

    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(hakusanaOppijanumero)))
      .thenReturn(Future.successful(Seq(onrPerustiedot)))

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?hakusana={hakusana}", hakusanaOppijanumero))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of(Oppija(hakusanaOppijanumero, Optional.empty(), "Teppo Hemmo Testinen"))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_HAKU_HAKUSANA_PARAM_NAME -> hakusanaOppijanumero,
      ApiConstants.UI_HAKU_OPPILAITOS_PARAM_NAME -> null,
      ApiConstants.UI_HAKU_VUOSI_PARAM_NAME -> null,
      ApiConstants.UI_HAKU_LUOKKA_PARAM_NAME -> null,
    ), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array("ROLE_APP_SUORITUSREKISTERI_READ_1.2.246.562.10.52320123197"))
  @Test def testHaeOppijatOppilaitoskayttajaAllowedByHakemus(): Unit =
    val hakusanaOppijanumero = "1.2.246.562.24.21583363333"
    val orgOid = "1.2.246.562.10.52320123197"
    val descendantOid = "1.2.246.562.10.52320123198"
    val organisaatio = Organisaatio(orgOid, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq(descendantOid))
    val onrPerustiedot = OnrHenkiloPerustiedot(oidHenkilo = hakusanaOppijanumero,
      etunimet = "Teppo Hemmo",
      sukunimi = "Testinen")

    val permissionRequest = AtaruPermissionRequest(Set(hakusanaOppijanumero), Set(orgOid, descendantOid), Set.empty)

    val permissionResponse = AtaruPermissionResponse(Some(true), None)

    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(hakusanaOppijanumero)))
      .thenReturn(Future.successful(Seq(onrPerustiedot)))
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(orgOid)).thenReturn(Some(organisaatio))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(hakusanaOppijanumero))).thenReturn(Future.successful(PersonOidsWithAliases(Map(hakusanaOppijanumero -> Set(hakusanaOppijanumero)))))
    Mockito.when(hakemuspalveluClient.checkPermission(permissionRequest)).thenReturn(Future.successful(permissionResponse))

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?hakusana={hakusana}", hakusanaOppijanumero))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of(Oppija(hakusanaOppijanumero, Optional.empty(), "Teppo Hemmo Testinen"))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_HAKU_HAKUSANA_PARAM_NAME -> hakusanaOppijanumero,
      ApiConstants.UI_HAKU_OPPILAITOS_PARAM_NAME -> null,
      ApiConstants.UI_HAKU_VUOSI_PARAM_NAME -> null,
      ApiConstants.UI_HAKU_LUOKKA_PARAM_NAME -> null,
    ), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array("ROLE_APP_SUORITUSREKISTERI_READ_1.2.246.562.10.52320123197"))
  @Test def testHaeOppijatOppilaitoskayttajaNotAllowedByHakemus(): Unit =
    val hakusanaOppijanumero = "1.2.246.562.24.21583363441"
    val orgOid = "1.2.246.562.10.52320123197"
    val descendantOid = "1.2.246.562.10.52320123198"
    val organisaatio = Organisaatio(orgOid, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq(descendantOid))
    val onrPerustiedot = OnrHenkiloPerustiedot(oidHenkilo = hakusanaOppijanumero,
      etunimet = "Teppo Hemmo",
      sukunimi = "Testinen")

    val permissionRequest = AtaruPermissionRequest(Set(hakusanaOppijanumero), Set(orgOid, descendantOid), Set.empty)

    val permissionResponse = AtaruPermissionResponse(Some(false), None)

    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(hakusanaOppijanumero)))
      .thenReturn(Future.successful(Seq(onrPerustiedot)))
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(orgOid)).thenReturn(Some(organisaatio))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(hakusanaOppijanumero))).thenReturn(Future.successful(PersonOidsWithAliases(Map(hakusanaOppijanumero -> Set(hakusanaOppijanumero)))))
    Mockito.when(hakemuspalveluClient.checkPermission(permissionRequest)).thenReturn(Future.successful(permissionResponse))

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?hakusana={hakusana}", hakusanaOppijanumero))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of()),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_HAKU_HAKUSANA_PARAM_NAME -> hakusanaOppijanumero,
      ApiConstants.UI_HAKU_OPPILAITOS_PARAM_NAME -> null,
      ApiConstants.UI_HAKU_VUOSI_PARAM_NAME -> null,
      ApiConstants.UI_HAKU_LUOKKA_PARAM_NAME -> null,
    ), auditLogEntry.target)


  @WithMockUser(value = "kayttaja", authorities = Array("ROLE_APP_SUORITUSREKISTERI_READ_1.2.246.562.10.52320123197"))
  @Test def testHaeOppijatByOppilaitosAndVuosiValmisPKAllowed(): Unit =
    val etunimet = "Teppo Hemmo"
    val sukunimi = "Testinen"
    val hakusanaOppijanumero = "1.2.246.562.24.21583363334"
    val oppilaitosOid = "1.2.246.562.10.52320123197"
    val vuosi = "2025"

    // mockataan onr-vastaus
    val onrPerustiedot = OnrHenkiloPerustiedot(hakusanaOppijanumero, etunimet, sukunimi)
    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(hakusanaOppijanumero)))
      .thenReturn(Future.successful(Seq(onrPerustiedot)))


    // mockataan organisaatiopalvelun vastaus
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq.empty)
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid)).thenReturn(Some(organisaatio))

    // tallennetaan valmis perusopetuksen oppimäärä
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(hakusanaOppijanumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      oppilaitosOid,
      Set(PerusopetuksenOppimaara(
        UUID.randomUUID(),
        None,
        fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
        None,
        Koodi("", "", None),
        VALMIS,
        Koodi("", "", None),
        Set.empty,
        None,
        None,
        Some(LocalDate.parse(s"$vuosi-08-18")),
        Set.empty
      )),
      None,
      VALMIS
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getPeruskoulunSuoritusMetadata(opiskeluoikeudet.toSeq))

    // haetaan oppijoita oppilaitoksella ja vuodella
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?oppilaitos={oppilaitos}&vuosi={vuosi}", oppilaitosOid, vuosi))
      .andExpect(status().isOk)
      .andReturn()

    // palautuu tallennettu oppija
    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of(Oppija(hakusanaOppijanumero, Optional.empty(), s"$etunimet $sukunimi"))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

    // ja auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_HAKU_HAKUSANA_PARAM_NAME -> null,
      ApiConstants.UI_HAKU_OPPILAITOS_PARAM_NAME -> oppilaitosOid,
      ApiConstants.UI_HAKU_VUOSI_PARAM_NAME -> vuosi,
      ApiConstants.UI_HAKU_LUOKKA_PARAM_NAME -> null,
    ), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array("ROLE_APP_SUORITUSREKISTERI_READ_1.2.246.562.10.52320123199"))
  @Test def testHaeOppijatByOppilaitosAndVuosiValmisYsiAllowed(): Unit =
    val etunimet = "Teppo Hemmo"
    val sukunimi = "Testinen"
    val hakusanaOppijanumero = "1.2.246.562.24.21583363335"
    val oppilaitosOid = "1.2.246.562.10.52320123199"
    val vuosi = "2025"

    // mockataan onr-vastaus
    val onrPerustiedot = OnrHenkiloPerustiedot(hakusanaOppijanumero, etunimet, sukunimi)
    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(hakusanaOppijanumero)))
      .thenReturn(Future.successful(Seq(onrPerustiedot)))

    // mockataan organisaatiopalvelun vastaus
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq.empty)
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid)).thenReturn(Some(organisaatio))

    // tallennetaan valmis perusopetuksen oppimäärä
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(hakusanaOppijanumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      oppilaitosOid,
      Set(PerusopetuksenVuosiluokka(
        UUID.randomUUID(),
        fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
        Kielistetty(None, None, None),
        Koodi("9", "perusopetuksenluokkaaste", None),
        None,
        None,
        false
      )),
      None,
      VALMIS
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getPeruskoulunSuoritusMetadata(opiskeluoikeudet.toSeq))

    // haetaan oppijoita oppilaitoksella ja vuodella
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HAKU_PATH + "?oppilaitos={oppilaitos}&vuosi={vuosi}", oppilaitosOid, vuosi))
      .andExpect(status().isOk)
      .andReturn()

    // palautuu tallennettu oppija
    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of(Oppija(hakusanaOppijanumero, Optional.empty(), s"$etunimet $sukunimi"))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

    // ja auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_HAKU_HAKUSANA_PARAM_NAME -> null,
      ApiConstants.UI_HAKU_OPPILAITOS_PARAM_NAME -> oppilaitosOid,
      ApiConstants.UI_HAKU_VUOSI_PARAM_NAME -> vuosi,
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
    val koskiVersio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
    val ammatillinenTutkinto = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi(tutkintoKoodi, "koulutus", Some(1)), fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Koodi("valmistunut", "jokutila", Some(1)), fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS, Some(LocalDate.now()), Some(LocalDate.now()), None, Koodi("tapa", "suoritustapa", Some(1)), suoritusKieli, Set.empty)
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(koskiVersio.get, Set(
      AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Set(ammatillinenTutkinto), None),
    ))

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
   * Integraatiotestit perusopetuksen oppimäärän suorituksen tallennukselle
   */

  def getSyotettyPerusopetuksenOppimaaranSuoritus(): SyotettyPerusopetuksenOppimaaranSuoritus =
    SyotettyPerusopetuksenOppimaaranSuoritus(
      Optional.of("1.2.246.562.24.21250967214"),
      Optional.of(UIService.EXAMPLE_OPPILAITOS_OID),
      Optional.of(LocalDate.now().toString),
      Optional.of("FI"),
      Optional.of(1),
      Optional.of(List(SyotettyPerusopetuksenOppiaine(
        Optional.of("MA"),
        Optional.empty(),
        Optional.of(9),
        Optional.of(false)
      )).asJava))

  @WithAnonymousUser
  @Test def testTallennaPerusopetuksenOppimaaranSuoritusAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppimaaranSuoritus())))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testTallennaPerusopetuksenOppimaaranSuoritusNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppimaaranSuoritus())))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPerusopetuksenOppimaaranSuoritusInvalidJson(): Unit =
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
  @Test def testTallennaPerusopetuksenOppimaaranSuoritusInvalidSuoritus(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppimaaranSuoritus().copy(oppijaOid = Optional.of("tämä ei ole validi oid")))))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UIValidator.VALIDATION_OPPIJANUMERO_EI_VALIDI), List.empty.asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuoPerusopetuksenOppimaaraFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPerusopetuksenOppimaaranSuoritusOppijaNotFound(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967214"

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(false))

    // tuntematon henkilöoid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppimaaranSuoritus().copy(oppijaOid = Optional.of(oppijaNumero)))))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UIVirheet.UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA), List.empty.asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuoPerusopetuksenOppimaaraFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPerusopetuksenOppimaaranSuoritusOppijaAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967213"
    val organisaatio = Organisaatio(UIService.EXAMPLE_OPPILAITOS_OID, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq.empty)

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(true))
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(UIService.EXAMPLE_OPPILAITOS_OID)).thenReturn(Some(organisaatio))

    // validin suorituksen tallentaminen tunnetulle henkilölle ok
    val suoritusPayload = objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppimaaranSuoritus().copy(oppijaOid = Optional.of(oppijaNumero)))
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(suoritusPayload))
      .andExpect(status().isOk)
      .andReturn()

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.TallennaPerusopetuksenOppimaaranSuoritus.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "oppijaNumero" -> oppijaNumero,
    ), auditLogEntry.target)
    Assertions.assertEquals(List(objectMapper.readValue(suoritusPayload, classOf[Map[Any, Any]])), auditLogEntry.changes)

    // ja suoritus tallentuu kantaan
    val suoritukset = kantaOperaatiot.haeSuoritukset(oppijaNumero).values.flatten.toSet
    Assertions.assertEquals(1, suoritukset.size)

  /*
   * Integraatiotestit perusopetuksen oppimäärän suorituksen tallennukselle
   */

  def getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus(): SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus =
    SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus(
      Optional.of("1.2.246.562.24.21250967212"),
      Optional.of(UIService.EXAMPLE_OPPILAITOS_OID),
      Optional.of(LocalDate.now().toString),
      Optional.of("FI"),
      Optional.of(1),
      Optional.of(SyotettyPerusopetuksenOppiaine(
        Optional.of("MA"),
        Optional.empty(),
        Optional.of(9),
        Optional.of(false)
      )))

  @WithAnonymousUser
  @Test def testTallennaPerusopetuksenOppiaineenOppimaaranSuoritusAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_OPPIAINE_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus())))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testTallennaPerusopetuksenOppiaineenOppimaaranSuoritusNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_OPPIAINE_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus())))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPerusopetuksenOppiaineenOppimaaranSuoritusInvalidJson(): Unit =
    // ei validi json ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_OPPIAINE_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("tämä ei ole validia jsonia"))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UIVirheet.UI_LUO_SUORITUS_OPPIAINE_JSON_VIRHE)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuoPerusopetuksenOppiaineenOppimaaraFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPerusopetuksenOppiaineenOppimaaranSuoritusInvalidSuoritus(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_OPPIAINE_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus().copy(oppijaOid = Optional.of("tämä ei ole validi oid")))))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UIValidator.VALIDATION_OPPIJANUMERO_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuoPerusopetuksenOppiaineenOppimaaraFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPerusopetuksenOppiaineenOppimaaranSuoritusOppijaNotFound(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967211"

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(false))

    // tuntematon henkilöoid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_OPPIAINE_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus().copy(oppijaOid = Optional.of(oppijaNumero)))))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UIVirheet.UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuoPerusopetuksenOppiaineenOppimaaraFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPerusopetuksenOppiaineenOppimaaranSuoritusOppijaAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967210"
    val organisaatio = Organisaatio(UIService.EXAMPLE_OPPILAITOS_OID, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq.empty)

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(true))
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(UIService.EXAMPLE_OPPILAITOS_OID)).thenReturn(Some(organisaatio))

    // validin suorituksen tallentaminen tunnetulle henkilölle ok
    val suoritusPayload = objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus().copy(oppijaOid = Optional.of(oppijaNumero)))
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_LUO_SUORITUS_OPPIAINE_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(suoritusPayload))
      .andExpect(status().isOk)
      .andReturn()

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.TallennaPerusopetuksenOppiaineenOppimaaranSuoritus.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "oppijaNumero" -> oppijaNumero,
    ), auditLogEntry.target)
    Assertions.assertEquals(List(objectMapper.readValue(suoritusPayload, classOf[Map[Any, Any]])), auditLogEntry.changes)

    // ja suoritus tallentuu kantaan
    val suoritukset = kantaOperaatiot.haeSuoritukset(oppijaNumero).values.flatten.toSet
    Assertions.assertEquals(1, suoritukset.size)

  /*
   * Integraatiotestit perusopetuksen oppimäärän suorituksen poistolle
   */

  @WithAnonymousUser
  @Test def testPoistaPerusopetuksenOppimaaranSuoritusAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, UUID.randomUUID().toString), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testPoistaPerusopetuksenOppimaaranSuoritusNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, UUID.randomUUID().toString), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testPoistaPerusopetuksenOppimaaranSuoritusSuoritusNotFound(): Unit =
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
  @Test def testPoistaPerusopetuksenOppimaaranSuoritusSuoritusEiVoimassa(): Unit =
    // tallennetaan versio ja päätetään voimassaolo
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.21250967212", SuoritusJoukko.SYOTETTY_PERUSOPETUS, Seq.empty, Instant.now())
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
  @Test def testPoistaPerusopetuksenOppimaaranSuoritusSuoritusEiPoistettavissa(): Unit =
    // tallennetaan versio lähdejärjestelmälle jonka suorituksia ei voi poistaa
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.21250967212", SuoritusJoukko.KOSKI, Seq.empty, Instant.now())

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
  @Test def testPoistaPerusopetuksenOppimaaranSuoritusSuoritusAllowed(): Unit =
    // tallennetaan versio
    val oppijaNumero = "1.2.246.562.24.21250967211"
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.SYOTETTY_PERUSOPETUS, Seq.empty, Instant.now())

    // poistetaan versio
    val result = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, versio.get.tunniste.toString), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.PoistaPerusopetuksenOppimaaranSuoritus.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "versioTunniste" -> versio.get.tunniste.toString,
    ), auditLogEntry.target)

    // ja suoritus tallentuu kantaan
    val suoritukset = kantaOperaatiot.haeSuoritukset(oppijaNumero).values.flatten.toSet
    Assertions.assertEquals(0, suoritukset.size)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testPoistaPerusopetuksenOppiaineenOppimaaranSuoritusSuoritusAllowed(): Unit =
    // tallennetaan versio
    val oppijaNumero = "1.2.246.562.24.21250967211"
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.SYOTETTY_OPPIAINE, Seq.empty, Instant.now())

    // poistetaan versio
    val result = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, versio.get.tunniste.toString), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.PoistaPerusopetuksenOppiaineenOppimaaranSuoritus.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "versioTunniste" -> versio.get.tunniste.toString,
    ), auditLogEntry.target)

    // ja suoritus tallentuu kantaan
    val suoritukset = kantaOperaatiot.haeSuoritukset(oppijaNumero).values.flatten.toSet
    Assertions.assertEquals(0, suoritukset.size)

}
