package fi.oph.suorituspalvelu.parsing.virkailija

import fi.oph.suorituspalvelu.business.{Koodi, NuortenPerusopetuksenOppiaineenOppimaara, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, SuoritusTila}
import fi.oph.suorituspalvelu.resource.ui.SyotettyPeruskoulunOppimaaranSuoritus

import java.time.LocalDate
import java.util.UUID
import scala.jdk.OptionConverters.*

/**
 * Muuntaa virkailjan syöttämän suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object VirkailijaToSuoritusConverter {


  /*

  def toNuortenPerusopetuksenOppiaineenOppimaara(suoritus: PeruskoulunOppimaaranSuoritus): NuortenPerusopetuksenOppiaineenOppimaara =
    NuortenPerusopetuksenOppiaineenOppimaara(
      UUID.randomUUID(),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => t.nimi)).getOrElse(dummy()),
      suoritus.koulutusmoduuli.flatMap(km => km.tunniste.map(t => asKoodiObject(t))).get,
      parasArviointi.map(arviointi => asKoodiObject(arviointi.arvosana)).get, //Yksi arviointi löytyy aina, tai muuten näitä ei edes haluta parsia
      suoritus.suorituskieli.map(k => asKoodiObject(k)).getOrElse(dummy()),
      parseAloitus(opiskeluoikeus),
      suoritus.vahvistus.map(v => LocalDate.parse(v.`päivä`))
    )
*/

  def toPerusopetuksenOppimaara(suoritus: SyotettyPeruskoulunOppimaaranSuoritus): PerusopetuksenOpiskeluoikeus =
    PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      null,
      suoritus.oppilaitosOid.get,
      Set(
        PerusopetuksenOppimaara(
          UUID.randomUUID(),
          suoritus.oppilaitosOid.get,
          Koodi("", "", None),
          SuoritusTila.valueOf(suoritus.tila.toString),
          Koodi(suoritus.suorituskieli.get, "kieli", None),
          Set(Koodi(suoritus.koulusivistyskieli.get, "kieli", None)),
          null,
          suoritus.valmistumispaiva.toScala,
          Set.empty
        )
      ),
      None,
      None
    )    

}
