/* Scala-koodista automaattisesti generoituja tyyppejä (kts. TypeScriptGenerator.scala). Älä muokkaa käsin! */

export interface IAmmatillinenOppilaitos {
  nimi: IAmmatillinenOppilaitosNimi;
  oid: string;
}

export interface IAmmatillinenOppilaitosNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IAmmatillinentutkinto {
  tunniste: string;
  nimi: IAmmatillinentutkintoNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
  painotettuKeskiarvo?: number;
  ytot: IYTO[];
  ammatillisenTutkinnonOsat: IAmmatillisenTutkinnonOsa[];
  suoritustapa?: Suoritustapa;
}

export interface IAmmatillinentutkintoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IAmmatillisenTutkinnonOsa {
  tunniste: string;
  nimi: IAmmatillisenTutkinnonOsaNimi;
  laajuus?: number;
  arvosana?: string;
  osaAlueet: IAmmatillisenTutkinnonOsaAlue[];
}

export interface IAmmatillisenTutkinnonOsaAlue {
  nimi: IAmmatillisenTutkinnonOsaAlueNimi;
  laajuus?: number;
  arvosana?: string;
}

export interface IAmmatillisenTutkinnonOsaAlueNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IAmmatillisenTutkinnonOsaNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IAmmattitutkinto {
  tunniste: string;
  nimi: IAmmattitutkintoNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
}

export interface IAmmattitutkintoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IAvainArvoContainerUI {
  avain: string;
  arvo: string;
  metadata: IAvainArvoMetadataUI;
}

export interface IAvainArvoMetadataUI {
  selitteet: string[];
  arvoEnnenYliajoa?: string;
  yliajo?: IAvainArvoYliajoUI;
  arvoOnHakemukselta: boolean;
}

export interface IAvainArvoYliajoUI {
  avain: string;
  arvo?: string;
  henkiloOid: string;
  hakuOid: string;
  virkailijaOid: string;
  selite: string;
}

export interface IDIAOppiaine {
  tunniste: string;
  nimi: IDIAOppiaineNimi;
  laajuus: number;
  keskiarvo: number;
}

export interface IDIAOppiaineNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IDIATutkinto {
  tunniste: string;
  nimi: IDIATutkintoNimi;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
}

export interface IDIATutkintoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IDIAVastaavuusTodistus {
  tunniste: string;
  nimi: IDIAVastaavuusTodistusNimi;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
  kieletKirjallisuusTaide: IDIAOppiaine[];
  matematiikkaLuonnontieteet: IDIAOppiaine[];
}

export interface IDIAVastaavuusTodistusNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IEBOppiaineNimiUI {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IEBOppiaineOsasuoritusUI {
  koodi: string;
  arvosana: number;
}

export interface IEBOppiaineUI {
  tunniste: string;
  nimi: IEBOppiaineNimiUI;
  suorituskieli: string;
  laajuus: number;
  written?: IEBOppiaineOsasuoritusUI;
  oral?: IEBOppiaineOsasuoritusUI;
  final?: IEBOppiaineOsasuoritusUI;
}

export interface IEBTutkintoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IEBTutkintoUI {
  tunniste: string;
  nimi: IEBTutkintoNimi;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
  oppiaineet: IEBOppiaineUI[];
}

export interface IErikoisammattitutkinto {
  tunniste: string;
  nimi: IErikoisammattitutkintoNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
}

export interface IErikoisammattitutkintoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IHaku {
  hakuOid: string;
  nimi: IHakuNimi;
}

export interface IHakuNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IIBOppiaine {
  tunniste: string;
  nimi: IIBOppiaineNimi;
  suoritukset: IIBSuoritus[];
}

export interface IIBOppiaineNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IIBSuoritus {
  tunniste: string;
  nimi: IIBSuoritusNimi;
  laajuus: number;
  predictedGrade?: number;
  arvosana: number;
}

export interface IIBSuoritusNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IIBTutkinto {
  tunniste: string;
  nimi: IIBTutkintoNimi;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
  oppiaineet: IIBOppiaine[];
}

export interface IIBTutkintoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IKKOpintojakso {
  tunniste: string;
  nimi: IKKOpintojaksoNimi;
  laajuus: number;
  arvosana?: string;
  opintojaksot: IKKOpintojakso[];
}

export interface IKKOpintojaksoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IKKOppilaitos {
  nimi: IKKOppilaitosNimi;
  oid: string;
}

export interface IKKOppilaitosNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IKKSuoritus {
  tunniste: string;
  nimi: IKKSuoritusNimi;
  oppilaitos: IKKOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  opintojaksot: IKKOpintojakso[];
}

export interface IKKSuoritusNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IKayttajaFailureResponse {
  virheet: string[];
}

export interface IKayttajaSuccessResponse {
  asiointiKieli: string;
  isRekisterinpitaja: boolean;
  isOrganisaationKatselija: boolean;
}

export interface ILukionOppiaine {
  tunniste: string;
  nimi: ILukionOppiaineNimi;
}

export interface ILukionOppiaineNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ILukionOppiaineenOppimaara {
  tunniste: string;
  nimi: ILukionOppiaineenOppimaaraNimi;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
  oppiaineet: ILukionOppiaine[];
}

export interface ILukionOppiaineenOppimaaraNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ILukionOppimaara {
  tunniste: string;
  nimi: ILukionOppimaaraNimi;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
  oppiaineet: ILukionOppiaine[];
}

export interface ILukionOppimaaraNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ILuoPerusopetuksenOppimaaraFailureResponse {
  yleisetVirheAvaimet: string[];
  oppiaineKohtaisetVirheet: ILuoPerusopetuksenOppimaaraFailureResponseOppiaineVirhe[];
}

export interface ILuoPerusopetuksenOppimaaraFailureResponseOppiaineVirhe {
  oppiaineKoodiArvo: string;
  virheAvaimet: string[];
}

export interface ILuoSuoritusDropdownDataFailureResponse {
  virheet: string[];
}

export interface ILuoSuoritusDropdownDataSuccessResponse {
  suoritusTilat: ISyotettavaSuoritusTilaVaihtoehto[];
  suoritusTyypit: ISyotettavaSuoritusTyyppiVaihtoehto[];
  oppiaineet: ISyotettavaOppiaineVaihtoehto[];
  suoritusKielet: ISyotettavaSuoritusKieliVaihtoehto[];
  aidinkielenOppimaarat: ISyotettavaAidinkielenOppimaaraVaihtoehto[];
  vieraatKielet: ISyotettavaVierasKieliVaihtoehto[];
  yksilollistaminen: ISyotettavaYksilollistamisVaihtoehto[];
  arvosanat: ISyotettavaArvosanaVaihtoehto[];
}

export interface ILuokatFailureResponse {
  virheet: string[];
}

export interface ILuokatSuccessResponse {
  luokat: string[];
}

export interface IOOOppilaitos {
  nimi: IOOOppilaitosNimi;
  oid: string;
}

export interface IOOOppilaitosNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IOppija {
  oppijaNumero: string;
  hetu?: string;
  etunimet?: string;
  sukunimi?: string;
  luokat: string[];
}

export interface IOppijanHakuFailureResponse {
  virheet: string[];
}

export interface IOppijanHakuSuccessResponse {
  oppijat: IOppija[];
}

export interface IOppijanHautFailureResponse {
  virheet: string[];
}

export interface IOppijanHautSuccessResponse {
  haut: IHaku[];
}

export interface IOppijanTiedotFailureResponse {
  virheet: string[];
}

export interface IOppijanTiedotSuccessResponse {
  etunimet?: string;
  sukunimi?: string;
  henkiloTunnus?: string;
  syntymaAika?: string;
  oppijaNumero: string;
  henkiloOID: string;
  opiskeluoikeudet: IUIOpiskeluoikeus[];
  kkTutkinnot: IKKSuoritus[];
  yoTutkinnot: IYOTutkinto[];
  lukionOppimaara?: ILukionOppimaara;
  lukionOppiaineenOppimaarat: ILukionOppiaineenOppimaara[];
  diaTutkinto?: IDIATutkinto;
  diaVastaavuusTodistus?: IDIAVastaavuusTodistus;
  ebTutkinto?: IEBTutkintoUI;
  ibTutkinto?: IIBTutkinto;
  preIB?: IPreIB;
  ammatillisetPerusTutkinnot: IAmmatillinentutkinto[];
  ammattitutkinnot: IAmmattitutkinto[];
  erikoisammattitutkinnot: IErikoisammattitutkinto[];
  telmat: ITelma[];
  tuvat: ITuva[];
  vapaaSivistystyoKoulutukset: IVapaaSivistystyoKoulutus[];
  perusopetuksenOppimaarat: IPerusopetuksenOppimaaraUI[];
  perusopetuksenOppimaara78Luokkalaiset?: IPerusopetuksenOppimaara78Luokkalaiset;
  perusopetuksenOppiaineenOppimaarat: IPerusopetuksenOppiaineenOppimaaratUI[];
}

export interface IOppijanValintaDataFailureResponse {
  virheAvaimet: string[];
}

export interface IOppijanValintaDataSuccessResponse {
  henkiloOID: string;
  hakuOID: string;
  avainArvot: IAvainArvoContainerUI[];
}

export interface IOppilaitos {
  nimi: IOppilaitosNimi;
  oid: string;
}

export interface IOppilaitosFailureResponse {
  virheet: string[];
}

export interface IOppilaitosNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IOppilaitosSuccessResponse {
  oppilaitokset: IOppilaitos[];
}

export interface IPKOppilaitos {
  nimi: IPKOppilaitosNimi;
  oid: string;
}

export interface IPKOppilaitosNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IPerusopetuksenOppiaineNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IPerusopetuksenOppiaineUI {
  tunniste: string;
  koodi: string;
  nimi: IPerusopetuksenOppiaineNimi;
  kieli?: string;
  arvosana: string;
  valinnainen: boolean;
}

export interface IPerusopetuksenOppiaineenOppimaaraNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IPerusopetuksenOppiaineenOppimaaratUI {
  tunniste: string;
  versioTunniste?: string;
  oppilaitos: IPKOppilaitos;
  nimi: IPerusopetuksenOppiaineenOppimaaraNimi;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
  oppiaineet: IPerusopetuksenOppiaineUI[];
  syotetty: boolean;
}

export interface IPerusopetuksenOppimaara78Luokkalaiset {
  tunniste: string;
  nimi: IPerusopetuksenOppimaara78LuokkalaisetNimi;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
  koulusivistyskieli: string;
  luokka: string;
  yksilollistetty: boolean;
}

export interface IPerusopetuksenOppimaara78LuokkalaisetNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IPerusopetuksenOppimaaraNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IPerusopetuksenOppimaaraUI {
  versioTunniste?: string;
  tunniste: string;
  nimi: IPerusopetuksenOppimaaraNimi;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
  luokka?: string;
  yksilollistaminen?: IYksilollistaminen;
  oppiaineet: IPerusopetuksenOppiaineUI[];
  syotetty: boolean;
}

export interface IPoistaSuoritusFailureResponse {
  virheAvaimet: string[];
}

export interface IPreIB {
  tunniste: string;
  nimi: IPreIBNimi;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
}

export interface IPreIBNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ISyotettavaAidinkielenOppimaaraVaihtoehto {
  nimi: ISyotettavaAidinkielenOppimaaraVaihtoehtoNimi;
  arvo: string;
}

export interface ISyotettavaAidinkielenOppimaaraVaihtoehtoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ISyotettavaArvosanaVaihtoehto {
  arvo: number;
}

export interface ISyotettavaOppiaineVaihtoehto {
  nimi: ISyotettavaOppiaineVaihtoehtoNimi;
  arvo: string;
  isKieli: boolean;
  isAidinkieli: boolean;
}

export interface ISyotettavaOppiaineVaihtoehtoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ISyotettavaSuoritusKieliVaihtoehto {
  nimi: ISyotettavaSuoritusKieliVaihtoehtoNimi;
  arvo: string;
}

export interface ISyotettavaSuoritusKieliVaihtoehtoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ISyotettavaSuoritusTilaVaihtoehto {
  nimi: ISyotettavaSuoritusTilaVaihtoehtoNimi;
  arvo: string;
}

export interface ISyotettavaSuoritusTilaVaihtoehtoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ISyotettavaSuoritusTyyppiVaihtoehto {
  nimi: ISyotettavaSuoritusTyyppiVaihtoehtoNimi;
  arvo: string;
}

export interface ISyotettavaSuoritusTyyppiVaihtoehtoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ISyotettavaVierasKieliVaihtoehto {
  nimi: ISyotettavaVierasKieliVaihtoehtoNimi;
  arvo: string;
}

export interface ISyotettavaVierasKieliVaihtoehtoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ISyotettavaYksilollistamisVaihtoehto {
  nimi: ISyotettavaYksilollistamisVaihtoehtoNimi;
  arvo: number;
}

export interface ISyotettavaYksilollistamisVaihtoehtoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ITelma {
  tunniste: string;
  nimi: ITelmaNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
}

export interface ITelmaNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ITuva {
  tunniste: string;
  nimi: ITuvaNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  laajuus?: ITuvaLaajuus;
}

export interface ITuvaLaajuus {
  arvo: number;
  yksikko: ITuvaLaajuusYksikko;
}

export interface ITuvaLaajuusYksikko {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ITuvaNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IUIOpiskeluoikeus {
  tunniste: string;
  nimi: IUIOpiskeluoikeusNimi;
  oppilaitos: IOOOppilaitos;
  voimassaolonAlku: string;
  voimassaolonLoppu: string;
  supaTila: OpiskeluoikeusTila;
  virtaTila: IUIOpiskeluoikeusVirtaTila;
}

export interface IUIOpiskeluoikeusNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IUIOpiskeluoikeusVirtaTila {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IVapaaSivistystyoKoulutus {
  tunniste: string;
  nimi: IVapaaSivistystyoKoulutusNimi;
  oppilaitos: IVapaaSivistystyoOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
  suorituskieli: string;
  laajuus?: IVapaaSivistystyoLaajuus;
}

export interface IVapaaSivistystyoKoulutusNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IVapaaSivistystyoLaajuus {
  arvo: number;
  yksikko: IVapaaSivistystyoLaajuusYksikko;
}

export interface IVapaaSivistystyoLaajuusYksikko {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IVapaaSivistystyoOppilaitos {
  nimi: IVapaaSivistystyoOppilaitosNimi;
  oid: string;
}

export interface IVapaaSivistystyoOppilaitosNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IVuodetFailureResponse {
  virheet: string[];
}

export interface IVuodetSuccessResponse {
  vuodet: string[];
}

export interface IYOKoe {
  tunniste: string;
  nimi: IYOKoeNimi;
  arvosana: string;
  yhteispistemaara?: number;
  tutkintokerta: string;
}

export interface IYOKoeNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IYOOppilaitos {
  nimi: IYOOppilaitosNimi;
  oid: string;
}

export interface IYOOppilaitosNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IYOTutkinto {
  tunniste: string;
  nimi: IYOTutkintoNimi;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: string;
  suorituskieli: string;
  yoKokeet: IYOKoe[];
}

export interface IYOTutkintoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IYTO {
  tunniste: string;
  nimi: IYTONimi;
  laajuus?: number;
  arvosana?: IYTOArvosana;
  osaAlueet: IYTOOsaAlue[];
}

export interface IYTOArvosana {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IYTONimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IYTOOsaAlue {
  nimi: IYTOOsaAlueNimi;
  laajuus?: number;
  arvosana?: string;
}

export interface IYTOOsaAlueNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IYksilollistaminen {
  arvo: number;
  nimi: IYksilollistamisNimi;
}

export interface IYksilollistamisNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IYliajo {
  avain?: string;
  arvo?: string;
  selite?: string;
}

export interface IYliajoTallennusContainer {
  henkiloOid?: string;
  hakuOid?: string;
  yliajot?: IYliajo[];
}

export interface IYliajonMuutosHistoriaFailureResponse {
  virheAvaimet: string[];
}

export interface IYliajonMuutosHistoriaSuccessResponse {
  muutokset: IYliajonMuutosUI[];
}

export interface IYliajonMuutosUI {
  arvo?: string;
  luotu: string;
  virkailija?: string;
  selite: string;
}

export type OpiskeluoikeusTila = ("VOIMASSA" | "EI_VOIMASSA" | "PAATTYNYT");

export type SuoritusTila = ("VALMIS" | "KESKEN" | "KESKEYTYNYT");

export type Suoritustapa = "NAYTTOTUTKINTO";
