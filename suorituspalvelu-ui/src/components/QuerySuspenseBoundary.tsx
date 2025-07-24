import { Suspense } from 'react';
import {
  ErrorBoundary,
  type ErrorBoundaryPropsWithRender,
} from 'react-error-boundary';
import { FullSpinner } from './FullSpinner';
import { useQueryErrorResetBoundary } from '@tanstack/react-query';
import { SessionExpiredError } from '@/http-client';
import { SessionExpired } from './SessionExpired';

type FallbackRenderType = ErrorBoundaryPropsWithRender['fallbackRender'];

const defaultFallbackRender: FallbackRenderType = ({
  resetErrorBoundary,
  error,
}) =>
  error instanceof SessionExpiredError ? (
    <SessionExpired />
  ) : (
    <div>
      <p>Jokin meni vikaan</p>
      <p>{JSON.stringify(error)}</p>
      <button onClick={resetErrorBoundary}>Yritä uudelleen</button>
    </div>
  );

export function QuerySuspenseBoundary({
  children,
  suspenseFallback = <FullSpinner />,
  errorFallbackRender = defaultFallbackRender,
}: {
  children: React.ReactNode;
  suspenseFallback?: React.ReactNode;
  errorFallbackRender?: FallbackRenderType;
}) {
  const { reset } = useQueryErrorResetBoundary();
  return (
    <ErrorBoundary onReset={reset} fallbackRender={errorFallbackRender}>
      <Suspense fallback={suspenseFallback}>{children}</Suspense>
    </ErrorBoundary>
  );
}
