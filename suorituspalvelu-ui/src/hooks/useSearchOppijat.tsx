import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsSearchOppilaitoksenOppijat } from '@/lib/suorituspalvelu-queries';
import type { OppijatSearchParams } from '@/lib/suorituspalvelu-service';
import { isEmpty, isNullish, omitBy, values } from 'remeda';
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

  return {
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
    hasEmptySearchParams: isEmptySearchParams({
      tunniste: tunniste ?? undefined,
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
        return (
          oppija.etunimet
            ?.toLocaleLowerCase()
            .includes(tunniste.toLowerCase() ?? '') ||
          oppija.sukunimi
            ?.toLocaleLowerCase()
            .includes(tunniste.toLowerCase() ?? '') ||
          oppija?.oppijaNumero.includes(tunniste) ||
          oppija.hetu?.includes(tunniste)
        );
      });
    } else {
      return result.data;
    }
  }, [params]);

  return { ...result, data };
};
