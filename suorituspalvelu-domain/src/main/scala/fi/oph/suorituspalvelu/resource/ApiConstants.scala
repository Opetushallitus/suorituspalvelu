package fi.oph.suorituspalvelu.resource

object ApiConstants {

  /**
   * Lähetys-API:n endpointtien polkuihin liittyvät vakiot
   */
  final val API_PREFIX                                      = ""
  final val VERSIONED_API_PREFIX                            = API_PREFIX + "/v1"

  final val LOGIN_PATH                                      = API_PREFIX + "/login"
  final val CAS_TICKET_VALIDATION_PATH                      = LOGIN_PATH + "/j_spring_cas_security_check"
  final val HEALTHCHECK_PATH                                = VERSIONED_API_PREFIX + "/healthcheck"

  final val SUORITUS_PATH                                   = VERSIONED_API_PREFIX + "/suoritus"
  final val DATASYNC_PATH                                   = VERSIONED_API_PREFIX + "/datasync"
  final val KOSKI_DATASYNC_PATH                             = DATASYNC_PATH + "/koski"
  final val KOSKI_DATASYNC_HAKU_PATH                        = KOSKI_DATASYNC_PATH + "/haku"

  final val VIRTA_DATASYNC_PARAM_NAME                       = "oppijaNumero"
  final val VIRTA_DATASYNC_PARAM_PLACEHOLDER                = "{" + VIRTA_DATASYNC_PARAM_NAME + "}"
  final val VIRTA_DATASYNC_PATH                             = DATASYNC_PATH + "/virta" + "/" + VIRTA_DATASYNC_PARAM_PLACEHOLDER
  final val VIRTA_DATASYNC_JOBIN_LUONTI_EPAONNISTUI         = "Datan virkistysjobin luonti epäonnistui"

  final val LEGACY_SUORITUKSET_HENKILO_PARAM_NAME           = "henkilo"
  final val LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME  = "muokattuJalkeen"
  final val LEGACY_SUORITUKSET_PATH                         = VERSIONED_API_PREFIX + "/suoritukset/legacy"
  final val LEGACY_SUORITUKSET_JOKO_OID_TAI_PVM_PAKOLLINEN  = "Joko henkilö tai muokattuJalkeen parametri pitää olla määritelty"
  final val LEGACY_SUORITUKSET_HAKU_EPAONNISTUI             = "YO tai ammatillisten tutkintojen haku epäonnistui"

  final val LEGACY_OPPIJAT_HAKU_PARAM_NAME                  = "haku"
  final val LEGACY_OPPIJAT_HAKUKOHDE_PARAM_NAME             = "hakukohde"
  final val LEGACY_OPPIJAT_ENSIKERTALAISUUDET_PARAM_NAME    = "ensikertalaisuudet"
  final val LEGACY_OPPIJAT_PATH                             = VERSIONED_API_PREFIX + "/oppijat/legacy"

  /**
   * Swagger-kuvauksiin liittyvät vakiot
   */
  final val SUORITUS_RESPONSE_403_DESCRIPTION               = "Käyttäjällä ei ole suorituksen luomiseen tarvittavia oikeuksia"
  final val ESIMERKKI_SUORITUSTUNNISTE                      = "0181a38f-0883-7a0e-8155-83f5d9a3c226"
  final val EXAMPLE_OPPIJANUMERO_VALIDOINTIVIRHE            = "[ \"oppijanumero: Kenttä on pakollinen\" ]"

  final val DATASYNC_ESIMERKKI_JOB_ID                       = "0181a38f-0883-7a0e-8155-83f5d9a3c226"
  final val DATASYNC_VIRTA_ESIMERKKI_VIRHE                  = "oppijaNumero ei ole validi oid"
  final val DATASYNC_RESPONSE_400_DESCRIPTION               = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val DATASYNC_RESPONSE_403_DESCRIPTION               = "Käyttäjälle ei ole rekisterinpitäjä-oikeutta"
  
  final val EXAMPLE_HAKU_OID                                = "1.2.246.562.29.00000000000000044639"
  final val EXAMPLE_HAKUKOHDE_OID                           = "1.2.246.562.20.00000000000000044758"

}
