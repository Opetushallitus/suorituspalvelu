package fi.oph.suorituspalvelu.integration.ytr

import fi.oph.suorituspalvelu.integration.{KoskiIntegration, OnrIntegration, OnrMasterHenkilo}
import fi.oph.suorituspalvelu.integration.client.{YtlHetuPostData, YtrClient}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired

import java.time.LocalDate
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class Student(
                    ssn: String,
                    lastname: String,
                    firstnames: String,
                    graduationPeriod: Option[String] = None, //Todo, parse as Kausi
                    graduationDate: Option[LocalDate] = None,
                    language: String,
                    exams: Seq[Exam]
                  )

case class Exam(
                 examId: String,
                 period: String, // Todo, parse as Kausi
                 grade: String,
                 points: Option[Int]
               )

class YtrIntegration {

  private val LOG: Logger = LoggerFactory.getLogger(classOf[KoskiIntegration])
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  @Autowired val ytrClient: YtrClient = null

  @Autowired val onrIntegration: OnrIntegration = null

  def fetchRawForStudents(personOids: Set[String]): Seq[Option[String]] = {
    val useHenkilot = personOids.take(10)
    val henkilot = onrIntegration.getMasterHenkilosForPersonOids(useHenkilot)

    //Todo, käytetään massahakutoiminnallisuutta vähänkin suuremmille erille (10+? 100+? 500+?)
    val resultF = henkilot.map((henkiloResult: Map[String, OnrMasterHenkilo]) => {
      LOG.info(s"Saatiin oppijanumerorekisteristä ${henkiloResult.size} henkilön tiedot ${useHenkilot.size} haetulle henkilölle")
      val ytrParams = henkiloResult.values.filter(_.hetu.isDefined).map(h => YtlHetuPostData(h.hetu.get, h.kaikkiHetut))
      LOG.info(s"Haetuista henkilöistä ${ytrParams.size} henkilölle löytyi hetu, eli haetaan ytr-tiedot")
      val k = ytrParams.map(ytrParam => {
        val resultF = ytrClient.fetchOne(ytrParam)
        Await.result(resultF, 1.minute)
      }).toList
      //Todo, persist jossain välissä
      k
    })
    Await.result(resultF, 5.minutes)

  }

  //Dataa ei välttämättä löydy ytr:stä
  def fetchRawForStudent(ssn: String): Option[String] = {
    val resultF = ytrClient.fetchOne(YtlHetuPostData(ssn, None))
    Await.result(resultF, 1.minute)
  }

}
