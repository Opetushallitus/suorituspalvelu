package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.service.{MuodostamisTulos, OvaraService}
import org.junit.jupiter.api.{Assertions, Test}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.context.bean.`override`.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.util.concurrent.{CountDownLatch, TimeUnit}

/**
 * OvaraResource-apin integraatiotestit. Testeissä on pyritty kattamaan molempien endpointtien kaikki eri
 * paluuarvoihin johtavat skenaariot.
 */
class OvaraResourceIntegraatioTest extends BaseIntegraatioTesti {

  @MockitoBean
  var ovaraService: OvaraService = null

  private val WINDOW_START = "2026-01-01T00:00:00Z"
  private val WINDOW_END   = "2026-06-01T00:00:00Z"

  private def paivittaisetPost =
    MockMvcRequestBuilders
      .post(ApiConstants.OVARA_PAIVITTAISET_PATH)
      .accept("application/json")

  private def opiskeluoikeudetPost =
    MockMvcRequestBuilders
      .post(ApiConstants.OVARA_OPISKELUOIKEUDET_PATH)
      .param("windowStart", WINDOW_START)
      .param("windowEnd", WINDOW_END)
      .accept("application/json")

  /*
   * Integraatiotestit muodostaPaivittaiset-endpointille
   */

  @WithAnonymousUser
  @Test def testMuodostaPaivittaisetAnonymousIsRedirected(): Unit =
    mvc.perform(paivittaisetPost)
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA))
  @Test def testMuodostaPaivittaisetForbidden(): Unit =
    mvc.perform(paivittaisetPost)
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testMuodostaPaivittaisetAccepted(): Unit =
    Mockito.when(ovaraService.muodostaPaivittaisetHauille(any()))
      .thenReturn(MuodostamisTulos(0, Map.empty))

    val result = mvc.perform(paivittaisetPost)
      .andExpect(status().isAccepted)
      .andReturn()

    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.MuodostaPaivittaisetOvara.name, auditLogEntry.operation)
    Assertions.assertEquals(Map("vainAktiiviset" -> "true"), auditLogEntry.target)

    val executionId = result.getResponse.getContentAsString(Charset.forName("UTF-8")).replaceAll("\"", "")
    Assertions.assertFalse(executionId.isBlank)

    Mockito.verify(ovaraService, Mockito.timeout(2000)).muodostaPaivittaisetHauille(any())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testMuodostaPaivittaisetConflictWhenAlreadyRunning(): Unit =
    val latch = new CountDownLatch(1)
    Mockito.when(ovaraService.muodostaPaivittaisetHauille(any()))
      .thenAnswer(_ => { latch.await(5, TimeUnit.SECONDS); MuodostamisTulos(0, Map.empty) })
    try
      mvc.perform(paivittaisetPost).andExpect(status().isAccepted)
      mvc.perform(paivittaisetPost).andExpect(status().isConflict)
    finally
      latch.countDown()
      Mockito.verify(ovaraService, Mockito.timeout(2000)).muodostaPaivittaisetHauille(any())

  /*
   * Integraatiotestit muodostaOpiskeluoikeussiirtotiedostot-endpointille
   */

  @WithAnonymousUser
  @Test def testMuodostaOpiskeluoikeussiirtotiedostotAnonymousIsRedirected(): Unit =
    mvc.perform(opiskeluoikeudetPost)
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA))
  @Test def testMuodostaOpiskeluoikeussiirtotiedostotForbidden(): Unit =
    mvc.perform(opiskeluoikeudetPost)
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testMuodostaOpiskeluoikeussiirtotiedostotBadRequestMissingParams(): Unit =
    mvc.perform(
      MockMvcRequestBuilders.post(ApiConstants.OVARA_OPISKELUOIKEUDET_PATH)
        .accept("application/json")
    ).andExpect(status().isBadRequest)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testMuodostaOpiskeluoikeussiirtotiedostotBadRequestInvalidWindowFormat(): Unit =
    mvc.perform(
      MockMvcRequestBuilders.post(ApiConstants.OVARA_OPISKELUOIKEUDET_PATH)
        .param("windowStart", "ei-validi-paiva")
        .param("windowEnd", WINDOW_END)
        .accept("application/json")
    ).andExpect(status().isBadRequest)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testMuodostaOpiskeluoikeussiirtotiedostotAccepted(): Unit =
    Mockito.when(ovaraService.muodostaOpiskeluoikeusSiirtotiedostot(any(), any(), any()))
      .thenReturn(0)

    val result = mvc.perform(opiskeluoikeudetPost)
      .andExpect(status().isAccepted)
      .andReturn()

    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.MuodostaOpiskeluoikeussiirtotiedostotOvara.name, auditLogEntry.operation)
    Assertions.assertEquals(Map("windowStart" -> WINDOW_START, "windowEnd" -> WINDOW_END), auditLogEntry.target)

    val executionId = result.getResponse.getContentAsString(Charset.forName("UTF-8")).replaceAll("\"", "")
    Assertions.assertFalse(executionId.isBlank)

    Mockito.verify(ovaraService, Mockito.timeout(2000)).muodostaOpiskeluoikeusSiirtotiedostot(any(), any(), any())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testMuodostaOpiskeluoikeussiirtotiedostotConflictWhenPaivittaisetRunning(): Unit =
    val latch = new CountDownLatch(1)
    Mockito.when(ovaraService.muodostaPaivittaisetHauille(any()))
      .thenAnswer(_ => { latch.await(5, TimeUnit.SECONDS); MuodostamisTulos(0, Map.empty) })
    try
      mvc.perform(paivittaisetPost).andExpect(status().isAccepted)
      mvc.perform(opiskeluoikeudetPost).andExpect(status().isConflict)
    finally
      latch.countDown()
      Mockito.verify(ovaraService, Mockito.timeout(2000)).muodostaPaivittaisetHauille(any())
}
