package fi.oph.suorituspalvelu.business

import fi.oph.suorituspalvelu.business.{AmmatillinenPerustutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, Koodi, Oppilaitos}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiKoodi, KoskiKoulutusModuuli, KoskiOpiskeluoikeus, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila, KoskiSuoritus, KoskiSuoritusTyyppi, KoskiToSuoritusConverter, KoskiVahvistus}

import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

object TestDataUtil {

  private val oidCounter = new AtomicInteger(1)

  private def nextOid(): String = s"1.2.246.562.15.${oidCounter.getAndIncrement()}"

  def getTestKoodi(arvo: String = "arvo", koodisto: String = "koodisto", versio: Option[Int] = Some(1)): Koodi =
    Koodi(arvo, koodisto, versio)

  def getTestAmmatillinenTutkinto(nimi: Kielistetty = Kielistetty(Some("tutkinnonNimi"), None, None),
                                  koodi: Koodi = getTestKoodi(),
                                  oppilaitos: Oppilaitos = Oppilaitos(Kielistetty(None, None, None), "1.2.246.562.10.95136889433"),
                                  tila: Koodi = Koodi("lasna", "koskiopiskeluoikeudentila", Some(1)),
                                  aloitusPaivamaara: Option[LocalDate] = Some(LocalDate.parse("2025-01-02")),
                                  vahvistusPaivamaara: Option[LocalDate] = Some(LocalDate.parse("2025-01-02")),
                                  keskiarvo: Option[BigDecimal] = Some(BigDecimal(3.5)),
                                  suoritustapa: Koodi = getTestKoodi(),
                                  suoritusKieli: Koodi = getTestKoodi(),
                                  osat: Set[AmmatillisenTutkinnonOsa] = Set.empty): AmmatillinenPerustutkinto =
    AmmatillinenPerustutkinto(UUID.randomUUID(), nimi, koodi, Oppilaitos(Kielistetty(Some(""), Some(""), Some("")), ""), tila, KoskiToSuoritusConverter.convertKoskiTila(tila.arvo), aloitusPaivamaara, vahvistusPaivamaara, keskiarvo, suoritustapa, suoritusKieli, osat)

  def mkKoodi(koodiarvo: String, koodistoUri: String = "koodisto"): KoskiKoodi =
    KoskiKoodi(koodiarvo, koodistoUri, None, Kielistetty(None, None, None), None)

  def mkJakso(alku: String, tilaKoodiarvo: String): KoskiOpiskeluoikeusJakso =
    KoskiOpiskeluoikeusJakso(LocalDate.parse(alku), mkKoodi(tilaKoodiarvo, "koskiopiskeluoikeudentila"))

  def mkOpiskeluoikeusWithTila(jaksot: KoskiOpiskeluoikeusJakso*): KoskiOpiskeluoikeus =
    KoskiOpiskeluoikeus("1.2.3", None, None, Some(KoskiOpiskeluoikeusTila(jaksot.toList)), None, None, None)

  def mkVuosiluokkaSuoritus(luokkaAste: String, alkamispaiva: Option[String] = None, vahvistuspaiva: Option[String] = None, luokka: Option[String] = Some("9A")): KoskiSuoritus =
    KoskiSuoritus(
      tyyppi = KoskiSuoritusTyyppi("perusopetuksenvuosiluokka", "suorituksentyyppi", Kielistetty(None, None, None)),
      koulutusmoduuli = Some(KoskiKoulutusModuuli(
        tunniste = Some(mkKoodi(luokkaAste, "perusopetuksenluokkaaste")),
        koulutustyyppi = None, laajuus = None, kieli = None, pakollinen = None, osaAlue = None, ryhmä = None)),
      suorituskieli = None,
      koulusivistyskieli = None,
      alkamispäivä = alkamispaiva,
      vahvistus = vahvistuspaiva.map(p => KoskiVahvistus(p)),
      osasuoritukset = Some(Set.empty),
      arviointi = None,
      keskiarvo = None,
      korotettuKeskiarvo = None,
      korotettuOpiskeluoikeusOid = None,
      suoritustapa = None,
      luokka = luokka,
      jääLuokalle = None
    )

  def getTestTuva(laajuusArvo: Option[BigDecimal] = Some(BigDecimal(10)),
                  aloitusPaivamaara: LocalDate = LocalDate.parse("2021-01-01"),
                  vahvistusPaivamaara: Option[LocalDate] = Some(LocalDate.parse("2022-05-15")),
                  suoritusVuosi: Int = 2022,
                  oppilaitos: Oppilaitos = Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
                  koskiTila: Koodi = Koodi("valmistunut", "suorituksentila", Some(1)),
                  supaTila: SuoritusTila = SuoritusTila.VALMIS): Tuva =
    Tuva(
      UUID.randomUUID(),
      Kielistetty(Some("Tutkintokoulutukseen valmentava koulutus"), None, None),
      Koodi("999904", "koulutus", Some(1)),
      oppilaitos,
      koskiTila,
      supaTila,
      aloitusPaivamaara,
      vahvistusPaivamaara,
      suoritusVuosi,
      laajuusArvo.map(arvo => Laajuus(arvo, Koodi("4", "opintojenlaajusyksikkö", Some(1)), None, None)),
      List(Lahtokoulu(aloitusPaivamaara, vahvistusPaivamaara, oppilaitos.oid, Some(suoritusVuosi), "tuva", Some(supaTila), None, LahtokouluTyyppi.TUVA))
    )

  def getTestTelma(laajuusArvo: Option[BigDecimal] = Some(BigDecimal(13)),
                   aloitusPaivamaara: LocalDate = LocalDate.parse("2021-01-01"),
                   vahvistusPaivamaara: Option[LocalDate] = Some(LocalDate.parse("2022-05-15")),
                   suoritusVuosi: Int = 2022,
                   oppilaitos: Oppilaitos = Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
                   koskiTila: Koodi = Koodi("valmistunut", "suorituksentila", Some(1)),
                   supaTila: SuoritusTila = SuoritusTila.VALMIS,
                   suoritusKieli: Koodi = Koodi("FI", "kieli", Some(1))): Telma =
    Telma(
      UUID.randomUUID(),
      Kielistetty(Some("Työhön ja itsenäiseen elämään valmentava koulutus"), None, None),
      Koodi("999903", "koulutus", Some(1)),
      oppilaitos,
      koskiTila,
      supaTila,
      aloitusPaivamaara,
      vahvistusPaivamaara,
      suoritusVuosi,
      suoritusKieli,
      laajuusArvo.map(arvo => Laajuus(arvo, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
      List(Lahtokoulu(aloitusPaivamaara, vahvistusPaivamaara, oppilaitos.oid, Some(suoritusVuosi), LahtokouluTyyppi.TELMA.defaultLuokka.get, Some(supaTila), None, LahtokouluTyyppi.TELMA))
    )

  def getTestVapaaSivistystyo(laajuusArvo: Option[BigDecimal] = Some(BigDecimal(14)),
                              aloitusPaivamaara: LocalDate = LocalDate.parse("2021-01-01"),
                              vahvistusPaivamaara: Option[LocalDate] = Some(LocalDate.parse("2022-05-15")),
                              suoritusVuosi: Int = 2022,
                              oppilaitos: Oppilaitos = Oppilaitos(Kielistetty(Some("Testioppilaitos"), None, None), "1.2.3.4"),
                              koskiTila: Koodi = Koodi("valmistunut", "suorituksentila", Some(1)),
                              supaTila: SuoritusTila = SuoritusTila.VALMIS,
                              suoritusKieli: Koodi = Koodi("FI", "kieli", Some(1))): VapaaSivistystyo =
    VapaaSivistystyo(
      UUID.randomUUID(),
      Kielistetty(Some("Kansanopiston opistovuosi"), None, None),
      Koodi("999901", "koulutus", Some(1)),
      oppilaitos,
      koskiTila,
      supaTila,
      aloitusPaivamaara,
      vahvistusPaivamaara,
      suoritusVuosi,
      laajuusArvo.map(arvo => Laajuus(arvo, Koodi("6", "opintojenlaajusyksikkö", Some(1)), None, None)),
      suoritusKieli,
      List(Lahtokoulu(aloitusPaivamaara, vahvistusPaivamaara, oppilaitos.oid, Some(suoritusVuosi), LahtokouluTyyppi.VAPAA_SIVISTYSTYO.defaultLuokka.get, Some(supaTila), None, LahtokouluTyyppi.VAPAA_SIVISTYSTYO))
    )

  def getTestGeneerinenOpiskeluoikeus(suoritukset: Set[Suoritus],
                                      tyyppi: Koodi = Koodi("", "", None),
                                      tila: Option[KoskiOpiskeluoikeusTila] = None,
                                      jaksot: List[OpiskeluoikeusJakso] = List.empty): GeneerinenOpiskeluoikeus =
    GeneerinenOpiskeluoikeus(UUID.randomUUID(), nextOid(), tyyppi, nextOid(), suoritukset, tila, jaksot)

  def getTestAmmatillinenOpiskeluoikeus(suoritukset: Set[Suoritus],
                                        oppilaitos: Oppilaitos = Oppilaitos(Kielistetty(None, None, None), ""),
                                        tila: Option[KoskiOpiskeluoikeusTila] = None,
                                        jaksot: List[OpiskeluoikeusJakso] = List.empty): AmmatillinenOpiskeluoikeus =
    AmmatillinenOpiskeluoikeus(UUID.randomUUID(), nextOid(), oppilaitos, suoritukset, tila, jaksot)
  }
