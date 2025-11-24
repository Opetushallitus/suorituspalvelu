import { useSearchParams, type NavigateOptions } from 'react-router';
import { useCallback } from 'react';

/**
 * A hook to manage a single URL query parameter using React Router.
 * Replaces nuqs functionality with native React Router.
 *
 * @param key - The query parameter key
 * @returns A tuple of [value, setValue] similar to useState
 */
export function useQueryParam(
  key: string,
): [string | null, (value: string | null, options?: NavigateOptions) => void] {
  const [searchParams, setSearchParams] = useSearchParams();

  const value = searchParams.get(key);

  const setValue = useCallback(
    (newValue: string | null, options?: NavigateOptions) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (newValue === null || newValue === '') {
            next.delete(key);
          } else {
            next.set(key, newValue);
          }
          return next;
        },
        { replace: false, ...options }, // Use push to add to history
      );
    },
    [key, setSearchParams],
  );

  return [value, setValue];
}
