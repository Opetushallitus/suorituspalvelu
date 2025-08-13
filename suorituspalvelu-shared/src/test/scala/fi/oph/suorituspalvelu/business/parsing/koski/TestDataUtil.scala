package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.{AmmatillinenPerustutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, Koodi, Oppilaitos}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty

import java.time.LocalDate
import java.util.UUID

object TestDataUtil {

  def getTestKoodi(arvo: String = "arvo", koodisto: String = "koodisto", versio: Option[Int] = Some(1)): Koodi =
    Koodi(arvo, koodisto, versio)

  def getTestAmmatillinenTutkinto(nimi: Kielistetty = Kielistetty(Some("tutkinnonNimi"), None, None),
                                  koodi: Koodi = getTestKoodi(),
                                  oppilaitos: Oppilaitos = Oppilaitos(Kielistetty(None, None, None), "1.2.246.562.10.95136889433"),
                                  tila: Koodi = getTestKoodi(),
                                  aloitusPaivamaara: Option[LocalDate] = Some(LocalDate.parse("2025-01-02")),
                                  vahvistusPaivamaara: Option[LocalDate] = Some(LocalDate.parse("2025-01-02")),
                                  keskiarvo: Option[BigDecimal] = Some(BigDecimal(3.5)),
                                  suoritustapa: Koodi = getTestKoodi(),
                                  suoritusKieli: Koodi = getTestKoodi(),
                                  osat: Set[AmmatillisenTutkinnonOsa] = Set.empty): AmmatillinenPerustutkinto =
    AmmatillinenPerustutkinto(UUID.randomUUID(), nimi, koodi, Oppilaitos(Kielistetty(Some(""), Some(""), Some("")), ""), tila, aloitusPaivamaara, vahvistusPaivamaara, keskiarvo, suoritustapa, suoritusKieli, osat)
  }
