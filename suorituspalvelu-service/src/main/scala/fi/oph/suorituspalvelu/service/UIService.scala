package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{KantaOperaatiot, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.client.{AtaruPermissionRequest, AtaruPermissionResponse, HakemuspalveluClientImpl, KoutaHaku}
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration}
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil.{PK_OPPIMAARA_OPPILAITOS_VUOSI_AVAIN, PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA_AVAIN}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiUtil, PKOppimaaraOppilaitosVuosiLuokkaMetadataArvo, PKOppimaaraOppilaitosVuosiMetadataArvo}
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.security.{SecurityConstants, SecurityOperaatiot, VirkailijaAuthorization}
import fi.oph.suorituspalvelu.ui.EntityToUIConverter
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}
import fi.oph.suorituspalvelu.validation.Validator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.time.{Instant, LocalDate}
import java.util.Optional
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.jdk.OptionConverters.*

object UIService {
  val EXAMPLE_OPPIJA_OID = "1.2.246.562.24.40483869857"
  val EXAMPLE_HETU = "010296-1230"
  val EXAMPLE_ETUNIMET = "Olli"
  val EXAMPLE_SUKUNIMI = "Oppija"

  val EXAMPLE_OPPILAITOS_OID = "1.2.246.562.10.56753942459"
  val EXAMPLE_OPPILAITOS_NIMI = "Esimerkki oppilaitos"

  val KOODISTO_SUORITUKSENTILAT = "suorituksentila"
  val SYOTETTAVAT_SUORITUSTILAT = List(
    "VALMIS",
    "KESKEN"
  )
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

  @Autowired val organisaatioProvider: OrganisaatioProvider = null

  @Autowired val koodistoProvider: KoodistoProvider = null

  @Autowired val tarjontaIntegration: fi.oph.suorituspalvelu.integration.TarjontaIntegration = null

  val ONR_TIMEOUT = 10.seconds;

  def haeOppilaitoksetJoihinOikeudet(oppilaitosOids: Set[String]): Set[Oppilaitos] = {
    oppilaitosOids
      .flatMap(oid => organisaatioProvider.haeOrganisaationTiedot(oid)
        .map(organisaatio => Oppilaitos(OppilaitosNimi(
          Optional.ofNullable(organisaatio.nimi.fi), Optional.ofNullable(organisaatio.nimi.sv), Optional.ofNullable(organisaatio.nimi.en)),
          organisaatio.oid)))
  }

  def haeKaikkiOppilaitoksetJoissaPKSuorituksia(): Set[Oppilaitos] = {
    val oppilaitosOids = kantaOperaatiot.haeMetadataAvaimenArvot(KoskiUtil.PK_OPPIMAARA_OPPILAITOS_VUOSI_AVAIN)
      .map(avain => new PKOppimaaraOppilaitosVuosiMetadataArvo(avain).oppilaitosOid)

    oppilaitosOids
      .flatMap(oppilaitosOid => organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid))
      .map(organisaatio => Oppilaitos(OppilaitosNimi(
        Optional.ofNullable(organisaatio.nimi.fi), Optional.ofNullable(organisaatio.nimi.sv), Optional.ofNullable(organisaatio.nimi.en)),
        organisaatio.oid))
  }

  def haeSyotettavienSuoritustenOppilaitokset(): List[Oppilaitos] = {
    organisaatioProvider.haeKaikkiOrganisaatiot()
      .values
      .filter(organisaatio => organisaatio.tyypit.contains("organisaatiotyyppi_02"))
      .map(organisaatio => Oppilaitos(OppilaitosNimi(
        Optional.ofNullable(organisaatio.nimi.fi), Optional.ofNullable(organisaatio.nimi.sv), Optional.ofNullable(organisaatio.nimi.en)),
        organisaatio.oid))
      .toList
  }

  def haeVuodet(oppilaitosOid: String): Set[String] = {
    kantaOperaatiot.haeMetadataAvaimenArvot(PK_OPPIMAARA_OPPILAITOS_VUOSI_AVAIN, Some(s"$oppilaitosOid"))
      .map(arvo => new PKOppimaaraOppilaitosVuosiMetadataArvo(arvo).vuosi.getOrElse(LocalDate.now().getYear).toString)
  }

  def haeLuokat(oppilaitosOid: String, vuosi: Int): Set[String] = {
    Set(
      if (LocalDate.now().getYear == vuosi)
        Some(kantaOperaatiot.haeMetadataAvaimenArvot(PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA_AVAIN, Some(s"$oppilaitosOid:KESKEN"))
          .map(arvo => new PKOppimaaraOppilaitosVuosiLuokkaMetadataArvo(arvo).luokka))
      else
        None,
      Some(kantaOperaatiot.haeMetadataAvaimenArvot(PK_OPPIMAARA_OPPILAITOS_VUOSI_LUOKKA_AVAIN, Some(s"$oppilaitosOid:$vuosi"))
        .map(arvo => new PKOppimaaraOppilaitosVuosiLuokkaMetadataArvo(arvo).luokka)),
    ).flatten.flatten
  }

  def haePKOppijaOidit(oppilaitosOid: String, vuosi: Int, luokka: Option[String]): Set[(String, Set[String])] = {
    KoskiUtil.getPeruskoulunOppimaaraHakuMetadata(oppilaitosOid, vuosi, luokka)
      .flatMap(metadata => kantaOperaatiot.haeVersiotJaMetadata(metadata, Instant.now()).map((versio, metadata) => (versio.oppijaNumero, KoskiUtil.extractLuokat(oppilaitosOid, metadata))))
      .toSet
  }

  def haePKOppijat(oppilaitos: String, vuosi: Int, luokka: Option[String]): Set[Oppija] = {
    val oppijaOids = haePKOppijaOidit(oppilaitos, vuosi, luokka).map(_._1)

    val ornOppijat = onrIntegration.getPerustiedotByPersonOids(oppijaOids)
      .map(onrResult => onrResult.map(onrOppija => Oppija(onrOppija.oidHenkilo, Optional.empty, onrOppija.etunimet.toJava, onrOppija.sukunimi.toJava)).toSet)

    Await.result(ornOppijat, 30.seconds)
  }

  def haeHenkilonPerustiedot(hakusana: Option[String]): Future[Option[OnrHenkiloPerustiedot]] = {
    hakusana match {
      case Some(h) if Validator.hetuPattern.matches(h) => onrIntegration.getPerustiedotByHetus(Set(h)).map(r => r.headOption)
      case Some(h) if Validator.oppijaOidPattern.matches(h) => onrIntegration.getPerustiedotByPersonOids(Set(h)).map(r => r.headOption)
      case _ => Future.successful(None)
    }
  }

  def haeOppijanSuoritukset(oppijaNumero: String): Option[OppijanTiedotSuccessResponse] =
    val masterHenkilo = Await.result(onrIntegration.getMasterHenkilosForPersonOids(Set(oppijaNumero)), ONR_TIMEOUT).values.headOption
    if (masterHenkilo.isEmpty)
      None
    else
      def haeAliakset(oppijaOid: String): Set[String] =
        try
          Set(Set(oppijaNumero), Await.result(onrIntegration.getAliasesForPersonOids(Set(masterHenkilo.get.oidHenkilo)), ONR_TIMEOUT).allOids).flatten
        catch
          case e: Exception =>
            LOG.warn("Aliaksien hakeminen ONR:stä epäonnistui henkilölle: " + oppijaNumero, e)
            Set(oppijaNumero)

      val suoritukset = haeAliakset(oppijaNumero).flatMap(oid => this.kantaOperaatiot.haeSuoritukset(oppijaNumero).values.toSet.flatten)
      Some(EntityToUIConverter.getOppijanTiedot(masterHenkilo.get.etunimet, masterHenkilo.get.sukunimi,
        masterHenkilo.get.hetu, oppijaNumero, suoritukset, organisaatioProvider, koodistoProvider))

  /**
   * Haetaan yksittäisen oppijan tiedot käyttäjän oikeuksilla. HUOM! tätä metodia ei voi kutsua suurelle joukolle oppijoita
   * koska jokaisesta kutsusta seuraa aina ONR- ja atarukutsu.
   *
   * @param hakusana haettava oppija
   * @return oppijan tiedot, None jos oppijaa ei löytynyt tai käyttäjällä ei ole tarvittavia oikeuksia
   */
  def haeOppija(hakusana: String): Option[Oppija] = {
    val oppija = Await.result(haeHenkilonPerustiedot(Some(hakusana)).map(onrResult => onrResult.map(onrOppija => Oppija(onrOppija.oidHenkilo, Optional.empty, onrOppija.etunimet.toJava, onrOppija.sukunimi.toJava))), 30.seconds)
    val hasOikeus = oppija.exists(o => hasOppijanKatseluOikeus(o.oppijaNumero))
    (oppija, hasOikeus) match
      case (Some(oppija), true) => Some(oppija)
      case (Some(oppija), false) => None
      case (_, _) => None
  }

  /**
   * Tarkastaa onko käyttäjällä oikeuksia nähdä oppijat tiedot. Tarkastetaan sekä rekisterinpitäjä-status että
   * lähettävän ja vastaanottavan organisaation oikeudet.
   *
   * @param oppijaOid oppijan oid jonka tietoja halutaan katsella
   * @return true jos käyttäjällä oikeudet nähdä oppijan tiedot
   */
  def hasOppijanKatseluOikeus(oppijaOid: String): Boolean = {
    val securityOperaatiot = new SecurityOperaatiot()
    lazy val aliases = onrIntegration.getAliasesForPersonOids(Set(oppijaOid)).map(aliasResult => aliasResult.allOids)

    def hasHakijaKatseluoikeus(): Boolean =
      val hakijaOikeusOrganisaatiot = securityOperaatiot.getAuthorization(Set(SecurityConstants.SECURITY_ROOLI_HAKENEIDEN_KATSELIJA), organisaatioProvider).oikeudellisetOrganisaatiot
      hakijaOikeusOrganisaatiot.nonEmpty && Await.result(aliases.flatMap(allOids => {
        hakemuspalveluClient.checkPermission(AtaruPermissionRequest(allOids, hakijaOikeusOrganisaatiot, Set.empty))
          .map(permissionResult => {
            if (permissionResult.errorMessage.isDefined)
              LOG.error(s"Virhe atarussa: ${permissionResult.errorMessage.get}")
            permissionResult.accessAllowed.getOrElse(false)
          })
      }), 30.seconds)

    def hasOrganisaatioKatseluoikeus(): Boolean =
      val vastaanottajaOikeusOrganisaatiot = securityOperaatiot.getAuthorization(Set(SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA), organisaatioProvider).oikeudellisetOrganisaatiot
      vastaanottajaOikeusOrganisaatiot.nonEmpty && Await.result(aliases.map(allOids => allOids.exists(oppijaOid => {
        val opiskeluoikeudet = this.kantaOperaatiot.haeSuoritukset(oppijaOid).values.flatten.toSeq
        val metadata = KoskiUtil.getMetadata(opiskeluoikeudet)
        vastaanottajaOikeusOrganisaatiot.exists(organisaatio => KoskiUtil.hasOrganisaatioPKMetadata(organisaatio, metadata))
      })), 30.seconds)

    securityOperaatiot.onRekisterinpitaja() || hasHakijaKatseluoikeus() || hasOrganisaatioKatseluoikeus()
  }

  /**
   * Hakee oppijan haut hakemuspalvelusta ja niille nimet koutasta
   *
   * @param oppijaOid Oppijan tunniste, jonka haut haetaan
   * @return Lista Haku-objekteja (hakuOid ja nimi)
   */
  def haeOppijanHaut(oppijaOid: String): Seq[Haku] = {
    val hautMap = Await.result(hakemuspalveluClient.getHenkilonHaut(Seq(oppijaOid)), 30.seconds)
    val hakuOids = hautMap.getOrElse(oppijaOid, Seq.empty)
    hakuOids.flatMap(hakuOid => {
      val koutaHaku = tarjontaIntegration.getHaku(hakuOid)
      if (koutaHaku.isEmpty) {
        LOG.debug(s"Hakemuspalvelusta löytyi hakemus haulle $hakuOid, mutta hakua ei löytynyt Koutasta.")
      }

      koutaHaku.map(haku => {
        val nimi = haku.nimi
        Haku(
          haku.oid,
          HakuNimi(
            Optional.ofNullable(nimi.get("fi").orNull),
            Optional.ofNullable(nimi.get("sv").orNull),
            Optional.ofNullable(nimi.get("en").orNull)
          )
        )
      })
    })
  }
}