package fi.oph.suorituspalvelu.business.yos

import fi.oph.suorituspalvelu.business.KKOpiskeluoikeusTila.VOIMASSA
import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, Koodi, Lahdejarjestelma, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.TarjontaIntegration
import fi.oph.suorituspalvelu.integration.client.{KoutaHaku, KoutaHakukohde, Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.parsing.OpiskeluoikeusParsingService
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import fi.oph.suorituspalvelu.yos.YosService
import org.junit.jupiter.api.Assertions.{assertEquals, assertFalse, assertNotNull, assertTrue}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, BeforeEach, Test, TestInstance}
import org.mockito.Mockito
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty

import java.time.{Instant, LocalDate}
import java.util.UUID

@Test
@TestInstance(Lifecycle.PER_CLASS)
class YosServiceTest {

  private val tarjontaMock: TarjontaIntegration = Mockito.mock(classOf[TarjontaIntegration])
  private val oikeusMock: OpiskeluoikeusParsingService = Mockito.mock(classOf[OpiskeluoikeusParsingService])
  private val organisaatioMock: OrganisaatioProvider = Mockito.mock(classOf[OrganisaatioProvider])

  private val service = YosService(tarjontaMock, oikeusMock, organisaatioMock)

  private val HAKIJA_OID = "1.2.246.562.24.71794920276"
  private val HAKU_OID = "1.2.246.562.29.00000000000000074021"
  private val HAKUKOHDE_OID = "1.2.246.562.20.00000000000000078520"

  private val ORGANISAATIO_OID = "1.2.246.562.10.2014040310315946122056"

  private val HAKU_JOKA_KUULUU_YOS_PIIRIIN = KoutaHaku(HAKU_OID, "julkaistu",
    Map.empty, "", Some("haunkohdejoukko_12"), List.empty, None, Some(2026))

  private val HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN = KoutaHakukohde(
    HAKUKOHDE_OID, ORGANISAATIO_OID, Map.empty, None, Some(true)
  )

  private val VIRTA_VERSIO = VersioEntiteetti(
    lahdeJarjestelma = Lahdejarjestelma.VIRTA,
    tunniste = UUID.randomUUID(),
    henkiloOid = HAKIJA_OID,
    alku = Instant.now,
    loppu = None,
    lahdeTunniste = "lahdeTunniste",
    lahdeVersio = Some(1),
    parserVersio = Some(1)
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
    virtaTila = Koodi(arvo = "1", koodisto = "virtakoodisto", versio = Some(1)),
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
  def hakutoiveKuuluuYOSPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertTrue(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).getOrElse(false))
  }

  @Test
  def hakuJokaEiOleKorkeakouluHakuEiKuuluYosPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(
      Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN.copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_11"))))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertFalse(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).getOrElse(false))
  }

  @Test
  def erasmusHakuEiKuuluYosPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(
      Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_010"))))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertFalse(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).getOrElse(false))
  }

  @Test
  def jatkotutkintoEiKuuluYosPiiriin(): Unit = {
    Mockito.when(tarjontaMock.getHaku(HAKU_OID)).thenReturn(
      Some(HAKU_JOKA_KUULUU_YOS_PIIRIIN.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_3"))))
    Mockito.when(tarjontaMock.getHakukohde(HAKUKOHDE_OID)).thenReturn(HAKUTOIVE_JOKA_KUULUU_YOS_PIIRIIN)
    assertFalse(service.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(HAKU_OID, HAKUKOHDE_OID).getOrElse(false))
  }

  @Test
  def hakijalleEiLoydyPaatettaviaOpiskeluOikeuksia(): Unit = {
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map.empty)
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def hakijalleLoytyyPaatettavaOpiskeluOikeus(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS)
    ))
    val oikeudet = service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID).getOrElse(Set.empty)
    assertEquals(1, oikeudet.size)
    val oikeus = oikeudet.head
    assertEquals("Sosionomikoulutus", oikeus.nimi.get.fi.get)
    assertNotNull(oikeus.tunniste)
    assertEquals("koulutuskoodi_1", oikeus.koulutusKoodi.get)
    assertEquals("Tinasepän kuparipaja", oikeus.organisaatio.nimi.fi.get)
    assertEquals(ORGANISAATIO_OID, oikeus.organisaatio.oid.get)
  }

  @Test
  def vaarassaTilassaOlevaOpiskeluOikeusEiKuuluYos(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(virtaTila = Koodi("3", "virtatila", Some(1))))
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def tutkintoonJohtamatonOpiskeluOikeusEiKuuluYos(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(isTutkintoonJohtava = false))
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def tilausKoulutusOpiskeluOikeusEiKuuluYos(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(rahoitusLahde = Some("6")))
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def vaarallaVirtaTyypillaOlevaOpiskeluOikeusEiKuuluYos(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(tyyppiKoodi = "5"))
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }

  @Test
  def kaksoistutkintoOpiskeluOikeusEiKuuluYos(): Unit = {
    Mockito.when(organisaatioMock.haeOrganisaationTiedot("02629")).thenReturn(ORGANISAATIO)
    Mockito.when(oikeusMock.haeSuoritukset(HAKIJA_OID)).thenReturn(Map(
      VIRTA_VERSIO -> Set(YOS_PIIRIIN_KUULUVA_OPISKELUOIKEUS.copy(luokittelu = Some("6")))
    ))
    assertTrue(service.hakijanPaatettavatOpiskeluOikeudet(HAKIJA_OID).getOrElse(Set.empty).isEmpty)
    Mockito.verifyNoInteractions(organisaatioMock)
  }
}
