package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmattiTutkinto,
  ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, NuortenPerusopetuksenOppiaineenOppimaara, Opiskeluoikeus,
  PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, Suoritus, YOOpiskeluoikeus}
import org.slf4j.LoggerFactory

import java.time.LocalDate
import scala.collection.immutable

//Lisätään filtteröityihin suorituksiin kaikki sellaiset suoritukset, joilta on poimittu avainArvoja. Eli jos jossain kohtaa pudotetaan pois suorituksia syystä tai toisesta, ne eivät ole mukana filtteröidyissä suorituksissa.
//Opiskeluoikeudet sisältävät kaiken lähdedatan.
case class ValintaData(personOid: String, avainArvot: Map[String, String], selitteet: Map[String, String], opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq.empty, filtteroidytSuoritukset: Seq[Suoritus])

object AvainArvoConstants {
  //Sama tieto tallennetaan kahden avaimen alle: vanhan Valintalaskentakoostepalvelusta periytyvän,
  // johon kenties viitataan nykyisistä valintaperusteista, ja lisäksi uuden selkeämmän avaimen, jota käytetään jatkossa uusissa valintaperusteissa.
  // Vanhat avaimet voi toivottavasti jossain kohtaa pudottaa pois.
  final val perusopetuksenKieliKeys = Set("perusopetuksen_kieli")
  final val peruskouluPaattotodistusvuosiKeys = Set("PK_PAATTOTODISTUSVUOSI", "PERUSKOULU_PAATTOTODISTUSVUOSI")
  final val peruskouluSuoritusvuosiKeys = Set("PK_SUORITUSVUOSI", "PERUSKOULU_SUORITUSVUOSI")
  final val peruskouluSuoritettuKeys = Set("PK_TILA", "PERUSKOULU_SUORITETTU")
  final val lukioSuoritettuKeys = Set("LK_TILA", "lukio_suoritettu")
  final val yoSuoritettuKeys = Set("YO_TILA", "yo-tutkinto_suoritettu")
  final val ammSuoritettuKeys = Set("AM_TILA", "ammatillinen_suoritettu")

  final val peruskouluAineenArvosanaPrefixes = Set("PK_", "PERUSKOULU_ARVOSANA_")

  //Nämä tulevat aineen arvosanojen perään, eli esimerkiksi jos varsinainen arvosana
  // on avaimen "PK_B1" alla, tulee kieli avainten "PK_B1_OPPIAINE" ja "PK_B1_OPPIAINEEN_KIELI" alle
  final val peruskouluAineenKieliPostfixes = Set("_OPPIAINE", "_OPPIAINEEN_KIELI")
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

object AvainArvoConverter {

  val LOG = LoggerFactory.getLogger(getClass)

  def convertOpiskeluoikeudet(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): ValintaData = {

    val peruskouluSuoritus: Option[PerusopetuksenOppimaara] = filterForPeruskoulu(personOid, opiskeluoikeudet)
    val peruskouluArvot: Map[String, String] = convertPeruskouluArvot(personOid, peruskouluSuoritus, Seq.empty)
    val (ammatillisetArvot, ammatillisetSelitteet) = convertAmmatillisetArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val (lukioArvot, lukioSelitteet) = convertLukioArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val (yoArvot, yoSelitteet) = convertYoArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)

    val avainArvot = peruskouluArvot ++ ammatillisetArvot ++ yoArvot ++ lukioArvot
    val selitteet = ammatillisetSelitteet ++ lukioSelitteet ++ yoSelitteet
    ValintaData(personOid, avainArvot, selitteet, opiskeluoikeudet, Seq.empty ++ peruskouluSuoritus)
  }

  def convertAmmatillisetArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): (Map[String, String], Map[String, String]) = {
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

    val arvot = AvainArvoConstants.ammSuoritettuKeys.map(key => (key, validSuoritukset.nonEmpty.toString)).toMap
    val selitteet = AvainArvoConstants.ammSuoritettuKeys.map(key => (key, ammSelite)).toMap
    LOG.info(s"Ammatilliset arvot käsitelty henkilölle $personOid. $ammSelite")
    (arvot, selitteet)
  }

  def convertYoArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate) = {
    val yoOpiskeluoikeudet: Seq[(YOOpiskeluoikeus, Option[LocalDate])] = opiskeluoikeudet.collect { case o: YOOpiskeluoikeus => (o, o.yoTutkinto.valmistumisPaiva) }

    val hasYoSuoritus = yoOpiskeluoikeudet.exists(_._2.exists(v => v.isBefore(vahvistettuViimeistaan) || v.equals(vahvistettuViimeistaan)))
    val yoSelite = s"Löytyi yhteensä ${yoOpiskeluoikeudet.size} YO-opiskeluoikeutta. Valmistumispäivät: ${yoOpiskeluoikeudet.flatMap(_._2).distinct.mkString(", ")}."

    val arvot = AvainArvoConstants.yoSuoritettuKeys.map(key => (key, hasYoSuoritus.toString)).toMap
    val selitteet = AvainArvoConstants.yoSuoritettuKeys.map(key => (key, yoSelite)).toMap
    LOG.info(s"Yo-arvot käsitelty henkilölle $personOid. $yoSelite")
    (arvot, selitteet)
  }

  //TODO lukiosuorituksia ei ole vielä parseroitu eikä niitä saada Koskesta massaluovutusrajapinnan kautta. Tämä päättely ei siis vielä toimi.
  def convertLukioArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): (Map[String, String], Map[String, String]) = {
    //val lukioOpiskeluoikeudet = opiskeluoikeudet.collect { case o: GeneerinenOpiskeluoikeus => o }
    val hasLukioSuoritus = false
    val lukioSelite = s"Lukiosuorituksia ei vielä saada Koskesta massaluovutusrajapinnan kautta."
    val arvot = AvainArvoConstants.lukioSuoritettuKeys.map(key => (key, hasLukioSuoritus.toString)).toMap
    val selitteet = AvainArvoConstants.lukioSuoritettuKeys.map(key => (key, lukioSelite)).toMap
    LOG.info(s"Lukioarvot käsitelty henkilölle $personOid. $lukioSelite")
    (arvot, selitteet)
  }

  def convertTutkintojenSuoritusArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: LocalDate): (Map[String, String], Map[String, String]) = {
    val (ammatillisetArvot, ammatillisetSelitteet) = convertAmmatillisetArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val (lukioArvot, lukioSelitteet) = convertLukioArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)
    val (yoArvot, yoSelitteet) = convertYoArvot(personOid, opiskeluoikeudet, vahvistettuViimeistaan)

    //Todo, nämä pitää vielä parseroida Koski-datasta
    val hasLukioSuoritus = false

    val arvot = lukioArvot ++ yoArvot ++ ammatillisetArvot

    val selitteet = ammatillisetSelitteet ++ lukioSelitteet ++ yoSelitteet

    (arvot, selitteet)
  }


  def filterForPeruskoulu(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): Option[PerusopetuksenOppimaara] = {
    val perusopetuksenOpiskeluoikeudet: Seq[PerusopetuksenOpiskeluoikeus] = opiskeluoikeudet.collect { case po: PerusopetuksenOpiskeluoikeus => po }
    val (vahvistetut, eiVahvistetut) = perusopetuksenOpiskeluoikeudet.flatMap(po => po.suoritukset.find(_.isInstanceOf[PerusopetuksenOppimaara]).map(_.asInstanceOf[PerusopetuksenOppimaara])).partition(o => o.vahvistusPaivamaara.isDefined)

    if (vahvistetut.size > 1) {
      LOG.error(s"Oppijalle $personOid enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
      throw new RuntimeException(s"Oppijalle $personOid löytyy enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
    }

    //Todo, lisätään mahdollisesti ehto, että suorituksen vahvistuspäivämäärän on oltava ennen haun leikkuripäivämäärää
    val valmisOppimaara: Option[PerusopetuksenOppimaara] = vahvistetut.headOption
    //Todo, tämän käsittelyyn tarvitaan jotain päivämäärätietoa suorituksille, jotta voidaan poimia tuorein. Periaatteessa ei-vahvistettuja voisi olla useita.
    // Toisaalta voi olla että riittää tieto siitä, että jotain keskeneräistä löytyy. Halutaan ehkä filtteröidä selkeästi keskeytyneet (eronnut, erotettu jne) pois.
    val keskenOppimaara: Option[PerusopetuksenOppimaara] = eiVahvistetut.headOption

    val useOppimaara = valmisOppimaara.orElse(keskenOppimaara)
    useOppimaara
  }


  //Ottaa huomioon ensi vaiheessa PerusopetuksenOppimaarat ja myöhemmin myös PerusopetuksenOppiaineenOppimaarien sisältämät arvosanat JOS löytyy suoritettu PerusopetuksenOppimaara
  def korkeimmatPerusopetuksenArvosanatAineittain(perusopetuksenOppimaara: Option[PerusopetuksenOppimaara], oppiaineenOppimaarat: Seq[NuortenPerusopetuksenOppiaineenOppimaara]): Set[(String, String)] = {
    val aineet: Set[PerusopetuksenOppiaine] = perusopetuksenOppimaara.map(om => om.aineet).getOrElse(Set.empty)
    val byAineKey: Map[String, Set[PerusopetuksenOppiaine]] = aineet.groupBy(_.koodi.arvo)

    val aineidenKorkeimmatArvosanat = byAineKey.map((key, arvosanat) => {
      val highestGrade = arvosanat.maxBy(_.arvosana)(Ordering.fromLessThan((a, b) =>
        PerusopetuksenArvosanaOrdering.compareArvosana(a.arvo, b.arvo) < 0))
      (key, highestGrade)
    })

    val avainArvot: Set[(String, String)] = aineidenKorkeimmatArvosanat.values.flatMap(aine =>
      val arvosanaArvot: Set[(String, String)] = AvainArvoConstants.peruskouluAineenArvosanaPrefixes.map(prefix => (prefix + aine.koodi.arvo, aine.arvosana.arvo))
      val kieliArvot: Set[(String, String)] = aine.kieli.map(k => AvainArvoConstants.peruskouluAineenKieliPostfixes.map(postfix => {
        arvosanaArvot.map(arvosanaArvo => (arvosanaArvo._1 + postfix, k.arvo))
      })).map(_.flatten).getOrElse(Set.empty)
      arvosanaArvot ++ kieliArvot
    ).toSet
    avainArvot
  }

  def convertPeruskouluArvot(personOid: String, perusopetuksenOppimaara: Option[PerusopetuksenOppimaara], oppiaineenOppimaarat: Seq[NuortenPerusopetuksenOppiaineenOppimaara]): Map[String, String] = {
    val oppimaaraOnVahvistettu: Boolean = perusopetuksenOppimaara.exists(_.vahvistusPaivamaara.isDefined)

    val kieliArvot: Set[(String, String)] = perusopetuksenOppimaara.map(_.suoritusKieli.arvo).map(kieli => AvainArvoConstants.perusopetuksenKieliKeys.map(key => (key, kieli))).getOrElse(Set.empty)

    if (oppimaaraOnVahvistettu) {
      val arvosanaArvot: Set[(String, String)] = korkeimmatPerusopetuksenArvosanatAineittain(perusopetuksenOppimaara, Seq.empty)
      val suoritusVuosiArvot: Set[(String, String)] = perusopetuksenOppimaara.flatMap(vo => vo.vahvistusPaivamaara.map(_.getYear)).map(year => AvainArvoConstants.peruskouluSuoritusvuosiKeys.map(key => (key, year.toString))).getOrElse(Set.empty)
      val suoritusArvot: Set[(String, String)] = AvainArvoConstants.peruskouluSuoritettuKeys.map(key => (key, oppimaaraOnVahvistettu.toString))
      (arvosanaArvot ++ kieliArvot ++ suoritusVuosiArvot ++ suoritusArvot).toMap
    } else {
      kieliArvot.toMap
    }
  }
}
