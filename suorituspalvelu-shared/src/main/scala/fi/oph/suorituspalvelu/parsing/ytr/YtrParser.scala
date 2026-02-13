package fi.oph.suorituspalvelu.parsing.ytr

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.integration.ytr.YtrDataForHenkilo

import java.io.{ByteArrayInputStream, InputStream}
import java.time.LocalDate

case class Exam(
                 examId: String,
                 period: String, // xxxxy, jossa xxxx on vuosi ja y joko K tai S.
                 grade: String,
                 points: Option[Int]
               )

case class Student(
                    ssn: Option[String],
                    lastname: String,
                    firstnames: String,
                    graduationPeriod: Option[String] = None,
                    graduationDate: Option[String] = None,
                    language: Option[String],
                    exams: Seq[Exam]
                  )

object YtrParser {
  val MAPPER: ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JavaTimeModule())
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    mapper
  }

  def sanitize(data: String): String = {
    splitAndSanitize("[" + data + "]").next()._2
  }

  def splitAndSanitize(data: String): Iterator[(String, String)] = {
    val jsonParser = MAPPER.getFactory().createParser(new ByteArrayInputStream(data.getBytes("UTF-8")))
    jsonParser.nextToken()

    Iterator.continually({
        val token = jsonParser.nextToken()
        if (token != JsonToken.END_ARRAY) {
          Some(jsonParser.readValueAs(classOf[Map[String, Any]]))
        } else
          None
      })
      .takeWhile(data => data.isDefined)
      .map(data => {
        (data.get("ssn").toString, MAPPER.writeValueAsString(data.get + ("ssn" -> None)))
      })
  }

  def parseYtrData(data: String): Student =
    MAPPER.readValue(data, classOf[Student])

}
