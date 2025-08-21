package fi.oph.suorituspalvelu.parsing.virta

import fi.oph.suorituspalvelu.business.{Opintosuoritus, Suoritus, VirtaOpiskeluoikeus, VirtaTutkinto}
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty

import java.util.UUID

/**
 * Muuntaa Kosken suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object VirtaToSuoritusConverter {

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

  def toOpiskeluoikeudet(virtaSuoritukset: VirtaSuoritukset): Seq[VirtaOpiskeluoikeus] = {
    val suorituksetByOpiskeluoikeusTunniste = virtaSuoritukset.Body.OpiskelijanKaikkiTiedotResponse.Virta.flatMap(o => o.Opintosuoritukset).flatten.groupBy(_.opiskeluoikeusAvain)

    val oikeudet = virtaSuoritukset.Body.OpiskelijanKaikkiTiedotResponse.Virta.flatMap(o =>
      o.Opiskeluoikeudet.map(oo => {
        val oikeudenSuoritukset: Set[Suoritus] = toSuoritukset(oo, suorituksetByOpiskeluoikeusTunniste.getOrElse(oo.avain, Seq.empty)).toSet
        VirtaOpiskeluoikeus(
          UUID.randomUUID(),
          oo.avain,
          oo.Jakso.Koulutuskoodi,
          oo.AlkuPvm,
          oo.LoppuPvm,
          fi.oph.suorituspalvelu.business.Koodi(oo.Tila.maxBy(t => t.AlkuPvm).Koodi, VIRTA_OO_TILA_KOODISTO, None), // otetaan viimeisin opiskeluoikeuden tila
          oo.Myontaja,
          oikeudenSuoritukset
        )
      }))
    oikeudet
  }

  def toSuoritukset(opiskeluoikeus: Opiskeluoikeus, opintosuoritukset: Seq[fi.oph.suorituspalvelu.parsing.virta.Opintosuoritus], allowMissingFieldsForTests: Boolean = false): Seq[Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      opintosuoritukset.flatMap(suoritus => {
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
            suoritus.opiskeluoikeusAvain
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
            suoritus.Opinnaytetyo.map(o => "1".equals(o)).getOrElse(false),
            suoritus.opiskeluoikeusAvain
          ))
          case default => None
      })
    finally
      allowMissingFields.set(false)
}
