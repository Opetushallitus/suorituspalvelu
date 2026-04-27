package fi.oph.suorituspalvelu.yos

import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, Opiskeluoikeus}

object YosPredicate {

  private val YOSSIN_PIIRIIN_KUULUVAT_VIRTAOPISKELUOIKEUDEN_TILAT = Seq("1", "2", "4")

  def kuuluukoHakutoiveYosinPiiriin(hakutoive: YosHakutoive): Boolean = {
    hakutoive match {
      case YosHakutoive(true, true, false, false, _, _) =>
        true
      case _ =>
        false
    }
  }

  def kuuluukoOpiskeluoikeusYosinPiiriin(opiskeluoikeus: Opiskeluoikeus): Boolean = {
    opiskeluoikeus match
      case oikeus: KKOpiskeluoikeus =>
        YOSSIN_PIIRIIN_KUULUVAT_VIRTAOPISKELUOIKEUDEN_TILAT.contains(oikeus.virtaTila.arvo)
        && oikeus.isTutkintoonJohtava
        // TODO tilaustutkinto
        // TODO jatkotutkinto
        // TODO yhteis- tai kaksoistutkinto
        // TODO maanpuolustuskorkeakoulu, poliisiammattikorkeakoulu tai Högskolan på Åland
      case _ =>
        false
  }

}
