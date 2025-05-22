package fi.oph.suorituspalvelu.security

import fi.vm.sade.auditlog.Operation

trait AuditOperation(val name: String) extends Operation

object AuditOperation {
  case object Login extends AuditOperation("KIRJAUTUMINEN")

  case object PaivitaVirtaTiedot extends AuditOperation("VIRTATIETOJEN PÄIVITYS")
}