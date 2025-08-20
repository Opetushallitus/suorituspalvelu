import { FullSpinner } from './FullSpinner';
import { useQueryErrorResetBoundary } from '@tanstack/react-query';
import { SuspenseBoundary } from './SuspenseBoundary';

export function QuerySuspenseBoundary({
  children,
  suspenseFallback = <FullSpinner />,
}: {
  children: React.ReactNode;
  suspenseFallback?: React.ReactNode;
}) {
  const { reset } = useQueryErrorResetBoundary();
  return (
    <SuspenseBoundary
      onResetErrorBoundary={reset}
      suspenseFallback={suspenseFallback}
    >
      {children}
    </SuspenseBoundary>
  );
}
