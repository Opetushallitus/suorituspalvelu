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
                    ssn: String,
                    lastname: String,
                    firstnames: String,
                    graduationPeriod: Option[String] = None,
                    graduationDate: Option[String] = None,
                    language: String,
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

  def parseYtrMassData(data: String, personOidByHetu: Map[String, String]): Iterator[YtrDataForHenkilo] = {
    splitYtrDataByOppija(new ByteArrayInputStream(data.getBytes("UTF-8")), personOidByHetu)
  }

  def parseSingleAndRemoveHetu(data: String, personOid: String): YtrDataForHenkilo = {
    val student = MAPPER.readValue(data, classOf[Student])
    YtrDataForHenkilo(personOid, Some(MAPPER.writeValueAsString(student.copy(ssn = ""))))
  }

  def splitYtrDataByOppija(input: InputStream, personOidByHetu: Map[String, String] = Map.empty): Iterator[YtrDataForHenkilo] = {
    val jsonParser = MAPPER.getFactory().createParser(input)
    jsonParser.nextToken()

    Iterator.continually({
        val token = jsonParser.nextToken()
        if (token != JsonToken.END_ARRAY) {
          //Fixme ehkä: Tämä on periaatteessa vähän outo hetki parsia data valmiiksi case classiksi,
          // mutta hetun poimimisen takia joku parsinta täytyy tehdä jos data halutaan tallentaa oppijanumeron eikä hetun alle.
          Some(jsonParser.readValueAs(classOf[Student]))
        } else
          None
      })
      .takeWhile(data => data.isDefined)
      .map(data => {
        val personOid = personOidByHetu.getOrElse(data.get.ssn, throw new RuntimeException(s"Missing personOid for ssn ${data.get.ssn}"))
        //Pudotetaan hetu pois datasta, jotta ei päädy kantaan.
        YtrDataForHenkilo(personOid, Some(MAPPER.writeValueAsString(data.get.copy(ssn = ""))))
      })
  }

}
