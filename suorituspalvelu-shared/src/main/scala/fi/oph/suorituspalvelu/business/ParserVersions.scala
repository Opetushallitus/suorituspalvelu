package fi.oph.suorituspalvelu.business

/**
 * Eri suoritusjoukkojen parserin versiot.
 *
 * Versionumero pitää kasvattaa joka kertaa, kun parser/converter logiikka muuttuu
 * niin että se vaikuttaa lopoputulokseen.
 */
object ParserVersions {
  val KOSKI = 1
  val VIRTA = 1
  val YTR = 1
  val SYOTETTY_PERUSOPETUS = 1
  val SYOTETYT_OPPIAINEET = 1

  def forSuoritusJoukko(suoritusJoukko: SuoritusJoukko): Int = suoritusJoukko match {
    case SuoritusJoukko.KOSKI => KOSKI
    case SuoritusJoukko.VIRTA => VIRTA
    case SuoritusJoukko.YTR => YTR
    case SuoritusJoukko.SYOTETTY_PERUSOPETUS => SYOTETTY_PERUSOPETUS
    case SuoritusJoukko.SYOTETYT_OPPIAINEET => SYOTETYT_OPPIAINEET
    case _ => 0
  }
}
