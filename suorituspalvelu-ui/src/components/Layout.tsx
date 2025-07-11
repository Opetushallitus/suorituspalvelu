'use client';

import { HenkilotSidebar } from './HenkilotSidebar';
import { QuerySuspenseBoundary } from './QuerySuspenseBoundary';
import { SearchControls } from './SearchControls';

export const Layout = ({ children }: { children: React.ReactNode }) => {
  return (
    <QuerySuspenseBoundary>
      <div style={{ display: 'flex', flexDirection: 'column' }}>
        <SearchControls />
        <div style={{ display: 'flex', flexDirection: 'row' }}>
          <HenkilotSidebar />
          <main style={{ flexGrow: 1 }}>{children}</main>
        </div>
      </div>
    </QuerySuspenseBoundary>
  );
};
