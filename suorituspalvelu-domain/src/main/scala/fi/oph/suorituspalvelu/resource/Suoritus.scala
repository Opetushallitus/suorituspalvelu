package fi.oph.suorituspalvelu.resource

import java.util.Optional

case class Suoritus(
                     oppijaNumero: Optional[String] = null,
                     suoritus: Optional[String] = null
)
