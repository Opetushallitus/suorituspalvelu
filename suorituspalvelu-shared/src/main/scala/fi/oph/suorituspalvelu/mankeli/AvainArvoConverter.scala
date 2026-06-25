package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.PerusopetuksenYksilollistaminen.toIntValue
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, DIATutkinto, EBTutkinto, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Koodi, Laajuus, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, PerusopetuksenOppimaaranOppiaineidenSuoritus, PerusopetuksenYksilollistaminen, Suoritus, SuoritusTila, Telma, Tuva, VapaaSivistystyo, YOOpiskeluoikeus}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, KoutaHaku, Ohjausparametrit}
import fi.oph.suorituspalvelu.mankeli.ataru.{AtaruArvosanaParser, AvainArvoConverterUtil, AvainArvoDTO}
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import org.slf4j.LoggerFactory

import java.util.{UUID, List as JavaList}
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
    pkSuorituslukukausiKey -> "Peruskoulun suorituslukukausi",
    lukioSuoritettuKey -> "Lukio suoritettu",
    yoSuoritettuKey -> "Ylioppilastutkinto suoritettu",
    yoSuoritusvuosiKey -> "Ylioppilastutkinnon suoritusvuosi",
    yoSuorituslukukausiKey -> "Ylioppilastutkinnon suorituslukukausi",
    ammSuoritettuKey -> "Ammatillinen tutkinto suoritettu",
    ebSuoritettuKey -> "EB-tutkinto suoritettu",
    ebSuoritusvuosiKey -> "EB-tutkinnon suoritusvuosi",
    diaSuoritettuKey -> "DIA-tutkinto suoritettu",
    diaSuoritusvuosiKey -> "DIA-tutkinnon suoritusvuosi",
    ammSuoritusvuosiKey -> "Ammatillisen tutkinnon suoritusvuosi",
    ammSuorituslukukausiKey -> "Ammatillisen tutkinnon suorituslukukausi",
    ammTutkintoKieliKey -> "Ammatillisen tutkinnon suorituskieli",
    yoTutkintoKieliKey -> "Ylioppilastutkinnon suorituskieli",
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
    yksMatAiKey -> "Onko joku hakemuksen hakutoiveista harkinnanvarainen siksi, että matematiikka ja äidinkieli yksilöllistettyjä",
    ensikertalainenKey -> "Korkeakoulun ensikertalainen"
  )

  final val perusopetuksenKieliKey = "perusopetuksen_kieli"
  final val peruskouluPaattotodistusvuosiKey = "PK_PAATTOTODISTUSVUOSI"
  final val peruskouluSuoritettuKey = "PK_TILA"
  final val lukioSuoritettuKey = "LK_TILA"
  final val yoSuoritettuKey = "YO_TILA"
  final val ammSuoritettuKey = "AM_TILA"
  final val ebSuoritettuKey = "EB_TILA"
  final val ebSuoritusvuosiKey = "EB_SUORITUSVUOSI"
  final val ebOppiainePrefix = "EB_"
  final val ebOppiaineLaajuusPostfix = "_LAAJUUS"
  final val ebOppiaineWrittenPostfix = "_WRITTEN"
  final val ebOppiaineOralPostfix = "_ORAL"
  final val ebOppiaineFinalPostfix = "_FINAL"
  final val ebOppiaineKieliPostfix = "_KIELI"
  final val ebWrittenKomponenttiKoodi = "Written" // koodisto ebtutkinnonoppiaineenkomponentti
  final val ebOralKomponenttiKoodi = "Oral" // koodisto ebtutkinnonoppiaineenkomponentti
  final val ebFinalKomponenttiKoodi = "Final" // koodisto ebtutkinnonoppiaineenkomponentti
  final val diaSuoritettuKey = "DIA_TILA"
  final val diaSuoritusvuosiKey = "DIA_SUORITUSVUOSI"
  final val diaOppiainePrefix = "DIA_"
  final val diaOppiaineLaajuusPostfix = "_LAAJUUS"
  final val diaOppiaineKirjallinenPostfix = "_KIRJALLINEN"
  final val diaOppiaineSuullinenPostfix = "_SUULLINEN"
  final val diaOppiaineVastaavuusPostfix = "_VASTAAVUUS"

  final val peruskouluSuoritusvuosiKey = "PK_SUORITUSVUOSI"
  final val ammSuoritusvuosiKey = "AM_SUORITUSVUOSI"
  final val yoSuoritusvuosiKey = "YO_SUORITUSVUOSI"

  final val pkSuorituslukukausiKey = "PK_SUORITUSLUKUKAUSI"
  final val ammSuorituslukukausiKey = "AM_SUORITUSLUKUKAUSI"
  final val yoSuorituslukukausiKey = "YO_SUORITUSLUKUKAUSI"

  final val ammTutkintoKieliKey = "AMM_TUTKINTO_KIELI"
  final val yoTutkintoKieliKey = "YO_TUTKINTO_KIELI"

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
  final val peruskouluArvosana_A3kieli = "PK_A3"
  final val peruskouluArvosana_B1kieli = "PK_B1"
  final val peruskouluArvosana_B2kieli = "PK_B2"
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
  final val peruskouluAineValinnainenPostfix = "_VAL"

  final val pohjakoulutusToinenAste = "POHJAKOULUTUS"

  //Nämä tulevat aineen arvosanojen perään, eli esimerkiksi jos varsinainen arvosana
  // on avaimen "PK_B1" alla, tulee kieli avaimen "PK_B1_OPPIAINE" alle
  final val peruskouluAineenKieliOppiainePostfix = "_OPPIAINE"
  final val peruskouluAineenKieliTietoPostfix = "_KIELITIETO"

  val aidinkieliKoodiMapping: Map[String, String] = Map(
    "AI1"  -> "FI",
    "AI2"  -> "SV",
    "AI3"  -> "SE",
    "AI4"  -> "RI",
    "AI5"  -> "VK",
    "AI6"  -> "XX",
    "AI7"  -> "FI_2",
    "AI8"  -> "SV_2",
    "AI9"  -> "FI_SE",
    "AI10" -> "XX",
    "AI11" -> "FI_VK",
    "AI12" -> "SV_VK",
    "AIAI" -> "XX"
  )

  // Käänteinen kuvaus: atarulta tuleva äidinkielitieto on jo muunnettu FI/SV/... -muotoon,
  // joten synteettistä oppiainetta varten täytyy palauttaa raaka AI-koodi.
  // "XX" vastaa atarussa "muu-oppilaan-aidinkieli" -> AI6.
  val aidinkieliKoodiReverseMapping: Map[String, String] = Map(
    "FI"    -> "AI1",
    "SV"    -> "AI2",
    "SE"    -> "AI3",
    "RI"    -> "AI4",
    "VK"    -> "AI5",
    "XX"    -> "AI6",
    "FI_2"  -> "AI7",
    "SV_2"  -> "AI8",
    "FI_SE" -> "AI9",
    //ruotsi saamenkielisille olisi AI10, mutta tätä arvoa ei saada hakemuksen kautta. XX tulkitaan arvoksi AI6.
    "FI_VK" -> "AI11",
    "SV_VK" -> "AI12",
    //Myös AIAI on arvo, jota ei saada atarun kautta.
  )

  val oppiaineKoodiMapping: Map[String, String] = Map(
    "ET"  -> "KT"
  )

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

  final val ataruPohjakoulutusKey = "base-education-2nd"
  final val ataruPohjakoulutusVuosiKey = "pohjakoulutus_vuosi"
  final val ataruPohjakoulutusKieliKey = "pohjakoulutus_kieli"

  final val POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS = "0"
  final val POHJAKOULUTUS_PERUSKOULU = "1"
  final val POHJAKOULUTUS_PERUSKOULU_OSITTAIN_YKSILOLLISTETTY = "2"
  final val POHJAKOULUTUS_PERUSKOULU_TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY = "3"
  final val POHJAKOULUTUS_PERUSKOULU_PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY = "6"
  final val POHJAKOULUTUS_PERUSKOULU_OSITTAIN_RAJOITETTU = "8"
  final val POHJAKOULUTUS_PERUSKOULU_PAAOSIN_TAI_KOKONAAN_RAJOITETTU = "9"
  final val POHJAKOULUTUS_EI_PAATTOTODISTUSTA = "7"

  val hakemuksenPohjakoulutuksetUskotaanHakemusta = Set(POHJAKOULUTUS_PERUSKOULU, POHJAKOULUTUS_PERUSKOULU_OSITTAIN_YKSILOLLISTETTY,
    POHJAKOULUTUS_PERUSKOULU_PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY, POHJAKOULUTUS_PERUSKOULU_TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY,
    POHJAKOULUTUS_PERUSKOULU_OSITTAIN_RAJOITETTU, POHJAKOULUTUS_PERUSKOULU_PAAOSIN_TAI_KOKONAAN_RAJOITETTU)

  final val arvosananLahdeSeliteSupa = "Tieto löytyi Suorituspalvelusta."
  final val arvosananLahdeSeliteHakemus = "Tieto löytyi hakemukselta."

  final val kielitiedonLahdeSeliteSupa = "Kielitieto löytyi Suorituspalvelusta."
  final val kielitiedonLahdeSeliteHakemus = "Kielitieto löytyi hakemukselta."

  final val yksMatAiKey = "yks_mat_ai"
  final val ensikertalainenKey = "ensikertalainen"

  val numeerisetPeruskoulunArvosanat = Set("10", "9", "8", "7", "6", "5", "4")

  val aKielet: Set[String] = Set("A1", "A2")
  val bKielet: Set[String] = Set("B1", "B2", "B3")
  val kieltenNumerointiKoodit: Set[String] = aKielet ++ bKielet ++ Set("AI")
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


  def convertHakutoiveet(hakemus: AtaruValintalaskentaHakemus, hakukohderyhmatByHakukohde: Map[String, Set[String]]): (List[ValintalaskentaHakutoive], Set[AvainArvoContainer]) = {
    val hakutoiveResults: List[(ValintalaskentaHakutoive, Set[AvainArvoContainer])] = hakemus.hakutoiveet.zipWithIndex.map((hakutoive, index) => {
      val prioriteetti = index + 1
      val valintalaskentaHakutoive = ValintalaskentaHakutoive(
        hakemus.hakuOid,
        hakutoive.hakukohdeOid,
        prioriteetti,
        hakukohderyhmatByHakukohde.getOrElse(hakutoive.hakukohdeOid, Set.empty)
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

  def convertHakemus(hakemus: AtaruValintalaskentaHakemus, hakukohderyhmatByHakukohde: Map[String, Set[String]]): ConvertedAtaruHakemus = {
    val hakutoiveData: (List[ValintalaskentaHakutoive], Set[AvainArvoContainer]) = convertHakutoiveet(hakemus, hakukohderyhmatByHakukohde)

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

  def convertOpiskeluoikeudet(personOid: String,
                              hakemus: Option[AtaruValintalaskentaHakemus],
                              opiskeluoikeudet: Seq[Opiskeluoikeus],
                              opiskeluoikeudetVahvistettuHetkella: Seq[Opiskeluoikeus],
                              vahvistettuViimeistaan: LocalDate,
                              haku: KoutaHaku,
                              harkinnanvaraisuudet: Option[HakemuksenHarkinnanvaraisuus],
                              hakukohderyhmatByHakukohde: Map[String, Set[String]]): AvainArvoConverterResults = {

    //Todo, valintapisteet avain-arvoiksi
    val convertedHakemus: Option[ConvertedAtaruHakemus] = hakemus.map(h => HakemusConverter.convertHakemus(h, hakukohderyhmatByHakukohde))

    val harkinnanvaraisuusArvot: Option[AvainArvoContainer] = harkinnanvaraisuudet.map(getHarkinnanvaraisuusArvot)

    // Lasketaan "tämä hetki" kerran, jotta kaikki tämän pyynnön sisäiset tarkistukset (ehdot-ikkuna, deadlineOhitettu)
    // näkevät yhtenevän arvon
    val today: LocalDate = LocalDate.now()
    val ehdotIkkunaAuki = !today.isBefore(vahvistettuViimeistaan.minusWeeks(2))
    val ehdotOverrideAktiivinen = ehdotIkkunaAuki && oliEhdotLeikkurihetkella(personOid, opiskeluoikeudetVahvistettuHetkella)

    val toisenAsteenPk: Option[AvainArvoContainer] = if (haku.isToisenAsteenHaku())
      hakemus.map(h => toisenAsteenPohjakoulutus(personOid, h, opiskeluoikeudet, vahvistettuViimeistaan, today, ehdotOverrideAktiivinen)) else None
    val peruskouluArvot = convertPeruskouluArvot(personOid, vahvistettuViimeistaan, hakemus, opiskeluoikeudet, ehdotOverrideAktiivinen)
    val ammatillisetArvot = convertAmmatillisetArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val yoArvot = convertYoArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val ebArvot = convertEbArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val diaArvot = convertDiaArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val lukioArvot = convertLukioArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan) //TODO, lukiosuoritukset pitää vielä parseroida
    val lisapistekoulutusArvot = convertLisapistekoulutukset(personOid, opiskeluoikeudet, haku, toisenAsteenPk)

    val paatellytArvot: Set[AvainArvoContainer] =
      peruskouluArvot
        ++ ammatillisetArvot
        ++ yoArvot
        ++ ebArvot
        ++ diaArvot
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

  def toisenAsteenPohjakoulutus(personOid: String, hakemus: AtaruValintalaskentaHakemus, opiskeluoikeudet: Seq[Opiskeluoikeus], deadline: LocalDate, today: LocalDate, ehdotOverrideAktiivinen: Boolean): AvainArvoContainer = {
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

    // Ehdot-override: ehdotOverrideAktiivinen (= ikkuna auki ja ehdot leikkurihetkellä) on laskettu ylempänä
    // convertOpiskeluoikeudet-tasolla. Vertailu deadlineen, ei wall-clock nykyhetkeen.
    val nykyinenEiVahvistettuAjoissa = viimeisinOppimaara.exists(po => !oppimaaraVahvistettuAjoissa(po, deadline))
    val ehdotOppimaara: Option[PerusopetuksenOppimaara] =
      if (ehdotOverrideAktiivinen && nykyinenEiVahvistettuAjoissa) viimeisinOppimaara else None

    val kelpaavaOppimaara = viimeisinOppimaara.filter(onKelpaavaOppimaara(_, deadline, today))

    val hakemusPohjakoulutus = hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusKey).flatMap(v => Option.apply(v))
    val hakemusPohjakoulutusVuosi = hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusVuosiKey).flatMap(v => Option.apply(v)).map(_.toInt)

    val (pkResult, pkSelite) = getPohjakoulutusResult(ehdotOppimaara, kelpaavaOppimaara, hakemusPohjakoulutus, hakemusPohjakoulutusVuosi)

    AvainArvoContainer(AvainArvoConstants.pohjakoulutusToinenAste, pkResult, pkSelite)
  }

  private def onKelpaavaOppimaara(oppimaara: PerusopetuksenOppimaara, deadline: LocalDate, today: LocalDate): Boolean = {
    val deadlineOhitettu = today.isAfter(deadline)
    val ysiluokkalainen = oppimaara.luokkaAste.contains(9)
    val suoritusValmis = oppimaara.vahvistusPaivamaara.isDefined
    val suoritusKesken = oppimaara.supaTila.equals(SuoritusTila.KESKEN)
    val vahvistettuAjoissa = oppimaara.vahvistusPaivamaara.exists(vp => vp.isBefore(deadline) || vp.equals(deadline))

    if (deadlineOhitettu) {
      suoritusValmis && vahvistettuAjoissa
    } else {
      (oppimaara.luokkaAste.isEmpty || ysiluokkalainen) && (suoritusValmis || suoritusKesken)
    }
  }

  private def getPohjakoulutusResult(ehdotOppimaara: Option[PerusopetuksenOppimaara],
                                     kelpaavaOppimaara: Option[PerusopetuksenOppimaara],
                                     hakemusPohjakoulutus: Option[String],
                                     hakemusPohjakoulutusVuosi: Option[Int]): (String, Seq[String]) = {
    (ehdotOppimaara, kelpaavaOppimaara, hakemusPohjakoulutus, hakemusPohjakoulutusVuosi) match {
      case (Some(ehdotOppimaara), _, _, _) =>
        val yksilollistaminenIntValue = ehdotOppimaara.yksilollistaminen.map(toIntValue).getOrElse(1).toString
        (yksilollistaminenIntValue, Seq("Hakijalla oli ehdot leikkurihetkellä (pakollisessa aineessa nelonen, oppimäärä vahvistamatta), joten pohjakoulutus päätellään perusopetuksen oppimäärältä vaikka sitä ei ole vahvistettu ajoissa."))

      case (_, Some(oppimaara), _, _) =>
        val yksilollistaminenIntValue = oppimaara.yksilollistaminen.map(toIntValue).getOrElse(1).toString
        (yksilollistaminenIntValue, Seq(s"Supasta löytyi suoritettu perusopetuksen oppimäärä. Vahvistuspäivä ${oppimaara.vahvistusPaivamaara.map(_.toString).getOrElse("")}."))

      case (_, _, Some(pohjakolutus), _) if pohjakolutus == AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS =>
        (AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS, Seq("Hakemuksella on ilmoitettu ulkomainen tutkinto."))

      case (_, _, Some(pohjakoulutus), Some(vuosi)) if vuosi <= 2017 && AvainArvoConstants.hakemuksenPohjakoulutuksetUskotaanHakemusta.contains(pohjakoulutus) =>
        (pohjakoulutus, Seq(s"Hakemuksen pohjakoulutusvuosi on 2017 tai aiemmin, joten käytettiin hakemuksella ilmoitettua pohjakoulutusta $pohjakoulutus."))

      case _ =>
        (AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA, Seq("Supasta tai hakemukselta ei löytynyt sopivaa pohjakoulutusta."))
    }
  }

  def convertTuva(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vuosiVahintaan: Int, toisenAsteenPohjakoulutus: Option[AvainArvoContainer]): Set[AvainArvoContainer] = {
    val tuvaSuoritukset = opiskeluoikeudet.collect {
      case o: GeneerinenOpiskeluoikeus => o.suoritukset.collect { case s: Tuva => s }
    }.flatten
    val yhteisLaajuus: BigDecimal = tuvaSuoritukset.flatMap(_.hyvaksyttyLaajuus.map(_.arvo)).sum
    val kynnyksenYlittamisvuosi: Option[Int] = {
      val sorted = tuvaSuoritukset.sortBy(_.suoritusVuosi)
      sorted.foldLeft((BigDecimal(0), Option.empty[Int])) { case ((cumulative, found), suoritus) =>
        if (found.isDefined) (cumulative, found)
        else {
          val newCumulative = cumulative + suoritus.hyvaksyttyLaajuus.map(_.arvo).getOrElse(BigDecimal(0))
          if (newCumulative >= AvainArvoConstants.tuvaMinimiLaajuus) (newCumulative, Some(suoritus.suoritusVuosi))
          else (newCumulative, None)
        }
      }._2
    }
    val tuoreinOnRiittavanTuore: Boolean = kynnyksenYlittamisvuosi.exists(_ >= vuosiVahintaan)
    val pohjakoulutusSallii = toisenAsteenPohjakoulutus.map(_.arvo).exists(arvo => !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS) && !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA))

    val suoritusSelite = tuvaSuoritukset match {
      case tuvat if tuvat.isEmpty =>
        Seq(s"Ei löytynyt lainkaan Tuva-suoritusta.")
      case _ if kynnyksenYlittamisvuosi.isDefined && !tuoreinOnRiittavanTuore =>
        Seq(s"Tuva-suoritusten minimilaajuus saavutettiin vuonna ${kynnyksenYlittamisvuosi.get}, mikä on liian vanha (vähintään $vuosiVahintaan vaaditaan).")
      case _ if kynnyksenYlittamisvuosi.isDefined && pohjakoulutusSallii =>
        Seq(s"Tuva-suoritusten yhteislaajuus on $yhteisLaajuus ja minimilaajuus saavutettiin vuonna ${kynnyksenYlittamisvuosi.get}.")
      case _ if kynnyksenYlittamisvuosi.isDefined && !pohjakoulutusSallii =>
        Seq(s"Tuva-suoritusten yhteislaajuus on $yhteisLaajuus, mutta hakijan pohjakoulutus (${toisenAsteenPohjakoulutus.map(_.arvo)}) ei salli lisäpistekoulutusten huomioimista.")
      case _ =>
        Seq(s"Tuva-suoritusten yhteislaajuus $yhteisLaajuus ei riitä (minimi: ${AvainArvoConstants.tuvaMinimiLaajuus}).")
    }

    val tuvaHuomioidaan: Boolean = pohjakoulutusSallii && tuoreinOnRiittavanTuore
    val suoritusArvo = AvainArvoContainer(AvainArvoConstants.tuvaSuoritettuKey, tuvaHuomioidaan.toString, suoritusSelite)

    val suoritusVuosiArvo = if (tuvaHuomioidaan) {
      Some(AvainArvoContainer(AvainArvoConstants.tuvaSuoritusvuosiKey, kynnyksenYlittamisvuosi.get.toString))
    } else None

    suoritusVuosiArvo.map(Set(suoritusArvo, _)).getOrElse(Set(suoritusArvo))
  }

  def convertTelma(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vuosiVahintaan: Int, toisenAsteenPohjakoulutus: Option[AvainArvoContainer]): Set[AvainArvoContainer] = {
    val telmat = opiskeluoikeudet.collect {
      case o: AmmatillinenOpiskeluoikeus => o.suoritukset.collect { case s: Telma => s }
    }.flatten
    val yhteisLaajuus: BigDecimal = telmat.flatMap(_.hyvaksyttyLaajuus.map(_.arvo)).sum
    val kynnyksenYlittamisvuosi: Option[Int] = {
      val sorted = telmat.sortBy(_.suoritusVuosi)
      sorted.foldLeft((BigDecimal(0), Option.empty[Int])) { case ((cumulative, found), suoritus) =>
        if (found.isDefined) (cumulative, found)
        else {
          val newCumulative = cumulative + suoritus.hyvaksyttyLaajuus.map(_.arvo).getOrElse(BigDecimal(0))
          if (newCumulative >= AvainArvoConstants.telmaMinimiLaajuus) (newCumulative, Some(suoritus.suoritusVuosi))
          else (newCumulative, None)
        }
      }._2
    }
    val tuoreinOnRiittavanTuore: Boolean = kynnyksenYlittamisvuosi.exists(_ >= vuosiVahintaan)
    val pohjakoulutusSallii = toisenAsteenPohjakoulutus.map(_.arvo).exists(arvo => !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS) && !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA))

    val suoritusSelite = telmat match {
      case telmaSuoritukset if telmaSuoritukset.isEmpty =>
        Seq(s"Ei löytynyt lainkaan Telma-suoritusta.")
      case _ if kynnyksenYlittamisvuosi.isDefined && !tuoreinOnRiittavanTuore =>
        Seq(s"Telma-suoritusten minimilaajuus saavutettiin vuonna ${kynnyksenYlittamisvuosi.get}, mikä on liian vanha (vähintään $vuosiVahintaan vaaditaan).")
      case _ if kynnyksenYlittamisvuosi.isDefined && pohjakoulutusSallii =>
        Seq(s"Telma-suoritusten yhteislaajuus on $yhteisLaajuus ja minimilaajuus saavutettiin vuonna ${kynnyksenYlittamisvuosi.get}.")
      case _ if kynnyksenYlittamisvuosi.isDefined && !pohjakoulutusSallii =>
        Seq(s"Telma-suoritusten yhteislaajuus on $yhteisLaajuus, mutta hakijan pohjakoulutus (${toisenAsteenPohjakoulutus.map(_.arvo)}) ei salli lisäpistekoulutusten huomioimista.")
      case _ =>
        Seq(s"Telma-suoritusten yhteislaajuus $yhteisLaajuus ei riitä (minimi: ${AvainArvoConstants.telmaMinimiLaajuus}).")
    }

    val telmaHuomioidaan: Boolean = pohjakoulutusSallii && tuoreinOnRiittavanTuore
    val suoritusArvo = AvainArvoContainer(AvainArvoConstants.telmaSuoritettuKey, telmaHuomioidaan.toString, suoritusSelite)

    val suoritusVuosiArvo = if (telmaHuomioidaan) {
      Some(AvainArvoContainer(AvainArvoConstants.telmaSuoritusvuosiKey, kynnyksenYlittamisvuosi.get.toString))
    } else None

    suoritusVuosiArvo.map(Set(suoritusArvo, _)).getOrElse(Set(suoritusArvo))
  }

  def convertOpistovuosi(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vuosiVahintaan: Int, toisenAsteenPohjakoulutus: Option[AvainArvoContainer]): Set[AvainArvoContainer] = {
    val vstOpistovuodet = opiskeluoikeudet.collect {
      case o: GeneerinenOpiskeluoikeus => o.suoritukset.collect { case s: VapaaSivistystyo => s }
    }.flatten

    val yhteisLaajuus: BigDecimal = vstOpistovuodet.flatMap(_.hyvaksyttyLaajuus.map(_.arvo)).sum
    val kynnyksenYlittamisvuosi: Option[Int] = {
      val sorted = vstOpistovuodet.sortBy(_.suoritusVuosi)
      sorted.foldLeft((BigDecimal(0), Option.empty[Int])) { case ((cumulative, found), suoritus) =>
        if (found.isDefined) (cumulative, found)
        else {
          val newCumulative = cumulative + suoritus.hyvaksyttyLaajuus.map(_.arvo).getOrElse(BigDecimal(0))
          if (newCumulative >= AvainArvoConstants.opistovuosiMinimiLaajuus) (newCumulative, Some(suoritus.suoritusVuosi))
          else (newCumulative, None)
        }
      }._2
    }
    val tuoreinOnRiittavanTuore: Boolean = kynnyksenYlittamisvuosi.exists(_ >= vuosiVahintaan)
    val pohjakoulutusSallii = toisenAsteenPohjakoulutus.map(_.arvo).exists(arvo => !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_ULKOMAILLA_SUORITETTU_KOULUTUS) && !arvo.equals(AvainArvoConstants.POHJAKOULUTUS_EI_PAATTOTODISTUSTA))

    val suoritusSelite = vstOpistovuodet match {
      case opistovuodet if opistovuodet.isEmpty =>
        Seq(s"Ei löytynyt lainkaan Opistovuosi-suoritusta.")
      case _ if kynnyksenYlittamisvuosi.isDefined && !tuoreinOnRiittavanTuore =>
        Seq(s"Opistovuosi-suoritusten minimilaajuus saavutettiin vuonna ${kynnyksenYlittamisvuosi.get}, mikä on liian vanha (vähintään $vuosiVahintaan vaaditaan).")
      case _ if kynnyksenYlittamisvuosi.isDefined && pohjakoulutusSallii =>
        Seq(s"Opistovuosi-suoritusten yhteislaajuus on $yhteisLaajuus ja minimilaajuus saavutettiin vuonna ${kynnyksenYlittamisvuosi.get}.")
      case _ if kynnyksenYlittamisvuosi.isDefined && !pohjakoulutusSallii =>
        Seq(s"Opistovuosi-suoritusten yhteislaajuus on $yhteisLaajuus, mutta hakijan pohjakoulutus (${toisenAsteenPohjakoulutus.map(_.arvo)}) ei salli lisäpistekoulutusten huomioimista.")
      case _ =>
        Seq(s"Opistovuosi-suoritusten yhteislaajuus $yhteisLaajuus ei riitä (minimi: ${AvainArvoConstants.opistovuosiMinimiLaajuus}).")
    }

    val opistovuosiHuomioidaan: Boolean = pohjakoulutusSallii && tuoreinOnRiittavanTuore
    val suoritusArvo = AvainArvoContainer(AvainArvoConstants.opistovuosiSuoritettuKey, opistovuosiHuomioidaan.toString, suoritusSelite)

    val suoritusVuosiArvo = if (opistovuosiHuomioidaan) {
      Some(AvainArvoContainer(AvainArvoConstants.opistovuosiSuoritusvuosiKey, kynnyksenYlittamisvuosi.get.toString))
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

    val suoritusArvo = Set(AvainArvoContainer(AvainArvoConstants.ammSuoritettuKey, validSuoritukset.nonEmpty.toString, Seq(ammSelite)))
    val valmistumishetkiArvot = validSuoritukset.minByOption(_._2).flatMap(_._2).map(valmistumisPaiva => {
      val suoritusVuosiArvo = AvainArvoContainer(AvainArvoConstants.ammSuoritusvuosiKey, valmistumisPaiva.getYear.toString, Seq(s"Vanhimman ammatillisen suorituksen valmistumispäivä: ${valmistumisPaiva.toString}"))
      val lukukausi = AvainArvoConverterUtil.getLukukausi(valmistumisPaiva)
      val suoritusLukukausiArvo = AvainArvoContainer(AvainArvoConstants.ammSuorituslukukausiKey, lukukausi, Seq(s"Vanhimman ammatillisen suorituksen suorituslukukausi: $lukukausi"))
      Set(suoritusVuosiArvo, suoritusLukukausiArvo)
    }).getOrElse(Set.empty)
    val suoritusKieliArvo = validSuoritukset.minByOption(_._2).map(_._1).flatMap(suoritus => {
      val kieli: Option[String] = suoritus match {
        case apt: AmmatillinenPerustutkinto => Some(apt.suoritusKieli.arvo)
        case att: AmmattiTutkinto => Some(att.suoritusKieli.arvo)
        case eat: ErikoisAmmattiTutkinto => Some(eat.suoritusKieli.arvo)
        case _ => None
      }
      kieli.map(k => AvainArvoContainer(
        AvainArvoConstants.ammTutkintoKieliKey,
        k,
        Seq.empty
      ))
    })

    LOG.info(s"Ammatilliset arvot käsitelty henkilölle $personOid. ${suoritusArvo ++ valmistumishetkiArvot ++ suoritusKieliArvo}")
    suoritusArvo ++ valmistumishetkiArvot ++ suoritusKieliArvo
  }

  def convertYoArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    val yoOpiskeluoikeudet: Seq[(YOOpiskeluoikeus, Option[LocalDate])] = opiskeluoikeudet.collect { case o: YOOpiskeluoikeus if o.yoTutkinto.isDefined => (o, o.yoTutkinto.get.valmistumisPaiva) }

    val hasYoSuoritus = yoOpiskeluoikeudet.exists(_._2.exists(v => v.isBefore(vahvistettuViimeistaan) || v.equals(vahvistettuViimeistaan)))
    val hyvaksyttyYoSuoritus = yoOpiskeluoikeudet.find(_._2.exists(v => v.isBefore(vahvistettuViimeistaan) || v.equals(vahvistettuViimeistaan)))

    val paivat = yoOpiskeluoikeudet.flatMap(_._2).distinct
    val valmistumispaivaSelite = if (paivat.nonEmpty) s" Valmistumispaivat: ${paivat.mkString(", ")}" else ""
    val yoSelite = s"Löytyi yhteensä ${yoOpiskeluoikeudet.size} YO-opiskeluoikeutta." + valmistumispaivaSelite

    val suoritusArvo = Set(AvainArvoContainer(AvainArvoConstants.yoSuoritettuKey, hyvaksyttyYoSuoritus.isDefined.toString, Seq(yoSelite)))
    val valmistumishetkiArvot = hyvaksyttyYoSuoritus.flatMap(_._2).map(valmistumisPaiva => {
      val suoritusVuosiArvo = AvainArvoContainer(AvainArvoConstants.yoSuoritusvuosiKey, valmistumisPaiva.getYear.toString, Seq(s"Ylioppilastutkinnon valmistumispäivä: ${valmistumisPaiva.toString}"))
      val lukukausi = AvainArvoConverterUtil.getLukukausi(valmistumisPaiva)
      val suoritusLukukausiArvo = AvainArvoContainer(AvainArvoConstants.yoSuorituslukukausiKey, lukukausi, Seq(s"Ylioppilastutkinnon suorituslukukausi: $lukukausi"))
      Set(suoritusVuosiArvo, suoritusLukukausiArvo)
    }).getOrElse(Set.empty)
    val suoritusKieliArvo = hyvaksyttyYoSuoritus.flatMap(yo => yo._1.yoTutkinto.map(_.suoritusKieli.arvo)).map(kieli =>
      AvainArvoContainer(
        AvainArvoConstants.yoTutkintoKieliKey,
        kieli,
        Seq(s"Ylioppilastutkinnon suorituskieli: $kieli")
      )
    )

    val kaikkiArvot = suoritusArvo ++ valmistumishetkiArvot ++ suoritusKieliArvo.toSet
    LOG.info(s"Yo-arvot käsitelty henkilölle $personOid. ${kaikkiArvot}")
    kaikkiArvot

  }

  def convertEbArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    val ebTutkinnot: Seq[EBTutkinto] = opiskeluoikeudet
      .collect { case o: GeneerinenOpiskeluoikeus => o }
      .flatMap(_.suoritukset)
      .collect { case eb: EBTutkinto => eb }

    // Päättely tehdään valmiilta (VALMIS-tilainen ja leikkuripäivään mennessä vahvistettu) EB-tutkinnolta. Tämän lisäksi
    // henkilöllä voi (ainakin teoriassa) olla esim. keskeytynyt tutkinto ilman että se on virhe, mutta valmiita sallitaan
    // korkeintaan yksi.
    val valmiitEbTutkinnot = ebTutkinnot.filter(eb =>
      eb.supaTila == SuoritusTila.VALMIS &&
        eb.vahvistusPaivamaara.exists(v => !v.isAfter(vahvistettuViimeistaan)))

    if (valmiitEbTutkinnot.size > 1)
      throw new RuntimeException(s"Oppijalla $personOid on ${valmiitEbTutkinnot.size} valmista EB-tutkintoa, odotettiin korkeintaan yhtä.")

    val valmisEbTutkinto = valmiitEbTutkinnot.headOption

    val ebSelite = valmisEbTutkinto match {
      case Some(eb) => s"Löytyi valmis EB-tutkinto, jonka vahvistuspäivä ${eb.vahvistusPaivamaara.map(_.toString).getOrElse("ei tiedossa")}."
      case None if ebTutkinnot.nonEmpty =>
        s"Löytyi ${ebTutkinnot.size} EB-tutkinto(a), mutta yksikään ei ollut valmis ja vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä."
      case None => "EB-tutkintoa ei löytynyt."
    }

    val suoritusvuosiArvo = valmisEbTutkinto.flatMap(_.vahvistusPaivamaara).map(vp =>
      AvainArvoContainer(AvainArvoConstants.ebSuoritusvuosiKey, vp.getYear.toString, Seq(s"EB-tutkinnon vahvistuspäivä: $vp.")))

    val laajuusArvot = valmisEbTutkinto.toSeq.flatMap(_.osasuoritukset).flatMap(oppiaine =>
      oppiaine.laajuus.map(l =>
        AvainArvoContainer(
          AvainArvoConstants.ebOppiainePrefix + oppiaine.koodi.arvo.toUpperCase + AvainArvoConstants.ebOppiaineLaajuusPostfix,
          l.arvo.toString,
          Seq(s"EB-oppiaineen ${oppiaine.koodi.arvo} laajuus.")))).toSet

    val writtenArvot = valmisEbTutkinto.toSeq.flatMap(_.osasuoritukset).flatMap(oppiaine =>
      oppiaine.osasuoritukset
        .find(_.koodi.arvo == AvainArvoConstants.ebWrittenKomponenttiKoodi)
        .map(written =>
          AvainArvoContainer(
            AvainArvoConstants.ebOppiainePrefix + oppiaine.koodi.arvo.toUpperCase + AvainArvoConstants.ebOppiaineWrittenPostfix,
            written.arvosana.arvosana.arvo,
            Seq(s"EB-oppiaineen ${oppiaine.koodi.arvo} kirjallisen kokeen arvosana.")))).toSet

    val oralArvot = valmisEbTutkinto.toSeq.flatMap(_.osasuoritukset).flatMap(oppiaine =>
      oppiaine.osasuoritukset
        .find(_.koodi.arvo == AvainArvoConstants.ebOralKomponenttiKoodi)
        .map(oral =>
          AvainArvoContainer(
            AvainArvoConstants.ebOppiainePrefix + oppiaine.koodi.arvo.toUpperCase + AvainArvoConstants.ebOppiaineOralPostfix,
            oral.arvosana.arvosana.arvo,
            Seq(s"EB-oppiaineen ${oppiaine.koodi.arvo} suullisen kokeen arvosana.")))).toSet

    val finalArvot = valmisEbTutkinto.toSeq.flatMap(_.osasuoritukset).flatMap(oppiaine =>
      oppiaine.osasuoritukset
        .find(_.koodi.arvo == AvainArvoConstants.ebFinalKomponenttiKoodi)
        .map(finalKomponentti =>
          AvainArvoContainer(
            AvainArvoConstants.ebOppiainePrefix + oppiaine.koodi.arvo.toUpperCase + AvainArvoConstants.ebOppiaineFinalPostfix,
            finalKomponentti.arvosana.arvosana.arvo,
            Seq(s"EB-oppiaineen ${oppiaine.koodi.arvo} lopullinen arvosana.")))).toSet

    val kieliArvot = valmisEbTutkinto.toSeq.flatMap(_.osasuoritukset).flatMap(oppiaine =>
      oppiaine.suorituskieli.map(kieli =>
        AvainArvoContainer(
          AvainArvoConstants.ebOppiainePrefix + oppiaine.koodi.arvo.toUpperCase + AvainArvoConstants.ebOppiaineKieliPostfix,
          kieli.arvo.toLowerCase,
          Seq(s"EB-oppiaineen ${oppiaine.koodi.arvo} suorituskieli.")))).toSet

    val arvot = Set(AvainArvoContainer(AvainArvoConstants.ebSuoritettuKey, valmisEbTutkinto.nonEmpty.toString, Seq(ebSelite))) ++ suoritusvuosiArvo ++ laajuusArvot ++ writtenArvot ++ oralArvot ++ finalArvot ++ kieliArvot
    LOG.info(s"EB-arvot käsitelty henkilölle $personOid. $arvot")
    arvot
  }

  def convertDiaArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    val diaTutkinnot: Seq[DIATutkinto] = opiskeluoikeudet
      .collect { case o: GeneerinenOpiskeluoikeus => o }
      .flatMap(_.suoritukset)
      .collect { case dia: DIATutkinto => dia }

    // Päättely tehdään valmiilta (VALMIS-tilainen ja leikkuripäivään mennessä vahvistettu) DIA-tutkinnolta. Tämän lisäksi
    // henkilöllä voi (ainakin teoriassa) olla esim. keskeytynyt tutkinto ilman että se on virhe, mutta valmiita sallitaan
    // korkeintaan yksi.
    val valmiitDiaTutkinnot = diaTutkinnot.filter(_.supaTila == SuoritusTila.VALMIS)
    if (valmiitDiaTutkinnot.size > 1)
      throw new RuntimeException(s"Oppijalla $personOid on ${valmiitDiaTutkinnot.size} valmista DIA-tutkintoa, odotettiin korkeintaan yhtä.")

    val diaTutkinto = valmiitDiaTutkinnot.headOption
    val vahvistettuAjoissa = diaTutkinto.flatMap(_.vahvistusPaivamaara).exists(v => !v.isAfter(vahvistettuViimeistaan))

    val diaSelite = diaTutkinto match {
      case Some(dia) if dia.vahvistusPaivamaara.exists(v => v.isAfter(vahvistettuViimeistaan)) =>
        s"Löytyi DIA-tutkinto, jonka tila on ${dia.supaTila}, mutta sen vahvistuspäivä ${dia.vahvistusPaivamaara.map(_.toString).getOrElse("ei tiedossa")} on leikkuripäivän $vahvistettuViimeistaan jälkeen."
      case Some(dia) => s"Löytyi DIA-tutkinto, jonka tila on ${dia.supaTila} ja vahvistuspäivä ${dia.vahvistusPaivamaara.map(_.toString).getOrElse("ei tiedossa")}."
      case None => "Valmista DIA-tutkintoa ei löytynyt."
    }

    val suoritusvuosiArvo = if (vahvistettuAjoissa) {
      diaTutkinto.flatMap(_.vahvistusPaivamaara).map(vp =>
        AvainArvoContainer(AvainArvoConstants.diaSuoritusvuosiKey, vp.getYear.toString, Seq(s"DIA-tutkinnon vahvistuspäivä: $vp.")))
    } else None

    val laajuusArvot = diaTutkinto.toSeq.flatMap(_.osasuoritukset).flatMap(oppiaine =>
      oppiaine.laajuus.map(l =>
        AvainArvoContainer(
          AvainArvoConstants.diaOppiainePrefix + oppiaine.koodi.arvo.toUpperCase +AvainArvoConstants.diaOppiaineLaajuusPostfix,
          l.arvo.toString,
          Seq(s"DIA-oppiaineen ${oppiaine.koodi.arvo} laajuus.")))).toSet

    val kirjallinenArvot = diaTutkinto.toSeq.flatMap(_.osasuoritukset).flatMap(oppiaine =>
      oppiaine.kirjallinenKoe.map(koe =>
        AvainArvoContainer(
          AvainArvoConstants.diaOppiainePrefix + oppiaine.koodi.arvo.toUpperCase +AvainArvoConstants.diaOppiaineKirjallinenPostfix,
          koe.arvosana.arvosana.arvo,
          Seq(s"DIA-oppiaineen ${oppiaine.koodi.arvo} kirjallisen kokeen arvosana.")))).toSet

    val suullinenArvot = diaTutkinto.toSeq.flatMap(_.osasuoritukset).flatMap(oppiaine =>
      oppiaine.suullinenKoe.map(koe =>
        AvainArvoContainer(
          AvainArvoConstants.diaOppiainePrefix + oppiaine.koodi.arvo.toUpperCase +AvainArvoConstants.diaOppiaineSuullinenPostfix,
          koe.arvosana.arvosana.arvo,
          Seq(s"DIA-oppiaineen ${oppiaine.koodi.arvo} suullisen kokeen arvosana.")))).toSet

    val vastaavuusArvot = diaTutkinto.toSeq.flatMap(_.osasuoritukset).flatMap(oppiaine =>
      oppiaine.vastaavuustodistuksenTiedot.map(vtt =>
        AvainArvoContainer(
          AvainArvoConstants.diaOppiainePrefix + oppiaine.koodi.arvo.toUpperCase +AvainArvoConstants.diaOppiaineVastaavuusPostfix,
          vtt.keskiarvo.toString,
          Seq(s"DIA-oppiaineen ${oppiaine.koodi.arvo} vastaavuustodistuksen keskiarvo.")))).toSet

    val arvot = Set(AvainArvoContainer(AvainArvoConstants.diaSuoritettuKey, vahvistettuAjoissa.toString, Seq(diaSelite))) ++ suoritusvuosiArvo ++ laajuusArvot ++ kirjallinenArvot ++ suullinenArvot ++ vastaavuusArvot
    LOG.info(s"DIA-arvot käsitelty henkilölle $personOid. $arvot")
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

  //Oppimäärä katsotaan vahvistetuksi ajoissa, kun vahvistusPäivämäärä on olemassa ja viimeistään deadlinena.
  //Negaatio kattaa sekä vahvistamattomat että leikkurin jälkeen vahvistetut oppimäärät.
  private def oppimaaraVahvistettuAjoissa(o: PerusopetuksenOppimaara, deadline: LocalDate): Boolean =
    o.vahvistusPaivamaara.exists(!_.isAfter(deadline))

  //Tarkistaa oliko hakijalla "ehdot" annettujen opiskeluoikeuksien tilassa: pakollisessa aineessa tai valinnaisessa kielessä nelonen,
  //oppimäärää ei ollut vahvistettu, eikä kyseessä ole vuosiluokkiin sitoutumaton opetus eikä luokalle jääminen.
  private def oliEhdotLeikkurihetkella(personOid: String, opiskeluoikeudetVahvistettuHetkella: Seq[Opiskeluoikeus]): Boolean = {
    opiskeluoikeudetVahvistettuHetkella.nonEmpty &&
      etsiViimeisinPeruskoulu(personOid, opiskeluoikeudetVahvistettuHetkella, salliMontaValmista = true).exists(o =>
        o.vahvistusPaivamaara.isEmpty
          && !o.vuosiluokkiinSitoutumatonOpetus
          && !o.jaaLuokalle.contains(true)
          && o.aineet.exists(a => (a.pakollinen || KoskiUtil.sisallytettavatEiPakollisetKielet.contains(a.koodi.arvo)) && a.arvosana.arvo == "4")
      )
  }

  def perusopetuksenValinnaisetOppiaineetToAvainArvot(aineet: Seq[PerusopetuksenOppiaine]): Set[AvainArvoContainer] = {
    val byAine = aineet.groupBy(_.koodi.arvo)

    byAine.flatMap((aine, ainesuoritukset) => {
      ainesuoritukset.zipWithIndex.map((aine, i) => {
        val arvosanaAvain = AvainArvoConstants.peruskouluAineenArvosanaPrefix + aine.koodi.arvo + AvainArvoConstants.peruskouluAineValinnainenPostfix + (i+1)
        val arvosanaArvot: AvainArvoContainer = AvainArvoContainer(arvosanaAvain, aine.arvosana.arvo, Seq(AvainArvoConstants.arvosananLahdeSeliteSupa))

        Set(arvosanaArvot)
      })
    }).flatten.toSet
  }

  def convertAidinkieliKielikoodi(kieliKoodi: String): String =
    AvainArvoConstants.aidinkieliKoodiMapping.getOrElse(kieliKoodi, kieliKoodi)

  def perusopetuksenPakollisetOppiaineetJaKieletToAvainArvot(personOid: String, aineet: Seq[PerusopetuksenOppiaine]): Set[AvainArvoContainer] = {
    val koodiArvot = aineet.map(_.koodi.arvo)
    AvainArvoConstants.oppiaineKoodiMapping.foreach { case (sourceKoodi, targetKoodi) =>
      if (koodiArvot.contains(sourceKoodi) && koodiArvot.contains(targetKoodi)) {
        LOG.error(s"Oppijalle $personOid: Oppiaineet sisältävät sekä koodin $sourceKoodi että $targetKoodi, jotka ovat ristiriidassa.")
      }
    }

    val remappedAineet = aineet.map { aine =>
      AvainArvoConstants.oppiaineKoodiMapping.get(aine.koodi.arvo) match {
        case Some(newKoodi) => aine.copy(koodi = aine.koodi.copy(arvo = newKoodi))
        case None => aine
      }
    }

    remappedAineet.flatMap(aine => {
      val arvosanaAvain = AvainArvoConstants.peruskouluAineenArvosanaPrefix + aine.koodi.arvo
      val arvosanaArvot: AvainArvoContainer = AvainArvoContainer(arvosanaAvain, aine.arvosana.arvo, Seq(AvainArvoConstants.arvosananLahdeSeliteSupa))

      val kieliOppiaineArvot: Option[AvainArvoContainer] = aine.kieli.map(aineenKieliKoodi => {
        val kieliAvain = arvosanaAvain + AvainArvoConstants.peruskouluAineenKieliOppiainePostfix
        val kieliArvo = if (aine.koodi.arvo == "AI") convertAidinkieliKielikoodi(aineenKieliKoodi.arvo) else aineenKieliKoodi.arvo
        AvainArvoContainer(kieliAvain, kieliArvo, Seq("Kielitieto löytyi Koskesta."))
      })

      val kieliTietoArvot: Option[AvainArvoContainer] = aine.kieli.map(aineenKieliKoodi => {
        val kieliTietoAvain = arvosanaAvain + AvainArvoConstants.peruskouluAineenKieliTietoPostfix
        AvainArvoContainer(kieliTietoAvain, aineenKieliKoodi.arvo, Seq("Kielitieto löytyi Koskesta."))
      })

      Set(arvosanaArvot) ++ kieliOppiaineArvot ++ kieliTietoArvot
    }).toSet
  }

  def hakemuksellaIlmoitettuPeruskoulu2017TaiAiempi(hakemus: AtaruValintalaskentaHakemus): Boolean = {
    hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusVuosiKey).flatMap(v => Option.apply(v)).map(_.toInt).exists(_ <= 2017)
  }

  //Muunnetaan aiemmin aineittain ryhmitellyt pohja-aineet + mahdolliset korotukset avain-arvoiksi
  def pakollisetJaKieletToContainers(
                                      aineetJaKorotukset: Seq[(PerusopetuksenOppiaine, Seq[PerusopetuksenOppiaine])],
                                      pohjaArvosanatOvatHakemukselta: Boolean = false
  ): Set[AvainArvoContainer] = {
    def teeOppiaineKoodiMuunnokset(aine: PerusopetuksenOppiaine): PerusopetuksenOppiaine =
      AvainArvoConstants.oppiaineKoodiMapping.get(aine.koodi.arvo)
        .map(newKoodi => aine.copy(koodi = aine.koodi.copy(arvo = newKoodi)))
        .getOrElse(aine)

    def parasArvosana(pohja: PerusopetuksenOppiaine, korotukset: Seq[PerusopetuksenOppiaine]): PerusopetuksenOppiaine =
      (pohja +: korotukset).maxBy(_.arvosana.arvo)(Ordering.fromLessThan((a, b) =>
        PerusopetuksenArvosanaOrdering.compareArvosana(a, b) < 0))

    //Ainoastaan pohja-arvosanat voivat tulla hakemukselta, kaikki mahdolliset korotukset tulevat Suorituspalvelusta.
    //Tästä seuraa, että arvoihin päätynyt arvosana on hakemukselta ainoastaan, jos pohjaArvosana == parasArvosana.
    def arvosanaSelite(parasArvosana: PerusopetuksenOppiaine, pohjaArvosana: PerusopetuksenOppiaine): String =
      if (parasArvosana.tunniste == pohjaArvosana.tunniste && pohjaArvosanatOvatHakemukselta) AvainArvoConstants.arvosananLahdeSeliteHakemus
      else AvainArvoConstants.arvosananLahdeSeliteSupa

    def kieliContainerit(avain: String, aine: PerusopetuksenOppiaine): Set[AvainArvoContainer] =
      aine.kieli.map { kieli =>
        val kieliArvo = if (aine.koodi.arvo == "AI") convertAidinkieliKielikoodi(kieli.arvo) else kieli.arvo

        val kielitietoLahdeSelite = if (pohjaArvosanatOvatHakemukselta) AvainArvoConstants.kielitiedonLahdeSeliteHakemus else AvainArvoConstants.kielitiedonLahdeSeliteSupa
        Set(
          AvainArvoContainer(avain + AvainArvoConstants.peruskouluAineenKieliOppiainePostfix, kieliArvo, Seq(kielitietoLahdeSelite)),
          AvainArvoContainer(avain + AvainArvoConstants.peruskouluAineenKieliTietoPostfix, kieli.arvo, Seq(kielitietoLahdeSelite))
        )
      }.getOrElse(Set.empty)

    val muunnetutAineetJaKorotukset = aineetJaKorotukset.map {
      case (aine, korotukset) => (teeOppiaineKoodiMuunnokset(aine), korotukset.map(teeOppiaineKoodiMuunnokset))
    }

    //A-kielet ja B-kielet numeroidaan ja päätyvät tämän perusteella eri avainten alle: A1, A12, A13 ja B1, B12, B13 jne.
    //Muille ei tehdä vastaavia numerointeja.
    val (numeroitavat, muut) = muunnetutAineetJaKorotukset.partition(t => AvainArvoConstants.kieltenNumerointiKoodit.contains(t._1.koodi.arvo))

    val numeroitujenContainerit: Set[AvainArvoContainer] =
      numeroitavat.groupBy(_._1.koodi.arvo).flatMap { case (koodiArvo, samaaKoodia) =>
        samaaKoodia.zipWithIndex.flatMap { case ((pohjaAine, korotukset), index) =>
          val numberedKoodi = if (index == 0) koodiArvo else koodiArvo + (index + 1).toString
          val avain = AvainArvoConstants.peruskouluAineenArvosanaPrefix + numberedKoodi
          val arvo = parasArvosana(pohjaAine, korotukset)
          Set(AvainArvoContainer(avain, arvo.arvosana.arvo, Seq(arvosanaSelite(arvo, pohjaAine)))) ++ kieliContainerit(avain, pohjaAine)
        }
      }.toSet

    val muutContainerit: Set[AvainArvoContainer] =
      muut.flatMap { case (aine, korotukset) =>
        val avain = AvainArvoConstants.peruskouluAineenArvosanaPrefix + aine.koodi.arvo
        val arvo = parasArvosana(aine, korotukset)
        Set(AvainArvoContainer(avain, arvo.arvosana.arvo, Seq(arvosanaSelite(arvo, aine)))) ++ kieliContainerit(avain, aine)
      }.toSet

    numeroitujenContainerit ++ muutContainerit
  }

  private def hakemuksenArvosanatSynteettisiksiOppiaineiksi(containers: Set[AvainArvoContainer]): Set[PerusopetuksenOppiaine] = {
    val kieliMap = containers
      .filter(_.avain.endsWith(AvainArvoConstants.peruskouluAineenKieliOppiainePostfix))
      .map(c => c.avain.stripSuffix(AvainArvoConstants.peruskouluAineenKieliOppiainePostfix) -> c.arvo)
      .toMap
    containers
      .filter(c => c.avain.startsWith(AvainArvoConstants.peruskouluAineenArvosanaPrefix)
        && !c.avain.endsWith(AvainArvoConstants.peruskouluAineenKieliOppiainePostfix)
        && !c.avain.endsWith(AvainArvoConstants.peruskouluAineenKieliTietoPostfix))
      .map { container =>
        val rawKoodi = container.avain.stripPrefix(AvainArvoConstants.peruskouluAineenArvosanaPrefix)
        val valIdx = rawKoodi.indexOf(AvainArvoConstants.peruskouluAineValinnainenPostfix)
        val (baseKoodi, pakollinen) = if (valIdx >= 0) {
          // Valinnainen aine: PK_KU_VAL1 → koodi="KU", pakollinen=false
          (rawKoodi.substring(0, valIdx), false)
        } else {
          // Pakollinen aine: PK_A12 → koodi="A1", PK_MA → koodi="MA", pakollinen=true
          val base = AvainArvoConstants.kieltenNumerointiKoodit
            .find(k => rawKoodi.startsWith(k) && rawKoodi.length > k.length)
            .orElse(AvainArvoConstants.kieltenNumerointiKoodit.find(_ == rawKoodi))
            .getOrElse(rawKoodi)
          (base, true)
        }
        val kieli = kieliMap.get(container.avain).map { k =>
          val reverseMappedKieli = if (baseKoodi == "AI") AvainArvoConstants.aidinkieliKoodiReverseMapping.getOrElse(k, k) else k
          Koodi(reverseMappedKieli, "kielivalikoima", None)
        }
        PerusopetuksenOppiaine(UUID.randomUUID(), Kielistetty(None, None, None),
          Koodi(baseKoodi, "koodisto", None), Koodi(container.arvo, "koodisto", None),
          kieli, pakollinen, None, None)
      }
  }

  private def convertParhaatPeruskoulunArvosanatJaKielet(personOid: String,
                                                         aineetPaasuoritukselta: Seq[PerusopetuksenOppiaine],
                                                         aineetOppiaineenOppimaarilta: Set[PerusopetuksenOppiaine], //Mahdolliset korotukset ovat aina Suorituspalvelusta
                                                         paasuoritusOnHakemukselta: Boolean = false): Set[AvainArvoContainer] = {

    //Selvitetään "pohjasuoritus" jokaiselle aineelle.
    val pakollisetJaKieletPohjasuoritukselta: Seq[PerusopetuksenOppiaine] = aineetPaasuoritukselta
      .filter(a => a.pakollinen || a.kieli.isDefined)
      .groupBy(a => (a.koodi.arvo, a.kieli.map(_.arvo)))
      .map { case (_, aineet) => aineet.maxBy(_.arvosana.arvo)(Ordering.fromLessThan((a, b) =>
        PerusopetuksenArvosanaOrdering.compareArvosana(a, b) < 0)) }
      .toSeq

    //Ryhmitellään pohjasuorituksen yhteyteen mahdolliset korotukset, joilla on kielten tapauksessa sama kieli ja kelpaava laajuus, ja muissa tapauksissa sama aine.
    val pakollinenToMahdollisetKorotukset: Seq[(PerusopetuksenOppiaine, Set[PerusopetuksenOppiaine])] = pakollisetJaKieletPohjasuoritukselta.map {
      //A-kielet
      case pohjaOppiaine: PerusopetuksenOppiaine if pohjaOppiaine.kieli.isDefined && AvainArvoConstants.aKielet.contains(pohjaOppiaine.koodi.arvo) =>
        pohjaOppiaine -> aineetOppiaineenOppimaarilta.filter(mahdollinenKorotus => {
          //Sama kieli kuin pohjaOppiaineella ja jokin A-kielen laajuus (esim. A2-kielellä voi korottaa samaa A1-kieltä ja A1-kielellä samaa A2-kieltä)
          mahdollinenKorotus.kieli.exists(k => k.arvo == pohjaOppiaine.kieli.get.arvo) && AvainArvoConstants.aKielet.contains(mahdollinenKorotus.koodi.arvo)
        })
      //B-kielet
      case pohjaOppiaine: PerusopetuksenOppiaine if pohjaOppiaine.kieli.isDefined && AvainArvoConstants.bKielet.contains(pohjaOppiaine.koodi.arvo) =>
        pohjaOppiaine -> aineetOppiaineenOppimaarilta.filter(mahdollinenKorotus => {
          //Sama kieli ja sama B-kielen laajuus kuin pohjaOppiaineella (vain sama B1-kieli voi korottaa sama B1-kieltä jne)
          mahdollinenKorotus.kieli.exists(korotuksenKieli => pohjaOppiaine.kieli.get.arvo.equals(korotuksenKieli.arvo)) && mahdollinenKorotus.koodi.arvo == pohjaOppiaine.koodi.arvo
        })
      //Äidinkielet
      case pohjaOppiaine: PerusopetuksenOppiaine if pohjaOppiaine.kieli.isDefined && pohjaOppiaine.koodi.arvo.equals("AI") =>
        pohjaOppiaine -> aineetOppiaineenOppimaarilta.filter(mahdollinenKorotus => {
          //Äidinkielellä oltava sama aine (AI) sekä sama kieli, jotta korotus huomioidaan
          mahdollinenKorotus.kieli.exists(korotuksenKieli => pohjaOppiaine.kieli.get.arvo.equals(korotuksenKieli.arvo)) && mahdollinenKorotus.koodi.arvo == pohjaOppiaine.koodi.arvo
        })

      //Kaikki muut aineet
      case pohjaOppiaine: PerusopetuksenOppiaine =>
        //Sama pohjaOppiaine
        pohjaOppiaine -> aineetOppiaineenOppimaarilta.filter(_.koodi.arvo == pohjaOppiaine.koodi.arvo)
    }

    val pakollisetAineetJaKorotukset =
      pakollisetJaKieletToContainers(
        pakollinenToMahdollisetKorotukset
          .sortBy { case (aine, _) => (aine.koodi.arvo, aine.kieli.map(_.arvo).getOrElse("")) }
          .map { case (aine, korotukset) => (aine, korotukset.toSeq) },
        paasuoritusOnHakemukselta
      )

    //Huomioidaan valinnaisiksi vain sellaiset oppiaineen oppimäärät, joiden aine löytyy varsinaisen oppimäärän aineista.
    val paasuorituksenAineet = aineetPaasuoritukselta.map(_.koodi.arvo)
    val valinnaisiksiHuomioitavatOppiaineenOppimaarat = aineetOppiaineenOppimaarilta.filter(a => paasuorituksenAineet.contains(a.koodi.arvo))
    val valinnaisetAineet = aineetPaasuoritukselta.filter(a => !a.pakollinen && a.kieli.isEmpty) ++ valinnaisiksiHuomioitavatOppiaineenOppimaarat.filter(a => !a.pakollinen && a.kieli.isEmpty)
    val valinnaisetSupasta = perusopetuksenValinnaisetOppiaineetToAvainArvot(valinnaisetAineet)

    val tuloksetIlmanEiNumeerisia = (pakollisetAineetJaKorotukset ++ valinnaisetSupasta).filter(aa => {
      //Päästetään parhaistakin arvosanoista läpi vain numeeriset arvosanat. Säilytetään lisäksi mukana olevat kielitiedot.
      aa.avain.endsWith(AvainArvoConstants.peruskouluAineenKieliOppiainePostfix) || aa.avain.endsWith(AvainArvoConstants.peruskouluAineenKieliTietoPostfix) || AvainArvoConstants.numeerisetPeruskoulunArvosanat.contains(aa.arvo)
    })

    tuloksetIlmanEiNumeerisia
  }

  def convertPeruskouluArvot(personOid: String, vahvistettuViimeistaan: LocalDate, hakemus: Option[AtaruValintalaskentaHakemus], opiskeluoikeudet: Seq[Opiskeluoikeus], ehdotOverrideAktiivinen: Boolean = false): Set[AvainArvoContainer] = {
    val perusopetuksenOppimaara: Option[PerusopetuksenOppimaara] = etsiViimeisinPeruskoulu(personOid, opiskeluoikeudet, false)
    val oppiaineenOppimaarat: Seq[PerusopetuksenOppimaaranOppiaineidenSuoritus] = etsiVahvistetutOppiaineenOppimaarat(opiskeluoikeudet)

    val arvot = (perusopetuksenOppimaara, hakemus) match {
      //Ehdot-override: hakijalla oli leikkurihetkellä ehdot (pakollisessa nelonen, oppimäärä vahvistamatta), 2 viikon ikkuna auki,
      //eikä nykyistä oppimäärää ollut vahvistettu ajoissa (ei joko lainkaan tai vasta leikkurin jälkeen). Arvosanat luetaan
      //nykyisistä opiskeluoikeuksista samoin kuin muillakin hakijoilla, jotta korotukset huomioidaan. Meta-avaimet johdetaan leikkuripäivästä.
      case (Some(po), _) if ehdotOverrideAktiivinen && !oppimaaraVahvistettuAjoissa(po, vahvistettuViimeistaan) =>
        val ehdotSelite = s"Hakijalla oli ehdot leikkuripäivänä $vahvistettuViimeistaan (pakollisessa aineessa nelonen, oppimäärää ei vahvistettu ajoissa), joten arvosanat huomioidaan vaikka oppimäärää ei ole vahvistettu ajoissa. Vahvistuspäivä: ${po.vahvistusPaivamaara.map(_.toString).getOrElse("-")}."
        val aineetPaasuoritukselta = po.aineet
        val aineetOppimaarilta = oppiaineenOppimaarat.flatMap(_.aineet).toSet
        val arvosanaArvotNykyiset = convertParhaatPeruskoulunArvosanatJaKielet(personOid, aineetPaasuoritukselta, aineetOppimaarilta)
        val arvosanaArvot = arvosanaArvotNykyiset.map(c => c.copy(selitteet = c.selitteet :+ ehdotSelite))

        val suoritusArvo = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritettuKey, "true", Seq(ehdotSelite))
        val suoritusVuosiArvo = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritusvuosiKey, vahvistettuViimeistaan.getYear.toString, Seq(ehdotSelite))
        val suoritusLukukausiArvo = AvainArvoContainer(AvainArvoConstants.pkSuorituslukukausiKey, AvainArvoConverterUtil.getLukukausi(vahvistettuViimeistaan), Seq(ehdotSelite))
        val suoritusKieliArvo = AvainArvoContainer(AvainArvoConstants.perusopetuksenKieliKey, po.suoritusKieli.arvo)

        arvosanaArvot ++ Some(suoritusVuosiArvo) ++ Some(suoritusArvo) ++ Some(suoritusLukukausiArvo) ++ Some(suoritusKieliArvo)

      case (Some(po), _) if oppimaaraVahvistettuAjoissa(po, vahvistettuViimeistaan) =>
        val vahvistettuAjoissaSelite = s"Löytyi perusopetuksen oppimäärä, joka on vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä. Vahvistuspäivä: ${po.vahvistusPaivamaara.getOrElse("-")}"
        val aineetPaasuoritukselta = perusopetuksenOppimaara.map(_.aineet).getOrElse(Seq.empty)
        val aineetOppimaarilta = oppiaineenOppimaarat.flatMap(_.aineet).toSet

        val arvosanaJaKieliArvot = convertParhaatPeruskoulunArvosanatJaKielet(personOid, aineetPaasuoritukselta, aineetOppimaarilta)

        val suoritusArvo = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritettuKey, "true", Seq(vahvistettuAjoissaSelite))
        val suoritusVuosiArvo = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritusvuosiKey, po.vahvistusPaivamaara.map(_.getYear).get.toString, Seq(vahvistettuAjoissaSelite))
        val paattotodistusVuosiArvo = AvainArvoContainer(AvainArvoConstants.peruskouluPaattotodistusvuosiKey, po.vahvistusPaivamaara.map(_.getYear).get.toString, Seq(vahvistettuAjoissaSelite))
        val suoritusLukukausiArvo = AvainArvoContainer(AvainArvoConstants.pkSuorituslukukausiKey, AvainArvoConverterUtil.getLukukausi(po.vahvistusPaivamaara.get), Seq(vahvistettuAjoissaSelite))
        val suoritusKieliArvo = AvainArvoContainer(AvainArvoConstants.perusopetuksenKieliKey, po.suoritusKieli.arvo)

        arvosanaJaKieliArvot ++ Some(suoritusVuosiArvo) ++ Some(suoritusArvo) ++ Some(suoritusLukukausiArvo) ++ Some(paattotodistusVuosiArvo) ++ Some(suoritusKieliArvo)

      case (Some(po), _) if po.vahvistusPaivamaara.isDefined =>
        val vahvistettuMyohassaSelite = s"Löytyi perusopetuksen oppimäärä, mutta sitä ei ole vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä. Vahvistuspäivä: ${perusopetuksenOppimaara.flatMap(_.vahvistusPaivamaara).getOrElse("-")}"
        val suoritusArvo: AvainArvoContainer = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritettuKey, "false", Seq(vahvistettuMyohassaSelite))
        Set(suoritusArvo)

      //Jos Supasta ei löydy perusopetuksen suoritusta, käytetään hakemuksen tietoja jos sieltä löytyy hakijan ilmoittama perusopetus vuodelta 2017 tai aiemmin.
      case (None, Some(hakemus)) if hakemuksellaIlmoitettuPeruskoulu2017TaiAiempi(hakemus) =>
        val arvosanatHakemukselta = HakemusConverter.convertArvosanatHakemukselta(hakemus)

        //Muunnetaan hakemuksen avain-arvot synteettisiksi PerusopetuksenOppiaineiksi, jotta voidaan hyödyntää
        //samaa korotuslogiikkaa (A/B-kielet, numerointi) kuin Supasta löytyville suorituksille-polussa.
        val aineetHakemukselta = hakemuksenArvosanatSynteettisiksiOppiaineiksi(arvosanatHakemukselta)
        //Suorituspalvelusta voi löytyä korotuksia hakemuksella ilmoitetuille arvosanoille (esim. perusopetus suoritettu 2017, korotuksia vuodelta 2018).
        val aineetOppimaarilta = oppiaineenOppimaarat.flatMap(_.aineet).toSet
        val arvosanaJaKieliArvot = convertParhaatPeruskoulunArvosanatJaKielet(
          personOid,
          aineetHakemukselta.toSeq,
          aineetOppimaarilta,
          paasuoritusOnHakemukselta = true
        )

        val suoritusKieliHakemukselta =
          hakemus.keyValues.get(AvainArvoConstants.perusopetuksenKieliKey).flatMap(v => Option.apply(v))
            .map(k => AvainArvoContainer(AvainArvoConstants.perusopetuksenKieliKey, k))
        val paattotodistusVuosiHakemukselta = hakemus.keyValues.get(AvainArvoConstants.ataruPohjakoulutusVuosiKey).flatMap(v => Option.apply(v))
          .map(k => AvainArvoContainer(AvainArvoConstants.peruskouluPaattotodistusvuosiKey, k, Seq(s"Päättötodistusvuosi $k poimittu hakemukselta.")))
        //Todo, halutaanko tässä tapauksessa asettaa myös avain-arvo peruskouluSuoritettuKey -> true? Onko tällä merkitystä?
        arvosanaJaKieliArvot ++ suoritusKieliHakemukselta ++ paattotodistusVuosiHakemukselta

      case _ =>
        val suoritusArvo: AvainArvoContainer = AvainArvoContainer(AvainArvoConstants.peruskouluSuoritettuKey, "false", Seq("Supasta tai hakemukselta ei löytynyt tietoa suoritetusta peruskoulusta."))
        Set(suoritusArvo)
    }

    arvot
  }
}
