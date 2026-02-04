package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.VUOSILUOKKA_9
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, Lahtokoulu, LahtokouluTyyppi, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, Suoritus, SuoritusTila, Telma, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiArviointi, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiKotiopetusjakso, KoskiLisatiedot, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila, KoskiParser, KoskiToSuoritusConverter, KoskiUtil}
import fi.oph.suorituspalvelu.resource.api.LahtokouluAuthorization
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.{Optional, UUID}
import scala.jdk.OptionConverters.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiUtilTest {

  @Test def testIsYsiluokkalainenEiSuoritusta(): Unit =
    Assertions.assertFalse(KoskiUtil.onkoJokinLahtokoulu(LocalDate.now, None, Some(Set(LahtokouluTyyppi.VUOSILUOKKA_9)), Set.empty))

  @Test def testIsYsiluokkalainenTrue(): Unit =
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = None,
      oppilaitosOid = "1.2.3",
      suoritukset = Set(
        PerusopetuksenOppimaara(
          tunniste = UUID.randomUUID(),
          versioTunniste = None,
          oppilaitos = Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
          luokka = None,
          koskiTila = null,
          supaTila = SuoritusTila.KESKEN,
          suoritusKieli = null,
          koulusivistyskieli = Set.empty,
          yksilollistaminen = None,
          aloitusPaivamaara = None,
          vahvistusPaivamaara = Some(LocalDate.now()),
          aineet = Set.empty,
          lahtokoulut = Set(Lahtokoulu(LocalDate.now().minusDays(1), Some(LocalDate.now()), "1.2.3", Some(LocalDate.now.getYear), "9A", Some(SuoritusTila.KESKEN), None, VUOSILUOKKA_9)),
          syotetty = false,
          vuosiluokkiinSitoutumatonOpetus = false
        )
      ),
      lisatiedot = None,
      tila = KESKEN,
      jaksot = List.empty
    )
    Assertions.assertTrue(KoskiUtil.onkoJokinLahtokoulu(LocalDate.now, None, Some(Set(VUOSILUOKKA_9)), Set(opiskeluoikeus)))

  @Test def testIsYsiluokkalainenValmisPerusopetus(): Unit =
    val vahvistusPaivamaara = LocalDate.parse("2025-06-01")
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = None,
      oppilaitosOid = "1.2.3",
      suoritukset = Set(
        PerusopetuksenOppimaara(
          tunniste = UUID.randomUUID(),
          versioTunniste = None,
          oppilaitos = Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
          luokka = None,
          koskiTila = null,
          supaTila = SuoritusTila.VALMIS,
          suoritusKieli = null,
          koulusivistyskieli = Set.empty,
          yksilollistaminen = None,
          aloitusPaivamaara = Some(LocalDate.now()),
          vahvistusPaivamaara = Some(LocalDate.now()),
          aineet = Set.empty,
          lahtokoulut = Set(Lahtokoulu(LocalDate.now(), Some(LocalDate.now()), "1.2.3", Some(LocalDate.now.getYear), "9A", Some(SuoritusTila.VALMIS), None, VUOSILUOKKA_9)),
          syotetty = false,
          vuosiluokkiinSitoutumatonOpetus = false
        ),
      ),
      lisatiedot = None,
      tila = SuoritusTila.VALMIS,
      jaksot = List.empty
    )
    Assertions.assertTrue(KoskiUtil.onkoJokinLahtokoulu(LocalDate.now, None, Some(Set(VUOSILUOKKA_9)), Set(opiskeluoikeus)))

  @Test def testLuoLahtokouluAuthorizations(): Unit =
    val lahtokoulu1 = Lahtokoulu(LocalDate.parse("2024-08-18"), Some(LocalDate.parse("2025-03-31")), "ensimmäinen", Some(LocalDate.now.getYear), "9A", Some(SuoritusTila.KESKEYTYNYT), None, VUOSILUOKKA_9)
    val lahtokoulu2 = Lahtokoulu(LocalDate.parse("2025-01-01"), Some(LocalDate.parse("2025-05-31")), "toinen", Some(LocalDate.now.getYear), "9A", Some(SuoritusTila.KESKEN), None, VUOSILUOKKA_9)

    Assertions.assertEquals(Seq(
      // ensimmäisen autorisaation loppu on toisen alku koska menevät limittäin
      LahtokouluAuthorization(lahtokoulu1.oppilaitosOid, lahtokoulu1.suorituksenAlku, Optional.of(lahtokoulu2.suorituksenAlku), "9A", lahtokoulu1.suoritusTyyppi.toString),
      // toisen autorisaation loppu on seuraavan vuoden tammikuun loppu (ei-inklusiivinen) (helmikuun loppu demoon)
      LahtokouluAuthorization(lahtokoulu2.oppilaitosOid, lahtokoulu2.suorituksenAlku, Optional.of(LocalDate.parse("2026-03-01")), "9A", lahtokoulu2.suoritusTyyppi.toString)
    ), KoskiUtil.luoLahtokouluAuthorizations(Seq(
      // varmistetaan ettei lähdedatan järjestys merkitse
      lahtokoulu2,
      lahtokoulu1
    )))
}
