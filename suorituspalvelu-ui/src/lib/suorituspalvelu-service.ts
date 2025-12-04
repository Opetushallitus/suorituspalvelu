import { isEmptyish, omitBy } from 'remeda';
import { configPromise } from './configuration';
import { client, FetchError } from './http-client';
import type {
  IKayttajaSuccessResponse,
  ILuokatSuccessResponse,
  ILuoSuoritusDropdownDataSuccessResponse,
  IOppijanHakuSuccessResponse,
  IOppijanHautSuccessResponse,
  IOppijanTiedotSuccessResponse,
  IOppijanValintaDataSuccessResponse,
  IOppilaitosSuccessResponse,
  IVuodetSuccessResponse,
  IYliajoTallennusContainer,
} from '@/types/backend';
import type { SuoritusFields } from '@/types/ui-types';
import { format } from 'date-fns';
import { toFinnishDate } from './time-utils';
import { isHenkiloOid, isHenkilotunnus } from './common';

export type BackendOppijatSearchParams = {
  oppilaitos?: string | null;
  vuosi?: string | null;
  luokka?: string | null;
};

const isNotFoundError = (error: unknown) => {
  return (
    error instanceof FetchError && [404, 410].includes(error?.response?.status)
  );
};

export const cleanSearchParams = (params: BackendOppijatSearchParams) => {
  return omitBy(params, (value) => isEmptyish(value));
};

export const nullWhenErrorMatches = async <T>(
  promise: Promise<T>,
  matcher: (error: unknown) => boolean,
): Promise<T | null> => {
  try {
    return await promise;
  } catch (e: unknown) {
    if (matcher(e)) {
      return Promise.resolve(null);
    }
    throw e;
  }
};

export const searchOppilaitoksenOppijat = async (
  params: BackendOppijatSearchParams,
) => {
  const cleanParams = cleanSearchParams(params);
  const { oppilaitos, vuosi } = cleanParams;

  if (!oppilaitos || !vuosi) {
    return [];
  }

  const config = await configPromise;
  const urlSearch = new URLSearchParams(cleanParams);

  const url = `${config.routes.suorituspalvelu.oppilaitoksenOppijatSearchUrl}?${urlSearch.toString()}`;

  const res = await client.get<IOppijanHakuSuccessResponse>(url);
  return res.data?.oppijat ?? [];
};

export const getOppija = async (tunniste?: string) => {
  const config = await configPromise;

  if (
    isEmptyish(tunniste) ||
    (!isHenkiloOid(tunniste) && !isHenkilotunnus(tunniste))
  ) {
    return null;
  }

  return nullWhenErrorMatches(
    client
      .post<IOppijanTiedotSuccessResponse>(
        config.routes.suorituspalvelu.oppijanTiedotUrl,
        { tunniste },
      )
      .then((res) => res.data),
    isNotFoundError,
  );
};

export const getOppilaitokset = async () => {
  const config = await configPromise;

  return client
    .get<IOppilaitosSuccessResponse>(
      config.routes.suorituspalvelu.oppilaitoksetUrl,
    )
    .then((res) => res.data);
};

export const getKayttaja = async () => {
  const config = await configPromise;

  const res = await client.get<IKayttajaSuccessResponse>(
    config.routes.suorituspalvelu.kayttajanTiedotUrl,
  );
  return res.data;
};

export const getSuoritusvaihtoehdot = async () => {
  const config = await configPromise;

  const res = await client.get<ILuoSuoritusDropdownDataSuccessResponse>(
    config.routes.suorituspalvelu.suoritusvaihtoehdotUrl,
  );
  return res.data;
};

export const getSuorituksenOppilaitosVaihtoehdot = async () => {
  const config = await configPromise;

  const res = await client.get<IOppilaitosSuccessResponse>(
    config.routes.suorituspalvelu.oppilaitosvaihtoehdotUrl,
  );
  return res.data?.oppilaitokset ?? [];
};

export const getValintadata = async ({
  oppijaNumero,
  hakuOid,
}: {
  oppijaNumero: string;
  hakuOid?: string;
}) => {
  const config = await configPromise;

  const searchParams = new URLSearchParams();
  searchParams.set('oppijaNumero', oppijaNumero);
  if (hakuOid) {
    searchParams.set('hakuOid', hakuOid);
  }

  const res = await client.get<IOppijanValintaDataSuccessResponse>(
    `${config.routes.suorituspalvelu.valintadataUrl}?${searchParams.toString()}`,
  );
  return res.data;
};

// Perusopetuksen oppimäärän ja oppiaineen oppimäärän tallentaminen
// TODO: Oppiaineen oppimäärän tallentaminen puuttuu vielä
export const saveSuoritus = async (
  suoritusFields: Omit<SuoritusFields, 'versioTunniste'>,
): Promise<void> => {
  const config = await configPromise;

  const postData: Record<string, unknown> = {
    tila: suoritusFields.tila,
    oppijaOid: suoritusFields.oppijaOid,
    oppilaitosOid: suoritusFields.oppilaitosOid,
    suorituskieli: suoritusFields.suorituskieli,
    yksilollistetty: parseInt(suoritusFields.yksilollistetty, 10),
    luokka: suoritusFields.luokka,
    valmistumispaiva: suoritusFields.valmistumispaiva
      ? format(toFinnishDate(suoritusFields.valmistumispaiva), 'yyyy-MM-dd')
      : undefined,
  };

  const oppiaineet = suoritusFields.oppiaineet?.flatMap((oa) => {
    const pakolliset = oa.arvosana
      ? [
          {
            koodi: oa.koodi,
            kieli: oa.kieli,
            arvosana: parseInt(oa.arvosana, 10),
            valinnainen: false,
          },
        ]
      : [];

    const valinnaiset = (oa.valinnaisetArvosanat ?? []).map((arv) => ({
      koodi: oa.koodi,
      kieli: oa.kieli,
      arvosana: parseInt(arv, 10),
      valinnainen: true,
    }));

    return [...pakolliset, ...valinnaiset].filter(
      (aine) => !isNaN(aine.arvosana),
    );
  });

  let url: string | null = null;
  if (suoritusFields.tyyppi === 'perusopetuksenoppimaara') {
    url = config.routes.suorituspalvelu.perusopetuksenOppimaaratUrl;
    postData.oppiaineet = oppiaineet;
  } else if (suoritusFields.tyyppi === 'perusopetuksenoppiaineenoppimaara') {
    url = config.routes.suorituspalvelu.perusopetuksenOppiaineenOppimaaratUrl;
    postData.oppiaine = oppiaineet?.[0];
  } else {
    throw new Error(`Tuntematon suoritustyyppi: ${suoritusFields.tyyppi}`);
  }

  await client.post(url, postData);
};

export const deleteSuoritus = async (versioTunniste: string) => {
  const config = await configPromise;
  const url =
    config.routes.suorituspalvelu.versioDeleteUrl + '/' + versioTunniste;
  const res = await client.delete(url);
  return res.data;
};

export const saveYliajot = async ({
  henkiloOid,
  hakuOid,
  yliajot,
}: IYliajoTallennusContainer) => {
  const config = await configPromise;

  const res = await client.post(
    config.routes.suorituspalvelu.tallennaYliajotUrl,
    {
      henkiloOid,
      hakuOid,
      yliajot,
    },
  );
  return res.data;
};

export const deleteYliajo = async ({
  henkiloOid,
  hakuOid,
  avain,
}: {
  henkiloOid: string;
  hakuOid: string;
  avain: string;
}) => {
  const config = await configPromise;

  const url = new URL(config.routes.suorituspalvelu.poistaYliajoUrl);
  url.searchParams.set('oppijaNumero', henkiloOid);
  url.searchParams.set('hakuOid', hakuOid);
  url.searchParams.set('avain', avain);

  const res = await client.delete(url.toString());
  return res.data;
};

export const getOppijanHaut = async (oppijaOid: string) => {
  const config = await configPromise;

  const url = `${config.routes.suorituspalvelu.oppijanHautUrl}/${oppijaOid}`;

  const res = await client.get<IOppijanHautSuccessResponse>(url);
  return res.data?.haut ?? [];
};

export const getOppilaitosVuodet = async ({
  oppilaitosOid,
}: {
  oppilaitosOid?: string;
}) => {
  const config = await configPromise;

  const res = await client.get<IVuodetSuccessResponse>(
    `${config.routes.suorituspalvelu.vuodetUrl}/${oppilaitosOid}`,
  );
  return res.data?.vuodet ?? [];
};

export const getOppilaitosVuosiLuokat = async ({
  oppilaitosOid,
  vuosi,
}: {
  oppilaitosOid?: string;
  vuosi?: string;
}) => {
  const config = await configPromise;

  const res = await client.get<ILuokatSuccessResponse>(
    `${config.routes.suorituspalvelu.luokatUrl}/${oppilaitosOid}/${vuosi}`,
  );
  return res.data?.luokat ?? [];
};
