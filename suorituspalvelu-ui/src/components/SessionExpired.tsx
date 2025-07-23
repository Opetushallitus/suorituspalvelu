'use client';

import { configPromise } from '@/configuration';
import React, { use } from 'react';

const LoginLink = ({ url }: { url: string }) => {
  const loginUrl = new URL(url);
  const serviceUrl = new URL(window.location.href);
  serviceUrl.searchParams.delete('ticket');
  loginUrl.searchParams.set('service', serviceUrl.toString());
  return <a href={loginUrl.toString()}>Log in</a>;
};

export function SessionExpired() {
  const config = use(configPromise);

  return (
    <div>
      <h1>Session expired</h1>
      <p>Please log in again.</p>
      <LoginLink url={config.routes.yleiset.casLoginUrl} />
    </div>
  );
}

export const SessionExpiredContext = React.createContext<
  | {
      isSessionExpired: boolean;
      setIsSessionExpired: React.Dispatch<React.SetStateAction<boolean>>;
    }
  | undefined
>(undefined);

export const SessionExpiredProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const [isSessionExpired, setIsSessionExpired] = React.useState(false);

  return (
    <SessionExpiredContext value={{ isSessionExpired, setIsSessionExpired }}>
      {children}
    </SessionExpiredContext>
  );
};

export const useIsSessionExpired = () => {
  const context = React.useContext(SessionExpiredContext);
  if (!context) {
    throw new Error(
      'useIsSessionExpired must be used within a SessionExpiredProvider',
    );
  }
  return context;
};
