package fi.oph.suorituspalvelu.yos

case class YosHakutoive(korkeakoulutus: Boolean,
                        tutkintoonJohtava: Boolean,
                        jatkoTutkinto: Boolean,
                        kaksoisTutkinto: Boolean,
                        organisaatioOid: String, //TODO OPHYOS-171, poliisi, ahvenanmaa ja maanpuolustustarkastelua varten
                        koulutusAste: YosKoulutusAsteLuokka
                       )
