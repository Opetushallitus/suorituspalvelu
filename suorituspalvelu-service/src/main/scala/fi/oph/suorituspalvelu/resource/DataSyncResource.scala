package fi.oph.suorituspalvelu.resource

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.integration.ytr.YtrIntegration
import fi.oph.suorituspalvelu.integration.SyncResultForHenkilo
import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_JSON_VIRHE, DATASYNC_RESPONSE_400_DESCRIPTION, DATASYNC_RESPONSE_403_DESCRIPTION, KOSKI_DATASYNC_500_VIRHE, KOSKI_DATASYNC_HAKU_PATH, KOSKI_DATASYNC_HENKILOT_LIIKAA, KOSKI_DATASYNC_HENKILOT_MAX_MAARA, KOSKI_DATASYNC_HENKILOT_PATH, KOSKI_DATASYNC_MUUTTUNEET_PATH, KOSKI_DATASYNC_RETRY_PATH, VIRTA_DATASYNC_HAKU_PATH, VIRTA_DATASYNC_HENKILO_PATH, VIRTA_DATASYNC_JOBIN_LUONTI_EPAONNISTUI, YTR_DATASYNC_HAKU_PATH, YTR_DATASYNC_HENKILOT_PATH}
import fi.oph.suorituspalvelu.resource.api.{KoskiHaeMuuttuneetJalkeenPayload, KoskiPaivitaTiedotHaullePayload, KoskiPaivitaTiedotHenkiloillePayload, KoskiRetryPayload, KoskiSyncFailureResponse, KoskiSyncSuccessResponse, SyncResponse, VirtaPaivitaTiedotHaullePayload, VirtaPaivitaTiedotHenkilollePayload, VirtaSyncFailureResponse, VirtaSyncSuccessResponse, YTRPaivitaTiedotHaullePayload, YTRPaivitaTiedotHenkilollePayload, YtrSyncFailureResponse, YtrSyncSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.{KoskiService, VirtaService}
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RequestMapping, RestController}

import java.time.Instant
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

@RequestMapping(path = Array(""))
@RestController
@Tag(
  name = "Manuaalinen tietojen päivitys",
  description = "Tietojen päivitys KOSKI-, VIRTA-, ja YRT-järjestelmissä tapahtuu SUPAssa lähtökohtaiseksi automaattisesti, " +
    "perustuen joko muuttuneiden tietojen pollaamiseen lähdejärjestelmistä, tai eräajoihin. Nämä rajapinnat tarjoavat " +
    "kuitenkin mahdollisuuden tehdä tietojen päivitys manuaalisesti virheiden selvittämistä tai korjaamista " +
    "varten. Vain rekisterinpitäjällä on pääsy näihin rajapintoihin.")
class DataSyncResource {

  val LOG = LoggerFactory.getLogger(classOf[DataSyncResource])

  @Autowired var mapper: ObjectMapper = null

  @Autowired var koskiService: KoskiService = null

  @Autowired var virtaService: VirtaService = null

  @Autowired var ytrIntegration: YtrIntegration = null

  @Autowired var objectMapper: ObjectMapper = null

  @PostMapping(
    path = Array(KOSKI_DATASYNC_HENKILOT_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää yksittäisten henkilöiden tiedot Koskesta",
    description = "SUPA seuraa KOSKI-tietoihin tapahtuvia muutoksia, ja tietojen päivitys SUPAan tapahtuu normaalisti\n" +
      "näiden muutosten seurauksena. Tämän endpointin avulla päivitys on kuitenkin mahdollista tehdä manuaalisesti esim.\n" +
      "virheiden selvittämistä tai nopeaa korjaamista varten.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[KoskiPaivitaTiedotHenkiloillePayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkronointi tehty, palauttaa listan henkilöOideista joille päivitys onnistui ja listan virheistä",
        content = Array(new Content(schema = new Schema(implementation = classOf[KoskiSyncSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[KoskiSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaKoskiTiedotHenkiloille(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = KOSKI_DATASYNC_HENKILOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
        .flatMap(_ =>
          // deserialisoidaan
          try
            Right(objectMapper.readValue(bytes, classOf[KoskiPaivitaTiedotHenkiloillePayload]).henkiloOidit)
          catch
            case e: Exception =>
              LOG.error("payloadin deserialisointi KOSKI-tietojen päivittämisessä henkilöille epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
        .flatMap(personOids =>
          // validoidaan parametri
          if (personOids.toScala.map(_.size()).getOrElse(0) > KOSKI_DATASYNC_HENKILOT_MAX_MAARA) {
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(KOSKI_DATASYNC_HENKILOT_LIIKAA))))
          } else {
            val virheet: Set[String] = personOids.toScala
              .map(oids => oids.asScala.flatMap(o => Validator.validateOppijanumero(Some(o), true)).toSet)
              .getOrElse(Set(Validator.VALIDATION_OPPIJANUMERO_TYHJA))
            if (virheet.isEmpty)
              Right(personOids.get.asScala)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(new java.util.ArrayList(virheet.asJava))))
          })
        .map(personOids => {
          try
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("personOids" -> personOids.mkString("Array(", ", ", ")")), AuditOperation.PaivitaKoskiTiedotHenkiloille, None)
            LOG.info(s"Haetaan Koski-tiedot henkilöille ${personOids.mkString("Array(", ", ", ")")}")
            val (changed, exceptions) = koskiService.syncKoskiForOppijat(personOids.toSet).foldLeft(Set.empty[SyncResultForHenkilo])((s, r) => s ++ Set(r))
              .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0) }))
            LOG.info(s"Tallennettiin yhteensä ${changed} versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia.")
            ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(changed, exceptions))
          catch
            case e: Exception =>
              LOG.error(s"KOSKI-tietojen päivitys oppijoille ${personOids.mkString(",")} epäonnistui", e)
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(KOSKI_DATASYNC_HAKU_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää haun hakijoiden tiedot Koskesta",
    description = "SUPA seuraa KOSKI-tietoihin tapahtuvia muutoksia, ja tietojen päivitys SUPAan tapahtuu normaalisti\n" +
      "näiden muutosten seurauksena. Tämän endpointin avulla päivitys on kuitenkin mahdollista tehdä manuaalisesti esim.\n" +
      "virheiden selvittämistä tai nopeaa korjaamista varten.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[KoskiPaivitaTiedotHaullePayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkronointi tehty, palauttaa onnistuneiden ja epäonnistuneiden henkilöpäivitysten määrän",
        content = Array(new Content(schema = new Schema(implementation = classOf[KoskiSyncSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[KoskiSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaKoskiTiedotHaulle(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = KOSKI_DATASYNC_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
        .flatMap(_ =>
          // deserialisoidaan
          try
            Right(objectMapper.readValue(bytes, classOf[KoskiPaivitaTiedotHaullePayload]).hakuOid)
          catch
            case e: Exception =>
              LOG.error("payloadin deserialisointi KOSKI-tietojen päivittämisessä haulle epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
        .flatMap(hakuOid =>
          // validoidaan parametri
          val virheet = Validator.validateHakuOid(hakuOid.toScala, true)
          if (virheet.isEmpty)
            Right(hakuOid.get)
          else
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
        .map(hakuOid => {
          try
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("hakuOid" -> hakuOid), AuditOperation.PaivitaKoskiTiedotHaunHakijoille, None)
            LOG.info(s"Haetaan Koski-tiedot haun $hakuOid henkilöille")
            val (changed, exceptions) = koskiService.syncKoskiForHaku(hakuOid)
              .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0) }))
            LOG.info(s"Tallennettiin haulle $hakuOid yhteensä ${changed} versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia.")
            ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(changed, exceptions))
          catch
            case e: Exception =>
              LOG.error(s"KOSKI-tietojen päivitys haulle ${hakuOid} epäonnistui", e)
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(KOSKI_DATASYNC_MUUTTUNEET_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää määritellyn aikaleiman jälkeen muuttuneet tiedot Koskesta",
    description = "Hakee KOSKI-järjestelmästä määritellyn ajankohdan jälkeen muuttuneet tiedot ja päivittää ne SUPAan " +
      "niiden henkilöiden osalta jotka ovat lähettävien koulujen seurannassa tai aktiivisessa haussa. SUPA seuraa KOSKI-" +
      "tietoihin tapahtuvia muutoksia, ja tietojen päivitys SUPAan tapahtuu normaalisti näiden muutosten seurauksena. " +
      "Tämän endpointin avulla päivitys on kuitenkin mahdollista tehdä manuaalisesti esim. virheiden selvittämistä tai " +
      "nopeaa korjaamista varten, tai kun tietoja ladataan SUPAan ensimmäistä kertaa.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[KoskiHaeMuuttuneetJalkeenPayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkronointi tehty, palauttaa onnistuneiden ja epäonnistuneiden henkilöpäivitysten määrän",
        content = Array(new Content(schema = new Schema(implementation = classOf[KoskiSyncSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[KoskiSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaKoskiTiedotMuuttuneet(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = KOSKI_DATASYNC_MUUTTUNEET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
        .flatMap(_ =>
          // deserialisoidaan
          try
            Right(objectMapper.readValue(bytes, classOf[KoskiHaeMuuttuneetJalkeenPayload]).aikaleima)
          catch
            case e: Exception =>
              LOG.error("payloadin deserialisointi muuttuneiden KOSKI-tietojen päivittämisessä epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
        .flatMap(aikaleima =>
          // validoidaan parametri
          val virheet = Validator.validateMuokattujalkeen(aikaleima.toScala, true)
          if(virheet.isEmpty)
            Right(Instant.parse(aikaleima.get))
          else
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
        .map(timestamp => {
          try
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("timestamp" -> timestamp.toString), AuditOperation.PaivitaMuuttuneetKoskiTiedot, None)
            LOG.info(s"Haetaan ${timestamp} jälkeen muuttuneet Koski-tiedot")
            val (changed, exceptions) = koskiService.syncKoskiChangesSince(timestamp)
              .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0) }))
            LOG.info(s"Tallennettiin yhteensä ${changed} muuttunutta versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia.")
            ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(changed, exceptions))
          catch
            case e: Exception =>
              LOG.error("Muuttuneiden KOSKI-tietojen haku epäonnistui", e)
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(KOSKI_DATASYNC_RETRY_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Yrittää uudelleen epäonnistuneiden KOSKI-massaluovutusrajapinnan tulostiedostojen prosessointia",
    description = "KOSKI-järjestelmä tuo massaluovutusrajanpintaan tehtyjen kyselyiden tulokset saataville tiedostoina. " +
      "Mikäli yksittäisten tiedostojen prosessointi epäonnistuu, niitä voi yrittää uudestaan tämän rajapinnan kautta.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[KoskiRetryPayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkronointi tehty, palauttaa onnistuneiden ja epäonnistuneiden henkilöpäivitysten määrän",
        content = Array(new Content(schema = new Schema(implementation = classOf[KoskiSyncSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[KoskiSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaKoskiTiedotRetry(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = KOSKI_DATASYNC_MUUTTUNEET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
        .flatMap(_ =>
          // deserialisoidaan
          try
            Right(objectMapper.readValue(bytes, classOf[KoskiRetryPayload]).tiedostot)
          catch
            case e: Exception =>
              LOG.error("payloadin deserialisointi KOSKI-tulostiedoston prosessoinnissa epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
        .flatMap(tiedostot =>
          // validoidaan parametrit
          if(tiedostot.isEmpty)
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(List("Tiedostoja ei määritelty").asJava)))
          else
            val virheet = tiedostot.get.asScala.flatMap(tiedosto => Validator.validateUrl(tiedosto)).toSet
            if(virheet.isEmpty)
              Right(tiedostot.get.asScala.toSeq)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(virheet.toList.asJava))))
        .map(tiedostot => {
          try
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("tiedostot" -> tiedostot.mkString(",")), AuditOperation.RetryKoskiTiedosto, None)
            LOG.info("Yritetään prosessoida uudelleen seuraavia KOSKI-massaluovutushaun tulostiedostoja: " + tiedostot.mkString(", "))
            val (changed, exceptions) = koskiService.retryKoskiResultFiles(tiedostot)
              .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0) }))
            LOG.info(s"Tallennettiin yhteensä ${changed} muuttunutta versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia.")
            ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(changed, exceptions))
          catch
            case e: Exception =>
              LOG.error(s"KOSKI-tiedostojen (${tiedostot.mkString(",")}) uudelleenprosessointi epäonnistui", e)
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(VIRTA_DATASYNC_HENKILO_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää yksittäisen oppijan tiedot Virrasta",
    description = "Tietojen päivitys SUPAan tapahtuu normaalisti eräajolla. Tämän endpointin avulla päivitys on kuitenkin " +
      "mahdollista tehdä manuaalisesti esim. virheiden selvittämistä tai nopeaa korjaamista varten.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[VirtaPaivitaTiedotHenkilollePayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus käynnistetty, palauttaa job-id:n", content = Array(new Content(schema = new Schema(implementation = classOf[VirtaSyncSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[VirtaSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaVirtaTiedotHenkilolle(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = VIRTA_DATASYNC_HENKILO_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // deserialisoidaan
            try
              Right(objectMapper.readValue(bytes, classOf[VirtaPaivitaTiedotHenkilollePayload]).henkiloOid.toScala)
            catch
              case e: Exception =>
                LOG.error("payloadin deserialisointi KOSKI-tietojen päivittämisessä henkilöille epäonnistui", e)
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
          .flatMap(henkiloOid =>
            // validoidaan parametri
            val virheet = Validator.validateOppijanumero(henkiloOid, true)
            if (virheet.isEmpty)
              Right(henkiloOid.get)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VirtaSyncFailureResponse(java.util.List.of(Validator.VALIDATION_OPPIJANUMERO_EI_VALIDI)))))
          .map(henkiloOid =>
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("henkiloOid" -> henkiloOid), AuditOperation.PaivitaVirtaTiedot, None)
            LOG.info(s"Haetaan Virta-tiedot henkilölle ${henkiloOid}")
            val jobId = virtaService.syncVirta(henkiloOid)
            LOG.info(s"Palautetaan rajapintavastaus, $jobId")
            ResponseEntity.status(HttpStatus.OK).body(VirtaSyncSuccessResponse(jobId)))
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijan Virta-päivitysjobin luonti epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(VirtaSyncFailureResponse(java.util.List.of(VIRTA_DATASYNC_JOBIN_LUONTI_EPAONNISTUI)))
  }

  @PostMapping(
    path = Array(VIRTA_DATASYNC_HAKU_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää yksittäisen haun hakijoiden tiedot Virrasta",
    description = "Tietojen päivitys SUPAan tapahtuu normaalisti eräajolla. Tämän endpointin avulla päivitys on kuitenkin " +
      "mahdollista tehdä manuaalisesti esim. virheiden selvittämistä tai nopeaa korjaamista varten.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[VirtaPaivitaTiedotHaullePayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus käynnistetty, palauttaa job-id:n", content = Array(new Content(schema = new Schema(implementation = classOf[VirtaSyncSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[VirtaSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaVirtaTiedotHaulle(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = VIRTA_DATASYNC_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
        .flatMap(_ =>
          // deserialisoidaan
          try
            Right(objectMapper.readValue(bytes, classOf[VirtaPaivitaTiedotHaullePayload]).hakuOid.toScala)
          catch
            case e: Exception =>
              LOG.error("payloadin deserialisointi KOSKI-tietojen päivittämisessä haulle epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
        .flatMap(hakuOid =>
          // validoidaan parametri
          val virheet = Validator.validateHakuOid(hakuOid, true)
          if (virheet.isEmpty)
            Right(hakuOid.get)
          else
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VirtaSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
        .map(hakuOid => {
          val user = AuditLog.getUser(request)
          AuditLog.log(user, Map("hakuOid" -> hakuOid), AuditOperation.PaivitaVirtaTiedotHaunHakijoille, None)
          LOG.info(s"Haetaan Virta-tiedot haun $hakuOid henkilöille")
          val jobId = virtaService.syncVirtaForHaku(hakuOid)
          LOG.info(s"Palautetaan rajapintavastaus, $jobId")
          ResponseEntity.status(HttpStatus.OK).body(VirtaSyncSuccessResponse(jobId))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(YTR_DATASYNC_HENKILOT_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää yksittäisten henkilöiden tiedot YTR:stä",
    description = "Tietojen päivitys YTR:stä SUPAan tapahtuu normaalisti eräajona. Tämän endpointin avulla päivitys on " +
      "kuitenkin mahdollista tehdä manuaalisesti esim. virheiden selvittämistä tai nopeaa korjaamista varten.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[YTRPaivitaTiedotHenkilollePayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkronointi tehty, palauttaa onnistuneiden ja epäonnistuneiden henkilöpäivitysten määrän",
        content = Array(new Content(schema = new Schema(implementation = classOf[YtrSyncSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[YtrSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaYtrTiedotHenkiloille(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = YTR_DATASYNC_HENKILOT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
        .flatMap(_ =>
          // deserialisoidaan
          try
            Right(objectMapper.readValue(bytes, classOf[YTRPaivitaTiedotHenkilollePayload]).henkiloOids.toScala.map(l => l.asScala.toList))
          catch
            case e: Exception =>
              LOG.error("payloadin deserialisointi YTR-tietojen päivittämisessä henkilöille epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
        .flatMap(personOids =>
          // validoidaan parametri
          if (personOids.isEmpty)
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YtrSyncFailureResponse(java.util.List.of(Validator.VALIDATION_OPPIJANUMERO_TYHJA))))
          else if (personOids.toSet.size > 5000)
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YtrSyncFailureResponse(java.util.List.of("Korkeintaan 5000 henkilöä kerrallaan"))))
          else
            val virheet: Set[String] = personOids.get.flatMap(oid => Validator.validateOppijanumero(Some(oid), true)).toSet
            if (virheet.isEmpty)
              Right(personOids.get)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YtrSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
        .map(personOids => {
          val user = AuditLog.getUser(request)
          AuditLog.log(user, Map("personOids" -> personOids.mkString("Array(", ", ", ")")), AuditOperation.PaivitaYtrTiedotHenkiloille, None)
          LOG.info(s"Haetaan Ytr-tiedot henkilöille ${personOids.mkString("Array(", ", ", ")")}")
          val (changed, exceptions) = ytrIntegration.fetchAndPersistStudents(personOids.toSet)
            .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0) }))
          LOG.info(s"Tallennettiin yhteensä ${changed} muuttunutta versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia.")
          ResponseEntity.status(HttpStatus.OK).body(YtrSyncSuccessResponse(changed, exceptions))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }

  @PostMapping(
    path = Array(YTR_DATASYNC_HAKU_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää yksittäisen haun hakijoiden tiedot YTR:stä",
    description = "SUPAan tapahtuu normaalisti eräajona. Tämän endpointin avulla päivitys on kuitenkin mahdollista tehdä " +
      "manuaalisesti esim. virheiden selvittämistä tai nopeaa korjaamista varten.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[YTRPaivitaTiedotHaullePayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkronointi tehty, palauttaa onnistuneiden ja epäonnistuneiden henkilöpäivitysten määrän",
        content = Array(new Content(schema = new Schema(implementation = classOf[YtrSyncSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[YtrSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaYtrTiedotHaulle(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    val securityOperaatiot = new SecurityOperaatiot
    LogContext(path = YTR_DATASYNC_HAKU_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
      Right(None)
        .flatMap(_ =>
          // tarkastetaan oikeudet
          if (securityOperaatiot.onRekisterinpitaja())
            Right(None)
          else
            Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
        .flatMap(_ =>
          // deserialisoidaan
          try
            Right(objectMapper.readValue(bytes, classOf[YTRPaivitaTiedotHaullePayload]).hakuOid.toScala)
          catch
            case e: Exception =>
              LOG.error("payloadin deserialisointi YTR-tietojen päivittämisessä haulle epäonnistui", e)
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
        .flatMap(hakuOid =>
          // validoidaan parametri
          val virheet = Validator.validateHakuOid(hakuOid, true)
          if (virheet.isEmpty)
            Right(hakuOid.get)
          else
            Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YtrSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
        .map(hakuOid => {
          val user = AuditLog.getUser(request)
          AuditLog.log(user, Map("hakuOid" -> hakuOid), AuditOperation.PaivitaYtrTiedotHaunHakijoille, None)
          LOG.info(s"Haetaan Ytr-tiedot haun $hakuOid henkilöille")

          val (changed, exceptions) = ytrIntegration.syncYtrForHaku(hakuOid)
            .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0) }))
          LOG.info(s"Tallennettiin yhteensä ${changed} muuttunutta versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia.")
          ResponseEntity.status(HttpStatus.OK).body(YtrSyncSuccessResponse(changed, exceptions))
        })
        .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
  }
}

