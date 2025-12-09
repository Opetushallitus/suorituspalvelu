package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, AvainArvoYliajo, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Laajuus, PerusopetuksenOppimaaranOppiaineidenSuoritus, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, Suoritus, Telma, VapaaSivistystyo, YOOpiskeluoikeus}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, KoutaHaku}
import org.slf4j.LoggerFactory

import java.time.LocalDate
import scala.collection.immutable

class UseitaVahvistettujaOppimaariaException(val message: String) extends RuntimeException(message)

//Opiskeluoikeudet sisältävät kaiken lähdedatan, käyttö nykyisellään vain debug-tarkoituksiin.
case class AvainArvoConverterResults(personOid: String,
                                     paatellytArvot: Set[AvainArvoContainer],
                                     convertedHakemus: Option[ConvertedAtaruHakemus], //Myös hakemus sisältää avain-arvoja. Niitä ei kuitenkaan voi yliajaa.
                                     opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq.empty) {
  //Todo, tämän voisi refaktoroida pois ja testit nojaamaan johonkin muuhun. Datan yhdistely eri tarpeisiin ValintaDataServicessä, ja testaus myös siellä.
  def getAvainArvoMap(): Map[String, String] = {
    paatellytArvot.map(aa => aa.avain -> aa.arvo).toMap
  }
}

case class AvainArvoContainer(avain: String,
                              arvo: String,
                              selitteet: Seq[String] = Seq.empty)

object AvainArvoConstants {

  //Tämä taulukko sisältää avainten selitteet, jotka voidaan näyttää käyttöliittymässä. Taulukko sisältää myös kaikki
  // mahdolliset arvot, joita AvainArvoConverter tuottaa.
  // Peruskoulun aineiden arvosanojen osalta perustuu koodiston koskioppiaineetyleissivistava koodiarvoille (29 kpl).
  val avainToAvaimenSeliteMap = Map(
    perusopetuksenKieliKey -> "Perusopetuksen kieli",
    peruskouluPaattotodistusvuosiKey -> "Peruskoulun päättötodistusvuosi",
    peruskouluSuoritusvuosiKey -> "Peruskoulun suoritusvuosi",
    peruskouluSuoritettuKey -> "Peruskoulu suoritettu",
    lukioSuoritettuKey -> "Lukio suoritettu",
    yoSuoritettuKey -> "Ylioppilastutkinto suoritettu",
    ammSuoritettuKey -> "Ammatillinen tutkinto suoritettu",
    telmaSuoritettuKey -> "Lisäpistekoulutus Telma suoritettu",
    telmaSuoritusvuosiKey -> "Lisäpistekoulutus Telma suoritusvuosi",
    opistovuosiSuoritettuKey -> "Lisäpistekoulutus opistovuosi suoritettu",
    opistovuosiSuoritusvuosiKey -> "Lisäpistekoulutus opistovuosi suoritusvuosi",
    peruskouluArvosana_Akieli -> "Peruskoulun arvosana: A-kieli",
    peruskouluArvosana_A1kieli -> "Peruskoulun arvosana: A1-kieli",
    peruskouluArvosana_A2kieli -> "Peruskoulun arvosana: A2-kieli",
    peruskouluArvosana_A3kieli -> "Peruskoulun arvosana: A3-kieli",
    peruskouluArvosana_B1kieli -> "Peruskoulun arvosana: B1-kieli",
    peruskouluArvosana_B2kieli -> "Peruskoulun arvosana: B2-kieli",
    peruskouluArvosana_biologia -> "Peruskoulun arvosana: Biologia",
    peruskouluArvosana_eitiedossa -> "Peruskoulun arvosana: Ei tiedossa",
    peruskouluArvosana_filosofia -> "Peruskoulun arvosana: Filosofia",
    peruskouluArvosana_fysiikka -> "Peruskoulun arvosana: Fysiikka",
    peruskouluArvosana_historia -> "Peruskoulun arvosana: Historia",
    peruskouluArvosana_kemia -> "Peruskoulun arvosana: Kemia",
    peruskouluArvosana_kotitalous -> "Peruskoulun arvosana: Kotitalous",
    peruskouluArvosana_kuvataide -> "Peruskoulun arvosana: Kuvataide",
    peruskouluArvosana_kasityo -> "Peruskoulun arvosana: Käsityö",
    peruskouluArvosana_liikunta -> "Peruskoulun arvosana: Liikunta",
    peruskouluArvosana_maantieto -> "Peruskoulun arvosana: Maantieto",
    peruskouluArvosana_matematiikka -> "Peruskoulun arvosana: Matematiikka",
    peruskouluArvosana_musiikki -> "Peruskoulun arvosana: Musiikki",
    peruskouluArvosana_opintoohjaus -> "Peruskoulun arvosana: Opinto-ohjaus",
    peruskouluArvosana_tyoelamataidot -> "Peruskoulun arvosana: Työelämätaidot",
    peruskouluArvosana_psykologia -> "Peruskoulun arvosana: Psykologia",
    peruskouluArvosana_terveystieto -> "Peruskoulun arvosana: Terveystieto",
    peruskouluArvosana_uskontoet -> "Peruskoulun arvosana: Uskonto/ET",
    peruskouluArvosana_uskontoet2 -> "Peruskoulun arvosana: Katsomusaine",
    peruskouluArvosana_yhteiskuntaoppi -> "Peruskoulun arvosana: Yhteiskuntaoppi",
    peruskouluArvosana_ymparistooppi -> "Peruskoulun arvosana: Ympäristöoppi",
    peruskouluArvosana_aidinkielenomainen -> "Peruskoulun arvosana: Äidinkielenomainen",
    peruskouluArvosana_aidinkielijakirjallisuus -> "Peruskoulun arvosana: Äidinkieli ja kirjallisuus",
    peruskouluOppiaineenKieli_A1 -> "Peruskoulun A1-kielen oppiaine",
    peruskouluOppiaineenKieli_A2 -> "Peruskoulun A2-kielen oppiaine",
    peruskouluOppiaineenKieli_A3 -> "Peruskoulun A3-kielen oppiaine",
    peruskouluOppiaineenKieli_B1 -> "Peruskoulun B1-kielen oppiaine",
    peruskouluOppiaineenKieli_B2 -> "Peruskoulun B2-kielen oppiaine",
    peruskouluOppiaineenKieli_AI -> "Peruskoulun äidinkielen oppiaine"
  )

  final val perusopetuksenKieliKey = "perusopetuksen_kieli"
  final val peruskouluPaattotodistusvuosiKey = "PK_PAATTOTODISTUSVUOSI"
  final val peruskouluSuoritusvuosiKey = "PK_SUORITUSVUOSI"
  final val peruskouluSuoritettuKey = "PK_TILA"
  final val lukioSuoritettuKey = "LK_TILA"
  final val yoSuoritettuKey = "YO_TILA"
  final val ammSuoritettuKey = "AM_TILA"

  //Nämä suoritusavaimet ja suoritusvuosiavaimet poikkeavat toisistaan vähän hämäävästi.
  // Ne kuitenkin vastaavat nykyistä proxysuoritusrajapinnan mallia.
  final val telmaSuoritettuKey = "LISAKOULUTUS_TELMA"
  final val telmaSuoritusvuosiKey = "LISAPISTEKOULUTUS_TELMA_SUORITUSVUOSI"
  final val opistovuosiSuoritettuKey = "LISAKOULUTUS_OPISTOVUOSI"
  final val opistovuosiSuoritusvuosiKey = "LISAPISTEKOULUTUS_OPISTO_SUORITUSVUOSI"

  final val peruskouluArvosana_Akieli = "PK_A"
  final val peruskouluArvosana_A1kieli = "PK_A1"
  final val peruskouluArvosana_A2kieli = "PK_A2"
  final val peruskouluArvosana_A3kieli = "PK_B1"
  final val peruskouluArvosana_B1kieli = "PK_B2"
  final val peruskouluArvosana_B2kieli = "PK_B3"
  final val peruskouluArvosana_biologia = "PK_BI"
  final val peruskouluArvosana_eitiedossa = "PK_XX"
  final val peruskouluArvosana_filosofia = "PK_FI"
  final val peruskouluArvosana_fysiikka = "PK_FY"
  final val peruskouluArvosana_historia = "PK_HI"
  final val peruskouluArvosana_kemia = "PK_KE"
  final val peruskouluArvosana_kotitalous = "PK_KO"
  final val peruskouluArvosana_kuvataide = "PK_KU"
  final val peruskouluArvosana_kasityo = "PK_KS"
  final val peruskouluArvosana_liikunta = "PK_LI"
  final val peruskouluArvosana_maantieto = "PK_GE"
  final val peruskouluArvosana_matematiikka = "PK_MA"
  final val peruskouluArvosana_musiikki = "PK_MU"
  final val peruskouluArvosana_opintoohjaus = "PK_OP"
  final val peruskouluArvosana_tyoelamataidot = "PK_OPA"
  final val peruskouluArvosana_psykologia = "PK_PS"
  final val peruskouluArvosana_terveystieto = "PK_TE"
  final val peruskouluArvosana_uskontoet = "PK_ET"
  final val peruskouluArvosana_uskontoet2 = "PK_KT"
  final val peruskouluArvosana_yhteiskuntaoppi = "PK_YH"
  final val peruskouluArvosana_ymparistooppi = "PK_YL"
  final val peruskouluArvosana_aidinkielenomainen = "PK_AOM"
  final val peruskouluArvosana_aidinkielijakirjallisuus = "PK_AI"

  //Todo, varmistettava että tässä on kaikki aineet joille on tarpeen merkitä kieli erillisen avaimen alle
  final val peruskouluOppiaineenKieli_A1 = "PK_A1_OPPIAINE"
  final val peruskouluOppiaineenKieli_A2 = "PK_A2_OPPIAINE"
  final val peruskouluOppiaineenKieli_A3 = "PK_A3_OPPIAINE"
  final val peruskouluOppiaineenKieli_B1 = "PK_B1_OPPIAINE"
  final val peruskouluOppiaineenKieli_B2 = "PK_B2_OPPIAINE"
  final val peruskouluOppiaineenKieli_AI = "PK_AI_OPPIAINE"

  final val peruskouluAineenArvosanaPrefix = "PK_"

  //Nämä tulevat aineen arvosanojen perään, eli esimerkiksi jos varsinainen arvosana
  // on avaimen "PK_B1" alla, tulee kieli avaimen "PK_B1_OPPIAINE" alle
  final val peruskouluAineenKieliPostfix = "_OPPIAINE"

  //Lisäpistekoulutusten minimilaajuudet
  final val telmaMinimiLaajuus: BigDecimal = 25
  final val tuvaMinimiLaajuus: BigDecimal = 19
  final val opistovuosiMinimiLaajuus: BigDecimal = 26.5

  val elibilityAtaruTilaToValintalaskentaTila = Map(
    "eligible" -> "ELIGIBLE",
    "uneligible" -> "INELIGIBLE",
    "unreviewed" -> "NOT_CHECKED",
    "conditionally-eligible" -> "CONDITIONALLY_ELIGIBLE"
  )
}

object PerusopetuksenArvosanaOrdering {
  private val letterGradeOrder = Map(
    "S" -> 2, // Suoritettu
    "O" -> 1, // Osallistunut
    "H" -> 0 // Hylätty
  )

  private val numericGrades = Set("10", "9", "8", "7", "6", "5", "4")

  def compareArvosana(a: String, b: String): Int = {
    def isNumeric(s: String) = numericGrades.contains(s)

    (isNumeric(a), isNumeric(b)) match {
      case (true, true) => a.toInt.compare(b.toInt)
      case (true, false) => 1 // Numeric always wins over letter
      case (false, true) => -1 // Letter always loses to numeric
      case (false, false) =>
        letterGradeOrder.getOrElse(a, -1).compare(letterGradeOrder.getOrElse(b, -1))
    }
  }
}

case class ValintalaskentaHakutoive(hakuOid: String,
                                    hakukohdeOid: String,
                                    prioriteetti: Int,
                                    hakukohderyhmaOids: Set[String],
                                    harkinnanvaraisuus: Boolean = false //Onkohan tämä tarpeellinen? Tarkistetaan, kun muuten laitetaan harkinnanvaraisuusasiat kuntoon.
                                   )
case class ConvertedAtaruHakemus(hakemusOid: String, hakutoiveet: List[ValintalaskentaHakutoive], avainArvot: Set[AvainArvoContainer])

object HakemusConverter {

  def convertHakutoiveet(hakemus: AtaruValintalaskentaHakemus): (List[ValintalaskentaHakutoive], Set[AvainArvoContainer]) = {
    val hakutoiveResults: List[(ValintalaskentaHakutoive, Set[AvainArvoContainer])] = hakemus.hakutoiveet.zipWithIndex.map((hakutoive, index) => {
      val prioriteetti = index + 1
      val valintalaskentaHakutoive = ValintalaskentaHakutoive(
        hakemus.hakuOid,
        hakutoive.hakukohdeOid,
        prioriteetti,
        Set.empty //Todo, add hakukohderyhmaoids
      )

      val aa = Set(
        AvainArvoContainer("preference" + prioriteetti + "-Koulutus-id", hakutoive.hakukohdeOid, Seq.empty),
        AvainArvoContainer("preference" + prioriteetti + "-Koulutus-id-eligibility",
          AvainArvoConstants.elibilityAtaruTilaToValintalaskentaTila
            .getOrElse(
              hakutoive.eligibilityState,
              throw new RuntimeException(s"Unknown state: ${hakutoive.eligibilityState}")), Seq.empty),
        AvainArvoContainer("preference" + prioriteetti + "-Koulutus-id-processingState", hakutoive.processingState.toUpperCase, Seq.empty),
        AvainArvoContainer("preference" + prioriteetti + "-Koulutus-id-paymentObligation", hakutoive.paymentObligation.toUpperCase, Seq.empty),
        AvainArvoContainer("preference" + prioriteetti + "-Koulutus-id-languageRequirement", hakutoive.languageRequirement.toUpperCase, Seq.empty),
        AvainArvoContainer("preference" + prioriteetti + "-Koulutus-id-degreeRequirement", hakutoive.degreeRequirement.toUpperCase, Seq.empty)
      )

      (valintalaskentaHakutoive, aa)
    })
    val hakutoiveet = hakutoiveResults.map(_._1)
    val aas = hakutoiveResults.map(_._2).foldLeft(Set.empty[AvainArvoContainer])(_ ++ _)

    (hakutoiveet, aas)
  }

  def convertHakemus(hakemus: AtaruValintalaskentaHakemus): ConvertedAtaruHakemus = {
    val hakutoiveData: (List[ValintalaskentaHakutoive], Set[AvainArvoContainer]) = convertHakutoiveet(hakemus)

    //Todo, arvoille "language" ja "pohjakoulutus_vuosi" erilliskäsittelyä Koostepalvelussa. Päästäänkö nyt eroon?
    val avainArvotHakemukselta: Set[AvainArvoContainer] = hakemus.keyValues.map((k, v) => {
      AvainArvoContainer(k, v, Seq.empty)
    }).toSet
    val avainArvotHakukohteilta: Set[AvainArvoContainer] = hakutoiveData._2

    ConvertedAtaruHakemus(hakemus.hakemusOid, hakutoiveData._1, avainArvotHakemukselta ++ avainArvotHakukohteilta)
  }
}

//Tätä moduulia varten tehdään kahdenlaista filtteröintiä. Vahvistuspäivän mukaan tapahtuva suoritusten filtteröinti selitteineen tapahtuu tämän moduulin sisällä.
//Laskennan alkamishetken mukaan tapahtuva oppijan tiettynä ajanhetkellä voimassaollut versio haetaan tämän moduulin ulkopuolella.
object AvainArvoConverter {

  val LOG = LoggerFactory.getLogger(getClass)

  def convertOpiskeluoikeudet(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate, haku: KoutaHaku): AvainArvoConverterResults = {
    convertOpiskeluoikeudet(personOid, None, opiskeluoikeudet, vahvistettuViimeistaan, haku)
  }

  def convertOpiskeluoikeudet(personOid: String, hakemus: Option[AtaruValintalaskentaHakemus], opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate, haku: KoutaHaku): AvainArvoConverterResults = {

    //Todo, valintapisteet avain-arvoiksi
    val convertedHakemus: Option[ConvertedAtaruHakemus] = hakemus.map(h => HakemusConverter.convertHakemus(h))

    val peruskouluArvot = convertPeruskouluArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val ammatillisetArvot = convertAmmatillisetArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val yoArvot = convertYoArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val lukioArvot = convertLukioArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan) //TODO, lukiosuoritukset pitää vielä parseroida
    val lisapistekoulutusArvot = convertLisapistekoulutukset(personOid, opiskeluoikeudet, haku)

    val paatellytArvot: Set[AvainArvoContainer] = peruskouluArvot ++ ammatillisetArvot ++ yoArvot ++ lukioArvot ++ lisapistekoulutusArvot

    AvainArvoConverterResults(personOid, paatellytArvot, convertedHakemus, opiskeluoikeudet)
  }

  def convertTelma(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vuosiVahintaan: Int): Set[AvainArvoContainer] = {
    val telmat = opiskeluoikeudet.collect {
      case o: AmmatillinenOpiskeluoikeus => o.suoritukset.collect { case s: Telma => s }
    }.flatten
    val riittavanTuoreetJaLaajat: Seq[Telma] =
      telmat
        .filter(t => t.suoritusVuosi >= vuosiVahintaan)
        .filter(t => t.hyvaksyttyLaajuus.exists(laajuus => laajuus.arvo >= AvainArvoConstants.telmaMinimiLaajuus))
    val tuoreinRiittava: Option[Telma] = riittavanTuoreetJaLaajat.maxByOption(_.suoritusVuosi)

    val suoritusSelite = (tuoreinRiittava, telmat) match {
      case (tuorein, _) if tuorein.isDefined =>
        Seq(s"Löytyneen Telma-suorituksen laajuus on ${tuoreinRiittava.flatMap(_.hyvaksyttyLaajuus.map(_.arvo))}.")
      case (_, telmat) if telmat.nonEmpty =>
        val korkeinLaajuus: Laajuus = telmat.flatMap(_.hyvaksyttyLaajuus).maxBy(_.arvo)
        Seq(s"Ei löytynyt tarpeeksi laajaa Telma-suoritusta. Korkein löytynyt laajuus: " +
          s"${korkeinLaajuus.arvo} ${korkeinLaajuus.nimi.flatMap(_.fi).getOrElse("")}.")
      case (_, telmat) =>
        Seq(s"Ei löytynyt lainkaan Telma-suoritusta.")
    }

    val suoritusArvo = AvainArvoContainer(AvainArvoConstants.telmaSuoritettuKey, tuoreinRiittava.isDefined.toString, suoritusSelite)

    val suoritusVuosiArvo = if (tuoreinRiittava.isDefined) {
      Some(AvainArvoContainer(AvainArvoConstants.telmaSuoritusvuosiKey, tuoreinRiittava.get.suoritusVuosi.toString))
    } else None

    suoritusVuosiArvo.map(Set(suoritusArvo, _)).getOrElse(Set(suoritusArvo))
  }

  def convertOpistovuosi(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vuosiVahintaan: Int): Set[AvainArvoContainer] = {
    val vstOpistovuodet = opiskeluoikeudet.collect {
      case o: GeneerinenOpiskeluoikeus => o.suoritukset.collect { case s: VapaaSivistystyo => s }
    }.flatten

    val riittavanTuoreetJaLaajat =
      vstOpistovuodet
        .filter(o => o.suoritusVuosi >= vuosiVahintaan)
        .filter(t => t.hyvaksyttyLaajuus.exists(laajuus => laajuus.arvo >= AvainArvoConstants.opistovuosiMinimiLaajuus))
    val tuoreinRiittava: Option[VapaaSivistystyo] = riittavanTuoreetJaLaajat.maxByOption(_.suoritusVuosi)

    val suoritusSelite = (tuoreinRiittava, vstOpistovuodet) match {
      case (tuorein, _) if tuorein.isDefined =>
        Seq(s"Löytyneen Opistovuosi-suorituksen laajuus on ${tuoreinRiittava.flatMap(_.hyvaksyttyLaajuus.map(_.arvo))}.")
      case (_, vstOpistovuodet) if vstOpistovuodet.nonEmpty =>
        val korkeinLaajuus: Laajuus = vstOpistovuodet.flatMap(_.hyvaksyttyLaajuus).maxBy(_.arvo)
        Seq(s"Ei löytynyt tarpeeksi laajaa Opistovuosi-suoritusta. Korkein löytynyt laajuus: " +
          s"${korkeinLaajuus.arvo} ${korkeinLaajuus.nimi.flatMap(_.fi).getOrElse("")}.")
      case (_, vstOpistovuodet) =>
        Seq(s"Ei löytynyt lainkaan Opistovuosi-suoritusta.")
    }

    val suoritusArvo = AvainArvoContainer(AvainArvoConstants.opistovuosiSuoritettuKey, tuoreinRiittava.isDefined.toString, suoritusSelite)

    val suoritusVuosiArvo = if (tuoreinRiittava.isDefined) {
      Some(AvainArvoContainer(AvainArvoConstants.opistovuosiSuoritusvuosiKey, tuoreinRiittava.get.suoritusVuosi.toString))
    } else None

    Set(suoritusArvo) ++ suoritusVuosiArvo
  }

  def convertLisapistekoulutukset(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], haku: KoutaHaku): Set[AvainArvoContainer] = {
    if (haku.isToisenAsteenYhteisHaku()) {
      val vuosiVahintaan = haku.hakuvuosi.map(vuosi => vuosi - 1).getOrElse(LocalDate.now().getYear)

      //todo tuva
      //todo kansanopisto?
      val telmaArvot = convertTelma(personOid, opiskeluoikeudet, vuosiVahintaan)
      val opistovuosiArvot = convertOpistovuosi(personOid, opiskeluoikeudet, vuosiVahintaan)

      telmaArvot ++ opistovuosiArvot
    } else {
      Set.empty
    }
  }

  def convertAmmatillisetArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    val ammatillisetOpiskeluoikeudet = opiskeluoikeudet.collect { case o: AmmatillinenOpiskeluoikeus => o }

    val allAmmSuoritukset: Seq[(Suoritus, Option[LocalDate])] = ammatillisetOpiskeluoikeudet.flatMap(ammOikeus => {
      ammOikeus.suoritukset.collect {
        case s: AmmatillinenPerustutkinto => (s, s.vahvistusPaivamaara)
        case s: AmmattiTutkinto => (s, s.vahvistusPaivamaara)
        case s: ErikoisAmmattiTutkinto => (s, s.vahvistusPaivamaara)
      }
    })

    val validSuoritukset = allAmmSuoritukset.filter(
      s => s._2.exists(v => v.isBefore(vahvistettuViimeistaan) || v.equals(vahvistettuViimeistaan))
    )

    val ammSelite = s"Löytyi yhteensä ${allAmmSuoritukset.size} ammatillista suoritusta. " +
      s"Näistä ${validSuoritukset.size} oli vahvistettu viimeistään ${vahvistettuViimeistaan}. Vahvistuspäivät: ${allAmmSuoritukset.flatMap(_._2).distinct.mkString(", ")}"

    val arvot = Set(AvainArvoContainer(AvainArvoConstants.ammSuoritettuKey, validSuoritukset.nonEmpty.toString, Seq(ammSelite)))

    LOG.info(s"Ammatilliset arvot käsitelty henkilölle $personOid. $arvot")
    arvot
  }

  def convertYoArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    val yoOpiskeluoikeudet: Seq[(YOOpiskeluoikeus, Option[LocalDate])] = opiskeluoikeudet.collect { case o: YOOpiskeluoikeus => (o, o.yoTutkinto.valmistumisPaiva) }

    val hasYoSuoritus = yoOpiskeluoikeudet.exists(_._2.exists(v => v.isBefore(vahvistettuViimeistaan) || v.equals(vahvistettuViimeistaan)))

    val paivat = yoOpiskeluoikeudet.flatMap(_._2).distinct
    val valmistumispaivaSelite = if (paivat.nonEmpty) s" Valmistumispaivat: ${paivat.mkString(", ")}" else ""
    val yoSelite = s"Löytyi yhteensä ${yoOpiskeluoikeudet.size} YO-opiskeluoikeutta." + valmistumispaivaSelite

    val arvot = Set(AvainArvoContainer(AvainArvoConstants.yoSuoritettuKey, hasYoSuoritus.toString, Seq(yoSelite)))

    LOG.info(s"Yo-arvot käsitelty henkilölle $personOid. $arvot")
    arvot
  }

  //TODO lukiosuorituksia ei ole vielä parseroitu eikä niitä saada Koskesta massaluovutusrajapinnan kautta. Tämä päättely ei siis vielä toimi.
  def convertLukioArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    //val lukioOpiskeluoikeudet = opiskeluoikeudet.collect { case o: GeneerinenOpiskeluoikeus => o }
    val hasLukioSuoritus = false
    val lukioSelite = s"Lukiosuorituksia ei vielä saada Koskesta massaluovutusrajapinnan kautta."

    val arvot = Set(AvainArvoContainer(AvainArvoConstants.lukioSuoritettuKey, hasLukioSuoritus.toString, Seq(lukioSelite)))

    LOG.info(s"Lukioarvot käsitelty henkilölle $personOid. $arvot")
    arvot
  }

  //Mahdolliset oppiaineen oppimäärät palautetaan vain, jos perusopetuksen oppimäärä löytyi.
  def filterForPeruskoulu(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): (Option[PerusopetuksenOppimaara], Seq[PerusopetuksenOppimaaranOppiaineidenSuoritus]) = {
    val perusopetuksenOpiskeluoikeudet = opiskeluoikeudet.collect { case po: PerusopetuksenOpiskeluoikeus => po }
    val (vahvistetut, eiVahvistetut) =
      perusopetuksenOpiskeluoikeudet
        .flatMap(po => po.suoritukset.find(_.isInstanceOf[PerusopetuksenOppimaara]).map(_.asInstanceOf[PerusopetuksenOppimaara]))
        .partition(o => o.vahvistusPaivamaara.isDefined)

    val oppiaineeOppimaarat = perusopetuksenOpiskeluoikeudet.flatMap(po => po.suoritukset.find(_.isInstanceOf[PerusopetuksenOppimaaranOppiaineidenSuoritus]).map(_.asInstanceOf[PerusopetuksenOppimaaranOppiaineidenSuoritus]))

    if (vahvistetut.size > 1) {
      LOG.error(s"Oppijalle $personOid enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
      throw new UseitaVahvistettujaOppimaariaException(s"Oppijalle $personOid enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
    }

    val valmisOppimaara: Option[PerusopetuksenOppimaara] = vahvistetut.headOption
    //Todo, tämän käsittelyyn tarvitaan jotain päivämäärätietoa suorituksille, jotta voidaan poimia tuorein. Periaatteessa ei-vahvistettuja voisi olla useita.
    // Toisaalta voi olla että riittää tieto siitä, että jotain keskeneräistä löytyy. Halutaan ehkä filtteröidä selkeästi keskeytyneet (eronnut, erotettu jne) pois.
    val keskenOppimaara: Option[PerusopetuksenOppimaara] = eiVahvistetut.headOption

    val useOppimaara = valmisOppimaara.orElse(keskenOppimaara)
    val useOppiaineenOppimaarat = if (valmisOppimaara.isDefined) oppiaineeOppimaarat else Seq.empty
    (useOppimaara, useOppiaineenOppimaarat)
  }

  //Ottaa huomioon ensi vaiheessa PerusopetuksenOppimaarat ja myöhemmin myös PerusopetuksenOppiaineenOppimaarien sisältämät arvosanat JOS löytyy suoritettu PerusopetuksenOppimaara
  def korkeimmatPerusopetuksenArvosanatAineittain(perusopetuksenOppimaara: Option[PerusopetuksenOppimaara], oppiaineenOppimaarat: Seq[PerusopetuksenOppimaaranOppiaineidenSuoritus]): Set[AvainArvoContainer] = {
    val aineet: Set[PerusopetuksenOppiaine] = perusopetuksenOppimaara.map(om => om.aineet).getOrElse(Set.empty)
    val byAineKey: Map[String, Set[PerusopetuksenOppiaine]] = aineet.groupBy(_.koodi.arvo)

    val aineidenKorkeimmatArvosanat = byAineKey.map((key, arvosanat) => {
      val highestGrade = arvosanat.maxBy(_.arvosana)(Ordering.fromLessThan((a, b) =>
        PerusopetuksenArvosanaOrdering.compareArvosana(a.arvo, b.arvo) < 0))
      (key, highestGrade)
    })

    val avainArvot: Set[AvainArvoContainer] = aineidenKorkeimmatArvosanat.values.flatMap(aine => {
      val arvosanaAvain = AvainArvoConstants.peruskouluAineenArvosanaPrefix + aine.koodi.arvo
      val arvosanaArvot: AvainArvoContainer = AvainArvoContainer(arvosanaAvain, aine.arvosana.arvo)

      val kieliArvot: Option[AvainArvoContainer] = aine.kieli.map(aineenKieliKoodi => {
        val kieliAvain = arvosanaAvain + AvainArvoConstants.peruskouluAineenKieliPostfix
        AvainArvoContainer(kieliAvain, aineenKieliKoodi.arvo)
      })

      Set(arvosanaArvot) ++ kieliArvot
    }).toSet

    avainArvot
  }

  def convertPeruskouluArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    val (perusopetuksenOppimaara: Option[PerusopetuksenOppimaara], oppiaineenOppimaarat: Seq[PerusopetuksenOppimaaranOppiaineidenSuoritus]) = filterForPeruskoulu(personOid, opiskeluoikeudet)

    val oppimaaraOnVahvistettu: Boolean = perusopetuksenOppimaara.exists(_.vahvistusPaivamaara.isDefined)
    val vahvistusPvm = perusopetuksenOppimaara.map(_.vahvistusPaivamaara)
    val vahvistettuAjoissa: Boolean = perusopetuksenOppimaara.flatMap(_.vahvistusPaivamaara).exists(v => v.isBefore(vahvistettuViimeistaan) || v.equals(vahvistettuViimeistaan))

    val kieliArvot: Option[AvainArvoContainer] = perusopetuksenOppimaara
      .map(_.suoritusKieli.arvo)
      .map(kieli => AvainArvoContainer(AvainArvoConstants.perusopetuksenKieliKey, kieli))

    val arvot = if (oppimaaraOnVahvistettu) {
      if (vahvistettuAjoissa) {
        val vahvistettuAjoissaSelite = s"Löytyi perusopetuksen oppimäärä, joka on vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä. Vahvistuspäivä: ${vahvistusPvm.flatten.getOrElse("-")}"
        val arvosanaArvot: Set[AvainArvoContainer] = korkeimmatPerusopetuksenArvosanatAineittain(perusopetuksenOppimaara, Seq.empty)

        val suoritusVuosiArvo: Option[AvainArvoContainer] = perusopetuksenOppimaara
          .flatMap(vo => vo.vahvistusPaivamaara.map(_.getYear))
          .map(year => AvainArvoContainer(AvainArvoConstants.peruskouluSuoritusvuosiKey, year.toString, Seq(vahvistettuAjoissaSelite)))

        val suoritusArvo = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritettuKey, vahvistettuAjoissa.toString, Seq(vahvistettuAjoissaSelite))

        arvosanaArvot ++ kieliArvot ++ suoritusVuosiArvo ++ Some(suoritusArvo)
      } else {
        val vahvistettuMyohassaSelite = s"Löytyi perusopetuksen oppimäärä, mutta sitä ei ole vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä. Vahvistuspäivä: ${perusopetuksenOppimaara.flatMap(_.vahvistusPaivamaara).getOrElse("-")}"
        val suoritusArvo = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritettuKey, vahvistettuAjoissa.toString, Seq(vahvistettuMyohassaSelite))
        Set(suoritusArvo) ++ kieliArvot
      }
    } else {
      kieliArvot.toSet
    }
    arvot
  }
}
