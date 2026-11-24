package fi.oph.suorituspalvelu.ovara

import fi.oph.suorituspalvelu.business.{KKOpintosuoritus, KKOpiskeluoikeus, KKSynteettinenOpiskeluoikeus, KKSynteettinenSuoritus, KKTutkinto, Suoritus}
import fi.oph.suorituspalvelu.ovara.OvaraLitistysTestUtil.{litistaVirtaXml, siirtotiedostonJson, tarkistaLitistyksenInvariantit, virtaXml}
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

import java.time.{Instant, LocalDate}

/**
 * End-to-end -testit Virta-XML:stä litistettyihin Ovara-KK-suorituksiin. Skenaariot vastaavat VirtaParsingTestin
 * reunatapauksia, joissa Virta-konversio muokkaa suorituspuun rakennetta.
 *
 * Juuritason suoritusten järjestys ei ole määrätty (opiskeluoikeuden suoritukset ovat Set), joten rivit haetaan
 * Virta-avaimella tai koulutusmoduulitunnisteella eikä sijainnin perusteella.
 */
@TestInstance(Lifecycle.PER_CLASS)
class VirtaToOvaraLitistysTest {

  private val META = OvaraVersioMetadata(
    lahdejarjestelma = "VIRTA",
    lahdeTunniste = "virta-lt",
    lahdeVersio = None,
    parserVersio = Some(10),
    luontiHetki = Some(Instant.parse("2024-01-01T00:00:00Z")),
    paivitysHetki = None,
    parserointiHetki = Some(Instant.parse("2024-01-02T00:00:00Z"))
  )

  private def litista(xml: String): Seq[OvaraLitistettyKKSuoritus] = {
    val (_, _, rivit) = litistaVirtaXml(xml, META)
    tarkistaLitistyksenInvariantit(rivit)
    Assertions.assertTrue(rivit.forall(_.metadata == META))
    rivit
  }

  private def avaimella(rivit: Seq[OvaraLitistettyKKSuoritus], avain: String): OvaraLitistettyKKSuoritus =
    rivit.find(_.avain.contains(avain)).getOrElse(Assertions.fail(s"Riviä avaimella $avain ei löydy"))

  private def komolla(rivit: Seq[OvaraLitistettyKKSuoritus], komo: String): OvaraLitistettyKKSuoritus =
    rivit.find(_.komoTunniste == komo).getOrElse(Assertions.fail(s"Riviä komolla $komo ei löydy"))

  @Test def testSyvaHierarkiaSisaltyvyyksista(): Unit = {
    // Tutkinto -> perus- ja aineopinnot -> perusopinnot -> kaksi opintojaksoa (ks. VirtaParsingTest.testSuorituksetHierarkia)
    val rivit = litista(virtaXml("TY¤75094",
      """          <virta:Opiskeluoikeudet>
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
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |          <virta:Opintosuoritukset>
        |            <virta:Opintosuoritus avain="TY¤75094¤24472" koulutusmoduulitunniste="TUTK2133" opiskelijaAvain="TY¤75094" opiskeluoikeusAvain="TY¤75094¤123049¤A">
        |              <virta:SuoritusPvm>2010-09-20</virta:SuoritusPvm>
        |              <virta:Laajuus><virta:Opintopiste>181.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Hyvaksytty>HYV</virta:Hyvaksytty></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>1</virta:Laji>
        |              <virta:Nimi kieli="fi">HUMANISTISTEN TIETEIDEN KANDIDAATTI</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutuskoodi>623404</virta:Koulutuskoodi>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="TY¤75094¤22545"><virta:Opintopiste>105.0</virta:Opintopiste></virta:Sisaltyvyys>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus avain="TY¤75094¤22545" koulutusmoduulitunniste="LOGO1001" opiskelijaAvain="TY¤75094">
        |              <virta:SuoritusPvm>2010-05-21</virta:SuoritusPvm>
        |              <virta:Laajuus><virta:Opintopiste>105.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>4</virta:Viisiportainen></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">LOGOPEDIAN PERUS- JA AINEOPINNOT</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="TY¤75094¤14781"><virta:Opintopiste>25.0</virta:Opintopiste></virta:Sisaltyvyys>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus avain="TY¤75094¤14781" koulutusmoduulitunniste="LOGO1000" opiskelijaAvain="TY¤75094">
        |              <virta:SuoritusPvm>2008-06-16</virta:SuoritusPvm>
        |              <virta:Laajuus><virta:Opintopiste>25.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>4</virta:Viisiportainen></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">LOGOPEDIAN PERUSOPINNOT</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="TY¤75094¤14791"><virta:Opintopiste>3.0</virta:Opintopiste></virta:Sisaltyvyys>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="TY¤75094¤14793"><virta:Opintopiste>3.0</virta:Opintopiste></virta:Sisaltyvyys>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus avain="TY¤75094¤14791" koulutusmoduulitunniste="LOGO1250" opiskelijaAvain="TY¤75094">
        |              <virta:SuoritusPvm>2008-04-29</virta:SuoritusPvm>
        |              <virta:Laajuus><virta:Opintopiste>3.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Hyvaksytty>HYV</virta:Hyvaksytty></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">KIELEN JA PUHEEN KEHITYKSEN TUKEMINEN</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus avain="TY¤75094¤14793" koulutusmoduulitunniste="LOGO1400" opiskelijaAvain="TY¤75094">
        |              <virta:SuoritusPvm>2008-02-28</virta:SuoritusPvm>
        |              <virta:Laajuus><virta:Opintopiste>3.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>4</virta:Viisiportainen></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">PUHETTA TUKEVAT JA KORVAAVAT KEINOT KOMMUNIKOINNISSA</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>""".stripMargin))

    Assertions.assertEquals(5, rivit.size)
    val tutkinto = avaimella(rivit, "TY¤75094¤24472")
    val perusJaAine = avaimella(rivit, "TY¤75094¤22545")
    val perus = avaimella(rivit, "TY¤75094¤14781")
    val jakso1 = avaimella(rivit, "TY¤75094¤14791")
    val jakso2 = avaimella(rivit, "TY¤75094¤14793")

    // Polku seuraa Sisaltyvyys-ketjua jokaisella tasolla
    Assertions.assertEquals(Seq.empty, tutkinto.juuriSuoritusPolku)
    Assertions.assertEquals(Seq(tutkinto.tunniste), perusJaAine.juuriSuoritusPolku)
    Assertions.assertEquals(Seq(tutkinto.tunniste, perusJaAine.tunniste), perus.juuriSuoritusPolku)
    Assertions.assertEquals(Seq(tutkinto.tunniste, perusJaAine.tunniste, perus.tunniste), jakso1.juuriSuoritusPolku)
    Assertions.assertEquals(Seq(tutkinto.tunniste, perusJaAine.tunniste, perus.tunniste), jakso2.juuriSuoritusPolku)
    Assertions.assertEquals(Set(jakso1.tunniste, jakso2.tunniste), perus.lapsiTunnisteet.toSet)

    Assertions.assertEquals("KKTutkinto", tutkinto.entiteetinTyyppi)
    Assertions.assertEquals(Seq("KKOpintosuoritus"), Seq(perusJaAine, perus, jakso1, jakso2).map(_.entiteetinTyyppi).distinct)
    Assertions.assertEquals(OvaraSuoritusTila.VALMIS, tutkinto.supaTila)
    Assertions.assertEquals(Some(LocalDate.of(2010, 9, 20)), tutkinto.suoritusPvm)
    Assertions.assertEquals(Some(LocalDate.of(2007, 8, 1)), tutkinto.aloitusPvm) // tutkinnon aloitus = opiskeluoikeuden alku
    Assertions.assertEquals(Some(BigDecimal(181)), tutkinto.opintoPisteet)
    Assertions.assertEquals(Some("623404"), tutkinto.koulutusKoodi)
    Assertions.assertEquals(Some("TY¤75094¤123049¤A"), tutkinto.opiskeluoikeusAvain)

    // Lehtitason suoritusten omat tiedot; osasuorituksilta puuttuu opiskeluoikeusAvain Virrassa
    Assertions.assertEquals(Some("4"), jakso2.arvosana)
    Assertions.assertEquals(Some("Viisiportainen"), jakso2.arvosanaAsteikko)
    Assertions.assertEquals(Some(BigDecimal(3)), jakso2.opintoPisteet)
    Assertions.assertEquals(Some(LocalDate.of(2008, 2, 28)), jakso2.suoritusPvm)
    Assertions.assertEquals(Some("HYV"), jakso1.arvosana)
    Assertions.assertEquals(None, jakso1.opiskeluoikeusAvain)

    // Opiskeluoikeustason tiedot kaikilla riveillä
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusTyyppi == "KKOpiskeluoikeus"))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusVirtaTila.map(_.arvo).contains("3")))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusAlkuPvm.contains(LocalDate.of(2007, 8, 1))))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusLoppuPvm.contains(LocalDate.of(2010, 9, 20))))
    // Virran opiskeluoikeusavain kaikilla riveillä, myös osasuorituksilla joilta opiskeluoikeusAvain puuttuu
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusVirtaTunniste.contains("TY¤75094¤123049¤A")))
  }

  @Test def testOpintojaksotSiirretaanAinoanTutkinnonAlle(): Unit = {
    // Tutkinto ja opintojaksot samalla tasolla ilman sisältyvyyksiä (ks. VirtaParsingTest.testMoveOpintojaksotUnderOnlyTutkinto)
    val rivit = litista(virtaXml("C10",
      """          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C10" avain="xxx004">
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
        |              <virta:Laajuus><virta:Opintopiste>180.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>3</virta:Viisiportainen></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>1</virta:Laji>
        |              <virta:Koulutuskoodi>751101</virta:Koulutuskoodi>
        |              <virta:Nimi kieli="fi">Kasvatustieteiden kandidaatti</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx004" opiskelijaAvain="C10" koulutusmoduulitunniste="MAT201" avain="op002">
        |              <virta:SuoritusPvm>2020-05-31</virta:SuoritusPvm>
        |              <virta:Laajuus><virta:Opintopiste>5.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>4</virta:Viisiportainen></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Analyysi I</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx004" opiskelijaAvain="C10" koulutusmoduulitunniste="FYS101" avain="op003">
        |              <virta:SuoritusPvm>2020-12-20</virta:SuoritusPvm>
        |              <virta:Laajuus><virta:Opintopiste>10.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>5</virta:Viisiportainen></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Fysiikan perusteet</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>""".stripMargin))

    Assertions.assertEquals(3, rivit.size)
    val juuret = rivit.filter(_.juuriSuoritusPolku.isEmpty)
    Assertions.assertEquals(1, juuret.size)
    val tutkinto = juuret.head
    Assertions.assertEquals("KKTutkinto", tutkinto.entiteetinTyyppi)
    Assertions.assertEquals(Some("op001"), tutkinto.avain)

    val mat201 = komolla(rivit, "MAT201")
    val fys101 = komolla(rivit, "FYS101")
    Assertions.assertEquals(Set(mat201.tunniste, fys101.tunniste), tutkinto.lapsiTunnisteet.toSet)
    Assertions.assertTrue(Seq(mat201, fys101).forall(_.juuriSuoritusPolku == Seq(tutkinto.tunniste)))
    Assertions.assertEquals(Some(LocalDate.of(2020, 5, 31)), mat201.suoritusPvm)
    Assertions.assertEquals(Some(LocalDate.of(2020, 12, 20)), fys101.suoritusPvm)
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusVirtaTila.map(_.arvo).contains("1")))
  }

  @Test def testKeskenerainenTutkintoSynteettisenaJuurena(): Unit = {
    // Aktiivinen opiskeluoikeus ilman tutkintosuoritusta -> synteettinen keskeneräinen tutkinto
    // (ks. VirtaParsingTest.testAddSyntheticKeskenrainenTutkinnonSuoritus)
    val rivit = litista(virtaXml("C10",
      """          <virta:Opiskeluoikeudet>
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
        |              <virta:Laajuus><virta:Opintopiste>5.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>3</virta:Viisiportainen></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Matematiikan perusteet</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>""".stripMargin))

    Assertions.assertEquals(2, rivit.size)
    val juuri = rivit.find(_.juuriSuoritusPolku.isEmpty).get
    Assertions.assertEquals("KKSynteettinenSuoritus", juuri.entiteetinTyyppi)
    Assertions.assertEquals("KKOpiskeluoikeus", juuri.opiskeluoikeusTyyppi)
    Assertions.assertEquals(OvaraSuoritusTila.KESKEN, juuri.supaTila)
    Assertions.assertEquals(None, juuri.suoritusPvm)
    Assertions.assertEquals(Some(LocalDate.of(2020, 8, 1)), juuri.aloitusPvm)
    Assertions.assertEquals("751101", juuri.komoTunniste)
    Assertions.assertEquals(Some("751101"), juuri.koulutusKoodi)
    Assertions.assertEquals(Some("xxx003"), juuri.opiskeluoikeusAvain)
    Assertions.assertEquals(None, juuri.avain)

    val mat101 = komolla(rivit, "MAT101")
    Assertions.assertEquals(Seq(juuri.tunniste), mat101.juuriSuoritusPolku)
    Assertions.assertEquals(Some(LocalDate.of(2021, 5, 31)), mat101.suoritusPvm)
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusVirtaTila.map(_.arvo).contains("1")))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusAlkuPvm.contains(LocalDate.of(2020, 8, 1))))
    // Aktiivisella opiskeluoikeudella ei ole loppupäivää Virrassa
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusLoppuPvm.isEmpty))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusVirtaTunniste.contains("xxx003")))
  }

  @Test def testPaattynytTutkintoonJohtavaIlmanSuorituksia(): Unit = {
    // Valmistunut opiskeluoikeus ilman suorituksia -> synteettinen suoritus, jonka suorituspäivä on valmistumistilan alku
    // (ks. VirtaParsingTest.testTutkintoonJohtavaPaattynytNoSuoritukset)
    val rivit = litista(virtaXml("C13",
      """          <virta:Opiskeluoikeudet>
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
        |          </virta:Opiskeluoikeudet>""".stripMargin))

    Assertions.assertEquals(1, rivit.size)
    val r = rivit.head
    Assertions.assertEquals("KKSynteettinenSuoritus", r.entiteetinTyyppi)
    Assertions.assertEquals(OvaraSuoritusTila.VALMIS, r.supaTila)
    Assertions.assertEquals(Some(LocalDate.of(2021, 12, 31)), r.suoritusPvm)
    Assertions.assertEquals(Seq.empty, r.lapsiTunnisteet)
    Assertions.assertEquals(Some("3"), r.opiskeluoikeusVirtaTila.map(_.arvo))
    Assertions.assertEquals(Some(LocalDate.of(2021, 12, 31)), r.opiskeluoikeusLoppuPvm)
  }

  @Test def testAvoimenOpintojenOpintojaksoSynteettisenWrapperinAlla(): Unit = {
    // Passivoitu avoimen opintojen opiskeluoikeus (tyyppi 13) -> synteettinen wrapper opintojakson ympärille
    // (ks. VirtaParsingTest.testAddSyntheticWrapperForAvoinYliopistoOpintojakso)
    val rivit = litista(virtaXml("C12",
      """          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C12" avain="xxx006">
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
        |              <virta:Laajuus><virta:Opintopiste>10.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>3</virta:Viisiportainen></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Psykologian perusteet</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>""".stripMargin))

    Assertions.assertEquals(2, rivit.size)
    val wrapper = rivit.find(_.juuriSuoritusPolku.isEmpty).get
    Assertions.assertEquals("KKSynteettinenSuoritus", wrapper.entiteetinTyyppi)
    Assertions.assertEquals(Some(OvaraKielistetty(Some("Kasvatustiede"), None, None)), wrapper.nimi)
    Assertions.assertEquals(OvaraSuoritusTila.KESKEYTYNYT, wrapper.supaTila)
    // Suorituspäivä tulee opiskeluoikeudelta vain valmistuneelle; passivoidulla None
    Assertions.assertEquals(None, wrapper.suoritusPvm)
    Assertions.assertEquals(Some(LocalDate.of(2018, 8, 1)), wrapper.aloitusPvm)
    Assertions.assertEquals(None, wrapper.koulutusKoodi)

    val psy101 = komolla(rivit, "PSY101")
    Assertions.assertEquals(Seq(wrapper.tunniste), psy101.juuriSuoritusPolku)
    Assertions.assertEquals(Some("3"), psy101.arvosana)
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusVirtaTila.map(_.arvo).contains("4")))
  }

  @Test def testOrvotSuorituksetSynteettisiinOpiskeluoikeuksiinMyontajittain(): Unit = {
    // Suoritukset ilman opiskeluoikeutta ryhmitellään myöntäjittäin synteettisiin opiskeluoikeuksiin
    // (ks. VirtaParsingTest.testSynteettinenOpiskeluoikeusGroupsByMyontaja)
    def orpo(oo: String, komo: String, avain: String, myontaja: String) =
      s"""            <virta:Opintosuoritus opiskeluoikeusAvain="$oo" opiskelijaAvain="C10" koulutusmoduulitunniste="$komo" avain="$avain">
         |              <virta:SuoritusPvm>2019-06-15</virta:SuoritusPvm>
         |              <virta:Laajuus><virta:Opintopiste>5</virta:Opintopiste></virta:Laajuus>
         |              <virta:Arvosana><virta:Viisiportainen>4</virta:Viisiportainen></virta:Arvosana>
         |              <virta:Myontaja>$myontaja</virta:Myontaja>
         |              <virta:Laji>2</virta:Laji>
         |              <virta:Nimi kieli="fi">$komo</virta:Nimi>
         |              <virta:Kieli>fi</virta:Kieli>
         |            </virta:Opintosuoritus>""".stripMargin
    val rivit = litista(virtaXml("C10",
      s"""          <virta:Opintosuoritukset>
         |${orpo("eiOlemassa1", "MAT101", "orphan1", "10108")}
         |${orpo("eiOlemassa2", "FYS201", "orphan2", "10108")}
         |${orpo("eiOlemassa3", "KEM101", "orphan3", "10089")}
         |          </virta:Opintosuoritukset>""".stripMargin))

    Assertions.assertEquals(3, rivit.size)
    Assertions.assertTrue(rivit.forall(_.juuriSuoritusPolku.isEmpty))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusTyyppi == "KKSynteettinenOpiskeluoikeus"))
    Assertions.assertTrue(rivit.forall(r => r.opiskeluoikeusVirtaTila.isEmpty && r.opiskeluoikeusAlkuPvm.isEmpty
      && r.opiskeluoikeusLoppuPvm.isEmpty && r.opiskeluoikeusKieli.isEmpty && r.opiskeluoikeusVirtaTunniste.isEmpty))

    val mat = komolla(rivit, "MAT101")
    val fys = komolla(rivit, "FYS201")
    val kem = komolla(rivit, "KEM101")
    Assertions.assertEquals(mat.opiskeluoikeusTunniste, fys.opiskeluoikeusTunniste)
    Assertions.assertNotEquals(mat.opiskeluoikeusTunniste, kem.opiskeluoikeusTunniste)
  }

  @Test def testSamaSuoritusKahdenParentinAlla(): Unit = {
    // Sama opintosuoritus (op400) sisältyy kahden eri opiskeluoikeuden tutkintoon
    // (ks. VirtaParsingTest.testSameSuoritusUnderTwoDifferentTutkinto)
    val rivit = litista(virtaXml("C15",
      """          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C15" avain="xxx008">
        |              <virta:AlkuPvm>2020-08-01</virta:AlkuPvm>
        |              <virta:LoppuPvm>2023-06-30</virta:LoppuPvm>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2020-08-01</virta:AlkuPvm>
        |                <virta:Koodi>1</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2023-06-30</virta:AlkuPvm>
        |                <virta:Koodi>3</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tyyppi>1</virta:Tyyppi>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Jakso>
        |                <virta:Koulutuskoodi>751101</virta:Koulutuskoodi>
        |                <virta:AlkuPvm>2020-08-01</virta:AlkuPvm>
        |              </virta:Jakso>
        |            </virta:Opiskeluoikeus>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C15" avain="xxx009">
        |              <virta:AlkuPvm>2021-01-01</virta:AlkuPvm>
        |              <virta:LoppuPvm>2022-12-31</virta:LoppuPvm>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2021-01-01</virta:AlkuPvm>
        |                <virta:Koodi>1</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tila>
        |                <virta:AlkuPvm>2022-12-31</virta:AlkuPvm>
        |                <virta:Koodi>4</virta:Koodi>
        |              </virta:Tila>
        |              <virta:Tyyppi>1</virta:Tyyppi>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Jakso>
        |                <virta:Koulutuskoodi>751101</virta:Koulutuskoodi>
        |                <virta:AlkuPvm>2021-01-01</virta:AlkuPvm>
        |                <virta:Nimi kieli="fi">Kasvatustiede</virta:Nimi>
        |              </virta:Jakso>
        |            </virta:Opiskeluoikeus>
        |          </virta:Opiskeluoikeudet>
        |          <virta:Opintosuoritukset>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx008" opiskelijaAvain="C15" koulutusmoduulitunniste="KAND2023" avain="tutkinto001">
        |              <virta:SuoritusPvm>2023-06-30</virta:SuoritusPvm>
        |              <virta:Laajuus><virta:Opintopiste>180.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>3</virta:Viisiportainen></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>1</virta:Laji>
        |              <virta:Nimi kieli="fi">Kasvatustieteiden kandidaatti</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutuskoodi>751101</virta:Koulutuskoodi>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="op400"><virta:Opintopiste>10.0</virta:Opintopiste></virta:Sisaltyvyys>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx009" opiskelijaAvain="C15" koulutusmoduulitunniste="AVOIN2022" avain="avoin001">
        |              <virta:SuoritusPvm>2022-05-30</virta:SuoritusPvm>
        |              <virta:Laajuus><virta:Opintopiste>20.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Hyvaksytty>HYV</virta:Hyvaksytty></virta:Arvosana>
        |              <virta:Myontaja>10088</virta:Myontaja>
        |              <virta:Laji>1</virta:Laji>
        |              <virta:Nimi kieli="fi">Kasvatustieteen opinnot</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="op400"><virta:Opintopiste>10.0</virta:Opintopiste></virta:Sisaltyvyys>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus opiskelijaAvain="C15" koulutusmoduulitunniste="PSY101" avain="op400">
        |              <virta:SuoritusPvm>2021-05-31</virta:SuoritusPvm>
        |              <virta:Laajuus><virta:Opintopiste>10.0</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>4</virta:Viisiportainen></virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>2</virta:Laji>
        |              <virta:Nimi kieli="fi">Psykologian perusteet</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutusala><virta:Koodi versio="ohjausala">1</virta:Koodi></virta:Koulutusala>
        |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>""".stripMargin))

    // Kaksi tutkintoa ja sama opintosuoritus kummankin alla omana rivinään omalla tunnisteellaan
    Assertions.assertEquals(4, rivit.size)
    val psy = rivit.filter(_.avain.contains("op400"))
    Assertions.assertEquals(2, psy.size)
    Assertions.assertEquals(2, psy.map(_.tunniste).distinct.size)
    Assertions.assertEquals(2, psy.map(_.opiskeluoikeusTunniste).distinct.size)
    Assertions.assertTrue(psy.forall(r => r.juuriSuoritusPolku.size == 1 && r.komoTunniste == "PSY101" && r.arvosana.contains("4")))
    val tutkintojenTunnisteet = rivit.filter(_.juuriSuoritusPolku.isEmpty).map(_.tunniste).toSet
    Assertions.assertEquals(tutkintojenTunnisteet, psy.flatMap(_.parentTunniste).toSet)

    // Lähdejärjestelmän tila tulee opiskeluoikeudelta (viimeisin tila)
    Assertions.assertEquals(Set("3", "4"), rivit.flatMap(_.opiskeluoikeusVirtaTila).map(_.arvo).toSet)
  }

  @Test def testVirtasuorituksenKentatPaatyvatLitistettyynRiviin(): Unit = {
    // Kaikki opintosuorituksen kentät kulkevat Virrasta litistettyyn riviin (ks. VirtaParsingTest.testVirtasuorituksenKentat)
    val rivit = litista(virtaXml("C10",
      """          <virta:Opiskeluoikeudet>
        |            <virta:Opiskeluoikeus opiskelijaAvain="C10" avain="xxx002">
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
        |              <virta:Laajuus><virta:Opintopiste>4</virta:Opintopiste></virta:Laajuus>
        |              <virta:Arvosana><virta:Viisiportainen>1</virta:Viisiportainen></virta:Arvosana>
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
        |              <virta:Koulutusala><virta:Koodi versio="ohjausala">5</virta:Koodi></virta:Koulutusala>
        |              <virta:HyvaksilukuPvm>2014-09-17</virta:HyvaksilukuPvm>
        |              <virta:Opinnaytetyo>0</virta:Opinnaytetyo>
        |            </virta:Opintosuoritus>
        |          </virta:Opintosuoritukset>""".stripMargin))

    val r = avaimella(rivit, "625422")
    Assertions.assertEquals("KKOpintosuoritus", r.entiteetinTyyppi)
    Assertions.assertEquals("LOG13A 01SUO", r.komoTunniste)
    Assertions.assertEquals(OvaraSuoritusTila.VALMIS, r.supaTila)
    Assertions.assertEquals(Some(LocalDate.of(2015, 5, 31)), r.suoritusPvm)
    Assertions.assertEquals(Some(LocalDate.of(2014, 9, 17)), r.hyvaksilukuPvm)
    Assertions.assertEquals(Some(BigDecimal(4)), r.opintoPisteet)
    Assertions.assertEquals(Some("1"), r.arvosana)
    Assertions.assertEquals(Some("Viisiportainen"), r.arvosanaAsteikko)
    Assertions.assertEquals("10108", r.myontaja)
    Assertions.assertEquals(Some("3"), r.jarjestavaRooli)
    Assertions.assertEquals(Some("XX"), r.jarjestavaKoodi)
    Assertions.assertEquals(Some(BigDecimal(1)), r.jarjestavaOsuus)
    Assertions.assertEquals(Some(OvaraKielistetty(Some("Asiantuntijaviestintä"), None, Some("Professional Communications"))), r.nimi)
    Assertions.assertEquals(Some("fi"), r.kieli)
    Assertions.assertEquals(Some(5), r.koulutusala)
    Assertions.assertEquals(Some("ohjausala"), r.koulutusalaKoodisto)
    Assertions.assertEquals(Some(false), r.opinnaytetyo)
    Assertions.assertEquals(Some("xxx002"), r.opiskeluoikeusAvain)
    Assertions.assertEquals(None, r.aloitusPvm)
    Assertions.assertEquals(None, r.koulutusKoodi)
  }

  @Test def testOikeaVirtaAineisto(): Unit = {
    val tiedosto = "/1_2_246_562_24_21250967215.xml"
    def lue() = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream(tiedosto), "UTF-8").mkString
    val rivit = litista(lue())

    // Rivejä on yhtä monta kuin KK-suorituksia business-entiteettien puussa
    def laske(s: Suoritus): Int = s match {
      case t: KKTutkinto => 1 + t.suoritukset.map(laske).sum
      case o: KKOpintosuoritus => 1 + o.suoritukset.map(laske).sum
      case ss: KKSynteettinenSuoritus => 1 + ss.suoritukset.map(laske).sum
      case _ => 0
    }
    val odotettu = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaOpiskelijat(lue()))
      .flatMap {
        case oo: KKOpiskeluoikeus => oo.suoritukset
        case oo: KKSynteettinenOpiskeluoikeus => oo.suoritukset
      }.map(laske).sum
    Assertions.assertTrue(odotettu > 0)
    Assertions.assertEquals(odotettu, rivit.size)
    Assertions.assertTrue(rivit.forall(r => r.myontaja.nonEmpty && r.komoTunniste != null))

    // Koko henkilön record serialisoituu ja litistetyt rivit ovat mukana
    val (kk, kkSynt, _) = litistaVirtaXml(lue(), META)
    val record = OvaraVersioJaOpiskeluoikeudet("1.2.246.562.24.21250967215", OvaraHenkiloMetadata(Instant.parse("2024-01-02T00:00:00Z")),
      kk, kkSynt, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Seq.empty,
      kkSuorituksetFlat = EntityToOvaraConverter.litistaKKSuoritukset(kk, kkSynt))
    val json = siirtotiedostonJson(Seq(record)).get(0)
    Assertions.assertEquals(odotettu, json.get("kkSuorituksetFlat").size)
  }
}
