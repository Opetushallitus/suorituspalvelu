import { useGlobalConfirmationModal } from '@/components/ConfirmationModal';
import { useEffect } from 'react';
import { useBlocker } from 'react-router';
import { useTranslations } from './useTranslations';

const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  e.preventDefault();
};

export const useConfirmNavigation = (
  shouldConfirm: boolean,
  onConfirm?: () => void,
) => {
  const blocker = useBlocker(shouldConfirm);
  const { showConfirmation } = useGlobalConfirmationModal();
  const { t } = useTranslations();

  // Handle browser hard reloads or tab closing
  useEffect(() => {
    if (!shouldConfirm) return;

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [shouldConfirm]);

  // Handle react-router navigation
  useEffect(() => {
    if (blocker.state === 'blocked') {
      showConfirmation({
        title: t('tallentamattomia-muutoksia.otsikko'),
        content: t('tallentamattomia-muutoksia.sisalto'),
        confirmLabel: t('tallentamattomia-muutoksia.jatka'),
        cancelLabel: t('peruuta'),
        onConfirm: () => {
          blocker.proceed();
          onConfirm?.();
        },
        onCancel: () => {
          blocker.reset();
        },
      });
    }
  }, [showConfirmation, onConfirm, blocker, t]);
};
