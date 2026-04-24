package fi.oph.suorituspalvelu.business.yos

import fi.oph.suorituspalvelu.integration.TarjontaIntegration
import fi.oph.suorituspalvelu.integration.client.{KoutaHaku, KoutaHakukohde}
import fi.oph.suorituspalvelu.yos.YosService
import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}
import org.mockito.Mockito

@Test
@TestInstance(Lifecycle.PER_CLASS)
class YosServiceTest {

  private val tarjontaMock: TarjontaIntegration = Mockito.mock(classOf[TarjontaIntegration])

  private val service = YosService(tarjontaMock)

  private val HAKU_OID = "1.2.246.562.29.00000000000000074021"
  private val HAKUKOHDE_OID = "1.2.246.562.20.00000000000000078520"

  private val HAKU_JOKA_KUULUU_YOS_PIIRIIN = KoutaHaku(HAKU_OID, "julkaistu",
    Map.empty, "", Some("haunkohdejoukko_12"), List.empty, None, Some(2026))

  private val HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN = KoutaHakukohde(
    HAKUKOHDE_OID, "organisaatio_oid", Map.empty, None, Some(true)
  )

  @Test
  def throwsExceptionWhenHakuIsNotFound(): Unit = {
    Assertions.assertThrows(classOf[RuntimeException], () => {
      Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(None)
      Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
      service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID)
    })
  }

  @Test
  def throwsExceptionWhenHakukohdeIsNotFound(): Unit = {
    Assertions.assertThrows(classOf[RuntimeException], () => {
      Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN))
      Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(null)
      service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID)
    })
  }

  @Test
  def hakutoiveKuuluuYOSPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertTrue(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID))
  }

  @Test
  def hakuJokaEiOleKorkeakouluHakuEiKuuluYosPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(
      Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN.copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_11"))))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertFalse(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID))
  }

  @Test
  def erasmusHakuEiKuuluYosPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(
      Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_010"))))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertFalse(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID))
  }

  @Test
  def jatkotutkintoEiKuulyYosPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(
      Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_3"))))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertFalse(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID))
  }
}
