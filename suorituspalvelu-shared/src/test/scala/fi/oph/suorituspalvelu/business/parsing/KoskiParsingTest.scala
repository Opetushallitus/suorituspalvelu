package fi.oph.suorituspalvelu.business.parsing

import fi.oph.suorituspalvelu.parsing.{KoskiParser, KoskiToSuoritusConverter}
import org.junit.jupiter.api.{Assertions, Test}

object KoskiParsing {
}


@Test
class KoskiParsingTest {

/*
  @Test def testKoskiNormalization(): Unit =
    val suoritukset = MAPPER. readValue(this.getClass.getResourceAsStream("/koski0.json"), classOf[Array[NormalisoitavaKoskiData]])

    val s = ""

  @Test def testKoskiParsing(): Unit =
    val suoritukset = MAPPER.readValue(this.getClass.getResourceAsStream("/koski0.json"), classOf[Array[KoskiSuoritukset]])
    val s = ""
*/

  @Test def testKoskiParsing(): Unit =
    val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream("/1_2_246_562_24_40483869857.json"))

    val suoritukset = splitData.map((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val suoritukset = KoskiToSuoritusConverter.toSuoritus(koskiOpiskeluoikeudet)
      val s = ""
    }).toSeq


}
