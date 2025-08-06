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

case class Arvosana(arvosana: String, koodi: String)

case class Oppilaitos(nimi: Kielistetty, oid: String)

case class ErikoisAmmattiTutkinto(nimi: Kielistetty, koodi: Koodi, oppilaitos: Oppilaitos, tila: Koodi, vahvistusPaivamaara: Option[LocalDate], suoritusKieli: Koodi) extends Suoritus, Tyypitetty

case class AmmattiTutkinto(nimi: Kielistetty, koodi: Koodi, oppilaitos: Oppilaitos, tila: Koodi, vahvistusPaivamaara: Option[LocalDate], suoritustapa: Koodi, suoritusKieli: Koodi) extends Suoritus, Tyypitetty

case class AmmatillinenPerustutkinto(nimi: Kielistetty, koodi: Koodi, oppilaitos: Oppilaitos, tila: Koodi, vahvistusPaivamaara: Option[LocalDate], keskiarvo: Option[BigDecimal], suoritustapa: Koodi, suoritusKieli: Koodi, osat: Set[AmmatillisenTutkinnonOsa]) extends Suoritus, Tyypitetty

case class AmmatillisenTutkinnonOsaAlue(nimi: Kielistetty, koodi: Koodi, arvosana: Option[Koodi], laajuus: Option[Int], laajuusKoodi: Option[Koodi]) extends Tyypitetty

case class AmmatillisenTutkinnonOsa(
                                     nimi: Kielistetty,
                                     koodi: Koodi, yto: Boolean, arvosana: Option[Koodi], laajuus: Option[Int], laajuusKoodi: Option[Koodi], osaAlueet: Set[AmmatillisenTutkinnonOsaAlue]) extends Tyypitetty

case class Tuva(koodi: Koodi, vahvistusPaivamaara: Option[LocalDate]) extends Suoritus, Tyypitetty

case class Telma(nimi: Kielistetty, koodi: Koodi, oppilaitos: Oppilaitos, tila: Koodi, vahvistusPaivamaara: Option[LocalDate], suoritusKieli: Koodi) extends Suoritus, Tyypitetty

case class NuortenPerusopetuksenOppiaineenOppimaara(nimi: Kielistetty, koodi: Koodi, arvosana: Koodi, suoritusKieli: Koodi, vahvistusPaivamaara: Option[LocalDate]) extends Suoritus, Tyypitetty

case class PerusopetuksenOppimaara(organisaatioOid: String, tila: Koodi, suoritusKieli: Koodi, koulusivistyskieli: Set[Koodi], vahvistusPaivamaara: Option[LocalDate], aineet: Set[PerusopetuksenOppiaine]) extends Suoritus, Tyypitetty

//Kieli määritelty oppiaineille kuten A1, B1 jne.
case class PerusopetuksenOppiaine(nimi: Kielistetty, koodi: Koodi, arvosana: Koodi, kieli: Option[Koodi]) extends Tyypitetty

case class PerusopetuksenVuosiluokka(nimi: Kielistetty, koodi: Koodi, alkamisPaiva: Option[LocalDate], jaaLuokalle: Boolean) extends Suoritus, Tyypitetty

case class PerusopetuksenOpiskeluoikeus(
                                         oid: String,
                                         oppilaitosOid: String,
                                         @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
                                         suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus],
                                         lisatiedot: Option[KoskiLisatiedot],
                                         tila: Option[OpiskeluoikeusTila]) extends Opiskeluoikeus, Tyypitetty

case class AmmatillinenOpiskeluoikeus(
                                       oid: String,
                                       oppilaitos: Oppilaitos,
                                       @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
                                       suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus],
                                       tila: Option[OpiskeluoikeusTila]) extends Opiskeluoikeus, Tyypitetty

case class GeneerinenOpiskeluoikeus(
                                     oid: String,
                                     tyyppi: Koodi,
                                     oppilaitosOid: String,
                                     @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
                                     suoritukset: Set[fi.oph.suorituspalvelu.business.Suoritus],
                                     tila: Option[OpiskeluoikeusTila]) extends Opiskeluoikeus, Tyypitetty

case class YOOpiskeluoikeus(yoTutkinto: YOTutkinto) extends Opiskeluoikeus, Tyypitetty

case class YOTutkinto(suoritusKieli: Koodi) extends Suoritus

case class VersioEntiteetti(tunniste: UUID, oppijaNumero: String, alku: Instant, loppu: Option[Instant], tietolahde: Tietolahde)

case class VirtaOpiskeluoikeus(
                              tunniste: String,
                              @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
                              suoritukset: Set[Suoritus]
                              ) extends Opiskeluoikeus, Tyypitetty

case class VirtaTutkinto(
                          nimiFi: Option[String],
                          nimiSv: Option[String],
                          nimiEn: Option[String],
                          komoTunniste: String,
                          opintoPisteet: BigDecimal,
                          suoritusPvm: LocalDate,
                          myontaja: String,
                          kieli: String,
                          koulutusKoodi: String,
                          opiskeluoikeusAvain: String
                        ) extends Suoritus, Tyypitetty

case class Opintosuoritus(
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
