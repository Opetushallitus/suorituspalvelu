package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, KantaOperaatiot, YOOpiskeluoikeus, YOTutkinto}
import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_RESPONSE_400_DESCRIPTION, DATASYNC_RESPONSE_403_DESCRIPTION, ESIMERKKI_HAKUKOHDE_OID, ESIMERKKI_HAKU_OID, HEALTHCHECK_PATH, LEGACY_OPPIJAT_ENSIKERTALAISUUDET_PARAM_NAME, LEGACY_OPPIJAT_HAKUKOHDE_PARAM_NAME, LEGACY_OPPIJAT_HAKU_PARAM_NAME, LEGACY_OPPIJAT_PATH, LEGACY_SUORITUKSET_HAKU_EPAONNISTUI, LEGACY_SUORITUKSET_HENKILO_PARAM_NAME, LEGACY_SUORITUKSET_JOKO_OID_TAI_PVM_PAKOLLINEN, LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME, LEGACY_SUORITUKSET_PATH}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.{Komot, LegacyOppijatService}
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.{ApiResponse, ApiResponses}
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.*
import slick.jdbc.JdbcBackend

import java.time.Instant
import java.util.Optional
import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@Schema(name = "Suoritus")
case class LegacySuoritus(
  @(Schema @field)(example = ApiConstants.ESIMERKKI_LEGACY_SUORITUSKIELI)
  @BeanProperty suoritusKieli: String,
  @(Schema @field)(example = Komot.perusopetus)
  @BeanProperty komo: String)

@Schema(name = "SuoritusJaArvosanat")
case class LegacySuoritusJaArvosanat(
  @BeanProperty suoritus: LegacySuoritus)

@Schema(name = "Oppija")
case class LegacyOppija(
  @(Schema @field)(example = ApiConstants.ESIMERKKI_OPPIJANUMERO)
  @BeanProperty oppijanumero: String,
  @BeanProperty suoritukset: java.util.Set[LegacySuoritusJaArvosanat])

@Schema(name = "OppijatFailureResponse")
case class LegacyOppijatFailureResponse(
  @(Schema @field)(example = Validator.VALIDATION_HAKUOID_EI_VALIDI)
  @BeanProperty virheet: java.util.Set[String])

@RequestMapping(path = Array(LEGACY_OPPIJAT_PATH))
@RestController
class LegacyOppijatResource {

  val LOG = LoggerFactory.getLogger(classOf[LegacyOppijatResource]);

  @Autowired val database: JdbcBackend.JdbcDatabaseDef = null

  @Autowired val legacyOppijatService: LegacyOppijatService = null

  @GetMapping(path = Array(""),
    produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @Operation(
    summary = "Tuottaa valintalaskennalle oppijan tiedot",
    description = "Valintalaskentakoostepalvelua varten tehty korvike suoritusrekisterin /rest/v1/oppijat-rajapinnalle jota hakemuspalvelu käyttää valintalaskennan lähtötietojen hakuun"
  )
  @ApiResponses(value = Array(
    new ApiResponse(responseCode = "200", description = "Palauttaa oppijat", content = Array(new Content(schema = new Schema(implementation = classOf[LegacyOppija])))),
    new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LegacyOppijatFailureResponse])))),
    new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
  ))
  def legacyOppijat(
                     @RequestParam(name = LEGACY_OPPIJAT_HAKU_PARAM_NAME, required = false) @Parameter(description = "haun oid", example = ESIMERKKI_HAKU_OID, required = true) hakuOid: Optional[String],
                     @RequestParam(name = LEGACY_OPPIJAT_HAKUKOHDE_PARAM_NAME, required = false) @Parameter(description = "hakukohteen oid", example = ESIMERKKI_HAKUKOHDE_OID) hakukohdeOid: Optional[String],
                     @RequestParam(name = LEGACY_OPPIJAT_ENSIKERTALAISUUDET_PARAM_NAME, required = false) @Parameter(description = "haetaanko myös ensikertalaisuustiedot") ensikertalaisuudet: Optional[java.lang.Boolean],
                     request: HttpServletRequest
  ): ResponseEntity[_] =
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = LEGACY_SUORITUKSET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if(securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(LegacyOppijatFailureResponse(java.util.Set.of("ei oikeuksia")))))
          .flatMap(_ =>
            // validoidaan parametrit
            val virheet = Set(
              Validator.validateHakuOid(hakuOid.toScala, true),
              Validator.validateHakukohdeOid(hakukohdeOid.toScala, false)
            ).flatten
            if(virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LegacyOppijatFailureResponse(virheet.asJava))))
          .map(_ =>
            // haetaan data
            val user = AuditLog.getUser(request)
            val kantaOperaatiot = KantaOperaatiot(database)

            AuditLog.log(user, Seq(
              hakuOid.toScala.map(h => LEGACY_OPPIJAT_HAKU_PARAM_NAME -> h),
              hakukohdeOid.toScala.map(h => LEGACY_OPPIJAT_HAKUKOHDE_PARAM_NAME -> h)
            ).flatten.toMap, AuditOperation.HaeOppijaTiedot, None)
            val oppijat = legacyOppijatService.getOppijat(hakuOid.get(), hakukohdeOid.toScala)
            ResponseEntity.status(HttpStatus.OK).body(oppijat.asJava)
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LegacyOppijatFailureResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijoiden haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LegacyOppijatFailureResponse(java.util.Set.of(LEGACY_SUORITUKSET_HAKU_EPAONNISTUI)))

}