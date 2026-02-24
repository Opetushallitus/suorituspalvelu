package fi.oph.suorituspalvelu.business

import com.fasterxml.jackson.annotation.{JsonCreator, JsonTypeInfo, JsonValue}
import fi.oph.suorituspalvelu.mankeli.HarkinnanvaraisuudenSyy
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiLisatiedot, KoskiOpiskeluoikeusTila}

import java.time.{Instant, LocalDate}
import java.util.UUID

case class Lahdejarjestelma(nimi: String) {
  @JsonValue
  def toJson: String = nimi
}

object Lahdejarjestelma {
  final val KOSKI = Lahdejarjestelma("KOSKI")
  final val VIRTA = Lahdejarjestelma("VIRTA")
  final val YTR = Lahdejarjestelma("YTR")
  final val SYOTETTY_PERUSOPETUS = Lahdejarjestelma("SYOTETTY_PERUSOPETUS")
  final val SYOTETYT_OPPIAINEET = Lahdejarjestelma("SYOTETYT_OPPIAINEET")

  @JsonCreator
  def fromString(value: String): Lahdejarjestelma = Lahdejarjestelma(value)

  def oppiaineenOppimaara(nimi: String): Lahdejarjestelma = Lahdejarjestelma(s"OPPIAINE_${nimi.toUpperCase()}")
  def kieliOppiaineenOppimaara(kieli: String, laajuus: String): Lahdejarjestelma =
    Lahdejarjestelma(s"OPPIAINE_${kieli.toUpperCase()}_${laajuus.toUpperCase}")

  def defaultLahdeTunniste(lahdejarjestelma: Lahdejarjestelma): String = lahdejarjestelma match {
    case VIRTA => "VIRTA"
    case YTR => "YTR"
    case SYOTETTY_PERUSOPETUS => "SYOTETTY"
    case SYOTETYT_OPPIAINEET => "SYOTETTY"
  }
}

enum SuoritusTila:
  case VALMIS
  case KESKEN
  case KESKEYTYNYT

case class Container(
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  opiskeluoikeudet: Set[Opiskeluoikeus]
)

sealed trait TallennettavaEntiteetti

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
sealed trait Tyypitetty

sealed trait Suoritus extends TallennettavaEntiteetti

sealed trait Opiskeluoikeus extends TallennettavaEntiteetti

case class PoistettuOpiskeluoikeus(oid: String) extends Opiskeluoikeus, Tyypitetty

case class Koodi(arvo: String, koodisto: String, versio: Option[Int])

case class Laajuus(arvo: BigDecimal, yksikko: Koodi, nimi: Option[Kielistetty], lyhytNimi: Option[Kielistetty])

case class Arvosana(koodi: Koodi, nimi: Kielistetty)

case class Oppilaitos(nimi: Kielistetty, oid: String)

case class EBArvosana(
  arvosana: Koodi,
  hyvaksytty: Boolean
)

case class EBOppiaineenOsasuoritus(
  nimi: Kielistetty,
  koodi: Koodi, // Final, Oral, Written.
  arvosana: EBArvosana, // Jos ei ole arvosanaa, ei luoda koko osasuoritusta
  laajuus: Option[Laajuus]
) //mutta tietomalli kuitenkin sallii puuttumisen.

case class EBLaajuus(arvo: BigDecimal, yksikko: Koodi)

case class EBOppiaine(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  laajuus: Option[EBLaajuus],
  suorituskieli: Koodi,
  osasuoritukset: Set[EBOppiaineenOsasuoritus]
)

case class EBTutkinto(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  osasuoritukset: Set[EBOppiaine]
) extends Suoritus, Tyypitetty

case class OpiskeluoikeusJakso(alku: LocalDate, tila: SuoritusTila)

case class ErikoisAmmattiTutkinto(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  suoritusKieli: Koodi
) extends Suoritus, Tyypitetty

case class AmmattiTutkinto(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  suoritustapa: Koodi,
  suoritusKieli: Koodi
) extends Suoritus, Tyypitetty

case class AmmatillinenPerustutkinto(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  keskiarvo: Option[BigDecimal],
  suoritustapa: Koodi,
  suoritusKieli: Koodi,
  osat: Set[AmmatillisenTutkinnonOsa]
) extends Suoritus, Tyypitetty

case class AmmatillisenTutkinnonOsaAlue(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  arvosana: Option[Koodi],
  laajuus: Option[Laajuus]
) extends Tyypitetty

case class AmmatillisenTutkinnonOsa(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  yto: Boolean,
  arviointiPaiva: Option[LocalDate],
  arvosana: Option[Arvosana],
  laajuus: Option[Laajuus],
  osaAlueet: Set[AmmatillisenTutkinnonOsaAlue]
) extends Tyypitetty

case class TelmaOsasuoritus(
  nimi: Kielistetty,
  koodi: Koodi,
  arviointi: TelmaArviointi,
  laajuus: Laajuus
)

case class TelmaArviointi(
  arvosana: Koodi,
  hyvaksytty: Boolean
)

case class Telma(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: LocalDate,
  vahvistusPaivamaara: Option[LocalDate],
  suoritusVuosi: Int,
  suoritusKieli: Koodi,
  hyvaksyttyLaajuus: Option[Laajuus],
  lahtokoulu: Lahtokoulu
) extends Suoritus, Tyypitetty

case class Tuva(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: LocalDate,
  vahvistusPaivamaara: Option[LocalDate],
  suoritusVuosi: Int,
  hyvaksyttyLaajuus: Option[Laajuus],
  lahtokoulu: Lahtokoulu
) extends Suoritus, Tyypitetty

//Opistovuosi
case class VapaaSivistystyo(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: LocalDate,
  vahvistusPaivamaara: Option[LocalDate],
  suoritusVuosi: Int,
  hyvaksyttyLaajuus: Option[Laajuus],
  suoritusKieli: Koodi,
  lahtokoulu: Lahtokoulu
) extends Suoritus, Tyypitetty

//Tähän entiteettiin mallinnetaan sekä NuortenPerusopetuksenOppiaineenOppimaarat että AikuistenPerusopetuksenOppiaineenOppimäärät.
case class PerusopetuksenOppimaaranOppiaineidenSuoritus(
  tunniste: UUID,
  versioTunniste: Option[UUID],
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  suoritusKieli: Koodi,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  aineet: Set[PerusopetuksenOppiaine],
  syotetty: Boolean // Käsin tallennetulle tiedolle true, muutoin false.
) extends Suoritus, Tyypitetty

enum LahtokouluTyyppi(val defaultLuokka: Option[String]):
  case VUOSILUOKKA_7 extends LahtokouluTyyppi(None)
  case VUOSILUOKKA_8 extends LahtokouluTyyppi(None)
  case VUOSILUOKKA_9 extends LahtokouluTyyppi(None)
  case AIKUISTEN_PERUSOPETUS extends LahtokouluTyyppi(Some("Aikuisten perusopetus"))
  case PERUSOPETUKSEEN_VALMISTAVA_OPETUS extends LahtokouluTyyppi(Some("Perusopetukseen valmistava opetus"))
  case TUVA extends LahtokouluTyyppi(Some("TUVA"))
  case TELMA extends LahtokouluTyyppi(Some("TELMA"))
  case VAPAA_SIVISTYSTYO extends LahtokouluTyyppi(Some("Vapaa sivistystyö"))

object LahtokouluTyyppi {

  val SUPAN_KAYTTOLIITTYMASSA_NAYTETTAVAT = Set(AIKUISTEN_PERUSOPETUS, VUOSILUOKKA_9, TELMA, TUVA, VAPAA_SIVISTYSTYO)

  val KOSKESTA_TUOTAVAT = Set(VUOSILUOKKA_9, TELMA, TUVA, VAPAA_SIVISTYSTYO)
}

case class Lahtokoulu(
  suorituksenAlku: LocalDate,
  suorituksenLoppu: Option[LocalDate],
  oppilaitosOid: String,
  valmistumisvuosi: Option[Int],
  luokka: String,
  tila: Option[SuoritusTila],
  arvosanaPuuttuu: Option[Boolean],
  suoritusTyyppi: LahtokouluTyyppi
)

object PerusopetuksenYksilollistaminen {
  def fromIntValue(value: Int): PerusopetuksenYksilollistaminen = value match {
    case 1 => EI_YKSILOLLISTETTY
    case 2 => OSITTAIN_YKSILOLLISTETTY
    case 6 => PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY
    case 3 => TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY
    case 8 => OSITTAIN_RAJATTU
    case 9 => PAAOSIN_TAI_KOKONAAN_RAJATTU
  }

  def toIntValue(yks: PerusopetuksenYksilollistaminen): Int = yks match {
    case EI_YKSILOLLISTETTY => 1
    case OSITTAIN_YKSILOLLISTETTY => 2
    case PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY => 6
    case TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY => 3
    case OSITTAIN_RAJATTU => 8
    case PAAOSIN_TAI_KOKONAAN_RAJATTU => 9
  }
}

enum PerusopetuksenYksilollistaminen(intValue: Int) {
  case EI_YKSILOLLISTETTY extends PerusopetuksenYksilollistaminen(1)
  case OSITTAIN_YKSILOLLISTETTY extends PerusopetuksenYksilollistaminen(2)
  case PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY extends PerusopetuksenYksilollistaminen(6)
  case TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY extends PerusopetuksenYksilollistaminen(3)
  case OSITTAIN_RAJATTU extends PerusopetuksenYksilollistaminen(8)
  case PAAOSIN_TAI_KOKONAAN_RAJATTU extends PerusopetuksenYksilollistaminen(9)
}

case class PerusopetuksenOppimaara(
  tunniste: UUID,
  versioTunniste: Option[UUID],
  oppilaitos: Oppilaitos,
  luokka: Option[String],
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  suoritusKieli: Koodi,
  koulusivistyskieli: Set[Koodi],
  yksilollistaminen: Option[PerusopetuksenYksilollistaminen],
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  aineet: Set[PerusopetuksenOppiaine],
  lahtokoulut: Set[Lahtokoulu],
  syotetty: Boolean, // Käsin tallennetulle tiedolle true, muutoin false.
  vuosiluokkiinSitoutumatonOpetus: Boolean
) extends Suoritus, Tyypitetty

case class PerusopetukseenValmistavaOpetus(
  lahtokoulu: Lahtokoulu,
) extends Suoritus, Tyypitetty

case class LukionOppimaara(
  tunniste: UUID,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  suoritusKieli: Option[Koodi],
  koulusivistyskieli: Set[Koodi]
) extends Suoritus, Tyypitetty

//Kieli määritelty oppiaineille kuten A1, B1 jne.
case class PerusopetuksenOppiaine(
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  arvosana: Koodi,
  kieli: Option[Koodi],
  pakollinen: Boolean,
  yksilollistetty: Option[Boolean],
  rajattu: Option[Boolean]
) extends Tyypitetty

case class PerusopetuksenOpiskeluoikeus(
  tunniste: UUID,
  oid: Option[String],
  oppilaitosOid: String,
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus],
  lisatiedot: Option[KoskiLisatiedot],
  tila: SuoritusTila,
  jaksot: List[OpiskeluoikeusJakso]
) extends Opiskeluoikeus, Tyypitetty

case class AmmatillinenOpiskeluoikeus(
  tunniste: UUID,
  oid: String,
  oppilaitos: Oppilaitos,
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus],
  tila: Option[KoskiOpiskeluoikeusTila],
  jaksot: List[OpiskeluoikeusJakso]
) extends Opiskeluoikeus, Tyypitetty

case class GeneerinenOpiskeluoikeus(
  tunniste: UUID,
  oid: String,
  tyyppi: Koodi,
  oppilaitosOid: String,
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus],
  tila: Option[KoskiOpiskeluoikeusTila],
  jaksot: List[OpiskeluoikeusJakso]
) extends Opiskeluoikeus, Tyypitetty

case class YOOpiskeluoikeus(tunniste: UUID, yoTutkinto: Option[YOTutkinto]) extends Opiskeluoikeus, Tyypitetty

case class YOTutkinto(
  tunniste: UUID,
  suoritusKieli: Koodi,
  supaTila: SuoritusTila,
  valmistumisPaiva: Option[LocalDate],
  aineet: Set[Koe]
) extends Suoritus

case class Koe(tunniste: UUID, koodi: Koodi, tutkintoKerta: LocalDate, arvosana: Koodi, pisteet: Option[Int])

case class VersioEntiteetti(
  tunniste: UUID,
  henkiloOid: String,
  alku: Instant,
  loppu: Option[Instant],
  lahdeJarjestelma: Lahdejarjestelma,
  lahdeTunniste: String,
  lahdeVersio: Option[Int],
  parserVersio: Option[Int]
)

enum KKOpiskeluoikeusTila:
  case VOIMASSA
  case PAATTYNYT

sealed trait KKOpiskeluoikeusBase(synteettinen: Boolean) extends Opiskeluoikeus, Tyypitetty

case class KKOpiskeluoikeus(
  tunniste: UUID,
  virtaTunniste: String,
  tyyppiKoodi: String,
  koulutusKoodi: Option[String],
  alkuPvm: LocalDate,
  loppuPvm: LocalDate,
  virtaTila: Koodi,
  supaTila: KKOpiskeluoikeusTila,
  myontaja: String,
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus]
) extends KKOpiskeluoikeusBase(synteettinen = false)

case class KKSynteettinenOpiskeluoikeus(
  tunniste: UUID,
  myontaja: String,
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus]
) extends KKOpiskeluoikeusBase(synteettinen = true)

sealed trait KKSuoritusBase(synteettinen: Boolean = false) extends Suoritus, Tyypitetty

case class KKSynteettinenSuoritus(
  tunniste: UUID,
  nimi: Option[Kielistetty],
  supaTila: SuoritusTila,
  komoTunniste: String,
  aloitusPvm: Option[LocalDate],
  suoritusPvm: Option[LocalDate],
  myontaja: String,
  koulutusKoodi: Option[String],
  opiskeluoikeusAvain: Option[String],
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  suoritukset: Seq[fi.oph.suorituspalvelu.business.Suoritus] = Seq.empty
) extends KKSuoritusBase(synteettinen = true)

case class KKTutkinto(
  tunniste: UUID,
  nimi: Option[Kielistetty],
  supaTila: SuoritusTila,
  komoTunniste: String,
  opintoPisteet: BigDecimal,
  aloitusPvm: Option[LocalDate],
  suoritusPvm: Option[LocalDate],
  myontaja: String,
  kieli: Option[String],
  koulutusKoodi: Option[String],
  opiskeluoikeusAvain: Option[String],
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  suoritukset: Seq[fi.oph.suorituspalvelu.business.Suoritus] = Seq.empty,
  avain: Option[String] // Puuttuu, jos synteettinen
) extends KKSuoritusBase(synteettinen = false)

case class KKOpintosuoritus(
  tunniste: UUID,
  nimi: Option[Kielistetty],
  supaTila: SuoritusTila,
  komoTunniste: String,
  opintoPisteet: BigDecimal,
  opintoviikot: Option[BigDecimal],
  suoritusPvm: Option[LocalDate],
  hyvaksilukuPvm: Option[LocalDate],
  myontaja: String,
  jarjestavaRooli: Option[String],
  jarjestavaKoodi: Option[String],
  jarjestavaOsuus: Option[BigDecimal],
  arvosana: Option[String],
  arvosanaAsteikko: Option[String],
  kieli: String,
  koulutusala: Option[Int],
  koulutusalaKoodisto: Option[String],
  opinnaytetyo: Boolean,
  opiskeluoikeusAvain: Option[String],
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  suoritukset: Seq[fi.oph.suorituspalvelu.business.Suoritus] = Seq.empty,
  avain: String
) extends KKSuoritusBase(synteettinen = false)

case class AvainArvoYliajo(
  avain: String,
  arvo: Option[String],
  henkiloOid: String,
  hakuOid: String,
  virkailijaOid: String,
  selite: String
)

case class AvainArvoYliajoMuutos(
  arvo: Option[String],
  luotu: Instant,
  virkailijaOid: String,
  selite: String
)

case class Job(tunniste: UUID, nimi: String, progress: BigDecimal, lastUpdated: Instant)

case class HarkinnanvaraisuusYliajo(
  hakemusOid: String,
  hakukohdeOid: String,
  harkinnanvaraisuudenSyy: Option[HarkinnanvaraisuudenSyy],
  virkailijaOid: String,
  selite: String
)
