package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.vm.sade.valinta.dokumenttipalvelu.SiirtotiedostoPalvelu
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import scala.collection.Seq

case class SiirtotiedostoClientConfig(region: String, bucket: String, roleArn: String)

class SiirtotiedostoClient(config: SiirtotiedostoClientConfig) {
  private val LOG = LoggerFactory.getLogger(classOf[SiirtotiedostoClient])

  LOG.info(s"Luodaan SiirtotiedostoClient konfiguraatiolla $config")
  lazy val siirtotiedostoPalvelu =
    new SiirtotiedostoPalvelu(config.region, config.bucket, config.roleArn)
  val saveRetryCount = 2

  private val mapper = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .registerModule(new Jdk8Module())
    .registerModule(DefaultScalaModule)

  def tallennaSiirtotiedosto[T](
    contentType: String,
    content: Seq[T],
    executionId: String,
    fileNumber: Int,
    additionalInfo: Option[String] = None //fixme hmm
  ): Unit = {
    try {
      if (content.nonEmpty) {
        val output = mapper.writeValueAsString(Seq(content.head))
        LOG.info(s"($executionId) Tallennetaan (leikisti) $contentType siirtotiedosto $fileNumber... ensimmäinen entiteetti: $output")
        /*siirtotiedostoPalvelu
          .saveSiirtotiedosto(
            "sure",
            contentType,
            additionalInfo.getOrElse(""),
            executionId,
            fileNumber,
            new ByteArrayInputStream(mapper.writeValueAsString(content).getBytes()),
            saveRetryCount
          ).key*/
        "mock-key"
      } else {
        LOG.info(s"($executionId) Ei tallennettavaa!")
      }
    } catch {
      case t: Throwable =>
        LOG.error(s"($executionId) Siirtotiedoston tallennus s3-ämpäriin epäonnistui:", t)
        throw t
    }
  }
}
