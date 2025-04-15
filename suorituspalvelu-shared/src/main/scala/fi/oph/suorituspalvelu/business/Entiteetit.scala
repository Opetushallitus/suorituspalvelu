package fi.oph.suorituspalvelu.business

import java.util.UUID
import java.time.Instant

enum Tietolahde:
  case KOSKI, YTR, VIRTA, VIRKAILIJA

// Suorituksella tarkoituksella ei tunnistetta, näin voidaan käyttää entiteettien luomisessa
case class Suoritus(koodiArvo: String, osaSuoritukset: Seq[Suoritus])

case class VersioEntiteetti(tunniste: UUID, oppijaNumero: String, alku: Instant, loppu: Option[Instant], tietolahde: Tietolahde)

// käsin syötetty suoritus voidaan myöhemmin muuttaa/poistaa jne.
// matchätään alkuperäiseen suoritukseen koodiArvon tms. avulla, UUID-tunniste on näet versiokohtainen
case class SuoritusEntiteetti(tunniste: UUID, koodiArvo: String, osaSuoritukset: Seq[SuoritusEntiteetti])