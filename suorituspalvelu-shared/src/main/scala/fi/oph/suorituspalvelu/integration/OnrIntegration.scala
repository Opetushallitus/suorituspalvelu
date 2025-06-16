package fi.oph.suorituspalvelu.integration

import fi.oph.suorituspalvelu.integration.client.{Henkiloviite, OnrClientImpl}
import org.springframework.beans.factory.annotation.Autowired

import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import java.util.concurrent.Executors

//Avaimina oidit joille kysyttiin, arvona setti jossa mukana kaikki aliakset sis. oid jolla kysyttiin
case class PersonOidsWithAliases(allOidsByQueriedOids: Map[String, Set[String]]) {
  def allOids: Set[String] = allOidsByQueriedOids.values.flatten.toSet
}

case class Kieli(kieliKoodi: String,
                 kieliTyyppi: Option[String] = None)

case class Kansalaisuus(kansalaisuusKoodi: String)

case class OnrMasterHenkilo(oidHenkilo: String, //Henkilön masterOid
                            hetu: Option[String],
                            kaikkiHetut: Option[Seq[String]] //Huom. Tämä kenttä ei sisällä hetu-kentän hetua
                            ) {
  def combinedHetut: Set[String] = {
    val hetut = kaikkiHetut.getOrElse(Seq.empty).toSet
    hetut ++ hetu.toSet
  }
}

trait OnrIntegration {
  def getAliasesForPersonOids(personOids: Set[String]): Future[PersonOidsWithAliases]

  def getMasterHenkilosForPersonOids(personOids: Set[String]): Future[Map[String, OnrMasterHenkilo]]
}

class OnrIntegrationImpl extends OnrIntegration {

  val LOG = LoggerFactory.getLogger(classOf[OnrIntegrationImpl])

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  @Autowired val onrClient: OnrClientImpl = null

  override def getAliasesForPersonOids(personOids: Set[String]): Future[PersonOidsWithAliases] = {
    val viitteet: Future[Set[Henkiloviite]] = onrClient.getHenkiloviitteetForHenkilot(personOids)

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

  override def getMasterHenkilosForPersonOids(personOids: Set[String]): Future[Map[String, OnrMasterHenkilo]] = {
    onrClient.getMasterHenkilosForPersonOids(personOids)
  }
}
