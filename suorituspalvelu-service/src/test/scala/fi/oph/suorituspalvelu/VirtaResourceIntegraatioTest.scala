package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.{Opiskeluoikeus, VersioEntiteetti, VirtaOpiskeluoikeus}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, HakemuspalveluClientImpl}
import fi.oph.suorituspalvelu.integration.virta.{VirtaClient, VirtaResultForHenkilo}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.api.{VirtaSyncFailureResponse, VirtaSyncSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.service.VirtaUtil
import fi.oph.suorituspalvelu.validation.Validator
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import scala.concurrent.Future

/**
 * Virta-apin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta.
 */
class VirtaResourceIntegraatioTest extends BaseIntegraatioTesti {

  @MockBean
  var virtaClient: VirtaClient = null

  @MockBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @MockBean
  var onrIntegration: OnrIntegration = null

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

    Assertions.assertEquals(VirtaSyncFailureResponse(java.util.List.of(Validator.VALIDATION_OPPIJANUMERO_EI_VALIDI)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[VirtaSyncFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshVirtaAllowedActuallySaveSuoritukset(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967215"

    val virtaXml: String = scala.io.Source.fromResource("1_2_246_562_24_21250967215.xml").mkString

    Mockito.when(virtaClient.haeKaikkiTiedot(oppijaNumero, None)).thenReturn(Future.successful(Seq(VirtaResultForHenkilo(oppijaNumero, virtaXml))))

    val result = mvc.perform(jsonPost(ApiConstants.VIRTA_DATASYNC_PATH.replace(ApiConstants.VIRTA_DATASYNC_PARAM_PLACEHOLDER, oppijaNumero), ""))
      .andExpect(status().isOk()).andReturn()

    val virtaSyncResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[VirtaSyncSuccessResponse])

    //Odotellaan että tiedot asynkronisesti synkkaava VIRTA_REFRESH_TASK ehtii pyörähtää
    Thread.sleep(2000)

    //Pitäisi syntyä kaksi opiskeluoikeutta, joista toisella 0 ja toisella 50 alisuoritusta.
    val suorituksetKannasta: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)
    Assertions.assertEquals(2, suorituksetKannasta.head._2.size)
    Assertions.assertTrue(suorituksetKannasta.head._2.exists(oo => oo.asInstanceOf[VirtaOpiskeluoikeus].suoritukset.isEmpty))
    Assertions.assertTrue(suorituksetKannasta.head._2.exists(oo => oo.asInstanceOf[VirtaOpiskeluoikeus].suoritukset.size == 50))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.PaivitaVirtaTiedot.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.VIRTA_DATASYNC_PARAM_NAME -> oppijaNumero), auditLogEntry.target)
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshVirtaReplacesHetuWithMockHetu(): Unit = {
    val oppijaNumero = "1.2.246.562.24.21250967215"

    val virtaXml: String = scala.io.Source.fromResource("1_2_246_562_24_21250967215.xml").mkString

    Mockito.when(virtaClient.haeKaikkiTiedot(oppijaNumero, None)).thenReturn(Future.successful(Seq(VirtaResultForHenkilo(oppijaNumero, virtaXml))))

    val result = mvc.perform(jsonPost(ApiConstants.VIRTA_DATASYNC_PATH.replace(ApiConstants.VIRTA_DATASYNC_PARAM_PLACEHOLDER, oppijaNumero), ""))
      .andExpect(status().isOk()).andReturn()

    val virtaSyncResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[VirtaSyncSuccessResponse])

    //Odotellaan että tiedot asynkronisesti synkkaava VIRTA_REFRESH_TASK ehtii pyörähtää
    Thread.sleep(2000)

    //Tarkistetaan että version yhteyteen tallennetusta lähdedatasta ei löydy alkuperäistä hetua mutta korvaava hetu löytyy
    val suorituksetKannasta: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)
    val versionData = kantaOperaatiot.haeData(suorituksetKannasta.head._1)
    Assertions.assertTrue(versionData._2.contains(VirtaUtil.replacementHetu))
    Assertions.assertFalse(versionData._2.contains("010296-1230"))
  }

  @WithAnonymousUser
  @Test def testRefreshVirtaForHakuAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.VIRTA_DATASYNC_HAKU_PATH, "1.2.246.562.29.01000000000000013275"))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshVirtaForHakuNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.VIRTA_DATASYNC_HAKU_PATH, "1.2.246.562.29.01000000000000013275"))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshVirtaForHakuMalformedOid(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.VIRTA_DATASYNC_HAKU_PATH, "1.2.246.562.23.01000000000000013275"))
      .andExpect(status().isBadRequest).andReturn()


  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshVirtaForHakuAllowedActuallySaveSuoritukset(): Unit = {
    val hakijaOid1 = "1.2.246.562.24.00000000111"
    val hakijaOid2 = "1.2.246.562.24.00000000222"
    val failingHakijaOid = "1.2.246.562.24.00000000987"
    val hakijaOid3 = "1.2.246.562.24.00000000333"
    val aliasForHakijaOid2 = "1.2.246.562.24.00000999222"

    val haunHakijatOids: Seq[String] = Seq(hakijaOid1, hakijaOid2, failingHakijaOid, hakijaOid3)

    val henkiloTiedot: Seq[AtaruHakemuksenHenkilotiedot] = haunHakijatOids.map(oppijaNumero => {
      AtaruHakemuksenHenkilotiedot("hakemusOid", Some(oppijaNumero), None)
    })

    //Yhdellä oppijoista on alias
    val aliasMap = haunHakijatOids.map(oppijaNumero => {
      val aliasSet = if (oppijaNumero == hakijaOid2) Set(oppijaNumero, aliasForHakijaOid2) else Set(oppijaNumero)
      (oppijaNumero, aliasSet)
    }).toMap

    val hakuOid = "1.2.246.562.29.01000000000000013275"

    val virtaXml: String = scala.io.Source.fromResource("1_2_246_562_24_21250967215.xml").mkString

    Mockito.when(onrIntegration.getAliasesForPersonOids(haunHakijatOids.toSet))
      .thenReturn(Future.successful(PersonOidsWithAliases(aliasMap)))

    Mockito.when(hakemuspalveluClient.getHaunHakijat(hakuOid))
      .thenReturn(Future.successful(henkiloTiedot))

    Mockito.when(virtaClient.haeTiedotOppijanumerolle(hakijaOid1))
      .thenReturn(Future.successful(VirtaResultForHenkilo(hakijaOid1, virtaXml)))
    Mockito.when(virtaClient.haeTiedotOppijanumerolle(hakijaOid2))
      .thenReturn(Future.successful(VirtaResultForHenkilo(hakijaOid2, virtaXml)))
    Mockito.when(virtaClient.haeTiedotOppijanumerolle(failingHakijaOid))
      .thenThrow(new RuntimeException("Yllättävä virhe haettaessa tietoja Virrasta"))
    Mockito.when(virtaClient.haeTiedotOppijanumerolle(hakijaOid3))
      .thenReturn(Future.successful(VirtaResultForHenkilo(hakijaOid3, virtaXml)))
    //Oppijan "1.2.246.562.24.00000000222" alias
    Mockito.when(virtaClient.haeTiedotOppijanumerolle(aliasForHakijaOid2))
      .thenReturn(Future.successful(VirtaResultForHenkilo(aliasForHakijaOid2, virtaXml)))


    val result = mvc.perform(jsonPostString(ApiConstants.VIRTA_DATASYNC_HAKU_PATH, hakuOid))
      .andExpect(status().isOk()).andReturn()

    val virtaSyncResponse = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[VirtaSyncSuccessResponse])

    //Odotellaan että tiedot asynkronisesti synkkaava VIRTA_REFRESH_TASK_FOR_HAKU ehtii pyörähtää
    Thread.sleep(2000)

    //Jokaiselle oppijaNumerolle pitäisi syntyä kaksi opiskeluoikeutta, joista toisella 0 ja toisella 50 alisuoritusta.
    haunHakijatOids.foreach(oppijaNumero => {
      //Virheeseen päätyneen hakijan tietoja ei löydy kannasta.
      if (oppijaNumero != failingHakijaOid) {
         val suorituksetKannasta: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(oppijaNumero)
         Assertions.assertEquals(2, suorituksetKannasta.head._2.size)
         Assertions.assertTrue(suorituksetKannasta.head._2.exists(oo => oo.asInstanceOf[VirtaOpiskeluoikeus].suoritukset.isEmpty))
         Assertions.assertTrue(suorituksetKannasta.head._2.exists(oo => oo.asInstanceOf[VirtaOpiskeluoikeus].suoritukset.size == 50))
      }
    })

    //Tarkistetaan myös aliaksen suoritukset
    val suorituksetKannasta: Map[VersioEntiteetti, Set[Opiskeluoikeus]] = kantaOperaatiot.haeSuoritukset(aliasForHakijaOid2)
    Assertions.assertEquals(2, suorituksetKannasta.head._2.size)
    Assertions.assertTrue(suorituksetKannasta.head._2.exists(oo => oo.asInstanceOf[VirtaOpiskeluoikeus].suoritukset.isEmpty))
    Assertions.assertTrue(suorituksetKannasta.head._2.exists(oo => oo.asInstanceOf[VirtaOpiskeluoikeus].suoritukset.size == 50))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.PaivitaVirtaTiedotHaunHakijoille.name, auditLogEntry.operation)
    Assertions.assertEquals(Map("hakuOid" -> hakuOid), auditLogEntry.target)
  }
}
