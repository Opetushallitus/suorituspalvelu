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
  final val DATASYNC_RESPONSE_403_DESCRIPTION               = "Käyttäjällä ei ole rekisterinpitäjä-oikeutta"

  final val KOSKI_DATASYNC_PATH                             = DATASYNC_PATH + "/koski"

  final val KOSKI_DATASYNC_HENKILOT_PATH                    = KOSKI_DATASYNC_PATH + "/henkilot"
  final val KOSKI_DATASYNC_HENKILOT_MAX_MAARA_STR           = "5000"
  final val KOSKI_DATASYNC_HENKILOT_MAX_MAARA               = KOSKI_DATASYNC_HENKILOT_MAX_MAARA_STR.toInt
  final val KOSKI_DATASYNC_HENKILOT_LIIKAA                  = "Tiedot voi hakea korkeintaan 5000 henkilölle kerrallaan"
  final val KOSKI_DATASYNC_500_VIRHE                        = "Tietojen päivitys KOSKI-järjestelmästä epäonnistui"
  final val KOSKI_DATASYNC_ESIMERKKI_VIRHE                  = "henkilöOid:" + ESIMERKKI_OPPIJANUMERO + ", virheet: Henkilön tietojen päivitys epäonnistui"

  final val KOSKI_DATASYNC_HAKU_PATH                        = KOSKI_DATASYNC_PATH + "/haku"
  final val KOSKI_DATASYNC_MUUTTUNEET_PATH                  = KOSKI_DATASYNC_PATH + "/muuttuneet"
  final val KOSKI_DATASYNC_RETRY_PATH                       = KOSKI_DATASYNC_PATH + "/retry"

  final val YTR_DATASYNC_PATH                               = DATASYNC_PATH + "/ytr"
  final val YTR_DATASYNC_HENKILOT_PATH                      = YTR_DATASYNC_PATH + "/henkilot"
  final val YTR_DATASYNC_HAKU_PATH                          = YTR_DATASYNC_PATH + "/haku"

  final val VIRTA_DATASYNC_HENKILO_PATH                     = DATASYNC_PATH + "/virta/henkilo"
  final val VIRTA_DATASYNC_HAKU_PATH                        = DATASYNC_PATH + "/virta/haku"
  final val VIRTA_DATASYNC_ESIMERKKI_VIRHE                  = "oppijaNumero ei ole validi oid"
  final val VIRTA_DATASYNC_JOBIN_LUONTI_EPAONNISTUI         = "Datan virkistysjobin luonti epäonnistui"

  final val LAHETTAVAT_OPPILAITOSOID_PARAM_NAME             = "oppilaitosOid"
  final val LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER      = "{" + LAHETTAVAT_OPPILAITOSOID_PARAM_NAME + "}"
  final val LAHETTAVAT_VUOSI_PARAM_NAME                     = "vuosi"
  final val LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER              = "{" + LAHETTAVAT_VUOSI_PARAM_NAME + "}"
  final val LAHETTAVAT_PATH                                 = VERSIONED_API_PREFIX + "/lahettavat/" + LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER + "/" + LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER
  final val LAHETTAVAT_EI_OIKEUKSIA                         = "Käyttäjällä ei ole oikeuksia hakea rajaimien tietoja"
  final val LAHETTAVAT_ESIMERKKI_VIRHE                      = "oppilaitosOid ei ole validi oid"
  final val LAHETTAVAT_500_VIRHE                            = "Tietojen haku epäonnistui"
  final val LAHETTAVAT_HAKU_EPÄONNISTUI                     = "Rajaimien tietojen haku epäonnistui"

  final val LAHETTAVAT_LUOKAT_PATH                          = LAHETTAVAT_PATH + "/luokat"
  final val LAHETTAVAT_HENKILOT_PATH                        = LAHETTAVAT_PATH + "/opiskelijat"

  final val LAHETTAVAT_RESPONSE_400_DESCRIPTION             = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val LAHETTAVAT_RESPONSE_403_DESCRIPTION             = "Käyttäjälle ei ole rekisterinpitäjä- tai palvelukäyttäjä-oikeutta"

  final val UI_TIEDOT_OPPIJANUMERO_PARAM_NAME               = "oppijaNumero"
  final val UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER        = "{" + UI_TIEDOT_OPPIJANUMERO_PARAM_NAME + "}"
  final val UI_TIEDOT_PATH                                  = UI_API_PREFIX + "/tiedot/" + UI_TIEDOT_OPPIJANUMERO_PARAM_PLACEHOLDER

  final val UI_VALINTADATA_PATH                             = UI_API_PREFIX + "/valintadata"

  final val UI_TALLENNA_YLIAJOT_PATH                        = UI_API_PREFIX + "/tallennayliajot"
  final val UI_POISTA_YLIAJO_PATH                           = UI_API_PREFIX + "/poistayliajo"

  final val UI_HENKILO_HAKU_PATH                            = UI_API_PREFIX + "/oppijat"
  final val UI_HENKILO_HAKU_HAKUSANA_PARAM_NAME             = "hakusana"
  final val UI_HENKILO_HAKU_ESIMERKKI_HAKUKENTAN_ARVO       = "Olli Op"

  final val UI_OPPILAITOS_HAKU_PATH                         = UI_API_PREFIX + "/oppilaitoksenoppijat"
  final val UI_OPPILAITOS_HAKU_OPPILAITOS_PARAM_NAME        = "oppilaitos"
  final val UI_OPPILAITOS_HAKU_VUOSI_PARAM_NAME             = "vuosi"
  final val UI_OPPILAITOS_HAKU_LUOKKA_PARAM_NAME            = "luokka"

  final val UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME          = "oppijaNumero"
  final val UI_VALINTADATA_HAKU_PARAM_NAME                  = "hakuOid"
  final val UI_VALINTADATA_AVAIN_PARAM_NAME                 = "avain"

  final val UI_OPPILAITOKSET_PATH                           = UI_API_PREFIX + "/oppilaitokset"
  final val UI_OPPILAITOKSET_EI_OIKEUKSIA                   = "Käyttäjällä ei ole oikeuksia hakea listaa oppilaitoksista"

  final val UI_VUODET_OPPILAITOS_PARAM_NAME                 = "oppilaitos"
  final val UI_VUODET_OPPILAITOS_PARAM_PLACEHOLDER          = "{" + UI_VUODET_OPPILAITOS_PARAM_NAME + "}"
  final val UI_VUODET_PATH                                  = UI_API_PREFIX + "/luokat/" + UI_VUODET_OPPILAITOS_PARAM_PLACEHOLDER
  final val UI_VUODET_EI_OIKEUKSIA                          = "Käyttäjällä ei ole oikeuksia hakea listaa vuosista"

  final val UI_LUOKAT_OPPILAITOS_PARAM_NAME                 = "oppilaitos"
  final val UI_LUOKAT_OPPILAITOS_PARAM_PLACEHOLDER          = "{" + UI_LUOKAT_OPPILAITOS_PARAM_NAME + "}"
  final val UI_LUOKAT_VUOSI_PARAM_NAME                      = "vuosi"
  final val UI_LUOKAT_VUOSI_PARAM_PLACEHOLDER               = "{" + UI_LUOKAT_VUOSI_PARAM_NAME + "}"
  final val UI_LUOKAT_PATH                                  = UI_API_PREFIX + "/luokat/" + UI_LUOKAT_OPPILAITOS_PARAM_PLACEHOLDER + "/" + UI_LUOKAT_VUOSI_PARAM_PLACEHOLDER
  final val UI_LUOKAT_EI_OIKEUKSIA                          = "Käyttäjällä ei ole oikeuksia hakea listaa luokista"

  final val UI_KAYTTAJAN_TIEDOT_PATH                        = UI_API_PREFIX + "/kayttaja"

  final val UI_LUO_SUORITUS_PERUSOPETUS_PATH                = UI_API_PREFIX + "/perusopetuksenoppimaarat"

  final val UI_LUO_SUORITUS_OPPIAINE_PATH                   = UI_API_PREFIX + "/perusopetuksenoppiaineenoppimaarat"

  final val UI_LUO_SUORITUS_OPPILAITOKSET_PATH              = UI_API_PREFIX + "/luosuoritusoppilaitokset"
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
  final val ESIMERKKI_SUORITUSKIELI                         = "FI"
  final val ESIMERKKI_OPPIAINEKOODI                         = "HI"
  final val ESIMERKKI_YKSILOLLISTAMINEN                     = "1"
  final val ESIMERKKI_VIERAS_KIELI_KIELIKOODI               = "DE"
  final val ESIMERKKI_PERUSOPETUKSEN_OPPIAINEEN_ARVOSANA    = "9"
  final val ESIMERKKI_TILA                                  = "KESKEN"
  final val ESIMERKKI_VALMISTUMISPAIVA                      = "2016-06-01"
  final val ESIMERKKI_VUOSI                                 = "2025"
  final val ESIMERKKI_LUOKKA                                = "9B"
  final val ESIMERKKI_AIKALEIMA                             = "2025-09-28T10:15:30.00Z"
  final val ESIMERKKI_TULOSTIEDOSTO                         = "https://virkailija.opintopolku.fi/koski/api/massaluovutus/a45ad5b4-88dc-4586-8401-34978956215d/1.json"

  final val ESIMERKKI_YLIAJO_AVAIN                          = "lisapistekoulutus_telma"
  final val ESIMERKKI_YLIAJO_ARVO                           = "true"
  final val ESIMERKKI_YLIAJO_VIRKAILIJA                     = "1.2.246.562.24.40483869921"
  final val ESIMERKKI_YLIAJO_SELITE                         = "Tarkistettu, että Telma-suoritus on tarpeeksi laaja. Tiedot puuttuvat lähdejärjestelmästä."

}
