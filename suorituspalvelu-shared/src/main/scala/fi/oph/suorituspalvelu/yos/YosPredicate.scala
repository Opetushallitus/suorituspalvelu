package fi.oph.suorituspalvelu.yos

import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, Opiskeluoikeus}
import fi.oph.suorituspalvelu.yos.YosKoulutusAsteLuokka.{ALEMMAT_ASTEET, YLEMMAT_JA_ALEMMAT_ASTEET, YLEMMAT_ASTEET, EI_YOS_KOULUTUSASTETTA}

object YosPredicate {

  /**
   * YOS piiriin kuuluvat Virta opiskeluoikeuden tilat
   * 1 = aktiivinen
   * 2 = optio
   * 4 = passivoitu
   */
  private val YOS_PIIRIIN_KUULUVAT_VIRTAOPISKELUOIKEUDEN_TILAT = Seq("1", "2", "4")
  private val RAHOITUSLAHDE_TILAUSKOULUTUS = "4"

  /**
   * Virta opiskeluoikeuden luokittelut jotka eivät kuulu YOS piiriin
   * 6 = Kansainvälinen yhteistutkinto
   * 7 = Kansainvälinen kaksoistutkinto
   */
  private val VIRTA_LUOKITTELUT_JOTKA_EIVAT_KUULU_YOS_PIIRIIN = Seq("6", "7")

  /**
   * YOS piiriin kuuluvat Virta opiskeluoikeuden tyypit
   * 1 = Ammattikorkeakoulututkinto
   * 2 = Alempi korkeakoulututkinto
   * 3 = Ylempi ammattikorkeakoulututkinto
   * 4 = Ylempi korkeakoulututkinto
   */
  private val YOS_PIIRIIN_KUULUVAT_VIRTA_OPISKELUOIKEUS_TYYPIT = Seq("1", "2", "3", "4")

  def kuuluukoHakutoiveYosinPiiriin(hakutoive: YosHakutoive): Boolean = {
    hakutoive match {
      case YosHakutoive(true, true, false, false, _, ALEMMAT_ASTEET) =>
        true
      case YosHakutoive(true, true, false, false, _, YLEMMAT_JA_ALEMMAT_ASTEET) =>
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
        && (oikeus.rahoitusLahde.isEmpty || oikeus.rahoitusLahde.exists(rl => rl.isBlank) || oikeus.rahoitusLahde.get != RAHOITUSLAHDE_TILAUSKOULUTUS)
        && YOS_PIIRIIN_KUULUVAT_VIRTA_OPISKELUOIKEUS_TYYPIT.contains(oikeus.tyyppiKoodi)
        && (oikeus.luokittelu.isEmpty || oikeus.luokittelu.exists(l => l.isBlank) || !VIRTA_LUOKITTELUT_JOTKA_EIVAT_KUULU_YOS_PIIRIIN.contains(oikeus.luokittelu.get))
        // TODO OPHYOS-171: maanpuolustuskorkeakoulu, poliisiammattikorkeakoulu tai Högskolan på Åland
      case _ =>
        false
  }

  def kuuluukoOpiskeluOikeusYosinPiiriinKoulutusAsteenMukaan(vastaanotettavanAste: YosKoulutusAsteLuokka, oikeudenAste: YosKoulutusAsteLuokka): Boolean = {
    (vastaanotettavanAste, oikeudenAste) match
      case (YLEMMAT_JA_ALEMMAT_ASTEET, YLEMMAT_ASTEET) =>
        true
      case (YLEMMAT_JA_ALEMMAT_ASTEET, ALEMMAT_ASTEET) =>
        true
      case (ALEMMAT_ASTEET, ALEMMAT_ASTEET) =>
        true
      case _ =>
        false
  }
}

