package fi.oph.suorituspalvelu.business.parsing.virta

import fi.oph.suorituspalvelu.business.{AmmatillinenPerustutkinto, Koodi, Opintosuoritus, VirtaOpiskeluoikeus, VirtaTutkinto}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiParser, KoskiToSuoritusConverter}
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeAll, Test, TestInstance}

import java.io.ByteArrayInputStream
import java.time.LocalDate

object KoskiParsing {
}

@Test
@TestInstance(Lifecycle.PER_CLASS)
class VirtaParsingTest {

  @Test def testVirtaParsingAndConversion(): Unit =
    Seq(
      "/1_2_246_562_24_21250967215.xml"
    ).foreach(fileName => {
      val suoritukset = VirtaParser.parseVirtaData(this.getClass.getResourceAsStream(fileName))
      val opiskeluoikeudet = VirtaToSuoritusConverter.toOpiskeluoikeudet(suoritukset)
    })

  @Test def testVirtaOpiskeluoikeudenKentat(): Unit =
    val opiskeluoikeus = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData(
      """
        |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
        |  <SOAP-ENV:Body>
        |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
        |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
        |        <virta:Opiskelija avain="C10">
        |          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C102" avain="xxx002">
        |              <virta:AlkuPvm>2018-01-01</virta:AlkuPvm>
        |              <virta:LoppuPvm>2019-01-01</virta:LoppuPvm>              
        |              <virta:Tila>
        |                <virta:AlkuPvm>2013-08-22</virta:AlkuPvm>
        |                <virta:LoppuPvm>2017-05-31</virta:LoppuPvm>
        |                <virta:Koodi>1</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2017-06-01</virta:AlkuPvm>
        |                <virta:Koodi>3</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Myontaja>01901</virta:Myontaja>
        |              <virta:Jakso>
        |                <virta:Koulutuskoodi>726302</virta:Koulutuskoodi>
        |              </virta:Jakso>
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |        </virta:Opiskelija>
        |      </virta:Virta>
        |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
        |  </SOAP-ENV:Body>
        |</SOAP-ENV:Envelope>""".stripMargin)).head

    Assertions.assertNotNull(opiskeluoikeus.tunniste)
    Assertions.assertEquals("xxx002", opiskeluoikeus.virtaTunniste)
    Assertions.assertEquals("726302", opiskeluoikeus.koulutusKoodi)
    Assertions.assertEquals(LocalDate.parse("2018-01-01"), opiskeluoikeus.alkuPvm)
    Assertions.assertEquals(LocalDate.parse("2019-01-01"), opiskeluoikeus.loppuPvm)
    Assertions.assertEquals(Koodi("3", VirtaToSuoritusConverter.VIRTA_OO_TILA_KOODISTO, None), opiskeluoikeus.virtaTila) // viimeiseksi alkanut tila on voimassa
    Assertions.assertEquals("01901", opiskeluoikeus.myontaja)

  @Test def testVirtatutkinnonKentat(): Unit =
    val suoritus = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData("""
          |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
          |  <SOAP-ENV:Body>
          |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
          |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
          |        <virta:Opiskelija avain="C10">
          |          <virta:Opiskeluoikeudet>
          |            <virta:Opiskeluoikeus opiskelijaAvain="C102" avain="xxx002">
          |              <virta:AlkuPvm>2018-01-01</virta:AlkuPvm>
          |              <virta:LoppuPvm>2019-01-01</virta:LoppuPvm>              
          |              <virta:Tila>
          |                <virta:AlkuPvm>2017-06-01</virta:AlkuPvm>
          |                <virta:Koodi>3</virta:Koodi>
          |              </virta:Tila>
          |              <virta:Jakso>
          |                <virta:Koulutuskoodi>726302</virta:Koulutuskoodi>
          |              </virta:Jakso>
          |            </virta:Opiskeluoikeus>
          |          </virta:Opiskeluoikeudet>
          |          <virta:Opintosuoritukset>
          |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx002" opiskelijaAvain="C10" koulutusmoduulitunniste="532" avain="cxxc7_532_R702T_TH00">
          |              <virta:SuoritusPvm>2017-05-31</virta:SuoritusPvm>
          |              <virta:Laajuus>
          |                <virta:Opintopiste>210.000000</virta:Opintopiste>
          |              </virta:Laajuus>
          |              <virta:Arvosana>
          |                <virta:EiKaytossa>Arvosana ei kaytossa</virta:EiKaytossa>
          |              </virta:Arvosana>
          |              <virta:Myontaja>10108</virta:Myontaja>
          |              <virta:Laji>1</virta:Laji>
          |              <virta:Nimi kieli="fi">Sosiaali- ja terveysalan ammattikorkeakoulututkinto</virta:Nimi>
          |              <virta:Nimi kieli="en">Bachelor of Health Care</virta:Nimi>
          |              <virta:Kieli>fi</virta:Kieli>
          |              <virta:Koulutuskoodi>671103</virta:Koulutuskoodi>
          |            </virta:Opintosuoritus>
          |          </virta:Opintosuoritukset>
          |        </virta:Opiskelija>
          |      </virta:Virta>
          |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
          |  </SOAP-ENV:Body>
          |</SOAP-ENV:Envelope>""".stripMargin)).head.suoritukset.head.asInstanceOf[VirtaTutkinto]

    Assertions.assertEquals("532", suoritus.komoTunniste)
    Assertions.assertEquals(LocalDate.parse("2017-05-31"), suoritus.suoritusPvm)
    Assertions.assertEquals(BigDecimal.valueOf(210.0000000), suoritus.opintoPisteet)
    Assertions.assertEquals("10108", suoritus.myontaja)
    Assertions.assertEquals(Some("Sosiaali- ja terveysalan ammattikorkeakoulututkinto"), suoritus.nimiFi)
    Assertions.assertEquals(Some("Bachelor of Health Care"), suoritus.nimiEn)
    Assertions.assertEquals("fi", suoritus.kieli)
    Assertions.assertEquals("671103", suoritus.koulutusKoodi)

  @Test def testVirtasuorituksenKentat(): Unit =
    val suoritus = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData("""
          |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
          |  <SOAP-ENV:Body>
          |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
          |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
          |        <virta:Opiskelija avain="C10">
          |          <virta:Opiskeluoikeudet>
          |            <virta:Opiskeluoikeus opiskelijaAvain="C102" avain="xxx002">
          |              <virta:AlkuPvm>2014-01-01</virta:AlkuPvm>
          |              <virta:LoppuPvm>2019-01-01</virta:LoppuPvm>              
          |              <virta:Tila>
          |                <virta:AlkuPvm>2017-06-01</virta:AlkuPvm>
          |                <virta:Koodi>3</virta:Koodi>
          |              </virta:Tila>
          |              <virta:Jakso>
          |                <virta:Koulutuskoodi>726302</virta:Koulutuskoodi>
          |              </virta:Jakso>
          |            </virta:Opiskeluoikeus>
          |          </virta:Opiskeluoikeudet>
          |          <virta:Opintosuoritukset>
          |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx002" opiskelijaAvain="C10" koulutusmoduulitunniste="LOG13A 01SUO" avain="625422">
          |              <virta:SuoritusPvm>2015-05-31</virta:SuoritusPvm>
          |              <virta:Laajuus>
          |                <virta:Opintopiste>4</virta:Opintopiste>
          |              </virta:Laajuus>
          |              <virta:Arvosana>
          |                <virta:Viisiportainen>HYV</virta:Viisiportainen>
          |              </virta:Arvosana>
          |              <virta:Myontaja>10108</virta:Myontaja>
          |              <virta:Organisaatio>
          |                <virta:Rooli>3</virta:Rooli>
          |                <virta:Koodi>XX</virta:Koodi>
          |                <virta:Osuus>1</virta:Osuus>
          |              </virta:Organisaatio>
          |              <virta:Laji>2</virta:Laji>
          |              <virta:Nimi kieli="fi">Asiantuntijaviestintä</virta:Nimi>
          |              <virta:Nimi kieli="en">Professional Communications</virta:Nimi>
          |              <virta:Kieli>fi</virta:Kieli>
          |              <virta:Koulutusala>
          |                <virta:Koodi versio="ohjausala">5</virta:Koodi>
          |              </virta:Koulutusala>
          |              <virta:HyvaksilukuPvm>2014-09-17</virta:HyvaksilukuPvm>
          |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
          |            </virta:Opintosuoritus>
          |          </virta:Opintosuoritukset>
          |        </virta:Opiskelija>
          |      </virta:Virta>
          |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
          |  </SOAP-ENV:Body>
          |</SOAP-ENV:Envelope>""".stripMargin)).head.suoritukset.head.asInstanceOf[Opintosuoritus]

    Assertions.assertEquals("LOG13A 01SUO", suoritus.komoTunniste)
    Assertions.assertEquals(LocalDate.parse("2015-05-31"), suoritus.suoritusPvm)
    Assertions.assertEquals(BigDecimal.valueOf(4), suoritus.opintoPisteet)
    Assertions.assertEquals(Some("HYV"), suoritus.arvosana)
    Assertions.assertEquals(Some("Viisiportainen"), suoritus.arvosanaAsteikko)
    Assertions.assertEquals("10108", suoritus.myontaja)

    Assertions.assertEquals("3", suoritus.jarjestavaRooli.get)
    Assertions.assertEquals("XX", suoritus.jarjestavaKoodi.get)
    Assertions.assertEquals(BigDecimal.valueOf(1), suoritus.jarjestavaOsuus.get)

    Assertions.assertEquals(Some("Asiantuntijaviestintä"), suoritus.nimiFi)
    Assertions.assertEquals(Some("Professional Communications"), suoritus.nimiEn)
    Assertions.assertEquals(None, suoritus.nimiSv)

    Assertions.assertEquals("fi", suoritus.kieli)

    Assertions.assertEquals(5, suoritus.koulutusala)
    Assertions.assertEquals("ohjausala", suoritus.koulutusalaKoodisto)

    Assertions.assertEquals(Some(LocalDate.parse("2014-09-17")), suoritus.hyvaksilukuPvm)
    Assertions.assertEquals(false, suoritus.opinnaytetyo)

  @Test def testVirtasuoritusMuuArvosana(): Unit =
    val suoritus = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData(
      """
        |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
        |  <SOAP-ENV:Body>
        |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
        |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
        |        <virta:Opiskelija avain="C10">
        |          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C102" avain="xxx002">
        |              <virta:AlkuPvm>2014-01-01</virta:AlkuPvm>
        |              <virta:LoppuPvm>2019-01-01</virta:LoppuPvm>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2017-06-01</virta:AlkuPvm>
        |                <virta:Koodi>3</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Jakso>
        |                <virta:Koulutuskoodi>726302</virta:Koulutuskoodi>
        |              </virta:Jakso>
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |          <virta:Opintosuoritukset>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx002" opiskelijaAvain="C10" koulutusmoduulitunniste="LOG13A 01SUO" avain="625422">
        |              <virta:SuoritusPvm>2015-05-31</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>4</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Muu>
        |                  <virta:Asteikko avain="11">
        |                    <virta:Nimi>Fail-Pass</virta:Nimi>
        |                    <virta:AsteikkoArvosana avain="10979859">
        |                      <virta:Koodi>HYV</virta:Koodi>
        |                      <virta:Nimi>Hyväksytty</virta:Nimi>
        |                      <virta:LaskennallinenArvo>2.0</virta:LaskennallinenArvo>
        |                    </virta:AsteikkoArvosana>
        |                  </virta:Asteikko>
        |                  <virta:Koodi>10979859</virta:Koodi>
        |                </virta:Muu>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10108</virta:Myontaja>
        |              <virta:Organisaatio>
        |                <virta:Rooli>3</virta:Rooli>
        |                <virta:Koodi>XX</virta:Koodi>
        |                <virta:Osuus>1</virta:Osuus>
        |              </virta:Organisaatio>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Asiantuntijaviestintä</virta:Nimi>
        |              <virta:Nimi kieli="en">Professional Communications</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">5</virta:Koodi>
        |              </virta:Koulutusala>
        |              <virta:HyvaksilukuPvm>2014-09-17</virta:HyvaksilukuPvm>
        |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>
        |        </virta:Opiskelija>
        |      </virta:Virta>
        |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
        |  </SOAP-ENV:Body>
        |</SOAP-ENV:Envelope>""".stripMargin)).head.suoritukset.head.asInstanceOf[Opintosuoritus]

    Assertions.assertEquals(Some("Hyväksytty"), suoritus.arvosana)
    Assertions.assertEquals(Some("Fail-Pass"), suoritus.arvosanaAsteikko)
}
