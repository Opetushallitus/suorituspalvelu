package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.{Opiskeluoikeus, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{KoskiDataForOppija, KoskiIntegration, SaferIterator}
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, AtaruHenkiloSearchParams, HakemuspalveluClientImpl, KoskiClient, KoskiMassaluovutusQueryParams, KoskiMassaluovutusQueryResponse}
import fi.oph.suorituspalvelu.resource.api.{KoskiHaeMuuttuneetJalkeenPayload, KoskiRetryPayload, KoskiSyncFailureResponse, KoskiSyncSuccessResponse}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.validation.Validator
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.*
import org.mockito
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.Charset
import java.time.Instant
import java.util.Optional
import scala.io.Source
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiResourceIntegraatioTest extends BaseIntegraatioTesti {

  @MockBean
  var koskiIntegration: KoskiIntegration = null

  @MockBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  // -- Koski sync for oppijat --

  @WithAnonymousUser
  @Test def testRefreshKoskiOppijaAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HENKILOT_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshKoskiOppijatNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HENKILOT_PATH, Set("1.2.3")))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiOppijatMalformedOid(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HENKILOT_PATH, Set("1.2.246.562.25.01000000000000056245")))
      .andExpect(status().isBadRequest).andReturn()

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiForOppijaAllowed(): Unit = {
    val oppijaNumero = "1.2.246.562.24.91423219238"
    val resultData: InputStream = new ByteArrayInputStream(scala.io.Source.fromResource("1_2_246_562_24_91423219238.json").mkString.getBytes())

    Mockito.when(koskiIntegration.fetchKoskiTiedotForOppijat(Set(oppijaNumero))).thenReturn(new SaferIterator(Iterator(KoskiDataForOppija(oppijaNumero, KoskiIntegration.splitKoskiDataByOppija(resultData).next()._2))))
    
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HENKILOT_PATH, Set(oppijaNumero)))
      .andExpect(status().isOk).andReturn()
    val koskiSyncResponse: KoskiSyncSuccessResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncSuccessResponse])

    val haetut: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)

    //Tarkistetaan että kantaan on tallennettu kolme opiskeluoikeutta
    Assertions.assertEquals(haetut.head._2.size, 3)

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.PaivitaKoskiTiedotHenkiloille.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "personOids" -> Seq(oppijaNumero).mkString("Array(", ", ", ")"),
    ), auditLogEntry.target)
  }

  //-- Koski sync for haku --

  @WithAnonymousUser
  @Test def testRefreshKoskiHakuAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HAKU_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshKoskiHakuNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HAKU_PATH, Set("1.2.3")))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiHakuAllowed(): Unit = {
    val hakuOid = "1.2.246.562.29.01000000000000013275"
    val oppijaNumero = "1.2.246.562.24.91423219238"

    val pollUrl = "https://mockopintopolku.fi/koski/api/massaluovutus/51fc6eee-3bc5-426d-a4d2-1d14bcb848ce)"
    val resultFileUrl = "https://mockopintopolku.fi/koski/massaluovutus/resultmock"

    val resultData: InputStream = new ByteArrayInputStream(scala.io.Source.fromResource("1_2_246_562_24_91423219238.json").mkString.getBytes())

    Mockito.when(hakemuspalveluClient.getHaunHakijat(hakuOid)).thenReturn(Future.successful(Set(AtaruHakemuksenHenkilotiedot("hakemusOid", Some(oppijaNumero), None))))
    Mockito.when(koskiIntegration.fetchKoskiTiedotForOppijat(Set(oppijaNumero))).thenReturn(new SaferIterator(Iterator(KoskiDataForOppija(oppijaNumero, KoskiIntegration.splitKoskiDataByOppija(resultData).next()._2))))

    val result = mvc.perform(jsonPostString(ApiConstants.KOSKI_DATASYNC_HAKU_PATH, hakuOid))
      .andExpect(status().isOk).andReturn()
    val koskiSyncResponse: KoskiSyncSuccessResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncSuccessResponse])

    val haetut: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)

    //Tarkistetaan että kantaan on tallennettu kolme opiskeluoikeutta
    Assertions.assertEquals(haetut.head._2.size, 3)

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.PaivitaKoskiTiedotHaunHakijoille.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "hakuOid" -> hakuOid,
    ), auditLogEntry.target)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiHakuMalformedOid(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPostString(ApiConstants.KOSKI_DATASYNC_HAKU_PATH, "1.2.246.562.28.01000000000000056245"))
      .andExpect(status().isBadRequest).andReturn()

  // -- Koski sync for muuttuneet --

  @WithAnonymousUser
  @Test def testRefreshKoskiMuuttuneetAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_MUUTTUNEET_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshKoskiMuuttuneetNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_MUUTTUNEET_PATH, KoskiHaeMuuttuneetJalkeenPayload(Optional.of("2025-09-28T10:15:30Z"))))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiMuuttuneetMalformedJson(): Unit =
    // ei validi aikaleima ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_MUUTTUNEET_PATH, "Tämä ei ole validia JSONia"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(KoskiSyncFailureResponse(java.util.List.of(ApiConstants.DATASYNC_JSON_VIRHE)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncFailureResponse]))
  
  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiMuuttuneetMalformedTimestamp(): Unit =
    // ei validi aikaleima ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_MUUTTUNEET_PATH, KoskiHaeMuuttuneetJalkeenPayload(Optional.of("tämä ei ole validi aikaleima"))))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(KoskiSyncFailureResponse(java.util.List.of(Validator.VALIDATION_MUOKATTUJALKEEN_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiMuuttuneetAllowed(): Unit = {
    val aikaleima = "2025-09-28T10:15:30Z"
    val oppijaNumero = "1_2_246_562_98_69863082363"
    val resultData: InputStream = new ByteArrayInputStream(scala.io.Source.fromResource("1_2_246_562_98_69863082363.json").mkString.getBytes())

    Mockito.when(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaNumero))).thenReturn(Future.successful(Map(oppijaNumero -> Seq.empty)))
    Mockito.when(koskiIntegration.fetchMuuttuneetKoskiTiedotSince(Instant.parse(aikaleima))).thenReturn(SaferIterator(Iterator(KoskiDataForOppija(oppijaNumero, KoskiIntegration.splitKoskiDataByOppija(resultData).next()._2))))

    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_MUUTTUNEET_PATH, KoskiHaeMuuttuneetJalkeenPayload(Optional.of(aikaleima))))
      .andExpect(status().isOk).andReturn()
    val koskiSyncResponse: KoskiSyncSuccessResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncSuccessResponse])

    val haetut: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)

    //Tarkistetaan että kantaan on tallennettu opiskeluoikeus
    Assertions.assertEquals(haetut.head._2.size, 1)

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.PaivitaMuuttuneetKoskiTiedot.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "timestamp" -> aikaleima,
    ), auditLogEntry.target)
  }

  // -- Koski sync for muuttuneet --

  @WithAnonymousUser
  @Test def testRefreshKoskiRetryAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_RETRY_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshKoskiRetryNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_RETRY_PATH, KoskiRetryPayload(Optional.of(List("https://valid.url.fi").asJava))))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiRetryMalformedJson(): Unit =
    // ei validi aikaleima ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_RETRY_PATH, "Tämä ei ole validia JSONia"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(KoskiSyncFailureResponse(java.util.List.of(ApiConstants.DATASYNC_JSON_VIRHE)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiMuuttuneetMalformedUrl(): Unit =
    val invalidUrl = "tämä ei ole validi url"

    // ei validi aikaleima ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_RETRY_PATH, KoskiRetryPayload(Optional.of(List(invalidUrl).asJava))))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(KoskiSyncFailureResponse(java.util.List.of(Validator.VALIDATION_URL_EI_VALIDI + invalidUrl)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiRetryAllowed(): Unit = {
    val fileUrl = "https://valid.url.fi"
    val oppijaNumero = "1_2_246_562_98_69863082363"
    val resultData: InputStream = new ByteArrayInputStream(scala.io.Source.fromResource("1_2_246_562_98_69863082363.json").mkString.getBytes())

    Mockito.when(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaNumero))).thenReturn(Future.successful(Map(oppijaNumero -> Seq.empty)))
    Mockito.when(koskiIntegration.retryKoskiResultFile(fileUrl)).thenReturn(SaferIterator(Iterator(KoskiDataForOppija(oppijaNumero, KoskiIntegration.splitKoskiDataByOppija(resultData).next()._2))))

    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_RETRY_PATH, KoskiRetryPayload(Optional.of(List(fileUrl).asJava))))
      .andExpect(status().isOk).andReturn()
    val koskiSyncResponse: KoskiSyncSuccessResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncSuccessResponse])

    val haetut: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)

    //Tarkistetaan että kantaan on tallennettu opiskeluoikeus
    Assertions.assertEquals(haetut.head._2.size, 1)

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.RetryKoskiTiedosto.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "tiedostot" -> fileUrl,
    ), auditLogEntry.target)

  }

}
