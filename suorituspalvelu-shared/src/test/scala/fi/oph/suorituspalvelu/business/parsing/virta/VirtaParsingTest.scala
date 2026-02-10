package fi.oph.suorituspalvelu.business.parsing.virta

import fi.oph.suorituspalvelu.business.{KKOpintosuoritus, KKOpiskeluoikeus, KKSynteettinenSuoritus, KKTutkinto, Koodi}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

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
        |                <virta:AlkuPvm>2018-01-01</virta:AlkuPvm>
        |                <virta:LoppuPvm>2019-01-01</virta:LoppuPvm>
        |              </virta:Jakso>
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |        </virta:Opiskelija>
        |      </virta:Virta>
        |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
        |  </SOAP-ENV:Body>
        |</SOAP-ENV:Envelope>""".stripMargin)).head.asInstanceOf[KKOpiskeluoikeus]

    Assertions.assertNotNull(opiskeluoikeus.tunniste)
    Assertions.assertEquals("xxx002", opiskeluoikeus.virtaTunniste)
    Assertions.assertEquals(Some("726302"), opiskeluoikeus.koulutusKoodi)
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
          |                <virta:AlkuPvm>2018-01-01</virta:AlkuPvm>
          |                <virta:LoppuPvm>2019-01-01</virta:LoppuPvm>
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
          |</SOAP-ENV:Envelope>""".stripMargin)).head.asInstanceOf[KKOpiskeluoikeus].suoritukset.head.asInstanceOf[KKTutkinto]

    Assertions.assertEquals("532", suoritus.komoTunniste)
    Assertions.assertEquals(Some(LocalDate.parse("2017-05-31")), suoritus.suoritusPvm)
    Assertions.assertEquals(BigDecimal.valueOf(210.0000000), suoritus.opintoPisteet)
    Assertions.assertEquals("10108", suoritus.myontaja)
    Assertions.assertEquals(Some("Sosiaali- ja terveysalan ammattikorkeakoulututkinto"), suoritus.nimi.flatMap(_.fi))
    Assertions.assertEquals(Some("Bachelor of Health Care"), suoritus.nimi.flatMap(_.en))
    Assertions.assertEquals(Some("fi"), suoritus.kieli)
    Assertions.assertEquals(Some("671103"), suoritus.koulutusKoodi)

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
          |                <virta:AlkuPvm>2014-01-01</virta:AlkuPvm>
          |                <virta:LoppuPvm>2019-01-01</virta:LoppuPvm>
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
          |</SOAP-ENV:Envelope>""".stripMargin)).asInstanceOf[Seq[KKOpiskeluoikeus]].head.suoritukset.head.asInstanceOf[KKOpintosuoritus]

    Assertions.assertEquals("LOG13A 01SUO", suoritus.komoTunniste)
    Assertions.assertEquals(Some(LocalDate.parse("2015-05-31")), suoritus.suoritusPvm)
    Assertions.assertEquals(BigDecimal.valueOf(4), suoritus.opintoPisteet)
    Assertions.assertEquals(Some("1"), suoritus.arvosana)
    Assertions.assertEquals(Some("Viisiportainen"), suoritus.arvosanaAsteikko)
    Assertions.assertEquals("10108", suoritus.myontaja)

    Assertions.assertEquals("3", suoritus.jarjestavaRooli.get)
    Assertions.assertEquals("XX", suoritus.jarjestavaKoodi.get)
    Assertions.assertEquals(BigDecimal.valueOf(1), suoritus.jarjestavaOsuus.get)

    Assertions.assertEquals(Some("Asiantuntijaviestintä"), suoritus.nimi.flatMap(_.fi))
    Assertions.assertEquals(Some("Professional Communications"), suoritus.nimi.flatMap(_.en))
    Assertions.assertEquals(None, suoritus.nimi.flatMap(_.sv))

    Assertions.assertEquals("fi", suoritus.kieli)

    Assertions.assertEquals(Some(5), suoritus.koulutusala)
    Assertions.assertEquals(Some("ohjausala"), suoritus.koulutusalaKoodisto)

    Assertions.assertEquals(Some(LocalDate.parse("2014-09-17")), suoritus.hyvaksilukuPvm)
    Assertions.assertEquals(false, suoritus.opinnaytetyo)

  @Test def testVirtasuoritusMuuArvosana(): Unit =
    val suoritukset = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData(
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
        |              <virta:Jakso koulutusmoduulitunniste="">
        |                <virta:Koulutuskoodi>726302</virta:Koulutuskoodi>
        |                <virta:AlkuPvm>2014-01-01</virta:AlkuPvm>
        |                <virta:LoppuPvm>2019-01-01</virta:LoppuPvm>
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
        |                    <virta:AsteikkoArvosana avain="10979858">
        |                      <virta:Koodi>HYL</virta:Koodi>
        |                      <virta:Nimi>Hylätty</virta:Nimi>
        |                      <virta:LaskennallinenArvo>1.0</virta:LaskennallinenArvo>
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
        |</SOAP-ENV:Envelope>""".stripMargin)).asInstanceOf[Seq[KKOpiskeluoikeus]].head.suoritukset

    Assertions.assertEquals(1, suoritukset.size)
    val suoritus = suoritukset.head.asInstanceOf[KKOpintosuoritus]
    Assertions.assertEquals(Some("Hyväksytty"), suoritus.arvosana)
    Assertions.assertEquals(Some("Fail-Pass"), suoritus.arvosanaAsteikko)

  @Test def testSuorituksetHierarkia(): Unit = {
    // Varmistetaan että Virta-datasta muodostuu vastaavanlainen monitasoinen suoritushierarkia kuin Virrasta
    // Taso 1: Tutkinto (HUMANISTISTEN TIETEIDEN KANDIDAATTI)
    // Taso 2: Opintosuoritus (LOGOPEDIAN PERUS- JA AINEOPINNOT, 105 op)
    // Taso 3: Opintosuoritus (LOGOPEDIAN PERUSOPINNOT, 25 op)
    // Taso 4: Kaksi opintosuoritusta (yksittäiset opintojaksot, 3 op kumpikin)
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
        |                <virta:AlkuPvm>2007-08-01</virta:AlkuPvm>
        |                <virta:LoppuPvm>2010-09-20</virta:LoppuPvm>
        |              </virta:Jakso>
        |              <virta:Jakso koulutusmoduulitunniste="">
        |                <virta:Koulutuskoodi>623404</virta:Koulutuskoodi>
        |                <virta:AlkuPvm>2010-09-20</virta:AlkuPvm>
        |                <virta:LoppuPvm>2011-09-20</virta:LoppuPvm>
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
        |</SOAP-ENV:Envelope>""".stripMargin)).asInstanceOf[Seq[KKOpiskeluoikeus]].head.suoritukset

    Assertions.assertEquals(suoritukset.toSeq.length, 1)
    val onlyFirstLevelSuoritus = suoritukset.head

    Assertions.assertTrue(onlyFirstLevelSuoritus.isInstanceOf[KKTutkinto])
    val onlyTutkinto = onlyFirstLevelSuoritus.asInstanceOf[KKTutkinto]
    Assertions.assertEquals(Some("TY¤75094¤123049¤A"), onlyTutkinto.opiskeluoikeusAvain)
    Assertions.assertEquals(Some("HUMANISTISTEN TIETEIDEN KANDIDAATTI, YHTEISKUNTATIETEELLINEN TIEDEKUNTA"), onlyTutkinto.nimi.flatMap(_.fi))
    val secondLevelSuoritukset = onlyTutkinto.suoritukset
    Assertions.assertEquals(1, secondLevelSuoritukset.length)
    val onlySecondLevelSuoritus = secondLevelSuoritukset.head.asInstanceOf[KKOpintosuoritus]
    Assertions.assertEquals(Some("LOGOPEDIAN PERUS- JA AINEOPINNOT"), onlySecondLevelSuoritus.nimi.flatMap(_.fi))
    val thirdLevelSuoritukset = onlySecondLevelSuoritus.suoritukset.map(_.asInstanceOf[KKOpintosuoritus])

    Assertions.assertEquals(1, thirdLevelSuoritukset.length)
    val onlyThirdLevelSuoritus = thirdLevelSuoritukset.head

    Assertions.assertEquals(Some("LOGOPEDIAN PERUSOPINNOT"), onlyThirdLevelSuoritus.nimi.flatMap(_.fi))
    val fourthLevelSuoritukset = onlyThirdLevelSuoritus.suoritukset.map(_.asInstanceOf[KKOpintosuoritus])
    Assertions.assertEquals(2, fourthLevelSuoritukset.length)
    val fourthLevelSuoritus1 = fourthLevelSuoritukset.find(_.avain == "TY¤75094¤14791").get
    Assertions.assertEquals(Some("KIELEN JA PUHEEN KEHITYKSEN TUKEMINEN"), fourthLevelSuoritus1.nimi.flatMap(_.fi))
    Assertions.assertEquals(0, fourthLevelSuoritus1.suoritukset.length)
    val fourthLevelSuoritus2 = fourthLevelSuoritukset.find(_.avain == "TY¤75094¤14793").get
    Assertions.assertEquals(Some("PUHETTA TUKEVAT JA KORVAAVAT KEINOT KOMMUNIKOINNISSA"), fourthLevelSuoritus2.nimi.flatMap(_.fi))
    Assertions.assertEquals(0, fourthLevelSuoritus2.suoritukset.length)
  }

  @Test def testMoveOpintojaksotUnderOnlyTutkinto(): Unit = {
    // Testi varmistaa, että irralliset opintojaksot siirretään ainoan tutkinnon alle:
    // Virta-datassa tutkinto ja opintojaksot ovat samalla tasolla ilman sisältyvyysviittauksia,
    // mutta ne muunnetaan hierarkiseksi rakenteeksi:
    // Taso 1: Tutkinto (Kasvatustieteiden kandidaatti, 180 op)
    // Taso 2: Kaksi opintosuoritusta (MAT201 "Analyysi I" 5 op, FYS101 "Fysiikan perusteet" 10 op)
    val opiskeluoikeus = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData(
      """
        |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
        |  <SOAP-ENV:Body>
        |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
        |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
        |        <virta:Opiskelija avain="C10">
        |          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C102" avain="xxx004">
        |              <virta:AlkuPvm>2019-08-01</virta:AlkuPvm>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2019-08-01</virta:AlkuPvm>
        |                <virta:Koodi>1</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tyyppi>1</virta:Tyyppi>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Jakso>
        |                <virta:Koulutuskoodi>751101</virta:Koulutuskoodi>
        |                <virta:AlkuPvm>2019-08-01</virta:AlkuPvm>
        |              </virta:Jakso>
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |          <virta:Opintosuoritukset>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx004" opiskelijaAvain="C10" koulutusmoduulitunniste="751101" avain="op001">
        |              <virta:SuoritusPvm>2022-06-15</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>180.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>3</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>1</virta:Laji>
        |              <virta:Koulutuskoodi>751101</virta:Koulutuskoodi>
        |              <virta:Nimi kieli="fi">Kasvatustieteiden kandidaatti</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">1</virta:Koodi>
        |              </virta:Koulutusala>
        |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx004" opiskelijaAvain="C10" koulutusmoduulitunniste="MAT201" avain="op002">
        |              <virta:SuoritusPvm>2020-05-31</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>5.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>4</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Analyysi I</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">1</virta:Koodi>
        |              </virta:Koulutusala>
        |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx004" opiskelijaAvain="C10" koulutusmoduulitunniste="FYS101" avain="op003">
        |              <virta:SuoritusPvm>2020-12-20</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>10.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>5</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Fysiikan perusteet</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">1</virta:Koodi>
        |              </virta:Koulutusala>
        |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>
        |        </virta:Opiskelija>
        |      </virta:Virta>
        |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
        |  </SOAP-ENV:Body>
        |</SOAP-ENV:Envelope>""".stripMargin)).head.asInstanceOf[KKOpiskeluoikeus]

    Assertions.assertEquals(1, opiskeluoikeus.suoritukset.size)
    val tutkinto = opiskeluoikeus.suoritukset.head.asInstanceOf[KKTutkinto]

    Assertions.assertEquals(Some("751101"), tutkinto.koulutusKoodi)
    Assertions.assertEquals(Some(LocalDate.parse("2022-06-15")), tutkinto.suoritusPvm)
    Assertions.assertEquals("10089", tutkinto.myontaja)

    Assertions.assertEquals(2, tutkinto.suoritukset.size)
    val opintojaksot = tutkinto.suoritukset.map(_.asInstanceOf[KKOpintosuoritus])

    val mat201 = opintojaksot.find(_.komoTunniste == "MAT201").get
    Assertions.assertEquals(Some("Analyysi I"), mat201.nimi.flatMap(_.fi))
    Assertions.assertEquals(Some(LocalDate.parse("2020-05-31")), mat201.suoritusPvm)

    val fys101 = opintojaksot.find(_.komoTunniste == "FYS101").get
    Assertions.assertEquals(Some("Fysiikan perusteet"), fys101.nimi.flatMap(_.fi))
    Assertions.assertEquals(Some(LocalDate.parse("2020-12-20")), fys101.suoritusPvm)
  }

  @Test def testAddSyntheticKeskenrainenTutkinnonSuoritus(): Unit = {
    // Testi varmistaa, että aktiiviselle opiskeluoikeudelle luodaan synteettinen keskeneräinen tutkinto,
    // kun Virta-datassa on opiskeluoikeus koulutuskoodilla mutta ei valmistunutta tutkinto-suoritusta.
    // Opintojaksot siirretään synteettisen tutkinnon alle:
    // Taso 1: Synteettinen kesken-tutkinto (751101, ei suorituspäivää, aloituspvm 2020-08-01)
    // Taso 2: Yksi opintosuoritus (MAT101 "Matematiikan perusteet" 5 op)
    val opiskeluoikeus = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData(
      """
        |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
        |  <SOAP-ENV:Body>
        |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
        |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
        |        <virta:Opiskelija avain="C10">
        |          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C10" avain="xxx003">
        |              <virta:AlkuPvm>2020-08-01</virta:AlkuPvm>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2020-08-01</virta:AlkuPvm>
        |                <virta:Koodi>1</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tyyppi>1</virta:Tyyppi>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Jakso>
        |                <virta:Koulutuskoodi>751101</virta:Koulutuskoodi>
        |                <virta:AlkuPvm>2020-08-01</virta:AlkuPvm>
        |              </virta:Jakso>
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |          <virta:Opintosuoritukset>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx003" opiskelijaAvain="C10" koulutusmoduulitunniste="MAT101" avain="op001">
        |              <virta:SuoritusPvm>2021-05-31</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>5.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>3</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Matematiikan perusteet</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">1</virta:Koodi>
        |              </virta:Koulutusala>
        |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>
        |        </virta:Opiskelija>
        |      </virta:Virta>
        |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
        |  </SOAP-ENV:Body>
        |</SOAP-ENV:Envelope>""".stripMargin)).head.asInstanceOf[KKOpiskeluoikeus]

    Assertions.assertEquals(1, opiskeluoikeus.suoritukset.size)
    val suoritus = opiskeluoikeus.suoritukset.head.asInstanceOf[KKSynteettinenSuoritus]

    Assertions.assertEquals("751101", suoritus.komoTunniste)
    Assertions.assertEquals(Some("751101"), suoritus.koulutusKoodi)
    Assertions.assertEquals(Some(LocalDate.parse("2020-08-01")), suoritus.aloitusPvm)
    Assertions.assertEquals(None, suoritus.suoritusPvm)
    Assertions.assertEquals("10089", suoritus.myontaja)
    Assertions.assertEquals(Some("xxx003"), suoritus.opiskeluoikeusAvain)

    val osaSuoritukset = suoritus.suoritukset
    Assertions.assertEquals(1, osaSuoritukset.size)
    val opintojakso = osaSuoritukset.head.asInstanceOf[KKOpintosuoritus]
    Assertions.assertEquals("MAT101", opintojakso.komoTunniste)
  }

  @Test def testTutkintoonJohtavaPaattynytWithSuoritukset(): Unit = {
    // Varmistetaan, että päättyneelle tutkintoon johtavalle opiskeluoikeudelle ei luoda synteettistä suoritusta,
    // kun Virta-datassa on Jakso-elementissä koulutuskoodi, mutta ei valmistunutta tutkintoa.
    // - Opiskeluoikeus on tutkintoon johtava (tyyppi=1)
    // - Opiskeluoikeus on päättynyt (tila=3)
    // - Jakso sisältää koulutuskoodin
    // - Löytyy kaksi suoritusta, jotka eivät ole tutkintoja, eivätkä niillä ole koulutuskoodia
    // Taso 1: Kaksi opintosuoritusta (BIO102 "Biologian perusteet" 5 op ja MAT101 "Matematiikan perusteet" 10 op)
    val opiskeluoikeus = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData(
      """
        |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
        |  <SOAP-ENV:Body>
        |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
        |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
        |        <virta:Opiskelija avain="C13">
        |          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C13" avain="xxx007">
        |              <virta:AlkuPvm>2019-08-01</virta:AlkuPvm>
        |              <virta:LoppuPvm>2021-12-31</virta:LoppuPvm>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2019-08-01</virta:AlkuPvm>
        |                <virta:Koodi>1</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2021-12-31</virta:AlkuPvm>
        |                <virta:Koodi>3</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tyyppi>1</virta:Tyyppi>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Jakso>
        |                <virta:Koulutuskoodi>751101</virta:Koulutuskoodi>
        |                <virta:AlkuPvm>2019-08-01</virta:AlkuPvm>
        |                <virta:Nimi kieli="fi">Kasvatustieteiden kandidaatti</virta:Nimi>
        |              </virta:Jakso>
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |          <virta:Opintosuoritukset>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx007" opiskelijaAvain="C13" koulutusmoduulitunniste="BIO102" avain="op301">
        |              <virta:SuoritusPvm>2020-05-15</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>5.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>4</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Biologian perusteet</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">1</virta:Koodi>
        |              </virta:Koulutusala>
        |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx007" opiskelijaAvain="C13" koulutusmoduulitunniste="MAT101" avain="op302">
        |              <virta:SuoritusPvm>2020-09-20</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>10.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>5</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Matematiikan perusteet</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">1</virta:Koodi>
        |              </virta:Koulutusala>
        |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>
        |        </virta:Opiskelija>
        |      </virta:Virta>
        |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
        |  </SOAP-ENV:Body>
        |</SOAP-ENV:Envelope>""".stripMargin)).head.asInstanceOf[KKOpiskeluoikeus]

    Assertions.assertEquals(2, opiskeluoikeus.suoritukset.size)
    val suoritusList = opiskeluoikeus.suoritukset.toList
    val suoritus1 = suoritusList.head.asInstanceOf[KKOpintosuoritus]

    Assertions.assertEquals(Some(Kielistetty(Some("Biologian perusteet"),None,None)), suoritus1.nimi)
    Assertions.assertEquals(Some(LocalDate.parse("2020-05-15")), suoritus1.suoritusPvm)
    Assertions.assertEquals("10089", suoritus1.myontaja)
    Assertions.assertEquals(Some("xxx007"), suoritus1.opiskeluoikeusAvain)
    Assertions.assertEquals(0, suoritus1.suoritukset.size)

    val suoritus2 = suoritusList(1).asInstanceOf[KKOpintosuoritus]

    Assertions.assertEquals(Some(Kielistetty(Some("Matematiikan perusteet"),None,None)), suoritus2.nimi)
    Assertions.assertEquals(Some(LocalDate.parse("2020-09-20")), suoritus2.suoritusPvm)
    Assertions.assertEquals("10089", suoritus2.myontaja)
    Assertions.assertEquals(Some("xxx007"), suoritus2.opiskeluoikeusAvain)
    Assertions.assertEquals(0, suoritus2.suoritukset.size)
  }

  @Test def testTutkintoonJohtavaPaattnynytNoSuoritukset(): Unit = {
    // Varmistetaan, että päättyneelle tutkintoon johtavalle opiskeluoikeudelle luodaan synteettinen suoritus,
    // kun opiskelukoikeudella ei ole lainkaan suorituksia.
    // - Opiskeluoikeus on tutkintoon johtava (tyyppi=1)
    // - Opiskeluoikeus on päättynyt (tila=3)
    // - Opiskeluoikeudella ei ole suorituksia
    // Taso 1: Yksi synteettinen suoritus
    val opiskeluoikeus = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData(
      """
        |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
        |  <SOAP-ENV:Body>
        |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
        |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
        |        <virta:Opiskelija avain="C13">
        |          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C13" avain="xxx007">
        |              <virta:AlkuPvm>2019-08-01</virta:AlkuPvm>
        |              <virta:LoppuPvm>2021-12-31</virta:LoppuPvm>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2019-08-01</virta:AlkuPvm>
        |                <virta:Koodi>1</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2021-12-31</virta:AlkuPvm>
        |                <virta:Koodi>3</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tyyppi>1</virta:Tyyppi>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |        </virta:Opiskelija>
        |      </virta:Virta>
        |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
        |  </SOAP-ENV:Body>
        |</SOAP-ENV:Envelope>""".stripMargin)).head.asInstanceOf[KKOpiskeluoikeus]

    Assertions.assertEquals(1, opiskeluoikeus.suoritukset.size)
    val suoritus = opiskeluoikeus.suoritukset.head.asInstanceOf[KKSynteettinenSuoritus]

    Assertions.assertEquals(None, suoritus.nimi) // Nimi on None kun koulutuskoodi on määritelty
    Assertions.assertEquals(Some(LocalDate.parse("2021-12-31")), suoritus.suoritusPvm)
    Assertions.assertEquals("10089", suoritus.myontaja)
    Assertions.assertEquals(Some("xxx007"), suoritus.opiskeluoikeusAvain)
    Assertions.assertEquals(0, suoritus.suoritukset.size)
  }

  @Test def testAddSyntheticWrapperForAvoinYliopistoOpintojakso(): Unit = {
    // Varmistetaan, että päättyneelle ei-tutkintoon johtavalle "Avoimen opinnot"-tyyppiselle (13) opiskeluoikeudelle luodaan synteettinen suoritus.
    // Virta-datassa on päättynyt opiskeluoikeus, jolle opintojaksot sisällytetään osasuorituksina (tyyppi=13, ei tutkintoon johtava, päättynyt 2021-12-31)
    // Virta-datassa vain yksi suoritus, mutta se ei ole tutkinto, joten luodaan synteettinen suoritus:
    // Taso 1: Synteettinen suoritus (ei koulutuskoodia, nimi "Kasvatustiede" opiskeluoikeudesta, ei suorituspäivää)
    // Taso 2: Yksi opintosuoritus (PSY101 "Psykologian perusteet" 10 op)
    val opiskeluoikeus = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaData(
      """
        |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
        |  <SOAP-ENV:Body>
        |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
        |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
        |        <virta:Opiskelija avain="C12">
        |          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C121" avain="xxx006">
        |              <virta:AlkuPvm>2018-08-01</virta:AlkuPvm>
        |              <virta:LoppuPvm>2021-12-31</virta:LoppuPvm>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2018-08-01</virta:AlkuPvm>
        |                <virta:Koodi>1</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2021-12-31</virta:AlkuPvm>
        |                <virta:Koodi>4</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tyyppi>13</virta:Tyyppi>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Jakso>
        |                <virta:Koulutuskoodi>751101</virta:Koulutuskoodi>
        |                <virta:AlkuPvm>2018-08-01</virta:AlkuPvm>
        |                <virta:Nimi kieli="fi">Kasvatustiede</virta:Nimi>
        |              </virta:Jakso>
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |          <virta:Opintosuoritukset>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx006" opiskelijaAvain="C12" koulutusmoduulitunniste="PSY101" avain="op201">
        |              <virta:SuoritusPvm>2019-05-30</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>10.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>3</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Psykologian perusteet</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala>
        |                <virta:Koodi versio="ohjausala">1</virta:Koodi>
        |              </virta:Koulutusala>
        |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>
        |        </virta:Opiskelija>
        |      </virta:Virta>
        |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
        |  </SOAP-ENV:Body>
        |</SOAP-ENV:Envelope>""".stripMargin)).head.asInstanceOf[KKOpiskeluoikeus]

    Assertions.assertEquals(1, opiskeluoikeus.suoritukset.size)
    val synteettinen = opiskeluoikeus.suoritukset.head.asInstanceOf[KKSynteettinenSuoritus]

    Assertions.assertEquals(None, synteettinen.koulutusKoodi) // Opiskeluoikeus ei tutkintoon johtava
    Assertions.assertEquals(Some(Kielistetty(Some("Kasvatustiede"), None, None)), synteettinen.nimi)
    Assertions.assertEquals(Some(LocalDate.parse("2018-08-01")), synteettinen.aloitusPvm)
    Assertions.assertEquals(None, synteettinen.suoritusPvm)
    Assertions.assertEquals("10089", synteettinen.myontaja)
    Assertions.assertEquals(Some("xxx006"), synteettinen.opiskeluoikeusAvain)

    Assertions.assertEquals(1, synteettinen.suoritukset.size)
    val opintojakso = synteettinen.suoritukset.head.asInstanceOf[KKOpintosuoritus]
    Assertions.assertEquals("PSY101", opintojakso.komoTunniste)
    Assertions.assertEquals(Some("Psykologian perusteet"), opintojakso.nimi.flatMap(_.fi))
  }
}


