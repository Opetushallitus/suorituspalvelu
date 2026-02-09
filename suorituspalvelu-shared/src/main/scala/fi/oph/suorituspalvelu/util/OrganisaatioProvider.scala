package fi.oph.suorituspalvelu.util

import fi.oph.suorituspalvelu.integration.client.Organisaatio

trait OrganisaatioProvider {
  /**
   * Hakee yksittäisen organisaation tiedot välimuistia käyttäen tunnisteen perusteella.
   * @param tunniste organisaation tunniste (oid tai oppilaitoskoodi)
   * @return Organisaation tiedot tai None, jos organisaatiota ei löydy tunnisteella
   */
  def haeOrganisaationTiedot(tunniste: String): Option[Organisaatio] =
    haeKaikkiOrganisaatiot().get(tunniste)

  def haeKaikkiOrganisaatiot(): Map[String, Organisaatio]
}
