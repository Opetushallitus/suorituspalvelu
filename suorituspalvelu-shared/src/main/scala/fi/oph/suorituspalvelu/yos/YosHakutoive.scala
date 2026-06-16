package fi.oph.suorituspalvelu.yos

case class YosHakutoive(korkeakoulutus: Boolean,
                        tutkintoonJohtava: Boolean,
                        jatkoTutkinto: Boolean,
                        kaksoisTutkinto: Boolean,
                        organisaatioJaVanhemmat: List[String],
                        koulutusAste: YosKoulutusAsteLuokka
                       )
