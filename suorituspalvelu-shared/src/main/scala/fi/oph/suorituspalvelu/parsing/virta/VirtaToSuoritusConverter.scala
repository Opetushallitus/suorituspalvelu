package fi.oph.suorituspalvelu.parsing.virta

import fi.oph.suorituspalvelu.business.{
  KKOpintosuoritus, KKOpiskeluoikeus, KKOpiskeluoikeusBase, KKOpiskeluoikeusTila, KKSynteettinenOpiskeluoikeus,
  KKSynteettinenSuoritus, KKTutkinto, Suoritus, SuoritusTila
}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import org.slf4j.LoggerFactory

import java.time.LocalDate
import java.util.UUID
import scala.collection.immutable.*

/**
 * Muuntaa Virran suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object VirtaToSuoritusConverter {
  private val LOG = LoggerFactory.getLogger(getClass)

  private val VIRTA_TUTKINTO_LAJI = 1
  private val VIRTA_OPINTOSUORITUS_LAJI = 2
  val VIRTA_OO_TILA_KOODISTO = "virtaopiskeluoikeudentila"

  private val OPISKELUOIKEUS_TILA_VALMISTUNUT = "3"

  val allowMissingFields = new ThreadLocal[Boolean]

  def dummy[A](): A =
    if (allowMissingFields.get())
      null.asInstanceOf[A]
    else
      throw new RuntimeException("Dummies not allowed")

  def getDefaultNimi(nimet: Seq[VirtaNimi]): Option[String] =
    getNimi(nimet, "fi").orElse(nimet.find(n => n.kieli.isEmpty).map(n => n.nimi))

  def getNimi(nimet: Seq[VirtaNimi], kieli: String): Option[String] =
    nimet.find(n => n.kieli.exists(k => kieli.equals(k))).map(n => n.nimi)

  private def latestJakso(opiskeluoikeus: VirtaOpiskeluoikeus): Option[VirtaJakso] = {
    opiskeluoikeus.Jakso.maxByOption(_.AlkuPvm)
  }

  private def latestTila(opiskeluoikeus: VirtaOpiskeluoikeus): VirtaTila = {
    // Opiskeluoikeudella on aina vähintään yksi tila
    opiskeluoikeus.Tila.maxBy(_.AlkuPvm)
  }

  // Lukee opiskeluoikeuden tilan ja muuntaa sen suorituspalvelun suorituksen tilaksi.
  private def getSuoritustilaFromOpiskeluoikeus(opiskeluoikeus: VirtaOpiskeluoikeus): SuoritusTila =
    latestTila(opiskeluoikeus).Koodi match
      case "1" => SuoritusTila.KESKEN // aktiivinen
      case "2" => SuoritusTila.KESKEN // optio
      case "3" => SuoritusTila.VALMIS // valmistunut
      case "4" => SuoritusTila.KESKEYTYNYT // passivoitu
      case "5" => SuoritusTila.KESKEYTYNYT // luopunut
      case "6" => SuoritusTila.KESKEYTYNYT // päättynyt

  // Muuntaa Virta-tilan suorituspalvelun opiskeluoikeuden tilaksi
  def convertVirtaOpiskeluoikeusTila(koodiArvo: String): KKOpiskeluoikeusTila =
    koodiArvo match
      case "1" => KKOpiskeluoikeusTila.VOIMASSA // aktiivinen
      case "2" => KKOpiskeluoikeusTila.VOIMASSA // optio
      case "3" => KKOpiskeluoikeusTila.PAATTYNYT // valmistunut
      case "4" => KKOpiskeluoikeusTila.PAATTYNYT // passivoitu
      case "5" => KKOpiskeluoikeusTila.PAATTYNYT // luopunut
      case "6" => KKOpiskeluoikeusTila.PAATTYNYT // päättynyt

  private def isPaattynytOpiskeluoikeus(opiskeluoikeus: VirtaOpiskeluoikeus) = {
    convertVirtaOpiskeluoikeusTila(latestTila(opiskeluoikeus).Koodi) == KKOpiskeluoikeusTila.PAATTYNYT
  }

  private def sisaltyvatAvaimet(suoritus: VirtaOpintosuoritus): Set[String] =
    suoritus.Sisaltyvyys.map(_.sisaltyvaOpintosuoritusAvain).toSet

  private def isSuorituksenOpiskeluoikeus = (suoritus: VirtaOpintosuoritus, opiskeluoikeus: VirtaOpiskeluoikeus) =>
    suoritus.opiskeluoikeusAvain.contains(opiskeluoikeus.avain) &&
      suoritus.opiskelijaAvain == opiskeluoikeus.opiskelijaAvain

  private def suorituksenOpiskeluoikeusIsMissingOrMatches(
    suoritus: Option[VirtaOpintosuoritus],
    opiskeluoikeus: VirtaOpiskeluoikeus
  ): Boolean =
    suoritus.isEmpty || suoritus.flatMap(_.opiskeluoikeusAvain).isEmpty ||
      isSuorituksenOpiskeluoikeus(suoritus.get, opiskeluoikeus)

  private def sisaltyyOpiskeluoikeuteen(
    suoritus: VirtaOpintosuoritus,
    opiskeluoikeus: VirtaOpiskeluoikeus,
    suorituksetByAvain: Map[String, VirtaOpintosuoritus],
    rootSuoritus: Option[VirtaOpintosuoritus] = None
  ): Boolean = {
    isSuorituksenOpiskeluoikeus(suoritus, opiskeluoikeus) &&
    suorituksenOpiskeluoikeusIsMissingOrMatches(rootSuoritus, opiskeluoikeus) ||
    suoritus.Sisaltyvyys.exists(sis => {
      suorituksetByAvain.get(sis.sisaltyvaOpintosuoritusAvain) match {
        case Some(s) =>
          sisaltyyOpiskeluoikeuteen(s, opiskeluoikeus, suorituksetByAvain, Some(rootSuoritus.getOrElse(suoritus)))
        case _ => false
      }
    })
  }

  private val TUTKINTOON_JOHTAVAT_OPISKELUOIKEUS_TYYPIT = Set(
    "1", // Ammattikorkeakoulututkinto
    "2", // Alempi korkeakoulututkinto
    "3", // Ylempi ammattikorkeakoulututkinto
    "4", // Ylempi korkeakoulututkinto
    "6", // Lisensiaatintutkinto
    "7" // Tohtorintutkinto
  )

  def isTutkintoonJohtavaOpiskeluoikeusTyyppi(opiskeluoikeusTyyppi: String): Boolean =
    TUTKINTOON_JOHTAVAT_OPISKELUOIKEUS_TYYPIT.contains(opiskeluoikeusTyyppi)

  // Jos juuritasolla vain yksi tutkinto ja opintosuorituksia, eikä tutkinnolla ole osasuorituksia,
  // siirretään kaikki opintosuoritukset tutkinnon alle
  private def moveOpintojaksotUnderTutkintoWhenNeeded(suoritukset: Seq[Suoritus]): Seq[Suoritus] = {
    val tutkinnot = suoritukset.collect { case t: KKTutkinto => t }
    val opintojaksot = suoritukset.collect { case o: KKOpintosuoritus => o }
    if (tutkinnot.size == 1 && tutkinnot.head.suoritukset.isEmpty && opintojaksot.nonEmpty) {
      Seq(tutkinnot.head.copy(suoritukset = opintojaksot))
    } else {
      suoritukset
    }
  }

  // Virrasta ei palaudu keskeneräisiä suorituksia, joten luodaan sellainen opiskeluoikeuden tiedoista
  private def addKeskenerainenTutkinnonSuoritus(
    suoritukset: Seq[Suoritus],
    opiskeluoikeus: VirtaOpiskeluoikeus
  ): List[Suoritus] = {
    val (opintojaksot, muutSuoritukset) = suoritukset.toList.partition(_.isInstanceOf[KKOpintosuoritus])
    val koulutusKoodi = latestJakso(opiskeluoikeus).flatMap(_.Koulutuskoodi)
    KKSynteettinenSuoritus(
      tunniste = UUID.randomUUID(),
      nimi = None,
      supaTila = SuoritusTila.KESKEN,
      komoTunniste = koulutusKoodi.getOrElse(""),
      aloitusPvm = Some(opiskeluoikeus.AlkuPvm),
      suoritusPvm = None,
      myontaja = opiskeluoikeus.Myontaja,
      koulutusKoodi = koulutusKoodi,
      opiskeluoikeusAvain = Some(opiskeluoikeus.avain),
      suoritukset = opintojaksot
    ) :: muutSuoritukset
  }

  private def sisallytaOpintojaksotOsasuorituksina(opiskeluoikeusTyyppi: String) = {
    Set(
      "8", // Kotimainen opiskelijaliikkuvuus
      "13" // Avoimen opinnot
    ).contains(opiskeluoikeusTyyppi)
  }

  private def createSyntheticSuoritusWrapper(
    suoritukset: Seq[Suoritus],
    opiskeluoikeus: VirtaOpiskeluoikeus,
    viimeisinTutkintoKoulutuskoodi: Option[String]
  ): KKSynteettinenSuoritus = {
    val tila = latestTila(opiskeluoikeus)
    val jaksonNimi = opiskeluoikeus.Jakso.sortBy(_.AlkuPvm)(
      Ordering[LocalDate].reverse
    ).find(_.Nimi.nonEmpty).map(_.Nimi).getOrElse(Seq.empty)

    KKSynteettinenSuoritus(
      tunniste = UUID.randomUUID(),
      nimi = virtaNimiToKielistetty(jaksonNimi),
      supaTila = getSuoritustilaFromOpiskeluoikeus(opiskeluoikeus),
      komoTunniste = opiskeluoikeus.koulutusmoduulitunniste,
      aloitusPvm = Some(opiskeluoikeus.AlkuPvm),
      suoritusPvm = if (tila.Koodi == OPISKELUOIKEUS_TILA_VALMISTUNUT) Some(tila.AlkuPvm) else None,
      myontaja = opiskeluoikeus.Myontaja,
      koulutusKoodi = viimeisinTutkintoKoulutuskoodi,
      opiskeluoikeusAvain = Some(opiskeluoikeus.avain),
      suoritukset = suoritukset
    )
  }

  private def fixSuoritusRoots(suoritukset: Seq[Suoritus], opiskeluoikeus: VirtaOpiskeluoikeus): Seq[Suoritus] = {
    if (isTutkintoonJohtavaOpiskeluoikeusTyyppi(opiskeluoikeus.Tyyppi)) {
      val opiskeluoikeusJaksoKoulutuskoodit = opiskeluoikeus.Jakso.flatMap(_.Koulutuskoodi)
      val suoritusLoytyyKoulutuskoodilla = opiskeluoikeusJaksoKoulutuskoodit.exists(jaksoKoulutusKoodi => {
        suoritukset.exists {
          case t: KKTutkinto => t.koulutusKoodi.contains(jaksoKoulutusKoodi)
          case _ => false
        }
      })

      if (suoritusLoytyyKoulutuskoodilla) {
        moveOpintojaksotUnderTutkintoWhenNeeded(suoritukset)
      } else if (opiskeluoikeusJaksoKoulutuskoodit.nonEmpty && !isPaattynytOpiskeluoikeus(opiskeluoikeus)) {
        addKeskenerainenTutkinnonSuoritus(suoritukset, opiskeluoikeus)
      } else {
        val viimeisinTutkintoKoodi = latestJakso(opiskeluoikeus).flatMap(_.Koulutuskoodi)
        if (suoritukset.isEmpty)
          Seq(createSyntheticSuoritusWrapper(suoritukset, opiskeluoikeus, viimeisinTutkintoKoodi))
        else
          suoritukset
      }
    } else if (sisallytaOpintojaksotOsasuorituksina(opiskeluoikeus.Tyyppi)) {
      val (rootSuoritukset, osaSuoritukset) = suoritukset.partition(_.isInstanceOf[KKTutkinto])
      if (osaSuoritukset.nonEmpty)
        rootSuoritukset :+ createSyntheticSuoritusWrapper(osaSuoritukset, opiskeluoikeus, None)
      else
        suoritukset
    } else {
      suoritukset
    }
  }

  private def getVirtaOpiskeluoikeusId(virtaOpiskeluoikeus: VirtaOpiskeluoikeus): String =
    s"${virtaOpiskeluoikeus.Myontaja}_${virtaOpiskeluoikeus.avain}"

  private def getVirtaOpintosuoritusId(virtaOpintosuoritus: VirtaOpintosuoritus): String =
    s"${virtaOpintosuoritus.Myontaja}_${virtaOpintosuoritus.avain}"

  /**
   * Muuntaa VIRTA-opiskelijat suorituspalvelun opiskeluoikeuksiksi suorituksineen.
   * Deduplikoi VIRTA-opiskeluoikeudet ja -suoritukset myöntäjän (oppilaitos) ja avaimen perusteella.
   * Suoritushierarkialle tehdään reunatapauksissa korjauksia, jotta saadaan hierarkia selkeämmäksi.
   * Suorituksille, joilla ei ole opiskeluoikeuksia luodaan synteettisen suoritukset myöntäjän perustella ryhmiteltynä.
   * @param virtaOpiskelijat VIRTA-opiskelijat, jotka halutaan muuntaa suorituspalvelun opiskeluoikeuksiksi. Voi
   *                         sisältää duplikaatteja opiskeluoikeuksista ja suorituksista.
   * @return Suorituspalvelun opiskeluoikeudet
   */
  def toOpiskeluoikeudet(virtaOpiskelijat: Seq[VirtaOpiskelija]): Seq[KKOpiskeluoikeusBase] = {
    var seenOpiskeluoikeusIds: Set[String] = Set.empty
    var seenSuoritusIds: Set[String] = Set.empty
    var synteettisetOpiskeluoikeudetByMyontaja: Map[String, KKSynteettinenOpiskeluoikeus] = Map()

    val kkOpiskeluoikeudet = virtaOpiskelijat.flatMap(opiskelija => {
      val virtaOpiskeluoikeudet = opiskelija.Opiskeluoikeudet.filterNot(oo =>
        seenOpiskeluoikeusIds.contains(getVirtaOpiskeluoikeusId(oo))
      )
      val virtaSuoritukset = opiskelija.Opintosuoritukset.getOrElse(Seq.empty).filterNot(s =>
        seenSuoritusIds.contains(getVirtaOpintosuoritusId(s))
      )
      val suoritusRoots = virtaSuoritukset.filterNot(s =>
        virtaSuoritukset.exists(sisaltyvatAvaimet(_).contains(s.avain))
      )
      val suorituksetByAvain = virtaSuoritukset.groupBy(_.avain).map((suoritusAvain, suoritukset) => {
        if (suoritukset.size > 1) {
          LOG.error(
            s"Virta-Opiskelijalle avaimella ${opiskelija.avain} löytyi useita Virta-opintosuorituksia avaimella $suoritusAvain. Käytetään viimeisintä suoritusta."
          )
        }
        suoritusAvain -> suoritukset.maxBy(_.SuoritusPvm)
      })

      seenSuoritusIds ++= virtaSuoritukset.map(getVirtaOpintosuoritusId)
      seenOpiskeluoikeusIds ++= virtaOpiskeluoikeudet.map(getVirtaOpiskeluoikeusId)

      val (orphanSuoritukset, opiskeluoikeudet) =
        virtaOpiskeluoikeudet.foldLeft((suoritusRoots, List.empty[KKOpiskeluoikeusBase])) {
          case ((remainingSuoritusRoots, kkOpiskeluoikeudet), oo) =>
            val virtaTila = latestTila(oo).Koodi
            val (opiskeluoikeudenSuoritukset, muutSuoritukset) =
              remainingSuoritusRoots.partition(sisaltyyOpiskeluoikeuteen(_, oo, suorituksetByAvain))
            val jakso = latestJakso(oo)

            val kkOpiskeluoikeus = KKOpiskeluoikeus(
              tunniste = UUID.randomUUID(),
              virtaTunniste = oo.avain,
              tyyppiKoodi = oo.Tyyppi,
              koulutusKoodi = jakso.flatMap(_.Koulutuskoodi),
              alkuPvm = oo.AlkuPvm,
              loppuPvm = oo.LoppuPvm,
              virtaTila = fi.oph.suorituspalvelu.business.Koodi(
                virtaTila,
                VIRTA_OO_TILA_KOODISTO,
                None
              ), // otetaan viimeisin opiskeluoikeuden tila
              supaTila = convertVirtaOpiskeluoikeusTila(virtaTila),
              myontaja = oo.Myontaja,
              suoritukset =
                fixSuoritusRoots(toSuoritukset(Some(oo), opiskeluoikeudenSuoritukset, suorituksetByAvain), oo).toSet
            )
            (muutSuoritukset, kkOpiskeluoikeus :: kkOpiskeluoikeudet)
        }

      orphanSuoritukset.groupBy(_.Myontaja).foreach { case (myontaja, suoritukset) =>
        val existingSuoritukset =
          synteettisetOpiskeluoikeudetByMyontaja.get(myontaja).map(_.suoritukset).getOrElse(Set.empty)
        synteettisetOpiskeluoikeudetByMyontaja +=
          (myontaja -> KKSynteettinenOpiskeluoikeus(
            tunniste = UUID.randomUUID(),
            myontaja = myontaja,
            suoritukset = existingSuoritukset ++ toSuoritukset(None, suoritukset, suorituksetByAvain)
          ))
      }
      opiskeluoikeudet
    })
    kkOpiskeluoikeudet ++ synteettisetOpiskeluoikeudetByMyontaja.values
  }

  private def virtaNimiToKielistetty(virtaNimi: Seq[VirtaNimi]) = {
    val k = Kielistetty(
      fi = getDefaultNimi(virtaNimi),
      sv = getNimi(virtaNimi, "sv"),
      en = getNimi(virtaNimi, "en")
    )
    if (k.fi.isDefined || k.sv.isDefined || k.en.isDefined) Some(k) else None
  }

  private def toSuoritus(
    suoritus: VirtaOpintosuoritus,
    suorituksetByAvain: Map[String, VirtaOpintosuoritus],
    // jos ei opiskeluoikeutta, kyseessä suoritus ilman opiskeluoikeutta eli on luotu synteettinen opiskeluoikeus
    opiskeluoikeus: Option[VirtaOpiskeluoikeus]
  ): Option[Suoritus] = {
    suoritus.Laji match
      case VIRTA_TUTKINTO_LAJI => Some(KKTutkinto(
          tunniste = UUID.randomUUID(),
          nimi = virtaNimiToKielistetty(suoritus.Nimi),
          supaTila = opiskeluoikeus.map(getSuoritustilaFromOpiskeluoikeus).getOrElse(SuoritusTila.VALMIS),
          komoTunniste = suoritus.koulutusmoduulitunniste,
          opintoPisteet = suoritus.Laajuus.Opintopiste,
          aloitusPvm = opiskeluoikeus.map(_.AlkuPvm),
          suoritusPvm = Some(suoritus.SuoritusPvm),
          myontaja = suoritus.Myontaja,
          kieli = Some(suoritus.Kieli),
          koulutusKoodi = suoritus.Koulutuskoodi,
          opiskeluoikeusAvain = suoritus.opiskeluoikeusAvain,
          suoritukset = suoritus.Sisaltyvyys.flatMap(sis => {
            suorituksetByAvain.get(sis.sisaltyvaOpintosuoritusAvain).flatMap(suoritus =>
              toSuoritus(suoritus, suorituksetByAvain, opiskeluoikeus)
            )
          }),
          avain = Some(suoritus.avain)
        ))
      case VIRTA_OPINTOSUORITUS_LAJI => Some(KKOpintosuoritus(
          tunniste = UUID.randomUUID(),
          nimi = virtaNimiToKielistetty(suoritus.Nimi),
          supaTila = SuoritusTila.VALMIS,
          komoTunniste = suoritus.koulutusmoduulitunniste,
          opintoPisteet = suoritus.Laajuus.Opintopiste,
          opintoviikot = None,
          suoritusPvm = Some(suoritus.SuoritusPvm),
          hyvaksilukuPvm = suoritus.HyvaksilukuPvm,
          myontaja = suoritus.Myontaja,
          jarjestavaRooli = suoritus.Organisaatio.map(_.Rooli),
          jarjestavaKoodi = suoritus.Organisaatio.map(_.Koodi),
          jarjestavaOsuus = suoritus.Organisaatio.flatMap(_.Osuus),
          arvosana = suoritus.Arvosana.map(_.arvosana),
          arvosanaAsteikko = suoritus.Arvosana.map(_.asteikko),
          kieli = suoritus.Kieli,
          koulutusala = suoritus.Koulutusala.map(_.Koodi.koodi),
          koulutusalaKoodisto = suoritus.Koulutusala.map(_.Koodi.versio),
          opinnaytetyo = suoritus.Opinnaytetyo.exists(o => "1".equals(o)),
          opiskeluoikeusAvain = suoritus.opiskeluoikeusAvain,
          suoritukset = suoritus.Sisaltyvyys.flatMap(sis => {
            suorituksetByAvain.get(sis.sisaltyvaOpintosuoritusAvain).flatMap(suoritus =>
              toSuoritus(suoritus, suorituksetByAvain, opiskeluoikeus)
            )
          }),
          avain = suoritus.avain
        ))
      case default => None
  }

  def toSuoritukset(
    opiskeluoikeus: Option[VirtaOpiskeluoikeus],
    opintosuoritukset: Seq[VirtaOpintosuoritus],
    allSuorituksetByAvain: Map[String, VirtaOpintosuoritus] = Map.empty,
    allowMissingFieldsForTests: Boolean = false
  ): Seq[Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      opintosuoritukset.flatMap(suoritus => toSuoritus(suoritus, allSuorituksetByAvain, opiskeluoikeus))
    finally
      allowMissingFields.set(false)
}
