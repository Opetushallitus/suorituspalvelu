import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsSearchOppijat } from '@/lib/suorituspalvelu-queries';
import type { OppijatSearchParams } from '@/lib/suorituspalvelu-service';
import { isEmpty, isNullish, omitBy, values } from 'remeda';
import { useSearchParams } from 'react-router';
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
  const vuosi = searchParams.get('vuosi');
  const oppilaitos = searchParams.get('oppilaitos');
  const luokka = searchParams.get('luokka');
  const tunniste = searchParams.get('tunniste');

  return {
    setSearchParams: (params: OppijatSearchParams) => {
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
        { replace: false },
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

export const useOppijatSearch = () => {
  const params = useOppijatSearchParamsState();

  const urlParams = useOppijatSearchURLParams();

  const result = useApiSuspenseQuery(queryOptionsSearchOppijat(urlParams));

  const data = useMemo(
    () =>
      result.data.filter((oppija) => {
        if (params.oppilaitos && params.vuosi && params.tunniste) {
          return (
            oppija.etunimet
              ?.toLocaleLowerCase()
              .includes(params.tunniste.toLowerCase() ?? '') ||
            oppija.sukunimi
              ?.toLocaleLowerCase()
              .includes(params.tunniste.toLowerCase() ?? '')
          );
        }
        return true;
      }),
    [params],
  );

  return {
    ...params,
    result: { ...result, data },
  };
};
