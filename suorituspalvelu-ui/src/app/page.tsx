'use client';
import { Header } from '@/components/Header';
import { HenkilotSidebar } from '@/components/HenkilotSidebar';
import { HenkiloView } from '@/components/HenkiloView';
import { MuokkausView } from '@/components/MuokkausView';
import { PageLayout } from '@/components/PageLayout';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { SearchControls } from '@/components/SearchControls';
import {
  SessionExpired,
  useIsSessionExpired,
} from '@/components/SessionExpired';
import { useSelectedOppijaNumero } from '@/hooks/useSelectedOppijaNumero';
import { DEFAULT_NUQS_OPTIONS } from '@/lib/common';
import { Stack } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { parseAsBoolean, useQueryState } from 'nuqs';

const HomePage = () => {
  const { t } = useTranslate();
  const { isSessionExpired } = useIsSessionExpired();
  const [oppijaNumero] = useSelectedOppijaNumero();
  const [isMuokkaus] = useQueryState(
    'muokkaus',
    parseAsBoolean.withOptions(DEFAULT_NUQS_OPTIONS).withDefault(false),
  );

  return (
    <QuerySuspenseBoundary>
      <PageLayout header={<Header title={t('suorituspalvelu')} />}>
        <title>{t('suorituspalvelu')}</title>
        <SearchControls />
        <Stack direction="row">
          <HenkilotSidebar />
          <main style={{ flexGrow: 1 }}>
            <QuerySuspenseBoundary>
              {oppijaNumero ? (
                isMuokkaus ? (
                  <MuokkausView />
                ) : (
                  <HenkiloView oppijaNumero={oppijaNumero} />
                )
              ) : (
                <ResultPlaceholder text={t('valitse-henkilo')} />
              )}
            </QuerySuspenseBoundary>
          </main>
        </Stack>
        {isSessionExpired && <SessionExpired />}
      </PageLayout>
    </QuerySuspenseBoundary>
  );
};

export default HomePage;
