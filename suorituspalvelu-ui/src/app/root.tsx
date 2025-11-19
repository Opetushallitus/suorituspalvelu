import { Outlet, Scripts, ScrollRestoration } from 'react-router';
import { Providers } from '@/components/Providers';
import { ErrorView } from '@/components/ErrorView';
import { queryClient } from '@/lib/queryClient';
import { queryOptionsGetKayttaja } from '@/lib/suorituspalvelu-queries';

export function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fi">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </head>
      <body>
        {children}
        <Scripts />
        <ScrollRestoration />
      </body>
    </html>
  );
}

export function HydrateFallback() {
  return null;
}

export default function App() {
  return (
    <Providers>
      <Outlet />
    </Providers>
  );
}

export const clientLoader = async () => {
  queryClient.ensureQueryData(queryOptionsGetKayttaja());
  return null;
};

export function ErrorBoundary({ error }: { error: Error }) {
  return <ErrorView error={error} />;
}
