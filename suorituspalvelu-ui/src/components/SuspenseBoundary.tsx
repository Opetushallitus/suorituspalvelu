import { ErrorBoundary } from 'react-error-boundary';
import { Suspense } from 'react';
import { SessionExpiredError } from '@/lib/http-client';
import { ErrorView } from './ErrorView';
import { FullSpinner } from './FullSpinner';

export const SuspenseBoundary = ({
  children,
  suspenseFallback,
  ErrorFallback = ErrorView,
  onResetErrorBoundary,
}: {
  children: React.ReactNode;
  suspenseFallback?: React.ReactNode;
  ErrorFallback?: React.ComponentType<{
    error: Error;
    reset: () => void;
  }>;
  onResetErrorBoundary?: () => void;
}) => {
  return (
    <ErrorBoundary
      fallbackRender={({ resetErrorBoundary, error }) =>
        error instanceof SessionExpiredError ? null : (
          <ErrorFallback error={error} reset={resetErrorBoundary} />
        )
      }
      onReset={onResetErrorBoundary}
    >
      <Suspense fallback={suspenseFallback ?? <FullSpinner />}>
        {children}
      </Suspense>
    </ErrorBoundary>
  );
};
