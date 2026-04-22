package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.business.{AmmatillinenPerustutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, Koodi, Oppilaitos}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiKoodi, KoskiKoulutusModuuli, KoskiOpiskeluoikeus, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila, KoskiSuoritus, KoskiSuoritusTyyppi, KoskiToSuoritusConverter, KoskiVahvistus}

import java.time.LocalDate
import java.util.UUID

object TestDataUtil {

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
  }
