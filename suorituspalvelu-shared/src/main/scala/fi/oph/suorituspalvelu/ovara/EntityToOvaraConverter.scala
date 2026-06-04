package fi.oph.suorituspalvelu.ovara

import fi.oph.suorituspalvelu.business.{
  AmmatillinenOpiskeluoikeus, AmmatillinenPerustutkinto, AmmatillinenTutkintoOsittainen,
  AmmattiTutkinto, DIAOppiaine, DIAOppiaineenKoesuoritus, DIATutkinto,
  EBArvosana, EBLaajuus, EBOppiaine, EBOppiaineenOsasuoritus, EBTutkinto,
  ErikoisAmmattiTutkinto, GeneerinenOpiskeluoikeus, IBArvosana, IBLaajuus,
  IBOppiaineSuoritus, IBOppiaineRyhma, IBTutkinto, KKOpintosuoritus,
  KKOpiskeluoikeus, KKSynteettinenOpiskeluoikeus, KKSynteettinenSuoritus,
  KKTutkinto, Koe, LukionOppimaara, Opiskeluoikeus, PerusopetukseenValmistavaOpetus,
  PerusopetuksenOpiskeluoikeus, PerusopetuksenOppiaine, PerusopetuksenOppimaara,
  PerusopetuksenOppimaaranOppiaineidenSuoritus, PoistettuOpiskeluoikeus, Suoritus, Telma,
  Tuva, VapaaSivistystyo, YOOpiskeluoikeus, YOTutkinto
}
object EntityToOvaraConverter {

  // ---- KK ----

  private def convertKKSuoritus(suoritus: Suoritus): Option[OvaraKKSuoritus] = suoritus match {
    case t: KKTutkinto => Some(OvaraKKTutkinto(
      tunniste = t.tunniste,
      nimi = t.nimi,
      supaTila = t.supaTila,
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
      nimi = o.nimi,
      supaTila = o.supaTila,
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
      nimi = s.nimi,
      supaTila = s.supaTila,
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
        virtaTunniste = oo.virtaTunniste,
        tyyppiKoodi = oo.tyyppiKoodi,
        koulutusKoodi = oo.koulutusKoodi,
        alkuPvm = oo.alkuPvm,
        loppuPvm = oo.loppuPvm,
        virtaTila = oo.virtaTila,
        supaTila = oo.supaTila,
        myontaja = oo.myontaja,
        isTutkintoonJohtava = oo.isTutkintoonJohtava,
        kieli = oo.kieli,
        suoritukset = oo.suoritukset.flatMap(convertKKSuoritus).toSeq
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
    OvaraKoe(tunniste = k.tunniste, koodi = k.koodi, tutkintoKerta = k.tutkintoKerta, arvosana = k.arvosana, pisteet = k.pisteet)

  private def convertYOTutkinto(t: YOTutkinto): OvaraYOTutkinto =
    OvaraYOTutkinto(
      tunniste = t.tunniste,
      suoritusKieli = t.suoritusKieli,
      supaTila = t.supaTila,
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
      nimi = o.nimi,
      koodi = o.koodi,
      laajuus = o.laajuus.map(l => OvaraDIALaajuus(arvo = l.arvo, yksikko = l.yksikko)),
      osaAlue = o.osaAlue,
      kieli = o.kieli,
      vastaavuustodistuksenTiedot = o.vastaavuustodistuksenTiedot.map(v =>
        OvaraDIAVastaavuustodistuksenTiedot(
          keskiarvo = v.keskiarvo,
          lukioOpintojenLaajuus = OvaraDIALaajuus(arvo = v.lukioOpintojenLaajuus.arvo, yksikko = v.lukioOpintojenLaajuus.yksikko)
        )
      ),
      kirjallinenKoe = o.kirjallinenKoe.map(convertDIAKoesuoritus),
      suullinenKoe = o.suullinenKoe.map(convertDIAKoesuoritus)
    )

  private def convertDIAKoesuoritus(k: DIAOppiaineenKoesuoritus): OvaraDIAOppiaineenKoesuoritus =
    OvaraDIAOppiaineenKoesuoritus(
      nimi = k.nimi,
      koodi = k.koodi,
      arvosana = OvaraDIAArvosana(arvosana = k.arvosana.arvosana, hyvaksytty = k.arvosana.hyvaksytty),
      laajuus = k.laajuus.map(l => OvaraDIALaajuus(arvo = l.arvo, yksikko = l.yksikko))
    )

  private def convertEBOppiaine(o: EBOppiaine): OvaraEBOppiaine =
    OvaraEBOppiaine(
      tunniste = o.tunniste,
      nimi = o.nimi,
      koodi = o.koodi,
      laajuus = o.laajuus.map(l => OvaraEBLaajuus(arvo = l.arvo, yksikko = l.yksikko)),
      suorituskieli = o.suorituskieli,
      osasuoritukset = o.osasuoritukset.map(convertEBOsasuoritus)
    )

  private def convertEBOsasuoritus(os: EBOppiaineenOsasuoritus): OvaraEBOppiaineenOsasuoritus =
    OvaraEBOppiaineenOsasuoritus(
      nimi = os.nimi,
      koodi = os.koodi,
      arvosana = OvaraEBArvosana(arvosana = os.arvosana.arvosana, hyvaksytty = os.arvosana.hyvaksytty),
      laajuus = os.laajuus.map(l => OvaraEBLaajuus(arvo = l.arvo, yksikko = l.yksikko))
    )

  private def convertIBOppiaineSuoritus(o: IBOppiaineSuoritus): OvaraIBOppiaineSuoritus =
    OvaraIBOppiaineSuoritus(
      tunniste = o.tunniste,
      nimi = o.nimi,
      koodi = o.koodi,
      ryhma = o.ryhma.map(r => OvaraIBOppiaineRyhma(nimi = r.nimi, koodi = r.koodi)),
      predictedArvosana = o.predictedArvosana.map(a => OvaraIBArvosana(arvosana = a.arvosana, hyvaksytty = a.hyvaksytty)),
      laajuus = o.laajuus.map(l => OvaraIBLaajuus(arvo = l.arvo, yksikko = l.yksikko)),
      suorituskieli = o.suorituskieli
    )

  private def convertGeneerinenSuoritus(s: Suoritus): Option[OvaraGeneerinenSuoritus] = s match {
    case lop: LukionOppimaara => Some(OvaraLukionOppimaara(
      tunniste = lop.tunniste,
      oppilaitos = lop.oppilaitos,
      koskiTila = lop.koskiTila,
      supaTila = lop.supaTila,
      aloitusPaivamaara = lop.aloitusPaivamaara,
      vahvistusPaivamaara = lop.vahvistusPaivamaara,
      suoritusKieli = lop.suoritusKieli,
      koulusivistyskieli = lop.koulusivistyskieli
    ))
    case dia: DIATutkinto => Some(OvaraDIATutkinto(
      tunniste = dia.tunniste,
      nimi = dia.nimi,
      koodi = dia.koodi,
      oppilaitos = dia.oppilaitos,
      suorituskieli = dia.suorituskieli,
      koskiTila = dia.koskiTila,
      supaTila = dia.supaTila,
      aloitusPaivamaara = dia.aloitusPaivamaara,
      vahvistusPaivamaara = dia.vahvistusPaivamaara,
      osasuoritukset = dia.osasuoritukset.map(convertDIAOppiaine)
    ))
    case eb: EBTutkinto => Some(OvaraEBTutkinto(
      tunniste = eb.tunniste,
      nimi = eb.nimi,
      koodi = eb.koodi,
      oppilaitos = eb.oppilaitos,
      koskiTila = eb.koskiTila,
      supaTila = eb.supaTila,
      aloitusPaivamaara = eb.aloitusPaivamaara,
      vahvistusPaivamaara = eb.vahvistusPaivamaara,
      osasuoritukset = eb.osasuoritukset.map(convertEBOppiaine)
    ))
    case ib: IBTutkinto => Some(OvaraIBTutkinto(
      tunniste = ib.tunniste,
      nimi = ib.nimi,
      koodi = ib.koodi,
      oppilaitos = ib.oppilaitos,
      koskiTila = ib.koskiTila,
      supaTila = ib.supaTila,
      aloitusPaivamaara = ib.aloitusPaivamaara,
      vahvistusPaivamaara = ib.vahvistusPaivamaara,
      suorituskieli = ib.suorituskieli,
      osasuoritukset = ib.osasuoritukset.map(convertIBOppiaineSuoritus)
    ))
    case tuva: Tuva => Some(OvaraTuva(
      tunniste = tuva.tunniste,
      nimi = tuva.nimi,
      koodi = tuva.koodi,
      oppilaitos = tuva.oppilaitos,
      koskiTila = tuva.koskiTila,
      supaTila = tuva.supaTila,
      aloitusPaivamaara = tuva.aloitusPaivamaara,
      vahvistusPaivamaara = tuva.vahvistusPaivamaara,
      suoritusVuosi = tuva.suoritusVuosi,
      hyvaksyttyLaajuus = tuva.hyvaksyttyLaajuus,
      lahtokoulut = tuva.lahtokoulut
    ))
    case vst: VapaaSivistystyo => Some(OvaraVapaaSivistystyo(
      tunniste = vst.tunniste,
      nimi = vst.nimi,
      koodi = vst.koodi,
      oppilaitos = vst.oppilaitos,
      koskiTila = vst.koskiTila,
      supaTila = vst.supaTila,
      aloitusPaivamaara = vst.aloitusPaivamaara,
      vahvistusPaivamaara = vst.vahvistusPaivamaara,
      suoritusVuosi = vst.suoritusVuosi,
      hyvaksyttyLaajuus = vst.hyvaksyttyLaajuus,
      suoritusKieli = vst.suoritusKieli,
      lahtokoulut = vst.lahtokoulut
    ))
    case _ => None
  }

  def getGeneerisetOpiskeluoikeudet(opiskeluoikeudet: Seq[(OvaraVersioMetadata, Opiskeluoikeus)]): Seq[OvaraGeneerinenOpiskeluoikeus] =
    opiskeluoikeudet.collect { case (meta, oo: GeneerinenOpiskeluoikeus) => (meta, oo) }
      .map { case (meta, oo) => OvaraGeneerinenOpiskeluoikeus(
        metadata = meta,
        tunniste = oo.tunniste,
        oid = oo.oid,
        tyyppi = oo.tyyppi,
        oppilaitosOid = oo.oppilaitosOid,
        suoritukset = oo.suoritukset.flatMap(convertGeneerinenSuoritus).toSeq,
        tila = oo.tila,
        jaksot = oo.jaksot
      )}

  // ---- Ammatillinen ----

  private def convertAmmatillisenTutkinnonOsaAlue(oa: fi.oph.suorituspalvelu.business.AmmatillisenTutkinnonOsaAlue): OvaraAmmatillisenTutkinnonOsaAlue =
    OvaraAmmatillisenTutkinnonOsaAlue(
      tunniste = oa.tunniste,
      nimi = oa.nimi,
      koodi = oa.koodi,
      arvosana = oa.arvosana,
      laajuus = oa.laajuus,
      korotettu = oa.korotettu
    )

  private def convertAmmatillisenTutkinnonOsa(o: fi.oph.suorituspalvelu.business.AmmatillisenTutkinnonOsa): OvaraAmmatillisenTutkinnonOsa =
    OvaraAmmatillisenTutkinnonOsa(
      tunniste = o.tunniste,
      nimi = o.nimi,
      koodi = o.koodi,
      yto = o.yto,
      arviointiPaiva = o.arviointiPaiva,
      arvosana = o.arvosana,
      laajuus = o.laajuus,
      osaAlueet = o.osaAlueet.map(convertAmmatillisenTutkinnonOsaAlue),
      korotettu = o.korotettu
    )

  private def convertAmmatillinenSuoritus(s: Suoritus): Option[OvaraAmmatillinenSuoritus] = s match {
    case pt: AmmatillinenPerustutkinto => Some(OvaraAmmatillinenPerustutkinto(
      tunniste = pt.tunniste,
      nimi = pt.nimi,
      koodi = pt.koodi,
      oppilaitos = pt.oppilaitos,
      koskiTila = pt.koskiTila,
      supaTila = pt.supaTila,
      aloitusPaivamaara = pt.aloitusPaivamaara,
      vahvistusPaivamaara = pt.vahvistusPaivamaara,
      keskiarvo = pt.keskiarvo,
      suoritustapa = pt.suoritustapa,
      suoritusKieli = pt.suoritusKieli,
      osat = pt.osat.map(convertAmmatillisenTutkinnonOsa)
    ))
    case to: AmmatillinenTutkintoOsittainen => Some(OvaraAmmatillinenTutkintoOsittainen(
      tunniste = to.tunniste,
      nimi = to.nimi,
      koodi = to.koodi,
      oppilaitos = to.oppilaitos,
      koskiTila = to.koskiTila,
      supaTila = to.supaTila,
      aloitusPaivamaara = to.aloitusPaivamaara,
      vahvistusPaivamaara = to.vahvistusPaivamaara,
      korotettuKeskiarvo = to.korotettuKeskiarvo,
      korotettuOpiskeluoikeusOid = to.korotettuOpiskeluoikeusOid,
      suoritustapa = to.suoritustapa,
      suoritusKieli = to.suoritusKieli,
      osat = to.osat.map(convertAmmatillisenTutkinnonOsa)
    ))
    case at: AmmattiTutkinto => Some(OvaraAmmattiTutkinto(
      tunniste = at.tunniste,
      nimi = at.nimi,
      koodi = at.koodi,
      oppilaitos = at.oppilaitos,
      koskiTila = at.koskiTila,
      supaTila = at.supaTila,
      aloitusPaivamaara = at.aloitusPaivamaara,
      vahvistusPaivamaara = at.vahvistusPaivamaara,
      suoritustapa = at.suoritustapa,
      suoritusKieli = at.suoritusKieli
    ))
    case eat: ErikoisAmmattiTutkinto => Some(OvaraErikoisAmmattiTutkinto(
      tunniste = eat.tunniste,
      nimi = eat.nimi,
      koodi = eat.koodi,
      oppilaitos = eat.oppilaitos,
      koskiTila = eat.koskiTila,
      supaTila = eat.supaTila,
      aloitusPaivamaara = eat.aloitusPaivamaara,
      vahvistusPaivamaara = eat.vahvistusPaivamaara,
      suoritusKieli = eat.suoritusKieli
    ))
    case telma: Telma => Some(OvaraTelma(
      tunniste = telma.tunniste,
      nimi = telma.nimi,
      koodi = telma.koodi,
      oppilaitos = telma.oppilaitos,
      koskiTila = telma.koskiTila,
      supaTila = telma.supaTila,
      aloitusPaivamaara = telma.aloitusPaivamaara,
      vahvistusPaivamaara = telma.vahvistusPaivamaara,
      suoritusVuosi = telma.suoritusVuosi,
      suoritusKieli = telma.suoritusKieli,
      hyvaksyttyLaajuus = telma.hyvaksyttyLaajuus,
      lahtokoulut = telma.lahtokoulut
    ))
    case _ => None
  }

  def getAmmatillisetOpiskeluoikeudet(opiskeluoikeudet: Seq[(OvaraVersioMetadata, Opiskeluoikeus)]): Seq[OvaraAmmatillinenOpiskeluoikeus] =
    opiskeluoikeudet.collect { case (meta, oo: AmmatillinenOpiskeluoikeus) => (meta, oo) }
      .map { case (meta, oo) => OvaraAmmatillinenOpiskeluoikeus(
        metadata = meta,
        tunniste = oo.tunniste,
        oid = oo.oid,
        oppilaitos = oo.oppilaitos,
        suoritukset = oo.suoritukset.flatMap(convertAmmatillinenSuoritus).toSeq,
        tila = oo.tila,
        jaksot = oo.jaksot
      )}

  // ---- Perusopetus ----

  private def convertPerusopetuksenOppiaine(a: PerusopetuksenOppiaine): OvaraPerusopetuksenOppiaine =
    OvaraPerusopetuksenOppiaine(
      tunniste = a.tunniste,
      nimi = a.nimi,
      koodi = a.koodi,
      arvosana = a.arvosana,
      kieli = a.kieli,
      pakollinen = a.pakollinen,
      yksilollistetty = a.yksilollistetty,
      rajattu = a.rajattu
    )

  private def convertPerusopetuksenSuoritus(s: Suoritus): Option[OvaraPerusopetuksenSuoritus] = s match {
    case om: PerusopetuksenOppimaara => Some(OvaraPerusopetuksenOppimaara(
      tunniste = om.tunniste,
      versioTunniste = om.versioTunniste,
      oppilaitos = om.oppilaitos,
      luokka = om.luokka,
      koskiTila = om.koskiTila,
      supaTila = om.supaTila,
      suoritusKieli = om.suoritusKieli,
      koulusivistyskieli = om.koulusivistyskieli,
      yksilollistaminen = om.yksilollistaminen,
      aloitusPaivamaara = om.aloitusPaivamaara,
      vahvistusPaivamaara = om.vahvistusPaivamaara,
      aineet = om.aineet.map(convertPerusopetuksenOppiaine),
      lahtokoulut = om.lahtokoulut,
      syotetty = om.syotetty,
      vuosiluokkiinSitoutumatonOpetus = om.vuosiluokkiinSitoutumatonOpetus,
      luokkaAste = om.luokkaAste
      //Todo, add jaaluokalle
    ))
    case oos: PerusopetuksenOppimaaranOppiaineidenSuoritus => Some(OvaraPerusopetuksenOppimaaranOppiaineidenSuoritus(
      tunniste = oos.tunniste,
      versioTunniste = oos.versioTunniste,
      oppilaitos = oos.oppilaitos,
      koskiTila = oos.koskiTila,
      supaTila = oos.supaTila,
      suoritusKieli = oos.suoritusKieli,
      aloitusPaivamaara = oos.aloitusPaivamaara,
      vahvistusPaivamaara = oos.vahvistusPaivamaara,
      aineet = oos.aineet.map(convertPerusopetuksenOppiaine),
      syotetty = oos.syotetty
    ))
    case pvo: PerusopetukseenValmistavaOpetus => Some(OvaraPerusopetukseenValmistavaOpetus(
      lahtokoulut = pvo.lahtokoulut
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
        lisatiedot = oo.lisatiedot,
        tila = oo.tila,
        jaksot = oo.jaksot
      )}

  // ---- Poistettu ----

  def getPoistetutOpiskeluoikeudet(opiskeluoikeudet: Seq[(OvaraVersioMetadata, Opiskeluoikeus)]): Seq[OvaraPoistettuOpiskeluoikeus] =
    opiskeluoikeudet.collect { case (meta, oo: PoistettuOpiskeluoikeus) => (meta, oo) }
      .map { case (meta, oo) => OvaraPoistettuOpiskeluoikeus(metadata = meta, oid = oo.oid) }
}
