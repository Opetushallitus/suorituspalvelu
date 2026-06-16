package fi.oph.suorituspalvelu.util

import fi.oph.suorituspalvelu.integration.client.Organisaatio

object OrganisaatioProvider {
  val AKTIIVINEN = "AKTIIVINEN"
}

trait OrganisaatioProvider {
  // Palauttaa koko organisaatio-hakutaulun, jossa voi olla sama organisaaatio sekä oid:lla että oppilaitoskoodilla
  protected def orgLookupTable(): Map[String, Organisaatio]

  private def onAktiivinen(o: Organisaatio): Boolean =
    o.status.contains(OrganisaatioProvider.AKTIIVINEN)

  /**
   * Hakee yksittäisen organisaation tiedot välimuistia käyttäen tunnisteen perusteella.
   *
   * Oletuksena palautetaan myös lakkautetut organisaatiot, jotta esim. vanhojen suoritusten oppilaitosten
   * nimet näkyvät käyttöliittymässä. Kutsuja voi tarkistaa `status`-kentästä, onko organisaatio aktiivinen.
   *
   * @param tunniste organisaation tunniste (oid tai oppilaitoskoodi)
   * @param mukaanLukienLakkautetut jos false, suodatetaan pois muut kuin AKTIIVINEN-statuksella varustetut
   * @return Organisaation tiedot tai None, jos organisaatiota ei löydy tai se on suodatettu pois
   */
  def haeOrganisaationTiedot(tunniste: String, mukaanLukienLakkautetut: Boolean = true): Option[Organisaatio] =
    orgLookupTable().get(tunniste).filter(o => mukaanLukienLakkautetut || onAktiivinen(o))

  /**
   * Palauttaa kaikki organisaatiot. Oletuksena lakkautetut (status != "AKTIIVINEN") suodatetaan pois;
   * käsin syötettävien suoritusten oppilaitoslistauksessa kutsutaan parametrilla `true`, jotta myös lakkautetut
   * näkyvät.
   */
  def haeKaikkiOrganisaatiot(mukaanLukienLakkautetut: Boolean = false): Map[String, Organisaatio] =
    orgLookupTable()
      .filter((tunniste, _) => tunniste.startsWith("1.2.246"))
      .filter((_, o) => mukaanLukienLakkautetut || onAktiivinen(o))

  // Palauttaa organisaation yläorganisaatioiden tunnisteet (oid)
  def haeKaikkiOrganisaationParenttienOidit(orgOid: String): List[String] =
    orgLookupTable().get(orgOid).flatMap(_.parentOid)
      .map(parent => List(parent) ++ haeKaikkiOrganisaationParenttienOidit(parent)).getOrElse(List.empty)
}
