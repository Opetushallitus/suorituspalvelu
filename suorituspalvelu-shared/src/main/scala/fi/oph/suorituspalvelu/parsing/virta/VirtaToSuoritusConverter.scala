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
    opiskeluoikeus.Jakso.sortBy(_.AlkuPvm).lastOption
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

  private val TUTKINTOON_JOHTAVAT_OPISKELUOIKEUS_TYYPIT = Set("1", "2", "3", "4", "6", "7")
  private def isTutkintoonJohtava(opiskeluoikeus: VirtaOpiskeluoikeus) =
    TUTKINTOON_JOHTAVAT_OPISKELUOIKEUS_TYYPIT.contains(opiskeluoikeus.Tyyppi)

  // Jos vain yksi tutkinto ja tavallisia opintosuorituksia, siirretään kaikki opintosuoritukset tutkinnon alle
  private def moveOpintojaksotUnderRootSuoritusIfNecessary(suoritukset: Seq[Suoritus]): Seq[Suoritus] = {
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

  private def addSyntheticSuoritusWrapperWhenNeeded(
    suoritukset: Seq[Suoritus],
    opiskeluoikeus: VirtaOpiskeluoikeus,
    viimeisinTutkintoKoulutuskoodi: Option[String]
  ): Seq[Suoritus] = {
    val tyyppi = opiskeluoikeus.Tyyppi
    val (rootSuoritukset, osaSuoritukset) = if (sisallytaOpintojaksotOsasuorituksina(tyyppi)) {
      suoritukset.partition(_.isInstanceOf[KKTutkinto])
    } else {
      (suoritukset, Seq.empty)
    }

    // Lisätään synteettinen suoritus vain, jos löytyi osasuorituksia, jotka halutaan yhdistää, tai jos suorituksia
    // ei ole lainkaan.
    val newSuoritus: Option[Suoritus] = if (osaSuoritukset.nonEmpty || suoritukset.isEmpty) {
      val tila = latestTila(opiskeluoikeus)
      val vahvistusPaiva = if (tila.Koodi == OPISKELUOIKEUS_TILA_VALMISTUNUT) Some(tila.AlkuPvm) else None
      val jaksonNimi = opiskeluoikeus.Jakso.sortBy(_.AlkuPvm)(
        Ordering[LocalDate].reverse
      ).find(_.Nimi.nonEmpty).map(_.Nimi).getOrElse(Seq.empty)

      Some(KKSynteettinenSuoritus(
        tunniste = UUID.randomUUID(),
        // Jos koulutuskoodi löytyy, ei aseteta nimeä. Käytetään UI:ssa koulutuskoodin nimeä.
        nimi = if (viimeisinTutkintoKoulutuskoodi.isDefined) None else virtaNimiToKielistetty(jaksonNimi),
        supaTila = getSuoritustilaFromOpiskeluoikeus(opiskeluoikeus),
        komoTunniste = opiskeluoikeus.koulutusmoduulitunniste,
        aloitusPvm = Some(opiskeluoikeus.AlkuPvm),
        suoritusPvm = vahvistusPaiva,
        myontaja = opiskeluoikeus.Myontaja,
        koulutusKoodi = viimeisinTutkintoKoulutuskoodi,
        opiskeluoikeusAvain = Some(opiskeluoikeus.avain),
        suoritukset = osaSuoritukset
      ))
    } else {
      None
    }

    newSuoritus.toSeq ++ rootSuoritukset
  }

  private def fixSuoritusRoots(suoritukset: Seq[Suoritus], opiskeluoikeus: VirtaOpiskeluoikeus): Seq[Suoritus] = {
    if (isTutkintoonJohtava(opiskeluoikeus)) {
      val opiskeluoikeusJaksoKoulutuskoodit = opiskeluoikeus.Jakso.flatMap(_.Koulutuskoodi)
      val suoritusLoytyyKoulutuskoodilla = opiskeluoikeusJaksoKoulutuskoodit.exists(jaksoKoulutusKoodi => {
        suoritukset.exists {
          case t: KKTutkinto => t.koulutusKoodi.contains(jaksoKoulutusKoodi)
          case _ => false
        }
      })

      if (suoritusLoytyyKoulutuskoodilla) {
        moveOpintojaksotUnderRootSuoritusIfNecessary(suoritukset)
      } else if (opiskeluoikeusJaksoKoulutuskoodit.nonEmpty && !isPaattynytOpiskeluoikeus(opiskeluoikeus)) {
        addKeskenerainenTutkinnonSuoritus(suoritukset, opiskeluoikeus)
      } else {
        val viimeisinTutkintoKoodi = latestJakso(opiskeluoikeus).flatMap(_.Koulutuskoodi)
        addSyntheticSuoritusWrapperWhenNeeded(suoritukset, opiskeluoikeus, viimeisinTutkintoKoodi)
      }
    } else {
      addSyntheticSuoritusWrapperWhenNeeded(suoritukset, opiskeluoikeus, None)
    }
  }

  private def getVirtaOpiskeluoikeusAvain(virtaOpiskeluoikeus: VirtaOpiskeluoikeus): String =
    s"${virtaOpiskeluoikeus.Myontaja}_${virtaOpiskeluoikeus.avain}"

  private def getVirtaOpintosuoritusAvain(virtaOpintosuoritus: VirtaOpintosuoritus): String =
    s"${virtaOpintosuoritus.Myontaja}_${virtaOpintosuoritus.avain}"

  def toOpiskeluoikeudet(virtaOpiskelijat: Seq[VirtaOpiskelija]): Seq[KKOpiskeluoikeusBase] = {
    var seenOpiskeluoikeusIds: Set[String] = Set.empty
    var seenSuoritusIds: Set[String] = Set.empty

    virtaOpiskelijat.flatMap(opiskelija => {
      val virtaOpiskeluoikeudet = opiskelija.Opiskeluoikeudet.filterNot(oo =>
        seenOpiskeluoikeusIds.contains(getVirtaOpiskeluoikeusAvain(oo))
      )
      val virtaSuoritukset = opiskelija.Opintosuoritukset.getOrElse(Seq.empty).filterNot(s =>
        seenSuoritusIds.contains(getVirtaOpintosuoritusAvain(s))
      )
      val suoritusRoots = virtaSuoritukset.filterNot(s =>
        virtaSuoritukset.exists(sisaltyvatAvaimet(_).contains(s.avain))
      )
      val suorituksetByAvain = virtaSuoritukset.groupBy(_.avain).map((suoritusAvain, suoritukset) => {
        if (suoritukset.size > 1) {
          LOG.error(
            s"Virta-Opiskelijalle avaimella ${opiskelija.avain} löytyi useita Virta-opintosuorituksia avaimella $suoritusAvain!"
          )
        }
        suoritusAvain -> suoritukset.head
      })

      seenSuoritusIds ++= virtaSuoritukset.map(getVirtaOpintosuoritusAvain)
      seenOpiskeluoikeusIds ++= virtaOpiskeluoikeudet.map(getVirtaOpiskeluoikeusAvain)

      val (orphanSuoritukset, opiskeluoikeudet) =
        virtaOpiskeluoikeudet.foldLeft((suoritusRoots, List.empty[KKOpiskeluoikeusBase])) {
          case ((remainingSuoritusRoots, opiskeluOikeudet), oo) =>
            val virtaTila = latestTila(oo).Koodi
            val (opiskeluoikeudenSuoritukset, muutSuoritukset) =
              remainingSuoritusRoots.partition(sisaltyyOpiskeluoikeuteen(_, oo, suorituksetByAvain))
            val jakso = latestJakso(oo)

            val opiskeluOikeus = KKOpiskeluoikeus(
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
            (muutSuoritukset, opiskeluOikeus :: opiskeluOikeudet)
        }

      val synteettisetOpiskeluoikeudet = orphanSuoritukset.groupBy(_.Myontaja).map { case (myontaja, suoritukset) =>
        KKSynteettinenOpiskeluoikeus(
          UUID.randomUUID(),
          myontaja,
          toSuoritukset(None, suoritukset, suorituksetByAvain).toSet
        )
      }
      opiskeluoikeudet ++ synteettisetOpiskeluoikeudet
    })
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
    suoritus: fi.oph.suorituspalvelu.parsing.virta.VirtaOpintosuoritus,
    suorituksetByAvain: Map[String, fi.oph.suorituspalvelu.parsing.virta.VirtaOpintosuoritus],
    // jos ei opiskeluoikeutta, kyseessä suoritus ilman opiskeluoikeutta eli on luotu synteettinen opiskeluoikeus
    opiskeluoikeus: Option[fi.oph.suorituspalvelu.parsing.virta.VirtaOpiskeluoikeus]
  ): Option[Suoritus] = {
    // Suoritus ilman opiskeluoikeutta -> asetetaan tila valmiiksi
    val supaTila = opiskeluoikeus.map(getSuoritustilaFromOpiskeluoikeus).getOrElse(SuoritusTila.VALMIS)
    suoritus.Laji match
      case VIRTA_TUTKINTO_LAJI => Some(KKTutkinto(
          tunniste = UUID.randomUUID(),
          nimi = virtaNimiToKielistetty(suoritus.Nimi),
          supaTila = supaTila,
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
          supaTila = supaTila,
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
    opintosuoritukset: Seq[fi.oph.suorituspalvelu.parsing.virta.VirtaOpintosuoritus],
    allSuorituksetByAvain: Map[String, fi.oph.suorituspalvelu.parsing.virta.VirtaOpintosuoritus] = Map.empty,
    allowMissingFieldsForTests: Boolean = false
  ): Seq[Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      opintosuoritukset.flatMap(suoritus => toSuoritus(suoritus, allSuorituksetByAvain, opiskeluoikeus))
    finally
      allowMissingFields.set(false)
}
