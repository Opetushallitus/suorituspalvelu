package fi.oph.suorituspalvelu.resource.ui

object UIVirheet {

  final val UI_TIEDOT_ESIMERKKI_VIRHE                       = "backend-virhe.oppijanumero.ei_validi"
  final val UI_TIEDOT_EI_OIKEUKSIA                          = "backend-virhe.oppijantiedot.ei_oikeuksia"
  final val UI_TIEDOT_HAKU_EPAONNISTUI                      = "backend-virhe.oppijantiedot.haku_epaonnistui"

  final val UI_HAKU_ESIMERKKI_VIRHE                         = "backend-virhe.oppilaitos_oid.ei_validi"
  final val UI_HAKU_KRITEERI_PAKOLLINEN                     = "backend-virhe.haku.jokin_kriteeri_pakollinen"
  final val UI_HAKU_OPPIJA_TAI_VUOSI_PAKOLLINEN             = "backend-virhe.haku.oppija_tai_vuosi_pakollinen"
  final val UI_HAKU_OPPILAITOS_PAKOLLINEN                   = "backend-virhe.haku.oppilaitos_pakollinen"
  final val UI_HAKU_VUOSI_PAKOLLINEN                        = "backend-virhe.haku.vuosi_pakollinen_jos_luokka_maaritelty"
  final val UI_HAKU_EI_OIKEUKSIA                            = "backend-virhe.haku.ei_oikeuksia"
  final val UI_HAKU_EPAONNISTUI                             = "backend-virhe.haku.haku_epaonnistui"

  final val UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT                = "backend-virhe.kayttajan_tiedot.ei_loytynyt"
  final val UI_KAYTTAJAN_TIEDOT_HAKU_EPAONNISTUI            = "backend-virhe.kayttajan_tiedot.haku_epaonnistui"

  final val UI_LUO_SUORITUS_VAIHTOEHDOT_ESIMERKKI_VIRHE     = "backend-virhe.luo_suoritus_vaihtoehdot.haku_epaonnistui"

  final val UI_LUO_SUORITUS_PERUSOPETUS_ESIMERKKI_VIRHE     = "backend-virhe.oppijanumero.ei_validi"
  final val UI_LUO_SUORITUS_PERUSOPETUS_ESIMERKKI_OPPIAINE_VIRHE = "backend-virhe.oppiaine.arvosana.ei_validi"
  final val UI_LUO_SUORITUS_PERUSOPETUS_EI_OIKEUKSIA        = "backend-virhe.luo_perusopetuksen_oppimaara.ei_oikeuksia"
  final val UI_LUO_SUORITUS_PERUSOPETUS_JSON_VIRHE          = "backend-virhe.luo_perusopetuksen_oppimaara.json_ei_validi"
  final val UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA   = "backend-virhe.luo_perusopetuksen_oppimaara.oppijanumerolla_ei_loydy_henkiloa"
  final val UI_LUO_SUORITUS_PERUSOPETUS_TALLENNUS_VIRHE     = "backend-virhe.luo_perusopetuksen_oppimaara.tallennus_epaonnistui"

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
}
