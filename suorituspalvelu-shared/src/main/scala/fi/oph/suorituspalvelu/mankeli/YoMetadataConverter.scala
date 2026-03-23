package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business.{Koe, Opiskeluoikeus, YOOpiskeluoikeus, YOTutkinto}

import java.time.LocalDate

case class AvainMetatiedotDTO(avain: String, metatiedot: Seq[Map[String, String]])

object YoMetadataConverter {

  val aineToLisatietoMap = Map(
    "SA" -> "SA",
    "EA" -> "EN",
    "EB" -> "EN",
    "HA" -> "UN",
    "FB" -> "RA",
    "E2" -> "EN",
    "M" -> "MA",
    "SC" -> "SA",
    "VA" -> "VE",
    "F2" -> "RA",
    "GB" -> "PG",
    "PS" -> "PS",
    "I" -> "IS",
    "HI" -> "HI",
    "V2" -> "VE",
    "RY" -> "ET",
    "TA" -> "IT",
    "CB" -> "FI",
    "CC" -> "FI",
    "S9" -> "SA",
    "G2" -> "PG",
    "V1" -> "VE",
    "HB" -> "UN",
    "TB" -> "IT",
    "O" -> "RU",
    "A" -> "FI",
    "P1" -> "ES",
    "GC" -> "PG",
    "S2" -> "SA",
    "PC" -> "ES",
    "FY" -> "FY",
    "EC" -> "EN",
    "L1" -> "LA",
    "H1" -> "UN",
    "O5" -> "RU",
    "FA" -> "RA",
    "CA" -> "FI",
    "F1" -> "RA",
    "J" -> "EN",
    "A5" -> "FI",
    "Z" -> "ZA",
    "IC" -> "IS",
    "KE" -> "KE",
    "T1" -> "IT",
    "RO" -> "UO",
    "YH" -> "YH",
    "BA" -> "RU",
    "H2" -> "UN",
    "BI" -> "BI",
    "VC" -> "VE",
    "FF" -> "FF",
    "BB" -> "RU",
    "E1" -> "EN",
    "T2" -> "IT",
    "DC" -> "ZA",
    "GE" -> "GE",
    "P2" -> "ES",
    "TC" -> "IT",
    "G1" -> "PG",
    "UO" -> "UO",
    "RR" -> "UE",
    "VB" -> "VE",
    "KC" -> "KR",
    "ET" -> "ET",
    "PB" -> "ES",
    "SB" -> "SA",
    "S1" -> "SA",
    "QC" -> "QC",
    "N" -> "MA",
    "L7" -> "LA",
    "PA" -> "ES",
    "FC" -> "RA",
    "TE" -> "TE",
    "GA" -> "PG",
    "UE" -> "UE",
    "W" -> "QS"
  )



  val aidinkieliAineet = Set("A", "O", "I", "W", "Z", "O5", "A5")
  val ainereaaliAineet = Set("UE", "UO", "ET", "PS", "HI", "FY", "KE", "BI", "GE", "TE", "YH", "FF")
  val reaaliAIneet = Set("RR", "RO", "RY")

  val pitkaKieliAineet = Set("EA", "FA", "GA", "HA", "PA", "SA", "TA", "VA", "S9")
  val keskipitkaKieliAineet = Set("EB", "FB", "GB", "HB", "PB", "SB", "TB", "VB")
  val lyhytKieliAineet = Set("EC", "FC", "GC", "L1", "PC", "SC", "TC", "VC", "KC", "L7")

  private def parseRyhma(yoTutkinto: YOTutkinto, ryhmaanKuuluvatAineet: Set[String], ryhmaAvain: String) = {
    val aineet =
      yoTutkinto.aineet
        .filter(a => ryhmaanKuuluvatAineet.contains(a.koodi.arvo))
        .filter(_.pisteet.isDefined)

    val resultMaps = aineet.map(aine => {
      val lisatieto = aineToLisatietoMap.get(aine.koodi.arvo)
      koeToMap(aine, lisatieto)
    })
    Map(
      ryhmaAvain -> resultMaps.toList
    )
  }

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

  def convert(opiskeluoikeudet: Seq[Opiskeluoikeus]): Seq[AvainMetatiedotDTO] = {
    val yoOpiskeluoikeudet = opiskeluoikeudet.collect { case yo: YOOpiskeluoikeus => yo }

    val yoTutkinto: Option[YOTutkinto] = yoOpiskeluoikeudet.flatMap(_.yoTutkinto).headOption

    val tutkinnonArvot: Map[String, Seq[Map[String, String]]] = yoTutkinto.map { tutkinto =>
      val yksittaisetKokeet = getYksittaisetKokeet(tutkinto)
      val aidinkieliRyhma = parseRyhma(tutkinto, aidinkieliAineet, "AIDINKIELI")
      val aineReaaliRyhma = parseRyhma(tutkinto, ainereaaliAineet, "AINEREAALI")
      val reaaliRyhma = parseRyhma(tutkinto, reaaliAIneet, "REAALI")
      val pitkaKieliRyhma = parseRyhma(tutkinto, pitkaKieliAineet, "PITKA_KIELI")
      val keskipitkaKieliRyhma = parseRyhma(tutkinto, keskipitkaKieliAineet, "KESKIPITKA_KIELI")
      val lyhytKieliRyhma = parseRyhma(tutkinto, lyhytKieliAineet, "LYHYT_KIELI")
      val combined = yksittaisetKokeet ++ aidinkieliRyhma ++ aineReaaliRyhma ++ reaaliRyhma ++ pitkaKieliRyhma ++ keskipitkaKieliRyhma ++ lyhytKieliRyhma
      combined.filter(_._2.nonEmpty) // poistetaan tyhjät ryhmät
    }.getOrElse(Map.empty)

    val DTOs = tutkinnonArvot.map(a => {
      AvainMetatiedotDTO(a._1, a._2)
    }).toSeq

    DTOs
  }
}
