package fi.oph.suorituspalvelu.business

import com.fasterxml.jackson.annotation.{JsonTypeInfo, JsonTypeName}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiLisatiedot, OpiskeluoikeusTila}

import java.util.UUID
import java.time.{Instant, LocalDate}

enum Tietolahde:
  case KOSKI, YTR, VIRTA, VIRKAILIJA

case class Container(
                      @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
                      opiskeluoikeudet: Set[Opiskeluoikeus])

sealed trait TallennettavaEntiteetti

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
sealed trait Tyypitetty

sealed trait Suoritus extends TallennettavaEntiteetti

sealed trait Opiskeluoikeus extends TallennettavaEntiteetti

case class Koodi(arvo: String, koodisto: String, versio: Option[Int])

case class Laajuus(arvo: BigDecimal, yksikko: Koodi, nimi: Option[Kielistetty], lyhytNimi: Option[Kielistetty])

case class Arvosana(koodi: Koodi, nimi: Kielistetty)

case class Oppilaitos(nimi: Kielistetty, oid: String)

case class ErikoisAmmattiTutkinto(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, oppilaitos: Oppilaitos, tila: Koodi, aloitusPaivamaara: Option[LocalDate], vahvistusPaivamaara: Option[LocalDate], suoritusKieli: Koodi) extends Suoritus, Tyypitetty

case class AmmattiTutkinto(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, oppilaitos: Oppilaitos, tila: Koodi, aloitusPaivamaara: Option[LocalDate], vahvistusPaivamaara: Option[LocalDate], suoritustapa: Koodi, suoritusKieli: Koodi) extends Suoritus, Tyypitetty

case class AmmatillinenPerustutkinto(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, oppilaitos: Oppilaitos, tila: Koodi, aloitusPaivamaara: Option[LocalDate], vahvistusPaivamaara: Option[LocalDate], keskiarvo: Option[BigDecimal], suoritustapa: Koodi, suoritusKieli: Koodi, osat: Set[AmmatillisenTutkinnonOsa]) extends Suoritus, Tyypitetty

case class AmmatillisenTutkinnonOsaAlue(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, arvosana: Option[Koodi], laajuus: Option[Laajuus]) extends Tyypitetty

case class AmmatillisenTutkinnonOsa(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, yto: Boolean, arviointiPaiva: Option[LocalDate], arvosana: Option[Arvosana], laajuus: Option[Laajuus], osaAlueet: Set[AmmatillisenTutkinnonOsaAlue]) extends Tyypitetty

case class Telma(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, oppilaitos: Oppilaitos, tila: Koodi, aloitusPaivamaara: Option[LocalDate], vahvistusPaivamaara: Option[LocalDate], suoritusKieli: Koodi) extends Suoritus, Tyypitetty

case class Tuva(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, oppilaitos: Oppilaitos, tila: Koodi, aloitusPaivamaara: Option[LocalDate], vahvistusPaivamaara: Option[LocalDate], laajuus: Option[Laajuus]) extends Suoritus, Tyypitetty

case class VapaaSivistystyo(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, oppilaitos: Oppilaitos, tila: Koodi, aloitusPaivamaara: Option[LocalDate], vahvistusPaivamaara: Option[LocalDate], laajuus: Option[Laajuus], suoritusKieli: Koodi) extends Suoritus, Tyypitetty

case class NuortenPerusopetuksenOppiaineenOppimaara(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, arvosana: Koodi, suoritusKieli: Koodi, aloitusPaivamaara: Option[LocalDate], vahvistusPaivamaara: Option[LocalDate]) extends Suoritus, Tyypitetty

case class PerusopetuksenOppimaara(tunniste: UUID, organisaatioOid: String, tila: Koodi, suoritusKieli: Koodi, koulusivistyskieli: Set[Koodi], aloitusPaivamaara: Option[LocalDate], vahvistusPaivamaara: Option[LocalDate], aineet: Set[PerusopetuksenOppiaine]) extends Suoritus, Tyypitetty

//Kieli määritelty oppiaineille kuten A1, B1 jne.
case class PerusopetuksenOppiaine(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, arvosana: Koodi, kieli: Option[Koodi]) extends Tyypitetty

case class PerusopetuksenVuosiluokka(tunniste: UUID, nimi: Kielistetty, koodi: Koodi, alkamisPaiva: Option[LocalDate], jaaLuokalle: Boolean) extends Suoritus, Tyypitetty

case class PerusopetuksenOpiskeluoikeus(
                                         tunniste: UUID,
                                         oid: String,
                                         oppilaitosOid: String,
                                         @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
                                         suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus],
                                         lisatiedot: Option[KoskiLisatiedot],
                                         tila: Option[OpiskeluoikeusTila]) extends Opiskeluoikeus, Tyypitetty

case class AmmatillinenOpiskeluoikeus(
                                       tunniste: UUID,
                                       oid: String,
                                       oppilaitos: Oppilaitos,
                                       @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
                                       suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus],
                                       tila: Option[OpiskeluoikeusTila]) extends Opiskeluoikeus, Tyypitetty

case class GeneerinenOpiskeluoikeus(
                                     tunniste: UUID,
                                     oid: String,
                                     tyyppi: Koodi,
                                     oppilaitosOid: String,
                                     @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
                                     suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus],
                                     tila: Option[OpiskeluoikeusTila]) extends Opiskeluoikeus, Tyypitetty

case class YOOpiskeluoikeus(tunniste: UUID, yoTutkinto: YOTutkinto) extends Opiskeluoikeus, Tyypitetty

case class YOTutkinto(tunniste: UUID, suoritusKieli: Koodi) extends Suoritus

case class VersioEntiteetti(tunniste: UUID, oppijaNumero: String, alku: Instant, loppu: Option[Instant], tietolahde: Tietolahde)

enum KKOpiskeluoikeusTila:
  case VOIMASSA
  case EI_VOIMASSA
  case PAATTYNYT

case class VirtaOpiskeluoikeus(
                                tunniste: UUID,
                                virtaTunniste: String,
                                koulutusKoodi: String,
                                alkuPvm: LocalDate,
                                loppuPvm: LocalDate,
                                virtaTila: Koodi,
                                supaTila: KKOpiskeluoikeusTila,
                                myontaja: String,
                                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
                                suoritukset: Set[Suoritus]
                              ) extends Opiskeluoikeus, Tyypitetty

case class VirtaTutkinto(
                          tunniste: UUID,
                          nimiFi: Option[String],
                          nimiSv: Option[String],
                          nimiEn: Option[String],
                          komoTunniste: String,
                          opintoPisteet: BigDecimal,
                          aloitusPvm: LocalDate,
                          suoritusPvm: LocalDate,
                          myontaja: String,
                          kieli: String,
                          koulutusKoodi: String,
                          opiskeluoikeusAvain: String
                        ) extends Suoritus, Tyypitetty

case class Opintosuoritus(
                           tunniste: UUID,
                           nimiFi: Option[String],
                           nimiSv: Option[String],
                           nimiEn: Option[String],
                           komoTunniste: String,
                           opintoPisteet: BigDecimal,
                           opintoviikot: Option[BigDecimal],
                           suoritusPvm: LocalDate,
                           hyvaksilukuPvm: Option[LocalDate],
                           myontaja: String,
                           jarjestavaRooli: Option[String],
                           jarjestavaKoodi: Option[String],
                           jarjestavaOsuus: Option[BigDecimal],
                           arvosana: Option[String],
                           arvosanaAsteikko: Option[String],
                           kieli: String,
                           koulutusala: Int,
                           koulutusalaKoodisto: String,
                           opinnaytetyo: Boolean,
                           opiskeluoikeusAvain: String //Onhan tämä aina saatavilla?
                         ) extends Suoritus, Tyypitetty
