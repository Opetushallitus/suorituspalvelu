package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.{ESIMERKKI_HAKUKOHDE_OID, ESIMERKKI_HAKU_OID, ESIMERKKI_OPPIJANUMERO, YOS_EI_OIKEUKSIA, YOS_PATH, YOS_RESPONSE_403_DESCRIPTION}
import fi.oph.suorituspalvelu.resource.api.{YosErrorResponse, YosNimi, YosOpiskeluOikeus, YosResponse, YosSuccessResponse, YosVirhe}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
import fi.oph.suorituspalvelu.yos.YosService
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, RequestMapping, RestController}

import scala.jdk.CollectionConverters.*

@RequestMapping(path = Array(YOS_PATH))
@RestController
@Tag(
  name = "YOS rajapinnat",
  description = "Rajapinnat joita tarvitaan YOS (Yhden Opiskeluoikeuden Säännös) logiikkaan")
class YosResource @Autowired (yosService: YosService) {

  val LOG: Logger = LoggerFactory.getLogger(classOf[YosResource])

  @GetMapping(
    path = Array("/hakija/{hakijaOid}/haku/{hakuOid}/hakukohde/{hakukohdeOid}/opiskeluoikeudet"),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee päätettävät opiskeluoikeudet hakijan vastaanottopaikalle.",
    description = "Päättelee onko vastaanotettava opiskeluoikeus YOS piirissä ja palauttaa hakijan päätettävät opiskeluoikeudet.\n" +
      "Vaatii joko rekisterinpitäjän tai sisäisten rajapintojen palvelukäyttäjän oikeudet.",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa päätettävät opiskeluoikeudet", content = Array(new Content(schema = new Schema(implementation = classOf[YosResponse])))),
      new ApiResponse(responseCode = "403", description = YOS_RESPONSE_403_DESCRIPTION)
    ))
  def haeHakijanPaatettavatOpiskeluOikeudet(@PathVariable(required = true) @Parameter(description = "Hakijan tunniste", example = ESIMERKKI_OPPIJANUMERO) hakijaOid: String,
                                            @PathVariable(required = true) @Parameter(description = "Haun tunniste", example = ESIMERKKI_HAKU_OID) hakuOid: String,
                                            @PathVariable(required = true) @Parameter(description = "Hakukohteen tunniste", example = ESIMERKKI_HAKUKOHDE_OID) hakukohdeOid: String,
                                            request: HttpServletRequest): ResponseEntity[YosResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = YOS_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          if (securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onPalveluKayttaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(YosErrorResponse(YosVirhe.PUUTTUVAT_OIKEUDET, YOS_EI_OIKEUKSIA))))
        .flatMap(_ => {
          val virheet: Set[String] = Validator.validateHenkiloOid(Some(hakijaOid), true) ++ Validator.validateHakukohdeOid(Some(hakukohdeOid), true) ++ Validator.validateHakuOid(Some(hakuOid), true)
          if (virheet.nonEmpty)
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YosErrorResponse(YosVirhe.PUUTTEELLISET_PARAMETRIT, virheet.reduce((a, b) => String.join(". ", a, b)))))
          else
            Right(None)
        })
        .flatMap(_ => {
          yosService.haeHakijanPaatettavatOpiskeluOikeudet(hakijaOid, hakuOid, hakukohdeOid).fold(
            e => {
              LOG.error(s"Virhe hakiessa hakijan päätettäviä opiskeluoikeuksia. ${e.virhe}: ${e.viesti}")
              Left(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e))
            },
            r => Right(YosSuccessResponse(r.map(oikeus => YosOpiskeluOikeus(
                  tunniste = oikeus.tunniste.toString,
                  organisaatioOid = oikeus.organisaatio.oid.getOrElse(""),
                  organisaatioNimi = YosNimi(
                    oikeus.organisaatio.nimi.fi.getOrElse(""),
                    oikeus.organisaatio.nimi.sv.getOrElse(""),
                    oikeus.organisaatio.nimi.en.getOrElse("")),
                  nimi = oikeus.nimi.map(
                    nimi => YosNimi(
                      nimi.fi.getOrElse(""),
                      nimi.sv.getOrElse(""),
                      nimi.en.getOrElse(""))).getOrElse(YosNimi("", "", "")),
                  koulutusKoodi = oikeus.koulutusKoodi.getOrElse("")
                ))
                .toList.asJava)))
        })
        .fold(e => e, r => {
          val user = AuditLog.getUser(request)
          AuditLog.log(
            user,
            Map(
              "hakijaOid" -> hakijaOid,
              "hakuOid" -> hakuOid,
              "hakukohdeOid" -> hakukohdeOid),
            AuditOperation.HaePaattyvatOpiskeluOikeudet,
            None
          )
          ResponseEntity.ok(r)
        }).asInstanceOf[ResponseEntity[YosResponse]])
  }

}
