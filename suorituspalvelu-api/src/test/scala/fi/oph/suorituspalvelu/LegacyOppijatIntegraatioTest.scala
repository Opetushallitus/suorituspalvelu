package fi.oph.suorituspalvelu

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.util.StandardCharset
import fi.oph.suorituspalvelu.business.Tietolahde.{KOSKI, YTR}
import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, AtaruHenkiloSearchParams, HakemuspalveluClientImpl}
import fi.oph.suorituspalvelu.integration.virta.VirtaClient
import fi.oph.suorituspalvelu.parsing.koski.KoskiToSuoritusConverter.SUORITYSTYYPPI_AMMATILLINENTUTKINTO
import fi.oph.suorituspalvelu.resource.ApiConstants.{EXAMPLE_HAKU_OID, LEGACY_OPPIJAT_HAKU_PARAM_NAME, LEGACY_SUORITUKSET_HAKU_EPAONNISTUI, LEGACY_SUORITUKSET_HENKILO_PARAM_NAME, LEGACY_SUORITUKSET_JOKO_OID_TAI_PVM_PAKOLLINEN, LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME}
import fi.oph.suorituspalvelu.resource.*
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.service.Komot
import fi.oph.suorituspalvelu.validation.Validator
import fi.oph.suorituspalvelu.validation.Validator.{VALIDATION_HAKUKOHDEOID_EI_VALIDI, VALIDATION_HAKUOID_EI_VALIDI, VALIDATION_HAKUOID_TYHJA, VALIDATION_MUOKATTUJALKEEN_EI_VALIDI, VALIDATION_OPPIJANUMERO_EI_VALIDI}
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.system.{CapturedOutput, OutputCaptureRule}
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.time.LocalDate
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

/**
 * 
 */
class LegacyOppijatIntegraatioTest extends BaseIntegraatioTesti {

  val OPPIJA_OID = "1.2.246.562.24.40483869857"

  def getOppijatPath(oid: String = OPPIJA_OID): String =
    ApiConstants.LEGACY_OPPIJAT_PATH

  @MockBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @WithAnonymousUser
  @Test def testLegacySuorituksetAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders.get(getOppijatPath()))
      .andExpect(status().is3xxRedirection)

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testLegacySuorituksetNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(MockMvcRequestBuilders.get(getOppijatPath()))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacyHakuEiMaaritelty(): Unit =
    // haku pitää olla määritelty
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_OPPIJAT_PATH))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(VALIDATION_HAKUOID_TYHJA)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[LegacySuorituksetFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacyHakuEiValidi(): Unit =
    // hakuOidin pitää olla validi hakuOid
    val invalidHakuOid = "1.2.3.4"
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_OPPIJAT_PATH + "?" +
        ApiConstants.LEGACY_OPPIJAT_HAKU_PARAM_NAME + "=" + invalidHakuOid))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(VALIDATION_HAKUOID_EI_VALIDI + invalidHakuOid)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[LegacySuorituksetFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacyHakukohdeEiValidi(): Unit =
    // hakukohdeOidin pitää olla validi hakuOid
    val invalidHakukohdeOid = "1.2.3.4"
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_OPPIJAT_PATH + "?" +
        ApiConstants.LEGACY_OPPIJAT_HAKU_PARAM_NAME + "=" + EXAMPLE_HAKU_OID + "&" + ApiConstants.LEGACY_OPPIJAT_HAKUKOHDE_PARAM_NAME + "=" + invalidHakukohdeOid))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(LegacySuorituksetFailureResponse(java.util.Set.of(VALIDATION_HAKUKOHDEOID_EI_VALIDI + invalidHakukohdeOid)),
      objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[LegacySuorituksetFailureResponse]))
  
  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testLegacySuorituksetHenkilolleAllowed(): Unit =
    val hakuOid = ApiConstants.EXAMPLE_HAKU_OID
    val tutkintoKoodi = "123456"
    val suoritusKieli = Koodi("fi", "kieli", Some(1))

    // tallennetaan tutkinnot
    val koskiVersio = kantaOperaatiot.tallennaJarjestelmaVersio(OPPIJA_OID, KOSKI, "{\"testi\": \"suorituksetHenkilölle\"}")
    val ammatillinenTutkinto = AmmatillinenTutkinto("diplomi", Koodi(tutkintoKoodi, "koulutus", Some(1)), Koodi("valmistunut", "jokutila", Some(1)), Some(LocalDate.now()), None, Koodi("tapa", "suoritustapa", Some(1)), suoritusKieli, Set.empty)
    val telma = Telma(Koodi("arvo", "koodisto", None), suoritusKieli)
    val perusopetuksenOppimaara = PerusopetuksenOppimaara("oid", Koodi("arvo", "koodisto", None), suoritusKieli, Set.empty, None, Set.empty)
    val perusopetuksenOppiaineenOppimaara = NuortenPerusopetuksenOppiaineenOppimaara("nimi", Koodi("arvo", "koodisto", None), "", suoritusKieli, None)
    val yoTutkinto = YOTutkinto(suoritusKieli)
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(koskiVersio.get, Set(
      AmmatillinenOpiskeluoikeus("1.2.3", "2.3.4", Set(ammatillinenTutkinto), None),
      AmmatillinenOpiskeluoikeus("1.2.3", "2.3.4", Set(telma), None),
      PerusopetuksenOpiskeluoikeus("1.2.3", "2.3.4", Set(perusopetuksenOppimaara), None, None),
      PerusopetuksenOpiskeluoikeus("1.2.3", "2.3.4", Set(perusopetuksenOppiaineenOppimaara), None, None),
      YOOpiskeluoikeus(yoTutkinto)
    ), Set.empty)

    // määritellään että hakemuspalvelun mukaan haun ainoa hakija
    Mockito.when(hakemuspalveluClient.getHaunHakijat(AtaruHenkiloSearchParams(None, Some(hakuOid), None))).thenReturn(Future.successful(Seq(AtaruHakemuksenHenkilotiedot("", Some(OPPIJA_OID), None))))

    // haetaan muuttuneet legacy-rajapinnasta
    val result = mvc.perform(MockMvcRequestBuilders.get(ApiConstants.LEGACY_OPPIJAT_PATH + "?" + LEGACY_OPPIJAT_HAKU_PARAM_NAME + "=" + hakuOid))
      .andExpect(status().isOk).andReturn()
    val legacyOppijatResponse = objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), new TypeReference[Seq[LegacyOppija]] {})

    // varmistetaan että tulokset täsmäävät
    Assertions.assertEquals(List(LegacyOppija(OPPIJA_OID, Set(
      LegacySuoritusJaArvosanat(LegacySuoritus(suoritusKieli.arvo, Komot.ammatillinen)),
      LegacySuoritusJaArvosanat(LegacySuoritus(suoritusKieli.arvo, Komot.telma)),
      LegacySuoritusJaArvosanat(LegacySuoritus(suoritusKieli.arvo, Komot.perusopetus)),
      LegacySuoritusJaArvosanat(LegacySuoritus(suoritusKieli.arvo, Komot.perusopetuksenOppiaineenOppimaara)),
      LegacySuoritusJaArvosanat(LegacySuoritus(suoritusKieli.arvo, Komot.yoTutkinto))
    ).asJava)),
    legacyOppijatResponse)

    // ja että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeOppijaTiedot.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(ApiConstants.LEGACY_OPPIJAT_HAKU_PARAM_NAME -> hakuOid), auditLogEntry.target)

}
