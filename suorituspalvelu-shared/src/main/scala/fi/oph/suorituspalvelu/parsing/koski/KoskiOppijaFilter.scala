package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.PerusopetuksenOpiskeluoikeus
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.util.KoodistoProvider

object KoskiOppijaFilter {

  /**
   * Henkilö on ysiluokalla jos:
   * - löytyy opiskeluoikeus joka ei ole eronnut-tilassa
   * - ja sen alta löytyy vuosiluokka joka on ysiluokka
   * - ja ei löydy vahvistettua perusopetuksen oppimäärän suoritusta
   *
   * @param opiskeluoikeudet
   * @return
   */
  def isYsiluokkalainen(opiskeluoikeudet: Set[fi.oph.suorituspalvelu.business.Opiskeluoikeus]): Boolean =
    val valmisPerusopetus = opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .filter(o => o.tila==VALMIS)
      .map(o => o.suoritukset)
      .flatten
      .exists(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenOppimaara])

    val eiEronnutYsiLuokka = opiskeluoikeudet
      .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
      .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])
      .filter(o => o.tila!=KESKEYTYNYT)
      .map(o => o.suoritukset)
      .flatten
      .filter(s => s.isInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
      .map(s => s.asInstanceOf[fi.oph.suorituspalvelu.business.PerusopetuksenVuosiluokka])
      .exists(t => {
        t.koodi.arvo=="9"
      })

    !valmisPerusopetus && eiEronnutYsiLuokka

}