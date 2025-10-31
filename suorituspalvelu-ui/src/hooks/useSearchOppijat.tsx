import { DEFAULT_NUQS_OPTIONS } from '@/lib/common';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsSearchOppijat } from '@/lib/suorituspalvelu-queries';
import type { OppijatSearchParams } from '@/lib/suorituspalvelu-service';
import { useQueryState } from 'nuqs';
import { isEmpty, isNullish, omitBy, values } from 'remeda';

export const useOppijatSearchURLParams = () => {
  const params = useOppijatSearchParamsState();
  return omitBy(
    {
      tunniste: params.tunniste ?? undefined,
      oppilaitos: params.oppilaitos ?? undefined,
      luokka: params.luokka ?? undefined,
      vuosi: params.vuosi ?? undefined,
    },
    isEmpty,
  );
};

export const useOppijatSearchParamsState = () => {
  const [tunniste, setTunniste] = useQueryState(
    'tunniste',
    DEFAULT_NUQS_OPTIONS,
  );
  const [oppilaitos, setOppilaitos] = useQueryState(
    'oppilaitos',
    DEFAULT_NUQS_OPTIONS,
  );
  const [luokka, setLuokka] = useQueryState('luokka', DEFAULT_NUQS_OPTIONS);
  const [vuosi, setVuosi] = useQueryState('vuosi', DEFAULT_NUQS_OPTIONS);

  return {
    setTunniste,
    tunniste,
    oppilaitos,
    setOppilaitos,
    luokka,
    setLuokka,
    vuosi,
    setVuosi,
    hasEmptySearchParams: isEmptySearchParams({
      tunniste,
      oppilaitos,
      luokka,
      vuosi,
    }),
  };
};

const isEmptySearchParams = (searchParams: OppijatSearchParams) => {
  return values(searchParams).every(
    (value) => isNullish(value) || isEmpty(value) || value === '',
  );
};

export const useOppijatSearch = () => {
  const params = useOppijatSearchParamsState();

  const urlParams = useOppijatSearchURLParams();

  const result = useApiSuspenseQuery(queryOptionsSearchOppijat(urlParams));

  return {
    ...params,
    result,
  };
};
