import { useTranslations } from '@/hooks/useTranslations';
import { useApiSuspenseQuery } from './http-client';
import { getKoodit } from './koodisto-service';

export const useKoodistoOptions = (koodisto: string) => {
  const res = useApiSuspenseQuery({
    queryKey: ['koodisto-options', koodisto],
    queryFn: () => getKoodit(koodisto),
  });

  const { translateKielistetty } = useTranslations();

  return res.data.map((k) => {
    return {
      label: translateKielistetty(k.nimi),
      value: k.koodiUri,
    };
  });
};

export const useSuorituksenTila = () => {
  return useApiSuspenseQuery({
    queryKey: ['koodisto-suorituksentila'],
    queryFn: () => getKoodit('suorituksentila'),
  });
};

export const useArvosanat = () => {
  return useApiSuspenseQuery({
    queryKey: ['koodisto-arvosanat'],
    queryFn: () => getKoodit('arvosanat'),
  });
};

export const useKielivalikoima = () => {
  return useApiSuspenseQuery({
    queryKey: ['koodisto-kielivalikoima'],
    queryFn: () => getKoodit('kielivalikoima'),
  });
};

export const useAidinkieliJaKirjallisuus = () => {
  return useApiSuspenseQuery({
    queryKey: ['koodisto-aidinkieli-ja-kirjallisuus'],
    queryFn: () => getKoodit('aidinkielijakirjallisuus'),
  });
};

export const useYksilollistaminen = () => {
  return useApiSuspenseQuery({
    queryKey: ['koodisto-yksilollistaminen'],
    queryFn: () => getKoodit('yksilollistaminen'),
  });
};

export const useOppiaineetYleissivistava = () => {
  return useApiSuspenseQuery({
    queryKey: ['koodisto-oppiaineet-yleissivistava'],
    queryFn: () => getKoodit('oppiaineet-yleissivistava'),
  });
};
