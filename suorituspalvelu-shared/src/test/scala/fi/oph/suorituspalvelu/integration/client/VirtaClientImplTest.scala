package fi.oph.suorituspalvelu.integration.client

import fi.oph.suorituspalvelu.integration.virta.VirtaClientImpl
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Disabled, Test, TestInstance}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@Test
@TestInstance(Lifecycle.PER_CLASS)
class VirtaClientImplTest {

  final val TIMEOUT = 30.seconds

  @Disabled
  @Test def testVirtaClient(): Unit =
    val client = VirtaClientImpl("", "", "salaisuus", "http://virtawstesti.csc.fi:8084")

    val result = Await.result(client.haeKaikkiTiedot("1.2.246.562.24.21250967215", Some("010296-1230")), TIMEOUT)

    val suoritukset = result.map(r => VirtaParser.parseVirtaData(r))
    
    val konvertoidut = suoritukset.map(s => VirtaToSuoritusConverter.toSuoritukset(s).toSet).flatten
    
    val s = ""
}
