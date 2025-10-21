package fi.oph.suorituspalvelu.parsing.virkailija

import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{Koodi, NuortenPerusopetuksenOppiaineenOppimaara, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, SuoritusTila}
import fi.oph.suorituspalvelu.parsing.koski.KoskiToSuoritusConverter.allowMissingFields
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.resource.ui.{SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, SyotettyPerusopetuksenOppimaaranSuoritus}
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}

import java.time.LocalDate
import java.util.{Optional, UUID}
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

  def toPerusopetuksenOppimaara(versioTunniste: UUID, suoritus: SyotettyPerusopetuksenOppimaaranSuoritus, koodistoProvider: KoodistoProvider, organisaatioProvider: OrganisaatioProvider): PerusopetuksenOpiskeluoikeus =
    PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      suoritus.oppilaitosOid.get,
      Set(
        PerusopetuksenOppimaara(
          UUID.randomUUID(),
          Some(versioTunniste),
          organisaatioProvider.haeOrganisaationTiedot(suoritus.oppilaitosOid.get).map(o => Oppilaitos(
            Kielistetty(
              Option.apply(o.nimi.fi),
              Option.apply(o.nimi.sv),
              Option.apply(o.nimi.en)
            ),
            suoritus.oppilaitosOid.get
          )).get,
          None,
          Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), // syötetään vain valmistuneita suorituksia
          SuoritusTila.VALMIS,
          Koodi(suoritus.suorituskieli.get, "kieli", Some(1)),
          Set(Koodi(suoritus.suorituskieli.get, "kieli", Some(1))),
          suoritus.yksilollistetty.toScala,
          None,
          suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp)),
          suoritus.oppiaineet.toScala.map(oppiaineet => oppiaineet.asScala.toSet.map(oppiaine => PerusopetuksenOppiaine(
            UUID.randomUUID(),
            toOppiaineenNimi(oppiaine.koodi.get, koodistoProvider),
            oppiaine.koodi.toScala.map(k => Koodi(k, "koskioppiaineetyleissivistava", Some(1))).getOrElse(dummy()),
            oppiaine.arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
            oppiaine.kieli.toScala.map(k => Koodi(k, if ("AI".equals(oppiaine.koodi.get())) "oppiaineaidinkielijakirjallisuus" else "kielivalikoima", None)),
            oppiaine.valinnainen.toScala.map(p => !p).getOrElse(dummy()),
            None,
            None
          ))).getOrElse(Set.empty)
        )
      ),
      None,
      VALMIS
    )

  def toPerusopetuksenOppiaineenOppimaara(versioTunniste: UUID, suoritus: SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, koodistoProvider: KoodistoProvider, organisaatioProvider: OrganisaatioProvider): PerusopetuksenOpiskeluoikeus =
    PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      suoritus.oppilaitosOid.get,
      Set(
        NuortenPerusopetuksenOppiaineenOppimaara(
          UUID.randomUUID(),
          Some(versioTunniste),
          organisaatioProvider.haeOrganisaationTiedot(suoritus.oppilaitosOid.get).map(o => Oppilaitos(
            Kielistetty(
              Option.apply(o.nimi.fi),
              Option.apply(o.nimi.sv),
              Option.apply(o.nimi.en)
            ),
            suoritus.oppilaitosOid.get
          )).get,
          toOppiaineenNimi(suoritus.oppiaine.get().koodi.get(), koodistoProvider),
          Koodi(suoritus.suorituskieli.get, "kieli", None),
          suoritus.oppiaine.get().arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
          Koodi(suoritus.suorituskieli.get(), "kieli", None),
          None,
          suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp))
        )
      ),
      None,
      VALMIS
    )

}
