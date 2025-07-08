package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{NuortenPerusopetuksenOppiaineenOppimaara, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara, Suoritus}
import org.slf4j.LoggerFactory

import scala.collection.immutable

//Lisätään filtteröityihin suorituksiin kaikki sellaiset suoritukset, joilta on poimittu avainArvoja. Eli jos jossain kohtaa pudotetaan pois suorituksia syystä tai toisesta, ne eivät ole mukana filtteröidyissä suorituksissa.
//Opiskeluoikeudet sisältävät kaiken lähdedatan.
case class ValintaData(personOid: String, hakemusOid: Option[String], keyValues: Map[String, String], opiskeluoikeudet: Seq[Opiskeluoikeus] = Seq.empty, filtteroidytSuoritukset: Seq[Suoritus])

object AvainArvoConstants {
  //Sama tieto tallennetaan kahden avaimen alle: vanhan Valintalaskentakoostepalvelusta periytyvän,
  // johon kenties viitataan nykyisistä valintaperusteista, ja lisäksi uuden selkeämmän avaimen, jota käytetään jatkossa uusissa valintaperusteissa.
  // Vanhat avaimet voi kenties jossain kohtaa pudottaa pois.
  final val perusopetuksenKieliKeys = Set("perusopetuksen_kieli")
  final val peruskouluPaattotodistusvuosiKeys = Set("PK_PAATTOTODISTUSVUOSI", "PERUSKOULU_PAATTOTODISTUSVUOSI")
  final val peruskouluSuoritusvuosiKeys = Set("PK_SUORITUSVUOSI", "PERUSKOULU_SUORITUSVUOSI")
  final val peruskouluSuoritettuKeys = Set("PK_TILA", "PERUSKOULU_SUORITETTU")

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

  def convertPeruskouluArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): ValintaData = {
    val perusopetukset: Seq[PerusopetuksenOpiskeluoikeus] = opiskeluoikeudet.collect { case po: PerusopetuksenOpiskeluoikeus => po }
    val (vahvistetut, eiVahvistetut) = perusopetukset.flatMap(po => po.suoritukset.find(_.isInstanceOf[PerusopetuksenOppimaara]).map(_.asInstanceOf[PerusopetuksenOppimaara])).partition(o => o.vahvistusPaivamaara.isDefined)

    if (vahvistetut.size > 1) {
      LOG.error(s"Oppijalle $personOid enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
      throw new RuntimeException(s"Oppijalle $personOid löytyy enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
    }

    val valmisOppimaara: Option[PerusopetuksenOppimaara] = vahvistetut.headOption
    //Todo, tämän käsittelyyn tarvitaan jotain päivämäärätietoa suorituksille, jotta voidaan poimia tuorein. Periaatteessa ei-vahvistettuja voisi olla useita.
    // Toisaalta voi olla että riittää tieto siitä, että jotain keskeneräistä löytyy. Halutaan ehkä filtteröidä selkeästi keskeytyneet pois.
    val keskenOppimaara: Option[PerusopetuksenOppimaara] = eiVahvistetut.headOption

    val useOppimaara = valmisOppimaara.orElse(keskenOppimaara)

    val kieliArvot: Set[(String, String)] = useOppimaara.map(_.suoritusKieli.arvo).map(kieli => AvainArvoConstants.perusopetuksenKieliKeys.map(key => (key, kieli))).getOrElse(Set.empty)

    val arvosanat: Set[(String, String)] = korkeimmatPerusopetuksenArvosanatAineittain(valmisOppimaara, Seq.empty)

    //Nämä poimitaan vain valmiilta suorituksilta
    val suoritusvuosi: Set[(String, String)] = valmisOppimaara.flatMap(vo => vo.vahvistusPaivamaara.map(_.getYear)).map(year => AvainArvoConstants.peruskouluSuoritusvuosiKeys.map(key => (key, year.toString))).getOrElse(Set.empty)
    val suoritettu: Set[(String, String)] = AvainArvoConstants.peruskouluSuoritettuKeys.map(key => (key, valmisOppimaara.isDefined.toString))

    val combined = (arvosanat ++ kieliArvot ++ suoritusvuosi ++ suoritettu).toMap
    ValintaData(personOid, None, combined, opiskeluoikeudet, Seq.empty ++ useOppimaara)
  }
}
