package fi.oph.suorituspalvelu.parsing.virkailija

import fi.oph.suorituspalvelu.business.{Koodi, NuortenPerusopetuksenOppiaineenOppimaara, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, SuoritusTila}
import fi.oph.suorituspalvelu.parsing.koski.KoskiToSuoritusConverter.allowMissingFields
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.resource.ui.{SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, SyotettyPerusopetuksenOppimaaranSuoritus}
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

  def toOppiaineenNimi(koodiArvo: String, koodistoProvider: KoodistoProvider): Kielistetty = {
    def getNimi(kieli: String, koodistoProvider: KoodistoProvider): Option[String] =
      koodistoProvider.haeKoodisto("koskioppiaineetyleissivistava")
        .get(koodiArvo)
        .flatMap(koodi => koodi.metadata.find(m => m.kieli.equalsIgnoreCase(kieli)).map(k => k.nimi))

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
            toOppiaineenNimi(oppiaine.koodi.get, koodistoProvider),
            oppiaine.koodi.toScala.map(k => Koodi(k, "koskioppiaineetyleissivistava", Some(1))).getOrElse(dummy()),
            oppiaine.arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
            suoritus.suorituskieli.toScala.map(k => Koodi(k, "kieli", None))
          ))).getOrElse(Set.empty)
        )
      ),
      None,
      None
    )

  def toPerusopetuksenOppiaineenOppimaara(versioTunniste: UUID, suoritus: SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, koodistoProvider: KoodistoProvider): PerusopetuksenOpiskeluoikeus =
    PerusopetuksenOpiskeluoikeus(
      Some(versioTunniste),
      UUID.randomUUID(),
      None,
      suoritus.oppilaitosOid.get,
      Set(
        NuortenPerusopetuksenOppiaineenOppimaara(
          UUID.randomUUID(),
          toOppiaineenNimi(suoritus.oppiaine.get().koodi.get(), koodistoProvider),
          Koodi(suoritus.suorituskieli.get, "kieli", None),
          suoritus.oppiaine.get().arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
          Koodi(suoritus.suorituskieli.get(), "kieli", None),
          None,
          suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp))
        )
      ),
      None,
      None
    )

}
