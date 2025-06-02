package fi.oph.suorituspalvelu.business

import fi.oph.suorituspalvelu.parsing.koski.{KoskiLisatiedot, OpiskeluoikeusTila}

import java.util.UUID
import java.time.{Instant, LocalDate}

enum Tietolahde:
  case KOSKI, YTR, VIRTA, VIRKAILIJA

sealed trait TallennettavaEntiteetti

sealed trait Suoritus extends TallennettavaEntiteetti

sealed trait Opiskeluoikeus extends TallennettavaEntiteetti

case class Koodi(arvo: String, koodisto: String, versio: Option[Int])

case class Arvosana(arvosana: String, koodi: String)

case class AmmatillinenTutkinto(nimi: String, tyyppi: Koodi, tila: Koodi, vahvistusPaivamaara: Option[LocalDate], keskiarvo: Option[BigDecimal], suoritustapa: Koodi, osat: Set[AmmatillisenTutkinnonOsa]) extends Suoritus

case class AmmatillisenTutkinnonOsaAlue(nimi: String, koodi: Koodi, arvosana: Option[Koodi], laajuus: Option[Int], laajuusKoodi: Option[Koodi])

case class AmmatillisenTutkinnonOsa(nimi: String, koodi: Koodi, yto: Boolean, arvosana: Option[Koodi], laajuus: Option[Int], laajuusKoodi: Option[Koodi], osaAlueet: Set[AmmatillisenTutkinnonOsaAlue])

case class Tuva(koodi: String, vahvistusPaivamaara: Option[LocalDate]) extends Suoritus

case class Telma(koodi: String) extends Suoritus

case class NuortenPerusopetuksenOppiaineenOppimaara(nimi: String, koodi: String, arvosana: String) extends Suoritus

case class PerusopetuksenOppimaara(organisaatioOid: String, tila: Koodi, koulusivistyskieli: Set[Koodi], vahvistusPaivamaara: Option[LocalDate], aineet: Set[PerusopetuksenOppiaine]) extends Suoritus

case class PerusopetuksenOppiaine(nimi: String, koodi: String, arvosana: String)

case class PerusopetuksenVuosiluokka(nimi: String, koodi: String, alkamisPaiva: Option[LocalDate]) extends Suoritus

case class PerusopetuksenOpiskeluoikeus(oid: String, oppilaitosOid: String, suoritukset: Seq[fi.oph.suorituspalvelu.business.Suoritus], lisatiedot: Option[KoskiLisatiedot], tila: Option[OpiskeluoikeusTila]) extends Opiskeluoikeus

case class AmmatillinenOpiskeluoikeus(oid: String, oppilaitosOid: String, suoritukset: Seq[fi.oph.suorituspalvelu.business.Suoritus], tila: Option[OpiskeluoikeusTila]) extends Opiskeluoikeus

case class GeneerinenOpiskeluoikeus(oid: String, tyyppi: String, oppilaitosOid: String, suoritukset: Seq[fi.oph.suorituspalvelu.business.Suoritus], tila: Option[OpiskeluoikeusTila]) extends Opiskeluoikeus

case class YOOpiskeluoikeus(yoTutkinto: YOTutkinto) extends Opiskeluoikeus

case class YOTutkinto() extends Suoritus

case class VersioEntiteetti(tunniste: UUID, oppijaNumero: String, alku: Instant, loppu: Option[Instant], tietolahde: Tietolahde)

case class VirtaTutkinto(
                          nimiFi: Option[String],
                          nimiSv: Option[String],
                          nimiEn: Option[String],
                          komoTunniste: String,
                          opintoPisteet: BigDecimal,
                          suoritusPvm: LocalDate,
                          myontaja: String,
                          kieli: String,
                          koulutusKoodi: String
                        ) extends Suoritus

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
                           opinnaytetyo: Boolean
                         ) extends Suoritus

