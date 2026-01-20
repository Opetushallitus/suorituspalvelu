package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.*
import fi.oph.suorituspalvelu.resource.api.{LahettavatHenkilo, LahettavatHenkilotFailureResponse, LahettavatHenkilotResponse, LahettavatHenkilotSuccessResponse, LahettavatLuokatFailureResponse, LahettavatLuokatResponse, LahettavatLuokatSuccessResponse, LahtokouluAuthorization, LahtokoulutFailureResponse, LahtokoulutResponse, LahtokoulutSuccessResponse, AvainarvotFailureResponse, AvainarvotResponse, AvainarvotSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.{LahtokoulutService, ValintaDataService}
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
  name = "Hakijat",
  description = "Hakemuspalvelun tarpeeseen rakennettuja rajapintoja, joiden avulla voidaan rajata hakemuspalvelussa 2. " +
    "asteen hakijoihin kohdistuvia hakuja, tarkastaa käyttöoikeuksia, ja hakea valintalaskennassa käytettäviä tietoja. " +
    "Vain rekisterinpitäjillä ja palvelukäyttäjillä on pääsy näihin rajapintoihin.")
class HakijatResource {

  val LOG = LoggerFactory.getLogger(classOf[HakijatResource])

  @Autowired var lahtokoulutService: LahtokoulutService = null

  @Autowired var valintaDataService: ValintaDataService = null

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
              val luokat = lahtokoulutService.haeLuokat(oppilaitosOid, vuosi)
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
              val oppijat = lahtokoulutService.haeOhjattavatJaLuokat(oppilaitosOid, vuosi).map(oppija => LahettavatHenkilo(oppija._1, oppija._2))
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

  @GetMapping(
    path = Array(OPISKELIJAT_LAHTOKOULUT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee henkilön lähtökouluista johdetut oppilaitosten autorisoinnit",
    description = "Tuloslistassa oleva autorisointi tarkoittaa sitä että kyseisellä (rajatulla) ajanhetkellä oppilaitoksen tiettyjen henkilöiden (tyyppillisesti opot) " +
      "on oikeus katsella henkilöön liittyviä tietoja (hakemukset) sillä perusteella että henkilö on ollut oppilaitoksen opiskelija kyseisellä hetkellä tai lähimenneisyydessä. " +
      "Tätä autorisointitietoja käytetään hakemuspalvelussa pääsyn rajaamiseen. Mikäli halutaan hakea muita lähtökouluihin liittyviä tietoja, tähän on syytä käyttää (ja " +
      "tarvittaessa tehdä) muita rajapintoja. Ei ole esim. täysin poissuljettua että tulevaisuudessa henkilöllä voi samalle ajanhetkelle olla useita autorisoituja lähtökouluja.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa listan henkilön lähtökouluista seuraavista autorisoinneista", content = Array(new Content(schema = new Schema(implementation = classOf[LahtokoulutSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = LAHETTAVAT_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LahtokoulutFailureResponse])))),
      new ApiResponse(responseCode = "403", description = LAHETTAVAT_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeLahtokoulu(@PathVariable(name = OPISKELIJAT_HENKILOOID_PARAM_NAME, required = false) @Parameter(description = "Henkilön tunniste", example = ESIMERKKI_OPPIJANUMERO) henkiloOid: Optional[String],
                    request: HttpServletRequest): ResponseEntity[LahtokoulutResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = OPISKELIJAT_LAHTOKOULUT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onPalveluKayttaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // validoidaan parametrit
            val virheet: Set[String] = Validator.validateOppijanumero(henkiloOid.toScala, true)
            if (virheet.isEmpty)
              Right(henkiloOid.get)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LahtokoulutFailureResponse(virheet.asJava))))
          .map(henkiloOid => {
            try
              val user = AuditLog.getUser(request)
              AuditLog.log(user, Map(OPISKELIJAT_HENKILOOID_PARAM_NAME -> henkiloOid), AuditOperation.HaeLahtokoulut, None)
              LOG.info(s"Haetaan lähtökouluihin perustuvat autorisoinnit henkilölle $henkiloOid")

              val lahtokoulut = lahtokoulutService.haeLahtokouluAuthorizations(henkiloOid)
              ResponseEntity.status(HttpStatus.OK).body(LahtokoulutSuccessResponse(lahtokoulut.toList.asJava))
            catch
              case e: Exception =>
                LOG.error(s"Lähtökouluautorisointien haku henkilölle $henkiloOid epäonnistui", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LahtokoulutFailureResponse(Set(LAHETTAVAT_500_VIRHE).asJava))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LahtokoulutResponse]])
    catch
      case e: Exception =>
        LOG.error("Lähtökouluautorisointien haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LahtokoulutFailureResponse(java.util.Set.of(LAHETTAVAT_500_VIRHE)))
  }

  @GetMapping(
    path = Array(HAKEMUKSET_AVAINARVOT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee yhteishaun hakemuksen laskentaan käytettävät avainarvot",
    description = "Tämä rajapinta tuottaa hakemuspalvelulle tarvittavat toiseen asteen yhteishaun avainarvotiedot " +
      "jotta ne voidaan näyttää hakulomakkeella",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa hakemuksen avainarvotiedot", content = Array(new Content(schema = new Schema(implementation = classOf[AvainarvotSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = LAHETTAVAT_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[AvainarvotFailureResponse])))),
      new ApiResponse(responseCode = "403", description = LAHETTAVAT_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeLomakkeenTiedot(@PathVariable(name = HAKEMUKSET_HAKEMUS_PARAM_NAME, required = false) @Parameter(description = "Hakemuksen tunniste", example = ESIMERKKI_HAKEMUS_OID) hakemusOid: Optional[String],
                         request: HttpServletRequest): ResponseEntity[AvainarvotResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = HAKEMUKSET_AVAINARVOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onPalveluKayttaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // validoidaan parametrit
            val virheet: Set[String] = Validator.validateHakemusOid(hakemusOid.toScala, true)
            if (virheet.isEmpty)
              Right(hakemusOid.get)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AvainarvotFailureResponse(virheet.asJava))))
          .map(hakemusOid => {
            try
              val user = AuditLog.getUser(request)
              AuditLog.log(user, Map(HAKEMUKSET_HAKEMUS_PARAM_NAME -> hakemusOid), AuditOperation.HaeAvainarvot, None)
              LOG.info(s"Haetaan avainarvotiedot hakemukselle $hakemusOid")
              valintaDataService.getValintaData(hakemusOid).fold(
                virhe => ResponseEntity.status(HttpStatus.NOT_FOUND).body(AvainarvotFailureResponse(Set(virhe).asJava)),
                valintaData => ResponseEntity.status(HttpStatus.OK).body(AvainarvotSuccessResponse(valintaData.kaikkiAvainArvotFull().map(aa => aa.avain -> aa.arvo).toMap.asJava))
              )
            catch
              case e: Exception =>
                LOG.error(s"Avainarvotietojen haku hakemukselle $hakemusOid epäonnistui", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AvainarvotFailureResponse(Set(LAHETTAVAT_500_VIRHE).asJava))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[AvainarvotResponse]])
    catch
      case e: Exception =>
        LOG.error("Avainarvotietojen haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AvainarvotFailureResponse(java.util.Set.of(LAHETTAVAT_500_VIRHE)))
  }
}

