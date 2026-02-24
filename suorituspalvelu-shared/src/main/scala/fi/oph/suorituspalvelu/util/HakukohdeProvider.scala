package fi.oph.suorituspalvelu.util

import fi.oph.suorituspalvelu.integration.client.{KoutaHakukohde, VanhaTarjontaHakukohde}

trait HakukohdeProvider {
  def haeHakukohde(hakukohdeOid: String): Option[KoutaHakukohde | VanhaTarjontaHakukohde]
}
