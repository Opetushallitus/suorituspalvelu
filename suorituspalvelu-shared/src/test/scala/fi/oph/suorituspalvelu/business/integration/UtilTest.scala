package fi.oph.suorituspalvelu.business.integration

import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.integration.KoskiIntegration
import fi.oph.suorituspalvelu.integration.Util
import fi.oph.suorituspalvelu.integration.client.Koodisto
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoContainer, AvainArvoConverter}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiParser, KoskiToSuoritusConverter}
import fi.oph.suorituspalvelu.parsing.ytr.{YtrParser, YtrToSuoritusConverter}
import fi.oph.suorituspalvelu.util.KoodistoProvider
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

import java.time.LocalDate
import java.util
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class UtilTest {

  @Test def testToIterator(): Unit = {
    val order: java.util.List[String] = util.ArrayList[String]()

    val iterators: Iterator[Future[Iterator[Int]]] = (1 to 10).iterator.map(i => {
      order.add(s"iterator$i")
      Future.successful(Seq(i).iterator)
    }).iterator

    val result = Util.toIterator(iterators, 3, 1.minute).flatten
    result.foreach(i => order.add(s"$i"))

    Assertions.assertEquals(List(
      "iterator1",
      "iterator2",
      "iterator3",
      "1",
      "iterator4",
      "2",
      "iterator5",
      "3",
      "iterator6",
      "4",
      "iterator7",
      "5",
      "iterator8",
      "6",
      "iterator9",
      "7",
      "iterator10",
      "8",
      "9",
      "10",
    ), order.asScala.toList)
  }
}
