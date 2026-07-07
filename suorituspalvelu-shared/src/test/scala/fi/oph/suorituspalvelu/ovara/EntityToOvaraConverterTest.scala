package fi.oph.suorituspalvelu.ovara

import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.parsing.koski.{
  Kielistetty, KoskiErityisenTuenPaatos, KoskiKotiopetusjakso, KoskiKoodi,
  KoskiLisatiedot, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila
}
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
