import { Alert, Stack } from '@mui/material';
import { useTranslations } from '@/hooks/useTranslations';
import type { Route } from './+types/TarkastusLayout';
import { queryOptionsGetOppija } from '@/lib/suorituspalvelu-queries';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { TarkastusSidebar } from '@/components/TarkastusSidebar';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { queryClient } from '@/lib/queryClient';
import { OppijanTiedotPage } from './OppijanTiedotPage';
import { TarkastusSearchControls } from '@/components/TarkastusSearchControls';
import { useOppijaNumeroParamState } from '@/hooks/useOppijanumeroParamState';
import { isOppijaNumero } from '@/lib/common';
import { useIsTarkastusnakymaAllowed } from '@/hooks/useIsTarkastusnakymaAllowed';
import { DoNotDisturb } from '@mui/icons-material';
import { Box } from '@mui/system';

export async function clientLoader({ params }: Route.ClientLoaderArgs) {
  const { oppijaNumero } = params;
  if (oppijaNumero) {
    queryClient.ensureQueryData(queryOptionsGetOppija(oppijaNumero));
  }
}

const TarkastusContent = () => {
  const { t } = useTranslations();

  const { oppijaNumero } = useOppijaNumeroParamState();

  if (!oppijaNumero) {
    return <ResultPlaceholder text={t('search.hae-ja-valitse-henkilo')} />;
  }

  return isOppijaNumero(oppijaNumero) ? (
    <OppijanTiedotPage oppijaTunniste={oppijaNumero} />
  ) : (
    <Box sx={{ padding: 2 }}>
      <Alert severity="error">
        {t('search.virheellinen-oppijanumero', { oppijaNumero })}
      </Alert>
    </Box>
  );
};

export default function TarkastusLayout() {
  const { t } = useTranslations();

  const isTarkastusNakymaAllowed = useIsTarkastusnakymaAllowed();
  if (!isTarkastusNakymaAllowed) {
    return (
      <ResultPlaceholder
        icon={<DoNotDisturb />}
        text={t('search.tarkastus-ei-oikeuksia')}
      />
    );
  }
  return (
    <>
      <TarkastusSearchControls />
      <Stack direction="row">
        <TarkastusSidebar />
        <main style={{ flexGrow: 1 }}>
          <QuerySuspenseBoundary>
            <TarkastusContent />
          </QuerySuspenseBoundary>
        </main>
      </Stack>
    </>
  );
}
