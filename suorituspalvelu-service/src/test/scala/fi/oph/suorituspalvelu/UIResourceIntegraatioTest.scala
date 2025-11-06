package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.SuoritusJoukko.KOSKI
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AvainArvoYliajo, Koodi, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusJoukko, SuoritusTila}
import fi.oph.suorituspalvelu.integration.client.{AtaruPermissionRequest, AtaruPermissionResponse, HakemuspalveluClientImpl, KoutaHaku, KoutaHakuaika, Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration, PersonOidsWithAliases, TarjontaIntegration}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiUtil}
import fi.oph.suorituspalvelu.resource.ui.{KayttajaFailureResponse, KayttajaSuccessResponse, LuoPerusopetuksenOppiaineenOppimaaraFailureResponse, LuoPerusopetuksenOppimaaraFailureResponse, LuoSuoritusOppilaitoksetSuccessResponse, LuokatSuccessResponse, Oppija, OppijanHakuFailureResponse, OppijanHakuSuccessResponse, OppijanTiedotFailureResponse, OppijanTiedotSuccessResponse, OppijanValintaDataSuccessResponse, Oppilaitos, OppilaitosNimi, OppilaitosSuccessResponse, PoistaSuoritusFailureResponse, PoistaYliajoFailureResponse, SuoritusTila, SyotettyPerusopetuksenOppiaine, SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, SyotettyPerusopetuksenOppimaaranSuoritus, TallennaYliajotOppijalleFailureResponse, UIVirheet, VuodetSuccessResponse, Yliajo, YliajoTallennusContainer}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.ApiConstants.{UI_VALINTADATA_AVAIN_PARAM_NAME, UI_VALINTADATA_HAKU_PARAM_NAME, UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME}
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.service.UIService
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import fi.oph.suorituspalvelu.validation.UIValidator
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
 * UI-apin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta.
 */
class UIResourceIntegraatioTest extends BaseIntegraatioTesti {

  @MockitoBean
  val onrIntegration: OnrIntegration = null

  @MockitoBean
  val organisaatioProvider: OrganisaatioProvider = null

  @MockitoBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  final val ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA = SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA + "_1.2.246.562.10.52320123196"

  @MockitoBean
  val tarjontaIntegration: TarjontaIntegration = null

  /*
   * Integraatiotestit käyttäjän tietojen haulle
   */

  @WithAnonymousUser
  @Test def testHaeKayttajanTiedotAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_KAYTTAJAN_TIEDOT_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array(
    SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA,
    ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA))
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

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA))
  @Test def testHaeKayttajanTiedotAllowed(): Unit =
    // mockataan onr-vastaus
    Mockito.when(onrIntegration.getAsiointikieli("kayttaja")).thenReturn(Future.successful(Some("fi")))

    // haetaan käyttäjän tiedot
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_KAYTTAJAN_TIEDOT_PATH, ""))
      .andExpect(status().isOk)
      .andReturn()

    // asiointikieli on "fi" ja kyseessä on organisaation katselija
    Assertions.assertEquals(KayttajaSuccessResponse("fi", true),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KayttajaSuccessResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA))
  @Test def testHaeKayttajanTiedotAllowedOrganisaationKatselija(): Unit =
    // mockataan onr-vastaus
    Mockito.when(onrIntegration.getAsiointikieli("kayttaja")).thenReturn(Future.successful(Some("fi")))

    // haetaan käyttäjän tiedot
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_KAYTTAJAN_TIEDOT_PATH, ""))
      .andExpect(status().isOk)
      .andReturn()

    // asiointikieli on "fi" ja kyseessä ei organisaation katselija
    Assertions.assertEquals(KayttajaSuccessResponse("fi", true),
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

  @WithMockUser(value = "kayttaja", authorities = Array(
    SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA,
    ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA))
  @Test def testHaeOppilaitoksetAllowedOpo(): Unit =
    val oppilaitosOid = "1.2.246.562.10.52320123196"

    // mockataan organisaatiopalvelun vastaus
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi(UIService.EXAMPLE_OPPILAITOS_NIMI, UIService.EXAMPLE_OPPILAITOS_NIMI, UIService.EXAMPLE_OPPILAITOS_NIMI), None, Seq.empty, Seq.empty)
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid)).thenReturn(Some(organisaatio))

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOKSET_PATH, ""))
      .andExpect(status().isOk)
      .andReturn()

    // opolle oppilaitoslistaus perustuu organisaatioihin joihin käyttäjälle on oikeudet
    Assertions.assertEquals(OppilaitosSuccessResponse(java.util.List.of(Oppilaitos(OppilaitosNimi(Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI), Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI), Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI)), oppilaitosOid))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppilaitosSuccessResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppilaitoksetAllowedRekisterinpitaja(): Unit =
    val oppijaNumero = "1.2.246.562.24.21583363331"
    val oppilaitosOid = "1.2.246.562.10.52320123196"

    // tallennetaan valmis perusopetuksen oppimäärä
    // (rekisterinpitäjälle palautettavat oppilaitokset perustuvat metadatan arvoihin)
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
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
        Some(LocalDate.parse(s"2025-08-18")),
        Set.empty
      )),
      None,
      VALMIS
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getMetadata(opiskeluoikeudet.toSeq))

    // mockataan organisaatiopalvelun vastaus
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi(UIService.EXAMPLE_OPPILAITOS_NIMI, UIService.EXAMPLE_OPPILAITOS_NIMI, UIService.EXAMPLE_OPPILAITOS_NIMI), None, Seq.empty, Seq.empty)
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid)).thenReturn(Some(organisaatio))

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOKSET_PATH, ""))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(OppilaitosSuccessResponse(java.util.List.of(Oppilaitos(OppilaitosNimi(Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI), Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI), Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI)), oppilaitosOid))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppilaitosSuccessResponse]))

  /*
   * Integraatiotestit vuosilistauksen haulle
   */

  @WithAnonymousUser
  @Test def testHaeVuodetAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders.get(ApiConstants.UI_VUODET_PATH
        .replace(ApiConstants.UI_VUODET_OPPILAITOS_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_OPPILAITOS_OID), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeVuodetNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders.get(ApiConstants.UI_VUODET_PATH
        .replace(ApiConstants.UI_VUODET_OPPILAITOS_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_OPPILAITOS_OID), ""))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA))
  @Test def testHaeVuodetAllowed(): Unit =
    val oppijanumero = "1.2.246.562.24.21583363331"
    val oppilaitosOid = "1.2.246.562.10.52320123196"
    val valmistumisvuosi = "2024"

    // tallennetaan valmis perusopetuksen oppimäärä
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijanumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
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
        Some(LocalDate.parse(s"$valmistumisvuosi-08-18")),
        Set.empty
      )),
      None,
      VALMIS
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getMetadata(opiskeluoikeudet.toSeq))

    // haetaan vuodet ja katsotaan että täsmää
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.UI_VUODET_PATH
        .replace(ApiConstants.UI_VUODET_OPPILAITOS_PARAM_PLACEHOLDER, oppilaitosOid), ""))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(VuodetSuccessResponse(java.util.List.of(valmistumisvuosi)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[VuodetSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeVuodetUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_LUOKAT_OPPILAITOS_PARAM_NAME -> oppilaitosOid
    ), auditLogEntry.target)

  /*
   * Integraatiotestit luokkalistauksen haulle
   */

  @WithAnonymousUser
  @Test def testHaeLuokatAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders.get(ApiConstants.UI_LUOKAT_PATH
        .replace(ApiConstants.UI_LUOKAT_OPPILAITOS_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_OPPILAITOS_OID)
        .replace(ApiConstants.UI_LUOKAT_VUOSI_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_VUOSI), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeLuokatNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders.get(ApiConstants.UI_LUOKAT_PATH
        .replace(ApiConstants.UI_LUOKAT_OPPILAITOS_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_OPPILAITOS_OID)
        .replace(ApiConstants.UI_LUOKAT_VUOSI_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_VUOSI), ""))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA))
  @Test def testHaeLuokatAllowed(): Unit =
    val oppijanumero = "1.2.246.562.24.21583363331"
    val oppilaitosOid = "1.2.246.562.10.52320123196"
    val valmistumisvuosi = "2025"

    // tallennetaan valmis perusopetuksen vuosiluokka
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijanumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
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
        Some(LocalDate.parse(s"$valmistumisvuosi-08-18")),
        false
      )),
      None,
      VALMIS
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getMetadata(opiskeluoikeudet.toSeq))

    // haetaan luokat ja katsotaan että täsmää, TODO: toistaiseksi luokka kovakoodattu kunnes saadaan koskesta
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.UI_LUOKAT_PATH
        .replace(ApiConstants.UI_LUOKAT_OPPILAITOS_PARAM_PLACEHOLDER, oppilaitosOid)
        .replace(ApiConstants.UI_LUOKAT_VUOSI_PARAM_PLACEHOLDER, valmistumisvuosi), ""))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(LuokatSuccessResponse(java.util.List.of("9A")),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuokatSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeLuokatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_LUOKAT_OPPILAITOS_PARAM_NAME -> oppilaitosOid,
      ApiConstants.UI_LUOKAT_VUOSI_PARAM_NAME -> valmistumisvuosi
    ), auditLogEntry.target)

  /*
   * Integraatiotestit oppijan haulle
   */

  @WithAnonymousUser
  @Test def testHaeOppijatAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HENKILO_HAKU_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeOppijatNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    val hakusanaOppijanumero = "1.2.246.562.24.21583363331"
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HENKILO_HAKU_PATH + "?tunniste={tunniste}", hakusanaOppijanumero))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatHakuKriteereitaEiMaaritelty(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HENKILO_HAKU_PATH))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIValidator.VALIDATION_TUNNISTE_TYHJA)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijatAllowed(): Unit =
    val hakusanaOppijanumero = "1.2.246.562.24.21583363332"
    val onrPerustiedot = OnrHenkiloPerustiedot(oidHenkilo = hakusanaOppijanumero,
      etunimet = "Teppo Hemmo",
      sukunimi = "Testinen")

    // mockataan onr-vastaus
    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(hakusanaOppijanumero)))
      .thenReturn(Future.successful(Seq(onrPerustiedot)))

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HENKILO_HAKU_PATH + "?tunniste={tunniste}", hakusanaOppijanumero))
      .andExpect(status().isOk)
      .andReturn()

    // palautuu oppijanumeroa vastaava henkilö
    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of(Oppija(hakusanaOppijanumero, Optional.empty(), "Teppo Hemmo Testinen"))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_HENKILO_HAKU_TUNNISTE_PARAM_NAME -> hakusanaOppijanumero
    ), auditLogEntry.target)

  final val ORGANISAATION_1_2_246_562_10_52320123197_HAKENEIDEN_KATSELIJA = SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA + "_1.2.246.562.10.52320123197"

  @WithMockUser(value = "kayttaja", authorities = Array(
    SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA, ORGANISAATION_1_2_246_562_10_52320123197_HAKENEIDEN_KATSELIJA))
  @Test def testHaeOppijatOppilaitoskayttajaAllowedByHakemus(): Unit =
    val hakusanaOppijanumero = "1.2.246.562.24.21583363333"
    val orgOid = "1.2.246.562.10.52320123197"
    val descendantOid = "1.2.246.562.10.52320123198"
    val organisaatio = Organisaatio(orgOid, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq(descendantOid), Seq.empty)
    val onrPerustiedot = OnrHenkiloPerustiedot(oidHenkilo = hakusanaOppijanumero,
      etunimet = "Teppo Hemmo",
      sukunimi = "Testinen")

    // mockataan hakemuksen perusteella auditointiin liittyvä kutsutanssi
    val permissionRequest = AtaruPermissionRequest(Set(hakusanaOppijanumero), Set(orgOid, descendantOid), Set.empty)
    val permissionResponse = AtaruPermissionResponse(Some(true), None)
    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(hakusanaOppijanumero)))
      .thenReturn(Future.successful(Seq(onrPerustiedot)))
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(orgOid)).thenReturn(Some(organisaatio))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(hakusanaOppijanumero))).thenReturn(Future.successful(PersonOidsWithAliases(Map(hakusanaOppijanumero -> Set(hakusanaOppijanumero)))))
    Mockito.when(hakemuspalveluClient.checkPermission(permissionRequest)).thenReturn(Future.successful(permissionResponse))

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HENKILO_HAKU_PATH + "?tunniste={tunniste}", hakusanaOppijanumero))
      .andExpect(status().isOk)
      .andReturn()

    // palautuu haettua oidia vastaava henkilö
    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of(Oppija(hakusanaOppijanumero, Optional.empty(), "Teppo Hemmo Testinen"))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

    // tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_HENKILO_HAKU_TUNNISTE_PARAM_NAME -> hakusanaOppijanumero
    ), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array(
    SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA, ORGANISAATION_1_2_246_562_10_52320123197_HAKENEIDEN_KATSELIJA))
  @Test def testHaeOppijatOppilaitoskayttajaNotAllowedByHakemus(): Unit =
    val hakusanaOppijanumero = "1.2.246.562.24.21583363441"
    val orgOid = "1.2.246.562.10.52320123197"
    val descendantOid = "1.2.246.562.10.52320123198"
    val organisaatio = Organisaatio(orgOid, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq(descendantOid), Seq.empty)
    val onrPerustiedot = OnrHenkiloPerustiedot(oidHenkilo = hakusanaOppijanumero,
      etunimet = "Teppo Hemmo",
      sukunimi = "Testinen")

    // mockataan hakemuksen perusteella auditointiin liittyvä kutsutanssi
    val permissionRequest = AtaruPermissionRequest(Set(hakusanaOppijanumero), Set(orgOid, descendantOid), Set.empty)
    val permissionResponse = AtaruPermissionResponse(Some(false), None)
    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(hakusanaOppijanumero)))
      .thenReturn(Future.successful(Seq(onrPerustiedot)))
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(orgOid)).thenReturn(Some(organisaatio))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(hakusanaOppijanumero))).thenReturn(Future.successful(PersonOidsWithAliases(Map(hakusanaOppijanumero -> Set(hakusanaOppijanumero)))))
    Mockito.when(hakemuspalveluClient.checkPermission(permissionRequest)).thenReturn(Future.successful(permissionResponse))

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_HENKILO_HAKU_PATH + "?tunniste={tunniste}", hakusanaOppijanumero))
      .andExpect(status().isOk)
      .andReturn()

    // palautuu tyhjä lista koska ataru sanoo ei oikeuksia
    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of()),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

    // tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_HENKILO_HAKU_TUNNISTE_PARAM_NAME -> hakusanaOppijanumero
    ), auditLogEntry.target)

  /*
   * Integraatiotestit oppilaitoksen oppijoiden haulle
   */

  @WithAnonymousUser
  @Test def testHaeOppilaitoksenOppijatAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOS_HAKU_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeOppilaitoksenOppijatNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOS_HAKU_PATH + "?oppilaitos={oppilaitos}&vuosi=2025", ApiConstants.ESIMERKKI_OPPILAITOS_OID))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array("ROLE_APP_SUORITUSREKISTERI_READ_1.2.246.562.10.52320123197"))
  @Test def testHaeOppilaitoksenOppijatEiOikeuksiaOrganisaatioon(): Unit =
    val oppilaitosOidJohonOikeudet = "1.2.246.562.10.52320123197"
    val oppilaitosOidHaettu = "1.2.246.562.10.52320123198"

    // mockataan organisaatiopalvelun vastaus
    val organisaatio = Organisaatio(oppilaitosOidJohonOikeudet, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq.empty, Seq.empty)
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosOidJohonOikeudet)).thenReturn(Some(organisaatio))

    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOS_HAKU_PATH + "?oppilaitos={oppilaitos}&vuosi=2025", oppilaitosOidHaettu))
      .andExpect(status().isForbidden)
      .andReturn()

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppilaitoksenOppijatHakuKriteereitaEiMaaritelty(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOS_HAKU_PATH))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_OPPILAITOS_HAKU_OPPILAITOS_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppilaitoksenOppijatVainOppilaitosMaaritelty(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOS_HAKU_PATH + "?oppilaitos={oppilaitos}", "1.2.246.562.10.56753942459"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_OPPILAITOS_HAKU_VUOSI_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppilaitoksenOppijatVainVuosiMaaritelty(): Unit =
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOS_HAKU_PATH + "?vuosi={vuosi}", "2025"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_OPPILAITOS_HAKU_OPPILAITOS_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppilaitoksenOppijatVainLuokkaMaaritelty(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOS_HAKU_PATH + "?luokka={luokka}", "9B"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(java.util.Set.of(UIVirheet.UI_OPPILAITOS_HAKU_OPPILAITOS_PAKOLLINEN)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppilaitoksenOppijatMalformedParameters(): Unit =
    // kaikki validoidut parametrit määritelty mutta mikään ei validi
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOS_HAKU_PATH + "?oppilaitos={oppilaitos}&luokka={luokka}&vuosi={vuosi}", "ei validi oppilaitos", "ei#validi luokka", "ei validi vuosi"))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHakuFailureResponse(
      java.util.Set.of(
        UIValidator.VALIDATION_OPPILAITOSOID_EI_VALIDI,
        UIValidator.VALIDATION_LUOKKA_EI_VALIDI,
        UIValidator.VALIDATION_VUOSI_EI_VALIDI
      )),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA))
  @Test def testHaeOppilaitoksenOppijatByOppilaitosAndVuosiValmisPKAllowed(): Unit =
    val etunimet = "Teppo Hemmo"
    val sukunimi = "Testinen"
    val hakusanaOppijanumero = "1.2.246.562.24.21583363334"
    val oppilaitosOid = "1.2.246.562.10.52320123196"
    val vuosi = "2025"

    // mockataan onr-vastaus
    val onrPerustiedot = OnrHenkiloPerustiedot(hakusanaOppijanumero, etunimet, sukunimi)
    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(hakusanaOppijanumero)))
      .thenReturn(Future.successful(Seq(onrPerustiedot)))

    // mockataan organisaatiopalvelun vastaus
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq.empty, Seq.empty)
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
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getMetadata(opiskeluoikeudet.toSeq))

    // haetaan oppijoita oppilaitoksella ja vuodella
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOS_HAKU_PATH + "?oppilaitos={oppilaitos}&vuosi={vuosi}", oppilaitosOid, vuosi))
      .andExpect(status().isOk)
      .andReturn()

    // palautuu tallennettu oppija
    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of(Oppija(hakusanaOppijanumero, Optional.empty(), s"$etunimet $sukunimi"))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

    // ja auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppilaitoksenOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_OPPILAITOS_HAKU_OPPILAITOS_PARAM_NAME -> oppilaitosOid,
      ApiConstants.UI_OPPILAITOS_HAKU_VUOSI_PARAM_NAME -> vuosi,
      ApiConstants.UI_OPPILAITOS_HAKU_LUOKKA_PARAM_NAME -> null,
    ), auditLogEntry.target)

  /*
   * Integraatiotestit oppijan tietojen haulle
   */

  @WithAnonymousUser
  @Test def testHaeOppijanTiedotAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_TIEDOT_PATH.replace(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_OPPIJANUMERO), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeOppijanTiedotNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_TIEDOT_PATH.replace(ApiConstants.UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_OPPIJANUMERO), ""))
      .andExpect(status().isForbidden)

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
   * Integraatiotestit suorituksen tallennuksen vaihtoehtojen haulle
   */

  @WithAnonymousUser
  @Test def testHaeSyotettavatOppilaitoksetAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_TALLENNA_SUORITUS_OPPILAITOKSET_PATH))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeSyotettavatOppilaitoksetNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_TALLENNA_SUORITUS_OPPILAITOKSET_PATH))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeSyotettavatOppilaitoksetAllowed(): Unit =
    // mockataan organisaatiopalvelun vastaus
    val oppilaitosOid = "1.2.246.562.10.52320123196"
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi(UIService.EXAMPLE_OPPILAITOS_NIMI, UIService.EXAMPLE_OPPILAITOS_NIMI, UIService.EXAMPLE_OPPILAITOS_NIMI), None, Seq.empty, Seq("organisaatiotyyppi_02"))
    Mockito.when(organisaatioProvider.haeKaikkiOrganisaatiot()).thenReturn(Map(oppilaitosOid -> organisaatio))

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_TALLENNA_SUORITUS_OPPILAITOKSET_PATH))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(LuoSuoritusOppilaitoksetSuccessResponse(java.util.List.of(Oppilaitos(OppilaitosNimi(Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI), Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI), Optional.of(UIService.EXAMPLE_OPPILAITOS_NIMI)), oppilaitosOid))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuoSuoritusOppilaitoksetSuccessResponse]))

  /*
   * Integraatiotestit perusopetuksen oppimäärän suorituksen tallennukselle
   */

  def getSyotettyPerusopetuksenOppimaaranSuoritus(): SyotettyPerusopetuksenOppimaaranSuoritus =
    SyotettyPerusopetuksenOppimaaranSuoritus(
      Optional.of("1.2.246.562.24.21250967214"),
      Optional.of(UIService.EXAMPLE_OPPILAITOS_OID),
      Optional.of("KESKEN"),
      Optional.of(LocalDate.now().toString),
      Optional.of("9A"),
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
        .post(ApiConstants.UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppimaaranSuoritus())))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testTallennaPerusopetuksenOppimaaranSuoritusNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppimaaranSuoritus())))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPerusopetuksenOppimaaranSuoritusInvalidJson(): Unit =
    // ei validi json ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH, "")
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
        .post(ApiConstants.UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH, "")
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
        .post(ApiConstants.UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH, "")
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
    val organisaatio = Organisaatio(UIService.EXAMPLE_OPPILAITOS_OID, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq.empty, Seq.empty)

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(true))
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(UIService.EXAMPLE_OPPILAITOS_OID)).thenReturn(Some(organisaatio))

    // validin suorituksen tallentaminen tunnetulle henkilölle ok
    val suoritusPayload = objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppimaaranSuoritus().copy(oppijaOid = Optional.of(oppijaNumero)))
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH, "")
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
        .post(ApiConstants.UI_TALLENNA_SUORITUS_OPPIAINE_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus())))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testTallennaPerusopetuksenOppiaineenOppimaaranSuoritusNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_SUORITUS_OPPIAINE_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus())))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaPerusopetuksenOppiaineenOppimaaranSuoritusInvalidJson(): Unit =
    // ei validi json ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_SUORITUS_OPPIAINE_PATH, "")
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
        .post(ApiConstants.UI_TALLENNA_SUORITUS_OPPIAINE_PATH, "")
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
        .post(ApiConstants.UI_TALLENNA_SUORITUS_OPPIAINE_PATH, "")
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
    val organisaatio = Organisaatio(UIService.EXAMPLE_OPPILAITOS_OID, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq.empty, Seq.empty)

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(true))
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(UIService.EXAMPLE_OPPILAITOS_OID)).thenReturn(Some(organisaatio))

    // validin suorituksen tallentaminen tunnetulle henkilölle ok
    val suoritusPayload = objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus().copy(oppijaOid = Optional.of(oppijaNumero)))
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_SUORITUS_OPPIAINE_PATH, "")
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
      .andExpect(status().isForbidden)

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

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeOppijanValintaDataNotAllowed(): Unit = {
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    val oppijaNumero = "1.2.246.562.24.21250967211"
    val hakuOid = "1.2.246.562.29.01000000000000013275"

    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_VALINTADATA_PATH, "")
        .queryParam(UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, oppijaNumero)
        .queryParam(UI_VALINTADATA_HAKU_PARAM_NAME, hakuOid)
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isForbidden)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanValintaDataWithYliajoAllowed(): Unit = {
    // tallennetaan versio
    val oppijaNumero = "1.2.246.562.24.21250967211"
    val virkailijaOid = "1.2.246.562.24.21250967299"
    val hakuOid = "1.2.246.562.29.01000000000000013275"

    val yliajettuAvain = "lisapistekoulutus_telma"
    val yliajettuArvo = "true"
    val yliajoSelite = "Kyllä on Telma suoritettu, katsoin aivan itse eilen."
    val yliajo = AvainArvoYliajo(yliajettuAvain, yliajettuArvo, oppijaNumero, hakuOid, virkailijaOid, yliajoSelite)

    val eriHaunOid = "1.2.246.562.29.01000000000000013918"
    val eriHaunYliajettuAvain = "lisapistekoulutus_opisto"
    val eriHaunYliajettuArvo = "true"
    val eriHaunYliajoSelite = "Kyllä on Telma suoritettu, katsoin aivan itse eilen."
    val eriHaunYliajo = AvainArvoYliajo(eriHaunYliajettuAvain, eriHaunYliajettuArvo, oppijaNumero, eriHaunOid, virkailijaOid, eriHaunYliajoSelite)

    kantaOperaatiot.tallennaYliajot(Seq(yliajo, eriHaunYliajo))
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.SYOTETTY_OPPIAINE, Seq("{}"), Instant.now())

    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero)))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaNumero -> Set(oppijaNumero)))))

    Mockito.when(tarjontaIntegration.getHaku(hakuOid))
      .thenReturn(Some(KoutaHaku(
        oid = hakuOid,
        tila = "julkaistu",
        nimi = Map("fi" -> s"Testi haku $hakuOid"),
        hakutapaKoodiUri = "hakutapa_01",
        kohdejoukkoKoodiUri = Some("kohdejoukko_01"),
        hakuajat = List.empty,
        kohdejoukonTarkenneKoodiUri = None
      )))
    Mockito.when(hakemuspalveluClient.getHenkilonHakemustenTiedot(oppijaNumero))
      .thenReturn(Future.successful(Map.empty))
    //Todo, lisätään tähän tai toiseen testiin hakemus, ja tarkistetaan että sen tiedot parsiutuvat oikein avain-arvoiksi

    // haetaan valintadata
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_VALINTADATA_PATH, "")
        .queryParam(UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, oppijaNumero)
        .queryParam(UI_VALINTADATA_HAKU_PARAM_NAME, hakuOid)
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaValintaDataUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "oppijaNumero" -> oppijaNumero,
      "hakuOid" -> hakuOid,
    ), auditLogEntry.target)

    val parsedResult = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanValintaDataSuccessResponse])
    Assertions.assertEquals(parsedResult.henkiloOID, oppijaNumero)
    Assertions.assertEquals(parsedResult.hakuOID, hakuOid)

    //Tarkistetaan, että yliajo, vanha arvo ja selite löytyvät vastauksesta
    val yliajettu = parsedResult.avainArvot.asScala.find(_.avain == yliajettuAvain).get
    Assertions.assertEquals(yliajettu.arvo, yliajettuArvo)
    Assertions.assertEquals(yliajettu.metadata.arvoEnnenYliajoa.get, "false")
    Assertions.assertEquals(yliajettu.metadata.yliajo.get().selite, yliajoSelite)

    //Tarkistetaan, että eri haulle tehty yliajo ei vaikuta täällä
    val eriHaunYliajettu = parsedResult.avainArvot.asScala.find(_.avain == eriHaunYliajettuAvain).get
    Assertions.assertEquals(eriHaunYliajettu.arvo, "false")
    Assertions.assertNotEquals(eriHaunYliajettu.arvo, eriHaunYliajettuArvo)
    Assertions.assertTrue(eriHaunYliajettu.metadata.arvoEnnenYliajoa.isEmpty)
  }

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testTallennaYliajotOppijalleNotAllowed(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967210"
    val virkailijaOid = "1.2.246.562.24.21250967987"
    val hakuOid = "1.2.246.562.29.01000000000000013275"

    val yliajot = Range(1, 5).map(i => {
      Yliajo(
        avain = Optional.of("avain" + i),
        arvo = Optional.of("arvo" + i),
        selite = Optional.of("selite " + i),
      )
    }).toList.asJava
    val yliajoContainer = YliajoTallennusContainer(
      henkiloOid = Optional.of(oppijaNumero),
      hakuOid = Optional.of(hakuOid),
      yliajot = Optional.of(yliajot)
    )

    // validin suorituksen tallentaminen tunnetulle henkilölle ok
    val yliajotPayload = objectMapper.writeValueAsString(yliajoContainer)
    println(s"payload $yliajotPayload")
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_YLIAJOT_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(yliajotPayload))
      .andExpect(status().isForbidden)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaYliajotOppijalleTyhjaAvainBadRequest(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967210"
    val virkailijaOid = "1.2.246.562.24.21250967987"
    val hakuOid = "1.2.246.562.29.01000000000000013275"

    val yliajot =
      java.util.List.of(Yliajo(
        avain = Optional.of(""),
        arvo = Optional.of("arvo"),
        selite = Optional.of("selite ")))
    val yliajoContainer = YliajoTallennusContainer(
      henkiloOid = Optional.of(oppijaNumero),
      hakuOid = Optional.of(hakuOid),
      yliajot = Optional.of(yliajot)
    )

    val yliajotPayload = objectMapper.writeValueAsString(yliajoContainer)
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_YLIAJOT_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(yliajotPayload))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(TallennaYliajotOppijalleFailureResponse(java.util.Set.of(UIValidator.VALIDATION_AVAIN_TYHJA)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[TallennaYliajotOppijalleFailureResponse]))
  }

  @WithMockUser(value = "1.2.246.562.24.21250967987", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaYliajotOppijalleAllowed(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967210"
    val virkailijaOid = "1.2.246.562.24.21250967987" //Tämä tieto poimitaan sessiosta, katso MockUser
    val hakuOid = "1.2.246.562.29.01000000000000013275"

    val yliajot = Range(1, 5).map(i => {
      Yliajo(
        avain = Optional.of("avain" + i),
        arvo = Optional.of("arvo" + i),
        selite = Optional.of("selite " + i),
      )
    }).toList.asJava
    val yliajoContainer = YliajoTallennusContainer(
      henkiloOid = Optional.of(oppijaNumero),
      hakuOid = Optional.of(hakuOid),
      yliajot = Optional.of(yliajot)
    )

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(true))

    // validin suorituksen tallentaminen tunnetulle henkilölle ok
    val yliajotPayload = objectMapper.writeValueAsString(yliajoContainer)
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_YLIAJOT_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(yliajotPayload))
      .andExpect(status().isOk)
      .andReturn()

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.TallennaYliajotOppijalle.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "oppijaNumero" -> oppijaNumero,
      "hakuOid" -> hakuOid,
    ), auditLogEntry.target)
    Assertions.assertEquals(List(objectMapper.readValue(yliajotPayload, classOf[Map[Any, Any]])), auditLogEntry.changes)

    // ja yliajot tallentuvat kantaan
    val tallennetutYliajot = kantaOperaatiot.haeOppijanYliajot(oppijaNumero, hakuOid)
    Assertions.assertEquals(4, tallennetutYliajot.size)
    Assertions.assertEquals(tallennetutYliajot.map(_.virkailijaOid).toSet, Set("1.2.246.562.24.21250967987"))
  }

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testPoistaYliajoNotAllowed(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967210"
    val hakuOid = "1.2.246.562.29.01000000000000013275"

    val result = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_YLIAJO_PATH, "")
        .queryParam(UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, oppijaNumero)
        .queryParam(UI_VALINTADATA_HAKU_PARAM_NAME, hakuOid)
        .queryParam(UI_VALINTADATA_AVAIN_PARAM_NAME, "avain2"))
      .andExpect(status().isForbidden)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testPoistaYliajoMalformedAvainBadRequest(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967210"
    val hakuOid = "1.2.246.562.29.01000000000000013275"
    val malformedAvain = "avain-2" //Vain numerot, kirjaimet ja alaviivat sallittu

    val result = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_YLIAJO_PATH, "")
        .queryParam(UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, oppijaNumero)
        .queryParam(UI_VALINTADATA_HAKU_PARAM_NAME, hakuOid)
        .queryParam(UI_VALINTADATA_AVAIN_PARAM_NAME, malformedAvain))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(PoistaYliajoFailureResponse(java.util.Set.of(UIValidator.VALIDATION_AVAIN_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[PoistaYliajoFailureResponse]))
  }

  @WithMockUser(value = "1.2.246.562.24.21250967987", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testPoistaYliajoAllowed(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967210"
    val virkailijaOid = "1.2.246.562.24.21250967987" //Tämä tieto poimitaan sessiosta, katso MockUser
    val hakuOid = "1.2.246.562.29.01000000000000013275"
    val poistettavaAvain = "avain2"

    val yliajot = Range(1, 5).map(i => {
      Yliajo(
        avain = Optional.of("avain" + i),
        arvo = Optional.of("arvo" + i),
        selite = Optional.of("selite " + i),
      )
    }).toList.asJava
    val yliajoContainer = YliajoTallennusContainer(
      henkiloOid = Optional.of(oppijaNumero),
      hakuOid = Optional.of(hakuOid),
      yliajot = Optional.of(yliajot)
    )

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(true))

    val yliajotPayload = objectMapper.writeValueAsString(yliajoContainer)
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_YLIAJOT_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(yliajotPayload))
      .andExpect(status().isOk)
      .andReturn()

    // ja yliajot tallentuvat kantaan, ja myöhemmin poistettava avain on vielä mukana
    val tallennetutYliajot = kantaOperaatiot.haeOppijanYliajot(oppijaNumero, hakuOid)
    Assertions.assertEquals(4, tallennetutYliajot.size)
    Assertions.assertTrue(tallennetutYliajot.exists(_.avain == poistettavaAvain))
    Assertions.assertEquals(tallennetutYliajot.map(_.virkailijaOid).toSet, Set("1.2.246.562.24.21250967987"))

    // poistetaan yksi yliajo
    val poistoResult = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_YLIAJO_PATH, "")
        .queryParam(UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, oppijaNumero)
        .queryParam(UI_VALINTADATA_HAKU_PARAM_NAME, hakuOid)
        .queryParam(UI_VALINTADATA_AVAIN_PARAM_NAME, poistettavaAvain))
      .andExpect(status().isOk)
      .andReturn()

    // tarkistetaan että yliajoja on yksi vähemmän, ja poistettava avain on kadonnut
    val tallennetutYliajotPoistonJalkeen: Seq[AvainArvoYliajo] = kantaOperaatiot.haeOppijanYliajot(oppijaNumero, hakuOid)
    Assertions.assertEquals(3, tallennetutYliajotPoistonJalkeen.size)
    Assertions.assertFalse(tallennetutYliajotPoistonJalkeen.exists(_.avain == poistettavaAvain))
  }
}
