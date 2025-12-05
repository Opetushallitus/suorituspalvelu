import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsSearchOppilaitoksenOppijat } from '@/lib/suorituspalvelu-queries';
import type { OppijatSearchParams } from '@/lib/suorituspalvelu-service';
import { isEmpty, isNullish, omitBy } from 'remeda';
import { useSearchParams, type NavigateOptions } from 'react-router';
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
  const [searchParams, setSearchParams] = useSearchParams();
  const oppilaitos = searchParams.get('oppilaitos');
  const vuosi = searchParams.get('vuosi');
  const luokka = searchParams.get('luokka');
  const tunniste = searchParams.get('tunniste');

  return useMemo(
    () => ({
      setSearchParams: (
        params: OppijatSearchParams,
        options?: NavigateOptions,
      ) => {
        setSearchParams(
          (prev) => {
            const next = new URLSearchParams(prev);
            Object.entries(params).forEach(([key, value]) => {
              if (isNullish(value) || value === '') {
                next.delete(key);
              } else {
                next.set(key, value);
              }
            });
            return next;
          },
          { replace: false, ...options },
        );
      },
      tunniste,
      oppilaitos,
      luokka,
      vuosi,
      hasValidSearchParams: oppilaitos !== null && vuosi !== null,
    }),
    [tunniste, oppilaitos, luokka, vuosi, setSearchParams],
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
