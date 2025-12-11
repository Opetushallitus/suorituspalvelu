import { Box } from '@mui/material';
import { useTranslations } from '@/hooks/useTranslations';
import { isOppijaNumero, isHenkilotunnus } from '@/lib/common';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { HenkiloSearchControls } from '@/components/HenkiloSearchControls';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { OppijanTiedotPage } from './OppijanTiedotPage';
import { useIsHenkilohakuAllowed } from '@/hooks/useIsHenkilohakuAllowed';
import { useLocationState } from '@/hooks/useLocationState';

const HenkiloTunnisteella = () => {
  const { t } = useTranslations();
  const [henkiloSearchTerm] = useLocationState('henkiloSearchTerm');

  if (
    !henkiloSearchTerm ||
    (!isOppijaNumero(henkiloSearchTerm) && !isHenkilotunnus(henkiloSearchTerm))
  ) {
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
