package fi.oph.suorituspalvelu.util

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.status.Status
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

import scala.jdk.CollectionConverters.*

/**
 * Varmistaa, että logstashin <provider>-elementti osaa instantioida oman providerimme
 * niin ettei logbackin konfiguraation lataus tuota virheitä (kattaa Joran-setterikutsut).
 */
class StackTraceArrayLogbackWiringTest {

  @Test
  def testLogbackKonfiguraatioLatautuuIlmanVirheita(): Unit = {
    val context = new LoggerContext()
    val configurator = new JoranConfigurator()
    configurator.setContext(context)

    val configUrl = getClass.getClassLoader.getResource("logback.xml")
    assertNotNull(configUrl, "logback.xml ei löytynyt test-classpathilta")
    configurator.doConfigure(configUrl)

    val errors = context.getStatusManager.getCopyOfStatusList.asScala
      .filter(_.getLevel == Status.ERROR)
      .map(s => s"${s.getMessage} ${Option(s.getThrowable).map(_.toString).getOrElse("")}")
      .toList

    assertTrue(errors.isEmpty, s"Logback-konfiguraatiossa virheitä: ${errors.mkString("; ")}")
  }
}
