import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsSearchOppilaitoksenOppijat } from '@/lib/suorituspalvelu-queries';
import type { BackendOppijatSearchParams } from '@/lib/suorituspalvelu-service';
import { isEmpty, isNullish, omitBy } from 'remeda';
import { useSearchParams, type NavigateOptions } from 'react-router';
import { useMemo } from 'react';

export const useOppijatSearchURLParams = () => {
  const params = useOppijatSearchParamsState();
  return omitBy(
    {
      suodatus: params.suodatus ?? undefined,
      oppilaitos: params.oppilaitos ?? undefined,
      luokka: params.luokka ?? undefined,
      vuosi: params.vuosi ?? undefined,
    },
    isEmpty,
  );
};

type OppijatSearchParams = BackendOppijatSearchParams & {
  suodatus?: string;
};

export const useOppijatSearchParamsState = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const oppilaitos = searchParams.get('oppilaitos');
  const vuosi = searchParams.get('vuosi');
  const luokka = searchParams.get('luokka');
  const suodatus = searchParams.get('suodatus');

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
          { replace: false, ...options }, // Use push to add to history
        );
      },
      suodatus,
      oppilaitos,
      luokka,
      vuosi,
      hasValidSearchParams: oppilaitos !== null && vuosi !== null,
    }),
    [suodatus, oppilaitos, luokka, vuosi],
  );
};

export const useOppilaitoksenOppijatSearch = () => {
  const params = useOppijatSearchParamsState();

  const urlParams = useOppijatSearchURLParams();

  const result = useApiSuspenseQuery(
    queryOptionsSearchOppilaitoksenOppijat(urlParams),
  );

  const data = useMemo(() => {
    const { oppilaitos, vuosi, suodatus } = params;
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
  }, [params]);

  return { ...result, data, totalCount: result.data.length };
};
