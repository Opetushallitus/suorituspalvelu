package fi.oph.suorituspalvelu.business.parsing

import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter}
import org.junit.jupiter.api.{Assertions, Test}

object KoskiParsing {
}

@Test
class KoskiParsingTest {

  @Test def testKoskiParsingAndConversion(): Unit =
    Seq(
      "/1_2_246_562_24_40483869857.json",
      "/1_2_246_562_24_30563266636.json"
    ).foreach(fileName => {
      val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName))
      splitData.foreach((oppijaOid, data) => {
        val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
        val suoritukset = KoskiToSuoritusConverter.toSuoritus(koskiOpiskeluoikeudet)
      })
    })
}
