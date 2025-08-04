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
  IOppijanTiedotSuccessResponse,
  IUIOpiskeluoikeus,
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

export type Opiskeluoikeus = IUIOpiskeluoikeus;

export type OppijaResponse = IOppijanTiedotSuccessResponse;

export type YOKoe = IYOKoe;

export type PerusopetuksenOppiaine = IPerusopetuksenOppiaine;

export type LukionOppiaine = ILukionOppiaine;

export type KorkeakouluSuoritus = IKKSuoritus & {
  koulutustyyppi: 'korkeakoulutus';
  key: string;
};

export type LukioSuoritus = (
  | IYOTutkinto
  | ILukionOppimaara
  | ILukionOppiaineenOppimaara
  | IDIATutkinto
  | IDIAVastaavuusTodistus
  | IEBTutkinto
  | IIBTutkinto
  | IPreIB
) & { koulutustyyppi: 'lukio'; nimi: string; key: string };

export type AmmatillinenSuoritus = (
  | IAmmatillinenTutkinto
  | IAmmattitutkinto
  | IErikoisammattitutkinto
) & { koulutustyyppi: 'ammatillinen'; key: string };

export type TUVASuoritus = ITuva & { koulutustyyppi: 'tuva'; key: string };

export type VapaaSivistystyoSuoritus = IVapaanSivistysTyonKoulutus & {
  koulutustyyppi: 'vapaa-sivistystyo';
  key: string;
};

export type PerusopetusSuoritus = (
  | IPerusopetuksenOppimaara
  | IPerusopetuksenOppiaineenOppimaara
  | IPerusopetuksenOppimaara78Luokkalaiset
  | INuortenPerusopetuksenOppiaineenOppimaara
  | IAikuistenPerusopetuksenOppimaara
) & { koulutustyyppi: 'perusopetus'; nimi: string; key: string };

export type Suoritus =
  | KorkeakouluSuoritus
  | LukioSuoritus
  | AmmatillinenSuoritus
  | TUVASuoritus
  | VapaaSivistystyoSuoritus
  | PerusopetusSuoritus;
