import { Stack } from '@mui/material';
import { useTranslations } from '@/hooks/useTranslations';
import type { Route } from './+types/HenkiloLayout';
import {
  queryOptionsGetOppija,
  queryOptionsGetOppilaitokset,
} from '@/lib/suorituspalvelu-queries';
import { redirect } from 'react-router';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { HenkilotSidebar } from '@/components/HenkilotSidebar';
import { TarkistusSearchControls } from '@/components/SearchControls';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { queryClient } from '@/lib/queryClient';
import { OppijanTiedotPage } from './OppijanTiedotPage';
import {
  getActiveTiedotTab,
  setActiveTiedotTab,
} from '@/hooks/useActiveTiedotTab';

export async function clientLoader({
  params,
  request,
}: Route.ClientLoaderArgs) {
  const oppijaNumero = params.oppijaNumero;
  const url = new URL(request.url);

  if (oppijaNumero) {
    const tiedotTab = getActiveTiedotTab(url.pathname);
    console.log('tiedotTab', tiedotTab);
    if (!tiedotTab) {
      url.pathname = setActiveTiedotTab(url.pathname, 'suoritustiedot');
      return redirect(url.toString());
    }
    queryClient.ensureQueryData(queryOptionsGetOppija(oppijaNumero));
  }
  queryClient.ensureQueryData(queryOptionsGetOppilaitokset());
}

export default function TarkistusLayout({ params }: Route.ComponentProps) {
  const { t } = useTranslations();

  const { oppijaNumero } = params;

  return (
    <>
      <TarkistusSearchControls />
      <Stack direction="row">
        <HenkilotSidebar />
        <main style={{ flexGrow: 1 }}>
          <QuerySuspenseBoundary>
            {oppijaNumero ? (
              <OppijanTiedotPage oppijaNumero={oppijaNumero} />
            ) : (
              <ResultPlaceholder text={t('search.hae-ja-valitse-henkilo')} />
            )}
          </QuerySuspenseBoundary>
        </main>
      </Stack>
    </>
  );
}
