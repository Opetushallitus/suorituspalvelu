package fi.oph.suorituspalvelu.business.yos

import fi.oph.suorituspalvelu.yos.YosKoulutusAsteLuokka.{ALEMMAT_ASTEET, EI_YOS_KOULUTUSASTETTA, YLEMMAT_ASTEET, YLEMMAT_JA_ALEMMAT_ASTEET}
import fi.oph.suorituspalvelu.yos.{YosHakutoive, YosPredicate}
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
    assertTrue(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, "", ALEMMAT_ASTEET)))
    assertTrue(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, "", YLEMMAT_JA_ALEMMAT_ASTEET)))
  }

  @Test
  def hakutoiveEiKuuluYosinPiiriin(): Unit = {
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(false, true, false, false, "", ALEMMAT_ASTEET)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, false, false, false, "", ALEMMAT_ASTEET)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, true, false, "", ALEMMAT_ASTEET)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, true, "", ALEMMAT_ASTEET)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, "", EI_YOS_KOULUTUSASTETTA)))
  }

}
