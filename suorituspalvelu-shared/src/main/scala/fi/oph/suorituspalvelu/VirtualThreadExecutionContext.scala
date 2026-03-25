package fi.oph.suorituspalvelu

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

/**
 * Jaettu ExecutionContext joka käyttää virtuaalisäikeitä. Tämä on tarkoitettu korvaamaan
 * kaikki yksittäiset ExecutionContext.Implicits.global- ja newFixedThreadPool-instanssit.
 *
 * Virtuaalisäikeet ovat kevyitä (JDK 21+), joten Thread.sleep ja Await.result eivät
 * blokkaa platform-säikeitä. Toisin kuin kiinteän kokoiset säiealtaat, virtuaalisäikeillä
 * ei ole thread starvation -riskiä.
 *
 * Rinnakkaisuuden rajoittaminen ulkoisiin palveluihin tehdään Util.toIterator-metodin
 * concurrency-parametrilla, ei säiealtaan koolla.
 */
object VirtualThreadExecutionContext {
  implicit val executor: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newVirtualThreadPerTaskExecutor())
}

