import { IYOKoe, SuoritusTila, IPerusopetuksenOppiaine } from './backend';

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
