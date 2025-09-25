package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.{Opiskeluoikeus, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{KoskiDataForOppija, KoskiIntegration}
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, AtaruHenkiloSearchParams, HakemuspalveluClientImpl, KoskiClient, KoskiMassaluovutusQueryParams, KoskiMassaluovutusQueryResponse}
import fi.oph.suorituspalvelu.resource.{ApiConstants, KoskiSyncSuccessResponse}
import fi.oph.suorituspalvelu.security.SecurityConstants
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
import scala.io.Source
import scala.concurrent.Future

@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiResourceIntegraatioTest extends BaseIntegraatioTesti {

  // -- Koski sync for oppijat --

  @MockBean
  var koskiIntegration: KoskiIntegration = null

  @MockBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

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

    Mockito.when(koskiIntegration.fetchKoskiTiedotForOppijat(Set(oppijaNumero))).thenReturn(Set(KoskiDataForOppija(oppijaNumero, KoskiIntegration.splitKoskiDataByOppija(resultData).next()._2)).iterator)
    
    val result = mvc.perform(jsonPost(ApiConstants.KOSKI_DATASYNC_HENKILOT_PATH, Set(oppijaNumero)))
      .andExpect(status().isOk).andReturn()
    val koskiSyncResponse: KoskiSyncSuccessResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncSuccessResponse])

    val haetut: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)

    //Tarkistetaan että kantaan on tallennettu kolme opiskeluoikeutta
    Assertions.assertEquals(haetut.head._2.size, 3)
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
    Mockito.when(koskiIntegration.fetchKoskiTiedotForOppijat(Set(oppijaNumero))).thenReturn(Set(KoskiDataForOppija(oppijaNumero, KoskiIntegration.splitKoskiDataByOppija(resultData).next()._2)).iterator)

    val result = mvc.perform(jsonPostString(ApiConstants.KOSKI_DATASYNC_HAKU_PATH, hakuOid))
      .andExpect(status().isOk).andReturn()
    val koskiSyncResponse: KoskiSyncSuccessResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncSuccessResponse])

    val haetut: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)

    //Tarkistetaan että kantaan on tallennettu kolme opiskeluoikeutta
    Assertions.assertEquals(haetut.head._2.size, 3)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshKoskiHakuMalformedOid(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPostString(ApiConstants.KOSKI_DATASYNC_HAKU_PATH, "1.2.246.562.28.01000000000000056245"))
      .andExpect(status().isBadRequest).andReturn()


}
