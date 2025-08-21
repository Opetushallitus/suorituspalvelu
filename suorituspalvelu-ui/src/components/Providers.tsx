'use client';

import '@/styles/global.css';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SessionExpiredProvider } from '@/components/SessionExpired';
import { NuqsAdapter } from 'nuqs/adapters/next';
import { LocalizationProvider } from './LocalizationProvider';

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
  return (
    <SessionExpiredProvider>
      <NuqsAdapter>
        <QueryClientProvider client={queryClient}>
          <LocalizationProvider>{children}</LocalizationProvider>
        </QueryClientProvider>
      </NuqsAdapter>
    </SessionExpiredProvider>
  );
}
