package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, HakemuspalveluClientImpl, Hakutoive}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, PersonOidsWithAliases, TarjontaIntegration}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.api.{ValintalaskentaDataFailureResponse, ValintalaskentaDataPayload, ValintalaskentaDataSuccessResponse}
import fi.oph.suorituspalvelu.resource.ui.{OppijanValintaDataSuccessResponse, UIVirheet}
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.test.context.bean.`override`.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.util
import java.util.Optional
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

/**
 * Valintalaskenta-apin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta.
 */
class ValintalaskentaResourceIntegraatioTest extends BaseIntegraatioTesti {

  @MockitoBean
  val onrIntegration: OnrIntegration = null

  @MockitoBean
  val organisaatioProvider: OrganisaatioProvider = null

  @MockitoBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @MockitoBean
  val tarjontaIntegration: TarjontaIntegration = null

  final val ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA = SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA + "_1.2.246.562.10.52320123196"


  @WithAnonymousUser
  @Test def testHaeValintadataAnonymousIsRedirected(): Unit = {
    val hakuOid = "1.2.246.562.29.01000000000000013275"
    val hakemusOid = "1.2.246.562.11.01000000000000023251"
    val result = mvc.perform(jsonPost(ApiConstants.VALINTALASKENTA_VALINTADATA_PATH, ValintalaskentaDataPayload(Optional.of(hakuOid), Optional.empty(), List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().is3xxRedirection())
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA))
  @Test def testHaeValintadataNotAllowed(): Unit = {
    val hakuOid = "1.2.246.562.29.01000000000000013275"
    val hakemusOid = "1.2.246.562.11.01000000000000023251"
    val result = mvc.perform(jsonPost(ApiConstants.VALINTALASKENTA_VALINTADATA_PATH, ValintalaskentaDataPayload(Optional.of(hakuOid), Optional.empty(), List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isForbidden())
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeValintadataBadRequestTooManyParameters(): Unit = {
    val hakuOid = "1.2.246.562.29.01000000000000013275"
    val hakemusOid = "1.2.246.562.11.01000000000000023251"
    val hakukohdeOid = "1.2.246.562.20.00000000000000044758"
    val result = mvc.perform(jsonPost(ApiConstants.VALINTALASKENTA_VALINTADATA_PATH, ValintalaskentaDataPayload(Optional.of(hakuOid), Optional.of(hakukohdeOid), List(hakemusOid).asJava))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(ValintalaskentaDataFailureResponse(java.util.List.of(ApiConstants.VALINTALASKENTA_LIIKAA_PARAMETREJA)),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[ValintalaskentaDataFailureResponse]))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeValintaDataAllowed(): Unit = {
    val personOid = "1.2.246.562.24.21250967211"
    val hakuOid = "1.2.246.562.29.01000000000000013275"
    val hakemusOid = "1.2.246.562.11.01000000000000023251"
    val hakukohdeOid = "1.2.246.562.20.00000000000000044758"

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
          hakukohdeOid = "1.2.246.562.20.00000000000000000001",
          languageRequirement = "unreviewed",
          degreeRequirement = "unreviewed",
          harkinnanvaraisuus = None
        ),
        Hakutoive(
          processingState = "unprocessed",
          eligibilityState = "eligible",
          paymentObligation = "not-obligated",
          kkApplicationPaymentObligation = "unreviewed",
          hakukohdeOid = "1.2.246.562.20.00000000000000000002",
          languageRequirement = "unreviewed",
          degreeRequirement = "unreviewed",
          harkinnanvaraisuus = Some("harkinnanvaraisesti_hyvaksyttavissa")
        )
      ),
      maksuvelvollisuus = Map.empty,
      keyValues = Map(
        "address" -> "Testitie 71794920276",
        "30ca1709-db90-46ac-94a0-b3e446932d4c" -> "12"
      )
    )

    Mockito.when(tarjontaIntegration.getHaku(hakuOid))
      .thenReturn(None)
    Mockito.when(hakemuspalveluClient.getValintalaskentaHakemukset(Some(hakukohdeOid), false, Set.empty))
      .thenReturn(Future.successful(Seq(testHakemus)))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(personOid)))
      .thenReturn(Future.successful(PersonOidsWithAliases(Map(personOid -> Set(personOid)))))

    val result = mvc.perform(jsonPost(ApiConstants.VALINTALASKENTA_VALINTADATA_PATH, ValintalaskentaDataPayload(Optional.of(hakuOid), Optional.of(hakukohdeOid), null))
        .contentType(MediaType.APPLICATION_JSON_VALUE))
      .andExpect(status().isOk)
      .andReturn()

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeValintadata.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      "hakuOid" -> hakuOid,
      "hakukohdeOid" -> hakukohdeOid,
      "hakemusOids" -> "Array()",
    ), auditLogEntry.target)

    println(s"result.getResponse.getContentAsString(Charset.forName(\"UTF-8\")): ${result.getResponse.getContentAsString(Charset.forName("UTF-8"))}")
    val parsedResult = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[ValintalaskentaDataSuccessResponse])

    val hakemus = parsedResult.valintaHakemukset.asScala.head

    //Tarkistetaan hakemukselta löytyvät oidit oidit, ja että suoraan hakemukselta välittyvät avain-arvot ovat mukana
    Assertions.assertEquals(hakemus.hakemusoid, hakemusOid)
    Assertions.assertEquals(hakemus.hakuoid, hakuOid)
    Assertions.assertEquals(hakemus.hakijaOid, personOid)
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "address" && aa.arvo == "Testitie 71794920276"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "30ca1709-db90-46ac-94a0-b3e446932d4c" && aa.arvo == "12"))

    //Tarkistetaan hakutoiveisiin liittyvät arvot
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference1-Koulutus-id" && aa.arvo == "1.2.246.562.20.00000000000000000001"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference1-Koulutus-id-processingState" && aa.arvo == "UNPROCESSED"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference1-Koulutus-id-paymentObligation" && aa.arvo == "NOT-OBLIGATED"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference1-Koulutus-id-degreeRequirement" && aa.arvo == "UNREVIEWED"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference1-Koulutus-id-eligibility" && aa.arvo == "ELIGIBLE"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference1-Koulutus-id-languageRequirement" && aa.arvo == "UNREVIEWED"))

    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference2-Koulutus-id" && aa.arvo == "1.2.246.562.20.00000000000000000002"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference2-Koulutus-id-processingState" && aa.arvo == "UNPROCESSED"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference2-Koulutus-id-paymentObligation" && aa.arvo == "NOT-OBLIGATED"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference2-Koulutus-id-degreeRequirement" && aa.arvo == "UNREVIEWED"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference2-Koulutus-id-eligibility" && aa.arvo == "ELIGIBLE"))
    Assertions.assertTrue(hakemus.avaimet.asScala.exists(aa => aa.avain == "preference2-Koulutus-id-languageRequirement" && aa.arvo == "UNREVIEWED"))
  }
}
