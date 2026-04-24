package fi.oph.suorituspalvelu.yos



case class YosHakutoive(korkeakoulutus: Boolean, 
                        tutkintoonJohtava: Boolean,
                        jatkoTutkinto: Boolean,
                        kaksoisTutkinto: Boolean,
                        organisaatioOid: String, //tarviiko hierarkiaa? poliisi ja maanpuolustustarkastelua varten
                        tutkinnonTaso: String, //TODO: enum sopivilla tyypeillä, tarvitaan päättelyyn mitä lakkautetaan
                       )
