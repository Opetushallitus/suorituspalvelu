package fi.oph.suorituspalvelu.util

import fi.oph.suorituspalvelu.integration.client.Koodi

trait KoodistoProvider {
  def haeKoodisto(koodisto: String): Map[String, Koodi]
}
