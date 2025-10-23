package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, AvainArvoYliajo, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Laajuus, NuortenPerusopetuksenOppiaineenOppimaara, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, Suoritus, Telma, VapaaSivistystyo, YOOpiskeluoikeus}
import org.slf4j.LoggerFactory

import java.time.LocalDate
import scala.collection.immutable

//Opiskeluoikeudet sisältävät kaiken lähdedatan, käyttö nykyisellään vain debug-tarkoituksiin.
case class AvainArvoConverterResults(personOid: String,
                                     containers: Set[AvainArvoContainer],
                                     opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq.empty) {
  def getAvainArvoMap(): Map[String, String] = {
    containers.map(aa => aa.avain -> aa.arvo).toMap
  }
}

case class AvainArvoContainer(avain: String,
                              arvo: String,
                              duplikaatti: Boolean = false,
                              selitteet: Seq[String] = Seq.empty)

object AvainArvoConstants {

  //Tämä taulukko on olemassa, jotta yliajot saadaan toimitettua myös avain-aliaksille.
  //Peruskoulun aineiden arvosanojen osalta perustuu koodiston koskioppiaineetyleissivistava koodiarvoille (29 kpl).
  val avainToRinnakkaisAvaimet = Map(
    perusopetuksenKieliKey -> Set.empty,
    peruskouluPaattotodistusvuosiKey -> Set("PK_PAATTOTODISTUSVUOSI"),
    peruskouluSuoritusvuosiKey -> Set("PK_SUORITUSVUOSI"),
    peruskouluSuoritettuKey -> Set("PK_TILA"),
    lukioSuoritettuKey -> Set("LK_TILA"),
    yoSuoritettuKey -> Set("YO_TILA"),
    ammSuoritettuKey -> Set("AM_TILA"),
    telmaSuoritettuKey -> Set("LISAKOULUTUS_TELMA"),
    telmaSuoritusvuosiKey -> Set("LISAPISTEKOULUTUS_TELMA_SUORITUSVUOSI"),
    opistovuosiSuoritettuKey -> Set("LISAKOULUTUS_OPISTO"),
    opistovuosiSuoritusvuosiKey -> Set("LISAPISTEKOULUTUS_OPISTO_SUORITUSVUOSI"),
    peruskouluArvosana_Akieli -> Set("PK_ARVOSANA_A"),
    peruskouluArvosana_A1kieli -> Set("PK_ARVOSANA_A1"),
    peruskouluArvosana_A2kieli -> Set("PK_ARVOSANA_A2"),
    peruskouluArvosana_A3kieli -> Set("PK_ARVOSANA_B1"),
    peruskouluArvosana_B1kieli -> Set("PK_ARVOSANA_B2"),
    peruskouluArvosana_B2kieli -> Set("PK_ARVOSANA_B3"),
    peruskouluArvosana_biologia -> Set("PK_ARVOSANA_BI"),
    peruskouluArvosana_eitiedossa -> Set("PK_ARVOSANA_XX"),
    peruskouluArvosana_filosofia -> Set("PK_ARVOSANA_FI"),
    peruskouluArvosana_fysiikka -> Set("PK_ARVOSANA_FY"),
    peruskouluArvosana_historia -> Set("PK_ARVOSANA_HI"),
    peruskouluArvosana_kemia -> Set("PK_ARVOSANA_KE"),
    peruskouluArvosana_kotitalous -> Set("PK_ARVOSANA_KO"),
    peruskouluArvosana_kuvataide -> Set("PK_ARVOSANA_KU"),
    peruskouluArvosana_kasityo -> Set("PK_ARVOSANA_KS"),
    peruskouluArvosana_liikunta -> Set("PK_ARVOSANA_LI"),
    peruskouluArvosana_maantieto -> Set("PK_ARVOSANA_GE"),
    peruskouluArvosana_matematiikka -> Set("PK_ARVOSANA_MA"),
    peruskouluArvosana_musiikki -> Set("PK_ARVOSANA_MU"),
    peruskouluArvosana_opintoohjaus -> Set("PK_ARVOSANA_OP"),
    peruskouluArvosana_tyoelamataidot -> Set("PK_ARVOSANA_OPA"),
    peruskouluArvosana_psykologia -> Set("PK_ARVOSANA_PS"),
    peruskouluArvosana_terveystieto -> Set("PK_ARVOSANA_TE"),
    peruskouluArvosana_uskontoet -> Set("PK_ARVOSANA_ET"),
    peruskouluArvosana_uskontoet -> Set("PK_ARVOSANA_KT"),
    peruskouluArvosana_yhteiskuntaoppi -> Set("PK_ARVOSANA_YH"),
    peruskouluArvosana_ymparistooppi -> Set("PK_ARVOSANA_YL"),
    peruskouluArvosana_aidinkielenomainen -> Set("PK_ARVOSANA_AOM"),
    peruskouluArvosana_aidinkielijakirjallisuus -> Set("PK_ARVOSANA_AI"),
    peruskouluOppiaineenKieli_A1 -> Set("PK_ARVOSANA_A1_OPPIAINE"),
    peruskouluOppiaineenKieli_A2 -> Set("PK_ARVOSANA_A2_OPPIAINE"),
    peruskouluOppiaineenKieli_A3 -> Set("PK_ARVOSANA_A3_OPPIAINE"),
    peruskouluOppiaineenKieli_B1 -> Set("PK_ARVOSANA_B1_OPPIAINE"),
    peruskouluOppiaineenKieli_B2 -> Set("PK_ARVOSANA_B2_OPPIAINE"),
    peruskouluOppiaineenKieli_AI -> Set("PK_ARVOSANA_AI_OPPIAINE")
  )

  final val perusopetuksenKieliKey = "perusopetuksen_kieli"
  final val peruskouluPaattotodistusvuosiKey = "peruskoulu_paattotodistusvuosi"
  final val peruskouluSuoritusvuosiKey = "peruskoulu_suoritusvuosi"
  final val peruskouluSuoritettuKey = "perustutkinto_suoritettu"
  final val lukioSuoritettuKey = "lukio_suoritettu"
  final val yoSuoritettuKey = "yo-tutkinto_suoritettu"
  final val ammSuoritettuKey = "ammatillinen_suoritettu"

  final val telmaSuoritettuKey = "lisapistekoulutus_telma"
  final val telmaSuoritusvuosiKey = "lisapistekoulutus_telma_vuosi"

  final val opistovuosiSuoritettuKey = "lisapistekoulutus_opisto"
  final val opistovuosiSuoritusvuosiKey = "lisapistekoulutus_opisto_vuosi"

  final val peruskouluArvosana_Akieli = "PERUSKOULU_ARVOSANA_A"
  final val peruskouluArvosana_A1kieli = "PERUSKOULU_ARVOSANA_A1"
  final val peruskouluArvosana_A2kieli = "PERUSKOULU_ARVOSANA_A2"
  final val peruskouluArvosana_A3kieli = "PERUSKOULU_ARVOSANA_B1"
  final val peruskouluArvosana_B1kieli = "PERUSKOULU_ARVOSANA_B2"
  final val peruskouluArvosana_B2kieli = "PERUSKOULU_ARVOSANA_B3"
  final val peruskouluArvosana_biologia = "PERUSKOULU_ARVOSANA_BI"
  final val peruskouluArvosana_eitiedossa = "PERUSKOULU_ARVOSANA_XX"
  final val peruskouluArvosana_filosofia = "PERUSKOULU_ARVOSANA_FI"
  final val peruskouluArvosana_fysiikka = "PERUSKOULU_ARVOSANA_FY"
  final val peruskouluArvosana_historia = "PERUSKOULU_ARVOSANA_HI"
  final val peruskouluArvosana_kemia = "PERUSKOULU_ARVOSANA_KE"
  final val peruskouluArvosana_kotitalous = "PERUSKOULU_ARVOSANA_KO"
  final val peruskouluArvosana_kuvataide = "PERUSKOULU_ARVOSANA_KU"
  final val peruskouluArvosana_kasityo = "PERUSKOULU_ARVOSANA_KS"
  final val peruskouluArvosana_liikunta = "PERUSKOULU_ARVOSANA_LI"
  final val peruskouluArvosana_maantieto = "PERUSKOULU_ARVOSANA_GE"
  final val peruskouluArvosana_matematiikka = "PERUSKOULU_ARVOSANA_MA"
  final val peruskouluArvosana_musiikki = "PERUSKOULU_ARVOSANA_MU"
  final val peruskouluArvosana_opintoohjaus = "PERUSKOULU_ARVOSANA_OP"
  final val peruskouluArvosana_tyoelamataidot = "PERUSKOULU_ARVOSANA_OPA"
  final val peruskouluArvosana_psykologia = "PERUSKOULU_ARVOSANA_PS"
  final val peruskouluArvosana_terveystieto = "PERUSKOULU_ARVOSANA_TE"
  final val peruskouluArvosana_uskontoet = "PERUSKOULU_ARVOSANA_ET"
  final val peruskouluArvosana_uskontoet2 = "PERUSKOULU_ARVOSANA_KT"
  final val peruskouluArvosana_yhteiskuntaoppi = "PERUSKOULU_ARVOSANA_YH"
  final val peruskouluArvosana_ymparistooppi = "PERUSKOULU_ARVOSANA_YL"
  final val peruskouluArvosana_aidinkielenomainen = "PERUSKOULU_ARVOSANA_AOM"
  final val peruskouluArvosana_aidinkielijakirjallisuus = "PERUSKOULU_ARVOSANA_AI"

  //Todo, varmistettava että tässä on kaikki aineet joille on tarpeen merkitä kieli erillisen avaimen alle
  final val peruskouluOppiaineenKieli_A1 = "PERUSKOULU_ARVOSANA_A1_OPPIAINEEN_KIELI"
  final val peruskouluOppiaineenKieli_A2 = "PERUSKOULU_ARVOSANA_A2_OPPIAINEEN_KIELI"
  final val peruskouluOppiaineenKieli_A3 = "PERUSKOULU_ARVOSANA_A3_OPPIAINEEN_KIELI"
  final val peruskouluOppiaineenKieli_B1 = "PERUSKOULU_ARVOSANA_B1_OPPIAINEEN_KIELI"
  final val peruskouluOppiaineenKieli_B2 = "PERUSKOULU_ARVOSANA_B2_OPPIAINEEN_KIELI"
  final val peruskouluOppiaineenKieli_AI = "PERUSKOULU_ARVOSANA_AI_OPPIAINEEN_KIELI"


  //Todo, tarkistetaan vielä arvosanojen muoto uudessa mallissa
  final val peruskouluAineenArvosanaPrefix = "PERUSKOULU_ARVOSANA_"
  //final val peruskouluAineenArvosanaDuplikaattiPrefix = "PK_"

  //Nämä tulevat aineen arvosanojen perään, eli esimerkiksi jos varsinainen arvosana
  // on avaimen "PK_B1" alla, tulee kieli avainten "PK_B1_OPPIAINE" ja "PK_B1_OPPIAINEEN_KIELI" alle
  final val peruskouluAineenKieliPostfix = "_OPPIAINEEN_KIELI"
  //final val peruskouluAineenKieliDuplikaattiPostfix = "_OPPIAINE"

  //Lisäpistekoulutusten minimilaajuudet
  final val telmaMinimiLaajuus: BigDecimal = 25
  final val tuvaMinimiLaajuus: BigDecimal = 19
  final val opistovuosiMinimiLaajuus: BigDecimal = 26.5
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

//Tätä moduulia varten tehdään kahdenlaista filtteröintiä. Vahvistuspäivän mukaan tapahtuva suoritusten filtteröinti selitteineen tapahtuu tämän moduulin sisällä.
//Laskennan alkamishetken mukaan tapahtuva oppijan tiettynä ajanhetkellä voimassaollut versio haetaan tämän moduulin ulkopuolella.
object AvainArvoConverter {

  val LOG = LoggerFactory.getLogger(getClass)
  
  def convertOpiskeluoikeudet(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): AvainArvoConverterResults = {

    val peruskouluArvot = convertPeruskouluArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val ammatillisetArvot = convertAmmatillisetArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val yoArvot = convertYoArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val lukioArvot = convertLukioArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan) //TODO, lukiosuoritukset pitää vielä parseroida
    val lisapistekoulutusArvot = convertLisapistekoulutukset(personOid, opiskeluoikeudet)

    val avainArvot = peruskouluArvot ++ ammatillisetArvot ++ yoArvot ++ lukioArvot ++ lisapistekoulutusArvot

    AvainArvoConverterResults(personOid, avainArvot, opiskeluoikeudet)
  }

  def convertTelma(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): Set[AvainArvoContainer] = {
    val telmat = opiskeluoikeudet.collect {
      case o: AmmatillinenOpiskeluoikeus => o.suoritukset.collect { case s: Telma => s }
    }.flatten

    val riittavaLaajuus: Seq[Telma] = telmat.filter(t => t.hyvaksyttyLaajuus.exists(laajuus => laajuus.arvo >= AvainArvoConstants.telmaMinimiLaajuus))
    val tuoreinRiittava: Option[Telma] = riittavaLaajuus.maxByOption(_.suoritusVuosi)

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

    val suoritusArvo = AvainArvoContainer(AvainArvoConstants.telmaSuoritettuKey, tuoreinRiittava.isDefined.toString, false, suoritusSelite)

    val suoritusVuosiArvo = if (tuoreinRiittava.isDefined) {
      Some(AvainArvoContainer(AvainArvoConstants.telmaSuoritusvuosiKey, tuoreinRiittava.get.suoritusVuosi.toString))
    } else None

    suoritusVuosiArvo.map(Set(suoritusArvo, _)).getOrElse(Set(suoritusArvo))
  }

  def convertOpistovuosi(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): Set[AvainArvoContainer] = {
    val vstOpistovuodet = opiskeluoikeudet.collect {
      case o: GeneerinenOpiskeluoikeus => o.suoritukset.collect { case s: VapaaSivistystyo => s }
    }.flatten

    val riittavaLaajuus: Seq[VapaaSivistystyo] =
      vstOpistovuodet.filter(t => t.hyvaksyttyLaajuus.exists(laajuus => laajuus.arvo >= AvainArvoConstants.opistovuosiMinimiLaajuus))
    val tuoreinRiittava: Option[VapaaSivistystyo] = riittavaLaajuus.maxByOption(_.suoritusVuosi)

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

    val suoritusArvo = AvainArvoContainer(AvainArvoConstants.opistovuosiSuoritettuKey, tuoreinRiittava.isDefined.toString, false, suoritusSelite)

    val suoritusVuosiArvo = if (tuoreinRiittava.isDefined) {
      Some(AvainArvoContainer(AvainArvoConstants.opistovuosiSuoritusvuosiKey, tuoreinRiittava.get.suoritusVuosi.toString))
    } else None

    suoritusVuosiArvo.map(Set(suoritusArvo, _)).getOrElse(Set(suoritusArvo))
  }

  def convertLisapistekoulutukset(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): Set[AvainArvoContainer] = {
    //todo tuva
    //todo kansanopisto?
    val telmaArvot = convertTelma(personOid, opiskeluoikeudet)
    val opistovuosiArvot = convertOpistovuosi(personOid, opiskeluoikeudet)

    telmaArvot ++ opistovuosiArvot
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

    val arvot = Set(AvainArvoContainer(AvainArvoConstants.ammSuoritettuKey, validSuoritukset.nonEmpty.toString, false, Seq(ammSelite)))

    LOG.info(s"Ammatilliset arvot käsitelty henkilölle $personOid. $arvot")
    arvot
  }

  def convertYoArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    val yoOpiskeluoikeudet: Seq[(YOOpiskeluoikeus, Option[LocalDate])] = opiskeluoikeudet.collect { case o: YOOpiskeluoikeus => (o, o.yoTutkinto.valmistumisPaiva) }

    val hasYoSuoritus = yoOpiskeluoikeudet.exists(_._2.exists(v => v.isBefore(vahvistettuViimeistaan) || v.equals(vahvistettuViimeistaan)))

    val paivat = yoOpiskeluoikeudet.flatMap(_._2).distinct
    val valmistumispaivaSelite = if (paivat.nonEmpty) s" Valmistumispaivat: ${paivat.mkString(", ")}" else ""
    val yoSelite = s"Löytyi yhteensä ${yoOpiskeluoikeudet.size} YO-opiskeluoikeutta." + valmistumispaivaSelite

    val arvot = Set(AvainArvoContainer(AvainArvoConstants.yoSuoritettuKey, hasYoSuoritus.toString, false, Seq(yoSelite)))

    LOG.info(s"Yo-arvot käsitelty henkilölle $personOid. $arvot")
    arvot
  }

  //TODO lukiosuorituksia ei ole vielä parseroitu eikä niitä saada Koskesta massaluovutusrajapinnan kautta. Tämä päättely ei siis vielä toimi.
  def convertLukioArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    //val lukioOpiskeluoikeudet = opiskeluoikeudet.collect { case o: GeneerinenOpiskeluoikeus => o }
    val hasLukioSuoritus = false
    val lukioSelite = s"Lukiosuorituksia ei vielä saada Koskesta massaluovutusrajapinnan kautta."

    val arvot = Set(AvainArvoContainer(AvainArvoConstants.lukioSuoritettuKey, hasLukioSuoritus.toString, false, Seq(lukioSelite)))

    LOG.info(s"Lukioarvot käsitelty henkilölle $personOid. $arvot")
    arvot
  }

  //Mahdolliset oppiaineen oppimäärät palautetaan vain, jos perusopetuksen oppimäärä löytyi.
  def filterForPeruskoulu(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): (Option[PerusopetuksenOppimaara], Seq[NuortenPerusopetuksenOppiaineenOppimaara]) = {
    val perusopetuksenOpiskeluoikeudet = opiskeluoikeudet.collect { case po: PerusopetuksenOpiskeluoikeus => po }
    val (vahvistetut, eiVahvistetut) =
      perusopetuksenOpiskeluoikeudet
        .flatMap(po => po.suoritukset.find(_.isInstanceOf[PerusopetuksenOppimaara]).map(_.asInstanceOf[PerusopetuksenOppimaara]))
        .partition(o => o.vahvistusPaivamaara.isDefined)

    val oppiaineeOppimaarat = perusopetuksenOpiskeluoikeudet.flatMap(po => po.suoritukset.find(_.isInstanceOf[NuortenPerusopetuksenOppiaineenOppimaara]).map(_.asInstanceOf[NuortenPerusopetuksenOppiaineenOppimaara]))

    if (vahvistetut.size > 1) {
      LOG.error(s"Oppijalle $personOid enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
      throw new RuntimeException(s"Oppijalle $personOid löytyy enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
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
  def korkeimmatPerusopetuksenArvosanatAineittain(perusopetuksenOppimaara: Option[PerusopetuksenOppimaara], oppiaineenOppimaarat: Seq[NuortenPerusopetuksenOppiaineenOppimaara]): Set[AvainArvoContainer] = {
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
    val (perusopetuksenOppimaara: Option[PerusopetuksenOppimaara], oppiaineenOppimaarat: Seq[NuortenPerusopetuksenOppiaineenOppimaara]) = filterForPeruskoulu(personOid, opiskeluoikeudet)

    val oppimaaraOnVahvistettu: Boolean = perusopetuksenOppimaara.exists(_.vahvistusPaivamaara.isDefined)
    val vahvistusPvm = perusopetuksenOppimaara.map(_.vahvistusPaivamaara)
    val vahvistettuAjoissa: Boolean = perusopetuksenOppimaara.flatMap(_.vahvistusPaivamaara).exists(v => v.isBefore(vahvistettuViimeistaan) || v.equals(vahvistettuViimeistaan))

    val kieliArvot: Option[AvainArvoContainer] = perusopetuksenOppimaara
      .map(_.suoritusKieli.arvo)
      .map(kieli => AvainArvoContainer(AvainArvoConstants.perusopetuksenKieliKey, kieli))

    val arvot = if (oppimaaraOnVahvistettu) {
      if (vahvistettuAjoissa) {
        val vahvistettuAjoissaSelite = s"Löytyi perusopetuksen oppimäärä, joka on vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä. Vahvistuspäivä: ${vahvistusPvm.map(_.toString).get}"
        val arvosanaArvot: Set[AvainArvoContainer] = korkeimmatPerusopetuksenArvosanatAineittain(perusopetuksenOppimaara, Seq.empty)

        val suoritusVuosiArvo: Option[AvainArvoContainer] = perusopetuksenOppimaara
          .flatMap(vo => vo.vahvistusPaivamaara.map(_.getYear))
          .map(year => AvainArvoContainer(AvainArvoConstants.peruskouluSuoritusvuosiKey, year.toString, false, Seq(vahvistettuAjoissaSelite)))

        val suoritusArvo = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritettuKey, vahvistettuAjoissa.toString, false, Seq(vahvistettuAjoissaSelite))

        arvosanaArvot ++ kieliArvot ++ suoritusVuosiArvo ++ Some(suoritusArvo)
      } else {
        val vahvistettuMyohassaSelite = s"Löytyi perusopetuksen oppimäärä, mutta sitä ei ole vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä. Vahvistuspäivä: ${perusopetuksenOppimaara.map(_.vahvistusPaivamaara)}"
        val suoritusArvo = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritettuKey, vahvistettuAjoissa.toString, false, Seq(vahvistettuMyohassaSelite))
        Set(suoritusArvo) ++ kieliArvot
      }
    } else {
      kieliArvot.toSet
    }
    arvot
  }
}
