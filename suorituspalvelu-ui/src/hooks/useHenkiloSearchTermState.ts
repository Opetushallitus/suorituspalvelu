import { isValidOppijaTunniste } from '@/lib/common';
import type { SearchNavigationState } from '@/types/navigation';
import { useCallback } from 'react';
import { useLocation, useNavigate, type NavigateOptions } from 'react-router';
import { setOppijaNumeroInPath } from '@/lib/navigationPathUtils';

export const useHenkiloSearchTermState = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const { pathname, state, search } = location;

  const setHenkiloSearchTerm = useCallback(
    (value: string, options?: NavigateOptions) => {
      let newPathname = pathname;

      if (!isValidOppijaTunniste(value)) {
        newPathname = setOppijaNumeroInPath(pathname, null);
      }

      navigate(
        { pathname: newPathname, search },
        {
          ...options,
          state: {
            ...(state as SearchNavigationState),
            henkiloSearchTerm: value,
          },
        },
      );
    },
    [pathname, state, search, navigate],
  );

  const henkiloSearchTerm = (location.state as SearchNavigationState)
    ?.henkiloSearchTerm;

  return [henkiloSearchTerm, setHenkiloSearchTerm] as const;
};
