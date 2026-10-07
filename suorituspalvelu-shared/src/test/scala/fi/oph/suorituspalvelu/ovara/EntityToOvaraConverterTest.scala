package fi.oph.suorituspalvelu.ovara

import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.parsing.koski.{
  Kielistetty, KoskiErityisenTuenPaatos, KoskiKotiopetusjakso, KoskiKoodi,
  KoskiLisatiedot, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila
}
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.junit.jupiter.api.{Assertions, Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle

import java.time.{Instant, LocalDate}
import java.util.UUID

@TestInstance(Lifecycle.PER_CLASS)
class EntityToOvaraConverterTest {

  // ---- Fixtures ----

  private val META = OvaraVersioMetadata(
    lahdejarjestelma = "KOSKI",
    lahdeTunniste = "lt",
    lahdeVersio = Some(1),
    parserVersio = Some(2),
    luontiHetki = Some(Instant.parse("2024-01-01T00:00:00Z")),
    paivitysHetki = Some(Instant.parse("2024-02-01T00:00:00Z")),
    parserointiHetki = Some(Instant.parse("2024-03-01T00:00:00Z"))
  )

  private def kielistetty(s: String) = Kielistetty(Some(s + "_fi"), Some(s + "_sv"), Some(s + "_en"))
  private def koodi(arvo: String, koodisto: String = "ks", versio: Option[Int] = Some(1)) = Koodi(arvo, koodisto, versio)
  private val OPPILAITOS = Oppilaitos(nimi = kielistetty("opl"), oid = "1.2.246.562.10.0001")
  private val LAAJUUS = Laajuus(arvo = BigDecimal(60), yksikko = koodi("op"), nimi = Some(kielistetty("opintopiste")), lyhytNimi = Some(kielistetty("op")))
  private val LAHTOKOULU = Lahtokoulu(
    suorituksenAlku = LocalDate.of(2023, 8, 1),
    suorituksenLoppu = Some(LocalDate.of(2024, 6, 1)),
    oppilaitosOid = "1.2.246.562.10.0099",
    valmistumisvuosi = Some(2024),
    luokka = "9A",
    tila = SuoritusTila.VALMIS,
    arvosanaPuuttuu = Some(false),
    suoritusTyyppi = LahtokouluTyyppi.VUOSILUOKKA_9
  )
  private val OO_JAKSO = OpiskeluoikeusJakso(alku = LocalDate.of(2023, 1, 1), tila = SuoritusTila.VALMIS)
  private val KOSKI_KOODI = KoskiKoodi("loa", "k_koodisto", Some(3), kielistetty("k_nimi"), Some(kielistetty("k_lyh")))
  private val KOSKI_TILA = KoskiOpiskeluoikeusTila(opiskeluoikeusjaksot = List(KoskiOpiskeluoikeusJakso(alku = LocalDate.of(2022, 1, 1), tila = KOSKI_KOODI)))
  private val KOSKI_LISATIEDOT = KoskiLisatiedot(
    erityisenTuenPäätökset = Some(List(KoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain = Some(true)))),
    vuosiluokkiinSitoutumatonOpetus = Some(false),
    kotiopetusjaksot = Some(List(KoskiKotiopetusjakso(alku = "2023-09-01", loppu = Some("2024-05-31"))))
  )

  private val OSA_ALUE = AmmatillisenTutkinnonOsaAlue(
    tunniste = UUID.fromString("00000000-0000-0000-0000-000000000051"),
    nimi = kielistetty("osa-alue"),
    koodi = koodi("oa"),
    arvosana = Some(koodi("a3")),
    laajuus = Some(LAAJUUS),
    korotettu = Some(Korotus.KOROTUKSENYRITYS)
  )

  private val OSA = AmmatillisenTutkinnonOsa(
    tunniste = UUID.fromString("00000000-0000-0000-0000-000000000050"),
    nimi = kielistetty("osa"),
    koodi = koodi("o"),
    yto = true,
    arviointiPaiva = Some(LocalDate.of(2024, 5, 1)),
    arvosana = Some(Arvosana(koodi = koodi("arv"), nimi = kielistetty("arvosananimi"))),
    laajuus = Some(LAAJUUS),
    osaAlueet = Seq(OSA_ALUE),
    korotettu = Some(Korotus.KOROTETTU)
  )

  // ---- Leaf converters (via public API) ----

  @Test def testLahtokouluKonvertoituuKaikkineKentteineen(): Unit = {
    val tuva = Tuva(
      tunniste = UUID.randomUUID(), nimi = kielistetty("tuva"), koodi = koodi("tuvak"),
      oppilaitos = OPPILAITOS, koskiTila = koodi("kt"), supaTila = SuoritusTila.VALMIS,
      aloitusPaivamaara = LocalDate.of(2023, 8, 1), vahvistusPaivamaara = Some(LocalDate.of(2024, 6, 1)),
      suoritusVuosi = 2024, hyvaksyttyLaajuus = Some(LAAJUUS), lahtokoulut = List(LAHTOKOULU)
    )
    val oo = GeneerinenOpiskeluoikeus(UUID.randomUUID(), "1.2.246.562.15.0001", koodi("tuva"), "1.2.246.562.10.1", Set(tuva), None, List.empty)
    val converted = EntityToOvaraConverter.getGeneerisetOpiskeluoikeudet(Seq((META, oo))).head
    val tuvaC = converted.suoritukset.collect { case t: OvaraTuva => t }.head
    val l = tuvaC.lahtokoulut.head
    Assertions.assertEquals(LAHTOKOULU.suorituksenAlku, l.suorituksenAlku)
    Assertions.assertEquals(LAHTOKOULU.suorituksenLoppu, l.suorituksenLoppu)
    Assertions.assertEquals(LAHTOKOULU.oppilaitosOid, l.oppilaitosOid)
    Assertions.assertEquals(LAHTOKOULU.valmistumisvuosi, l.valmistumisvuosi)
    Assertions.assertEquals(LAHTOKOULU.luokka, l.luokka)
    Assertions.assertEquals(OvaraSuoritusTila.VALMIS, l.tila)
    Assertions.assertEquals(LAHTOKOULU.arvosanaPuuttuu, l.arvosanaPuuttuu)
    Assertions.assertEquals(OvaraLahtokouluTyyppi.VUOSILUOKKA_9, l.suoritusTyyppi)
  }

  // ---- Enum coverage (via public API) ----

  @Test def testSuoritusTilaKaikkiCaset(): Unit = {
    val mapping = Map(
      SuoritusTila.VALMIS      -> OvaraSuoritusTila.VALMIS,
      SuoritusTila.KESKEN      -> OvaraSuoritusTila.KESKEN,
      SuoritusTila.KESKEYTYNYT -> OvaraSuoritusTila.KESKEYTYNYT
    )
    mapping.foreach { case (in, expected) =>
      val tutkinto = KKTutkinto(UUID.randomUUID(), Some(kielistetty("t")), in, "komo", BigDecimal(0), None, None, "m", None, None, None, Seq.empty, None)
      val kk = KKOpiskeluoikeus(UUID.randomUUID(), "vt", None, "1", None, LocalDate.of(2020, 1, 1), LocalDate.of(2024, 6, 1), koodi("v"), KKOpiskeluoikeusTila.VOIMASSA, "myo", true, None, Set(tutkinto), None, None, None)
      val out = EntityToOvaraConverter.getKKOpiskeluoikeudet(Seq((META, kk))).head
      val outT = out.suoritukset.collect { case t: OvaraKKTutkinto => t }.head
      Assertions.assertEquals(expected, outT.supaTila, s"$in")
    }
    Assertions.assertEquals(SuoritusTila.values.length, OvaraSuoritusTila.values.length)
  }

  @Test def testKorotusKaikkiCaset(): Unit = {
    val mapping = Map(
      Korotus.KOROTETTU        -> OvaraKorotus.KOROTETTU,
      Korotus.KOROTUKSENYRITYS -> OvaraKorotus.KOROTUKSENYRITYS
    )
    mapping.foreach { case (in, expected) =>
      val osa = OSA.copy(korotettu = Some(in))
      val pt = AmmatillinenPerustutkinto(
        UUID.randomUUID(), kielistetty("pt"), koodi("ptk"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS,
        Some(LocalDate.of(2023, 1, 1)), Some(LocalDate.of(2024, 6, 1)), Some(BigDecimal(4.5)),
        koodi("st"), koodi("sk"), Seq(osa)
      )
      val amm = AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.246.562.15.0002", OPPILAITOS, Set(pt), None, List.empty)
      val out = EntityToOvaraConverter.getAmmatillisetOpiskeluoikeudet(Seq((META, amm))).head
      val outOsa = out.suoritukset.collect { case p: OvaraAmmatillinenPerustutkinto => p }.head.osat.head
      Assertions.assertEquals(Some(expected), outOsa.korotettu, s"$in")
    }
    Assertions.assertEquals(Korotus.values.length, OvaraKorotus.values.length)
  }

  @Test def testKKOpiskeluoikeusTilaKaikkiCaset(): Unit = {
    val mapping = Map(
      KKOpiskeluoikeusTila.VOIMASSA  -> OvaraKKOpiskeluoikeusTila.VOIMASSA,
      KKOpiskeluoikeusTila.PAATTYNYT -> OvaraKKOpiskeluoikeusTila.PAATTYNYT
    )
    mapping.foreach { case (in, expected) =>
      val kk = KKOpiskeluoikeus(UUID.randomUUID(), "vt", None, "1", None, LocalDate.of(2020, 1, 1), LocalDate.of(2024, 6, 1), koodi("v"), in, "myo", true, None, Set.empty, None, None, None)
      val out = EntityToOvaraConverter.getKKOpiskeluoikeudet(Seq((META, kk))).head
      Assertions.assertEquals(expected, out.supaTila, s"$in")
    }
    Assertions.assertEquals(KKOpiskeluoikeusTila.values.length, OvaraKKOpiskeluoikeusTila.values.length)
  }

  @Test def testPerusopetuksenYksilollistaminenKaikkiCaset(): Unit = {
    val mapping = Map(
      PerusopetuksenYksilollistaminen.EI_YKSILOLLISTETTY                  -> OvaraPerusopetuksenYksilollistaminen.EI_YKSILOLLISTETTY,
      PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY            -> OvaraPerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY,
      PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY -> OvaraPerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY,
      PerusopetuksenYksilollistaminen.TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY  -> OvaraPerusopetuksenYksilollistaminen.TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY,
      PerusopetuksenYksilollistaminen.OSITTAIN_RAJATTU                    -> OvaraPerusopetuksenYksilollistaminen.OSITTAIN_RAJATTU,
      PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_RAJATTU        -> OvaraPerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_RAJATTU
    )
    mapping.foreach { case (in, expected) =>
      val om = PerusopetuksenOppimaara(
        UUID.randomUUID(), None, OPPILAITOS, Some("9A"), koodi("kt"), SuoritusTila.VALMIS, koodi("FI"), Set(koodi("FI")),
        Some(in), Some(LocalDate.of(2023, 8, 1)), Some(LocalDate.of(2024, 6, 1)), Seq.empty, List.empty,
        syotetty = false, vuosiluokkiinSitoutumatonOpetus = false, luokkaAste = Some(9)
      )
      val po = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.0003"), "1.2.246.562.10.1", Set(om), None, SuoritusTila.VALMIS, List.empty)
      val out = EntityToOvaraConverter.getPerusopetuksenOpiskeluoikeudet(Seq((META, po))).head
      val outOm = out.suoritukset.collect { case x: OvaraPerusopetuksenOppimaara => x }.head
      Assertions.assertEquals(Some(expected), outOm.yksilollistaminen, s"$in")
    }
    Assertions.assertEquals(PerusopetuksenYksilollistaminen.values.length, OvaraPerusopetuksenYksilollistaminen.values.length)
  }

  @Test def testLahtokouluTyyppiKaikkiCaset(): Unit = {
    val mapping = Map(
      LahtokouluTyyppi.VUOSILUOKKA_7                  -> OvaraLahtokouluTyyppi.VUOSILUOKKA_7,
      LahtokouluTyyppi.VUOSILUOKKA_8                  -> OvaraLahtokouluTyyppi.VUOSILUOKKA_8,
      LahtokouluTyyppi.VUOSILUOKKA_9                  -> OvaraLahtokouluTyyppi.VUOSILUOKKA_9,
      LahtokouluTyyppi.AIKUISTEN_PERUSOPETUS          -> OvaraLahtokouluTyyppi.AIKUISTEN_PERUSOPETUS,
      LahtokouluTyyppi.PERUSOPETUKSEEN_VALMISTAVA_OPETUS -> OvaraLahtokouluTyyppi.PERUSOPETUKSEEN_VALMISTAVA_OPETUS,
      LahtokouluTyyppi.TUVA                           -> OvaraLahtokouluTyyppi.TUVA,
      LahtokouluTyyppi.TELMA                          -> OvaraLahtokouluTyyppi.TELMA,
      LahtokouluTyyppi.VAPAA_SIVISTYSTYO              -> OvaraLahtokouluTyyppi.VAPAA_SIVISTYSTYO
    )
    mapping.foreach { case (in, expected) =>
      val l = LAHTOKOULU.copy(suoritusTyyppi = in)
      val pvo = PerusopetukseenValmistavaOpetus(lahtokoulut = List(l))
      val po = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), None, "1.2.246.562.10.1", Set(pvo), None, SuoritusTila.VALMIS, List.empty)
      val out = EntityToOvaraConverter.getPerusopetuksenOpiskeluoikeudet(Seq((META, po))).head
      val outL = out.suoritukset.collect { case p: OvaraPerusopetukseenValmistavaOpetus => p }.head.lahtokoulut.head
      Assertions.assertEquals(expected, outL.suoritusTyyppi, s"$in")
    }
    Assertions.assertEquals(LahtokouluTyyppi.values.length, OvaraLahtokouluTyyppi.values.length)
  }

  // ---- Aggregate get*Opiskeluoikeudet per opiskeluoikeus type ----

  @Test def testGetKKOpiskeluoikeudetKonvertoiKaikkiSuoritusvariantit(): Unit = {
    val tutkinto = KKTutkinto(UUID.randomUUID(), Some(kielistetty("t")), SuoritusTila.VALMIS, "komo", BigDecimal(180), Some(LocalDate.of(2020, 9, 1)), Some(LocalDate.of(2024, 6, 1)), "myo", Some("fi"), Some("613101"), Some("a-1"), Seq.empty, Some("avain-t"))
    val opinto = KKOpintosuoritus(UUID.randomUUID(), Some(kielistetty("o")), SuoritusTila.VALMIS, "komo", BigDecimal(5), Some(BigDecimal(3)), Some(LocalDate.of(2023, 5, 1)), Some(LocalDate.of(2023, 6, 1)), "myo", Some("vastuu"), Some("jk"), Some(BigDecimal(1)), Some("4"), Some("4-1"), Some("fi"), Some(1), Some("ka"), opinnaytetyo = false, Some("a-1"), Seq.empty, "avain-o")
    val synt = KKSynteettinenSuoritus(UUID.randomUUID(), Some(kielistetty("s")), SuoritusTila.KESKEN, "komo", Some(LocalDate.of(2023, 9, 1)), Some(LocalDate.of(2024, 6, 1)), "myo", Some("613101"), Some("a-1"), Seq.empty)
    val kk = KKOpiskeluoikeus(UUID.randomUUID(), "vt", None, "1", Some("613101"), LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 1), koodi("v"), KKOpiskeluoikeusTila.PAATTYNYT, "myo", true, Some("fi"), Set(tutkinto, opinto, synt), None, None, None)

    val out = EntityToOvaraConverter.getKKOpiskeluoikeudet(Seq((META, kk))).head

    Assertions.assertEquals(META, out.metadata)
    Assertions.assertEquals(kk.tunniste, out.tunniste)
    Assertions.assertEquals("vt", out.virtaTunniste)
    Assertions.assertEquals(OvaraKoodi("v", "ks", Some(1)), out.virtaTila)
    Assertions.assertEquals(OvaraKKOpiskeluoikeusTila.PAATTYNYT, out.supaTila)
    Assertions.assertEquals(3, out.suoritukset.size)
    val outT = out.suoritukset.collect { case t: OvaraKKTutkinto => t }.head
    Assertions.assertEquals(Some(OvaraKielistetty(Some("t_fi"), Some("t_sv"), Some("t_en"))), outT.nimi)
    Assertions.assertEquals(OvaraSuoritusTila.VALMIS, outT.supaTila)
    val outO = out.suoritukset.collect { case o: OvaraKKOpintosuoritus => o }.head
    Assertions.assertEquals("avain-o", outO.avain)
    val outS = out.suoritukset.collect { case s: OvaraKKSynteettinenSuoritus => s }.head
    Assertions.assertEquals(OvaraSuoritusTila.KESKEN, outS.supaTila)
  }

  @Test def testGetKKSynteettisetOpiskeluoikeudet(): Unit = {
    val synt = KKSynteettinenSuoritus(UUID.randomUUID(), Some(kielistetty("s")), SuoritusTila.VALMIS, "komo", None, None, "myo", None, None, Seq.empty)
    val kk = KKSynteettinenOpiskeluoikeus(UUID.randomUUID(), "myo-1", containsKKTutkinto = true, Set(synt))
    val out = EntityToOvaraConverter.getKKSynteettisetOpiskeluoikeudet(Seq((META, kk))).head
    Assertions.assertEquals("myo-1", out.myontaja)
    Assertions.assertTrue(out.containsKKTutkinto)
    Assertions.assertEquals(1, out.suoritukset.size)
    Assertions.assertEquals(OvaraSuoritusTila.VALMIS, out.suoritukset.head.asInstanceOf[OvaraKKSynteettinenSuoritus].supaTila)
  }

  // ---- KK-suoritusten litistys ----

  @Test def testLitistaKKSuorituksetSailyttaaHierarkian(): Unit = {
    val lapsenlapsi = KKOpintosuoritus(UUID.randomUUID(), Some(kielistetty("ll")), SuoritusTila.VALMIS, "komo-ll", BigDecimal(2), None, Some(LocalDate.of(2023, 5, 1)), None, "myo", None, None, None, Some("5"), Some("5-1"), Some("fi"), None, None, opinnaytetyo = false, Some("a-1"), Seq.empty, "avain-ll")
    val lapsi1 = KKOpintosuoritus(UUID.randomUUID(), Some(kielistetty("l1")), SuoritusTila.VALMIS, "komo-l1", BigDecimal(5), Some(BigDecimal(3)), Some(LocalDate.of(2023, 6, 1)), Some(LocalDate.of(2023, 7, 1)), "myo", Some("vastuu"), Some("jk"), Some(BigDecimal(1)), Some("4"), Some("4-1"), Some("fi"), Some(1), Some("ka"), opinnaytetyo = true, Some("a-1"), Seq(lapsenlapsi), "avain-l1")
    val lapsi2 = KKOpintosuoritus(UUID.randomUUID(), None, SuoritusTila.VALMIS, "komo-l2", BigDecimal(3), None, None, None, "myo", None, None, None, None, None, None, None, None, opinnaytetyo = false, None, Seq.empty, "avain-l2")
    val tutkinto = KKTutkinto(UUID.randomUUID(), Some(kielistetty("t")), SuoritusTila.VALMIS, "komo-t", BigDecimal(180), Some(LocalDate.of(2020, 9, 1)), Some(LocalDate.of(2024, 6, 1)), "myo", Some("fi"), Some("613101"), Some("a-1"), Seq(lapsi1, lapsi2), Some("avain-t"))
    val kk = KKOpiskeluoikeus(UUID.randomUUID(), "vt", None, "1", Some("613101"), LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 1), koodi("v"), KKOpiskeluoikeusTila.PAATTYNYT, "myo", true, Some("fi"), Set(tutkinto), None, None, None)

    val rivit = EntityToOvaraConverter.litistaKKSuoritukset(EntityToOvaraConverter.getKKOpiskeluoikeudet(Seq((META, kk))), Seq.empty)
    tarkistaLitistyksenInvariantit(rivit)

    // Esijärjestys: parent ennen lapsiaan
    Assertions.assertEquals(Seq(tutkinto.tunniste, lapsi1.tunniste, lapsenlapsi.tunniste, lapsi2.tunniste), rivit.map(_.tunniste))
    Assertions.assertEquals(Seq(None, Some(tutkinto.tunniste), Some(lapsi1.tunniste), Some(tutkinto.tunniste)), rivit.map(_.parentTunniste))
    Assertions.assertEquals(Seq(Seq(lapsi1.tunniste, lapsi2.tunniste), Seq(lapsenlapsi.tunniste), Seq.empty, Seq.empty), rivit.map(_.lapsiTunnisteet))
    // Polku sisältää kaikki parentit juuritason suorituksesta alkaen
    Assertions.assertEquals(Seq(Seq.empty, Seq(tutkinto.tunniste), Seq(tutkinto.tunniste, lapsi1.tunniste), Seq(tutkinto.tunniste)), rivit.map(_.juuriSuoritusPolku))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusTunniste == kk.tunniste))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusTyyppi == "KKOpiskeluoikeus"))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusVirtaTila.contains(OvaraKoodi("v", "ks", Some(1)))))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusAlkuPvm.contains(LocalDate.of(2020, 9, 1))))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusLoppuPvm.contains(LocalDate.of(2024, 6, 1))))
    Assertions.assertTrue(rivit.forall(_.opiskeluoikeusKieli.contains("fi")))
    Assertions.assertTrue(rivit.forall(_.metadata == META))
    Assertions.assertEquals(Seq("KKTutkinto", "KKOpintosuoritus", "KKOpintosuoritus", "KKOpintosuoritus"), rivit.map(_.entiteetinTyyppi))

    val t = rivit.head
    Assertions.assertEquals(Some(OvaraKielistetty(Some("t_fi"), Some("t_sv"), Some("t_en"))), t.nimi)
    Assertions.assertEquals(OvaraSuoritusTila.VALMIS, t.supaTila)
    Assertions.assertEquals("komo-t", t.komoTunniste)
    Assertions.assertEquals("myo", t.myontaja)
    Assertions.assertEquals(Some(LocalDate.of(2024, 6, 1)), t.suoritusPvm)
    Assertions.assertEquals(Some("a-1"), t.opiskeluoikeusAvain)
    Assertions.assertEquals(Some("fi"), t.kieli)
    Assertions.assertEquals(Some(BigDecimal(180)), t.opintoPisteet)
    Assertions.assertEquals(Some(LocalDate.of(2020, 9, 1)), t.aloitusPvm)
    Assertions.assertEquals(Some("613101"), t.koulutusKoodi)
    Assertions.assertEquals(Some("avain-t"), t.avain)
    Assertions.assertEquals(None, t.arvosana)
    Assertions.assertEquals(None, t.opinnaytetyo)

    val o = rivit(1)
    Assertions.assertEquals("komo-l1", o.komoTunniste)
    Assertions.assertEquals(Some(OvaraKielistetty(Some("l1_fi"), Some("l1_sv"), Some("l1_en"))), o.nimi)
    Assertions.assertEquals(OvaraSuoritusTila.VALMIS, o.supaTila)
    Assertions.assertEquals("myo", o.myontaja)
    Assertions.assertEquals(Some(LocalDate.of(2023, 6, 1)), o.suoritusPvm)
    Assertions.assertEquals(Some("a-1"), o.opiskeluoikeusAvain)
    Assertions.assertEquals(Some("fi"), o.kieli)
    Assertions.assertEquals(None, o.koulutusKoodi)
    Assertions.assertEquals(Some(BigDecimal(5)), o.opintoPisteet)
    Assertions.assertEquals(Some(BigDecimal(3)), o.opintoviikot)
    Assertions.assertEquals(Some(LocalDate.of(2023, 7, 1)), o.hyvaksilukuPvm)
    Assertions.assertEquals(Some("vastuu"), o.jarjestavaRooli)
    Assertions.assertEquals(Some("jk"), o.jarjestavaKoodi)
    Assertions.assertEquals(Some(BigDecimal(1)), o.jarjestavaOsuus)
    Assertions.assertEquals(Some("4"), o.arvosana)
    Assertions.assertEquals(Some("4-1"), o.arvosanaAsteikko)
    Assertions.assertEquals(Some(1), o.koulutusala)
    Assertions.assertEquals(Some("ka"), o.koulutusalaKoodisto)
    Assertions.assertEquals(Some(true), o.opinnaytetyo)
    Assertions.assertEquals(Some("avain-l1"), o.avain)
    Assertions.assertEquals(None, o.aloitusPvm)
  }

  @Test def testLitistaKKSuorituksetSynteettinenOpiskeluoikeus(): Unit = {
    val synt = KKSynteettinenSuoritus(UUID.randomUUID(), Some(kielistetty("s")), SuoritusTila.KESKEN, "komo-s", Some(LocalDate.of(2023, 9, 1)), None, "myo", Some("613101"), Some("a-1"), Seq.empty)
    val kk = KKSynteettinenOpiskeluoikeus(UUID.randomUUID(), "myo", containsKKTutkinto = false, Set(synt))

    val rivit = EntityToOvaraConverter.litistaKKSuoritukset(Seq.empty, EntityToOvaraConverter.getKKSynteettisetOpiskeluoikeudet(Seq((META, kk))))
    tarkistaLitistyksenInvariantit(rivit)

    Assertions.assertEquals(1, rivit.size)
    val r = rivit.head
    Assertions.assertEquals("KKSynteettinenSuoritus", r.entiteetinTyyppi)
    Assertions.assertEquals("KKSynteettinenOpiskeluoikeus", r.opiskeluoikeusTyyppi)
    Assertions.assertEquals(kk.tunniste, r.opiskeluoikeusTunniste)
    Assertions.assertEquals(None, r.parentTunniste)
    Assertions.assertEquals(Seq.empty, r.juuriSuoritusPolku)
    Assertions.assertEquals(Seq.empty, r.lapsiTunnisteet)
    Assertions.assertEquals(None, r.opiskeluoikeusVirtaTila)
    Assertions.assertEquals(None, r.opiskeluoikeusAlkuPvm)
    Assertions.assertEquals(None, r.opiskeluoikeusLoppuPvm)
    Assertions.assertEquals(None, r.opiskeluoikeusKieli)
    Assertions.assertEquals(OvaraSuoritusTila.KESKEN, r.supaTila)
    Assertions.assertEquals(Some(LocalDate.of(2023, 9, 1)), r.aloitusPvm)
    Assertions.assertEquals(Some("613101"), r.koulutusKoodi)
    Assertions.assertEquals(Some("a-1"), r.opiskeluoikeusAvain)
    Assertions.assertEquals(None, r.opintoPisteet)
    Assertions.assertEquals(None, r.avain)
  }

  @Test def testLitistaKKSuorituksetTyhja(): Unit = {
    Assertions.assertEquals(Seq.empty, EntityToOvaraConverter.litistaKKSuoritukset(Seq.empty, Seq.empty))
  }

  @Test def testLitistaKKSuorituksetUseitaOpiskeluoikeuksiaJaJuuria(): Unit = {
    val META2 = META.copy(lahdejarjestelma = "VIRTA", lahdeTunniste = "lt2")

    // Opiskeluoikeus A: kaksi juuritason suoritusta, joista toisella lapsi
    val aLapsi = KKOpintosuoritus(UUID.randomUUID(), None, SuoritusTila.VALMIS, "komo-al", BigDecimal(5), None, None, None, "myo-a", None, None, None, None, None, None, None, None, opinnaytetyo = false, None, Seq.empty, "avain-al")
    val aTutkinto = KKTutkinto(UUID.randomUUID(), None, SuoritusTila.VALMIS, "komo-at", BigDecimal(180), None, None, "myo-a", None, None, None, Seq(aLapsi), None)
    val aOpintojakso = KKOpintosuoritus(UUID.randomUUID(), None, SuoritusTila.VALMIS, "komo-ao", BigDecimal(3), None, None, None, "myo-a", None, None, None, None, None, None, None, None, opinnaytetyo = false, None, Seq.empty, "avain-ao")
    val ooA = KKOpiskeluoikeus(UUID.randomUUID(), "vt-a", None, "1", None, LocalDate.of(2018, 1, 1), LocalDate.of(2021, 1, 1), koodi("3"), KKOpiskeluoikeusTila.PAATTYNYT, "myo-a", true, Some("sv"), Set(aTutkinto, aOpintojakso), None, None, None)

    // Opiskeluoikeus B: synteettinen (keskeneräinen tutkinto) suoritus lapsineen normaalin opiskeluoikeuden alla
    val bLapsi1 = KKOpintosuoritus(UUID.randomUUID(), None, SuoritusTila.VALMIS, "komo-b1", BigDecimal(5), None, None, None, "myo-b", None, None, None, None, None, None, None, None, opinnaytetyo = false, None, Seq.empty, "avain-b1")
    val bLapsi2 = KKOpintosuoritus(UUID.randomUUID(), None, SuoritusTila.VALMIS, "komo-b2", BigDecimal(5), None, None, None, "myo-b", None, None, None, None, None, None, None, None, opinnaytetyo = false, None, Seq.empty, "avain-b2")
    val bSynt = KKSynteettinenSuoritus(UUID.randomUUID(), None, SuoritusTila.KESKEN, "komo-bs", Some(LocalDate.of(2022, 8, 1)), None, "myo-b", Some("751101"), Some("vt-b"), Seq(bLapsi1, bLapsi2))
    val ooB = KKOpiskeluoikeus(UUID.randomUUID(), "vt-b", None, "1", None, LocalDate.of(2022, 8, 1), LocalDate.of(2026, 7, 31), koodi("1"), KKOpiskeluoikeusTila.VOIMASSA, "myo-b", true, Some("fi"), Set(bSynt), None, None, None)

    // Synteettinen opiskeluoikeus C
    val cSynt = KKSynteettinenSuoritus(UUID.randomUUID(), None, SuoritusTila.VALMIS, "komo-cs", None, None, "myo-c", None, None, Seq.empty)
    val ooC = KKSynteettinenOpiskeluoikeus(UUID.randomUUID(), "myo-c", containsKKTutkinto = false, Set(cSynt))

    val rivit = EntityToOvaraConverter.litistaKKSuoritukset(
      EntityToOvaraConverter.getKKOpiskeluoikeudet(Seq((META, ooA), (META2, ooB))),
      EntityToOvaraConverter.getKKSynteettisetOpiskeluoikeudet(Seq((META2, ooC))))
    tarkistaLitistyksenInvariantit(rivit)

    // Kaikki suoritukset mukana, jokainen oikean opiskeluoikeuden alla
    val odotetut = Map(
      ooA.tunniste -> Set(aTutkinto.tunniste, aLapsi.tunniste, aOpintojakso.tunniste),
      ooB.tunniste -> Set(bSynt.tunniste, bLapsi1.tunniste, bLapsi2.tunniste),
      ooC.tunniste -> Set(cSynt.tunniste)
    )
    Assertions.assertEquals(odotetut, rivit.groupBy(_.opiskeluoikeusTunniste).view.mapValues(_.map(_.tunniste).toSet).toMap)

    // Opiskeluoikeuksien järjestys säilyy: ensin KK-opiskeluoikeudet annetussa järjestyksessä, sitten synteettiset
    Assertions.assertEquals(Seq(ooA.tunniste, ooB.tunniste, ooC.tunniste), rivit.map(_.opiskeluoikeusTunniste).distinct)

    // Opiskeluoikeustason tiedot eivät vuoda opiskeluoikeudelta toiselle
    val a = rivit.filter(_.opiskeluoikeusTunniste == ooA.tunniste)
    Assertions.assertTrue(a.forall(r => r.metadata == META && r.opiskeluoikeusVirtaTila.contains(OvaraKoodi("3", "ks", Some(1)))
      && r.opiskeluoikeusAlkuPvm.contains(LocalDate.of(2018, 1, 1)) && r.opiskeluoikeusKieli.contains("sv")))
    val b = rivit.filter(_.opiskeluoikeusTunniste == ooB.tunniste)
    Assertions.assertTrue(b.forall(r => r.metadata == META2 && r.opiskeluoikeusVirtaTila.contains(OvaraKoodi("1", "ks", Some(1)))
      && r.opiskeluoikeusLoppuPvm.contains(LocalDate.of(2026, 7, 31)) && r.opiskeluoikeusKieli.contains("fi")))
    val c = rivit.filter(_.opiskeluoikeusTunniste == ooC.tunniste)
    Assertions.assertTrue(c.forall(r => r.metadata == META2 && r.opiskeluoikeusTyyppi == "KKSynteettinenOpiskeluoikeus" && r.opiskeluoikeusVirtaTila.isEmpty))

    // Opiskeluoikeuden A kaksi juurta
    Assertions.assertEquals(Set(aTutkinto.tunniste, aOpintojakso.tunniste), a.filter(_.juuriSuoritusPolku.isEmpty).map(_.tunniste).toSet)
    Assertions.assertEquals(Seq(aTutkinto.tunniste), a.find(_.tunniste == aLapsi.tunniste).get.juuriSuoritusPolku)

    // Synteettinen suoritus lapsineen normaalin opiskeluoikeuden alla
    val bJuuri = b.find(_.tunniste == bSynt.tunniste).get
    Assertions.assertEquals("KKSynteettinenSuoritus", bJuuri.entiteetinTyyppi)
    Assertions.assertEquals("KKOpiskeluoikeus", bJuuri.opiskeluoikeusTyyppi)
    Assertions.assertEquals(Seq(bLapsi1.tunniste, bLapsi2.tunniste), bJuuri.lapsiTunnisteet)
    Assertions.assertEquals(Seq(bLapsi1.tunniste, bLapsi2.tunniste), b.filter(_.juuriSuoritusPolku == Seq(bSynt.tunniste)).map(_.tunniste))
  }

  @Test def testLitistaKKSuorituksetVirtaDatastaSamaSuoritusKahdenParentinAlla(): Unit = {
    // Sama opintosuoritus (op400) sisältyy kahden eri opiskeluoikeuden tutkintoon, ks. VirtaParsingTest.testSameSuoritusUnderTwoDifferentTutkinto
    val opiskeluoikeudet = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaOpiskelijat(
      """
        |<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
        |  <SOAP-ENV:Body>
        |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
        |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
        |        <virta:Opiskelija avain="C15">
        |          <virta:Opiskeluoikeudet>
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
        |              <virta:Laajuus>
        |                <virta:Opintopiste>180.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>3</virta:Viisiportainen>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10089</virta:Myontaja>
        |              <virta:Laji>1</virta:Laji>
        |              <virta:Nimi kieli="fi">Kasvatustieteiden kandidaatti</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Koulutuskoodi>751101</virta:Koulutuskoodi>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="op400">
        |                <virta:Opintopiste>10.0</virta:Opintopiste>
        |              </virta:Sisaltyvyys>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus opiskeluoikeusAvain="xxx009" opiskelijaAvain="C15" koulutusmoduulitunniste="AVOIN2022" avain="avoin001">
        |              <virta:SuoritusPvm>2022-05-30</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>20.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Hyvaksytty>HYV</virta:Hyvaksytty>
        |              </virta:Arvosana>
        |              <virta:Myontaja>10088</virta:Myontaja>
        |              <virta:Laji>1</virta:Laji>
        |              <virta:Nimi kieli="fi">Kasvatustieteen opinnot</virta:Nimi>
        |              <virta:Kieli>fi</virta:Kieli>
        |              <virta:Sisaltyvyys sisaltyvaOpintosuoritusAvain="op400">
        |                <virta:Opintopiste>10.0</virta:Opintopiste>
        |              </virta:Sisaltyvyys>
        |            </virta:Opintosuoritus>
        |            <virta:Opintosuoritus opiskelijaAvain="C15" koulutusmoduulitunniste="PSY101" avain="op400">
        |              <virta:SuoritusPvm>2021-05-31</virta:SuoritusPvm>
        |              <virta:Laajuus>
        |                <virta:Opintopiste>10.0</virta:Opintopiste>
        |              </virta:Laajuus>
        |              <virta:Arvosana>
        |                <virta:Viisiportainen>4</virta:Viisiportainen>
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
        |</SOAP-ENV:Envelope>""".stripMargin
    ))
    val ooJaMeta = opiskeluoikeudet.map(oo => (META, oo))

    val rivit = EntityToOvaraConverter.litistaKKSuoritukset(
      EntityToOvaraConverter.getKKOpiskeluoikeudet(ooJaMeta),
      EntityToOvaraConverter.getKKSynteettisetOpiskeluoikeudet(ooJaMeta))
    tarkistaLitistyksenInvariantit(rivit)

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

  @Test def testLitistettyKKSuoritusSerialisoituuJsoniksi(): Unit = {
    // Sama ObjectMapper-konfiguraatio kuin SiirtotiedostoClientissa
    val mapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .registerModule(new Jdk8Module())
      .registerModule(DefaultScalaModule)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val lapsi = KKOpintosuoritus(UUID.fromString("00000000-0000-0000-0000-000000000102"), Some(kielistetty("l")), SuoritusTila.VALMIS, "komo-l", BigDecimal(5), None, Some(LocalDate.of(2023, 6, 1)), None, "myo", None, None, None, Some("4"), Some("4-1"), Some("fi"), None, None, opinnaytetyo = false, None, Seq.empty, "avain-l")
    val tutkinto = KKTutkinto(UUID.fromString("00000000-0000-0000-0000-000000000101"), None, SuoritusTila.VALMIS, "komo-t", BigDecimal(180), None, None, "myo", None, None, None, Seq(lapsi), None)
    val kk = KKOpiskeluoikeus(UUID.fromString("00000000-0000-0000-0000-000000000100"), "vt", None, "1", None, LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 1), koodi("v"), KKOpiskeluoikeusTila.PAATTYNYT, "myo", true, None, Set(tutkinto), None, None, None)
    val kkOo = EntityToOvaraConverter.getKKOpiskeluoikeudet(Seq((META, kk)))
    val rivit = EntityToOvaraConverter.litistaKKSuoritukset(kkOo, Seq.empty)
    val record = OvaraVersioJaOpiskeluoikeudet("1.2.246.562.24.1", OvaraHenkiloMetadata(Instant.parse("2024-03-01T00:00:00Z")),
      kkOo, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Seq.empty, litistetytKKSuoritukset = rivit)

    val json = mapper.readTree(mapper.writeValueAsString(record))
    val litistetyt = json.get("litistetytKKSuoritukset")
    Assertions.assertTrue(litistetyt.isArray)
    Assertions.assertEquals(2, litistetyt.size)

    val juuri = litistetyt.get(0)
    Assertions.assertEquals("KKTutkinto", juuri.get("entiteetinTyyppi").asText)
    Assertions.assertTrue(juuri.get("parentTunniste").isNull)
    Assertions.assertTrue(juuri.get("juuriSuoritusPolku").isArray)
    Assertions.assertEquals(0, juuri.get("juuriSuoritusPolku").size)
    Assertions.assertEquals("00000000-0000-0000-0000-000000000102", juuri.get("lapsiTunnisteet").get(0).asText)

    val l = litistetyt.get(1)
    Assertions.assertEquals("00000000-0000-0000-0000-000000000102", l.get("tunniste").asText)
    Assertions.assertEquals("00000000-0000-0000-0000-000000000100", l.get("opiskeluoikeusTunniste").asText)
    Assertions.assertEquals("KKOpiskeluoikeus", l.get("opiskeluoikeusTyyppi").asText)
    Assertions.assertEquals("00000000-0000-0000-0000-000000000101", l.get("parentTunniste").asText)
    Assertions.assertEquals("00000000-0000-0000-0000-000000000101", l.get("juuriSuoritusPolku").get(0).asText)
    Assertions.assertEquals("v", l.get("opiskeluoikeusVirtaTila").get("arvo").asText)
    Assertions.assertEquals("2020-09-01", l.get("opiskeluoikeusAlkuPvm").asText)
    Assertions.assertTrue(l.get("opiskeluoikeusKieli").isNull)
    Assertions.assertEquals("2023-06-01", l.get("suoritusPvm").asText)
    Assertions.assertEquals(5, l.get("opintoPisteet").asInt)
    Assertions.assertEquals("4", l.get("arvosana").asText)
    Assertions.assertFalse(l.get("opinnaytetyo").asBoolean)
    Assertions.assertTrue(l.get("aloitusPvm").isNull)
    Assertions.assertEquals("l_fi", l.get("nimi").get("fi").asText)
    Assertions.assertEquals("KOSKI", l.get("metadata").get("lahdejarjestelma").asText)
    Assertions.assertEquals("2024-01-01T00:00:00Z", l.get("metadata").get("luontiHetki").asText)
  }

  // Rakenteelliset invariantit, joiden pitää päteä mille tahansa litistetylle suorituspuulle
  private def tarkistaLitistyksenInvariantit(rivit: Seq[OvaraLitistettyKKSuoritus]): Unit = {
    Assertions.assertEquals(rivit.size, rivit.map(_.tunniste).distinct.size, "tunnisteet eivät ole uniikkeja")
    val tunnisteella = rivit.map(r => r.tunniste -> r).toMap
    val indeksi = rivit.map(_.tunniste).zipWithIndex.toMap
    rivit.foreach { r =>
      Assertions.assertEquals(r.juuriSuoritusPolku.lastOption, r.parentTunniste, s"${r.tunniste}: polun viimeinen ei ole parent")
      r.parentTunniste.foreach { p =>
        val parent = tunnisteella.getOrElse(p, Assertions.fail(s"${r.tunniste}: parentia $p ei löydy"))
        Assertions.assertTrue(parent.lapsiTunnisteet.contains(r.tunniste), s"${r.tunniste}: parent ei listaa lasta")
        Assertions.assertEquals(parent.juuriSuoritusPolku :+ p, r.juuriSuoritusPolku, s"${r.tunniste}: polku ei jatka parentin polkua")
        Assertions.assertEquals(parent.opiskeluoikeusTunniste, r.opiskeluoikeusTunniste, s"${r.tunniste}: eri opiskeluoikeus kuin parentilla")
        Assertions.assertTrue(indeksi(p) < indeksi(r.tunniste), s"${r.tunniste}: parent ei ole ennen lasta")
      }
      r.lapsiTunnisteet.foreach { l =>
        val lapsi = tunnisteella.getOrElse(l, Assertions.fail(s"${r.tunniste}: lasta $l ei löydy"))
        Assertions.assertEquals(Some(r.tunniste), lapsi.parentTunniste, s"$l: lapsi ei viittaa parentiin")
      }
    }
  }

  @Test def testGetYOOpiskeluoikeudet(): Unit = {
    val koe = Koe(UUID.randomUUID(), koodi("MA"), LocalDate.of(2024, 3, 15), koodi("E"), Some(80))
    val yot = YOTutkinto(UUID.randomUUID(), koodi("FI"), SuoritusTila.VALMIS, Some(LocalDate.of(2024, 6, 1)), Set(koe))
    val yo = YOOpiskeluoikeus(UUID.randomUUID(), Some(yot))
    val out = EntityToOvaraConverter.getYOOpiskeluoikeudet(Seq((META, yo))).head
    val outT = out.yoTutkinto.get
    Assertions.assertEquals(OvaraKoodi("FI", "ks", Some(1)), outT.suoritusKieli)
    Assertions.assertEquals(OvaraSuoritusTila.VALMIS, outT.supaTila)
    val outK = outT.aineet.head
    Assertions.assertEquals(OvaraKoodi("MA", "ks", Some(1)), outK.koodi)
    Assertions.assertEquals(OvaraKoodi("E", "ks", Some(1)), outK.arvosana)
    Assertions.assertEquals(Some(80), outK.pisteet)
  }

  @Test def testGetGeneerisetOpiskeluoikeudet_LukioDIAEBIBTuvaVST(): Unit = {
    val lop = LukionOppimaara(UUID.randomUUID(), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS, Some(LocalDate.of(2021, 8, 1)), Some(LocalDate.of(2024, 6, 1)), Some(koodi("FI")), Set(koodi("FI")))
    val diaOa = DIAOppiaine(UUID.randomUUID(), kielistetty("diaOa"), koodi("doa"), Some(DIALaajuus(BigDecimal(5), koodi("op"))), Some(koodi("kkt-1")), Some(koodi("FI")),
      Some(DIAVastaavuustodistuksenTiedot(BigDecimal(4.5), DIALaajuus(BigDecimal(150), koodi("op")))),
      Some(DIAOppiaineenKoesuoritus(kielistetty("kirj"), koodi("KIRJ"), DIAArvosana(koodi("4"), hyvaksytty = true), Some(DIALaajuus(BigDecimal(5), koodi("op"))))),
      None
    )
    val dia = DIATutkinto(UUID.randomUUID(), kielistetty("dia"), koodi("d"), OPPILAITOS, koodi("FI"), koodi("kt"), SuoritusTila.VALMIS, Some(LocalDate.of(2022, 8, 1)), Some(LocalDate.of(2024, 6, 1)), Seq(diaOa))
    val ebOs = EBOppiaineenOsasuoritus(kielistetty("ebOs"), koodi("FIN"), EBArvosana(koodi("8"), hyvaksytty = true), Some(LAAJUUS))
    val ebOa = EBOppiaine(UUID.randomUUID(), kielistetty("ebOa"), koodi("eboa"), Some(EBLaajuus(BigDecimal(5), koodi("op"))), Some(koodi("EN")), Seq(ebOs))
    val eb = EBTutkinto(UUID.randomUUID(), kielistetty("eb"), koodi("e"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS, Some(LocalDate.of(2022, 8, 1)), Some(LocalDate.of(2024, 6, 1)), Seq(ebOa))
    val ibOa = IBOppiaineSuoritus(UUID.randomUUID(), kielistetty("ibOa"), koodi("iboa"), Some(IBOppiaineRyhma(kielistetty("ryhma"), koodi("r1"))), Some(IBArvosana(koodi("6"), hyvaksytty = true)), Some(IBLaajuus(BigDecimal(150), koodi("h"))), Some(koodi("EN")), Some(koodi("EN")), Some(koodi("HL")))
    val ib = IBTutkinto(UUID.randomUUID(), kielistetty("ib"), koodi("i"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS, Some(LocalDate.of(2022, 8, 1)), Some(LocalDate.of(2024, 6, 1)), Some(koodi("EN")), Seq(ibOa))
    val tuva = Tuva(UUID.randomUUID(), kielistetty("tuva"), koodi("tk"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS, LocalDate.of(2023, 8, 1), Some(LocalDate.of(2024, 6, 1)), 2024, Some(LAAJUUS), List(LAHTOKOULU))
    val vst = VapaaSivistystyo(UUID.randomUUID(), kielistetty("vst"), koodi("vk"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS, LocalDate.of(2023, 8, 1), Some(LocalDate.of(2024, 6, 1)), 2024, Some(LAAJUUS), koodi("FI"), List(LAHTOKOULU))

    val oo = GeneerinenOpiskeluoikeus(UUID.randomUUID(), "1.2.246.562.15.0004", koodi("gen"), "1.2.246.562.10.1", Set(lop, dia, eb, ib, tuva, vst), Some(KOSKI_TILA), List(OO_JAKSO))
    val out = EntityToOvaraConverter.getGeneerisetOpiskeluoikeudet(Seq((META, oo))).head

    Assertions.assertEquals(6, out.suoritukset.size)
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraLukionOppimaara]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraDIATutkinto]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraEBTutkinto]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraIBTutkinto]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraTuva]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraVapaaSivistystyo]))

    // tila ja jaksot kanavoituvat oikein
    Assertions.assertEquals(Some(OvaraKoskiOpiskeluoikeusTila(List(OvaraKoskiOpiskeluoikeusJakso(LocalDate.of(2022, 1, 1), OvaraKoskiKoodi("loa", "k_koodisto", Some(3), OvaraKielistetty(Some("k_nimi_fi"), Some("k_nimi_sv"), Some("k_nimi_en")), Some(OvaraKielistetty(Some("k_lyh_fi"), Some("k_lyh_sv"), Some("k_lyh_en")))))))), out.tila)
    Assertions.assertEquals(List(OvaraOpiskeluoikeusJakso(LocalDate.of(2023, 1, 1), OvaraSuoritusTila.VALMIS)), out.jaksot)

    val outDia = out.suoritukset.collect { case d: OvaraDIATutkinto => d }.head
    val outDiaOa = outDia.osasuoritukset.head
    Assertions.assertEquals(Some(OvaraDIALaajuus(arvo = BigDecimal(5), yksikko = OvaraKoodi("op", "ks", Some(1)))), outDiaOa.laajuus)
    Assertions.assertEquals(Some(OvaraKielistetty(Some("kirj_fi"), Some("kirj_sv"), Some("kirj_en"))), outDiaOa.kirjallinenKoe.map(_.nimi))

    val outIb = out.suoritukset.collect { case i: OvaraIBTutkinto => i }.head
    Assertions.assertEquals(Some(OvaraIBOppiaineRyhma(nimi = OvaraKielistetty(Some("ryhma_fi"), Some("ryhma_sv"), Some("ryhma_en")), koodi = OvaraKoodi("r1", "ks", Some(1)))), outIb.osasuoritukset.head.ryhma)

    val outTuva = out.suoritukset.collect { case t: OvaraTuva => t }.head
    Assertions.assertEquals(OvaraOppilaitos(OvaraKielistetty(Some("opl_fi"), Some("opl_sv"), Some("opl_en")), "1.2.246.562.10.0001"), outTuva.oppilaitos)
    Assertions.assertEquals(Some(OvaraLaajuus(BigDecimal(60), OvaraKoodi("op", "ks", Some(1)), Some(OvaraKielistetty(Some("opintopiste_fi"), Some("opintopiste_sv"), Some("opintopiste_en"))), Some(OvaraKielistetty(Some("op_fi"), Some("op_sv"), Some("op_en"))))), outTuva.hyvaksyttyLaajuus)
  }

  @Test def testGetAmmatillisetOpiskeluoikeudet_KaikkiSuoritusvariantit(): Unit = {
    val pt = AmmatillinenPerustutkinto(UUID.randomUUID(), kielistetty("pt"), koodi("ptk"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS, Some(LocalDate.of(2022, 8, 1)), Some(LocalDate.of(2024, 6, 1)), Some(BigDecimal(4.5)), koodi("st"), koodi("FI"), Seq(OSA))
    val toi = AmmatillinenTutkintoOsittainen(UUID.randomUUID(), kielistetty("to"), koodi("tok"), OPPILAITOS, koodi("kt"), SuoritusTila.KESKEN, Some(LocalDate.of(2023, 1, 1)), None, Some(BigDecimal(4.0)), Some("1.2.246.562.15.0099"), koodi("st"), koodi("FI"), Seq(OSA))
    val at = AmmattiTutkinto(UUID.randomUUID(), kielistetty("at"), koodi("atk"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS, Some(LocalDate.of(2022, 1, 1)), Some(LocalDate.of(2024, 6, 1)), koodi("st"), koodi("FI"))
    val eat = ErikoisAmmattiTutkinto(UUID.randomUUID(), kielistetty("eat"), koodi("eatk"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS, Some(LocalDate.of(2022, 1, 1)), Some(LocalDate.of(2024, 6, 1)), koodi("FI"))
    val telma = Telma(UUID.randomUUID(), kielistetty("telma"), koodi("tek"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS, LocalDate.of(2023, 8, 1), Some(LocalDate.of(2024, 6, 1)), 2024, koodi("FI"), Some(LAAJUUS), List(LAHTOKOULU))
    val amm = AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.246.562.15.0005", OPPILAITOS, Set(pt, toi, at, eat, telma), Some(KOSKI_TILA), List(OO_JAKSO))

    val out = EntityToOvaraConverter.getAmmatillisetOpiskeluoikeudet(Seq((META, amm))).head

    Assertions.assertEquals(5, out.suoritukset.size)
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraAmmatillinenPerustutkinto]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraAmmatillinenTutkintoOsittainen]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraAmmattiTutkinto]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraErikoisAmmattiTutkinto]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraTelma]))

    val outPt = out.suoritukset.collect { case p: OvaraAmmatillinenPerustutkinto => p }.head
    val outOsa = outPt.osat.head
    Assertions.assertEquals(Some(OvaraArvosana(OvaraKoodi("arv", "ks", Some(1)), OvaraKielistetty(Some("arvosananimi_fi"), Some("arvosananimi_sv"), Some("arvosananimi_en")))), outOsa.arvosana)
    Assertions.assertEquals(Some(OvaraKorotus.KOROTETTU), outOsa.korotettu)
    val outOsaAlue = outOsa.osaAlueet.head
    Assertions.assertEquals(Some(OvaraKorotus.KOROTUKSENYRITYS), outOsaAlue.korotettu)
    Assertions.assertEquals(Some(OvaraKoodi("a3", "ks", Some(1))), outOsaAlue.arvosana)
  }

  @Test def testGetPerusopetuksenOpiskeluoikeudet_KaikkiSuoritusvariantit(): Unit = {
    val aine = PerusopetuksenOppiaine(UUID.randomUUID(), kielistetty("aine"), koodi("ai"), koodi("8"), Some(koodi("FI")), pakollinen = true, yksilollistetty = Some(false), rajattu = None)
    val om = PerusopetuksenOppimaara(
      UUID.randomUUID(), Some(UUID.randomUUID()), OPPILAITOS, Some("9A"), koodi("kt"), SuoritusTila.VALMIS, koodi("FI"), Set(koodi("FI")),
      Some(PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY),
      Some(LocalDate.of(2023, 8, 1)), Some(LocalDate.of(2024, 6, 1)), Seq(aine), List(LAHTOKOULU),
      syotetty = false, vuosiluokkiinSitoutumatonOpetus = false, luokkaAste = Some(9)
    )
    val oos = PerusopetuksenOppimaaranOppiaineidenSuoritus(UUID.randomUUID(), None, OPPILAITOS, koodi("kt"), SuoritusTila.KESKEN, koodi("FI"), Some(LocalDate.of(2023, 8, 1)), None, Set(aine), syotetty = true)
    val pvo = PerusopetukseenValmistavaOpetus(List(LAHTOKOULU))
    val po = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.0006"), "1.2.246.562.10.1", Set(om, oos, pvo), Some(KOSKI_LISATIEDOT), SuoritusTila.VALMIS, List(OO_JAKSO))

    val out = EntityToOvaraConverter.getPerusopetuksenOpiskeluoikeudet(Seq((META, po))).head

    Assertions.assertEquals(3, out.suoritukset.size)
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraPerusopetuksenOppimaara]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraPerusopetuksenOppimaaranOppiaineidenSuoritus]))
    Assertions.assertTrue(out.suoritukset.exists(_.isInstanceOf[OvaraPerusopetukseenValmistavaOpetus]))

    val outOm = out.suoritukset.collect { case x: OvaraPerusopetuksenOppimaara => x }.head
    Assertions.assertEquals(Some(OvaraPerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY), outOm.yksilollistaminen)
    Assertions.assertEquals(1, outOm.aineet.size)
    Assertions.assertEquals(OvaraKoodi("8", "ks", Some(1)), outOm.aineet.head.arvosana)
    Assertions.assertEquals(1, outOm.lahtokoulut.size)

    // KoskiLisatiedot konvertoituu kaikkine alikenttineen
    Assertions.assertEquals(
      Some(OvaraKoskiLisatiedot(
        erityisenTuenPäätökset = Some(List(OvaraKoskiErityisenTuenPaatos(Some(true)))),
        vuosiluokkiinSitoutumatonOpetus = Some(false),
        kotiopetusjaksot = Some(List(OvaraKoskiKotiopetusjakso("2023-09-01", Some("2024-05-31"))))
      )),
      out.lisatiedot
    )
    Assertions.assertEquals(OvaraSuoritusTila.VALMIS, out.tila)
    Assertions.assertEquals(List(OvaraOpiskeluoikeusJakso(LocalDate.of(2023, 1, 1), OvaraSuoritusTila.VALMIS)), out.jaksot)
  }

  @Test def testGetPoistetutOpiskeluoikeudet(): Unit = {
    val p = PoistettuOpiskeluoikeus("1.2.246.562.15.0007")
    val out = EntityToOvaraConverter.getPoistetutOpiskeluoikeudet(Seq((META, p))).head
    Assertions.assertEquals(META, out.metadata)
    Assertions.assertEquals("1.2.246.562.15.0007", out.oid)
  }

  @Test def testGetLahtokoulutKokoaaKaikistaEntiteeteista(): Unit = {
    def lk(alku: LocalDate, tyyppi: LahtokouluTyyppi, oppilaitosSuffix: String): Lahtokoulu =
      LAHTOKOULU.copy(suorituksenAlku = alku, suoritusTyyppi = tyyppi, oppilaitosOid = s"1.2.246.562.10.$oppilaitosSuffix")

    val lkTuva  = lk(LocalDate.of(2024, 8, 1), LahtokouluTyyppi.TUVA, "001")
    val lkVst   = lk(LocalDate.of(2024, 8, 2), LahtokouluTyyppi.VAPAA_SIVISTYSTYO, "002")
    val lkTelma = lk(LocalDate.of(2024, 8, 3), LahtokouluTyyppi.TELMA, "003")
    val lkPom   = lk(LocalDate.of(2024, 8, 4), LahtokouluTyyppi.VUOSILUOKKA_9, "004")
    val lkPvo   = lk(LocalDate.of(2024, 8, 5), LahtokouluTyyppi.PERUSOPETUKSEEN_VALMISTAVA_OPETUS, "005")

    val tuva = Tuva(UUID.randomUUID(), kielistetty("tuva"), koodi("tk"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS,
      LocalDate.of(2023, 8, 1), Some(LocalDate.of(2024, 6, 1)), 2024, Some(LAAJUUS), List(lkTuva))
    val vst = VapaaSivistystyo(UUID.randomUUID(), kielistetty("vst"), koodi("vk"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS,
      LocalDate.of(2023, 8, 1), Some(LocalDate.of(2024, 6, 1)), 2024, Some(LAAJUUS), koodi("FI"), List(lkVst))
    // PerusopetukseenValmistavaOpetus elää GeneerinenOpiskeluoikeus-puolella (ks. KoskiUtil.getLahtokouluMetadata).
    val pvo = PerusopetukseenValmistavaOpetus(List(lkPvo))
    val genOo = GeneerinenOpiskeluoikeus(UUID.randomUUID(), "1.2.246.562.15.0011", koodi("gen"), "1.2.246.562.10.1", Set(tuva, vst, pvo), None, List.empty)

    val telma = Telma(UUID.randomUUID(), kielistetty("telma"), koodi("tek"), OPPILAITOS, koodi("kt"), SuoritusTila.VALMIS,
      LocalDate.of(2023, 8, 1), Some(LocalDate.of(2024, 6, 1)), 2024, koodi("FI"), Some(LAAJUUS), List(lkTelma))
    val ammOo = AmmatillinenOpiskeluoikeus(UUID.randomUUID(), "1.2.246.562.15.0012", OPPILAITOS, Set(telma), None, List.empty)

    val pom = PerusopetuksenOppimaara(UUID.randomUUID(), None, OPPILAITOS, Some("9A"), koodi("kt"), SuoritusTila.VALMIS, koodi("FI"), Set(koodi("FI")),
      None, Some(LocalDate.of(2023, 8, 1)), Some(LocalDate.of(2024, 6, 1)), Seq.empty, List(lkPom),
      syotetty = false, vuosiluokkiinSitoutumatonOpetus = false, luokkaAste = Some(9))
    val pkOo = PerusopetuksenOpiskeluoikeus(UUID.randomUUID(), Some("1.2.246.562.15.0013"), "1.2.246.562.10.1", Set(pom), None, SuoritusTila.VALMIS, List.empty)

    val out = EntityToOvaraConverter.getLahtokoulut(Set[Opiskeluoikeus](genOo, ammOo, pkOo))

    Assertions.assertEquals(5, out.size)
    Assertions.assertEquals(
      Seq(LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 2), LocalDate.of(2024, 8, 3), LocalDate.of(2024, 8, 4), LocalDate.of(2024, 8, 5)),
      out.map(_.suorituksenAlku)
    )
    // Kaikki viisi tyyppiä mukana
    Assertions.assertEquals(
      Set(OvaraLahtokouluTyyppi.TUVA, OvaraLahtokouluTyyppi.VAPAA_SIVISTYSTYO, OvaraLahtokouluTyyppi.TELMA, OvaraLahtokouluTyyppi.VUOSILUOKKA_9, OvaraLahtokouluTyyppi.PERUSOPETUKSEEN_VALMISTAVA_OPETUS),
      out.map(_.suoritusTyyppi).toSet
    )
  }

  @Test def testGetLahtokoulutTyhjaKkYo(): Unit = {
    // KK/YO eivät kanna lähtökouluja — palautuu tyhjä lista vaikka opiskeluoikeuksia ja niiden suorituksia on olemassa.
    val kkTutkinto = KKTutkinto(UUID.randomUUID(), Some(kielistetty("t")), SuoritusTila.VALMIS, "komo", BigDecimal(180),
      Some(LocalDate.of(2020, 9, 1)), Some(LocalDate.of(2024, 6, 1)), "myo", Some("fi"), Some("613101"), Some("a-1"), Seq.empty, Some("avain-t"))
    val kk = KKOpiskeluoikeus(UUID.randomUUID(), "vt", None, "1", None, LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 1),
      koodi("v"), KKOpiskeluoikeusTila.VOIMASSA, "myo", true, None, Set(kkTutkinto), None, None, None)
    val koe = Koe(UUID.randomUUID(), koodi("MA"), LocalDate.of(2024, 3, 15), koodi("E"), Some(80))
    val yot = YOTutkinto(UUID.randomUUID(), koodi("FI"), SuoritusTila.VALMIS, Some(LocalDate.of(2024, 6, 1)), Set(koe))
    val yo = YOOpiskeluoikeus(UUID.randomUUID(), Some(yot))

    val out = EntityToOvaraConverter.getLahtokoulut(Set[Opiskeluoikeus](kk, yo))
    Assertions.assertTrue(out.isEmpty)
  }
}
