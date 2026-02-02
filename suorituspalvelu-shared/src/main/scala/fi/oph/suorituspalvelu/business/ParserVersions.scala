package fi.oph.suorituspalvelu.business

/**
 * Eri lähdejärjestelmien parserin versiot.
 *
 * Versionumero pitää kasvattaa joka kertaa, kun parser/converter logiikka muuttuu
 * niin että se vaikuttaa lopoputulokseen.
 */
object ParserVersions {
  val KOSKI = 2
  val VIRTA = 1
  val YTR = 1
  val SYOTETTY_PERUSOPETUS = 1
  val SYOTETYT_OPPIAINEET = 1

  def forLahdejarjestelma(lahdejarjest: Lahdejarjestelma): Int = lahdejarjest match {
    case Lahdejarjestelma.KOSKI => KOSKI
    case Lahdejarjestelma.VIRTA => VIRTA
    case Lahdejarjestelma.YTR => YTR
    case Lahdejarjestelma.SYOTETTY_PERUSOPETUS => SYOTETTY_PERUSOPETUS
    case Lahdejarjestelma.SYOTETYT_OPPIAINEET => SYOTETYT_OPPIAINEET
    case _ => 0
  }
}
