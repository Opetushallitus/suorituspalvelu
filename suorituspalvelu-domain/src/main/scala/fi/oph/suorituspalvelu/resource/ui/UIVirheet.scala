package fi.oph.suorituspalvelu.resource.ui

object UIVirheet {

  final val UI_TIEDOT_ESIMERKKI_VIRHE                       = "backend-virhe.oppijanumero.ei_validi"
  final val UI_TIEDOT_EI_OIKEUKSIA                          = "backend-virhe.oppijantiedot.ei_oikeuksia"
  final val UI_TIEDOT_HAKU_EPAONNISTUI                      = "backend-virhe.oppijantiedot.haku_epaonnistui"

  final val UI_HAKU_ESIMERKKI_VIRHE                         = "backend-virhe.oppilaitos_oid.ei_validi"
  final val UI_HAKU_JOKO_HAKUSANA_TAI_OPPILAITOS            = "backend-virhe.haku.joko_hakusana_tai_oppilaitos_sallittu"
  final val UI_HAKU_EI_OIKEUKSIA                            = "backend-virhe.haku.ei_oikeuksia"
  final val UI_HAKU_EPAONNISTUI                             = "backend-virhe.haku.haku_epaonnistui"

  final val UI_OPPILAITOS_HAKU_VUOSI_PAKOLLINEN             = "backend-virhe.haku.vuosi_pakollinen"
  final val UI_OPPILAITOS_HAKU_OPPILAITOS_PAKOLLINEN        = "backend-virhe.haku.oppilaitos_pakollinen"

  final val UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT                = "backend-virhe.kayttajan_tiedot.ei_loytynyt"
  final val UI_KAYTTAJAN_TIEDOT_HAKU_EPAONNISTUI            = "backend-virhe.kayttajan_tiedot.haku_epaonnistui"

  final val UI_RAJAIMEN_TIEDOT_HAKU_EPAONNISTUI             = "backend-virhe.rajaimen_tiedot.haku_epaonnistui"

  final val UI_LUO_SUORITUS_OPPILAITOKSET_ESIMERKKI_VIRHE   = "backend-virhe.luo_suoritus_oppilaitokset.haku_epaonnistui"
  final val UI_LUO_SUORITUS_VAIHTOEHDOT_ESIMERKKI_VIRHE     = "backend-virhe.luo_suoritus_vaihtoehdot.haku_epaonnistui"

  final val UI_LUO_SUORITUS_PERUSOPETUS_ESIMERKKI_VIRHE     = "backend-virhe.oppijanumero.ei_validi"
  final val UI_LUO_SUORITUS_PERUSOPETUS_ESIMERKKI_OPPIAINE_VIRHE = "backend-virhe.oppiaine.arvosana.ei_validi"
  final val UI_LUO_SUORITUS_PERUSOPETUS_EI_OIKEUKSIA        = "backend-virhe.luo_perusopetuksen_oppimaara.ei_oikeuksia"
  final val UI_LUO_SUORITUS_PERUSOPETUS_JSON_VIRHE          = "backend-virhe.luo_perusopetuksen_oppimaara.json_ei_validi"
  final val UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA   = "backend-virhe.luo_perusopetuksen_oppimaara.oppijanumerolla_ei_loydy_henkiloa"
  final val UI_LUO_SUORITUS_PERUSOPETUS_TALLENNUS_VIRHE     = "backend-virhe.luo_perusopetuksen_oppimaara.tallennus_epaonnistui"

  final val UI_TALLENNA_YLIAJO_OPPIJALLE_TALLENNUS_VIRHE    = "backend-virhe.tallenna_yliajo_oppijalle.tallennus_epaonnistui"
  final val UI_TALLENNA_YLIAJO_OPPIJALLE_TUNTEMATON_OPPIJA  = "backend-virhe.tallenna_yliajo_oppijalle.oppijanumerolla_ei_loydy_henkiloa"

  final val UI_LUO_SUORITUS_OPPIAINE_ESIMERKKI_VIRHE        = "backend-virhe.oppijanumero.ei_validi"
  final val UI_LUO_SUORITUS_OPPIAINE_EI_OIKEUKSIA           = "backend-virhe.luo_perusopetuksen_oppimaara.ei_oikeuksia"
  final val UI_LUO_SUORITUS_OPPIAINE_JSON_VIRHE             = "backend-virhe.luo_perusopetuksen_oppimaara.json_ei_validi"
  final val UI_LUO_SUORITUS_OPPIAINE_TUNTEMATON_OPPIJA      = "backend-virhe.luo_perusopetuksen_oppimaara.oppijanumerolla_ei_loydy_henkiloa"
  final val UI_LUO_SUORITUS_OPPIAINE_TALLENNUS_VIRHE        = "backend-virhe.luo_perusopetuksen_oppimaara.tallennus_epaonnistui"

  final val UI_POISTA_SUORITUS_EI_OIKEUKSIA                 = "backend-virhe.poista_suoritus.ei_oikeuksia"
  final val UI_POISTA_SUORITUS_SUORITUSTA_EI_LOYTYNYT       = "backend-virhe.poista_suoritus.suoritusta_ei_loytynyt"
  final val UI_POISTA_SUORITUS_SUORITUSTA_EI_POISTETTAVISSA = "backend-virhe.poista_suoritus.suoritus_ei_poistettavissa"
  final val UI_POISTA_SUORITUS_SUORITUS_EI_VOIMASSA         = "backend-virhe.poista_suoritus.suoritus_ei_voimassa"
  final val UI_POISTA_SUORITUS_TALLENNUS_VIRHE              = "backend-virhe.poista_suoritus.poisto_epaonnistui"

  final val UI_POISTA_YLIAJO_EI_OIKEUKSIA                   = "backend-virhe.poista_yliajo.ei_oikeuksia"
  final val UI_POISTA_YLIAJO_VIRHE                          = "backend-virhe.poista_yliajo.poisto_epaonnistui"

  final val UI_VALINTADATA_EI_OIKEUKSIA                     = "backend-virhe.valintadata.ei_oikeuksia"
  final val UI_VALINTADATA_OPPIJAA_EI_LOYTYNYT              = "backend-virhe.valintadata.oppijaa_ei_loytynyt"
  final val UI_VALINTADATA_GENEERINEN_BACKEND_VIRHE         = "backend-virhe.valintadata.geneerinen_virhe"
  final val UI_VALINTADATA_USEITA_VAHVISTETTUJA_OPPIMAARIA  = "backend-virhe.valintadata.useita_vahvistettuja_oppimaaria"
}
