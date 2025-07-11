'use client';
import { Layout } from '@/components/Layout';
import type { AppProps } from 'next/app';
import { configPromise } from '@/configuration';
import Script from 'next/script';
import { use, useEffect, useState } from 'react';
import '@/styles/global.css';
import { SessionExpiredError } from '@/http-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const LoginLink = ({ url }: { url: string }) => {
  const loginUrl = new URL(url);
  const serviceUrl = new URL(window.location.href);
  serviceUrl.searchParams.delete('ticket');
  loginUrl.searchParams.set('service', serviceUrl.toString());
  return <a href={loginUrl.toString()}>Log in</a>;
};

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
  const [isSessionExpired, setSessionExpired] = useState(false);

  useEffect(() => {
    const handleSessionExpired = (event: PromiseRejectionEvent) => {
      if (event.reason instanceof SessionExpiredError) {
        setSessionExpired(true);
      }
    };

    window.addEventListener('unhandledrejection', handleSessionExpired);

    return () => {
      window.removeEventListener('unhandledrejection', handleSessionExpired);
    };
  }, []);

  return (
    <Layout>
      <QueryClientProvider client={queryClient}>
        <Component {...pageProps} />
      </QueryClientProvider>
      <Script src={config.routes.yleiset.raamitUrl} />
      {isSessionExpired && (
        <div>
          <h1>Session expired</h1>
          <p>Please log in again.</p>
          <LoginLink url={config.routes.yleiset.casLoginUrl} />
        </div>
      )}
    </Layout>
  );
}
