import { Header } from '@/components/Header';
import { PageLayout } from '@/components/PageLayout';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import {
  SessionExpired,
  useIsSessionExpired,
} from '@/components/SessionExpired';
import { useTranslate } from '@tolgee/react';
import { Outlet } from 'react-router';
import { SearchTabNavi } from './SearchTabNavi';
import { NavigationSpinner } from './NavigationSpinner';

export default function RootLayout() {
  const { t } = useTranslate();
  const { isSessionExpired } = useIsSessionExpired();

  return (
    <QuerySuspenseBoundary>
      {isSessionExpired && <SessionExpired />}
      <title>{t('suorituspalvelu')}</title>
      <PageLayout header={<Header title={t('suorituspalvelu')} />}>
        <SearchTabNavi />
        <NavigationSpinner>
          <Outlet />
        </NavigationSpinner>
      </PageLayout>
    </QuerySuspenseBoundary>
  );
}
