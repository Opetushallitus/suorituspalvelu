package fi.oph.suorituspalvelu.mankeli

import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.integration.{OnrIntegration, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, Ensikertalaisuus, HakemuspalveluClient, KoutaHaku, VTSClient}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

object EnsikertalaisuusConstants {
  val seliteSuoritettuKkTutkinto = "Henkilöllä on VIRTA-järjestelmässä ennen leikkuripäivämäärää suoritettu korkeakoulututkinto"
  val seliteOpiskeluoikeusAlkanut = "Henkilöllä on VIRTA-järjestelmässä ennen leikkuripäivämäärää alkanut korkeakoulutuksen opiskeluoikeus"
  val seliteKkVastaanotto = "Henkilöllä on ennen leikkuripäivämäärää vastaanotettu korkeakoulun opiskelupaikka"
  val seliteSuoritettuKkTutkintoHakemukselta = "Hakemuksella on suoritettu korkeakoulututkinto"
}

@Component
class EnsikertalaisuusService {

  private val LOG = LoggerFactory.getLogger(classOf[EnsikertalaisuusService])

  @Autowired val kantaOperaatiot: KantaOperaatiot = null
  @Autowired val onrIntegration: OnrIntegration = null
  @Autowired val tarjontaIntegration: TarjontaIntegration = null
  @Autowired val vtsClient: VTSClient = null
  @Autowired val hakemuspalveluClient: HakemuspalveluClient = null

  private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  private val helsinkiZone = ZoneId.of("Europe/Helsinki")

  private val OPISKELUOIKEUS_ALKU_RAJA = LocalDate.of(2014, 8, 1)

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(6))

  /**
   * Päättelee ensikertalaisuuden ja palauttaa tuloksen AvainArvoContainerina.
   * Tätä kutsutaan ValintaDataServicestä valintadatan koostamisessa.
   *
   * Opiskeluoikeudet ja hakemus annetaan parametreina, jotta niitä ei haeta uudelleen.
   * VTS-data haetaan tässä metodissa.
   */
  def haeEnsikertalaisuusAvainArvo(
    henkiloOid: String,
    haku: KoutaHaku,
    allOidsForPerson: Set[String],
    opiskeluoikeudet: Seq[Opiskeluoikeus],
    hakemus: Option[AtaruValintalaskentaHakemus]
  ): AvainArvoContainer = {
    val leikkuriLocalDate = resolveLeikkuriPvm(haku).atZone(helsinkiZone).toLocalDate

    val kkOpiskeluoikeudet = opiskeluoikeudet.collect {
      case o: KKOpiskeluoikeus => o
        // synteettisten opiskeluoikeuksien alla ei ole tutkintoja, eikä niiden tutkintoonjohtavuudesta voida sanoa
        // mitään joten niitä ei oteta tarkasteluun mukaan
    }

    val vtsEnsikertalaisuudet = Await.result(vtsClient.fetchEnsikertalaisuudet(allOidsForPerson.toSeq), 30.seconds)
    val henkilonVtsData = vtsEnsikertalaisuudet.filter(e => allOidsForPerson.contains(e.personOid))

    val hakemusHasKkTutkintoVuosi = hakemus.exists(h => h.korkeakoulututkintoVuosi.isDefined)

    paatteleEnsikertalaisuusAvainArvo(henkiloOid, leikkuriLocalDate, kkOpiskeluoikeudet, henkilonVtsData, hakemusHasKkTutkintoVuosi)
  }

  /**
   * Puhdas päättelymetodi joka palauttaa AvainArvoContainerin.
   * Kaikki tarvittava data annetaan parametreina.
   */
  def paatteleEnsikertalaisuusAvainArvo(
    henkiloOid: String,
    leikkuriLocalDate: LocalDate,
    kkOpiskeluoikeudet: Seq[KKOpiskeluoikeus],
    vtsEnsikertalaisuudet: Seq[Ensikertalaisuus],
    hakemusHasKkTutkintoVuosi: Boolean
  ): AvainArvoContainer = {

    // Tarkistus 1: SuoritettuKkTutkinto
    val suoritettuKkTutkinto = tarkistaSuoritettuKkTutkinto(kkOpiskeluoikeudet, leikkuriLocalDate)
    if (suoritettuKkTutkinto.isDefined) {
      return AvainArvoContainer(AvainArvoConstants.ensikertalainenKey, "false", Seq(EnsikertalaisuusConstants.seliteSuoritettuKkTutkinto))
    }

    // Tarkistus 2: OpiskeluoikeusAlkanut
    val opiskeluoikeusAlkanut = tarkistaOpiskeluoikeusAlkanut(kkOpiskeluoikeudet, leikkuriLocalDate)
    if (opiskeluoikeusAlkanut.isDefined) {
      return AvainArvoContainer(AvainArvoConstants.ensikertalainenKey, "false", Seq(EnsikertalaisuusConstants.seliteOpiskeluoikeusAlkanut))
    }

    // Tarkistus 3: KkVastaanotto (VTS)
    val kkVastaanotto = tarkistaKkVastaanotto(vtsEnsikertalaisuudet, leikkuriLocalDate)
    if (kkVastaanotto.isDefined) {
      return AvainArvoContainer(AvainArvoConstants.ensikertalainenKey, "false", Seq(EnsikertalaisuusConstants.seliteKkVastaanotto))
    }

    // Tarkistus 4: SuoritettuKkTutkintoHakemukselta
    if (hakemusHasKkTutkintoVuosi) {
      return AvainArvoContainer(AvainArvoConstants.ensikertalainenKey, "false", Seq(EnsikertalaisuusConstants.seliteSuoritettuKkTutkintoHakemukselta))
    }

    AvainArvoContainer(AvainArvoConstants.ensikertalainenKey, "true")
  }

  def resolveLeikkuriPvm(haku: KoutaHaku): Instant = {
    val hakuajat = haku.hakuajat.flatMap(_.paattyy)
    if (hakuajat.isEmpty) {
      throw new RuntimeException(s"Haulla ${haku.oid} ei ole päättymispäivämäärää hakuajoissa!")
    }
    hakuajat
      .map(paattyy => LocalDateTime.parse(paattyy, formatter).atZone(helsinkiZone).toInstant)
      .max
  }

  /**
   * Tarkistus 1: Onko henkilöllä suoritettu KK-tutkinto ennen leikkuripäivämäärää?
   * Palauttaa aikaisimman valmistumispäivän jos löytyy.
   */
  private def tarkistaSuoritettuKkTutkinto(kkOpiskeluoikeudet: Seq[KKOpiskeluoikeus], leikkuriLocalDate: LocalDate): Option[LocalDate] = {
    val suoritusPvmt = kkOpiskeluoikeudet.flatMap {
      _.suoritukset.collect {
        case t: KKTutkinto if t.supaTila == SuoritusTila.VALMIS => t.suoritusPvm
      }.flatten
    }

    val aikaisinPvm = suoritusPvmt.sorted.headOption
    aikaisinPvm.filter(pvm => pvm.isBefore(leikkuriLocalDate))
  }

  /**
   * Tarkistus 2: Onko henkilöllä tutkintoon johtava KK-opiskeluoikeus joka on alkanut 1.8.2014 jälkeen
   * ja ennen leikkuripäivämäärää? Se missä tilassa opiskeluoikeus on tällä hetkellä ei ole merkityksellistä.
   */
  private def tarkistaOpiskeluoikeusAlkanut(kkOpiskeluoikeudet: Seq[Opiskeluoikeus], leikkuriLocalDate: LocalDate): Option[LocalDate] = {
    val alkuPvmt = kkOpiskeluoikeudet.collect {
      case o: KKOpiskeluoikeus if o.isTutkintoonJohtava =>
        o.alkuPvm
    }.filter(pvm => !pvm.isBefore(OPISKELUOIKEUS_ALKU_RAJA))

    val aikaisinPvm = alkuPvmt.sorted.headOption
    aikaisinPvm.filter(pvm => pvm.isBefore(leikkuriLocalDate))
  }

  /**
   * Tarkistus 3: Onko henkilöllä ensikertalaisuus päättynyt VTS:n mukaan ennen leikkuripäivämäärää?
   */
  private def tarkistaKkVastaanotto(henkilonVtsData: Seq[Ensikertalaisuus], leikkuriLocalDate: LocalDate): Option[LocalDate] = {
    val paattymisPvmt = henkilonVtsData.flatMap(_.paattyi).map(Instant.parse(_).atZone(ZoneId.of("Europe/Helsinki")).toLocalDate)
    val aikaisin = paattymisPvmt.sorted.headOption
    aikaisin.filter(pvm => pvm.isBefore(leikkuriLocalDate))
  }
}
