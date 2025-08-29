import {
  type IYOKoe,
  type SuoritusTila,
  type IPerusopetuksenOppiaine,
  type ILukionOppiaine,
  type IKKSuoritus,
  type ILukionOppimaara,
  type ILukionOppiaineenOppimaara,
  type IYOTutkinto,
  type IDIATutkinto,
  type IDIAVastaavuusTodistus,
  type IEBTutkinto,
  type IIBTutkinto,
  type IPreIB,
  type IAmmatillinentutkinto,
  type IAmmattitutkinto,
  type IErikoisammattitutkinto,
  type IVapaaSivistystyoKoulutus,
  type ITuva,
  type IPerusopetuksenOppiaineenOppimaara,
  type IPerusopetuksenOppimaara78Luokkalaiset,
  type INuortenPerusopetuksenOppiaineenOppimaara,
  type IAikuistenPerusopetuksenOppimaara,
  type IPerusopetuksenOppimaara,
  type IOppijanTiedotSuccessResponse,
  type IUIOpiskeluoikeus,
  type ITelma,
  type IEBOppiaine,
  type IIBOppiaine,
  type IAmmatillisenTutkinnonOsa,
  type IYTO,
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
};

export type EBSuoritus = IEBTutkinto & {
  koulutustyyppi: 'eb';
};

export type IBSuoritus = IIBTutkinto & {
  koulutustyyppi: 'ib';
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
    ) & { koulutustyyppi: 'lukio' });

export type AmmatillinenSuoritus = (
  | IAmmatillinentutkinto
  | IAmmattitutkinto
  | IErikoisammattitutkinto
) & { koulutustyyppi: 'ammatillinen' };

export type TUVASuoritus = ITuva & {
  koulutustyyppi: 'tuva';
};
export type TelmaSuoritus = ITelma & {
  koulutustyyppi: 'telma';
};

export type VapaaSivistystyoSuoritus = IVapaaSivistystyoKoulutus & {
  koulutustyyppi: 'vapaa-sivistystyo';
};

export type PerusopetusSuoritus = (
  | IPerusopetuksenOppimaara
  | IPerusopetuksenOppiaineenOppimaara
  | IPerusopetuksenOppimaara78Luokkalaiset
  | INuortenPerusopetuksenOppiaineenOppimaara
  | IAikuistenPerusopetuksenOppimaara
) & { koulutustyyppi: 'perusopetus' };

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

export type KayttajaTiedot = {
  asiointiKieli: Language;
};
