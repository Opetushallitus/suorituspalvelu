package fi.oph.suorituspalvelu.resource

object ApiConstants {

  /**
   * Lähetys-API:n endpointtien polkuihin liittyvät vakiot
   */
  final val API_PREFIX                  = ""
  final val VERSIONED_API_PREFIX        = API_PREFIX + "/v1"

  final val LOGIN_PATH                  = API_PREFIX + "/login"
  final val CAS_TICKET_VALIDATION_PATH  = LOGIN_PATH + "/j_spring_cas_security_check"
  final val HEALTHCHECK_PATH            = VERSIONED_API_PREFIX + "/healthcheck"

  final val SUORITUS_PATH               = VERSIONED_API_PREFIX + "/suoritus"
  final val DATASYNC_PATH               = VERSIONED_API_PREFIX + "/datasync"
  final val KOSKI_DATASYNC_PATH         = DATASYNC_PATH + "/koski"

  final val VIRTA_DATASYNC_PARAM_NAME   = "oppijaNumero"
  final val VIRTA_DATASYNC_PARAM_PLACEHOLDER  = "{" + VIRTA_DATASYNC_PARAM_NAME + "}"
  final val VIRTA_DATASYNC_PATH         = DATASYNC_PATH + "/virta" + "/" + VIRTA_DATASYNC_PARAM_PLACEHOLDER

  /**
   * Swagger-kuvauksiin liittyvät vakiot
   */
  final val SUORITUS_RESPONSE_403_DESCRIPTION     = "Käyttäjällä ei ole suorituksen luomiseen tarvittavia oikeuksia"
  final val ESIMERKKI_SUORITUSTUNNISTE            = "0181a38f-0883-7a0e-8155-83f5d9a3c226"
  final val EXAMPLE_OPPIJANUMERO_VALIDOINTIVIRHE  = "[ \"oppijanumero: Kenttä on pakollinen\" ]"

  final val DATASYNC_ESIMERKKI_JOB_ID             = "0181a38f-0883-7a0e-8155-83f5d9a3c226"
  final val DATASYNC_VIRTA_ESIMERKKI_VIRHE        = "oppijaNumero ei ole validi oid"
  final val DATASYNC_RESPONSE_400_DESCRIPTION     = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val DATASYNC_RESPONSE_403_DESCRIPTION     = "Käyttäjälle ei ole rekisterinpitäjä-oikeutta"
  final val DATASYNC_JOBIN_LUONTI_EPAONNISTUI     = "Datan virkistysjobin luonti epäonnistui"

  /**
   * Virhetilanteisiin liittyvät vakiot
   */
  final val VIRHEELLINEN_SUORITUS_JSON_VIRHE      = "Suorituksen json-deserialisointi epäonnistui"
  final val SUORITUKSEN_LUONTI_EPAONNISTUI        = "Suorituksen luonti epäonnistui"
}
