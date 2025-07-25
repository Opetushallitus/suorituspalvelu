import {
  IYOKoe,
  SuoritusTila,
  IPerusopetuksenOppiaine,
  ILukionOppiaine,
  IKKSuoritus,
  ILukionOppimaara,
  ILukionOppiaineenOppimaara,
  IYOTutkinto,
  IDIATutkinto,
  IDIAVastaavuusTodistus,
  IEBTutkinto,
  IIBTutkinto,
  IPreIB,
  IAmmatillinenTutkinto,
  IAmmattitutkinto,
  IErikoisammattitutkinto,
} from './backend';

export type SuorituksenTila = SuoritusTila;

export type SuorituksenPerustiedot = {
  oppilaitos: {
    nimi: string;
    oid: string;
  };
  tila: SuorituksenTila;
  suorituskieli?: string;
  valmistumispaiva?: Date;
};

export type YOKoe = IYOKoe;

export type PerusopetuksenOppiaine = IPerusopetuksenOppiaine;

export type LukionOppiaine = ILukionOppiaine;

export type KorkeakouluSuoritus = IKKSuoritus;

export type LukioSuoritus =
  | IYOTutkinto
  | ILukionOppimaara
  | ILukionOppiaineenOppimaara
  | IDIATutkinto
  | IDIAVastaavuusTodistus
  | IEBTutkinto
  | IIBTutkinto
  | IPreIB;

export type AmmatillinenSuoritus =
  | IAmmatillinenTutkinto
  | IAmmattitutkinto
  | IErikoisammattitutkinto;
