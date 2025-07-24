'use client';
import { Header } from '@/components/Header';
import HenkiloPage from '@/components/HenkiloPage';
import { HenkilotSidebar } from '@/components/HenkilotSidebar';
import { PageLayout } from '@/components/PageLayout';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { SearchControls } from '@/components/SearchControls';
import {
  SessionExpired,
  useIsSessionExpired,
} from '@/components/SessionExpired';
import { useSelectedOppijaNumero } from '@/hooks/useSelectedOppijaNumero';
import { useTranslate } from '@tolgee/react';

const useDocumentTitle = () => {
  const { t } = useTranslate();
  const [oppijaNumero] = useSelectedOppijaNumero();
  if (oppijaNumero) {
    return `${t('suorituspalvelu')} - ${t('oppija.otsikko')} - ${oppijaNumero}`;
  }

  return t('suorituspalvelu');
};

const HomePage = () => {
  const { t } = useTranslate();
  const { isSessionExpired } = useIsSessionExpired();
  const [oppijaNumero] = useSelectedOppijaNumero();

  return (
    <QuerySuspenseBoundary>
      <PageLayout header={<Header title={t('suorituspalvelu')} />}>
        {/* Set the document title */}
        <title>{useDocumentTitle()}</title>
        <SearchControls />
        <div style={{ display: 'flex', flexDirection: 'row' }}>
          <HenkilotSidebar />
          <main style={{ flexGrow: 1 }}>
            {oppijaNumero ? (
              <HenkiloPage oppijaNumero={oppijaNumero} />
            ) : (
              <div>{t('valitse-oppija')}</div>
            )}
          </main>
        </div>
        {isSessionExpired && <SessionExpired />}
      </PageLayout>
    </QuerySuspenseBoundary>
  );
};

export default HomePage;
