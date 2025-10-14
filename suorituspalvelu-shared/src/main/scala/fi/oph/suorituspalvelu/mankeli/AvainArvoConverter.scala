package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto, ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, Laajuus, NuortenPerusopetuksenOppiaineenOppimaara, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, Suoritus, Telma, VapaaSivistystyo, YOOpiskeluoikeus}
import org.slf4j.LoggerFactory

import java.time.LocalDate
import scala.collection.immutable

//Lisätään filtteröityihin suorituksiin kaikki sellaiset suoritukset, joilta on poimittu avainArvoja.
// Eli jos jossain kohtaa pudotetaan pois suorituksia syystä tai toisesta, ne eivät ole mukana filtteröidyissä suorituksissa.
//Opiskeluoikeudet sisältävät kaiken lähdedatan.
case class ValintaData(personOid: String, avainArvot: Set[AvainArvoContainer], opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq.empty, filtteroidytSuoritukset: Seq[Suoritus]) {
  def getAvainArvoMap(): Map[String, String] = avainArvot.map(a => (a.avain, a.arvo)).toMap
}

//Sama tieto sijoitetaan useamman avaimen alle, koska eri valintaperusteissa saatetaan viitata eri avaimiin.
case class Avaimet(avain: String, rinnakkaisAvaimet: Set[String]) {
  //(Avain, isDuplikaatti)
  def kaikkiAvaimet: Set[(String, Boolean)] = {
    val pa: (String, Boolean) = (avain, false)
    val ra: Set[(String, Boolean)] = rinnakkaisAvaimet.map(avain => (avain, true))
    ra + pa
  }
}

case class AvainArvoContainerN(avain: String, arvo: String, selitteet: Seq[String] = Seq.empty) {

}
case class AvainArvoContainer(avain: String, arvo: String, duplikaatti: Boolean, selitteet: Seq[String] = Seq.empty)

object AvainArvoConstants {
  //Sama tieto tallennetaan sekä pääavaimen että rinnakkaisten avaimien alle. Tämä antaa tuen vanhoille valintaperusteille.
  //Pääavaimia ei kannata vaihtaa, koska mahdolliset yliajot on tehty niille. Uusia rinnakkaisia avaimia voi lisätä tarpeen mukaan.
  final val perusopetuksenKieliKeys = Avaimet("perusopetuksen_kieli", Set.empty)
  final val peruskouluPaattotodistusvuosiKeys = Avaimet("peruskoulu_paattotodistusvuosi", Set("PK_PAATTOTODISTUSVUOSI"))
  final val peruskouluSuoritusvuosiKeys = Avaimet("peruskoulu_suoritusvuosi", Set("PK_SUORITUSVUOSI"))
  final val peruskouluSuoritettuKeys = Avaimet("perustutkinto_suoritettu", Set("PK_TILA"))
  final val lukioSuoritettuKeys = Avaimet("lukio_suoritettu", Set("LK_TILA"))
  final val yoSuoritettuKeys = Avaimet("yo-tutkinto_suoritettu", Set("YO_TILA"))
  final val ammSuoritettuKeys = Avaimet("ammatillinen_suoritettu", Set("AM_TILA"))

  //Lisäpistekoulutukset
  final val telmaMinimiLaajuus: BigDecimal = 25
  final val tuvaMinimiLaajuus: BigDecimal = 19
  final val opistovuosiMinimiLaajuus: BigDecimal = 26.5

  final val telmaSuoritettuKeys = Avaimet("lisapistekoulutus_telma", Set("LISAKOULUTUS_TELMA"))
  final val telmaSuoritusvuosiKeys = Avaimet("lisapistekoulutus_telma_vuosi", Set("LISAPISTEKOULUTUS_TELMA_SUORITUSVUOSI"))

  final val opistovuosiSuoritettuKeys = Avaimet("lisapistekoulutus_opisto", Set("LISAKOULUTUS_OPISTO"))
  final val opistovuosiSuoritusvuosiKeys = Avaimet("lisapistekoulutus_opisto_vuosi", Set("LISAPISTEKOULUTUS_OPISTO_SUORITUSVUOSI"))

  //Todo, tarkistetaan vielä arvosanojen muoto uudessa mallissa
  final val peruskouluAineenArvosanaPrefixes = Avaimet("PERUSKOULU_ARVOSANA_", Set("PK_"))

  //Nämä tulevat aineen arvosanojen perään, eli esimerkiksi jos varsinainen arvosana
  // on avaimen "PK_B1" alla, tulee kieli avainten "PK_B1_OPPIAINE" ja "PK_B1_OPPIAINEEN_KIELI" alle
  final val peruskouluAineenKieliPostfixes = Avaimet("_OPPIAINEEN_KIELI", Set("_OPPIAINE"))
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

  def convertOpiskeluoikeudet(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): ValintaData = {

    val peruskouluArvot = convertPeruskouluArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val ammatillisetArvot = convertAmmatillisetArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val yoArvot = convertYoArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val lukioArvot = convertLukioArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan) //TODO, lukiosuoritukset pitää vielä parseroida
    val lisapistekoulutusArvot = convertLisapistekoulutukset(personOid, opiskeluoikeudet)

    val avainArvot = peruskouluArvot ++ ammatillisetArvot ++ yoArvot ++ lukioArvot ++ lisapistekoulutusArvot
    ValintaData(personOid, avainArvot, opiskeluoikeudet, Seq.empty)
  }

  def convertTelma(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): Set[AvainArvoContainer] = {
    val telmat = opiskeluoikeudet.collect {
      case o: AmmatillinenOpiskeluoikeus => o.suoritukset.collect { case s: Telma => s }
    }.flatten

    val riittavaLaajuus: Seq[Telma] = telmat.filter(t => t.hyvaksyttyLaajuus.exists(laajuus => laajuus.arvo >= AvainArvoConstants.telmaMinimiLaajuus))
    val tuoreinRiittava: Option[Telma] = riittavaLaajuus.maxByOption(_.suoritusVuosi)

    val suoritusSelite = (tuoreinRiittava, telmat) match {
      case (tuorein, _) if tuorein.isDefined =>
        Seq(s"Löytyneen Opistovuosi-suorituksen laajuus on ${tuoreinRiittava.flatMap(_.hyvaksyttyLaajuus.map(_.arvo))}.")
      case (_, telmat) if telmat.nonEmpty =>
        val korkeinLaajuus: Laajuus = telmat.flatMap(_.hyvaksyttyLaajuus).maxBy(_.arvo)
        Seq(s"Ei löytynyt tarpeeksi laajaa Opistovuosi-suoritusta. Korkein löytynyt laajuus: " +
          s"${korkeinLaajuus.arvo} ${korkeinLaajuus.nimi.flatMap(_.fi).getOrElse("")}.")
      case (_, telmat) =>
        Seq(s"Ei löytynyt lainkaan Opistovuosi-suoritusta.")
    }

    val suoritusArvot = AvainArvoConstants.telmaSuoritettuKeys.kaikkiAvaimet.map((avain, isDuplikaatti) => AvainArvoContainer(avain, tuoreinRiittava.isDefined.toString, isDuplikaatti, suoritusSelite))
    val suoritusVuosiArvot = if (tuoreinRiittava.isDefined) {
      AvainArvoConstants.telmaSuoritusvuosiKeys.kaikkiAvaimet.map((avain, isDuplikaatti) => AvainArvoContainer(avain, tuoreinRiittava.get.suoritusVuosi.toString, isDuplikaatti))
    } else Set.empty

    suoritusArvot ++ suoritusVuosiArvot
  }

  def convertOpistovuosi(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]) = {
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

    val suoritusArvot = AvainArvoConstants.opistovuosiSuoritettuKeys.kaikkiAvaimet.map((avain, isDuplikaatti) => AvainArvoContainer(avain, tuoreinRiittava.isDefined.toString, isDuplikaatti, suoritusSelite))
    val suoritusVuosiArvot = if (tuoreinRiittava.isDefined) {
      AvainArvoConstants.opistovuosiSuoritusvuosiKeys.kaikkiAvaimet.map((avain, isDuplikaatti) => AvainArvoContainer(avain, tuoreinRiittava.get.suoritusVuosi.toString, isDuplikaatti))
    } else Set.empty

    suoritusArvot ++ suoritusVuosiArvot
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

    val arvot = AvainArvoConstants.ammSuoritettuKeys.kaikkiAvaimet.map((avain, isDuplikaatti) => AvainArvoContainer(avain, validSuoritukset.nonEmpty.toString, isDuplikaatti, Seq(ammSelite)))
    LOG.info(s"Ammatilliset arvot käsitelty henkilölle $personOid. $arvot")
    arvot
  }

  def convertYoArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    val yoOpiskeluoikeudet: Seq[(YOOpiskeluoikeus, Option[LocalDate])] = opiskeluoikeudet.collect { case o: YOOpiskeluoikeus => (o, o.yoTutkinto.valmistumisPaiva) }

    val hasYoSuoritus = yoOpiskeluoikeudet.exists(_._2.exists(v => v.isBefore(vahvistettuViimeistaan) || v.equals(vahvistettuViimeistaan)))

    val paivat = yoOpiskeluoikeudet.flatMap(_._2).distinct
    val valmistumispaivaSelite = if (paivat.nonEmpty) s" Valmistumispaivat: ${paivat.mkString(", ")}" else ""
    val yoSelite = s"Löytyi yhteensä ${yoOpiskeluoikeudet.size} YO-opiskeluoikeutta." + valmistumispaivaSelite

    val arvot = AvainArvoConstants.yoSuoritettuKeys.kaikkiAvaimet.map((avain, isDuplikaatti) => AvainArvoContainer(avain, hasYoSuoritus.toString, isDuplikaatti, Seq(yoSelite)))
    LOG.info(s"Yo-arvot käsitelty henkilölle $personOid. $arvot")
    arvot
  }

  //TODO lukiosuorituksia ei ole vielä parseroitu eikä niitä saada Koskesta massaluovutusrajapinnan kautta. Tämä päättely ei siis vielä toimi.
  def convertLukioArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    //val lukioOpiskeluoikeudet = opiskeluoikeudet.collect { case o: GeneerinenOpiskeluoikeus => o }
    val hasLukioSuoritus = false
    val lukioSelite = s"Lukiosuorituksia ei vielä saada Koskesta massaluovutusrajapinnan kautta."
    val arvot = AvainArvoConstants.lukioSuoritettuKeys.kaikkiAvaimet.map((avain, isDuplikaatti) => AvainArvoContainer(avain, hasLukioSuoritus.toString, isDuplikaatti, Seq(lukioSelite)))
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

    val avainArvot: Set[AvainArvoContainer] = aineidenKorkeimmatArvosanat.values.flatMap(aine =>
      val arvosanaArvot: Set[AvainArvoContainer] = AvainArvoConstants.peruskouluAineenArvosanaPrefixes.kaikkiAvaimet.map((prefix, isDuplikaatti) => AvainArvoContainer(prefix + aine.koodi.arvo, aine.arvosana.arvo, isDuplikaatti, Seq.empty))
      val kieliArvot: Set[AvainArvoContainer] = aine.kieli.map(k => AvainArvoConstants.peruskouluAineenKieliPostfixes.kaikkiAvaimet.map((postfix, isDuplikaatti) => {
        arvosanaArvot.map(arvosanaArvo => AvainArvoContainer(arvosanaArvo._1 + postfix, k.arvo, isDuplikaatti, Seq.empty))
      })).map(_.flatten).getOrElse(Set.empty)
      arvosanaArvot ++ kieliArvot
    ).toSet
    avainArvot
  }

  def convertPeruskouluArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): Set[AvainArvoContainer] = {
    val (perusopetuksenOppimaara: Option[PerusopetuksenOppimaara], oppiaineenOppimaarat: Seq[NuortenPerusopetuksenOppiaineenOppimaara]) = filterForPeruskoulu(personOid, opiskeluoikeudet)

    val oppimaaraOnVahvistettu: Boolean = perusopetuksenOppimaara.exists(_.vahvistusPaivamaara.isDefined)
    val vahvistusPvm = perusopetuksenOppimaara.map(_.vahvistusPaivamaara)
    val vahvistettuAjoissa: Boolean = perusopetuksenOppimaara.flatMap(_.vahvistusPaivamaara).exists(v => v.isBefore(vahvistettuViimeistaan) || v.equals(vahvistettuViimeistaan))

    val kieliArvot: Set[AvainArvoContainer] =
      perusopetuksenOppimaara
        .map(_.suoritusKieli.arvo)
        .map(kieli =>
          AvainArvoConstants
            .perusopetuksenKieliKeys.kaikkiAvaimet
            .map((avain, isDuplikaatti) => AvainArvoContainer(avain, kieli, isDuplikaatti, Seq.empty)))
        .getOrElse(Set.empty)

    val arvot = if (oppimaaraOnVahvistettu) {
      if (vahvistettuAjoissa) {
        val vahvistettuAjoissaSelite = s"Löytyi perusopetuksen oppimäärä, joka on vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä. Vahvistuspäivä: ${vahvistusPvm.map(_.toString).get}"
        val arvosanaArvot: Set[AvainArvoContainer] = korkeimmatPerusopetuksenArvosanatAineittain(perusopetuksenOppimaara, Seq.empty)
        val suoritusVuosiArvot: Set[AvainArvoContainer] =
          perusopetuksenOppimaara
            .flatMap(vo => vo.vahvistusPaivamaara.map(_.getYear))
            .map(year => AvainArvoConstants.peruskouluSuoritusvuosiKeys.kaikkiAvaimet
              .map((avain, isDuplikaatti) => AvainArvoContainer(avain, year.toString, isDuplikaatti, Seq(vahvistettuAjoissaSelite))))
            .getOrElse(Set.empty)
        val suoritusArvot: Set[AvainArvoContainer] = AvainArvoConstants.peruskouluSuoritettuKeys.kaikkiAvaimet.map((avain, isDuplikaatti) => AvainArvoContainer(avain, vahvistettuAjoissa.toString, isDuplikaatti, Seq(vahvistettuAjoissaSelite)))
        arvosanaArvot ++ kieliArvot ++ suoritusVuosiArvot ++ suoritusArvot
      } else {
        val vahvistettuMyohassaSelite = s"Löytyi perusopetuksen oppimäärä, mutta sitä ei ole vahvistettu leikkuripäivään $vahvistettuViimeistaan mennessä. Vahvistuspäivä: ${perusopetuksenOppimaara.map(_.vahvistusPaivamaara)}"
        val suoritusArvot: Set[AvainArvoContainer] = AvainArvoConstants.peruskouluSuoritettuKeys.kaikkiAvaimet.map((avain, isDuplikaatti) => AvainArvoContainer(avain, vahvistettuAjoissa.toString, isDuplikaatti, Seq(vahvistettuMyohassaSelite)))
        suoritusArvot ++ kieliArvot
      }
    } else {
      kieliArvot
    }
    arvot
  }
}
