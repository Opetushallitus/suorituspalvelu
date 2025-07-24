import { queryOptions, useSuspenseQuery } from '@tanstack/react-query';
import { getOppija, searchOppijat, SearchParams } from './api';

export const useOppija = (oppijaNumero: string) => {
  return useSuspenseQuery({
    queryKey: ['getOppija', oppijaNumero],
    queryFn: () => getOppija(oppijaNumero),
  });
};

export const queryOptionsSearchOppijat = (params: SearchParams) =>
  queryOptions({
    queryKey: ['searchOppijat', params],
    queryFn: () => searchOppijat(params),
  });
