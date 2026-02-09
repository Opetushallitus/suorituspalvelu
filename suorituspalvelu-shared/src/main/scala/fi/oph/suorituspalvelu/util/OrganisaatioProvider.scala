package fi.oph.suorituspalvelu.util

import fi.oph.suorituspalvelu.integration.client.Organisaatio

trait OrganisaatioProvider {
  // Palauttaa koko organisaatio-hakutaulun, jossa voi olla sama organisaaatio sekä oid:lla että oppilaitoskoodilla
  protected def orgLookupTable(): Map[String, Organisaatio]

  /**
   * Hakee yksittäisen organisaation tiedot välimuistia käyttäen tunnisteen perusteella.
   * @param tunniste organisaation tunniste (oid tai oppilaitoskoodi)
   * @return Organisaation tiedot tai None, jos organisaatiota ei löydy tunnisteella
   */
  def haeOrganisaationTiedot(tunniste: String): Option[Organisaatio] =
    orgLookupTable().get(tunniste)

  // Palauttaa vain oid-avaimet, ettei tule duplikaatteja oppilaitoskoodi-avaimilla
  def haeKaikkiOrganisaatiot(): Map[String, Organisaatio] =
    orgLookupTable().filter((tunniste, _) => tunniste.startsWith("1.2.246"))

}
