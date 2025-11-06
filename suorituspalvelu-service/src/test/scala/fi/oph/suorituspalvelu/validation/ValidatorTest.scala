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
    Assertions.assertEquals(Set.empty, Validator.validateOppijanumero(Some(ApiConstants.ESIMERKKI_OPPIJANUMERO), true))
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
    Assertions.assertEquals(Set.empty, Validator.validateHakuOid(Some(ApiConstants.ESIMERKKI_HAKU_OID), true))
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
    Assertions.assertEquals(Set.empty, Validator.validateHakukohdeOid(Some(ApiConstants.ESIMERKKI_HAKUKOHDE_OID), true))
  }

  // oppilaitosoid
  @Test def testValidateOppilaitosOidRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_OPPILAITOSOID_TYHJA), Validator.validateOppilaitosOid(None, true))
  }

  @Test def testValidateOppilaitosOidInvalid(): Unit = {
    val hakukohdeOid = "tämä ei ole validi hakukohdeOid"
    Assertions.assertEquals(Set(Validator.VALIDATION_OPPILAITOSOID_EI_VALIDI + hakukohdeOid), Validator.validateOppilaitosOid(Some(hakukohdeOid), true))
  }

  @Test def testValidateOppilaitosOidValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateOppilaitosOid(Some(ApiConstants.ESIMERKKI_OPPILAITOS_OID), true))
  }

  // personOid
  @Test def testValidatePersonOidsEmpty(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_OPPIJANUMERO_TYHJA), Validator.validatePersonOids(Set.empty))
  }

  @Test def testValidatePersonOidsInvalid(): Unit = {
    val invalidOids = Set("invalid-oid-1", "invalid-oid-2")
    Assertions.assertEquals(Set(Validator.VALIDATION_EI_VALIDIT_OIDIT + invalidOids), Validator.validatePersonOids(invalidOids))
  }

  @Test def testValidatePersonOidsValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validatePersonOids(Set(ApiConstants.ESIMERKKI_OPPIJANUMERO)))
  }

  @Test def testValidatePersonOidsMixed(): Unit = {
    val validOid = ApiConstants.ESIMERKKI_OPPIJANUMERO
    val invalidOid = "invalid-oid"
    Assertions.assertEquals(Set(Validator.VALIDATION_EI_VALIDIT_OIDIT + Set(invalidOid)),
      Validator.validatePersonOids(Set(validOid, invalidOid)))
  }

  // vuosi
  @Test def testValidateVuosiRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_VUOSI_TYHJA), Validator.validateVuosi(None, true))
  }

  @Test def testValidateVuosiInvalid(): Unit = {
    val vuosi = "19xx"
    Assertions.assertEquals(Set(Validator.VALIDATION_VUOSI_EI_VALIDI + vuosi), Validator.validateVuosi(Some(vuosi), true))
  }

  @Test def testValidateVuosiValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateVuosi(Some("2023"), true))
  }

  @Test def testValidateVuosiNotRequired(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateVuosi(None, false))
  }

  // luokka
  @Test def testValidateLuokkaRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_LUOKKA_TYHJA), Validator.validateLuokka(None, true))
  }

  @Test def testValidateLuokkaInvalid(): Unit = {
    val luokka = "ABC"
    Assertions.assertEquals(Set(Validator.VALIDATION_LUOKKA_EI_VALIDI + luokka), Validator.validateLuokka(Some(luokka), true))
  }

  @Test def testValidateLuokkaValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateLuokka(Some("9A"), true))
  }

  @Test def testValidateLuokkaNotRequired(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateLuokka(None, false))
  }

  // url
  @Test def testValidateUrlInvalid(): Unit = {
    val url = "not-a-url"
    Assertions.assertEquals(Set(Validator.VALIDATION_URL_EI_VALIDI + url), Validator.validateUrl(url))
  }

  @Test def testValidateUrlValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateUrl("https://example.com/foo.txt"))
  }

  // hakemusOid
  @Test def testValidateHakemusOidRequiredMissing(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_HAKEMUSOID_TYHJA), Validator.validateHakemusOid(None, true))
  }

  @Test def testValidateHakemusOidInvalid(): Unit = {
    val hakemusOid = "not-valid-oid"
    Assertions.assertEquals(Set(Validator.VALIDATION_HAKEMUSOID_EI_VALIDI + hakemusOid),
      Validator.validateHakemusOid(Some(hakemusOid), true))
  }

  @Test def testValidateHakemusOidValid(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateHakemusOid(Some(ApiConstants.ESIMERKKI_HAKEMUS_OID), true))
  }

  @Test def testValidateHakemusOidEmptyString(): Unit = {
    Assertions.assertEquals(Set(Validator.VALIDATION_HAKEMUSOID_TYHJA), Validator.validateHakemusOid(Some(""), true))
  }

  @Test def testValidateHakemusOidNotRequired(): Unit = {
    Assertions.assertEquals(Set.empty, Validator.validateHakemusOid(None, false))
  }
}
