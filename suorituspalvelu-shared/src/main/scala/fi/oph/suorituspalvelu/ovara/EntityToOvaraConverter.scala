package fi.oph.suorituspalvelu.ovara

import fi.oph.suorituspalvelu.business.{
  AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmatillinenTutkintoOsittainen,
  AmmatillisenTutkinnonOsa, AmmatillisenTutkinnonOsaAlue, AmmattiTutkinto,
  Arvosana, DIAOppiaine, DIAOppiaineenKoesuoritus, DIATutkinto,
  EBArvosana, EBLaajuus, EBOppiaine, EBOppiaineenOsasuoritus, EBTutkinto,
  ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, IBArvosana, IBLaajuus,
  IBOppiaineSuoritus, IBOppiaineRyhma, IBTutkinto, KKOpintosuoritus,
  KKOpiskeluoikeus, KKOpiskeluoikeusTila, KKSynteettinenOpiskeluoikeus,
  KKSynteettinenSuoritus, KKTutkinto, Koe, Koodi, Korotus, Laajuus,
  Lahtokoulu, LahtokouluTyyppi, LukionOppimaara, Opiskeluoikeus, OpiskeluoikeusJakso,
  Oppilaitos, PerusopetukseenValmistavaOpetus, PerusopetuksenOpiskeluoikeus,
  PerusopetuksenOppiaine, PerusopetuksenOppimaara,
  PerusopetuksenOppimaaranOppiaineidenSuoritus, PerusopetuksenYksilollistaminen,
  PoistettuOpiskeluoikeus, Suoritus, SuoritusTila, Telma,
  Tuva, VapaaSivistystyo, YOOpiskeluoikeus, YOTutkinto
}
import fi.oph.suorituspalvelu.parsing.koski.{
  Kielistetty, KoskiErityisenTuenPaatos, KoskiKotiopetusjakso, KoskiKoodi,
  KoskiLisatiedot, KoskiOpiskeluoikeusJakso, KoskiOpiskeluoikeusTila, KoskiUtil
}

object EntityToOvaraConverter {

  // ---- Yhteiset apufunktiot ----

  private def convertKielistetty(k: Kielistetty): OvaraKielistetty =
    OvaraKielistetty(fi = k.fi, sv = k.sv, en = k.en)

  private def convertKoodi(k: Koodi): OvaraKoodi =
    OvaraKoodi(arvo = k.arvo, koodisto = k.koodisto, versio = k.versio)

  private def convertKoskiKoodi(k: KoskiKoodi): OvaraKoskiKoodi =
    OvaraKoskiKoodi(
      koodiarvo = k.koodiarvo,
      koodistoUri = k.koodistoUri,
      koodistoVersio = k.koodistoVersio,
      nimi = convertKielistetty(k.nimi),
      lyhytNimi = k.lyhytNimi.map(convertKielistetty)
    )

  private def convertLaajuus(l: Laajuus): OvaraLaajuus =
    OvaraLaajuus(
      arvo = l.arvo,
      yksikko = convertKoodi(l.yksikko),
      nimi = l.nimi.map(convertKielistetty),
      lyhytNimi = l.lyhytNimi.map(convertKielistetty)
    )

  private def convertArvosana(a: Arvosana): OvaraArvosana =
    OvaraArvosana(koodi = convertKoodi(a.koodi), nimi = convertKielistetty(a.nimi))

  private def convertOppilaitos(o: Oppilaitos): OvaraOppilaitos =
    OvaraOppilaitos(nimi = convertKielistetty(o.nimi), oid = o.oid)

  private def convertSuoritusTila(t: SuoritusTila): OvaraSuoritusTila = t match {
    case SuoritusTila.VALMIS      => OvaraSuoritusTila.VALMIS
    case SuoritusTila.KESKEN      => OvaraSuoritusTila.KESKEN
    case SuoritusTila.KESKEYTYNYT => OvaraSuoritusTila.KESKEYTYNYT
  }

  private def convertKorotus(k: Korotus): OvaraKorotus = k match {
    case Korotus.KOROTETTU        => OvaraKorotus.KOROTETTU
    case Korotus.KOROTUKSENYRITYS => OvaraKorotus.KOROTUKSENYRITYS
  }

  private def convertKKOpiskeluoikeusTila(t: KKOpiskeluoikeusTila): OvaraKKOpiskeluoikeusTila = t match {
    case KKOpiskeluoikeusTila.VOIMASSA  => OvaraKKOpiskeluoikeusTila.VOIMASSA
    case KKOpiskeluoikeusTila.PAATTYNYT => OvaraKKOpiskeluoikeusTila.PAATTYNYT
  }

  private def convertPerusopetuksenYksilollistaminen(y: PerusopetuksenYksilollistaminen): OvaraPerusopetuksenYksilollistaminen = y match {
    case PerusopetuksenYksilollistaminen.EI_YKSILOLLISTETTY                => OvaraPerusopetuksenYksilollistaminen.EI_YKSILOLLISTETTY
    case PerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY          => OvaraPerusopetuksenYksilollistaminen.OSITTAIN_YKSILOLLISTETTY
    case PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY => OvaraPerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_YKSILOLLISTETTY
    case PerusopetuksenYksilollistaminen.TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY => OvaraPerusopetuksenYksilollistaminen.TOIMINTA_ALUEITTAIN_YKSILOLLISTETTY
    case PerusopetuksenYksilollistaminen.OSITTAIN_RAJATTU                  => OvaraPerusopetuksenYksilollistaminen.OSITTAIN_RAJATTU
    case PerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_RAJATTU      => OvaraPerusopetuksenYksilollistaminen.PAAOSIN_TAI_KOKONAAN_RAJATTU
  }

  private def convertLahtokouluTyyppi(t: LahtokouluTyyppi): OvaraLahtokouluTyyppi = t match {
    case LahtokouluTyyppi.VUOSILUOKKA_7                  => OvaraLahtokouluTyyppi.VUOSILUOKKA_7
    case LahtokouluTyyppi.VUOSILUOKKA_8                  => OvaraLahtokouluTyyppi.VUOSILUOKKA_8
    case LahtokouluTyyppi.VUOSILUOKKA_9                  => OvaraLahtokouluTyyppi.VUOSILUOKKA_9
    case LahtokouluTyyppi.AIKUISTEN_PERUSOPETUS          => OvaraLahtokouluTyyppi.AIKUISTEN_PERUSOPETUS
    case LahtokouluTyyppi.PERUSOPETUKSEEN_VALMISTAVA_OPETUS => OvaraLahtokouluTyyppi.PERUSOPETUKSEEN_VALMISTAVA_OPETUS
    case LahtokouluTyyppi.TUVA                           => OvaraLahtokouluTyyppi.TUVA
    case LahtokouluTyyppi.TELMA                          => OvaraLahtokouluTyyppi.TELMA
    case LahtokouluTyyppi.VAPAA_SIVISTYSTYO              => OvaraLahtokouluTyyppi.VAPAA_SIVISTYSTYO
  }

  private def convertOpiskeluoikeusJakso(j: OpiskeluoikeusJakso): OvaraOpiskeluoikeusJakso =
    OvaraOpiskeluoikeusJakso(alku = j.alku, tila = convertSuoritusTila(j.tila))

  private def convertKoskiOpiskeluoikeusJakso(j: KoskiOpiskeluoikeusJakso): OvaraKoskiOpiskeluoikeusJakso =
    OvaraKoskiOpiskeluoikeusJakso(alku = j.alku, tila = convertKoskiKoodi(j.tila))

  private def convertKoskiOpiskeluoikeusTila(t: KoskiOpiskeluoikeusTila): OvaraKoskiOpiskeluoikeusTila =
    OvaraKoskiOpiskeluoikeusTila(opiskeluoikeusjaksot = t.opiskeluoikeusjaksot.map(convertKoskiOpiskeluoikeusJakso))

  private def convertKoskiErityisenTuenPaatos(p: KoskiErityisenTuenPaatos): OvaraKoskiErityisenTuenPaatos =
    OvaraKoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain = p.opiskeleeToimintaAlueittain)

  private def convertKoskiKotiopetusjakso(k: KoskiKotiopetusjakso): OvaraKoskiKotiopetusjakso =
    OvaraKoskiKotiopetusjakso(alku = k.alku, loppu = k.loppu)

  private def convertKoskiLisatiedot(l: KoskiLisatiedot): OvaraKoskiLisatiedot =
    OvaraKoskiLisatiedot(
      erityisenTuenPäätökset = l.erityisenTuenPäätökset.map(_.map(convertKoskiErityisenTuenPaatos)),
      vuosiluokkiinSitoutumatonOpetus = l.vuosiluokkiinSitoutumatonOpetus,
      kotiopetusjaksot = l.kotiopetusjaksot.map(_.map(convertKoskiKotiopetusjakso))
    )

  private def convertLahtokoulu(l: Lahtokoulu): OvaraLahtokoulu =
    OvaraLahtokoulu(
      suorituksenAlku = l.suorituksenAlku,
      suorituksenLoppu = l.suorituksenLoppu,
      oppilaitosOid = l.oppilaitosOid,
      valmistumisvuosi = l.valmistumisvuosi,
      luokka = l.luokka,
      tila = convertSuoritusTila(l.tila),
      arvosanaPuuttuu = l.arvosanaPuuttuu,
      suoritusTyyppi = convertLahtokouluTyyppi(l.suoritusTyyppi)
    )

  // ---- KK ----

  private def convertKKSuoritus(suoritus: Suoritus): Option[OvaraKKSuoritus] = suoritus match {
    case t: KKTutkinto => Some(OvaraKKTutkinto(
      tunniste = t.tunniste,
      nimi = t.nimi.map(convertKielistetty),
      supaTila = convertSuoritusTila(t.supaTila),
      komoTunniste = t.komoTunniste,
      opintoPisteet = t.opintoPisteet,
      aloitusPvm = t.aloitusPvm,
      suoritusPvm = t.suoritusPvm,
      myontaja = t.myontaja,
      kieli = t.kieli,
      koulutusKoodi = t.koulutusKoodi,
      opiskeluoikeusAvain = t.opiskeluoikeusAvain,
      suoritukset = t.suoritukset.flatMap(convertKKSuoritus),
      avain = t.avain
    ))
    case o: KKOpintosuoritus => Some(OvaraKKOpintosuoritus(
      tunniste = o.tunniste,
      nimi = o.nimi.map(convertKielistetty),
      supaTila = convertSuoritusTila(o.supaTila),
      komoTunniste = o.komoTunniste,
      opintoPisteet = o.opintoPisteet,
      opintoviikot = o.opintoviikot,
      suoritusPvm = o.suoritusPvm,
      hyvaksilukuPvm = o.hyvaksilukuPvm,
      myontaja = o.myontaja,
      jarjestavaRooli = o.jarjestavaRooli,
      jarjestavaKoodi = o.jarjestavaKoodi,
      jarjestavaOsuus = o.jarjestavaOsuus,
      arvosana = o.arvosana,
      arvosanaAsteikko = o.arvosanaAsteikko,
      kieli = o.kieli,
      koulutusala = o.koulutusala,
      koulutusalaKoodisto = o.koulutusalaKoodisto,
      opinnaytetyo = o.opinnaytetyo,
      opiskeluoikeusAvain = o.opiskeluoikeusAvain,
      suoritukset = o.suoritukset.flatMap(convertKKSuoritus),
      avain = o.avain
    ))
    case s: KKSynteettinenSuoritus => Some(OvaraKKSynteettinenSuoritus(
      tunniste = s.tunniste,
      nimi = s.nimi.map(convertKielistetty),
      supaTila = convertSuoritusTila(s.supaTila),
      komoTunniste = s.komoTunniste,
      aloitusPvm = s.aloitusPvm,
      suoritusPvm = s.suoritusPvm,
      myontaja = s.myontaja,
      koulutusKoodi = s.koulutusKoodi,
      opiskeluoikeusAvain = s.opiskeluoikeusAvain,
      suoritukset = s.suoritukset.flatMap(convertKKSuoritus)
    ))
    case _ => None
  }

  def getKKOpiskeluoikeudet(opiskeluoikeudet: Seq[(OvaraVersioMetadata, Opiskeluoikeus)]): Seq[OvaraKKOpiskeluoikeus] =
    opiskeluoikeudet.collect { case (meta, oo: KKOpiskeluoikeus) => (meta, oo) }
      .map { case (meta, oo) => OvaraKKOpiskeluoikeus(
        metadata = meta,
        tunniste = oo.tunniste,
        nimi = oo.nimi.map(convertKielistetty),
        virtaTunniste = oo.virtaTunniste,
        tyyppiKoodi = oo.tyyppiKoodi,
        koulutusKoodi = oo.koulutusKoodi,
        alkuPvm = oo.alkuPvm,
        loppuPvm = oo.loppuPvm,
        virtaTila = convertKoodi(oo.virtaTila),
        supaTila = convertKKOpiskeluoikeusTila(oo.supaTila),
        myontaja = oo.myontaja,
        isTutkintoonJohtava = oo.isTutkintoonJohtava,
        kieli = oo.kieli,
        suoritukset = oo.suoritukset.flatMap(convertKKSuoritus).toSeq,
        rahoitusLahde = oo.rahoitusLahde,
        luokittelu = oo.luokittelu,
        liittyvaOpiskeluoikeusAvain = oo.liittyvaOpiskeluoikeusAvain
      )}

  def getKKSynteettisetOpiskeluoikeudet(opiskeluoikeudet: Seq[(OvaraVersioMetadata, Opiskeluoikeus)]): Seq[OvaraKKSynteettinenOpiskeluoikeus] =
    opiskeluoikeudet.collect { case (meta, oo: KKSynteettinenOpiskeluoikeus) => (meta, oo) }
      .map { case (meta, oo) => OvaraKKSynteettinenOpiskeluoikeus(
        metadata = meta,
        tunniste = oo.tunniste,
        myontaja = oo.myontaja,
        containsKKTutkinto = oo.containsKKTutkinto,
        suoritukset = oo.suoritukset.flatMap(convertKKSuoritus).toSeq
      )}

  // ---- YO ----

  private def convertKoe(k: Koe): OvaraKoe =
    OvaraKoe(
      tunniste = k.tunniste,
      koodi = convertKoodi(k.koodi),
      tutkintoKerta = k.tutkintoKerta,
      arvosana = convertKoodi(k.arvosana),
      pisteet = k.pisteet
    )

  private def convertYOTutkinto(t: YOTutkinto): OvaraYOTutkinto =
    OvaraYOTutkinto(
      tunniste = t.tunniste,
      suoritusKieli = convertKoodi(t.suoritusKieli),
      supaTila = convertSuoritusTila(t.supaTila),
      valmistumisPaiva = t.valmistumisPaiva,
      aineet = t.aineet.map(convertKoe)
    )

  def getYOOpiskeluoikeudet(opiskeluoikeudet: Seq[(OvaraVersioMetadata, Opiskeluoikeus)]): Seq[OvaraYOOpiskeluoikeus] =
    opiskeluoikeudet.collect { case (meta, oo: YOOpiskeluoikeus) => (meta, oo) }
      .map { case (meta, oo) => OvaraYOOpiskeluoikeus(
        metadata = meta,
        tunniste = oo.tunniste,
        yoTutkinto = oo.yoTutkinto.map(convertYOTutkinto)
      )}

  // ---- Geneerinen ----

  private def convertDIAOppiaine(o: DIAOppiaine): OvaraDIAOppiaine =
    OvaraDIAOppiaine(
      tunniste = o.tunniste,
      nimi = convertKielistetty(o.nimi),
      koodi = convertKoodi(o.koodi),
      laajuus = o.laajuus.map(l => OvaraDIALaajuus(arvo = l.arvo, yksikko = convertKoodi(l.yksikko))),
      osaAlue = o.osaAlue.map(convertKoodi),
      kieli = o.kieli.map(convertKoodi),
      vastaavuustodistuksenTiedot = o.vastaavuustodistuksenTiedot.map(v =>
        OvaraDIAVastaavuustodistuksenTiedot(
          keskiarvo = v.keskiarvo,
          lukioOpintojenLaajuus = OvaraDIALaajuus(arvo = v.lukioOpintojenLaajuus.arvo, yksikko = convertKoodi(v.lukioOpintojenLaajuus.yksikko))
        )
      ),
      kirjallinenKoe = o.kirjallinenKoe.map(convertDIAKoesuoritus),
      suullinenKoe = o.suullinenKoe.map(convertDIAKoesuoritus)
    )

  private def convertDIAKoesuoritus(k: DIAOppiaineenKoesuoritus): OvaraDIAOppiaineenKoesuoritus =
    OvaraDIAOppiaineenKoesuoritus(
      nimi = convertKielistetty(k.nimi),
      koodi = convertKoodi(k.koodi),
      arvosana = OvaraDIAArvosana(arvosana = convertKoodi(k.arvosana.arvosana), hyvaksytty = k.arvosana.hyvaksytty),
      laajuus = k.laajuus.map(l => OvaraDIALaajuus(arvo = l.arvo, yksikko = convertKoodi(l.yksikko)))
    )

  private def convertEBOppiaine(o: EBOppiaine): OvaraEBOppiaine =
    OvaraEBOppiaine(
      tunniste = o.tunniste,
      nimi = convertKielistetty(o.nimi),
      koodi = convertKoodi(o.koodi),
      laajuus = o.laajuus.map(l => OvaraEBLaajuus(arvo = l.arvo, yksikko = convertKoodi(l.yksikko))),
      suorituskieli = o.suorituskieli.map(convertKoodi),
      osasuoritukset = o.osasuoritukset.map(convertEBOsasuoritus)
    )

  private def convertEBOsasuoritus(os: EBOppiaineenOsasuoritus): OvaraEBOppiaineenOsasuoritus =
    OvaraEBOppiaineenOsasuoritus(
      nimi = convertKielistetty(os.nimi),
      koodi = convertKoodi(os.koodi),
      arvosana = OvaraEBArvosana(arvosana = convertKoodi(os.arvosana.arvosana), hyvaksytty = os.arvosana.hyvaksytty),
      laajuus = os.laajuus.map(l => OvaraEBLaajuus(arvo = l.arvo, yksikko = convertKoodi(l.yksikko)))
    )

  private def convertIBOppiaineSuoritus(o: IBOppiaineSuoritus): OvaraIBOppiaineSuoritus =
    OvaraIBOppiaineSuoritus(
      tunniste = o.tunniste,
      nimi = convertKielistetty(o.nimi),
      koodi = convertKoodi(o.koodi),
      ryhma = o.ryhma.map(r => OvaraIBOppiaineRyhma(nimi = convertKielistetty(r.nimi), koodi = convertKoodi(r.koodi))),
      predictedArvosana = o.predictedArvosana.map(a => OvaraIBArvosana(arvosana = convertKoodi(a.arvosana), hyvaksytty = a.hyvaksytty)),
      laajuus = o.laajuus.map(l => OvaraIBLaajuus(arvo = l.arvo, yksikko = convertKoodi(l.yksikko))),
      suorituskieli = o.suorituskieli.map(convertKoodi)
    )

  private def convertGeneerinenSuoritus(s: Suoritus): Option[OvaraGeneerinenSuoritus] = s match {
    case lop: LukionOppimaara => Some(OvaraLukionOppimaara(
      tunniste = lop.tunniste,
      oppilaitos = convertOppilaitos(lop.oppilaitos),
      koskiTila = convertKoodi(lop.koskiTila),
      supaTila = convertSuoritusTila(lop.supaTila),
      aloitusPaivamaara = lop.aloitusPaivamaara,
      vahvistusPaivamaara = lop.vahvistusPaivamaara,
      suoritusKieli = lop.suoritusKieli.map(convertKoodi),
      koulusivistyskieli = lop.koulusivistyskieli.map(convertKoodi)
    ))
    case dia: DIATutkinto => Some(OvaraDIATutkinto(
      tunniste = dia.tunniste,
      nimi = convertKielistetty(dia.nimi),
      koodi = convertKoodi(dia.koodi),
      oppilaitos = convertOppilaitos(dia.oppilaitos),
      suorituskieli = convertKoodi(dia.suorituskieli),
      koskiTila = convertKoodi(dia.koskiTila),
      supaTila = convertSuoritusTila(dia.supaTila),
      aloitusPaivamaara = dia.aloitusPaivamaara,
      vahvistusPaivamaara = dia.vahvistusPaivamaara,
      osasuoritukset = dia.osasuoritukset.map(convertDIAOppiaine)
    ))
    case eb: EBTutkinto => Some(OvaraEBTutkinto(
      tunniste = eb.tunniste,
      nimi = convertKielistetty(eb.nimi),
      koodi = convertKoodi(eb.koodi),
      oppilaitos = convertOppilaitos(eb.oppilaitos),
      koskiTila = convertKoodi(eb.koskiTila),
      supaTila = convertSuoritusTila(eb.supaTila),
      aloitusPaivamaara = eb.aloitusPaivamaara,
      vahvistusPaivamaara = eb.vahvistusPaivamaara,
      osasuoritukset = eb.osasuoritukset.map(convertEBOppiaine)
    ))
    case ib: IBTutkinto => Some(OvaraIBTutkinto(
      tunniste = ib.tunniste,
      nimi = convertKielistetty(ib.nimi),
      koodi = convertKoodi(ib.koodi),
      oppilaitos = convertOppilaitos(ib.oppilaitos),
      koskiTila = convertKoodi(ib.koskiTila),
      supaTila = convertSuoritusTila(ib.supaTila),
      aloitusPaivamaara = ib.aloitusPaivamaara,
      vahvistusPaivamaara = ib.vahvistusPaivamaara,
      suorituskieli = ib.suorituskieli.map(convertKoodi),
      osasuoritukset = ib.osasuoritukset.map(convertIBOppiaineSuoritus)
    ))
    case tuva: Tuva => Some(OvaraTuva(
      tunniste = tuva.tunniste,
      nimi = convertKielistetty(tuva.nimi),
      koodi = convertKoodi(tuva.koodi),
      oppilaitos = convertOppilaitos(tuva.oppilaitos),
      koskiTila = convertKoodi(tuva.koskiTila),
      supaTila = convertSuoritusTila(tuva.supaTila),
      aloitusPaivamaara = tuva.aloitusPaivamaara,
      vahvistusPaivamaara = tuva.vahvistusPaivamaara,
      suoritusVuosi = tuva.suoritusVuosi,
      hyvaksyttyLaajuus = tuva.hyvaksyttyLaajuus.map(convertLaajuus),
      lahtokoulut = tuva.lahtokoulut.map(convertLahtokoulu)
    ))
    case vst: VapaaSivistystyo => Some(OvaraVapaaSivistystyo(
      tunniste = vst.tunniste,
      nimi = convertKielistetty(vst.nimi),
      koodi = convertKoodi(vst.koodi),
      oppilaitos = convertOppilaitos(vst.oppilaitos),
      koskiTila = convertKoodi(vst.koskiTila),
      supaTila = convertSuoritusTila(vst.supaTila),
      aloitusPaivamaara = vst.aloitusPaivamaara,
      vahvistusPaivamaara = vst.vahvistusPaivamaara,
      suoritusVuosi = vst.suoritusVuosi,
      hyvaksyttyLaajuus = vst.hyvaksyttyLaajuus.map(convertLaajuus),
      suoritusKieli = convertKoodi(vst.suoritusKieli),
      lahtokoulut = vst.lahtokoulut.map(convertLahtokoulu)
    ))
    case _ => None
  }

  def getGeneerisetOpiskeluoikeudet(opiskeluoikeudet: Seq[(OvaraVersioMetadata, Opiskeluoikeus)]): Seq[OvaraGeneerinenOpiskeluoikeus] =
    opiskeluoikeudet.collect { case (meta, oo: GeneerinenOpiskeluoikeus) => (meta, oo) }
      .map { case (meta, oo) => OvaraGeneerinenOpiskeluoikeus(
        metadata = meta,
        tunniste = oo.tunniste,
        oid = oo.oid,
        tyyppi = convertKoodi(oo.tyyppi),
        oppilaitosOid = oo.oppilaitosOid,
        suoritukset = oo.suoritukset.flatMap(convertGeneerinenSuoritus).toSeq,
        tila = oo.tila.map(convertKoskiOpiskeluoikeusTila),
        jaksot = oo.jaksot.map(convertOpiskeluoikeusJakso)
      )}

  // ---- Ammatillinen ----

  private def convertAmmatillisenTutkinnonOsaAlue(oa: AmmatillisenTutkinnonOsaAlue): OvaraAmmatillisenTutkinnonOsaAlue =
    OvaraAmmatillisenTutkinnonOsaAlue(
      tunniste = oa.tunniste,
      nimi = convertKielistetty(oa.nimi),
      koodi = convertKoodi(oa.koodi),
      arvosana = oa.arvosana.map(convertKoodi),
      laajuus = oa.laajuus.map(convertLaajuus),
      korotettu = oa.korotettu.map(convertKorotus)
    )

  private def convertAmmatillisenTutkinnonOsa(o: AmmatillisenTutkinnonOsa): OvaraAmmatillisenTutkinnonOsa =
    OvaraAmmatillisenTutkinnonOsa(
      tunniste = o.tunniste,
      nimi = convertKielistetty(o.nimi),
      koodi = convertKoodi(o.koodi),
      yto = o.yto,
      arviointiPaiva = o.arviointiPaiva,
      arvosana = o.arvosana.map(convertArvosana),
      laajuus = o.laajuus.map(convertLaajuus),
      osaAlueet = o.osaAlueet.map(convertAmmatillisenTutkinnonOsaAlue),
      korotettu = o.korotettu.map(convertKorotus)
    )

  private def convertAmmatillinenSuoritus(s: Suoritus): Option[OvaraAmmatillinenSuoritus] = s match {
    case pt: AmmatillinenPerustutkinto => Some(OvaraAmmatillinenPerustutkinto(
      tunniste = pt.tunniste,
      nimi = convertKielistetty(pt.nimi),
      koodi = convertKoodi(pt.koodi),
      oppilaitos = convertOppilaitos(pt.oppilaitos),
      koskiTila = convertKoodi(pt.koskiTila),
      supaTila = convertSuoritusTila(pt.supaTila),
      aloitusPaivamaara = pt.aloitusPaivamaara,
      vahvistusPaivamaara = pt.vahvistusPaivamaara,
      keskiarvo = pt.keskiarvo,
      suoritustapa = convertKoodi(pt.suoritustapa),
      suoritusKieli = convertKoodi(pt.suoritusKieli),
      osat = pt.osat.map(convertAmmatillisenTutkinnonOsa)
    ))
    case to: AmmatillinenTutkintoOsittainen => Some(OvaraAmmatillinenTutkintoOsittainen(
      tunniste = to.tunniste,
      nimi = convertKielistetty(to.nimi),
      koodi = convertKoodi(to.koodi),
      oppilaitos = convertOppilaitos(to.oppilaitos),
      koskiTila = convertKoodi(to.koskiTila),
      supaTila = convertSuoritusTila(to.supaTila),
      aloitusPaivamaara = to.aloitusPaivamaara,
      vahvistusPaivamaara = to.vahvistusPaivamaara,
      korotettuKeskiarvo = to.korotettuKeskiarvo,
      korotettuOpiskeluoikeusOid = to.korotettuOpiskeluoikeusOid,
      suoritustapa = convertKoodi(to.suoritustapa),
      suoritusKieli = convertKoodi(to.suoritusKieli),
      osat = to.osat.map(convertAmmatillisenTutkinnonOsa)
    ))
    case at: AmmattiTutkinto => Some(OvaraAmmattiTutkinto(
      tunniste = at.tunniste,
      nimi = convertKielistetty(at.nimi),
      koodi = convertKoodi(at.koodi),
      oppilaitos = convertOppilaitos(at.oppilaitos),
      koskiTila = convertKoodi(at.koskiTila),
      supaTila = convertSuoritusTila(at.supaTila),
      aloitusPaivamaara = at.aloitusPaivamaara,
      vahvistusPaivamaara = at.vahvistusPaivamaara,
      suoritustapa = convertKoodi(at.suoritustapa),
      suoritusKieli = convertKoodi(at.suoritusKieli)
    ))
    case eat: ErikoisAmmattiTutkinto => Some(OvaraErikoisAmmattiTutkinto(
      tunniste = eat.tunniste,
      nimi = convertKielistetty(eat.nimi),
      koodi = convertKoodi(eat.koodi),
      oppilaitos = convertOppilaitos(eat.oppilaitos),
      koskiTila = convertKoodi(eat.koskiTila),
      supaTila = convertSuoritusTila(eat.supaTila),
      aloitusPaivamaara = eat.aloitusPaivamaara,
      vahvistusPaivamaara = eat.vahvistusPaivamaara,
      suoritusKieli = convertKoodi(eat.suoritusKieli)
    ))
    case telma: Telma => Some(OvaraTelma(
      tunniste = telma.tunniste,
      nimi = convertKielistetty(telma.nimi),
      koodi = convertKoodi(telma.koodi),
      oppilaitos = convertOppilaitos(telma.oppilaitos),
      koskiTila = convertKoodi(telma.koskiTila),
      supaTila = convertSuoritusTila(telma.supaTila),
      aloitusPaivamaara = telma.aloitusPaivamaara,
      vahvistusPaivamaara = telma.vahvistusPaivamaara,
      suoritusVuosi = telma.suoritusVuosi,
      suoritusKieli = convertKoodi(telma.suoritusKieli),
      hyvaksyttyLaajuus = telma.hyvaksyttyLaajuus.map(convertLaajuus),
      lahtokoulut = telma.lahtokoulut.map(convertLahtokoulu)
    ))
    case _ => None
  }

  def getAmmatillisetOpiskeluoikeudet(opiskeluoikeudet: Seq[(OvaraVersioMetadata, Opiskeluoikeus)]): Seq[OvaraAmmatillinenOpiskeluoikeus] =
    opiskeluoikeudet.collect { case (meta, oo: AmmatillinenOpiskeluoikeus) => (meta, oo) }
      .map { case (meta, oo) => OvaraAmmatillinenOpiskeluoikeus(
        metadata = meta,
        tunniste = oo.tunniste,
        oid = oo.oid,
        oppilaitos = convertOppilaitos(oo.oppilaitos),
        suoritukset = oo.suoritukset.flatMap(convertAmmatillinenSuoritus).toSeq,
        tila = oo.tila.map(convertKoskiOpiskeluoikeusTila),
        jaksot = oo.jaksot.map(convertOpiskeluoikeusJakso)
      )}

  // ---- Perusopetus ----

  private def convertPerusopetuksenOppiaine(a: PerusopetuksenOppiaine): OvaraPerusopetuksenOppiaine =
    OvaraPerusopetuksenOppiaine(
      tunniste = a.tunniste,
      nimi = convertKielistetty(a.nimi),
      koodi = convertKoodi(a.koodi),
      arvosana = convertKoodi(a.arvosana),
      kieli = a.kieli.map(convertKoodi),
      pakollinen = a.pakollinen,
      yksilollistetty = a.yksilollistetty,
      rajattu = a.rajattu
    )

  private def convertPerusopetuksenSuoritus(s: Suoritus): Option[OvaraPerusopetuksenSuoritus] = s match {
    case om: PerusopetuksenOppimaara => Some(OvaraPerusopetuksenOppimaara(
      tunniste = om.tunniste,
      versioTunniste = om.versioTunniste,
      oppilaitos = convertOppilaitos(om.oppilaitos),
      luokka = om.luokka,
      koskiTila = convertKoodi(om.koskiTila),
      supaTila = convertSuoritusTila(om.supaTila),
      suoritusKieli = convertKoodi(om.suoritusKieli),
      koulusivistyskieli = om.koulusivistyskieli.map(convertKoodi),
      yksilollistaminen = om.yksilollistaminen.map(convertPerusopetuksenYksilollistaminen),
      aloitusPaivamaara = om.aloitusPaivamaara,
      vahvistusPaivamaara = om.vahvistusPaivamaara,
      aineet = om.aineet.map(convertPerusopetuksenOppiaine),
      lahtokoulut = om.lahtokoulut.map(convertLahtokoulu),
      syotetty = om.syotetty,
      vuosiluokkiinSitoutumatonOpetus = om.vuosiluokkiinSitoutumatonOpetus,
      luokkaAste = om.luokkaAste
      //Todo, add jaaluokalle
    ))
    case oos: PerusopetuksenOppimaaranOppiaineidenSuoritus => Some(OvaraPerusopetuksenOppimaaranOppiaineidenSuoritus(
      tunniste = oos.tunniste,
      versioTunniste = oos.versioTunniste,
      oppilaitos = convertOppilaitos(oos.oppilaitos),
      koskiTila = convertKoodi(oos.koskiTila),
      supaTila = convertSuoritusTila(oos.supaTila),
      suoritusKieli = convertKoodi(oos.suoritusKieli),
      aloitusPaivamaara = oos.aloitusPaivamaara,
      vahvistusPaivamaara = oos.vahvistusPaivamaara,
      aineet = oos.aineet.map(convertPerusopetuksenOppiaine),
      syotetty = oos.syotetty
    ))
    case pvo: PerusopetukseenValmistavaOpetus => Some(OvaraPerusopetukseenValmistavaOpetus(
      lahtokoulut = pvo.lahtokoulut.map(convertLahtokoulu)
    ))
    case _ => None
  }

  def getPerusopetuksenOpiskeluoikeudet(opiskeluoikeudet: Seq[(OvaraVersioMetadata, Opiskeluoikeus)]): Seq[OvaraPerusopetuksenOpiskeluoikeus] =
    opiskeluoikeudet.collect { case (meta, oo: PerusopetuksenOpiskeluoikeus) => (meta, oo) }
      .map { case (meta, oo) => OvaraPerusopetuksenOpiskeluoikeus(
        metadata = meta,
        tunniste = oo.tunniste,
        oid = oo.oid,
        oppilaitosOid = oo.oppilaitosOid,
        suoritukset = oo.suoritukset.flatMap(convertPerusopetuksenSuoritus).toSeq,
        lisatiedot = oo.lisatiedot.map(convertKoskiLisatiedot),
        tila = convertSuoritusTila(oo.tila),
        jaksot = oo.jaksot.map(convertOpiskeluoikeusJakso)
      )}

  // ---- Poistettu ----

  def getPoistetutOpiskeluoikeudet(opiskeluoikeudet: Seq[(OvaraVersioMetadata, Opiskeluoikeus)]): Seq[OvaraPoistettuOpiskeluoikeus] =
    opiskeluoikeudet.collect { case (meta, oo: PoistettuOpiskeluoikeus) => (meta, oo) }
      .map { case (meta, oo) => OvaraPoistettuOpiskeluoikeus(metadata = meta, oid = oo.oid) }

  // Kokoaa henkilön kaikki lähtökoulut käyttäen samaa logiikkaa kuin Koski-pään tallennus (KoskiUtil.getLahtokouluMetadata).
  def getLahtokoulut(opiskeluoikeudet: Set[Opiskeluoikeus]): Seq[OvaraLahtokoulu] =
    KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet).map(convertLahtokoulu)
}
