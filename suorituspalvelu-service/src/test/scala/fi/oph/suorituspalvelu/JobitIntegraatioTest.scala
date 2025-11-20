

package fi.oph.suorituspalvelu

import com.nimbusds.jose.util.StandardCharset
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.api.{SyncJob, SyncJobFailureResponse, SyncJobStatusResponse}
import fi.oph.suorituspalvelu.security.SecurityConstants
import fi.oph.suorituspalvelu.validation.Validator
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.context.bean.`override`.mockito.MockitoBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.Instant
import java.util.UUID
import scala.jdk.CollectionConverters.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class JobitIntegraatioTest extends BaseIntegraatioTesti {

  @WithAnonymousUser
  @Test def testGetJobStatusesAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonGet(ApiConstants.DATASYNC_JOBIT_PATH))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testGetJobStatusesNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonGet(ApiConstants.DATASYNC_JOBIT_PATH))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testGetJobStatusesInvalidNimi(): Unit = {
    val nimi = "$$$ei_validi_nimi$$$"

    val result = mvc.perform(jsonGet(ApiConstants.DATASYNC_JOBIT_PATH + "?" + ApiConstants.DATASYNC_JOBIT_NIMI_PARAM_NAME + "=" + nimi))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(SyncJobFailureResponse(java.util.List.of(Validator.VALIDATION_JOBIN_NIMI_EI_VALIDI + nimi)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[SyncJobFailureResponse]))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testGetJobStatusesAllowedNimi(): Unit = {
    // mockataan kantaan jobeja
    val haettuJobId = UUID.randomUUID()
    val muuJobId = UUID.randomUUID()
    val haettuJobName = "test-job"
    val muuJobName = "muu-job"
    val lastUpdated = Instant.ofEpochMilli((Instant.now.toEpochMilli/1000)*1000)
    kantaOperaatiot.updateJobStatus(haettuJobId, haettuJobName, 0.5, lastUpdated)
    kantaOperaatiot.updateJobStatus(muuJobId, muuJobName, 0.5, lastUpdated)

    // haetaan jobin status tunnisteella
    val result = mvc.perform(jsonGet(ApiConstants.DATASYNC_JOBIT_PATH + "?" + ApiConstants.DATASYNC_JOBIT_NIMI_PARAM_NAME + "=" + haettuJobName))
      .andExpect(status().isOk).andReturn()
    val jobResponse = objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[SyncJobStatusResponse])

    // palautuu vain haetun jobin status
    Assertions.assertEquals(SyncJobStatusResponse(List(SyncJob(haettuJobId, haettuJobName, 50, lastUpdated)).asJava), jobResponse)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testGetJobStatusesInvalidTunniste(): Unit = {
    val tunniste = "ei_validi_tunniste"

    val result = mvc.perform(jsonGet(ApiConstants.DATASYNC_JOBIT_PATH + "?" + ApiConstants.DATASYNC_JOBIT_TUNNISTE_PARAM_NAME + "=" + tunniste))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(SyncJobFailureResponse(java.util.List.of(Validator.VALIDATION_TUNNISTE_EI_VALIDI + tunniste)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[SyncJobFailureResponse]))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testGetJobStatusesAllowedTunniste(): Unit = {
    // mockataan kantaan jobeja
    val haettuJobId = UUID.randomUUID()
    val muuJobId = UUID.randomUUID()
    val lastUpdated = Instant.ofEpochMilli((Instant.now.toEpochMilli/1000)*1000)
    kantaOperaatiot.updateJobStatus(haettuJobId, "test-job", 0.5, lastUpdated)
    kantaOperaatiot.updateJobStatus(muuJobId, "test-job", 0.5, lastUpdated)

    // haetaan jobin status tunnisteella
    val result = mvc.perform(jsonGet(ApiConstants.DATASYNC_JOBIT_PATH + "?" + ApiConstants.DATASYNC_JOBIT_TUNNISTE_PARAM_NAME + "=" + haettuJobId.toString))
      .andExpect(status().isOk).andReturn()
    val jobResponse = objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[SyncJobStatusResponse])

    // palautuu vain haetun jobin status
    Assertions.assertEquals(SyncJobStatusResponse(List(SyncJob(haettuJobId, "test-job", 50, lastUpdated)).asJava), jobResponse)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testGetJobStatusesAllowedNoParameters(): Unit = {
    // mockataan kantaan jobi
    val jobId = UUID.randomUUID()
    val lastUpdated = Instant.ofEpochMilli((Instant.now.toEpochMilli/1000)*1000)
    kantaOperaatiot.updateJobStatus(jobId, "test-job", 0.5, lastUpdated)

    // haetaan kaikkien jobien status
    val result = mvc.perform(jsonGet(ApiConstants.DATASYNC_JOBIT_PATH))
      .andExpect(status().isOk).andReturn()
    val jobResponse = objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[SyncJobStatusResponse])

    // palautuu kaikkien jobien status
    Assertions.assertEquals(SyncJobStatusResponse(List(SyncJob(jobId, "test-job", 50, lastUpdated)).asJava), jobResponse)    
  }
}
