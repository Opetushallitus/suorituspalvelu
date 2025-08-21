import { ErrorBoundary } from 'react-error-boundary';
import { Suspense } from 'react';
import { FullSpinner } from './FullSpinner';
import { ErrorBoundaryPropsWithRender } from 'react-error-boundary';
import { SessionExpired } from './SessionExpired';
import { SessionExpiredError } from '@/http-client';
import { ErrorView } from './ErrorView';

type FallbackRenderType = ErrorBoundaryPropsWithRender['fallbackRender'];

const errorFallbackRender: FallbackRenderType = ({
  resetErrorBoundary,
  error,
}) =>
  error instanceof SessionExpiredError ? (
    <SessionExpired />
  ) : (
    <ErrorView error={error} reset={resetErrorBoundary} />
  );

export const SuspenseBoundary = ({
  children,
  suspenseFallback = <FullSpinner />,
  onResetErrorBoundary,
}: {
  children: React.ReactNode;
  suspenseFallback?: React.ReactNode;
  onResetErrorBoundary?: () => void;
}) => {
  return (
    <ErrorBoundary
      fallbackRender={errorFallbackRender}
      onReset={onResetErrorBoundary}
    >
      <Suspense fallback={suspenseFallback}>{children}</Suspense>
    </ErrorBoundary>
  );
};
