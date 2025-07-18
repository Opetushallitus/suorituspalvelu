'use client';

import { Providers } from '@/components/Providers';
import { configPromise, isTest } from '@/configuration';
import Script from 'next/script';
import { use } from 'react';

function Layout({ children }: { children: React.ReactNode }) {
  const config = use(configPromise);

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
