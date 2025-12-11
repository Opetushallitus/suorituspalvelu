package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, NuortenPerusopetuksenOppiaineenOppimaara, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, Suoritus, SuoritusTila, Telma, Tuva, VapaaSivistystyo}
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.parsing.koski.{KoskiArviointi, Kielistetty, KoskiErityisenTuenPaatos, KoskiKoodi, KoskiLisatiedot, KoskiUtil, KoskiParser, KoskiToSuoritusConverter, KoskiKotiopetusjakso, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila}
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
    Assertions.assertFalse(KoskiUtil.isYsiluokkalainen(Set.empty))
  
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
          aineet = Set.empty
        ),
        PerusopetuksenVuosiluokka(
          tunniste = UUID.randomUUID(),
          oppilaitos = Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
          nimi = Kielistetty(None, None, None),
          koodi = Koodi("9", "perusopetuksenluokkaaste", None),
          alkamisPaiva = Some(LocalDate.now().minusDays(1)),
          vahvistusPaivamaara = Some(LocalDate.now()),
          jaaLuokalle = false
        )
      ),
      lisatiedot = None,
      tila = KESKEN,
      jaksot = List.empty
    )
    
    Assertions.assertTrue(KoskiUtil.isYsiluokkalainen(Set(opiskeluoikeus)))

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
          aineet = Set.empty
        ),
        PerusopetuksenVuosiluokka(
          tunniste = UUID.randomUUID(),
          oppilaitos = Oppilaitos(Kielistetty(None, None, None), "1.2.3"),
          nimi = Kielistetty(None, None, None),
          koodi = Koodi("9", "perusopetuksenluokkaaste", None),
          alkamisPaiva = Some(LocalDate.now().minusDays(1)),
          vahvistusPaivamaara = Some(LocalDate.now()),
          jaaLuokalle = false
        )
      ),
      lisatiedot = None,
      tila = SuoritusTila.VALMIS,
      jaksot = List.empty
    )

    Assertions.assertFalse(KoskiUtil.isYsiluokkalainen(Set(opiskeluoikeus)))
}
