package fi.oph.suorituspalvelu.parsing.virkailija

import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{Koodi, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaine, PerusopetuksenOppimaaranOppiaineidenSuoritus, SuoritusTila}
import fi.oph.suorituspalvelu.parsing.koski.KoskiToSuoritusConverter.allowMissingFields
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
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

  def toPerusopetuksenOppimaara(versioTunniste: UUID, suoritus: SyotettyPerusopetuksenOppimaaranSuoritus, koodistoProvider: KoodistoProvider, organisaatioProvider: OrganisaatioProvider): PerusopetuksenOpiskeluoikeus =
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
          luokka = suoritus.luokka.toScala,
          koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), // syötetään vain valmistuneita suorituksia
          supaTila = SuoritusTila.valueOf(suoritus.tila.get),
          suoritusKieli = Koodi(suoritus.suorituskieli.get, "kieli", Some(1)),
          koulusivistyskieli = Set(Koodi(suoritus.suorituskieli.get, "kieli", Some(1))),
          yksilollistaminen = suoritus.yksilollistetty.toScala,
          aloitusPaivamaara = None,
          vahvistusPaivamaara = suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp)),
          aineet = suoritus.oppiaineet.toScala.map(oppiaineet => oppiaineet.asScala.toSet.map(oppiaine => PerusopetuksenOppiaine(
            UUID.randomUUID(),
            toOppiaineenNimi(oppiaine.oppiaineKoodi.get, koodistoProvider),
            oppiaine.oppiaineKoodi.toScala.map(k => Koodi(k, "koskioppiaineetyleissivistava", Some(1))).getOrElse(dummy()),
            oppiaine.arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
            oppiaine.kieliLisatieto.toScala.map(k => Koodi(k, if ("AI".equals(oppiaine.oppiaineKoodi.get())) "oppiaineaidinkielijakirjallisuus" else "kielivalikoima", None)),
            oppiaine.valinnainen.toScala.map(p => !p).getOrElse(dummy()),
            None,
            None
          ))).getOrElse(Set.empty),
          syotetty = true
        )
      ),
      None,
      VALMIS
    )

  def toPerusopetuksenOppiaineenOppimaara(versioTunniste: UUID, suoritus: SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer, koodistoProvider: KoodistoProvider, organisaatioProvider: OrganisaatioProvider): PerusopetuksenOpiskeluoikeus = {
    val oppiaineet = suoritus.oppiaineet.asScala.map((oppiaine: SyotettyPerusopetuksenOppiaine) => {
      PerusopetuksenOppimaaranOppiaine(
        tunniste = UUID.randomUUID(),
        nimi = toOppiaineenNimi(oppiaine.oppiaineKoodi.get(), koodistoProvider),
        oppiaine.oppiaineKoodi.toScala.map(k => Koodi(k, "koskioppiaineetyleissivistava", Some(1))).getOrElse(dummy()),
        arvosana = oppiaine.arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
        kieli = oppiaine.kieliLisatieto.toScala.map(k => Koodi(k, "kielivalikoima", None)),
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
      VALMIS
    )
  }

}
