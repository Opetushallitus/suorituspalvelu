package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.PerusopetuksenYksilollistaminen.toIntValue
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Laajuus, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, PerusopetuksenYksilollistaminen, Suoritus, SuoritusTila, Telma, Tuva, VapaaSivistystyo, YOOpiskeluoikeus}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, KoutaHaku, Ohjausparametrit}
import fi.oph.suorituspalvelu.mankeli.ataru.{AtaruArvosanaParser, AvainArvoDTO}
import org.slf4j.LoggerFactory

import java.util.List as JavaList
import scala.jdk.CollectionConverters.*
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
    peruskouluOppiaineenKieli_AI -> "Peruskoulun äidinkielen oppiaine",
    pohjakoulutusToinenAste -> "Toisen asteen pohjakoulutus",
    yksMatAiKey -> "Onko joku hakemuksen hakutoiveista harkinnanvarainen siksi, että matematiikka ja äidinkieli yksilöllistettyjä"
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
  final val tuvaSuoritettuKey = "LISAKOULUTUS_TUVA"
  final val tuvaSuoritusvuosiKey = "LISAPISTEKOULUTUS_TUVA_SUORITUSVUOSI"
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

  final val pohjakoulutusToinenAste = "POHJAKOULUTUS"

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

  val ataruPohjakoulutusKey = "base-education-2nd"
  val ataruPohjakoulutusVuosiKey = "pohjakoulutus_vuosi"
  val ataruPohjakoulutusKieliKey = "pohjakoulutus_kieli"

  val POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS = "0"
  val POHJAKOULUTUS_PERUSKOULU = "1"
  val POHJAKOULUTUS_PERUSKOULU_OSITTAIN_YKSILOLLISTETTY = "2"
  val POHJAKOULUTUS_PERUSKOULU_TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY = "3"
  val POHJAKOULUTUS_PERUSKOULU_PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY = "6"
  val POHJAKOULUTUS_PERUSKOULU_OSITTAIN_RAJOITETTU = "8"
  val POHJAKOULUTUS_PERUSKOULU_PAAOSIN_TAI_KOKONAAN_RAJOITETTU = "9"
  val POHJAKOULUTUS_EI_PAATTOTODISTUSTA = "7"

  val hakemuksenPohjakoulutuksetUskotaanHakemusta = Set(POHJAKOULUTUS_PERUSKOULU, POHJAKOULUTUS_PERUSKOULU_OSITTAIN_YKSILOLLISTETTY,
    POHJAKOULUTUS_PERUSKOULU_PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY, POHJAKOULUTUS_PERUSKOULU_TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY,
    POHJAKOULUTUS_PERUSKOULU_OSITTAIN_RAJOITETTU, POHJAKOULUTUS_PERUSKOULU_PAAOSIN_TAI_KOKONAAN_RAJOITETTU)

  val arvosananLahdeSeliteSupa = "Tieto löytyi Suorituspalvelusta."
  val arvosananLahdeSeliteHakemus = "Tieto löytyi hakemukselta."

  val yksMatAiKey = "yks_mat_ai"
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

  //Käytetään muunnokseen Valintalaskentakoostepalvelusta kopioitua valmista java-luokkaa.
  //Arvosanat parsitaan vain, jos pohjakoulutusvuosi 2017 tai aiempi.
  def convertArvosanatHakemukselta(hakemus: AtaruValintalaskentaHakemus): Set[AvainArvoContainer] = {
    val hakemusPohjakoulutusVuosi = hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusVuosiKey).flatMap(v => Option.apply(v)).map(_.toInt)
    if (hakemusPohjakoulutusVuosi.exists(_ <= 2017)) {
      // Muokataan hakemukselta tulevat arvot AtaruArvosanaParserin ymmärtämään muotoon
      val avainArvoDTOMap = hakemus.keyValues.map { case (key, value) =>
        key -> new fi.oph.suorituspalvelu.mankeli.ataru.AvainArvoDTO(key, value)
      }

      val convertedArvosanat: JavaList[AvainArvoDTO] =
        AtaruArvosanaParser.convertAtaruArvosanas(avainArvoDTOMap.asJava, hakemus.hakemusOid)

      // Muokataan tulokset takaisin Supa-muotoon
      convertedArvosanat.asScala.map(dto =>
        AvainArvoContainer(dto.getAvain, dto.getArvo, Seq(AvainArvoConstants.arvosananLahdeSeliteHakemus))
      ).toSet
    } else Set.empty
  }


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

  def convertOpiskeluoikeudet(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate, haku: KoutaHaku, harkinnanvaraisuudet: Option[HakemuksenHarkinnanvaraisuus]): AvainArvoConverterResults = {
    convertOpiskeluoikeudet(personOid, None, opiskeluoikeudet, vahvistettuViimeistaan, haku, harkinnanvaraisuudet)
  }

  def convertOpiskeluoikeudet(personOid: String, hakemus: Option[AtaruValintalaskentaHakemus], opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate, haku: KoutaHaku, harkinnanvaraisuudet: Option[HakemuksenHarkinnanvaraisuus]): AvainArvoConverterResults = {

    //Todo, valintapisteet avain-arvoiksi
    val convertedHakemus: Option[ConvertedAtaruHakemus] = hakemus.map(h => HakemusConverter.convertHakemus(h))

    val harkinnanvaraisuusArvot: Option[AvainArvoContainer] = harkinnanvaraisuudet.map(getHarkinnanvaraisuusArvot)

    val toisenAsteenPk: Option[AvainArvoContainer] = if (haku.isToisenAsteenHaku())
      hakemus.map(h => toisenAsteenPohjakoulutus(h, opiskeluoikeudet, vahvistettuViimeistaan)) else None
    val peruskouluArvot = convertPeruskouluArvot(personOid, hakemus, opiskeluoikeudet, vahvistettuViimeistaan)
    val ammatillisetArvot = convertAmmatillisetArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val yoArvot = convertYoArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val lukioArvot = convertLukioArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan) //TODO, lukiosuoritukset pitää vielä parseroida
    val lisapistekoulutusArvot = convertLisapistekoulutukset(personOid, opiskeluoikeudet, haku, toisenAsteenPk)

    val paatellytArvot: Set[AvainArvoContainer] =
      peruskouluArvot
        ++ ammatillisetArvot
        ++ yoArvot
        ++ lukioArvot
        ++ lisapistekoulutusArvot
        ++ toisenAsteenPk.toSet
        ++ harkinnanvaraisuusArvot.toSet

    AvainArvoConverterResults(personOid, paatellytArvot, convertedHakemus, opiskeluoikeudet)
  }

  def getHarkinnanvaraisuusArvot(harkinnanvaraisuus: HakemuksenHarkinnanvaraisuus): AvainArvoContainer = {
    val syyt = harkinnanvaraisuus.hakutoiveet.map(_.harkinnanvaraisuudenSyy)
    val isYksMatAI = syyt.exists(syy => Set(HarkinnanvaraisuudenSyy.SURE_YKS_MAT_AI, HarkinnanvaraisuudenSyy.ATARU_YKS_MAT_AI).contains(syy))
    AvainArvoContainer(AvainArvoConstants.yksMatAiKey, isYksMatAI.toString)
  }

  def toisenAsteenPohjakoulutus(hakemus: AtaruValintalaskentaHakemus, opiskeluoikeudet: Seq[Opiskeluoikeus], deadline: LocalDate): AvainArvoContainer = {
    val perusopetuksenOppimaarat = opiskeluoikeudet
      .collect { case oo: PerusopetuksenOpiskeluoikeus => oo }
      .flatMap(_.suoritukset)
      .collect { case po: PerusopetuksenOppimaara => po }

    val vahvistetutOppimaarat = perusopetuksenOppimaarat.filter(_.vahvistusPaivamaara.isDefined)
    if (vahvistetutOppimaarat.size > 1) {
      throw new RuntimeException(s"Hakemuksen ${hakemus.hakemusOid} oppijalta ${hakemus.personOid} löytyi ${vahvistetutOppimaarat.size} vahvistettua perusopetuksen suoritusta.")
    }
    val keskenOppimaarat = perusopetuksenOppimaarat.filter(_.supaTila.equals(SuoritusTila.KESKEN))
    val keskeytyneetOppimaarat = perusopetuksenOppimaarat.filter(_.supaTila.equals(SuoritusTila.KESKEYTYNYT))

    val viimeisinOppimaara =
      vahvistetutOppimaarat.headOption
        .orElse(keskenOppimaarat.maxByOption(_.aloitusPaivamaara))
        .orElse(keskeytyneetOppimaarat.maxByOption(_.aloitusPaivamaara))
    val kelpaavaOppimaara = viimeisinOppimaara.filter(onKelpaavaOppimaara(_, deadline))

    val hakemusPohjakoulutus = hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusKey).flatMap(v => Option.apply(v))
    val hakemusPohjakoulutusVuosi = hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusVuosiKey).flatMap(v => Option.apply(v)).map(_.toInt)

    val (pkResult, pkSelite) = getPohjakoulutusResult(kelpaavaOppimaara, hakemusPohjakoulutus, hakemusPohjakoulutusVuosi)

    AvainArvoContainer(AvainArvoConstants.pohjakoulutusToinenAste, pkResult, pkSelite)
  }

  private def onKelpaavaOppimaara(oppimaara: PerusopetuksenOppimaara, deadline: LocalDate): Boolean = {
    val deadlineOhitettu = LocalDate.now().isAfter(deadline)
    val arvosanoissaNelosia = oppimaara.aineet.exists(a => a.pakollinen && a.arvosana.arvo.equals("4"))
    val suoritusValmis = oppimaara.vahvistusPaivamaara.isDefined
    val suoritusKesken = oppimaara.supaTila.equals(SuoritusTila.KESKEN)
    val vahvistettuAjoissa = oppimaara.vahvistusPaivamaara.exists(vp => vp.isBefore(deadline) || vp.equals(deadline))

    if (deadlineOhitettu) {
      (suoritusValmis && vahvistettuAjoissa) || (suoritusKesken && arvosanoissaNelosia && !oppimaara.vuosiluokkiinSitoutumatonOpetus)
    } else {
      suoritusValmis || suoritusKesken
    }
  }

  private def getPohjakoulutusResult(kelpaavaOppimaara: Option[PerusopetuksenOppimaara],
                                     hakemusPohjakoulutus: Option[String],
                                     hakemusPohjakoulutusVuosi: Option[Int]): (String, Seq[String]) = {
    (kelpaavaOppimaara, hakemusPohjakoulutus, hakemusPohjakoulutusVuosi) match {
      case (Some(oppimaara), _, _) =>
        val yksilollistaminenIntValue = oppimaara.yksilollistaminen.map(toIntValue).getOrElse(1).toString
        (yksilollistaminenIntValue, Seq(s"Supasta löytyi suoritettu perusopetuksen oppimäärä. Vahvistuspäivä ${oppimaara.vahvistusPaivamaara.map(_.toString).getOrElse("")}."))

      case (_, Some(pohjakolutus), _) if pohjakolutus == AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS =>
        (AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS, Seq("Hakemuksella on ilmoitettu ulkomainen tutkinto."))

      case (_, Some(pohjakoulutus), Some(vuosi)) if vuosi <= 2017 && AvainArvoConstants.hakemuksenPohjakoulutuksetUskotaanHakemusta.contains(pohjakoulutus) =>
        (pohjakoulutus, Seq(s"Hakemuksen pohjakoulutusvuosi on 2017 tai aiemmin, joten käytettiin hakemuksella ilmoitettua pohjakoulutusta $pohjakoulutus."))

      case _ =>
        (AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA, Seq("Supasta tai hakemukselta ei löytynyt sopivaa pohjakoulutusta."))
    }
  }

  def convertTuva(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vuosiVahintaan: Int, toisenAsteenPohjakoulutus: Option[AvainArvoContainer]): Set[AvainArvoContainer] = {
    val tuvaSuoritukset = opiskeluoikeudet.collect {
      case o: GeneerinenOpiskeluoikeus => o.suoritukset.collect { case s: Tuva => s }
    }.flatten
    val riittavanTuoreetJaLaajat: Seq[Tuva] =
      tuvaSuoritukset
        .filter(t => t.suoritusVuosi >= vuosiVahintaan)
        .filter(t => t.hyvaksyttyLaajuus.exists(laajuus => laajuus.arvo >= AvainArvoConstants.tuvaMinimiLaajuus))
    val tuoreinRiittava: Option[Tuva] = riittavanTuoreetJaLaajat.maxByOption(_.suoritusVuosi)
    val pohjakoulutusSallii = toisenAsteenPohjakoulutus.map(_.arvo).exists(arvo => !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS) && !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA))

    val suoritusSelite = (tuoreinRiittava, tuvaSuoritukset) match {
      case (tuorein, _) if tuorein.isDefined && pohjakoulutusSallii =>
        Seq(s"Löytyneen Tuva-suorituksen laajuus on ${tuoreinRiittava.flatMap(_.hyvaksyttyLaajuus.map(_.arvo))}.")
      case (tuorein, _) if tuorein.isDefined && !pohjakoulutusSallii =>
        Seq(s"Löytyneen Tuva-suorituksen laajuus on ${tuoreinRiittava.flatMap(_.hyvaksyttyLaajuus.map(_.arvo))}, mutta hakijan pohjakoulutus (${toisenAsteenPohjakoulutus.map(_.arvo)}) ei salli lisäpistekoulutusten huomioimista.")
      case (_, tuvat) if tuvat.nonEmpty =>
        val korkeinLaajuus: Option[Laajuus] = tuvat.flatMap(_.hyvaksyttyLaajuus).maxByOption(_.arvo)
        Seq(s"Ei löytynyt tarpeeksi laajaa Tuva-suoritusta. Korkein löytynyt laajuus: " +
          s"${korkeinLaajuus.map(_.arvo).getOrElse(0)} ${korkeinLaajuus.flatMap(_.nimi).flatMap(_.fi).getOrElse("")}.")
      case (_, tuvat) =>
        Seq(s"Ei löytynyt lainkaan Tuva-suoritusta.")
    }

    val tuvaHuomioidaan: Boolean = pohjakoulutusSallii && tuoreinRiittava.isDefined
    val suoritusArvo = AvainArvoContainer(AvainArvoConstants.tuvaSuoritettuKey, tuvaHuomioidaan.toString, suoritusSelite)

    val suoritusVuosiArvo = if (tuvaHuomioidaan) {
      Some(AvainArvoContainer(AvainArvoConstants.tuvaSuoritusvuosiKey, tuoreinRiittava.get.suoritusVuosi.toString))
    } else None

    suoritusVuosiArvo.map(Set(suoritusArvo, _)).getOrElse(Set(suoritusArvo))
  }

  def convertTelma(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vuosiVahintaan: Int, toisenAsteenPohjakoulutus: Option[AvainArvoContainer]): Set[AvainArvoContainer] = {
    val telmat = opiskeluoikeudet.collect {
      case o: AmmatillinenOpiskeluoikeus => o.suoritukset.collect { case s: Telma => s }
    }.flatten
    val riittavanTuoreetJaLaajat: Seq[Telma] =
      telmat
        .filter(t => t.suoritusVuosi >= vuosiVahintaan)
        .filter(t => t.hyvaksyttyLaajuus.exists(laajuus => laajuus.arvo >= AvainArvoConstants.telmaMinimiLaajuus))
    val tuoreinRiittava: Option[Telma] = riittavanTuoreetJaLaajat.maxByOption(_.suoritusVuosi)
    val pohjakoulutusSallii = toisenAsteenPohjakoulutus.map(_.arvo).exists(arvo => !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS) && !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA))

    val suoritusSelite = (tuoreinRiittava, telmat) match {
      case (tuorein, _) if tuorein.isDefined && pohjakoulutusSallii =>
        Seq(s"Löytyneen Telma-suorituksen laajuus on ${tuoreinRiittava.flatMap(_.hyvaksyttyLaajuus.map(_.arvo))}.")
      case (tuorein, _) if tuorein.isDefined && !pohjakoulutusSallii =>
        Seq(s"Löytyneen Telma-suorituksen laajuus on ${tuoreinRiittava.flatMap(_.hyvaksyttyLaajuus.map(_.arvo))}, mutta hakijan pohjakoulutus (${toisenAsteenPohjakoulutus.map(_.arvo)}) ei salli lisäpistekoulutusten huomioimista.")
      case (_, telmat) if telmat.nonEmpty =>
        val korkeinLaajuus: Option[Laajuus] = telmat.flatMap(_.hyvaksyttyLaajuus).maxByOption(_.arvo)
        Seq(s"Ei löytynyt tarpeeksi laajaa Telma-suoritusta. Korkein löytynyt laajuus: " +
          s"${korkeinLaajuus.map(_.arvo).getOrElse(0)} ${korkeinLaajuus.flatMap(_.nimi).flatMap(_.fi).getOrElse("")}.")
      case (_, telmat) =>
        Seq(s"Ei löytynyt lainkaan Telma-suoritusta.")
    }

    val telmaHuomioidaan: Boolean = pohjakoulutusSallii && tuoreinRiittava.isDefined
    val suoritusArvo = AvainArvoContainer(AvainArvoConstants.telmaSuoritettuKey, telmaHuomioidaan.toString, suoritusSelite)

    val suoritusVuosiArvo = if (telmaHuomioidaan) {
      Some(AvainArvoContainer(AvainArvoConstants.telmaSuoritusvuosiKey, tuoreinRiittava.get.suoritusVuosi.toString))
    } else None

    suoritusVuosiArvo.map(Set(suoritusArvo, _)).getOrElse(Set(suoritusArvo))
  }

  def convertOpistovuosi(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vuosiVahintaan: Int, toisenAsteenPohjakoulutus: Option[AvainArvoContainer]): Set[AvainArvoContainer] = {
    val vstOpistovuodet = opiskeluoikeudet.collect {
      case o: GeneerinenOpiskeluoikeus => o.suoritukset.collect { case s: VapaaSivistystyo => s }
    }.flatten

    val riittavanTuoreetJaLaajat =
      vstOpistovuodet
        .filter(o => o.suoritusVuosi >= vuosiVahintaan)
        .filter(t => t.hyvaksyttyLaajuus.exists(laajuus => laajuus.arvo >= AvainArvoConstants.opistovuosiMinimiLaajuus))
    val tuoreinRiittava: Option[VapaaSivistystyo] = riittavanTuoreetJaLaajat.maxByOption(_.suoritusVuosi)
    val pohjakoulutusSallii = toisenAsteenPohjakoulutus.map(_.arvo).exists(arvo => !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS) && !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA))

    val suoritusSelite = (tuoreinRiittava, vstOpistovuodet) match {
      case (tuorein, _) if tuorein.isDefined && pohjakoulutusSallii =>
        Seq(s"Löytyneen Opistovuosi-suorituksen laajuus on ${tuoreinRiittava.flatMap(_.hyvaksyttyLaajuus.map(_.arvo))}.")
      case (tuorein, _) if tuorein.isDefined && !pohjakoulutusSallii =>
        Seq(s"Löytyneen Opistovuosi-suorituksen laajuus on ${tuoreinRiittava.flatMap(_.hyvaksyttyLaajuus.map(_.arvo))}, mutta hakijan pohjakoulutus (${toisenAsteenPohjakoulutus.map(_.arvo)}) ei salli lisäpistekoulutusten huomioimista.")
      case (_, vstOpistovuodet) if vstOpistovuodet.exists(_.hyvaksyttyLaajuus.nonEmpty) =>
        val korkeinLaajuus: Option[Laajuus] = vstOpistovuodet.flatMap(_.hyvaksyttyLaajuus).maxByOption(_.arvo)
        Seq(s"Ei löytynyt tarpeeksi laajaa Opistovuosi-suoritusta. Korkein löytynyt laajuus: " +
          s"${korkeinLaajuus.map(_.arvo)} ${korkeinLaajuus.flatMap(_.nimi).flatMap(_.fi).getOrElse("")}.")
      case (_, vstOpistovuodet) =>
        Seq(s"Ei löytynyt lainkaan Opistovuosi-suoritusta.")
    }

    val opistovuosiHuomioidaan: Boolean = pohjakoulutusSallii && tuoreinRiittava.isDefined
    val suoritusArvo = AvainArvoContainer(AvainArvoConstants.opistovuosiSuoritettuKey, opistovuosiHuomioidaan.toString, suoritusSelite)

    val suoritusVuosiArvo = if (opistovuosiHuomioidaan) {
      Some(AvainArvoContainer(AvainArvoConstants.opistovuosiSuoritusvuosiKey, tuoreinRiittava.get.suoritusVuosi.toString))
    } else None

    Set(suoritusArvo) ++ suoritusVuosiArvo
  }

  def convertLisapistekoulutukset(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], haku: KoutaHaku, toisenAsteenPohjakoulutus: Option[AvainArvoContainer]): Set[AvainArvoContainer] = {
    if (haku.isToisenAsteenYhteisHaku()) {
      val vuosiVahintaan = haku.hakuvuosi.map(vuosi => vuosi - 1).getOrElse(LocalDate.now().getYear)

      //todo kansanopisto?
      val tuvaArvot = convertTuva(personOid, opiskeluoikeudet, vuosiVahintaan, toisenAsteenPohjakoulutus)
      val telmaArvot = convertTelma(personOid, opiskeluoikeudet, vuosiVahintaan, toisenAsteenPohjakoulutus)
      val opistovuosiArvot = convertOpistovuosi(personOid, opiskeluoikeudet, vuosiVahintaan, toisenAsteenPohjakoulutus)

      tuvaArvot ++ telmaArvot ++ opistovuosiArvot
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

  def etsiVahvistetutOppiaineenOppimaarat(opiskeluoikeudet: Seq[Opiskeluoikeus]): Seq[PerusopetuksenOppimaaranOppiaineidenSuoritus] = {
    opiskeluoikeudet.collect { case po: PerusopetuksenOpiskeluoikeus => po }
      .flatMap(_.suoritukset.collect { case s: PerusopetuksenOppimaaranOppiaineidenSuoritus => s })
      .filter(_.vahvistusPaivamaara.isDefined)
  }

  //Valitaan vahvistettu peruskoulu jos löytyy, tuorein kesken-tilainen jos ei ole vahvistettua ja tuorein keskeytynyt jos ei ole kesken-tilaisia.
  def etsiViimeisinPeruskoulu(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], salliMontaValmista: Boolean): Option[PerusopetuksenOppimaara] = {
    val perusopetuksenOpiskeluoikeudet = opiskeluoikeudet.collect { case po: PerusopetuksenOpiskeluoikeus => po }
    val (vahvistetut, eiVahvistetut) =
      perusopetuksenOpiskeluoikeudet
        .flatMap(po => po.suoritukset.find(_.isInstanceOf[PerusopetuksenOppimaara]).map(_.asInstanceOf[PerusopetuksenOppimaara]))
        .partition(o => o.vahvistusPaivamaara.isDefined)

    //Jos on tarpeen vain tarkistaa että löytyy joku valmis-tilainen eikä muulla sisällöllä ole merkitystä, ei ole pakko räjähtää.
    if (!salliMontaValmista && vahvistetut.size > 1) {
      LOG.error(s"Oppijalle $personOid löytyi enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
      throw new UseitaVahvistettujaOppimaariaException(s"Oppijalle $personOid enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
    }

    val valmisOppimaara: Option[PerusopetuksenOppimaara] = vahvistetut.maxByOption(_.aloitusPaivamaara)

    //Jos kesken- tai keskeytynyt-tilaisia on useita, käytetään sitä jonka alkamispävä on myöhäisin.
    val keskenOppimaara: Option[PerusopetuksenOppimaara] = eiVahvistetut.filter(s => !SuoritusTila.KESKEYTYNYT.equals(s.supaTila)).maxByOption(_.aloitusPaivamaara)
    val keskeytynytOppimaara: Option[PerusopetuksenOppimaara] = eiVahvistetut.filter(s => SuoritusTila.KESKEYTYNYT.equals(s.supaTila)).maxByOption(_.aloitusPaivamaara)

    val useOppimaara = valmisOppimaara.orElse(keskenOppimaara).orElse(keskeytynytOppimaara)
    useOppimaara
  }

  //AvainArvoja voi tulla hakemukselta (2017 tai aiempi pohjakoulutus), perusopetuksen oppimääriltä (Koski) sekä perusopetuksen oppiaineen oppimääriltä (Koski)
  //Pudotetaan tässä pois muut kuin parhaat kultakin aineelta. Containerien mukana kulkee selite, joka kertoo kunkin arvon lähteen.
  def valitseKorkeimmatPerusopetuksenArvosanatAineittain(avainArvot: Set[AvainArvoContainer]) = {
    val byKey: Map[String, Set[AvainArvoContainer]] = avainArvot.groupBy(_.avain)

    val korkeimmatArvosanatAineittain = byKey.map((kv: (String, Set[AvainArvoContainer])) => kv._2.maxBy(_.arvo)(Ordering.fromLessThan((a, b) =>
      PerusopetuksenArvosanaOrdering.compareArvosana(a, b) < 0))).toSet
    korkeimmatArvosanatAineittain
  }

  def perusopetuksenOppiaineetToAvainArvot(aineet: Set[PerusopetuksenOppiaine]): Set[AvainArvoContainer] = {
    aineet.flatMap(aine => {
      val arvosanaAvain = AvainArvoConstants.peruskouluAineenArvosanaPrefix + aine.koodi.arvo
      val arvosanaArvot: AvainArvoContainer = AvainArvoContainer(arvosanaAvain, aine.arvosana.arvo, Seq(AvainArvoConstants.arvosananLahdeSeliteSupa))

      val kieliArvot: Option[AvainArvoContainer] = aine.kieli.map(aineenKieliKoodi => {
        val kieliAvain = arvosanaAvain + AvainArvoConstants.peruskouluAineenKieliPostfix
        AvainArvoContainer(kieliAvain, aineenKieliKoodi.arvo, Seq("Kielitieto löytyi Koskesta."))
      })

      Set(arvosanaArvot) ++ kieliArvot
    })
  }

  def hakemuksellaIlmoitettuPeruskoulu2017TaiAiempi(hakemus: AtaruValintalaskentaHakemus): Boolean = {
    hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusVuosiKey).flatMap(v => Option.apply(v)).map(_.toInt).exists(_ <= 2017)
  }

  def convertPeruskouluArvot(personOid: String, hakemus: Option[AtaruValintalaskentaHakemus], opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    def oppimaaraVahvistettuAjoissa(o: PerusopetuksenOppimaara): Boolean = o.vahvistusPaivamaara.exists(pvm => pvm.isBefore(vahvistettuViimeistaan) || pvm.equals(vahvistettuViimeistaan))

    val perusopetuksenOppimaara: Option[PerusopetuksenOppimaara] = etsiViimeisinPeruskoulu(personOid, opiskeluoikeudet, false)
    val oppiaineenOppimaarat: Seq[PerusopetuksenOppimaaranOppiaineidenSuoritus] = etsiVahvistetutOppiaineenOppimaarat(opiskeluoikeudet)

    val arvot = (perusopetuksenOppimaara, hakemus) match {
      case (Some(po), _) if oppimaaraVahvistettuAjoissa(po) =>
        val vahvistettuAjoissaSelite = s"Löytyi perusopetuksen oppimäärä, joka on vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä. Vahvistuspäivä: ${po.vahvistusPaivamaara.getOrElse("-")}"
        val aineetPaasuoritukselta = perusopetuksenOppimaara.map(_.aineet).getOrElse(Set.empty)
        val aineetOppimaarilta = oppiaineenOppimaarat.flatMap(_.aineet).toSet
        val arvosanatSuorituspalvelusta = perusopetuksenOppiaineetToAvainArvot(aineetPaasuoritukselta ++ aineetOppimaarilta)

        val arvosanaArvot: Set[AvainArvoContainer] = valitseKorkeimmatPerusopetuksenArvosanatAineittain(arvosanatSuorituspalvelusta)
        val suoritusArvo = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritettuKey, "true", Seq(vahvistettuAjoissaSelite))
        val suoritusVuosiArvo = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritusvuosiKey, po.vahvistusPaivamaara.map(_.getYear).get.toString, Seq(vahvistettuAjoissaSelite))
        val suoritusKieliArvo = AvainArvoContainer(AvainArvoConstants.perusopetuksenKieliKey, po.suoritusKieli.arvo)

        arvosanaArvot ++ Some(suoritusVuosiArvo) ++ Some(suoritusArvo) ++ Some(suoritusKieliArvo)

      case (Some(po), _) if po.vahvistusPaivamaara.isDefined =>
        val vahvistettuMyohassaSelite = s"Löytyi perusopetuksen oppimäärä, mutta sitä ei ole vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä. Vahvistuspäivä: ${perusopetuksenOppimaara.flatMap(_.vahvistusPaivamaara).getOrElse("-")}"
        val suoritusArvo: AvainArvoContainer = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritettuKey, "false", Seq(vahvistettuMyohassaSelite))
        Set(suoritusArvo)

      //Jos Supasta ei löydy perusopetuksen suoritusta, käytetään hakemuksen tietoja jos sieltä löytyy hakijan ilmoittama perusopetus vuodelta 2017 tai aiemmin.
      case (None, Some(hakemus)) if hakemuksellaIlmoitettuPeruskoulu2017TaiAiempi(hakemus) =>
        val arvosanatHakemukselta = HakemusConverter.convertArvosanatHakemukselta(hakemus)
        //Suorituspalvelusta voi löytyä korotuksia hakemuksella ilmoitetuille arvosanoille (esim. perusopetus suoritettu 2017, korotuksia vuodelta 2018). Otetaan ne huomioon.
        val korotuksetSuorituspalvelusta = perusopetuksenOppiaineetToAvainArvot(oppiaineenOppimaarat.flatMap(_.aineet).toSet)
        val korkeimmatArvosanatHakemukseltaJaSupasta = valitseKorkeimmatPerusopetuksenArvosanatAineittain(korotuksetSuorituspalvelusta ++ arvosanatHakemukselta)
        val suoritusKieliHakemukselta =
          hakemus.keyValues.get(AvainArvoConstants.perusopetuksenKieliKey).flatMap(v => Option.apply(v))
            .map(k => AvainArvoContainer(AvainArvoConstants.perusopetuksenKieliKey, k))

        //Todo, halutaanko tässä tapauksessa asettaa myös avain-arvo peruskouluSuoritettuKey -> true? Onko tällä merkitystä?
        korkeimmatArvosanatHakemukseltaJaSupasta ++ suoritusKieliHakemukselta

      case _ => Set.empty
    }

    arvot
  }
}
