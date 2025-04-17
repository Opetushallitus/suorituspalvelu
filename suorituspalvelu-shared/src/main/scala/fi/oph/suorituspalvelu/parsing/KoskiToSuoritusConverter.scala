package fi.oph.suorituspalvelu.parsing

import fi.oph.suorituspalvelu.business.TutkinnonOsa

/**
 * Muuntaa Kosken suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object KoskiToSuoritusConverter {

  def isAmmatillisenTutkinnonOsa(tyyppi: SuoritusTyyppi): Boolean =
    tyyppi.koodiarvo=="ammatillisentutkinnonosa" && tyyppi.koodistoUri=="suorituksentyyppi"

  def toSuoritus(osaSuoritus: OsaSuoritus): fi.oph.suorituspalvelu.business.Suoritus =
    val tyyppi = osaSuoritus.koulutusmoduuli.tunniste.koodistoUri + "_" + osaSuoritus.koulutusmoduuli.tunniste.koodiarvo
    osaSuoritus match
      case suoritus if suoritus.tyyppi.koodiarvo=="ammatillisentutkinnonosa" => TutkinnonOsa(tyyppi)
      case default => fi.oph.suorituspalvelu.business.GenericSuoritus(tyyppi, Seq.empty)

  def toSuoritus(suoritus: Suoritus): fi.oph.suorituspalvelu.business.Suoritus =
    fi.oph.suorituspalvelu.business.GenericSuoritus(
      suoritus.koulutusmoduuli.tunniste.koodistoUri + "_" + suoritus.koulutusmoduuli.tunniste.koodiarvo,
      suoritus.osasuoritukset.map(os => toSuoritus(os)).toSeq
    )

  def toSuoritus(opiskeluoikeudet: Seq[Opiskeluoikeus]): Seq[fi.oph.suorituspalvelu.business.Suoritus] =
    opiskeluoikeudet.map(oo => oo.suoritukset.map(s => toSuoritus(s))).flatten

}
