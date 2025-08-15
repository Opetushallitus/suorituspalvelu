'use client';

import { Providers } from '@/components/Providers';
import { isTest, useConfig } from '@/configuration';
import Script from 'next/script';

function Layout({ children }: { children: React.ReactNode }) {
  const config = useConfig();

  return (
    <html lang="fi">
      {!isTest && <Script src={config.routes.yleiset.raamitUrl} />}
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}

export default Layout;
