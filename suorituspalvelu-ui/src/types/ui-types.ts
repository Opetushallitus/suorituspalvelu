import {
  IYOKoe,
  SuoritusTila,
  IPerusopetuksenOppiaine,
  ILukionOppiaine,
  IKKSuoritus,
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
