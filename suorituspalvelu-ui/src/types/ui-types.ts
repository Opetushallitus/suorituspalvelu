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
  IAmmatillinentutkinto,
  IAmmattitutkinto,
  IErikoisammattitutkinto,
  IVapaaSivistystyoKoulutus,
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
  IAmmatillisenTutkinnonOsa,
  IYTO,
} from './backend';

export type SuorituksenTila = SuoritusTila;

export type SuorituksenPerustiedot = {
  oppilaitos: {
    nimi: Kielistetty;
    oid: string;
  };
  tila: SuorituksenTila;
  suorituskieli?: string;
  valmistumispaiva?: Date;
};

export type SuoritusOtsikkoTiedot = {
  nimi: Kielistetty;
  tila: SuorituksenTila;
  aloituspaiva?: Date;
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
};

export type EBSuoritus = IEBTutkinto & {
  koulutustyyppi: 'eb';
  key: string;
};

export type IBSuoritus = IIBTutkinto & {
  koulutustyyppi: 'ib';
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
  | IAmmatillinentutkinto
  | IAmmattitutkinto
  | IErikoisammattitutkinto
) & { koulutustyyppi: 'ammatillinen'; key: string; nimi: string };

export type TUVASuoritus = ITuva & {
  koulutustyyppi: 'tuva';
  key: string;
};
export type TelmaSuoritus = ITelma & {
  koulutustyyppi: 'telma';
  key: string;
};

export type VapaaSivistystyoSuoritus = IVapaaSivistystyoKoulutus & {
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
  | TelmaSuoritus
  | TUVASuoritus
  | VapaaSivistystyoSuoritus
  | PerusopetusSuoritus;

export type Language = 'fi' | 'sv' | 'en';

export type AmmatillinenTutkinnonOsa = IAmmatillisenTutkinnonOsa | IYTO;

export type TutkinnonOsanOsaAlue = {
  nimi: Kielistetty;
  laajuus?: number;
  arvosana?: string;
};
