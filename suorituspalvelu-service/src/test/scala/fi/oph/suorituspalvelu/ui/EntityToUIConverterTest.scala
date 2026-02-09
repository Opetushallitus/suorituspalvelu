package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{TELMA, TUVA, VAPAA_SIVISTYSTYO}
import fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, AmmattiTutkinto, Arvosana, EBTutkinto, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, KKOpiskeluoikeusTila, Koe, Koodi, Laajuus, Lahtokoulu, LukionOppimaara, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenYksilollistaminen, Telma, Tuva, VapaaSivistystyo, YOOpiskeluoikeus, YOTutkinto}
import fi.oph.suorituspalvelu.integration.client.{KoodiMetadata, Koodisto, Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.parsing.virta.VirtaToSuoritusConverter
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.resource.ui.SuoritusTapa.NAYTTOTUTKINTO
import fi.oph.suorituspalvelu.service.UIService
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}
import org.junit.jupiter.api.*

import java.time.LocalDate
import java.util.{Optional, UUID}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

/**
 * Testit sisäinen tietomallin konvertoinnille UI:n käyttämään tietomalliin. Mallinnus on pääosin hyvin yksinkertaista,
 * joten ei ole täysin selvää kannattaako näitä testejä ylläpitää erillisinä.
 */
class EntityToUIConverterTest {

  val DUMMY_ORGANISAATIOPROVIDER = new OrganisaatioProvider {
    override def haeKaikkiOrganisaatiot(): Map[String, Organisaatio] = Map("1.2.3" -> Organisaatio("1.2.3", OrganisaatioNimi("", "", ""), None, Seq.empty, Seq.empty))
  }

  val DUMMY_KOODISTOPROVIDER = new KoodistoProvider {
    override def haeKoodisto(koodisto: String): Map[String, fi.oph.suorituspalvelu.integration.client.Koodi] = Map.empty
  }

  @Test def testConvertAmmatillinenTutkinto(): Unit = {
    val tutkinto = AmmatillinenPerustutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkinnon nimi"), None, None),
      Koodi("351301", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en")), "1.2.246.562.10.41945921983"),
      Koodi("valmistunut", "", None),
      fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      Some(LocalDate.parse("2020-01-01")),
      Some(LocalDate.parse("2020-01-01")),
      Some(3.4),
      Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      Koodi("FI", "kieli", Some(1)),
      Set(
        AmmatillisenTutkinnonOsa(
          UUID.randomUUID(),
          Kielistetty(Some("Ajoneuvokaupan myyntitehtävissä toimiminen"), None, None),
          Koodi("106915", "tutkinnonosat", None),
          false,
          Some(LocalDate.parse("2022-01-01")),
          Some(Arvosana(Koodi("1", "arviointiasteikkoammatillinen15", None), Kielistetty(None, None, None))),
          Some(Laajuus(10, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
          Set(AmmatillisenTutkinnonOsaAlue(
            UUID.randomUUID(),
            Kielistetty(Some("Ajoneuvokaupan myyntitehtävissä toimiminen 1"), None, None),
            Koodi("106915", "ammatillisenoppiaineet", None),
            Some(Koodi("1", "arviointiasteikkoammatillinen15", None)),
            Some(Laajuus(10, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None))
          ))
        ),
        AmmatillisenTutkinnonOsa(
          UUID.randomUUID(),
          Kielistetty(Some("Viestintä- ja vuorovaikutusosaaminen"), None, None),
          Koodi("106727", "tutkinnonosat", None),
          true,
          Some(LocalDate.parse("2022-01-01")),
          Some(Arvosana(Koodi("Hyväksytty", "arviointiasteikkoammatillinen15", None), Kielistetty(Some("Hyväksytty"), None, None))),
          Some(Laajuus(10, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
          Set.empty
        )
      )
    )

    Assertions.assertEquals(List(fi.oph.suorituspalvelu.resource.ui.Ammatillinentutkinto(
      tutkinto.tunniste,
      AmmatillinentutkintoNimi(
        tutkinto.nimi.fi.toJava,
        tutkinto.nimi.sv.toJava,
        tutkinto.nimi.en.toJava,
      ),
      AmmatillinenOppilaitos(
        AmmatillinenOppilaitosNimi(
          tutkinto.oppilaitos.nimi.fi.toJava,
          tutkinto.oppilaitos.nimi.sv.toJava,
          tutkinto.oppilaitos.nimi.en.toJava
        ),
        tutkinto.oppilaitos.oid
      ),
      SuoritusTila.VALMIS,
      tutkinto.aloitusPaivamaara.toJava,
      tutkinto.vahvistusPaivamaara.toJava,
      tutkinto.suoritusKieli.arvo,
      tutkinto.keskiarvo.toJava,
      tutkinto.osat
        .filter(osa => osa.yto)
        .map(osa => YTO(
          osa.tunniste,
          YTONimi(
            osa.nimi.fi.toJava,
            osa.nimi.sv.toJava,
            osa.nimi.en.toJava
          ),
          osa.laajuus.map(l => l.arvo).toJava,
          osa.arvosana.map(a =>
            YTOArvosana(
              a.nimi.fi.toJava,
              a.nimi.sv.toJava,
              a.nimi.en.toJava
            )
          ).toJava,
          osa.osaAlueet.map(oa => fi.oph.suorituspalvelu.resource.ui.YTOOsaAlue(
            YTOOsaAlueNimi(
              oa.nimi.fi.toJava,
              oa.nimi.sv.toJava,
              oa.nimi.en.toJava
            ),
            oa.laajuus.map(l => l.arvo).toJava,
            oa.arvosana.map(a => a.arvo).toJava
          )).toList.asJava
        ))
        .toList.asJava,
      java.util.List.of(),
      Optional.empty()
    )), EntityToUIConverter.getAmmatillisetPerusTutkinnot(Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tutkinto), None, List.empty))))
  }

  @Test def testConvertAmmatillinenTutkintoNaytto(): Unit = {
    // TODO: pitääkö suorituksen olla valmis jotta voidaan päätellä onko kyseessä näyttö vai ei?
    val tutkinto = AmmatillinenPerustutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkinnon nimi"), None, None),
      Koodi("351301", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en")), "1.2.246.562.10.41945921983"),
      Koodi("valmistunut", "", None),
      fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      Some(LocalDate.parse("2020-01-01")),
      Some(LocalDate.parse("2020-01-01")),
      None,
      Koodi("naytto", "ammatillisentutkinnonsuoritustapa", None),
      Koodi("FI", "kieli", Some(1)),
      Set(
        AmmatillisenTutkinnonOsa(
          UUID.randomUUID(),
          Kielistetty(Some("Viestintä- ja vuorovaikutusosaaminen"), None, None),
          Koodi("", "tutkinnonosat", None),
          false,
          Some(LocalDate.parse("2022-01-01")),
          None,
          Some(Laajuus(10, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
          Set.empty
        )
      )
    )

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.Ammatillinentutkinto(
      tutkinto.tunniste,
      AmmatillinentutkintoNimi(
        tutkinto.nimi.fi.toJava,
        tutkinto.nimi.sv.toJava,
        tutkinto.nimi.en.toJava,
      ),
      AmmatillinenOppilaitos(
        AmmatillinenOppilaitosNimi(
          tutkinto.oppilaitos.nimi.fi.toJava,
          tutkinto.oppilaitos.nimi.sv.toJava,
          tutkinto.oppilaitos.nimi.en.toJava
        ),
        tutkinto.oppilaitos.oid
      ),
      SuoritusTila.VALMIS,
      tutkinto.aloitusPaivamaara.toJava,
      tutkinto.vahvistusPaivamaara.toJava,
      tutkinto.suoritusKieli.arvo,
      Optional.empty(),
      java.util.List.of(),
      java.util.List.of(),
      Optional.of(NAYTTOTUTKINTO) // Suorituksen osilla ei arvosanoja => näyttötutkinto
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tutkinto), None, List.empty)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOODISTOPROVIDER).ammatillisetPerusTutkinnot)
  }

  @Test def testConvertAmmatillinenTutkintoEnnenReformia(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val tutkinto = AmmatillinenPerustutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkinnon nimi"), None, None),
      Koodi("351301", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en")), "1.2.246.562.10.41945921983"),
      Koodi("valmistunut", "", None),
      fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      Some(LocalDate.parse("2020-01-01")),
      Some(LocalDate.parse("2020-01-01")),
      Some(3.4),
      Koodi("ops", "ammatillisentutkinnonsuoritustapa", None),
      Koodi("FI", "kieli", Some(1)),
      Set(
        AmmatillisenTutkinnonOsa(
          UUID.randomUUID(),
          Kielistetty(Some("Ajoneuvokaupan myyntitehtävissä toimiminen"), None, None),
          Koodi("106915", "tutkinnonosat", None),
          false,
          Some(LocalDate.parse("2022-01-01")),
          None,
          Some(Laajuus(10, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
          Set(AmmatillisenTutkinnonOsaAlue(
            UUID.randomUUID(),
            Kielistetty(Some("Ajoneuvokaupan myyntitehtävissä toimiminen 1"), None, None),
            Koodi("106915", "ammatillisenoppiaineet", None),
            Some(Koodi("1", "arviointiasteikkoammatillinen15", None)),
            Some(Laajuus(10, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None))
          ))
        )
      )
    )

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.Ammatillinentutkinto(
      tutkinto.tunniste,
      AmmatillinentutkintoNimi(
        tutkinto.nimi.fi.toJava,
        tutkinto.nimi.sv.toJava,
        tutkinto.nimi.en.toJava,
      ),
      AmmatillinenOppilaitos(
        AmmatillinenOppilaitosNimi(
          tutkinto.oppilaitos.nimi.fi.toJava,
          tutkinto.oppilaitos.nimi.sv.toJava,
          tutkinto.oppilaitos.nimi.en.toJava
        ),
        tutkinto.oppilaitos.oid
      ),
      SuoritusTila.VALMIS,
      tutkinto.aloitusPaivamaara.toJava,
      tutkinto.vahvistusPaivamaara.toJava,
      tutkinto.suoritusKieli.arvo,
      tutkinto.keskiarvo.toJava,
      java.util.List.of(),
      tutkinto.osat
        .filter(osa => !osa.yto)
        .map(osa => fi.oph.suorituspalvelu.resource.ui.AmmatillisenTutkinnonOsa(
          osa.tunniste,
          AmmatillisenTutkinnonOsaNimi(
            osa.nimi.fi.toJava,
            osa.nimi.sv.toJava,
            osa.nimi.en.toJava
          ),
          osa.laajuus.map(l => l.arvo).toJava,
          osa.arvosana.map(_.koodi.arvo).toJava,
          osa.osaAlueet.map(oa => fi.oph.suorituspalvelu.resource.ui.AmmatillisenTutkinnonOsaAlue(
            AmmatillisenTutkinnonOsaAlueNimi(
              oa.nimi.fi.toJava,
              oa.nimi.sv.toJava,
              oa.nimi.en.toJava
            ),
            oa.laajuus.map(l => l.arvo).toJava,
            oa.arvosana.map(a => a.arvo).toJava
          )).toList.asJava
        ))
        .toList.asJava,
      Optional.of(NAYTTOTUTKINTO)
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tutkinto), None, List.empty)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOODISTOPROVIDER).ammatillisetPerusTutkinnot)
  }

  @Test def testConvertAmmattiTutkinto(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val tutkinto = AmmattiTutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("Hieronnan ammattitutkinto"), None, None),
      Koodi("351301", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Pirkanmaan urheiluhierojakoulu"), Some("Pirkanmaan urheiluhierojakoulu sv"), Some("Pirkanmaan urheiluhierojakoulu en")), "1.2.246.562.10.41945921983"),
      Koodi("valmistunut", "", None),
      fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      Some(LocalDate.parse("2020-01-01")),
      Some(LocalDate.parse("2020-01-01")),
      Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      Koodi("FI", "kieli", Some(1))
    )

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.Ammattitutkinto(
      tutkinto.tunniste,
      AmmattitutkintoNimi(
        tutkinto.nimi.fi.toJava,
        tutkinto.nimi.sv.toJava,
        tutkinto.nimi.en.toJava,
      ),
      AmmatillinenOppilaitos(
        AmmatillinenOppilaitosNimi(
          tutkinto.oppilaitos.nimi.fi.toJava,
          tutkinto.oppilaitos.nimi.sv.toJava,
          tutkinto.oppilaitos.nimi.en.toJava
        ),
        tutkinto.oppilaitos.oid
      ),
      SuoritusTila.VALMIS,
      tutkinto.aloitusPaivamaara.toJava,
      tutkinto.vahvistusPaivamaara.toJava,
      tutkinto.suoritusKieli.arvo,
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tutkinto), None, List.empty)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOODISTOPROVIDER).ammattitutkinnot)
  }

  @Test def testConvertErikoisAmmattiTutkinto(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val tutkinto = ErikoisAmmattiTutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("Talous- ja henkilöstöhallinnon erikoisammattitutkinto"), None, None),
      Koodi("437109", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("HAUS kehittämiskeskus Oy"), Some("HAUS kehittämiskeskus Oy sv"), Some("HAUS kehittämiskeskus Oy en")), "1.2.246.562.10.54019331674"),
      Koodi("valmistunut", "", None),
      fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      Some(LocalDate.parse("2020-01-01")),
      Some(LocalDate.parse("2020-01-01")),
      Koodi("FI", "kieli", Some(1))
    )

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.Erikoisammattitutkinto(
      tutkinto.tunniste,
      ErikoisammattitutkintoNimi(
        tutkinto.nimi.fi.toJava,
        tutkinto.nimi.sv.toJava,
        tutkinto.nimi.en.toJava,
      ),
      AmmatillinenOppilaitos(
        AmmatillinenOppilaitosNimi(
          tutkinto.oppilaitos.nimi.fi.toJava,
          tutkinto.oppilaitos.nimi.sv.toJava,
          tutkinto.oppilaitos.nimi.en.toJava
        ),
        tutkinto.oppilaitos.oid
      ),
      SuoritusTila.VALMIS,
      tutkinto.aloitusPaivamaara.toJava,
      tutkinto.vahvistusPaivamaara.toJava,
      tutkinto.suoritusKieli.arvo
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tutkinto), None, List.empty)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOODISTOPROVIDER).erikoisammattitutkinnot)
  }

  @Test def testConvertTelma(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val telma = Telma(
      UUID.randomUUID(),
      Kielistetty(Some("Työhön ja itsenäiseen elämään valmentava koulutus (TELMA)"), None, None),
      Koodi("999903", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Savon ammattiopisto"), Some("Savon ammattiopisto sv"), Some("Savon ammattiopisto en")), "1.2.246.562.10.11168857016"),
      Koodi("valmistunut", "", None),
      fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      LocalDate.parse("2020-01-01"),
      Some(LocalDate.parse("2020-01-01")),
      2020,
      Koodi("FI", "kieli", Some(1)),
      Some(Laajuus(18, Koodi("8", "opintojenlaajuusyksikko", Some(1)), None, Some(Kielistetty(Some("op"), None, None)))),
      Lahtokoulu(LocalDate.parse("2020-01-01"), Some(LocalDate.parse("2020-01-01")), "1.2.246.562.10.11168857016", Some(2020), TELMA.defaultLuokka.get, Some(VALMIS), None, TELMA)
    )

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.Telma(
      telma.tunniste,
      TelmaNimi(
        telma.nimi.fi.toJava,
        telma.nimi.sv.toJava,
        telma.nimi.en.toJava,
      ),
      AmmatillinenOppilaitos(
        AmmatillinenOppilaitosNimi(
          telma.oppilaitos.nimi.fi.toJava,
          telma.oppilaitos.nimi.sv.toJava,
          telma.oppilaitos.nimi.en.toJava
        ),
        telma.oppilaitos.oid
      ),
      SuoritusTila.VALMIS,
      Optional.of(telma.aloitusPaivamaara),
      telma.vahvistusPaivamaara.toJava,
      telma.suoritusKieli.arvo
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(telma), None, List.empty)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOODISTOPROVIDER).telmat)
  }

  @Test def testConvertTuva(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val tuva = Tuva(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkintokoulutukseen valmentava koulutus (TUVA)"), None, None),
      Koodi("999907", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Savon ammattiopisto"), Some("Savon ammattiopisto sv"), Some("Savon ammattiopisto en")), "1.2.246.562.10.11168857016"),
      Koodi("valmistunut", "", None),
      fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      LocalDate.parse("2020-01-01"),
      Some(LocalDate.parse("2020-01-01")),
      2020,
      Some(Laajuus(11, Koodi("8", "opintojenlaajuusyksikko", Some(1)), None, Some(Kielistetty(Some("vk"), None, None)))),
      Lahtokoulu(LocalDate.parse("2020-01-01"), Some(LocalDate.parse("2020-01-01")), "1.2.246.562.10.11168857016", Some(2020), TUVA.defaultLuokka.get, Some(VALMIS), None, TUVA)
    )

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.TuvaUI(
      tuva.tunniste,
      TuvaNimi(
        tuva.nimi.fi.toJava,
        tuva.nimi.sv.toJava,
        tuva.nimi.en.toJava,
      ),
      AmmatillinenOppilaitos(
        AmmatillinenOppilaitosNimi(
          tuva.oppilaitos.nimi.fi.toJava,
          tuva.oppilaitos.nimi.sv.toJava,
          tuva.oppilaitos.nimi.en.toJava
        ),
        tuva.oppilaitos.oid
      ),
      SuoritusTila.VALMIS,
      Optional.of(tuva.aloitusPaivamaara),
      tuva.vahvistusPaivamaara.toJava,
      tuva.hyvaksyttyLaajuus.map(l => TuvaLaajuus(l.arvo, TuvaLaajuusYksikko(
        l.lyhytNimi.get.fi.toJava,
        l.lyhytNimi.get.sv.toJava,
        l.lyhytNimi.get.en.toJava
      ))).toJava,
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(GeneerinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Koodi("tuva", "opiskeluoikeudentyyppi", Some(1)), "1.2.3.4.5", Set(tuva), None, List.empty)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOODISTOPROVIDER).tuvat)
  }

  @Test def testConvertVapaaSivistystyoKoulutus(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val vst = VapaaSivistystyo(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkintokoulutukseen valmentava koulutus (TUVA)"), None, None),
      Koodi("999907", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Savon ammattiopisto"), Some("Savon ammattiopisto sv"), Some("Savon ammattiopisto en")), "1.2.246.562.10.11168857016"),
      Koodi("valmistunut", "", None),
      fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      LocalDate.parse("2020-01-01"),
      Some(LocalDate.parse("2020-01-01")),
      2020,
      Some(Laajuus(11, Koodi("8", "opintojenlaajuusyksikko", Some(1)), None, Some(Kielistetty(Some("op"), None, None)))),
      Koodi("FI", "kieli", Some(1)),
      Lahtokoulu(LocalDate.parse("2020-01-01"), Some(LocalDate.parse("2020-01-01")), "1.2.246.562.10.11168857016", Some(2020), VAPAA_SIVISTYSTYO.defaultLuokka.get, Some(fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS), None, VAPAA_SIVISTYSTYO)
    )

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.VapaaSivistystyoKoulutus(
      vst.tunniste,
      VapaaSivistystyoKoulutusNimi(
        vst.nimi.fi.toJava,
        vst.nimi.sv.toJava,
        vst.nimi.en.toJava,
      ),
      VapaaSivistystyoOppilaitos(
        VapaaSivistystyoOppilaitosNimi(
          vst.oppilaitos.nimi.fi.toJava,
          vst.oppilaitos.nimi.sv.toJava,
          vst.oppilaitos.nimi.en.toJava
        ),
        vst.oppilaitos.oid
      ),
      SuoritusTila.VALMIS,
      Optional.of(vst.aloitusPaivamaara),
      vst.vahvistusPaivamaara.toJava,
      vst.suoritusKieli.arvo,
      vst.hyvaksyttyLaajuus.map(l => VapaaSivistystyoLaajuus(l.arvo, VapaaSivistystyoLaajuusYksikko(
        l.lyhytNimi.get.fi.toJava,
        l.lyhytNimi.get.sv.toJava,
        l.lyhytNimi.get.en.toJava
      ))).toJava,
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(GeneerinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Koodi("", "", None), "", Set(vst), None, List.empty)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOODISTOPROVIDER).vapaaSivistystyoKoulutukset)
  }

  @Test def testConvertPerusopetuksenOppimaara(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val oppimaara = PerusopetuksenOppimaara(
      tunniste = UUID.randomUUID(),
      versioTunniste = Some(UUID.randomUUID()),
      oppilaitos = Oppilaitos(Kielistetty(Some("Pitäjänmäen peruskoulu"), None, None), "1.2.246.562.10.11168857016"),
      luokka = Some("9A"),
      koskiTila = Koodi("valmistunut", "", None),
      supaTila = fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      suoritusKieli = Koodi("FI", "kieli", Some(1)),
      koulusivistyskieli = Set(Koodi("FI", "kieli", Some(1))),
      yksilollistaminen = Some(PerusopetuksenYksilollistaminen.EI_YKSILOLLISTETTY),
      aloitusPaivamaara = Some(LocalDate.parse("2020-01-01")),
      vahvistusPaivamaara = Some(LocalDate.parse("2020-01-01")),
      aineet = Set(
        PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = Kielistetty(Some("Historia"), None, None),
          koodi = Koodi("HI", "koskioppiaineetyleissivistava", Some(1)),
          arvosana = Koodi("9", "arviointiasteikkoyleissivistava", Some(1)),
          kieli = None,
          pakollinen = true,
          yksilollistetty = Some(false),
          rajattu = Some(false)
        ),
        PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = Kielistetty(Some("A1-kieli"), None, None),
          koodi = Koodi("A1", "koskioppiaineetyleissivistava", Some(1)),
          arvosana = Koodi("9", "arviointiasteikkoyleissivistava", Some(1)),
          kieli = Some(Koodi("DE", "kielivalikoima", Some(1))),
          pakollinen = true,
          yksilollistetty = Some(false),
          rajattu = Some(false)
        ),
      ),
      lahtokoulut = Set.empty,
      syotetty = false,
      vuosiluokkiinSitoutumatonOpetus = false
    )

    val koodistoProvider = new KoodistoProvider {
      override def haeKoodisto(koodisto: String): Map[String, fi.oph.suorituspalvelu.integration.client.Koodi] = Map(
        "1" -> fi.oph.suorituspalvelu.integration.client.Koodi(
          "1",
          Koodisto("2asteenpohjakoulutus2021"),
          List(
            KoodiMetadata("FI", "Perusopetuksen oppimäärä")
          )
        ),
        "DE" -> fi.oph.suorituspalvelu.integration.client.Koodi(
          "DE",
          Koodisto("kielivalikoima"),
          List(
            KoodiMetadata("FI", "saksa")
          )
        ),
      )
    }

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.PerusopetuksenOppimaaraUI(
      versioTunniste = oppimaara.versioTunniste.toJava,
      tunniste = oppimaara.tunniste,
      nimi = PerusopetuksenOppimaaraNimi(
        Optional.of("Perusopetuksen oppimäärä"),
        Optional.of("Grundläggande utbildningens lärokurs"),
        Optional.of("Basic education syllabus")
      ),
      oppilaitos = PKOppilaitos(
        nimi = PKOppilaitosNimi(
          oppimaara.oppilaitos.nimi.fi.toJava,
          oppimaara.oppilaitos.nimi.sv.toJava,
          oppimaara.oppilaitos.nimi.en.toJava
        ),
        oid = oppimaara.oppilaitos.oid
      ),
      tila = SuoritusTila.valueOf(oppimaara.supaTila.toString),
      aloituspaiva = oppimaara.aloitusPaivamaara.toJava,
      valmistumispaiva = oppimaara.vahvistusPaivamaara.toJava,
      suorituskieli = oppimaara.suoritusKieli.arvo,
      luokka = oppimaara.luokka.toJava,
      yksilollistaminen = Optional.of(Yksilollistaminen(
        arvo = PerusopetuksenYksilollistaminen.toIntValue(oppimaara.yksilollistaminen.get),
        nimi = YksilollistamisNimi(
          Optional.of("Perusopetuksen oppimäärä"),
          Optional.empty,
          Optional.empty
        )
      )),
      oppiaineet = oppimaara.aineet.map(aine => fi.oph.suorituspalvelu.resource.ui.PerusopetuksenOppiaineUI(
        tunniste = aine.tunniste,
        koodi = aine.koodi.arvo,
        nimi = PerusopetuksenOppiaineNimi(
          if(aine.koodi.arvo=="A1") Optional.of("A1-kieli, saksa") else aine.nimi.fi.toJava,
          aine.nimi.sv.toJava,
          aine.nimi.en.toJava
        ),
        kieli = aine.kieli.map(_.arvo).toJava,
        arvosana = aine.arvosana.arvo,
        valinnainen = !aine.pakollinen
      )).toList.asJava,
      syotetty = oppimaara.syotetty
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.3"), "", Set(oppimaara), None, fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS, List.empty)), DUMMY_ORGANISAATIOPROVIDER, koodistoProvider).perusopetuksenOppimaarat)
  }

  @Test def testPerusopetuksenOppimaaraOppiaineeetNotIncludedWhenNotValmis(): Unit = {
    val oppimaara = PerusopetuksenOppimaara(
      tunniste = UUID.randomUUID(),
      versioTunniste = None,
      oppilaitos = Oppilaitos(Kielistetty(Some("Testikoulu"), None, None), "1.2.3.4.5"),
      luokka = Some("9A"),
      koskiTila = Koodi("lasna", "", None),
      supaTila = fi.oph.suorituspalvelu.business.SuoritusTila.KESKEN,
      suoritusKieli = Koodi("FI", "kieli", Some(1)),
      koulusivistyskieli = Set.empty,
      yksilollistaminen = None,
      aloitusPaivamaara = Some(LocalDate.parse("2020-01-01")),
      vahvistusPaivamaara = None,
      aineet = Set(
        PerusopetuksenOppiaine(
          tunniste = UUID.randomUUID(),
          nimi = Kielistetty(Some("Historia"), None, None),
          koodi = Koodi("HI", "koskioppiaineetyleissivistava", Some(1)),
          arvosana = Koodi("9", "arviointiasteikkoyleissivistava", Some(1)),
          kieli = None,
          pakollinen = true,
          yksilollistetty = None,
          rajattu = None
        )
      ),
      lahtokoulut = Set.empty,
      syotetty = false,
      vuosiluokkiinSitoutumatonOpetus = false
    )

    val result = EntityToUIConverter.getOppijanTiedot(
      None, None, None, "1.2.3", "2.3.4", None,
      Set(PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.3"), "", Set(oppimaara), None, fi.oph.suorituspalvelu.business.SuoritusTila.KESKEN, List.empty)),
      DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOODISTOPROVIDER
    )

    Assertions.assertEquals(1, result.perusopetuksenOppimaarat.size())
    Assertions.assertTrue(result.perusopetuksenOppimaarat.get(0).oppiaineet.isEmpty, "Oppiaineet should be empty when supaTila is not VALMIS")
  }

  @Test def testConvertOpiskeluoikeudet(): Unit = {
    val OPPIJANUMERO = "1.2.3"
    val ORGANISAATION_OID = "2.3.4"

    val virtaOpiskeluoikeus = KKOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      virtaTunniste = "",
      tyyppiKoodi = "1",
      koulutusKoodi = Some("671103"),
      alkuPvm = LocalDate.parse("2020-01-01"),
      loppuPvm = LocalDate.parse("2021-01-01"),
      virtaTila = Koodi("1", VirtaToSuoritusConverter.VIRTA_OO_TILA_KOODISTO, None), // aktiivinen
      supaTila = KKOpiskeluoikeusTila.VOIMASSA,
      myontaja = ORGANISAATION_OID,
      suoritukset = Set.empty
    )

    val ORGANISAATION_NIMI_FI = "Lapin ammattikorkeakoulu"
    val ORGANISAATION_NIMI_SV = "Lapin ammattikorkeakoulu"
    val ORGANISAATION_NIMI_EN = "Lapland University of Applied Sciences"
    val organisaatioProvider = new OrganisaatioProvider {
      override def haeKaikkiOrganisaatiot(): Map[String, Organisaatio] = {
        Map(ORGANISAATION_OID -> Organisaatio(ORGANISAATION_OID, OrganisaatioNimi(ORGANISAATION_NIMI_FI, ORGANISAATION_NIMI_SV, ORGANISAATION_NIMI_EN), None, Seq.empty, Seq.empty))
      }
    }

    val KOULUTUKSEN_NIMI_FI = "Sosiaali- ja terveysalan ammattikorkeakoulututkinto"
    val KOULUTUKSEN_NIMI_EN = "Bachelor of Health Care"
    val KOULUTUKSEN_TILA_FI = "aktiivinen"
    val KOULUTUKSEN_TILA_EN = "active"
    val koulutusProvider = new KoodistoProvider {
      override def haeKoodisto(koodisto: String): Map[String, fi.oph.suorituspalvelu.integration.client.Koodi] = Map(
        "671103" -> fi.oph.suorituspalvelu.integration.client.Koodi(
          "671103",
          Koodisto("koulutus"),
          List(
            KoodiMetadata("FI", KOULUTUKSEN_NIMI_FI),
            KoodiMetadata("EN", KOULUTUKSEN_NIMI_EN),
          )
        ),
        "1" -> fi.oph.suorituspalvelu.integration.client.Koodi(
          "1",
          Koodisto("virtaopintooikeudentila"),
          List(
            KoodiMetadata("FI", KOULUTUKSEN_TILA_FI),
            KoodiMetadata("EN", KOULUTUKSEN_TILA_EN),
          )
        )
      )
    }

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.UIOpiskeluoikeus(
      virtaOpiskeluoikeus.tunniste,
      UIOpiskeluoikeusNimi(
        Optional.of(KOULUTUKSEN_NIMI_FI),
        Optional.empty(),
        Optional.of(KOULUTUKSEN_NIMI_EN)
      ),
      UIOppilaitos(
        UIOppilaitosNimi(
          Optional.of(ORGANISAATION_NIMI_FI),
          Optional.of(ORGANISAATION_NIMI_SV),
          Optional.of(ORGANISAATION_NIMI_EN)
        ),
        organisaatioProvider.haeOrganisaationTiedot(ORGANISAATION_OID).get.oid
      ),
      virtaOpiskeluoikeus.alkuPvm,
      virtaOpiskeluoikeus.loppuPvm,
      OpiskeluoikeusTila.VOIMASSA,
      UIOpiskeluoikeusVirtaTila(
        Optional.of(KOULUTUKSEN_TILA_FI),
        Optional.empty(),
        Optional.of(KOULUTUKSEN_TILA_EN)
      )
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(virtaOpiskeluoikeus), organisaatioProvider, koulutusProvider).opiskeluoikeudet)
  }

  @Test def testConvertKKTutkinto(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val virtaTutkinto = KKTutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Some(Kielistetty(
        fi = Some("Sosiaali- ja terveysalan ammattikorkeakoulututkinto"),
        sv = Some("Bachelor of Health Care"),
        en = None)),
      komoTunniste = "532",
      opintoPisteet = 30.5,
      aloitusPvm = Some(LocalDate.parse("2020-01-01")),
      suoritusPvm = Some(LocalDate.parse("2021-01-01")),
      myontaja = "10108",
      kieli = Some("fi"),
      koulutusKoodi = Some("671103"),
      opiskeluoikeusAvain = Some("xxx002"),
      suoritukset = Seq.empty,
      avain = None
    )

    val organisaatioProvider = new OrganisaatioProvider {
      override def haeKaikkiOrganisaatiot(): Map[String, Organisaatio] =
        Map(virtaTutkinto.myontaja -> Organisaatio("1.2.3", OrganisaatioNimi("fi", "sv", "en"), None, Seq.empty, Seq.empty))
    }

    val opiskeluoikeus = KKOpiskeluoikeus(
      null,
      null,
      null,
      None,
      null,
      null,
      Koodi("1", "", None),
      KKOpiskeluoikeusTila.VOIMASSA,
      virtaTutkinto.myontaja,
      Set(virtaTutkinto)
    )

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.KKSuoritus(
      virtaTutkinto.tunniste,
      Optional.of(KKSuoritusNimi(
        virtaTutkinto.nimi.flatMap(_.fi).toJava,
        virtaTutkinto.nimi.flatMap(_.sv).toJava,
        virtaTutkinto.nimi.flatMap(_.en).toJava
      )),
      UIOppilaitos(
        UIOppilaitosNimi(
          Optional.of(organisaatioProvider.haeOrganisaationTiedot(virtaTutkinto.myontaja).get.nimi.fi),
          Optional.of(organisaatioProvider.haeOrganisaationTiedot(virtaTutkinto.myontaja).get.nimi.sv),
          Optional.of(organisaatioProvider.haeOrganisaationTiedot(virtaTutkinto.myontaja).get.nimi.en)
        ),
        "1.2.3"
      ),
      SuoritusTila.KESKEN,
      virtaTutkinto.aloitusPvm.toJava,
      virtaTutkinto.suoritusPvm.toJava,
      java.util.List.of()
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(opiskeluoikeus), organisaatioProvider, DUMMY_KOODISTOPROVIDER).kkTutkinnot)
  }

  @Test def testConvertYlioppilasTutkinto(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val yoTutkinto = YOTutkinto(
      tunniste = UUID.randomUUID(),
      suoritusKieli = Koodi("FI", "kieli", Some(1)),
      supaTila = fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      valmistumisPaiva = Some(LocalDate.parse("2013-06-01")),
      aineet = Set(Koe(
        tunniste = UUID.randomUUID(),
        koodi = Koodi("EA", "koskiyokokeet", Some(1)),
        tutkintoKerta = LocalDate.parse("2012-06-01"),
        arvosana = Koodi("M", "koskiyoarvosanat", Some(1)),
        pisteet = Some(236)
      ))
    )

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.YOTutkinto(
      tunniste = yoTutkinto.tunniste,
      nimi = YOTutkintoNimi(
        fi = Optional.of("Ylioppilastutkinto"),
        sv = Optional.of("Studentexamen"),
        en = Optional.of("Matriculation Examination")
      ),
      oppilaitos = YOOppilaitos(
        nimi = YOOppilaitosNimi(
          fi = Optional.of("Ylioppilastutkintolautakunta"),
          sv = Optional.of("Studentexamensnämnden"),
          en = Optional.of("The Matriculation Examination Board")
        ),
        oid = UIService.YTL_ORGANISAATIO_OID
      ),
      tila = SuoritusTila.valueOf(yoTutkinto.supaTila.toString()),
      valmistumispaiva = yoTutkinto.valmistumisPaiva.toJava,
      suorituskieli = "FI",
      yoKokeet = yoTutkinto.aineet.map(a => YOKoe(
        tunniste = a.tunniste,
        nimi = YOKoeNimi(
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        ),
        arvosana = a.arvosana.arvo,
        yhteispistemaara = a.pisteet.toJava,
        tutkintokerta = a.tutkintoKerta
      )).toList.asJava
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(YOOpiskeluoikeus(null, yoTutkinto)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOODISTOPROVIDER).yoTutkinnot)
  }

  @Test def testConvertLukionOppimaara(): Unit = {
    val lukionOppimaara = LukionOppimaara(
      tunniste = UUID.randomUUID(),
      oppilaitos = Oppilaitos(Kielistetty(Some("Ressun lukio"), Some("Ressun lukio sv"), Some("Ressun lukio en")), "1.2.246.562.10.12345678901"),
      koskiTila = Koodi("valmistunut", "", None),
      supaTila = fi.oph.suorituspalvelu.business.SuoritusTila.VALMIS,
      vahvistusPaivamaara = Some(LocalDate.parse("2024-06-01")),
      suoritusKieli = Some(Koodi("FI", "kieli", Some(1))),
      koulusivistyskieli = Set(Koodi("FI", "kieli", Some(1)))
    )

    Assertions.assertEquals(Optional.of(LukionOppimaaraUI(
      tunniste = lukionOppimaara.tunniste,
      nimi = LukionOppimaaraNimi(
        fi = Optional.of("Lukion oppimäärä"),
        sv = Optional.of("Gymnasiets lärokurs"),
        en = Optional.of("Upper secondary school syllabus")
      ),
      oppilaitos = YOOppilaitos(
        nimi = YOOppilaitosNimi(
          fi = lukionOppimaara.oppilaitos.nimi.fi.toJava,
          sv = lukionOppimaara.oppilaitos.nimi.sv.toJava,
          en = lukionOppimaara.oppilaitos.nimi.en.toJava
        ),
        oid = lukionOppimaara.oppilaitos.oid
      ),
      tila = SuoritusTila.VALMIS,
      aloituspaiva = Optional.empty(),
      valmistumispaiva = lukionOppimaara.vahvistusPaivamaara.toJava,
      suorituskieli = "FI"
    )), EntityToUIConverter.getOppijanTiedot(None, None, None, "1.2.3", "2.3.4", None, Set(GeneerinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Koodi("lukiokoulutus", "opiskeluoikeudentyyppi", None), "", Set(lukionOppimaara), None, List.empty)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOODISTOPROVIDER).lukionOppimaara)
  }

}
