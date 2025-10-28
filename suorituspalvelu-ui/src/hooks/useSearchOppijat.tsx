import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsSearchOppijat } from '@/lib/suorituspalvelu-queries';
import type { OppijatSearchParams } from '@/lib/suorituspalvelu-service';
import { isEmpty, isNullish, omitBy, values } from 'remeda';
import { useQueryParam } from './useQueryParam';

export const useOppijatSearchURLParams = () => {
  const params = useOppijatSearchParamsState();
  return omitBy(
    {
      hakusana: params.hakusana ?? undefined,
      oppilaitos: params.oppilaitos ?? undefined,
      luokka: params.luokka ?? undefined,
      vuosi: params.vuosi ?? undefined,
    },
    isEmpty,
  );
};

export const useOppijatSearchParamsState = () => {
  const [hakusana, setHakusana] = useQueryParam('hakusana');
  const [oppilaitos, setOppilaitos] = useQueryParam('oppilaitos');
  const [luokka, setLuokka] = useQueryParam('luokka');
  const [vuosi, setVuosi] = useQueryParam('vuosi');

  return {
    setHakusana,
    hakusana,
    oppilaitos,
    setOppilaitos,
    luokka,
    setLuokka,
    vuosi,
    setVuosi,
    hasEmptySearchParams: isEmptySearchParams({
      hakusana: hakusana ?? undefined,
      oppilaitos: oppilaitos ?? undefined,
      luokka: luokka ?? undefined,
      vuosi: vuosi ?? undefined,
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
