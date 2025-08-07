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
  ITelma,
  IEBOppiaine,
  IIBOppiaine,
} from './backend';

export type SuorituksenTila = SuoritusTila;

export type SuorituksenPerustiedot = {
  oppilaitos: {
    nimi: string | Kielistetty;
    oid: string;
  };
  tila: SuorituksenTila;
  suorituskieli?: string;
  valmistumispaiva?: Date;
};

export type Kielistetty = {
  fi?: string;
  sv?: string;
  en?: string;
};

export type Opiskeluoikeus = IUIOpiskeluoikeus;

export type OppijanTiedot = IOppijanTiedotSuccessResponse;

export type YOKoe = IYOKoe;

export type PerusopetuksenOppiaine = IPerusopetuksenOppiaine;

export type LukionOppiaine = ILukionOppiaine | IEBOppiaine | IIBOppiaine;

export type IBOppiaine = IIBOppiaine;

export type EBOppiaine = IEBOppiaine;

export type KorkeakouluSuoritus = IKKSuoritus & {
  koulutustyyppi: 'korkeakoulutus';
  key: string;
  nimi: string;
};

export type EBSuoritus = IEBTutkinto & {
  koulutustyyppi: 'eb';
  nimi: string;
  key: string;
};

export type IBSuoritus = IIBTutkinto & {
  koulutustyyppi: 'ib';
  nimi: string;
  key: string;
};

export type LukioSuoritus =
  | EBSuoritus
  | IBSuoritus
  | ((
      | IYOTutkinto
      | ILukionOppimaara
      | ILukionOppiaineenOppimaara
      | IDIATutkinto
      | IDIAVastaavuusTodistus
      | IPreIB
    ) & { koulutustyyppi: 'lukio'; nimi: string; key: string });

export type AmmatillinenSuoritus = (
  | IAmmatillinenTutkinto
  | IAmmattitutkinto
  | IErikoisammattitutkinto
) & { koulutustyyppi: 'ammatillinen'; key: string; nimi: string };

export type TUVASuoritus = ITuva & {
  koulutustyyppi: 'tuva';
  key: string;
  nimi: string;
};
export type TelmaSuoritus = ITelma & {
  koulutustyyppi: 'telma';
  key: string;
  nimi: string;
};

export type VapaaSivistystyoSuoritus = IVapaanSivistysTyonKoulutus & {
  koulutustyyppi: 'vapaa-sivistystyo';
  key: string;
  nimi: string;
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
  | TelmaSuoritus
  | TUVASuoritus
  | VapaaSivistystyoSuoritus
  | PerusopetusSuoritus;

export type Language = 'fi' | 'sv' | 'en';
