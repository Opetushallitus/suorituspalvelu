package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, ErikoisAmmattiTutkinto, Koe, Koodi, Opiskeluoikeus, Oppilaitos, YOOpiskeluoikeus, YOTutkinto}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import org.junit.jupiter.api.{Assertions, Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle

import java.time.LocalDate
import java.util.UUID

@TestInstance(Lifecycle.PER_CLASS)
class AutomaattinenHakukelpoisuusTest {

  val PERSON_OID = "1.2.246.562.24.12345678901"
  val OPPILAITOS_OID = "1.2.246.562.10.12345678901"
  val OPISKELUOIKEUS_OID = "1.2.246.562.15.94501385312"

  @Test
  def testGetAutomaattinenHakukelpoisuusYoTutkinnolla(): Unit = {
    val yoTutkinto = YOTutkinto(
      tunniste = UUID.randomUUID(),
      suoritusKieli = Koodi("FI", "kieli", None),
      supaTila = VALMIS,
      valmistumisPaiva = Some(LocalDate.now().minusDays(30)),
      aineet = Set.empty
    )

    val yoOpiskeluoikeus = YOOpiskeluoikeus(
      UUID.randomUUID(),
      Some(yoTutkinto)
    )

    val opiskeluoikeudet = Seq(yoOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertTrue(result, "YO-tutkinnolla pitäisi olla automaattisesti hakukelpoinen")
  }

  @Test
  def testGetAutomaattinenHakukelpoisuusYoTutkintoKeskenaEiHakukelpoinen(): Unit = {
    val yoTutkinto = YOTutkinto(
      tunniste = UUID.randomUUID(),
      suoritusKieli = Koodi("FI", "kieli", None),
      supaTila = KESKEN,
      valmistumisPaiva = None,
      aineet = Set.empty
    )

    val yoOpiskeluoikeus = YOOpiskeluoikeus(
      UUID.randomUUID(),
      Some(yoTutkinto)
    )

    val opiskeluoikeudet = Seq(yoOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertFalse(result, "Keskeneräisellä YO-tutkinnolla ei pitäisi olla automaattisesti hakukelpoinen")
  }

  @Test
  def testGetAutomaattinenHakukelpoisuusAmmatillinenTutkinnolla(): Unit = {
    val ammattiTutkinto = AmmattiTutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("Ammattitutkinto"), None, None),
      koodi = Koodi("AMMATTI", "koulutus", None),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", None),
      supaTila = VALMIS,
      aloitusPaivamaara = Some(LocalDate.now().minusYears(1)),
      vahvistusPaivamaara = Some(LocalDate.now().minusDays(15)),
      suoritustapa = Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      suoritusKieli = Koodi("FI", "kieli", None)
    )

    val ammatillinenOpiskeluoikeus = AmmatillinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPPILAITOS_OID,
      oppilaitos = createOppilaitos(OPPILAITOS_OID),
      suoritukset = Set(ammattiTutkinto),
      tila = None,
      jaksot = List.empty
    )

    val opiskeluoikeudet = Seq(ammatillinenOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertTrue(result, "Ammattitutkinnolla pitäisi olla automaattisesti hakukelpoinen")
  }

  @Test
  def testGetAutomaattinenHakukelpoisuusErikoisammatillinenTutkinnolla(): Unit = {
    val erikoisammattiTutkinto = ErikoisAmmattiTutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("Erikoisammattitutkinto"), None, None),
      koodi = Koodi("ERIKOIS", "koulutus", None),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", None),
      supaTila = VALMIS,
      aloitusPaivamaara = Some(LocalDate.now().minusYears(1)),
      vahvistusPaivamaara = Some(LocalDate.now().minusDays(10)),
      suoritusKieli = Koodi("FI", "kieli", None)
    )

    val ammatillinenOpiskeluoikeus = AmmatillinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPPILAITOS_OID,
      oppilaitos = createOppilaitos(OPPILAITOS_OID),
      suoritukset = Set(erikoisammattiTutkinto),
      tila = None,
      jaksot = List.empty
    )

    val opiskeluoikeudet = Seq(ammatillinenOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertTrue(result, "Erikoisammattitutkinnolla pitäisi olla automaattisesti hakukelpoinen")
  }

  @Test
  def testGetAutomaattinenHakukelpoisuusAmmatillinenVahvistamattaEiHakukelpoinen(): Unit = {
    val ammatillinenPerustutkinto = AmmatillinenPerustutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("Testiperuskoulutus"), None, None),
      koodi = Koodi("AMMATILLINEN", "koulutus", None),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("lasna", "koskiopiskeluoikeudentila", None),
      supaTila = KESKEN,
      aloitusPaivamaara = Some(LocalDate.now().minusYears(2)),
      vahvistusPaivamaara = None,
      keskiarvo = None,
      suoritustapa = Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      suoritusKieli = Koodi("FI", "kieli", None),
      osat = Set.empty
    )

    val ammatillinenOpiskeluoikeus = AmmatillinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPPILAITOS_OID,
      oppilaitos = createOppilaitos(OPPILAITOS_OID),
      suoritukset = Set(ammatillinenPerustutkinto),
      tila = None,
      jaksot = List.empty
    )

    val opiskeluoikeudet = Seq(ammatillinenOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertFalse(result, "Vahvistamattomalla ammatillisella tutkinnolla ei pitäisi olla automaattisesti hakukelpoinen")
  }

  @Test
  def testGetAutomaattinenHakukelpoisuusAmmatillinenPerustutkinnolla(): Unit = {
    val ammatillinenPerustutkinto = AmmatillinenPerustutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("Testiperuskoulutus"), None, None),
      koodi = Koodi("AMMATILLINEN", "koulutus", None),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", None),
      supaTila = VALMIS,
      aloitusPaivamaara = Some(LocalDate.now().minusYears(2)),
      vahvistusPaivamaara = Some(LocalDate.now().minusDays(30)),
      keskiarvo = None,
      suoritustapa = Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      suoritusKieli = Koodi("FI", "kieli", None),
      osat = Set.empty
    )

    val ammatillinenOpiskeluoikeus = AmmatillinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPPILAITOS_OID,
      oppilaitos = createOppilaitos(OPPILAITOS_OID),
      suoritukset = Set(ammatillinenPerustutkinto),
      tila = None,
      jaksot = List.empty
    )

    val opiskeluoikeudet = Seq(ammatillinenOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertTrue(result, "Ammatillisella perustutkinnolla pitäisi olla automaattisesti hakukelpoinen")
  }

  @Test
  def testGetAutomaattinenHakukelpoisuusUseampiTutkinto(): Unit = {
    val yoTutkinto = YOTutkinto(
      tunniste = UUID.randomUUID(),
      suoritusKieli = Koodi("FI", "kieli", None),
      supaTila = VALMIS,
      valmistumisPaiva = Some(LocalDate.now().minusDays(60)),
      aineet = Set.empty
    )

    val yoOpiskeluoikeus = YOOpiskeluoikeus(
      UUID.randomUUID(),
      Some(yoTutkinto)
    )

    val ammatillinenPerustutkinto = AmmatillinenPerustutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("Testiperuskoulutus"), None, None),
      koodi = Koodi("AMMATILLINEN", "koulutus", None),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", None),
      supaTila = VALMIS,
      aloitusPaivamaara = Some(LocalDate.now().minusYears(2)),
      vahvistusPaivamaara = Some(LocalDate.now().minusDays(30)),
      keskiarvo = None,
      suoritustapa = Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      suoritusKieli = Koodi("FI", "kieli", None),
      osat = Set.empty
    )

    val ammatillinenOpiskeluoikeus = AmmatillinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPPILAITOS_OID,
      oppilaitos = createOppilaitos(OPPILAITOS_OID),
      suoritukset = Set(ammatillinenPerustutkinto),
      tila = None,
      jaksot = List.empty
    )

    val opiskeluoikeudet = Seq(yoOpiskeluoikeus, ammatillinenOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertTrue(result, "Useammalla hakukelpoisella tutkinnolla pitäisi olla automaattisesti hakukelpoinen")
  }

  @Test
  def testGetAutomaattinenHakukelpoisuusKeskeytynyitaTutkintojaEiHakukelpoinen(): Unit = {
    val yoTutkinto = YOTutkinto(
      tunniste = UUID.randomUUID(),
      suoritusKieli = Koodi("FI", "kieli", None),
      supaTila = KESKEYTYNYT,
      valmistumisPaiva = None,
      aineet = Set.empty
    )

    val yoOpiskeluoikeus = YOOpiskeluoikeus(
      UUID.randomUUID(),
      Some(yoTutkinto)
    )

    val ammatillinenPerustutkinto = AmmatillinenPerustutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("Testiperuskoulutus"), None, None),
      koodi = Koodi("AMMATILLINEN", "koulutus", None),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("keskeytynyt", "koskiopiskeluoikeudentila", None),
      supaTila = KESKEYTYNYT,
      aloitusPaivamaara = Some(LocalDate.now().minusYears(2)),
      vahvistusPaivamaara = None,
      keskiarvo = None,
      suoritustapa = Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      suoritusKieli = Koodi("FI", "kieli", None),
      osat = Set.empty
    )

    val ammatillinenOpiskeluoikeus = AmmatillinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPPILAITOS_OID,
      oppilaitos = createOppilaitos(OPPILAITOS_OID),
      suoritukset = Set(ammatillinenPerustutkinto),
      tila = None,
      jaksot = List.empty
    )

    val opiskeluoikeudet = Seq(yoOpiskeluoikeus, ammatillinenOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertFalse(result, "Useammalla keskeytyneellä tutkinnolla ei pitäisi olla automaattisesti hakukelpoinen")
  }

  @Test
  def testGetAutomaattinenHakukelpoisuusSekaValmisJaKeskenerainen(): Unit = {
    val yoTutkinto = YOTutkinto(
      tunniste = UUID.randomUUID(),
      suoritusKieli = Koodi("FI", "kieli", None),
      supaTila = VALMIS,
      valmistumisPaiva = Some(LocalDate.now().minusDays(30)),
      aineet = Set.empty
    )

    val yoOpiskeluoikeus = YOOpiskeluoikeus(
      UUID.randomUUID(),
      Some(yoTutkinto)
    )

    val ammatillinenPerustutkinto = AmmatillinenPerustutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("Testiperuskoulutus"), None, None),
      koodi = Koodi("AMMATILLINEN", "koulutus", None),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("lasna", "koskiopiskeluoikeudentila", None),
      supaTila = KESKEN,
      aloitusPaivamaara = Some(LocalDate.now().minusYears(1)),
      vahvistusPaivamaara = None,
      keskiarvo = None,
      suoritustapa = Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      suoritusKieli = Koodi("FI", "kieli", None),
      osat = Set.empty
    )

    val ammatillinenOpiskeluoikeus = AmmatillinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPPILAITOS_OID,
      oppilaitos = createOppilaitos(OPPILAITOS_OID),
      suoritukset = Set(ammatillinenPerustutkinto),
      tila = None,
      jaksot = List.empty
    )

    val opiskeluoikeudet = Seq(yoOpiskeluoikeus, ammatillinenOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertTrue(result, "Vähintään yksi valmis tutkinto riittää hakukelpoisuuteen")
  }

  @Test
  def testGetAutomaattinenHakukelpoisuusYoVahvistamattaEiHakukelpoinen(): Unit = {
    val yoTutkinto = YOTutkinto(
      tunniste = UUID.randomUUID(),
      suoritusKieli = Koodi("FI", "kieli", None),
      supaTila = VALMIS, // VALMIS-tila mutta ei valmistumispäivää
      valmistumisPaiva = None,
      aineet = Set.empty
    )

    val yoOpiskeluoikeus = YOOpiskeluoikeus(
      UUID.randomUUID(),
      Some(yoTutkinto)
    )

    val opiskeluoikeudet = Seq(yoOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertFalse(result, "YO-tutkinto ilman valmistumispäivää ei ole hakukelpoinen")
  }

  @Test
  def testGetAutomaattinenHakukelpoisuusAmmatillinenValmisIlmanVahvistusta(): Unit = {
    val ammatillinenPerustutkinto = AmmatillinenPerustutkinto(
      tunniste = UUID.randomUUID(),
      nimi = Kielistetty(Some("Testiperuskoulutus"), None, None),
      koodi = Koodi("AMMATILLINEN", "koulutus", None),
      oppilaitos = createOppilaitos(),
      koskiTila = Koodi("valmistunut", "koskiopiskeluoikeudentila", None),
      supaTila = VALMIS, // VALMIS-tila mutta ei vahvistuspäivämäärää
      aloitusPaivamaara = Some(LocalDate.now().minusYears(2)),
      vahvistusPaivamaara = None, // Ei vahvistuspäivämäärää
      keskiarvo = None,
      suoritustapa = Koodi("reformi", "ammatillisentutkinnonsuoritustapa", None),
      suoritusKieli = Koodi("FI", "kieli", None),
      osat = Set.empty
    )

    val ammatillinenOpiskeluoikeus = AmmatillinenOpiskeluoikeus(
      tunniste = UUID.randomUUID(),
      oid = OPPILAITOS_OID,
      oppilaitos = createOppilaitos(OPPILAITOS_OID),
      suoritukset = Set(ammatillinenPerustutkinto),
      tila = None,
      jaksot = List.empty
    )

    val opiskeluoikeudet = Seq(ammatillinenOpiskeluoikeus)

    val result = AutomaattinenHakukelpoisuus.getAutomaattinenHakukelpoisuus(PERSON_OID, opiskeluoikeudet)

    Assertions.assertFalse(result, "Ammatillinen tutkinto ilman vahvistuspäivämäärää ei ole hakukelpoinen")
  }

  private def createOppilaitos(oid: String = OPPILAITOS_OID): Oppilaitos = {
    Oppilaitos(
      Kielistetty(Some("Testikoulu"), None, None),
      oid
    )
  }
}
