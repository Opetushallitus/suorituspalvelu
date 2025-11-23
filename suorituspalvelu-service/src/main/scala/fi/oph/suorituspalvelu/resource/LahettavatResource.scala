package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.*
import fi.oph.suorituspalvelu.resource.api.{LahettavatHenkilo, LahettavatHenkilotFailureResponse, LahettavatHenkilotResponse, LahettavatHenkilotSuccessResponse, LahettavatLuokatFailureResponse, LahettavatLuokatResponse, LahettavatLuokatSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.UIService
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
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
@Tag(
  name = "Organisaation rajaimet",
  description = "Hakemuspalvelun tarpeeseen rakennettuja rajapintoja, joiden avulla voidaan rajata hakemuspalvelussa 2. " +
    "asteen hakijoihin kohdistuvia hakuja. Vain rekisterinpitäjillä ja palvelukäyttäjillä on pääsy näihin rajapintoihin.")
class LahettavatResource {

  val LOG = LoggerFactory.getLogger(classOf[LahettavatResource])

  @Autowired var uiService: UIService = null

  @GetMapping(
    path = Array(LAHETTAVAT_LUOKAT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee listan oppilaitoksen luokista joilla on mahdollisia 2. asteen hakijoita",
    description = "Hakemuspalvelun käyttöliittymän hakemusten haun rajaimet sisältävät mahdollisuuden rajata hakemuksia " +
      "tiettyjen pohjakoulutusten lähettävän oppilaitoksen ja valmistumisvuoden perusteella. Tämän endpointin palauttama " +
      "lista sisältää haetun oppilaitoksen luokat joilla on mahdollisia 2. asteen hakijoita",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa listan oppilaitoksen luokista", content = Array(new Content(schema = new Schema(implementation = classOf[LahettavatLuokatSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = LAHETTAVAT_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LahettavatLuokatFailureResponse])))),
      new ApiResponse(responseCode = "403", description = LAHETTAVAT_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeLuokat(@PathVariable(name = LAHETTAVAT_OPPILAITOSOID_PARAM_NAME, required = false) @Parameter(description = "Lähettävän oppilaitoksen oid", example = ESIMERKKI_OPPILAITOS_OID) oppilaitosOid: Optional[String],
                @PathVariable(name = LAHETTAVAT_VUOSI_PARAM_NAME, required = false) @Parameter(description = "Valmistumisvuosi", example = ESIMERKKI_VUOSI) vuosi: Optional[String],
                request: HttpServletRequest): ResponseEntity[LahettavatLuokatResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = LAHETTAVAT_LUOKAT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onPalveluKayttaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // validoidaan parametrit
            val virheet: Set[String] = Set(
              Validator.validateOppilaitosOid(oppilaitosOid.toScala, true),
              Validator.validateVuosi(vuosi.toScala, true)
            ).flatten
            if (virheet.isEmpty)
              Right((oppilaitosOid.get, vuosi.get.toInt))
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LahettavatLuokatFailureResponse(virheet.asJava))))
          .map((oppilaitosOid, vuosi) => {
            try
              val user = AuditLog.getUser(request)
              AuditLog.log(user, Map(LAHETTAVAT_OPPILAITOSOID_PARAM_NAME -> oppilaitosOid, LAHETTAVAT_VUOSI_PARAM_NAME -> vuosi.toString), AuditOperation.HaeLuokatLahettava, None)
              LOG.info(s"Haetaan 2. asteen mahdollisten hakijoiden luokat oppilaitokselle $oppilaitosOid")
              val luokat = uiService.haeLuokat(oppilaitosOid, vuosi)
              ResponseEntity.status(HttpStatus.OK).body(LahettavatLuokatSuccessResponse(luokat.toList.asJava))
            catch
              case e: Exception =>
                LOG.error(s"Luokkien haku oppilaitokselle $oppilaitosOid epäonnistui", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LahettavatLuokatFailureResponse(Set(LAHETTAVAT_500_VIRHE).asJava))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LahettavatLuokatResponse]])
    catch
      case e: Exception =>
        LOG.error("Luokkien haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LahettavatLuokatFailureResponse(java.util.Set.of(LAHETTAVAT_HAKU_EPÄONNISTUI)))
  }

  @GetMapping(
    path = Array(LAHETTAVAT_HENKILOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee listan oppilaitoksen mahdollisista 2. asteen hakijoista",
    description = "Hakemuspalvelun käyttöliittymän hakemusten haun rajaimet sisältävät mahdollisuuden rajata hakemuksia " +
      "tiettyjen pohjakoulutusten lähettävän oppilaitoksen ja valmistumisvuoden perusteella. Tämän endpointin palauttama " +
      "lista sisältää haetun oppilaitoksen opiskelijat jotka ovat mahdollisia 2. asteen hakijoita",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa listan opiskelijoista", content = Array(new Content(schema = new Schema(implementation = classOf[LahettavatHenkilotSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = LAHETTAVAT_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LahettavatHenkilotFailureResponse])))),
      new ApiResponse(responseCode = "403", description = LAHETTAVAT_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeOpiskelijat(@PathVariable(name = LAHETTAVAT_OPPILAITOSOID_PARAM_NAME, required = false) @Parameter(description = "Lähettävän oppilaitoksen oid", example = ESIMERKKI_OPPILAITOS_OID) oppilaitosOid: Optional[String],
                     @PathVariable(name = LAHETTAVAT_VUOSI_PARAM_NAME, required = false) @Parameter(description = "Valmistumisvuosi", example = ESIMERKKI_VUOSI) vuosi: Optional[String],
                     request: HttpServletRequest): ResponseEntity[LahettavatHenkilotResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = LAHETTAVAT_HENKILOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onPalveluKayttaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // validoidaan parametrit
            val virheet: Set[String] = Set(
              Validator.validateOppilaitosOid(oppilaitosOid.toScala, true),
              Validator.validateVuosi(vuosi.toScala, true)
            ).flatten
            if (virheet.isEmpty)
              Right((oppilaitosOid.get, vuosi.get.toInt))
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LahettavatHenkilotFailureResponse(virheet.asJava))))
          .map((oppilaitosOid, vuosi) => {
            try
              val user = AuditLog.getUser(request)
              AuditLog.log(user, Map(LAHETTAVAT_OPPILAITOSOID_PARAM_NAME -> oppilaitosOid, LAHETTAVAT_VUOSI_PARAM_NAME -> vuosi.toString), AuditOperation.HaeHenkilotLahettava, None)
              LOG.info(s"Haetaan 2. asteen mahdolliset hakijat oppilaitokselle $oppilaitosOid")
              val oppijat = uiService.haeOhjattavatJaLuokat(oppilaitosOid, vuosi, None, false, false).map(oppija => LahettavatHenkilo(oppija._1, oppija._2.toList.asJava))
              ResponseEntity.status(HttpStatus.OK).body(LahettavatHenkilotSuccessResponse(oppijat.toList.asJava))
            catch
              case e: Exception =>
                LOG.error(s"Luokkien haku oppilaitokselle $oppilaitosOid epäonnistui", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LahettavatLuokatFailureResponse(Set(LAHETTAVAT_500_VIRHE).asJava))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LahettavatHenkilotResponse]])
    catch
      case e: Exception =>
        LOG.error("Henkilöiden haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LahettavatHenkilotFailureResponse(java.util.Set.of(LAHETTAVAT_HAKU_EPÄONNISTUI)))
  }
}

