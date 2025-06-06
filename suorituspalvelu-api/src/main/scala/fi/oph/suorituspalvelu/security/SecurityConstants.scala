package fi.oph.suorituspalvelu.security

object SecurityConstants {

  final val OPH_ORGANISAATIO_OID = "1.2.246.562.10.00000000001"

  final val SECURITY_ROOLI_PREFIX = "ROLE_"
  final val SECURITY_ROOLI_REKISTERINPITAJA = "APP_SUORITUSREKISTERI_CRUD"
  final val SECURITY_ROOLI_REKISTERINPITAJA_FULL = SECURITY_ROOLI_PREFIX + SECURITY_ROOLI_REKISTERINPITAJA + "_" + OPH_ORGANISAATIO_OID
}