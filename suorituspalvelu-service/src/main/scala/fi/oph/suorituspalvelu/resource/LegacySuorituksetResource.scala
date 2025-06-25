package fi.oph.suorituspalvelu.resource

import fi.oph.suorituspalvelu.business.{AmmatillinenOpiskeluoikeus, AmmatillinenTutkinto, KantaOperaatiot, Tietolahde, YOOpiskeluoikeus, YOTutkinto}
import fi.oph.suorituspalvelu.resource.ApiConstants.{DATASYNC_RESPONSE_400_DESCRIPTION, DATASYNC_RESPONSE_403_DESCRIPTION, HEALTHCHECK_PATH, LEGACY_SUORITUKSET_JOKO_OID_TAI_PVM_PAKOLLINEN, LEGACY_SUORITUKSET_HENKILO_PARAM_NAME, LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME, LEGACY_SUORITUKSET_PATH, VIRTA_DATASYNC_PARAM_NAME, LEGACY_SUORITUKSET_HAKU_EPAONNISTUI}
import fi.oph.suorituspalvelu.security.{AuditLog, AuditOperation, SecurityOperaatiot}
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

@Schema(name = "LegacySuorituksetFailureResponse")
case class LegacySuorituksetFailureResponse(
                                     @(Schema @field)(example = "")
                                     @BeanProperty virheet: java.util.Set[String])

/*
Hakemuspalvelu on käyttänyt suren suoritukset-rajapintaa automaattisen hakukelpoisuuden määrittämiseen. Tämä on tapahtunut
kahdella mekanismilla:

1. Hakemalla yksittäiselle henkilölle erikseen ylioppilastutkinto, ammatillinen perustutkinto, ammattitutkinto ja
erikoisammattitutkinto-tyyppiset suoritukset (vanhassa rajapinnassa voidaan määritellä haettavien suoritusten komo-koodi)
ja katsomalla onko jokin näistä valmis, jolloin henkilö on kk-hakukelpoinen.

2. Hakemalla ne henkilöt joilla on muuttuneita suorituksia sen jälkeen kun automaattisen hakukelpoisuuden päivitysjobi
on ajettu, joille sitten päivitetään hakukelpoisuus yksitellen.

Vanha suoritukset, rajapinta on palauttanut seuraavan tyyppisen jsonin:

[
  {
    "henkiloOid": "1.2.246.562.24.40483869857",
    "source": "koski",
    "vahvistettu": true,
    "komo": "1.2.246.562.13.62959769647",
    "myontaja": "1.2.246.562.10.42923230215",
    "tila": "VALMIS",
    "valmistuminen": "16.04.2022",
    "yksilollistaminen": "Ei",
    "suoritusKieli": "FI",
    "id": "01706b39-09a5-461f-bbf9-431196b9b502",
    "lahdeArvot": {
      "last modified": "1747477417224",
      "yksilollistetty_ma_ai": "false"
    }
  }
]

Tapauksessa 1. ainoa hyödynnettävä kenttä on tila, ja vaihtoehdossa 2. henkiloOid.

Rajapintaa korvaamaan on alla toteutettu rajapinta jossa palautetaan joko a) ammatilliset ja yo-suoritukset yksittäiselle
henkilölle (jos henkilo-parametri on määritelty), tai b) henkilöt joilla on muuttuneita suorituksia määritellyn aikaleiman
jälkeen (jos muokattuJalkeen-parametri on määritelty).

Keskeinen ero käytössä on että tapauksessa a) rajapintaa ei tarvitse erikseen kutsua neljälle erityyppiselle suoritukselle,
vaan se palauttaa automaattisesti kaikki sopivat suoritukset (hakemuspalvelun toimintaa voidaan muuttaa sopivassa kohtaa
niin että kutsu tehdään vain kerran eikä neljästi).
*/

case class LegacyAmmatillinenTaiYOSuoritus(
  @(Schema @field)(example = "1.2.246.562.24.40483869857") @BeanProperty henkiloOid: String,
  @(Schema @field)(example = "ammatillinentutkinto") @BeanProperty tyyppiKoodi: String,
  @(Schema @field)(example = "351301") @BeanProperty koulutusModuuliKoodi: Optional[String],
  @(Schema @field)(example = "VALMIS") @BeanProperty tila: String,
)

case class LegacyMuuttunutSuoritus(
  @(Schema @field)(example = "1.2.246.562.24.40483869857") @BeanProperty henkiloOid: String,
)

@RequestMapping(path = Array(LEGACY_SUORITUKSET_PATH))
@RestController
class LegacySuorituksetResource {

  val LOG = LoggerFactory.getLogger(classOf[LegacySuorituksetResource]);

  @Autowired var database: JdbcBackend.JdbcDatabaseDef = null

  def getSuorituksetForOppija(oppijaNumero: String): Seq[LegacyAmmatillinenTaiYOSuoritus] =
    val kantaOperaatiot = KantaOperaatiot(database)
    val opiskeluoikeudet = kantaOperaatiot.haeSuoritukset(oppijaNumero)
    val ammatillisetTutkinnot = opiskeluoikeudet
      .filter((v, o) => v.tietolahde == Tietolahde.KOSKI)
      .map((v, o) => o)
      .flatten
      // tähän sisältyvät ammatilliset perustutkinnot, ammattitutkinnot ja erikoisammattitutkinnot
      .filter(o => o.isInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.asInstanceOf[AmmatillinenOpiskeluoikeus])
      .map(o => o.suoritukset)
      .flatten
      .filter(s => s.isInstanceOf[AmmatillinenTutkinto])
      .map(s => s.asInstanceOf[AmmatillinenTutkinto])
      .map(t => LegacyAmmatillinenTaiYOSuoritus(oppijaNumero, "ammatillinentutkinto", Optional.of(t.tyyppi.arvo), if (t.vahvistusPaivamaara.isDefined) "VALMIS" else "KESKEN"))
      .toSeq

    val yoTutkinnot = opiskeluoikeudet
      .filter((v, o) => v.tietolahde == Tietolahde.YTR)
      .map((v, o) => o)
      .flatten
      .filter(o => o.isInstanceOf[YOOpiskeluoikeus])
      .map(o => o.asInstanceOf[YOOpiskeluoikeus])
      .map(o => o.yoTutkinto)
      .map(t => LegacyAmmatillinenTaiYOSuoritus(oppijaNumero, "yotutkinto", Optional.empty(), "VALMIS"))
      .toSeq

    Seq(ammatillisetTutkinnot, yoTutkinnot).flatten

  @GetMapping(path = Array(""),
    produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @Operation(
    summary = "Tuottaa hakemuspalvelulle automaattisen hakukelpoisuuden määrittelyyn tarvittavat tiedot",
    description = "Hakemuspalvelua varten tehty korvike suoritusrekisterin /rest/v1/suoritukset-rajapinnalle jota hakemuspalvelu käyttää" +
      "automaattisen hakukelpoisuuden määrittämiseen kahdella tavalla:\n" +
      "- " + LEGACY_SUORITUKSET_HENKILO_PARAM_NAME + "-parametri määrilteltynä halutaan tietään onko yksittäisellä hakijalla yo- tai ammatillinen tutkinto. Hakemuspalvelu etsii tuloksista vähintään yhtä suoritusta jonka tila on VALMIS\n" +
      "- " + LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME + "-parametri määriteltynä halutaan tietää hakijat joilla on muuttuneita suorituksia määritellyn aikaleiman jälkeen. Hakemuspalvelu käynnistää palautetuille henkilöille hakukelpoisuuden tarkistuksen\n",
    parameters = Array(new Parameter(name = VIRTA_DATASYNC_PARAM_NAME, in = ParameterIn.PATH))
  )
  @ApiResponses(value = Array(
    new ApiResponse(responseCode = "200", description = "Palauttaa suoritukset", content = Array(new Content(
      mediaType = "application/json",
      examples = Array(
        new ExampleObject(name="palautetaan kun henkilo-parametri määritelty", summary="Ammatilliset ja YO-tutkinnot", value =
          """
            [
              {
                "henkiloOd": "1.2.246.562.24.40483869857",
                "tyyppikoodi": "ammatillinentutkinto",
                "351301": "koulutusModuuliKoodi",
                "tila": "VAlMIS"
              }
            ]
          """),
        new ExampleObject(name="palautetaan kun muokattuJalkeen-parametri määritelty", summary="Muuttuneet henkilöt", value =
          """
            [
              {
                "henkiloOd": "1.2.246.562.24.40483869857"
              }
            ]
          """))
    ))),
    new ApiResponse(responseCode = "400", description = DATASYNC_RESPONSE_400_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[LegacySuorituksetFailureResponse])))),
    new ApiResponse(responseCode = "403", description = DATASYNC_RESPONSE_403_DESCRIPTION, content = Array(new Content(schema = new Schema(implementation = classOf[Void]))))
  ))
  def legacySuoritukset(
                         @RequestParam(name = LEGACY_SUORITUKSET_HENKILO_PARAM_NAME, required = false) @Parameter(description = "henkilön oid", example = "1.2.246.562.24.40483869857") oppijaNumero: Optional[String],
                         @RequestParam(name = LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME, required = false) @Parameter(description = "ISO aikaleima jonka jälkeen muokatut haetaan", example = "2015-01-01T12:34:56.000+02:00") muokattuJalkeen: Optional[String],
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
              Left(ResponseEntity.status(HttpStatus.FORBIDDEN).body(LegacySuorituksetFailureResponse(java.util.Set.of("ei oikeuksia")))))
          .flatMap(_ =>
            if(oppijaNumero.isPresent ^ muokattuJalkeen.isPresent)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LegacySuorituksetFailureResponse(java.util.Set.of(LEGACY_SUORITUKSET_JOKO_OID_TAI_PVM_PAKOLLINEN)))))
          .flatMap(_ =>
            val virheet = Set(Validator.validateOppijanumero(oppijaNumero.toScala, false), Validator.validateMuokattujalkeen(muokattuJalkeen.toScala, false)).flatten
            if(virheet.isEmpty)
              Right(None)
            else
              Left(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LegacySuorituksetFailureResponse(virheet.asJava))))
          .map(_ =>
            val user = AuditLog.getUser(request)
            val kantaOperaatiot = KantaOperaatiot(database)

            if(oppijaNumero.isPresent)
              LOG.info(s"Haetaan ammatilliset ja yo-suoritukset henkilölle ${oppijaNumero}")
              AuditLog.log(user, Map(LEGACY_SUORITUKSET_HENKILO_PARAM_NAME -> oppijaNumero.orElse(null)), AuditOperation.HaeYoTaiAmmatillinenTutkintoTiedot, None)
              ResponseEntity.status(HttpStatus.OK).body(getSuorituksetForOppija(oppijaNumero.get).asJava)
            else
              LOG.info(s"Haetaan hakijat joilla muuttuneita suorituksia ${muokattuJalkeen} jälkeen")
              AuditLog.log(user, Map(LEGACY_SUORITUKSET_MUOKATTU_JALKEEN_PARAM_NAME -> muokattuJalkeen.orElse(null)), AuditOperation.HaeKoskiTaiYTRMuuttuneet, None)

              val muuttuneet = kantaOperaatiot.haeUusimmatMuuttuneetVersiot(Instant.parse(muokattuJalkeen.get)).map(m => LegacyMuuttunutSuoritus(m.oppijaNumero))
              ResponseEntity.status(HttpStatus.OK).body(muuttuneet)
          )
          .fold(e => e, r => r).asInstanceOf[ResponseEntity[LegacySuorituksetFailureResponse]])
    catch
      case e: Exception =>
        LOG.error("YO tai ammatillisten tutkintojen haku epäonnistui", e)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LegacySuorituksetFailureResponse(java.util.Set.of(LEGACY_SUORITUKSET_HAKU_EPAONNISTUI)))

}