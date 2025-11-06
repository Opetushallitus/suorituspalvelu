package fi.oph.suorituspalvelu.resource

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.resource.ApiConstants.*
import fi.oph.suorituspalvelu.resource.api.{ValintalaskentaDataFailureResponse, ValintalaskentaDataPayload, ValintalaskentaDataResponse, ValintalaskentaDataSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.{ValintaData, ValintaDataService, ValintalaskentaHakemus}
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.*

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@RequestMapping(path = Array(""))
@RestController
@Tag(
  name = "Valintalaskennan rajapinnat",
  description = "Rajapinnat jotka palauttavat Suorituspalvelun ja Hakemuspalvelun dataa Valintalaskennan ymmärtämässä muodossa")
class ValintalaskentaResource {

  val LOG = LoggerFactory.getLogger(classOf[ValintalaskentaResource])

  val objectMapper: ObjectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  objectMapper.registerModule(Jdk8Module())
  objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
  objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
  objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)

  @Autowired val valintaDataService: ValintaDataService = null

  @PostMapping(
    path = Array(VALINTALASKENTA_VALINTADATA_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee Valintalaskennan laskentaa varten tarvitsemat tiedot hakukohteen hakijoille.",
    description = "Tiedot sisältävät sekä Supan tiedoista pääteltyjä arvoja että Ataru-hakemusten arvoja.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = Array(new Content(schema = new Schema(implementation = classOf[ValintalaskentaDataPayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa valintadataa Valintalaskennan ymmärtämässä muodossa"),
      new ApiResponse(responseCode = "400", description = VALINTALASKENTA_RESPONSE_400_DESCRIPTION),
      new ApiResponse(responseCode = "403", description = VALINTALASKENTA_RESPONSE_403_DESCRIPTION)
    ))
  def haeHakijoidenValintalaskentaData(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[ValintalaskentaDataResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = KOSKI_DATASYNC_HENKILOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onPalveluKayttaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(ValintalaskentaDataFailureResponse(java.util.List.of(VALINTALASKENTA_EI_OIKEUKSIA)))))
        .flatMap(_ =>
          // deserialisoidaan
          try
            Right(objectMapper.readValue(bytes, classOf[ValintalaskentaDataPayload]))
          catch
            case e: Exception =>
              LOG.error("parametrin deserialisointi epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ValintalaskentaDataFailureResponse(java.util.List.of(VALINTALASKENTA_JSON_VIRHE)))))
        .flatMap(payload =>
          // validoidaan parametrit
          (payload.hakuOid.toScala, payload.hakukohdeOid.toScala, payload.hakemusOids.asScala) match {
            case (None, _, _) =>
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ValintalaskentaDataFailureResponse(java.util.List.of(VALINTALASKENTA_HAKUOID_PAKOLLINEN))))
            case (_, Some(hakukohdeOid), hakemusOids) if hakemusOids.nonEmpty =>
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ValintalaskentaDataFailureResponse(java.util.List.of(VALINTALASKENTA_LIIKAA_PARAMETREJA))))
            case (_, _, hakemusOids) if hakemusOids.size > VALINTALASKENTA_HAKEMUKSET_MAX_MAARA =>
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ValintalaskentaDataFailureResponse(java.util.List.of(VALINTALASKENTA_HAKEMUKSET_LIIKAA))))
            case (_, None, hakemusOids) if hakemusOids.isEmpty =>
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ValintalaskentaDataFailureResponse(java.util.List.of(VALINTALASKENTA_PUUTTUVA_PARAMETRI))))
            case (hakuOid, hakukohdeOid, hakemusOids) =>
              val virheet: Set[String] = hakemusOids.flatMap(o => Validator.validateHakemusOid(Some(o), true)).toSet ++ Validator.validateHakukohdeOid(hakukohdeOid, false) ++ Validator.validateHakuOid(hakuOid, true)
              if (virheet.nonEmpty)
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ValintalaskentaDataFailureResponse(new java.util.ArrayList(virheet.asJava))))
              else
                Right(payload)
          })
        .map(payload => {
          try {
            val user = AuditLog.getUser(request)
            AuditLog.log(
              user,
              Map(
                "hakuOid" -> payload.hakuOid.get(),
                "hakemusOids" -> payload.hakemusOids.asScala.mkString("Array(", ", ", ")"),
                "hakukohdeOid" -> payload.hakukohdeOid.toScala.getOrElse("")),
              AuditOperation.HaeValintadata,
              None
            )
            LOG.info(s"Haetaan valintalaskennan tarvitsemat tiedot parametreille $payload")
            val result: Seq[ValintalaskentaHakemus] = valintaDataService.getValintalaskentaHakemukset(payload.hakuOid.get, payload.hakukohdeOid.toScala, payload.hakemusOids.asScala.toSet)
            val parsedResult = result.map(objectMapper.writeValueAsString(_)).toList.asJava
            LOG.info(s"Palautetaan rajapintavastaus, $parsedResult")
            //val schematizedResult = ValintalaskentaHakemusConverter.toSchematizedList(result)

            //ResponseEntity.status(HttpStatus.OK).body(ValintalaskentaDataSuccessResponse(schematizedResult))

            //ResponseEntity.status(HttpStatus.OK).body(ValintalaskentaDataSuccessResponse(result.map(_.toString).toList.asJava))

            ResponseEntity.status(HttpStatus.OK).body(ValintalaskentaDataSuccessResponse(parsedResult))


          } catch {
            case e: Exception =>
              LOG.error(s"ValintaDatan hakeminen haun ${payload.hakuOid.get()} hakukohteen ${payload.hakukohdeOid.toScala.getOrElse("")} hakemuksille ${payload.hakemusOids.asScala} epäonnistui", e)
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ValintalaskentaDataFailureResponse(Seq(VALINTALASKENTA_500_VIRHE).asJava))
          }
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[ValintalaskentaDataResponse]])
  }


}

