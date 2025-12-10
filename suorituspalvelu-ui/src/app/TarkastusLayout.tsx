import { Stack } from '@mui/material';
import { useTranslations } from '@/hooks/useTranslations';
import type { Route } from './+types/TarkastusLayout';
import {
  queryOptionsGetOppija,
  useOppija,
} from '@/lib/suorituspalvelu-queries';
import { redirect } from 'react-router';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { TarkastusSidebar } from '@/components/TarkastusSidebar';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { queryClient } from '@/lib/queryClient';
import { OppijanTiedotPage } from './OppijanTiedotPage';
import {
  getSelectedTiedotTab,
  setSelectedTiedotTab,
} from '@/hooks/useSelectedTiedotTab';
import { TarkastusSearchControls } from '@/components/TarkastusSearchControls';
import { useOppijaTunnisteParamState } from '@/hooks/useOppijanumeroParamState';
import { isHenkilotunnus } from '@/lib/common';
import { isDefined } from 'remeda';
import { useIsTarkastusnakymaAllowed } from '@/hooks/useIsTarkastusnakymaAllowed';
import { DoNotDisturb } from '@mui/icons-material';
import { useEffect } from 'react';

export async function clientLoader({
  params,
  request,
}: Route.ClientLoaderArgs) {
  const { oppijaTunniste } = params;
  const url = new URL(request.url);

  if (oppijaTunniste) {
    const tiedotTab = getSelectedTiedotTab(url.pathname);
    if (!tiedotTab) {
      url.pathname = setSelectedTiedotTab(url.pathname, 'suoritustiedot');
      return redirect(url.toString());
    }
    queryClient.ensureQueryData(queryOptionsGetOppija(oppijaTunniste));
  }
}

const TarkastusContent = () => {
  const { t } = useTranslations();

  const { oppijaTunniste, setOppijaTunniste } = useOppijaTunnisteParamState();

  const { data: oppija } = useOppija(oppijaTunniste);

  const oppijaNumero = oppija?.oppijaNumero;

  useEffect(() => {
    if (isDefined(oppijaNumero) && isHenkilotunnus(oppijaTunniste)) {
      // Asetetaan sama querydata oppijanumerollle, jotta ei tarvitse noutaa uudelleen
      queryClient.setQueryData(
        queryOptionsGetOppija(oppijaNumero).queryKey,
        oppija,
      );
      // Uudelleenohjaus henkilötunnus -> oppijanumero
      setOppijaTunniste(oppijaNumero, { replace: true });
    }
  }, [oppijaNumero, oppijaTunniste, setOppijaTunniste]);

  return oppijaTunniste ? (
    <OppijanTiedotPage oppijaTunniste={oppijaTunniste} />
  ) : (
    <ResultPlaceholder text={t('search.hae-ja-valitse-henkilo')} />
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
