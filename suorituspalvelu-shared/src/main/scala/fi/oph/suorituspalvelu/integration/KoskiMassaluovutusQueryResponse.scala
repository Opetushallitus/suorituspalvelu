package fi.oph.suorituspalvelu.integration

case class KoskiMassaluovutusQueryResponse(
                                            queryId: String,
                                            requestedBy: String,
                                            query: KoskiMassaluovutusQueryParams,
                                            createdAt: Option[String],
                                            startedAt: Option[String],
                                            finishedAt: Option[String],
                                            files: Seq[String],
                                            resultsUrl: Option[String],
                                            progress: Option[Map[String, String]],
                                            sourceDataUpdatedAt: Option[String],
                                            status: String
                                          ) {
  def isFinished() = status.equals("complete") || isFailed()

  def isComplete() = status.equals("complete")
  
  def isFailed() = status.equals("failed")

  def getTruncatedLoggable(): KoskiMassaluovutusQueryResponse = {
    if (query.oppijaOids.exists(_.size > 10))
      this.copy(query = query.copy(oppijaOids = query.oppijaOids.map(oids => Set(s"Total of ${oids.size} oppijaOids"))))
    else
      this
  }
}
