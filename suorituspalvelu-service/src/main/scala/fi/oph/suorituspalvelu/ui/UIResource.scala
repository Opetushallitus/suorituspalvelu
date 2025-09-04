package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.configuration.{KoodistoProvider, OrganisaatioProvider}
import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, SuoritusJoukko}
import fi.oph.suorituspalvelu.integration.OnrIntegration
import fi.oph.suorituspalvelu.resource.ApiConstants.{EXAMPLE_OPPIJANUMERO, UI_HAKU_EI_OIKEUKSIA, UI_HAKU_EPAONNISTUI, UI_HAKU_ESIMERKKI_LUOKKA, UI_HAKU_ESIMERKKI_OPPIJA, UI_HAKU_ESIMERKKI_OPPILAITOS_OID, UI_HAKU_ESIMERKKI_VUOSI, UI_HAKU_KRITEERI_PAKOLLINEN, UI_HAKU_LUOKKA_PARAM_NAME, UI_HAKU_OPPIJA_PARAM_NAME, UI_HAKU_OPPIJA_TAI_VUOSI_PAKOLLINEN, UI_HAKU_OPPILAITOS_PAKOLLINEN, UI_HAKU_OPPILAITOS_PARAM_NAME, UI_HAKU_PATH, UI_HAKU_VUOSI_PAKOLLINEN, UI_HAKU_VUOSI_PARAM_NAME, UI_KAYTTAJAN_TIETOJA_EI_LOYTYNYT, UI_KAYTTAJAN_TIEDOT_HAKU_EPAONNISTUI, UI_KAYTTAJAN_TIEDOT_PATH, UI_LUO_PERUSKOULUN_OPPIMAARA_EI_OIKEUKSIA, UI_LUO_PERUSKOULUN_OPPIMAARA_JSON_VIRHE, UI_LUO_PERUSKOULUN_OPPIMAARA_TUNTEMATON_OPPIJA, UI_LUO_PERUSKOULUN_OPPIMAARA_PATH, UI_LUO_PERUSKOULUN_OPPIMAARA_TALLENNUS_VIRHE, UI_OPPILAITOKSET_EI_OIKEUKSIA, UI_OPPILAITOKSET_PATH, UI_TIEDOT_400_DESCRIPTION, UI_TIEDOT_403_DESCRIPTION, UI_TIEDOT_EI_OIKEUKSIA, UI_TIEDOT_HAKU_EPAONNISTUI, UI_TIEDOT_OPPIJANUMERO_PARAM_NAME, UI_TIEDOT_PATH}
import fi.oph.suorituspalvelu.resource.ui.{KayttajaFailureResponse, KayttajaResponse, KayttajaSuccessResponse, LuoPeruskoulunOppimaaraFailureResponse, LuoPeruskoulunOppimaaraResponse, LuoPeruskoulunOppimaaraSuccessResponse, OppijanHakuFailureResponse, OppijanHakuResponse, OppijanHakuSuccessResponse, OppijanTiedotFailureResponse, OppijanTiedotResponse, OppijanTiedotSuccessResponse, OppilaitosFailureResponse, OppilaitosResponse, OppilaitosSuccessResponse, PeruskoulunOppimaaranSuoritus}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
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

import java.util.Optional
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

  private def getAliases(oppijaNumero: String): Set[String] =
    try
      Set(Set(oppijaNumero), Await.result(onrIntegration.getAliasesForPersonOids(Set(oppijaNumero)), 5.seconds).allOids).flatten
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
              val kieli = Await.result(this.onrIntegration.getAsiointikieli(principal.getUsername), 5.seconds)
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
      new ApiResponse(responseCode = "400", description = UI_TIEDOT_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_TIEDOT_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeOppilaitokset(request: HttpServletRequest): ResponseEntity[OppilaitosResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_OPPILAITOKSET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            // TODO: muutetaan tulevaisuudessa perustumaan myös siihen minkä oppilaitosten oppijoita käyttäjälle on oikeus nähdä
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(UI_OPPILAITOKSET_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            val user = AuditLog.getUser(request)

            LOG.info(s"Haetaan käyttöliittymälle lista oppilaitoksista")
            AuditLog.log(user, Map.empty, AuditOperation.HaeOppijatUI, None)

            val oppilaitokset = uiService.haeOppilaitokset()
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
      new Parameter(name = UI_HAKU_OPPIJA_PARAM_NAME, in = ParameterIn.QUERY),
      new Parameter(name = UI_HAKU_OPPILAITOS_PARAM_NAME, in = ParameterIn.QUERY),
      new Parameter(name = UI_HAKU_VUOSI_PARAM_NAME, in = ParameterIn.QUERY),
      new Parameter(name = UI_HAKU_LUOKKA_PARAM_NAME, in = ParameterIn.QUERY),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää hakukriteereiden perusteellä löytyneet oppijat", content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_TIEDOT_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_TIEDOT_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeOppijat(
                  @RequestParam(name = UI_HAKU_OPPIJA_PARAM_NAME, required = false) @Parameter(description = "oppija", example = UI_HAKU_ESIMERKKI_OPPIJA) oppija: Optional[String],
                  @RequestParam(name = UI_HAKU_OPPILAITOS_PARAM_NAME, required = false) @Parameter(description = "oppilaitos", example = UI_HAKU_ESIMERKKI_OPPILAITOS_OID) oppilaitos: Optional[String],
                  @RequestParam(name = UI_HAKU_VUOSI_PARAM_NAME, required = false) @Parameter(description = "vuosi", example = UI_HAKU_ESIMERKKI_VUOSI) vuosi: Optional[String],
                  @RequestParam(name = UI_HAKU_LUOKKA_PARAM_NAME, required = false) @Parameter(description = "luokka", example = UI_HAKU_ESIMERKKI_LUOKKA) luokka: Optional[String],
                  request: HttpServletRequest): ResponseEntity[OppijanHakuResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            // TODO: muutetaan tulevaisuudessa perustumaan myös siihen minkä oppilaitosten oppijoita käyttäjälle on oikeus nähdä
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(UI_HAKU_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            val virheet: Set[String] = Set(Set((oppija.toScala, oppilaitos.toScala, vuosi.toScala, luokka.toScala) match
              case (None, None, None, None) => Some(UI_HAKU_KRITEERI_PAKOLLINEN)
              case (None, Some(oppilaitos), None, _) => Some(UI_HAKU_OPPIJA_TAI_VUOSI_PAKOLLINEN)
              case (_, None, Some(vuosi), _) => Some(UI_HAKU_OPPILAITOS_PAKOLLINEN)
              case (_, _, None, Some(luokka)) => Some(UI_HAKU_VUOSI_PAKOLLINEN)
              case default => None).flatten,
              Validator.validateOppilaitosOid(oppilaitos.toScala, pakollinen = false),
              Validator.validateVuosi(vuosi.toScala, pakollinen = false),
              Validator.validateLuokka(luokka.toScala, pakollinen = false)
            ).flatten

            if(virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanHakuFailureResponse(virheet.asJava))))
          .flatMap(_ =>
            val user = AuditLog.getUser(request)

            LOG.info(s"Haetaan käyttöliittymälle lista oppijoista")
            AuditLog.log(user, Map(
              UI_HAKU_OPPIJA_PARAM_NAME -> oppija.orElse(null),
              UI_HAKU_OPPILAITOS_PARAM_NAME -> oppilaitos.orElse(null),
              UI_HAKU_VUOSI_PARAM_NAME -> vuosi.orElse(null),
              UI_HAKU_LUOKKA_PARAM_NAME -> luokka.orElse(null),
            ), AuditOperation.HaeOppijatUI, None)

            val oppijat = uiService.haeOppijat(oppija.toScala, oppilaitos.toScala, vuosi.toScala, luokka.toScala)
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
      new ApiResponse(responseCode = "400", description = UI_TIEDOT_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanTiedotFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_TIEDOT_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
  ))
  def haeOppijanTiedot(
                      @PathVariable(UI_TIEDOT_OPPIJANUMERO_PARAM_NAME)  @Parameter(description = "Oppijanumero", example = EXAMPLE_OPPIJANUMERO, required = true) oppijaNumero: Optional[String],
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
            val virheet = Validator.validateOppijanumero(oppijaNumero.toScala, pakollinen = true)
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

  @PostMapping(
    path = Array(UI_LUO_PERUSKOULUN_OPPIMAARA_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Luo peruskoulun oppimäärän suorituksen yksittäiselle oppijalle",
    description = "Huomioita:\n" +
      "- Huomio 1", // TODO: päivitä
    requestBody =
      new io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = Array(new Content(schema = new Schema(implementation = classOf[PeruskoulunOppimaaranSuoritus])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description="Pyyntö vastaanotettu", content = Array(new Content(schema = new Schema(implementation = classOf[LuoPeruskoulunOppimaaraSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_TIEDOT_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LuoPeruskoulunOppimaaraFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_TIEDOT_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def luoPeruskoulunOppimaaraSuoritus(
                                       @RequestBody @Parameter(description = "Suoritukset", required = true) suoritusBytes: Array[Byte],
                                       request: HttpServletRequest): ResponseEntity[LuoPeruskoulunOppimaaraResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_LUO_PERUSKOULUN_OPPIMAARA_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanTiedotFailureResponse(java.util.Set.of(UI_LUO_PERUSKOULUN_OPPIMAARA_EI_OIKEUKSIA)))))
          .flatMap(_ =>
            // deserialisoidaan
            try
              Right(objectMapper.readValue(suoritusBytes, classOf[PeruskoulunOppimaaranSuoritus]))
            catch
              case e: Exception =>
                LOG.error("Peruskoulun oppimaaran suorituksen deserialisointi epäonnistui")
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LuoPeruskoulunOppimaaraFailureResponse(java.util.Set.of(UI_LUO_PERUSKOULUN_OPPIMAARA_JSON_VIRHE)))))
          .flatMap(suoritus =>
            val virheet = Validator.validatePeruskoulunOppimaaranSuoritus(suoritus)
            if(virheet.isEmpty)
              Right(suoritus)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanTiedotFailureResponse(virheet.asJava))))
          .flatMap(suoritus =>
            if(Await.result(onrIntegration.henkiloExists(suoritus.oppijaOid.get), 5.seconds))
              Right(suoritus)
            else
              LOG.error(s"Peruskoulun oppimaaran suorituksen tallennus oppijalle ${suoritus.oppijaOid.get} epäonnistui, henkilöä ei löydy ONR:stä")
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OppijanTiedotFailureResponse(java.util.Set.of(UI_LUO_PERUSKOULUN_OPPIMAARA_TUNTEMATON_OPPIJA)))))
          .flatMap(suoritus =>
            val user = AuditLog.getUser(request)

            LOG.info(s"Tallennetaan peruskoulun oppimaaran suoritus oppijalle ${suoritus.oppijaOid}")
            AuditLog.log(user, Map(UI_TIEDOT_OPPIJANUMERO_PARAM_NAME -> suoritus.oppijaOid.get()), AuditOperation.TallennaPeruskoulunOppimaaranSuoritus, Some(suoritus))

            this.kantaOperaatiot.tallennaJarjestelmaVersio(suoritus.oppijaOid.get(), SuoritusJoukko.PERUSOPETUS, objectMapper.writeValueAsString(suoritus))
            // TODO: "konvertoi sisäiseen formaattiin"

            Right(ResponseEntity.status(HttpStatus.OK).body(LuoPeruskoulunOppimaaraSuccessResponse())))
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LuoPeruskoulunOppimaaraResponse]]
    catch
      case e: Exception =>
        LOG.error("Peruskoulun oppimaaran tallentaminen oppijalle epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LuoPeruskoulunOppimaaraFailureResponse(java.util.Set.of(UI_LUO_PERUSKOULUN_OPPIMAARA_TALLENNUS_VIRHE)))

}