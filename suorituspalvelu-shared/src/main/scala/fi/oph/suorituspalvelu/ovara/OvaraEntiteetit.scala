package fi.oph.suorituspalvelu.ovara

import java.time.{Instant, LocalDate}
import java.util.UUID

// ---- Yhteiset Ovara-tyypit (eivät riipu business- tai parsing.koski-paketeista) ----

enum OvaraSuoritusTila:
  case VALMIS
  case KESKEN
  case KESKEYTYNYT

enum OvaraKorotus:
  case KOROTETTU
  case KOROTUKSENYRITYS

enum OvaraKKOpiskeluoikeusTila:
  case VOIMASSA
  case PAATTYNYT

enum OvaraPerusopetuksenYksilollistaminen:
  case EI_YKSILOLLISTETTY
  case OSITTAIN_YKSILOLLISTETTY
  case PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY
  case TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY
  case OSITTAIN_RAJATTU
  case PAAOSIN_TAI_KOKONAAN_RAJATTU

enum OvaraLahtokouluTyyppi:
  case VUOSILUOKKA_7
  case VUOSILUOKKA_8
  case VUOSILUOKKA_9
  case AIKUISTEN_PERUSOPETUS
  case PERUSOPETUKSEEN_VALMISTAVA_OPETUS
  case TUVA
  case TELMA
  case VAPAA_SIVISTYSTYO

case class OvaraKielistetty(
  fi: Option[String],
  sv: Option[String],
  en: Option[String]
)

case class OvaraKoodi(arvo: String, koodisto: String, versio: Option[Int])

case class OvaraKoskiKoodi(
  koodiarvo: String,
  koodistoUri: String,
  koodistoVersio: Option[Int],
  nimi: OvaraKielistetty,
  lyhytNimi: Option[OvaraKielistetty]
)

case class OvaraLaajuus(
  arvo: BigDecimal,
  yksikko: OvaraKoodi,
  nimi: Option[OvaraKielistetty],
  lyhytNimi: Option[OvaraKielistetty]
)

case class OvaraArvosana(koodi: OvaraKoodi, nimi: OvaraKielistetty)

case class OvaraOppilaitos(nimi: OvaraKielistetty, oid: String)

case class OvaraOpiskeluoikeusJakso(alku: LocalDate, tila: OvaraSuoritusTila)

case class OvaraKoskiOpiskeluoikeusJakso(alku: LocalDate, tila: OvaraKoskiKoodi)

case class OvaraKoskiOpiskeluoikeusTila(opiskeluoikeusjaksot: List[OvaraKoskiOpiskeluoikeusJakso])

case class OvaraKoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain: Option[Boolean])

case class OvaraKoskiKotiopetusjakso(alku: String, loppu: Option[String])

case class OvaraKoskiLisatiedot(
  erityisenTuenPäätökset: Option[List[OvaraKoskiErityisenTuenPaatos]],
  vuosiluokkiinSitoutumatonOpetus: Option[Boolean],
  kotiopetusjaksot: Option[List[OvaraKoskiKotiopetusjakso]]
)

case class OvaraLahtokoulu(
  suorituksenAlku: LocalDate,
  suorituksenLoppu: Option[LocalDate],
  oppilaitosOid: String,
  valmistumisvuosi: Option[Int],
  luokka: String,
  tila: OvaraSuoritusTila,
  arvosanaPuuttuu: Option[Boolean],
  suoritusTyyppi: OvaraLahtokouluTyyppi
)

// ---- KK ----

case class OvaraKKOpiskeluoikeus(
  entiteetinTyyppi: String = "KKOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  nimi: Option[Kielistetty],
  tunniste: UUID,
  virtaTunniste: String,
  tyyppiKoodi: String,
  koulutusKoodi: Option[String],
  alkuPvm: LocalDate,
  loppuPvm: LocalDate,
  virtaTila: OvaraKoodi,
  supaTila: OvaraKKOpiskeluoikeusTila,
  myontaja: String,
  isTutkintoonJohtava: Boolean,
  kieli: Option[String],
  suoritukset: Seq[OvaraKKSuoritus],
  rahoitusLahde: Option[String],
  luokittelu: Option[String],
  liittyvaOpiskeluoikeusAvain: Option[String]
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
  nimi: Option[OvaraKielistetty],
  supaTila: OvaraSuoritusTila,
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
  nimi: Option[OvaraKielistetty],
  supaTila: OvaraSuoritusTila,
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
  nimi: Option[OvaraKielistetty],
  supaTila: OvaraSuoritusTila,
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
  suoritusKieli: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  valmistumisPaiva: Option[LocalDate],
  aineet: Set[OvaraKoe]
)

case class OvaraKoe(
  entiteetinTyyppi: String = "Koe",
  tunniste: UUID,
  koodi: OvaraKoodi,
  tutkintoKerta: LocalDate,
  arvosana: OvaraKoodi,
  pisteet: Option[Int]
)

// ---- Geneerinen (Lukio, DIA, EB, IB, Tuva, VapaaSivistystyo) ----

case class OvaraGeneerinenOpiskeluoikeus(
  entiteetinTyyppi: String = "GeneerinenOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  tunniste: UUID,
  oid: String,
  tyyppi: OvaraKoodi,
  oppilaitosOid: String,
  suoritukset: Seq[OvaraGeneerinenSuoritus],
  tila: Option[OvaraKoskiOpiskeluoikeusTila],
  jaksot: List[OvaraOpiskeluoikeusJakso]
)

sealed trait OvaraGeneerinenSuoritus

case class OvaraLukionOppimaara(
  entiteetinTyyppi: String = "LukionOppimaara",
  tunniste: UUID,
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  suoritusKieli: Option[OvaraKoodi],
  koulusivistyskieli: Set[OvaraKoodi]
) extends OvaraGeneerinenSuoritus

case class OvaraDIATutkinto(
  entiteetinTyyppi: String = "DIATutkinto",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  oppilaitos: OvaraOppilaitos,
  suorituskieli: OvaraKoodi,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  osasuoritukset: Seq[OvaraDIAOppiaine]
) extends OvaraGeneerinenSuoritus

case class OvaraDIAOppiaine(
  entiteetinTyyppi: String = "DIAOppiaine",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  laajuus: Option[OvaraDIALaajuus],
  osaAlue: Option[OvaraKoodi],
  kieli: Option[OvaraKoodi],
  vastaavuustodistuksenTiedot: Option[OvaraDIAVastaavuustodistuksenTiedot],
  kirjallinenKoe: Option[OvaraDIAOppiaineenKoesuoritus],
  suullinenKoe: Option[OvaraDIAOppiaineenKoesuoritus]
)

case class OvaraDIAOppiaineenKoesuoritus(
  entiteetinTyyppi: String = "DIAOppiaineenKoesuoritus",
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  arvosana: OvaraDIAArvosana,
  laajuus: Option[OvaraDIALaajuus]
)

case class OvaraDIAArvosana(
  entiteetinTyyppi: String = "DIAArvosana",
  arvosana: OvaraKoodi,
  hyvaksytty: Boolean
)

case class OvaraDIALaajuus(
  entiteetinTyyppi: String = "DIALaajuus",
  arvo: BigDecimal,
  yksikko: OvaraKoodi
)

case class OvaraDIAVastaavuustodistuksenTiedot(
  entiteetinTyyppi: String = "DIAVastaavuustodistuksenTiedot",
  keskiarvo: BigDecimal,
  lukioOpintojenLaajuus: OvaraDIALaajuus
)

case class OvaraEBTutkinto(
  entiteetinTyyppi: String = "EBTutkinto",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  osasuoritukset: Seq[OvaraEBOppiaine]
) extends OvaraGeneerinenSuoritus

case class OvaraEBOppiaine(
  entiteetinTyyppi: String = "EBOppiaine",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  laajuus: Option[OvaraEBLaajuus],
  suorituskieli: Option[OvaraKoodi],
  osasuoritukset: Seq[OvaraEBOppiaineenOsasuoritus]
)

case class OvaraEBOppiaineenOsasuoritus(
  entiteetinTyyppi: String = "EBOppiaineenOsasuoritus",
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  arvosana: OvaraEBArvosana,
  laajuus: Option[OvaraEBLaajuus]
)

case class OvaraEBArvosana(
  entiteetinTyyppi: String = "EBArvosana",
  arvosana: OvaraKoodi,
  hyvaksytty: Boolean
)

case class OvaraEBLaajuus(
  entiteetinTyyppi: String = "EBLaajuus",
  arvo: BigDecimal,
  yksikko: OvaraKoodi
)

case class OvaraIBTutkinto(
  entiteetinTyyppi: String = "IBTutkinto",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  suorituskieli: Option[OvaraKoodi],
  osasuoritukset: Seq[OvaraIBOppiaineSuoritus]
) extends OvaraGeneerinenSuoritus

case class OvaraIBOppiaineSuoritus(
  entiteetinTyyppi: String = "IBOppiaineSuoritus",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  ryhma: Option[OvaraIBOppiaineRyhma],
  predictedArvosana: Option[OvaraIBArvosana],
  laajuus: Option[OvaraIBLaajuus],
  suorituskieli: Option[OvaraKoodi]
)

case class OvaraIBOppiaineRyhma(
  entiteetinTyyppi: String = "IBOppiaineRyhma",
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi
)

case class OvaraIBArvosana(
  entiteetinTyyppi: String = "IBArvosana",
  arvosana: OvaraKoodi,
  hyvaksytty: Boolean
)

case class OvaraIBLaajuus(
  entiteetinTyyppi: String = "IBLaajuus",
  arvo: BigDecimal,
  yksikko: OvaraKoodi
)

case class OvaraTuva(
  entiteetinTyyppi: String = "Tuva",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: LocalDate,
  vahvistusPaivamaara: Option[LocalDate],
  suoritusVuosi: Int,
  hyvaksyttyLaajuus: Option[OvaraLaajuus],
  lahtokoulut: List[OvaraLahtokoulu]
) extends OvaraGeneerinenSuoritus

case class OvaraVapaaSivistystyo(
  entiteetinTyyppi: String = "VapaaSivistystyo",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: LocalDate,
  vahvistusPaivamaara: Option[LocalDate],
  suoritusVuosi: Int,
  hyvaksyttyLaajuus: Option[OvaraLaajuus],
  suoritusKieli: OvaraKoodi,
  lahtokoulut: List[OvaraLahtokoulu]
) extends OvaraGeneerinenSuoritus

// ---- Ammatillinen ----

case class OvaraAmmatillinenOpiskeluoikeus(
  entiteetinTyyppi: String = "AmmatillinenOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  tunniste: UUID,
  oid: String,
  oppilaitos: OvaraOppilaitos,
  suoritukset: Seq[OvaraAmmatillinenSuoritus],
  tila: Option[OvaraKoskiOpiskeluoikeusTila],
  jaksot: List[OvaraOpiskeluoikeusJakso]
)

sealed trait OvaraAmmatillinenSuoritus

case class OvaraAmmatillinenPerustutkinto(
  entiteetinTyyppi: String = "AmmatillinenPerustutkinto",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  keskiarvo: Option[BigDecimal],
  suoritustapa: OvaraKoodi,
  suoritusKieli: OvaraKoodi,
  osat: Seq[OvaraAmmatillisenTutkinnonOsa]
) extends OvaraAmmatillinenSuoritus

case class OvaraAmmatillinenTutkintoOsittainen(
  entiteetinTyyppi: String = "AmmatillinenTutkintoOsittainen",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  korotettuKeskiarvo: Option[BigDecimal],
  korotettuOpiskeluoikeusOid: Option[String],
  suoritustapa: OvaraKoodi,
  suoritusKieli: OvaraKoodi,
  osat: Seq[OvaraAmmatillisenTutkinnonOsa]
) extends OvaraAmmatillinenSuoritus

case class OvaraAmmatillisenTutkinnonOsa(
  entiteetinTyyppi: String = "AmmatillisenTutkinnonOsa",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  yto: Boolean,
  arviointiPaiva: Option[LocalDate],
  arvosana: Option[OvaraArvosana],
  laajuus: Option[OvaraLaajuus],
  osaAlueet: Seq[OvaraAmmatillisenTutkinnonOsaAlue],
  korotettu: Option[OvaraKorotus]
)

case class OvaraAmmatillisenTutkinnonOsaAlue(
  entiteetinTyyppi: String = "AmmatillisenTutkinnonOsaAlue",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  arvosana: Option[OvaraKoodi],
  laajuus: Option[OvaraLaajuus],
  korotettu: Option[OvaraKorotus]
)

case class OvaraAmmattiTutkinto(
  entiteetinTyyppi: String = "AmmattiTutkinto",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  suoritustapa: OvaraKoodi,
  suoritusKieli: OvaraKoodi
) extends OvaraAmmatillinenSuoritus

case class OvaraErikoisAmmattiTutkinto(
  entiteetinTyyppi: String = "ErikoisAmmattiTutkinto",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  suoritusKieli: OvaraKoodi
) extends OvaraAmmatillinenSuoritus

case class OvaraTelma(
  entiteetinTyyppi: String = "Telma",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  aloitusPaivamaara: LocalDate,
  vahvistusPaivamaara: Option[LocalDate],
  suoritusVuosi: Int,
  suoritusKieli: OvaraKoodi,
  hyvaksyttyLaajuus: Option[OvaraLaajuus],
  lahtokoulut: List[OvaraLahtokoulu]
) extends OvaraAmmatillinenSuoritus

// ---- Perusopetus ----

case class OvaraPerusopetuksenOpiskeluoikeus(
  entiteetinTyyppi: String = "PerusopetuksenOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  tunniste: UUID,
  oid: Option[String],
  oppilaitosOid: String,
  suoritukset: Seq[OvaraPerusopetuksenSuoritus],
  lisatiedot: Option[OvaraKoskiLisatiedot],
  tila: OvaraSuoritusTila,
  jaksot: List[OvaraOpiskeluoikeusJakso]
)

sealed trait OvaraPerusopetuksenSuoritus

case class OvaraPerusopetuksenOppimaara(
  entiteetinTyyppi: String = "PerusopetuksenOppimaara",
  tunniste: UUID,
  versioTunniste: Option[UUID],
  oppilaitos: OvaraOppilaitos,
  luokka: Option[String],
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  suoritusKieli: OvaraKoodi,
  koulusivistyskieli: Set[OvaraKoodi],
  yksilollistaminen: Option[OvaraPerusopetuksenYksilollistaminen],
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  aineet: Seq[OvaraPerusopetuksenOppiaine],
  lahtokoulut: List[OvaraLahtokoulu],
  syotetty: Boolean,
  vuosiluokkiinSitoutumatonOpetus: Boolean,
  luokkaAste: Option[Int]
) extends OvaraPerusopetuksenSuoritus

case class OvaraPerusopetuksenOppimaaranOppiaineidenSuoritus(
  entiteetinTyyppi: String = "PerusopetuksenOppimaaranOppiaineidenSuoritus",
  tunniste: UUID,
  versioTunniste: Option[UUID],
  oppilaitos: OvaraOppilaitos,
  koskiTila: OvaraKoodi,
  supaTila: OvaraSuoritusTila,
  suoritusKieli: OvaraKoodi,
  aloitusPaivamaara: Option[LocalDate],
  vahvistusPaivamaara: Option[LocalDate],
  aineet: Set[OvaraPerusopetuksenOppiaine],
  syotetty: Boolean
) extends OvaraPerusopetuksenSuoritus

case class OvaraPerusopetuksenOppiaine(
  entiteetinTyyppi: String = "PerusopetuksenOppiaine",
  tunniste: UUID,
  nimi: OvaraKielistetty,
  koodi: OvaraKoodi,
  arvosana: OvaraKoodi,
  kieli: Option[OvaraKoodi],
  pakollinen: Boolean,
  yksilollistetty: Option[Boolean],
  rajattu: Option[Boolean]
)

case class OvaraPerusopetukseenValmistavaOpetus(
  entiteetinTyyppi: String = "PerusopetukseenValmistavaOpetus",
  lahtokoulut: List[OvaraLahtokoulu]
) extends OvaraPerusopetuksenSuoritus

case class OvaraPoistettuOpiskeluoikeus(
  entiteetinTyyppi: String = "PoistettuOpiskeluoikeus",
  metadata: OvaraVersioMetadata,
  oid: String
)

case class OvaraHenkiloMetadata(
  viimeisinMuutos: Instant
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
  metadata: OvaraHenkiloMetadata,
  kkOpiskeluoikeudet: Seq[OvaraKKOpiskeluoikeus],
  kkSynteettisetOpiskeluoikeudet: Seq[OvaraKKSynteettinenOpiskeluoikeus],
  yoOpiskeluoikeudet: Seq[OvaraYOOpiskeluoikeus],
  geneerisetOpiskeluoikeudet: Seq[OvaraGeneerinenOpiskeluoikeus],
  ammatillisetOpiskeluoikeudet: Seq[OvaraAmmatillinenOpiskeluoikeus],
  perusopetuksenOpiskeluoikeudet: Seq[OvaraPerusopetuksenOpiskeluoikeus],
  poistetutOpiskeluoikeudet: Seq[OvaraPoistettuOpiskeluoikeus],
  lahtokoulut: Seq[OvaraLahtokoulu] = Seq.empty
)
