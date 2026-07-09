package fi.oph.suorituspalvelu.util

import ch.qos.logback.classic.spi.{LoggingEvent, ThrowableProxy}
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

import java.io.StringWriter
import scala.jdk.CollectionConverters.*

class StackTraceArrayJsonProviderTest {

  private val mapper = JsonMapper.builder().build()
  private val NEWLINE = 10.toChar.toString

  private def renderStackTrace(event: LoggingEvent): Option[List[String]] = {
    val provider = new StackTraceArrayJsonProvider()
    provider.setMaxDepthPerThrowable(-1)
    provider.setMaxLength(-1)
    provider.setShortenedClassNameLength(-1)
    provider.setOmitCommonFrames(false)
    provider.start()

    val writer = new StringWriter()
    val generator = mapper.createGenerator(writer)
    generator.writeStartObject()
    provider.writeTo(generator, event)
    generator.writeEndObject()
    generator.close()

    val map = mapper.readValue(writer.toString, classOf[java.util.Map[String, Object]])
    Option(map.get("stack_trace")).map(_.asInstanceOf[java.util.List[String]].asScala.toList)
  }

  private def eventWith(t: Throwable): LoggingEvent = {
    val event = new LoggingEvent()
    event.setThrowableProxy(new ThrowableProxy(t))
    event
  }

  @Test
  def stackTraceKirjoitetaanTaulukkona(): Unit = {
    val exception = new IllegalStateException("boom", new RuntimeException("juurisyy"))
    val lines = renderStackTrace(eventWith(exception))
      .getOrElse(fail("stack_trace-kenttä puuttuu"))

    assertTrue(lines.size > 1, s"Odotettiin useita rivejä, saatiin: $lines")

    // Ensimmäinen rivi on poikkeuksen otsikko, ja juurisyy löytyy Caused by -rivinä.
    assertTrue(lines.head.contains("boom"), s"Ensimmäinen rivi: ${lines.head}")
    assertTrue(lines.exists(_.contains("Caused by")), "Caused by -riviä ei löytynyt")
    assertTrue(lines.exists(_.contains("juurisyy")), "Juurisyyn viestiä ei löytynyt")

    // Ydinvaatimus: ei rivinvaihtoja alkioiden sisällä eikä sisennystä alussa (ei \n\t).
    lines.foreach { line =>
      assertFalse(line.contains(NEWLINE), s"Rivi sisältää rivinvaihdon: $line")
      assertEquals(line, line.stripLeading, s"Rivin alussa on sisennystä: [$line]")
    }
  }

  @Test
  def ilmanPoikkeustaEiKirjoitetaKenttaa(): Unit = {
    assertTrue(renderStackTrace(new LoggingEvent()).isEmpty, "stack_trace ei saisi olla läsnä ilman poikkeusta")
  }
}
