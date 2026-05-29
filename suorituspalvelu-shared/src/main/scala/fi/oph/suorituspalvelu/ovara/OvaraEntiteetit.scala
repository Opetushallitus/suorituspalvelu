package fi.oph.suorituspalvelu.ovara

import fi.oph.suorituspalvelu.business.{Arvosana, Koodi, KKOpiskeluoikeusTila, Korotus, Laajuus, Oppilaitos, OpiskeluoikeusJakso, Lahtokoulu, PerusopetuksenYksilollistaminen, SuoritusTila}
import fi.oph.suorituspalvelu.parsing.koski.{Kielistetty, KoskiLisatiedot, KoskiOpiskeluoikeusTila}

import java.time.{Instant, LocalDate}
import java.util.UUID

// ---- KK ----

case class OvaraKKOpiskeluoikeus(
  entiteetinTyyppi: String = "KKOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  tunniste: UUID,
  virtaTunniste: String,
  tyyppiKoodi: String,
  koulutusKoodi: Option[String],
  alkuPvm: LocalDate,
  loppuPvm: LocalDate,
  virtaTila: Koodi,
  supaTila: KKOpiskeluoikeusTila,
  myontaja: String,
  isTutkintoonJohtava: Boolean,
  kieli: Option[String],
  suoritukset: Seq[OvaraKKSuoritus]
)

case class OvaraKKSynteettinenOpiskeluoikeus(
  entiteetinTyyppi: String = "KKSynteettinenOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  tunniste: UUID,
  myontaja: String,
  containsKKTutkinto: Boolean,
  suoritukset: Seq[OvaraKKSuoritus]
)

sealed trait OvaraKKSuoritus

case class OvaraKKTutkinto(
  entiteetinTyyppi: String = "KKTutkinto",
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
  suoritukset: Seq[OvaraKKSuoritus],
  avain: Option[String]
) extends OvaraKKSuoritus

case class OvaraKKOpintosuoritus(
  entiteetinTyyppi: String = "KKOpintosuoritus",
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
  kieli: Option[String],
  koulutusala: Option[Int],
  koulutusalaKoodisto: Option[String],
  opinnaytetyo: Boolean,
  opiskeluoikeusAvain: Option[String],
  suoritukset: Seq[OvaraKKSuoritus],
  avain: String
) extends OvaraKKSuoritus

case class OvaraKKSynteettinenSuoritus(
  entiteetinTyyppi: String = "KKSynteettinenSuoritus",
  tunniste: UUID,
  nimi: Option[Kielistetty],
  supaTila: SuoritusTila,
  komoTunniste: String,
  aloitusPvm: Option[LocalDate],
  suoritusPvm: Option[LocalDate],
  myontaja: String,
  koulutusKoodi: Option[String],
  opiskeluoikeusAvain: Option[String],
  suoritukset: Seq[OvaraKKSuoritus]
) extends OvaraKKSuoritus

// ---- YO ----

case class OvaraYOOpiskeluoikeus(
  entiteetinTyyppi: String = "YOOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  tunniste: UUID,
  yoTutkinto: Option[OvaraYOTutkinto]
)

case class OvaraYOTutkinto(
  entiteetinTyyppi: String = "YOTutkinto",
  tunniste: UUID,
  suoritusKieli: Koodi,
  supaTila: SuoritusTila,
  valmistumisPaiva: Option[LocalDate],
  aineet: Set[OvaraKoe]
)

case class OvaraKoe(
  entiteetinTyyppi: String = "Koe",
  tunniste: UUID,
  koodi: Koodi,
  tutkintoKerta: LocalDate,
  arvosana: Koodi,
  pisteet: Option[Int]
)

// ---- Geneerinen (Lukio, DIA, EB, IB, Tuva, VapaaSivistystyo) ----

case class OvaraGeneerinenOpiskeluoikeus(
  entiteetinTyyppi: String = "GeneerinenOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  tunniste: UUID,
  oid: String,
  tyyppi: Koodi,
  oppilaitosOid: String,
  suoritukset: Seq[OvaraGeneerinenSuoritus],
  tila: Option[KoskiOpiskeluoikeusTila],
  jaksot: List[OpiskeluoikeusJakso]
)

sealed trait OvaraGeneerinenSuoritus

case class OvaraLukionOppimaara(
  entiteetinTyyppi: String = "LukionOppimaara",
  tunniste: UUID,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  suoritusKieli: Option[Koodi],
  koulusivistyskieli: Set[Koodi]
) extends OvaraGeneerinenSuoritus

case class OvaraDIATutkinto(
  entiteetinTyyppi: String = "DIATutkinto",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  suorituskieli: Koodi,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  osasuoritukset: Seq[OvaraDIAOppiaine]
) extends OvaraGeneerinenSuoritus

case class OvaraDIAOppiaine(
  entiteetinTyyppi: String = "DIAOppiaine",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  laajuus: Option[OvaraDIALaajuus],
  osaAlue: Option[Koodi],
  kieli: Option[Koodi],
  vastaavuustodistuksenTiedot: Option[OvaraDIAVastaavuustodistuksenTiedot],
  kirjallinenKoe: Option[OvaraDIAOppiaineenKoesuoritus],
  suullinenKoe: Option[OvaraDIAOppiaineenKoesuoritus]
)

case class OvaraDIAOppiaineenKoesuoritus(
  entiteetinTyyppi: String = "DIAOppiaineenKoesuoritus",
  nimi: Kielistetty,
  koodi: Koodi,
  arvosana: OvaraDIAArvosana,
  laajuus: Option[OvaraDIALaajuus]
)

case class OvaraDIAArvosana(
  entiteetinTyyppi: String = "DIAArvosana",
  arvosana: Koodi,
  hyvaksytty: Boolean
)

case class OvaraDIALaajuus(
  entiteetinTyyppi: String = "DIALaajuus",
  arvo: BigDecimal,
  yksikko: Koodi
)

case class OvaraDIAVastaavuustodistuksenTiedot(
  entiteetinTyyppi: String = "DIAVastaavuustodistuksenTiedot",
  keskiarvo: BigDecimal,
  lukioOpintojenLaajuus: OvaraDIALaajuus
)

case class OvaraEBTutkinto(
  entiteetinTyyppi: String = "EBTutkinto",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  osasuoritukset: Seq[OvaraEBOppiaine]
) extends OvaraGeneerinenSuoritus

case class OvaraEBOppiaine(
  entiteetinTyyppi: String = "EBOppiaine",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  laajuus: Option[OvaraEBLaajuus],
  suorituskieli: Option[Koodi],
  osasuoritukset: Seq[OvaraEBOppiaineenOsasuoritus]
)

case class OvaraEBOppiaineenOsasuoritus(
  entiteetinTyyppi: String = "EBOppiaineenOsasuoritus",
  nimi: Kielistetty,
  koodi: Koodi,
  arvosana: OvaraEBArvosana,
  laajuus: Option[OvaraEBLaajuus]
)

case class OvaraEBArvosana(
  entiteetinTyyppi: String = "EBArvosana",
  arvosana: Koodi,
  hyvaksytty: Boolean
)

case class OvaraEBLaajuus(
  entiteetinTyyppi: String = "EBLaajuus",
  arvo: BigDecimal,
  yksikko: Koodi
)

case class OvaraIBTutkinto(
  entiteetinTyyppi: String = "IBTutkinto",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  suorituskieli: Option[Koodi],
  osasuoritukset: Seq[OvaraIBOppiaineSuoritus]
) extends OvaraGeneerinenSuoritus

case class OvaraIBOppiaineSuoritus(
  entiteetinTyyppi: String = "IBOppiaineSuoritus",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  ryhma: Option[OvaraIBOppiaineRyhma],
  predictedArvosana: Option[OvaraIBArvosana],
  laajuus: Option[OvaraIBLaajuus],
  suorituskieli: Option[Koodi]
)

case class OvaraIBOppiaineRyhma(
  entiteetinTyyppi: String = "IBOppiaineRyhma",
  nimi: Kielistetty,
  koodi: Koodi
)

case class OvaraIBArvosana(
  entiteetinTyyppi: String = "IBArvosana",
  arvosana: Koodi,
  hyvaksytty: Boolean
)

case class OvaraIBLaajuus(
  entiteetinTyyppi: String = "IBLaajuus",
  arvo: BigDecimal,
  yksikko: Koodi
)

case class OvaraTuva(
  entiteetinTyyppi: String = "Tuva",
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
  lahtokoulut: List[Lahtokoulu]
) extends OvaraGeneerinenSuoritus

case class OvaraVapaaSivistystyo(
  entiteetinTyyppi: String = "VapaaSivistystyo",
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
  lahtokoulut: List[Lahtokoulu]
) extends OvaraGeneerinenSuoritus

// ---- Ammatillinen ----

case class OvaraAmmatillinenOpiskeluoikeus(
  entiteetinTyyppi: String = "AmmatillinenOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  tunniste: UUID,
  oid: String,
  oppilaitos: Oppilaitos,
  suoritukset: Seq[OvaraAmmatillinenSuoritus],
  tila: Option[KoskiOpiskeluoikeusTila],
  jaksot: List[OpiskeluoikeusJakso]
)

sealed trait OvaraAmmatillinenSuoritus

case class OvaraAmmatillinenPerustutkinto(
  entiteetinTyyppi: String = "AmmatillinenPerustutkinto",
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
  osat: Seq[OvaraAmmatillisenTutkinnonOsa]
) extends OvaraAmmatillinenSuoritus

case class OvaraAmmatillinenTutkintoOsittainen(
  entiteetinTyyppi: String = "AmmatillinenTutkintoOsittainen",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  korotettuKeskiarvo: Option[BigDecimal],
  korotettuOpiskeluoikeusOid: Option[String],
  suoritustapa: Koodi,
  suoritusKieli: Koodi,
  osat: Seq[OvaraAmmatillisenTutkinnonOsa]
) extends OvaraAmmatillinenSuoritus

case class OvaraAmmatillisenTutkinnonOsa(
  entiteetinTyyppi: String = "AmmatillisenTutkinnonOsa",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  yto: Boolean,
  arviointiPaiva: Option[LocalDate],
  arvosana: Option[Arvosana],
  laajuus: Option[Laajuus],
  osaAlueet: Seq[OvaraAmmatillisenTutkinnonOsaAlue],
  korotettu: Option[Korotus]
)

case class OvaraAmmatillisenTutkinnonOsaAlue(
  entiteetinTyyppi: String = "AmmatillisenTutkinnonOsaAlue",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  arvosana: Option[Koodi],
  laajuus: Option[Laajuus],
  korotettu: Option[Korotus]
)

case class OvaraAmmattiTutkinto(
  entiteetinTyyppi: String = "AmmattiTutkinto",
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
) extends OvaraAmmatillinenSuoritus

case class OvaraErikoisAmmattiTutkinto(
  entiteetinTyyppi: String = "ErikoisAmmattiTutkinto",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  suoritusKieli: Koodi
) extends OvaraAmmatillinenSuoritus

case class OvaraTelma(
  entiteetinTyyppi: String = "Telma",
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
  lahtokoulut: List[Lahtokoulu]
) extends OvaraAmmatillinenSuoritus

// ---- Perusopetus ----

case class OvaraPerusopetuksenOpiskeluoikeus(
  entiteetinTyyppi: String = "PerusopetuksenOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  tunniste: UUID,
  oid: Option[String],
  oppilaitosOid: String,
  suoritukset: Seq[OvaraPerusopetuksenSuoritus],
  lisatiedot: Option[KoskiLisatiedot],
  tila: SuoritusTila,
  jaksot: List[OpiskeluoikeusJakso]
)

sealed trait OvaraPerusopetuksenSuoritus

case class OvaraPerusopetuksenOppimaara(
  entiteetinTyyppi: String = "PerusopetuksenOppimaara",
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
  aineet: Seq[OvaraPerusopetuksenOppiaine],
  lahtokoulut: List[Lahtokoulu],
  syotetty: Boolean,
  vuosiluokkiinSitoutumatonOpetus: Boolean,
  luokkaAste: Option[Int]
) extends OvaraPerusopetuksenSuoritus

case class OvaraPerusopetuksenOppimaaranOppiaineidenSuoritus(
  entiteetinTyyppi: String = "PerusopetuksenOppimaaranOppiaineidenSuoritus",
  tunniste: UUID,
  versioTunniste: Option[UUID],
  oppilaitos: Oppilaitos,
  koskiTila: Koodi,
  supaTila: SuoritusTila,
  suoritusKieli: Koodi,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  aineet: Set[OvaraPerusopetuksenOppiaine],
  syotetty: Boolean
) extends OvaraPerusopetuksenSuoritus

case class OvaraPerusopetuksenOppiaine(
  entiteetinTyyppi: String = "PerusopetuksenOppiaine",
  tunniste: UUID,
  nimi: Kielistetty,
  koodi: Koodi,
  arvosana: Koodi,
  kieli: Option[Koodi],
  pakollinen: Boolean,
  yksilollistetty: Option[Boolean],
  rajattu: Option[Boolean]
)

case class OvaraPoistettuOpiskeluoikeus(
  entiteetinTyyppi: String = "PoistettuOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  oid: String
)

case class OvaraVersioMetadata(
  lahdejarjestelma: String,
  lahdeTunniste: String,
  lahdeVersio: Option[Int],
  parserVersio: Option[Int],
  luontiHetki: Option[Instant],
  paivitysHetki: Option[Instant],
  parserointiHetki: Option[Instant]
)

//Sisältää henkilön kaikki opiskeluoikeudet kaikista lähdejärjestelmistä.
case class OvaraVersioJaOpiskeluoikeudet(
  henkiloOid: String,
  kkOpiskeluoikeudet: Seq[OvaraKKOpiskeluoikeus],
  kkSynteettisetOpiskeluoikeudet: Seq[OvaraKKSynteettinenOpiskeluoikeus],
  yoOpiskeluoikeudet: Seq[OvaraYOOpiskeluoikeus],
  geneerisetOpiskeluoikeudet: Seq[OvaraGeneerinenOpiskeluoikeus],
  ammatillisetOpiskeluoikeudet: Seq[OvaraAmmatillinenOpiskeluoikeus],
  perusopetuksenOpiskeluoikeudet: Seq[OvaraPerusopetuksenOpiskeluoikeus],
  poistetutOpiskeluoikeudet: Seq[OvaraPoistettuOpiskeluoikeus]
)
