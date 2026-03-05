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
      organisaatioOid = "1.2.3",
      nimi = Map.empty,
      voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita = Some(true)
    )
    val hakukohde2 = KoutaHakukohde(
      oid = hakukohdeOid2,
      organisaatioOid = "1.2.3",
      nimi = Map.empty,
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

  /*
   * Integraatiotestit harkinnanvaraisuusyliajojen palauttamiselle
   */

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeHarkinnanvaraisuudetWithYliajoOverridesCalculatedValue(): Unit = {
    val personOid = "1.2.246.562.24.21250967212"
    val hakemusOid = "1.2.246.562.11.01000000000000023252"
    val hakuOid = "1.2.246.562.29.01000000000000013276"
    val hakukohdeOid = "1.2.246.562.20.00000000000000000003"
    val virkailijaOid = "1.2.246.562.24.21250967987"

    // Tallennetaan yliajo joka ylikirjoittaa lasketun harkinnanvaraisuuden
    kantaOperaatiot.tallennaHarkinnanvaraisuusYliajot(Seq(
      fi.oph.suorituspalvelu.business.HarkinnanvaraisuusYliajo(
        hakemusOid = hakemusOid,
        hakukohdeOid = hakukohdeOid,
        harkinnanvaraisuudenSyy = Some(HarkinnanvaraisuudenSyy.ATARU_ULKOMAILLA_OPISKELTU),
        virkailijaOid = virkailijaOid,
        selite = "Manuaalinen yliajo testissä"
      )
    ))

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
          hakukohdeOid = hakukohdeOid,
          languageRequirement = "unreviewed",
          degreeRequirement = "unreviewed",
          harkinnanvaraisuus = Some("harkinnanvaraisesti_hyvaksyttavissa")
        )
      ),
      maksuvelvollisuus = Map.empty,
      keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017",
        "harkinnanvaraisuus-reason_" + hakukohdeOid -> "1"  // 1 = ATARU_OPPIMISVAIKEUDET (eri kuin yliajossa)
      )
    )

    val hakukohde = KoutaHakukohde(
      oid = hakukohdeOid,
      organisaatioOid = "1.2.3",
      nimi = Map.empty,
      voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita = Some(true)
    )

    val ohjausparametrit = Ohjausparametrit(
      suoritustenVahvistuspaiva = Some(DateParam(1765290747152L)),
      valintalaskentapaiva = Some(DateParam(1768290647351L))
    )

    // Setup mocks
    Mockito.when(hakemuspalveluClient.getValintalaskentaHakemukset(None, true, Set(hakemusOid)))
      .thenReturn(Future.successful(Seq(testHakemus)))

    Mockito.when(tarjontaIntegration.getHakukohde(hakukohdeOid))
      .thenReturn(hakukohde)

    Mockito.when(tarjontaIntegration.getOhjausparametrit(hakuOid))
      .thenReturn(ohjausparametrit)

    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(personOid)))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(personOid -> Set(personOid)))))

    // Execute request
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // Verify response
    val typeRef = new TypeReference[List[ValintaApiHakemuksenHarkinnanvaraisuus]] {}
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), typeRef)
    Assertions.assertEquals(1, response.size)

    val harkinnanvaraisuus = response.head
    Assertions.assertEquals(hakemusOid, harkinnanvaraisuus.hakemusOid)
    Assertions.assertEquals(1, harkinnanvaraisuus.hakutoiveet.size())

    // Varmistetaan että yliajo on voimassa - ATARU_ULKOMAILLA_OPISKELTU eikä ATARU_OPPIMISVAIKEUDET
    val hakutoive = harkinnanvaraisuus.hakutoiveet.asScala.head
    Assertions.assertEquals(hakukohdeOid, hakutoive.hakukohdeOid)
    Assertions.assertEquals(HarkinnanvaraisuudenSyy.ATARU_ULKOMAILLA_OPISKELTU.toString, hakutoive.harkinnanvaraisuudenSyy,
      "Yliajon pitäisi ylikirjoittaa laskettu harkinnanvaraisuus")
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeHarkinnanvaraisuudetMultipleHakutoiveetWithMixedYliajot(): Unit = {
    val personOid = "1.2.246.562.24.21250967213"
    val hakemusOid = "1.2.246.562.11.01000000000000023253"
    val hakuOid = "1.2.246.562.29.01000000000000013277"
    val hakukohdeOid1 = "1.2.246.562.20.00000000000000000004"
    val hakukohdeOid2 = "1.2.246.562.20.00000000000000000005"
    val hakukohdeOid3 = "1.2.246.562.20.00000000000000000006"
    val virkailijaOid = "1.2.246.562.24.21250967987"

    // Tallennetaan yliajo vain yhdelle hakutoiveelle
    kantaOperaatiot.tallennaHarkinnanvaraisuusYliajot(Seq(
      fi.oph.suorituspalvelu.business.HarkinnanvaraisuusYliajo(
        hakemusOid = hakemusOid,
        hakukohdeOid = hakukohdeOid2,
        harkinnanvaraisuudenSyy = Some(HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA),
        virkailijaOid = virkailijaOid,
        selite = "Yliajo toiselle hakutoiveelle"
      )
    ))

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
        ),
        Hakutoive(
          processingState = "unprocessed",
          eligibilityState = "eligible",
          paymentObligation = "not-obligated",
          kkApplicationPaymentObligation = "unreviewed",
          hakukohdeOid = hakukohdeOid3,
          languageRequirement = "unreviewed",
          degreeRequirement = "unreviewed",
          harkinnanvaraisuus = Some("harkinnanvaraisesti_hyvaksyttavissa")
        )
      ),
      maksuvelvollisuus = Map.empty,
      keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017",
        "harkinnanvaraisuus-reason_" + hakukohdeOid2 -> "0",  // ATARU_OPPIMISVAIKEUDET
        "harkinnanvaraisuus-reason_" + hakukohdeOid3 -> "1"   // ATARU_SOSIAALISET_SYYT
      )
    )

    val hakukohde1 = KoutaHakukohde(oid = hakukohdeOid1, organisaatioOid = "1.2.3", nimi = Map.empty, voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita = Some(true))
    val hakukohde2 = KoutaHakukohde(oid = hakukohdeOid2, organisaatioOid = "1.2.3", nimi = Map.empty, voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita = Some(true))
    val hakukohde3 = KoutaHakukohde(oid = hakukohdeOid3, organisaatioOid = "1.2.3", nimi = Map.empty, voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita = Some(true))

    val ohjausparametrit = Ohjausparametrit(
      suoritustenVahvistuspaiva = Some(DateParam(1765290747152L)),
      valintalaskentapaiva = Some(DateParam(1768290647351L))
    )

    // Setup mocks
    Mockito.when(hakemuspalveluClient.getValintalaskentaHakemukset(None, true, Set(hakemusOid)))
      .thenReturn(Future.successful(Seq(testHakemus)))

    Mockito.when(tarjontaIntegration.getHakukohde(hakukohdeOid1)).thenReturn(hakukohde1)
    Mockito.when(tarjontaIntegration.getHakukohde(hakukohdeOid2)).thenReturn(hakukohde2)
    Mockito.when(tarjontaIntegration.getHakukohde(hakukohdeOid3)).thenReturn(hakukohde3)
    Mockito.when(tarjontaIntegration.getOhjausparametrit(hakuOid)).thenReturn(ohjausparametrit)
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(personOid)))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(personOid -> Set(personOid)))))

    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    val typeRef = new TypeReference[List[ValintaApiHakemuksenHarkinnanvaraisuus]] {}
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), typeRef)
    Assertions.assertEquals(1, response.size)

    val harkinnanvaraisuus = response.head
    Assertions.assertEquals(3, harkinnanvaraisuus.hakutoiveet.size())

    // Ensimmäinen hakutoive: ei yliajoa, ei harkinnanvarainen
    val hakutoive1 = harkinnanvaraisuus.hakutoiveet.asScala.find(_.hakukohdeOid == hakukohdeOid1).get
    Assertions.assertEquals(HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN.toString, hakutoive1.harkinnanvaraisuudenSyy)

    // Toinen hakutoive: yliajo SURE_EI_PAATTOTODISTUSTA (ylikirjoittaa ATARU_OPPIMISVAIKEUDET)
    val hakutoive2 = harkinnanvaraisuus.hakutoiveet.asScala.find(_.hakukohdeOid == hakukohdeOid2).get
    Assertions.assertEquals(HarkinnanvaraisuudenSyy.SURE_EI_PAATTOTODISTUSTA.toString, hakutoive2.harkinnanvaraisuudenSyy)

    // Kolmas hakutoive: ei yliajoa, käytetään laskettua
    val hakutoive3 = harkinnanvaraisuus.hakutoiveet.asScala.find(_.hakukohdeOid == hakukohdeOid3).get
    Assertions.assertEquals(HarkinnanvaraisuudenSyy.ATARU_SOSIAALISET_SYYT.toString, hakutoive3.harkinnanvaraisuudenSyy)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeHarkinnanvaraisuudetYliajoDoesNotOverrideEiHarkinnanvarainenHakukohde(): Unit = {
    val personOid = "1.2.246.562.24.21250967214"
    val hakemusOid = "1.2.246.562.11.01000000000000023254"
    val hakuOid = "1.2.246.562.29.01000000000000013278"
    val hakukohdeOid = "1.2.246.562.20.00000000000000000007"
    val virkailijaOid = "1.2.246.562.24.21250967987"

    // Yritetään yliajaa hakukohde joka ei ole harkinnanvarainen
    kantaOperaatiot.tallennaHarkinnanvaraisuusYliajot(Seq(
      fi.oph.suorituspalvelu.business.HarkinnanvaraisuusYliajo(
        hakemusOid = hakemusOid,
        hakukohdeOid = hakukohdeOid,
        harkinnanvaraisuudenSyy = Some(HarkinnanvaraisuudenSyy.ATARU_OPPIMISVAIKEUDET),
        virkailijaOid = virkailijaOid,
        selite = "Yritys yliajaa ei-harkinnanvarainen"
      )
    ))

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
          hakukohdeOid = hakukohdeOid,
          languageRequirement = "unreviewed",
          degreeRequirement = "unreviewed",
          harkinnanvaraisuus = None
        )
      ),
      maksuvelvollisuus = Map.empty,
      keyValues = Map(
        AvainArvoConstants.ataruPohjakoulutusVuosiKey -> "2017"
      )
    )

    val hakukohde = KoutaHakukohde(
      oid = hakukohdeOid,
      organisaatioOid = "1.2.3",
      nimi = Map.empty,
      voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita = Some(false)  // Ei harkinnanvarainen hakukohde
    )

    val ohjausparametrit = Ohjausparametrit(
      suoritustenVahvistuspaiva = Some(DateParam(1765290747152L)),
      valintalaskentapaiva = Some(DateParam(1768290647351L))
    )

    // Setup mocks
    Mockito.when(hakemuspalveluClient.getValintalaskentaHakemukset(None, true, Set(hakemusOid)))
      .thenReturn(Future.successful(Seq(testHakemus)))

    Mockito.when(tarjontaIntegration.getHakukohde(hakukohdeOid)).thenReturn(hakukohde)
    Mockito.when(tarjontaIntegration.getOhjausparametrit(hakuOid)).thenReturn(ohjausparametrit)
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(personOid)))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(personOid -> Set(personOid)))))

    // Execute request
    val result = mvc.perform(jsonPost(ApiConstants.VALINNAT_HARKINNANVARAISUUS_PATH, HakemustenHarkinnanvaraisuudetPayload(List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // Verify response
    val typeRef = new TypeReference[List[ValintaApiHakemuksenHarkinnanvaraisuus]] {}
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), typeRef)
    Assertions.assertEquals(1, response.size)

    val harkinnanvaraisuus = response.head
    Assertions.assertEquals(1, harkinnanvaraisuus.hakutoiveet.size())

    // Varmistetaan että yliajo EI ole voimassa - pitäisi pysyä EI_HARKINNANVARAINEN_HAKUKOHDE
    val hakutoive = harkinnanvaraisuus.hakutoiveet.asScala.head
    Assertions.assertEquals(HarkinnanvaraisuudenSyy.EI_HARKINNANVARAINEN_HAKUKOHDE.toString, hakutoive.harkinnanvaraisuudenSyy,
      "EI_HARKINNANVARAINEN_HAKUKOHDE ei saa yliajaa")
  }
}
