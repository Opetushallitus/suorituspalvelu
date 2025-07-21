'use client';
import { Layout } from '@/components/Layout';
import type { AppProps } from 'next/app';
import { configPromise } from '@/configuration';
import Script from 'next/script';
import { use } from 'react';
import '@/styles/global.css';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SessionExpiredProvider } from '@/components/SessionExpired';

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

export default function App({ Component, pageProps }: AppProps) {
  const config = use(configPromise);

  return (
    <QueryClientProvider client={queryClient}>
      <SessionExpiredProvider>
        <Layout>
          <Component {...pageProps} />
          <Script src={config.routes.yleiset.raamitUrl} />
        </Layout>
      </SessionExpiredProvider>
    </QueryClientProvider>
  );
}
