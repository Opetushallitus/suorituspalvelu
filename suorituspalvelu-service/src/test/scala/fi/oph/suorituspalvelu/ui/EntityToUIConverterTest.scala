package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, AmmattiTutkinto, Arvosana, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, Opiskeluoikeus, Oppilaitos, Telma, Tuva, VapaaSivistystyo, VirtaOpiskeluoikeus, VirtaTutkinto}
import fi.oph.suorituspalvelu.configuration.{KoulutusProvider, OrganisaatioProvider}
import fi.oph.suorituspalvelu.integration.client.{Koodisto, KoodiMetadata, Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.parsing.virta.VirtaToSuoritusConverter
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.resource.ui.SuoritusTapa.NAYTTOTUTKINTO
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
    override def haeOrganisaationTiedot(koodiArvo: String): Organisaatio = Organisaatio("1.2.3", OrganisaatioNimi("", "", ""))
  }

  val DUMMY_KOULUTUSPROVIDER = new KoulutusProvider {
    override def haeKoulutus(koodiArvo: String): Option[fi.oph.suorituspalvelu.integration.client.Koodi] = None
  }

  @Test def testConvertAmmatillinenTutkinto(): Unit = {
    val tutkinto = AmmatillinenPerustutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkinnon nimi"), None, None),
      Koodi("351301", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en")), "1.2.246.562.10.41945921983"),
      Koodi("valmistunut", "", None),
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
      Tila.VALMIS,
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
    )), EntityToUIConverter.getAmmatillisetPerusTutkinnot(Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tutkinto), None))))
  }

  @Test def testConvertAmmatillinenTutkintoNaytto(): Unit = {
    // TODO: pitääkö suorituksen olla valmis jotta voidaan päätellä onko kyseessä näyttö vai ei?
    val tutkinto = AmmatillinenPerustutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkinnon nimi"), None, None),
      Koodi("351301", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en")), "1.2.246.562.10.41945921983"),
      Koodi("valmistunut", "", None),
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
      Tila.VALMIS,
      tutkinto.aloitusPaivamaara.toJava,
      tutkinto.vahvistusPaivamaara.toJava,
      tutkinto.suoritusKieli.arvo,
      Optional.empty(),
      java.util.List.of(),
      java.util.List.of(),
      Optional.of(NAYTTOTUTKINTO) // Suorituksen osilla ei arvosanoja => näyttötutkinto
    )), EntityToUIConverter.getOppijanTiedot("1.2.3", Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tutkinto), None)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOULUTUSPROVIDER).get.ammatillisetPerusTutkinnot)
  }

  @Test def testConvertAmmatillinenTutkintoEnnenReformia(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val tutkinto = AmmatillinenPerustutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkinnon nimi"), None, None),
      Koodi("351301", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Stadin ammattiopisto"), Some("Stadin ammattiopisto sv"), Some("Stadin ammattiopisto en")), "1.2.246.562.10.41945921983"),
      Koodi("valmistunut", "", None),
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
      Tila.VALMIS,
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
    )), EntityToUIConverter.getOppijanTiedot("1.2.3", Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tutkinto), None)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOULUTUSPROVIDER).get.ammatillisetPerusTutkinnot)
  }

  @Test def testConvertAmmattiTutkinto(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val tutkinto = AmmattiTutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("Hieronnan ammattitutkinto"), None, None),
      Koodi("351301", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Pirkanmaan urheiluhierojakoulu"), Some("Pirkanmaan urheiluhierojakoulu sv"), Some("Pirkanmaan urheiluhierojakoulu en")), "1.2.246.562.10.41945921983"),
      Koodi("valmistunut", "", None),
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
      Tila.VALMIS,
      tutkinto.aloitusPaivamaara.toJava,
      tutkinto.vahvistusPaivamaara.toJava,
      tutkinto.suoritusKieli.arvo,
    )), EntityToUIConverter.getOppijanTiedot("1.2.3", Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tutkinto), None)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOULUTUSPROVIDER).get.ammattitutkinnot)
  }

  @Test def testConvertErikoisAmmattiTutkinto(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val tutkinto = ErikoisAmmattiTutkinto(
      UUID.randomUUID(),
      Kielistetty(Some("Talous- ja henkilöstöhallinnon erikoisammattitutkinto"), None, None),
      Koodi("437109", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("HAUS kehittämiskeskus Oy"), Some("HAUS kehittämiskeskus Oy sv"), Some("HAUS kehittämiskeskus Oy en")), "1.2.246.562.10.54019331674"),
      Koodi("valmistunut", "", None),
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
      Tila.VALMIS,
      tutkinto.aloitusPaivamaara.toJava,
      tutkinto.vahvistusPaivamaara.toJava,
      tutkinto.suoritusKieli.arvo
    )), EntityToUIConverter.getOppijanTiedot("1.2.3", Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tutkinto), None)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOULUTUSPROVIDER).get.erikoisammattitutkinnot)
  }
  
  @Test def testConvertTelma(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val telma = Telma(
      UUID.randomUUID(),
      Kielistetty(Some("Työhön ja itsenäiseen elämään valmentava koulutus (TELMA)"), None, None),
      Koodi("999903", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Savon ammattiopisto"), Some("Savon ammattiopisto sv"), Some("Savon ammattiopisto en")), "1.2.246.562.10.11168857016"),
      Koodi("valmistunut", "", None),
      Some(LocalDate.parse("2020-01-01")),
      Some(LocalDate.parse("2020-01-01")),
      Koodi("FI", "kieli", Some(1))
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
      Tila.VALMIS,
      telma.aloitusPaivamaara.toJava,
      telma.vahvistusPaivamaara.toJava,
      telma.suoritusKieli.arvo
    )), EntityToUIConverter.getOppijanTiedot("1.2.3", Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(telma), None)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOULUTUSPROVIDER).get.telmat)
  }

  @Test def testConvertTuva(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val tuva = Tuva(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkintokoulutukseen valmentava koulutus (TUVA)"), None, None),
      Koodi("999907", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Savon ammattiopisto"), Some("Savon ammattiopisto sv"), Some("Savon ammattiopisto en")), "1.2.246.562.10.11168857016"),
      Koodi("valmistunut", "", None),
      Some(LocalDate.parse("2020-01-01")),
      Some(LocalDate.parse("2020-01-01")),
      Some(Laajuus(11, Koodi("8", "opintojenlaajuusyksikko", Some(1)), None, Some(Kielistetty(Some("vk"), None, None))))
    )

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.Tuva(
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
      Tila.VALMIS,
      tuva.aloitusPaivamaara.toJava,
      tuva.vahvistusPaivamaara.toJava,
      tuva.laajuus.map(l => TuvaLaajuus(l.arvo, TuvaLaajuusYksikko(
        l.lyhytNimi.get.fi.toJava,
        l.lyhytNimi.get.sv.toJava,
        l.lyhytNimi.get.en.toJava
      ))).toJava,
    )), EntityToUIConverter.getOppijanTiedot("1.2.3", Set(AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Oppilaitos(Kielistetty(None, None, None), ""), Set(tuva), None)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOULUTUSPROVIDER).get.tuvat)
  }

  @Test def testConvertVapaaSivistystyoKoulutus(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val vst = VapaaSivistystyo(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkintokoulutukseen valmentava koulutus (TUVA)"), None, None),
      Koodi("999907", "koulutus", Some(12)),
      Oppilaitos(Kielistetty(Some("Savon ammattiopisto"), Some("Savon ammattiopisto sv"), Some("Savon ammattiopisto en")), "1.2.246.562.10.11168857016"),
      Koodi("valmistunut", "", None),
      Some(LocalDate.parse("2020-01-01")),
      Some(LocalDate.parse("2020-01-01")),
      Some(Laajuus(11, Koodi("8", "opintojenlaajuusyksikko", Some(1)), None, Some(Kielistetty(Some("op"), None, None)))),
      Koodi("FI", "kieli", Some(1))
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
      Tila.VALMIS,
      vst.aloitusPaivamaara.toJava,
      vst.vahvistusPaivamaara.toJava,
      vst.suoritusKieli.arvo,
      vst.laajuus.map(l => VapaaSivistystyoLaajuus(l.arvo, VapaaSivistystyoLaajuusYksikko(
        l.lyhytNimi.get.fi.toJava,
        l.lyhytNimi.get.sv.toJava,
        l.lyhytNimi.get.en.toJava
      ))).toJava,
    )), EntityToUIConverter.getOppijanTiedot("1.2.3", Set(GeneerinenOpiskeluoikeus(UUID.randomUUID(), "1.2.3", Koodi("", "", None), "", Set(vst), None)), DUMMY_ORGANISAATIOPROVIDER, DUMMY_KOULUTUSPROVIDER).get.vapaaSivistystyoKoulutukset)
  }

  @Test def testConvertOpiskeluoikeudet(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val virtaOpiskeluoikeus = VirtaOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      virtaTunniste = "",
      koulutusKoodi = "671103",
      alkuPvm = LocalDate.parse("2020-01-01"),
      loppuPvm = LocalDate.parse("2021-01-01"),
      tila = Koodi("1", VirtaToSuoritusConverter.VIRTA_OO_TILA_KOODISTO, None), // aktiivinen
      myontaja = "",
      suoritukset = Set.empty
    )

    val ORGANISAATION_NIMI_FI = "Lapin ammattikorkeakoulu"
    val ORGANISAATION_NIMI_SV = "Lapin ammattikorkeakoulu"
    val ORGANISAATION_NIMI_EN = "Lapland University of Applied Sciences"
    val organisaatioProvider = new OrganisaatioProvider {
      override def haeOrganisaationTiedot(koodiArvo: String): Organisaatio = {
        Organisaatio("1.2.3", OrganisaatioNimi(ORGANISAATION_NIMI_FI, ORGANISAATION_NIMI_SV, ORGANISAATION_NIMI_EN))
      }
    }

    val KOULUTUKSEN_NIMI_FI = "Sosiaali- ja terveysalan ammattikorkeakoulututkinto"
    val KOULUTUKSEN_NIMI_EN = "Bachelor of Health Care"
    val koulutusProvider = new KoulutusProvider {
      override def haeKoulutus(koodiArvo: String): Option[fi.oph.suorituspalvelu.integration.client.Koodi] = Some(fi.oph.suorituspalvelu.integration.client.Koodi(
        "671103",
        Koodisto("koulutus"),
        List(
          KoodiMetadata("FI", KOULUTUKSEN_NIMI_FI),
          KoodiMetadata("EN", KOULUTUKSEN_NIMI_EN),
        )
      ))
    }

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.UIOpiskeluoikeus(
      virtaOpiskeluoikeus.tunniste,
      UIOpiskeluoikeusNimi(
        Optional.of(KOULUTUKSEN_NIMI_FI),
        Optional.empty(),
        Optional.of(KOULUTUKSEN_NIMI_EN)
      ),
      OOOppilaitos(
        OOOppilaitosNimi(
          Optional.of(ORGANISAATION_NIMI_FI),
          Optional.of(ORGANISAATION_NIMI_SV),
          Optional.of(ORGANISAATION_NIMI_EN)
        ),
        organisaatioProvider.haeOrganisaationTiedot("").oid
      ),
      virtaOpiskeluoikeus.alkuPvm,
      virtaOpiskeluoikeus.loppuPvm,
      Tila.KESKEN
    )), EntityToUIConverter.getOppijanTiedot("1.2.3", Set(virtaOpiskeluoikeus), organisaatioProvider, koulutusProvider).get.opiskeluoikeudet)
  }

  @Test def testConvertKKTutkinto(): Unit = {
    val OPPIJANUMERO = "1.2.3"

    val virtaTutkinto = VirtaTutkinto(
      tunniste = UUID.randomUUID(),
      nimiFi = Some("Sosiaali- ja terveysalan ammattikorkeakoulututkinto"),
      nimiSv = Some("Bachelor of Health Care"),
      nimiEn = None,
      komoTunniste = "532",
      opintoPisteet = 30.5,
      aloitusPvm = LocalDate.parse("2020-01-01"),
      suoritusPvm = LocalDate.parse("2021-01-01"),
      myontaja = "10108",
      kieli = "fi",
      koulutusKoodi = "671103",
      opiskeluoikeusAvain = "xxx002"
    )

    val organisaatioProvider = new OrganisaatioProvider {
      override def haeOrganisaationTiedot(koodiArvo: String): Organisaatio = {
        if(koodiArvo == virtaTutkinto.myontaja)
          Organisaatio("1.2.3", OrganisaatioNimi("fi", "sv", "en"))
        else
          throw new RuntimeException()
      }
    }

    Assertions.assertEquals(java.util.List.of(fi.oph.suorituspalvelu.resource.ui.KKSuoritus(
      virtaTutkinto.tunniste,
      KKSuoritusNimi(
        virtaTutkinto.nimiFi.toJava,
        virtaTutkinto.nimiSv.toJava,
        virtaTutkinto.nimiEn.toJava
      ),
      KKOppilaitos(
        KKOppilaitosNimi(
          Optional.of(organisaatioProvider.haeOrganisaationTiedot(virtaTutkinto.myontaja).nimi.fi),
          Optional.of(organisaatioProvider.haeOrganisaationTiedot(virtaTutkinto.myontaja).nimi.sv),
          Optional.of(organisaatioProvider.haeOrganisaationTiedot(virtaTutkinto.myontaja).nimi.en)
        ),
        organisaatioProvider.haeOrganisaationTiedot(virtaTutkinto.myontaja).oid
      ),
      Tila.VALMIS,
      Optional.of(virtaTutkinto.aloitusPvm),
      Optional.of(virtaTutkinto.suoritusPvm)
    )), EntityToUIConverter.getOppijanTiedot("1.2.3", Set(VirtaOpiskeluoikeus(null, null, null, null, null, Koodi("1", "", None), virtaTutkinto.myontaja, Set(virtaTutkinto))), organisaatioProvider, DUMMY_KOULUTUSPROVIDER).get.kkTutkinnot)
  }

}
