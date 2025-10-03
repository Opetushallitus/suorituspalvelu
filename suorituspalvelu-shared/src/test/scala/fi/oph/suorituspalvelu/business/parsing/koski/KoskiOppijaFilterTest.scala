package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, NuortenPerusopetuksenOppiaineenOppimaara, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, Suoritus, SuoritusTila, Telma, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.parsing.koski.{Arviointi, Kielistetty, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiLisatiedot, KoskiOppijaFilter, KoskiParser, KoskiToSuoritusConverter, Kotiopetusjakso, OpiskeluoikeusJakso, OpiskeluoikeusTila}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.UUID

@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiOppijaFilterTest {

  @Test def testIsYsiluokkalainenEiSuoritusta(): Unit =
    Assertions.assertFalse(KoskiOppijaFilter.isYsiluokkalainen(Set.empty))
  
  @Test def testIsYsiluokkalainenTrue(): Unit =
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = None,
      oppilaitosOid = "1.2.3",
      suoritukset = Set(
        PerusopetuksenOppimaara(
          tunniste = UUID.randomUUID(),
          versioTunniste = None,
          oppilaitos = null,
          luokka = None,
          koskiTila = null,
          supaTila = SuoritusTila.KESKEN,
          suoritusKieli = null,
          koulusivistyskieli = Set.empty,
          yksilollistaminen = None,
          aloitusPaivamaara = None,
          vahvistusPaivamaara = Some(LocalDate.now()),
          aineet = Set.empty
        ),
        PerusopetuksenVuosiluokka(
          tunniste = UUID.randomUUID(),
          nimi = Kielistetty(None, None, None),
          koodi = Koodi("9", "perusopetuksenluokkaaste", None),
          alkamisPaiva = None,
          jaaLuokalle = false
        )
      ),
      lisatiedot = None,
      tila = KESKEN
    )
    
    Assertions.assertTrue(KoskiOppijaFilter.isYsiluokkalainen(Set(opiskeluoikeus)))

  @Test def testIsYsiluokkalainenValmisPerusopetus(): Unit =
    val opiskeluoikeus = PerusopetuksenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = None,
      oppilaitosOid = "1.2.3",
      suoritukset = Set(
        PerusopetuksenOppimaara(
          tunniste = UUID.randomUUID(),
          versioTunniste = None,
          oppilaitos = null,
          luokka = None,
          koskiTila = null,
          supaTila = SuoritusTila.VALMIS,
          suoritusKieli = null,
          koulusivistyskieli = Set.empty,
          yksilollistaminen = None,
          aloitusPaivamaara = None,
          vahvistusPaivamaara = Some(LocalDate.now()),
          aineet = Set.empty
        ),
        PerusopetuksenVuosiluokka(
          tunniste = UUID.randomUUID(),
          nimi = Kielistetty(None, None, None),
          koodi = Koodi("9", "perusopetuksenluokkaaste", None),
          alkamisPaiva = None,
          jaaLuokalle = false
        )
      ),
      lisatiedot = None,
      tila = SuoritusTila.VALMIS
    )

    Assertions.assertFalse(KoskiOppijaFilter.isYsiluokkalainen(Set(opiskeluoikeus)))
}
