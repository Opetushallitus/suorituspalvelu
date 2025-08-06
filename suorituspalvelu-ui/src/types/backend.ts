/* Scala-koodista automaattisesti generoituja tyyppejä (kts. TypeScriptGenerator.scala). Älä muokkaa käsin! */

export interface IAikuistenPerusopetuksenOppimaara {
  tunniste: string;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IPerusopetuksenOppiaine[];
}

export interface IAmmatillinenOppilaitos {
  nimi: IAmmatillisenOppilaitoksenNimi;
  oid: string;
}

export interface IAmmatillinenTutkinto {
  tunniste: string;
  nimi: IAmmatillisenTutkinnonNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  painotettuKeskiarvo?: number;
  ytot: IYTO[];
  ammatillisenTutkinnonOsat: IAmmatillisenTutkinnonOsa[];
  suoritustapa?: Suoritustapa;
}

export interface IAmmatillisenOppilaitoksenNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IAmmatillisenTutkinnonNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IAmmatillisenTutkinnonOsa {
  tunniste: string;
  nimi: IAmmatillisenTutkinnonOsanNimi;
  laajuus?: number;
  arvosana?: string;
}

export interface IAmmatillisenTutkinnonOsanNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IAmmattitutkinnonNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IAmmattitutkinto {
  tunniste: string;
  nimi: IAmmattitutkinnonNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface IDIATutkinto {
  tunniste: string;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface IDIAVastaavuusTodistus {
  tunniste: string;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  kieletKirjallisuusTaide: IOppiaine[];
  matematiikkaLuonnontieteet: IOppiaine[];
}

export interface IEBOppiaine {
  tunniste: string;
  nimi: string;
  suorituskieli: string;
  laajuus: number;
  written: IEBSuoritus;
  oral?: IEBSuoritus;
  final: IEBSuoritus;
}

export interface IEBSuoritus {
  tunniste: string;
  arvosana: number;
}

export interface IEBTutkinto {
  tunniste: string;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IEBOppiaine[];
}

export interface IErikoisammattitutkinnonNimi {
  fi?: string;
  sv?: string;
  en?: string;
}

export interface IErikoisammattitutkinto {
  tunniste: string;
  nimi: IErikoisammattitutkinnonNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface IHakukohde {
  nimi: string;
}

export interface IIBOppiaine {
  tunniste: string;
  nimi: string;
  suoritukset: IIBSuoritus[];
}

export interface IIBSuoritus {
  tunniste: string;
  nimi: string;
  laajuus: number;
  predictedGrade?: number;
  arvosana: number;
}

export interface IIBTutkinto {
  tunniste: string;
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
  tunniste: string;
  tutkinto: string;
  oppilaitos: IKKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  hakukohde: IHakukohde;
}

export interface ILukionOppiaine {
  tunniste: string;
  nimi: string;
}

export interface ILukionOppiaineenOppimaara {
  tunniste: string;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: ILukionOppiaine[];
}

export interface ILukionOppimaara {
  tunniste: string;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: ILukionOppiaine[];
}

export interface INuortenPerusopetuksenOppiaineenOppimaara {
  tunniste: string;
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
  tunniste: string;
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
  ammatillisetPerusTutkinnot: IAmmatillinenTutkinto[];
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
  tunniste: string;
  nimi: string;
  arvosana: number;
}

export interface IPKOppilaitos {
  nimi: string;
  oid: string;
}

export interface IPerusopetuksenOppiaine {
  tunniste: string;
  nimi: string;
  arvosana?: number;
  valinnainen?: string;
}

export interface IPerusopetuksenOppiaineenOppimaara {
  tunniste: string;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  oppiaineet: IOppimaaranOppiaine[];
}

export interface IPerusopetuksenOppimaara {
  tunniste: string;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  luokka: string;
  yksilollistetty: boolean;
  oppiaineet: IPerusopetuksenOppiaine[];
}

export interface IPerusopetuksenOppimaara78Luokkalaiset {
  tunniste: string;
  oppilaitos: IPKOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  koulusivistyskieli: string;
  luokka: string;
  yksilollistetty: boolean;
}

export interface IPreIB {
  tunniste: string;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
}

export interface ITelma {
  tunniste: string;
  nimi: ITelmaNimi;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
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
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  laajuus: number;
}

export interface IUIOpiskeluoikeus {
  tunniste: string;
  tutkinto: string;
  oppilaitos: IOOOppilaitos;
  voimassaolonAlku: Date;
  voimassaolonLoppu: Date;
}

export interface IVapaanSivistysTyonKoulutus {
  tunniste: string;
  nimi: string;
  oppilaitos: IAmmatillinenOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  laajuus: number;
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
  nimi: string;
  oid: string;
}

export interface IYOTutkinto {
  tunniste: string;
  oppilaitos: IYOOppilaitos;
  tila: SuoritusTila;
  valmistumispaiva?: Date;
  suorituskieli: string;
  yoKokeet: IYOKoe[];
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
