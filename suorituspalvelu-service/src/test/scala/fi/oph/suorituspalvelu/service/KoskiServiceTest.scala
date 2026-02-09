package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.BaseIntegraatioTesti
import fi.oph.suorituspalvelu.integration.{KoskiDataForOppija, KoskiIntegration, SaferIterator, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.HakemuspalveluClientImpl
import org.junit.jupiter.api.{Assertions, Test}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.`override`.mockito.MockitoBean

import java.io.ByteArrayInputStream
import java.time.LocalDate
import scala.concurrent.Future

/**
 * Testataan että retryKoskiResultFiles-metodi filtteröi henkilöt samalla logiikalla kuin refreshKoskiChangesSince:
 * vain aktiivisen haun omaavat tai ysiluokkalaiset/lisäpistekoulutuksessa olevat henkilöt päästetään läpi.
 */
@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiServiceTest extends BaseIntegraatioTesti {

  @MockitoBean
  var koskiIntegration: KoskiIntegration = null

  @MockitoBean
  var tarjontaIntegration: TarjontaIntegration = null

  @MockitoBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @Autowired
  var koskiService: KoskiService = null

  val FILE_URL = "https://koski-filter-test.example.fi/result"

  /**
   * a) Henkilö aktiivisella haulla pääsee filterin läpi vaikka ei ole ysiluokkalainen
   */
  @Test def testRetryPassesFilterWithAktiivinenHaku(): Unit =
    val oppijaOid = "1.2.246.562.24.00000000101"
    val hakuOid = "1.2.246.562.29.00000000101"
    val data = KoskiDataForOppija(oppijaOid, Seq(Left(new Exception("test data"))))

    Mockito.when(koskiIntegration.retryKoskiResultFile(s"$FILE_URL/a"))
      .thenReturn(SaferIterator(Iterator(data)))
    Mockito.when(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaOid)))
      .thenReturn(Future.successful(Map(oppijaOid -> Seq(hakuOid))))
    Mockito.when(tarjontaIntegration.tarkistaHaunAktiivisuus(hakuOid))
      .thenReturn(true)

    val results = koskiService.retryKoskiResultFiles(Seq(s"$FILE_URL/a"))
    Assertions.assertTrue(results.hasNext, "Henkilön aktiivisella haulla pitäisi päästä filterin läpi")

  /**
   * b) Ysiluokkalainen pääsee filterin läpi ilman aktiivista hakua
   */
  @Test def testRetryPassesFilterWithNinthGrader(): Unit =
    val oppijaOid = "1.2.246.562.24.00000000102"
    val valmistumisvuosi = LocalDate.now.getYear
    val resultData = scala.io.Source.fromResource("1_2_246_562_98_69863082363.json").mkString
      .replace("2024-08-15", s"${valmistumisvuosi-1}-08-15")
      .replace("2025-05-31", s"${valmistumisvuosi}-05-31")
    val data = KoskiDataForOppija(oppijaOid, KoskiIntegration.splitKoskiDataByHenkilo(
      new ByteArrayInputStream(resultData.getBytes())).next()._2)

    Mockito.when(koskiIntegration.retryKoskiResultFile(s"$FILE_URL/b"))
      .thenReturn(SaferIterator(Iterator(data)))
    Mockito.when(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaOid)))
      .thenReturn(Future.successful(Map(oppijaOid -> Seq.empty)))

    val results = koskiService.retryKoskiResultFiles(Seq(s"$FILE_URL/b"))
    Assertions.assertTrue(results.hasNext, "Ysiluokkalaisen pitäisi päästä filterin läpi")

  /**
   * c) Henkilö ilman aktiivista hakua tai ysiluokkaa/lisäpistekoulutusta filtteröidään pois
   */
  @Test def testRetryFilteredOutWithoutHakuOrNinthGrade(): Unit =
    val oppijaOid = "1.2.246.562.24.00000000103"
    val data = KoskiDataForOppija(oppijaOid, Seq(Left(new Exception("test data"))))

    Mockito.when(koskiIntegration.retryKoskiResultFile(s"$FILE_URL/c"))
      .thenReturn(SaferIterator(Iterator(data)))
    Mockito.when(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaOid)))
      .thenReturn(Future.successful(Map(oppijaOid -> Seq.empty)))

    val results = koskiService.retryKoskiResultFiles(Seq(s"$FILE_URL/c"))
    Assertions.assertFalse(results.hasNext, "Henkilö ilman aktiivista hakua tai ysiluokkaa pitäisi filtteröidä pois")
}
