package fi.oph.suorituspalvelu.business.yos

import fi.oph.suorituspalvelu.business.KKOpiskeluoikeusTila.VOIMASSA
import fi.oph.suorituspalvelu.business.testsupport.TestUtil.buildDummyKoodistoProvider
import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, Lahdejarjestelma, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.TarjontaIntegration
import fi.oph.suorituspalvelu.integration.client.{Koodi, KoodiMetadata, Koodisto, KoutaHaku, KoutaHakukohde, Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.parsing.OpiskeluoikeusParsingService
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}
import fi.oph.suorituspalvelu.yos.{YosHakutoive, YosService}
import org.junit.jupiter.api.Assertions.{assertEquals, assertFalse, assertNotNull, assertTrue}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeEach, Test, TestInstance}
import org.mockito.Mockito
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.resource.api.YosVirhe.{VIRHE_HAKUTOIVEEN_PAATTELYSSA, VIRHE_PAATETTAVIEN_OPISKELUOIKEUKSIEN_HAUSSA}
import fi.oph.suorituspalvelu.yos.YosKoulutusAsteLuokka.{ALEMMAT_ASTEET, YLEMMAT_JA_ALEMMAT_ASTEET}

import java.time.{Instant, LocalDate}
import java.util.UUID

@Test
@TestInstance(Lifecycle.PER_CLASS)
class YosServiceTest {

  private val tarjontaMock: TarjontaIntegration = Mockito.mock(classOf[TarjontaIntegration])
  private val oikeusMock: OpiskeluoikeusParsingService = Mockito.mock(classOf[OpiskeluoikeusParsingService])
  private val organisaatioMock: OrganisaatioProvider = Mockito.mock(classOf[OrganisaatioProvider])

  private val DUMMY_KOODISTOPROVIDER = buildDummyKoodistoProvider(Map("koulutuskoodi_1" ->
    Koodi(koodiArvo = "1", koodisto = Koodisto("koulutus"), metadata = List(KoodiMetadata(kieli = "fi", nimi = "Agrologi")), koodiUri = "koulutus_1")),
    List(Koodi("72", Koodisto("kansallinenkoulutusluokitus2016koulutusastetaso2"), List.empty, "kansallinenkoulutusluokitus2016koulutusastetaso2_72"))
  )

  private val service = YosService(tarjontaMock, oikeusMock, organisaatioMock, DUMMY_KOODISTOPROVIDER)

  private val HAKIJA_OID = "1.2.246.562.24.71794920276"
  private val HAKU_OID = "1.2.246.562.29.00000000000000074021"
  private val HAKUKOHDE_OID = "1.2.246.562.20.00000000000000078520"

  private val ORGANISAATIO_OID = "1.2.246.562.10.2014040310315946122056"

  private val HAKU_JOKA_KUULUU_YOS_PIIRIIN = KoutaHaku(HAKU_OID, "julkaistu",
    Map.empty, "", Some("haunkohdejoukko_12"), List.empty, None, Some(2026))

  private val HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN = KoutaHakukohde(
    HAKUKOHDE_OID, ORGANISAATIO_OID, Map.empty, None, Some(true), HAKU_OID, List("kansallinenkoulutusluokitus2016koulutusastetaso2_72")
  )

  private val YOS_HAKUTOIVE = YosHakutoive(korkeakoulutus = true, tutkintoonJohtava = true, jatkoTutkinto = false,
    kaksoisTutkinto = false, organisaatioOid = "", koulutusAste = YLEMMAT_JA_ALEMMAT_ASTEET)

  private val VIRTA_VERSIO = VersioEntiteetti(
    lahdeJarjestelma = Lahdejarjestelma.VIRTA,
    tunniste = UUID.randomUUID(),
    henkiloOid = HAKIJA_OID,
    alku = Instant.now,
    loppu = None,
    lahdeTunniste = "lahdeTunniste",
    lahdeVersio = Some(1),
    parserVersio = Some(1),
    luontiHetki = Some(Instant.now), 
    paivitysHetki = None, 
    parserointiHetki = Some(Instant.now)
  )

  private val ORGANISAATIO = Some(Organisaatio(
    oid = ORGANISAATIO_OID,
    nimi = OrganisaatioNimi(fi = "Tinasepän kuparipaja", sv = "", en = ""),
    parentOid = None,
    allDescendantOids = Seq.empty,
    tyypit = Seq.empty,
    oppilaitosTyyppi = None
  ))

  private val YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS = KKOpiskeluoikeus(
    virtaTila = fi.oph.suorituspalvelu.business.Koodi(arvo = "1", koodisto = "virtakoodisto", versio = Some(1)),
    isTutkintoonJohtava = true,
    rahoitusLahde = Some("1"),
    tyyppiKoodi = "1",
    luokittelu = Some("4"),
    myontaja = "02629",
    nimi = Some(Kielistetty(fi = Some("Sosionomikoulutus"), sv = None, en = None)),
    tunniste = UUID.randomUUID(),
    virtaTunniste = "virtatunniste",
    koulutusKoodi = Some("koulutuskoodi_1"),
    alkuPvm = LocalDate.now,
    loppuPvm = null,
    supaTila = VOIMASSA,
    kieli = Some("fi"),
    suoritukset = Set.empty
  )

  @BeforeEach
  def init(): Unit = {
    Mockito.reset(oikeusMock)
    Mockito.reset(organisaatioMock)
    Mockito.reset(tarjontaMock)
  }

  @Test
  def returnsExceptionWhenHakuIsNotFound(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(None)
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertTrue(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).isLeft)
  }

  @Test
  def returnsExceptionWhenHakukohdeIsNotFound(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(null)
    assertTrue(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).isLeft)
  }

  @Test
  def returnsExceptionWhenHakukohdeDoesNotMatchHaku(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN.copy(hakuOid = "1.2.3.4"))
    assertTrue(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).isLeft)
  }

  @Test
  def hakutoiveKuuluuYOSPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertTrue(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).map(t => t.kuuluukoYosPiiriin)
      .getOrElse(false))
  }

  @Test
  def hakuJokaEiOleKorkeakouluHakuEiKuuluYosPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(
      Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN.copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_11"))))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertFalse(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).map(t => t.kuuluukoYosPiiriin)
      .getOrElse(false))
  }

  @Test
  def erasmusHakuEiKuuluYosPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(
      Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_010"))))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertFalse(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).map(t => t.kuuluukoYosPiiriin)
      .getOrElse(false))
  }

  @Test
  def jatkotutkintoEiKuuluYosPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(
      Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_3"))))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertFalse(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).map(t => t.kuuluukoYosPiiriin)
      .getOrElse(false))
  }

  @Test
  def hakutoiveenKoulutusAsteEiKuuluYosPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN
      .copy(koulutusasteKoodiUrit = List("kansallinenkoulutusluokitus2016koulutusastetaso2_82")))
    assertFalse(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).map(t => t.kuuluukoYosPiiriin)
      .getOrElse(false))
  }

  @Test
  def hakijalleEiLoydyPaatettaviaOpiskeluOikeuksia(): Unit = {
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map.empty)
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, YOS_HAKUTOIVE).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def hakijalleLoytyyPaatettavaOpiskeluOikeus(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS)
    ))
    val oikeudet = service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, YOS_HAKUTOIVE).getOrElse(Set.empty)
    assertEquals(1, oikeudet.size)
    val oikeus = oikeudet.head
    assertEquals("Sosionomikoulutus", oikeus.virtaNimi.get.fi.get)
    assertNotNull(oikeus.virtaOpiskeluOikeusId)
    assertEquals("Agrologi", oikeus.supaNimi.get.fi.get)
    assertEquals("Tinasepän kuparipaja", oikeus.organisaatio.nimi.fi.get)
    assertEquals(ORGANISAATIO_OID, oikeus.organisaatio.oid.get)
  }

  @Test
  def hakijalleLoytyyPaatettavaOpiskeluOikeusVaikkaLuokitteluPuuttuu(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(luokittelu = Some("")))
    ))
    val oikeudet = service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, YOS_HAKUTOIVE).getOrElse(Set.empty)
    assertEquals(1, oikeudet.size)
    val oikeus = oikeudet.head
    assertEquals("Sosionomikoulutus", oikeus.virtaNimi.get.fi.get)
    assertNotNull(oikeus.virtaOpiskeluOikeusId)
    assertEquals("Agrologi", oikeus.supaNimi.get.fi.get)
    assertEquals("Tinasepän kuparipaja", oikeus.organisaatio.nimi.fi.get)
    assertEquals(ORGANISAATIO_OID, oikeus.organisaatio.oid.get)
  }

  @Test
  def hakijalleLoytyyPaatettavaOpiskeluOikeusVaikkaRahoitusLahdePuuttuu(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(rahoitusLahde = None))
    ))
    val oikeudet = service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, YOS_HAKUTOIVE).getOrElse(Set.empty)
    assertEquals(1, oikeudet.size)
    val oikeus = oikeudet.head
    assertEquals("Sosionomikoulutus", oikeus.virtaNimi.get.fi.get)
    assertNotNull(oikeus.virtaOpiskeluOikeusId)
    assertEquals("Agrologi", oikeus.supaNimi.get.fi.get)
    assertEquals("Tinasepän kuparipaja", oikeus.organisaatio.nimi.fi.get)
    assertEquals(ORGANISAATIO_OID, oikeus.organisaatio.oid.get)
  }

  @Test
  def korkeammanAsteenOpiskeluOikeusJaAlemmanAsteenVastaanotettavaEiKuuluYosinPiiriin(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS)
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, YOS_HAKUTOIVE.copy(koulutusAste = ALEMMAT_ASTEET)).getOrElse(Set.empty).isEmpty)
  }

  @Test
  def vaarassaTilassaOlevaOpiskeluOikeusEiKuuluYos(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(virtaTila = fi.oph.suorituspalvelu.business.Koodi("3", "virtatila", Some(1))))
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, YOS_HAKUTOIVE).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def tutkintoonJohtamatonOpiskeluOikeusEiKuuluYos(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(isTutkintoonJohtava = false))
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, YOS_HAKUTOIVE).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def tilausKoulutusOpiskeluOikeusEiKuuluYos(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(rahoitusLahde = Some("4")))
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, YOS_HAKUTOIVE).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def vaarallaVirtaTyypillaOlevaOpiskeluOikeusEiKuuluYos(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(tyyppiKoodi = "5"))
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, YOS_HAKUTOIVE).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def kaksoistutkintoOpiskeluOikeusEiKuuluYos(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(luokittelu = Some("6")))
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, YOS_HAKUTOIVE).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def palauttaaHakijanPaatettavatOpiskeluoikeudet(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS)
    ))
    val oikeudet = service.haeHakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, HAKU_OID, HAKUKOHDE_OID).getOrElse(Set.empty)
    assertEquals(1, oikeudet.size)
    val oikeus = oikeudet.head
    assertEquals("Sosionomikoulutus", oikeus.virtaNimi.get.fi.get)
    assertNotNull(oikeus.virtaOpiskeluOikeusId)
    assertEquals("Agrologi", oikeus.supaNimi.get.fi.get)
    assertEquals("Tinasepän kuparipaja", oikeus.organisaatio.nimi.fi.get)
    assertEquals(ORGANISAATIO_OID, oikeus.organisaatio.oid.get)
  }

  @Test
  def palauttaaVirheHakuToiveenPaattellyssa(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenThrow(RuntimeException("FAIL"))
    val virhe = service.haeHakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, HAKU_OID, HAKUKOHDE_OID).left
    assertEquals(VIRHE_HAKUTOIVEEN_PAATTELYSSA, virhe.get.virhe)
  }

  @Test
  def palauttaaVirhePaattyvienOpiskeluOikeuksienHaussa(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenThrow(RuntimeException("FAIL"))
    val virhe = service.haeHakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID, HAKU_OID, HAKUKOHDE_OID).left
    assertEquals(VIRHE_PAATETTAVIEN_OPISKELUOIKEUKSIEN_HAUSSA, virhe.get.virhe)
  }

}
