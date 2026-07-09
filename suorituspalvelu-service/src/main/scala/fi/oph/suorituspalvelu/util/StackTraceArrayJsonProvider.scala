package fi.oph.suorituspalvelu.util

import ch.qos.logback.classic.spi.ILoggingEvent
import net.logstash.logback.composite.AbstractFieldJsonProvider
import net.logstash.logback.stacktrace.ShortenedThrowableConverter
import tools.jackson.core.JsonGenerator

/**
 * Lokittaa stack tracen JSON-taulukkona (yksi rivi per alkio), jotta se näkyy luettavana
 * CloudWatchissa yhden merkkijonon sijaan, joka on täynnä escapetettuja \n\t-merkkejä.
 * Varsinaisen muotoilun (lyhennykset, "Caused by", syvyys-/pituusrajat) hoitaa
 * ShortenedThrowableConverter, jonka tuloste pilkotaan riveittäin alkioiksi.
 */
class StackTraceArrayJsonProvider extends AbstractFieldJsonProvider[ILoggingEvent] {

  private val converter = new ShortenedThrowableConverter()

  // Setterit delegoidaan, jotta logback.xml voi käyttää tuttuja asetuksia.
  def setMaxDepthPerThrowable(v: Int): Unit = converter.setMaxDepthPerThrowable(v)
  def setMaxLength(v: Int): Unit = converter.setMaxLength(v)
  def setShortenedClassNameLength(v: Int): Unit = converter.setShortenedClassNameLength(v)
  def setOmitCommonFrames(v: Boolean): Unit = converter.setOmitCommonFrames(v)

  override def start(): Unit = {
    if (getFieldName == null) setFieldName("stack_trace")
    // Kiinnitetään rivinvaihto UNIX-muotoon, jotta split alla on deterministinen.
    converter.setLineSeparator("UNIX")
    converter.start()
    super.start()
  }

  override def stop(): Unit = {
    converter.stop()
    super.stop()
  }

  override def writeTo(generator: JsonGenerator, event: ILoggingEvent): Unit = {
    if (event.getThrowableProxy != null) {
      generator.writeArrayPropertyStart(getFieldName)
      // Pilkotaan riveittäin ja poistetaan logbackin sisennystabi kunkin rivin alusta.
      converter.convert(event).split("\n", -1).foreach(line => generator.writeString(line.stripLeading))
      generator.writeEndArray()
    }
  }
}
