package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.BaseIntegraatioTesti
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{Koodi, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, PerusopetuksenVuosiluokka, Suoritus, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.OnrHenkiloPerustiedot
import fi.oph.suorituspalvelu.integration.client.{Organisaatio, OrganisaatioNimi}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiUtil}
import fi.oph.suorituspalvelu.resource.ApiConstants
import fi.oph.suorituspalvelu.resource.ui.{Oppija, OppijanHakuSuccessResponse}
import org.junit.jupiter.api.{Assertions, Test}
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.Charset
import java.time.{Instant, LocalDate}
import java.util.{Optional, UUID}
import scala.concurrent.Future

class UIServiceTest extends BaseIntegraatioTesti {

  @Autowired var uiService: UIService = null

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
      Set(OPPIJANUMERO_YSI_KESKEN, OPPIJANUMERO_YSI_VALMIS_TAMA_VUOSI),
      uiService.haePKOppijaOidit(OPPILAITOS_OID, TAMA_VUOSI, None))

  @Test def testHaePKOppijaOiditViimevuosi(): Unit =
    lisaaSuoritukset()
    
    // palautuu vain oppijat joilla valmis suoritus haetulta vuodelta
    Assertions.assertEquals(
      Set(OPPIJANUMERO_YSI_VALMIS_VIIMEVUOSI),
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
      Set(OPPIJANUMERO_YSI_KESKEN, OPPIJANUMERO_YSI_VALMIS_TAMA_VUOSI),
      uiService.haePKOppijaOidit(OPPILAITOS_OID, TAMA_VUOSI, Some("9A")))

  @Test def testHaePKOppijaOiditViimevuosiLuokka(): Unit =
    lisaaSuoritukset()

    // palautuu oppijat joilla valmis suoritus haetulta vuodelta ja luokka täsmää
    Assertions.assertEquals(
      Set(OPPIJANUMERO_YSI_VALMIS_VIIMEVUOSI),
      uiService.haePKOppijaOidit(OPPILAITOS_OID, VIIMEVUOSI, Some("9A")))

  @Test def testHaePKOppijaOiditToissavuosiLuokka(): Unit =
    lisaaSuoritukset()
    
    // ei palaudu mitään koska toissavuonna valmistuneita ei ole
    Assertions.assertEquals(
      Set.empty,
      uiService.haePKOppijaOidit(OPPILAITOS_OID, TOISSAVUOSI, Some("9A")))

}
