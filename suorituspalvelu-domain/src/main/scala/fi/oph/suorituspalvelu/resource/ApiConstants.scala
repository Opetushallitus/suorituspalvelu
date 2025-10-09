package fi.oph.suorituspalvelu.resource

/**
 * API-endpointtien polkuihin ja Swagger-kuvauksiin liittyvät vakiot
 */
object ApiConstants {

  final val API_PREFIX                                      = "/api"
  final val VERSIONED_API_PREFIX                            = API_PREFIX + "/v1"
  final val UI_API_PREFIX                                   = API_PREFIX + "/ui"

  final val LOGIN_PATH                                      = API_PREFIX + "/login"
  final val CAS_TICKET_VALIDATION_PATH                      = LOGIN_PATH + "/j_spring_cas_security_check"
  final val HEALTHCHECK_PATH                                = VERSIONED_API_PREFIX + "/healthcheck"

  final val DATASYNC_PATH                                   = VERSIONED_API_PREFIX + "/datasync"
  final val DATASYNC_ESIMERKKI_JOB_ID                       = "0181a38f-0883-7a0e-8155-83f5d9a3c226"
  final val DATASYNC_EI_OIKEUKSIA                           = "Käyttäjällä ei ole oikeuksia käynnistää tietojen synkronointia"
  final val DATASYNC_JSON_VIRHE                             = "JSON-deserialisointi epäonnistui"
  final val DATASYNC_RESPONSE_400_DESCRIPTION               = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val DATASYNC_RESPONSE_403_DESCRIPTION               = "Käyttäjälle ei ole rekisterinpitäjä-oikeutta"

  final val KOSKI_DATASYNC_PATH                             = DATASYNC_PATH + "/koski"

  final val KOSKI_DATASYNC_HENKILOT_PATH                    = KOSKI_DATASYNC_PATH + "/henkilot"
  final val KOSKI_DATASYNC_HENKILOT_MAX_MAARA_STR           = "5000"
  final val KOSKI_DATASYNC_HENKILOT_MAX_MAARA               = KOSKI_DATASYNC_HENKILOT_MAX_MAARA_STR.toInt
  final val KOSKI_DATASYNC_HENKILOT_LIIKAA                  = "Tiedot voi hakea korkeintaan 5000 henkilölle kerrallaan"
  final val KOSKI_DATASYNC_500_VIRHE                        = "Tietojen päivitys KOSKI-järjestelmästä epäonnistui"

  final val KOSKI_DATASYNC_HAKU_PATH                        = KOSKI_DATASYNC_PATH + "/haku"
  final val KOSKI_DATASYNC_MUUTTUNEET_PATH                  = KOSKI_DATASYNC_PATH + "/muuttuneet"
  final val KOSKI_DATASYNC_RETRY_PATH                       = KOSKI_DATASYNC_PATH + "/retry"

  final val YTR_DATASYNC_PATH = DATASYNC_PATH + "/ytr"
  final val YTR_DATASYNC_HAKU_PATH = YTR_DATASYNC_PATH + "/haku"

  final val VIRTA_DATASYNC_PARAM_NAME                       = "oppijaNumero"
  final val VIRTA_DATASYNC_PARAM_PLACEHOLDER                = "{" + VIRTA_DATASYNC_PARAM_NAME + "}"
  final val VIRTA_DATASYNC_PATH                             = DATASYNC_PATH + "/virta" + "/" + VIRTA_DATASYNC_PARAM_PLACEHOLDER
  final val VIRTA_DATASYNC_HAKU_PATH                        = DATASYNC_PATH + "/virta" + "/haku/"
  final val VIRTA_DATASYNC_ESIMERKKI_VIRHE                  = "oppijaNumero ei ole validi oid"
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

  final val UI_HAKU_PATH                                    = UI_API_PREFIX + "/oppijat"
  final val UI_HAKU_HAKUSANA_PARAM_NAME                     = "hakusana"
  final val UI_HAKU_OPPILAITOS_PARAM_NAME                   = "oppilaitos"
  final val UI_HAKU_VUOSI_PARAM_NAME                        = "vuosi"
  final val UI_HAKU_LUOKKA_PARAM_NAME                       = "luokka"
  final val UI_HAKU_ESIMERKKI_HAKUKENTAN_ARVO               = "Olli Op"

  final val UI_OPPILAITOKSET_PATH                           = UI_API_PREFIX + "/oppilaitokset"
  final val UI_OPPILAITOKSET_EI_OIKEUKSIA                   = "Käyttäjällä ei ole oikeuksia hakea listaa oppilaitoksista"

  final val UI_KAYTTAJAN_TIEDOT_PATH                        = UI_API_PREFIX + "/kayttaja"

  final val UI_LUO_SUORITUS_PERUSOPETUS_PATH                = UI_API_PREFIX + "/perusopetuksenoppimaarat"

  final val UI_LUO_SUORITUS_OPPIAINE_PATH                   = UI_API_PREFIX + "/perusopetuksenoppiaineenoppimaarat"

  final val UI_LUO_SUORITUS_VAIHTOEHDOT_PATH                = UI_API_PREFIX + "/luosuoritusvaihtoehdot"

  final val UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME    = "versioTunniste"
  final val UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER = "{" + UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME + "}"
  final val UI_POISTA_SUORITUS_PATH                         = UI_API_PREFIX + "/versiot/" + UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER

  final val UI_400_DESCRIPTION                              = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val UI_403_DESCRIPTION                              = "Käyttäjällä ei ole suorittaa pyyntöä"

  /**
   * Swagger-kuvauksiin liittyvät esimerkkiarvot
   */

  final val ESIMERKKI_VERSIOTUNNISTE                        = "0181a38f-0883-7a0e-8155-83f5d9a3c226"
  final val ESIMERKKI_HAKU_OID                              = "1.2.246.562.29.00000000000000044639"
  final val ESIMERKKI_HAKUKOHDE_OID                         = "1.2.246.562.20.00000000000000044758"
  final val ESIMERKKI_OPPILAITOS_OID                        = "1.2.246.562.10.95136889433"
  final val ESIMERKKI_OPPILAITOS_NIMI                       = "Pitäjänmäen peruskoulu"
  final val ESIMERKKI_OPPIJANUMERO                          = "1.2.246.562.24.40483869857"
  final val ESIMERKKI_OPPIJANIMI                            = "Olli Oppija"
  final val ESIMERKKI_HETU                                  = "010296-1230"
  final val ESIMERKKI_SYNTYMAIKA                            = "2030-01-01"
  final val ESIMERKKI_LEGACY_SUORITUSKIELI                  = "fi"
  final val ESIMERKKI_SUORITUSKIELI                         = "FI"
  final val ESIMERKKI_OPPIAINEKOODI                         = "HI"
  final val ESIMERKKI_YKSILOLLISTAMINEN                     = "1"
  final val ESIMERKKI_VIERAS_KIELI_KIELIKOODI               = "DE"
  final val ESIMERKKI_PERUSOPETUKSEN_OPPIAINEEN_ARVOSANA    = "9"
  final val ESIMERKKI_VALMISTUMISPAIVA                      = "2016-06-01"
  final val ESIMERKKI_VUOSI                                 = "2025"
  final val ESIMERKKI_LUOKKA                                = "9B"
  final val ESIMERKKI_AIKALEIMA                             = "2025-09-28T10:15:30.00Z"
  final val ESIMERKKI_TULOSTIEDOSTO                         = "https://virkailija.opintopolku.fi/koski/api/massaluovutus/a45ad5b4-88dc-4586-8401-34978956215d/1.json"

}
