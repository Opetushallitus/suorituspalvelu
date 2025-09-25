package fi.oph.suorituspalvelu.business.parsing.koski

import fi.oph.suorituspalvelu.business.KantaOperaatiot.KantaEntiteetit.{AMMATILLINEN_OPISKELUOIKEUS, GENEERINEN_OPISKELUOIKEUS, PERUSOPETUKSEN_OPISKELUOIKEUS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, GeneerinenOpiskeluoikeus, KantaOperaatiot, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

@Test
@TestInstance(Lifecycle.PER_CLASS)
class KoskiToSuoritusConverterTest {

  def getOikeudetByType(oikeudet: Seq[Opiskeluoikeus]): (Seq[PerusopetuksenOpiskeluoikeus], Seq[AmmatillinenOpiskeluoikeus], Seq[GeneerinenOpiskeluoikeus]) = {
    val perusopetukset = oikeudet.collect { case po: PerusopetuksenOpiskeluoikeus => po }
    val ammatilliset = oikeudet.collect { case am: AmmatillinenOpiskeluoikeus => am }
    val geneeriset = oikeudet.collect { case g: GeneerinenOpiskeluoikeus => g }
    (perusopetukset, ammatilliset, geneeriset)
  }

  @Test def testKoskiParsingAndConversion1(): Unit =
    val fileName = "/1_2_246_562_24_40483869857.json"
    val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName))
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet)

      Assertions.assertEquals(4, oikeudet.size) // kolme mitätöityä opiskeluoikeutta

      val (perusopetukset, ammatilliset, geneeriset) = getOikeudetByType(oikeudet)
      Assertions.assertEquals(1, perusopetukset.size)
      Assertions.assertEquals(3, ammatilliset.size)
      Assertions.assertEquals(0, geneeriset.size)
    })

  @Test def testKoskiParsingAndConversion2(): Unit =
    val fileName = "/1_2_246_562_24_30563266636.json"
    val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName))
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet)

      Assertions.assertEquals(2, oikeudet.size) // kuusi mitätöityä opiskeluoikeutta

      val (perusopetukset, ammatilliset, geneeriset) = getOikeudetByType(oikeudet)
      Assertions.assertEquals(1, perusopetukset.size)
      Assertions.assertEquals(0, ammatilliset.size)
      Assertions.assertEquals(1, geneeriset.size)
    })

  @Test def testKoskiParsingForPerusopetuksenOpiskeluoikeudenSuoritukset(): Unit = {
    val fileName = "/oo_1.2.246.562.15.94501385358.json"
    val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName)).toList
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oikeudet = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet)

      //Tarkistetaan että löytyy yksi perusopetuksen opiskeluoikeusopiskeluoikeus, joka sisältää neljä suoritusta
      Assertions.assertEquals(oikeudet.size, 1)
      Assertions.assertEquals(oikeudet.head.asInstanceOf[PerusopetuksenOpiskeluoikeus].suoritukset.size, 4)
     })
  }

  @Test def testKoskiParsingForAmmatillistenOpiskeluoikeuksienSuoritukset(): Unit = {
    val fileName = "/1_2_246_562_24_56916824272.json"
    val splitData = KoskiParser.splitKoskiDataByOppija(this.getClass.getResourceAsStream(fileName)).toList
    splitData.foreach((oppijaOid, data) => {
      val koskiOpiskeluoikeudet = KoskiParser.parseKoskiData(data)
      val oikeudet: Seq[AmmatillinenOpiskeluoikeus] = KoskiToSuoritusConverter.parseOpiskeluoikeudet(koskiOpiskeluoikeudet)
        .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
        .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])

      //Pitäisi syntyä kolme ammatillista opiskeluoikeutta, joista kahdella on yksi suoritus ja yhdellä ei suorituksia.
      Assertions.assertEquals(oikeudet.size, 3)
      Assertions.assertEquals(1, oikeudet.find(o => o.oid == "1.2.246.562.15.79299730741").get.suoritukset.size)
      Assertions.assertEquals(1, oikeudet.find(o => o.oid == "1.2.246.562.15.24748024759").get.suoritukset.size)
      Assertions.assertEquals(0, oikeudet.find(o => o.oid == "1.2.246.562.15.54761186631").get.suoritukset.size)
    })
  }

}
