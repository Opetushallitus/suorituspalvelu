package fi.oph.suorituspalvelu.integration

case class KoskiMassaluovutusQueryParams(
                                          `type`: String,
                                          format: String,
                                          oppijaOids: Option[Set[String]], //Käytännössä joko oppijaOids tai muuttuneetJälkeen on määritelty
                                          muuttuneetJälkeen: Option[String]
                                        ) {
  def forOids(oids: Seq[String]): KoskiMassaluovutusQueryParams = {
    KoskiMassaluovutusQueryParams("tyyppi", "json", Some(oids.toSet), None)
  }
}
