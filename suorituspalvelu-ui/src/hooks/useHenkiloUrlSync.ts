import { useEffect } from 'react';
import { useOppijaNumeroParamState } from './useOppijanumeroParamState';
import { useHenkiloSearchTermState } from './useHenkiloSearchTermState';
import { useNotifications } from '@/components/NotificationProvider';
import { useTranslations } from './useTranslations';
import { isHenkilotunnus } from '@/lib/common';
import { isEmptyish } from 'remeda';

/**
 * Custom hook that handles URL synchronization for Henkilo search
 *
 * This hook manages the bidirectional sync between:
 * - URL path parameter (oppijaNumero)
 * - Navigation state (henkiloSearchTerm)
 *
 * Returns whether the URL is in sync (no synchronization needed).
 * Components should only fetch data when this is true.
 *
 * Rules:
 * 1. Prevent henkilotunnus in URL (security) - clear search and show error
 * 2. Sync URL param to search term when search term is not set
 */
export const useHenkiloUrlSync = () => {
  const { t } = useTranslations();
  const { oppijaNumero } = useOppijaNumeroParamState();
  const [henkiloSearchTerm, setHenkiloSearchTerm] = useHenkiloSearchTermState();
  const { showNotification } = useNotifications();

  // Determine if synchronization is needed
  const needsHenkilotunnusClearing = isHenkilotunnus(oppijaNumero);
  const needsUrlToStateSync =
    henkiloSearchTerm === undefined && !isEmptyish(oppijaNumero);

  useEffect(() => {
    if (needsHenkilotunnusClearing) {
      setHenkiloSearchTerm('', { replace: true });
      showNotification({
        message: t('search.henkilotunnukseen-linkitys-kielletty'),
        type: 'error',
      });
    } else if (needsUrlToStateSync) {
      setHenkiloSearchTerm(oppijaNumero, { replace: true });
    }
  }, [
    needsHenkilotunnusClearing,
    needsUrlToStateSync,
    oppijaNumero,
    setHenkiloSearchTerm,
    showNotification,
    t,
  ]);

  // URL is in sync when no synchronization is needed
  const isUrlInSync = !needsHenkilotunnusClearing && !needsUrlToStateSync;

  return {
    isUrlInSync,
    henkiloSearchTerm,
  };
};
