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

  @Test def testKoskiParsingForAmmatillistenOpiskeluoikeuksienSuoritukset(): Unit = {
    val fileName = "/1_2_246_562_98_69863082363.json"
    val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName)).toList
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oos: Seq[Opiskeluoikeus] =  KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet)
      val oikeudet: Seq[PerusopetuksenOpiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet)
        .filter(o => o.isInstanceOf[PerusopetuksenOpiskeluoikeus])
        .map(o => o.asInstanceOf[PerusopetuksenOpiskeluoikeus])

      Assertions.assertEquals(1, oikeudet.size)

      val converterResult = AvainArvoConverter.convertPeruskouluArvot("1.2.246.562.98.69863082363", oos)

      Assertions.assertEquals(Some("9"), converterResult.keyValues.get(AvainArvoConstants.peruskouluAineenArvosanaPrefixes.head+"GE"))
      Assertions.assertEquals(Some("FI"), converterResult.keyValues.get(AvainArvoConstants.perusopetuksenKieliKeys.head))
      Assertions.assertEquals(Some("2025"), converterResult.keyValues.get(AvainArvoConstants.peruskouluSuoritusvuosiKeys.head))
      Assertions.assertEquals(Some("true"), converterResult.keyValues.get(AvainArvoConstants.peruskouluSuoritettuKeys.head))
    })
  }

}
