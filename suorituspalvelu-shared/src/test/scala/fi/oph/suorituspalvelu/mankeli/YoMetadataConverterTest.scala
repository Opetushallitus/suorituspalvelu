package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business.SuoritusTila.KESKEN
import fi.oph.suorituspalvelu.business.{Koe, Koodi, Opiskeluoikeus, Oppilaitos, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, SuoritusTila, YOOpiskeluoikeus, YOTutkinto}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, DateParam, Hakutoive, KoutaHakukohde, Ohjausparametrit}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainMetatiedotDTO, HarkinnanvaraisuudenSyy, HarkinnanvaraisuusPaattely, YoMetadataConverter}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import org.junit.jupiter.api.{Assertions, Test, TestInstance}
import org.junit.jupiter.api.TestInstance.Lifecycle

import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.time.{Instant, LocalDate, ZoneId}
import java.util.UUID

@Test
@TestInstance(Lifecycle.PER_CLASS)
class YoMetadataConverterTest {

  val PERSON_OID = "1.2.246.562.24.12345678901"
  val HAKEMUS_OID = "1.2.246.562.11.12345678901"
  val HAKU_OID = "1.2.246.562.29.12345678901"
  val HAKUKOHDE_OID_1 = "1.2.246.562.20.12345678901"
  val HAKUKOHDE_OID_2 = "1.2.246.562.20.12345678902"

  val BASE_HAKEMUS = AtaruValintalaskentaHakemus(
    hakemusOid = HAKEMUS_OID,
    personOid = PERSON_OID,
    hakuOid = HAKU_OID,
    asiointikieli = "fi",
    hakutoiveet = List(
      Hakutoive(
        processingState = "unprocessed",
        eligibilityState = "eligible",
        paymentObligation = "not-obligated",
        kkApplicationPaymentObligation = "unreviewed",
        hakukohdeOid = HAKUKOHDE_OID_1,
        languageRequirement = "unreviewed",
        degreeRequirement = "unreviewed",
        harkinnanvaraisuus = None
      ),
      Hakutoive(
        processingState = "unprocessed",
        eligibilityState = "eligible",
        paymentObligation = "not-obligated",
        kkApplicationPaymentObligation = "unreviewed",
        hakukohdeOid = HAKUKOHDE_OID_2,
        languageRequirement = "unreviewed",
        degreeRequirement = "unreviewed",
        harkinnanvaraisuus = None
      )
    ),
    maksuvelvollisuus = Map.empty,
    keyValues = Map.empty
  )

  val DEFAULT_OHJAUSPARAMETRIT = Ohjausparametrit(
    PH_HKP = None,
    suoritustenVahvistuspaiva = Some(DateParam(
      date = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli  // 30 days ago
    ))
  )

  @Test
  def testConvertYoBaseValues(): Unit = {
    val yoOpiskeluoikeus =
      YOOpiskeluoikeus(
        UUID.randomUUID(),
        Some(YOTutkinto(
          UUID.randomUUID(),
          Koodi("FI", "kieli", Some(1)),
          SuoritusTila.VALMIS,
          Some(LocalDate.parse("2022-06-01")),
          Set(
            Koe(UUID.randomUUID(), Koodi("EA", "yokokeet", Some(1)), LocalDate.parse("2022-06-01"), Koodi("M", "koskiyoarvosanat", Some(1)), Some(95)),
            Koe(UUID.randomUUID(), Koodi("PS", "yokokeet", Some(1)), LocalDate.parse("2022-12-21"), Koodi("M", "koskiyoarvosanat", Some(1)), Some(103)),
            Koe(UUID.randomUUID(), Koodi("A", "yokokeet", Some(1)), LocalDate.parse("2022-06-01"), Koodi("E", "koskiyoarvosanat", Some(1)), Some(40)),
            Koe(UUID.randomUUID(), Koodi("N", "yokokeet", Some(1)), LocalDate.parse("2022-06-01"), Koodi("A", "koskiyoarvosanat", Some(1)), Some(66)),
            Koe(UUID.randomUUID(), Koodi("BB", "yokokeet", Some(1)), LocalDate.parse("2022-06-01"), Koodi("B", "koskiyoarvosanat", Some(1)), Some(102)),
            Koe(UUID.randomUUID(), Koodi("EA", "yokokeet", Some(1)), LocalDate.parse("2022-12-21"), Koodi("L", "koskiyoarvosanat", Some(1)), Some(140)),
            Koe(UUID.randomUUID(), Koodi("YH", "yokokeet", Some(1)), LocalDate.parse("2022-06-01"), Koodi("C", "koskiyoarvosanat", Some(1)), Some(140)),
            Koe(UUID.randomUUID(), Koodi("FB", "yokokeet", Some(1)), LocalDate.parse("2022-06-01"), Koodi("E", "koskiyoarvosanat", Some(1)), Some(122)),
            Koe(UUID.randomUUID(), Koodi("SC", "yokokeet", Some(1)), LocalDate.parse("2022-06-01"), Koodi("B", "koskiyoarvosanat", Some(1)), Some(99)),
          )
        ))
      )

    val convertedArvot = YoMetadataConverter.convert(Seq(yoOpiskeluoikeus))
    println(s"Converted arvot: $convertedArvot")

    //N Lyhyt matematiikka, yksi koesuoritus
    val lyhytMatematiikka: Map[String, String] = convertedArvot.find(_.avain.equals("N")).get.metatiedot.head
    println(s"Lyhyt matematiikka: $lyhytMatematiikka")
    Assertions.assertEquals("A", lyhytMatematiikka("ARVO"))
    Assertions.assertEquals("66", lyhytMatematiikka("PISTEET"))
    Assertions.assertEquals("2022", lyhytMatematiikka("SUORITUSVUOSI"))
    Assertions.assertEquals("1", lyhytMatematiikka("SUORITUSLUKUKAUSI"))

    //EA Englanti, pitkä, kaksi koesuoritusta
    val englanti = convertedArvot.find(_.avain.equals("EA")).get
    val englantiKoe1 = englanti.metatiedot.find(_("ARVO").equals("M")).get
    Assertions.assertEquals("M", englantiKoe1("ARVO"))
    Assertions.assertEquals("95", englantiKoe1("PISTEET"))
    Assertions.assertEquals("2022", englantiKoe1("SUORITUSVUOSI"))
    Assertions.assertEquals("1", englantiKoe1("SUORITUSLUKUKAUSI"))

    val englantiKoe2 = englanti.metatiedot.find(_("ARVO").equals("L")).get
    Assertions.assertEquals("L", englantiKoe2("ARVO"))
    Assertions.assertEquals("140", englantiKoe2("PISTEET"))
    Assertions.assertEquals("2022", englantiKoe2("SUORITUSVUOSI"))
    Assertions.assertEquals("2", englantiKoe2("SUORITUSLUKUKAUSI"))

    //YH Yhteiskuntaoppi, yksi koesuoritus
    val yhteiskuntaoppi = convertedArvot.find(_.avain.equals("YH")).get.metatiedot.head
    Assertions.assertEquals("C", yhteiskuntaoppi("ARVO"))
    Assertions.assertEquals("140", yhteiskuntaoppi("PISTEET"))
    Assertions.assertEquals("2022", yhteiskuntaoppi("SUORITUSVUOSI"))
    Assertions.assertEquals("1", yhteiskuntaoppi("SUORITUSLUKUKAUSI"))

    //PS Psykologia, yksi koesuoritus
    val psykologia = convertedArvot.find(_.avain.equals("PS")).get.metatiedot.head
    Assertions.assertEquals("M", psykologia("ARVO"))
    Assertions.assertEquals("103", psykologia("PISTEET"))
    Assertions.assertEquals("2022", psykologia("SUORITUSVUOSI"))
    Assertions.assertEquals("2", psykologia("SUORITUSLUKUKAUSI"))

    //A Äidinkieli (suomi), yksi koesuoritus
    val aidinkieli = convertedArvot.find(_.avain.equals("A")).get.metatiedot.head
    Assertions.assertEquals("E", aidinkieli("ARVO"))
    Assertions.assertEquals("40", aidinkieli("PISTEET"))
    Assertions.assertEquals("2022", aidinkieli("SUORITUSVUOSI"))
    Assertions.assertEquals("1", aidinkieli("SUORITUSLUKUKAUSI"))

    //BB Ruotsi keskipitkä oppimäärä, yksi koesuoritus
    val ruotsi = convertedArvot.find(_.avain.equals("BB")).get.metatiedot.head
    Assertions.assertEquals("B", ruotsi("ARVO"))
    Assertions.assertEquals("102", ruotsi("PISTEET"))
    Assertions.assertEquals("2022", ruotsi("SUORITUSVUOSI"))
    Assertions.assertEquals("1", ruotsi("SUORITUSLUKUKAUSI"))

    //FB Ranska keskipitkä oppimäärä, yksi koesuoritus
    val ranska = convertedArvot.find(_.avain.equals("FB")).get.metatiedot.head
    Assertions.assertEquals("E", ranska("ARVO"))
    Assertions.assertEquals("122", ranska("PISTEET"))
    Assertions.assertEquals("2022", ranska("SUORITUSVUOSI"))
    Assertions.assertEquals("1", ranska("SUORITUSLUKUKAUSI"))

    //SC saksa lyhyt oppimäärä, yksi koesuoritus
    val saksa = convertedArvot.find(_.avain.equals("SC")).get.metatiedot.head
    Assertions.assertEquals("B", saksa("ARVO"))
    Assertions.assertEquals("99", saksa("PISTEET"))
    Assertions.assertEquals("2022", saksa("SUORITUSVUOSI"))
    Assertions.assertEquals("1", saksa("SUORITUSLUKUKAUSI"))

    //AIDINKIELI-ryhma, yksi koesuoritus, sama kuin "A" mutta kielen kertovalla lisätiedolla
    val aidinkieliRyhma = convertedArvot.find(_.avain.equals("AIDINKIELI")).get.metatiedot.head
    Assertions.assertEquals("E", aidinkieliRyhma("ARVO"))
    Assertions.assertEquals("40", aidinkieliRyhma("PISTEET"))
    Assertions.assertEquals("2022", aidinkieliRyhma("SUORITUSVUOSI"))
    Assertions.assertEquals("1", aidinkieliRyhma("SUORITUSLUKUKAUSI"))
    Assertions.assertEquals("FI", aidinkieliRyhma("LISATIETO"))

    //PITKA_KIELI-ryhma, kaksi koesuoritusta, samat kuin "EA" mutta kielen kertovalla lisätiedolla
    val pitkaKieliRyhma = convertedArvot.find(_.avain.equals("PITKA_KIELI")).get.metatiedot

    val pitkaKieliKoesuoritus1 = pitkaKieliRyhma.find(_("ARVO").equals("M")).get
    Assertions.assertEquals("M", pitkaKieliKoesuoritus1("ARVO"))
    Assertions.assertEquals("95", pitkaKieliKoesuoritus1("PISTEET"))
    Assertions.assertEquals("2022", pitkaKieliKoesuoritus1("SUORITUSVUOSI"))
    Assertions.assertEquals("1", pitkaKieliKoesuoritus1("SUORITUSLUKUKAUSI"))
    Assertions.assertEquals("EN", pitkaKieliKoesuoritus1("LISATIETO"))

    val pitkaKieliKoesuoritus2 = pitkaKieliRyhma.find(_("ARVO").equals("L")).get
    Assertions.assertEquals("L", pitkaKieliKoesuoritus2("ARVO"))
    Assertions.assertEquals("140", pitkaKieliKoesuoritus2("PISTEET"))
    Assertions.assertEquals("2022", pitkaKieliKoesuoritus2("SUORITUSVUOSI"))
    Assertions.assertEquals("2", pitkaKieliKoesuoritus2("SUORITUSLUKUKAUSI"))
    Assertions.assertEquals("EN", pitkaKieliKoesuoritus2("LISATIETO"))

    //KESKIPITKA_KIELI-ryhma, yksi koesuoritus, samat kuin "FB" mutta kielen kertovalla lisätiedolla
    val keskipitkaKieliRyhma = convertedArvot.find(_.avain.equals("KESKIPITKA_KIELI")).get.metatiedot

    val keskipitkaKieliKoesuoritus1 = keskipitkaKieliRyhma.head
    Assertions.assertEquals("E", keskipitkaKieliKoesuoritus1("ARVO"))
    Assertions.assertEquals("122", keskipitkaKieliKoesuoritus1("PISTEET"))
    Assertions.assertEquals("2022", keskipitkaKieliKoesuoritus1("SUORITUSVUOSI"))
    Assertions.assertEquals("1", keskipitkaKieliKoesuoritus1("SUORITUSLUKUKAUSI"))
    Assertions.assertEquals("RA", keskipitkaKieliKoesuoritus1("LISATIETO"))

    //LYHYT_KIELI-ryhma, yksi koesuoritus, samat kuin "SC" mutta kielen kertovalla lisätiedolla
    val lyhytKieliRyhma = convertedArvot.find(_.avain.equals("LYHYT_KIELI")).get.metatiedot

    val lyhytKieliKoesuoritus1 = lyhytKieliRyhma.head
    Assertions.assertEquals("B", lyhytKieliKoesuoritus1("ARVO"))
    Assertions.assertEquals("99", lyhytKieliKoesuoritus1("PISTEET"))
    Assertions.assertEquals("2022", lyhytKieliKoesuoritus1("SUORITUSVUOSI"))
    Assertions.assertEquals("1", lyhytKieliKoesuoritus1("SUORITUSLUKUKAUSI"))
    Assertions.assertEquals("SA", lyhytKieliKoesuoritus1("LISATIETO"))
  }

  @Test
  def testImprobaturValues(): Unit = {
    val yoOpiskeluoikeus =
      YOOpiskeluoikeus(
        UUID.randomUUID(),
        Some(YOTutkinto(
          UUID.randomUUID(),
          Koodi("FI", "kieli", Some(1)),
          SuoritusTila.VALMIS,
          Some(LocalDate.parse("2022-06-01")),
          Set(
            Koe(UUID.randomUUID(), Koodi("EA", "yokokeet", Some(1)), LocalDate.parse("2022-06-01"), Koodi("I+", "koskiyoarvosanat", Some(1)), Some(95)),
            Koe(UUID.randomUUID(), Koodi("PS", "yokokeet", Some(1)), LocalDate.parse("2022-12-21"), Koodi("I-", "koskiyoarvosanat", Some(1)), Some(103)),
            Koe(UUID.randomUUID(), Koodi("A", "yokokeet", Some(1)), LocalDate.parse("2022-06-01"), Koodi("I=", "koskiyoarvosanat", Some(1)), Some(40)),
            Koe(UUID.randomUUID(), Koodi("N", "yokokeet", Some(1)), LocalDate.parse("2022-06-01"), Koodi("I", "koskiyoarvosanat", Some(1)), Some(66)),
          )
        ))
      )

    val convertedArvot = YoMetadataConverter.convert(Seq(yoOpiskeluoikeus))

    //N Lyhyt matematiikka, yksi koesuoritus (I)
    val lyhytMatematiikka: Map[String, String] = convertedArvot.find(_.avain.equals("N")).get.metatiedot.head
    Assertions.assertEquals("I", lyhytMatematiikka("ARVO"))

    //EA Englanti, pitkä, yksi koesuoritus (I+)
    val englanti = convertedArvot.find(_.avain.equals("EA")).get.metatiedot.head
    Assertions.assertEquals("I", englanti("ARVO"))

    //PS Psykologia, yksi koesuoritus (I-)
    val psykologia = convertedArvot.find(_.avain.equals("PS")).get.metatiedot.head
    Assertions.assertEquals("I", psykologia("ARVO"))

    //A Äidinkieli (suomi), yksi koesuoritus (I=)
    val aidinkieli = convertedArvot.find(_.avain.equals("A")).get.metatiedot.head
    Assertions.assertEquals("I", aidinkieli("ARVO"))

  }
}
