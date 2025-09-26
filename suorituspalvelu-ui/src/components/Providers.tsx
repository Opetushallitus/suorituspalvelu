import '@fontsource/open-sans/latin-400.css';
import '@fontsource/open-sans/latin-600.css';
import '@fontsource/open-sans/latin-700.css';
import '@/styles/global.css';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SessionExpiredProvider } from '@/components/SessionExpired';
import { NuqsAdapter } from 'nuqs/adapters/react-router/v7';
import { LocalizationProvider } from './LocalizationProvider';
import { use } from 'react';
import { configPromise } from '@/configuration';

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
  const config = use(configPromise);
  return (
    <SessionExpiredProvider>
      <NuqsAdapter>
        <script async src={config.routes.yleiset.raamitUrl}></script>
        <QueryClientProvider client={queryClient}>
          <LocalizationProvider>{children}</LocalizationProvider>
        </QueryClientProvider>
      </NuqsAdapter>
    </SessionExpiredProvider>
  );
}
