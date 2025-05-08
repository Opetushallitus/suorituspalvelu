package fi.oph.suorituspalvelu.integration

object KoskiMassaluovutusQueryParams {
  def forOids(oids: Set[String]): KoskiMassaluovutusQueryParams = {
    KoskiMassaluovutusQueryParams("sure-oppijat", "application/json", Some(oids), None)
  }
}

case class KoskiMassaluovutusQueryParams(`type`: String,
                                         format: String,
                                         oppijaOids: Option[Set[String]], //Käytännössä joko oppijaOids tai muuttuneetJälkeen on määritelty
                                         muuttuneetJälkeen: Option[String])