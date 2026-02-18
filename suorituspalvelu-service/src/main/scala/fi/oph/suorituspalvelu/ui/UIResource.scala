package fi.oph.suorituspalvelu.ui

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.business.{AvainArvoYliajo, HarkinnanvaraisuusYliajo, KantaOperaatiot, Lahdejarjestelma, Opiskeluoikeus, ParserVersions}
import fi.oph.suorituspalvelu.integration.OnrIntegration
import fi.oph.suorituspalvelu.mankeli.{HarkinnanvaraisuudenSyy, UseitaVahvistettujaOppimaariaException}
import fi.oph.suorituspalvelu.parsing.koski.KoskiUtil
import fi.oph.suorituspalvelu.parsing.virkailija.VirkailijaToSuoritusConverter
import fi.oph.suorituspalvelu.resource.ApiConstants.*
import fi.oph.suorituspalvelu.resource.ui.UIVirheet.*
import fi.oph.suorituspalvelu.resource.ui.*
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityConstants, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.UIService.*
import fi.oph.suorituspalvelu.service.{UIService, ValintaDataService}
import fi.oph.suorituspalvelu.util.{KoodistoProvider, LogContext, OrganisaatioProvider}
import fi.oph.suorituspalvelu.validation.UIValidator
import fi.vm.sade.auditlog.User
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.servlet.http.{HttpServletRequest, HttpSession}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

import java.time.{Instant, LocalDate}
import java.util.{Optional, UUID}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

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
      new ApiResponse(responseCode = "200", description = "Palauttaa käyttäjän tiedot", content = Array(new Content(schema = new Schema(implementation = classOf[KayttajaSuccessResponse])))),
      new ApiResponse(responseCode = "403", description = "Käyttäjä ei ole UI-käyttäjä", content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeKayttajanTiedot(request: HttpServletRequest, session: HttpSession): ResponseEntity[KayttajaResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_KAYTTAJAN_TIEDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onUIKayttaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            val securityOperaatiot = new SecurityOperaatiot
            val onRekisterinpitaja = securityOperaatiot.onRekisterinpitaja()
            val onOrganisaationKatselija = securityOperaatiot.onOrganisaationKatselija()
            val onHakeneidenKatselija = securityOperaatiot.onHakeneidenKatselija()
            val storedKieli = Option.apply(session.getAttribute(ASIOINTIKIELI_SESSION_KEY).asInstanceOf[String])
            if (storedKieli.isDefined)
              Right(ResponseEntity.status(HttpStatus.OK).body(KayttajaSuccessResponse(
                asiointiKieli = storedKieli.get,
                isRekisterinpitaja = onRekisterinpitaja,
                isOrganisaationKatselija = onOrganisaationKatselija,
                isHakeneidenKatselija = onHakeneidenKatselija)
              ))
            else
              val principal = SecurityContextHolder.getContext.getAuthentication.getPrincipal.asInstanceOf[UserDetails]
              val kieli = Await.result(this.onrIntegration.getAsiointikieli(principal.getUsername), ONR_TIMEOUT)
              if (kieli.isEmpty)
                Left(ResponseEntity.status(HttpStatus.NOT_FOUND).body(KayttajaFailureResponse(java.util.Set.of(UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT))))
              else
                session.setAttribute(ASIOINTIKIELI_SESSION_KEY, kieli.get)
                Right(ResponseEntity.status(HttpStatus.OK).body(KayttajaSuccessResponse(
                  asiointiKieli = kieli.get,
                  isRekisterinpitaja = onRekisterinpitaja,
                  isOrganisaationKatselija = onOrganisaationKatselija,
                  isHakeneidenKatselija = onHakeneidenKatselija)
                ))
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
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeOppilaitokset(request: HttpServletRequest): ResponseEntity[OppilaitosResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_OPPILAITOKSET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onOrganisaationKatselija())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            LOG.info(s"Haetaan käyttöliittymälle lista oppilaitoksista")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map.empty, AuditOperation.HaeOppilaitoksetUI, None)

            if (securityOperaatiot.onRekisterinpitaja())
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
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeVuodet(@PathVariable(UI_VUODET_OPPILAITOS_PARAM_NAME)  @Parameter(description = "oppilaitoksen tunniste", example = ESIMERKKI_OPPILAITOS_OID, required = true) oppilaitosOid: Optional[String],
                request: HttpServletRequest): ResponseEntity[VuodetResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_VUODET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet, ei rekisterinpitäjällä pitää olla organisaation katselija-oikeus valittuun oppilaitokseen tai sen parent-organisaatioon
            val virkailijaAuth = securityOperaatiot.getAuthorization(Set(SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA), organisaatioProvider)
            if (virkailijaAuth.onRekisterinpitaja || oppilaitosOid.toScala.exists(oppilaitos => virkailijaAuth.oikeudellisetOrganisaatiot.contains(oppilaitos)))
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(suoritus =>
            // validoidaan parametri
            val virheet: Set[String] = UIValidator.validateOppilaitosOid(oppilaitosOid.toScala, true)

            if (virheet.isEmpty)
              Right(suoritus)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VuodetFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            LOG.info(s"Haetaan käyttöliittymälle lista vuosista")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_LUOKAT_OPPILAITOS_PARAM_NAME -> oppilaitosOid.get), AuditOperation.HaeVuodetUI, None)

            // rekisterinpitäjille ei rajoiteta ohjausvelvollisuuden keston perusteella, muille rajoitetaan
            val paivamaara = if (securityOperaatiot.onRekisterinpitaja()) None else Some(LocalDate.now)
            val vuodet = uiService.haeVuodet(paivamaara, oppilaitosOid.get)
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
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeLuokat(@PathVariable(UI_LUOKAT_OPPILAITOS_PARAM_NAME)  @Parameter(description = "oppilaitoksen tunniste", example = ESIMERKKI_OPPILAITOS_OID, required = true) oppilaitosOid: Optional[String],
                @PathVariable(UI_LUOKAT_VUOSI_PARAM_NAME)  @Parameter(description = "vuosi", example = ESIMERKKI_VUOSI, required = true) vuosi: Optional[String],
                request: HttpServletRequest): ResponseEntity[LuokatResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_LUOKAT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet, ei rekisterinpitäjällä pitää olla organisaation katselija-oikeus valittuun oppilaitokseen tai sen parent-organisaatioon
            val virkailijaAuth = securityOperaatiot.getAuthorization(Set(SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA), organisaatioProvider)
            if (virkailijaAuth.onRekisterinpitaja || oppilaitosOid.toScala.exists(oppilaitos => virkailijaAuth.oikeudellisetOrganisaatiot.contains(oppilaitos)))
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(suoritus =>
            // validoidaan
            val virheet: Set[String] = Set(
              UIValidator.validateOppilaitosOid(oppilaitosOid.toScala, true),
              UIValidator.validateVuosi(vuosi.toScala, true)
            ).flatten
            if (virheet.isEmpty)
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

            // rekisterinpitäjille ei rajoiteta ohjausvelvollisuuden keston perusteella, muille rajoitetaan
            val paivamaara = if (securityOperaatiot.onRekisterinpitaja()) None else Some(LocalDate.now)
            val luokat = uiService.haeLuokat(paivamaara, oppilaitosOid.get, vuosi.get.toInt)
            Right(ResponseEntity.status(HttpStatus.OK).body(LuokatSuccessResponse(luokat.toList.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuokatResponse]])
    catch
      case e: Exception =>
        LOG.error("Luokkien haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuokatFailureResponse(java.util.Set.of(UI_RAJAIMEN_TIEDOT_HAKU_EPAONNISTUI)))

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
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeOppilaitoksenOhjattavat(@RequestParam(name = UI_OPPILAITOS_HAKU_OPPILAITOS_PARAM_NAME, required = true) @Parameter(description = "oppilaitoksen tunniste", example = ESIMERKKI_OPPILAITOS_OID) oppilaitos: Optional[String],
                                 @RequestParam(name = UI_OPPILAITOS_HAKU_VUOSI_PARAM_NAME, required = true) @Parameter(description = "vuosi", example = ESIMERKKI_VUOSI) vuosi: Optional[String],
                                 @RequestParam(name = UI_OPPILAITOS_HAKU_LUOKKA_PARAM_NAME, required = false) @Parameter(description = "luokka", example = ESIMERKKI_LUOKKA) luokka: Optional[String],
                                 @RequestParam(name = UI_OPPILAITOS_HAKU_KESKEN_TAI_KESKEYTYNYT_PARAM_NAME, required = false) @Parameter(description = "Perusopetuksen oppimäärän suoritus on Supa-tilassa KESKEN tai KESKEYTYNYT", example = "false") keskenTaiKeskeytynyt: Boolean,
                                 @RequestParam(name = UI_OPPILAITOS_HAKU_EI_YHTEISTEN_ARVOSANAA_PARAM_NAME, required = false) @Parameter(description = "Perusopetuksen oppimäärän suoritukselta puuttuu yhteisen aineen arvosana", example = "false") yhteistenArvosanaPuuttuu: Boolean,
                                 request: HttpServletRequest): ResponseEntity[OppijanHakuResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_OPPILAITOS_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet, täytyy olla joka rekisterinpitäjä tai vaihtoehtoisesti organisaation katselija valitussa oppilaitoksessa
            val virkailijaAuth = securityOperaatiot.getAuthorization(Set(SecurityConstants.SECURITY_ROOLI_OPPIJOIDEN_KATSELIJA), organisaatioProvider)
            if (!virkailijaAuth.onRekisterinpitaja && oppilaitos.toScala.exists(oppilaitos => !virkailijaAuth.oikeudellisetOrganisaatiot.contains(oppilaitos)))
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build)
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
            if (virheet.isEmpty)
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
            val oppijat = uiService.haeOhjattavat(if (securityOperaatiot.onRekisterinpitaja()) None else Some(LocalDate.now), oppilaitos.get, vuosi.get.toInt, luokka.toScala, keskenTaiKeskeytynyt, yhteistenArvosanaPuuttuu)
            Right(ResponseEntity.status(HttpStatus.OK).body(OppijanHakuSuccessResponse(oppijat.toList.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppijanHakuResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijalistauksen haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppijanHakuFailureResponse(java.util.Set.of(UI_HAKU_EPAONNISTUI)))

  @PostMapping(
    path = Array(UI_TIEDOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa yksittäisen oppijan suoritustiedot käyttöliittymälle",
    description = "Tämä rajapinta palauttaa yksittäisen oppijan suoritustiedot käyttöliittymän suoritustietonäkymää varten. " +
      "Pääsy on sallittu rekisterinpitäjille, organisaation katselijoille ja hakijoiden katselijoille.",
    requestBody =
      new SwaggerRequestBody(
        required = true,
        content = Array(new Content(schema = new Schema(implementation = classOf[OppijanTiedotRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää yksittäisen oppijan tiedot", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanTiedotSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanTiedotFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
  ))
  def haeOppijanTiedot(@RequestBody bodyBytes: Array[Byte],
                       request: HttpServletRequest): ResponseEntity[OppijanTiedotResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TIEDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan että käyttäjällä on organisaation- tai hakijoiden katselija oikeudet
            if (securityOperaatiot.onUIKayttaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // deserialisoidaan
            try
              Right(objectMapper.readValue(bodyBytes, classOf[OppijanTiedotRequest]))
            catch
              case e: Exception =>
                LOG.error("Oppijan tietojen pyynnön deserialisointi epäonnistui")
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanTiedotFailureResponse(java.util.Set.of(UI_TIEDOT_JSON_EI_VALIDI)))))
          .flatMap((oppijanTiedotRequest: OppijanTiedotRequest) =>
            // validoidaan parametri
            val virheet = Set(
              UIValidator.validateTunniste(oppijanTiedotRequest.tunniste.toScala),
              UIValidator.validateAikaleima(oppijanTiedotRequest.aikaleima.toScala, false)
            ).flatten
            if (virheet.isEmpty)
              Right((oppijanTiedotRequest.tunniste.get, oppijanTiedotRequest.aikaleima.toScala.map(Instant.parse)))
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanTiedotFailureResponse(virheet.asJava))))
          .flatMap((tunniste, aikaleima) => {
            this.uiService.resolveOppijaNumero(tunniste).map(oppijaNumero => (oppijaNumero, aikaleima)).toRight(ResponseEntity.status(HttpStatus.NOT_FOUND).build)
          })
          .flatMap((oppijaNumero, aikaleima) => {
              // tarkastetaan oikeudet haetulle oppijanumerolle
              if (this.uiService.hasOppijanKatseluOikeus(oppijaNumero))
                Right((oppijaNumero, aikaleima))
              else
                Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build)
            })
          .flatMap((oppijaNumero, aikaleima) =>
            val ajanhetki = aikaleima.getOrElse(Instant.now)
            LOG.info(s"Haetaan käyttöliittymälle tiedot oppijasta ${oppijaNumero} hetkellä ${ajanhetki}")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), AuditOperation.HaeOppijaTiedotUI, None)
            val oppijanTiedot = uiService.haeOppijanSuoritukset(oppijaNumero, ajanhetki)
            if (oppijanTiedot.isEmpty)
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
    path = Array(UI_OPPIJAN_HAUT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa käyttöliittymälle ne oppijan haut, joilla on hakemuspalvelussa tehty hakemus ja joka löytyy koutasta.",
    description = "Tämä rajapinta palauttaa yksittäisen oppijan haut hakemuspalvelusta. " +
      "Pääsy on sallittu vain rekisterinpitäjille.",
    parameters = Array(new Parameter(name = UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_NAME, in = ParameterIn.PATH)),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Lista hakuja (hakuOid ja nimi)", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHautSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHautFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
  ))
  def haeOppijanHaut(@PathVariable(UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_NAME) @Parameter(description = "Oppijanumero", example = ESIMERKKI_OPPIJANUMERO, required = true) oppijaNumero: Optional[String],
                     request: HttpServletRequest): ResponseEntity[OppijanHautResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_OPPIJAN_HAUT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            val virheet = UIValidator.validateOppijanumero(oppijaNumero.toScala, pakollinen = true)
            if (virheet.isEmpty)
              Right(oppijaNumero.get)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanHautFailureResponse(virheet.asJava))))
          .flatMap(oppijaNumero =>
            LOG.info(s"Haetaan käyttöliittymälle haut oppijalta ${oppijaNumero}")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_OPPIJAN_HAUT_OPPIJANUMERO_PARAM_NAME -> oppijaNumero), AuditOperation.HaeOppijanHautUI, None)
            val haut = uiService.haeOppijanHaut(oppijaNumero)
            Right(ResponseEntity.status(HttpStatus.OK).body(OppijanHautSuccessResponse(haut.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppijanHautResponse]])
    catch
      case e: Exception =>
        LOG.error(s"Oppijan ${oppijaNumero} hakujen hakeminen käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppijanHautFailureResponse(java.util.Set.of(UI_OPPIJAN_HAUT_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_TALLENNA_SUORITUS_OPPILAITOKSET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa käyttöliittymälle listan oppilaitoksista joille voi lisätä käsin syötettäviä suorituksia.",
    description = "Rekisterinpitäjän on käyttöliittymässä mahdollista lisätä perusopetuksen oppimäärän ja perusopetuksen " +
      "oppiaineen oppimäärän suorituksia henkilöille. Tämä rajapinta palauttaa listan oppilaitoksista joihin suorituksia " +
      "voi lisätä. Pääsy rajapintaan on rajattu rekisterinpitäjille.",
    parameters = Array(new Parameter(name = UI_OPPIJANUMERO_PARAM_NAME, in = ParameterIn.PATH)),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää listan oppilaitoksista", content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusOppilaitoksetSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusOppilaitoksetFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeSyotettavatOppilaitokset(request: HttpServletRequest): ResponseEntity[LuoSuoritusOppilaitoksetResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_SUORITUS_OPPILAITOKSET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            val securityOperaatiot = new SecurityOperaatiot
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
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
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeSyotettavienSuoritustenVaihtoehdot(request: HttpServletRequest): ResponseEntity[LuoSuoritusDropdownDataResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_SUORITUS_VAIHTOEHDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            val securityOperaatiot = new SecurityOperaatiot
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
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
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoSuoritusDropdownDataFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_VAIHTOEHDOT_HAKU_EPAONNISTUI)))

  @PostMapping(
    path = Array(UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Tallentaa perusopetuksen oppimäärän suorituksen yksittäiselle oppijalle",
    description = "Rekisterinpitäjän on käyttöliittymässä mahdollista lisätä perusopetuksen oppimäärän suorituksia henkilöille, " +
      "ja muuttaa lisättyjä suorituksia. Tämä rajapinta tallentaa lisäykset/muutokset. Pääsy rajapintaan on rajattu rekisterinpitäjille.",
    requestBody =
      new SwaggerRequestBody(
        required = true,
        description = "Tallennettava suoritus",
        content = Array(new Content(schema = new Schema(implementation = classOf[SyotettyPerusopetuksenOppimaaranSuoritus])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description="Tallennus onnistunut", content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppimaaraSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppimaaraFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def tallennaPerusopetuksenOppimaaranSuoritus(@RequestBody suoritusBytes: Array[Byte],
                                               request: HttpServletRequest): ResponseEntity[LuoPerusopetuksenOppimaaraResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_SUORITUS_PERUSOPETUS_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // deserialisoidaan
            try
              Right(objectMapper.readValue(suoritusBytes, classOf[SyotettyPerusopetuksenOppimaaranSuoritus]))
            catch
              case e: Exception =>
                LOG.error("Perusopetuksen oppimaaran suorituksen deserialisointi epäonnistui")
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_JSON_EI_VALIDI), List.empty.asJava))))
          .flatMap(suoritus =>
            // validoidaan tallennettava suoritus
            val yleisetVirheet = UIValidator.validatePerusopetuksenOppimaaranYleisetKentat(suoritus, koodistoProvider)
            val oppiaineKohtaisetVirheet = UIValidator.validatePerusopetuksenOppimaaranYksittaisetOppiaineet(suoritus.oppiaineet, koodistoProvider)

            if (yleisetVirheet.isEmpty && oppiaineKohtaisetVirheet.isEmpty)
              Right(suoritus)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppimaaraFailureResponse(
                yleisetVirheet.asJava,
                oppiaineKohtaisetVirheet.map((oppiaineKoodi, virheet) => LuoPerusopetuksenOppimaaraFailureResponseOppiaineVirhe(oppiaineKoodi, virheet.asJava)).toList.asJava
              ))))
          .flatMap(suoritus =>
            // varmistetaan että henkilö löytyy
            if (Await.result(onrIntegration.henkiloExists(suoritus.oppijaOid.get), ONR_TIMEOUT))
              Right(suoritus)
            else
              LOG.error(s"Perusopetuksen oppimaaran suorituksen tallennus oppijalle ${suoritus.oppijaOid.get} epäonnistui, henkilöä ei löydy ONR:stä")
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA), List.empty.asJava))))
          .flatMap(suoritus =>
            LOG.info(s"Tallennetaan perusopetuksen oppimaaran suoritus oppijalle ${suoritus.oppijaOid}")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_OPPIJANUMERO_PARAM_NAME -> suoritus.oppijaOid.get()), AuditOperation.TallennaPerusopetuksenOppimaaranSuoritus, Some(suoritus))
            val now = Instant.now()
            val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(suoritus.oppijaOid.get(), Lahdejarjestelma.SYOTETTY_PERUSOPETUS, Seq(objectMapper.writeValueAsString(suoritus)), Seq.empty, now, Lahdejarjestelma.defaultLahdeTunniste(Lahdejarjestelma.SYOTETTY_PERUSOPETUS), None)

            if (versio.isEmpty)
              LOG.info(s"Tallennettava perusopetuksen oppimaaran suoritus oppijalle ${suoritus.oppijaOid} ei sisältänyt muutoksia aikaisempaan versioon verrattuna")
            else
              val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(VirkailijaToSuoritusConverter.toPerusopetuksenOppimaara(versio.get.tunniste, suoritus, koodistoProvider, organisaatioProvider))
              this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet), ParserVersions.SYOTETTY_PERUSOPETUS)

            Right(ResponseEntity.status(HttpStatus.OK).body(LuoPerusopetuksenOppimaaraSuccessResponse())))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuoPerusopetuksenOppimaaraResponse]]
    catch
      case e: Exception =>
        LOG.error("Perusopetuksen oppimaaran tallentaminen oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_TALLENNUS_EPAONNISTUI), List.empty.asJava))

  @PostMapping(
    path = Array(UI_TALLENNA_SUORITUS_OPPIAINE_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Tallentaa perusopetuksen oppiaineen oppimäärän suorituksen yksittäiselle oppijalle",
    description = "Rekisterinpitäjän on käyttöliittymässä mahdollista lisätä perusopetuksen oppiaineen oppimäärän suorituksia henkilöille, " +
      "ja muuttaa lisättyjä suorituksia. Tämä rajapinta tallentaa lisäykset/muutokset. Pääsy rajapintaan on rajattu rekisterinpitäjille.",
    requestBody =
      new SwaggerRequestBody(
        required = true,
        description = "Tallennettava suoritus",
        content = Array(new Content(schema = new Schema(implementation = classOf[SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description="Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppiaineenOppimaaraSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppiaineenOppimaaraFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def tallennaPerusopetuksenOppiaineenOppimaaraSuoritus(@RequestBody suoritusBytes: Array[Byte],
                                                        request: HttpServletRequest): ResponseEntity[LuoPerusopetuksenOppiaineenOppimaaraResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_SUORITUS_OPPIAINE_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // deserialisoidaan
            try
              Right(objectMapper.readValue(suoritusBytes, classOf[SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer]))
            catch
              case e: Exception =>
                LOG.error("Perusopetuksen oppiaineen oppimaaran suorituksen deserialisointi epäonnistui")
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_OPPIAINE_JSON_EI_VALIDI)))))
          .flatMap(suoritus =>
            // validoidaan tallennettava suoritus
            val virheet: Set[String] = UIValidator.validatePerusopetuksenOppiaineenOppimaarat(suoritus, koodistoProvider)
            if (virheet.isEmpty)
              Right(suoritus)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(virheet.asJava))))
          .flatMap(suoritus =>
            // varmistetaan että henkilö löytyy
            if (Await.result(onrIntegration.henkiloExists(suoritus.oppijaOid.get), ONR_TIMEOUT))
              Right(suoritus)
            else
              LOG.error(s"Perusopetuksen oppiaineen oppimaaran suorituksen tallennus oppijalle ${suoritus.oppijaOid.get} epäonnistui, henkilöä ei löydy ONR:stä")
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_OPPIAINE_TUNTEMATON_OPPIJA)))))
          .flatMap(suoritus =>
            LOG.info(s"Tallennetaan perusopetuksen oppiaineen oppimaaran suoritus oppijalle ${suoritus.oppijaOid}")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_OPPIJANUMERO_PARAM_NAME -> suoritus.oppijaOid.get()), AuditOperation.TallennaPerusopetuksenOppiaineenOppimaaranSuoritus, Some(suoritus))
            val now = Instant.now()
            val versio = this.kantaOperaatiot.tallennaJarjestelmaVersio(suoritus.oppijaOid.get(), Lahdejarjestelma.SYOTETTY_PERUSOPETUS, Seq(objectMapper.writeValueAsString(suoritus)), Seq.empty, now, Lahdejarjestelma.defaultLahdeTunniste(Lahdejarjestelma.SYOTETTY_PERUSOPETUS), None)

            if (versio.isEmpty)
              LOG.info(s"Tallennettava perusopetuksen oppiaineen oppimaaran suoritus oppijalle ${suoritus.oppijaOid} ei sisältänyt muutoksia aikaisempaan versioon verrattuna")
            else {
              val opiskeluoikeudet: Set[Opiskeluoikeus] = Set(VirkailijaToSuoritusConverter.toPerusopetuksenOppiaineenOppimaara(versio.get.tunniste, suoritus, koodistoProvider, organisaatioProvider))
              this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet), ParserVersions.SYOTETYT_OPPIAINEET)
            }
            Right(ResponseEntity.status(HttpStatus.OK).body(LuoPerusopetuksenOppiaineenOppimaaraSuccessResponse())))
      )
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuoPerusopetuksenOppiaineenOppimaaraResponse]]
    catch
      case e: Exception =>
        LOG.error("Perusopetuksen oppimaaran tallentaminen oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoPerusopetuksenOppiaineenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_OPPIAINE_TALLENNUS_EPAONNISTUI)))

  @DeleteMapping(
    path = Array(UI_POISTA_SUORITUS_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Poistaa käsin syötetyn perusopetuksen oppimäärään tai oppiaineen oppimäärän suorituksen.",
    description = "Rekisterinpitäjän on käyttöliittymässä mahdollista lisätä perusopetuksen oppimäärän ja oppiaineen oppimäärän suorituksia " +
      "henkilöille, ja poistaa lisättyjä suorituksia. Tämä rajapinta poistaa lisätyn suorituksen. Pääsy rajapintaan on rajattu rekisterinpitäjille.",
    responses = Array(
      new ApiResponse(responseCode = "200", description="Suoritus poistettu", content = Array(new Content(schema = new Schema(implementation = classOf[PoistaSuoritusSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[PoistaSuoritusFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def poistaSuoritus(@PathVariable(UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME)  @Parameter(description = "Poistettavan version tunniste", example = ESIMERKKI_VERSIOTUNNISTE, required = true) versioTunniste: Optional[String],
                     request: HttpServletRequest): ResponseEntity[PoistaSuoritusResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_POISTA_SUORITUS_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // validoidaan version tunniste
            val virheet = UIValidator.validateVersioTunniste(versioTunniste.toScala)
            if (virheet.isEmpty)
              Right(UUID.fromString(versioTunniste.get()))
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PoistaSuoritusFailureResponse(virheet.asJava))))
          .flatMap(versioTunniste =>
            // varmistetaan että versio olemassa
            val versio = this.kantaOperaatiot.haeVersio(versioTunniste)
            if (versio.isEmpty)
              Left(ResponseEntity.status(HttpStatus.GONE).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_SUORITUSTA_EI_LOYTYNYT))))
            else
              versio.get.lahdeJarjestelma match
                // ja että se on poistettavissa (ts. käsin syötetty)
                case Lahdejarjestelma.SYOTETTY_PERUSOPETUS => Right(versio.get)
                case Lahdejarjestelma.SYOTETYT_OPPIAINEET => Right(versio.get)
                case default =>
                  LOG.error(s"Yritettiin poistaa versiota ${versio.get.tunniste} joka joka ei ole käsin syötetty suoritus")
                  Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_SUORITUS_EI_POISTETTAVISSA)))))
          .flatMap(versio =>
            LOG.info(s"Poistetaan suorituksen versio ${versio.tunniste} henkilöltä ${versio.henkiloOid}")
            val user = AuditLog.getUser(request)
            versio.lahdeJarjestelma match
              case Lahdejarjestelma.SYOTETTY_PERUSOPETUS => AuditLog.log(user, Map(UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME -> versio.tunniste.toString), AuditOperation.PoistaPerusopetuksenOppimaaranSuoritus, None)
              case Lahdejarjestelma.SYOTETYT_OPPIAINEET => AuditLog.log(user, Map(UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME -> versio.tunniste.toString), AuditOperation.PoistaPerusopetuksenOppiaineenOppimaaranSuoritus, None)
            if (!this.kantaOperaatiot.paataVersionVoimassaolo(versio.tunniste)) {
              // versio oli jo poistettu
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_SUORITUS_EI_VOIMASSA))))
            } else
              Right(ResponseEntity.status(HttpStatus.OK).body(PoistaSuoritusSuccessResponse()))))
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[PoistaSuoritusResponse]]
    catch
      case e: Exception =>
        LOG.error("Tallennetun suorituksen poistaminen oppijalta epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_TALLENNUS_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_VALINTADATA_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa yksittäisen oppijan valintoja varten parsitut avain-arvot",
    description = "Käyttöliittymässä on mahdollista tarkastella hakija- ja hakukohtaista valintalaskennalle menevää avain-" +
      "arvopareiksi jalostettua dataa. Tämä rajapinta palauttaa nämä avain-arvoparit. Pääsy on (ainakin toistaiseksi) " +
      "rajattu rekisterinpitäjiin.",
    parameters = Array(new Parameter(name = UI_OPPIJANUMERO_PARAM_NAME, in = ParameterIn.PATH)),
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
      LogContext(path = UI_VALINTADATA_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // validoidaan parametrit
            val virheAvaimet: Set[String] = Set(
              UIValidator.validateOppijanumero(oppijaNumero.toScala, pakollinen = true),
              UIValidator.validateHakuOid(hakuOid.toScala, pakollinen = false)
            ).flatten
            if (virheAvaimet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanValintaDataFailureResponse(virheAvaimet.asJava))))
          .flatMap(_ =>
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME -> oppijaNumero.orElse(null), UI_VALINTADATA_HAKU_PARAM_NAME -> hakuOid.orElse(null)), AuditOperation.HaeOppijaValintaDataUI, None)
            val data = valintaDataService.getValintaData(oppijaNumero.get, hakuOid.get)
            val oppijanValintaData: OppijanValintaDataSuccessResponse = EntityToUIConverter.getOppijanValintaDataForUI(oppijaNumero.get(), hakuOid.get(), data)
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

  @GetMapping(
    path = Array(UI_YLIAJOT_HISTORIA_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa yksittäisen avain-arvon muutoshistorian",
    description = "Käyttöliittymässä on mahdollista tarkastella hakija- ja hakukohtaista valintalaskennalle menevää avain-" +
      "arvopareiksi jalostettua dataa. Tämä rajapinta palauttaa yksittäisen avain-arvoparin muutoshistorian.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää yksittäisen avaimen yliajojen historian", content = Array(new Content(schema = new Schema(implementation = classOf[YliajonMuutosHistoriaSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[YliajonMuutosHistoriaFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[YliajonMuutosHistoriaFailureResponse])))),
      new ApiResponse(responseCode = "500", description = UI_500_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[YliajonMuutosHistoriaFailureResponse]))))
    ))
  def haeAvaimenHistoria(@RequestParam(name = UI_YLIAJOT_HISTORIA_OPPIJANUMERO_PARAM_NAME, required = true) @Parameter(description = "henkilönumero", example = ESIMERKKI_OPPIJANUMERO) oppijaNumero: Optional[String],
                         @RequestParam(name = UI_YLIAJOT_HISTORIA_HAKU_PARAM_NAME, required = true) @Parameter(description = "haun tunniste", example = ESIMERKKI_HAKU_OID) hakuOid: Optional[String],
                         @RequestParam(name = UI_YLIAJOT_HISTORIA_AVAIN_PARAM_NAME, required = true) @Parameter(description = "avain", example = ESIMERKKI_YLIAJO_AVAIN) avain: Optional[String],
                         request: HttpServletRequest): ResponseEntity[YliajonMuutosHistoriaResponse] = {
    try {
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_YLIAJOT_HISTORIA_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // validoidaan parametrit
            val virheAvaimet: Set[String] = Set(
              UIValidator.validateOppijanumero(oppijaNumero.toScala, pakollinen = true),
              UIValidator.validateHakuOid(hakuOid.toScala, pakollinen = true),
              UIValidator.validateAvain(avain.toScala, pakollinen = true)
            ).flatten
            if (virheAvaimet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YliajonMuutosHistoriaFailureResponse(virheAvaimet.asJava))))
          .flatMap(_ =>
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(
              UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME -> oppijaNumero.orElse(null),
              UI_VALINTADATA_HAKU_PARAM_NAME -> hakuOid.orElse(null),
              UI_VALINTADATA_AVAIN_PARAM_NAME -> avain.orElse(null)), AuditOperation.HaeOppijaValintaDataAvainMuutoksetUI, None)
            val muutosHistoria = uiService.haeYliajonMuutosHistoria(oppijaNumero.get, hakuOid.get, avain.get)
            Right(ResponseEntity.status(HttpStatus.OK).body(YliajonMuutosHistoriaSuccessResponse(muutosHistoria.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[YliajonMuutosHistoriaResponse]])
    } catch {
      case e: Exception =>
        LOG.error("Avainmuutoshistorian haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(YliajonMuutosHistoriaFailureResponse(java.util.Set.of(UI_VALINTADATA_GENEERINEN_BACKEND_VIRHE)))
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
      new SwaggerRequestBody(
        required = true,
        description = "Yliajojen tiedot",
        content = Array(new Content(schema = new Schema(implementation = classOf[YliajoTallennusContainer])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[TallennaYliajotOppijalleSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[TallennaYliajotOppijalleFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def tallennaYliajoOppijalle(@RequestBody bodyBytes: Array[Byte],
                              request: HttpServletRequest): ResponseEntity[TallennaYliajotOppijalleResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_YLIAJOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // deserialisoidaan
            try
              Right(objectMapper.readValue(bodyBytes, classOf[YliajoTallennusContainer]))
            catch
              case e: Exception =>
                LOG.error("Yliajojen deserialisointi epäonnistui")
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_JSON_EI_VALIDI), List.empty.asJava))))
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
            AuditLog.log(user, Map(UI_OPPIJANUMERO_PARAM_NAME -> oppijaOid, UI_VALINTADATA_HAKU_PARAM_NAME -> yliajoContainer.hakuOid.get), AuditOperation.TallennaYliajotOppijalle, Some(yliajoContainer))

            val yliajotToSave = yliajoContainer.yliajot.toScala.get.asScala.map(y => {
              AvainArvoYliajo(avain = y.avain.get, arvo = y.arvo.toScala, henkiloOid = yliajoContainer.henkiloOid.get, hakuOid = yliajoContainer.hakuOid.get, virkailijaOid = virkailijaOid, selite = y.selite.get)
            }).toSeq
            kantaOperaatiot.tallennaYliajot(yliajotToSave)
            Right(ResponseEntity.status(HttpStatus.OK).body(TallennaYliajotOppijalleSuccessResponse()))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[TallennaYliajotOppijalleResponse]])
    catch
      case e: Exception =>
        LOG.error("Yliajojen tallentaminen oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(TallennaYliajotOppijalleFailureResponse(java.util.Set.of(UI_TALLENNA_YLIAJO_OPPIJALLE_TALLENNUS_EPAONNISTUI)))
  }

  @DeleteMapping(
    path = Array(UI_POISTA_YLIAJO_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Poistaa tietylle avaimelle tietyssä haussa tehdyn yliajon yksittäiseltä oppijalta",
    description = "Valintoja varten tuotettavia yksittäisiä avain-arvopareja on mahdollista yliajaa hakukohtaisesti. Tämä " +
      "rajapinta poistaa yksittäisen yliajon. Pääsy on (ainakin toistaiseksi) rajattu rekisterinpitäjiin.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[PoistaYliajoSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[PoistaYliajoFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def poistaOppijanYliajo(
                           @RequestParam(name = UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, required = true) @Parameter(description = "oppijanumero", example = ESIMERKKI_OPPIJANUMERO) oppijaNumero: Optional[String],
                           @RequestParam(name = UI_VALINTADATA_HAKU_PARAM_NAME, required = false) @Parameter(description = "haun oid", example = ESIMERKKI_HAKU_OID) hakuOid: Optional[String],
                           @RequestParam(name = UI_VALINTADATA_AVAIN_PARAM_NAME, required = false) @Parameter(description = "poistettavan yliajon avain", example = ESIMERKKI_YLIAJO_AVAIN) avain: Optional[String],
                           @RequestParam(name = UI_SELITE_PARAM_NAME, required = false) @Parameter(description = "poiston selite", example = ESIMERKKI_YLIAJO_SELITE) selite: Optional[String],
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
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // validoidaan tunniste
            val virheet =
              UIValidator.validateAvain(avain.toScala, pakollinen = true) ++
              UIValidator.validateOppijanumero(oppijaNumero.toScala, pakollinen = true) ++
              UIValidator.validateHakuOid(hakuOid.toScala, pakollinen = true) ++
              UIValidator.validateSelite(selite.toScala, pakollinen = true)
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

            val virkailijaOid = SecurityContextHolder.getContext.getAuthentication.getName
            kantaOperaatiot.poistaYliajo(oppijaNumero.get, hakuOid.get, avain.get, virkailijaOid, selite.get)
            Right(ResponseEntity.status(HttpStatus.OK).body(PoistaYliajoSuccessResponse()))))
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[PoistaYliajotResponse]]
    catch
      case e: Exception =>
        LOG.error("Yliajon poisto oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(PoistaYliajoFailureResponse(java.util.Set.of(UI_POISTA_YLIAJO_EPAONNISTUI)))
  }

  @GetMapping(
    path = Array(UI_HAE_HARKINNANVARAISUUS_YLIAJOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee hakemukselle tallennetut harkinnanvaraisuusyliajot",
    description = "Palauttaa hakemukselle tallennetut harkinnanvaraisuusyliajot. Pääsy on (ainakin toistaiseksi) rajattu rekisterinpitäjiin.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[HaeHarkinnanvaraisuusYliajotSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[HaeHarkinnanvaraisuusYliajotFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    )
  )
  def haeHarkinnanvaraisuusYliajot(
    @RequestParam(name = UI_HARKINNANVARAISUUS_HAKEMUS_OID_PARAM_NAME, required = true) @Parameter(description = "hakemuksen oid", example = ESIMERKKI_HAKEMUS_OID) hakemusOid: Optional[String],
    request: HttpServletRequest
  ): ResponseEntity[HaeHarkinnanvaraisuusYliajotResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_HAE_HARKINNANVARAISUUS_YLIAJOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            val virheet = UIValidator.validateHakemusOid(hakemusOid.toScala, pakollinen = true)
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(HaeHarkinnanvaraisuusYliajotFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            LOG.info(s"Haetaan harkinnanvaraisuusyliajot hakemukselle ${hakemusOid.get}")
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_HARKINNANVARAISUUS_HAKEMUS_OID_PARAM_NAME -> hakemusOid.get), AuditOperation.HaeHarkinnanvaraisuusYliajot, None)

            val yliajot = kantaOperaatiot.haeHakemuksenHarkinnanvaraisuusYliajot(hakemusOid.get)
            val yliajoDTOs = yliajot.map(y =>
              HarkinnanvaraisuusYliajoDTO(
                hakemusOid = Optional.of(y.hakemusOid),
                hakukohdeOid = Optional.of(y.hakukohdeOid),
                harkinnanvaraisuudenSyy = Optional.ofNullable(y.harkinnanvaraisuudenSyy.map(_.toString).orNull),
                selite = Optional.of(y.selite)
              )
            )
            Right(ResponseEntity.status(HttpStatus.OK).body(HaeHarkinnanvaraisuusYliajotSuccessResponse(yliajoDTOs.asJava)))))
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[HaeHarkinnanvaraisuusYliajotResponse]]
    catch
      case e: Exception =>
        LOG.error("Harkinnanvaraisuusyliajojen haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HaeHarkinnanvaraisuusYliajotFailureResponse(java.util.Set.of(UI_HAE_HARKINNANVARAISUUS_YLIAJOT_HAKU_EPAONNISTUI)))
  }

  @PostMapping(
    path = Array(UI_TALLENNA_HARKINNANVARAISUUS_YLIAJOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Tallentaa listan harkinnanvaraisuustietojen yliajoja",
    description = "Tallentaa harkinnanvaraisuusyliajot. Pääsy on (ainakin toistaiseksi) rajattu rekisterinpitäjiin.",
    requestBody =
      new SwaggerRequestBody(
        required = true,
        description = "Harkinnanvaraisuustietojen yliajot",
        content = Array(new Content(schema = new Schema(implementation = classOf[HarkinnanvaraisuusYliajoTallennusContainer])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[TallennaHarkinnanvaraisuusYliajotSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[TallennaHarkinnanvaraisuusYliajotFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    )
  )
  def tallennaHarkinnanvaraisuusYliajot(
    @RequestBody bodyBytes: Array[Byte],
    request: HttpServletRequest
  ): ResponseEntity[TallennaHarkinnanvaraisuusYliajotResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TALLENNA_HARKINNANVARAISUUS_YLIAJOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            try
              Right(objectMapper.readValue(bodyBytes, classOf[HarkinnanvaraisuusYliajoTallennusContainer]))
            catch
              case e: Exception =>
                LOG.error("Harkinnanvaraisuusyliajojen deserialisointi epäonnistui")
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TallennaHarkinnanvaraisuusYliajotFailureResponse(java.util.Set.of(UI_TALLENNA_HARKINNANVARAISUUS_YLIAJO_JSON_EI_VALIDI)))))
          .flatMap((yliajot: HarkinnanvaraisuusYliajoTallennusContainer) =>
            val virheet: Set[String] = UIValidator.validateHarkinnanvaraisuusYliajot(yliajot)
            if (virheet.isEmpty)
              Right(yliajot)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TallennaHarkinnanvaraisuusYliajotFailureResponse(
                virheet.asJava
              ))))
          .flatMap((yliajoContainer: HarkinnanvaraisuusYliajoTallennusContainer) =>
            val virkailijaOid = SecurityContextHolder.getContext.getAuthentication.getName
            val yliajotToSave = yliajoContainer.yliajot.toScala.get.asScala.map(y =>
              HarkinnanvaraisuusYliajo(
                hakemusOid = y.hakemusOid.get,
                hakukohdeOid = y.hakukohdeOid.get,
                harkinnanvaraisuudenSyy = y.harkinnanvaraisuudenSyy.toScala.map(HarkinnanvaraisuudenSyy.valueOf),
                virkailijaOid = virkailijaOid,
                selite = y.selite.get
              )
            ).toSeq
            LOG.info(s"Tallennetaan ${yliajotToSave.size} harkinnanvaraisuusyliajoa")
            val user: User = AuditLog.getUser(request)
            AuditLog.log(user, Map(), AuditOperation.TallennaHarkinnanvaraisuusYliajot, Some(yliajoContainer))

            kantaOperaatiot.tallennaHarkinnanvaraisuusYliajot(yliajotToSave)
            Right(ResponseEntity.status(HttpStatus.OK).body(TallennaHarkinnanvaraisuusYliajotSuccessResponse()))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[TallennaHarkinnanvaraisuusYliajotResponse]])
    catch
      case e: Exception =>
        LOG.error("Harkinnanvaraisuusyliajojen tallentaminen epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(TallennaHarkinnanvaraisuusYliajotFailureResponse(java.util.Set.of(UI_TALLENNA_HARKINNANVARAISUUS_YLIAJO_TALLENNUS_EPAONNISTUI)))
  }

  @DeleteMapping(
    path = Array(UI_POISTA_HARKINNANVARAISUUS_YLIAJO_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Poistaa harkinnanvaraisuusyliajon hakemukselta",
    description = "Poistaa yksittäisen harkinnanvaraisuusyliajon. Pääsy on (ainakin toistaiseksi) rajattu rekisterinpitäjiin.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[PoistaHarkinnanvaraisuusYliajoSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[PoistaHarkinnanvaraisuusYliajoFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    )
  )
  def poistaHarkinnanvaraisuusYliajo(
    @RequestParam(name = UI_HARKINNANVARAISUUS_HAKEMUS_OID_PARAM_NAME, required = true) @Parameter(description = "hakemuksen oid", example = ESIMERKKI_HAKEMUS_OID) hakemusOid: Optional[String],
    @RequestParam(name = UI_HARKIKNNANVARAISUUS_HAKUKOHDE_OID_PARAM_NAME, required = true) @Parameter(description = "hakukohteen oid", example = ESIMERKKI_HAKUKOHDE_OID) hakukohdeOid: Optional[String],
    @RequestParam(name = UI_SELITE_PARAM_NAME, required = true) @Parameter(description = "poiston selite", example = ESIMERKKI_YLIAJO_SELITE) selite: Optional[String],
    request: HttpServletRequest
  ): ResponseEntity[PoistaHarkinnanvaraisuusYliajoResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_POISTA_HARKINNANVARAISUUS_YLIAJO_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            val virheet =
              UIValidator.validateHakemusOid(hakemusOid.toScala, pakollinen = true) ++
              UIValidator.validateHakukohdeOid(hakukohdeOid.toScala, pakollinen = true) ++
              UIValidator.validateSelite(selite.toScala, pakollinen = true)
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PoistaHarkinnanvaraisuusYliajoFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            LOG.info(s"Poistetaan harkinnanvaraisuusyliajo hakemukselta ${hakemusOid.get} hakukohteessa ${hakukohdeOid.get}")
            val user = AuditLog.getUser(request)
            AuditLog.log(user,
              Map(
                UI_HARKINNANVARAISUUS_HAKEMUS_OID_PARAM_NAME -> hakemusOid.get,
                UI_HARKIKNNANVARAISUUS_HAKUKOHDE_OID_PARAM_NAME -> hakukohdeOid.get
              ), AuditOperation.PoistaHarkinnanvaraisuusYliajo, None)

            val virkailijaOid = SecurityContextHolder.getContext.getAuthentication.getName
            kantaOperaatiot.poistaHarkinnanvaraisuusYliajo(hakemusOid.get, hakukohdeOid.get, virkailijaOid, selite.get)
            Right(ResponseEntity.status(HttpStatus.OK).body(PoistaHarkinnanvaraisuusYliajoSuccessResponse()))))
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[PoistaHarkinnanvaraisuusYliajoResponse]]
    catch
      case e: Exception =>
        LOG.error("Harkinnanvaraisuusyliajon poisto epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(PoistaHarkinnanvaraisuusYliajoFailureResponse(java.util.Set.of(UI_POISTA_HARKINNANVARAISUUS_YLIAJO_EPAONNISTUI)))
  }
}
