import { Stack } from '@mui/material';
import { useTranslations } from '@/hooks/useTranslations';
import type { Route } from './+types/TarkistusLayout';
import {
  queryOptionsGetOppija,
  useOppija,
} from '@/lib/suorituspalvelu-queries';
import { redirect } from 'react-router';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { HenkilotSidebar } from '@/components/HenkilotSidebar';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { queryClient } from '@/lib/queryClient';
import { OppijanTiedotPage } from './OppijanTiedotPage';
import {
  getActiveTiedotTab,
  setActiveTiedotTab,
} from '@/hooks/useActiveTiedotTab';
import { TarkistusSearchControls } from '@/components/TarkistusSearchControls';
import { useOppijaTunnisteParamState } from '@/hooks/useOppijanumeroParamState';
import { isHenkilotunnus } from '@/lib/common';
import { isDefined } from 'remeda';

export async function clientLoader({
  params,
  request,
}: Route.ClientLoaderArgs) {
  const { oppijaTunniste } = params;
  const url = new URL(request.url);

  if (oppijaTunniste) {
    const tiedotTab = getActiveTiedotTab(url.pathname);
    if (!tiedotTab) {
      url.pathname = setActiveTiedotTab(url.pathname, 'suoritustiedot');
      return redirect(url.toString());
    }
    queryClient.ensureQueryData(queryOptionsGetOppija(oppijaTunniste));
  }
}

const TarkistusContent = () => {
  const { t } = useTranslations();

  const { oppijaTunniste, setOppijaTunniste } = useOppijaTunnisteParamState();

  const { data: oppija } = useOppija(oppijaTunniste ?? '');

  const oppijaNumero = oppija?.oppijaNumero;

  if (isDefined(oppijaNumero) && isHenkilotunnus(oppijaTunniste ?? '')) {
    // Asetetaan sama querydata oppijanumerollle, jotta ei tarvitse noutaa uudelleen
    queryClient.setQueryData(
      queryOptionsGetOppija(oppijaNumero).queryKey,
      oppija,
    );
    // Uudelleenohjaus henkilötunnus -> oppijanumero
    setOppijaTunniste(oppijaNumero);
    return null;
  }

  return oppijaTunniste ? (
    <OppijanTiedotPage oppijaNumero={oppijaTunniste} />
  ) : (
    <ResultPlaceholder text={t('search.hae-ja-valitse-henkilo')} />
  );
};

export default function TarkistusLayout() {
  return (
    <>
      <TarkistusSearchControls />
      <Stack direction="row">
        <HenkilotSidebar />
        <main style={{ flexGrow: 1 }}>
          <QuerySuspenseBoundary>
            <TarkistusContent />
          </QuerySuspenseBoundary>
        </main>
      </Stack>
    </>
  );
}
