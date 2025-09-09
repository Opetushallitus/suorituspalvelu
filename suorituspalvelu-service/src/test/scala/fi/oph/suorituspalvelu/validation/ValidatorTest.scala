package fi.oph.suorituspalvelu.validation

import fi.oph.suorituspalvelu.integration.client.{Koodi, Koodisto}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.ui.SyotettyPeruskoulunOppiaine
import org.junit.jupiter.api.*

import java.time.Instant
import java.util.Optional

/**
 */
class ValidatorTest {

  // oppijanumero
  @Test def testValidateOppijanumeroRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_OPPIJANUMERO_TYHJA), Validator.validateOppijanumero(None, true))
  }

  @Test def testValidateOppijanumeroInvalid(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_OPPIJANUMERO_EI_VALIDI), Validator.validateOppijanumero(Some("tämä ei ole validi oppijanumero"), true))
  }

  @Test def testValidateOppijanumeroValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateOppijanumero(Some(ApiConstants.EXAMPLE_OPPIJANUMERO), true))
  }

  // muokattuJalkeen
  @Test def testValidateMuokattuJalkeenRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_MUOKATTUJALKEEN_TYHJA), Validator.validateMuokattujalkeen(None, true))
  }

  @Test def testValidateMuokattuJalkeenInvalid(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_MUOKATTUJALKEEN_EI_VALIDI), Validator.validateMuokattujalkeen(Some("tämä ei ole validi aikaleima"), true))
  }

  @Test def testValidateMuokattuJalkeenValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateMuokattujalkeen(Some(Instant.now.toString), true))
  }

  // hakuoid
  @Test def testValidateHakuOidRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_HAKUOID_TYHJA), Validator.validateHakuOid(None, true))
  }

  @Test def testValidateHakuOidInvalid(): Unit = {
    val hakuOid = "tämä ei ole validi hakuOid"
    Assertions.assertEquals(Set(Validator.VALIDATION_HAKUOID_EI_VALIDI + hakuOid), Validator.validateHakuOid(Some(hakuOid), true))
  }

  @Test def testValidateHakuOidValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateHakuOid(Some(ApiConstants.EXAMPLE_HAKU_OID), true))
  }

  // hakukohdeoid
  @Test def testValidateHakukohdeOidRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_HAKUKOHDEOID_TYHJA), Validator.validateHakukohdeOid(None, true))
  }

  @Test def testValidateHakukohdeOidInvalid(): Unit = {
    val hakukohdeOid = "tämä ei ole validi hakukohdeOid"
    Assertions.assertEquals(Set(Validator.VALIDATION_HAKUKOHDEOID_EI_VALIDI + hakukohdeOid), Validator.validateHakukohdeOid(Some(hakukohdeOid), true))
  }

  @Test def testValidateHakukohdeOidValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateHakukohdeOid(Some(ApiConstants.EXAMPLE_HAKUKOHDE_OID), true))
  }

  // oppilaitosoid
  @Test def testValidateOppilaitosOidRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_OPPILAITOSOID_TYHJA), Validator.validateOppilaitosOid(None, true))
  }

  @Test def testValidateOppilaitosOidInvalid(): Unit = {
    val oppilaitosOid = "tämä ei ole validi oppilaitosOid"
    Assertions.assertEquals(Set(Validator.VALIDATION_OPPILAITOSOID_EI_VALIDI + oppilaitosOid), Validator.validateOppilaitosOid(Some(oppilaitosOid), true))
  }

  @Test def testValidateOppilaitosOidValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateOppilaitosOid(Some(ApiConstants.UI_HAKU_ESIMERKKI_OPPILAITOS_OID), true))
  }

  // oppilaitosoid
  @Test def testValidateVuosiRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_VUOSI_TYHJA), Validator.validateVuosi(None, true))
  }

  @Test def testValidateVuosiInvalid(): Unit = {
    val vuosi = "tämä ei ole validi vuosi"
    Assertions.assertEquals(Set(Validator.VALIDATION_VUOSI_EI_VALIDI + vuosi), Validator.validateVuosi(Some(vuosi), true))
  }

  @Test def testValidateVuosiValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateVuosi(Some(ApiConstants.UI_HAKU_ESIMERKKI_VUOSI), true))
  }

  // luokka
  @Test def testValidateLuokkaRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_LUOKKA_TYHJA), Validator.validateLuokka(None, true))
  }

  @Test def testValidateLuokkaInvalid(): Unit = {
    val luokka = "tämä ei ole validi luokka"
    Assertions.assertEquals(Set(Validator.VALIDATION_LUOKKA_EI_VALIDI + luokka), Validator.validateLuokka(Some(luokka), true))
  }

  @Test def testValidateLuokkaValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateLuokka(Some(ApiConstants.UI_HAKU_ESIMERKKI_LUOKKA), true))
  }

  // valmistumispäivä
  @Test def testValidateValmistumispaivaRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_VALMISTUMISPAIVA_TYHJA), Validator.validateValmistumisPaiva(None))
  }

  @Test def testValidateValmistumispaivaInvalid(): Unit = {
    val valmistumispaiva = "tämä ei ole validi valmistumispaiva"
    Assertions.assertEquals(Set(Validator.VALIDATION_VALMISTUMISPAIVA_EI_VALIDI + valmistumispaiva), Validator.validateValmistumisPaiva(Some(valmistumispaiva)))
  }

  @Test def testValidateValmistumispaivaValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateValmistumisPaiva(Some(ApiConstants.UI_LUO_PERUSKOULUN_OPPIMAARA_ESIMERKKI_VALMISTUMISPAIVA)))
  }

  // suorituskieli
  @Test def testValidateSuorituskieliRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_SUORITUSKIELI_TYHJA), Validator.validatePeruskoulunOppimaaranSuorituskieli(None))
  }

  @Test def testValidateSuorituskieliInvalid(): Unit = {
    val suorituskieli = "tämä ei ole validi suorituskieli"
    Assertions.assertEquals(Set(Validator.VALIDATION_SUORITUSKIELI_EI_VALIDI + suorituskieli), Validator.validatePeruskoulunOppimaaranSuorituskieli(Some(suorituskieli)))
  }

  @Test def testValidateSuorituskieliValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validatePeruskoulunOppimaaranSuorituskieli(Some(ApiConstants.UI_LUO_PERUSKOULUN_OPPIMAARA_ESIMERKKI_SUORITUSKIELI)))
  }

  // yksilöllistäminen
  @Test def testValidateYksilollistaminenRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_YKSILOLLISTETTY_TYHJA), Validator.validatePeruskoulunOppimaaranYksilollistaminen(None))
  }

  @Test def testValidateYksilollistaminenValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validatePeruskoulunOppimaaranYksilollistaminen(Some(true)))
  }

  // koodi
  @Test def testValidateOppiaineKoodiRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_KOODI_TYHJA), Validator.validatePeruskoulunOppimaaranOppiaineenKoodi(None))
  }

  @Test def testValidateOppiaineKoodiInvalid(): Unit = {
    val koodi = "tämä ei ole validi oppiainekoodi"
    Assertions.assertEquals(Set(Validator.VALIDATION_KOODI_EI_VALIDI + koodi), Validator.validatePeruskoulunOppimaaranOppiaineenKoodi(Some(koodi)))
  }

  @Test def testValidateOppiaineKoodiValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validatePeruskoulunOppimaaranOppiaineenKoodi(Some(ApiConstants.UI_LUO_PERUSKOULUN_OPPIMAARA_ESIMERKKI_OPPIAINEKOODI)))
  }

  // arvosana
  @Test def testValidateArvosanaRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_ARVOSANA_TYHJA), Validator.validatePeruskoulunOppimaaranOppiaineenArvosana(None))
  }

  @Test def testValidateArvosanaLiianMatala(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_ARVOSANA_EI_VALIDI), Validator.validatePeruskoulunOppimaaranOppiaineenArvosana(Some(3)))
  }

  @Test def testValidateArvosanaLiianKorkea(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_ARVOSANA_EI_VALIDI), Validator.validatePeruskoulunOppimaaranOppiaineenArvosana(Some(11)))
  }

  @Test def testValidateArvosanaValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validatePeruskoulunOppimaaranOppiaineenArvosana(Some(8)))
  }

  // oppiaineen kieli
  @Test def testValidateOppiaineenKieliRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_KIELI_EI_MAARITELTY), Validator.validatePeruskoulunOppimaaranOppiaineenKieli(SyotettyPeruskoulunOppiaine(
      Optional.of("A1"),
      Optional.empty(),
      Optional.empty(), // kieli tyhjä vaikka pitäisi olla määritelty
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenKieliMaariteltyNotAllowed(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_KIELI_MAARITELTY), Validator.validatePeruskoulunOppimaaranOppiaineenKieli(SyotettyPeruskoulunOppiaine(
      Optional.of("HI"),
      Optional.empty(),
      Optional.of("DE"), // kieli määritelty vaikka pitäisi olla tyhjä
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenKieliMaariteltyInvalid(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_KIELI_INVALID), Validator.validatePeruskoulunOppimaaranOppiaineenKieli(SyotettyPeruskoulunOppiaine(
      Optional.of("A1"),
      Optional.empty(),
      Optional.of("tämä ei ole validi kieli"), // kieli ei löydy koodistosta
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenKieliMaariteltyValid(): Unit = {
    Assertions.assertEquals(Set(), Validator.validatePeruskoulunOppimaaranOppiaineenKieli(SyotettyPeruskoulunOppiaine(
      Optional.of("A1"),
      Optional.empty(),
      Optional.of("DE"), // kieli löytyy koodistosta
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map("DE" -> Koodi("", Koodisto(""), List()))))
  }

  // valinnaisuus
  @Test def testValidateOppiaineValinnainenRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_VALINNAINEN_EI_MAARITELTY), Validator.validatePeruskoulunOppimaaranOppiaineenValinnainen(None))
  }

  @Test def testValidateOppiaineValinnainenValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validatePeruskoulunOppimaaranOppiaineenValinnainen(Some(false)))
  }

  // äidinkielen oppimäärä
  @Test def testValidateOppiaineenAidinkielenOppimaaraRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_AI_OPPIMAARA_EI_MAARITELTY), Validator.validatePeruskoulunOppimaaranOppiaineenAidinkielenOppimaara(SyotettyPeruskoulunOppiaine(
      Optional.of("AI"),
      Optional.empty(), // äidinkielen oppimäärä tyhjä vaikka pitäisi olla määritelty
      Optional.empty(),
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenAidinkielenOppimaaraMaariteltyNotAllowed(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_AI_OPPIMAARA_MAARITELTY), Validator.validatePeruskoulunOppimaaranOppiaineenAidinkielenOppimaara(SyotettyPeruskoulunOppiaine(
      Optional.of("HI"),
      Optional.of("jeejee"), // äidinkielen oppimäärä määritelty vaikka pitäisi olla tyhjä
      Optional.empty(),
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenAidinkielenOppimaaraMaariteltyInvalid(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_AI_OPPIMAARA_EI_VALIDI), Validator.validatePeruskoulunOppimaaranOppiaineenAidinkielenOppimaara(SyotettyPeruskoulunOppiaine(
      Optional.of("AI"),
      Optional.of("XYZ"),
      Optional.empty(),
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map.empty))
  }

  @Test def testValidateOppiaineenAidinkielenOppimaaraMaariteltyValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validatePeruskoulunOppimaaranOppiaineenAidinkielenOppimaara(SyotettyPeruskoulunOppiaine(
      Optional.of("AI"),
      Optional.of("AI1"), // Suomen kieli ja kirjallisuus -koodi
      Optional.empty(),
      Optional.of(8),
      Optional.of(false)
    ), koodisto => Map("AI1" -> Koodi("", Koodisto(""), List()))))
  }




}
