package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.BaseIntegraatioTesti
import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.VUOSILUOKKA_9
import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, VALMIS}
import fi.oph.suorituspalvelu.business.{
  Koodi, Lahdejarjestelma, Lahtokoulu, Opiskeluoikeus, ParserVersions, PerusopetuksenOpiskeluoikeus,
  PerusopetuksenOppimaara, Suoritus, SuoritusTila
}
import fi.oph.suorituspalvelu.integration.OnrIntegration
import fi.oph.suorituspalvelu.integration.client.*
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiUtil}
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import org.junit.jupiter.api.{Assertions, Test}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.`override`.mockito.MockitoBean

import java.time.{Instant, LocalDate}
import java.util.UUID

class LahtokoulutServiceTest extends BaseIntegraatioTesti {

  @Autowired var lahtokoulutService: LahtokoulutService = null

  @MockitoBean
  val onrIntegration: OnrIntegration = null

  @MockitoBean
  var hakemuspalveluClient: HakemuspalveluClientImpl = null

  @MockitoBean
  var organisaatioProvider: OrganisaatioProvider = null

  val OPPIJANUMERO_YSI_KESKEN = "1.2.246.562.24.21583363334"
  val OPPIJANUMERO_YSI_VALMIS_TAMA_VUOSI = "1.2.246.562.24.21583363335"
  val OPPIJANUMERO_YSI_VALMIS_VIIMEVUOSI = "1.2.246.562.24.21583363336"

  val OPPILAITOS_OID = "1.2.246.562.10.52320123199"

  val TAMA_VUOSI: Int = LocalDate.now().getYear
  val VIIMEVUOSI: Int = LocalDate.now().getYear - 1
  val TOISSAVUOSI: Int = LocalDate.now().getYear - 2

  private def getLahtokoulut(oppilaitosOid: String, aloitusVuosi: Int, tila: SuoritusTila): Set[Lahtokoulu] =
    Set(Lahtokoulu(
      LocalDate.parse(s"$aloitusVuosi-08-01"),
      if (tila == VALMIS) Some(LocalDate.parse(s"${aloitusVuosi + 1}-06-01")) else None,
      oppilaitosOid,
      Some(aloitusVuosi + 1),
      "9A",
      Some(tila),
      None,
      VUOSILUOKKA_9
    ))

  private def getOppimaara(vuosi: Option[Int], lahtokoulut: Set[Lahtokoulu]): Suoritus =
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
      Set.empty,
      lahtokoulut,
      false,
      false
    )

  def tallennaOppimaara(oppijaOid: String, tila: SuoritusTila, suoritukset: Set[Suoritus]): Unit =
    val versio = kantaOperaatiot.tallennaJarjestelmaVersio(
      oppijaOid,
      Lahdejarjestelma.KOSKI,
      Seq.empty,
      Seq.empty,
      Instant.now(),
      "1.2.3",
      Some(1)
    ).get
    val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(PerusopetuksenOpiskeluoikeus(
      UUID.randomUUID(),
      None,
      OPPILAITOS_OID,
      suoritukset,
      None,
      tila,
      List.empty
    ))
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(
      versio,
      opiskeluoikeudet,
      KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet),
      ParserVersions.KOSKI
    )

  private def lisaaSuoritukset(): Unit =
    tallennaOppimaara(
      OPPIJANUMERO_YSI_KESKEN,
      KESKEN,
      Set(getOppimaara(Some(VIIMEVUOSI), getLahtokoulut(OPPILAITOS_OID, VIIMEVUOSI, KESKEN)))
    )
    tallennaOppimaara(
      OPPIJANUMERO_YSI_VALMIS_TAMA_VUOSI,
      VALMIS,
      Set(getOppimaara(Some(TAMA_VUOSI), getLahtokoulut(OPPILAITOS_OID, VIIMEVUOSI, VALMIS)))
    )
    tallennaOppimaara(
      OPPIJANUMERO_YSI_VALMIS_VIIMEVUOSI,
      VALMIS,
      Set(getOppimaara(Some(VIIMEVUOSI), getLahtokoulut(OPPILAITOS_OID, TOISSAVUOSI, VALMIS)))
    )

  @Test def testHaeOhjattavatJaLuokatTamaVuosi(): Unit =
    lisaaSuoritukset()
    // palautuu oppijat joilla keskeneräinen tai valmis suoritus tältä vuodelta
    Assertions.assertEquals(
      Set((OPPIJANUMERO_YSI_KESKEN, "9A"), (OPPIJANUMERO_YSI_VALMIS_TAMA_VUOSI, "9A")),
      lahtokoulutService.haeOhjattavatJaLuokat(OPPILAITOS_OID, TAMA_VUOSI)
    )

  @Test def testHaeOhjattavatJaLuokatViimevuosi(): Unit =
    lisaaSuoritukset()

    // palautuu vain oppijat joilla valmis suoritus haetulta vuodelta
    Assertions.assertEquals(
      Set((OPPIJANUMERO_YSI_VALMIS_VIIMEVUOSI, "9A")),
      lahtokoulutService.haeOhjattavatJaLuokat(OPPILAITOS_OID, VIIMEVUOSI)
    )

  @Test def testHaeOhjattavatJaLuokatToissavuosi(): Unit =
    lisaaSuoritukset()
    // ei palaudu mitään koska toissavuonna valmistuneita ei ole
    Assertions.assertEquals(
      Set.empty,
      lahtokoulutService.haeOhjattavatJaLuokat(OPPILAITOS_OID, TOISSAVUOSI)
    )

  @Test def testHaeOhjattavatJaLuokatTamaVuosiLuokka(): Unit =
    lisaaSuoritukset()
    // palautuu oppijat joilla keskeneräinen tai valmis suoritus tältä vuodelta ja luokka täsmää
    Assertions.assertEquals(
      Set((OPPIJANUMERO_YSI_KESKEN, "9A"), (OPPIJANUMERO_YSI_VALMIS_TAMA_VUOSI, "9A")),
      lahtokoulutService.haeOhjattavatJaLuokat(OPPILAITOS_OID, TAMA_VUOSI)
    )

  @Test def testHaeOhjattavatJaLuokatViimevuosiLuokka(): Unit =
    lisaaSuoritukset()

    // palautuu oppijat joilla valmis suoritus haetulta vuodelta ja luokka täsmää
    Assertions.assertEquals(
      Set((OPPIJANUMERO_YSI_VALMIS_VIIMEVUOSI, "9A")),
      lahtokoulutService.haeOhjattavatJaLuokat(OPPILAITOS_OID, VIIMEVUOSI)
    )

  @Test def testHaeOhjattavatJaLuokatToissavuosiLuokka(): Unit =
    lisaaSuoritukset()
    // ei palaudu mitään koska toissavuonna valmistuneita ei ole
    Assertions.assertEquals(
      Set.empty,
      lahtokoulutService.haeOhjattavatJaLuokat(OPPILAITOS_OID, TOISSAVUOSI)
    )

}
