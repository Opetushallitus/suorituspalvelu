package fi.oph.suorituspalvelu.integration

import java.time.Instant

object KoskiMassaluovutusQueryParams {
  def forOids(oids: Set[String]): KoskiMassaluovutusQueryParams = {
    KoskiMassaluovutusQueryParams("supa-oppijat", "application/json", Some(oids), None)
  }

  def forSince(since: Instant): KoskiMassaluovutusQueryParams = {
    KoskiMassaluovutusQueryParams("supa-oppijat", "application/json", None, Some(since.toString))
  }
}

case class KoskiMassaluovutusQueryParams(`type`: String,
                                         format: String,
                                         oppijaOids: Option[Set[String]], //Käytännössä joko oppijaOids tai muuttuneetJälkeen on määritelty
                                         muuttuneetJälkeen: Option[String])