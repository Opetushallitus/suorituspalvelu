package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business.{Koe, Opiskeluoikeus, YOOpiskeluoikeus, YOTutkinto}

import java.time.LocalDate

case class AvainMetatiedotDTO(avain: String, metatiedot: List[Map[String, String]])

object YoMetadataConverter {
/*  val KOOSTETUT = Array(
    "AINEREAALI" -> "Ainereaali (UE, UO, ET, FF, PS, HI, FY, KE, BI, GE, TE, YH)",
    "REAALI" -> "Reaali (RR, RO, RY)",
    "PITKA_KIELI" -> "Kieli, pitkä oppimäärä (EA, FA, GA, HA, PA, SA, TA, VA, S9)",
    "KESKIPITKA_KIELI" -> "Kieli, keskipitkä oppimäärä (EB, FB, GB, HB, PB, SB, TB, VB)",
    "LYHYT_KIELI" -> "Kieli, lyhyt oppimäärä (EC, FC, GC, L1, PC, SC, TC, VC, KC, L7)",
    "AIDINKIELI" -> "Äidinkieli (O, A, I, W, Z, O5, A5)"
  )*/

  val aidinkieliAineet = Set("A", "O", "I", "W", "Z", "O5", "A5")
  val ainereaaliAineet = Set("UE", "UO", "ET", "PS", "HI", "FY", "KE", "BI", "GE", "TE", "YH", "FF")
  val reaaliAIneet = Set("RR", "RO", "RY")

  val pitkaKieliAineet = Set("EA", "FA", "GA", "HA", "PA", "SA", "TA", "VA", "S9")
  val keskipitkaKieliAineet = Set("EB", "FB", "GB", "HB", "PB", "SB", "TB", "VB")
  val lyhytKieliAineet = Set("EC", "FC", "GC", "L1", "PC", "SC", "TC", "VC", "KC", "L7")

  val aidinkieliLisatietoMap = Map(
    "A" -> "FI",
    "I" -> "IS",
    "W" -> "QS",
    "Z" -> "ZA",
    "O" -> "RU",
    "O5" -> "O5", //Fixme?
    "A5" -> "A5" //Fixme?
  )

  private def getTutkintokerta(suorituspaiva: LocalDate) = {
    suorituspaiva.getMonthValue match {
      case m if m > 0 && m <= 6 => "1"
      case m if m > 6 && m <= 12 => "2"
    }
  }

  def koeToMap(koe: Koe, lisatieto: Option[String] = None): Map[String, String] = {
    val baseMap = Map(
      "ARVO" -> koe.arvosana.arvo,
      "PISTEET" -> koe.pisteet.get.toString,
      "SUORITUSVUOSI" -> koe.tutkintoKerta.getYear.toString,
      "SUORITUSLUKUKAUSI" -> getTutkintokerta(koe.tutkintoKerta)
    )
    if (lisatieto.isDefined) {
      baseMap + ("LISATIETO" -> lisatieto.get)
    } else {
      baseMap
    }
  }

  private def getReaaliRyhma(yo: YOTutkinto): Map[String, List[Map[String, String]]] = {
    val aineet: Set[Koe] =
      yo.aineet
        .filter(a => reaaliAIneet.contains(a.koodi.arvo))
        .filter(_.pisteet.isDefined)

    val resultMaps = aineet.map(aine => {
      koeToMap(aine, Some(aine.koodi.arvo))
    })
    Map(
      "REAALI" -> resultMaps.toList
    )
  }

  private def getAinereaaliRyhma(yo: YOTutkinto): Map[String, List[Map[String, String]]] = {
    val aineet: Set[Koe] =
      yo.aineet
        .filter(a => ainereaaliAineet.contains(a.koodi.arvo))
        .filter(_.pisteet.isDefined)

    val resultMaps = aineet.map(aine => {
      koeToMap(aine, Some(aine.koodi.arvo))
    })
    Map(
      "AINEREAALI" -> resultMaps.toList
    )
  }

  private def getAidinkieliRyhma(yo: YOTutkinto): Map[String, List[Map[String, String]]] = {
    val aineet: Set[Koe] =
      yo.aineet
        .filter(a => aidinkieliAineet.contains(a.koodi.arvo))
        .filter(_.pisteet.isDefined)

    val resultMaps = aineet.map(aine => {
      val aidinkieliLisatieto = aidinkieliLisatietoMap.get(aine.koodi.arvo)
      koeToMap(aine, aidinkieliLisatieto)
    })
    Map(
      "AIDINKIELI" -> resultMaps.toList
    )
  }

  private def getYksittaisetKokeet(yo: YOTutkinto): Map[String, List[Map[String, String]]] = {
    val byKokeenTyyppi: Map[String, Set[Koe]] = yo.aineet.groupBy(_.koodi.arvo)
    byKokeenTyyppi.map { case (arvo, kokeet) =>
      val kokeidenMetatiedot = kokeet
        .filter(_.pisteet.isDefined)
        .map(koe => koeToMap(koe, None))
        .toList
      (arvo, kokeidenMetatiedot)
    }
  }


  def convert(opiskeluoikeudet: Seq[Opiskeluoikeus]): List[AvainMetatiedotDTO] = {
    val yoOpiskeluoikeudet = opiskeluoikeudet.collect { case yo: YOOpiskeluoikeus => yo }

    val yoTutkinto: Option[YOTutkinto] = yoOpiskeluoikeudet.flatMap(_.yoTutkinto).headOption

    val tutkinnonArvot: Map[String, List[Map[String, String]]] = yoTutkinto.map { tutkinto =>
      val yksittaisetKokeet = getYksittaisetKokeet(tutkinto)
      val aidinkieliRyhma = getAidinkieliRyhma(tutkinto)
      yksittaisetKokeet ++ aidinkieliRyhma
    }.getOrElse(Map.empty)

    val DTOs = tutkinnonArvot.map(a => {
      AvainMetatiedotDTO(a._1, a._2)
    }).toList

    DTOs
  }
}
