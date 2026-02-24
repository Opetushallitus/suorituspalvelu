
package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, EBTutkinto, GeneerinenOpiskeluoikeus, Koodi, Lahdejarjestelma, Opiskeluoikeus, Oppilaitos, ParserVersions, YOOpiskeluoikeus, YOTutkinto}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.api.{AutomaattinenHakukelpoisuusFailureResponse, AutomaattinenHakukelpoisuusPayload, AutomaattinenHakukelpoisuusSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.validation.Validator
import org.junit.jupiter.api.{Assertions, Test}
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.context.bean.`override`.mockito.MockitoBean
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

  private val PERSON_OID_1 = "1.2.246.562.24.21250967211"
  private val PERSON_OID_2 = "1.2.246.562.24.21250967212"
  private val OPPILAITOS_OID = "1.2.246.562.10.12345678901"
  val OPISKELUOIKEUS_OID = "1.2.246.562.15.94501385312"

  /*
   * Integraatiotestit automaattisen hakukelpoisuuden haulle
   */

  @WithAnonymousUser
  @Test def testHaeAutomaattisetHakukelpoisuudetAnonymousIsRedirected(): Unit = {
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(List(PERSON_OID_1).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().is3xxRedirection())
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA))
  @Test def testHaeAutomaattisetHakukelpoisuudetNotAllowed(): Unit = {
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(List(PERSON_OID_1).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isForbidden)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetBadRequestEmptyHenkiloOids(): Unit = {
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(List().asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(
      AutomaattinenHakukelpoisuusFailureResponse(java.util.List.of(ApiConstants.AUTOM_HAKUKELPOISUUS_PUUTTUVA_PARAMETRI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusFailureResponse])
    )
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetBadRequestInvalidHenkiloOid(): Unit = {
    val invalidHenkiloOid = "1.2.246.562.20.00000000000000000001" // HakukohdeOid
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(List(invalidHenkiloOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest).andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusFailureResponse])
    Assertions.assertTrue(response.virheet.asScala.exists(_.contains(Validator.VALIDATION_OPPIJANUMERO_EI_VALIDI)))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetBadRequestTooManyHenkiloOids(): Unit = {
    val manyHenkiloOids = (1 to ApiConstants.AUTOM_HAKUKELPOISUUS_HENKILOT_MAX_MAARA + 1)
      .map(i => s"1.2.246.562.24.2125096721${i}")
      .toList

    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(manyHenkiloOids.asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(
      AutomaattinenHakukelpoisuusFailureResponse(java.util.List.of(ApiConstants.AUTOM_HAKUKELPOISUUS_HENKILOT_LIIKAA)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusFailureResponse])
    )
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

    setupMocksWithOpiskeluoikeudet(PERSON_OID_1, Seq(yoOpiskeluoikeus))

    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(List(PERSON_OID_1).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeHenkiloidenAutomaattisetHakukelpoisuudet.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "henkiloOids" -> s"Array($PERSON_OID_1)"
    ), auditLogEntry.target)

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertEquals(1, response.automaattisetHakukelpoisuudet.size())
    Assertions.assertTrue(response.automaattisetHakukelpoisuudet.get(PERSON_OID_1))
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

    setupMocksWithOpiskeluoikeudet(PERSON_OID_1, Seq(geneerinenOpiskeluoikeus))

    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(List(PERSON_OID_1).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertEquals(1, response.automaattisetHakukelpoisuudet.size())
    Assertions.assertTrue(response.automaattisetHakukelpoisuudet.get(PERSON_OID_1))
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

    setupMocksWithOpiskeluoikeudet(PERSON_OID_1, Seq(ammatillinenOpiskeluoikeus))

   val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(List(PERSON_OID_1).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertEquals(1, response.automaattisetHakukelpoisuudet.size())
    Assertions.assertTrue(response.automaattisetHakukelpoisuudet.get(PERSON_OID_1))
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

    setupMocksWithOpiskeluoikeudet(PERSON_OID_1, Seq(yoOpiskeluoikeus))

    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(List(PERSON_OID_1).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertEquals(1, response.automaattisetHakukelpoisuudet.size())
    Assertions.assertFalse(response.automaattisetHakukelpoisuudet.get(PERSON_OID_1)) // Ei automaattisesti hakukelpoinen
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetMultiplePersons(): Unit = {
    // Henkilö 1: YO-tutkinto valmis
    val yoTutkinto = YOTutkinto(
      tunniste = UUID.randomUUID(),
      suoritusKieli = Koodi("FI", "kieli", None),
      supaTila = VALMIS,
      valmistumisPaiva = Some(LocalDate.now().minusDays(1)),
      aineet = Set.empty
    )

    val yoOpiskeluoikeus = YOOpiskeluoikeus(
      UUID.randomUUID(),
      Some(yoTutkinto)
    )

    // Henkilö 2: Valmis ammatillinen perustutkinto
    val ammatillinenPerustutkinto = AmmatillinenPerustutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("Testiperuskoulutus"), None, None),
      koodi = Koodi("AMMATILLINEN", "koulutus", None),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("lasna", "koskiopiskeluoikeudentila", None),
      supaTila = KESKEN,
      aloitusPaivamaara = Some(LocalDate.now().minusYears(2)),
      vahvistusPaivamaara = None,
      keskiarvo = None,
      suoritustapa = Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      suoritusKieli = Koodi("FI", "kieli", None),
      osat = Set.empty
    )

    val ammatillinenOpiskeluoikeus = AmmatillinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPPILAITOS_OID,
      oppilaitos = createOppilaitos(OPPILAITOS_OID),
      suoritukset = Set(ammatillinenPerustutkinto),
      tila = None,
      jaksot = List.empty
    )

    setupMocksWithOpiskeluoikeudet(PERSON_OID_1, Seq(yoOpiskeluoikeus))
    setupMocksWithOpiskeluoikeudet(PERSON_OID_2, Seq(ammatillinenOpiskeluoikeus))

    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(List(PERSON_OID_1, PERSON_OID_2).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertEquals(2, response.automaattisetHakukelpoisuudet.size())
    Assertions.assertTrue(response.automaattisetHakukelpoisuudet.get(PERSON_OID_1)) // Automaattisesti hakukelpoinen
    Assertions.assertFalse(response.automaattisetHakukelpoisuudet.get(PERSON_OID_2)) // Ei automaattisesti hakukelpoinen
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAutomaattisetHakukelpoisuudetNoOpiskeluoikeudet(): Unit = {
    // Person with no opiskeluoikeudet
    setupMocksWithOpiskeluoikeudet(PERSON_OID_1, Seq.empty)

    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HAKUKELPOISUUS_PATH, AutomaattinenHakukelpoisuusPayload(List(PERSON_OID_1).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AutomaattinenHakukelpoisuusSuccessResponse])
    Assertions.assertEquals(1, response.automaattisetHakukelpoisuudet.size())
    Assertions.assertFalse(response.automaattisetHakukelpoisuudet.get(PERSON_OID_1)) // Ei automaattisesti hakukelpoinen
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
