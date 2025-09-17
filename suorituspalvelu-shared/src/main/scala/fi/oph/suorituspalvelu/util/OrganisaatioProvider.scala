package fi.oph.suorituspalvelu.util

import fi.oph.suorituspalvelu.integration.client.Organisaatio

trait OrganisaatioProvider {
  def haeOrganisaationTiedot(koodiArvo: String): Option[Organisaatio]
}