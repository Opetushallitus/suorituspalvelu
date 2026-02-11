package fi.oph.suorituspalvelu

import com.fasterxml.jackson.core.`type`.TypeReference
import fi.oph.suorituspalvelu.integration.{OnrIntegration, PersonOidsWithAliases, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, DateParam, HakemuspalveluClientImpl, Hakutoive, KoutaHakukohde, Ohjausparametrit}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, HarkinnanvaraisuudenSyy}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.api.{HakemustenHarkinnanvaraisuudetPayload, HarkinnanvaraisuusFailureResponse, ValintaApiHakemuksenHarkinnanvaraisuus}
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import org.springframework.test.context.bean.`override`.mockito.MockitoBean
import org.mockito.Mockito
import org.mockito.ArgumentMatchers.any
import org.junit.jupiter.api.{Assertions, Test}
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*


/**
 * Harkinnanvaraisuus-apin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että vastaus sisältää oikeita tietoja.
 */
class HarkinnanvaraisuusResourceIntegraatioTest extends BaseIntegraatioTesti {

  @MockitoBean
  val onrIntegration: OnrIntegration = null

  @MockitoBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @MockitoBean
  val tarjontaIntegration: TarjontaIntegration = null

  /*
   * Integraatiotestit harkinnanvaraisuuden haulle
   */

  @WithAnonymousUser
  @Test def testHaeHarkinnanvaraisuudetAnonymousIsRedirected(): Unit = {
    val hakemusOid = "1.2.246.562.11.01000000000000023251"
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().is3xxRedirection())
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA))
  @Test def testHaeHarkinnanvaraisuudetNotAllowed(): Unit = {
    val hakemusOid = "1.2.246.562.11.01000000000000023251"
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isForbidden)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeHarkinnanvaraisuudetBadRequestEmptyHakemusOids(): Unit = {
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List().asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(
      HarkinnanvaraisuusFailureResponse(java.util.List.of(ApiConstants.HARKINNANVARAISUUS_PUUTTUVA_PARAMETRI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[HarkinnanvaraisuusFailureResponse])
    )
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeHarkinnanvaraisuudetBadRequestInvalidHakemusOid(): Unit = {
    val invalidHakemusOid = "1.2.246.562.20.00000000000000000001" //HakukohdeOid
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(invalidHakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest).andReturn()

    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[HarkinnanvaraisuusFailureResponse])
    Assertions.assertTrue(response.virheet.asScala.exists(_.contains(invalidHakemusOid)))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeHarkinnanvaraisuudetBadRequestTooManyHakemusOids(): Unit = {
    // Create a list with more than the allowed max number of hakemusOids
    val manyHakemusOids = (1 to ApiConstants.HARKINNANVARAISUUS_HAKEMUKSET_MAX_MAARA + 1)
      .map(i => s"1.2.246.562.11.0100000000000000${i}")
      .toList

    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(manyHakemusOids.asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(
      HarkinnanvaraisuusFailureResponse(java.util.List.of(ApiConstants.HARKINNANVARAISUUS_HAKEMUKSET_LIIKAA)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[HarkinnanvaraisuusFailureResponse])
    )
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeHarkinnanvaraisuudetSuccessful(): Unit = {
    val personOid = "1.2.246.562.24.21250967211"
    val hakemusOid = "1.2.246.562.11.01000000000000023251"
    val hakuOid = "1.2.246.562.29.01000000000000013275"
    val hakukohdeOid1 = "1.2.246.562.20.00000000000000000001"
    val hakukohdeOid2 = "1.2.246.562.20.00000000000000000002"

    val testHakemus = AtaruValintalaskentaHakemus(
      hakemusOid = hakemusOid,
      personOid = personOid,
      hakuOid = hakuOid,
      asiointikieli = "fi",
      hakutoiveet = List(
        Hakutoive(
          processingState = "unprocessed",
          eligibilityState = "eligible",
          paymentObligation = "not-obligated",
          kkApplicationPaymentObligation = "unreviewed",
          hakukohdeOid = hakukohdeOid1,
          languageRequirement = "unreviewed",
          degreeRequirement = "unreviewed",
          harkinnanvaraisuus = None
        ),
        Hakutoive(
          processingState = "unprocessed",
          eligibilityState = "eligible",
          paymentObligation = "not-obligated",
          kkApplicationPaymentObligation = "unreviewed",
          hakukohdeOid = hakukohdeOid2,
          languageRequirement = "unreviewed",
          degreeRequirement = "unreviewed",
          harkinnanvaraisuus = Some("harkinnanvaraisesti_hyvaksyttavissa")
        )
      ),
      maksuvelvollisuus = Map.empty,
      keyValues = Map(
        "address" -> "Testitie 123",
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017",
        "harkinnanvaraisuus-reason_" + hakukohdeOid2 -> "0"  // 0 = ATARU_OPPIMISVAIKEUDET
      )
    )

    val hakukohde1 = KoutaHakukohde(
      oid = hakukohdeOid1,
      voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita = Some(true)
    )
    val hakukohde2 = KoutaHakukohde(
      oid = hakukohdeOid2,
      voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita = Some(true)
    )

    val ohjausparametrit = Ohjausparametrit(
      suoritustenVahvistuspaiva = Some(DateParam(1765290747152L)),
      valintalaskentapaiva = Some(DateParam(1768290647351L))
    )

    // Set up mocks for dependencies
    Mockito.when(hakemuspalveluClient.getValintalaskentaHakemukset(None, true, Set(hakemusOid)))
      .thenReturn(Future.successful(Seq(testHakemus)))

    Mockito.when(tarjontaIntegration.getHakukohde(hakukohdeOid1))
      .thenReturn(hakukohde1)

    Mockito.when(tarjontaIntegration.getHakukohde(hakukohdeOid2))
      .thenReturn(hakukohde2)

    Mockito.when(tarjontaIntegration.getOhjausparametrit(hakuOid))
      .thenReturn(ohjausparametrit)

    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(personOid)))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(personOid -> Set(personOid)))))

    // Execute the request
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // Check audit log
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeHakemustenHarkinnanvaraisuudet.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "hakemusOids" -> s"Array($hakemusOid)"
    ), auditLogEntry.target)

    // Parse and verify the response
    val typeRef = new TypeReference[List[ValintaApiHakemuksenHarkinnanvaraisuus]] {}
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), typeRef)
    Assertions.assertEquals(1, response.size)

    val harkinnanvaraisuus: ValintaApiHakemuksenHarkinnanvaraisuus = response.head
    Assertions.assertEquals(hakemusOid, harkinnanvaraisuus.hakemusOid)
    Assertions.assertEquals(personOid, harkinnanvaraisuus.henkiloOid)

    // Check hakutoiveet
    Assertions.assertEquals(2, harkinnanvaraisuus.hakutoiveet.size())

    // First hakutoive should be EI_HARKINNANVARAINEN
    val hakutoive1 = harkinnanvaraisuus.hakutoiveet.asScala.find(_.hakukohdeOid == hakukohdeOid1).get
    Assertions.assertEquals(HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN.toString, hakutoive1.harkinnanvaraisuudenSyy)

    // Second hakutoive should be ATARU_OPPIMISVAIKEUDET based on the keyValues in the hakemus
    val hakutoive2 = harkinnanvaraisuus.hakutoiveet.asScala.find(_.hakukohdeOid == hakukohdeOid2).get
    Assertions.assertEquals(HarkinnanvaraisuudenSyy.ATARU_OPPIMISVAIKEUDET.toString, hakutoive2.harkinnanvaraisuudenSyy)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeHarkinnanvaraisuudetWithPalvelukayttaja(): Unit = {
    val hakemusOid = "1.2.246.562.11.01000000000000023251"

    // Setup minimal mocks to pass
    Mockito.when(hakemuspalveluClient.getValintalaskentaHakemukset(any(), any(), any()))
      .thenReturn(Future.successful(Seq.empty))

    // Execute the request
    mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // Verify that service method was called
    Mockito.verify(hakemuspalveluClient).getValintalaskentaHakemukset(None, true, Set(hakemusOid))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SUPA_VALINTAKAYTTAJA_CRUD))
  @Test def testHaeHarkinnanvaraisuudetWithValintaKayttajaCrud(): Unit = {
    val hakemusOid = "1.2.246.562.11.01000000000000023251"

    // Setup minimal mocks to pass
    Mockito.when(hakemuspalveluClient.getValintalaskentaHakemukset(any(), any(), any()))
      .thenReturn(Future.successful(Seq.empty))

    // Execute the request
    mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // Verify that service method was called
    Mockito.verify(hakemuspalveluClient).getValintalaskentaHakemukset(None, true, Set(hakemusOid))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SUPA_VALINTAKAYTTAJA_READ_UPDATE))
  @Test def testHaeHarkinnanvaraisuudetWithValintaKayttajaReadUpdate(): Unit = {
    val hakemusOid = "1.2.246.562.11.01000000000000023251"

    // Setup minimal mocks to pass
    Mockito.when(hakemuspalveluClient.getValintalaskentaHakemukset(any(), any(), any()))
      .thenReturn(Future.successful(Seq.empty))

    // Execute the request
    mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // Verify that service method was called
    Mockito.verify(hakemuspalveluClient).getValintalaskentaHakemukset(None, true, Set(hakemusOid))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeHarkinnanvaraisuudetServerError(): Unit = {
    val hakemusOid = "1.2.246.562.11.01000000000000023251"

    // Setup mock to throw an exception
    Mockito.when(hakemuspalveluClient.getValintalaskentaHakemukset(None, true, Set(hakemusOid)))
      .thenReturn(Future.failed(new RuntimeException("Test error")))

    // Execute the request
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isInternalServerError)
      .andReturn()

    // Check response contains expected error message
    Assertions.assertEquals(
      HarkinnanvaraisuusFailureResponse(java.util.List.of(ApiConstants.HARKINNANVARAISUUS_500_VIRHE)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[HarkinnanvaraisuusFailureResponse])
    )
  }
}
