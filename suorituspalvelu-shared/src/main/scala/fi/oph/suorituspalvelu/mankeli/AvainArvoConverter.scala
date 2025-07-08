package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business
import fi.oph.suorituspalvelu.business.{NuortenPerusopetuksenOppiaineenOppimaara, Opiskeluoikeus, PerusopetuksenOpiskeluoikeus, PerusopetuksenOppimaara, Tietolahde}
import org.slf4j.LoggerFactory

case class ValintaData(personOid: String, hakemusOid: Option[String], keyValues: Map[String, String])

object AvainArvoConstants {
  //Sama tieto tallennetaan kahden avaimen alle: vanhan Valintalaskentakoostepalvelusta periytyvän,
  // johon kenties viitataan nykyisistä valintaperusteista, ja lisäksi uuden selkeämmän avaimen, jota käytetään jatkossa uusissa valintaperusteissa.
  // Vanhat avaimet voi kenties jossain kohtaa pudottaa pois.
  final val perusopetuksenKieliKeys = Set("perusopetuksen_kieli")
  final val peruskouluPaattotodistusvuosiKeys = Set("PK_PAATTOTODISTUSVUOSI", "PERUSKOULU_PAATTOTODISTUSVUOSI")
  final val peruskouluSuoritusvuosiKeys = Set("PK_SUORITUSVUOSI", "PERUSKOULU_SUORITUSVUOSI")
  final val peruskouluSuoritettuKeys = Set("PK_TILA", "PERUSKOULU_SUORITETTU")

  final val peruskouluAineenArvosanaPrefixes = Set("PK_", "PERUSKOULU_ARVOSANA_")
}

object AvainArvoConverter {

  val LOG = LoggerFactory.getLogger(getClass)

  //Ottaa huomioon ensi vaiheessa PerusopetuksenOppimaarat ja myöhemmin myös PerusopetuksenOppiaineenOppimaarien sisältämät arvosanat JOS löytyy suoritettu PerusopetuksenOppimaara
  def korkeimmatPerusopetuksenArvosanatAineittain(perusopetuksenOppimaara: Option[PerusopetuksenOppimaara], oppiaineenOppimaarat: Seq[NuortenPerusopetuksenOppiaineenOppimaara]): Set[(String, String)] = {
    val oppimaaranArvosanat: Set[(String, String)] = perusopetuksenOppimaara.map(s => {
      if (s.vahvistusPaivamaara.isDefined) {
        s.aineet.flatMap(aine => AvainArvoConstants.peruskouluAineenArvosanaPrefixes.map(prefix => (prefix + aine.koodi.arvo, aine.arvosana.arvo)))
      } else Set.empty
    }).getOrElse(Set.empty)
    oppimaaranArvosanat
  }


  def convertPeruskouluArvot(personOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus]): ValintaData = {
    val perusopetukset: Seq[PerusopetuksenOpiskeluoikeus] = opiskeluoikeudet.collect { case po: PerusopetuksenOpiskeluoikeus => po }
    val (vahvistetut, eiVahvistetut) = perusopetukset.flatMap(po => po.suoritukset.find(_.isInstanceOf[PerusopetuksenOppimaara]).map(_.asInstanceOf[PerusopetuksenOppimaara])).partition(o => o.vahvistusPaivamaara.isDefined)

    //Todo, tässä voi harkita myös virheen heittämistä. Ei liene järkevä tilanne, että vahvistettuja perusopetuksen oppimääriä olisi koskaan useita.
    if (vahvistetut.size > 1) {
      LOG.warn(s"Oppijalle $personOid enemmän kuin yksi vahvistettu perusopetuksen oppimäärä!")
    }

    val valmisOppimaara: Option[PerusopetuksenOppimaara] = vahvistetut.headOption
    val keskenOppimaara: Option[PerusopetuksenOppimaara] = eiVahvistetut.headOption //Todo, tämän käsittelyyn tarvitaan jotain päivämäärätietoa suorituksille, jotta voidaan poimia tuorein. Periaatteessa näitä voisi olla useita.

    val useOppimaara = valmisOppimaara.orElse(keskenOppimaara)

    val kieliArvot: Set[(String, String)] = useOppimaara.map(_.suoritusKieli.arvo).map(kieli => AvainArvoConstants.perusopetuksenKieliKeys.map(key => (key, kieli))).getOrElse(Set.empty)

    val arvosanat: Set[(String, String)] = korkeimmatPerusopetuksenArvosanatAineittain(valmisOppimaara, Seq.empty)

    //Nämä poimitaan vain valmiilta suorituksilta
    val suoritusvuosi: Set[(String, String)] = valmisOppimaara.flatMap(vo => vo.vahvistusPaivamaara.map(_.getYear)).map(year => AvainArvoConstants.peruskouluSuoritusvuosiKeys.map(key => (key, year.toString))).getOrElse(Set.empty)
    val suoritettu: Set[(String, String)] = AvainArvoConstants.peruskouluSuoritettuKeys.map(key => (key, valmisOppimaara.isDefined.toString))

    val combined = (arvosanat ++ kieliArvot ++ suoritusvuosi ++ suoritettu).toMap
    ValintaData(personOid, None, combined)
  }

}
