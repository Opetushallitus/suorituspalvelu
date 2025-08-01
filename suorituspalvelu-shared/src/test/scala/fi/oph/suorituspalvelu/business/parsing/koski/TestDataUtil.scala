package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.{AmmatillinenPerustutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, Koodi, Oppilaitos}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty

import java.time.LocalDate

object TestDataUtil {

  def getTestKoodi(arvo: String = "arvo", koodisto: String = "koodisto", versio: Option[Int] = Some(1)): Koodi =
    Koodi(arvo, koodisto, versio)

  def getTestAmmatillisenTutkinnonOsa(nimi: Kielistetty = Kielistetty(Some("tutkinnonOsa"), None, None),
                                      koodi: Koodi = getTestKoodi(),
                                      yto: Boolean = false,
                                      arvosana: Option[Koodi] = Some(getTestKoodi()),
                                      laajuus: Option[Int] = Some(2),
                                      laajuusKoodi: Option[Koodi] = Some(getTestKoodi()),
                                      osaAlueet: Set[AmmatillisenTutkinnonOsaAlue] = Set.empty) = {
    AmmatillisenTutkinnonOsa(nimi, koodi, yto, arvosana, laajuus, laajuusKoodi, osaAlueet)
  }

  def getTestAmmatillinenTutkinto(nimi: Kielistetty = Kielistetty(Some("tutkinnonNimi"), None, None),
                                  koodi: Koodi = getTestKoodi(),
                                  oppilaitos: Oppilaitos = Oppilaitos(None, None, None, "1.2.246.562.10.95136889433"),
                                  tila: Koodi = getTestKoodi(),
                                  vahvistusPaivamaara: Option[LocalDate] = Some(LocalDate.parse("2025-01-02")),
                                  keskiarvo: Option[BigDecimal] = Some(BigDecimal(3.5)),
                                  suoritustapa: Koodi = getTestKoodi(),
                                  suoritusKieli: Koodi = getTestKoodi(),
                                  osat: Set[AmmatillisenTutkinnonOsa] = Set.empty): AmmatillinenPerustutkinto =
    AmmatillinenPerustutkinto(nimi, koodi, Oppilaitos(Some(""), Some(""), Some(""), ""), tila, vahvistusPaivamaara, keskiarvo, suoritustapa, suoritusKieli, osat)
  }
