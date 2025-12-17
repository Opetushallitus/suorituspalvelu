import { Box } from '@mui/material';
import { useTranslations } from '@/hooks/useTranslations';
import { isHenkilotunnus, isValidOppijaTunniste } from '@/lib/common';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { HenkiloSearchControls } from '@/components/HenkiloSearchControls';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { OppijanTiedotPage } from './OppijanTiedotPage';
import { useIsHenkilohakuAllowed } from '@/hooks/useIsHenkilohakuAllowed';
import { useHenkiloSearchTermState } from '@/hooks/useHenkiloSearchTermState';
import { useNotifications } from '@/components/NotificationProvider';
import { useOppijaNumeroParamState } from '@/hooks/useOppijanumeroParamState';
import { useEffect } from 'react';
import { isEmptyish } from 'remeda';
import { FullSpinner } from '@/components/FullSpinner';

const HenkiloTunnisteella = () => {
  const { t } = useTranslations();

  const { oppijaNumero } = useOppijaNumeroParamState();
  const [henkiloSearchTerm, setHenkiloSearchTerm] = useHenkiloSearchTermState();

  const { showNotification } = useNotifications();

  const needSyncHenkiloSearchTerm =
    henkiloSearchTerm === undefined && !isEmptyish(oppijaNumero);

  const urlOppijaNumeroNotInSync =
    isHenkilotunnus(oppijaNumero) || needSyncHenkiloSearchTerm;

  useEffect(() => {
    if (isHenkilotunnus(oppijaNumero)) {
      // Poistetaan henkilotunnus URL:sta ja näytetään virhe
      setHenkiloSearchTerm('', { replace: true });
      showNotification({
        message: t('search.henkilotunnukseen-linkitys-kielletty'),
        type: 'error',
      });
      // Synkronoidaan henkiloSearchTerm ja oppijanumero-parametri
    } else if (needSyncHenkiloSearchTerm) {
      setHenkiloSearchTerm(oppijaNumero, { replace: true });
    }
  }, [
    oppijaNumero,
    needSyncHenkiloSearchTerm,
    setHenkiloSearchTerm,
    showNotification,
    t,
  ]);

  if (urlOppijaNumeroNotInSync) {
    return <FullSpinner />;
  }

  if (!isValidOppijaTunniste(henkiloSearchTerm)) {
    return <ResultPlaceholder text={t('search.ei-validi-tunniste')} />;
  }

  return <OppijanTiedotPage oppijaTunniste={henkiloSearchTerm} />;
};

export default function HenkiloLayout() {
  const { t } = useTranslations();
  const isHenkilohakuAllowed = useIsHenkilohakuAllowed();

  if (!isHenkilohakuAllowed) {
    return (
      <Box component="main">
        <ResultPlaceholder text={t('search.henkilohaku-ei-oikeuksia')} />
      </Box>
    );
  }

  return (
    <>
      <HenkiloSearchControls />
      <Box component="main">
        <QuerySuspenseBoundary>
          <HenkiloTunnisteella />
        </QuerySuspenseBoundary>
      </Box>
    </>
  );
}
