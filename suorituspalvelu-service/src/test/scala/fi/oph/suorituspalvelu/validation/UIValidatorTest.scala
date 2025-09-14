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
    Assertions.assertEquals(Set(UIValidator.VALIDATION_OPPIJANUMERO_TYHJA), UIValidator.validateOppijanumero(None, true))
  }

  @Test def testValidateOppijanumeroInvalid(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_OPPIJANUMERO_EI_VALIDI), UIValidator.validateOppijanumero(Some("tämä ei ole validi oppijanumero"), true))
  }

  @Test def testValidateOppijanumeroValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateOppijanumero(Some(ApiConstants.ESIMERKKI_OPPIJANUMERO), true))
  }

  // oppilaitosoid
  @Test def testValidateOppilaitosOidRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_OPPILAITOSOID_TYHJA), UIValidator.validateOppilaitosOid(None, true))
  }

  @Test def testValidateOppilaitosOidInvalid(): Unit = {
    val oppilaitosOid = "tämä ei ole validi oppilaitosOid"
    Assertions.assertEquals(Set(UIValidator.VALIDATION_OPPILAITOSOID_EI_VALIDI), UIValidator.validateOppilaitosOid(Some(oppilaitosOid), true))
  }

  @Test def testValidateOppilaitosOidValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateOppilaitosOid(Some(ApiConstants.ESIMERKKI_OPPILAITOS_OID), true))
  }

  // oppilaitosoid
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

  @Test def testValidateLuokkaInvalid(): Unit = {
    val luokka = "tämä ei ole validi luokka"
    Assertions.assertEquals(Set(UIValidator.VALIDATION_LUOKKA_EI_VALIDI), UIValidator.validateLuokka(Some(luokka), true))
  }

  @Test def testValidateLuokkaValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateLuokka(Some(ApiConstants.ESIMERKKI_LUOKKA), true))
  }

  // valmistumispäivä
  @Test def testValidateValmistumispaivaRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_VALMISTUMISPAIVA_TYHJA), UIValidator.validateValmistumisPaiva(None))
  }

  @Test def testValidateValmistumispaivaInvalid(): Unit = {
    val valmistumispaiva = "tämä ei ole validi valmistumispaiva"
    Assertions.assertEquals(Set(UIValidator.VALIDATION_VALMISTUMISPAIVA_EI_VALIDI), UIValidator.validateValmistumisPaiva(Some(valmistumispaiva)))
  }

  @Test def testValidateValmistumispaivaValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateValmistumisPaiva(Some(ApiConstants.ESIMERKKI_VALMISTUMISPAIVA)))
  }

  // suorituskieli
  @Test def testValidateSuorituskieliRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_SUORITUSKIELI_TYHJA), UIValidator.validatePeruskoulunOppimaaranSuorituskieli(None))
  }

  @Test def testValidateSuorituskieliInvalid(): Unit = {
    val suorituskieli = "tämä ei ole validi suorituskieli"
    Assertions.assertEquals(Set(UIValidator.VALIDATION_SUORITUSKIELI_EI_VALIDI), UIValidator.validatePeruskoulunOppimaaranSuorituskieli(Some(suorituskieli)))
  }

  @Test def testValidateSuorituskieliValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validatePeruskoulunOppimaaranSuorituskieli(Some(ApiConstants.ESIMERKKI_SUORITUSKIELI)))
  }

  // yksilöllistäminen
  @Test def testValidateYksilollistaminenRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_YKSILOLLISTETTY_TYHJA), UIValidator.validatePeruskoulunOppimaaranYksilollistaminen(None))
  }

  @Test def testValidateYksilollistaminenInvalid(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_YKSILOLLISTETTY_EI_VALIDI), UIValidator.validatePeruskoulunOppimaaranYksilollistaminen(Some(0)))
  }

  @Test def testValidateYksilollistaminenValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validatePeruskoulunOppimaaranYksilollistaminen(Some(6)))
  }

  // oppiaineet
  @Test def testValidateOppiaineetRequiredMissin(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_OPPIAINEET_TYHJA), UIValidator.validatePeruskoulunOppimaaranOppiaineet(None))
  }

  @Test def testValidateOppiaineetKoodiRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_OPPIAINEET_KOODI_TYHJA), UIValidator.validatePeruskoulunOppimaaranOppiaineet(Some(List(SyotettyPerusopetuksenOppiaine(
      koodi = Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()
    )))))
  }

  // koodi
  @Test def testValidateKoodiRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_KOODI_EI_VALIDI), UIValidator.validatePeruskoulunOppimaaranOppiaineenKoodi("XYZ"))
  }

  @Test def testValidateKoodiValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validatePeruskoulunOppimaaranOppiaineenKoodi("MA"))
  }

  // arvosana
  @Test def testValidateArvosanaRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_ARVOSANA_TYHJA), UIValidator.validatePeruskoulunOppimaaranOppiaineenArvosana(None))
  }

  @Test def testValidateArvosanaLiianMatala(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_ARVOSANA_EI_VALIDI), UIValidator.validatePeruskoulunOppimaaranOppiaineenArvosana(Some(3)))
  }

  @Test def testValidateArvosanaLiianKorkea(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_ARVOSANA_EI_VALIDI), UIValidator.validatePeruskoulunOppimaaranOppiaineenArvosana(Some(11)))
  }

  @Test def testValidateArvosanaValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validatePeruskoulunOppimaaranOppiaineenArvosana(Some(8)))
  }

  // oppiaineen kieli
  @Test def testValidateOppiaineenKieliRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_KIELI_EI_MAARITELTY), UIValidator.validatePeruskoulunOppimaaranOppiaineenKieli(SyotettyPerusopetuksenOppiaine(
      Optional.of("A1"),
      Optional.empty(),
      Optional.empty(), // kieli tyhjä vaikka pitäisi olla määritelty
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenKieliMaariteltyNotAllowed(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_KIELI_MAARITELTY), UIValidator.validatePeruskoulunOppimaaranOppiaineenKieli(SyotettyPerusopetuksenOppiaine(
      Optional.of("HI"),
      Optional.empty(),
      Optional.of("DE"), // kieli määritelty vaikka pitäisi olla tyhjä
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenKieliMaariteltyInvalid(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_KIELI_EI_VALIDI), UIValidator.validatePeruskoulunOppimaaranOppiaineenKieli(SyotettyPerusopetuksenOppiaine(
      Optional.of("A1"),
      Optional.empty(),
      Optional.of("tämä ei ole validi kieli"), // kieli ei löydy koodistosta
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenKieliMaariteltyValid(): Unit = {
    Assertions.assertEquals(Set(), UIValidator.validatePeruskoulunOppimaaranOppiaineenKieli(SyotettyPerusopetuksenOppiaine(
      Optional.of("A1"),
      Optional.empty(),
      Optional.of("DE"), // kieli löytyy koodistosta
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map("DE" -> Koodi("", Koodisto(""), List()))))
  }

  // valinnaisuus
  @Test def testValidateOppiaineValinnainenRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_VALINNAINEN_EI_MAARITELTY), UIValidator.validatePeruskoulunOppimaaranOppiaineenValinnainen(None))
  }

  @Test def testValidateOppiaineValinnainenValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validatePeruskoulunOppimaaranOppiaineenValinnainen(Some(false)))
  }

  // äidinkielen oppimäärä
  @Test def testValidateOppiaineenAidinkielenOppimaaraRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_AI_OPPIMAARA_EI_MAARITELTY), UIValidator.validatePeruskoulunOppimaaranOppiaineenAidinkielenOppimaara(SyotettyPerusopetuksenOppiaine(
      Optional.of("AI"),
      Optional.empty(), // äidinkielen oppimäärä tyhjä vaikka pitäisi olla määritelty
      Optional.empty(),
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenAidinkielenOppimaaraMaariteltyNotAllowed(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_AI_OPPIMAARA_MAARITELTY), UIValidator.validatePeruskoulunOppimaaranOppiaineenAidinkielenOppimaara(SyotettyPerusopetuksenOppiaine(
      Optional.of("HI"),
      Optional.of("jeejee"), // äidinkielen oppimäärä määritelty vaikka pitäisi olla tyhjä
      Optional.empty(),
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenAidinkielenOppimaaraMaariteltyInvalid(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_AI_OPPIMAARA_EI_VALIDI), UIValidator.validatePeruskoulunOppimaaranOppiaineenAidinkielenOppimaara(SyotettyPerusopetuksenOppiaine(
      Optional.of("AI"),
      Optional.of("XYZ"),
      Optional.empty(),
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenAidinkielenOppimaaraMaariteltyValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validatePeruskoulunOppimaaranOppiaineenAidinkielenOppimaara(SyotettyPerusopetuksenOppiaine(
      Optional.of("AI"),
      Optional.of("AI1"), // Suomen kieli ja kirjallisuus -koodi
      Optional.empty(),
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map("AI1" -> Koodi("", Koodisto(""), List()))))
  }

  // versiotunniste
  @Test def testValidateVersioTunnisteRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_VERSIOTUNNISTE_TYHJA), UIValidator.validateVersioTunniste(None))
  }

  @Test def testValidateVersioTunnisteInvalid(): Unit = {
    Assertions.assertEquals(Set(UIValidator.VALIDATION_VERSIOTUNNISTE_EI_VALIDI), UIValidator.validateVersioTunniste(Some("tämä ei ole validi versiotunniste")))
  }

  @Test def testValidateVersioTunnisteValid(): Unit = {
    Assertions.assertEquals(Set.empty, UIValidator.validateVersioTunniste(Some(UUID.randomUUID().toString)))
  }

}
