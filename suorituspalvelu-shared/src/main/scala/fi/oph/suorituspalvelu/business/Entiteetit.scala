package fi.oph.suorituspalvelu.business

import java.util.UUID
import java.time.Instant

enum Tietolahde:
  case KOSKI, YTR, VIRTA, VIRKAILIJA

trait Suoritus {
  def tyyppi: String
  def osaSuoritukset: Seq[Suoritus]
}

// Suorituksella tarkoituksella ei tunnistetta, näin voidaan käyttää entiteettien luomisessa
case class GenericSuoritus(tyyppi: String, osaSuoritukset: Seq[Suoritus]) extends Suoritus

case class TutkinnonOsa(tyyppi: String) extends Suoritus {
  def osaSuoritukset: Seq[Suoritus] = Seq.empty
}

case class VersioEntiteetti(tunniste: UUID, oppijaNumero: String, alku: Instant, loppu: Option[Instant], tietolahde: Tietolahde)