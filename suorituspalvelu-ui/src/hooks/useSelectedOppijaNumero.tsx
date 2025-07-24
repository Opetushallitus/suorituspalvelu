import { DEFAULT_NUQS_OPTIONS } from '@/common';
import { useQueryState } from 'nuqs';

export const useSelectedOppijaNumero = () => {
  return useQueryState('oppijaNumero', DEFAULT_NUQS_OPTIONS);
};
