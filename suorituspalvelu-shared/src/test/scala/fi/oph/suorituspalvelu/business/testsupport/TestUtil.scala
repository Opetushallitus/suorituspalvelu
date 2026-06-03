package fi.oph.suorituspalvelu.business.testsupport

import fi.oph.suorituspalvelu.integration.client.{Koodi, Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}

object TestUtil {

  val DUMMY_ORGANISAATIOPROVIDER: OrganisaatioProvider = () => Map("1.2.3" -> Organisaatio("1.2.3", OrganisaatioNimi("", "", ""), None, Seq.empty, Seq.empty))

  val DUMMY_KOODISTOPROVIDER: KoodistoProvider = new KoodistoProvider {
    override def haeKoodisto(koodisto: String): Map[String, Koodi] = Map.empty

    override def haeAlakoodit(koodiUri: String): List[Koodi] = List.empty
  }

  def buildDummyKoodistoProvider(koodistoMap: Map[String, Koodi] = Map.empty, alaKoodit: List[Koodi] = List.empty): KoodistoProvider =
    new KoodistoProvider {
      override def haeKoodisto(koodisto: String): Map[String, Koodi] = koodistoMap

      override def haeAlakoodit(koodiUri: String): List[Koodi] = alaKoodit
    }
}
