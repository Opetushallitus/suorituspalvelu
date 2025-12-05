import { queryOptions, useSuspenseQuery } from '@tanstack/react-query';
import {
  getKayttaja,
  getOppija,
  getOppijanHaut,
  getOppilaitokset,
  getOppilaitosVuodet,
  getOppilaitosVuosiLuokat,
  getSuorituksenOppilaitosVaihtoehdot,
  getSuoritusvaihtoehdot,
  getValintadata,
  searchOppilaitoksenOppijat,
  type BackendOppijatSearchParams,
} from './suorituspalvelu-service';
import { useApiQuery, useApiSuspenseQuery } from './http-client';
import { useTranslations } from '@/hooks/useTranslations';
import { prop, sortBy, unique } from 'remeda';

export const queryOptionsGetOppija = (tunniste?: string) =>
  queryOptions({
    queryKey: ['getOppija', tunniste],
    queryFn: () => getOppija(tunniste),
  });

export const useOppija = (oppijaNumero: string) => {
  return useApiSuspenseQuery(queryOptionsGetOppija(oppijaNumero));
};

export const queryOptionsSearchOppilaitoksenOppijat = (
  params: BackendOppijatSearchParams,
) =>
  queryOptions({
    queryKey: ['searchOppilaitoksenOppijat', params],
    queryFn: () => searchOppilaitoksenOppijat(params),
  });

export const queryOptionsGetOppilaitokset = () =>
  queryOptions({
    queryKey: ['getOppilaitokset'],
    queryFn: () => getOppilaitokset(),
  });

export const useOppilaitoksetOptions = () => {
  const { translateKielistetty } = useTranslations();
  return useApiQuery({
    ...queryOptionsGetOppilaitokset(),
    select: (data) =>
      sortBy(
        data?.oppilaitokset?.map(($) => ({
          value: $.oid,
          label: translateKielistetty($.nimi),
        })) ?? [],
        [prop('label'), 'asc'],
      ),
  });
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
    queryKey: hakuOid
      ? ['getValintadata', oppijaNumero, hakuOid]
      : ['getValintadata', oppijaNumero],
    queryFn: () => getValintadata({ oppijaNumero, hakuOid }),
  });

export const queryOptionsGetKayttaja = () =>
  queryOptions({
    queryKey: ['getKayttaja'],
    queryFn: () => getKayttaja(),
    staleTime: Infinity,
  });

export const useKayttaja = () => useSuspenseQuery(queryOptionsGetKayttaja());

export const queryOptionsGetOppijanHaut = (oppijaOid: string) =>
  queryOptions({
    queryKey: ['getOppijanHaut', oppijaOid],
    queryFn: () => getOppijanHaut(oppijaOid),
  });

export const queryOptionsGetOppilaitosVuosiOptions = ({
  oppilaitosOid,
}: {
  oppilaitosOid: string | null;
}) =>
  queryOptions({
    queryKey: ['getOppilaitosVuodet', oppilaitosOid],
    queryFn: () => getOppilaitosVuodet({ oppilaitosOid }),
    enabled: !!oppilaitosOid,
    select: (data) => {
      const vuodet = data ?? [];
      const currentYear = new Date().getFullYear().toString();
      vuodet.push(currentYear);
      vuodet.sort((a, b) => b.localeCompare(a));
      return unique(vuodet).map((vuosi) => ({
        label: vuosi,
        value: vuosi,
      }));
    },
  });

export const queryOptionsGetOppilaitosVuosiLuokatOptions = ({
  oppilaitosOid,
  vuosi,
}: {
  oppilaitosOid: string | null;
  vuosi: string | null;
}) =>
  queryOptions({
    queryKey: ['getOppilaitosVuosiLuokat', oppilaitosOid, vuosi],
    queryFn: () => getOppilaitosVuosiLuokat({ oppilaitosOid, vuosi }),
    enabled: oppilaitosOid != null && vuosi != null,
    select: (data) => {
      const luokat = data ?? [];
      luokat.sort((a, b) => a.localeCompare(b));
      return luokat.map((luokka) => ({
        label: luokka,
        value: luokka,
      }));
    },
  });
