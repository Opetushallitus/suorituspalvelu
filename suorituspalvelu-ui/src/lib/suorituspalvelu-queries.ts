import { queryOptions } from '@tanstack/react-query';
import {
  getOppija,
  getOppilaitokset,
  getSuoritusvaihtoehdot,
  searchOppijat,
  type OppijatSearchParams,
} from './suorituspalvelu-service';
import { useApiSuspenseQuery } from './http-client';
import { useTranslations } from '@/hooks/useTranslations';

export const useOppija = (oppijaNumero: string) => {
  return useApiSuspenseQuery({
    queryKey: ['getOppija', oppijaNumero],
    queryFn: () => getOppija(oppijaNumero),
  });
};

export const queryOptionsSearchOppijat = (params: OppijatSearchParams) =>
  queryOptions({
    queryKey: ['searchOppijat', params],
    queryFn: () => searchOppijat(params),
  });

export const queryOptionsGetOppilaitokset = () =>
  queryOptions({
    queryKey: ['getOppilaitokset'],
    queryFn: () => getOppilaitokset(),
  });

export const useOppilaitoksetOptions = () => {
  const { translateKielistetty } = useTranslations();
  const { data: oppilaitoksetOptions } = useApiSuspenseQuery({
    ...queryOptionsGetOppilaitokset(),
    select: (data) =>
      data?.oppilaitokset?.map(($) => ({
        value: $.oid,
        label: translateKielistetty($.nimi),
      })) ?? [],
  });
  return oppilaitoksetOptions;
};

export const queryOptionsGetSuoritusvaihtoehdot = () =>
  queryOptions({
    queryKey: ['getSuoritusvaihtoehdot'],
    queryFn: () => getSuoritusvaihtoehdot(),
    staleTime: 10 * 60 * 1000,
  });
