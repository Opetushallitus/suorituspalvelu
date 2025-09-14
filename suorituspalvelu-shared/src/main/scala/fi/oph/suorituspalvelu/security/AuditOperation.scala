package fi.oph.suorituspalvelu.security

import fi.vm.sade.auditlog.Operation

trait AuditOperation(val name: String) extends Operation

object AuditOperation {
  case object Login extends AuditOperation("KIRJAUTUMINEN")

  case object PaivitaVirtaTiedot extends AuditOperation("VIRTATIETOJEN PÄIVITYS")

  case object HaeYoTaiAmmatillinenTutkintoTiedot extends AuditOperation("HAE YO TAI AMMATILLINEN TUTKINNOT")

  case object HaeOppijaTiedot extends AuditOperation("HAE OPPIJATIEDOT")

  case object HaeOppijatUI extends AuditOperation("HAE OPPIJAT UI")

  case object HaeOppijaTiedotUI extends AuditOperation("HAE OPPIJATIEDOT UI")

  case object HaeKoskiTaiYTRMuuttuneet extends AuditOperation("HAE KOSKI TAI YTR MUUTTUNEET HENKILÖT")

  case object PaivitaKoskiTiedotHenkiloille extends AuditOperation("KOSKITIETOJEN PÄIVITYS HENKILÖILLE")

  case object PaivitaKoskiTiedotHaunHakijoille extends AuditOperation("KOSKITIETOJEN PÄIVITYS HAUN HAKIJOILLE")

  case object PaivitaVirtaTiedotHaunHakijoille extends AuditOperation("VIRTATIETOJEN PÄIVITYS HAUN HAKIJOILLE")

  case object PaivitaYtrTiedotHaunHakijoille extends AuditOperation("YLIOPPILASTUTKINTOREKISTERITIETOJEN PÄIVITYS HAUN HAKIJOILLE")

  case object PaivitaYtrTiedotHenkiloille extends AuditOperation("YLIOPPILASTUTKINTOREKISTERITIETOJEN PÄIVITYS HENKILÖILLE")

  case object TallennaPeruskoulunOppimaaranSuoritus extends AuditOperation("TALLENNA PERUSKOULUN OPPIMAARAN SUORITUS UI")

  case object PoistaPeruskoulunOppimaaranSuoritus extends AuditOperation("POISTA PERUSKOULUN OPPIMAARAN SUORITUS UI")

}
