package fi.oph.suorituspalvelu.parsing.virta

import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeusTila, Opintosuoritus, Suoritus, VirtaOpiskeluoikeus, VirtaTutkinto}
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

  private def withOpiskeluoikeusAvaimet(suoritukset: Seq[fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus]): Seq[fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus] = {
    val suoritusParentsByAvain: Map[String, fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus] =
      suoritukset.flatMap(suoritus => suoritus.Sisaltyvyys.map(_.avain -> suoritus)).toMap

    def getBranchOpiskeluoikeusAvain(suoritus: Option[fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus]): Option[String] = {
        suoritus match {
          case Some(s) =>
            if (s.opiskeluoikeusAvain == null || s.opiskeluoikeusAvain.isEmpty)
              getBranchOpiskeluoikeusAvain(suoritusParentsByAvain.get(s.avain))
            else
              s.opiskeluoikeusAvain
          case _ => None
        }
    }

    suoritukset.map(suoritus => {
      val parentOpiskeluoikeusAvain = getBranchOpiskeluoikeusAvain(Some(suoritus))
      if (parentOpiskeluoikeusAvain.isEmpty) {
       // Suoritukselle löytynyt opiskeluoikeus-avainta, eli suoritus ei kuulu mihinkään opiskeluoikeuteen
       LOG.debug("Ei löydetty opiskeluoikeutta VIRTA-suoritukselle avaimella " + suoritus.avain)
       suoritus
      } else {
        suoritus.copy(opiskeluoikeusAvain = parentOpiskeluoikeusAvain)
      }
    })

  }

  def toOpiskeluoikeudet(virtaSuoritukset: VirtaSuoritukset): Seq[VirtaOpiskeluoikeus] = {
    val oikeudet = virtaSuoritukset.Body.OpiskelijanKaikkiTiedotResponse.Virta.flatMap(o => {
      val opintosuoritukset = withOpiskeluoikeusAvaimet(o.Opintosuoritukset.getOrElse(Seq.empty))
      o.Opiskeluoikeudet.map(oo => {
        val oikeudenSuoritukset: Set[Suoritus] = toSuoritukset(oo, opintosuoritukset.filter(_.opiskeluoikeusAvain.getOrElse("") == oo.avain)).toSet
        val virtaTila = oo.Tila.maxBy(t => t.AlkuPvm).Koodi
        VirtaOpiskeluoikeus(
          UUID.randomUUID(),
          oo.avain,
          oo.Jakso.Koulutuskoodi,
          oo.AlkuPvm,
          oo.LoppuPvm,
          fi.oph.suorituspalvelu.business.Koodi(virtaTila, VIRTA_OO_TILA_KOODISTO, None), // otetaan viimeisin opiskeluoikeuden tila
          convertVirtaTila(virtaTila),
          oo.Myontaja,
          oikeudenSuoritukset
        )
      })})
    oikeudet
  }

  def toSuoritukset(opiskeluoikeus: Opiskeluoikeus, opintosuoritukset: Seq[fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus], allowMissingFieldsForTests: Boolean = false): Seq[Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      opintosuoritukset.flatMap(suoritus => {
        val osaSuoritukset = suoritus.Sisaltyvyys.map(_.avain)
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
            suoritus.opiskeluoikeusAvain.get,
            osaSuoritukset
          ))
          case 2 => Some(Opintosuoritus(
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
            suoritus.opiskeluoikeusAvain.get,
            osaSuoritukset
          ))
          case default => None
      })
    finally
      allowMissingFields.set(false)
}
