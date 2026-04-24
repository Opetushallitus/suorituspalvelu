package fi.oph.suorituspalvelu.yos

object YosPredicate {

  def kuuluukoHakutoiveYosinPiiriin(hakutoive: YosHakutoive): Boolean = {
    hakutoive match {
      case YosHakutoive(true, true, false, false, _, _) =>
        true
      case _ =>
        false
    }
  }

}
