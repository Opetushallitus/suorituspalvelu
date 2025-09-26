import { Header } from '@/components/Header';
import { HenkilotSidebar } from '@/components/HenkilotSidebar';
import { PageLayout } from '@/components/PageLayout';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { SearchControls } from '@/components/SearchControls';
import {
  SessionExpired,
  useIsSessionExpired,
} from '@/components/SessionExpired';
import { Stack } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Outlet } from 'react-router';
import { NavigationSpinner } from './NavigationSpinner';

export default function HenkiloSearchLayout() {
  const { t } = useTranslate();
  const { isSessionExpired } = useIsSessionExpired();

  return (
    <QuerySuspenseBoundary>
      {isSessionExpired && <SessionExpired />}
      <PageLayout header={<Header title={t('suorituspalvelu')} />}>
        <title>{t('suorituspalvelu')}</title>
        <SearchControls />
        <Stack direction="row">
          <HenkilotSidebar />
          <main style={{ flexGrow: 1 }}>
            <NavigationSpinner>
              <QuerySuspenseBoundary>
                <Outlet />
              </QuerySuspenseBoundary>
            </NavigationSpinner>
          </main>
        </Stack>
      </PageLayout>
    </QuerySuspenseBoundary>
  );
}
