/* Scala-koodista automaattisesti generoituja tyyppejä (kts. TypeScriptGenerator.scala). Älä muokkaa käsin! */

export interface IAikuistenPerusopetuksenOppimaara {
  tunniste: string;
  nimi: IAikuistenPerusopetuksenOppimaaraNimi;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IPerusopetuksenOppiaine[];
}

export interface IAikuistenPerusopetuksenOppimaaraNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

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
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
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
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface IAmmattitutkintoNimi {
  fi?: string;
  sv?: string;
  en?: string;
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
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
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
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
  kieletKirjallisuusTaide: IDIAOppiaine[];
  matematiikkaLuonnontieteet: IDIAOppiaine[];
}

export interface IDIAVastaavuusTodistusNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IEBOppiaine {
  tunniste: string;
  nimi: IEBOppiaineNimi;
  suorituskieli: string;
  laajuus: number;
  written: IEBSuoritus;
  oral?: IEBSuoritus;
  final: IEBSuoritus;
}

export interface IEBOppiaineNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IEBSuoritus {
  tunniste: string;
  arvosana: number;
}

export interface IEBTutkinto {
  tunniste: string;
  nimi: IEBTutkintoNimi;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IEBOppiaine[];
}

export interface IEBTutkintoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IErikoisammattitutkinto {
  tunniste: string;
  nimi: IErikoisammattitutkintoNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface IErikoisammattitutkintoNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IHakukohde {
  nimi: IHakukohdeNimi;
}

export interface IHakukohdeNimi {
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
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IIBOppiaine[];
}

export interface IIBTutkintoNimi {
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
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  hakukohde: IHakukohde;
}

export interface IKKSuoritusNimi {
  fi?: string;
  sv?: string;
  en?: string;
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
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
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
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: ILukionOppiaine[];
}

export interface ILukionOppimaaraNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface INuortenPerusopetuksenOppiaineenOppimaara {
  tunniste: string;
  nimi: INuortenPerusopetuksenOppiaineenOppimaaraNimi;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IOppimaaranOppiaine[];
}

export interface INuortenPerusopetuksenOppiaineenOppimaaraNimi {
  fi?: string;
  sv?: string;
  en?: string;
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
  nimi: string;
}

export interface IOppijanHakuFailureResponse {
  virheet: string[];
}

export interface IOppijanHakuSuccessResponse {
  oppijat: IOppija[];
}

export interface IOppijanTiedotFailureResponse {
  virheet: string[];
}

export interface IOppijanTiedotSuccessResponse {
  nimi: string;
  henkiloTunnus: string;
  syntymaAika: Date;
  oppijaNumero: string;
  henkiloOID: string;
  opiskeluoikeudet: IUIOpiskeluoikeus[];
  kkTutkinnot: IKKSuoritus[];
  yoTutkinto?: IYOTutkinto;
  lukionOppimaara?: ILukionOppimaara;
  lukionOppiaineenOppimaarat: ILukionOppiaineenOppimaara[];
  diaTutkinto?: IDIATutkinto;
  diaVastaavuusTodistus?: IDIAVastaavuusTodistus;
  ebTutkinto?: IEBTutkinto;
  ibTutkinto?: IIBTutkinto;
  preIB?: IPreIB;
  ammatillisetPerusTutkinnot: IAmmatillinentutkinto[];
  ammattitutkinnot: IAmmattitutkinto[];
  erikoisammattitutkinnot: IErikoisammattitutkinto[];
  telmat: ITelma[];
  tuvat: ITuva[];
  vapaanSivistystyonKoulutukset: IVapaanSivistystyonKoulutus[];
  perusopetuksenOppimaarat: IPerusopetuksenOppimaara[];
  perusopetuksenOppimaara78Luokkalaiset?: IPerusopetuksenOppimaara78Luokkalaiset;
  nuortenPerusopetuksenOppiaineenOppimaarat: INuortenPerusopetuksenOppiaineenOppimaara[];
  perusopetuksenOppiaineenOppimaarat: IPerusopetuksenOppiaineenOppimaara[];
  aikuistenPerusopetuksenOppimaarat: IAikuistenPerusopetuksenOppimaara[];
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

export interface IOppimaaranOppiaine {
  tunniste: string;
  nimi: IOppimaaranOppiaineNimi;
  arvosana: number;
}

export interface IOppimaaranOppiaineNimi {
  fi?: string;
  sv?: string;
  en?: string;
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

export interface IPerusopetuksenOppiaine {
  tunniste: string;
  nimi: IPerusopetuksenOppiaineNimi;
  arvosana?: number;
  valinnainen?: string;
}

export interface IPerusopetuksenOppiaineNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IPerusopetuksenOppiaineenOppimaara {
  tunniste: string;
  nimi: IPerusopetuksenOppiaineenOppimaaraNimi;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IOppimaaranOppiaine[];
}

export interface IPerusopetuksenOppiaineenOppimaaraNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IPerusopetuksenOppimaara {
  tunniste: string;
  nimi: IPerusopetuksenOppimaaraNimi;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
  luokka: string;
  yksilollistetty: boolean;
  oppiaineet: IPerusopetuksenOppiaine[];
}

export interface IPerusopetuksenOppimaara78Luokkalaiset {
  tunniste: string;
  nimi: IPerusopetuksenOppimaara78LuokkalaisetNimi;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
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

export interface IPreIB {
  tunniste: string;
  nimi: IPreIBNimi;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface IPreIBNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface ITelma {
  tunniste: string;
  nimi: ITelmaNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
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
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
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
  voimassaolonAlku: Date;
  voimassaolonLoppu: Date;
}

export interface IUIOpiskeluoikeusNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IVapaanSivistystyonKoulutus {
  tunniste: string;
  nimi: IVapaanSivistystyonKoulutusNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  aloituspaiva?: Date;
  valmistumispaiva?: Date;
  suorituskieli: string;
  laajuus: number;
}

export interface IVapaanSivistystyonKoulutusNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IYOKoe {
  tunniste: string;
  aine: string;
  taso: string;
  arvosana: string;
  yhteispistemaara: number;
  tutkintokerta: Date;
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
  valmistumispaiva?: Date;
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
  arvosana?: string;
}

export interface IYTONimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export type SuoritusTila =
  | 'VALMIS'
  | 'KESKEN'
  | 'KESKEYTYNYT'
  | 'MITATOITY'
  | 'PERUUTETTU'
  | 'PAATTYNYT';

export type Suoritustapa = 'NAYTTOTUTKINTO';
