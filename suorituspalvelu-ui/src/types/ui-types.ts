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
  IEBTutkintoUI,
  IIBTutkinto,
  IPreIB,
  IAmmatillinentutkinto,
  IAmmattitutkinto,
  IErikoisammattitutkinto,
  IVapaaSivistystyoKoulutus,
  ITuva,
  IPerusopetuksenOppiaineenOppimaaratUI,
  IPerusopetuksenOppimaara78Luokkalaiset,
  IPerusopetuksenOppimaaraUI,
  IOppijanTiedotSuccessResponse,
  IUIOpiskeluoikeus,
  ITelma,
  IEBOppiaine,
  IIBOppiaine,
  IAmmatillisenTutkinnonOsa,
  IYTO,
  ILuoSuoritusDropdownDataSuccessResponse,
  IYksilollistaminen,
  IPoistaSuoritusFailureResponse,
  ILuoPerusopetuksenOppimaaraFailureResponse,
  IAvainArvoContainerUI,
  IYliajo,
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
  koodi?: string;
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

export type EBSuoritus = IEBTutkintoUI & {
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
