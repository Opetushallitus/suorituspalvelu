package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.LahtokouluTyyppi.{AIKUISTEN_PERUSOPETUS, TELMA, TUVA, VAPAA_SIVISTYSTYO, VUOSILUOKKA_9}
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.client.{AtaruPermissionRequest, AtaruPermissionResponse, HakemuspalveluClientImpl, KoutaHaku}
import fi.oph.suorituspalvelu.integration.{OnrHenkiloPerustiedot, OnrIntegration, OnrMasterHenkilo}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiUtil, NOT_DEFINED_PLACEHOLDER, Ohjattavuus}
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
  
  def haeSyotettavienSuoritustenOppilaitokset(): List[Oppilaitos] = {
    organisaatioProvider.haeKaikkiOrganisaatiot()
      .values
      .filter(organisaatio => organisaatio.tyypit.contains("organisaatiotyyppi_02"))
      .map(organisaatio => Oppilaitos(OppilaitosNimi(
        Optional.ofNullable(organisaatio.nimi.fi), Optional.ofNullable(organisaatio.nimi.sv), Optional.ofNullable(organisaatio.nimi.en)),
        organisaatio.oid))
      .toList
  }

  def haeKaikkiOppilaitoksetJoissaPKSuorituksia(): Set[Oppilaitos] = {
    // haetaan kaikkien suoritustyyppien paitsi 7. ja 8. vuosiluokkien perusteella koska niitä ei haluta näyttää kälissä
    val oppilaitosOids = kantaOperaatiot.haePKOppilaitokset(Set(AIKUISTEN_PERUSOPETUS, VUOSILUOKKA_9, TELMA, TUVA, VAPAA_SIVISTYSTYO))

    oppilaitosOids
      .flatMap(oppilaitosOid => organisaatioProvider.haeOrganisaationTiedot(oppilaitosOid))
      .map(organisaatio => Oppilaitos(OppilaitosNimi(
        Optional.ofNullable(organisaatio.nimi.fi), Optional.ofNullable(organisaatio.nimi.sv), Optional.ofNullable(organisaatio.nimi.en)),
        organisaatio.oid))
  }

  def haeVuodet(oppilaitosOid: String): Set[String] = {
    kantaOperaatiot.haeVuodet(oppilaitosOid, Set(AIKUISTEN_PERUSOPETUS, VUOSILUOKKA_9, TELMA, TUVA, VAPAA_SIVISTYSTYO))
  }

  def haeLuokat(oppilaitosOid: String, valmistumisVuosi: Int): Set[String] = {
    kantaOperaatiot.haeLuokat(oppilaitosOid, valmistumisVuosi)
  }

  def haeOhjattavatJaLuokat(oppilaitosOid: String, vuosi: Int): Set[(String, String)] = {
    kantaOperaatiot.haeHenkilotJaLuokat(oppilaitosOid, vuosi).map((henkilo, luokka) => henkilo -> luokka)
  }

  def haeOhjattavat(oppilaitos: String, valmistumisVuosi: Int, luokka: Option[String], keskenTaiKeskeytynyt: Boolean, yhteistenArvosanaPuuttuu: Boolean): Seq[Oppija] = {
    // haetaan oppijat jotka ovat oppilaitoksen halutun valmistumisvuoden ohjattavia edellyttäen ettei opiskeluoikeuksia muissa oppilaitoksissa aliaksilla
    val mahdollisetOppijaOids = kantaOperaatiot.haeLahtokoulunOppilaat(LocalDate.now, oppilaitos, valmistumisVuosi, luokka, keskenTaiKeskeytynyt, yhteistenArvosanaPuuttuu, Set(VUOSILUOKKA_9, TELMA, TUVA, VAPAA_SIVISTYSTYO))
    val r = onrIntegration.getPerustiedotByPersonOids(mahdollisetOppijaOids)
      .map(perustiedot => perustiedot.flatMap(henkilo => {
        Some(Oppija(henkilo.oidHenkilo, henkilo.hetu.toJava, henkilo.etunimet.toJava, henkilo.sukunimi.toJava))
      }))
        
    // TODO: aliaskäsittelyä ei tarvita tässä, mutta tarvitaan yksikäsitteisen lähtökoulun määrittämisessä
/*
    val r = onrIntegration.getAliasesForPersonOids(mahdollisetOppijaOids)
      .zip(onrIntegration.getPerustiedotByPersonOids(mahdollisetOppijaOids))
      .map((aliakset, perustiedot) => perustiedot.flatMap(henkilo => {
        val currAliakset = aliakset.allOidsByQueriedOids(henkilo.oidHenkilo)
        // mikäli henkilöllä on aliaksia haetaan kaikkien aliaksien suoritukset kannasta ja katsotaan onko henkilö siirtynyt muuhun oppilaitokseen
        if (currAliakset.size==1 || KoskiUtil.onkoKatseluoikeus(Set(oppilaitos), Some(valmistumisVuosi), currAliakset.flatMap(oppijaNumero => this.kantaOperaatiot.haeSuoritukset(oppijaNumero).values.toSet.flatten)))
          Some(Oppija(henkilo.oidHenkilo, henkilo.hetu.toJava, henkilo.etunimet.toJava, henkilo.sukunimi.toJava))
        else
          None
      }))
*/
    
    Await.result(r, 30.seconds)
  }

  def resolveOppijaNumero(tunniste: String): Option[String] = {
    if (Validator.hetuPattern.matches(tunniste)) {
      Await.result(
        onrIntegration.getPerustiedotByHetus(Set(tunniste)).map(_.headOption.map(_.oidHenkilo)),
        ONR_TIMEOUT
      )
    } else {
      Some(tunniste)
    }
  }

  private def resolveMasterHenkilo(oppijaNumero: String): Option[OnrMasterHenkilo] = {
    Await.result(
      onrIntegration.getMasterHenkilosForPersonOids(Set(oppijaNumero)),
      ONR_TIMEOUT
    ).values.headOption
  }

  def haeOppijanSuoritukset(oppijaNumero: String): Option[OppijanTiedotSuccessResponse] =
    resolveMasterHenkilo(oppijaNumero).map(masterHenkilo => {
      def haeAliakset(oppijaOid: String): Set[String] = {
        try
          Set(Set(oppijaNumero), Await.result(onrIntegration.getAliasesForPersonOids(Set(masterHenkilo.oidHenkilo)), ONR_TIMEOUT).allOids).flatten
        catch
          case e: Exception =>
            LOG.warn("Aliaksien hakeminen ONR:stä epäonnistui henkilölle: " + oppijaNumero, e)
            Set(oppijaNumero)
      }

      val suoritukset = haeAliakset(oppijaNumero).flatMap(oid => this.kantaOperaatiot.haeSuoritukset(oid).values.flatten)
      EntityToUIConverter.getOppijanTiedot(masterHenkilo.etunimet, masterHenkilo.sukunimi,
        masterHenkilo.hetu, oppijaNumero, masterHenkilo.syntymaaika, suoritukset, organisaatioProvider, koodistoProvider)
      })

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
      val lahettajaOikeusOrganisaatiot = securityOperaatiot.getAuthorization(Set(SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA), organisaatioProvider).oikeudellisetOrganisaatiot
      lahettajaOikeusOrganisaatiot.nonEmpty && Await.result(aliases.map(allOids => {
        val opiskeluoikeudet = allOids.flatMap(oppijaNumero => this.kantaOperaatiot.haeSuoritukset(oppijaNumero).values.toSet.flatten)
        KoskiUtil.onkoJokinLahtokoulu(Some(lahettajaOikeusOrganisaatiot), None, opiskeluoikeudet)        
      }), 30.seconds)

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
