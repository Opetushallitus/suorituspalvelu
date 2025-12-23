package fi.oph.suorituspalvelu.parsing.virkailija

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{AIKUISTEN_PERUSOPETUS, VUOSILUOKKA_9}
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{Koodi, Lahtokoulu, OpiskeluoikeusJakso, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaine, PerusopetuksenOppimaaranOppiaineidenSuoritus, SuoritusTila}
import fi.oph.suorituspalvelu.parsing.koski.KoskiToSuoritusConverter.{allowMissingFields, yhteisenAineenArvosanaPuuttuu}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiOpiskeluoikeusJakso}
import fi.oph.suorituspalvelu.resource.ui.{SyotettyPerusopetuksenOppiaine, SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer, SyotettyPerusopetuksenOppimaaranSuoritus}
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}

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

  def toPerusopetuksenOppimaara(versioTunniste: UUID, suoritus: SyotettyPerusopetuksenOppimaaranSuoritus, koodistoProvider: KoodistoProvider, organisaatioProvider: OrganisaatioProvider): PerusopetuksenOpiskeluoikeus = {
    val vahvistusPaivamaara = suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp))
    val luokka = suoritus.luokka.toScala
    val supaTila = SuoritusTila.valueOf(suoritus.tila.get)
    val aineet = suoritus.oppiaineet.toScala.map(oppiaineet => oppiaineet.asScala.toSet.map(oppiaine => PerusopetuksenOppiaine(
      UUID.randomUUID(),
      toOppiaineenNimi(oppiaine.koodi.get, koodistoProvider),
      oppiaine.koodi.toScala.map(k => Koodi(k, "koskioppiaineetyleissivistava", Some(1))).getOrElse(dummy()),
      oppiaine.arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
      oppiaine.kieli.toScala.map(k => Koodi(k, if ("AI".equals(oppiaine.koodi.get())) "oppiaineaidinkielijakirjallisuus" else "kielivalikoima", None)),
      oppiaine.valinnainen.toScala.map(p => !p).getOrElse(dummy()),
      None,
      None
    ))).getOrElse(Set.empty)

    PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      suoritus.oppilaitosOid.get,
      Set(
        PerusopetuksenOppimaara(
          tunniste = UUID.randomUUID(),
          versioTunniste = Some(versioTunniste),
          oppilaitos = organisaatioProvider.haeOrganisaationTiedot(suoritus.oppilaitosOid.get).map(o =>
            Oppilaitos(
              Kielistetty(
                Option.apply(o.nimi.fi),
                Option.apply(o.nimi.sv),
                Option.apply(o.nimi.en)
              ),
              suoritus.oppilaitosOid.get)
          ).get,
          luokka = luokka,
          koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), // syötetään vain valmistuneita suorituksia
          supaTila = supaTila,
          suoritusKieli = Koodi(suoritus.suorituskieli.get, "kieli", Some(1)),
          koulusivistyskieli = Set(Koodi(suoritus.suorituskieli.get, "kieli", Some(1))),
          yksilollistaminen = suoritus.yksilollistetty.toScala,
          aloitusPaivamaara = None,
          vahvistusPaivamaara = suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp)),
          aineet = aineet,
          lahtokoulut = Set(Lahtokoulu(LocalDate.now, vahvistusPaivamaara, suoritus.oppilaitosOid.get, Some(LocalDate.now.getYear), luokka, Some(supaTila), Some(yhteisenAineenArvosanaPuuttuu(aineet)), VUOSILUOKKA_9)),
          syotetty = true
        )
      ),
      None,
      VALMIS,
      List(OpiskeluoikeusJakso(suoritus.valmistumispaiva.toScala.map(p => LocalDate.parse(p)).getOrElse(dummy()), VALMIS))
    )
  }

  def toPerusopetuksenOppiaineenOppimaara(versioTunniste: UUID, suoritus: SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer, koodistoProvider: KoodistoProvider, organisaatioProvider: OrganisaatioProvider): PerusopetuksenOpiskeluoikeus = {
    val oppiaineet = suoritus.oppiaineet.asScala.map((oppiaine: SyotettyPerusopetuksenOppiaine) => {
      PerusopetuksenOppimaaranOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = toOppiaineenNimi(oppiaine.koodi.get(), koodistoProvider),
        oppiaine.koodi.toScala.map(k => Koodi(k, "koskioppiaineetyleissivistava", Some(1))).getOrElse(dummy()),
        arvosana = oppiaine.arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
        kieli = oppiaine.kieli.toScala.map(k => Koodi(k, "kielivalikoima", None)),
        pakollinen = oppiaine.valinnainen.toScala.map(p => !p).getOrElse(true) //Fixme, vähän ruma.
      )
    }).toSet

    PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      suoritus.oppilaitosOid.get,
      Set(
        PerusopetuksenOppimaaranOppiaineidenSuoritus(
          tunniste = UUID.randomUUID(),
          versioTunniste = Some(versioTunniste),
          oppilaitos = organisaatioProvider.haeOrganisaationTiedot(suoritus.oppilaitosOid.get).map(o => Oppilaitos(
            Kielistetty(
              Option.apply(o.nimi.fi),
              Option.apply(o.nimi.sv),
              Option.apply(o.nimi.en)
            ),
            suoritus.oppilaitosOid.get
          )).get,
          koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), // syötetään vain valmistuneita suorituksia
          supaTila = SuoritusTila.VALMIS,
          suoritusKieli = Koodi(suoritus.suorituskieli.get(), "kieli", None),
          aloitusPaivamaara = None,
          vahvistusPaivamaara = suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp)),
          oppiaineet = oppiaineet,
          syotetty = true
        )
      ),
      None,
      VALMIS,
      List(OpiskeluoikeusJakso(suoritus.valmistumispaiva.toScala.map(p => LocalDate.parse(p)).getOrElse(dummy()), VALMIS))
    )
  }

}
