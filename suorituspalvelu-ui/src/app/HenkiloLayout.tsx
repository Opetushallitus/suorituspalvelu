import { Box } from '@mui/material';
import { useTranslations } from '@/hooks/useTranslations';
import { isHenkiloOid, isHenkilotunnus } from '@/lib/common';
import { queryOptionsGetOppija } from '@/lib/suorituspalvelu-queries';
import { redirect, useParams } from 'react-router';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { HenkiloSearchControls } from '@/components/HenkiloSearchControls';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { OppijanTiedotPage } from './OppijanTiedotPage';
import {
  getSelectedTiedotTab,
  setSelectedTiedotTab,
} from '@/hooks/useSelectedTiedotTab';
import { queryClient } from '@/lib/queryClient';
import type { Route } from './+types/HenkiloLayout';
import { useIsHenkilohakuAllowed } from '@/hooks/useIsHenkilohakuAllowed';

export const clientLoader = async ({
  params,
  request,
}: Route.ClientLoaderArgs) => {
  const { oppijaTunniste } = params;
  const url = new URL(request.url);

  if (oppijaTunniste) {
    const tiedotTab = getSelectedTiedotTab(url.pathname);
    if (!tiedotTab) {
      url.pathname = setSelectedTiedotTab(url.pathname, 'suoritustiedot');
      throw redirect(url.toString());
    }
    queryClient.ensureQueryData(queryOptionsGetOppija(oppijaTunniste));
  }
};

const HenkiloTunnisteella = () => {
  const { t } = useTranslations();
  const { oppijaTunniste } = useParams();

  if (
    !oppijaTunniste ||
    (!isHenkiloOid(oppijaTunniste) && !isHenkilotunnus(oppijaTunniste))
  ) {
    return <ResultPlaceholder text={t('search.ei-validi-tunniste')} />;
  }

  return <OppijanTiedotPage oppijaTunniste={oppijaTunniste} />;
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
