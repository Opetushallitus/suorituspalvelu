package fi.oph.suorituspalvelu.integration

import fi.oph.suorituspalvelu.business.VersioEntiteetti

case class SyncResultForHenkilo(henkiloOid: String, versio: Option[VersioEntiteetti], exception: Option[Exception])