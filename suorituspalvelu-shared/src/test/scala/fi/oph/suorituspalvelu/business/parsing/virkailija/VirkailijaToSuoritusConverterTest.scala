package fi.oph.suorituspalvelu.business.parsing.virkailija

import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.integration.client.KoodiMetadata
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.parsing.virkailija.VirkailijaToSuoritusConverter
import fi.oph.suorituspalvelu.parsing.virkailija.VirkailijaToSuoritusConverter.{dummy, toOppiaineenNimi}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.ui.{SyotettyPerusopetuksenOppiaine, SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, SyotettyPerusopetuksenOppimaaranSuoritus}
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
      Optional.of(LocalDate.now().toString),
      Optional.of("FI"),
      Optional.of(1),
      Optional.of(List(SyotettyPerusopetuksenOppiaine(
        Optional.of("MA"),
        Optional.empty(),
        Optional.empty(),
        Optional.of(9),
        Optional.of(false)
      )).asJava))

    val converted = VirkailijaToSuoritusConverter.toPerusopetuksenOppimaara(versioTunniste, suoritus, koodisto => Map("MA" -> fi.oph.suorituspalvelu.integration.client.Koodi("", null, List(KoodiMetadata("FI", "matematiikka")))))

    val expected = PerusopetuksenOpiskeluoikeus(
      Some(versioTunniste),
      converted.tunniste,
      None,
      suoritus.oppilaitosOid.get,
      Set(
        PerusopetuksenOppimaara(
          converted.suoritukset.head.asInstanceOf[PerusopetuksenOppimaara].tunniste,
          suoritus.oppilaitosOid.toScala.getOrElse(dummy()),
          Koodi("valmistunut", "koskiopiskeluoikeudentila", Some(1)), // syötetään vain valmistuneita suorituksia
          SuoritusTila.VALMIS,
          Koodi(suoritus.suorituskieli.get, "kieli", None),
          Set.empty,
          None,
          suoritus.valmistumispaiva.toScala.map(vp => LocalDate.parse(vp)),
          suoritus.oppiaineet.toScala.map(oppiaineet => oppiaineet.asScala.toSet.map(oppiaine => PerusopetuksenOppiaine(
            converted.suoritukset.head.asInstanceOf[PerusopetuksenOppimaara].aineet.head.tunniste,
            Kielistetty(Some("matematiikka"), None, None),
            oppiaine.koodi.toScala.map(k => Koodi(k, "koskioppiaineetyleissivistava", Some(1))).getOrElse(dummy()),
            oppiaine.arvosana.toScala.map(a => Koodi(a.toString.toLowerCase(), "arviointiasteikkoyleissivistava", Some(1))).getOrElse(dummy()),
            suoritus.suorituskieli.toScala.map(k => Koodi(k, "kieli", None))
          ))).getOrElse(Set.empty)
        )
      ),
      None,
      None
    )

    Assertions.assertEquals(expected, converted)

  @Test def testConvertPerusopetuksenOppiaineenOppimaara(): Unit =
    val versioTunniste = UUID.randomUUID()

    val suoritus = SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus(
        Optional.of("1.2.246.562.24.21250967212"),
        Optional.of(ApiConstants.ESIMERKKI_OPPILAITOS_OID),
        Optional.of(LocalDate.now().toString),
        Optional.of("FI"),
        Optional.of(1),
        Optional.of(SyotettyPerusopetuksenOppiaine(
          Optional.of("MA"),
          Optional.empty(),
          Optional.empty(),
          Optional.of(9),
          Optional.of(false)
        )))

    val converted = VirkailijaToSuoritusConverter.toPerusopetuksenOppiaineenOppimaara(versioTunniste, suoritus, koodisto => Map("MA" -> fi.oph.suorituspalvelu.integration.client.Koodi("", null, List(KoodiMetadata("FI", "matematiikka")))))

    val expected = PerusopetuksenOpiskeluoikeus(
      Some(versioTunniste),
      converted.tunniste,
      None,
      suoritus.oppilaitosOid.get,
      Set(
        NuortenPerusopetuksenOppiaineenOppimaara(
          converted.suoritukset.head.asInstanceOf[NuortenPerusopetuksenOppiaineenOppimaara].tunniste,
          Kielistetty(Some("matematiikka"), None, None),
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

    Assertions.assertEquals(expected, converted)

}
