import type {
  IYOKoe,
  SuoritusTila,
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
  ILuoSuoritusDropdownDataSuccessResponse,
} from './backend';

export type SuorituksenTila = SuoritusTila;

export type SuorituksenPerustiedot = {
  oppilaitos: {
    nimi: Kielistetty;
    oid: string;
  };
  tila: SuorituksenTila;
  suorituskieli?: string;
  valmistumispaiva?: string;
};

export type SuoritusOtsikkoTiedot = {
  nimi: Kielistetty;
  tila: SuorituksenTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
};

export type Kielistetty = {
  fi?: string;
  sv?: string;
  en?: string;
};

export type Opiskeluoikeus = IUIOpiskeluoikeus;

export type OppijanTiedot = IOppijanTiedotSuccessResponse;

export type YOKoe = IYOKoe;

export type PerusopetuksenOppiaine = {
  tunniste: string;
  nimi: Kielistetty;
  kieli?: string;
  arvosana?: string;
  valinnaisetArvosanat?: Array<string>;
};

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

type PerusopetuksenOppimaara = Omit<IPerusopetuksenOppimaara, 'oppiaineet'> & {
  oppiaineet: Array<PerusopetuksenOppiaine>;
};

type PerusopetuksenOppiaineenOppimaara = Omit<
  IPerusopetuksenOppiaineenOppimaara,
  'oppiaineet'
> & {
  oppiaineet: Array<PerusopetuksenOppiaine>;
};

type AikuistenPerusopetuksenOppimaara = Omit<
  IAikuistenPerusopetuksenOppimaara,
  'oppiaineet'
> & {
  oppiaineet: Array<PerusopetuksenOppiaine>;
};

type NuortenPerusopetuksenOppiaineenOppimaara = Omit<
  INuortenPerusopetuksenOppiaineenOppimaara,
  'oppiaineet'
> & {
  oppiaineet: Array<PerusopetuksenOppiaine>;
};

export type PerusopetusSuoritus = (
  | PerusopetuksenOppimaara
  | PerusopetuksenOppiaineenOppimaara
  | IPerusopetuksenOppimaara78Luokkalaiset
  | NuortenPerusopetuksenOppiaineenOppimaara
  | AikuistenPerusopetuksenOppimaara
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

export type Suoritusvaihtoehdot = ILuoSuoritusDropdownDataSuccessResponse;

export type OppiaineFields = {
  koodi: string;
  kieli?: string;
  arvosana: string;
  valinnainen: boolean;
};

export type SuoritusFields = {
  oppijaOid: string;
  oppilaitosOid: string;
  tila?: string;
  tyyppi: string;
  valmistumispaiva?: Date;
  suorituskieli: string;
  koulusivistyskieli?: string;
  yksilollistetty: string;
  oppiaineet: Array<OppiaineFields>;
};
