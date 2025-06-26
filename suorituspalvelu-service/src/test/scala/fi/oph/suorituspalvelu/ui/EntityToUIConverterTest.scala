package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenTutkinto, AmmatillisenTutkinnonOsa, Koodi, Opiskeluoikeus, Oppilaitos}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.resource.ui.SuoritusTapa.NAYTTOTUTKINTO
import org.junit.jupiter.api.*

import java.time.LocalDate
import java.util.Optional

/**
 */
class EntityToUIConverterTest {

  @Test def testConvertAmmatillinenTutkinto(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(AmmatillinenOpiskeluoikeus(
      OPPIJANUMERO,
      Oppilaitos(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en"), "1.2.246.562.10.41945921983"),
      Set(AmmatillinenTutkinto(
        Kielistetty(Some("Tutkinnon nimi"), None, None),
        Koodi("351301", "koulutus", Some(12)),
        Oppilaitos(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en"), "1.2.246.562.10.41945921983"),
        Koodi("", "", None),
        Some(LocalDate.parse("2020-01-01")),
        Some(3.4),
        Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
        Koodi("FI", "kieli", Some(1)),
        Set(
          AmmatillisenTutkinnonOsa(
            Kielistetty(Some("Ajoneuvokaupan myyntitehtävissä toimiminen"), None, None),
            Koodi("106915", "tutkinnonosat", None),
            false,
            Some(Koodi("1", "arviointiasteikkoammatillinen15", None)),
            Some(10),
            None,
            Set.empty
          ),
          AmmatillisenTutkinnonOsa(
            Kielistetty(Some("Viestintä- ja vuorovaikutusosaaminen"), None, None),
            Koodi("106727", "tutkinnonosat", None),
            true,
            Some(Koodi("Hyväksytty", "arviointiasteikkoammatillinen15", None)),
            Some(10),
            None,
            Set.empty
          )
        )
      )),
      None
    ))
    
    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.AmmatillinenTutkinto(
      AmmatillisenTutkinnonNimi(
        Optional.of("Tutkinnon nimi"),
        Optional.empty(),
        Optional.empty()
      ),
      AmmatillinenOppilaitos(
        AmmatillisenOppilaitoksenNimi(
          fi = Optional.of("Stadin ammattiopisto"),
          sv = Optional.of("Stadin ammattiopisto sv"),
          en = Optional.of("Stadin ammattiopisto en")
        ),
        "1.2.246.562.10.41945921983"
      ),
      Tila.VALMIS,
      Optional.of(LocalDate.parse("2020-01-01")),
      "FI",
      Optional.of(3.4),
      java.util.List.of(YTO(
        YTONimi(
          Optional.of("Viestintä- ja vuorovaikutusosaaminen"),
          Optional.empty(),
          Optional.empty()
        ),
        Optional.of(10),
        Optional.of("Hyväksytty"),
      )),
      java.util.List.of(),
      Optional.empty()
    )), EntityToUIConverter.getOppijanTiedot(OPPIJANUMERO, opiskeluoikeudet).get.ammatillisetTutkinnot)
  }

  @Test def testConvertAmmatillinenTutkintoNaytto(): Unit = {
    // TODO: pitääkö suorituksen olla valmis jotta voidaan päätellä onko kyseessä näyttö vai ei?
    
    val OPPIJANUMERO = "1.2.3"

    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(AmmatillinenOpiskeluoikeus(
      OPPIJANUMERO,
      Oppilaitos(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en"), "1.2.246.562.10.41945921983"),
      Set(AmmatillinenTutkinto(
        Kielistetty(Some("Tutkinnon nimi"), None, None),
        Koodi("351301", "koulutus", Some(12)),
        Oppilaitos(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en"), "1.2.246.562.10.41945921983"),
        Koodi("", "", None),
        Some(LocalDate.parse("2020-01-01")),
        Some(3.4),
        Koodi("naytto", "ammatillisentutkinnonsuoritustapa", None),
        Koodi("FI", "kieli", Some(1)),
        Set(
          AmmatillisenTutkinnonOsa(
            Kielistetty(Some("Viestintä- ja vuorovaikutusosaaminen"), None, None),
            Koodi("", "tutkinnonosat", None),
            false,
            None,
            Some(10),
            None,
            Set.empty
          )
        )
      )),
      None
    ))

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.AmmatillinenTutkinto(
      AmmatillisenTutkinnonNimi(
        Optional.of("Tutkinnon nimi"),
        Optional.empty(),
        Optional.empty()
      ),
      AmmatillinenOppilaitos(
        AmmatillisenOppilaitoksenNimi(
          fi = Optional.of("Stadin ammattiopisto"),
          sv = Optional.of("Stadin ammattiopisto sv"),
          en = Optional.of("Stadin ammattiopisto en")
        ),
        "1.2.246.562.10.41945921983"
      ),
      Tila.VALMIS,
      Optional.of(LocalDate.parse("2020-01-01")),
      "FI",
      Optional.of(3.4),
      java.util.List.of(),
      java.util.List.of(),
      Optional.of(NAYTTOTUTKINTO) // Suorituksen osilla ei arvosanoja => näyttötutkinto
    )), EntityToUIConverter.getOppijanTiedot(OPPIJANUMERO, opiskeluoikeudet).get.ammatillisetTutkinnot)
  }

  @Test def testConvertAmmatillinenTutkintoEnnenReformia(): Unit = {
    // TODO: pitääkö suorituksen olla valmis jotta voidaan päätellä onko kyseessä näyttö vai ei?

    val OPPIJANUMERO = "1.2.3"

    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(AmmatillinenOpiskeluoikeus(
      OPPIJANUMERO,
      Oppilaitos(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en"), "1.2.246.562.10.41945921983"),
      Set(AmmatillinenTutkinto(
        Kielistetty(Some("Tutkinnon nimi"), None, None),
        Koodi("351301", "koulutus", Some(12)),
        Oppilaitos(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en"), "1.2.246.562.10.41945921983"),
        Koodi("", "", None),
        Some(LocalDate.parse("2020-01-01")),
        Some(3.4),
        Koodi("ops", "ammatillisentutkinnonsuoritustapa", None),
        Koodi("FI", "kieli", Some(1)),
        Set(
          AmmatillisenTutkinnonOsa(
            Kielistetty(Some("Ajoneuvokaupan myyntitehtävissä toimiminen"), None, None),
            Koodi("106915", "tutkinnonosat", None),
            false,
            None,
            Some(10),
            None,
            Set.empty
          )
        )
      )),
      None
    ))

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.AmmatillinenTutkinto(
      AmmatillisenTutkinnonNimi(
        Optional.of("Tutkinnon nimi"),
        Optional.empty(),
        Optional.empty()
      ),
      AmmatillinenOppilaitos(
        AmmatillisenOppilaitoksenNimi(
          fi = Optional.of("Stadin ammattiopisto"),
          sv = Optional.of("Stadin ammattiopisto sv"),
          en = Optional.of("Stadin ammattiopisto en")
        ),
        "1.2.246.562.10.41945921983"
      ),
      Tila.VALMIS,
      Optional.of(LocalDate.parse("2020-01-01")),
      "FI",
      Optional.of(3.4),
      java.util.List.of(),
      java.util.List.of(fi.oph.suorituspalvelu.resource.ui.AmmatillisenTutkinnonOsa(
        AmmatillisenTutkinnonOsanNimi(
          Optional.of("Ajoneuvokaupan myyntitehtävissä toimiminen"),
          Optional.empty(),
          Optional.empty()
        ),
        Optional.of(10),
        Optional.empty,
      )),
      Optional.of(NAYTTOTUTKINTO)
    )), EntityToUIConverter.getOppijanTiedot(OPPIJANUMERO, opiskeluoikeudet).get.ammatillisetTutkinnot)
  }

}
