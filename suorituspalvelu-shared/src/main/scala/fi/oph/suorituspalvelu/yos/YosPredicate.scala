package fi.oph.suorituspalvelu.yos

import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, Opiskeluoikeus}

object YosPredicate {

  private val YOS_PIIRIIN_KUULUVAT_VIRTAOPISKELUOIKEUDEN_TILAT = Seq("1", "2", "4")
  private val RAHOITUSLAHDE_TILAUSKOULUTUS = "6"
  private val VIRTA_LUOKITTELUT_JOTKA_EIVAT_KUULU_YOS_PIIRIIN = Seq("6", "7")
  private val YOS_PIIRIIN_KUULUVAT_VIRTA_OPISKELUOIKEUS_TYYPIT = Seq("1", "2", "3", "4")

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
        YOS_PIIRIIN_KUULUVAT_VIRTAOPISKELUOIKEUDEN_TILAT.contains(oikeus.virtaTila.arvo)
        && oikeus.isTutkintoonJohtava
        && (oikeus.rahoitusLahde.isDefined && oikeus.rahoitusLahde.get != RAHOITUSLAHDE_TILAUSKOULUTUS)
        && YOS_PIIRIIN_KUULUVAT_VIRTA_OPISKELUOIKEUS_TYYPIT.contains(oikeus.tyyppiKoodi)
        && (oikeus.luokittelu.isDefined && !VIRTA_LUOKITTELUT_JOTKA_EIVAT_KUULU_YOS_PIIRIIN.contains(oikeus.luokittelu.get))
        // TODO OPHYOS-173: tutkinnontasovertailu 
        // TODO OPHYOS-171: maanpuolustuskorkeakoulu, poliisiammattikorkeakoulu tai Högskolan på Åland
      case _ =>
        false
  }

}
