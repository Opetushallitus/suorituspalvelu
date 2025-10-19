package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.business.{KantaOperaatiot, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.client.{AtaruPermissionRequest, AtaruPermissionResponse, HakemuspalveluClientImpl}
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration}
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil.{PK_OPPIMAARA_OPPILAITOS_KESKEN_AVAIN, PK_OPPIMAARA_OPPILAITOS_KESKEN_LUOKKA_AVAIN, PK_OPPIMAARA_OPPILAITOS_VUOSI_AVAIN, PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA_AVAIN}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiUtil, PKOppimaaraOppilaitosKeskenLuokkaMetadataArvo, PKOppimaaraOppilaitosKeskenMetadataArvo, PKOppimaaraOppilaitosVuosiMetadataArvo}
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.security.VirkailijaAuthorization
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import fi.oph.suorituspalvelu.validation.Validator
import org.slf4j.LoggerFactory

import java.time.{Instant, LocalDate}
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

  val EXAMPLE_OPPIJAT: Set[Oppija] = Set(Oppija(
    EXAMPLE_OPPIJA_OID,
    Optional.of(EXAMPLE_HETU),
    EXAMPLE_NIMI
  ))
}

@Component
class UIService {

  val LOG = LoggerFactory.getLogger(classOf[UIService])

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClientImpl = null

  @Autowired val organisaatioProvider: OrganisaatioProvider = null

  def haeOppilaitoksetJoihinOikeudet(oppilaitosOids: Set[String]): Set[Oppilaitos] = {
    oppilaitosOids
      .flatMap(oid => organisaatioProvider.haeOrganisaationTiedot(oid)
      .map(organisaatio => Oppilaitos(OppilaitosNimi(
        Optional.of(organisaatio.nimi.fi), Optional.of(organisaatio.nimi.sv), Optional.of(organisaatio.nimi.en)),
        organisaatio.oid)))
  }

  def haeKaikkiOppilaitoksetJoissaPKSuorituksia(): Set[Oppilaitos] = {
    val oppilaitosOids = Set(
      kantaOperaatiot.haeMetadataAvaimenArvot(KoskiUtil.PK_OPPIMAARA_OPPILAITOS_KESKEN_AVAIN)
        .map(avain => PKOppimaaraOppilaitosKeskenMetadataArvo(avain).oppilaitosOid),
      kantaOperaatiot.haeMetadataAvaimenArvot(KoskiUtil.PK_OPPIMAARA_OPPILAITOS_VUOSI_AVAIN)
        .map(avain => new PKOppimaaraOppilaitosVuosiMetadataArvo(avain).oppilaitosOid)
    ).flatten

    oppilaitosOids
      .flatMap(oppilaitosOid => organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid))
      .map(organisaatio => Oppilaitos(OppilaitosNimi(
        Optional.of(organisaatio.nimi.fi), Optional.of(organisaatio.nimi.sv), Optional.of(organisaatio.nimi.en)),
        organisaatio.oid))
  }

  def haeVuodet(oppilaitosOid: String): Set[String] = {
    Set(
      if(kantaOperaatiot.haeMetadataAvaimenArvot(PK_OPPIMAARA_OPPILAITOS_KESKEN_AVAIN, Some(s"$oppilaitosOid")).nonEmpty)
        Some(Set(LocalDate.now().getYear.toString))
      else
        None,
      Some(kantaOperaatiot.haeMetadataAvaimenArvot(PK_OPPIMAARA_OPPILAITOS_VUOSI_AVAIN, Some(s"$oppilaitosOid"))
        .map(arvo => new PKOppimaaraOppilaitosVuosiMetadataArvo(arvo).vuosi.toString)),
    ).flatten.flatten
  }

  def haeLuokat(oppilaitosOid: String, vuosi: Int): Set[String] = {
    Set(
      if(LocalDate.now().getYear==vuosi)
        Some(kantaOperaatiot.haeMetadataAvaimenArvot(PK_OPPIMAARA_OPPILAITOS_KESKEN_LUOKKA_AVAIN, Some(s"$oppilaitosOid"))
          .map(arvo => new PKOppimaaraOppilaitosKeskenLuokkaMetadataArvo(arvo).luokka))
      else
        None,
      Some(kantaOperaatiot.haeMetadataAvaimenArvot(PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA_AVAIN, Some(s"$oppilaitosOid"))
        .map(arvo => new PKOppimaaraOppilaitosKeskenLuokkaMetadataArvo(arvo).luokka)),
    ).flatten.flatten
  }

  def suoritaOnrHaku(hakusana: Option[String]): Future[Seq[OnrHenkiloPerustiedot]] = {
    hakusana match {
      case Some(h) if Validator.hetuPattern.matches(h) => onrIntegration.getPerustiedotByHetus(Set(h))
      case Some(h) if Validator.oppijaOidPattern.matches(h) => onrIntegration.getPerustiedotByPersonOids(Set(h))
      case _ => Future.successful(Seq.empty)
    }
  }

  def haePKOppijaOidit(oppilaitos: String, vuosi: Int, luokka: Option[String]): Set[String] = {
    KoskiUtil.getPeruskoulunOppimaaraHakuMetadata(oppilaitos, vuosi, luokka)
      .flatMap(metadata => kantaOperaatiot.haeVersiot(metadata, Instant.now()).map(v => v.oppijaNumero))
      .toSet
  }

  def haePKOppijat(oppilaitos: String, vuosi: Int, luokka: Option[String]): Set[Oppija] = {
    val oppijaOids = haePKOppijaOidit(oppilaitos, vuosi, luokka)

    val ornOppijat = onrIntegration.getPerustiedotByPersonOids(oppijaOids)
      .map(onrResult => onrResult.map(onrOppija => Oppija(onrOppija.oidHenkilo, Optional.empty, onrOppija.getNimi)).toSet)

    Await.result(ornOppijat, 30.seconds)
  }

  //Tämä ei oikeasti toimi kovin tehokkaasti suurille joukoille Oppijoita, koska Atarun permissioncheck-rajapinta käsittelee yhden henkilön kerrallaan.
  def filtteroiHakemuspohjaisillaOikeuksilla(oppijat: Set[Oppija], authorization: VirkailijaAuthorization, aliakset: Map[String, Set[String]]): Future[Set[Oppija]] = {
    LOG.info(s"Tarkistetaan käyttäjälle $authorization oikeudet Atarusta.")
    oppijat.foldLeft(Future.successful(Seq.empty[(Oppija, AtaruPermissionResponse)])) {
      case (prevFuture, oppija) =>
        prevFuture.flatMap(results => {
          val pRequest = AtaruPermissionRequest(
            aliakset.getOrElse(oppija.oppijaNumero, throw new RuntimeException(s"Oppijan ${oppija.oppijaNumero} aliaksia ei löytynyt!")),
            authorization.oikeudellisetOrganisaatiot,
            Set.empty)
          LOG.info(s"Kutsutaan atarua, $pRequest")
          hakemuspalveluClient.checkPermission(pRequest).map(p => results :+ (oppija, p))
        })
    }.flatMap((results: Seq[(Oppija, AtaruPermissionResponse)]) => {
      val filtered = results.toSet.flatMap({
        case (o: Oppija, r: AtaruPermissionResponse) if r.accessAllowed.contains(true) =>
          Some(o)
        case (o: Oppija, r: AtaruPermissionResponse) if r.accessAllowed.contains(false) =>
          LOG.warn(s"Ei oikeuksia käyttäjälle $authorization oppijaan ${oppijat.head.oppijaNumero}. Filtteröidään oppija pois.")
          None
        case (o: Oppija, r: AtaruPermissionResponse) if r.errorMessage.isDefined =>
          LOG.error(s"Virhe atarussa: ${r.errorMessage.get}")
          throw new RuntimeException(s"Virhe atarussa: ${r.errorMessage.get}")
        case _ => ???
      })
      Future.successful(filtered)
    })
  }

  def haeOppija(oppijaOid: String, authorization: VirkailijaAuthorization): Set[Oppija] = {
    val resultF = suoritaOnrHaku(Some(oppijaOid)).flatMap(onrResult => {
      val onrOppijat: Set[Oppija] = onrResult.map(onrOppija => Oppija(onrOppija.oidHenkilo, Optional.empty, onrOppija.getNimi)).toSet
      if (onrOppijat.nonEmpty) {
        if (authorization.onRekisterinpitaja) {
          Future.successful(onrOppijat)
        } else {
          onrIntegration.getAliasesForPersonOids(onrOppijat.map(_.oppijaNumero)).map(_.allOidsByQueriedOids).flatMap(aliasResult => {
            filtteroiHakemuspohjaisillaOikeuksilla(onrOppijat, authorization, aliasResult)
          })
        }
      } else {
        //Jos hakusanalla ei löytynyt, palautetaan toistaiseksi esimerkkioppija. Tämän voinee purkaa siinä vaiheessa kun kälille ei ylipäätään palauteta mock-dataa.
        Future.successful(UIService.EXAMPLE_OPPIJAT)
      }
    })
    Await.result(resultF, 30.seconds)
  }
}
