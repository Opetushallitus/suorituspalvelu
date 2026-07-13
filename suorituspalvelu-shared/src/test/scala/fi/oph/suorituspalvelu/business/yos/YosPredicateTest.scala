package fi.oph.suorituspalvelu.business.yos

import fi.oph.suorituspalvelu.yos.YosKoulutusAsteLuokka.{ALEMMAT_ASTEET, EI_YOS_KOULUTUSASTETTA, YLEMMAT_ASTEET, YLEMMAT_JA_ALEMMAT_ASTEET}
import fi.oph.suorituspalvelu.yos.{YosConstants, YosHakutoive, YosPredicate}
import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Test, TestInstance}

@Test
@TestInstance(Lifecycle.PER_CLASS)
class YosPredicateTest {

  // arvot, joilla YOS on voimassa sekä hakuajan että koulutuksen alkamisvuoden perusteella
  private val VOIMASSA_HAUN_ALKAMISAIKA = Some("2026-08-01T00:00:00")
  private val VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI = Some("2027")

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
    assertTrue(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
    assertTrue(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), YLEMMAT_JA_ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
  }

  @Test
  def hakutoiveEiKuuluYosinPiiriin(): Unit = {
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(false, true, false, false, List("123.23.123"), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, false, false, false, List("123.23.123"), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, true, false, List("123.23.123"), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, true, List("123.23.123"), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), EI_YOS_KOULUTUSASTETTA, VOIMASSA_HAUN_ALKAMISAIKA, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List(YosConstants.POLIISI_AMK_OID), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123", YosConstants.MAANPUOLUSTUS_KK_OID), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List(YosConstants.AHVENANMAAN_KK_OID, "123.23.123"), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
  }

  @Test
  def hakutoiveEiKuuluYosinPiiriinKunHaunAlkamisaikaOnEnnenLeikkuripaivaa(): Unit = {
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), ALEMMAT_ASTEET, Some("2026-07-31T23:59:59"), VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
  }

  @Test
  def hakutoiveEiKuuluYosinPiiriinKunHaunAlkamisaikaaEiOleTiedossa(): Unit = {
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), ALEMMAT_ASTEET, None, VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
  }

  @Test
  def hakutoiveKuuluuYosinPiiriinKunHaunAlkamisaikaOnTasmalleenLeikkurihetkella(): Unit = {
    assertTrue(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), ALEMMAT_ASTEET, Some("2026-08-01T00:00:00"), VOIMASSA_KOULUTUKSEN_ALKAMISVUOSI)))
  }

  @Test
  def hakutoiveEiKuuluYosinPiiriinKunKoulutuksenAlkamisvuosiOnEnnenLeikkurivuotta(): Unit = {
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, Some("2026"))))
  }

  @Test
  def hakutoiveEiKuuluYosinPiiriinKunKoulutuksenAlkamisvuottaEiOleTiedossa(): Unit = {
    assertFalse(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, None)))
  }

  @Test
  def hakutoiveKuuluuYosinPiiriinKunKoulutuksenAlkamisvuosiOnTasmalleenLeikkurivuosi(): Unit = {
    assertTrue(YosPredicate.kuuluukoHakutoiveYosinPiiriin(YosHakutoive(true, true, false, false, List("123.23.123"), ALEMMAT_ASTEET, VOIMASSA_HAUN_ALKAMISAIKA, Some("2027"))))
  }

}
