package fi.oph.suorituspalvelu.ui

import fi.oph.suorituspalvelu.resource.ApiConstants.{EXAMPLE_OPPIJANUMERO, UI_HAKU_EPAONNISTUI, UI_HAKU_ESIMERKKI_LUOKKA, UI_HAKU_ESIMERKKI_OPPIJA, UI_HAKU_ESIMERKKI_OPPILAITOS_OID, UI_HAKU_ESIMERKKI_VUOSI, UI_HAKU_KRITEERI_PAKOLLINEN, UI_HAKU_LUOKKA_PARAM_NAME, UI_HAKU_OPPIJA_PARAM_NAME, UI_HAKU_OPPIJA_TAI_VUOSI_PAKOLLINEN, UI_HAKU_OPPILAITOS_PAKOLLINEN, UI_HAKU_OPPILAITOS_PARAM_NAME, UI_HAKU_PATH, UI_HAKU_VUOSI_PAKOLLINEN, UI_HAKU_VUOSI_PARAM_NAME, UI_OPPILAITOKSET_PATH, UI_TIEDOT_400_DESCRIPTION, UI_TIEDOT_403_DESCRIPTION, UI_TIEDOT_HAKU_EPAONNISTUI, UI_TIEDOT_OPPIJANUMERO_PARAM_NAME, UI_TIEDOT_PATH}
import fi.oph.suorituspalvelu.resource.ui.{OppijanHakuFailureResponse, OppijanHakuResponse, OppijanHakuSuccessResponse, OppijanTiedotFailureResponse, OppijanTiedotResponse, OppijanTiedotSuccessResponse, OppilaitosFailureResponse, OppilaitosResponse, OppilaitosSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.UIService
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.*

import java.util.Optional
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@RequestMapping(path = Array(""))
@RestController
@Tag("UI")
class UIResource {

  val LOG = LoggerFactory.getLogger(classOf[UIResource]);

  @Autowired val uiService: UIService = null

  @GetMapping(
    path = Array(UI_OPPILAITOKSET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee oppilaitokset joiden opiskelijatietoihin käyttäjällä on oikeus käyttöliittymän oppijahakua varten",
    description = "Huomioita:\n" +
      "- Huomio 1",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Sisältää oppilaitokset joihin opiskelijatietoihin käyttäjällä on oikeus", content = Array(new Content(schema = new Schema(implementation = classOf[OppilaitosSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = UI_TIEDOT_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[OppijanHakuFailureResponse])))),
      new ApiResponse(responseCode = "403", description = UI_TIEDOT_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeOppilaitokset(request: HttpServletRequest): ResponseEntity[OppilaitosResponse] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = UI_TIEDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            // TODO: muutetaan tulevaisuudessa perustumaan myös siihen minkä oppilaitosten oppijoita käyttäjälle on oikeus nähdä
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(s"Käyttäjällä ei oikeuksia hakea listaa oppilaitoksista")))))
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
      "- Huomio 1",
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
      LogContext(path = UI_TIEDOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            // TODO: muutetaan tulevaisuudessa perustumaan myös siihen minkä oppilaitosten oppijoita käyttäjälle on oikeus nähdä
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanHakuFailureResponse(java.util.Set.of(s"Käyttäjällä ei oikeuksia hakea listaa oppijoista")))))
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
      "- Huomio 1",
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
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(OppijanTiedotFailureResponse(java.util.Set.of(s"Käyttäjällä ei oikeuksia hakea käyttöliittymälle tietoja oppijasta")))))
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
            val oppijanTiedot = uiService.getOppijanTiedot(oppijaNumero.get())
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

}