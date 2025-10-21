

package fi.oph.suorituspalvelu

import com.nimbusds.jose.util.StandardCharset
import fi.oph.suorituspalvelu.business.SuoritusJoukko
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemuksenHenkilotiedot, HakemuspalveluClientImpl, YtrClient, YtrHetuPostData, YtrMassOperation, YtrMassOperationQueryResponse}
import fi.oph.suorituspalvelu.integration.ytr.YtrDataForHenkilo
import fi.oph.suorituspalvelu.parsing.ytr.YtrParser
import fi.oph.suorituspalvelu.util.ZipUtil
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.boot.test.mock.mockito.MockBean
import org.mockito
import org.mockito.Mockito
import fi.oph.suorituspalvelu.integration.{OnrIntegration, OnrMasterHenkilo}
import fi.oph.suorituspalvelu.security.SecurityConstants
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.api.YtrSyncSuccessResponse
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import fi.oph.suorituspalvelu.parsing.ytr.Student
import fi.oph.suorituspalvelu.parsing.ytr.YtrParser.MAPPER

import java.util.UUID
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import scala.concurrent.Future
import scala.util.Try

@Test
@TestInstance(Lifecycle.PER_CLASS)
class YtrSyncIntegraatioTest extends BaseIntegraatioTesti {

  @MockBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @MockBean
  var ytrClient: YtrClient = null

  @MockBean
  var onrIntegration: OnrIntegration = null

  def toInputStreams(z: ZipInputStream): Iterator[InputStream] = {
    Iterator
      .continually(Try(z.getNextEntry).getOrElse(null))
      .takeWhile(_ != null)
      .map(_ => z)
  }

  def doZip(i: InputStream): InputStream = {
    val b = new ByteArrayOutputStream()
    val z = new ZipOutputStream(b)
    val e = new ZipEntry("s.json")
    z.putNextEntry(e)
    IOUtils.copy(i, z)
    IOUtils.closeQuietly(i)
    z.closeEntry()
    z.close()
    IOUtils.closeQuietly(z)
    new ByteArrayInputStream(b.toByteArray)
  }

  @Test
  def testMassUnzip(): Unit = {
    val sourceFile = "/ytr_mass.json"
    val zipped = doZip(this.getClass.getResourceAsStream(sourceFile))

    val hetuToPersonOid = Map(
      "150875-935M" -> "1.2.246.562.24.91423259222",
      "080578-945T" -> "1.2.246.562.24.91423259333",
      "040577-967N" -> "1.2.246.562.24.91423259444",
      "080562-9273" -> "1.2.246.562.24.91423259555",
      "060864-933X" -> "1.2.246.562.24.91423259666"
    )

    val unz = ZipUtil.unzipStreamByFile(zipped)

    unz.map((filename, data) => {
      println(s"Processing file $filename")
      val parsed: Seq[(String, String)] = YtrParser.splitAndSanitize(data).toList
      println(s"Parsed ${parsed.size} records: $parsed")

      Assertions.assertTrue(parsed.find(_._1 == "150875-935M").isDefined)
      Assertions.assertTrue(parsed.find(_._1 == "080578-945T").isDefined)
      Assertions.assertTrue(parsed.find(_._1 == "040577-967N").isDefined)
      Assertions.assertTrue(parsed.find(_._1 == "080562-9273").isDefined)
      Assertions.assertTrue(parsed.find(_._1 == "060864-933X").isDefined)
      parsed
    })
  }

  // Testataan yksittäisen oppijan tietojen päivitys YTR:stä

  @WithAnonymousUser
  @Test def testRefreshYtrOppijaAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.YTR_DATASYNC_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshYtrOppijatNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.YTR_DATASYNC_PATH, Set("1.2.3")))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshYtrOppijatMalformedOid(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPost(ApiConstants.YTR_DATASYNC_PATH, Set("1.2.246.562.25.01000000000000056245")))
      .andExpect(status().isBadRequest).andReturn()

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshYtrForOppijaAllowed(): Unit = {
    val oppijaNumero = "1.2.246.562.24.91423219111"

    val sourceFile = "/ytr_single.json"
    val personJson = new String(this.getClass.getResourceAsStream(sourceFile).readAllBytes(), "UTF-8")

    val hetuToPersonOid = Map(
      "150875-935M" -> oppijaNumero
    )

    val onrData = hetuToPersonOid.values.map(personOid => {
      personOid -> OnrMasterHenkilo(personOid, hetuToPersonOid.find(_._2 == personOid).map(_._1), None)
    }).toMap

    Mockito.when(onrIntegration.getMasterHenkilosForPersonOids(hetuToPersonOid.values.toSet))
      .thenReturn(Future.successful(onrData))
    Mockito.when(ytrClient.fetchOne(YtrHetuPostData(hetuToPersonOid.keySet.head, Some(List.empty))))
      .thenReturn(Future.successful(Some(personJson)))

    val result = mvc.perform(jsonPost(ApiConstants.YTR_DATASYNC_PATH, Set(oppijaNumero)))
      .andExpect(status().isOk).andReturn()
    val ytrSyncResponse: YtrSyncSuccessResponse = objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[YtrSyncSuccessResponse])

    val versiot = kantaOperaatiot.haeOppijanVersiot(oppijaNumero)

    //Tarkistetaan että kantaan on tallennettu oppijalle yksi versio
    Assertions.assertEquals(versiot.size, 1)
    Assertions.assertEquals(versiot.head.suoritusJoukko, SuoritusJoukko.YTR)

    val data = kantaOperaatiot.haeData(versiot.head)
    val parsed: Seq[Student] = data._2.map(data => objectMapper.readValue(data, classOf[Student]))
    parsed.foreach(p => Assertions.assertTrue(p.ssn.isEmpty))
  }


  // Testataan yksittäisen haun oppijoiden tietojen päivitys YTR:stä

  @WithAnonymousUser
  @Test def testRefreshYtrHakuAnonymous(): Unit =
    // tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(jsonPost(ApiConstants.YTR_DATASYNC_HAKU_PATH, ""))
      .andExpect(status().is3xxRedirection())

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testRefreshYtrHakuNotAllowed(): Unit =
    // tunnistettu käyttäjä jolla ei oikeuksia => 403
    mvc.perform(jsonPost(ApiConstants.YTR_DATASYNC_PATH, Set("1.2.3")))
      .andExpect(status().isForbidden())

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshYtrHakuMalformedOid(): Unit =
    // ei validi oid ei sallittu
    val result = mvc.perform(jsonPostString(ApiConstants.YTR_DATASYNC_PATH, "1.2.246.562.28.01000000000000056245"))
      .andExpect(status().isBadRequest).andReturn()

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testRefreshYtrForHaku(): Unit = {
    val hetuToPersonOid = Map(
      "150875-935M" -> "1.2.246.562.24.91423259222",
      "080578-945T" -> "1.2.246.562.24.91423259333",
      "040577-967N" -> "1.2.246.562.24.91423259444",
      "080562-9273" -> "1.2.246.562.24.91423259555",
      "060864-933X" -> "1.2.246.562.24.91423259666"
    )

    val ataruTiedot = hetuToPersonOid.values.map(personOid => AtaruHakemuksenHenkilotiedot("hakemusOid", Some(personOid), None)).toSeq

    val opUuid = UUID.randomUUID().toString
    val massOperation = YtrMassOperation(opUuid)
    val pollResponse = YtrMassOperationQueryResponse("2025-08-15T14:20:57.073511+03:00", "oph-transfer-generation",
      Some("2025-08-15T14:20:57.288401+03:00"), None, None)

    val onrData = hetuToPersonOid.values.map(personOid => {
      personOid -> OnrMasterHenkilo(personOid, hetuToPersonOid.find(_._2 == personOid).map(_._1), None)
    }).toMap

    val hakuOid = "1.2.246.562.29.01000000000000013275"

    val sourceFile = "/ytr_mass.json"
    val zippedBytes = doZip(this.getClass.getResourceAsStream(sourceFile)).readAllBytes()
    println(s"Zipped size: ${zippedBytes.length}")

    Mockito.when(hakemuspalveluClient.getHaunHakijat(hakuOid))
      .thenReturn(Future.successful(ataruTiedot))
    Mockito.when(onrIntegration.getMasterHenkilosForPersonOids(hetuToPersonOid.values.toSet))
      .thenReturn(Future.successful(onrData))
    Mockito.when(ytrClient.createYtrMassOperation(org.mockito.ArgumentMatchers.any()))
      .thenReturn(Future.successful(massOperation))
    Mockito.when(ytrClient.pollMassOperation(opUuid))
      .thenReturn(Future.successful(pollResponse))
    Mockito.when(ytrClient.fetchYtlMassResult(org.mockito.ArgumentMatchers.anyString()))
      .thenReturn(Future.successful(Some(zippedBytes)))

    val result = mvc.perform(jsonPostString(ApiConstants.YTR_DATASYNC_HAKU_PATH, hakuOid))
      .andExpect(status().isOk).andReturn()
    val ytrSyncResponse: YtrSyncSuccessResponse = objectMapper.readValue(result.getResponse.getContentAsString(StandardCharset.UTF_8), classOf[YtrSyncSuccessResponse])

    //Tarkistetaan että kaikille oppijanumeroille on muodostunut yksi versio kantaan, ja versio on parseroitu suorituksiksi
    hetuToPersonOid.values.foreach(personOid => {
      val versiot = kantaOperaatiot.haeOppijanVersiot(personOid)
      Assertions.assertTrue(versiot.size == 1)
      Assertions.assertEquals(versiot.head.suoritusJoukko, SuoritusJoukko.YTR)
      val data = kantaOperaatiot.haeData(versiot.head)
      val parsed: Seq[Student] = data._2.map(data => objectMapper.readValue(data, classOf[Student]))
      parsed.foreach(p => Assertions.assertTrue(p.ssn.isEmpty))

      val suoritukset = kantaOperaatiot.haeSuoritukset(versiot.head.oppijaNumero)
      Assertions.assertFalse(suoritukset.isEmpty)
    })
  }

}
