'use client';
import { use } from 'react';
import '@/styles/global.css';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SessionExpiredProvider } from '@/components/SessionExpired';
import { TolgeeProvider } from '@tolgee/react';
import { tolgeePromise } from '@/localization/tolgee-config';
import { NuqsAdapter } from 'nuqs/adapters/next';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
      refetchOnMount: false,
    },
  },
});

export function Providers({ children }: { children: React.ReactNode }) {
  const tolgee = use(tolgeePromise);

  return (
    <SessionExpiredProvider>
      <TolgeeProvider tolgee={tolgee}>
        <NuqsAdapter>
          <QueryClientProvider client={queryClient}>
            {children}
          </QueryClientProvider>
        </NuqsAdapter>
      </TolgeeProvider>
    </SessionExpiredProvider>
  );
}
