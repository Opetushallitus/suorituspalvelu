import { queryOptions, useSuspenseQuery } from '@tanstack/react-query';
import {
  getKayttaja,
  getOppija,
  getOppilaitokset,
  getSuorituksenOppilaitosVaihtoehdot,
  getSuoritusvaihtoehdot,
  getValintadata,
  searchOppijat,
  type OppijatSearchParams,
} from './suorituspalvelu-service';
import { useApiQuery, useApiSuspenseQuery } from './http-client';
import { useTranslations } from '@/hooks/useTranslations';
import { prop, sortBy } from 'remeda';

export const queryOptionsGetOppija = (oppijaNumero: string) =>
  queryOptions({
    queryKey: ['getOppija', oppijaNumero],
    queryFn: () => getOppija(oppijaNumero),
  });

export const useOppija = (oppijaNumero: string) => {
  return useApiSuspenseQuery(queryOptionsGetOppija(oppijaNumero));
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

export const useSuoritusOppilaitosOptions = () => {
  const { translateKielistetty } = useTranslations();

  return useApiQuery({
    ...queryOptionsGetSuorituksenOppilaitosVaihtoehdot(),
    select: (data) =>
      sortBy(
        data?.map(($) => ({
          value: $.oid,
          label: translateKielistetty($.nimi).trim() + ` (${$.oid})`,
        })) ?? [],
        [prop('label'), 'asc'],
      ),
    throwOnError: false,
  });
};

export const queryOptionsGetSuoritusvaihtoehdot = () =>
  queryOptions({
    queryKey: ['getSuoritusvaihtoehdot'],
    queryFn: () => getSuoritusvaihtoehdot(),
    staleTime: Infinity, // Pidetään muistissa niin kauan kunnes sivu ladataan uudelleen
    throwOnError: false,
  });

export const queryOptionsGetSuorituksenOppilaitosVaihtoehdot = () =>
  queryOptions({
    queryKey: ['getSuorituksenOppilaitosVaihtoehdot'],
    queryFn: () => getSuorituksenOppilaitosVaihtoehdot(),
    staleTime: Infinity, // Pidetään muistissa niin kauan kunnes sivu ladataan uudelleen
    throwOnError: false,
  });

export const queryOptionsGetValintadata = ({
  oppijaNumero,
  hakuOid,
}: {
  oppijaNumero: string;
  hakuOid?: string;
}) =>
  queryOptions({
    queryKey: ['getValintadata', oppijaNumero, hakuOid],
    queryFn: () => getValintadata({ oppijaNumero, hakuOid }),
  });

export const queryOptionsGetKayttaja = () =>
  queryOptions({
    queryKey: ['getKayttaja'],
    queryFn: () => getKayttaja(),
    staleTime: Infinity,
  });

export const useKayttaja = () => useSuspenseQuery(queryOptionsGetKayttaja());
