package fi.oph.suorituspalvelu.integration.client

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.vm.sade.valinta.dokumenttipalvelu.SiirtotiedostoPalvelu
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import scala.collection.Seq

case class SiirtotiedostoClientConfig(region: String, bucket: String, roleArn: String)

object SiirtotiedostoClient {
  private val mapper = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .registerModule(new Jdk8Module())
    .registerModule(DefaultScalaModule)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  // Siirtotiedoston sisällön JSON-muoto
  def serialisoi[T](content: Seq[T]): String = mapper.writeValueAsString(content)
}

class SiirtotiedostoClient(config: SiirtotiedostoClientConfig) {
  private val LOG = LoggerFactory.getLogger(classOf[SiirtotiedostoClient])

  lazy val siirtotiedostoPalvelu =
    new SiirtotiedostoPalvelu(config.region, config.bucket, config.roleArn)
  val saveRetryCount = 2

  def tallennaSiirtotiedosto[T](
    contentType: String,
    content: Seq[T],
    executionId: String,
    fileNumber: Int,
    additionalInfo: Option[String] = None
  ): Unit = {
    try {
      if (content.nonEmpty) {
        val output = SiirtotiedostoClient.serialisoi(Seq(content.head))
        LOG.info(s"($executionId) Tallennetaan $contentType siirtotiedosto $fileNumber. Ensimmäinen entiteetti: $output")
        siirtotiedostoPalvelu
          .saveSiirtotiedosto(
            "supa",
            contentType,
            additionalInfo.getOrElse(""),
            executionId,
            fileNumber,
            new ByteArrayInputStream(SiirtotiedostoClient.serialisoi(content).getBytes()),
            saveRetryCount
          ).key
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
