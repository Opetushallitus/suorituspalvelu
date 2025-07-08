package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.KantaOperaatiot.KantaEntiteetit.{AMMATILLINEN_OPISKELUOIKEUS, GENEERINEN_OPISKELUOIKEUS, PERUSOPETUKSEN_OPISKELUOIKEUS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, GeneerinenOpiskeluoikeus, KantaOperaatiot, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConverter, AvainArvoConstants}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

@Test
@TestInstance(Lifecycle.PER_CLASS)
class AvainArvoConverterTest {

  @Test def testAvainArvoConverterForPeruskouluKeys(): Unit = {
    val fileName = "/1_2_246_562_98_69863082363.json"
    val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName)).toList
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oos: Seq[Opiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet)

      Assertions.assertEquals(1, oos.size)

      val converterResult = AvainArvoConverter.convertPeruskouluArvot("1.2.246.562.98.69863082363", oos)

      AvainArvoConstants.peruskouluAineenArvosanaPrefixes.foreach(prefix => {
        Assertions.assertEquals(Some("9"), converterResult.keyValues.get(prefix + "GE"))
      })
      AvainArvoConstants.perusopetuksenKieliKeys.foreach(key => {
        Assertions.assertEquals(Some("FI"), converterResult.keyValues.get(key))
      })
      AvainArvoConstants.peruskouluSuoritusvuosiKeys.foreach(key => {
        Assertions.assertEquals(Some("2025"), converterResult.keyValues.get(key))
      })
      AvainArvoConstants.peruskouluSuoritettuKeys.foreach(key => {
        Assertions.assertEquals(Some("true"), converterResult.keyValues.get(AvainArvoConstants.peruskouluSuoritettuKeys.head))
      })
    })
  }
}
