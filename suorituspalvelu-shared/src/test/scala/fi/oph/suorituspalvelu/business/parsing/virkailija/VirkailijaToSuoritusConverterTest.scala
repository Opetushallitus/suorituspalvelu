package fi.oph.suorituspalvelu.business.parsing.virkailija

import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.VUOSILUOKKA_9
import fi.oph.suorituspalvelu.business.PerusopetuksenYksilollistaminen.EI_YKSILOLLISTETTY
import fi.oph.suorituspalvelu.integration.client.{KoodiMetadata, Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.parsing.virkailija.VirkailijaToSuoritusConverter
import fi.oph.suorituspalvelu.parsing.virkailija.VirkailijaToSuoritusConverter.dummy
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.ui.{SyotettyPerusopetuksenOppiaine, SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer, SyotettyPerusopetuksenOppimaaranSuoritus}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

import java.time.LocalDate
import java.util.{Optional, UUID}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class VirkailijaToSuoritusConverterTest {

  @Test def testConvertPerusopetuksenOppimaara(): Unit =
    val versioTunniste = UUID.randomUUID()

    val suoritus = SyotettyPerusopetuksenOppimaaranSuoritus(
      Optional.of("1.2.246.562.24.21250967214"),
      Optional.of(ApiConstants.ESIMERKKI_OPPILAITOS_OID),
      Optional.of(SuoritusTila.VALMIS.toString),
      Optional.of(LocalDate.now().toString),
      Optional.of("9A"),
      Optional.of("FI"),
      Optional.of(1),
      Optional.of(List(SyotettyPerusopetuksenOppiaine(
        Optional.of("MA"),
        Optional.empty(),
        Optional.of(9),
        Optional.of(false)
      )).asJava))

    val organisaatio = Organisaatio(
      ApiConstants.ESIMERKKI_OPPILAITOS_OID,
      OrganisaatioNimi(
        "Oppilaitos",
        "Oppilaitos",
        "Oppilaitos"
      ),
      None,
      Seq.empty,
      Seq.empty
    )
    val converted = VirkailijaToSuoritusConverter.toPerusopetuksenOppimaara(versioTunniste, suoritus, koodisto => Map("MA" -> fi.oph.suorituspalvelu.integration.client.Koodi("", null, List(KoodiMetadata("FI", "matematiikka")))), () => Map(organisaatio.oid -> organisaatio))

    val expected = PerusopetuksenOpiskeluoikeus(
      converted.tunniste,
      None,
      suoritus.oppilaitosOid.get,
      Set(
        PerusopetuksenOppimaara(
          converted.suoritukset.head.asInstanceOf[PerusopetuksenOppimaara].tunniste,
          Some(versioTunniste),
          Oppilaitos(
            Kielistetty(
              Some("Oppilaitos"),
              Some("Oppilaitos"),
              Some("Oppilaitos")
            ),
            suoritus.oppilaitosOid.get
          ),
          Some("9A"),
          Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), // syötetään vain valmistuneita suorituksia
          SuoritusTila.VALMIS,
          Koodi(suoritus.suorituskieli.get, "kieli", Some(1)),
          Set(Koodi(suoritus.suorituskieli.get, "kieli", Some(1))),
          Some(EI_YKSILOLLISTETTY),
          None,
          suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp)),
          suoritus.oppiaineet.toScala.map(oppiaineet => oppiaineet.asScala.toSet.map(oppiaine => PerusopetuksenOppiaine(
            converted.suoritukset.head.asInstanceOf[PerusopetuksenOppimaara].aineet.head.tunniste,
            Kielistetty(Some("matematiikka"), None, None),
            oppiaine.koodi.toScala.map(k => Koodi(k, "koskioppiaineetyleissivistava", Some(1))).getOrElse(dummy()),
            oppiaine.arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
            oppiaine.kieli.toScala.map(k => Koodi(k, "kieli", None)),
            oppiaine.valinnainen.toScala.map(p => !p).getOrElse(dummy()),
            None,
            None
          ))).getOrElse(Set.empty),
          Set(Lahtokoulu(LocalDate.now, Some(LocalDate.now), suoritus.oppilaitosOid.get, Some(LocalDate.now.getYear), Some("9A"), Some(SuoritusTila.VALMIS), Some(true), VUOSILUOKKA_9)),
          syotetty = true,
          vuosiluokkiinSitoutumatonOpetus = false
        )
      ),
      None,
      SuoritusTila.VALMIS,
      List(OpiskeluoikeusJakso(suoritus.valmistumispaiva.toScala.map(p => LocalDate.parse(p)).get, SuoritusTila.VALMIS))
    )

    Assertions.assertEquals(expected, converted)

  @Test def testConvertPerusopetuksenOppiaineenOppimaara(): Unit = {
    val versioTunniste = UUID.randomUUID()

    val suoritus = SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer(
      Optional.of("1.2.246.562.24.21250967212"),
      Optional.of(ApiConstants.ESIMERKKI_OPPILAITOS_OID),
      Optional.of(LocalDate.now().toString),
      Optional.of("FI"),
      Optional.of(1),
      java.util.List.of(SyotettyPerusopetuksenOppiaine(
        Optional.of("MA"),
        Optional.empty(),
        Optional.of(9),
        Optional.of(false)
      )))

    val organisaatio = Organisaatio(
      ApiConstants.ESIMERKKI_OPPILAITOS_OID,
      OrganisaatioNimi(
        "Oppilaitos",
        "Oppilaitos",
        "Oppilaitos"
      ),
      None,
      Seq.empty,
      Seq.empty
    )
    val converted = VirkailijaToSuoritusConverter.toPerusopetuksenOppiaineenOppimaara(versioTunniste, suoritus, koodisto => Map("MA" -> fi.oph.suorituspalvelu.integration.client.Koodi("", null, List(KoodiMetadata("FI", "matematiikka")))), () => Map(organisaatio.oid -> organisaatio))

    val expected = PerusopetuksenOpiskeluoikeus(
      converted.tunniste,
      None,
      suoritus.oppilaitosOid.get,
      Set(
        PerusopetuksenOppimaaranOppiaineidenSuoritus(
          tunniste = converted.suoritukset.head.asInstanceOf[PerusopetuksenOppimaaranOppiaineidenSuoritus].tunniste,
          versioTunniste = Some(versioTunniste),
          oppilaitos =
            Oppilaitos(
              Kielistetty(
                Some("Oppilaitos"),
                Some("Oppilaitos"),
                Some("Oppilaitos")
              ),
              suoritus.oppilaitosOid.get
            ),
          koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)),
          supaTila = SuoritusTila.VALMIS,
          suoritusKieli = Koodi(suoritus.suorituskieli.get(), "kieli", None),
          aloitusPaivamaara = None,
          vahvistusPaivamaara = suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp)),
          aineet = converted.suoritukset.head.asInstanceOf[PerusopetuksenOppimaaranOppiaineidenSuoritus].aineet,
          syotetty = true
        )
      ),
      None,
      SuoritusTila.VALMIS,
      List(OpiskeluoikeusJakso(suoritus.valmistumispaiva.toScala.map(p => LocalDate.parse(p)).get, SuoritusTila.VALMIS)  )
    )

    Assertions.assertEquals(expected, converted)
  }

}
