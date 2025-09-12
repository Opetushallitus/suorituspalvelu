package fi.oph.suorituspalvelu.validation

import fi.oph.suorituspalvelu.resource.ApiConstants
import org.junit.jupiter.api.*

import java.time.Instant

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

}
