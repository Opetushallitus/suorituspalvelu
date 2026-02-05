import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsSearchOppilaitoksenOppijat } from '@/lib/suorituspalvelu-queries';
import type { BackendOppijatSearchParams } from '@/lib/suorituspalvelu-service';
import { isNonNullish, isNullish, pickBy, sortBy } from 'remeda';
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
  type NavigateOptions,
} from 'react-router';
import { useMemo } from 'react';
import type { SearchNavigationState } from '@/types/navigation';
import { setOppijaNumeroInPath } from '@/lib/navigationPathUtils';

type OppijatSearchParams = BackendOppijatSearchParams & {
  suodatus?: string;
};

type SetSearchParamsOptions = NavigateOptions & {
  resetHenkilo?: boolean;
};

export function useOppilaitoksenOppijatSearchParamsState() {
  const [searchParams] = useSearchParams();
  const oppilaitos = searchParams.get('oppilaitos');
  const vuosi = searchParams.get('vuosi');
  const luokka = searchParams.get('luokka');

  const location = useLocation();
  const navigate = useNavigate();
  const { oppijaNumero } = useParams();
  const locationState = location.state as SearchNavigationState;
  const tarkastusSearchTerm = locationState?.tarkastusSearchTerm;

  return useMemo(
    () => ({
      setSearchParams: (
        params: OppijatSearchParams,
        options?: SetSearchParamsOptions,
      ) => {
        const { suodatus, ...rest } = params;
        const { resetHenkilo, ...navOptions } = options ?? {};
        const newState = { ...locationState, tarkastusSearchTerm: suodatus };

        const newSearch = new URLSearchParams(searchParams);
        Object.entries(rest).forEach(([key, value]) => {
          if (isNullish(value) || value === '') {
            newSearch.delete(key);
          } else {
            newSearch.set(key, value);
          }
        });

        const newPathname =
          resetHenkilo && oppijaNumero
            ? setOppijaNumeroInPath(location.pathname, null)
            : location.pathname;

        navigate(
          { pathname: newPathname, search: newSearch.toString() },
          { replace: false, ...navOptions, state: newState },
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
      searchParams,
      navigate,
      location.pathname,
      oppijaNumero,
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
    let filtered = result.data;
    if (oppilaitos && vuosi && suodatus) {
      filtered = result.data.filter((oppija) => {
        const lowercaseSuodatus = suodatus.toLowerCase() ?? '';
        return (
          oppija.etunimet?.toLocaleLowerCase().includes(lowercaseSuodatus) ||
          oppija.sukunimi?.toLocaleLowerCase().includes(lowercaseSuodatus) ||
          oppija?.hetu?.toLowerCase()?.includes(lowercaseSuodatus)
        );
      });
    }
    return sortBy(filtered ?? [], (oppija) => oppija.sukunimi ?? '');
  }, [oppilaitos, vuosi, suodatus, result.data]);

  return { ...result, data, totalCount: result.data.length };
};
