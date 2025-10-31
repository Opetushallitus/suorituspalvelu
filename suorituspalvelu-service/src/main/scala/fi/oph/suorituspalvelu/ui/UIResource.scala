package fi.oph.suorituspalvelu.ui

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.business.{AvainArvoYliajo, KantaOperaatiot, Opiskeluoikeus, SuoritusJoukko}
import fi.oph.suorituspalvelu.integration.OnrIntegration
import fi.oph.suorituspalvelu.integration.client.{AtaruPermissionRequest, HakemuspalveluClient}
import fi.oph.suorituspalvelu.mankeli.UseitaVahvistettujaOppimaariaException
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil
import fi.oph.suorituspalvelu.parsing.virkailija.VirkailijaToSuoritusConverter
import fi.oph.suorituspalvelu.resource.ApiConstants.{ESIMERKKI_HAKU_OID, ESIMERKKI_LUOKKA, ESIMERKKI_OPPIJANUMERO, ESIMERKKI_OPPILAITOS_OID, ESIMERKKI_VERSIOTUNNISTE, ESIMERKKI_VUOSI, ESIMERKKI_YLIAJO_AVAIN, UI_400_DESCRIPTION, UI_403_DESCRIPTION, UI_500_DESCRIPTION, UI_HENKILO_HAKU_PATH, UI_HENKILO_HAKU_TUNNISTE_PARAM_NAME, UI_KAYTTAJAN_TIEDOT_PATH, UI_LUOKAT_OPPILAITOS_PARAM_NAME, UI_LUOKAT_PATH, UI_LUOKAT_VUOSI_PARAM_NAME, UI_OPPILAITOKSET_EI_OIKEUKSIA, UI_OPPILAITOKSET_PATH, UI_OPPILAITOS_HAKU_LUOKKA_PARAM_NAME, UI_OPPILAITOS_HAKU_OPPILAITOS_PARAM_NAME, UI_OPPILAITOS_HAKU_PATH, UI_OPPILAITOS_HAKU_VUOSI_PARAM_NAME, UI_POISTA_SUORITUS_PATH, UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME, UI_POISTA_YLIAJO_PATH, UI_TALLENNA_SUORITUS_OPPIAINE_PATH, UI_TALLENNA_SUORITUS_OPPILAITOKSET_PATH, UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH, UI_TALLENNA_SUORITUS_VAIHTOEHDOT_PATH, UI_TALLENNA_YLIAJOT_PATH, UI_TIEDOT_OPPIJANUMERO_PARAM_NAME, UI_TIEDOT_PATH, UI_VALINTADATA_AVAIN_PARAM_NAME, UI_VALINTADATA_HAKU_PARAM_NAME, UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, UI_VALINTADATA_PATH, UI_VUODET_EI_OIKEUKSIA, UI_VUODET_OPPILAITOS_PARAM_NAME, UI_VUODET_PATH}
import fi.oph.suorituspalvelu.resource.ui.UIVirheet.{UI_HAKU_EI_OIKEUKSIA, UI_HAKU_EPAONNISTUI, UI_HAKU_JOKO_HAKUSANA_TAI_OPPILAITOS, UI_KAYTTAJAN_TIEDOT_HAKU_EPAONNISTUI, UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT, UI_LUO_SUORITUS_OPPIAINE_EI_OIKEUKSIA, UI_LUO_SUORITUS_OPPIAINE_JSON_VIRHE, UI_LUO_SUORITUS_OPPIAINE_TALLENNUS_VIRHE, UI_LUO_SUORITUS_OPPIAINE_TUNTEMATON_OPPIJA, UI_LUO_SUORITUS_PERUSOPETUS_EI_OIKEUKSIA, UI_LUO_SUORITUS_PERUSOPETUS_JSON_VIRHE, UI_LUO_SUORITUS_PERUSOPETUS_TALLENNUS_VIRHE, UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA, UI_LUO_SUORITUS_VAIHTOEHDOT_ESIMERKKI_VIRHE, UI_OPPILAITOS_HAKU_OPPILAITOS_PAKOLLINEN, UI_OPPILAITOS_HAKU_VUOSI_PAKOLLINEN, UI_POISTA_SUORITUS_EI_OIKEUKSIA, UI_POISTA_SUORITUS_SUORITUSTA_EI_LOYTYNYT, UI_POISTA_SUORITUS_SUORITUSTA_EI_POISTETTAVISSA, UI_POISTA_SUORITUS_SUORITUS_EI_VOIMASSA, UI_POISTA_SUORITUS_TALLENNUS_VIRHE, UI_POISTA_YLIAJO_EI_OIKEUKSIA, UI_POISTA_YLIAJO_VIRHE, UI_RAJAIMEN_TIEDOT_HAKU_EPAONNISTUI, UI_TALLENNA_YLIAJO_OPPIJALLE_TALLENNUS_VIRHE, UI_TIEDOT_EI_OIKEUKSIA, UI_TIEDOT_HAKU_EPAONNISTUI, UI_VALINTADATA_EI_OIKEUKSIA, UI_VALINTADATA_GENEERINEN_BACKEND_VIRHE, UI_VALINTADATA_USEITA_VAHVISTETTUJA_OPPIMAARIA}
import fi.oph.suorituspalvelu.validation.UIValidator.VALIDATION_TUNNISTE_EI_VALIDI
import fi.oph.suorituspalvelu.validation.Validator
import fi.oph.suorituspalvelu.resource.ui.{KayttajaFailureResponse, KayttajaResponse, KayttajaSuccessResponse, LuoPerusopetuksenOppiaineenOppimaaraFailureResponse, LuoPerusopetuksenOppiaineenOppimaaraResponse, LuoPerusopetuksenOppiaineenOppimaaraSuccessResponse, LuoPerusopetuksenOppimaaraFailureResponse, LuoPerusopetuksenOppimaaraFailureResponseOppiaineVirhe, LuoPerusopetuksenOppimaaraResponse, LuoPerusopetuksenOppimaaraSuccessResponse, LuoSuoritusDropdownDataFailureResponse, LuoSuoritusDropdownDataResponse, LuoSuoritusDropdownDataSuccessResponse, LuoSuoritusOppilaitoksetFailureResponse, LuoSuoritusOppilaitoksetResponse, LuoSuoritusOppilaitoksetSuccessResponse, LuokatFailureResponse, LuokatResponse, LuokatSuccessResponse, OppijanHakuFailureResponse, OppijanHakuResponse, OppijanHakuSuccessResponse, OppijanTiedotFailureResponse, OppijanTiedotResponse, OppijanTiedotSuccessResponse, OppijanValintaDataFailureResponse, OppijanValintaDataSuccessResponse, OppilaitosFailureResponse, OppilaitosResponse, OppilaitosSuccessResponse, PoistaSuoritusFailureResponse, PoistaSuoritusResponse, PoistaSuoritusSuccessResponse, PoistaYliajoFailureResponse, PoistaYliajoSuccessResponse, PoistaYliajotResponse, SyotettavaAidinkielenOppimaaraVaihtoehto, SyotettavaAidinkielenOppimaaraVaihtoehtoNimi, SyotettavaArvosanaVaihtoehto, SyotettavaOppiaineVaihtoehto, SyotettavaOppiaineVaihtoehtoNimi, SyotettavaSuoritusKieliVaihtoehto, SyotettavaSuoritusKieliVaihtoehtoNimi, SyotettavaSuoritusTilaVaihtoehto, SyotettavaSuoritusTilaVaihtoehtoNimi, SyotettavaSuoritusTyyppiVaihtoehto, SyotettavaSuoritusTyyppiVaihtoehtoNimi, SyotettavaVierasKieliVaihtoehto, SyotettavaVierasKieliVaihtoehtoNimi, SyotettavaYksilollistamisVaihtoehto, SyotettavaYksilollistamisVaihtoehtoNimi, SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, SyotettyPerusopetuksenOppimaaranSuoritus, TallennaYliajotOppijalleFailureResponse, TallennaYliajotOppijalleResponse, TallennaYliajotOppijalleSuccessResponse, VuodetFailureResponse, VuodetResponse, VuodetSuccessResponse, YliajoTallennusContainer}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityConstants, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.{UIService, ValintaDataService}
import fi.oph.suorituspalvelu.service.UIService.{KOODISTO_KIELIVALIKOIMA, KOODISTO_OPPIAINEET, KOODISTO_OPPIAINE_AIDINKIELI_JA_KIRJALLISUUS, KOODISTO_POHJAKOULUTUS, KOODISTO_SUORITUKSENTILAT, KOODISTO_SUORITUKSENTYYPIT, KOODISTO_SUORITUSKIELET, SYOTETTAVAT_OPPIAINEET, SYOTETTAVAT_SUORITUSTILAT, SYOTETTAVAT_SUORITUSTYYPIT, SYOTETYN_OPPIMAARAN_KIELIAINEKOODIT, SYOTETYN_OPPIMAARAN_SUORITUSKIELET, SYOTETYN_OPPIMAARAN_YKSILOLLISTAMINEN}
import fi.oph.suorituspalvelu.util.{KoodistoProvider, LogContext, OrganisaatioProvider}
import fi.oph.suorituspalvelu.validation.UIValidator
import fi.vm.sade.auditlog.User
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.{HttpServletRequest, HttpSession}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

import java.time.Instant
import java.util
import java.util.{Optional, UUID}
import scala.concurrent.Await
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.concurrent.duration.DurationInt

@RequestMapping(path = Array(""))
@RestController
@Tag(
  name = "Käyttöliittymä",
  description = "Rajapinnat suorituspalvelun käyttöliittymää varten. Nämä rajapinnat voivat lähtökohtaisesti muuttua koska tahansa, eikä niitä " +
    "näin ollen voi käyttää muuhun kuin suorituspalvelun oman käyttöliittymän tarpeisiin. Palvelukäyttäjillä ei ole pääsyä näihin rajapintoihin. ")
class UIResource {

  val ASIOINTIKIELI_SESSION_KEY = "asiointikieli"

  val LOG = LoggerFactory.getLogger(classOf[UIResource]);

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired val organisaatioProvider: OrganisaatioProvider = null

  @Autowired val koodistoProvider: KoodistoProvider = null

  @Autowired val uiService: UIService = null

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired var objectMapper: ObjectMapper = null

  @Autowired val valintaDataService: ValintaDataService = null

  val ONR_TIMEOUT = 10.seconds;

  @GetMapping(
    path = Array(UI_KAYTTAJAN_TIEDOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa käyttöliittymän tarvitsemat suorituspalvelu-sovelluksen käyttäjään liittyvät tiedot",
    description = "Palauttaa suorituspalvelu-sovelluksen käyttäjän tiedot joita tarvitaan käyttöliittymän mukauttamiseen, " +
      "esim. asiointikieli",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa käyttäjän tiedot", content = Array(new Content(schema = new Schema(implementation = classOf[KayttajaSuccessResponse]))))
    ))
  def haeKayttajanTiedot(request: HttpServletRequest, session: HttpSession): ResponseEntity[KayttajaResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_KAYTTAJAN_TIEDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if(securityOperaatiot.onUIKayttaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(UI_OPPILAITOKSET_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            val securityOperaatiot = new SecurityOperaatiot
            val onOrganisaationKatselija = securityOperaatiot.onOrganisaationKatselija()
            val storedKieli = Option.apply(session.getAttribute(ASIOINTIKIELI_SESSION_KEY).asInstanceOf[String])
            if(storedKieli.isDefined)
              Right(ResponseEntity.status(HttpStatus.OK).body(KayttajaSuccessResponse(storedKieli.get, onOrganisaationKatselija)))
            else
              val principal = SecurityContextHolder.getContext.getAuthentication.getPrincipal.asInstanceOf[UserDetails]
              val kieli = Await.result(this.onrIntegration.getAsiointikieli(principal.getUsername), ONR_TIMEOUT)
              if(kieli.isEmpty)
                Left(ResponseEntity.status(HttpStatus.NOT_FOUND).body(KayttajaFailureResponse(java.util.Set.of(UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT))))
              else
                session.setAttribute(ASIOINTIKIELI_SESSION_KEY, kieli.get)
                Right(ResponseEntity.status(HttpStatus.OK).body(KayttajaSuccessResponse(kieli.get, onOrganisaationKatselija)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[KayttajaResponse]])
    catch
      case e: Exception =>
        LOG.error("Käyttäjän tietojen haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KayttajaFailureResponse(java.util.Set.of(UI_KAYTTAJAN_TIEDOT_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_OPPILAITOKSET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa käyttöliittymän henkilöhaun vuosirajainta varten listan valittavissa olevista oppilaitoksista",
    description = "Organisaation oppilaiden tietojen tarkastelu perustuu oppilaitos- ja vuosirajaimeen. Tämä rajapinta palauttaa " +
      "oppilaitosrajaimen tarvitseman listan oppilaitoksista joiden opiskelijoiden tietoihin käyttäjällä on oikeus. Pääsy on " +
      "on rajoitettu rekisterinpitäjiin ja organisaation katselijoihin.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää oppilaitokset joiden opiskelijatietoihin käyttäjällä on oikeus", content = Array(new Content(schema = new Schema(implementation = classOf[OppilaitosSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppilaitosFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppilaitosFailureResponse])))),
    ))
  def haeOppilaitokset(request: HttpServletRequest): ResponseEntity[OppilaitosResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_OPPILAITOKSET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if(securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onOrganisaationKatselija())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(UI_OPPILAITOKSET_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            LOG.info(s"Haetaan käyttöliittymälle lista oppilaitoksista")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map.empty, AuditOperation.HaeOppilaitoksetUI, None)

            if(securityOperaatiot.onRekisterinpitaja())
              Right(ResponseEntity.status(HttpStatus.OK).body(OppilaitosSuccessResponse(uiService.haeKaikkiOppilaitoksetJoissaPKSuorituksia().toList.asJava)))
            else
              val virkailijaAuth = securityOperaatiot.getAuthorization(SecurityConstants.ROOLIT_OPPIJA_HAULLE, organisaatioProvider)
              val oppilaitokset = uiService.haeOppilaitoksetJoihinOikeudet(virkailijaAuth.oikeudellisetOrganisaatiot)
              Right(ResponseEntity.status(HttpStatus.OK).body(OppilaitosSuccessResponse(oppilaitokset.toList.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppilaitosResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppilaitosten haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppilaitosFailureResponse(java.util.Set.of(UI_RAJAIMEN_TIEDOT_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_VUODET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa käyttöliittymän henkilöhaun vuosirajainta varten listan valittavissa olevista vuosista",
    description = "Organisaation oppilaiden tietojen tarkastelu perustuu oppilaitos- ja vuosirajaimeen. Tämä rajapinta palauttaa " +
      "vuosirajaimen tarvitseman listan niistä vuosista joille on tallennettu oppilaiden tietoja valitussa oppilaitoksessa. Pääsy " +
      "on rajoitettu rekisterinpitäjiin ja organisaation katselijoihin.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa vuodet joille on seurattavia oppijoita valitussa oppilaitoksessa", content = Array(new Content(schema = new Schema(implementation = classOf[VuodetSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[VuodetFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[VuodetFailureResponse])))),
    ))
  def haeVuodet(@PathVariable(UI_VUODET_OPPILAITOS_PARAM_NAME)  @Parameter(description = "oppilaitoksen tunniste", example = ESIMERKKI_OPPILAITOS_OID, required = true) oppilaitosOid: Optional[String],
                request: HttpServletRequest): ResponseEntity[VuodetResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_VUODET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet, ei rekisterinpitäjällä pitää olla organisaation katselija-oikeus valittuun oppilaitokseen
            if(securityOperaatiot.onRekisterinpitaja() || oppilaitosOid.toScala.exists(oid => securityOperaatiot.getOrganisaatiotOikeuksille(Set(SecurityConstants.SECURITY_ROOLI_ORGANISAATION_KATSELIJA)).contains(oid)))
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(VuodetFailureResponse(java.util.Set.of(UI_VUODET_EI_OIKEUKSIA)))))
          .flatMap(suoritus =>
            // validoidaan parametri
            val virheet: Set[String] = UIValidator.validateOppilaitosOid(oppilaitosOid.toScala, true)

            if(virheet.isEmpty)
              Right(suoritus)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VuodetFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            LOG.info(s"Haetaan käyttöliittymälle lista vuosista")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_LUOKAT_OPPILAITOS_PARAM_NAME -> oppilaitosOid.get), AuditOperation.HaeVuodetUI, None)

            val vuodet = uiService.haeVuodet(oppilaitosOid.get)
            Right(ResponseEntity.status(HttpStatus.OK).body(VuodetSuccessResponse(vuodet.toList.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[VuodetResponse]])
    catch
      case e: Exception =>
        LOG.error("Vuosien haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(VuodetFailureResponse(java.util.Set.of(UI_RAJAIMEN_TIEDOT_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_LUOKAT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa käyttöliittymän henkilöhaun vuosirajainta varten listan valittavissa olevista luokista",
    description = "Organisaation oppilaiden tietojen tarkastelu perustuu oppilaitos- ja vuosirajaimeen. Tämä rajapinta palauttaa " +
      "luokkarajaimen tarvitseman listan niistä luokista joille on tallennettu oppilaiden tietoja valitussa oppilaitoksessa " +
      "valittuna vuonna. Pääsy on rajoitettu rekisterinpitäjiin ja organisaation katselijoihin.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa vuodet luokat on seurattavia oppijoita valitussa oppilaitoksessa valittuna vuonna", content = Array(new Content(schema = new Schema(implementation = classOf[LuokatSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuokatFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuokatFailureResponse])))),
    ))
  def haeLuokat(@PathVariable(UI_LUOKAT_OPPILAITOS_PARAM_NAME)  @Parameter(description = "oppilaitoksen tunniste", example = ESIMERKKI_OPPILAITOS_OID, required = true) oppilaitosOid: Optional[String],
                @PathVariable(UI_LUOKAT_VUOSI_PARAM_NAME)  @Parameter(description = "vuosi", example = ESIMERKKI_OPPILAITOS_OID, required = true) vuosi: Optional[String],
                request: HttpServletRequest): ResponseEntity[LuokatResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_LUOKAT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet, ei rekisterinpitäjällä pitää olla organisaation katselija-oikeus valittuun oppilaitokseen
            if(securityOperaatiot.onRekisterinpitaja() || oppilaitosOid.toScala.exists(oid => securityOperaatiot.getOrganisaatiotOikeuksille(SecurityConstants.ROOLIT_OPPIJA_HAULLE).contains(oid)))
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(LuokatFailureResponse(java.util.Set.of(UI_OPPILAITOKSET_EI_OIKEUKSIA)))))
          .flatMap(suoritus =>
            // validoidaan
            val virheet: Set[String] = Set(
              UIValidator.validateOppilaitosOid(oppilaitosOid.toScala, true),
              UIValidator.validateVuosi(vuosi.toScala, true)
            ).flatten
            if(virheet.isEmpty)
              Right(suoritus)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuokatFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            LOG.info(s"Haetaan käyttöliittymälle lista luokista")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(
              UI_LUOKAT_OPPILAITOS_PARAM_NAME -> oppilaitosOid.get,
              UI_LUOKAT_VUOSI_PARAM_NAME -> vuosi.get
            ), AuditOperation.HaeLuokatUI, None)

            val luokat = uiService.haeLuokat(oppilaitosOid.get, vuosi.get.toInt)
            Right(ResponseEntity.status(HttpStatus.OK).body(LuokatSuccessResponse(luokat.toList.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuokatResponse]])
    catch
      case e: Exception =>
        LOG.error("Luokkien haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuokatFailureResponse(java.util.Set.of(UI_RAJAIMEN_TIEDOT_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_HENKILO_HAKU_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa käyttöliittymälle listausnäkymään oppijan ja duplikaatit hakusanan (oid tai hetu) perusteella",
    description = "Tämä rajapinta palauttaa oidin tai hetun perusteella listan oppijoita listausnäkymää varten. Pääsy on sallittu " +
      "kaikille UI-käyttäjillw (rekisterinpitäjä, organisaation katselija, hakijoiden katselija)",
    parameters = Array(
      new Parameter(name = UI_HENKILO_HAKU_TUNNISTE_PARAM_NAME, in = ParameterIn.QUERY)
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää hakukriteerin perusteellä löytyneet oppijat", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuFailureResponse]))))
    ))
  def haeOppija(@RequestParam(name = UI_HENKILO_HAKU_TUNNISTE_PARAM_NAME, required = false) @Parameter(description = "tunniste (oppijanumero tai hetu)", example = ESIMERKKI_OPPIJANUMERO) tunniste: Optional[String],
                request: HttpServletRequest): ResponseEntity[OppijanHakuResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_HENKILO_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            val virheet: Set[String] = UIValidator.validateTunniste(tunniste.toScala)
            if(virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanHakuFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if(!securityOperaatiot.onUIKayttaja())
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(UI_HAKU_EI_OIKEUKSIA))))
            else
              Right(None))
          .flatMap(_ =>
            LOG.info(s"Haetaan käyttöliittymälle lista oppijoista")
            AuditLog.log(AuditLog.getUser(request), Map(
              UI_HENKILO_HAKU_TUNNISTE_PARAM_NAME -> tunniste.orElse(null),
            ), AuditOperation.HaeOppijatUI, None)
            val oppijat = uiService.haeOppija(tunniste.get)
            Right(ResponseEntity.status(HttpStatus.OK).body(OppijanHakuSuccessResponse(oppijat.toList.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppijanHakuResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijalistauksen haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppijanHakuFailureResponse(java.util.Set.of(UI_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_OPPILAITOS_HAKU_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa käyttöliittymälle listausnäkymään oppijan ja duplikaatit rajaimen arvojen perusteella",
    description = "Tämä rajapinta palauttaa rajaimen arvojen (oppilaitostunniste, vuosi, mahdollinen luokka) perusteella " +
      "listan henkilöitä listausnäkymää varten. Pääsy on sallittu rekisterinpitäjille ja organisaation katselijoille.",
    parameters = Array(
      new Parameter(name = UI_OPPILAITOS_HAKU_OPPILAITOS_PARAM_NAME, in = ParameterIn.QUERY),
      new Parameter(name = UI_OPPILAITOS_HAKU_VUOSI_PARAM_NAME, in = ParameterIn.QUERY),
      new Parameter(name = UI_OPPILAITOS_HAKU_LUOKKA_PARAM_NAME, in = ParameterIn.QUERY),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää hakukriteereiden perusteellä löytyneet oppilaitoksen oppijat", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuFailureResponse])))),
    ))
  def haeOppilaitoksenOppijat(
                  @RequestParam(name = UI_OPPILAITOS_HAKU_OPPILAITOS_PARAM_NAME, required = false) @Parameter(description = "oppilaitoksen tunniste", example = ESIMERKKI_OPPILAITOS_OID) oppilaitos: Optional[String],
                  @RequestParam(name = UI_OPPILAITOS_HAKU_VUOSI_PARAM_NAME, required = false) @Parameter(description = "vuosi", example = ESIMERKKI_VUOSI) vuosi: Optional[String],
                  @RequestParam(name = UI_OPPILAITOS_HAKU_LUOKKA_PARAM_NAME, required = false) @Parameter(description = "luokka", example = ESIMERKKI_LUOKKA) luokka: Optional[String],
                  request: HttpServletRequest): ResponseEntity[OppijanHakuResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_OPPILAITOS_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet, täytyy olla joka rekisterinpitäjä tai vaihtoehtoisesti organisaation katselija valitussa oppilaitoksessa
            val virkailijaAuth = securityOperaatiot.getAuthorization(Set(SecurityConstants.SECURITY_ROOLI_ORGANISAATION_KATSELIJA), organisaatioProvider)
            if(!securityOperaatiot.onRekisterinpitaja() && oppilaitos.toScala.exists(oppilaitos => !virkailijaAuth.oikeudellisetOrganisaatiot.contains(oppilaitos)))
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(UI_HAKU_EI_OIKEUKSIA))))
            else
              Right(virkailijaAuth))
          .flatMap(_ =>
            // validoidaan parametrit
            val virheet: Set[String] = Set(Set((oppilaitos.toScala, vuosi.toScala, luokka.toScala) match
              case (None, _, _) => Some(UI_OPPILAITOS_HAKU_OPPILAITOS_PAKOLLINEN)
              case (_, None, _) => Some(UI_OPPILAITOS_HAKU_VUOSI_PAKOLLINEN)
              case default => None).flatten,
              UIValidator.validateOppilaitosOid(oppilaitos.toScala, pakollinen = false),
              UIValidator.validateVuosi(vuosi.toScala, pakollinen = false),
              UIValidator.validateLuokka(luokka.toScala, pakollinen = false)
            ).flatten
            if(virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanHakuFailureResponse(virheet.asJava))))
          .flatMap(virkailijaAuth =>
            LOG.info(s"Haetaan käyttöliittymälle lista oppijoista")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(
              UI_OPPILAITOS_HAKU_OPPILAITOS_PARAM_NAME -> oppilaitos.orElse(null),
              UI_OPPILAITOS_HAKU_VUOSI_PARAM_NAME -> vuosi.orElse(null),
              UI_OPPILAITOS_HAKU_LUOKKA_PARAM_NAME -> luokka.orElse(null),
            ), AuditOperation.HaeOppilaitoksenOppijatUI, None)
            val oppijat = uiService.haePKOppijat(oppilaitos.get, vuosi.get.toInt, luokka.toScala)
            Right(ResponseEntity.status(HttpStatus.OK).body(OppijanHakuSuccessResponse(oppijat.toList.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppijanHakuResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijalistauksen haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppijanHakuFailureResponse(java.util.Set.of(UI_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_TIEDOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa yksittäisen oppijan suoritustiedot käyttöliittymälle",
    description = "Tämä rajapinta palauttaa yksittäisen oppijan suoritustiedot käyttöliittymän suoritustietonäkymää varten. " +
      "Pääsy on sallitu rekisterinpitäjille ja organisaation katselijoille.",
    parameters = Array(new Parameter(name = UI_TIEDOT_OPPIJANUMERO_PARAM_NAME, in = ParameterIn.PATH)),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää yksittäisen oppijan tiedot", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanTiedotSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanTiedotFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
  ))
  def haeOppijanTiedot(@PathVariable(UI_TIEDOT_OPPIJANUMERO_PARAM_NAME)  @Parameter(description = "Oppijanumero", example = ESIMERKKI_OPPIJANUMERO, required = true) oppijaNumero: Optional[String],
                       request: HttpServletRequest): ResponseEntity[OppijanTiedotResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TIEDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan että käyttäjällä on hakijoiden katselija oikeudet
            if(securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onOrganisaationKatselija())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanTiedotFailureResponse(java.util.Set.of(UI_TIEDOT_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            // validoidaan parametri
            val virheet = UIValidator.validateOppijanumero(oppijaNumero.toScala, pakollinen = true)
            if(virheet.isEmpty)
              Right(oppijaNumero.get)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanTiedotFailureResponse(virheet.asJava))))
          .flatMap(oppijaNumero =>
            // tarkastetaan oikeudet haetulle oppijanumerolle
            if(this.uiService.hasOppijanKatseluOikeus(oppijaNumero))
              Right(oppijaNumero)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanTiedotFailureResponse(java.util.Set.of(UI_TIEDOT_EI_OIKEUKSIA)))))
          .flatMap(oppijaNumero =>
            LOG.info(s"Haetaan käyttöliittymälle tiedot oppijasta ${oppijaNumero}")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_TIEDOT_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), AuditOperation.HaeOppijaTiedotUI, None)
            val suoritukset = uiService.haeAliakset(oppijaNumero).flatMap(oid => this.kantaOperaatiot.haeSuoritukset(oppijaNumero).values.toSet.flatten)
            val oppijanTiedot = EntityToUIConverter.getOppijanTiedot(oppijaNumero, suoritukset, organisaatioProvider, koodistoProvider)
            if(oppijanTiedot.isEmpty)
              Left(ResponseEntity.status(HttpStatus.GONE).body(""))
            else
              Right(ResponseEntity.status(HttpStatus.OK).body(oppijanTiedot))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppijanTiedotResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijan suoritustietojen haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppijanTiedotFailureResponse(java.util.Set.of(UI_TIEDOT_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_TALLENNA_SUORITUS_OPPILAITOKSET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa käyttöliittymälle listan oppilaitoksista joille voi lisätä käsin syötettäviä suorituksia.",
    description = "Rekisterinpitäjän on käyttöliittymässä mahdollista lisätä perusopetuksen oppimäärän ja perusopetuksen " +
      "oppiaineen oppimäärän suorituksia henkilöille. Tämä rajapinta palauttaa listan oppilaitoksista joihin suorituksia " +
      "voi lisätä. Pääsy rajapintaan on rajattu rekisterinpitäjille.",
    parameters = Array(new Parameter(name = UI_TIEDOT_OPPIJANUMERO_PARAM_NAME, in = ParameterIn.PATH)),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää listan oppilaitoksista", content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusOppilaitoksetSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusOppilaitoksetFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusOppilaitoksetFailureResponse]))))
    ))
  def haeSyotettavatOppilaitokset(request: HttpServletRequest): ResponseEntity[LuoSuoritusOppilaitoksetResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_SUORITUS_OPPILAITOKSET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            val securityOperaatiot = new SecurityOperaatiot
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(LuoSuoritusOppilaitoksetFailureResponse(java.util.Set.of(UI_TIEDOT_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            LOG.info(s"Haetaan käyttöliittymälle lista syötettävien suoritusten oppilaitoksista")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map.empty, AuditOperation.HaeOppijaTiedotUI, None)
            Right(ResponseEntity.status(HttpStatus.OK).body(LuoSuoritusOppilaitoksetSuccessResponse(uiService.haeSyotettavienSuoritustenOppilaitokset().asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuoSuoritusOppilaitoksetResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppilaitoslistauksen haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoSuoritusOppilaitoksetFailureResponse(java.util.Set.of(UI_TIEDOT_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_TALLENNA_SUORITUS_VAIHTOEHDOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa käyttöliittymälle alasvetovalikoiden sisällön käsin syötettävien suoritusten lisäämistä varten.",
    description = "Rekisterinpitäjän on käyttöliittymässä mahdollista lisätä perusopetuksen oppimäärän ja perusopetuksen " +
      "oppiaineen oppimäärän suorituksia henkilöille. Tämä rajapinta palauttaa suoritusten lisäämiskäyttöliittymän " +
      "alasvetovalikoiden (muut kuin oppilaitos) mahdolliset arvot. Pääsy rajapintaan on rajattu rekisterinpitäjille.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää alasvetovalikoiden arvot syötettyjen suoritusten lisäämista varten", content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusDropdownDataResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusDropdownDataFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusDropdownDataFailureResponse])))),
    ))
  def haeSyotettavienSuoritustenVaihtoehdot(request: HttpServletRequest): ResponseEntity[LuoSuoritusDropdownDataResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_SUORITUS_VAIHTOEHDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            val securityOperaatiot = new SecurityOperaatiot
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(LuoSuoritusDropdownDataFailureResponse(java.util.Set.of(UI_TIEDOT_EI_OIKEUKSIA)))))
          .flatMap(_ => {
            LOG.info(s"Haetaan käyttöliittymälle alasvetovalikoiden arvot suoritusten syöttämistä varten")

            val suoritustilat         = koodistoProvider.haeKoodisto(KOODISTO_SUORITUKSENTILAT)
            val suoritustyypit        = koodistoProvider.haeKoodisto(KOODISTO_SUORITUKSENTYYPIT)
            val oppiaineet            = koodistoProvider.haeKoodisto(KOODISTO_OPPIAINEET)
            val suorituskielet        = koodistoProvider.haeKoodisto(KOODISTO_SUORITUSKIELET)
            val aidinkielenOppimaarat = koodistoProvider.haeKoodisto(KOODISTO_OPPIAINE_AIDINKIELI_JA_KIRJALLISUUS)
            val kielivalikoima        = koodistoProvider.haeKoodisto(KOODISTO_KIELIVALIKOIMA)
            val pohjakoulutus         = koodistoProvider.haeKoodisto(KOODISTO_POHJAKOULUTUS)

            Right(ResponseEntity.status(HttpStatus.OK).body(LuoSuoritusDropdownDataSuccessResponse(
            SYOTETTAVAT_SUORITUSTILAT.map(t => {
                val suoritustilaKoodi = suoritustilat(t)
                SyotettavaSuoritusTilaVaihtoehto(
                  arvo = t,
                  nimi = SyotettavaSuoritusTilaVaihtoehtoNimi(
                    suoritustilaKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(m => m.nimi).toJava,
                    suoritustilaKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(m => m.nimi).toJava,
                    suoritustilaKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(m => m.nimi).toJava,
                  )
                )
              }).asJava,
              SYOTETTAVAT_SUORITUSTYYPIT.map(t => {
                val suoritustyyppiKoodi = suoritustyypit(t)
                SyotettavaSuoritusTyyppiVaihtoehto(
                  arvo = t,
                  nimi = SyotettavaSuoritusTyyppiVaihtoehtoNimi(
                    suoritustyyppiKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(m => m.nimi).toJava,
                    suoritustyyppiKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(m => m.nimi).toJava,
                    suoritustyyppiKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(m => m.nimi).toJava,
                  )
                )
              }).asJava,
              SYOTETTAVAT_OPPIAINEET.map(oa => {
                val oppiaineKoodi = oppiaineet(oa)
                SyotettavaOppiaineVaihtoehto(
                  arvo = oa,
                  nimi = SyotettavaOppiaineVaihtoehtoNimi(
                    oppiaineKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(m => m.nimi).toJava,
                    oppiaineKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(m => m.nimi).toJava,
                    oppiaineKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(m => m.nimi).toJava,
                  ),
                  isKieli = SYOTETYN_OPPIMAARAN_KIELIAINEKOODIT.contains(oa),
                  isAidinkieli = "AI".equals(oa)
              )}).asJava,
              SYOTETYN_OPPIMAARAN_SUORITUSKIELET.map(k => {
                val suorituskieliKoodi = suorituskielet(k)
                SyotettavaSuoritusKieliVaihtoehto(
                  arvo = k,
                  nimi = SyotettavaSuoritusKieliVaihtoehtoNimi(
                    suorituskieliKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(m => m.nimi).toJava,
                    suorituskieliKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(m => m.nimi).toJava,
                    suorituskieliKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(m => m.nimi).toJava,
                  )
              )}).asJava,
              aidinkielenOppimaarat.values.toList.sortBy(k => {
                try
                  k.koodiArvo.substring(2).toInt
                catch
                  case e: Exception => 100
              }).map(k =>
                SyotettavaAidinkielenOppimaaraVaihtoehto(
                  arvo = k.koodiArvo,
                  nimi = SyotettavaAidinkielenOppimaaraVaihtoehtoNimi(
                    k.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(m => m.nimi).toJava,
                    k.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(m => m.nimi).toJava,
                    k.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(m => m.nimi).toJava,
                  )
                )).asJava,
              kielivalikoima.values.map(k =>
                SyotettavaVierasKieliVaihtoehto(
                  arvo = k.koodiArvo,
                  nimi = SyotettavaVierasKieliVaihtoehtoNimi(
                    k.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(m => m.nimi).toJava,
                    k.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(m => m.nimi).toJava,
                    k.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(m => m.nimi).toJava,
                  )
                )).toList.asJava,
              SYOTETYN_OPPIMAARAN_YKSILOLLISTAMINEN.map(a => {
                val pohjakoulutusKoodi = pohjakoulutus(a.toString)
                SyotettavaYksilollistamisVaihtoehto(
                  arvo = a,
                  nimi = SyotettavaYksilollistamisVaihtoehtoNimi(
                    pohjakoulutusKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(m => m.nimi).toJava,
                    pohjakoulutusKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(m => m.nimi).toJava,
                    pohjakoulutusKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(m => m.nimi).toJava,
                  )
                )
              }).asJava,
              Range.inclusive(4, 10).map(a => SyotettavaArvosanaVaihtoehto(
                arvo = a
              )).toList.asJava
            )))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuoSuoritusDropdownDataResponse]])
    catch
      case e: Exception =>
        LOG.error("Suoritusten syöttämisen alasvetovalikoiden tietojen haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoSuoritusDropdownDataFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_VAIHTOEHDOT_ESIMERKKI_VIRHE)))

  @PostMapping(
    path = Array(UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Tallentaa perusopetuksen oppimäärän suorituksen yksittäiselle oppijalle",
    description = "Rekisterinpitäjän on käyttöliittymässä mahdollista lisätä perusopetuksen oppimäärän suorituksia henkilöille, " +
      "ja muuttaa lisättyjä suorituksia. Tämä rajapinta tallentaa lisäykset/muutokset. Pääsy rajapintaan on rajattu rekisterinpitäjille.",
    requestBody =
      new io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = Array(new Content(schema = new Schema(implementation = classOf[SyotettyPerusopetuksenOppimaaranSuoritus])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description="Tallennus onnistunut", content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppimaaraSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppimaaraFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppimaaraFailureResponse]))))
    ))
  def tallennaPerusopetuksenOppimaaranSuoritus(@RequestBody @Parameter(description = "Tallennettava suoritus", required = true) suoritusBytes: Array[Byte],
                                               request: HttpServletRequest): ResponseEntity[LuoPerusopetuksenOppimaaraResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_EI_OIKEUKSIA), List.empty.asJava))))
          .flatMap(_ =>
            // deserialisoidaan
            try
              Right(objectMapper.readValue(suoritusBytes, classOf[SyotettyPerusopetuksenOppimaaranSuoritus]))
            catch
              case e: Exception =>
                LOG.error("Perusopetuksen oppimaaran suorituksen deserialisointi epäonnistui")
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_JSON_VIRHE), List.empty.asJava))))
          .flatMap(suoritus =>
            // validoidaan tallennettava suoritus
            val yleisetVirheet = UIValidator.validatePerusopetuksenOppimaaranYleisetKentat(suoritus, koodistoProvider)
            val oppiaineKohtaisetVirheet = UIValidator.validatePerusopetuksenOppimaaranYksittaisetOppiaineet(suoritus.oppiaineet, koodistoProvider)

            if(yleisetVirheet.isEmpty && oppiaineKohtaisetVirheet.isEmpty)
              Right(suoritus)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppimaaraFailureResponse(
                yleisetVirheet.asJava,
                oppiaineKohtaisetVirheet.map((oppiaineKoodi, virheet) => LuoPerusopetuksenOppimaaraFailureResponseOppiaineVirhe(oppiaineKoodi, virheet.asJava)).toList.asJava
              ))))
          .flatMap(suoritus =>
            // varmistetaan että henkilö löytyy
            if(Await.result(onrIntegration.henkiloExists(suoritus.oppijaOid.get), ONR_TIMEOUT))
              Right(suoritus)
            else
              LOG.error(s"Perusopetuksen oppimaaran suorituksen tallennus oppijalle ${suoritus.oppijaOid.get} epäonnistui, henkilöä ei löydy ONR:stä")
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA), List.empty.asJava))))
          .flatMap(suoritus =>
            LOG.info(s"Tallennetaan perusopetuksen oppimaaran suoritus oppijalle ${suoritus.oppijaOid}")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_TIEDOT_OPPIJANUMERO_PARAM_NAME -> suoritus.oppijaOid.get()), AuditOperation.TallennaPerusopetuksenOppimaaranSuoritus, Some(suoritus))
            val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(suoritus.oppijaOid.get(), SuoritusJoukko.SYOTETTY_PERUSOPETUS, Seq(objectMapper.writeValueAsString(suoritus)), Instant.now())

            if(versio.isEmpty)
              LOG.info(s"Tallennettava perusopetuksen oppimaaran suoritus oppijalle ${suoritus.oppijaOid} ei sisältänyt muutoksia aikaisempaan versioon verrattuna")
            else
              val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(VirkailijaToSuoritusConverter.toPerusopetuksenOppimaara(versio.get.tunniste, suoritus, koodistoProvider, organisaatioProvider))
              this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getMetadata(opiskeluoikeudet.toSeq))

            Right(ResponseEntity.status(HttpStatus.OK).body(LuoPerusopetuksenOppimaaraSuccessResponse())))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuoPerusopetuksenOppimaaraResponse]]
    catch
      case e: Exception =>
        LOG.error("Perusopetuksen oppimaaran tallentaminen oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_TALLENNUS_VIRHE), List.empty.asJava))

  @PostMapping(
    path = Array(UI_TALLENNA_SUORITUS_OPPIAINE_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Tallentaa perusopetuksen oppiaineen oppimäärän suorituksen yksittäiselle oppijalle",
    description = "Rekisterinpitäjän on käyttöliittymässä mahdollista lisätä perusopetuksen oppiaineen oppimäärän suorituksia henkilöille, " +
      "ja muuttaa lisättyjä suorituksia. Tämä rajapinta tallentaa lisäykset/muutokset. Pääsy rajapintaan on rajattu rekisterinpitäjille.",
    requestBody =
      new io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = Array(new Content(schema = new Schema(implementation = classOf[SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description="Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppiaineenOppimaaraSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppiaineenOppimaaraFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppiaineenOppimaaraFailureResponse]))))
    ))
  def tallennaPerusopetuksenOppiaineenOppimaaraSuoritus(@RequestBody @Parameter(description = "Tallennettava suoritus", required = true) suoritusBytes: Array[Byte],
                                                        request: HttpServletRequest): ResponseEntity[LuoPerusopetuksenOppiaineenOppimaaraResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_SUORITUS_OPPIAINE_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_OPPIAINE_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            // deserialisoidaan
            try
              Right(objectMapper.readValue(suoritusBytes, classOf[SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus]))
            catch
              case e: Exception =>
                LOG.error("Perusopetuksen oppiaineen oppimaaran suorituksen deserialisointi epäonnistui")
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_OPPIAINE_JSON_VIRHE)))))
          .flatMap(suoritus =>
            // validoidaan tallennettava suoritus
            val virheet: Set[String] = UIValidator.validatePerusopetuksenOppiaineenOppimaara(suoritus, koodistoProvider)
            if(virheet.isEmpty)
              Right(suoritus)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(virheet.asJava))))
          .flatMap(suoritus =>
            // varmistetaan että henkilö löytyy
            if(Await.result(onrIntegration.henkiloExists(suoritus.oppijaOid.get), ONR_TIMEOUT))
              Right(suoritus)
            else
              LOG.error(s"Perusopetuksen oppiaineen oppimaaran suorituksen tallennus oppijalle ${suoritus.oppijaOid.get} epäonnistui, henkilöä ei löydy ONR:stä")
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_OPPIAINE_TUNTEMATON_OPPIJA)))))
          .flatMap(suoritus =>
            LOG.info(s"Tallennetaan perusopetuksen oppiaineen oppimaaran suoritus oppijalle ${suoritus.oppijaOid}")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_TIEDOT_OPPIJANUMERO_PARAM_NAME -> suoritus.oppijaOid.get()), AuditOperation.TallennaPerusopetuksenOppiaineenOppimaaranSuoritus, Some(suoritus))
            val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(suoritus.oppijaOid.get(), SuoritusJoukko.SYOTETTY_PERUSOPETUS, Seq(objectMapper.writeValueAsString(suoritus)), Instant.now())

            if(versio.isEmpty)
              LOG.info(s"Tallennettava perusopetuksen oppiaineen oppimaaran suoritus oppijalle ${suoritus.oppijaOid} ei sisältänyt muutoksia aikaisempaan versioon verrattuna")
            else {
              val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(VirkailijaToSuoritusConverter.toPerusopetuksenOppiaineenOppimaara(versio.get.tunniste, suoritus, koodistoProvider, organisaatioProvider))
              this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getMetadata(opiskeluoikeudet.toSeq))
            }
            Right(ResponseEntity.status(HttpStatus.OK).body(LuoPerusopetuksenOppiaineenOppimaaraSuccessResponse())))
      )
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuoPerusopetuksenOppiaineenOppimaaraResponse]]
    catch
      case e: Exception =>
        LOG.error("Perusopetuksen oppimaaran tallentaminen oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_OPPIAINE_TALLENNUS_VIRHE)))

  @DeleteMapping(
    path = Array(UI_POISTA_SUORITUS_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Poistaa käsin syötetyn perusopetuksen oppimäärään tai oppiaineen oppimäärän suorituksen.",
    description = "Rekisterinpitäjän on käyttöliittymässä mahdollista lisätä perusopetuksen oppimäärän ja oppiaineen oppimäärän suorituksia " +
      "henkilöille, ja poistaa lisättyjä suorituksia. Tämä rajapinta poistaa lisätyn suorituksen. Pääsy rajapintaan on rajattu rekisterinpitäjille.",
    requestBody =
      new io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = Array(new Content(schema = new Schema(implementation = classOf[SyotettyPerusopetuksenOppimaaranSuoritus])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description="Suoritus poistettu", content = Array(new Content(schema = new Schema(implementation = classOf[PoistaSuoritusSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[PoistaSuoritusFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[PoistaSuoritusFailureResponse]))))
    ))
  def poistaSuoritus(@PathVariable(UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME)  @Parameter(description = "Poistettavan version tunniste", example = ESIMERKKI_VERSIOTUNNISTE, required = true) versioTunniste: Optional[String],
                     request: HttpServletRequest): ResponseEntity[PoistaSuoritusResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_POISTA_SUORITUS_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            // validoidaan version tunniste
            val virheet = UIValidator.validateVersioTunniste(versioTunniste.toScala)
            if(virheet.isEmpty)
              Right(UUID.fromString(versioTunniste.get()))
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PoistaSuoritusFailureResponse(virheet.asJava))))
          .flatMap(versioTunniste =>
            // varmistetaan että versio olemassa
            val versio = this.kantaOperaatiot.haeVersio(versioTunniste)
            if(versio.isEmpty)
              Left(ResponseEntity.status(HttpStatus.GONE).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_SUORITUSTA_EI_LOYTYNYT))))
            else
              versio.get.suoritusJoukko match
                // ja että se on poistettavissa (ts. käsin syötetty)
                case SuoritusJoukko.SYOTETTY_PERUSOPETUS => Right(versio.get)
                case SuoritusJoukko.SYOTETTY_OPPIAINE => Right(versio.get)
                case default =>
                  LOG.error(s"Yritettiin poistaa versiota ${versio.get.tunniste} joka joka ei ole käsin syötetty suoritus")
                  Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_SUORITUSTA_EI_POISTETTAVISSA)))))
          .flatMap(versio =>
            LOG.info(s"Poistetaan suorituksen versio ${versio.tunniste} henkilöltä ${versio.oppijaNumero}")
            val user = AuditLog.getUser(request)
            versio.suoritusJoukko match
              case SuoritusJoukko.SYOTETTY_PERUSOPETUS => AuditLog.log(user, Map(UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME -> versio.tunniste.toString), AuditOperation.PoistaPerusopetuksenOppimaaranSuoritus, None)
              case SuoritusJoukko.SYOTETTY_OPPIAINE => AuditLog.log(user, Map(UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME -> versio.tunniste.toString), AuditOperation.PoistaPerusopetuksenOppiaineenOppimaaranSuoritus, None)
            if(!this.kantaOperaatiot.paataVersionVoimassaolo(versio.tunniste)) {
              // versio oli jo poistettu
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_SUORITUS_EI_VOIMASSA))))
            } else
              Right(ResponseEntity.status(HttpStatus.OK).body(PoistaSuoritusSuccessResponse()))))
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[PoistaSuoritusResponse]]
    catch
      case e: Exception =>
        LOG.error("Tallennetun suorituksen poistaminen oppijalta epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_TALLENNUS_VIRHE)))

  @GetMapping(
    path = Array(UI_VALINTADATA_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa yksittäisen oppijan valintoja varten parsitut avain-arvot",
    description = "Käyttöliittymässä on mahdollista tarkastella hakija- ja hakukohtaista valintalaskennalle menevää avain-" +
      "arvopareiksi jalostettua dataa. Tämä rajapinta palauttaa nämä avain-arvoparit. Pääsy on (ainakin toistaiseksi) " +
      "rajattu rekisterinpitäjiin.",
    parameters = Array(new Parameter(name = UI_TIEDOT_OPPIJANUMERO_PARAM_NAME, in = ParameterIn.PATH)),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää yksittäisen oppijan tiedot", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanValintaDataSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanValintaDataFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanValintaDataFailureResponse])))),
      new ApiResponse(responseCode = "500", description = UI_500_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanValintaDataFailureResponse]))))
    ))
  def haeValintaData(@RequestParam(name = UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, required = true) @Parameter(description = "henkilönumero", example = ESIMERKKI_OPPIJANUMERO) oppijaNumero: Optional[String],
                     @RequestParam(name = UI_VALINTADATA_HAKU_PARAM_NAME, required = false) @Parameter(description = "haun tunniste", example = ESIMERKKI_HAKU_OID) hakuOid: Optional[String],
                     request: HttpServletRequest): ResponseEntity[OppijanTiedotResponse] = {
    try {
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TIEDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanTiedotFailureResponse(java.util.Set.of(UI_TIEDOT_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            // validoidaan parametrit
            val virheet: Set[String] = Set(
              UIValidator.validateOppijanumero(oppijaNumero.toScala, pakollinen = true),
              UIValidator.validateHakuOid(hakuOid.toScala, pakollinen = false)
            ).flatten
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanValintaDataFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME -> oppijaNumero.orElse(null), UI_VALINTADATA_HAKU_PARAM_NAME -> hakuOid.orElse(null)), AuditOperation.HaeOppijaValintaDataUI, None)
            val data = valintaDataService.fetchValintaDataForOppija(oppijaNumero.get, hakuOid.toScala)
            val oppijanValintaData: OppijanValintaDataSuccessResponse = EntityToUIConverter.getOppijanValintaDataForUI(oppijaNumero.get(), hakuOid.toScala, data)
            Right(ResponseEntity.status(HttpStatus.OK).body(oppijanValintaData))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppijanTiedotResponse]])
    } catch {
      case e: UseitaVahvistettujaOppimaariaException =>
        LOG.error("Virhe muodostettaessa avain-arvoja: ", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppijanValintaDataFailureResponse(java.util.Set.of(UI_VALINTADATA_USEITA_VAHVISTETTUJA_OPPIMAARIA)))
      case e: Exception =>
        LOG.error("Oppijan valintoja varten muodostettujen avain-arvojen haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppijanValintaDataFailureResponse(java.util.Set.of(UI_VALINTADATA_GENEERINEN_BACKEND_VIRHE)))
    }
  }

  @PostMapping(
    path = Array(UI_TALLENNA_YLIAJOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Tallentaa listan yliajoja oppijan avain-arvoihin haulle",
    description = "Valintoja varten tuotettavia yksittäisiä avain-arvopareja on mahdollista yliajaa hakukohtaisesti. Tämä " +
      "rajapinta tallentaa joukon yliajoja ja niihin liittyviä selitteitä. Pääsy on (ainakin toistaiseksi) rajattu " +
      "rekisterinpitäjiin.",
    requestBody =
      new io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = Array(new Content(schema = new Schema(implementation = classOf[YliajoTallennusContainer])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[TallennaYliajotOppijalleSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[TallennaYliajotOppijalleFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def tallennaYliajoOppijalle(
                                           @RequestBody @Parameter(description = "Suoritukset", required = true) bodyBytes: Array[Byte],
                                           request: HttpServletRequest): ResponseEntity[TallennaYliajotOppijalleResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_EI_OIKEUKSIA), List.empty.asJava))))
          .flatMap(_ =>
            // deserialisoidaan
            try
              Right(objectMapper.readValue(bodyBytes, classOf[YliajoTallennusContainer]))
            catch
              case e: Exception =>
                LOG.error("Yliajojen deserialisointi epäonnistui")
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_JSON_VIRHE), List.empty.asJava))))
          .flatMap((yliajot: YliajoTallennusContainer) =>
            // validoidaan
            val virheet: Set[String] = UIValidator.validateYliajot(yliajot)
            if (virheet.isEmpty)
              Right(yliajot)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TallennaYliajotOppijalleFailureResponse(
                virheet.asJava
              ))))
          .flatMap(yliajoContainer =>
            // varmistetaan että henkilö löytyy
            if (Await.result(onrIntegration.henkiloExists(yliajoContainer.henkiloOid.get), ONR_TIMEOUT))
              Right(yliajoContainer)
            else
              LOG.error(s"Yliajojen tallennus oppijalle ${yliajoContainer.henkiloOid.get} epäonnistui, henkilöä ei löydy ONR:stä")
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA), List.empty.asJava))))
          .flatMap((yliajoContainer: YliajoTallennusContainer) =>
            val oppijaOid = yliajoContainer.henkiloOid.get
            val virkailijaOid = SecurityContextHolder.getContext.getAuthentication.getName
            LOG.info(s"Tallennetaan ${yliajoContainer.yliajot.toScala.size} yliajoa oppijalle ${oppijaOid}")
            val user: User = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_TIEDOT_OPPIJANUMERO_PARAM_NAME -> oppijaOid, UI_VALINTADATA_HAKU_PARAM_NAME -> yliajoContainer.hakuOid.get), AuditOperation.TallennaYliajotOppijalle, Some(yliajoContainer))

            val yliajotToSave = yliajoContainer.yliajot.toScala.get.asScala.map(y => {
              AvainArvoYliajo(avain = y.avain.get, arvo = y.arvo.get, henkiloOid = yliajoContainer.henkiloOid.get, hakuOid = yliajoContainer.hakuOid.get, virkailijaOid = virkailijaOid, selite = y.selite.get)
            }).toSeq
            kantaOperaatiot.tallennaYliajot(yliajotToSave)
            Right(ResponseEntity.status(HttpStatus.OK).body(TallennaYliajotOppijalleSuccessResponse()))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[TallennaYliajotOppijalleResponse]])
    catch
      case e: Exception =>
        LOG.error("Yliajojen tallentaminen oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(TallennaYliajotOppijalleFailureResponse(java.util.Set.of(UI_TALLENNA_YLIAJO_OPPIJALLE_TALLENNUS_VIRHE)))
  }

  @DeleteMapping(
    path = Array(UI_POISTA_YLIAJO_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Poistaa tietylle avaimelle tietyssä haussa tehdyn yliajon yksittäiseltä oppijalta",
    description = "Valintoja varten tuotettavia yksittäisiä avain-arvopareja on mahdollista yliajaa hakukohtaisesti. Tämä " +
      "rajapinta yksittäisen yliajon.. Pääsy on (ainakin toistaiseksi) rajattu rekisterinpitäjiin.",
    requestBody =
      new io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = Array(new Content(schema = new Schema(implementation = classOf[SyotettyPerusopetuksenOppimaaranSuoritus])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[PoistaYliajoSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[PoistaYliajoFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def poistaOppijanYliajo(
                           @RequestParam(name = UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, required = true) @Parameter(description = "oppijanumero", example = ESIMERKKI_OPPIJANUMERO) oppijaNumero: Optional[String],
                           @RequestParam(name = UI_VALINTADATA_HAKU_PARAM_NAME, required = false) @Parameter(description = "haun oid", example = ESIMERKKI_HAKU_OID) hakuOid: Optional[String],
                           @RequestParam(name = UI_VALINTADATA_AVAIN_PARAM_NAME, required = false) @Parameter(description = "poistettavan yliajon avain", example = ESIMERKKI_YLIAJO_AVAIN) avain: Optional[String],
                      request: HttpServletRequest): ResponseEntity[PoistaYliajotResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_POISTA_YLIAJO_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(PoistaYliajoFailureResponse(java.util.Set.of(UI_POISTA_YLIAJO_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            // validoidaan tunniste
            val virheet =
              UIValidator.validateAvain(avain.toScala, pakollinen = true) ++
              UIValidator.validateOppijanumero(oppijaNumero.toScala, pakollinen = true) ++
              UIValidator.validateHakuOid(hakuOid.toScala, pakollinen = true)
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PoistaYliajoFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            LOG.info(s"Poistetaan yliajo ${avain.get} oppijalta ${oppijaNumero.get} haussa ${hakuOid.get}")
            val user = AuditLog.getUser(request)
            AuditLog.log(user,
              Map(
                UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME -> oppijaNumero.get,
                UI_VALINTADATA_HAKU_PARAM_NAME -> hakuOid.get,
                UI_VALINTADATA_AVAIN_PARAM_NAME -> avain.get
              ), AuditOperation.PoistaOppijanYliajot, None)

            kantaOperaatiot.poistaYliajo(oppijaNumero.get, hakuOid.get, avain.get)
            Right(ResponseEntity.status(HttpStatus.OK).body(PoistaYliajoSuccessResponse()))))
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[PoistaYliajotResponse]]
    catch
      case e: Exception =>
        LOG.error("Yliajon poisto oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(PoistaYliajoFailureResponse(java.util.Set.of(UI_POISTA_YLIAJO_VIRHE)))
  }
}
