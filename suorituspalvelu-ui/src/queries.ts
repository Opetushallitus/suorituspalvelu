import { queryOptions } from '@tanstack/react-query';
import {
  getOppija,
  getOppilaitokset,
  searchOppijat,
  SearchParams,
} from './api';
import { useApiSuspenseQuery } from './http-client';

export const useOppija = (oppijaNumero: string) => {
  return useApiSuspenseQuery({
    queryKey: ['getOppija', oppijaNumero],
    queryFn: () => getOppija(oppijaNumero),
  });
};

export const queryOptionsSearchOppijat = (params: SearchParams) =>
  queryOptions({
    queryKey: ['searchOppijat', params],
    queryFn: () => searchOppijat(params),
  });

export const queryOptionsGetOppilaitokset = () =>
  queryOptions({
    queryKey: ['getOppilaitokset'],
    queryFn: () => getOppilaitokset(),
  });
