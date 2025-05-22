package fi.oph.suorituspalvelu

import com.nimbusds.jose.util.StandardCharset
import fi.oph.suorituspalvelu.integration.virta.VirtaClient
import fi.oph.suorituspalvelu.resource.{ApiConstants, VirtaSyncFailureResponse, VirtaSyncSuccessResponse}
import fi.oph.suorituspalvelu.security.SecurityConstants
import fi.oph.suorituspalvelu.validation.Validator
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import scala.concurrent.Future

/**
 * Virta-apin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta.
 */
class VirtaResourceIntegraatioTest extends BaseIntegraatioTesti {

  @MockBean
  var virtaClient: VirtaClient = null

  @WithAnonymousUser
  @Test def testRefreshVirtaAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.VIRTA_DATASYNC_PATH.replace(ApiConstants.VIRTA_DATASYNC_PARAM_PLACEHOLDER, "tällä ei väliä"), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshVirtaNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.VIRTA_DATASYNC_PATH.replace(ApiConstants.VIRTA_DATASYNC_PARAM_PLACEHOLDER, "tällä ei väliä"), ""))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshVirtaMalformedOid(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.VIRTA_DATASYNC_PATH.replace(ApiConstants.VIRTA_DATASYNC_PARAM_PLACEHOLDER, "tämä ei ole validi oid"), ""))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(VirtaSyncFailureResponse(List(Validator.VALIDATION_OPPIJANUMERO_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[VirtaSyncFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshVirtaAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21250967215"
    Mockito.when(virtaClient.haeKaikkiTiedot(oppijaNumero, None)).thenReturn(Future.successful(Seq("""
      |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
      |  <SOAP-ENV:Body>
      |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
      |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
      |        <virta:Opiskelija avain="C10">
      |          <virta:Opintosuoritukset>
      |          </virta:Opintosuoritukset>
      |        </virta:Opiskelija>
      |      </virta:Virta>
      |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
      |  </SOAP-ENV:Body>
      |</SOAP-ENV:Envelope>""".stripMargin)))

    val result = mvc.perform(jsonPost(ApiConstants.VIRTA_DATASYNC_PATH.replace(ApiConstants.VIRTA_DATASYNC_PARAM_PLACEHOLDER, oppijaNumero), ""))
      .andExpect(status().isOk()).andReturn()

    val virtaSyncResponse = objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[VirtaSyncSuccessResponse])

    // varmistetaan että kentät tulevat kantaan oikein
    // TODO: lisää suoritusten vertailu

}
