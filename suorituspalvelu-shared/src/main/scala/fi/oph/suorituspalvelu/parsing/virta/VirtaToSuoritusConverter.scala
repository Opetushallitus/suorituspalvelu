package fi.oph.suorituspalvelu.parsing.virta

import fi.oph.suorituspalvelu.business.{Suoritus, Opintosuoritus, VirtaTutkinto}

/**
 * Muuntaa Kosken suoritusmallin suorituspuun SUPAn suoritusrakenteeksi
 */
object VirtaToSuoritusConverter {

  val allowMissingFields = new ThreadLocal[Boolean]

  def dummy[A](): A =
    if(allowMissingFields.get())
      null.asInstanceOf[A]
    else
      throw new RuntimeException("Dummies not allowed")

  def getDefaultNimi(nimet: Seq[Nimi]): Option[String] =
    getNimi(nimet, "fi").orElse(nimet.find(n => n.kieli.isEmpty).map(n => n.nimi))

  def getNimi(nimet: Seq[Nimi], kieli: String): Option[String] =
    nimet.find(n => n.kieli.map(k => kieli.equals(k)).getOrElse(false)).map(n => n.nimi)


  def toSuoritukset(virtaSuoritukset: VirtaSuoritukset, allowMissingFieldsForTests: Boolean = false): Seq[Suoritus] =
    try
      allowMissingFields.set(allowMissingFieldsForTests)
      virtaSuoritukset.Body.OpiskelijanKaikkiTiedotResponse.Virta.map(o => o.Opintosuoritukset.map(o => o.flatMap(suoritus => {
        suoritus.Laji match
          case 1 => Seq(VirtaTutkinto(
            getDefaultNimi(suoritus.Nimi),
            getNimi(suoritus.Nimi, "sv"),
            getNimi(suoritus.Nimi, "en"),
            suoritus.koulutusmoduulitunniste,
            suoritus.Laajuus.Opintopiste,
            suoritus.SuoritusPvm,
            suoritus.Myontaja,
            suoritus.Kieli,
            suoritus.Koulutuskoodi.get
          ))
          case 2 => Seq(Opintosuoritus(
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
            suoritus.Opinnaytetyo.map(o => "1".equals(o)).getOrElse(false)
          ))
          case default => Seq.empty
      })).getOrElse(Seq.empty)).flatten
    finally
      allowMissingFields.set(false)
}
