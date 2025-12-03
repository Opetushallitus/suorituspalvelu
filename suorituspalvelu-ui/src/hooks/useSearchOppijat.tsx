import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsSearchOppilaitoksenOppijat } from '@/lib/suorituspalvelu-queries';
import type { OppijatSearchParams } from '@/lib/suorituspalvelu-service';
import { isEmpty, isNullish, omitBy } from 'remeda';
import {
  useLocation,
  useNavigate,
  useSearchParams,
  type NavigateOptions,
} from 'react-router';
import { useMemo } from 'react';

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
  const [searchParams] = useSearchParams();
  const oppilaitos = searchParams.get('oppilaitos');
  const vuosi = searchParams.get('vuosi');
  const luokka = searchParams.get('luokka');
  const tunniste = searchParams.get('tunniste');

  const navigate = useNavigate();
  const location = useLocation();

  return useMemo(
    () => ({
      setSearchParams: (
        params: OppijatSearchParams,
        options?: NavigateOptions,
      ) => {
        let pathname = location.pathname;
        const locationSearch = new URLSearchParams(location.search);
        Object.entries(params).forEach(([key, value]) => {
          if (isNullish(value) || value === '') {
            locationSearch.delete(key);
          } else {
            locationSearch.set(key, value);
          }
        });
        //Jos tyhjennetään tunniste, poistetaan myös oppijanumero polusta
        if (params.tunniste === '' || params.tunniste === null) {
          const pathParts = location.pathname.split('/');
          pathParts.splice(2);
          pathname = pathParts.join('/');
        }
        navigate(
          { pathname, search: locationSearch.toString() },
          { replace: false, ...options },
        );
      },
      tunniste,
      oppilaitos,
      luokka,
      vuosi,
      hasValidSearchParams: oppilaitos !== null && vuosi !== null,
    }),
    [tunniste, oppilaitos, luokka, vuosi],
  );
};

export const useOppilaitoksenOppijatSearch = () => {
  const params = useOppijatSearchParamsState();

  const urlParams = useOppijatSearchURLParams();

  const result = useApiSuspenseQuery(
    queryOptionsSearchOppilaitoksenOppijat(urlParams),
  );

  const data = useMemo(() => {
    const { oppilaitos, vuosi, tunniste } = params;
    if (oppilaitos && vuosi && tunniste) {
      return result.data.filter((oppija) => {
        const lowercaseTunniste = tunniste.toLowerCase() ?? '';
        return (
          oppija.etunimet?.toLocaleLowerCase().includes(lowercaseTunniste) ||
          oppija.sukunimi?.toLocaleLowerCase().includes(lowercaseTunniste) ||
          oppija?.hetu?.toLowerCase()?.includes(lowercaseTunniste)
        );
      });
    } else {
      return result.data;
    }
  }, [params]);

  return { ...result, data, totalCount: result.data.length };
};
