import '@fontsource/open-sans/latin-400.css';
import '@fontsource/open-sans/latin-600.css';
import '@fontsource/open-sans/latin-700.css';
import '@/styles/global.css';
import { QueryClientProvider } from '@tanstack/react-query';
import { SessionExpiredProvider } from '@/components/SessionExpired';
import { LocalizationProvider } from './LocalizationProvider';
import { use } from 'react';
import { configPromise } from '@/lib/configuration';
import { ConfirmationModalProvider } from './ConfirmationModal';
import { NotificationProvider } from './NotificationProvider';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { queryClient } from '@/lib/queryClient';

export function Providers({ children }: { children: React.ReactNode }) {
  const config = use(configPromise);
  return (
    <SessionExpiredProvider>
      <script async src={config.routes.yleiset.raamitUrl}></script>
      <QueryClientProvider client={queryClient}>
        <ReactQueryDevtools initialIsOpen={false} />
        <LocalizationProvider>
          <NotificationProvider>
            <ConfirmationModalProvider>{children}</ConfirmationModalProvider>
          </NotificationProvider>
        </LocalizationProvider>
      </QueryClientProvider>
    </SessionExpiredProvider>
  );
}
