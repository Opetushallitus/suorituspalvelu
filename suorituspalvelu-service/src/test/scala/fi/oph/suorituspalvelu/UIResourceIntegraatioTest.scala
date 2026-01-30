package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.VUOSILUOKKA_9
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AvainArvoYliajo, EBArvosana, EBLaajuus, EBOppiaine, EBOppiaineenOsasuoritus, EBTutkinto, GeneerinenOpiskeluoikeus, Koodi, Lahtokoulu, Opiskeluoikeus, ParserVersions, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, Lahdejarjestelma, SuoritusTila}
import fi.oph.suorituspalvelu.integration.client.{AtaruPermissionRequest, AtaruPermissionResponse, DateParam, HakemuspalveluClientImpl, KoutaHaku, Ohjausparametrit, Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration, OnrMasterHenkilo, PersonOidsWithAliases, TarjontaIntegration}
import fi.oph.suorituspalvelu.mankeli.AvainArvoConstants
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiUtil}
import fi.oph.suorituspalvelu.resource.ui.{KayttajaFailureResponse, KayttajaSuccessResponse, LuoPerusopetuksenOppiaineenOppimaaraFailureResponse, LuoPerusopetuksenOppimaaraFailureResponse, LuoSuoritusOppilaitoksetSuccessResponse, LuokatSuccessResponse, Oppija, OppijanHakuFailureResponse, OppijanHakuSuccessResponse, OppijanHautFailureResponse, OppijanHautSuccessResponse, OppijanTiedotFailureResponse, OppijanTiedotRequest, OppijanTiedotSuccessResponse, OppijanValintaDataSuccessResponse, Oppilaitos, OppilaitosNimi, OppilaitosSuccessResponse, PerusopetuksenOppiaineenOppimaaratUI, PoistaSuoritusFailureResponse, PoistaYliajoFailureResponse, SyotettyPerusopetuksenOppiaine, SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer, SyotettyPerusopetuksenOppimaaranSuoritus, TallennaYliajotOppijalleFailureResponse, UIVirheet, VuodetSuccessResponse, Yliajo, YliajoTallennusContainer, YliajonMuutosHistoriaFailureResponse, YliajonMuutosHistoriaSuccessResponse, YliajonMuutosUI}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.ApiConstants.{UI_VALINTADATA_AVAIN_PARAM_NAME, UI_VALINTADATA_HAKU_PARAM_NAME, UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, UI_YLIAJOT_HISTORIA_AVAIN_PARAM_NAME, UI_YLIAJOT_HISTORIA_HAKU_PARAM_NAME, UI_YLIAJOT_HISTORIA_OPPIJANUMERO_PARAM_NAME}
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.{Instant, LocalDate}
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
    Assertions.assertEquals(KayttajaSuccessResponse("fi", false, true),
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
      .andExpect(status().isForbidden)

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
    val vuosi = 2025

    // tallennetaan valmis perusopetuksen oppimäärä
    // (rekisterinpitäjälle palautettavat oppilaitokset perustuvat metadatan arvoihin)
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, Lahdejarjestelma.KOSKI, Seq.empty, Seq.empty, Instant.now(), "1.2.3", Some(1))
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
        Some(LocalDate.parse(s"$vuosi-06-01")),
        Set.empty,
        Set(Lahtokoulu(LocalDate.parse(s"${vuosi-1}-08-01"), Some(LocalDate.parse(s"$vuosi-06-01")), oppilaitosOid, Some(LocalDate.now.getYear), Some("9A"), Some(VALMIS), None, VUOSILUOKKA_9)),
        false,
        false
      )),
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet), ParserVersions.KOSKI)

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
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA))
  @Test def testHaeVuodetAllowed(): Unit =
    val oppijanumero = "1.2.246.562.24.21583363331"
    val oppilaitosOid = "1.2.246.562.10.52320123196"
    val valmistumisvuosi = 2025

    // tallennetaan valmis perusopetuksen oppimäärä
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijanumero, Lahdejarjestelma.KOSKI, Seq.empty, Seq.empty, Instant.now(), "1.2.3", Some(1))
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
        Some(LocalDate.parse(s"$valmistumisvuosi-06-01")),
        Set.empty,
        Set(Lahtokoulu(LocalDate.parse(s"${valmistumisvuosi-1}-08-01"), Some(LocalDate.parse(s"$valmistumisvuosi-06-01")), oppilaitosOid, Some(valmistumisvuosi), Some("9A"), Some(VALMIS), None, VUOSILUOKKA_9)),
        false,
        false
      )),
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet), ParserVersions.KOSKI)

    // haetaan vuodet ja katsotaan että täsmää
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.UI_VUODET_PATH
        .replace(ApiConstants.UI_VUODET_OPPILAITOS_PARAM_PLACEHOLDER, oppilaitosOid), ""))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(VuodetSuccessResponse(java.util.List.of(valmistumisvuosi.toString)),
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
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA))
  @Test def testHaeLuokatAllowed(): Unit =
    val oppijanumero = "1.2.246.562.24.21583363331"
    val oppilaitosOid = "1.2.246.562.10.52320123196"
    val valmistumisvuosi = 2025

    // tallennetaan valmis perusopetuksen vuosiluokka
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijanumero, Lahdejarjestelma.KOSKI, Seq.empty, Seq.empty, Instant.now(), "1.2.3", Some(1))
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
        Some(LocalDate.parse(s"$valmistumisvuosi-06-01")),
        Set.empty,
        Set(Lahtokoulu(LocalDate.parse(s"${valmistumisvuosi-1}-08-18"), Some(LocalDate.parse(s"$valmistumisvuosi-06-01")), oppilaitosOid, Some(valmistumisvuosi), Some("9A"), Some(VALMIS), None, VUOSILUOKKA_9)),
        false,
        false
      )),
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet), ParserVersions.KOSKI)

    // haetaan luokat ja katsotaan että täsmää, TODO: toistaiseksi luokka kovakoodattu kunnes saadaan koskesta
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.UI_LUOKAT_PATH
        .replace(ApiConstants.UI_LUOKAT_OPPILAITOS_PARAM_PLACEHOLDER, oppilaitosOid)
        .replace(ApiConstants.UI_LUOKAT_VUOSI_PARAM_PLACEHOLDER, valmistumisvuosi.toString), ""))
      .andExpect(status().isOk)
      .andReturn()

    Assertions.assertEquals(LuokatSuccessResponse(java.util.List.of("9A")),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LuokatSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeLuokatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_LUOKAT_OPPILAITOS_PARAM_NAME -> oppilaitosOid,
      ApiConstants.UI_LUOKAT_VUOSI_PARAM_NAME -> valmistumisvuosi.toString
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
    val hetu = "123456-789A"
    val hakusanaOppijanumero = "1.2.246.562.24.21583363334"
    val oppilaitosOid = "1.2.246.562.10.52320123196"
    val valmistumisvuosi = 2025
    // Tällä hetkellä luokka kovakoodattu KoskiUtil-moduulissa "9A":ksi
    val luokka = "9A"

    // mockataan onr-vastaus
    val onrPerustiedot = OnrHenkiloPerustiedot(hakusanaOppijanumero, Some(etunimet), Some(sukunimi), Some(hetu))
    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(hakusanaOppijanumero)))
      .thenReturn(Future.successful(Seq(onrPerustiedot)))

    // mockataan organisaatiopalvelun vastaus
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq.empty, Seq.empty)
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid)).thenReturn(Some(organisaatio))

    // tallennetaan valmis perusopetuksen oppimäärä
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(hakusanaOppijanumero, Lahdejarjestelma.KOSKI, Seq.empty, Seq.empty, Instant.now(), "1.2.3", Some(1))
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      oppilaitosOid,
      Set(PerusopetuksenOppimaara(
        UUID.randomUUID(),
        None,
        fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
        Some(luokka),
        Koodi("", "", None),
        VALMIS,
        Koodi("", "", None),
        Set.empty,
        None,
        None,
        Some(LocalDate.parse(s"$valmistumisvuosi-06-01")),
        Set.empty,
        Set(Lahtokoulu(LocalDate.parse(s"${valmistumisvuosi-1}-08-01"), Some(LocalDate.parse(s"$valmistumisvuosi-06-01")), oppilaitosOid, Some(valmistumisvuosi), Some(luokka), Some(VALMIS), None, VUOSILUOKKA_9)),
        false,
        false
      )),
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet), ParserVersions.KOSKI)

    // haetaan oppijoita oppilaitoksella ja vuodella
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPILAITOS_HAKU_PATH + "?oppilaitos={oppilaitos}&vuosi={vuosi}", oppilaitosOid, valmistumisvuosi))
      .andExpect(status().isOk)
      .andReturn()

    // palautuu tallennettu oppija
    Assertions.assertEquals(OppijanHakuSuccessResponse(java.util.List.of(Oppija(hakusanaOppijanumero, Optional.of(hetu), Optional.of(etunimet), Optional.of(sukunimi), java.util.Set.of(luokka)))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHakuSuccessResponse]))

    // ja auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppilaitoksenOppijatUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_OPPILAITOS_HAKU_OPPILAITOS_PARAM_NAME -> oppilaitosOid,
      ApiConstants.UI_OPPILAITOS_HAKU_VUOSI_PARAM_NAME -> valmistumisvuosi.toString,
      ApiConstants.UI_OPPILAITOS_HAKU_LUOKKA_PARAM_NAME -> null,
    ), auditLogEntry.target)

  /*
   * Integraatiotestit oppijan tietojen haulle
   */

  @WithAnonymousUser
  @Test def testHaeOppijanTiedotAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    val request = OppijanTiedotRequest(Optional.of(ApiConstants.ESIMERKKI_OPPIJANUMERO), Optional.empty())
    mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TIEDOT_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeOppijanTiedotNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    val request = OppijanTiedotRequest(Optional.of(ApiConstants.ESIMERKKI_OPPIJANUMERO), Optional.empty())
    mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TIEDOT_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotMalformedParameters(): Unit =
    // ei validi oid ei sallittu
    val request = OppijanTiedotRequest(Optional.of("tämä ei ole validi oid"), Optional.of("tämä ei ole validi aikaleima"))
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TIEDOT_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanTiedotFailureResponse(java.util.Set.of(UIValidator.VALIDATION_TUNNISTE_EI_VALIDI, UIValidator.VALIDATION_AIKALEIMA_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanTiedotFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotNotFound(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967216"

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.getMasterHenkilosForPersonOids(Set(oppijaNumero))).thenReturn(Future.successful(Map.empty))

    // suoritetaan kutsu ja parseroidaan vastaus
    val request = OppijanTiedotRequest(Optional.of(oppijaNumero), Optional.empty())
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TIEDOT_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)))
      .andExpect(status.isGone)
      .andReturn()

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaTiedotUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotOppijanumerollaAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967215"
    val tutkintoKoodi = "123456"
    val suoritusKieli = Koodi("fi", "kieli", Some(1))
    val syntymaAika = Some(LocalDate.of(2000, 1, 1))

    // tallennetaan tutkinnot
    val koskiVersio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, Lahdejarjestelma.KOSKI, Seq.empty, Seq.empty, Instant.now(), "1.2.3.4", Some(1))
    val ammatillinenTutkinto = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi(tutkintoKoodi, "koulutus", Some(1)), fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Koodi("valmistunut", "jokutila", Some(1)), fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS, Some(LocalDate.now()), Some(LocalDate.now()), None, Koodi("tapa", "suoritustapa", Some(1)), suoritusKieli, Set.empty)
    val opiskeluoikeudet = Set(
      AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Set(ammatillinenTutkinto), None, List.empty),
    ).asInstanceOf[Set[Opiskeluoikeus]]
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(koskiVersio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet), ParserVersions.KOSKI)

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.getMasterHenkilosForPersonOids(Set(oppijaNumero))).thenReturn(Future.successful(Map(oppijaNumero -> OnrMasterHenkilo(oppijaNumero, None, None, None, None, syntymaAika))))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero))).thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaNumero -> Set(oppijaNumero)))))

    // suoritetaan kutsu ja parseroidaan vastaus
    val request = OppijanTiedotRequest(Optional.of(oppijaNumero), Optional.empty())
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TIEDOT_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)))
      .andExpect(status().isOk)
      .andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanTiedotSuccessResponse])


    // TODO: validoidaan vastauksen sisältö kun liitetty oikeisiin suorituksiin

    Assertions.assertEquals(response.syntymaAika.get, syntymaAika.get)

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaTiedotUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotHenkilotunnuksellaAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967215"
    val tutkintoKoodi = "123456"
    val suoritusKieli = Koodi("fi", "kieli", Some(1))
    val henkilotunnus = "123456-789A"

    // tallennetaan tutkinnot
    val koskiVersio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, Lahdejarjestelma.KOSKI, Seq.empty, Seq.empty, Instant.now(), "1.2.3.4", Some(1))
    val ammatillinenTutkinto = AmmatillinenPerustutkinto(UUID.randomUUID(), Kielistetty(Some("diplomi"), None, None), Koodi(tutkintoKoodi, "koulutus", Some(1)), fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Koodi("valmistunut", "jokutila", Some(1)), fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS, Some(LocalDate.now()), Some(LocalDate.now()), None, Koodi("tapa", "suoritustapa", Some(1)), suoritusKieli, Set.empty)
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(koskiVersio.get, Set(
      AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), "1.2.3.4"), Set(ammatillinenTutkinto), None, List.empty),
    ), List.empty, ParserVersions.KOSKI)

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.getMasterHenkilosForPersonOids(Set(oppijaNumero))).thenReturn(Future.successful(Map(oppijaNumero -> OnrMasterHenkilo(oppijaNumero, None, None, Some(henkilotunnus), None, None))))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero))).thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaNumero -> Set(oppijaNumero)))))
    Mockito.when(onrIntegration.getPerustiedotByHetus(Set(henkilotunnus))).thenReturn(Future.successful(List(OnrHenkiloPerustiedot(oppijaNumero, None, None, Some(henkilotunnus)))))

    // suoritetaan kutsu ja parseroidaan vastaus
    val request = OppijanTiedotRequest(Optional.of(henkilotunnus), Optional.empty())
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TIEDOT_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)))
      .andExpect(status().isOk)
      .andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanTiedotSuccessResponse])

    // TODO: validoidaan vastauksen sisältö kun liitetty oikeisiin suorituksiin

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaTiedotUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotHenkilotunnuksellaNotFound(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967215"
    val tutkintoKoodi = "123456"
    val suoritusKieli = Koodi("fi", "kieli", Some(1))
    val henkilotunnus = "123456-789A"

    // mockataan ONR-vastaus
    Mockito.when(onrIntegration.getPerustiedotByHetus(Set(henkilotunnus))).thenReturn(Future.successful(List.empty))

    // suoritetaan kutsu ja parseroidaan vastaus
    val request = OppijanTiedotRequest(Optional.of(henkilotunnus), Optional.empty())
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TIEDOT_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)))
      .andExpect(status().isNotFound)
      .andReturn()

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotHenkilotunnuksellaONRError(): Unit =
    val henkilotunnus = "123456-789A"

    // mockataan ONR-vastaus palauttamaan virheen
    Mockito.when(onrIntegration.getPerustiedotByHetus(Set(henkilotunnus))).thenReturn(Future.failed(new RuntimeException("ONR connection failed")))

    // suoritetaan kutsu ja tarkistetaan että palautetaan 500
    val request = OppijanTiedotRequest(Optional.of(henkilotunnus), Optional.empty())
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TIEDOT_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)))
      .andExpect(status().isInternalServerError)
      .andReturn()

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanTiedotSyotettyOppimaaraAllowed(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967215"
    val tutkintoKoodi = "123456"
    val suoritusKieli = Koodi("fi", "kieli", Some(1))
    val organisaatio = Organisaatio(UIService.EXAMPLE_OPPILAITOS_OID, OrganisaatioNimi("org nimi", "org namn", "org name"), None, Seq.empty, Seq.empty)

    // mockataan ONR-vastaus suorituksen tallennusta varten
    Mockito.when(onrIntegration.henkiloExists(oppijaNumero)).thenReturn(Future.successful(true))
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(UIService.EXAMPLE_OPPILAITOS_OID)).thenReturn(Some(organisaatio))

    // tallennetaan käsin syötetty oppimäärä
    val suoritusPayload = objectMapper.writeValueAsString(getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus().copy(oppijaOid = Optional.of(oppijaNumero)))
    val saveResult = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TALLENNA_SUORITUS_OPPIAINE_PATH, "")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(suoritusPayload))
      .andExpect(status().isOk)
      .andReturn()

    // mockataan ONR-vastaus tietojen hakua varten
    Mockito.when(onrIntegration.getMasterHenkilosForPersonOids(Set(oppijaNumero))).thenReturn(Future.successful(Map(oppijaNumero -> OnrMasterHenkilo(oppijaNumero, None, None, None, None, None))))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero))).thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaNumero -> Set(oppijaNumero)))))

    // suoritetaan kutsu ja parseroidaan vastaus
    val request = OppijanTiedotRequest(Optional.of(oppijaNumero), Optional.empty())
    val result = mvc.perform(MockMvcRequestBuilders
      .post(ApiConstants.UI_TIEDOT_PATH)
      .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsBytes(request)))
      .andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanTiedotSuccessResponse])

    val POOS = response.perusopetuksenOppiaineenOppimaarat.asScala.toList

    Assertions.assertEquals(1, POOS.size)
    Assertions.assertTrue(POOS.head.syotetty)

    val savedSuoritus: PerusopetuksenOppiaineenOppimaaratUI = POOS.head
    Assertions.assertEquals(UIService.EXAMPLE_OPPILAITOS_OID, savedSuoritus.oppilaitos.oid)
    Assertions.assertEquals("FI", savedSuoritus.suorituskieli)
    Assertions.assertEquals(Optional.of(LocalDate.now().toString), savedSuoritus.valmistumispaiva.map(_.toString))

    val oppiaineet = savedSuoritus.oppiaineet.asScala.toList
    Assertions.assertEquals(1, oppiaineet.size)
    val oppiaine = oppiaineet.head
    Assertions.assertEquals("MA", oppiaine.koodi)
    Assertions.assertEquals("9", oppiaine.arvosana)
    Assertions.assertFalse(oppiaine.valinnainen)
    Assertions.assertTrue(oppiaine.kieli.isEmpty)

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaTiedotUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)
  }

  /*
   * Integraatiotestit oppijan hakujen haulle
   */

  @WithAnonymousUser
  @Test def testHaeOppijanHautAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPIJAN_HAUT_PATH.replace(ApiConstants.UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_OPPIJANUMERO), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeOppijanHautNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPIJAN_HAUT_PATH.replace(ApiConstants.UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_OPPIJANUMERO), ""))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanHautMalformedOid(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPIJAN_HAUT_PATH.replace(ApiConstants.UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_PLACEHOLDER, "tämä ei ole validi oid"), ""))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(OppijanHautFailureResponse(java.util.Set.of(UIValidator.VALIDATION_OPPIJANUMERO_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHautFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanHautEiHakuja(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967220"

    // mockataan hakemuspalvelun vastaus - ei hakuja
    Mockito.when(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaNumero)))
      .thenReturn(Future.successful(Map(oppijaNumero -> Seq.empty)))

    // suoritetaan kutsu ja parseroidaan vastaus
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPIJAN_HAUT_PATH.replace(ApiConstants.UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_PLACEHOLDER, oppijaNumero), ""))
      .andExpect(status().isOk)
      .andReturn()

    // palautuu tyhjä lista
    Assertions.assertEquals(OppijanHautSuccessResponse(java.util.List.of()),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHautSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijanHautUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanHautAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967221"
    val hakuOid1 = "1.2.246.562.29.00000000000000000001"
    val hakuOid2 = "1.2.246.562.29.00000000000000000002"

    // mockataan hakemuspalvelun vastaus
    Mockito.when(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaNumero)))
      .thenReturn(Future.successful(Map(oppijaNumero -> Seq(hakuOid1, hakuOid2))))

    // mockataan Kouta-vastaukset
    val koutaHaku1 = KoutaHaku(
      oid = hakuOid1,
      tila = "julkaistu",
      nimi = Map("fi" -> "Testihaku 1", "sv" -> "Testansökan 1", "en" -> "Test application 1"),
      hakutapaKoodiUri = "hakutapa_01",
      kohdejoukkoKoodiUri = Some("haunkohdejoukko_11"),
      hakuajat = List.empty,
      kohdejoukonTarkenneKoodiUri = None,
      hakuvuosi = Some(LocalDate.now().getYear)
    )
    val koutaHaku2 = KoutaHaku(
      oid = hakuOid2,
      tila = "julkaistu",
      nimi = Map("fi" -> "Testihaku 2", "sv" -> "Testansökan 2"),
      hakutapaKoodiUri = "hakutapa_01",
      kohdejoukkoKoodiUri = Some("haunkohdejoukko_12"),
      hakuajat = List.empty,
      kohdejoukonTarkenneKoodiUri = None,
      hakuvuosi = Some(LocalDate.now().getYear)
    )
    Mockito.when(tarjontaIntegration.getHaku(hakuOid1)).thenReturn(Some(koutaHaku1))
    Mockito.when(tarjontaIntegration.getHaku(hakuOid2)).thenReturn(Some(koutaHaku2))

    // suoritetaan kutsu ja parseroidaan vastaus
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPIJAN_HAUT_PATH.replace(ApiConstants.UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_PLACEHOLDER, oppijaNumero), ""))
      .andExpect(status().isOk)
      .andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHautSuccessResponse])
    Assertions.assertEquals(2, response.haut.size())
    val firstHaku = response.haut.get(0)
    val secondHaku = response.haut.get(1)
    Assertions.assertEquals(hakuOid1, firstHaku.hakuOid)
    Assertions.assertEquals("Testihaku 1", firstHaku.nimi.fi.get)
    Assertions.assertEquals("Testansökan 1", firstHaku.nimi.sv.get)
    Assertions.assertEquals("Test application 1", firstHaku.nimi.en.get)
    Assertions.assertEquals(hakuOid2, secondHaku.hakuOid)
    Assertions.assertEquals("Testihaku 2", secondHaku.nimi.fi.get)
    Assertions.assertEquals("Testansökan 2", secondHaku.nimi.sv.get)
    Assertions.assertFalse(secondHaku.nimi.en.isPresent)

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijanHautUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanHautHakuEiLoydyKoutasta(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967222"
    val hakuOid1 = "1.2.246.562.29.00000000000000000003"
    val hakuOid2 = "1.2.246.562.29.00000000000000000004"

    // mockataan hakemuspalvelun vastaus
    Mockito.when(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaNumero)))
      .thenReturn(Future.successful(Map(oppijaNumero -> Seq(hakuOid1, hakuOid2))))

    // mockataan Kouta-vastaukset - vain ensimmäinen löytyy
    val koutaHaku1 = KoutaHaku(
      oid = hakuOid1,
      tila = "julkaistu",
      nimi = Map("fi" -> "Testihaku 1"),
      hakutapaKoodiUri = "hakutapa_01",
      kohdejoukkoKoodiUri = Some("haunkohdejoukko_11"),
      hakuajat = List.empty,
      kohdejoukonTarkenneKoodiUri = None,
      hakuvuosi = Some(LocalDate.now().getYear)
    )
    Mockito.when(tarjontaIntegration.getHaku(hakuOid1)).thenReturn(Some(koutaHaku1))
    Mockito.when(tarjontaIntegration.getHaku(hakuOid2)).thenReturn(None)

    // suoritetaan kutsu ja parseroidaan vastaus
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_OPPIJAN_HAUT_PATH.replace(ApiConstants.UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_PLACEHOLDER, oppijaNumero), ""))
      .andExpect(status().isOk)
      .andReturn()

    // palautuu vain yksi haku (se joka löytyi Koutasta)
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanHautSuccessResponse])
    Assertions.assertEquals(1, response.haut.size())
    Assertions.assertEquals(hakuOid1, response.haut.get(0).hakuOid)
    Assertions.assertEquals("Testihaku 1", response.haut.get(0).nimi.fi.get)

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijanHautUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)

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
    Assertions.assertEquals(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UIVirheet.UI_LUO_SUORITUS_PERUSOPETUS_JSON_EI_VALIDI), List.empty.asJava),
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

  def getSyotettyPerusopetuksenOppiaineenOppimaaranSuoritus(): SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer =
    SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer(
      Optional.of("1.2.246.562.24.21250967212"),
      Optional.of(UIService.EXAMPLE_OPPILAITOS_OID),
      Optional.of(LocalDate.now().toString),
      Optional.of("FI"),
      Optional.of(1),
      java.util.List.of(
        SyotettyPerusopetuksenOppiaine(
          Optional.of("MA"),
          Optional.empty(),
          Optional.of(9),
          Optional.of(false)
        )
      ))

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
    Assertions.assertEquals(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UIVirheet.UI_LUO_SUORITUS_OPPIAINE_JSON_EI_VALIDI)),
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
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.21250967212", Lahdejarjestelma.SYOTETTY_PERUSOPETUS, Seq.empty, Seq.empty, Instant.now(), "SYOTETTY", None)
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
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio("1.2.246.562.24.21250967212", Lahdejarjestelma.KOSKI, Seq.empty, Seq.empty, Instant.now(), "1.2.3", Some(1))

    // versio joka ei poistettavissa aiheuttaa virheen
    val result = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_SUORITUS_PATH.replace(ApiConstants.UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER, versio.get.tunniste.toString), "")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest)
      .andReturn()

    // tarkistetaan että virhe täsmää
    Assertions.assertEquals(PoistaSuoritusFailureResponse(java.util.Set.of(UIVirheet.UI_POISTA_SUORITUS_SUORITUS_EI_POISTETTAVISSA)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[PoistaSuoritusFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testPoistaPerusopetuksenOppimaaranSuoritusSuoritusAllowed(): Unit =
    // tallennetaan versio
    val oppijaNumero = "1.2.246.562.24.21250967211"
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, Lahdejarjestelma.SYOTETTY_PERUSOPETUS, Seq.empty, Seq.empty, Instant.now(), "SYOTETTY", None)

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
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, Lahdejarjestelma.SYOTETYT_OPPIAINEET, Seq.empty, Seq.empty, Instant.now(), "SYOTETTY", None)

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

  /*
   * Integraatiotestit valintadatan haulle
   */

  @WithAnonymousUser
  @Test def testHaeOppijanValintaDataAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    val oppijaNumero = "1.2.246.562.24.21250967211"
    val hakuOid = "1.2.246.562.29.01000000000000013275"

    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_KAYTTAJAN_TIEDOT_PATH, "")
        .queryParam(UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, oppijaNumero)
        .queryParam(UI_VALINTADATA_HAKU_PARAM_NAME, hakuOid)
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().is3xxRedirection())

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

    val yliajettuAvain = AvainArvoConstants.telmaSuoritettuKey
    val yliajettuArvo = "true"
    val yliajoSelite = "Kyllä on Telma suoritettu, katsoin aivan itse eilen."
    val yliajo = AvainArvoYliajo(yliajettuAvain, Some(yliajettuArvo), oppijaNumero, hakuOid, virkailijaOid, yliajoSelite)

    val eriHaunOid = "1.2.246.562.29.01000000000000013918"
    val eriHaunYliajettuAvain = AvainArvoConstants.opistovuosiSuoritettuKey
    val eriHaunYliajettuArvo = "true"
    val eriHaunYliajoSelite = "Kyllä on Opistovuosikin suoritettu, katsoin toista hakua varten itse eilen."
    val eriHaunYliajo = AvainArvoYliajo(eriHaunYliajettuAvain, Some(eriHaunYliajettuArvo), oppijaNumero, eriHaunOid, virkailijaOid, eriHaunYliajoSelite)

    kantaOperaatiot.tallennaYliajot(Seq(yliajo, eriHaunYliajo))
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, Lahdejarjestelma.SYOTETYT_OPPIAINEET, Seq.empty, Seq.empty, Instant.now(), "SYOTETTY", None)

    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero)))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaNumero -> Set(oppijaNumero)))))

    Mockito.when(tarjontaIntegration.getHaku(hakuOid))
      .thenReturn(Some(KoutaHaku(
        oid = hakuOid,
        tila = "julkaistu",
        nimi = Map("fi" -> s"Testi haku $hakuOid"),
        hakutapaKoodiUri = "hakutapa_01",
        kohdejoukkoKoodiUri = Some("haunkohdejoukko_11"),
        hakuajat = List.empty,
        kohdejoukonTarkenneKoodiUri = None,
        hakuvuosi = Some(2022)
      )))
    Mockito.when(hakemuspalveluClient.getHenkilonHakemustenTiedot(oppijaNumero))
      .thenReturn(Future.successful(Map.empty))
    //Todo, lisätään tähän tai toiseen testiin hakemus, ja tarkistetaan että sen tiedot parsiutuvat oikein avain-arvoiksi

    Mockito.when(tarjontaIntegration.getOhjausparametrit(hakuOid))
      .thenReturn(Ohjausparametrit(suoritustenVahvistuspaiva = Some(DateParam(1765290747152L)), valintalaskentapaiva = Some(DateParam(1768290647351L))))

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

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaYliajotOppijalleEiSallittuAvainBadRequest(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967210"
    val virkailijaOid = "1.2.246.562.24.21250967987"
    val hakuOid = "1.2.246.562.29.01000000000000013275"

    val yliajot =
      java.util.List.of(Yliajo(
        avain = Optional.of("hatusta_vetaisty_avain"),
        arvo = Optional.of("arvo"),
        selite = Optional.of("selite")))
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

    Assertions.assertEquals(TallennaYliajotOppijalleFailureResponse(java.util.Set.of(UIValidator.VALIDATION_AVAIN_EI_SALLITTU)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[TallennaYliajotOppijalleFailureResponse]))
  }

  @WithMockUser(value = "1.2.246.562.24.21250967987", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testTallennaYliajotOppijalleAllowed(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967210"
    val virkailijaOid = "1.2.246.562.24.21250967987" //Tämä tieto poimitaan sessiosta, katso MockUser
    val hakuOid = "1.2.246.562.29.01000000000000013275"

    val yliajokey1 = AvainArvoConstants.telmaSuoritettuKey
    val yliajokey2 = AvainArvoConstants.opistovuosiSuoritettuKey

    val yliajot = Seq(
      Yliajo(
        avain = Optional.of(yliajokey1),
        arvo = Optional.of("true"),
        selite = Optional.of("selite 1"),
      ),
      Yliajo(
        avain = Optional.of(yliajokey2),
        arvo = Optional.of("true"),
        selite = Optional.of("selite 2"),
      )
    ).toList.asJava

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
    val tallennetutYliajot = kantaOperaatiot.haeHenkilonYliajot(oppijaNumero, hakuOid)
    Assertions.assertEquals(2, tallennetutYliajot.size)
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
    val avainJokaPoistetaan = AvainArvoConstants.telmaSuoritettuKey
    val avainJotaEiPoisteta = AvainArvoConstants.opistovuosiSuoritettuKey

    val yliajot = Seq(
      Yliajo(
        avain = Optional.of(avainJokaPoistetaan),
        arvo = Optional.of("true"),
        selite = Optional.of("poistettavan selite"),
      ),
      Yliajo(
        avain = Optional.of(avainJotaEiPoisteta),
        arvo = Optional.of("true"),
        selite = Optional.of("ei-poistettavan selite"),
      )
    ).toList.asJava
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
    val tallennetutYliajot = kantaOperaatiot.haeHenkilonYliajot(oppijaNumero, hakuOid)
    Assertions.assertEquals(2, tallennetutYliajot.size)
    Assertions.assertTrue(tallennetutYliajot.exists(_.avain == avainJokaPoistetaan))
    Assertions.assertTrue(tallennetutYliajot.exists(_.avain == avainJotaEiPoisteta))
    Assertions.assertEquals(tallennetutYliajot.map(_.virkailijaOid).toSet, Set("1.2.246.562.24.21250967987"))

    // poistetaan yksi yliajo
    val poistoResult = mvc.perform(MockMvcRequestBuilders
        .delete(ApiConstants.UI_POISTA_YLIAJO_PATH, "")
        .queryParam(UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, oppijaNumero)
        .queryParam(UI_VALINTADATA_HAKU_PARAM_NAME, hakuOid)
        .queryParam(UI_VALINTADATA_AVAIN_PARAM_NAME, avainJokaPoistetaan))
      .andExpect(status().isOk)
      .andReturn()

    // tarkistetaan että poistetun avaimen arvo on nyt tyhjä
    val tallennetutYliajotPoistonJalkeen: Seq[AvainArvoYliajo] = kantaOperaatiot.haeHenkilonYliajot(oppijaNumero, hakuOid)
    Assertions.assertEquals(2, tallennetutYliajotPoistonJalkeen.size)
    Assertions.assertTrue(tallennetutYliajotPoistonJalkeen.exists(ya => ya.avain == avainJokaPoistetaan && ya.arvo.isEmpty))
    Assertions.assertTrue(tallennetutYliajotPoistonJalkeen.exists(_.avain == avainJotaEiPoisteta))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeOppijanEBTutkintoTiedot(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21583363335"
    val oppilaitosOid = "1.2.246.562.10.73383452575"
    val oppilaitosNimi = "European School of Helsinki"

    // Create a generic opiskeluoikeus with EB tutkinto
    val ebTutkinto = EBTutkinto(
      UUID.randomUUID(),
      Kielistetty(
        Some("Eurooppalainen ylioppilastutkinto (EB)"),
        Some("Europeisk studentexamen (EB)"),
        Some("European Baccalaureate (EB)")
      ),
      Koodi("301103", "koulutus", Some(12)),
      fi.oph.suorituspalvelu.business.Oppilaitos(
        Kielistetty(
          Some(oppilaitosNimi),
          Some("Europaskolan i Helsingfors"),
          Some("European School of Helsinki")
        ),
        oppilaitosOid
      ),
      Koodi("lasna", "koskiopiskeluoikeudentila", Some(1)),
      fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      Some(LocalDate.parse("2022-08-15")),
      Some(LocalDate.parse("2023-06-30")),
      Set(
        // L1 oppiaine
        EBOppiaine(
          UUID.randomUUID(),
          Kielistetty(
            Some("Ensimmäinen kieli (L1)"),
            Some("Första språket (L1)"),
            Some("First Language (L1)")
          ),
          Koodi("L1", "eboppiaineet", Some(1)),
          Some(EBLaajuus(4.0, Koodi("4", "opintojenlaajuusyksikko", Some(1)))),
          Koodi("FI", "kieli", Some(1)),
          Set(
            EBOppiaineenOsasuoritus(
              Kielistetty(
                Some("Kirjallinen koe"),
                Some("Skriftligt förhör"),
                Some("Written examination")
              ),
              Koodi("Written", "ebtutkinnonoppiaineenkomponentti", Some(1)),
              EBArvosana(
                Koodi("9.0", "arviointiasteikkoeuropeanschoolofhelsinkifinalmark", Some(1)),
                true
              ),
              None
            ),
            EBOppiaineenOsasuoritus(
              Kielistetty(
                Some("Suullinen koe"),
                Some("Muntligt förhör"),
                Some("Oral examination")
              ),
              Koodi("Oral", "ebtutkinnonoppiaineenkomponentti", Some(1)),
              EBArvosana(
                Koodi("8.5", "arviointiasteikkoeuropeanschoolofhelsinkifinalmark", Some(1)),
                true
              ),
              None
            ),
            EBOppiaineenOsasuoritus(
              Kielistetty(
                Some("Lopullinen arvosana"),
                Some("Slutligt vitsord"),
                Some("Final mark")
              ),
              Koodi("Final", "ebtutkinnonoppiaineenkomponentti", Some(1)),
              EBArvosana(
                Koodi("9.0", "arviointiasteikkoeuropeanschoolofhelsinkifinalmark", Some(1)),
                true
              ),
              None
            )
          )
        ),
        // L2 oppiaine
        EBOppiaine(
          UUID.randomUUID(),
          Kielistetty(
            Some("Toinen kieli (L2)"),
            Some("Andra språket (L2)"),
            Some("Second Language (L2)")
          ),
          Koodi("L2", "eboppiaineet", Some(1)),
          Some(EBLaajuus(3.0, Koodi("4", "opintojenlaajuusyksikko", Some(1)))),
          Koodi("EN", "kieli", Some(1)),
          Set(
            EBOppiaineenOsasuoritus(
              Kielistetty(
                Some("Lopullinen arvosana"),
                Some("Slutligt vitsord"),
                Some("Final mark")
              ),
              Koodi("Final", "ebtutkinnonoppiaineenkomponentti", Some(1)),
              EBArvosana(
                Koodi("8.0", "arviointiasteikkoeuropeanschoolofhelsinkifinalmark", Some(1)),
                true
              ),
              None
            )
          )
        )
      )
    )

    // Save version and EB tutkinto
    val koskiVersio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, Lahdejarjestelma.KOSKI, Seq.empty, Seq.empty, Instant.now(), "1.2.3", Some(1))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(koskiVersio.get, Set(
      GeneerinenOpiskeluoikeus(
        UUID.randomUUID(),
        "1.2.3.4.5",
        Koodi("ebtutkinto", "suorituksentyyppi", Some(1)),
        oppilaitosOid,
        Set(ebTutkinto),
        None,
        List.empty
      )
    ), Seq.empty, ParserVersions.KOSKI)

    // Mock ONR & organisaatiopalvelu
    Mockito.when(onrIntegration.getMasterHenkilosForPersonOids(Set(oppijaNumero)))
      .thenReturn(Future.successful(Map(oppijaNumero -> OnrMasterHenkilo(oppijaNumero, None, None, None, None, None))))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero)))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaNumero -> Set(oppijaNumero)))))
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi(oppilaitosNimi, "Europaskolan i Helsingfors", "European School of Helsinki"), None, Seq.empty, Seq.empty)
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid)).thenReturn(Some(organisaatio))

    val request = OppijanTiedotRequest(Optional.of(oppijaNumero), Optional.empty())
    val result = mvc.perform(MockMvcRequestBuilders
        .post(ApiConstants.UI_TIEDOT_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(request)))
      .andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[OppijanTiedotSuccessResponse])

    Assertions.assertNotNull(response.ebTutkinto)
    val ebTutkintoResponse = response.ebTutkinto.get()

    Assertions.assertEquals("Eurooppalainen ylioppilastutkinto (EB)", ebTutkintoResponse.nimi.fi.get())
    Assertions.assertEquals(oppilaitosOid, ebTutkintoResponse.oppilaitos.oid)
    Assertions.assertEquals(oppilaitosNimi, ebTutkintoResponse.oppilaitos.nimi.fi.get())
    Assertions.assertEquals(fi.oph.suorituspalvelu.resource.ui.SuoritusTila.VALMIS.toString, ebTutkintoResponse.tila.toString)
    Assertions.assertEquals("2022-08-15", ebTutkintoResponse.aloituspaiva.get().toString)
    Assertions.assertEquals("2023-06-30", ebTutkintoResponse.valmistumispaiva.get().toString)

    Assertions.assertEquals(2, ebTutkintoResponse.oppiaineet.size())

    // Validate L1 oppiaine
    val l1Oppiaine = ebTutkintoResponse.oppiaineet.stream()
      .filter(a => a.nimi.fi.get().contains("Ensimmäinen kieli"))
      .findFirst()
      .orElse(null)

    Assertions.assertNotNull(l1Oppiaine)
    Assertions.assertEquals("FI", l1Oppiaine.suorituskieli)
    Assertions.assertEquals(BigDecimal(4.0), l1Oppiaine.laajuus)
    Assertions.assertTrue(l1Oppiaine.written.isPresent)
    Assertions.assertEquals(BigDecimal(9.0), l1Oppiaine.written.get().arvosana)
    Assertions.assertTrue(l1Oppiaine.oral.isPresent)
    Assertions.assertEquals(BigDecimal(8.5), l1Oppiaine.oral.get().arvosana)
    Assertions.assertTrue(l1Oppiaine.`final`.isPresent)
    Assertions.assertEquals(BigDecimal(9.0), l1Oppiaine.`final`.get().arvosana)

    // Validate L2 oppiaine
    val l2Oppiaine = ebTutkintoResponse.oppiaineet.stream()
      .filter(a => a.nimi.fi.get().contains("Toinen kieli"))
      .findFirst()
      .orElse(null)

    Assertions.assertNotNull(l2Oppiaine)
    Assertions.assertEquals("EN", l2Oppiaine.suorituskieli)
    Assertions.assertEquals(BigDecimal(3.0), l2Oppiaine.laajuus)
    Assertions.assertFalse(l2Oppiaine.written.isPresent)
    Assertions.assertFalse(l2Oppiaine.oral.isPresent)
    Assertions.assertTrue(l2Oppiaine.`final`.isPresent)
    Assertions.assertEquals(BigDecimal(8.0), l2Oppiaine.`final`.get().arvosana)

    // Verify audit log
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaTiedotUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), auditLogEntry.target)
  }

  /*
   * Integraatiotestit valintadatan yliajon muutosten haulle
   */

  @WithAnonymousUser
  @Test def testHaeYliajonMuutoshistoriaAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_YLIAJOT_HISTORIA_PATH, "")
        .queryParam(UI_YLIAJOT_HISTORIA_OPPIJANUMERO_PARAM_NAME, ApiConstants.ESIMERKKI_OPPIJANUMERO)
        .queryParam(UI_YLIAJOT_HISTORIA_HAKU_PARAM_NAME, ApiConstants.ESIMERKKI_HAKU_OID)
        .queryParam(UI_YLIAJOT_HISTORIA_AVAIN_PARAM_NAME, AvainArvoConstants.perusopetuksenKieliKey)
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeYliajonMuutoshistoriaNotAllowed(): Unit = {
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_YLIAJOT_HISTORIA_PATH, "")
        .queryParam(UI_YLIAJOT_HISTORIA_OPPIJANUMERO_PARAM_NAME, ApiConstants.ESIMERKKI_OPPIJANUMERO)
        .queryParam(UI_YLIAJOT_HISTORIA_HAKU_PARAM_NAME, ApiConstants.ESIMERKKI_HAKU_OID)
        .queryParam(UI_YLIAJOT_HISTORIA_AVAIN_PARAM_NAME, AvainArvoConstants.perusopetuksenKieliKey)
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isForbidden)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeYliajonMuutoshistoriaBadRequest(): Unit = {
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_YLIAJOT_HISTORIA_PATH, "")
        .queryParam(UI_YLIAJOT_HISTORIA_OPPIJANUMERO_PARAM_NAME, "ei validi oppijanumero")
        .queryParam(UI_YLIAJOT_HISTORIA_HAKU_PARAM_NAME, "ei validi hakuoOid")
        .queryParam(UI_YLIAJOT_HISTORIA_AVAIN_PARAM_NAME, "ei validi avain")
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest)
      .andReturn()

    Assertions.assertEquals(YliajonMuutosHistoriaFailureResponse(java.util.Set.of(
        UIValidator.VALIDATION_OPPIJANUMERO_EI_VALIDI,
        UIValidator.VALIDATION_HAKUOID_EI_VALIDI,
        UIValidator.VALIDATION_AVAIN_EI_VALIDI
      )),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[YliajonMuutosHistoriaFailureResponse]))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeYliajonMuutoshistoriaAllowed(): Unit = {
    val virkailijaEtunimi = "Veera"
    val virkailijaSukunimi = "Virkailija"
    val arvo = "FI"
    val selite = "selite"
    val tallennusHetki = Instant.ofEpochMilli((Instant.now.toEpochMilli/1000)*1000)

    // tallennetaan yliajo
    kantaOperaatiot.tallennaYliajot(Seq(AvainArvoYliajo(AvainArvoConstants.perusopetuksenKieliKey, Some(arvo),
      ApiConstants.ESIMERKKI_OPPIJANUMERO, ApiConstants.ESIMERKKI_HAKU_OID, ApiConstants.ESIMERKKI_YLIAJO_VIRKAILIJA, selite)), tallennusHetki)

    // mockataan onr-vastaus ja tehdään kutsu
    Mockito.when(onrIntegration.getPerustiedotByPersonOids(Set(ApiConstants.ESIMERKKI_YLIAJO_VIRKAILIJA)))
      .thenReturn(Future.successful(Seq(OnrHenkiloPerustiedot(ApiConstants.ESIMERKKI_YLIAJO_VIRKAILIJA, Some(virkailijaEtunimi), Some(virkailijaSukunimi), None))))
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.UI_YLIAJOT_HISTORIA_PATH, "")
        .queryParam(UI_YLIAJOT_HISTORIA_OPPIJANUMERO_PARAM_NAME, ApiConstants.ESIMERKKI_OPPIJANUMERO)
        .queryParam(UI_YLIAJOT_HISTORIA_HAKU_PARAM_NAME, ApiConstants.ESIMERKKI_HAKU_OID)
        .queryParam(UI_YLIAJOT_HISTORIA_AVAIN_PARAM_NAME, AvainArvoConstants.perusopetuksenKieliKey)
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // tuloksena tulee yksi muutos joka vastaa tallennettua yliajoa
    Assertions.assertEquals(YliajonMuutosHistoriaSuccessResponse(java.util.List.of(
      YliajonMuutosUI(Optional.of(arvo), tallennusHetki, Optional.of(s"$virkailijaEtunimi $virkailijaSukunimi"), selite))),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[YliajonMuutosHistoriaSuccessResponse]))

    // tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaValintaDataAvainMuutoksetUI.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.UI_YLIAJOT_HISTORIA_OPPIJANUMERO_PARAM_NAME -> ApiConstants.ESIMERKKI_OPPIJANUMERO,
      ApiConstants.UI_YLIAJOT_HISTORIA_HAKU_PARAM_NAME -> ApiConstants.ESIMERKKI_HAKU_OID,
      ApiConstants.UI_YLIAJOT_HISTORIA_AVAIN_PARAM_NAME -> AvainArvoConstants.perusopetuksenKieliKey
    ), auditLogEntry.target)
  }
}
