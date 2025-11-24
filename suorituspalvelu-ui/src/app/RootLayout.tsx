import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import {
  SessionExpired,
  useIsSessionExpired,
} from '@/components/SessionExpired';
import { useTranslate } from '@tolgee/react';
import { Outlet } from 'react-router';

export default function HenkiloSearchLayout() {
  const { t } = useTranslate();
  const { isSessionExpired } = useIsSessionExpired();

  return (
    <QuerySuspenseBoundary>
      {isSessionExpired && <SessionExpired />}
      <title>{t('suorituspalvelu')}</title>
      <Outlet />
    </QuerySuspenseBoundary>
  );
}
