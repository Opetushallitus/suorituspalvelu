
package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmattiTutkinto, EBTutkinto, GeneerinenOpiskeluoikeus, Koodi, Lahdejarjestelma, Opiskeluoikeus, Oppilaitos, ParserVersions, YOOpiskeluoikeus, YOTutkinto}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.api.{AutomaattinenHakukelpoisuusFailureResponse, AutomaattinenHakukelpoisuusSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.validation.Validator
import org.junit.jupiter.api.{Assertions, Test}
import org.mockito.Mockito
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.context.bean.`override`.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

/**
 * Automaattisen hakukelpoisuuden integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien
 * kaikki eri paluuarvoihin johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset
 * validointiongelmat) testataan yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että vastaus sisältää
 * oikeita tietoja.
 */
class AutomaattinenHakukelpoisuusResourceIntegraatioTest extends BaseIntegraatioTesti {

  @MockitoBean
  val onrIntegration: OnrIntegration = null

  private val HENKILO_OID = "1.2.246.562.24.21250967211"
  private val OPPILAITOS_OID = "1.2.246.562.10.12345678901"
  private val OPISKELUOIKEUS_OID = "1.2.246.562.15.94501385312"

  /*
   * Integraatiotestit automaattisen hakukelpoisuuden haulle
   */

  @WithAnonymousUser
  @Test def testHaeAutomaattisetHakukelpoisuudetAnonymousIsRedirected(): Unit = {
    mvc.perform(MockMvcRequestBuilders.get(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH
        .replace(ApiConstants.HAKUKELPOISUUS_HENKILO_PARAM_PLACEHOLDER, HENKILO_OID), ""))
        .andExpect(status().is3xxRedirection())
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA))
  @Test def testHaeAutomaattisetHakukelpoisuudetNotAllowed(): Unit = {
    mvc.perform(MockMvcRequestBuilders.get(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH
        .replace(ApiConstants.HAKUKELPOISUUS_HENKILO_PARAM_PLACEHOLDER, HENKILO_OID), ""))
      .andExpect(status().isForbidden)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetBadRequestInvalidHenkiloOid(): Unit = {
    val invalidHenkiloOid = "1.2.246.562.20.00000000000000000001" // HakukohdeOid

    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH
        .replace(ApiConstants.HAKUKELPOISUUS_HENKILO_PARAM_PLACEHOLDER, invalidHenkiloOid), ""))
      .andExpect(status().isBadRequest).andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusFailureResponse])
    println(s"$response")
    Assertions.assertTrue(response.virheet.asScala.exists(_.contains(Validator.VALIDATION_HENKILOOID_EI_VALIDI)))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetSuccessfulWithYOTutkinto(): Unit = {
    val yoTutkinto = YOTutkinto(
      tunniste = UUID.randomUUID(),
      suoritusKieli = Koodi("FI", "kieli", None),
      supaTila = VALMIS,
      valmistumisPaiva = Some(LocalDate.now().minusDays(30)),
      aineet = Set.empty
    )

    val yoOpiskeluoikeus = YOOpiskeluoikeus(
      UUID.randomUUID(),
      Some(yoTutkinto)
    )

    setupMocksWithOpiskeluoikeudet(HENKILO_OID, Seq(yoOpiskeluoikeus))

    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH
        .replace(ApiConstants.HAKUKELPOISUUS_HENKILO_PARAM_PLACEHOLDER, HENKILO_OID), ""))
      .andExpect(status().isOk).andReturn()

    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeHenkiloidenAutomaattisetHakukelpoisuudet.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "henkiloOid" -> s"$HENKILO_OID"
    ), auditLogEntry.target)

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertTrue(response.automaattisestiHakukelpoinen)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetSuccessfulWithEBTutkinto(): Unit = {
    // Valmis EB tutkinto
    val ebTutkinto = EBTutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("European Baccalaureate"), Some("Europeisk studentexamen"), Some("European Baccalaureate")),
      koodi = Koodi("301103", "koulutus", Some(12)),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", None),
      supaTila = VALMIS,
      aloitusPaivamaara = Some(LocalDate.now().minusYears(2)),
      vahvistusPaivamaara = Some(LocalDate.now().minusDays(60)),
      osasuoritukset = Set.empty
    )

    val geneerinenOpiskeluoikeus = GeneerinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPISKELUOIKEUS_OID,
      tyyppi = Koodi("ebtutkinto", "opiskeluoikeudentyyppi", None),
      oppilaitosOid = OPPILAITOS_OID,
      suoritukset = Set(ebTutkinto),
      tila = None,
      jaksot = List.empty
    )

    setupMocksWithOpiskeluoikeudet(HENKILO_OID, Seq(geneerinenOpiskeluoikeus))

    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH
        .replace(ApiConstants.HAKUKELPOISUUS_HENKILO_PARAM_PLACEHOLDER, HENKILO_OID), ""))
      .andExpect(status().isOk).andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertTrue(response.automaattisestiHakukelpoinen)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetSuccessfulWithAmmatillinenTutkinto(): Unit = {
    // Create test data with ammatillinen tutkinto
    val ammattiTutkinto = AmmattiTutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("Ammattitutkinto"), None, None),
      koodi = Koodi("AMMATTI", "koulutus", None),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", None),
      supaTila = VALMIS,
      aloitusPaivamaara = Some(LocalDate.now().minusYears(1)),
      vahvistusPaivamaara = Some(LocalDate.now().minusDays(15)),
      suoritustapa = Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      suoritusKieli = Koodi("FI", "kieli", None)
    )

    val ammatillinenOpiskeluoikeus = AmmatillinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPPILAITOS_OID,
      oppilaitos = createOppilaitos(OPPILAITOS_OID),
      suoritukset = Set(ammattiTutkinto),
      tila = None,
      jaksot = List.empty
    )

    setupMocksWithOpiskeluoikeudet(HENKILO_OID, Seq(ammatillinenOpiskeluoikeus))

    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH
        .replace(ApiConstants.HAKUKELPOISUUS_HENKILO_PARAM_PLACEHOLDER, HENKILO_OID), ""))
      .andExpect(status().isOk).andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertTrue(response.automaattisestiHakukelpoinen)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetNotEligibleWithKeskenerainen(): Unit = {
    // Keskeneräinen YO tutkinto
    val yoTutkinto = YOTutkinto(
      tunniste = UUID.randomUUID(),
      suoritusKieli = Koodi("FI", "kieli", None),
      supaTila = KESKEN, // Not completed
      valmistumisPaiva = None,
      aineet = Set.empty
    )

    val yoOpiskeluoikeus = YOOpiskeluoikeus(
      UUID.randomUUID(),
      Some(yoTutkinto)
    )

    setupMocksWithOpiskeluoikeudet(HENKILO_OID, Seq(yoOpiskeluoikeus))

    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH
        .replace(ApiConstants.HAKUKELPOISUUS_HENKILO_PARAM_PLACEHOLDER, HENKILO_OID), ""))
      .andExpect(status().isOk).andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertEquals(response.henkiloOid, HENKILO_OID)
    Assertions.assertFalse(response.automaattisestiHakukelpoinen) // Ei automaattisesti hakukelpoinen
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetNoOpiskeluoikeudet(): Unit = {
    // Person with no opiskeluoikeudet
    setupMocksWithOpiskeluoikeudet(HENKILO_OID, Seq.empty)

    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH
        .replace(ApiConstants.HAKUKELPOISUUS_HENKILO_PARAM_PLACEHOLDER, HENKILO_OID), ""))
      .andExpect(status().isOk).andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertFalse(response.automaattisestiHakukelpoinen) // Ei automaattisesti hakukelpoinen
  }

  private def setupMocksWithOpiskeluoikeudet(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): Unit = {
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(personOid)))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(personOid -> Set(personOid)))))
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(personOid, Lahdejarjestelma.KOSKI, Seq.empty, Seq.empty, Instant.now(), "SYOTETTY", Some(1)).get
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, opiskeluoikeudet.toSet, Seq.empty, ParserVersions.SYOTETYT_OPPIAINEET)
  }

  private def createOppilaitos(oid: String = OPPILAITOS_OID): Oppilaitos = {
    Oppilaitos(
      Kielistetty(Some("Testikoulu"), None, None),
      oid
    )
  }

}
