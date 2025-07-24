import { useApiSuspenseQuery } from '@/http-client';
import { queryOptionsSearchOppijat } from '@/queries';
import { useQueryState } from 'nuqs';
import { isEmpty, omitBy } from 'remeda';

export const useOppijatSearchURLParams = () => {
  const params = useOppijatSearchParamsState();
  return omitBy(
    {
      oppija: params.oppijaSearchTerm ?? undefined,
      oppilaitos: params.oppilaitos ?? undefined,
      luokka: params.luokka ?? undefined,
      vuosi: params.vuosi ?? undefined,
    },
    isEmpty,
  );
};

export const useOppijatSearchParamsState = () => {
  const [oppijaSearchTerm, setOppijaSearchTerm] = useQueryState('oppija');
  const [oppilaitos, setOppilaitos] = useQueryState('oppilaitos');
  const [luokka, setLuokka] = useQueryState('luokka');
  const [vuosi, setVuosi] = useQueryState('vuosi');

  return {
    oppijaSearchTerm,
    setOppijaSearchTerm,
    oppilaitos,
    setOppilaitos,
    luokka,
    setLuokka,
    vuosi,
    setVuosi,
  };
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
