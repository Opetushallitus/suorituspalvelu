package fi.oph.suorituspalvelu.resource

object ApiConstants {

  /**
   * Lähetys-API:n endpointtien polkuihin liittyvät vakiot
   */
  final val API_PREFIX                                      = ""
  final val VERSIONED_API_PREFIX                            = API_PREFIX + "/v1"
  final val UI_API_PREFIX                                   = "/ui"

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
  final val VIRTA_DATASYNC_HAKU_PATH                        = DATASYNC_PATH + "/virta" + "/haku/"
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

  final val UI_TIEDOT_OPPIJANUMERO_PARAM_NAME               = "oppijaNumero"
  final val UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER        = "{" + UI_TIEDOT_OPPIJANUMERO_PARAM_NAME + "}"
  final val UI_TIEDOT_PATH                                  = UI_API_PREFIX + "/tiedot/" + UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER
  final val UI_TIEDOT_400_DESCRIPTION                       = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val UI_TIEDOT_403_DESCRIPTION                       = "Käyttäjällä ei ole oikeuksia nähdä pyydetyn henkilön suorituksia"
  final val UI_TIEDOT_ESIMERKKI_VIRHE                       = "oppijaNumero ei ole validi oid"
  final val UI_TIEDOT_HAKU_EPAONNISTUI                      = "Oppijan tietojen haku epäonnistui"

  final val UI_HAKU_PATH                                    = UI_API_PREFIX + "/oppijat"
  final val UI_HAKU_OPPIJA_PARAM_NAME                       = "oppija"
  final val UI_HAKU_OPPILAITOS_PARAM_NAME                   = "oppilaitos"
  final val UI_HAKU_VUOSI_PARAM_NAME                        = "vuosi"
  final val UI_HAKU_LUOKKA_PARAM_NAME                       = "luokka"
  final val UI_HAKU_ESIMERKKI_OPPIJA                        = "Olli Op"
  final val UI_HAKU_ESIMERKKI_OPPILAITOS_OID                = "1.2.246.562.10.95136889433"
  final val UI_HAKU_ESIMERKKI_OPPILAITOS_NIMI               = "Pitäjänmäen peruskoulu"
  final val UI_HAKU_ESIMERKKI_VUOSI                         = "2025"
  final val UI_HAKU_ESIMERKKI_LUOKKA                        = "9B"
  final val UI_HAKU_ESIMERKKI_VIRHE                         = "oppilaitoksen tunniste ei ole validi oid"
  final val UI_HAKU_ESIMERKKI_OPPIJANUMERO                  = "1.2.246.562.24.40483869857"
  final val UI_HAKU_ESIMERKKI_HETU                          = "010296-1230"
  final val UI_HAKU_ESIMERKKI_NIMI                          = "Olli Oppija"
  final val UI_HAKU_KRITEERI_PAKOLLINEN                     = "Jokin hakukriteeri on pakollinen"
  final val UI_HAKU_OPPIJA_TAI_VUOSI_PAKOLLINEN             = "Oppija tai vuosi on pakollinen jos oppilaitos määritelty"
  final val UI_HAKU_OPPILAITOS_PAKOLLINEN                   = "Oppilaitos on pakollinen jos vuosi määritelty"
  final val UI_HAKU_VUOSI_PAKOLLINEN                        = "Vuosi on pakollinen jos luokka määritelty"
  final val UI_HAKU_EPAONNISTUI                             = "Oppijoiden haku epäonnistui"

  final val UI_OPPILAITOKSET_PATH                           = UI_API_PREFIX + "/oppilaitokset"

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
  final val EXAMPLE_SUORITUSKIELI                           = "fi"
  final val EXAMPLE_OPPIJANUMERO                            = "1.2.246.562.24.40483869857"

}
