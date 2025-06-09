package fi.oph.suorituspalvelu.integration

import fi.oph.suorituspalvelu.integration.client.{Henkiloviite, OnrClientImpl}
import org.springframework.beans.factory.annotation.Autowired

import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import java.util.concurrent.Executors

case class PersonOidsWithAliases(
                                 allOidsByQueriedOids: Map[String, Set[String]] //Avaimina oidit joille kysyttiin, arvona setti jossa mukana kaikki aliakset sis. oid jolla kysyttiin
                                 ) {
  def allOids: Set[String] = allOidsByQueriedOids.values.flatten.toSet
}

trait OnrIntegration {
  def getAliasesForPersonOids(personOids: Set[String]): Future[PersonOidsWithAliases]
}

class OnrIntegrationImpl extends OnrIntegration {

  val LOG = LoggerFactory.getLogger(classOf[OnrIntegrationImpl])

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  @Autowired val onrClient: OnrClientImpl = null

  override def getAliasesForPersonOids(personOids: Set[String]): Future[PersonOidsWithAliases] = {
    val viitteet = onrClient.getHenkiloviitteetForHenkilot(personOids)

    viitteet.flatMap((viiteResult: Set[Henkiloviite]) => {
      LOG.info(s"Got ${viiteResult.size} viittees for ${personOids.size} personOids")

      val viitteetByMasterOid =
        viiteResult.groupBy(_.masterOid).map(kv => (kv._1, kv._2.map(_.henkiloOid)))
      val linkedOidToMasterOid: Map[String, Set[String]] =
        viiteResult.groupBy(_.henkiloOid).map(kv => (kv._1, kv._2.map(_.masterOid)))

      val resultMap = personOids.map(queriedOid => {
        //Jos ei löydy linkkiä queriedOid -> masterOid, queriedOid on henkilön masterOid
        val masterOid = linkedOidToMasterOid.getOrElse(queriedOid, Set.empty).headOption.getOrElse(queriedOid)

        val linkedOids = viitteetByMasterOid.getOrElse(masterOid, Set.empty)
        val allOids = Set(masterOid) ++ linkedOids

        queriedOid -> allOids
      }).toMap
      Future.successful(PersonOidsWithAliases(resultMap))

    })

  }

}
