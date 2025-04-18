package fi.oph.suorituspalvelu.business

import java.util.UUID
import java.time.{Instant, LocalDate}

enum Tietolahde:
  case KOSKI, YTR, VIRTA, VIRKAILIJA

sealed trait Suoritus {
  def tyyppi: String
  def osaSuoritukset: Seq[Suoritus]
}

// Suorituksella tarkoituksella ei tunnistetta, näin voidaan käyttää entiteettien luomisessa
case class GenericSuoritus(tyyppi: String, osaSuoritukset: Seq[Suoritus]) extends Suoritus

case class Arvosana(arvosana: String, koodi: String)

case class AmmatillinenTutkinto(nimi: String, koodi: String, vahvistusPaivamaara: Option[LocalDate], osat: Seq[AmmatillisenTutkinnonOsa]) extends Suoritus {

  // TODO nämä lähtee pois
  override def tyyppi: String = ""
  override def osaSuoritukset: Seq[Suoritus] = Seq.empty
}

case class AmmatillisenTutkinnonOsa(nimi: String, koodi: String, arvosana: Option[String])


case class PerusopetuksenOppimaara(vahvistusPaivamaara: Option[LocalDate], aineet: Seq[PerusopetuksenOppiaine]) extends Suoritus {
  // TODO nämä lähtee pois
  override def tyyppi: String = ""
  override def osaSuoritukset: Seq[Suoritus] = Seq.empty
}

case class PerusopetuksenOppiaine(nimi: String, koodi: String, arvosana: String)


case class VersioEntiteetti(tunniste: UUID, oppijaNumero: String, alku: Instant, loppu: Option[Instant], tietolahde: Tietolahde)