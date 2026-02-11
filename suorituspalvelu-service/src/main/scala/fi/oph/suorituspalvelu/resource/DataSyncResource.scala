package fi.oph.suorituspalvelu.resource

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.business.KantaOperaatiot
import fi.oph.suorituspalvelu.integration.ytr.YtrIntegration

import java.util.{Optional, UUID}
import fi.oph.suorituspalvelu.integration.SyncResultForHenkilo
import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_JOBIEN_TIETOJEN_HAKU_EPAONNISTUI, DATASYNC_JOBIN_LUONTI_EPAONNISTUI, DATASYNC_JOBIT_NIMI_PARAM_NAME, DATASYNC_JOBIT_PATH, DATASYNC_JOBIT_TUNNISTE_PARAM_NAME, DATASYNC_JSON_VIRHE, DATASYNC_RESPONSE_400_DESCRIPTION, DATASYNC_RESPONSE_403_DESCRIPTION, DATASYNC_UUDELLEENPARSEROINTI_EPAONNISTUI, DATASYNC_UUDELLEENPARSEROI_PATH, ESIMERKKI_JOB_NIMI, ESIMERKKI_JOB_TUNNISTE, KOSKI_DATASYNC_500_VIRHE, KOSKI_DATASYNC_HAUT_PATH, KOSKI_DATASYNC_HENKILOT_LIIKAA, KOSKI_DATASYNC_HENKILOT_MAX_MAARA, KOSKI_DATASYNC_HENKILOT_PATH, KOSKI_DATASYNC_MUUTTUNEET_PATH, KOSKI_DATASYNC_RETRY_PATH, VIRTA_DATASYNC_AKTIIVISET_PATH, VIRTA_DATASYNC_HAUT_PATH, VIRTA_DATASYNC_HENKILO_PATH, VIRTA_DATASYNC_PARAM_NAME, YTR_DATASYNC_500_VIRHE, YTR_DATASYNC_AKTIIVISET_PATH, YTR_DATASYNC_HAUT_PATH, YTR_DATASYNC_HENKILOT_PATH}
import fi.oph.suorituspalvelu.resource.api.{KoskiHaeMuuttuneetJalkeenPayload, KoskiPaivitaTiedotHaullePayload, KoskiPaivitaTiedotHenkiloillePayload, KoskiRetryPayload, KoskiSyncFailureResponse, KoskiSyncSuccessResponse, ReparseFailureResponse, ReparsePayload, ReparseSuccessResponse, SyncJob, SyncJobFailureResponse, SyncJobStatusResponse, SyncResponse, SyncSuccessJobResponse, VirtaPaivitaTiedotHaullePayload, VirtaPaivitaTiedotHenkilollePayload, VirtaSyncFailureResponse, YTRPaivitaTiedotHaullePayload, YTRPaivitaTiedotHenkilollePayload, YtrSyncFailureResponse, YtrSyncSuccessResponse}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
import fi.oph.suorituspalvelu.service.{KoskiService, ReparseService, VirtaService, YTRService}
import fi.oph.suorituspalvelu.util.LogContext
import fi.oph.suorituspalvelu.validation.Validator
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation.{GetMapping, PostMapping, RequestBody, RequestMapping, RequestParam, RestController}

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

  @Autowired var ytrService: YTRService = null

  @Autowired var reparseService: ReparseService = null

  @Autowired var ytrIntegration: YtrIntegration = null

  @Autowired var objectMapper: ObjectMapper = null

  @Autowired var kantaOperaatiot: KantaOperaatiot = null

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
    try
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
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("personOids" -> personOids.mkString("Array(", ", ", ")")), AuditOperation.PaivitaKoskiTiedotHenkiloille, None)
            LOG.info(s"Haetaan Koski-tiedot henkilöille ${personOids.mkString("Array(", ", ", ")")}")
            val (changed, exceptions) = koskiService.syncKoskiForHenkilot(personOids.toSet).foldLeft(Set.empty[SyncResultForHenkilo])((s, r) => s ++ Set(r))
              .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0) }))
            LOG.info(s"Tallennettiin yhteensä ${changed} versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia.")
            ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(changed, exceptions))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Henkilöiden tietojen päivitys KOSKI-järjestelmästä epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava))
  }

  @PostMapping(
    path = Array(KOSKI_DATASYNC_HAUT_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää hakujen hakijoiden tiedot Koskesta",
    description = "SUPA seuraa KOSKI-tietoihin tapahtuvia muutoksia, ja tietojen päivitys SUPAan tapahtuu normaalisti\n" +
      "näiden muutosten seurauksena. Tämän endpointin avulla päivitys on kuitenkin mahdollista tehdä manuaalisesti esim.\n" +
      "virheiden selvittämistä tai nopeaa korjaamista varten.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[KoskiPaivitaTiedotHaullePayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus käynnistetty, palauttaa job-id:n", content = Array(new Content(schema = new Schema(implementation = classOf[SyncSuccessJobResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[KoskiSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaKoskiTiedotHaulle(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = KOSKI_DATASYNC_HAUT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
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
              Right(objectMapper.readValue(bytes, classOf[KoskiPaivitaTiedotHaullePayload]).hakuOids)
            catch
              case e: Exception =>
                LOG.error("payloadin deserialisointi KOSKI-tietojen päivittämisessä hauille epäonnistui", e)
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
          .flatMap(hakuOids =>
            // validoidaan parametri
            val virheet = hakuOids.toScala.map(oids => oids.asScala.flatMap(oid => Validator.validateHakuOid(Some(oid), true)).toSet)
              .getOrElse(Set(Validator.VALIDATION_HAKUOID_TYHJA))
            if (virheet.isEmpty)
              Right(hakuOids.get.asScala.toSet)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
          .map(hakuOids => {
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("hakuOids" -> hakuOids.mkString(",")), AuditOperation.PaivitaKoskiTiedotHaunHakijoille, None)
            LOG.info(s"Haetaan Koski-tiedot hakujen ${hakuOids.mkString(",")} henkilöille")
            val jobId = koskiService.startRefreshKoskiForHaut(hakuOids)
            ResponseEntity.status(HttpStatus.OK).body(SyncSuccessJobResponse(jobId))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error(s"KOSKI-tietojen päivitys hauille epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava))
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
      new ApiResponse(responseCode = "200", description = "Synkkaus käynnistetty, palauttaa job-id:n", content = Array(new Content(schema = new Schema(implementation = classOf[SyncSuccessJobResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[KoskiSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaKoskiTiedotMuuttuneet(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    try
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
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("timestamp" -> timestamp.toString), AuditOperation.PaivitaMuuttuneetKoskiTiedot, None)
            LOG.info(s"Haetaan ${timestamp} jälkeen muuttuneet Koski-tiedot")
            val jobId = koskiService.startRefreshForKoskiChangesSince(timestamp)
            ResponseEntity.status(HttpStatus.OK).body(SyncSuccessJobResponse(jobId))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Muuttuneiden KOSKI-tietojen haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava))
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
    try
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
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("tiedostot" -> tiedostot.mkString(",")), AuditOperation.RetryKoskiTiedosto, None)
            LOG.info("Yritetään prosessoida uudelleen seuraavia KOSKI-massaluovutushaun tulostiedostoja: " + tiedostot.mkString(", "))
            val (changed, exceptions) = koskiService.retryKoskiResultFiles(tiedostot)
              .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0) }))
            LOG.info(s"Tallennettiin yhteensä ${changed} muuttunutta versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia.")
            ResponseEntity.status(HttpStatus.OK).body(KoskiSyncSuccessResponse(changed, exceptions))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error(s"KOSKI-tiedostojen uudelleenprosessointi epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(KoskiSyncFailureResponse(Seq(KOSKI_DATASYNC_500_VIRHE).asJava))
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
      new ApiResponse(responseCode = "200", description = "Synkkaus käynnistetty, palauttaa job-id:n", content = Array(new Content(schema = new Schema(implementation = classOf[SyncSuccessJobResponse])))),
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
            AuditLog.log(user, Map(VIRTA_DATASYNC_PARAM_NAME -> henkiloOid), AuditOperation.PaivitaVirtaTiedot, None)
            LOG.info(s"Haetaan Virta-tiedot henkilölle ${henkiloOid}")
            val jobId = virtaService.startRefreshForHenkilot(Set(henkiloOid))
            LOG.info(s"Palautetaan rajapintavastaus, $jobId")
            ResponseEntity.status(HttpStatus.OK).body(SyncSuccessJobResponse(jobId)))
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Oppijan Virta-päivitysjobin luonti epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(VirtaSyncFailureResponse(java.util.List.of(DATASYNC_JOBIN_LUONTI_EPAONNISTUI)))
  }

  @PostMapping(
    path = Array(VIRTA_DATASYNC_HAUT_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää hakujen hakijoiden tiedot Virrasta",
    description = "Tietojen päivitys SUPAan tapahtuu normaalisti eräajolla. Tämän endpointin avulla päivitys on kuitenkin " +
      "mahdollista tehdä manuaalisesti esim. virheiden selvittämistä tai nopeaa korjaamista varten.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[VirtaPaivitaTiedotHaullePayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Synkkaus käynnistetty, palauttaa job-id:n", content = Array(new Content(schema = new Schema(implementation = classOf[SyncSuccessJobResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[VirtaSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaVirtaTiedotHaulle(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = VIRTA_DATASYNC_HAUT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
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
              Right(objectMapper.readValue(bytes, classOf[VirtaPaivitaTiedotHaullePayload]).hakuOids)
            catch
              case e: Exception =>
                LOG.error("payloadin deserialisointi Virta-tietojen päivittämisessä haulle epäonnistui", e)
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VirtaSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
          .flatMap(hakuOids =>
            // validoidaan parametri
            val virheet = hakuOids.toScala.map(oids => oids.asScala.flatMap(oid => Validator.validateHakuOid(Some(oid), true)).toSet)
              .getOrElse(Set(Validator.VALIDATION_HAKUOID_TYHJA))
            if (virheet.isEmpty)
              Right(hakuOids.get.asScala.toSet)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VirtaSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
          .map(hakuOids => {
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("hakuOids" -> hakuOids.mkString(",")), AuditOperation.PaivitaVirtaTiedotHaunHakijoille, None)
            LOG.info(s"Haetaan Virta-tiedot hakujen ${hakuOids.mkString(",")} henkilöille")
            val jobId = virtaService.syncVirtaForHaut(hakuOids.toSeq)
            LOG.info(s"Palautetaan rajapintavastaus, $jobId")
            ResponseEntity.status(HttpStatus.OK).body(SyncSuccessJobResponse(jobId))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Virta-tietojen päivitys haulle epäonnistui")
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(VirtaSyncFailureResponse(List(DATASYNC_JOBIN_LUONTI_EPAONNISTUI).asJava))
  }

  @PostMapping(
    path = Array(VIRTA_DATASYNC_AKTIIVISET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää aktiivisten hakujen hakijoiden tiedot Virrasta",
    description = "Tietojen päivitys SUPAan tapahtuu normaalisti eräajolla. Tämän endpointin avulla päivitys on kuitenkin " +
      "mahdollista tehdä manuaalisesti esim. virheiden selvittämistä tai nopeaa korjaamista varten.",
    responses = Array(
      new ApiResponse(responseCode = "200", description =  "Synkkaus käynnistetty, palauttaa job-id:n",
        content = Array(new Content(schema = new Schema(implementation = classOf[SyncSuccessJobResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[VirtaSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaVirtaTiedotAktiivisetHaut(request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = VIRTA_DATASYNC_AKTIIVISET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .map(_ => {
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map.empty, AuditOperation.PaivitaVirtaTiedotAktiivisille, None)
            LOG.info(s"Päivitetään aktiivisten hakujen hakijoiden tiedot Virrasta")
            val jobId = virtaService.syncVirtaForAktiivisetHaut()
            ResponseEntity.status(HttpStatus.OK).body(SyncSuccessJobResponse(jobId))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Aktiivisten hakujen päivittäminen VIRTA-järjestelmästä epäonnistui")
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(VirtaSyncFailureResponse(List(DATASYNC_JOBIN_LUONTI_EPAONNISTUI).asJava))
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
    try
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
            val (changed, exceptions) = ytrService.fetchAndPersistStudents(personOids.toSet)
              .foldLeft((0, 0))((counts, result) => (counts._1 + { result.versio.map(_ => 1).getOrElse(0) }, counts._2 + { result.exception.map(_ => 1).getOrElse(0) }))
            LOG.info(s"Tallennettiin yhteensä ${changed} muuttunutta versiotietoa. Yhteensä ${exceptions} henkilön tietojen tallennuksessa oli ongelmia.")
            ResponseEntity.status(HttpStatus.OK).body(YtrSyncSuccessResponse(changed, exceptions))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Henkilöiden tietojen päivittäminen YTR-järjestelmästä epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(YtrSyncFailureResponse(List(YTR_DATASYNC_500_VIRHE).asJava))

  }

  @PostMapping(
    path = Array(YTR_DATASYNC_HAUT_PATH),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää hakujen hakijoiden tiedot YTR:stä",
    description = "SUPAan tapahtuu normaalisti eräajona. Tämän endpointin avulla päivitys on kuitenkin mahdollista tehdä " +
      "manuaalisesti esim. virheiden selvittämistä tai nopeaa korjaamista varten.",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[YTRPaivitaTiedotHaullePayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description =  "Synkkaus käynnistetty, palauttaa job-id:n",
        content = Array(new Content(schema = new Schema(implementation = classOf[SyncSuccessJobResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[YtrSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaYtrTiedotHaulle(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = YTR_DATASYNC_HAUT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
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
              Right(objectMapper.readValue(bytes, classOf[YTRPaivitaTiedotHaullePayload]).hakuOids)
            catch
              case e: Exception =>
                LOG.error("payloadin deserialisointi YTR-tietojen päivittämisessä haulle epäonnistui", e)
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
          .flatMap(hakuOids =>
            // validoidaan parametri
            val virheet = hakuOids.toScala.map(oids => oids.asScala.flatMap(oid => Validator.validateHakuOid(Some(oid), true)).toSet)
              .getOrElse(Set(Validator.VALIDATION_HAKUOID_TYHJA))
            if (virheet.isEmpty)
              Right(hakuOids.get.asScala.toSet)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(YtrSyncFailureResponse(new java.util.ArrayList(virheet.asJava)))))
          .map(hakuOids => {
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map("hakuOids" -> hakuOids.mkString(",")), AuditOperation.PaivitaYtrTiedotHaunHakijoille, None)
            LOG.info(s"Haetaan Ytr-tiedot hakujen ${hakuOids.mkString(",")} henkilöille")

            val jobId = ytrService.startRefreshYTRForHautJob(hakuOids.toSeq)
            ResponseEntity.status(HttpStatus.OK).body(SyncSuccessJobResponse(jobId))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Tietojen päivitys haulle YTR-järjestelmästä epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(YtrSyncFailureResponse(List(DATASYNC_JOBIN_LUONTI_EPAONNISTUI).asJava))
  }

  @PostMapping(
    path = Array(YTR_DATASYNC_AKTIIVISET_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Päivittää aktiivisten hakujen hakijoiden tiedot YTR:stä",
    description = "Tietojen päivitys SUPAan tapahtuu normaalisti eräajolla. Tämän endpointin avulla päivitys on kuitenkin " +
      "mahdollista tehdä manuaalisesti esim. virheiden selvittämistä tai nopeaa korjaamista varten.",
    responses = Array(
      new ApiResponse(responseCode = "200", description =  "Synkkaus käynnistetty, palauttaa job-id:n",
        content = Array(new Content(schema = new Schema(implementation = classOf[SyncSuccessJobResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[YtrSyncFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def paivitaYTRTiedotAktiivisetHaut(request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = YTR_DATASYNC_AKTIIVISET_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .map(_ => {
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map.empty, AuditOperation.PaivitaVirtaTiedotAktiivisille, None)
            LOG.info(s"Päivitetään aktiivisten hakujen hakijoiden tiedot YTR:stä")
            val jobId = ytrService.startRefreshYTRForAktiivisetHautJob()
            ResponseEntity.status(HttpStatus.OK).body(SyncSuccessJobResponse(jobId))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Aktiivisten hakujen tietojen YTR-järjestelmästä epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(YtrSyncFailureResponse(List(DATASYNC_JOBIN_LUONTI_EPAONNISTUI).asJava))
  }

  @GetMapping(
    path = Array(DATASYNC_JOBIT_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Hakee käynnissä olevien jobien tiedot",
    description = "Tietojen päivitys SUPAan tapahtuu normaalisti eräajoilla. Tämän endpointin avulla voidaan tarkastella eräajojen " +
      "tai muiden päivitysjobien tilannetta.",
    responses = Array(
      new ApiResponse(responseCode = "200", description =  "Palauttaa jobien tiedot",
        content = Array(new Content(schema = new Schema(implementation = classOf[SyncJobStatusResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[SyncJobFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def haeJobit(@RequestParam(name = DATASYNC_JOBIT_NIMI_PARAM_NAME, required = false) @Parameter(description = "jobin nimi", example = ESIMERKKI_JOB_NIMI) nimi: Optional[String],
               @RequestParam(name = DATASYNC_JOBIT_TUNNISTE_PARAM_NAME, required = false) @Parameter(description = "jobin tunniste (UUID)", example = ESIMERKKI_JOB_TUNNISTE) tunniste: Optional[String],
               request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = DATASYNC_JOBIT_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
        Right(None)
          .flatMap(_ =>
            // tarkastetaan oikeudet
            if (securityOperaatiot.onRekisterinpitaja())
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).build))
          .flatMap(_ =>
            // validoidaan parametrit
            val virheet = Set(
              Validator.validateJobinNimi(nimi.toScala, false),
              Validator.validateTunniste(tunniste.toScala, false)
            ).flatten
            if (virheet.isEmpty)
              Right((nimi.toScala, tunniste.toScala.map(t => UUID.fromString(t))))
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(SyncJobFailureResponse(new java.util.ArrayList(virheet.asJava)))))
          .map((nimi, tunniste) => {
            val user = AuditLog.getUser(request)
            AuditLog.log(user, Map(
              DATASYNC_JOBIT_NIMI_PARAM_NAME -> nimi.getOrElse(null),
              DATASYNC_JOBIT_TUNNISTE_PARAM_NAME -> tunniste.map(_.toString).getOrElse(null)
            ), AuditOperation.HaeJobData, None)
            LOG.info(s"Haetaan jobien tiedot")
            val jobit = kantaOperaatiot.getLastJobStatuses(nimi, tunniste, 100)
            ResponseEntity.status(HttpStatus.OK).body(SyncJobStatusResponse(jobit.map(j => SyncJob(j.tunniste, j.nimi, (j.progress*100).toInt, j.lastUpdated)).asJava))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Jobien tietojen haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(SyncJobFailureResponse(List(DATASYNC_JOBIEN_TIETOJEN_HAKU_EPAONNISTUI).asJava))
  }

  @PostMapping(
    path = Array(DATASYNC_UUDELLEENPARSEROI_PATH),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @Operation(
    summary = "Parseroi lähdedatan uudelleen vastaamaan nykyistä tietomallia",
    description = "Jos tietomalliin tulee muutoksia (esim. lähdedatasta halutaan lisää tietoa), täytyy olemassaoleva data parseroida uudestaan. Sen voi tehdä" +
      " halutussa laajuudessa tämän endpointin avulla",
    requestBody = new io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[ReparsePayload])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description =  "Synkkaus käynnistetty, palauttaa job-id:t",
        content = Array(new Content(schema = new Schema(implementation = classOf[ReparseSuccessResponse])))),
      new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[ReparseFailureResponse])))),
      new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
    ))
  def uudelleenParseroi(@RequestBody bytes: Array[Byte], request: HttpServletRequest): ResponseEntity[SyncResponse] = {
    try
      val securityOperaatiot = new SecurityOperaatiot
      LogContext(path = DATASYNC_UUDELLEENPARSEROI_PATH, identiteetti = securityOperaatiot.getIdentiteetti())(() =>
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
              Right(objectMapper.readValue(bytes, classOf[ReparsePayload]))
            catch
              case e: Exception =>
                LOG.error("payloadin deserialisointi uudelleenparserointia varten epäonnistui", e)
                Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(KoskiSyncFailureResponse(java.util.List.of(DATASYNC_JSON_VIRHE)))))
          .map(payload => {
            val user = AuditLog.getUser(request)
            val typeRef = new TypeReference[java.util.Map[String, String]] {}
            AuditLog.log(user, objectMapper.readValue(bytes, typeRef).asScala.toMap, AuditOperation.Uudelleenparseroi, None)
            LOG.info(s"Uudelleenparseroidaan lähdedata koski: ${payload.koski}, virta: ${payload.virta}, ytr: ${payload.ytr}, dry-run: ${payload.dryRun}")
            val jobIds = List(
              if(payload.koski) Some(reparseService.reparseKoski(payload.dryRun)) else None,
              if(payload.virta) Some(reparseService.reparseVirta(payload.dryRun)) else None,
              if(payload.ytr) Some(reparseService.reparseYTR(payload.dryRun)) else None,
              if(payload.perusopetuksenOppimaarat) Some(reparseService.reparsePerusopetuksenOppimaarat(payload.dryRun)) else None,
              if(payload.perusopetuksenOppiaineet) Some(reparseService.reparsePerusopetuksenOppiaineenOppimaarat(payload.dryRun)) else None,
            ).flatten
            ResponseEntity.status(HttpStatus.OK).body(ReparseSuccessResponse(jobIds.asJava))
          })
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[SyncResponse]])
    catch
      case e: Exception =>
        LOG.error("Aktiivisten hakujen tietojen YTR-järjestelmästä epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ReparseFailureResponse(List(DATASYNC_UUDELLEENPARSEROINTI_EPAONNISTUI).asJava))
  }
}

