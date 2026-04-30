package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.resource.ApiConstants.{ESIMERKKI_HAKUKOHDE_OID, ESIMERKKI_HAKU_OID, ESIMERKKI_OPPIJANUMERO, YOS_EI_OIKEUKSIA, YOS_PATH, YOS_RESPONSE_403_DESCRIPTION}
import fi.oph.suorituspalvelu.resource.api.YosVirhe.VIRHE_PAATTYVIEN_OPISKELUOIKEUKSIEN_HAUSSA
import fi.oph.suorituspalvelu.resource.api.{ValintalaskentaDataResponse, YosErrorResponse, YosNimi, YosOpiskeluOikeus, YosResponse, YosSuccessResponse, YosVirhe}
import fi.oph.suorituspalvelu.security.SecurityOperaatiot
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.yos.{YosPaatettavaOpiskeluOikeus, YosService}
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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
                                            @PathVariable(required = true) @Parameter(description = "Hakukohteen tunniste", example = ESIMERKKI_HAKUKOHDE_OID) hakukohdeOid: String): ResponseEntity[YosResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = YOS_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          if (securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onPalveluKayttaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(YosErrorResponse(YosVirhe.PUUTTUVAT_OIKEUDET, YOS_EI_OIKEUKSIA))))
        .flatMap(_ => {
          LOG.info(s"Tarkistetaan kuuluuko vastaanotettava opiskelupaikka YOS piiriin. Parametrit = (hakija: $hakijaOid, haku: ${hakuOid}, hakukohde: ${hakukohdeOid}")
          yosService.kuuluukoVastaanotettavaHakutoiveYossinpiiriin(hakuOid, hakukohdeOid).fold(
            e => {
              LOG.error("Virhe vastaanotettavan hakutoiveen päättelyssä", e)
              Left(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(YosErrorResponse(YosVirhe.VIRHE_HAKUTOIVEEN_PAATTELYSSA, e.getMessage)))
            },
            r => Right(r)
          )
        }).flatMap(kuuluuYosPiiriin => {
          if (kuuluuYosPiiriin) {
            LOG.info(s"Vastaanotettava opiskelupaikka kuului YOS piiriin. Haetaan päätettävät opiskeluoikeudet. Parametrit = (hakija: $hakijaOid, haku: ${hakuOid}, hakukohde: ${hakukohdeOid}")
            yosService.hakijanPaatettavatOpiskeluOikeudet(hakijaOid).fold(
              e =>
                LOG.error("Virhe lopetettavien opiskeluoikeuksien haussa", e)
                Left(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(YosErrorResponse(VIRHE_PAATTYVIEN_OPISKELUOIKEUKSIEN_HAUSSA, e.getMessage))),
              r =>
                Right(YosSuccessResponse(r.map(oikeus => YosOpiskeluOikeus(
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
                  .toList.asJava))
            )
          } else {
            LOG.info(s"Vastaanotettava opiskelupaikka ei kuulunut YOS piiriin. Palautetaan tyhjä lista. Parametrit = (hakija: $hakijaOid, haku: ${hakuOid}, hakukohde: ${hakukohdeOid}")
            Right(YosSuccessResponse(new java.util.ArrayList()))
          }
        })
        .fold(e => e, r => ResponseEntity.ok(r)).asInstanceOf[ResponseEntity[YosResponse]])
  }

}
