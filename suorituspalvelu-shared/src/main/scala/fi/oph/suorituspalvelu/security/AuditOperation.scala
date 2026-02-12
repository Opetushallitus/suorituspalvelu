package fi.oph.suorituspalvelu.security

import fi.vm.sade.auditlog.Operation

trait AuditOperation(val name: String) extends Operation

object AuditOperation {
  case object Login extends AuditOperation("KIRJAUTUMINEN")

  case object PaivitaVirtaTiedot extends AuditOperation("VIRTATIETOJEN PÄIVITYS")

  case object HaeYoTaiAmmatillinenTutkintoTiedot extends AuditOperation("HAE YO TAI AMMATILLINEN TUTKINNOT")

  case object HaeOppijaTiedot extends AuditOperation("HAE OPPIJATIEDOT")

  case object HaeOppilaitoksetUI extends AuditOperation("HAE OPPILAITOKSET UI")

  case object HaeVuodetUI extends AuditOperation("HAE VUODET UI")

  case object HaeLuokatUI extends AuditOperation("HAE LUOKAT UI")

  case object HaeLuokatLahettava extends AuditOperation("HAE LUOKAT LAHETTAVA")

  case object HaeHenkilotLahettava extends AuditOperation("HAE HENKILOT LAHETTAVA")

  case object HaeLahtokoulut extends AuditOperation("HAE LAHTOKOULUT")

  case object HaeAvainarvot extends AuditOperation("HAE AVAINARVOT")

  case object HaeOppilaitoksenOppijatUI extends AuditOperation("HAE OPPILAITOKSEN OPPIJAT UI")

  case object HaeOppijaTiedotUI extends AuditOperation("HAE OPPIJATIEDOT UI")

  case object HaeOppijanHautUI extends AuditOperation("HAE OPPIJAN HAUT UI")

  case object HaeOppijaValintaDataUI extends AuditOperation("HAE OPPIJA VALINTADATA UI")

  case object HaeOppijaValintaDataAvainMuutoksetUI extends AuditOperation("HAE OPPIJA VALINTADATA AVAIN MUUTOKSET UI")

  case object HaeKoskiTaiYTRMuuttuneet extends AuditOperation("HAE KOSKI TAI YTR MUUTTUNEET HENKILÖT")

  case object PaivitaKoskiTiedotHenkiloille extends AuditOperation("KOSKITIETOJEN PÄIVITYS HENKILÖILLE")

  case object PaivitaKoskiTiedotHaunHakijoille extends AuditOperation("KOSKITIETOJEN PÄIVITYS HAUN HAKIJOILLE")

  case object PaivitaVirtaTiedotHaunHakijoille extends AuditOperation("VIRTATIETOJEN PÄIVITYS HAUN HAKIJOILLE")

  case object PaivitaVirtaTiedotAktiivisille extends AuditOperation("VIRTATIETOJEN PÄIVITYS AKTIIVISILLE HAUILLE")

  case object PaivitaMuuttuneetKoskiTiedot extends AuditOperation("MUUTTUNEIDEN KOSKITIETOJEN PÄIVITYS")

  case object RetryKoskiTiedosto extends AuditOperation("KOSKI TULOSTIEDOSTON PROSESSOINNIN UUDELLEENYRITYS")

  case object PaivitaYtrTiedotHaunHakijoille extends AuditOperation("YLIOPPILASTUTKINTOREKISTERITIETOJEN PÄIVITYS HAUN HAKIJOILLE")

  case object PaivitaYtrTiedotHenkiloille extends AuditOperation("YLIOPPILASTUTKINTOREKISTERITIETOJEN PÄIVITYS HENKILÖILLE")

  case object TallennaPeruskoulunOppimaaranSuoritus extends AuditOperation("TALLENNA PERUSKOULUN OPPIMAARAN SUORITUS UI")

  case object TallennaPerusopetuksenOppimaaranSuoritus extends AuditOperation("TALLENNA PERUSOPETUKSEN OPPIMAARAN SUORITUS UI")

  case object TallennaPerusopetuksenOppiaineenOppimaaranSuoritus extends AuditOperation("TALLENNA PERUSOPETUKSEN OPPIAINEEN OPPIMAARAN SUORITUS UI")

  case object PoistaPerusopetuksenOppimaaranSuoritus extends AuditOperation("POISTA PERUSOPETUKSEN OPPIMAARAN SUORITUS UI")

  case object PoistaPerusopetuksenOppiaineenOppimaaranSuoritus extends AuditOperation("POISTA PERUSOPETUKSEN OPPIAINEEN OPPIMAARAN SUORITUS UI")

  case object TallennaYliajotOppijalle extends AuditOperation("TALLENNA YLIAJOT OPPIJALLE UI")

  case object PoistaOppijanYliajot extends AuditOperation("POISTA YLIAJO OPPIJALTA UI")

  case object HaeHarkinnanvaraisuusYliajot extends AuditOperation("HAE HARKINNANVARAISUUSYLIAJOT UI")

  case object TallennaHarkinnanvaraisuusYliajot extends AuditOperation("TALLENNA HARKINNANVARAISUUSYLIAJOT UI")

  case object PoistaHarkinnanvaraisuusYliajo extends AuditOperation("POISTA HARKINNANVARAISUUSYLIAJO UI")

  case object HaeValintadata extends AuditOperation("VALINTADATAN HAKU HAKUKOHTEELLE TAI HAKEMUKSILLE")

  case object HaeHakemustenHarkinnanvaraisuudet extends AuditOperation("HARKINNANVARAISUUSTIEDON HAKU HAKEMUKSILLE")

  case object Uudelleenparseroi extends AuditOperation("LÄHDEDATAN UUDELLEENPARSEROINTI")

  case object HaeJobData extends AuditOperation("JOBIEN TIETOJEN HAKU")
}
