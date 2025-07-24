/* Generated using Scala-TSI (https://github.com/scala-tsi/scala-tsi) */

export interface IAikuistenPerusopetuksenOppimaara {
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IPerusopetuksenOppiaine[];
}

export interface IAmmatillinenOppilaitos {
  nimi: string;
  oid: string;
}

export interface IAmmatillinenTutkinto {
  nimi: string;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  painotettuKeskiarvo: number;
  ammatillisetYtotKeskiarvo: number;
  ytot: IYTO[];
  ammatillisenTutkinnonOsat: IAmmatillisenTutkinnonOsa[];
  suoritustapa?: string;
}

export interface IAmmatillisenTutkinnonOsa {
  nimi: string;
  laajuus: number;
  arvosana: number;
}

export interface IAmmattitutkinto {
  nimi: string;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface IDIATutkinto {
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface IDIAVastaavuusTodistus {
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  kieletKirjallisuusTaide: IOppiaine[];
  matematiikkaLuonnontieteet: IOppiaine[];
}

export interface IEBOppiaine {
  nimi: string;
  suorituskieli: string;
  laajuus: number;
  written: IEBSuoritus;
  oral?: IEBSuoritus;
  final: IEBSuoritus;
}

export interface IEBSuoritus {
  arvosana: number;
}

export interface IEBTutkinto {
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IEBOppiaine[];
}

export interface IErikoisammattitutkinto {
  nimi: string;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface IHakukohde {
  nimi: string;
}

export interface IIBOppiaine {
  nimi: string;
  suoritukset: IIBSuoritus[];
}

export interface IIBSuoritus {
  nimi: string;
  laajuus: number;
  predictedGrade?: number;
  arvosana: number;
}

export interface IIBTutkinto {
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IIBOppiaine[];
}

export interface IKKOppilaitos {
  nimi: string;
  oid: string;
}

export interface IKKSuoritus {
  tutkinto: string;
  oppilaitos: IKKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  hakukohde: IHakukohde;
}

export interface ILukionOppiaine {
  nimi: string;
}

export interface ILukionOppiaineenOppimaara {
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: ILukionOppiaine[];
}

export interface ILukionOppimaara {
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: ILukionOppiaine[];
}

export interface INuortenPerusopetuksenOppiaineenOppimaara {
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IOppimaaranOppiaine[];
}

export interface IOOOppilaitos {
  nimi: string;
  oid: string;
}

export interface IOppiaine {
  nimi: string;
  laajuus: number;
  keskiarvo: number;
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
  oppijaNumero: string;
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
  ammatillisetTutkinnot: IAmmatillinenTutkinto[];
  ammattitutkinnot: IAmmattitutkinto[];
  erikoisammattitutkinnot: IErikoisammattitutkinto[];
  telmat: ITelma[];
  tuvat: ITuva[];
  vapaanSivistystyonKoulutukset: IVapaanSivistysTyonKoulutus[];
  perusopetuksenOppimaarat: IPerusopetuksenOppimaara[];
  perusopetuksenOppimaara78Luokkalaiset?: IPerusopetuksenOppimaara78Luokkalaiset;
  nuortenPerusopetuksenOppiaineenOppimaarat: INuortenPerusopetuksenOppiaineenOppimaara[];
  perusopetuksenOppiaineenOppimaarat: IPerusopetuksenOppiaineenOppimaara[];
  aikuistenPerusopetuksenOppimaarat: IAikuistenPerusopetuksenOppimaara[];
}

export interface IOppilaitos {
  nimi: string;
  oid: string;
}

export interface IOppilaitosFailureResponse {
  virheet: string[];
}

export interface IOppilaitosSuccessResponse {
  oppilaitokset: IOppilaitos[];
}

export interface IOppimaaranOppiaine {
  nimi: string;
  arvosana: number;
}

export interface IPKOppilaitos {
  nimi: string;
  oid: string;
}

export interface IPerusopetuksenOppiaine {
  nimi: string;
  arvosana?: number;
  valinnainen?: string;
}

export interface IPerusopetuksenOppiaineenOppimaara {
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IOppimaaranOppiaine[];
}

export interface IPerusopetuksenOppimaara {
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  luokka: string;
  yksilollistetty: boolean;
  oppiaineet: IPerusopetuksenOppiaine[];
}

export interface IPerusopetuksenOppimaara78Luokkalaiset {
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  koulusivistyskieli: string;
  luokka: string;
  yksilollistetty: boolean;
}

export interface IPreIB {
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface ITelma {
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface ITuva {
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  laajuus: number;
}

export interface IUIOpiskeluoikeus {
  tutkinto: string;
  oppilaitos: IOOOppilaitos;
  voimassaolonAlku: Date;
  voimassaolonLoppu: Date;
}

export interface IVapaanSivistysTyonKoulutus {
  nimi: string;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  laajuus: number;
}

export interface IYOKoe {
  aine: string;
  taso: string;
  arvosana: string;
  yhteispistemaara: number;
  tutkintokerta: Date;
}

export interface IYOOppilaitos {
  nimi: string;
  oid: string;
}

export interface IYOTutkinto {
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  yoKokeet: IYOKoe[];
}

export interface IYTO {
  nimi: string;
  laajuus: number;
  tila: YTOTila;
}

export type SuoritusTila = 'VALMIS' | 'KESKEN' | 'KESKEYTYNYT';

export type YTOTila = 'HYVAKSYTTY';
