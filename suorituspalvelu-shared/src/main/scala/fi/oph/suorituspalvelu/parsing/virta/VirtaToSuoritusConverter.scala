package fi.oph.suorituspalvelu.parsing.virta

import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeusTila, VirtaOpintosuoritus, Suoritus, VirtaOpiskeluoikeus, VirtaTutkinto}
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
        )
      })})
    oikeudet
  }

  private def toSuoritus(suoritus: fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus,
    suorituksetByAvain: Map[String, fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus],
    opiskeluoikeus: fi.oph.suorituspalvelu.parsing.virta.Opiskeluoikeus
  ): Option[Suoritus] = {
    suoritus.Laji match
      case 1 => Some(VirtaTutkinto(
        UUID.randomUUID(),
        getDefaultNimi(suoritus.Nimi),
        getNimi(suoritus.Nimi, "sv"),
        getNimi(suoritus.Nimi, "en"),
        suoritus.koulutusmoduulitunniste,
        suoritus.Laajuus.Opintopiste,
        opiskeluoikeus.AlkuPvm,
        suoritus.SuoritusPvm,
        suoritus.Myontaja,
        suoritus.Kieli,
        suoritus.Koulutuskoodi.get,
        suoritus.opiskeluoikeusAvain,
        suoritus.Sisaltyvyys.flatMap(sis => {
           suorituksetByAvain.get(sis.avain).flatMap(s =>
             toSuoritus(s, suorituksetByAvain, opiskeluoikeus).asInstanceOf[Option[VirtaOpintosuoritus]]
           )
        }),
        suoritus.avain
      ))
      case 2 => Some(VirtaOpintosuoritus(
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

  def toSuoritukset(opiskeluoikeus: Opiskeluoikeus, opintosuoritukset: Seq[fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus], allowMissingFieldsForTests: Boolean = false): Seq[Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      val ylatasonSuoritukset = opintosuoritukset.filter(_.opiskeluoikeusAvain.getOrElse("") == opiskeluoikeus.avain)
      val suorituksetByAvain = opintosuoritukset.map(suoritus => suoritus.avain -> suoritus).toMap
      ylatasonSuoritukset.flatMap(suoritus => toSuoritus(suoritus, suorituksetByAvain, opiskeluoikeus))
    finally
      allowMissingFields.set(false)
}
