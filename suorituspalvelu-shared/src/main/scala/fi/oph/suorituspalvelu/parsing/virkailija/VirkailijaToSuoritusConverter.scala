package fi.oph.suorituspalvelu.parsing.virkailija

import fi.oph.suorituspalvelu.business.{Koodi, NuortenPerusopetuksenOppiaineenOppimaara, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, SuoritusTila}
import fi.oph.suorituspalvelu.parsing.koski.KoskiToSuoritusConverter.allowMissingFields
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, OpiskeluoikeusJaksoTila}
import fi.oph.suorituspalvelu.resource.ui.{SyotettyPerusopetuksenOppiaine, SyotettyPerusopetuksenOppimaaranSuoritus}
import fi.oph.suorituspalvelu.util.KoodistoProvider

import java.time.LocalDate
import java.util.UUID
import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*

/**
 * Muuntaa virkailjan syöttämän suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object VirkailijaToSuoritusConverter {

  def dummy[A](): A =
    if (allowMissingFields.get())
      null.asInstanceOf[A]
    else
      throw new RuntimeException("Dummies not allowed")

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

  def toOppiaineenNimi(oppiaine: SyotettyPerusopetuksenOppiaine, koodistoProvider: KoodistoProvider): Kielistetty = {
    def getNimi(kieli: String, koodistoProvider: KoodistoProvider): Option[String] =
      koodistoProvider.haeKoodisto("koskioppiaineetyleissivistava")
        .get(oppiaine.koodi.get)
        .flatMap(koodi => koodi.metadata.find(m => m.kieli.equals(kieli)).map(k => k.nimi))

    Kielistetty(getNimi("fi", koodistoProvider), getNimi("sv", koodistoProvider), getNimi("en", koodistoProvider))
  }

  def toPerusopetuksenOppimaara(versioTunniste: UUID, suoritus: SyotettyPerusopetuksenOppimaaranSuoritus, koodistoProvider: KoodistoProvider): PerusopetuksenOpiskeluoikeus =
    PerusopetuksenOpiskeluoikeus(
      Some(versioTunniste),
      UUID.randomUUID(),
      None,
      suoritus.oppilaitosOid.get,
      Set(
        PerusopetuksenOppimaara(
          UUID.randomUUID(),
          suoritus.oppilaitosOid.toScala.getOrElse(dummy()),
          Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), // syötetään vain valmistuneita suorituksia
          SuoritusTila.VALMIS,
          Koodi(suoritus.suorituskieli.get, "kieli", None),
          Set.empty,
          None,
          suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp)),
          suoritus.oppiaineet.toScala.map(oppiaineet => oppiaineet.asScala.toSet.map(oppiaine => PerusopetuksenOppiaine(
            UUID.randomUUID(),
            toOppiaineenNimi(oppiaine, koodistoProvider),
            oppiaine.koodi.toScala.map(k => Koodi(k, "koskioppiaineetyleissivistava", Some(1))).getOrElse(dummy()),
            oppiaine.arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
            suoritus.suorituskieli.toScala.map(k => Koodi(k, "kieli", None))
          ))).getOrElse(Set.empty)
        )
      ),
      None,
      None
    )    

}
