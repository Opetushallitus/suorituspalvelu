package fi.oph.suorituspalvelu.business.parsing.virta

import fi.oph.suorituspalvelu.business.{AmmatillinenPerustutkinto, Koodi, VirtaOpintosuoritus, VirtaOpiskeluoikeus, VirtaTutkinto}
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
          |                <virta:Viisiportainen>1</virta:Viisiportainen>
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
          |</SOAP-ENV:Envelope>""".stripMargin)).head.suoritukset.head.asInstanceOf[VirtaOpintosuoritus]

    Assertions.assertEquals("LOG13A 01SUO", suoritus.komoTunniste)
    Assertions.assertEquals(LocalDate.parse("2015-05-31"), suoritus.suoritusPvm)
    Assertions.assertEquals(BigDecimal.valueOf(4), suoritus.opintoPisteet)
    Assertions.assertEquals(Some("1"), suoritus.arvosana)
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
        |</SOAP-ENV:Envelope>""".stripMargin)).head.suoritukset.head.asInstanceOf[VirtaOpintosuoritus]

    Assertions.assertEquals(Some("Hyväksytty"), suoritus.arvosana)
    Assertions.assertEquals(Some("Fail-Pass"), suoritus.arvosanaAsteikko)

  @Test def testSuorituksetHierarkia(): Unit = {
    val suoritukset = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData(
      """
        |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
        |  <SOAP-ENV:Body>
        |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
        |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
        |        <virta:Opiskelija avain="TY¤75094">
        |          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus avain="TY¤75094¤123049¤A" opiskelijaAvain="TY¤75094">
        |              <virta:AlkuPvm>2007-08-01</virta:AlkuPvm>
        |              <virta:LoppuPvm>2010-09-20</virta:LoppuPvm>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2007-08-01</virta:AlkuPvm>
        |                <virta:LoppuPvm>2010-09-20</virta:LoppuPvm>
        |                <virta:Koodi>1</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2010-09-21</virta:AlkuPvm>
        |                <virta:Koodi>3</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tyyppi>2</virta:Tyyppi>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Jakso koulutusmoduulitunniste="">
        |                <virta:Koulutuskoodi>623404</virta:Koulutuskoodi>
        |              </virta:Jakso>
        |              <virta:Jakso koulutusmoduulitunniste="">
        |                <virta:Koulutuskoodi>623404</virta:Koulutuskoodi>
        |              </virta:Jakso>
        |              <virta:Koulutusala versio="ohjausala">3</virta:Koulutusala>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>180.000000</virta:Opintopiste>
        |              </virta:Laajuus>
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |          <virta:Opintosuoritukset>
        |            <virta:Opintosuoritus avain="TY¤75094¤24472" koulutusmoduulitunniste="TUTK2133" opiskelijaAvain="TY¤75094" opiskeluoikeusAvain="TY¤75094¤123049¤A">
        |              <virta:SuoritusPvm>2010-09-20</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>181.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Hyvaksytty>HYV</virta:Hyvaksytty>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>1</virta:Laji>
        |              <virta:Nimi kieli="fi">HUMANISTISTEN TIETEIDEN KANDIDAATTI, YHTEISKUNTATIETEELLINEN TIEDEKUNTA</virta:Nimi>
        |              <virta:Nimi kieli="en">Bachelor of Arts </virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutuskoodi>623404</virta:Koulutuskoodi>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="TY¤75094¤22545">
        |                <virta:Opintopiste>105.0</virta:Opintopiste>
        |              </virta:Sisaltyvyys>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus avain="TY¤75094¤22545" koulutusmoduulitunniste="LOGO1001" opiskelijaAvain="TY¤75094">
        |              <virta:SuoritusPvm>2010-05-21</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>105.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>4</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">LOGOPEDIAN PERUS- JA AINEOPINNOT</virta:Nimi>
        |              <virta:Nimi kieli="en">Logopedics, Basic and Intermediate Studies </virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">3</virta:Koodi>
        |              </virta:Koulutusala>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="TY¤75094¤14781">
        |                <virta:Opintopiste>25.0</virta:Opintopiste>
        |              </virta:Sisaltyvyys>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus avain="TY¤75094¤14781" koulutusmoduulitunniste="LOGO1000" opiskelijaAvain="TY¤75094">
        |              <virta:SuoritusPvm>2008-06-16</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>25.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>4</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">LOGOPEDIAN PERUSOPINNOT</virta:Nimi>
        |              <virta:Nimi kieli="en">Logopedics, Basic Studies</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">3</virta:Koodi>
        |              </virta:Koulutusala>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="TY¤75094¤14791">
        |                <virta:Opintopiste>3.0</virta:Opintopiste>
        |              </virta:Sisaltyvyys>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="TY¤75094¤14793">
        |                <virta:Opintopiste>3.0</virta:Opintopiste>
        |              </virta:Sisaltyvyys>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus avain="TY¤75094¤14791" koulutusmoduulitunniste="LOGO1250" opiskelijaAvain="TY¤75094">
        |              <virta:SuoritusPvm>2008-04-29</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>3.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Hyvaksytty>HYV</virta:Hyvaksytty>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">KIELEN JA PUHEEN KEHITYKSEN TUKEMINEN</virta:Nimi>
        |              <virta:Nimi kieli="en">Supporting Language and Speech Development </virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">3</virta:Koodi>
        |              </virta:Koulutusala>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus avain="TY¤75094¤14793" koulutusmoduulitunniste="LOGO1400" opiskelijaAvain="TY¤75094">
        |              <virta:SuoritusPvm>2008-02-28</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>3.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>4</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">PUHETTA TUKEVAT JA KORVAAVAT KEINOT KOMMUNIKOINNISSA</virta:Nimi>
        |              <virta:Nimi kieli="en">Augmentative and Alternative Communication Methods</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">3</virta:Koodi>
        |              </virta:Koulutusala>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>
        |        </virta:Opiskelija>
        |      </virta:Virta>
        |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
        |  </SOAP-ENV:Body>
        |</SOAP-ENV:Envelope>""".stripMargin)).head.suoritukset

    val tutkinnot = suoritukset.filter(_.isInstanceOf[VirtaTutkinto]).map(_.asInstanceOf[VirtaTutkinto]).toSeq
    Assertions.assertEquals(tutkinnot.length, 1)
    val onlyTutkinto = tutkinnot.head
    Assertions.assertEquals(onlyTutkinto.opiskeluoikeusAvain, "TY¤75094¤123049¤A")
    Assertions.assertEquals(onlyTutkinto.nimiFi.get, "HUMANISTISTEN TIETEIDEN KANDIDAATTI, YHTEISKUNTATIETEELLINEN TIEDEKUNTA")
    Assertions.assertEquals(onlyTutkinto.osaSuoritusAvaimet.length, 1)
    Assertions.assertEquals(onlyTutkinto.osaSuoritusAvaimet.head, "TY¤75094¤22545")

    val opintosuoritukset = suoritukset.filter(_.isInstanceOf[VirtaOpintosuoritus]).map(_.asInstanceOf[VirtaOpintosuoritus]).toSeq
    Assertions.assertTrue(opintosuoritukset.forall(_.opiskeluoikeusAvain == "TY¤75094¤123049¤A"))
    Assertions.assertEquals(opintosuoritukset.length, 4)
    val osaSuoritus1 = opintosuoritukset.find(s => s.avain == "TY¤75094¤22545").get
    Assertions.assertEquals(osaSuoritus1.nimiFi.get, "LOGOPEDIAN PERUS- JA AINEOPINNOT")
    Assertions.assertEquals(osaSuoritus1.osaSuoritusAvaimet.length, 1)
    Assertions.assertEquals(osaSuoritus1.osaSuoritusAvaimet.head, "TY¤75094¤14781")
    val osaSuoritus2 = opintosuoritukset.find(s => s.avain == "TY¤75094¤14781").get
    Assertions.assertEquals(osaSuoritus2.nimiFi.get, "LOGOPEDIAN PERUSOPINNOT")
    Assertions.assertEquals(osaSuoritus2.osaSuoritusAvaimet.length, 2)
    Assertions.assertTrue(osaSuoritus2.osaSuoritusAvaimet.contains("TY¤75094¤14791"))
    Assertions.assertTrue(osaSuoritus2.osaSuoritusAvaimet.contains("TY¤75094¤14793"))
    val osaSuoritus3 = opintosuoritukset.find(s => s.avain == "TY¤75094¤14791").get
    Assertions.assertEquals(osaSuoritus3.nimiFi.get, "KIELEN JA PUHEEN KEHITYKSEN TUKEMINEN")
    Assertions.assertEquals(osaSuoritus3.osaSuoritusAvaimet.length, 0)
    val osaSuoritus4 = opintosuoritukset.find(s => s.avain == "TY¤75094¤14793").get
    Assertions.assertEquals(osaSuoritus4.nimiFi.get, "PUHETTA TUKEVAT JA KORVAAVAT KEINOT KOMMUNIKOINNISSA")
    Assertions.assertEquals(osaSuoritus4.osaSuoritusAvaimet.length, 0)
  }
}
