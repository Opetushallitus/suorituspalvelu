package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, PerusopetuksenOppimaaranOppiaineidenSuoritus, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, Suoritus, SuoritusTila, Telma, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.parsing.koski.{Arviointi, Kielistetty, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiLisatiedot, KoskiUtil, KoskiParser, KoskiToSuoritusConverter, Kotiopetusjakso, OpiskeluoikeusJakso, OpiskeluoikeusTila}
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
    Assertions.assertFalse(KoskiUtil.isOponSeurattava(Seq.empty))

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
          syotetty = false
        ),
        PerusopetuksenVuosiluokka(
          tunniste = UUID.randomUUID(),
          oppilaitos = Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
          nimi = Kielistetty(None, None, None),
          koodi = Koodi("9", "perusopetuksenluokkaaste", None),
          alkamisPaiva = None,
          vahvistusPaivamaara = Some(LocalDate.now()),
          jaaLuokalle = false
        )
      ),
      lisatiedot = None,
      tila = KESKEN
    )

    Assertions.assertTrue(KoskiUtil.isOponSeurattava(Seq(opiskeluoikeus)))

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
          aloitusPaivamaara = None,
          vahvistusPaivamaara = Some(LocalDate.now()),
          aineet = Set.empty,
          syotetty = false
        ),
        PerusopetuksenVuosiluokka(
          tunniste = UUID.randomUUID(),
          oppilaitos = Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
          nimi = Kielistetty(None, None, None),
          koodi = Koodi("9", "perusopetuksenluokkaaste", None),
          alkamisPaiva = None,
          vahvistusPaivamaara = Some(LocalDate.now()),
          jaaLuokalle = false
        )
      ),
      lisatiedot = None,
      tila = SuoritusTila.VALMIS
    )

    Assertions.assertFalse(KoskiUtil.isOponSeurattava(Seq(opiskeluoikeus)))
}
