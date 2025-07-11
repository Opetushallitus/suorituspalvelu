import { QueryErrorResetBoundary } from '@tanstack/react-query';
import { Suspense } from 'react';
import {
  ErrorBoundary,
  type ErrorBoundaryPropsWithRender,
} from 'react-error-boundary';
import { FullSpinner } from './FullSpinner';

type FallbackRenderType = ErrorBoundaryPropsWithRender['fallbackRender'];

const defaultFallbackRender: FallbackRenderType = ({
  resetErrorBoundary,
  error,
}) => (
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
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <ErrorBoundary
          onReset={reset}
          fallbackRender={errorFallbackRender ?? defaultFallbackRender}
        >
          <Suspense fallback={suspenseFallback}>{children}</Suspense>
        </ErrorBoundary>
      )}
    </QueryErrorResetBoundary>
  );
}
