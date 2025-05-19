package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.{AmmatillinenTutkinto, AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, Koodi}

object TestDataUtil {

  def getTestKoodi(arvo: String = "arvo", koodisto: String = "koodisto", versio: Int = 1): Koodi =
    Koodi(arvo, koodisto, versio)

  def getAmmatillisenTutkinnonOsa(nimi: String = "tutkinnonOsa",
                                  koodi: Koodi = getTestKoodi(),
                                  yto: Boolean = false,
                                  arvosana: Option[Koodi] = Some(getTestKoodi()),
                                  laajuus: Option[Int] = Some(2),
                                  laajuusKoodi: Option[Koodi] = Some(getTestKoodi()),
                                  osaAlueet: Set[AmmatillisenTutkinnonOsaAlue] = Set.empty) = {
    AmmatillisenTutkinnonOsa(nimi, koodi, yto, arvosana, laajuus, laajuusKoodi, osaAlueet)
  }

  def getAmmatillinenTutkinto(osat: Set[AmmatillisenTutkinnonOsa] = Set.empty): AmmatillinenTutkinto =
    AmmatillinenTutkinto("tutkinnonNimi", getTestKoodi(), getTestKoodi(), None, None, getTestKoodi(), osat)

}
