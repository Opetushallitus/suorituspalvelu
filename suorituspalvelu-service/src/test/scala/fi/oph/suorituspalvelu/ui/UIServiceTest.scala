package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.BaseIntegraatioTesti
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{Koodi, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, Suoritus, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration, PersonOidsWithAliases}
import fi.oph.suorituspalvelu.integration.client.{AtaruPermissionRequest, AtaruPermissionResponse, HakemuspalveluClientImpl, Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiUtil}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.ui.{Oppija, OppijanHakuSuccessResponse}
import fi.oph.suorituspalvelu.security.SecurityConstants
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import org.junit.jupiter.api.{Assertions, Test}
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.{Instant, LocalDate}
import java.util.{Optional, UUID}
import scala.concurrent.Future

class UIServiceTest extends BaseIntegraatioTesti {

  @Autowired var uiService: UIService = null

  @MockBean
  val onrIntegration: OnrIntegration = null

  @MockBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @MockBean
  var organisaatioProvider: OrganisaatioProvider = null

  val OPPIJANUMERO_YSI_KESKEN             = "1.2.246.562.24.21583363334"
  val OPPIJANUMERO_YSI_VALMIS_TAMA_VUOSI  = "1.2.246.562.24.21583363335"
  val OPPIJANUMERO_YSI_VALMIS_VIIMEVUOSI  = "1.2.246.562.24.21583363336"

  val OPPILAITOS_OID                      = "1.2.246.562.10.52320123199"

  val TAMA_VUOSI                          = LocalDate.now().getYear
  val VIIMEVUOSI                          = LocalDate.now().getYear - 1
  val TOISSAVUOSI                         = LocalDate.now().getYear - 2

  private def getYsiLuokka(vuosi: Option[Int]): Suoritus =
    PerusopetuksenVuosiluokka(
      UUID.randomUUID(),
      fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), OPPILAITOS_OID),
      Kielistetty(None, None, None),
      Koodi("9", "perusopetuksenluokkaaste", None),
      None,
      if (vuosi.isDefined) Some(LocalDate.parse(s"${vuosi.get}-08-18")) else None,
      false
    )

  private def getOppimaara(vuosi: Option[Int]): Suoritus =
    PerusopetuksenOppimaara(
      UUID.randomUUID(),
      None,
      fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), OPPILAITOS_OID),
      None,
      Koodi("", "", None),
      if (vuosi.isDefined) VALMIS else KESKEN,
      Koodi("", "", None),
      Set.empty,
      None,
      None,
      if (vuosi.isDefined) Some(LocalDate.parse(s"${vuosi.get}-08-18")) else None,
      Set.empty
    )

  def tallennaOppimaara(oppijaOid: String, vuosi: Option[Int], suoritukset: Set[Suoritus]): Unit =
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaOid, SuoritusJoukko.KOSKI, Seq.empty, Instant.now()).get
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      OPPILAITOS_OID,
      suoritukset,
      None,
      if (vuosi.isDefined) VALMIS else KESKEN
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, opiskeluoikeudet, KoskiUtil.getMetadata(opiskeluoikeudet.toSeq))

  /*
   * Integraatiotestit metadatapohjaiselle oppijoiden haulle
   */

  // TODO: tätä pitää vielä täydentää kun saadaan esim. Juholta mahdollisest kombinaatiot
  private def lisaaSuoritukset(): Unit =
    tallennaOppimaara(OPPIJANUMERO_YSI_KESKEN, None, Set(getYsiLuokka(None)))

    tallennaOppimaara(OPPIJANUMERO_YSI_VALMIS_TAMA_VUOSI, Some(TAMA_VUOSI),
      Set(
        getYsiLuokka(Some(TAMA_VUOSI)),
        getOppimaara(Some(TAMA_VUOSI))
      )
    )

    tallennaOppimaara(OPPIJANUMERO_YSI_VALMIS_VIIMEVUOSI, Some(VIIMEVUOSI),
      Set(
        getYsiLuokka(Some(VIIMEVUOSI)),
        getOppimaara(Some(VIIMEVUOSI))
      )
    )

  @Test def testHaePKOppijaOiditTamaVuosi(): Unit =
    lisaaSuoritukset()
    
    // palautuu oppijat joilla keskeneräinen tai valmis suoritus tältä vuodelta
    Assertions.assertEquals(
      Set((OPPIJANUMERO_YSI_KESKEN, Set("9A")), (OPPIJANUMERO_YSI_VALMIS_TAMA_VUOSI, Set("9A"))),
      uiService.haePKOppijaOidit(OPPILAITOS_OID, TAMA_VUOSI, None))

  @Test def testHaePKOppijaOiditViimevuosi(): Unit =
    lisaaSuoritukset()
    
    // palautuu vain oppijat joilla valmis suoritus haetulta vuodelta
    Assertions.assertEquals(
      Set((OPPIJANUMERO_YSI_VALMIS_VIIMEVUOSI, Set("9A"))),
      uiService.haePKOppijaOidit(OPPILAITOS_OID, VIIMEVUOSI, None))

  @Test def testHaePKOppijaOiditToissavuosi(): Unit =
    lisaaSuoritukset()
    
    // ei palaudu mitään koska toissavuonna valmistuneita ei ole
    Assertions.assertEquals(
      Set.empty,
      uiService.haePKOppijaOidit(OPPILAITOS_OID, TOISSAVUOSI, None))

  @Test def testHaePKOppijaOiditTamaVuosiLuokka(): Unit =
    lisaaSuoritukset()

    // palautuu oppijat joilla keskeneräinen tai valmis suoritus tältä vuodelta ja luokka täsmää
    Assertions.assertEquals(
      Set((OPPIJANUMERO_YSI_KESKEN, Set("9A")), (OPPIJANUMERO_YSI_VALMIS_TAMA_VUOSI, Set("9A"))),
      uiService.haePKOppijaOidit(OPPILAITOS_OID, TAMA_VUOSI, Some("9A")))

  @Test def testHaePKOppijaOiditViimevuosiLuokka(): Unit =
    lisaaSuoritukset()

    // palautuu oppijat joilla valmis suoritus haetulta vuodelta ja luokka täsmää
    Assertions.assertEquals(
      Set((OPPIJANUMERO_YSI_VALMIS_VIIMEVUOSI, Set("9A"))),
      uiService.haePKOppijaOidit(OPPILAITOS_OID, VIIMEVUOSI, Some("9A")))

  @Test def testHaePKOppijaOiditToissavuosiLuokka(): Unit =
    lisaaSuoritukset()
    
    // ei palaudu mitään koska toissavuonna valmistuneita ei ole
    Assertions.assertEquals(
      Set.empty,
      uiService.haePKOppijaOidit(OPPILAITOS_OID, TOISSAVUOSI, Some("9A")))

  /*
   * Integraatiotestit oikeuksien tarkistukselle atarusta
   */

  final val ROOLI_HAKENEIDEN_1_2_246_562_10_52320123196_KATSELIJA = SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA + "_1.2.246.562.10.52320123196"
  final val ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA = SecurityConstants.SECURITY_ROOLI_ORGANISAATION_KATSELIJA + "_1.2.246.562.10.52320123196"

  @WithMockUser(value = "kayttaja", authorities = Array(SecurityConstants.SECURITY_ROOLI_REKISTERINPITAJA_FULL))
  @Test def testCheckAccessRekisterinpitaja(): Unit =
    val oppijaOid = "1.2.246.562.24.21583363334"

    Assertions.assertEquals(true, uiService.hasOppijanKatseluOikeus(oppijaOid))

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testCheckAccessNoPermission(): Unit =
    val oppijaOid = "1.2.246.562.24.21583363334"

    // mockataan onr-vastaus, ei aliaksia
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaOid))).thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaOid -> Set(oppijaOid)))))

    Assertions.assertEquals(false, uiService.hasOppijanKatseluOikeus(oppijaOid))

  @WithMockUser(value = "kayttaja", authorities = Array(ROOLI_HAKENEIDEN_1_2_246_562_10_52320123196_KATSELIJA))
  @Test def testCheckAccessAtaruPermission(): Unit =
    val oppijaOid = "1.2.246.562.24.21583363334"
    val oppilaitosOid = "1.2.246.562.10.52320123196"
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi("", "", ""), None, Seq.empty)
    val onOikeus = true

    // mockataan onr ja ataru-vastaukset
    val permissionRequest = AtaruPermissionRequest(Set(oppijaOid), Set(oppilaitosOid), Set.empty)
    val permissionResponse = AtaruPermissionResponse(Some(onOikeus), None)
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid)).thenReturn(Some(organisaatio))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaOid))).thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaOid -> Set(oppijaOid)))))
    Mockito.when(hakemuspalveluClient.checkPermission(permissionRequest)).thenReturn(Future.successful(permissionResponse))

    // palautuu atarun vastaus
    Assertions.assertEquals(onOikeus, uiService.hasOppijanKatseluOikeus(oppijaOid))

  @WithMockUser(value = "kayttaja", authorities = Array(ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA))
  @Test def testCheckAccessOrganisaatioPermission(): Unit =
    val oppijaOid = "1.2.246.562.24.21583363334"
    val oppilaitosOid = "1.2.246.562.10.52320123196"
    val organisaatio = Organisaatio(oppilaitosOid, OrganisaatioNimi("", "", ""), None, Seq.empty)

    // mockataan onr ja organisaatiopalvelun vastaukset
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid)).thenReturn(Some(organisaatio))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaOid))).thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaOid -> Set(oppijaOid)))))

    // tallennetaan valmis perusopetuksen oppimäärä
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaOid, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      oppilaitosOid,
      Set(PerusopetuksenOppimaara(
        UUID.randomUUID(),
        None,
        fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), oppilaitosOid),
        None,
        Koodi("", "", None),
        VALMIS,
        Koodi("", "", None),
        Set.empty,
        None,
        None,
        Some(LocalDate.parse("2025-08-18")),
        Set.empty
      )),
      None,
      VALMIS
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getMetadata(opiskeluoikeudet.toSeq))

    // palautuu true koska oppijalla oppilaitoksessa pk-suoritus
    Assertions.assertEquals(true, uiService.hasOppijanKatseluOikeus(oppijaOid))

  @WithMockUser(value = "kayttaja", authorities = Array(ROOLI_ORGANISAATION_1_2_246_562_10_52320123196_KATSELIJA))
  @Test def testCheckAccessOrganisaatioNoPermission(): Unit =
    val oppijaOid = "1.2.246.562.24.21583363334"
    val oppilaitosJohonOikeudetOid = "1.2.246.562.10.52320123196"
    val oppilaitosJossaSuoritusOid = "1.2.246.562.10.52320123197"
    val organisaatio = Organisaatio(oppilaitosJohonOikeudetOid, OrganisaatioNimi("", "", ""), None, Seq.empty)

    // mockataan onr ja organisaatiopalvelun vastaukset
    Mockito.when(organisaatioProvider.haeOrganisaationTiedot(oppilaitosJohonOikeudetOid)).thenReturn(Some(organisaatio))
    Mockito.when(onrIntegration.getAliasesForPersonOids(Set(oppijaOid))).thenReturn(Future.successful(PersonOidsWithAliases(Map(oppijaOid -> Set(oppijaOid)))))

    // tallennetaan valmis perusopetuksen oppimäärä
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(oppijaOid, SuoritusJoukko.KOSKI, Seq.empty, Instant.now())
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      oppilaitosJossaSuoritusOid,
      Set(PerusopetuksenOppimaara(
        UUID.randomUUID(),
        None,
        fi.oph.suorituspalvelu.business.Oppilaitos(Kielistetty(None, None, None), oppilaitosJossaSuoritusOid),
        None,
        Koodi("", "", None),
        VALMIS,
        Koodi("", "", None),
        Set.empty,
        None,
        None,
        Some(LocalDate.parse("2025-08-18")),
        Set.empty
      )),
      None,
      VALMIS
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getMetadata(opiskeluoikeudet.toSeq))

    // palautuu false koska oppijalla pk-suoritus muuta toisessa oppilaitoksessa kuin oikeus
    Assertions.assertEquals(false, uiService.hasOppijanKatseluOikeus(oppijaOid))

}
