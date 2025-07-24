import { queryOptionsSearchOppijat } from '@/queries';
import { useSuspenseQuery } from '@tanstack/react-query';
import { useQueryState } from 'nuqs';
import { isEmpty, omitBy } from 'remeda';

export const useURLParams = () => {
  const params = useSearchQueryParamsState();
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

export const useSearchQueryParamsState = () => {
  const [oppijaSearchTerm, setOppijaSearchTerm] = useQueryState('oppija');
  const [oppilaitos, setOppijaitos] = useQueryState('oppilaitos');
  const [luokka, setLuokka] = useQueryState('luokka');
  const [vuosi, setVuosi] = useQueryState('vuosi');

  return {
    oppijaSearchTerm,
    setOppijaSearchTerm,
    oppilaitos,
    setOppijaitos,
    luokka,
    setLuokka,
    vuosi,
    setVuosi,
  };
};

export const useSearchOppijat = () => {
  const params = useSearchQueryParamsState();

  const urlParams = useURLParams();

  const result = useSuspenseQuery(queryOptionsSearchOppijat(urlParams));

  return {
    ...params,
    result,
  };
};
