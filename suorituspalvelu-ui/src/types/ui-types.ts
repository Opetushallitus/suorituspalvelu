import type {
  IYOKoe,
  SuoritusTilaUI,
  ILukionOppiaine,
  IKKSuoritusUI,
  ILukionOppimaaraUI,
  ILukionOppiaineenOppimaara,
  IYOTutkinto,
  IDIAVastaavuusTodistusUI,
  IEBTutkintoUI,
  IIBTutkintoUI,
  IAmmatillinentutkinto,
  IAmmattitutkinto,
  IErikoisammattitutkinto,
  IVapaaSivistystyoKoulutus,
  ITuvaUI,
  IPerusopetuksenOppiaineenOppimaaratUI,
  IPerusopetuksenOppimaara78Luokkalaiset,
  IPerusopetuksenOppimaaraUI,
  IOppijanTiedotSuccessResponse,
  IOpiskeluoikeusUI,
  ITelma,
  IEBOppiaineUI,
  IIBOppiaineUI,
  IAmmatillisenTutkinnonOsa,
  IYTO,
  ILuoSuoritusDropdownDataSuccessResponse,
  IYksilollistaminen,
  IPoistaSuoritusFailureResponse,
  ILuoPerusopetuksenOppimaaraFailureResponse,
  IAvainArvoContainerUI,
  IYliajo,
  IOppijanValintaDataSuccessResponse,
  IDIATutkintoUI,
} from './backend';

export type SuorituksenTila = SuoritusTilaUI;

export type SuorituksenPerustiedot = {
  nimi?: Kielistetty;
  oppilaitos: {
    nimi: Kielistetty;
    oid: string;
  };
  suorituskieli?: Kielistetty | string;
  tila: SuorituksenTila;
  aloituspaiva?: string;
  valmistumispaiva?: string;
};

export type Kielistetty = {
  fi?: string;
  sv?: string;
  en?: string;
};

export type Opiskeluoikeus = IOpiskeluoikeusUI;

export type OppijanTiedot = IOppijanTiedotSuccessResponse;

export type YOKoe = IYOKoe;

export type PerusopetuksenOppiaine = {
  tunniste: string;
  koodi?: string;
  nimi: Kielistetty;
  kieli?: string;
  arvosana?: string;
  valinnaisetArvosanat?: Array<string>;
};

export type LukionOppiaine = ILukionOppiaine | IEBOppiaineUI;

export type IBOppiaine = IIBOppiaineUI;

export type EBOppiaine = IEBOppiaineUI;

export type KorkeakouluSuoritus = IKKSuoritusUI & {
  koulutustyyppi: 'korkeakoulutus';
};

export type EBSuoritus = IEBTutkintoUI & {
  koulutustyyppi: 'eb';
};

export type IBSuoritus = IIBTutkintoUI & {
  koulutustyyppi: 'ib';
};

export type LukioSuoritus =
  | EBSuoritus
  | IBSuoritus
  | ((
      | IYOTutkinto
      | ILukionOppimaaraUI
      | ILukionOppiaineenOppimaara
      | IDIATutkintoUI
      | IDIAVastaavuusTodistusUI
    ) & { koulutustyyppi: 'lukio' });

export type AmmatillinenSuoritus = (
  | IAmmatillinentutkinto
  | IAmmattitutkinto
  | IErikoisammattitutkinto
) & { koulutustyyppi: 'ammatillinen' };

export type TUVASuoritus = ITuvaUI & {
  koulutustyyppi: 'tuva';
};
export type TelmaSuoritus = ITelma & {
  koulutustyyppi: 'telma';
};

export type VapaaSivistystyoSuoritus = IVapaaSivistystyoKoulutus & {
  koulutustyyppi: 'vapaa-sivistystyo';
};

export type PerusopetuksenOppimaara = Omit<
  IPerusopetuksenOppimaaraUI,
  'oppiaineet'
> & {
  oppiaineet: Array<PerusopetuksenOppiaine>;
  suoritustyyppi: 'perusopetuksenoppimaara';
  koulutustyyppi: 'perusopetus';
  isEditable: boolean;
};

export type PerusopetuksenOppiaineenOppimaarat = Omit<
  IPerusopetuksenOppiaineenOppimaaratUI,
  'oppiaineet'
> & {
  oppiaineet: Array<PerusopetuksenOppiaine>;
  suoritustyyppi: 'perusopetuksenoppiaineenoppimaara';
  koulutustyyppi: 'perusopetus';
  isEditable: boolean;
};

export type PerusopetusSuoritus = (
  | PerusopetuksenOppimaara
  | PerusopetuksenOppiaineenOppimaarat
  | IPerusopetuksenOppimaara78Luokkalaiset
) & { koulutustyyppi: 'perusopetus'; isEditable: boolean };

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

export type Suoritusvaihtoehdot = ILuoSuoritusDropdownDataSuccessResponse;

export type PerusopetusOppiaineFields = {
  koodi: string;
  kieli?: string;
  arvosana?: string;
  valinnaisetArvosanat?: Array<string>;
};

export type SuoritusFields = {
  versioTunniste: string;
  oppijaOid: string;
  oppilaitosOid: string;
  tila: string;
  tyyppi: string;
  valmistumispaiva?: Date;
  suorituskieli: string;
  luokka?: string;
  yksilollistetty: string;
  oppiaineet: Array<PerusopetusOppiaineFields>;
};

export type Yksilollistaminen = IYksilollistaminen;

export type GenericBackendError = IPoistaSuoritusFailureResponse;

export type AvainArvo = IAvainArvoContainerUI;

export const isGenericBackendErrorResponse = (
  error: unknown,
): error is GenericBackendError => {
  return (
    typeof error === 'object' &&
    error != null &&
    'virheAvaimet' in error &&
    Array.isArray(error.virheAvaimet)
  );
};

export const isPerusopetusOppimaaraBackendErrorResponse = (
  body: unknown,
): body is ILuoPerusopetuksenOppimaaraFailureResponse => {
  return (
    typeof body === 'object' &&
    body != null &&
    'yleisetVirheAvaimet' in body &&
    Array.isArray(body.yleisetVirheAvaimet)
  );
};

export type SelectOption = {
  label: string;
  value: string;
};

export type Yliajo = IYliajo;

export type YliajoParams = {
  avain: string;
  arvo: string;
  selite: string;
};

export type Henkilo = {
  etunimet?: string;
  sukunimi?: string;
};

export type ValintaData = IOppijanValintaDataSuccessResponse;
