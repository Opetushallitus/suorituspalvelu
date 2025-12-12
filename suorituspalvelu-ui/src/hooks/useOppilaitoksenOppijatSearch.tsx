import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsSearchOppilaitoksenOppijat } from '@/lib/suorituspalvelu-queries';
import type { BackendOppijatSearchParams } from '@/lib/suorituspalvelu-service';
import { isNonNullish, isNullish, pickBy } from 'remeda';
import {
  useLocation,
  useSearchParams,
  type NavigateOptions,
} from 'react-router';
import { useMemo } from 'react';
import type { SearchNavigationState } from '@/types/navigation';

type OppijatSearchParams = BackendOppijatSearchParams & {
  suodatus?: string;
};

export function useOppilaitoksenOppijatSearchParamsState() {
  const [searchParams, setSearchParams] = useSearchParams();
  const oppilaitos = searchParams.get('oppilaitos');
  const vuosi = searchParams.get('vuosi');
  const luokka = searchParams.get('luokka');

  const location = useLocation();
  const locationState = location.state as SearchNavigationState;
  const tarkastusSearchTerm = locationState?.tarkastusSearchTerm;

  return useMemo(
    () => ({
      setSearchParams: (
        params: OppijatSearchParams,
        options?: NavigateOptions,
      ) => {
        const { suodatus, ...rest } = params;
        setSearchParams(
          (prev) => {
            const next = new URLSearchParams(prev);
            Object.entries(rest).forEach(([key, value]) => {
              if (isNullish(value) || value === '') {
                next.delete(key);
              } else {
                next.set(key, value);
              }
            });
            return next;
          },
          {
            replace: false,
            ...options,
            state: { ...locationState, tarkastusSearchTerm: suodatus },
          }, // Use push to add to history
        );
      },
      searchParams: pickBy(
        { suodatus: tarkastusSearchTerm, oppilaitos, luokka, vuosi },
        isNonNullish,
      ),
      hasValidSearchParams: oppilaitos !== null && vuosi !== null,
    }),
    [
      locationState,
      setSearchParams,
      tarkastusSearchTerm,
      oppilaitos,
      luokka,
      vuosi,
    ],
  );
}

export const useOppilaitoksenOppijatSearchResult = () => {
  const { searchParams } = useOppilaitoksenOppijatSearchParamsState();
  const { oppilaitos, vuosi, luokka, suodatus } = searchParams;

  const result = useApiSuspenseQuery(
    queryOptionsSearchOppilaitoksenOppijat({
      oppilaitos,
      vuosi,
      luokka,
    }),
  );

  const data = useMemo(() => {
    if (oppilaitos && vuosi && suodatus) {
      return result.data.filter((oppija) => {
        const lowercaseSuodatus = suodatus.toLowerCase() ?? '';
        return (
          oppija.etunimet?.toLocaleLowerCase().includes(lowercaseSuodatus) ||
          oppija.sukunimi?.toLocaleLowerCase().includes(lowercaseSuodatus) ||
          oppija?.hetu?.toLowerCase()?.includes(lowercaseSuodatus)
        );
      });
    } else {
      return result.data;
    }
  }, [oppilaitos, vuosi, suodatus, result.data]);

  return { ...result, data, totalCount: result.data.length };
};
