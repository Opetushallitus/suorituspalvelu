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
  IVapaanSivistysTyonKoulutus,
  ITuva,
  IPerusopetuksenOppiaineenOppimaara,
  IPerusopetuksenOppimaara78Luokkalaiset,
  INuortenPerusopetuksenOppiaineenOppimaara,
  IAikuistenPerusopetuksenOppimaara,
  IPerusopetuksenOppimaara,
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

export type TUVASuoritus = ITuva;

export type VapaaSivistystyoSuoritus = IVapaanSivistysTyonKoulutus;

export type PerusopetusSuoritus =
  | IPerusopetuksenOppimaara
  | IPerusopetuksenOppiaineenOppimaara
  | IPerusopetuksenOppimaara78Luokkalaiset
  | INuortenPerusopetuksenOppiaineenOppimaara
  | IAikuistenPerusopetuksenOppimaara;
