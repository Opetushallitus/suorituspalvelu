package fi.oph.suorituspalvelu.parsing.virta

import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeusTila, Suoritus, VirtaOpintosuoritus, VirtaOpiskeluoikeus, VirtaOpiskeluoikeusBase, VirtaSynteettinenOpiskeluoikeus, VirtaTutkinto}
import org.slf4j.LoggerFactory

import java.util.UUID

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
  def toOpiskeluoikeudet(virtaSuoritukset: VirtaSuoritukset): Seq[VirtaOpiskeluoikeus] = {
    val oikeudet = virtaSuoritukset.Body.OpiskelijanKaikkiTiedotResponse.Virta.flatMap(o => {
      o.Opiskeluoikeudet.map(oo => {
        val virtaTila = oo.Tila.maxBy(t => t.AlkuPvm).Koodi
        VirtaOpiskeluoikeus(
          UUID.randomUUID(),
          oo.avain,
          oo.Tyyppi,
          oo.Jakso.Koulutuskoodi,
          oo.AlkuPvm,
          oo.LoppuPvm,
          fi.oph.suorituspalvelu.business.Koodi(virtaTila, VIRTA_OO_TILA_KOODISTO, None), // otetaan viimeisin opiskeluoikeuden tila
          convertVirtaTila(virtaTila),
          oo.Myontaja,
          toSuoritukset(oo, o.Opintosuoritukset.getOrElse(Seq.empty)).toSet

  def toOpiskeluoikeudet(virtaSuoritukset: VirtaSuoritukset): Seq[VirtaOpiskeluoikeusBase] = {
    virtaSuoritukset.Body.OpiskelijanKaikkiTiedotResponse.Virta.flatMap(o => {
      val suoritukset = o.Opintosuoritukset.getOrElse(Seq.empty)
      val suoritusRoots = suoritukset.filter(s => !suoritukset.exists(sisaltyvatAvaimet(_).contains(s.avain)))

      val suorituksetByAvain = suoritukset.map(s => s.avain -> s).toMap

      val (orphanSuoritukset, opiskeluoikeudet) = o.Opiskeluoikeudet.foldLeft((suoritusRoots, List.empty[VirtaOpiskeluoikeusBase]))
        { case ((remainingSuoritusRoots, opiskeluOikeudet), oo) => {
          val virtaTila = oo.Tila.maxBy(t => t.AlkuPvm).Koodi
          val (opiskeluoikeudenSuoritukset, muutSuoritukset) = remainingSuoritusRoots.partition(sisaltyyOpiskeluoikeuteen(_, oo, suorituksetByAvain))
          val osaSuoritukset = toSuoritukset(Some(oo), opiskeluoikeudenSuoritukset).toSet

          val opiskeluOikeus = VirtaOpiskeluoikeus(
            UUID.randomUUID(),
            oo.avain,
            oo.Tyyppi,
            oo.Jakso.flatMap(_.Koulutuskoodi),
            oo.AlkuPvm,
            oo.LoppuPvm,
            fi.oph.suorituspalvelu.business.Koodi(virtaTila, VIRTA_OO_TILA_KOODISTO, None), // otetaan viimeisin opiskeluoikeuden tila
            convertVirtaTila(virtaTila),
            oo.Myontaja,
            osaSuoritukset
          )
          (muutSuoritukset, opiskeluOikeus :: opiskeluOikeudet)
        }
      }

      val synteettisetOpiskeluoikeudet = orphanSuoritukset.groupBy(_.Myontaja).map { case (myontaja, suoritukset) =>
        VirtaSynteettinenOpiskeluoikeus(
          UUID.randomUUID(),
          myontaja,
          toSuoritukset(None, suoritukset).toSet,
        )
      }

      opiskeluoikeudet ++ synteettisetOpiskeluoikeudet
    })
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
        suoritus.SuoritusPvm,
        suoritus.Myontaja,
        suoritus.Kieli,
        oo.Jakso.flatMap(_.Koulutuskoodi),
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
        suoritus.SuoritusPvm,
        suoritus.HyvaksilukuPvm,
        suoritus.Myontaja,
        suoritus.Organisaatio.map(o => o.Rooli),
        suoritus.Organisaatio.map(o => o.Koodi),
        suoritus.Organisaatio.flatMap(o => o.Osuus),
        suoritus.Arvosana.map(a => a.arvosana),
        suoritus.Arvosana.map(a => a.asteikko),
        suoritus.Kieli,
        suoritus.Koulutusala.map(k => k.Koodi.koodi).get,
        suoritus.Koulutusala.map(k => k.Koodi.versio).get,
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

  def toSuoritukset(opiskeluoikeus: Option[Opiskeluoikeus], opintosuoritukset: Seq[fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus], allowMissingFieldsForTests: Boolean = false): Seq[Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      val suorituksetByAvain = opintosuoritukset.map(suoritus => suoritus.avain -> suoritus).toMap
      opintosuoritukset.flatMap(suoritus => toSuoritus(suoritus, suorituksetByAvain, opiskeluoikeus))
    finally
      allowMissingFields.set(false)
}
