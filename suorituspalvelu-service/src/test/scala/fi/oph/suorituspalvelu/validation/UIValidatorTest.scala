package fi.oph.suorituspalvelu.validation

import fi.oph.suorituspalvelu.integration.client.{Koodi, Koodisto}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.ui.SyotettyPerusopetuksenOppiaine
import org.junit.jupiter.api.*

import java.time.Instant
import java.util.{Optional, UUID}

/**
 */
class UIValidatorTest {

  // oppijanumero
  @Test def testValidateOppijanumeroRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_OPPIJANUMERO_TYHJA),
      UIValidator.validateOppijanumero(None, true)
    )
  }

  @Test def testValidateOppijanumeroInvalid(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_OPPIJANUMERO_EI_VALIDI),
      UIValidator.validateOppijanumero(Some("tämä ei ole validi oppijanumero"), true)
    )
  }

  @Test def testValidateOppijanumeroValid(): Unit = {
    Assertions.assertEquals(
      Set.empty,
      UIValidator.validateOppijanumero(Some(ApiConstants.ESIMERKKI_OPPIJANUMERO), true)
    )
  }

  // ajanhetki
  @Test def testValidateAikaleimaRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_AIKALEIMA_TYHJA), UIValidator.validateAikaleima(None, true))
  }

  @Test def testValidateAikaleimaEmptyMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_AIKALEIMA_TYHJA), UIValidator.validateAikaleima(Some(""), true))
  }

  @Test def testValidateAikaleimaNotRequiredMissing(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateAikaleima(None, false))
  }

  @Test def testValidateAikaleimaInvalid(): Unit = {
    val aikaleima = "tämä ei ole validi aikaleima"
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_AIKALEIMA_EI_VALIDI),
      UIValidator.validateAikaleima(Some(aikaleima), true)
    )
  }

  @Test def testValidateAikaleimaValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateAikaleima(Some(ApiConstants.ESIMERKKI_AIKALEIMA), true))
  }

  // oppilaitosoid
  @Test def testValidateOppilaitosOidRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_OPPILAITOSOID_TYHJA),
      UIValidator.validateOppilaitosOid(None, true)
    )
  }

  @Test def testValidateOppilaitosOidInvalid(): Unit = {
    val oppilaitosOid = "tämä ei ole validi oppilaitosOid"
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_OPPILAITOSOID_EI_VALIDI),
      UIValidator.validateOppilaitosOid(Some(oppilaitosOid), true)
    )
  }

  @Test def testValidateOppilaitosOidValid(): Unit = {
    Assertions.assertEquals(
      Set.empty,
      UIValidator.validateOppilaitosOid(Some(ApiConstants.ESIMERKKI_OPPILAITOS_OID), true)
    )
  }

  // hakuoid
  @Test def testValidateHakuOidRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_HAKUOID_TYHJA), UIValidator.validateHakuOid(None, true))
  }

  @Test def testValidateHakuOidInvalid(): Unit = {
    val oppilaitosOid = "tämä ei ole validi hakuOid"
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_HAKUOID_EI_VALIDI),
      UIValidator.validateHakuOid(Some(oppilaitosOid), true)
    )
  }

  @Test def testValidateHakuOidValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateHakuOid(Some(ApiConstants.ESIMERKKI_HAKU_OID), true))
  }

  // vuosi
  @Test def testValidateVuosiRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_VUOSI_TYHJA), UIValidator.validateVuosi(None, true))
  }

  @Test def testValidateVuosiInvalid(): Unit = {
    val vuosi = "tämä ei ole validi vuosi"
    Assertions.assertEquals(Set(UIValidator.VALIDATION_VUOSI_EI_VALIDI), UIValidator.validateVuosi(Some(vuosi), true))
  }

  @Test def testValidateVuosiValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateVuosi(Some(ApiConstants.ESIMERKKI_VUOSI), true))
  }

  // luokka
  @Test def testValidateLuokkaRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_LUOKKA_TYHJA), UIValidator.validateLuokka(None, true))
  }

  @Test def testValidateLuokkaInvalidWithDoubleQuotes(): Unit = {
    val eiValidiLuokka = "9A-\"luokka\""
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_LUOKKA_EI_VALIDI),
      UIValidator.validateLuokka(Some(eiValidiLuokka), true)
    )
  }

  @Test def testValidateLuokkaInvalidWithSingleQuotes(): Unit = {
    val eiValidiLuokka = "9A-'luokka'"
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_LUOKKA_EI_VALIDI),
      UIValidator.validateLuokka(Some(eiValidiLuokka), true)
    )
  }

  @Test def testValidateLuokkaValidStandard(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateLuokka(Some(ApiConstants.ESIMERKKI_LUOKKA), true))
  }

  @Test def testValidateLuokkaValidWithSpecialChars(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateLuokka(Some("meiän_luokka 9-A"), true))
  }

  // tila
  @Test def testValidateTilaRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_TILA_TYHJA), UIValidator.validateTila(None, true))
  }

  @Test def testValidateTilaInvalid(): Unit = {
    val tila = "tämä ei ole validi tila"
    Assertions.assertEquals(Set(UIValidator.VALIDATION_TILA_EI_VALIDI), UIValidator.validateTila(Some(tila), true))
  }

  @Test def testValidateTilaValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateTila(Some(ApiConstants.ESIMERKKI_TILA), true))
  }

  // valmistumispäivä
  @Test def testValidateValmistumispaivaRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_VALMISTUMISPAIVA_TYHJA),
      UIValidator.validateValmistumisPaiva(None)
    )
  }

  @Test def testValidateValmistumispaivaInvalid(): Unit = {
    val valmistumispaiva = "tämä ei ole validi valmistumispaiva"
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_VALMISTUMISPAIVA_EI_VALIDI),
      UIValidator.validateValmistumisPaiva(Some(valmistumispaiva))
    )
  }

  @Test def testValidateValmistumispaivaValid(): Unit = {
    Assertions.assertEquals(
      Set.empty,
      UIValidator.validateValmistumisPaiva(Some(ApiConstants.ESIMERKKI_VALMISTUMISPAIVA))
    )
  }

  // suorituskieli
  @Test def testValidateSuorituskieliRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_SUORITUSKIELI_TYHJA),
      UIValidator.validatePerusopetuksenOppimaaranSuorituskieli(None)
    )
  }

  @Test def testValidateSuorituskieliInvalid(): Unit = {
    val suorituskieli = "tämä ei ole validi suorituskieli"
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_SUORITUSKIELI_EI_VALIDI),
      UIValidator.validatePerusopetuksenOppimaaranSuorituskieli(Some(suorituskieli))
    )
  }

  @Test def testValidateSuorituskieliValid(): Unit = {
    Assertions.assertEquals(
      Set.empty,
      UIValidator.validatePerusopetuksenOppimaaranSuorituskieli(Some(ApiConstants.ESIMERKKI_SUORITUSKIELI))
    )
  }

  // yksilöllistäminen
  @Test def testValidateYksilollistaminenRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_YKSILOLLISTETTY_TYHJA),
      UIValidator.validatePerusopetuksenOppimaaranYksilollistaminen(None)
    )
  }

  @Test def testValidateYksilollistaminenInvalid(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_YKSILOLLISTETTY_EI_VALIDI),
      UIValidator.validatePerusopetuksenOppimaaranYksilollistaminen(Some(0))
    )
  }

  @Test def testValidateYksilollistaminenValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validatePerusopetuksenOppimaaranYksilollistaminen(Some(6)))
  }

  // oppiaine
  @Test def testValidateOppiaineRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_OPPIAINE_TYHJA),
      UIValidator.validatePerusopetuksenOppimaaranOppiaine(None, koodisto => Map.empty)
    )
  }

  // oppiaineet
  @Test def testValidateOppiaineetRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_OPPIAINEET_TYHJA),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineet(None)
    )
  }

  @Test def testValidateOppiaineetKoodiRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_OPPIAINEET_KOODI_TYHJA),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineet(Some(List(SyotettyPerusopetuksenOppiaine(
        koodi = Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ))))
    )
  }

  // koodi
  @Test def testValidateKoodiRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_OPPIAINE_KOODI_TYHJA),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenKoodi(None)
    )
  }

  @Test def testValidateKoodiInvalid(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_KOODI_EI_VALIDI),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenKoodi(Some("XYZ"))
    )
  }

  @Test def testValidateKoodiValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validatePerusopetuksenOppimaaranOppiaineenKoodi(Some("MA")))
  }

  // arvosana
  @Test def testValidateArvosanaRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_ARVOSANA_TYHJA),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenArvosana(None)
    )
  }

  @Test def testValidateArvosanaLiianMatala(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_ARVOSANA_EI_VALIDI),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenArvosana(Some(3))
    )
  }

  @Test def testValidateArvosanaLiianKorkea(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_ARVOSANA_EI_VALIDI),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenArvosana(Some(11))
    )
  }

  @Test def testValidateArvosanaValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validatePerusopetuksenOppimaaranOppiaineenArvosana(Some(8)))
  }

  // oppiaineen kieli
  @Test def testValidateOppiaineenKieliRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_KIELI_EI_MAARITELTY),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenKieli(
        SyotettyPerusopetuksenOppiaine(
          Optional.of("A1"),
          Optional.empty(), // kieli tyhjä vaikka pitäisi olla määritelty
          Optional.of(8),
          Optional.of(false)
        ),
        koodisto => Map.empty
      )
    )
  }

  @Test def testValidateOppiaineenKieliMaariteltyNotAllowed(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_KIELI_MAARITELTY),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenKieli(
        SyotettyPerusopetuksenOppiaine(
          Optional.of("HI"),
          Optional.of("DE"), // kieli määritelty vaikka pitäisi olla tyhjä
          Optional.of(8),
          Optional.of(false)
        ),
        koodisto => Map.empty
      )
    )
  }

  @Test def testValidateOppiaineenKieliMaariteltyInvalid(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_KIELI_EI_VALIDI),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenKieli(
        SyotettyPerusopetuksenOppiaine(
          Optional.of("A1"),
          Optional.of("tämä ei ole validi kieli"), // kieli ei löydy koodistosta
          Optional.of(8),
          Optional.of(false)
        ),
        koodisto => Map.empty
      )
    )
  }

  @Test def testValidateOppiaineenKieliMaariteltyValid(): Unit = {
    Assertions.assertEquals(
      Set(),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenKieli(
        SyotettyPerusopetuksenOppiaine(
          Optional.of("A1"),
          Optional.of("DE"), // kieli löytyy koodistosta
          Optional.of(8),
          Optional.of(false)
        ),
        koodisto => Map("DE" -> Koodi("", Koodisto(""), List()))
      )
    )
  }

  // äidinkielen oppimäärä
  @Test def testValidateOppiaineenAidinkielenOppimaaraRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_KIELI_EI_MAARITELTY),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenKieli(
        SyotettyPerusopetuksenOppiaine(
          Optional.of("AI"),
          Optional.empty(), // äidinkielen oppimäärä tyhjä vaikka pitäisi olla määritelty
          Optional.of(8),
          Optional.of(false)
        ),
        koodisto => Map.empty
      )
    )
  }

  @Test def testValidateOppiaineenAidinkielenOppimaaraMaariteltyNotAllowed(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_KIELI_MAARITELTY),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenKieli(
        SyotettyPerusopetuksenOppiaine(
          Optional.of("HI"),
          Optional.of("AI1"), // äidinkielen oppimäärä määritelty vaikka pitäisi olla tyhjä
          Optional.of(8),
          Optional.of(false)
        ),
        koodisto => Map.empty
      )
    )
  }

  @Test def testValidateOppiaineenAidinkielenOppimaaraMaariteltyInvalid(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_AI_OPPIMAARA_EI_VALIDI),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenKieli(
        SyotettyPerusopetuksenOppiaine(
          Optional.of("AI"),
          Optional.of("XYZ"),
          Optional.of(8),
          Optional.of(false)
        ),
        koodisto => Map.empty
      )
    )
  }

  @Test def testValidateOppiaineenAidinkielenOppimaaraMaariteltyValid(): Unit = {
    Assertions.assertEquals(
      Set.empty,
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenKieli(
        SyotettyPerusopetuksenOppiaine(
          Optional.of("AI"),
          Optional.of("AI1"), // Suomen kieli ja kirjallisuus -koodi
          Optional.of(8),
          Optional.of(false)
        ),
        koodisto => Map("AI1" -> Koodi("", Koodisto(""), List()))
      )
    )
  }

  // valinnaisuus
  @Test def testValidateOppiaineValinnainenRequiredMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_VALINNAINEN_EI_MAARITELTY),
      UIValidator.validatePerusopetuksenOppimaaranOppiaineenValinnainen(None)
    )
  }

  @Test def testValidateOppiaineValinnainenValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validatePerusopetuksenOppimaaranOppiaineenValinnainen(Some(false)))
  }

  // versiotunniste
  @Test def testValidateVersioTunnisteRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_VERSIOTUNNISTE_TYHJA), UIValidator.validateVersioTunniste(None))
  }

  @Test def testValidateVersioTunnisteInvalid(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_VERSIOTUNNISTE_EI_VALIDI),
      UIValidator.validateVersioTunniste(Some("tämä ei ole validi versiotunniste"))
    )
  }

  @Test def testValidateVersioTunnisteValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateVersioTunniste(Some(UUID.randomUUID().toString)))
  }

  @Test def testValidateHakemusOidValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateHakemusOid(Some("1.2.246.562.11.01000000000000023251"), true))
  }

  @Test def testValidateHakemusOidInvalid(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_HAKEMUSOID_EI_VALIDI),
      UIValidator.validateHakemusOid(Some("123451362546"), true)
    )
  }

  @Test def testValidateHakemusOidMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_HAKEMUSOID_TYHJA), UIValidator.validateHakemusOid(None, true))
  }

  @Test def testValidateHakukohdeOidValid(): Unit = {
    Assertions.assertEquals(
      Set.empty,
      UIValidator.validateHakukohdeOid(Some("1.2.246.562.20.00000000000000000003"), true)
    )
  }

  @Test def testValidateHakukohdeOidInvalid(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_HAKUKOHDEOID_EI_VALIDI),
      UIValidator.validateHakukohdeOid(Some("123451362546"), true)
    )
  }

  @Test def testValidateHakukohdeOidMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_HAKUKOHDEOID_TYHJA),
      UIValidator.validateHakukohdeOid(None, true)
    )
  }

  @Test def testValidateSeliteValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateSelite(Some("tämä on selite"), true))
  }

  @Test def testValidateSeliteMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_SELITE_TYHJA), UIValidator.validateSelite(Some(""), true))
  }

  @Test def testValidateHarkinnanvaraisuudenSyyValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateHarkinnanvaraisuudenSyy(Some("SURE_YKS_MAT_AI"), true))
  }

  @Test def testValidateHarkinnanvaraisuudenSyyInvalid(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_HARKINNANVARAISUUDEN_SYY_EI_VALIDI),
      UIValidator.validateHarkinnanvaraisuudenSyy(Some("KEKSIN_TAMAN_ITSE"), true)
    )
  }

  @Test def testValidateHarkinnanvaraisuudenSyyMissing(): Unit = {
    Assertions.assertEquals(
      Set(UIValidator.VALIDATION_HARKINNANVARAISUUDEN_SYY_TYHJA),
      UIValidator.validateHarkinnanvaraisuudenSyy(None, true)
    )
  }

}
