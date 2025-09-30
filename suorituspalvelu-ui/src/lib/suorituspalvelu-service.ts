import { isEmpty, isNullish, omitBy } from 'remeda';
import { configPromise } from './configuration';
import { client } from './http-client';
import type {
  IOppijanHakuSuccessResponse,
  IOppijanTiedotSuccessResponse,
  IOppilaitosSuccessResponse,
} from '@/types/backend';
import type { KayttajaTiedot } from '@/types/ui-types';

export type OppijatSearchParams = {
  hakusana?: string;
  oppilaitos?: string;
  vuosi?: string;
  luokka?: string;
};

export const cleanSearchParams = (params: OppijatSearchParams) => {
  return omitBy(params, (value) => isEmpty(value) || value === '');
};

export const searchOppijat = async (params: OppijatSearchParams) => {
  const cleanParams = cleanSearchParams(params);
  if (isEmpty(cleanParams)) {
    return { oppijat: [] };
  }

  const config = await configPromise;
  const urlSearch = new URLSearchParams(omitBy(params, isNullish));

  const url = `${config.routes.suorituspalvelu.oppijatSearchUrl}?${urlSearch.toString()}`;

  const res = await client.get<IOppijanHakuSuccessResponse>(url);
  return res.data;
};

export const getOppija = async (oppijaNumero: string) => {
  const config = await configPromise;

  const res = await client.get<IOppijanTiedotSuccessResponse>(
    `${config.routes.suorituspalvelu.oppijanTiedotUrl}/${oppijaNumero}`,
  );

  return res.data;
};

export const getOppilaitokset = async () => {
  const config = await configPromise;

  const res = await client.get<IOppilaitosSuccessResponse>(
    config.routes.suorituspalvelu.oppilaitoksetUrl,
  );
  return res.data;
};

export const getKayttaja = async () => {
  const config = await configPromise;

  const res = await client.get<KayttajaTiedot>(
    config.routes.suorituspalvelu.kayttajanTiedotUrl,
  );
  return res.data;
};

export const getAsiointiKieli = async () => {
  const kayttaja = await getKayttaja();
  return kayttaja.asiointiKieli;
};
