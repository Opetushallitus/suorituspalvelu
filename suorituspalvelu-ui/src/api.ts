import { isEmpty, isNullish, omitBy } from 'remeda';
import { configPromise } from './configuration';
import { client } from './http-client';

export type SearchParams = {
  oppija?: string;
  oppilaitos?: string;
  vuosi?: string;
  luokka?: string;
};

type SearchResult = {
  oppijat: Array<{
    oppijaNumero: string;
    hetu?: string;
    nimi: string;
  }>;
};

export const cleanSearchParams = (params: SearchParams) => {
  return omitBy(params, (value) => isEmpty(value) || value === '');
};

export const searchOppijat = async (params: SearchParams) => {
  const cleanParams = cleanSearchParams(params);
  if (isEmpty(cleanParams)) {
    return { oppijat: [] } as SearchResult;
  }

  const config = await configPromise;
  const urlSearch = new URLSearchParams(omitBy(params, isNullish));

  const url = `${config.routes.suorituspalvelu.oppijatSearchUrl}?${urlSearch.toString()}`;

  const res = await client.get<SearchResult>(url);
  return res.data;
};

export type Opiskeluoikeus = {
  tutkinto: string;
  oppilaitos: {
    oid: string;
    nimi: string;
  };
  voimassaolonAlku?: string;
  voimassaolonLoppu?: string;
};

export type OppijaResponse = {
  opiskeluoikeudet: Array<{
    tutkinto: string;
    oppilaitos: {
      oid: string;
      nimi: string;
    };
    voimassaolonAlku?: string;
    voimassaolonLoppu?: string;
  }>;
};

export const getOppija = async (oppijaNumero: string) => {
  const config = await configPromise;

  const res = await client.get<OppijaResponse>(
    `${config.routes.suorituspalvelu.oppijanTiedotUrl}/${oppijaNumero}`,
  );

  return res.data;
};

type OppilaitoksetResponse = {
  oppilaitokset: Array<{
    oid: string;
    nimi: string;
  }>;
};

export const getOppilaitokset = async () => {
  const config = await configPromise;

  const res = await client.get<OppilaitoksetResponse>(
    `${config.routes.suorituspalvelu.oppilaitoksetUrl}`,
  );
  return res.data;
};
