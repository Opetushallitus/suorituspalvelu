package fi.oph.suorituspalvelu.util

import fi.oph.suorituspalvelu.integration.client.{KoutaHaku, VanhaTarjontaHaku}

trait HakuProvider {
  def haeHaku(hakuOid: String): Option[KoutaHaku | VanhaTarjontaHaku]
}
