import { useCallback } from 'react';
import { useLocation, useNavigate, type NavigateOptions } from 'react-router';

export const useLocationState = (key: string) => {
  const location = useLocation();
  const navigate = useNavigate();

  const setLocationState = useCallback(
    (newValue: string, options?: NavigateOptions) => {
      navigate(location, {
        state: {
          ...(location.state ?? {}),
          [key]: newValue,
        },
        ...options,
      });
    },
    [location, navigate],
  );

  return [
    location.state?.[key] as string | undefined,
    setLocationState,
  ] as const;
};
