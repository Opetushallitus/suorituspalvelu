package fi.oph.suorituspalvelu

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.VUOSILUOKKA_9
import fi.oph.suorituspalvelu.business.SuoritusJoukko.KOSKI
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, Koodi, Lahtokoulu, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusJoukko, SuoritusTila}
import fi.oph.suorituspalvelu.integration.client.*
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiUtil}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.ApiConstants.{ESIMERKKI_AIKALEIMA, ESIMERKKI_HAKEMUS_OID, ESIMERKKI_OPPIJANUMERO, ESIMERKKI_OPPILAITOS_OID}
import fi.oph.suorituspalvelu.resource.api.{AvainarvotFailureResponse, AvainarvotSuccessResponse, LahettavatHenkilo, LahettavatHenkilotSuccessResponse, LahettavatLuokatFailureResponse, LahettavatLuokatSuccessResponse, LahtokoulutFailureResponse, LahtokoulutSuccessResponse}
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import fi.oph.suorituspalvelu.service.{AvainArvoMetadata, CombinedAvainArvoContainer, ValintaData, ValintaDataService}
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import fi.oph.suorituspalvelu.validation.Validator
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.context.bean.`override`.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.{Instant, LocalDate}
import java.util
import java.util.{Optional, UUID}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

/**
 * Lähettävät-apin integraatiotestit. Testeissä on pyritty kattamaan kaikkien endpointtien kaikki eri paluuarvoihin
 * johtavat skenaariot. Eri variaatiot näiden skenaarioiden sisällä (esim. erityyppiset validointiongelmat) testataan
 * yksikkötasolla. Onnistuneiden kutsujen osalta validoidaan että kannan tila kutsun jälkeen vastaa oletusta.
 */
class LahtokoulutIntegraatioTest extends BaseIntegraatioTesti {

  @MockitoBean
  val onrIntegration: OnrIntegration = null

  val OPPILAITOS_OID = "1.2.246.562.10.52320123196"

  /*
   * Integraatiotestit luokkien haulle
   */

  @WithAnonymousUser
  @Test def testHaeLuokatAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_LUOKAT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "2025"), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeLuokatEiOikeuksia(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_LUOKAT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "2025"), ""))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeLuokatInvalidParams(): Unit =
    // haetaan virheellisillä parametreilla
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_LUOKAT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, "Tämä ei ole validi oid")
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "Tämä ei ole validi vuosi"), ""))
      .andExpect(status().isBadRequest).andReturn()

    // virhe on kuten pitää
    Assertions.assertEquals(LahettavatLuokatFailureResponse(java.util.Set.of(
      Validator.VALIDATION_OPPILAITOSOID_EI_VALIDI + "Tämä ei ole validi oid",
      Validator.VALIDATION_VUOSI_EI_VALIDI + "Tämä ei ole validi vuosi")
    ),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LahettavatLuokatFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeLuokatAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21583363331"
    val valmistumisvuosi = 2025

    // tallennetaan valmis perusopetuksen oppimäärä ja vuosiluokka
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      OPPILAITOS_OID,
      Set(
        PerusopetuksenOppimaara(
          UUID.randomUUID(),
          None,
          fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), OPPILAITOS_OID),
          None,
          Koodi("", "", None),
          VALMIS,
          Koodi("", "", None),
          Set.empty,
          None,
          None,
          Some(LocalDate.parse(s"$valmistumisvuosi-06-01")),
          Set.empty,
          Set(Lahtokoulu(LocalDate.parse(s"${valmistumisvuosi-1}-08-01"), Some(LocalDate.parse(s"$valmistumisvuosi-06-01")), OPPILAITOS_OID, Some(valmistumisvuosi), Some("9A"), Some(VALMIS), None, VUOSILUOKKA_9)),
          false,
          false
        ),
        PerusopetuksenVuosiluokka(
          UUID.randomUUID(),
          fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), OPPILAITOS_OID),
          Kielistetty(None, None, None),
          Koodi("9", "perusopetuksenluokkaaste", None),
          None,
          Some(LocalDate.parse(s"$valmistumisvuosi-08-18")),
          false
        )
      ),
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet))

    // haetaan luokat
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_LUOKAT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, valmistumisvuosi.toString), ""))
      .andExpect(status().isOk).andReturn()

    // vastaa (toistaiseksi hardkoodattua) luokkaa
    Assertions.assertEquals(LahettavatLuokatSuccessResponse(List("9A").asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LahettavatLuokatSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeLuokatLahettava.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_NAME -> OPPILAITOS_OID,
      ApiConstants.LAHETTAVAT_VUOSI_PARAM_NAME -> valmistumisvuosi.toString
    ), auditLogEntry.target)

  /*
   * Integraatiotestit luokkien haulle
   */

  @WithAnonymousUser
  @Test def testHaeHenkilotAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_HENKILOT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "2025"), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeHenkilotEiOikeuksia(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_HENKILOT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "2025"), ""))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeHenkilotInvalidParams(): Unit =
    // haetaan virheellisillä parametreilla
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_HENKILOT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, "Tämä ei ole validi oid")
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, "Tämä ei ole validi vuosi"), ""))
      .andExpect(status().isBadRequest).andReturn()

    // virhe on kuten pitää
    Assertions.assertEquals(LahettavatLuokatFailureResponse(java.util.Set.of(
      Validator.VALIDATION_OPPILAITOSOID_EI_VALIDI + "Tämä ei ole validi oid",
      Validator.VALIDATION_VUOSI_EI_VALIDI + "Tämä ei ole validi vuosi")
    ),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LahettavatLuokatFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeHenkilotAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21583363331"
    val valmistumisVuosi = 2025

    // tallennetaan valmis perusopetuksen oppimäärä ja vuosiluokka
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      OPPILAITOS_OID,
      Set(
        PerusopetuksenOppimaara(
          UUID.randomUUID(),
          None,
          fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), OPPILAITOS_OID),
          None,
          Koodi("", "", None),
          VALMIS,
          Koodi("", "", None),
          Set.empty,
          None,
          None,
          Some(LocalDate.parse(s"$valmistumisVuosi-08-18")),
          Set.empty,
          Set(Lahtokoulu(LocalDate.parse(s"${valmistumisVuosi-1}-08-18"), Some(LocalDate.parse(s"$valmistumisVuosi-06-01")), OPPILAITOS_OID, Some(valmistumisVuosi), Some("9A"), Some(VALMIS), None, VUOSILUOKKA_9)),
          false,
          false
        ),
        PerusopetuksenVuosiluokka(
          UUID.randomUUID(),
          fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), OPPILAITOS_OID),
          Kielistetty(None, None, None),
          Koodi("9", "perusopetuksenluokkaaste", None),
          None,
          Some(LocalDate.parse(s"$valmistumisVuosi-08-18")),
          false
        )
      ),
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet))

    // haetaan luokat
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.LAHETTAVAT_HENKILOT_PATH
          .replace(ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER, OPPILAITOS_OID)
          .replace(ApiConstants.LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER, valmistumisVuosi.toString), ""))
      .andExpect(status().isOk).andReturn()

    // henkilön luokka vastaa (toistaiseksi hardkoodattua) luokkaa
    Assertions.assertEquals(LahettavatHenkilotSuccessResponse(List(LahettavatHenkilo(oppijaNumero, "9A")).asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LahettavatHenkilotSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeHenkilotLahettava.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.LAHETTAVAT_OPPILAITOSOID_PARAM_NAME -> OPPILAITOS_OID,
      ApiConstants.LAHETTAVAT_VUOSI_PARAM_NAME -> valmistumisVuosi.toString
    ), auditLogEntry.target)

  /*
   * Integraatiotestit lähtökoulujen haulle
   */

  @WithAnonymousUser
  @Test def testHaeLahtokoulutAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.OPISKELIJAT_LAHTOKOULUT_PATH
          .replace(ApiConstants.OPISKELIJAT_HENKILOOID_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_OPPIJANUMERO), ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeLahtokoulutEiOikeuksia(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.OPISKELIJAT_LAHTOKOULUT_PATH
          .replace(ApiConstants.OPISKELIJAT_HENKILOOID_PARAM_PLACEHOLDER, ApiConstants.ESIMERKKI_OPPIJANUMERO), ""))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeLahtokoulutInvalidParams(): Unit =
    // haetaan virheellisillä parametreilla
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.OPISKELIJAT_LAHTOKOULUT_PATH
          .replace(ApiConstants.OPISKELIJAT_HENKILOOID_PARAM_PLACEHOLDER, "Tämä ei ole validi oid"), ""))
      .andExpect(status().isBadRequest).andReturn()

    // virhe on kuten pitää
    Assertions.assertEquals(LahtokoulutFailureResponse(java.util.Set.of(
      Validator.VALIDATION_OPPIJANUMERO_EI_VALIDI
    )), objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LahtokoulutFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeLahtokoulutAllowed(): Unit =
    val oppijaNumero = "1.2.246.562.24.21583363331"
    val valmistumisVuosi = 2025
    val aloitusPaiva = LocalDate.parse(s"${valmistumisVuosi-1}-08-18")
    val valmistumisPaiva = LocalDate.parse(s"$valmistumisVuosi-06-01")

    // tallennetaan valmis perusopetuksen oppimäärä ja vuosiluokka
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaNumero, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      OPPILAITOS_OID,
      Set(
        PerusopetuksenOppimaara(
          UUID.randomUUID(),
          None,
          fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), OPPILAITOS_OID),
          None,
          Koodi("", "", None),
          VALMIS,
          Koodi("", "", None),
          Set.empty,
          None,
          None,
          Some(LocalDate.parse(s"$valmistumisVuosi-08-18")),
          Set.empty,
          Set(Lahtokoulu(aloitusPaiva, Some(valmistumisPaiva), OPPILAITOS_OID, Some(valmistumisVuosi), Some("9A"), Some(VALMIS), None, VUOSILUOKKA_9)),
          false,
          false
        )
      ),
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet))

    // mockataan onr-vastaus ja haetaan luokat
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero))).thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaNumero -> Set(oppijaNumero)))))
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.OPISKELIJAT_LAHTOKOULUT_PATH
          .replace(ApiConstants.OPISKELIJAT_HENKILOOID_PARAM_PLACEHOLDER, oppijaNumero), ""))
      .andExpect(status().isOk).andReturn()

    // saadaan lähtökoulua vastaava autorisointi joka päättyy seuraavan vuoden tammikuun loppuun
    Assertions.assertEquals(LahtokoulutSuccessResponse(List(fi.oph.suorituspalvelu.resource.api.LahtokouluAuthorization(OPPILAITOS_OID, aloitusPaiva, Optional.of(LocalDate.parse(s"${valmistumisPaiva.getYear+1}-02-01")), VUOSILUOKKA_9.toString)).asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[LahtokoulutSuccessResponse]))

    //Tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeLahtokoulut.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.OPISKELIJAT_HENKILOOID_PARAM_NAME -> oppijaNumero,
    ), auditLogEntry.target)
}

class AvainarvotIntegraatioTest extends BaseIntegraatioTesti {

  @MockitoBean
  val valintaDataService: ValintaDataService = null

  /*
   * Integraatiotestit lomakkeen tietojen haulle
   */

  @WithAnonymousUser
  @Test def testHaeAvainarvotAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.HAKEMUKSET_AVAINARVOT_PATH
          .replace(ApiConstants.HAKEMUKSET_PARAM_PLACEHOLDER, ESIMERKKI_HAKEMUS_OID)))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testHaeAvainarvotEiOikeuksia(): Unit =
    // käyttäjällä ei ole oikeuksia
    mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.HAKEMUKSET_AVAINARVOT_PATH
          .replace(ApiConstants.HAKEMUKSET_PARAM_PLACEHOLDER, ESIMERKKI_HAKEMUS_OID)))
      .andExpect(status().isForbidden)

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeAvainarvotInvalidParam(): Unit =
    // haetaan virheellisillä parametreillä - virheellinen henkilöOid
    val result1 = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.HAKEMUKSET_AVAINARVOT_PATH
          .replace(ApiConstants.HAKEMUKSET_PARAM_PLACEHOLDER, "Tämä ei ole validi oid")))
      .andExpect(status().isBadRequest).andReturn()

    // virhe on kuten pitää
    Assertions.assertEquals(AvainarvotFailureResponse(java.util.Set.of(Validator.VALIDATION_HAKEMUSOID_EI_VALIDI +  "Tämä ei ole validi oid")),
      objectMapper.readValue(result1.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AvainarvotFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_SISAISET_RAJAPINNAT))
  @Test def testHaeAvainarvotNotFound(): Unit =
    // mockataan virhe
    Mockito.when(valintaDataService.getValintaData(ESIMERKKI_HAKEMUS_OID)).thenReturn(Left("Ei löydy"))

    // haetaan virheellisillä parametreillä - virheellinen henkilöOid
    val result1 = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.HAKEMUKSET_AVAINARVOT_PATH
          .replace(ApiConstants.HAKEMUKSET_PARAM_PLACEHOLDER, ESIMERKKI_HAKEMUS_OID)))
      .andExpect(status().isNotFound).andReturn()

    // virhe on kuten pitää
    Assertions.assertEquals(AvainarvotFailureResponse(java.util.Set.of("Ei löydy")),
      objectMapper.readValue(result1.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AvainarvotFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testHaeAvainarvotAllowed(): Unit =
    // mockataan avainarvot
    Mockito.when(valintaDataService.getValintaData(ESIMERKKI_HAKEMUS_OID)).thenReturn(Right(ValintaData(ESIMERKKI_OPPIJANUMERO, Seq(CombinedAvainArvoContainer("avain", "arvo", AvainArvoMetadata(Seq.empty, None, None, false))), None, Seq.empty, "", "")))

    // haetaan lomakkeen tiedot
    val result = mvc.perform(MockMvcRequestBuilders
        .get(ApiConstants.HAKEMUKSET_AVAINARVOT_PATH
          .replace(ApiConstants.HAKEMUKSET_PARAM_PLACEHOLDER, ESIMERKKI_HAKEMUS_OID)))
      .andExpect(status().isOk).andReturn()

    // tarkistetaan vastaus
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[AvainarvotSuccessResponse])
    Assertions.assertEquals(AvainarvotSuccessResponse(Map("avain" -> "arvo").asJava), response)

    // tarkistetaan että auditloki täsmää
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.HaeAvainarvot.name, auditLogEntry.operation)
    Assertions.assertEquals(Map(
      ApiConstants.HAKEMUKSET_HAKEMUS_PARAM_NAME -> ESIMERKKI_HAKEMUS_OID,
    ), auditLogEntry.target)
}