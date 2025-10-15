package fi.oph.suorituspalvelu.ui

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, SuoritusJoukko}
import fi.oph.suorituspalvelu.integration.OnrIntegration
import fi.oph.suorituspalvelu.parsing.virkailija.VirkailijaToSuoritusConverter
import fi.oph.suorituspalvelu.resource.ApiConstants.{ESIMERKKI_HAKU_OID, ESIMERKKI_LUOKKA, ESIMERKKI_OPPIJANUMERO, ESIMERKKI_OPPILAITOS_OID, ESIMERKKI_VERSIOTUNNISTE, ESIMERKKI_VUOSI, UI_400_DESCRIPTION, UI_403_DESCRIPTION, UI_DATA_PATH, UI_HAKU_ESIMERKKI_HAKUKENTAN_ARVO, UI_HAKU_HAKUSANA_PARAM_NAME, UI_HAKU_LUOKKA_PARAM_NAME, UI_HAKU_OPPILAITOS_PARAM_NAME, UI_HAKU_PATH, UI_HAKU_VUOSI_PARAM_NAME, UI_KAYTTAJAN_TIEDOT_PATH, UI_LUO_SUORITUS_OPPIAINE_PATH, UI_LUO_SUORITUS_PERUSOPETUS_PATH, UI_LUO_SUORITUS_VAIHTOEHDOT_PATH, UI_OPPILAITOKSET_EI_OIKEUKSIA, UI_OPPILAITOKSET_PATH, UI_POISTA_SUORITUS_PATH, UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME, UI_TIEDOT_OPPIJANUMERO_PARAM_NAME, UI_TIEDOT_PATH, UI_VALINTADATA_HAKU_PARAM_NAME, UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME}
import fi.oph.suorituspalvelu.resource.ui.UIVirheet.{UI_HAKU_EI_OIKEUKSIA, UI_HAKU_EPAONNISTUI, UI_HAKU_JOKO_HAKUSANA_TAI_OPPILAITOS, UI_HAKU_KRITEERI_PAKOLLINEN, UI_HAKU_OPPILAITOS_PAKOLLINEN, UI_HAKU_VUOSI_PAKOLLINEN, UI_HAKU_VUOSI_PAKOLLINEN_JOS_LUOKKA, UI_KAYTTAJAN_TIEDOT_HAKU_EPAONNISTUI, UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT, UI_LUO_SUORITUS_OPPIAINE_EI_OIKEUKSIA, UI_LUO_SUORITUS_OPPIAINE_JSON_VIRHE, UI_LUO_SUORITUS_OPPIAINE_TALLENNUS_VIRHE, UI_LUO_SUORITUS_OPPIAINE_TUNTEMATON_OPPIJA, UI_LUO_SUORITUS_PERUSOPETUS_EI_OIKEUKSIA, UI_LUO_SUORITUS_PERUSOPETUS_JSON_VIRHE, UI_LUO_SUORITUS_PERUSOPETUS_TALLENNUS_VIRHE, UI_LUO_SUORITUS_PERUSOPETUS_TUNTEMATON_OPPIJA, UI_LUO_SUORITUS_VAIHTOEHDOT_ESIMERKKI_VIRHE, UI_POISTA_SUORITUS_EI_OIKEUKSIA, UI_POISTA_SUORITUS_SUORITUSTA_EI_LOYTYNYT, UI_POISTA_SUORITUS_SUORITUSTA_EI_POISTETTAVISSA, UI_POISTA_SUORITUS_SUORITUS_EI_VOIMASSA, UI_POISTA_SUORITUS_TALLENNUS_VIRHE, UI_TIEDOT_EI_OIKEUKSIA, UI_TIEDOT_HAKU_EPAONNISTUI, UI_VALINTADATA_EI_OIKEUKSIA}
import fi.oph.suorituspalvelu.validation.UIValidator.VALIDATION_HAKUSANA_EI_VALIDI
import fi.oph.suorituspalvelu.validation.Validator
import fi.oph.suorituspalvelu.resource.ui.{KayttajaFailureResponse, KayttajaResponse, KayttajaSuccessResponse, LuoPerusopetuksenOppiaineenOppimaaraFailureResponse, LuoPerusopetuksenOppiaineenOppimaaraResponse, LuoPerusopetuksenOppiaineenOppimaaraSuccessResponse, LuoPerusopetuksenOppimaaraFailureResponse, LuoPerusopetuksenOppimaaraFailureResponseOppiaineVirhe, LuoPerusopetuksenOppimaaraResponse, LuoPerusopetuksenOppimaaraSuccessResponse, LuoSuoritusDropdownDataFailureResponse, LuoSuoritusDropdownDataResponse, LuoSuoritusDropdownDataSuccessResponse, OppijanHakuFailureResponse, OppijanHakuResponse, OppijanHakuSuccessResponse, OppijanTiedotFailureResponse, OppijanTiedotResponse, OppijanTiedotSuccessResponse, OppijanValintaDataFailureResponse, OppijanValintaDataSuccessResponse, OppilaitosFailureResponse, OppilaitosResponse, OppilaitosSuccessResponse, PoistaSuoritusFailureResponse, PoistaSuoritusResponse, PoistaSuoritusSuccessResponse, SyotettavaAidinkielenOppimaaraVaihtoehto, SyotettavaAidinkielenOppimaaraVaihtoehtoNimi, SyotettavaOppiaineVaihtoehto, SyotettavaOppiaineVaihtoehtoNimi, SyotettavaSuoritusKieliVaihtoehto, SyotettavaSuoritusKieliVaihtoehtoNimi, SyotettavaSuoritusTyyppiVaihtoehto, SyotettavaSuoritusTyyppiVaihtoehtoNimi, SyotettavaVierasKieliVaihtoehto, SyotettavaVierasKieliVaihtoehtoNimi, SyotettavaYksilollistamisVaihtoehto, SyotettavaYksilollistamisVaihtoehtoNimi, SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus, SyotettyPerusopetuksenOppimaaranSuoritus}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityConstants, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.ValintaDataService
import fi.oph.suorituspalvelu.ui.UIService.{KOODISTO_KIELIVALIKOIMA, KOODISTO_OPPIAINEET, KOODISTO_OPPIAINE_AIDINKIELI_JA_KIRJALLISUUS, KOODISTO_POHJAKOULUTUS, KOODISTO_SUORITUKSENTYYPIT, KOODISTO_SUORITUSKIELET, SYOTETTAVAT_OPPIAINEET, SYOTETTAVAT_SUORITUSTYYPIT, SYOTETYN_OPPIMAARAN_KIELIAINEKOODIT, SYOTETYN_OPPIMAARAN_SUORITUSKIELET, SYOTETYN_OPPIMAARAN_YKSILOLLISTAMINEN}
import fi.oph.suorituspalvelu.util.{KoodistoProvider, LogContext, OrganisaatioProvider}
import fi.oph.suorituspalvelu.validation.UIValidator
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
import java.util.{Optional, UUID}
import scala.concurrent.Await
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.concurrent.duration.DurationInt

@RequestMapping(path = Array(""))
@RestController
@Tag("UI")
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

  private def getAliases(oppijaNumero: String): Set[String] =
    try
      Set(Set(oppijaNumero), Await.result(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero)), ONR_TIMEOUT).allOids).flatten
    catch
      case e: Exception =>
        LOG.warn("Aliaksien hakeminen ONR:stä epäonnistui henkilölle: " + oppijaNumero, e)
        Set(oppijaNumero)

  @GetMapping(
    path = Array(UI_KAYTTAJAN_TIEDOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee UI:n tarvitsemat käyttäjään liittyvät tiedot",
    description = "Huomioita:\n" +
      "- Tällä hetkellä palautetaan vain asiointikieli, tulevaisuudessa hyvin todennäköisesti tarvittaan mm. käyttöoikeuksiin liittyvää tietoa",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa käyttäjän tiedot", content = Array(new Content(schema = new Schema(implementation = classOf[KayttajaSuccessResponse]))))
    ))
  def haeKayttajanTiedot(request: HttpServletRequest, session: HttpSession): ResponseEntity[KayttajaResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_KAYTTAJAN_TIEDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            val storedKieli = Option.apply(session.getAttribute(ASIOINTIKIELI_SESSION_KEY).asInstanceOf[String])
            if(storedKieli.isDefined)
              Right(ResponseEntity.status(HttpStatus.OK).body(KayttajaSuccessResponse(storedKieli.get)))
            else
              val principal = SecurityContextHolder.getContext.getAuthentication.getPrincipal.asInstanceOf[UserDetails]
              val kieli = Await.result(this.onrIntegration.getAsiointikieli(principal.getUsername), ONR_TIMEOUT)
              if(kieli.isEmpty)
                Left(ResponseEntity.status(HttpStatus.NOT_FOUND).body(KayttajaFailureResponse(java.util.Set.of(UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT))))
              else
                session.setAttribute(ASIOINTIKIELI_SESSION_KEY, kieli.get)
                Right(ResponseEntity.status(HttpStatus.OK).body(KayttajaSuccessResponse(kieli.get)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[KayttajaResponse]])
    catch
      case e: Exception =>
        LOG.error("Asiointikielen haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KayttajaFailureResponse(java.util.Set.of(UI_KAYTTAJAN_TIEDOT_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_OPPILAITOKSET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee oppilaitokset joiden opiskelijatietoihin käyttäjällä on oikeus käyttöliittymän oppijahakua varten",
    description = "Huomioita:\n" +
      "- Huomio 1", // TODO: päivitä
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää oppilaitokset joihin opiskelijatietoihin käyttäjällä on oikeus", content = Array(new Content(schema = new Schema(implementation = classOf[OppilaitosSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeOppilaitokset(request: HttpServletRequest): ResponseEntity[OppilaitosResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_OPPILAITOKSET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if(securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.getOrganisaatiotOikeuksille(SecurityConstants.ROOLIT_OPPIJA_HAULLE).nonEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(UI_OPPILAITOKSET_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            val user = AuditLog.getUser(request)

            LOG.info(s"Haetaan käyttöliittymälle lista oppilaitoksista")
            AuditLog.log(user, Map.empty, AuditOperation.HaeOppijatUI, None)

            if(securityOperaatiot.onRekisterinpitaja())
              Right(ResponseEntity.status(HttpStatus.OK).body(OppilaitosSuccessResponse(uiService.haeKaikkiOppilaitoksetJoissaPKSuorituksia().toList.asJava)))
            else
              val virkailijaAuth = securityOperaatiot.getAuthorization(SecurityConstants.ROOLIT_OPPIJA_HAULLE, organisaatioProvider)
              val oppilaitokset = uiService.haeOppilaitoksetJoihinOikeudet(virkailijaAuth)
              Right(ResponseEntity.status(HttpStatus.OK).body(OppilaitosSuccessResponse(oppilaitokset.toList.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppilaitosResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijoiden haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppilaitosFailureResponse(java.util.Set.of(UI_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_HAKU_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee oppijat käyttöliittymälle hakukriteerien perusteella",
    description = "Huomioita:\n" +
      "- Huomio 1", // TODO: päivitä
    parameters = Array(
      new Parameter(name = UI_HAKU_HAKUSANA_PARAM_NAME, in = ParameterIn.QUERY),
      new Parameter(name = UI_HAKU_OPPILAITOS_PARAM_NAME, in = ParameterIn.QUERY),
      new Parameter(name = UI_HAKU_VUOSI_PARAM_NAME, in = ParameterIn.QUERY),
      new Parameter(name = UI_HAKU_LUOKKA_PARAM_NAME, in = ParameterIn.QUERY),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää hakukriteereiden perusteellä löytyneet oppijat", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeOppijat(
                  @RequestParam(name = UI_HAKU_HAKUSANA_PARAM_NAME, required = false) @Parameter(description = "hakusana", example = UI_HAKU_ESIMERKKI_HAKUKENTAN_ARVO) hakusana: Optional[String],
                  @RequestParam(name = UI_HAKU_OPPILAITOS_PARAM_NAME, required = false) @Parameter(description = "oppilaitos", example = ESIMERKKI_OPPILAITOS_OID) oppilaitos: Optional[String],
                  @RequestParam(name = UI_HAKU_VUOSI_PARAM_NAME, required = false) @Parameter(description = "vuosi", example = ESIMERKKI_VUOSI) vuosi: Optional[String],
                  @RequestParam(name = UI_HAKU_LUOKKA_PARAM_NAME, required = false) @Parameter(description = "luokka", example = ESIMERKKI_LUOKKA) luokka: Optional[String],
                  request: HttpServletRequest): ResponseEntity[OppijanHakuResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            val virheet: Set[String] = Set(Set((hakusana.toScala, oppilaitos.toScala, vuosi.toScala, luokka.toScala) match
              case (None, None, None, None) => Some(UI_HAKU_KRITEERI_PAKOLLINEN)
              case (Some(hakusana), None, None, None) if UIValidator.validateHakusana(hakusana).nonEmpty => Some(VALIDATION_HAKUSANA_EI_VALIDI)
              case (None, Some(oppilaitos), None, _) => Some(UI_HAKU_VUOSI_PAKOLLINEN)
              case (_, None, Some(vuosi), _) => Some(UI_HAKU_OPPILAITOS_PAKOLLINEN)
              case (_, _, None, Some(luokka)) => Some(UI_HAKU_VUOSI_PAKOLLINEN_JOS_LUOKKA)
              case (Some(_), Some(_), _, _) => Some(UI_HAKU_JOKO_HAKUSANA_TAI_OPPILAITOS)
              case default => None).flatten,
              UIValidator.validateOppilaitosOid(oppilaitos.toScala, pakollinen = false),
              UIValidator.validateVuosi(vuosi.toScala, pakollinen = false),
              UIValidator.validateLuokka(luokka.toScala, pakollinen = false)
            ).flatten

            if(virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanHakuFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            // tarkastetaan oikeudet
            // TODO: muutetaan tulevaisuudessa perustumaan myös siihen minkä oppilaitosten oppijoita käyttäjälle on oikeus nähdä
            val virkailijaAuth = securityOperaatiot.getAuthorization(SecurityConstants.ROOLIT_OPPIJA_HAULLE, organisaatioProvider)
            if(!securityOperaatiot.onRekisterinpitaja() && securityOperaatiot.getOrganisaatiotOikeuksille(SecurityConstants.ROOLIT_OPPIJA_HAULLE).isEmpty)
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(UI_HAKU_EI_OIKEUKSIA))))
            else if(oppilaitos.isPresent && !virkailijaAuth.oikeudellisetOrganisaatiot.contains(oppilaitos.get))
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(UI_HAKU_EI_OIKEUKSIA))))
            else
              Right(virkailijaAuth))
          .flatMap(virkailijaAuth =>
            val user = AuditLog.getUser(request)

            LOG.info(s"Haetaan käyttöliittymälle lista oppijoista")
            AuditLog.log(user, Map(
              UI_HAKU_HAKUSANA_PARAM_NAME -> hakusana.orElse(null),
              UI_HAKU_OPPILAITOS_PARAM_NAME -> oppilaitos.orElse(null),
              UI_HAKU_VUOSI_PARAM_NAME -> vuosi.orElse(null),
              UI_HAKU_LUOKKA_PARAM_NAME -> luokka.orElse(null),
            ), AuditOperation.HaeOppijatUI, None)
            val oppijat = if(hakusana.isPresent)
              uiService.haeOppija(hakusana.get, virkailijaAuth)
            else
              uiService.haePKOppijat(oppilaitos.get, vuosi.get.toInt, luokka.toScala)
            Right(ResponseEntity.status(HttpStatus.OK).body(OppijanHakuSuccessResponse(oppijat.toList.asJava)))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppijanHakuResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijoiden haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppijanHakuFailureResponse(java.util.Set.of(UI_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_TIEDOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa yksittäisen oppijan tiedot käyttöliittymälle",
    description = "Huomioita:\n" +
      "- Huomio 1", // TODO: päivitä
    parameters = Array(new Parameter(name = UI_TIEDOT_OPPIJANUMERO_PARAM_NAME, in = ParameterIn.PATH)),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää yksittäisen oppijan tiedot", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanTiedotSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanTiedotFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
  ))
  def haeOppijanTiedot(
                        @PathVariable(UI_TIEDOT_OPPIJANUMERO_PARAM_NAME)  @Parameter(description = "Oppijanumero", example = ESIMERKKI_OPPIJANUMERO, required = true) oppijaNumero: Optional[String],
                        request: HttpServletRequest): ResponseEntity[OppijanTiedotResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TIEDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanTiedotFailureResponse(java.util.Set.of(UI_TIEDOT_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            val virheet = UIValidator.validateOppijanumero(oppijaNumero.toScala, pakollinen = true)
            if(virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanTiedotFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            val user = AuditLog.getUser(request)

            LOG.info(s"Haetaan käyttöliittymälle tiedot oppijasta ${oppijaNumero.get}")
            AuditLog.log(user, Map(UI_TIEDOT_OPPIJANUMERO_PARAM_NAME -> oppijaNumero.orElse(null)), AuditOperation.HaeOppijaTiedotUI, None)
            val suoritukset = this.getAliases(oppijaNumero.get).flatMap(oid => this.kantaOperaatiot.haeSuoritukset(oppijaNumero.get()).values.toSet.flatten)
            val oppijanTiedot = EntityToUIConverter.getOppijanTiedot(oppijaNumero.get(), suoritukset, organisaatioProvider, koodistoProvider)

            if(oppijanTiedot.isEmpty)
              Left(ResponseEntity.status(HttpStatus.GONE).body(""))
            else
              Right(ResponseEntity.status(HttpStatus.OK).body(oppijanTiedot))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppijanTiedotResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijan tietojen haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppijanTiedotFailureResponse(java.util.Set.of(UI_TIEDOT_HAKU_EPAONNISTUI)))

  @GetMapping(
    path = Array(UI_LUO_SUORITUS_VAIHTOEHDOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee käyttöliittymän tiedot syötettäviä suorituksia varten",
    description = "Huomioita:\n" +
      "- Huomio 1", // TODO: päivitä
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää alasvetovalikoiden arvot syötettyjen suoritusten lisäämista varten", content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusDropdownDataResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoSuoritusDropdownDataFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeSyotettavienSuoritustenVaihtoehdot(request: HttpServletRequest): ResponseEntity[LuoSuoritusDropdownDataResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_LUO_SUORITUS_VAIHTOEHDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ => {
            LOG.info(s"Haetaan käyttöliittymälle alasvetovalikoiden arvot suoritusten syöttämistä varten")

            val suoritustyypit        = koodistoProvider.haeKoodisto(KOODISTO_SUORITUKSENTYYPIT)
            val oppiaineet            = koodistoProvider.haeKoodisto(KOODISTO_OPPIAINEET)
            val suorituskielet        = koodistoProvider.haeKoodisto(KOODISTO_SUORITUSKIELET)
            val aidinkielenOppimaarat = koodistoProvider.haeKoodisto(KOODISTO_OPPIAINE_AIDINKIELI_JA_KIRJALLISUUS)
            val kielivalikoima        = koodistoProvider.haeKoodisto(KOODISTO_KIELIVALIKOIMA)
            val pohjakoulutus         = koodistoProvider.haeKoodisto(KOODISTO_POHJAKOULUTUS)

            Right(ResponseEntity.status(HttpStatus.OK).body(LuoSuoritusDropdownDataSuccessResponse(
              SYOTETTAVAT_SUORITUSTYYPIT.map(t => {
                val suoritustyyppiKoodi = suoritustyypit.get(t).get
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
                val oppiaineKoodi = oppiaineet.get(oa).get
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
                val suorituskieliKoodi = suorituskielet.get(k).get
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
                val pohjakoulutusKoodi = pohjakoulutus.get(a.toString).get
                SyotettavaYksilollistamisVaihtoehto(
                  arvo = a,
                  nimi = SyotettavaYksilollistamisVaihtoehtoNimi(
                    pohjakoulutusKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("fi")).map(m => m.nimi).toJava,
                    pohjakoulutusKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("sv")).map(m => m.nimi).toJava,
                    pohjakoulutusKoodi.metadata.find(m => m.kieli.equalsIgnoreCase("en")).map(m => m.nimi).toJava,
                  )
                )
              }).asJava,
            )))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuoSuoritusDropdownDataResponse]])
    catch
      case e: Exception =>
        LOG.error("Suoritusten syöttämisen alasvetovalikoiden tietojen haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoSuoritusDropdownDataFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_VAIHTOEHDOT_ESIMERKKI_VIRHE)))

  @PostMapping(
    path = Array(UI_LUO_SUORITUS_PERUSOPETUS_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Luo perusopetuksen oppimäärän suorituksen yksittäiselle oppijalle",
    description = "Huomioita:\n" +
      "- Huomio 1", // TODO: päivitä
    requestBody =
      new io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = Array(new Content(schema = new Schema(implementation = classOf[SyotettyPerusopetuksenOppimaaranSuoritus])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description="Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppimaaraSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppimaaraFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def luoPerusopetuksenOppimaaranSuoritus(
                                       @RequestBody @Parameter(description = "Suoritukset", required = true) suoritusBytes: Array[Byte],
                                       request: HttpServletRequest): ResponseEntity[LuoPerusopetuksenOppimaaraResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_LUO_SUORITUS_PERUSOPETUS_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
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
            // validoidaan
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
              this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, Set(VirkailijaToSuoritusConverter.toPerusopetuksenOppimaara(versio.get.tunniste, suoritus, koodistoProvider, organisaatioProvider)))

            Right(ResponseEntity.status(HttpStatus.OK).body(LuoPerusopetuksenOppimaaraSuccessResponse())))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuoPerusopetuksenOppimaaraResponse]]
    catch
      case e: Exception =>
        LOG.error("Perusopetuksen oppimaaran tallentaminen oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoPerusopetuksenOppimaaraFailureResponse(java.util.Set.of(UI_LUO_SUORITUS_PERUSOPETUS_TALLENNUS_VIRHE), List.empty.asJava))

  @PostMapping(
    path = Array(UI_LUO_SUORITUS_OPPIAINE_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Luo perusopetuksen oppiaineen oppimäärän suorituksen yksittäiselle oppijalle",
    description = "Huomioita:\n" +
      "- Huomio 1", // TODO: päivitä
    requestBody =
      new io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = Array(new Content(schema = new Schema(implementation = classOf[SyotettyPerusopetuksenOppiaineenOppimaaranSuoritus])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description="Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppiaineenOppimaaraSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoPerusopetuksenOppiaineenOppimaaraFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def luoPerusopetuksenOppiaineenOppimaaraSuoritus(
                                                 @RequestBody @Parameter(description = "Suoritukset", required = true) suoritusBytes: Array[Byte],
                                                 request: HttpServletRequest): ResponseEntity[LuoPerusopetuksenOppiaineenOppimaaraResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_LUO_SUORITUS_OPPIAINE_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
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
            // validoidaan
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
            else
              this.kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio.get, Set(VirkailijaToSuoritusConverter.toPerusopetuksenOppiaineenOppimaara(versio.get.tunniste, suoritus, koodistoProvider, organisaatioProvider)))

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
    summary = "Poistaa perusopetuksen oppimäärän suorituksen yksittäiseltä oppijalta",
    description = "Huomioita:\n" +
      "- Huomio 1", // TODO: päivitä
    requestBody =
      new io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = Array(new Content(schema = new Schema(implementation = classOf[SyotettyPerusopetuksenOppimaaranSuoritus])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description="Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[PoistaSuoritusSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[PoistaSuoritusFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def poistaSuoritus(
                                          @PathVariable(UI_POISTA_SUORITUS_VERSIOTUNNISTE_PARAM_NAME)  @Parameter(description = "Version tunniste", example = ESIMERKKI_VERSIOTUNNISTE, required = true) versioTunniste: Optional[String],
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
            // validoidaan tunniste
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
                  LOG.error(s"Yritettiin poistaa versiota ${versio.get.tunniste} joka joka ei ole perusopetuksen oppimäärän suoritus")
                  Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_SUORITUSTA_EI_POISTETTAVISSA)))))
          .flatMap(versio =>
            LOG.info(s"Poistetaan uorituksen versio ${versio.tunniste} oppijalta ${versio.oppijaNumero}")
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
        LOG.error("Perusopetuksen oppimaaran tallentaminen oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(PoistaSuoritusFailureResponse(java.util.Set.of(UI_POISTA_SUORITUS_TALLENNUS_VIRHE)))

  @GetMapping(
    path = Array(UI_DATA_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa yksittäisen oppijan valintoja varten parsitut avain-arvot",
    description = "Huomioita:\n" +
      "- Huomio 1", // TODO: päivitä
    parameters = Array(new Parameter(name = UI_TIEDOT_OPPIJANUMERO_PARAM_NAME, in = ParameterIn.PATH)),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää yksittäisen oppijan tiedot", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanValintaDataSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanValintaDataFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeValintaData(@RequestParam(name = UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME, required = true) @Parameter(description = "oppijanumero", example = ESIMERKKI_OPPIJANUMERO) oppijaNumero: Optional[String],
                     @RequestParam(name = UI_VALINTADATA_HAKU_PARAM_NAME, required = false) @Parameter(description = "haun oid", example = ESIMERKKI_HAKU_OID) hakuOid: Optional[String],
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
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanValintaDataFailureResponse(java.util.Set.of(UI_VALINTADATA_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            val virheet = UIValidator.validateOppijanumero(oppijaNumero.toScala, pakollinen = true)
            if (virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanValintaDataFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(UI_VALINTADATA_OPPIJANUMERO_PARAM_NAME -> oppijaNumero.orElse(null), UI_VALINTADATA_HAKU_PARAM_NAME -> hakuOid.orElse(null)), AuditOperation.HaeOppijaValintaDataUI, None)
            val data = valintaDataService.fetchValintaDataForOppija(oppijaNumero.get, hakuOid.toScala)
            val oppijanValintaData: OppijanValintaDataSuccessResponse = EntityToUIConverter.getOppijanValintaData(oppijaNumero.get(), hakuOid.toScala, data)
            Right(ResponseEntity.status(HttpStatus.OK).body(oppijanValintaData))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[OppijanTiedotResponse]])
    } catch {
      case e: Exception =>
        LOG.error("Oppijan valintoja varten muodostettujen avain-arvojen haku käyttöliitymälle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OppijanValintaDataFailureResponse(java.util.Set.of(UI_TIEDOT_HAKU_EPAONNISTUI)))
    }
  }

}
