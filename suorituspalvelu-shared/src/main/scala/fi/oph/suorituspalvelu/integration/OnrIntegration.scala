package fi.oph.suorituspalvelu.integration

import fi.oph.suorituspalvelu.integration.client.{Henkiloviite, OnrClientImpl, RetryConfig}
import org.springframework.beans.factory.annotation.Autowired

import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import java.time.LocalDate
import java.util.concurrent.Executors

//Avaimina oidit joille kysyttiin, arvona setti jossa mukana kaikki aliakset sis. oid jolla kysyttiin
case class PersonOidsWithAliases(allOidsByQueriedOids: Map[String, Set[String]]) {
  def allOids: Set[String] = allOidsByQueriedOids.values.flatten.toSet
}

case class Kieli(kieliKoodi: String,
                 kieliTyyppi: Option[String] = None)

case class Kansalaisuus(kansalaisuusKoodi: String)

case class OnrMasterHenkilo(oidHenkilo: String, //Henkilön masterOid
                            etunimet: Option[String],
                            sukunimi: Option[String],
                            hetu: Option[String],
                            kaikkiHetut: Option[Seq[String]], //Huom. Tämä kenttä ei sisällä hetu-kentän hetua
                            syntymaaika: Option[LocalDate],
                            ) {
  def combinedHetut: Set[String] = {
    val hetut = kaikkiHetut.getOrElse(Seq.empty).toSet
    hetut ++ hetu.toSet
  }
}

case class OnrHenkiloPerustiedot(oidHenkilo: String,
                                 etunimet: Option[String],
                                 sukunimi: Option[String],
                                 hetu: Option[String])

trait OnrIntegration {

  /**
   * Palauttaa henkiloOidin aliakset, ts. muut masterhenkilöön linkatut henkilöoidit sekä itse masteroidin. Kysely
   * voidaan suorittaa joko masterhenkilön tai jonkin sen duplikaatin oidilla.
   *
   * @param personOids  joukko master- tai duplikaattihenkilöoideja joilla halutaan aliakset
   * @return            duplikaatit per haettu henkilöOid
   */
  def getAliasesForPersonOids(personOids: Set[String])(implicit retryConfig: RetryConfig): Future[PersonOidsWithAliases]

  def getMasterHenkilosForPersonOids(personOids: Set[String])(implicit retryConfig: RetryConfig): Future[Map[String, OnrMasterHenkilo]]

  def getAsiointikieli(oid: String)(implicit retryConfig: RetryConfig): Future[Option[String]]

  def henkiloExists(oid: String)(implicit retryConfig: RetryConfig): Future[Boolean]

  def getPerustiedotByHetus(hetus: Set[String])(implicit retryConfig: RetryConfig): Future[Seq[OnrHenkiloPerustiedot]]

  def getPerustiedotByPersonOids(personOids: Set[String])(implicit retryConfig: RetryConfig): Future[Seq[OnrHenkiloPerustiedot]]
}

class OnrIntegrationImpl extends OnrIntegration {

  val LOG = LoggerFactory.getLogger(classOf[OnrIntegrationImpl])

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  @Autowired val onrClient: OnrClientImpl = null

  override def getAliasesForPersonOids(personOids: Set[String])(implicit retryConfig: RetryConfig): Future[PersonOidsWithAliases] = {
    val viitteet: Future[Set[Henkiloviite]] = onrClient.getHenkiloviitteetForHenkilot(personOids, retryConfig)

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

  override def getMasterHenkilosForPersonOids(personOids: Set[String])(implicit retryConfig: RetryConfig): Future[Map[String, OnrMasterHenkilo]] = {
    onrClient.getMasterHenkilosForPersonOids(personOids, retryConfig)
  }

  override def getAsiointikieli(oid: String)(implicit retryConfig: RetryConfig): Future[Option[String]] =
    onrClient.getAsiointikieli(oid, retryConfig)

  override def henkiloExists(oid: String)(implicit retryConfig: RetryConfig): Future[Boolean] =
    this.getAsiointikieli(oid)(retryConfig).map(optKieli => optKieli.isDefined)

  override def getPerustiedotByHetus(hetus: Set[String])(implicit retryConfig: RetryConfig): Future[Seq[OnrHenkiloPerustiedot]] = {
    onrClient.getPerustiedotByHetus(hetus, retryConfig)
  }

  override def getPerustiedotByPersonOids(personOids: Set[String])(implicit retryConfig: RetryConfig): Future[Seq[OnrHenkiloPerustiedot]] = {
    if(personOids.isEmpty)
      Future.successful(Seq.empty)
    else
      onrClient.getPerustiedotByPersonOids(personOids, retryConfig)
  }
}
