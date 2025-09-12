package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.KantaOperaatiot
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.ui.UIService.{EXAMPLE_HETU, EXAMPLE_NIMI, EXAMPLE_OPPIJA_OID, EXAMPLE_OPPILAITOS_NIMI, EXAMPLE_OPPILAITOS_OID}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.util.Optional
import scala.jdk.OptionConverters.*

object UIService {
  val EXAMPLE_OPPIJA_OID = "1.2.246.562.24.40483869857"
  val EXAMPLE_HETU = "010296-1230"
  val EXAMPLE_NIMI = "Olli Oppija"

  val EXAMPLE_OPPILAITOS_OID = "1.2.246.562.10.56753942459"
  val EXAMPLE_OPPILAITOS_NIMI = "Esimerkki oppilaitos"

  val KOODISTO_SUORITUKSENTYYPIT = "suorituksentyyppi"
  val SYOTETTAVAT_SUORITUSTYYPIT = List(
    "perusopetuksenoppimaara",
    "perusopetuksenoppiaineenoppimaara"
  )

  val KOODISTO_OPPIAINEET = "koskioppiaineetyleissivistava"
  val SYOTETTAVAT_OPPIAINEET = List(
    "AI",
    "A1",
    "A2",
    "B1",
    "MA",
    "BI",
    "GE",
    "FY",
    "KE",
    "TE",
    "KT",
    "HI",
    "YH",
    "MU",
    "KU",
    "KS",
    "LI",
    "KO"
  )

  val SYOTETYN_OPPIMAARAN_KIELIAINEKOODIT = List(
    "A",
    "A1",
    "A2",
    "B1",
    "B2",
    "B3"
  )

  val KOODISTO_SUORITUSKIELET = "kieli"
  val SYOTETYN_OPPIMAARAN_SUORITUSKIELET = List(
    "FI",
    "SV",
    "EN",
    "SE",
    "DE"
  )

  val KOODISTO_OPPIAINE_AIDINKIELI_JA_KIRJALLISUUS = "oppiaineaidinkielijakirjallisuus"
  val KOODISTO_KIELIVALIKOIMA = "kielivalikoima"

  val KOODISTO_POHJAKOULUTUS = "2asteenpohjakoulutus2021"
  val SYOTETYN_OPPIMAARAN_YKSILOLLISTAMINEN = List(
    1, // Perusopetuksen oppimäärä
    2, // Perusopetuksen osittain yksilöllistetty oppimäärä
    3, // Perusopetuksen yksilöllistetty oppimäärä, opetus järjestetty toiminta-alueittain
    6, // Perusopetuksen pääosin tai kokonaan yksilöllistetty oppimäärä
    8, // Perusopetuksen osittain rajattu oppimäärä
    9  // Perusopetuksen pääosin tai kokonaan rajattu oppimäärä
  )
}

@Component
class UIService {

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  def haeOppilaitokset(): Set[Oppilaitos] =
    Set(Oppilaitos(
      nimi = OppilaitosNimi(
        fi = Optional.of(EXAMPLE_OPPILAITOS_NIMI),
        sv = Optional.of(EXAMPLE_OPPILAITOS_NIMI),
        en = Optional.of(EXAMPLE_OPPILAITOS_NIMI)
      ),
      oid = EXAMPLE_OPPILAITOS_OID
    ))

  def haeOppijat(oppija: Option[String], oppilaitos: Option[String], vuosi: Option[String], luokka: Option[String]): Set[Oppija] =
    // TODO: implementaatiohuomio. Todennäköisesti halutaan purkaa olennainen tieto (oppilaitos, vuosi, luokka) erilliseen sopivasti GIN-indeksoituun tauluun KOSKI-hakujen yhteydessä josta sitten haetaan tässä
    Set(Oppija(
      EXAMPLE_OPPIJA_OID,
      Optional.of(EXAMPLE_HETU),
      EXAMPLE_NIMI
    )).filter(o => {
      oppija.isDefined && (o.oppijaNumero == oppija.get || o.hetu.get() == oppija.get || o.nimi.contains(oppija.get))
    })

}
