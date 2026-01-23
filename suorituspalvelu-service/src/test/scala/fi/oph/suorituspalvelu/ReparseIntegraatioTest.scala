package fi.oph.suorituspalvelu

import com.fasterxml.jackson.core.`type`.TypeReference
import fi.oph.suorituspalvelu.business.KKOpiskeluoikeusTila.VOIMASSA
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{Koodi, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, SuoritusJoukko, VirtaOpiskeluoikeus, YOOpiskeluoikeus, YOTutkinto}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiUtil}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.api.*
import fi.oph.suorituspalvelu.security.{AuditOperation, SecurityConstants}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.jdk.CollectionConverters.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class ReparseIntegraatioTest extends BaseIntegraatioTesti {

  // -- Uudelleenparserointi endpoint tests --

  @WithAnonymousUser
  @Test def testUudelleenParseroiAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.DATASYNC_UUDELLEENPARSEROI_PATH, "payloadilla ei ole väliä"))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testUudelleenParseroiNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.DATASYNC_UUDELLEENPARSEROI_PATH, "payloadilla ei ole väliä"))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testUudelleenParseroiMalformedJson(): Unit =
    // ei validi JSON ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.DATASYNC_UUDELLEENPARSEROI_PATH, "tämä ei ole validia jsonia"))
      .andExpect(status().isBadRequest).andReturn()

    Assertions.assertEquals(KoskiSyncFailureResponse(List(ApiConstants.DATASYNC_JSON_VIRHE).asJava),
      objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[KoskiSyncFailureResponse]))

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testUudelleenParseroiKoski(): Unit = {
    // tallennetaan versio ja parseroitua dataa
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(ApiConstants.ESIMERKKI_OPPIJANUMERO, SuoritusJoukko.KOSKI, Seq.empty, Instant.now()).get
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      ApiConstants.ESIMERKKI_OPPILAITOS_OID,
      Set.empty,
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, opiskeluoikeudet, Seq.empty)
    
    // suoritetaan kutsu ja parseroidaan vastaus
    val payload = new ReparsePayload().copy(koski = true)
    val result = mvc.perform(jsonPost(ApiConstants.DATASYNC_UUDELLEENPARSEROI_PATH, payload))
      .andExpect(status().isOk).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[ReparseSuccessResponse])

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.Uudelleenparseroi.name, auditLogEntry.operation)
    val typeRef = new TypeReference[java.util.Map[String, String]] {}
    Assertions.assertEquals(objectMapper.readValue(objectMapper.writeValueAsString(payload), typeRef).asScala.toMap, auditLogEntry.target)

    // odotetaan että parserointi valmis
    Assertions.assertEquals(1, response.jobids.size())
    waitUntilReady(response.jobids.asScala.head)
    
    // uudelleenparserointi poistanut opiskeluoikeuden
    Assertions.assertEquals(Map(versio -> Set.empty), kantaOperaatiot.haeSuoritukset(ApiConstants.ESIMERKKI_OPPIJANUMERO))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testUudelleenParseroiVirta(): Unit = {
    // tallennetaan versio ja parseroitua dataa
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(ApiConstants.ESIMERKKI_OPPIJANUMERO, SuoritusJoukko.VIRTA, Seq.empty, Instant.now()).get
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(VirtaOpiskeluoikeus(
      UUID.randomUUID(),
      "",
      "",
      LocalDate.now(),
      LocalDate.now(),
      Koodi("arvo", "koodisto", None),
      VOIMASSA,
      "",
      Set.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, opiskeluoikeudet, Seq.empty)

    // suoritetaan kutsu ja parseroidaan vastaus
    val payload = new ReparsePayload().copy(virta = true)
    val result = mvc.perform(jsonPost(ApiConstants.DATASYNC_UUDELLEENPARSEROI_PATH, payload))
      .andExpect(status().isOk).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[ReparseSuccessResponse])

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.Uudelleenparseroi.name, auditLogEntry.operation)
    val typeRef = new TypeReference[java.util.Map[String, String]] {}
    Assertions.assertEquals(objectMapper.readValue(objectMapper.writeValueAsString(payload), typeRef).asScala.toMap, auditLogEntry.target)

    // odotetaan että parserointi valmis
    Assertions.assertEquals(1, response.jobids.size())
    waitUntilReady(response.jobids.asScala.head)

    // uudelleenparserointi poistanut opiskeluoikeuden
    Assertions.assertEquals(Map(versio -> Set.empty), kantaOperaatiot.haeSuoritukset(ApiConstants.ESIMERKKI_OPPIJANUMERO))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testUudelleenParseroiYTR(): Unit = {
    // tallennetaan versio ja parseroitua dataa
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(ApiConstants.ESIMERKKI_OPPIJANUMERO, SuoritusJoukko.YTR, Seq.empty, Instant.now()).get
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(YOOpiskeluoikeus(UUID.randomUUID(), YOTutkinto(UUID.randomUUID(), Koodi("arvo", "koodisto", None), VALMIS, None, Set.empty)))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, opiskeluoikeudet, Seq.empty)

    // suoritetaan kutsu ja parseroidaan vastaus
    val payload = new ReparsePayload().copy(ytr = true)
    val result = mvc.perform(jsonPost(ApiConstants.DATASYNC_UUDELLEENPARSEROI_PATH, payload))
      .andExpect(status().isOk).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[ReparseSuccessResponse])

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.Uudelleenparseroi.name, auditLogEntry.operation)
    val typeRef = new TypeReference[java.util.Map[String, String]] {}
    Assertions.assertEquals(objectMapper.readValue(objectMapper.writeValueAsString(payload), typeRef).asScala.toMap, auditLogEntry.target)

    // odotetaan että parserointi valmis
    Assertions.assertEquals(1, response.jobids.size())
    waitUntilReady(response.jobids.asScala.head)

    // uudelleenparserointi poistanut opiskeluoikeuden
    Assertions.assertEquals(Map(versio -> Set.empty), kantaOperaatiot.haeSuoritukset(ApiConstants.ESIMERKKI_OPPIJANUMERO))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testUudelleenParseroiPerusopetuksenOppimaarat(): Unit = {
    // tallennetaan versio ja parseroitua dataa
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(ApiConstants.ESIMERKKI_OPPIJANUMERO, SuoritusJoukko.SYOTETTY_PERUSOPETUS, Seq.empty, Instant.now()).get
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      ApiConstants.ESIMERKKI_OPPILAITOS_OID,
      Set.empty,
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, opiskeluoikeudet, Seq.empty)

    // suoritetaan kutsu ja parseroidaan vastaus
    val payload = new ReparsePayload().copy(perusopetuksenOppimaarat = true)
    val result = mvc.perform(jsonPost(ApiConstants.DATASYNC_UUDELLEENPARSEROI_PATH, payload))
      .andExpect(status().isOk).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[ReparseSuccessResponse])

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.Uudelleenparseroi.name, auditLogEntry.operation)
    val typeRef = new TypeReference[java.util.Map[String, String]] {}
    Assertions.assertEquals(objectMapper.readValue(objectMapper.writeValueAsString(payload), typeRef).asScala.toMap, auditLogEntry.target)

    // odotetaan että parserointi valmis
    Assertions.assertEquals(1, response.jobids.size())
    waitUntilReady(response.jobids.asScala.head)

    // uudelleenparserointi poistanut opiskeluoikeuden
    Assertions.assertEquals(Map(versio -> Set.empty), kantaOperaatiot.haeSuoritukset(ApiConstants.ESIMERKKI_OPPIJANUMERO))
  }

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testUudelleenParseroiPerusopetuksenOppiaineenOppimaarat(): Unit = {
    // tallennetaan versio ja parseroitua dataa
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(ApiConstants.ESIMERKKI_OPPIJANUMERO, SuoritusJoukko.SYOTETYT_OPPIAINEET, Seq.empty, Instant.now()).get
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      ApiConstants.ESIMERKKI_OPPILAITOS_OID,
      Set.empty,
      None,
      VALMIS,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, opiskeluoikeudet, Seq.empty)

    // suoritetaan kutsu ja parseroidaan vastaus
    val payload = new ReparsePayload().copy(perusopetuksenOppiaineet = true)
    val result = mvc.perform(jsonPost(ApiConstants.DATASYNC_UUDELLEENPARSEROI_PATH, payload))
      .andExpect(status().isOk).andReturn()
    val response = objectMapper.readValue(result.getResponse.getContentAsString(Charset.forName("UTF-8")), classOf[ReparseSuccessResponse])

    // katsotaan että kutsun tiedot tallentuvat auditlokiin
    val auditLogEntry = getLatestAuditLogEntry()
    Assertions.assertEquals(AuditOperation.Uudelleenparseroi.name, auditLogEntry.operation)
    val typeRef = new TypeReference[java.util.Map[String, String]] {}
    Assertions.assertEquals(objectMapper.readValue(objectMapper.writeValueAsString(payload), typeRef).asScala.toMap, auditLogEntry.target)

    // odotetaan että parserointi valmis
    Assertions.assertEquals(1, response.jobids.size())
    waitUntilReady(response.jobids.asScala.head)

    // uudelleenparserointi poistanut opiskeluoikeuden
    Assertions.assertEquals(Map(versio -> Set.empty), kantaOperaatiot.haeSuoritukset(ApiConstants.ESIMERKKI_OPPIJANUMERO))
  }
}