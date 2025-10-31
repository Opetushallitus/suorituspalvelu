import { FullSpinner } from './FullSpinner';
import { useQueryErrorResetBoundary } from '@tanstack/react-query';
import { SuspenseBoundary } from './SuspenseBoundary';

export function QuerySuspenseBoundary({
  children,
  suspenseFallback,
  ErrorFallback,
}: {
  children: React.ReactNode;
  suspenseFallback?: React.ReactNode;
  ErrorFallback?: React.ComponentType<{
    error: Error;
    reset: () => void;
  }>;
}) {
  const { reset } = useQueryErrorResetBoundary();
  return (
    <SuspenseBoundary
      onResetErrorBoundary={reset}
      ErrorFallback={ErrorFallback}
      suspenseFallback={suspenseFallback ?? <FullSpinner />}
    >
      {children}
    </SuspenseBoundary>
  );
}
