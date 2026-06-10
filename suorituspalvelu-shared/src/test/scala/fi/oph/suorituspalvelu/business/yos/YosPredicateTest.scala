package fi.oph.suorituspalvelu.business.yos

import fi.oph.suorituspalvelu.yos.YosKoulutusAsteLuokka.{ALEMMAT_ASTEET, EI_YOS_KOULUTUSASTETTA, YLEMMAT_ASTEET, YLEMMAT_JA_ALEMMAT_ASTEET}
import fi.oph.suorituspalvelu.yos.{YosConstants, YosHakutoive, YosPredicate}
import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Test, TestInstance}

@Test
@TestInstance(Lifecycle.PER_CLASS)
class YosPredicateTest {

  @Test
  def opiskeluOikeusKuuluuYosinPiiriinKoulutusAsteenMukaan(): Unit = {
    assertTrue(YosPredicate.kuuluukoOpiskeluOikeusYosinPiiriinKoulutusAsteenMukaan(ALEMMAT_ASTEET, ALEMMAT_ASTEET))
    assertTrue(YosPredicate.kuuluukoOpiskeluOikeusYosinPiiriinKoulutusAsteenMukaan(YLEMMAT_JA_ALEMMAT_ASTEET, ALEMMAT_ASTEET))
    assertTrue(YosPredicate.kuuluukoOpiskeluOikeusYosinPiiriinKoulutusAsteenMukaan(YLEMMAT_JA_ALEMMAT_ASTEET, YLEMMAT_ASTEET))
  }

  @Test
  def opiskeluOikeusEiKuuluYosinPiiriinKoulutusAsteenMukaan(): Unit = {
    assertFalse(YosPredicate.kuuluukoOpiskeluOikeusYosinPiiriinKoulutusAsteenMukaan(ALEMMAT_ASTEET, YLEMMAT_ASTEET))
    assertFalse(YosPredicate.kuuluukoOpiskeluOikeusYosinPiiriinKoulutusAsteenMukaan(ALEMMAT_ASTEET, EI_YOS_KOULUTUSASTETTA))
    assertFalse(YosPredicate.kuuluukoOpiskeluOikeusYosinPiiriinKoulutusAsteenMukaan(YLEMMAT_JA_ALEMMAT_ASTEET, EI_YOS_KOULUTUSASTETTA))
    assertFalse(YosPredicate.kuuluukoOpiskeluOikeusYosinPiiriinKoulutusAsteenMukaan(EI_YOS_KOULUTUSASTETTA, YLEMMAT_ASTEET))
    assertFalse(YosPredicate.kuuluukoOpiskeluOikeusYosinPiiriinKoulutusAsteenMukaan(EI_YOS_KOULUTUSASTETTA, ALEMMAT_ASTEET))
  }

  @Test
  def hakutoiveKuuluuYosinPiiriin(): Unit = {
    assertTrue(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), ALEMMAT_ASTEET)))
    assertTrue(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), YLEMMAT_JA_ALEMMAT_ASTEET)))
  }

  @Test
  def hakutoiveEiKuuluYosinPiiriin(): Unit = {
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(false, true, false, false, List("123.23.123"), ALEMMAT_ASTEET)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, false, false, false, List("123.23.123"), ALEMMAT_ASTEET)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, true, false, List("123.23.123"), ALEMMAT_ASTEET)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, true, List("123.23.123"), ALEMMAT_ASTEET)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), EI_YOS_KOULUTUSASTETTA)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List(YosConstants.POLIISI_AMK_OID), ALEMMAT_ASTEET)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123", YosConstants.MAANPUOLUSTUS_KK_OID), ALEMMAT_ASTEET)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List(YosConstants.AHVENANMAAN_KK_OID, "123.23.123"), ALEMMAT_ASTEET)))
  }

}
