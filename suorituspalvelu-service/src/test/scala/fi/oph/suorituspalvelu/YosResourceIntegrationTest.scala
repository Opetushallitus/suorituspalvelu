package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.KKOpiskeluoikeusTila.VOIMASSA
import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, Koodi, Lahdejarjestelma, Opiskeluoikeus, ParserVersions}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, OnrMasterHenkilo, PersonOidsWithAliases, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.{KoodiMetadata, Koodisto, KoutaHaku, KoutaHakukohde}
import fi.oph.suorituspalvelu.parsing.OpiskeluoikeusParsingService
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.api.{YosErrorResponse, YosSuccessResponse, YosVirhe}
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import org.junit.jupiter.api.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.context.bean.`override`.mockito.MockitoBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.Future

class YosResourceIntegrationTest extends BaseIntegraatioTesti {

  @MockitoBean
  val onrIntegration: OnrIntegration = null

  @MockitoBean
  val organisaatioProvider: OrganisaatioProvider = null

  @MockitoBean
  val tarjontaIntegration: TarjontaIntegration = null

  @Autowired
  var opiskeluoikeusParsingService: OpiskeluoikeusParsingService = null

  val HAKIJA_OID = "1.2.246.562.24.21250967215"
  val HAKU_OID = "1.2.246.562.29.00000000000000043630"
  val HAKUKOHDE_OID = "1.2.246.562.20.00000000000000043758"
  val ORGANISAATIO_TUNNISTE = "Nuketehdas"

  @BeforeEach
  def init(): Unit = {
    Mockito.reset(onrIntegration)
    Mockito.reset(tarjontaIntegration)
    Mockito.reset(organisaatioProvider)
    Mockito.reset(koodistoProvider)
    Mockito.when(onrIntegration.getMasterHenkilosForPersonOids(Set(HAKIJA_OID))).thenReturn(Future.successful(Map(HAKIJA_OID -> OnrMasterHenkilo(HAKIJA_OID, None, None, None, None, None))))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(HAKIJA_OID))).thenReturn(Future.successful(PersonOidsWithAliases(Map(HAKIJA_OID -> Set.empty))))
    Mockito.when(tarjontaIntegration.getHaku(HAKU_OID)).thenReturn(Some(
      KoutaHaku(
        oid = HAKU_OID,
        tila = "JULKAISTU",
        nimi = Map.empty,
        hakutapaKoodiUri = "???",
        kohdejoukkoKoodiUri = Some("haunkohdejoukko_12"),
        hakuajat = List.empty,
        kohdejoukonTarkenneKoodiUri = None,
        hakuvuosi = Some(2026))))

    Mockito.when(tarjontaIntegration.getHakukohde(HAKUKOHDE_OID)).thenReturn(
      KoutaHakukohde(
        oid = HAKUKOHDE_OID,
        organisaatioOid = "NukeTehdas",
        nimi = Map.empty,
        voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita = Some(false),
        johtaaTutkintoon = Some(true),
        hakuOid = HAKU_OID,
        koulutusasteKoodiUrit = List("kansallinenkoulutusluokitus2016koulutusastetaso2_72")
      ))
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(ORGANISAATIO_TUNNISTE)).thenReturn(None)
  }

  @WithAnonymousUser
  @Test def testRedirectsToIdentificationAnonymousUser(): Unit =
    mvc.perform(jsonGet(s"${ApiConstants.YOS_PATH}/hakija/$HAKIJA_OID/haku/$HAKU_OID/hakukohde/$HAKUKOHDE_OID/opiskeluoikeudet"))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "Hui Haamu", authorities = Array())
  @Test def testUsingYosNotAllowed(): Unit = {
    val result = mvc.perform(jsonGet(s"${ApiConstants.YOS_PATH}/hakija/$HAKIJA_OID/haku/$HAKU_OID/hakukohde/$HAKUKOHDE_OID/opiskeluoikeudet"))
      .andExpect(status().isForbidden).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[YosErrorResponse])
    Assertions.assertEquals(YosVirhe.PUUTTUVAT_OIKEUDET, response.virhe)
  }

  @WithMockUser(value = "Ruhtinas Nukettaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testInvalidParameters(): Unit = {
    val result = mvc.perform(jsonGet(s"${ApiConstants.YOS_PATH}/hakija/$HAKIJA_OID/haku/haku-oid-jotain/hakukohde/$HAKUKOHDE_OID/opiskeluoikeudet"))
      .andExpect(status().isBadRequest).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[YosErrorResponse])
    Assertions.assertEquals(YosVirhe.PUUTTEELLISET_PARAMETRIT, response.virhe)
  }

  @WithMockUser(value = "Ruhtinas Nukettaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testReturnsEmptyListForHakijaWithNoOpiskeluOikeuksia(): Unit = {
    Mockito.when(organisaatioProvider.haeKaikkiOrganisaationParenttienOidit(any())).thenReturn(List.empty)
    val result = mvc.perform(jsonGet(s"${ApiConstants.YOS_PATH}/hakija/$HAKIJA_OID/haku/$HAKU_OID/hakukohde/$HAKUKOHDE_OID/opiskeluoikeudet"))
      .andExpect(status().isOk).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[YosSuccessResponse])
    Assertions.assertTrue(response.paatettavatOpiskeluOikeudet.isEmpty)
  }

  @WithMockUser(value = "Ruhtinas Nukettaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testReturnsPaatettavatOpiskeluOikeudet(): Unit = {
    insertOpiskeluOikeus()
    Mockito.when(organisaatioProvider.haeKaikkiOrganisaationParenttienOidit(any())).thenReturn(List.empty)
    Mockito.when(koodistoProvider.haeKoodisto("koulutus")).thenReturn(Map("koulutus_1" ->
      fi.oph.suorituspalvelu.integration.client.Koodi(koodiArvo = "1", koodisto = Koodisto("koulutus"), metadata = List(KoodiMetadata(kieli = "fi", nimi = "Agrologi")), koodiUri = "koulutus_1")))
    Mockito.when(koodistoProvider.haeAlakoodit("koulutus_1")).thenReturn(List(fi.oph.suorituspalvelu.integration.client.Koodi("72", Koodisto("kansallinenkoulutusluokitus2016koulutusastetaso2"), List.empty, "kansallinenkoulutusluokitus2016koulutusastetaso2_72")))

    val result = mvc.perform(jsonGet(s"${ApiConstants.YOS_PATH}/hakija/$HAKIJA_OID/haku/$HAKU_OID/hakukohde/$HAKUKOHDE_OID/opiskeluoikeudet"))
      .andExpect(status().isOk).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[YosSuccessResponse])
    Assertions.assertEquals(1, response.paatettavatOpiskeluOikeudet.size())
    Assertions.assertEquals("Laivan rakennusala", response.paatettavatOpiskeluOikeudet.get(0).virtaNimi.fi)
    Assertions.assertEquals("Agrologi", response.paatettavatOpiskeluOikeudet.get(0).supaNimi.fi)
    // tarkistetaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaePaattyvatOpiskeluOikeudet.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "henkiloOid" -> HAKIJA_OID,
      "hakuOid" -> HAKU_OID,
      "hakukohdeOid" -> HAKUKOHDE_OID,
    ), auditLogEntry.target)
  }

  @WithMockUser(value = "Ruhtinas Nukettaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testReturnsErrorVirheHakutoiveenPaattelyssa(): Unit = {
    Mockito.when(tarjontaIntegration.getHaku(HAKU_OID)).thenReturn(None)
    val result = mvc.perform(jsonGet(s"${ApiConstants.YOS_PATH}/hakija/$HAKIJA_OID/haku/$HAKU_OID/hakukohde/$HAKUKOHDE_OID/opiskeluoikeudet"))
      .andExpect(status().is5xxServerError()).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[YosErrorResponse])
    Assertions.assertEquals(YosVirhe.VIRHE_HAKUTOIVEEN_PAATTELYSSA, response.virhe)
  }

  @WithMockUser(value = "Ruhtinas Nukettaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testReturnsErrorVirhePaattyvienOpiskeluOikeuksienHaussa(): Unit = {
    insertOpiskeluOikeus()
    Mockito.when(organisaatioProvider.haeKaikkiOrganisaationParenttienOidit(any())).thenReturn(List.empty)
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(ORGANISAATIO_TUNNISTE)).thenThrow(new RuntimeException("FAIL"))
    val result = mvc.perform(jsonGet(s"${ApiConstants.YOS_PATH}/hakija/$HAKIJA_OID/haku/$HAKU_OID/hakukohde/$HAKUKOHDE_OID/opiskeluoikeudet"))
      .andExpect(status().is5xxServerError()).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[YosErrorResponse])
    Assertions.assertEquals(YosVirhe.VIRHE_PAATETTAVIEN_OPISKELUOIKEUKSIEN_HAUSSA, response.virhe)
  }

  private def insertOpiskeluOikeus(): Unit = {
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(HAKIJA_OID, Lahdejarjestelma.VIRTA, Seq.empty, Seq.empty, Instant.now(), "VIRTA", None).get
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(KKOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      virtaTunniste = "",
      tyyppiKoodi = "1",
      koulutusKoodi = Some("koulutus_1"),
      alkuPvm = LocalDate.now(),
      loppuPvm = LocalDate.now(),
      virtaTila = Koodi("1", "koodisto", None),
      supaTila = VOIMASSA,
      myontaja = ORGANISAATIO_TUNNISTE,
      isTutkintoonJohtava = true,
      kieli = Some("fi"),
      suoritukset = Set.empty,
      rahoitusLahde = Some("5"),
      nimi = Some(Kielistetty(Some("Laivan rakennusala"), None, None)),
      luokittelu = Some("3")
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, opiskeluoikeudet, Seq.empty, ParserVersions.VIRTA)
  }
}

