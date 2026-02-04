package fi.oph.suorituspalvelu.resource

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.mankeli.HarkinnanvaraisuusService
import fi.oph.suorituspalvelu.resource.ApiConstants.*
import fi.oph.suorituspalvelu.resource.api.{HakemustenHarkinnanvaraisuudetPayload, HarkinnanvaraisuusFailureResponse, HarkinnanvaraisuusResponse, HarkinnanvaraisuusSuccessResponse, ValintaApiHakemuksenHarkinnanvaraisuus, ValintaApiHakukohteenHarkinnanvaraisuus}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.{ValintaData, ValintaDataService}
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.*

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@RequestMapping(path = Array(""))
@RestController
@Tag(
  name = "Harkinnanvaraisuusrajapinnat",
  description = "Rajapinnat jotka palauttavat harkinnanvaraisuuteen liittyviä tietoja")
class HarkinnanvaraisuusResource {

  val LOG = LoggerFactory.getLogger(classOf[HarkinnanvaraisuusResource])

  val objectMapper: ObjectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  objectMapper.registerModule(Jdk8Module())
  objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
  objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
  objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)

  @Autowired val harkinnanvaraisuusService: HarkinnanvaraisuusService = null

  @PostMapping(
    path = Array(VALINNAT_HARKINNANVARAISUUS_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Palauttaa tiedot hakemusten harkinnanvaraisuuden syistä hakutoiveittain.",
    description = "Tiedot ovat yhdistelmä Supasta löytyviä suoritustietoja ja hakijan hakemuksella antamia vastauksia." +
      "Vaatii joko rekisterinpitäjän tai sisäisten rajapintojen palvelukäyttäjän oikeudet.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = Array(new Content(schema = new Schema(implementation = classOf[HakemustenHarkinnanvaraisuudetPayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Palauttaa hakemusten harkinnanvaraisuustiedot hakutoiveittain.", content = Array(new Content(schema = new Schema(implementation = classOf[HarkinnanvaraisuusSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = HARKINNANVARAISUUS_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[HarkinnanvaraisuusFailureResponse])))),
      new ApiResponse(responseCode = "403", description = HARKINNANVARAISUUS_RESPONSE_403_DESCRIPTION)
    ))
  def haeHakemuksenHarkinnanvaraisuudet(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[HarkinnanvaraisuusResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = VALINNAT_HARKINNANVARAISUUS_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja() || securityOperaatiot.onPalveluKayttaja() || securityOperaatiot.onValintaKayttaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(HarkinnanvaraisuusFailureResponse(java.util.List.of(HARKINNANVARAISUUS_EI_OIKEUKSIA)))))
        .flatMap(_ =>
          // deserialisoidaan
          try
            Right(objectMapper.readValue(bytes, classOf[HakemustenHarkinnanvaraisuudetPayload]))
          catch
            case e: Exception =>
              LOG.error("parametrin deserialisointi epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(HarkinnanvaraisuusFailureResponse(java.util.List.of(HARKINNANVARAISUUS_JSON_VIRHE)))))
        .flatMap(payload =>
          // validoidaan parametrit
          val hakemusOids = Option(payload.hakemusOids).map(_.asScala).getOrElse(Seq.empty)
          hakemusOids match {
            case hakemusOids if hakemusOids.size > HARKINNANVARAISUUS_HAKEMUKSET_MAX_MAARA =>
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(HarkinnanvaraisuusFailureResponse(java.util.List.of(HARKINNANVARAISUUS_HAKEMUKSET_LIIKAA))))
            case hakemusOids if hakemusOids.isEmpty =>
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(HarkinnanvaraisuusFailureResponse(java.util.List.of(HARKINNANVARAISUUS_PUUTTUVA_PARAMETRI))))
            case hakemusOids =>
              val virheet: Set[String] = hakemusOids.flatMap(o => Validator.validateHakemusOid(Some(o), true)).toSet
              if (virheet.nonEmpty)
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(HarkinnanvaraisuusFailureResponse(new java.util.ArrayList(virheet.asJava))))
              else
                Right(payload)
          })
        .map(payload => {
          try {
            val user = AuditLog.getUser(request)
            val hakemusOids = Option(payload.hakemusOids).map(_.asScala.toSet).getOrElse(Set.empty)
            AuditLog.log(
              user,
              Map("hakemusOids" -> hakemusOids.mkString("Array(", ", ", ")")),
              AuditOperation.HaeHakemustenHarkinnanvaraisuudet,
              None
            )
            LOG.info(s"Haetaan harkinnanvaraisuudet parametreille $payload")
            val result = harkinnanvaraisuusService.getHakemustenHarkinnanvaraisuudet(hakemusOids)
            val typedResult =
              result
                .map(hhv => {
                  ValintaApiHakemuksenHarkinnanvaraisuus(
                    hakemusOid = hhv.hakemusOid,
                    henkiloOid = hhv.henkiloOid,
                    hakutoiveet = hhv.hakutoiveet.map(
                      ht => ValintaApiHakukohteenHarkinnanvaraisuus(
                        hakukohdeOid = ht.hakukohdeOid,
                        harkinnanvaraisuudenSyy = ht.harkinnanvaraisuudenSyy.toString)
                    ).asJava
                  )
                }).toList.asJava
            val parsedResult = result.map(objectMapper.writeValueAsString(_)).toList.asJava
            LOG.info(s"Palautetaan rajapintavastaus, $parsedResult")
            ResponseEntity.status(HttpStatus.OK).body(typedResult)
          } catch {
            case e: Exception =>
              LOG.error(s"Harkinnanvaraisuustiedon hakeminen ${payload.hakemusOids.size} hakemukselle epäonnistui: ", e)
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HarkinnanvaraisuusFailureResponse(Seq(HARKINNANVARAISUUS_500_VIRHE).asJava))
          }
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[HarkinnanvaraisuusResponse]])
  }


}

