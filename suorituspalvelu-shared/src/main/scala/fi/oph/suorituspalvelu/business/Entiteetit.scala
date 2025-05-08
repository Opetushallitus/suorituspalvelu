package fi.oph.suorituspalvelu.business

import java.util.UUID
import java.time.{Instant, LocalDate}

enum Tietolahde:
  case KOSKI, YTR, VIRTA, VIRKAILIJA

sealed trait Suoritus

case class Koodi(arvo: String, koodisto: String, versio: Int)

case class Arvosana(arvosana: String, koodi: String)

case class AmmatillinenTutkinto(nimi: String, tyyppi: Koodi, tila: Koodi, vahvistusPaivamaara: Option[LocalDate], keskiarvo: Option[BigDecimal], suoritustapa: Koodi, osat: Set[AmmatillisenTutkinnonOsa]) extends Suoritus

case class AmmatillisenTutkinnonOsaAlue(nimi: String, koodi: Koodi, arvosana: Option[Koodi], laajuus: Option[Int], laajuusKoodi: Option[Koodi])

case class AmmatillisenTutkinnonOsa(nimi: String, koodi: Koodi, yto: Boolean, arvosana: Option[Koodi], laajuus: Option[Int], laajuusKoodi: Option[Koodi], osaAlueet: Set[AmmatillisenTutkinnonOsaAlue])

case class Tuva(koodi: String, vahvistusPaivamaara: Option[LocalDate]) extends Suoritus

case class Telma(koodi: String) extends Suoritus

case class NuortenPerusopetuksenOppiaineenOppimaara(nimi: String, koodi: String, arvosana: String) extends Suoritus

case class PerusopetuksenOppimaara(vahvistusPaivamaara: Option[LocalDate], aineet: Set[PerusopetuksenOppiaine]) extends Suoritus

case class PerusopetuksenOppiaine(nimi: String, koodi: String, arvosana: String)

case class PerusopetuksenVuosiluokka(nimi: String, koodi: String) extends Suoritus

case class VersioEntiteetti(tunniste: UUID, oppijaNumero: String, alku: Instant, loppu: Option[Instant], tietolahde: Tietolahde)