package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.vm.sade.javautils.nio.cas.CasClient
import org.asynchttpclient.RequestBuilder
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.jdk.javaapi.FutureConverters.asScala

//Todo, oma ec?
import scala.concurrent.ExecutionContext.Implicits.global

case class AtaruHenkiloSearchParams(hakukohdeOids: Option[List[String]], hakuOid: Option[String], offset: Option[String] = None)


case class AtaruHakemuksenHenkilotiedot(oid: String, //hakemuksen oid
                                        personOid: Option[String],
                                        ssn: Option[String])

case class AtaruResponseHenkilot(applications: List[AtaruHakemuksenHenkilotiedot],
                                 offset: Option[String])

case class AtaruPermissionRequest(personOidsForSamePerson: Set[String],
                                  organisationOids: Set[String],
                                  loggedInUserRoles: Set[String])

case class AtaruPermissionResponse(accessAllowed: Option[Boolean] = None,
                                   errorMessage: Option[String] = None)

trait HakemuspalveluClient {
  def getHaunHakijat(hakuOid: String): Future[Seq[AtaruHakemuksenHenkilotiedot]]
  def getHakemustenHenkilotiedot(params: AtaruHenkiloSearchParams): Future[Seq[AtaruHakemuksenHenkilotiedot]]
  def checkPermission(permissionRequest: AtaruPermissionRequest): Future[AtaruPermissionResponse]
}

class HakemuspalveluClientImpl(casClient: CasClient, environmentBaseUrl: String) extends HakemuspalveluClient {


  val LOG = LoggerFactory.getLogger(classOf[HakemuspalveluClientImpl]);

  val mapper: ObjectMapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  override def getHaunHakijat(hakuOid: String): Future[Seq[AtaruHakemuksenHenkilotiedot]] = {
    getHakemustenHenkilotiedot(AtaruHenkiloSearchParams(None, Some(hakuOid)))
  }

  override def getHakemustenHenkilotiedot(params: AtaruHenkiloSearchParams): Future[Seq[AtaruHakemuksenHenkilotiedot]] = {
    def fetchAllRecursive(currentParams: AtaruHenkiloSearchParams, accResults: Seq[AtaruHakemuksenHenkilotiedot] = Seq.empty): Future[Seq[AtaruHakemuksenHenkilotiedot]] = {
      val bodyMap = List(
        currentParams.hakuOid.map("hakuOid" -> _),
        currentParams.hakukohdeOids.map("hakukohdeOids" -> _),
        currentParams.offset.map("offset" -> _)
      ).flatten.toMap
      doPost(environmentBaseUrl + "/lomake-editori/api/external/suoritusrekisteri/henkilot", bodyMap)
        .flatMap(data => {
          val parsed: AtaruResponseHenkilot = mapper.readValue[AtaruResponseHenkilot](data, classOf[AtaruResponseHenkilot])
          val newResults = accResults ++ parsed.applications

          parsed.offset match {
            case Some(nextOffset) =>
              LOG.info(s"Saatiin ${parsed.applications.size} hakemusta, haetaan seuraava erä (offset $nextOffset)")
              fetchAllRecursive(currentParams.copy(offset = Some(nextOffset)), newResults)
            case None =>
              LOG.info(s"Saatiin hakemuspalvelusta yhteensä ${newResults.size} hakemuksen henkilötiedot parametreille $params, ei enää haettavaa.")
              Future.successful(newResults)
          }
        })
    }
    fetchAllRecursive(params)
  }

  override def checkPermission(permissionRequest: AtaruPermissionRequest): Future[AtaruPermissionResponse] = {
    doPost(environmentBaseUrl + "/lomake-editori/api/checkpermission", permissionRequest).flatMap(responseString => {
      val parsed: AtaruPermissionResponse = mapper.readValue[AtaruPermissionResponse](responseString, classOf[AtaruPermissionResponse])
      Future.successful(parsed)
    })
  }

  private def doPost(url: String, body: Object): Future[String] = {
    val req = new RequestBuilder()
      .setMethod("POST")
      .setHeader("Content-Type", "application/json")
      .setBody(mapper.writeValueAsString(body))
      .setUrl(url)
      .build()
    try {
      asScala(casClient.execute(req)).map {
        case r if r.getStatusCode == 200 =>
          r.getResponseBody()
        case r =>
          val errorStr = s"Failed to fetch data from hakemuspalvelu: ${r.getStatusCode} ${r.getStatusText} ${r.getResponseBody()}"
          LOG.error(
            errorStr
          )
          throw new RuntimeException(errorStr)
      }
    } catch {
      case e: Throwable =>
        LOG.error(
          s"Failed to fetch data from hakemuspalvelu", e
        )
        Future.failed(e)
    }
  }
}
