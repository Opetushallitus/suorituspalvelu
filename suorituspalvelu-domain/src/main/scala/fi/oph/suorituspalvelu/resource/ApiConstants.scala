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
  final val DATASYNC_JSON_VIRHE                             = "JSON-deserialisointi epäonnistui"
  final val DATASYNC_RESPONSE_400_DESCRIPTION               = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val DATASYNC_RESPONSE_403_DESCRIPTION               = "Käyttäjällä ei ole rekisterinpitäjä-oikeutta"
  final val DATASYNC_JOBIN_LUONTI_EPAONNISTUI               = "Datan virkistysjobin luonti epäonnistui"

  final val DATASYNC_JOBIT_PATH                             = DATASYNC_PATH + "/jobit"
  final val DATASYNC_JOBIEN_TIETOJEN_HAKU_EPAONNISTUI       = "Jobien tietojen haku epäonnistui"
  final val DATASYNC_JOBIT_TUNNISTE_PARAM_NAME              = "tunniste"
  final val DATASYNC_JOBIT_NIMI_PARAM_NAME                  = "nimi"

  final val KOSKI_DATASYNC_PATH                             = DATASYNC_PATH + "/koski"

  final val KOSKI_DATASYNC_HENKILOT_PATH                    = KOSKI_DATASYNC_PATH + "/henkilot"
  final val KOSKI_DATASYNC_HENKILOT_MAX_MAARA_STR           = "5000"
  final val KOSKI_DATASYNC_HENKILOT_MAX_MAARA               = KOSKI_DATASYNC_HENKILOT_MAX_MAARA_STR.toInt
  final val KOSKI_DATASYNC_HENKILOT_LIIKAA                  = "Tiedot voi hakea korkeintaan 5000 henkilölle kerrallaan"
  final val KOSKI_DATASYNC_500_VIRHE                        = "Tietojen päivitys KOSKI-järjestelmästä epäonnistui"
  final val KOSKI_DATASYNC_ESIMERKKI_VIRHE                  = "henkilöOid:" + ESIMERKKI_OPPIJANUMERO + ", virheet: Henkilön tietojen päivitys epäonnistui"

  final val KOSKI_DATASYNC_HAUT_PATH                        = KOSKI_DATASYNC_PATH + "/haut"
  final val KOSKI_DATASYNC_MUUTTUNEET_PATH                  = KOSKI_DATASYNC_PATH + "/muuttuneet"
  final val KOSKI_DATASYNC_RETRY_PATH                       = KOSKI_DATASYNC_PATH + "/retry"

  final val YTR_DATASYNC_PATH                               = DATASYNC_PATH + "/ytr"
  final val YTR_DATASYNC_HENKILOT_PATH                      = YTR_DATASYNC_PATH + "/henkilot"
  final val YTR_DATASYNC_HAUT_PATH                          = YTR_DATASYNC_PATH + "/haut"
  final val YTR_DATASYNC_AKTIIVISET_PATH                    = YTR_DATASYNC_PATH + "/aktiiviset/"
  final val YTR_DATASYNC_500_VIRHE                          = "Tietojen päivitys YTR-järjestelmästä epäonnistui"

  final val VIRTA_DATASYNC_PARAM_NAME                       = "oppijaNumero"
  final val VIRTA_DATASYNC_PARAM_PLACEHOLDER                = "{" + VIRTA_DATASYNC_PARAM_NAME + "}"
  final val VIRTA_DATASYNC_PATH                             = DATASYNC_PATH + "/virta"
  final val VIRTA_DATASYNC_PATH_OPPIJA                      = VIRTA_DATASYNC_PATH + "/oppija/" + VIRTA_DATASYNC_PARAM_PLACEHOLDER
  final val VIRTA_DATASYNC_HAUT_PATH                        = VIRTA_DATASYNC_PATH + "/haut/"
  final val VIRTA_DATASYNC_HENKILO_PATH                     = VIRTA_DATASYNC_PATH + "/henkilo"
  final val VIRTA_DATASYNC_AKTIIVISET_PATH                  = VIRTA_DATASYNC_PATH + "/aktiiviset/"
  final val VIRTA_DATASYNC_ESIMERKKI_VIRHE                  = "oppijaNumero ei ole validi oid"

  final val DATASYNC_UUDELLEENPARSEROI_PATH                 = DATASYNC_PATH + "/uudelleenparseroi"
  final val DATASYNC_UUDELLEENPARSEROINTI_EPAONNISTUI       = "Uudelleenparseroinnin käynnistys epäonnistui"

  final val LAHETTAVAT_OPPILAITOSOID_PARAM_NAME             = "oppilaitosOid"
  final val LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER      = "{" + LAHETTAVAT_OPPILAITOSOID_PARAM_NAME + "}"
  final val LAHETTAVAT_VUOSI_PARAM_NAME                     = "vuosi"
  final val LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER              = "{" + LAHETTAVAT_VUOSI_PARAM_NAME + "}"
  final val LAHETTAVAT_PATH                                 = VERSIONED_API_PREFIX + "/oppilaitokset/" + LAHETTAVAT_OPPILAITOSOID_PARAM_PLACEHOLDER + "/vuosi/" + LAHETTAVAT_VUOSI_PARAM_PLACEHOLDER
  final val LAHETTAVAT_ESIMERKKI_VIRHE                      = "oppilaitosOid ei ole validi oid"
  final val LAHETTAVAT_500_VIRHE                            = "Tietojen haku epäonnistui"
  final val LAHETTAVAT_HAKU_EPÄONNISTUI                     = "Rajaimien tietojen haku epäonnistui"

  final val LAHETTAVAT_LUOKAT_PATH                          = LAHETTAVAT_PATH + "/luokat"
  final val LAHETTAVAT_HENKILOT_PATH                        = LAHETTAVAT_PATH + "/opiskelijat"

  final val OPISKELIJAT_HENKILOOID_PARAM_NAME               = "henkiloOid"
  final val OPISKELIJAT_HENKILOOID_PARAM_PLACEHOLDER        = "{" + OPISKELIJAT_HENKILOOID_PARAM_NAME + "}"
  final val OPISKELIJAT_AIKALEIMA_PARAM_NAME                = "aikaleima"
  final val OPISKELIJAT_PATH                                = VERSIONED_API_PREFIX + "/opiskelijat/" + OPISKELIJAT_HENKILOOID_PARAM_PLACEHOLDER
  final val OPISKELIJAT_LAHTOKOULUT_PATH                    = OPISKELIJAT_PATH + "/lahtokoulut"

  final val HAKEMUKSET_HAKEMUS_PARAM_NAME                   = "hakemusOid"
  final val HAKEMUKSET_PARAM_PLACEHOLDER                    = "{" + HAKEMUKSET_HAKEMUS_PARAM_NAME + "}"
  final val HAKEMUKSET_PATH                                 = VERSIONED_API_PREFIX + "/hakemukset/" + HAKEMUKSET_PARAM_PLACEHOLDER
  final val HAKEMUKSET_AVAINARVOT_PATH                      = HAKEMUKSET_PATH + "/avainarvot"

  final val LAHETTAVAT_RESPONSE_400_DESCRIPTION             = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val LAHETTAVAT_RESPONSE_403_DESCRIPTION             = "Käyttäjälle ei ole rekisterinpitäjä- tai palvelukäyttäjä-oikeutta"

  final val VALINTALASKENTA_HAKEMUKSET_MAX_MAARA_STR        = "5000"


  final val HAKUKELPOISUUS_HENKILO_PARAM_NAME               = "henkiloOid"
  final val HAKUKELPOISUUS_HENKILO_PARAM_PLACEHOLDER        = "{" + HAKUKELPOISUUS_HENKILO_PARAM_NAME + "}"
  final val VALINNAT_HAKUKELPOISUUS_PATH                    = VERSIONED_API_PREFIX + "/valinnat/hakukelpoisuus/" + HAKUKELPOISUUS_HENKILO_PARAM_PLACEHOLDER

  final val VALINTALASKENTA_VALINTADATA_PATH                = VERSIONED_API_PREFIX + "/valintalaskenta/valintadata"
  final val VALINNAT_HARKINNANVARAISUUS_PATH                = VERSIONED_API_PREFIX + "/valinnat/harkinnanvaraisuus"
  final val VALINTALASKENTA_EI_OIKEUKSIA                    = "Käyttäjällä ei ole oikeuksia hakea valintadataa"
  final val VALINTALASKENTA_JSON_VIRHE                      = "JSON-deserialisointi epäonnistui"
  final val VALINTALASKENTA_RESPONSE_400_DESCRIPTION        = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val VALINTALASKENTA_RESPONSE_403_DESCRIPTION        = "Käyttäjällä ei ole rekisterinpitäjä-oikeutta"
  final val VALINTALASKENTA_HAKEMUKSET_LIIKAA               = "Tiedot voi hakea korkeintaan 5000 hakemukselle kerrallaan"
  final val VALINTALASKENTA_PUUTTUVA_PARAMETRI              = "Joko hakukohdeOid tai 1-5000 hakemusOidia on annettava"
  final val VALINTALASKENTA_HAKUOID_PAKOLLINEN              = "HakuOid on pakollinen parametri"
  final val VALINTALASKENTA_LIIKAA_PARAMETREJA              = "Anna parametrina vain joko hakukohdeOid tai lista hakemusOideja"
  final val VALINTALASKENTA_500_VIRHE                       = "Valintalaskentatietojen haku epäonnistui"
  final val VALINTALASKENTA_HAKEMUKSET_MAX_MAARA            = VALINTALASKENTA_HAKEMUKSET_MAX_MAARA_STR.toInt

  final val HARKINNANVARAISUUS_HAKEMUKSET_MAX_MAARA_STR     = "5000"
  final val HARKINNANVARAISUUS_EI_OIKEUKSIA                 = "Käyttäjällä ei ole oikeuksia hakea harkinnanvaraisuustietoja"
  final val HARKINNANVARAISUUS_HAKEMUKSET_LIIKAA            = "Tiedot voi hakea korkeintaan 5000 hakemukselle kerrallaan"
  final val HARKINNANVARAISUUS_PUUTTUVA_PARAMETRI           = "Joko hakukohdeOid tai 1-5000 hakemusOidia on annettava"
  final val HARKINNANVARAISUUS_HAKEMUKSET_MAX_MAARA         = HARKINNANVARAISUUS_HAKEMUKSET_MAX_MAARA_STR.toInt
  final val HARKINNANVARAISUUS_JSON_VIRHE                   = "JSON-deserialisointi epäonnistui"
  final val HARKINNANVARAISUUS_500_VIRHE                    = "Harkinnanvaraisuustietojen haku epäonnistui"
  final val HARKINNANVARAISUUS_RESPONSE_400_DESCRIPTION     = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val HARKINNANVARAISUUS_RESPONSE_403_DESCRIPTION     = "Käyttäjällä ei ole oikeuksia hakea harkinnanvaraisuustietoja"

  final val AUTOM_HAKUKELPOISUUS_HAKEMUKSET_MAX_MAARA_STR   = "5000"
  final val AUTOM_HAKUKELPOISUUS_EI_OIKEUKSIA               = "Käyttäjällä ei ole oikeuksia hakea automaattisen hakukelpoisuuden tietoja"
  final val AUTOM_HAKUKELPOISUUS_HENKILOT_LIIKAA            = "Tiedot voi hakea korkeintaan 5000 henkilölle kerrallaan"
  final val AUTOM_HAKUKELPOISUUS_PUUTTUVA_PARAMETRI         = "1-5000 henkiloOidia on annettava"
  final val AUTOM_HAKUKELPOISUUS_HENKILOT_MAX_MAARA         = AUTOM_HAKUKELPOISUUS_HAKEMUKSET_MAX_MAARA_STR.toInt
  final val AUTOM_HAKUKELPOISUUS_JSON_VIRHE                 = "JSON-deserialisointi epäonnistui"
  final val AUTOM_HAKUKELPOISUUS_500_VIRHE                  = "Automaattisen hakukelpoisuustiedon haku epäonnistui"
  final val AUTOM_HAKUKELPOISUUS_RESPONSE_400_DESCRIPTION   = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val AUTOM_HAKUKELPOISUUS_RESPONSE_403_DESCRIPTION   = "Käyttäjällä ei ole oikeuksia hakea automaattisen hakukelpoisuuden tietoja"

  final val UI_TIEDOT_PATH                                  = UI_API_PREFIX + "/tiedot"

  final val UI_TALLENNA_YLIAJOT_PATH                        = UI_API_PREFIX + "/tallennayliajot"
  final val UI_POISTA_YLIAJO_PATH                           = UI_API_PREFIX + "/poistayliajo"

  final val UI_HAE_HARKINNANVARAISUUS_YLIAJOT_PATH          = UI_API_PREFIX + "/haeharkinnanvaraisuusyliajot"
  final val UI_TALLENNA_HARKINNANVARAISUUS_YLIAJOT_PATH     = UI_API_PREFIX + "/tallennaharkinnanvaraisuusyliajot"
  final val UI_POISTA_HARKINNANVARAISUUS_YLIAJO_PATH        = UI_API_PREFIX + "/poistaharkinnanvaraisuusyliajo"
  final val UI_HARKINNANVARAISUUS_HAKEMUS_OID_PARAM_NAME    = "hakemusOid"
  final val UI_HARKIKNNANVARAISUUS_HAKUKOHDE_OID_PARAM_NAME = "hakukohdeOid"

  final val UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_NAME         = "oppijaNumero"
  final val UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_PLACEHOLDER  = "{" + UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_NAME + "}"
  final val UI_OPPIJAN_HAUT_PATH                            = UI_API_PREFIX + "/oppijanhaut/" + UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_PLACEHOLDER

  final val UI_VALINTADATA_PATH                             = UI_API_PREFIX + "/valintadata"
  final val UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME          = "oppijaNumero"
  final val UI_VALINTADATA_HAKU_PARAM_NAME                  = "hakuOid"
  final val UI_VALINTADATA_AVAIN_PARAM_NAME                 = "avain"


  final val UI_YLIAJOT_HISTORIA_PATH                        = UI_API_PREFIX + "/valintadatahistoria"
  final val UI_YLIAJOT_HISTORIA_OPPIJANUMERO_PARAM_NAME     = "oppijaNumero"
  final val UI_YLIAJOT_HISTORIA_HAKU_PARAM_NAME             = "hakuOid"
  final val UI_YLIAJOT_HISTORIA_AVAIN_PARAM_NAME            = "avain"

  final val UI_RAJAIN_PATH                                  = UI_API_PREFIX + "/rajain"

  final val UI_OPPILAITOKSET_PATH                           = UI_RAJAIN_PATH + "/oppilaitokset"

  final val UI_VUODET_OPPILAITOS_PARAM_NAME                 = "oppilaitos"
  final val UI_VUODET_OPPILAITOS_PARAM_PLACEHOLDER          = "{" + UI_VUODET_OPPILAITOS_PARAM_NAME + "}"
  final val UI_VUODET_PATH                                  = UI_RAJAIN_PATH + "/vuodet/" + UI_VUODET_OPPILAITOS_PARAM_PLACEHOLDER

  final val UI_LUOKAT_OPPILAITOS_PARAM_NAME                 = "oppilaitos"
  final val UI_LUOKAT_OPPILAITOS_PARAM_PLACEHOLDER          = "{" + UI_LUOKAT_OPPILAITOS_PARAM_NAME + "}"
  final val UI_LUOKAT_VUOSI_PARAM_NAME                      = "vuosi"
  final val UI_LUOKAT_VUOSI_PARAM_PLACEHOLDER               = "{" + UI_LUOKAT_VUOSI_PARAM_NAME + "}"
  final val UI_LUOKAT_PATH                                  = UI_RAJAIN_PATH + "/luokat/" + UI_LUOKAT_OPPILAITOS_PARAM_PLACEHOLDER + "/" + UI_LUOKAT_VUOSI_PARAM_PLACEHOLDER

  final val UI_OPPILAITOS_HAKU_PATH                         = UI_RAJAIN_PATH + "/oppilaitoksenoppijat"
  final val UI_OPPILAITOS_HAKU_OPPILAITOS_PARAM_NAME        = "oppilaitos"
  final val UI_OPPILAITOS_HAKU_VUOSI_PARAM_NAME             = "vuosi"
  final val UI_OPPILAITOS_HAKU_LUOKKA_PARAM_NAME            = "luokka"
  final val UI_OPPILAITOS_HAKU_KESKEN_TAI_KESKEYTYNYT_PARAM_NAME = "keskentaikeskeytynyt"
  final val UI_OPPILAITOS_HAKU_EI_YHTEISTEN_ARVOSANAA_PARAM_NAME = "yhteistenarvosanapuuttuu"

  final val UI_KAYTTAJAN_TIEDOT_PATH                        = UI_API_PREFIX + "/kayttaja"

  final val UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH           = UI_API_PREFIX + "/perusopetuksenoppimaarat"

  final val UI_TALLENNA_SUORITUS_OPPIAINE_PATH              = UI_API_PREFIX + "/perusopetuksenoppiaineenoppimaarat"

  final val UI_TALLENNA_SUORITUS_OPPILAITOKSET_PATH         = UI_API_PREFIX + "/tallennasuoritusoppilaitokset"
  final val UI_TALLENNA_SUORITUS_VAIHTOEHDOT_PATH           = UI_API_PREFIX + "/tallennasuoritusvaihtoehdot"

  final val UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME    = "versioTunniste"
  final val UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER = "{" + UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME + "}"
  final val UI_POISTA_SUORITUS_PATH                         = UI_API_PREFIX + "/versiot/" + UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_PLACEHOLDER

  final val UI_400_DESCRIPTION                              = "Pyyntö virheellinen, palauttaa listan pyynnössä olevista virheistä"
  final val UI_403_DESCRIPTION                              = "Käyttäjällä ei ole oikeuksia suorittaa pyyntöä"
  final val UI_500_DESCRIPTION                              = "Palvelin kohtasi odottamattoman tilanteen, joka esti pyynnön täyttämisen"

  final val UI_OPPIJANUMERO_PARAM_NAME                      = "oppijaNumero"
  final val UI_SELITE_PARAM_NAME                            = "selite"



  /**
   * Swagger-kuvauksiin liittyvät esimerkkiarvot
   */

  final val ESIMERKKI_VERSIOTUNNISTE                        = "0181a38f-0883-7a0e-8155-83f5d9a3c226"
  final val ESIMERKKI_HAKU_OID                              = "1.2.246.562.29.00000000000000044639"
  final val ESIMERKKI_HAKUKOHDE_OID                         = "1.2.246.562.20.00000000000000044758"
  final val ESIMERKKI_HAKEMUS_OID                           = "1.2.246.562.11.00000000000000061316"
  final val ESIMERKKI_OPPILAITOS_OID                        = "1.2.246.562.10.95136889433"
  final val ESIMERKKI_OPPILAITOS_NIMI                       = "Pitäjänmäen peruskoulu"
  final val ESIMERKKI_OPPIJANUMERO                          = "1.2.246.562.24.40483869857"
  final val ESIMERKKI_ETUNIMET                              = "Olli"
  final val ESIMERKKI_SUKUNIMI                              = "Oppija"
  final val ESIMERKKI_HETU                                  = "010296-1230"
  final val ESIMERKKI_SYNTYMAIKA                            = "2030-01-01"
  final val ESIMERKKI_SUORITUSKIELI                         = "FI"
  final val ESIMERKKI_OPPIAINEKOODI                         = "A1"
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

  final val ESIMERKKI_JOB_NIMI                              = "esimerkki-jobi"
  final val ESIMERKKI_JOB_TUNNISTE                          = "5cc3b63f-f3f8-40e9-b535-ba4027d490bb"

  final val ESIMERKKI_HARKINNANVARAISUUDEN_SYY              = "ATARU_OPPIMISVAIKEUDET"
}
