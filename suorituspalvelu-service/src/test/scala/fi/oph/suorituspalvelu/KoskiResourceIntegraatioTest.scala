package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.{Opiskeluoikeus, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{KoskiDataForOppija, KoskiIntegration, SaferIterator, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, AtaruHenkiloSearchParams, HakemuspalveluClientImpl, KoskiClient, KoskiMassaluovutusQueryParams, KoskiMassaluovutusQueryResponse}
import fi.oph.suorituspalvelu.resource.api.{KoskiHaeMuuttuneetJalkeenPayload, KoskiPaivitaTiedotHaullePayload, KoskiPaivitaTiedotHenkiloillePayload, KoskiRetryPayload, KoskiSyncFailureResponse, KoskiSyncSuccessResponse, SyncSuccessJobResponse}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.validation.Validator
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.*
import org.mockito
import org.mockito.Mockito
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.context.bean.`override`.mockito.MockitoBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.Charset
import java.time.{Instant, LocalDate}
import java.util.Optional
import scala.io.Source
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

/**
 * Koski-apin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta ja että
 * auditlokitus toimii.
 */
@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiResourceIntegraatioTest extends BaseIntegraatioTesti {

  @MockitoBean
  var koskiIntegration: KoskiIntegration = null

  @MockitoBean
  var tarjontaIntegration: TarjontaIntegration = null

  @MockitoBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  // -- Koski sync for oppijat --

  @WithAnonymousUser
  @Test def testRefreshKoskiOppijaAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HENKILOT_PATH, "payloadilla ei väliä"))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshKoskiOppijatNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HENKILOT_PATH, "payloadilla ei väliä"))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiOppijatMalformedJson(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HENKILOT_PATH, "Tämä ei ole validia JSONia"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(KoskiSyncFailureResponse(java.util.List.of(ApiConstants.DATASYNC_JSON_VIRHE)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiOppijatMalformedOid(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HENKILOT_PATH, KoskiPaivitaTiedotHenkiloillePayload(Optional.of(List("1.2.246.562.25.01000000000000056245").asJava))))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(KoskiSyncFailureResponse(java.util.List.of(Validator.VALIDATION_HENKILOOID_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiForOppijaAllowed(): Unit = {
    val oppijaNumero = "1.2.246.562.24.91423219238"
    val resultData: InputStream = new ByteArrayInputStream(scala.io.Source.fromResource("1_2_246_562_24_91423219238.json").mkString.getBytes())

    // mockataan KOSKI-vastaus
    Mockito.when(koskiIntegration.fetchKoskiTiedotForOppijat(Set(oppijaNumero))).thenReturn(new SaferIterator(Iterator(KoskiDataForOppija(oppijaNumero, KoskiIntegration.splitKoskiDataByHenkilo(resultData).next()._2))))

    // suoritetaan kutsu ja varmistetaan että vastaus täsmää
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HENKILOT_PATH, KoskiPaivitaTiedotHenkiloillePayload(Optional.of(List(oppijaNumero).asJava))))
      .andExpect(status().isOk).andReturn()
    Assertions.assertEquals(KoskiSyncSuccessResponse(3, 0),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncSuccessResponse]))

    // tarkistetaan että kantaan on tallentunut kolme opiskeluoikeutta
    val haetut = kantaOperaatiot.haeSuoritukset(oppijaNumero).flatMap(_._2)
    Assertions.assertEquals(3, haetut.size)

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
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HAUT_PATH, "payloadilla ei väliä"))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshKoskiHakuNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HAUT_PATH, "payloadilla ei väliä"))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiHakuMalformedJson(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HAUT_PATH, "tämä ei ole validia JSONia"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(KoskiSyncFailureResponse(java.util.List.of(ApiConstants.DATASYNC_JSON_VIRHE)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiHakuMalformedOid(): Unit =
    val hakuOid = "1.2.246.562.28.01000000000000056245"

    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HAUT_PATH, KoskiPaivitaTiedotHaullePayload(Optional.of(java.util.List.of(hakuOid)))))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(KoskiSyncFailureResponse(java.util.List.of(Validator.VALIDATION_HAKUOID_EI_VALIDI + hakuOid)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiHakuAllowed(): Unit = {
    val hakuOid = "1.2.246.562.29.01000000000000013275"
    val oppijaNumero = "1.2.246.562.24.91423219238"

    // mockataan hakemuspalvelun (haun hakijoiden haku) ja Kosken vastaukset
    val resultData: InputStream = new ByteArrayInputStream(scala.io.Source.fromResource("1_2_246_562_24_91423219238.json").mkString.getBytes())
    Mockito.when(hakemuspalveluClient.getHaunHakijat(hakuOid)).thenReturn(Future.successful(Set(AtaruHakemuksenHenkilotiedot("hakemusOid", Some(oppijaNumero), None))))
    Mockito.when(koskiIntegration.fetchKoskiTiedotForOppijat(Set(oppijaNumero))).thenReturn(new SaferIterator(Iterator(KoskiDataForOppija(oppijaNumero, KoskiIntegration.splitKoskiDataByHenkilo(resultData).next()._2))))

    // suoritetaan kutsu ja varmistetaan että vastaus täsmää
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HAUT_PATH, KoskiPaivitaTiedotHaullePayload(Optional.of(java.util.List.of(hakuOid)))))
      .andExpect(status().isOk).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[SyncSuccessJobResponse])

    waitUntilReady(response.jobId)

    // tarkistetaan että kantaan on tallentunut kolme opiskeluoikeutta
    val haetut = kantaOperaatiot.haeSuoritukset(oppijaNumero).flatMap(_._2)
    Assertions.assertEquals(3, haetut.size)

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.PaivitaKoskiTiedotHaunHakijoille.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "hakuOids" -> hakuOid,
    ), auditLogEntry.target)
  }

  // -- Koski sync for muuttuneet --

  @WithAnonymousUser
  @Test def testRefreshKoskiMuuttuneetAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_MUUTTUNEET_PATH, "payloadilla ei väliä"))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshKoskiMuuttuneetNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_MUUTTUNEET_PATH, "payloadilla ei väliä"))
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
    val oppijaNumero = "1.2.246.562.98.69863082363"
    val hakuOid = "1.2.246.562.29.00000000000000044639"

    // mockataan hakemuspalvelun (haun hakijoiden haku) ja Kosken vastaukset
    val resultData: InputStream = new ByteArrayInputStream(scala.io.Source.fromResource("1_2_246_562_98_69863082363.json").mkString.getBytes())
    Mockito.when(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaNumero))).thenReturn(Future.successful(Map(oppijaNumero -> Seq(hakuOid))))
    Mockito.when(koskiIntegration.fetchMuuttuneetKoskiTiedotSince(Instant.parse(aikaleima))).thenReturn(SaferIterator(Iterator(KoskiDataForOppija(oppijaNumero, KoskiIntegration.splitKoskiDataByHenkilo(resultData).next()._2))))
    Mockito.when(tarjontaIntegration.tarkistaHaunAktiivisuus(hakuOid)).thenReturn(true)

    // suoritetaan kutsu ja varmistetaan että vastaus täsmää
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_MUUTTUNEET_PATH, KoskiHaeMuuttuneetJalkeenPayload(Optional.of(aikaleima))))
      .andExpect(status().isOk).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[SyncSuccessJobResponse])

    waitUntilReady(response.jobId)

    // tarkistetaan että kantaan on tallennettu opiskeluoikeus
    val haetut: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)

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
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_RETRY_PATH, "payloadilla ei väliä"))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshKoskiRetryNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_RETRY_PATH, "payloadilla ei väliä"))
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
    val oppijaNumero = "1.2.246.562.98.69863082363"
    val valmistumisvuosi = LocalDate.now.getYear

    // mockataan hakemuspalvelun (haun hakijoiden haku) ja Kosken vastaukset
    val resultData = scala.io.Source.fromResource("1_2_246_562_98_69863082363.json").mkString
      .replace("2024-08-15", s"${valmistumisvuosi-1}-08-15")
      .replace("2025-05-31", s"${valmistumisvuosi}-05-31")

    Mockito.when(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaNumero))).thenReturn(Future.successful(Map(oppijaNumero -> Seq.empty)))
    Mockito.when(koskiIntegration.retryKoskiResultFile(fileUrl)).thenReturn(SaferIterator(Iterator(KoskiDataForOppija(oppijaNumero, KoskiIntegration.splitKoskiDataByHenkilo(new ByteArrayInputStream(resultData.getBytes())).next()._2))))

    // suoritetaan kutsu ja varmistetaan että vastaus täsmää
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_RETRY_PATH, KoskiRetryPayload(Optional.of(List(fileUrl).asJava))))
      .andExpect(status().isOk).andReturn()
    val koskiSyncResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncSuccessResponse])

    // tarkistetaan että kantaan on tallennettu opiskeluoikeus
    val haetut: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)
    Assertions.assertEquals(haetut.head._2.size, 1)

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.RetryKoskiTiedosto.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "tiedostot" -> fileUrl,
    ), auditLogEntry.target)
  }

}
