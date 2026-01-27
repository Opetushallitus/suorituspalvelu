package fi.oph.suorituspalvelu.parsing.virta

import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeusTila, Suoritus, SuoritusTila, VirtaMuuSuoritus, VirtaOpintosuoritus, VirtaOpiskeluoikeus, VirtaOpiskeluoikeusBase, VirtaSynteettinenOpiskeluoikeus, VirtaTutkinto}
import org.slf4j.LoggerFactory

import java.time.LocalDate
import java.util.UUID
import scala.collection.immutable.*
/**
 * Muuntaa Kosken suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object VirtaToSuoritusConverter {
  val LOG = LoggerFactory.getLogger(getClass)

  var VIRTA_TUTKINTO_LAJI = 1
  var VIRTA_OO_TILA_KOODISTO = "virtaopiskeluoikeudentila"

  val allowMissingFields = new ThreadLocal[Boolean]

  def dummy[A](): A =
    if(allowMissingFields.get())
      null.asInstanceOf[A]
    else
      throw new RuntimeException("Dummies not allowed")

  def getDefaultNimi(nimet: Seq[Nimi]): Option[String] =
    getNimi(nimet, "fi").orElse(nimet.find(n => n.kieli.isEmpty).map(n => n.nimi))

  def getNimi(nimet: Seq[Nimi], kieli: String): Option[String] =
    nimet.find(n => n.kieli.exists(k => kieli.equals(k))).map(n => n.nimi)

  def convertVirtaTila(koodiArvo: String): KKOpiskeluoikeusTila =
    koodiArvo match
      case "1" => KKOpiskeluoikeusTila.VOIMASSA   // aktiivinen
      case "2" => KKOpiskeluoikeusTila.VOIMASSA   // optio
      case "3" => KKOpiskeluoikeusTila.PAATTYNYT  // valmistunut
      case "4" => KKOpiskeluoikeusTila.PAATTYNYT  // passivoitu
      case "5" => KKOpiskeluoikeusTila.PAATTYNYT  // luopunut
      case "6" => KKOpiskeluoikeusTila.PAATTYNYT  // päättynyt

  private def isPaattynytOpiskeluoikeus(opiskeluoikeus: Opiskeluoikeus) = {
    latestTila(opiskeluoikeus).exists(t => {
      convertVirtaTila(t.Koodi) == KKOpiskeluoikeusTila.PAATTYNYT
    })
  }

  private def sisaltyvatAvaimet(suoritus: Opintosuoritus): Set[String] =  suoritus.Sisaltyvyys.map(_.avain).toSet

  private def sisaltyyOpiskeluoikeuteen(suoritus: Opintosuoritus, opiskeluoikeus: Opiskeluoikeus, suorituksetByAvain: Map[String, Opintosuoritus], rootSuoritus: Option[Opintosuoritus] = None): Boolean = {
    suoritus.opiskeluoikeusAvain.contains(opiskeluoikeus.avain) &&
      (rootSuoritus.isEmpty || rootSuoritus.flatMap(_.opiskeluoikeusAvain).isEmpty || rootSuoritus.get.opiskeluoikeusAvain.contains(opiskeluoikeus.avain)) ||
      suoritus.Sisaltyvyys.exists(sis => {
        suorituksetByAvain.get(sis.avain) match {
          case Some(s) => sisaltyyOpiskeluoikeuteen(s, opiskeluoikeus, suorituksetByAvain, Some(rootSuoritus.getOrElse(suoritus)))
          case _ => false
        }
      })
  }

  private val TUTKINTOON_JOHTAVAT_OPISKELUOIKEUS_TYYPIT = Set("1", "2", "3", "4", "6", "7")

  private def isTutkintoonJohtava(opiskeluoikeus: Opiskeluoikeus) = TUTKINTOON_JOHTAVAT_OPISKELUOIKEUS_TYYPIT.contains(opiskeluoikeus.Tyyppi)

  private def moveOpintojaksotUnderRootSuoritusIfNecessary(suoritukset: Seq[Suoritus]): Seq[Suoritus] = {
    val tutkinnot = suoritukset.collect { case t: VirtaTutkinto => t }
    val opintojaksot = suoritukset.collect { case o: VirtaOpintosuoritus => o }
    if (tutkinnot.size == 1 && tutkinnot.head.suoritukset.isEmpty && opintojaksot.nonEmpty) {
      Seq(tutkinnot.head.copy(suoritukset = opintojaksot))
    } else {
      suoritukset
    }
  }

  private def addKeskenerainenTutkinnonSuoritus(suoritukset: Seq[Suoritus], opiskeluoikeus: Opiskeluoikeus): List[Suoritus] = {
    val (opintojaksot, muutSuoritukset) = suoritukset.toList.partition(_.isInstanceOf[VirtaOpintosuoritus])
    val koulutusKoodi = latestJakso(opiskeluoikeus).flatMap(_.Koulutuskoodi)
    VirtaTutkinto(
      tunniste = UUID.randomUUID(),
      nimiFi = None,
      nimiEn = None,
      nimiSv = None,
      komoTunniste = koulutusKoodi.getOrElse(""),
      opintoPisteet = 0,
      aloitusPvm = opiskeluoikeus.AlkuPvm,
      suoritusPvm = None,
      myontaja = opiskeluoikeus.Myontaja,
      kieli = "",
      koulutusKoodi = koulutusKoodi,
      opiskeluoikeusAvain = Some(opiskeluoikeus.avain),
      suoritukset = opintojaksot.collect { case o: VirtaOpintosuoritus => o },
      avain = ""
    ) :: muutSuoritukset
  }

  private def sisallytaOpintojaksotOsasuorituksina(opiskeluoikeusTyyppi: String) = {
    Set(
      "8", // Kotimainen opiskelijaliikkuvuus
      "13" // Avoimen opinnot
      ).contains(opiskeluoikeusTyyppi)
  }

  private def latestJakso(opiskeluoikeus: Opiskeluoikeus): Option[Jakso] = {
    opiskeluoikeus.Jakso.sortBy(_.AlkuPvm).lastOption
  }

  private def latestTila(opiskeluoikeus: Opiskeluoikeus): Option[Tila] = {
    opiskeluoikeus.Tila.sortBy(_.AlkuPvm).lastOption
  }

  private val OPISKELUOIKEUS_TILA_VALMISTUNUT = "3"

  private def addMuuKorkeakouluSuoritus(
    suoritukset: Seq[Suoritus],
    opiskeluoikeus: Opiskeluoikeus,
    viimeisinTutkintoKoulutuskoodi: Option[String]): Seq[Suoritus] = {
    val tyyppi = opiskeluoikeus.Tyyppi
    val (rootSuoritukset, osaSuoritukset) = if (sisallytaOpintojaksotOsasuorituksina(opiskeluoikeus.Tyyppi)) {
      val (opintojaksot, muut) = suoritukset.foldRight((List.empty[VirtaOpintosuoritus], List.empty[Suoritus])) {
        case (jakso: VirtaOpintosuoritus, (jaksot, muut)) => (jakso :: jaksot, muut)
        case (muu, (jaksot, muut)) => (jaksot, muu :: muut)
      }
      (muut, if (opintojaksot.isEmpty) Seq.empty else opintojaksot)
    } else {
      (suoritukset, Seq.empty)
    }

    val vahvistusPaiva = latestTila(opiskeluoikeus).filter(_.Koodi == OPISKELUOIKEUS_TILA_VALMISTUNUT).map(_.AlkuPvm)
    val jaksonNimi = opiskeluoikeus.Jakso.sortBy(_.AlkuPvm)(Ordering[LocalDate].reverse).find(_.Nimi.nonEmpty).map(_.Nimi).getOrElse(Seq.empty)
    val nimiFallback = Some(opiskeluoikeus.koulutusmoduulitunniste.stripPrefix("#").stripSuffix("/").trim).filter(_.nonEmpty)

    val newSuoritus: Option[Suoritus] = if (osaSuoritukset.nonEmpty) {
      Some(viimeisinTutkintoKoulutuskoodi.map(viimeisinKoulutusKoodi => {
        VirtaTutkinto(
          tunniste = UUID.randomUUID(),
          nimiFi = None,
          nimiSv = None,
          nimiEn = None,
          komoTunniste = viimeisinKoulutusKoodi,
          opintoPisteet = 0,
          aloitusPvm = opiskeluoikeus.AlkuPvm,
          suoritusPvm = vahvistusPaiva,
          myontaja = opiskeluoikeus.Myontaja,
          kieli = "",
          koulutusKoodi = Some(viimeisinKoulutusKoodi),
          opiskeluoikeusAvain = Some(opiskeluoikeus.avain),
          suoritukset = osaSuoritukset,
          avain = ""
        )
      }).getOrElse(VirtaOpintosuoritus(
        tunniste = UUID.randomUUID(),
        nimiFi = getDefaultNimi(jaksonNimi).orElse(nimiFallback),
        nimiSv = getNimi(jaksonNimi, "sv").orElse(nimiFallback),
        nimiEn = getNimi(jaksonNimi, "en").orElse(nimiFallback),
        komoTunniste = opiskeluoikeus.koulutusmoduulitunniste,
        opintoPisteet = 0,
        opintoviikot = None,
        suoritusPvm = vahvistusPaiva,
        hyvaksilukuPvm = None,
        myontaja = opiskeluoikeus.Myontaja,
        jarjestavaRooli = None,
        jarjestavaKoodi = None,
        jarjestavaOsuus = None,
        arvosana = None,
        arvosanaAsteikko = None,
        kieli = "",
        koulutusala = 1,
        koulutusalaKoodisto = "koulutusala",
        opinnaytetyo = false,
        opiskeluoikeusAvain = Some(opiskeluoikeus.avain),
        suoritukset = osaSuoritukset,
        avain = ""
     )))
    } else {
      None
    }

    newSuoritus.toSeq ++ rootSuoritukset
  }

  private def fixRootSuoritusIfNecessary(suoritukset: Seq[Suoritus], opiskeluoikeus: Opiskeluoikeus): Seq[Suoritus] = {
    val opiskeluoikeusJaksoKoulutuskoodit = opiskeluoikeus.Jakso.flatMap(_.Koulutuskoodi)
    val suoritusLoytyyKoulutuskoodilla = opiskeluoikeusJaksoKoulutuskoodit.exists(jaksoKoulutusKoodi => {
      suoritukset.exists {
        case t: VirtaTutkinto => t.koulutusKoodi.contains(jaksoKoulutusKoodi)
        case _ => false
      }
    })

    if (suoritusLoytyyKoulutuskoodilla) {
      moveOpintojaksotUnderRootSuoritusIfNecessary(suoritukset)
    } else if (opiskeluoikeusJaksoKoulutuskoodit.nonEmpty && !isPaattynytOpiskeluoikeus(opiskeluoikeus)) {
      addKeskenerainenTutkinnonSuoritus(suoritukset, opiskeluoikeus)
    } else if (opiskeluoikeusJaksoKoulutuskoodit.nonEmpty) {
      val viimeisinTutkintoKoodi = latestJakso(opiskeluoikeus).flatMap(_.Koulutuskoodi)
      addMuuKorkeakouluSuoritus(suoritukset, opiskeluoikeus, viimeisinTutkintoKoodi)
    } else {
      addMuuKorkeakouluSuoritus(suoritukset, opiskeluoikeus, None)
    }
  }

  def rearrangeSuorituksetIfNecessary(suoritukset: Seq[Suoritus], opiskeluoikeus: Opiskeluoikeus): Seq[Suoritus] = {
    if (isTutkintoonJohtava(opiskeluoikeus)) {
      fixRootSuoritusIfNecessary(suoritukset, opiskeluoikeus)
    } else {
      addMuuKorkeakouluSuoritus(suoritukset, opiskeluoikeus, None)
    }
  }

  def toOpiskeluoikeudet(virtaSuoritukset: VirtaSuoritukset): Seq[VirtaOpiskeluoikeusBase] = {
      val suoritukset = virtaSuoritukset.Body.OpiskelijanKaikkiTiedotResponse.Virta.flatMap(_.Opintosuoritukset).flatten
      val virtaOpiskeluoikeudet = virtaSuoritukset.Body.OpiskelijanKaikkiTiedotResponse.Virta.flatMap(_.Opiskeluoikeudet)
      val suoritusRoots = suoritukset.filter(s => !suoritukset.exists(sisaltyvatAvaimet(_).contains(s.avain)))

      val suorituksetByAvain = suoritukset.map(s => s.avain -> s).toMap

      val (orphanSuoritukset, opiskeluoikeudet) = virtaOpiskeluoikeudet.foldLeft((suoritusRoots, List.empty[VirtaOpiskeluoikeusBase]))
        { case ((remainingSuoritusRoots, opiskeluOikeudet), oo) => {
          val virtaTila = oo.Tila.sortBy(_.AlkuPvm).lastOption.map(_.Koodi).getOrElse("")
          val (opiskeluoikeudenSuoritukset, muutSuoritukset) = remainingSuoritusRoots.partition(sisaltyyOpiskeluoikeuteen(_, oo, suorituksetByAvain))
          val jakso = latestJakso(oo)

          val opiskeluOikeus = VirtaOpiskeluoikeus(
            tunniste = UUID.randomUUID(),
            virtaTunniste = oo.avain,
            tyyppiKoodi = oo.Tyyppi,
            koulutusKoodi = jakso.flatMap(_.Koulutuskoodi),
            alkuPvm = oo.AlkuPvm,
            loppuPvm = oo.LoppuPvm,
            virtaTila = fi.oph.suorituspalvelu.business.Koodi(virtaTila, VIRTA_OO_TILA_KOODISTO, None), // otetaan viimeisin opiskeluoikeuden tila
            supaTila = convertVirtaTila(virtaTila),
            myontaja = oo.Myontaja,
            suoritukset = rearrangeSuorituksetIfNecessary(toSuoritukset(Some(oo), opiskeluoikeudenSuoritukset, suorituksetByAvain), oo).toSet
          )
          (muutSuoritukset, opiskeluOikeus :: opiskeluOikeudet)
        }
      }

      val synteettisetOpiskeluoikeudet = orphanSuoritukset.groupBy(_.Myontaja).map { case (myontaja, suoritukset) =>
        VirtaSynteettinenOpiskeluoikeus(
          UUID.randomUUID(),
          myontaja,
          toSuoritukset(None, suoritukset, suorituksetByAvain).toSet,
        )
      }

      opiskeluoikeudet ++ synteettisetOpiskeluoikeudet
  }


  private def toSuoritus(suoritus: fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus,
    suorituksetByAvain: Map[String, fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus],
    opiskeluoikeus: Option[fi.oph.suorituspalvelu.parsing.virta.Opiskeluoikeus]
  ): Option[Suoritus] = {
    (suoritus.Laji, opiskeluoikeus) match
      // TODO: Onko ongelma, jos vaaditaan opiskeluoikeuden olemassaolo tässä?
      case (1, Some(oo)) => Some(VirtaTutkinto(
        UUID.randomUUID(),
        getDefaultNimi(suoritus.Nimi),
        getNimi(suoritus.Nimi, "sv"),
        getNimi(suoritus.Nimi, "en"),
        suoritus.koulutusmoduulitunniste,
        suoritus.Laajuus.Opintopiste,
        oo.AlkuPvm,
        Some(suoritus.SuoritusPvm),
        suoritus.Myontaja,
        suoritus.Kieli,
        suoritus.Koulutuskoodi.orElse(latestJakso(oo).flatMap(_.Koulutuskoodi)),
        suoritus.opiskeluoikeusAvain,
        suoritus.Sisaltyvyys.flatMap(sis => {
           suorituksetByAvain.get(sis.avain).flatMap(s =>
             toSuoritus(s, suorituksetByAvain, opiskeluoikeus).asInstanceOf[Option[VirtaOpintosuoritus]]
           )
        }),
        suoritus.avain
      ))
      case (2, _) => Some(VirtaOpintosuoritus(
        UUID.randomUUID(),
        getDefaultNimi(suoritus.Nimi),
        getNimi(suoritus.Nimi, "sv"),
        getNimi(suoritus.Nimi, "en"),
        suoritus.koulutusmoduulitunniste,
        suoritus.Laajuus.Opintopiste,
        None,
        Some(suoritus.SuoritusPvm),
        suoritus.HyvaksilukuPvm,
        suoritus.Myontaja,
        suoritus.Organisaatio.map(_.Rooli),
        suoritus.Organisaatio.map(_.Koodi),
        suoritus.Organisaatio.flatMap(_.Osuus),
        suoritus.Arvosana.map(_.arvosana),
        suoritus.Arvosana.map(_.asteikko),
        suoritus.Kieli,
        suoritus.Koulutusala.map(_.Koodi.koodi).get,
        suoritus.Koulutusala.map(_.Koodi.versio).get,
        suoritus.Opinnaytetyo.exists(o => "1".equals(o)),
        suoritus.opiskeluoikeusAvain,
        suoritus.Sisaltyvyys.flatMap(sis => {
          suorituksetByAvain.get(sis.avain).flatMap(s =>
            toSuoritus(s, suorituksetByAvain, opiskeluoikeus).asInstanceOf[Option[VirtaOpintosuoritus]]
          )
        }),
        suoritus.avain
      ))
      case default => None
  }

  def toSuoritukset(
    opiskeluoikeus: Option[Opiskeluoikeus],
    opintosuoritukset: Seq[fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus],
    allSuorituksetByAvain: Map[String, fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus] = Map.empty,
    allowMissingFieldsForTests: Boolean = false): Seq[Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      opintosuoritukset.flatMap(suoritus => toSuoritus(suoritus, allSuorituksetByAvain, opiskeluoikeus))
    finally
      allowMissingFields.set(false)
}
