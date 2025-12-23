package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.VUOSILUOKKA_9
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, Lahtokoulu, LahtokouluTyyppi, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, PerusopetuksenVuosiluokka, Suoritus, SuoritusTila, Telma, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiArviointi, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiKotiopetusjakso, KoskiLisatiedot, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila, KoskiParser, KoskiToSuoritusConverter, KoskiUtil}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.UUID

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
          lahtokoulut = Set(Lahtokoulu(LocalDate.now().minusDays(1), Some(LocalDate.now()), "1.2.3", Some(LocalDate.now.getYear), Some("9A"), Some(SuoritusTila.KESKEN), None, VUOSILUOKKA_9)),
          syotetty = false
        )
      ),
      lisatiedot = None,
      tila = KESKEN,
      jaksot = List.empty
    )
    Assertions.assertTrue(KoskiUtil.onkoJokinLahtokoulu(LocalDate.now, None, Some(Set(VUOSILUOKKA_9)), Set(opiskeluoikeus)))

  @Test def testIsYsiluokkalainenValmisPerusopetus(): Unit =
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
          lahtokoulut = Set(Lahtokoulu(LocalDate.now(), Some(LocalDate.now()), "1.2.3", Some(LocalDate.now.getYear), Some("9A"), Some(SuoritusTila.VALMIS), None, VUOSILUOKKA_9)),
          syotetty = false
        ),
      ),
      lisatiedot = None,
      tila = SuoritusTila.VALMIS,
      jaksot = List.empty
    )
    Assertions.assertTrue(KoskiUtil.onkoJokinLahtokoulu(LocalDate.now, None, Some(Set(VUOSILUOKKA_9)), Set(opiskeluoikeus)))
}
