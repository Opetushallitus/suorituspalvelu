package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.KantaOperaatiot
import fi.oph.suorituspalvelu.integration.client.{AtaruPermissionRequest, HakemuspalveluClientImpl}
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration}
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.security.VirkailijaAuthorization
import fi.oph.suorituspalvelu.ui.UIService.{EXAMPLE_HETU, EXAMPLE_NIMI, EXAMPLE_OPPIJA_OID, EXAMPLE_OPPILAITOS_NIMI, EXAMPLE_OPPILAITOS_OID}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import fi.oph.suorituspalvelu.validation.Validator
import org.slf4j.LoggerFactory

import java.util.Optional
import scala.jdk.OptionConverters.*
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Optional
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

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

  val KOODISTO_YOKOKEET = "koskiyokokeet"

  val YTL_ORGANISAATIO_OID = "1.2.246.562.10.43628088406"

}

@Component
class UIService {

  val LOG = LoggerFactory.getLogger(classOf[UIService])

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClientImpl = null

  def haeOppilaitokset(): Set[Oppilaitos] =
    Set(Oppilaitos(
      nimi = OppilaitosNimi(
        fi = Optional.of(EXAMPLE_OPPILAITOS_NIMI),
        sv = Optional.of(EXAMPLE_OPPILAITOS_NIMI),
        en = Optional.of(EXAMPLE_OPPILAITOS_NIMI)
      ),
      oid = EXAMPLE_OPPILAITOS_OID
    ))

  def suoritaOnrHaku(hakusana: Option[String]): Future[Seq[OnrHenkiloPerustiedot]] = {
    hakusana match {
      case Some(h) if Validator.hetuPattern.matches(h) => onrIntegration.getPerustiedotByHetus(Set(h))
      case Some(h) if Validator.oppijaOidPattern.matches(h) => onrIntegration.getPerustiedotByPersonOids(Set(h))
      case _ => Future.successful(Seq.empty)
    }
  }

  //Hakusanalla (hetu, oppijanumero) haetaan oppijanumerorekisteristä. Muilla parametreilla haetaan myöhemmin toteutettavasta indeksistä Supan kannassa.
  //Paluuarvona leikkaus eri hakutavoista, jos molempia käytetty.
  //Muut parametrit kuin hakusana eivät toistaiseksi vaikuta mihinkään. Korjataan asia kun Supan hakuindeksi on olemassa.
  // TODO: implementaatiohuomio. Todennäköisesti halutaan purkaa olennainen tieto (oppilaitos, vuosi, luokka) erilliseen sopivasti GIN-indeksoituun tauluun KOSKI-hakujen yhteydessä josta sitten haetaan tässä
  def haeOppijat(hakusana: Option[String], oppilaitos: Option[String], vuosi: Option[String], luokka: Option[String], authorization: VirkailijaAuthorization): Set[Oppija] = {
    val resultF = suoritaOnrHaku(hakusana).flatMap(onrResult => {
      val onrOppijat: Set[Oppija] = onrResult.map(onrOppija => Oppija(onrOppija.oidHenkilo, Optional.empty, onrOppija.getNimi)).toSet

      if (onrOppijat.size > 1) {
        throw new RuntimeException(s"Oppijanumerorekisteristä löytyi useita oppijoita annetulla hakusanalla. Tämän ei pitäisi olla nykyisellään mahdollista.")
      } else if (onrOppijat.isEmpty) {
        //Jos hakusanalla ei löytynyt, palautetaan toistaiseksi esimerkkioppija. Tämän voinee purkaa siinä vaiheessa kun kälille ei ylipäätään palauteta mock-dataa.
        val esimerkkiVastaus = Set(Oppija(
          EXAMPLE_OPPIJA_OID,
          Optional.of(EXAMPLE_HETU),
          EXAMPLE_NIMI
        ))
        LOG.info(s"Hakusanalla ei löytynyt oppijaa. Palautetaan esimerkkioppija. $esimerkkiVastaus")
        Future.successful(esimerkkiVastaus)
      } else {
        if (authorization.onRekisterinpitaja) {
          LOG.info(s"Käyttäjä $authorization on rekisterinpitäjä. Ei tarkistella oikeuksia enempää.")
          Future.successful(onrOppijat)
        } else {
          LOG.info(s"Tarkistetaan käyttäjälle $authorization oikeudet Atarusta.")
          val pRequest = AtaruPermissionRequest(Set(onrOppijat.head.oppijaNumero), authorization.organisaatioOids, Set.empty)
          val oikeusCheckF = hakemuspalveluClient.checkPermission(pRequest)
            .flatMap(permissionResult => {
              val hasAccess = permissionResult.accessAllowed.contains(true)
              if (hasAccess)
                Future.successful(onrOppijat)
              else {
                LOG.warn(s"Ei oikeuksia käyttäjälle $authorization oppijaan ${onrOppijat.head.oppijaNumero}. Palautetaan tyhjä vastaus.")
                Future.successful(Set.empty)
              }
            })
          oikeusCheckF
        }
      }
    })
    Await.result(resultF, 30.seconds)
  }
}
