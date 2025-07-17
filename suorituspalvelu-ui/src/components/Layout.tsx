'use client';

import { HenkilotSidebar } from './HenkilotSidebar';
import { SearchControls } from './SearchControls';
import { SessionExpired, useIsSessionExpired } from './SessionExpired';

export const Layout = ({ children }: { children: React.ReactNode }) => {
  const { isSessionExpired } = useIsSessionExpired();

  return (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      <SearchControls />
      <div style={{ display: 'flex', flexDirection: 'row' }}>
        <HenkilotSidebar />
        <main style={{ flexGrow: 1 }}>{children}</main>
      </div>
      {isSessionExpired && <SessionExpired />}
    </div>
  );
};
