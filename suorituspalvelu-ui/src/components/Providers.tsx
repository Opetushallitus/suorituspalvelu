import '@fontsource/open-sans/latin-400.css';
import '@fontsource/open-sans/latin-600.css';
import '@fontsource/open-sans/latin-700.css';
import '@/styles/global.css';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SessionExpiredProvider } from '@/components/SessionExpired';
import { LocalizationProvider } from './LocalizationProvider';
import { use } from 'react';
import { configPromise } from '@/lib/configuration';
import { ConfirmationModalProvider } from './ConfirmationModal';

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
      <script async src={config.routes.yleiset.raamitUrl}></script>
      <QueryClientProvider client={queryClient}>
        <LocalizationProvider>
          <ConfirmationModalProvider>{children}</ConfirmationModalProvider>
        </LocalizationProvider>
      </QueryClientProvider>
    </SessionExpiredProvider>
  );
}
