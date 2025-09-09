package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, KantaOperaatiot, Opiskeluoikeus}
import fi.oph.suorituspalvelu.resource.ui.SuoritusTila.{KESKEN, KESKEYTYNYT, VALMIS}
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.ui.UIService.{EXAMPLE_HETU, EXAMPLE_NIMI, EXAMPLE_OPPIJA_OID, EXAMPLE_OPPILAITOS_NIMI, EXAMPLE_OPPILAITOS_OID}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.time.LocalDate
import java.util.Optional
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

object UIService {
  val EXAMPLE_OPPIJA_OID = "1.2.246.562.24.40483869857"
  val EXAMPLE_HETU = "010296-1230"
  val EXAMPLE_NIMI = "Olli Oppija"

  val EXAMPLE_OPPILAITOS_OID = "1.2.246.562.10.56753942459"
  val EXAMPLE_OPPILAITOS_NIMI = "Esimerkki oppilaitos"
}

// TODO: tähän tarvitaan lopullinen lista Sadulta
enum SyotetynOppiaineenKoodi:
  case A
  case A1
  case A2
  case B1
  case B2
  case B3
  case BI
  case XX
  case FI
  case FY
  case HI
  case KE
  case KO
  case KU
  case KS
  case LI
  case GE
  case MA
  case MU
  case OP
  case OPA
  case PS
  case TE
  case ET
  case KT
  case YH
  case YL
  case AOM
  case AI

val SYOTETYN_OPPIAINEEN_KOODIT = SyotetynOppiaineenKoodi.values.map(_.toString).toSet

enum SyotetynOppimaaranSuorituskieli:
  case FI
  case SV
  case EN
  case SE
  case DE

val SYOTETYN_OPPIMAARAN_SUORITUSKIELET = SyotetynOppimaaranSuorituskieli.values.map(_.toString).toSet

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
