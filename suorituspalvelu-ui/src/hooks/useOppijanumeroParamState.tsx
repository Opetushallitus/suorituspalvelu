import { useCallback } from 'react';
import {
  useLocation,
  useNavigate,
  useParams,
  type NavigateOptions,
} from 'react-router';
import { isEmptyish } from 'remeda';
import { setOppijaNumeroInPath } from '@/lib/navigationPathUtils';

export const useOppijaNumeroParamState = () => {
  const { oppijaNumero } = useParams();

  const location = useLocation();
  const navigate = useNavigate();

  const { pathname, state, search } = location;

  const setOppijaNumero = useCallback(
    (newOppijaNumero: string, options?: NavigateOptions) => {
      const newPathname = isEmptyish(newOppijaNumero)
        ? setOppijaNumeroInPath(pathname, null)
        : setOppijaNumeroInPath(pathname, newOppijaNumero);

      navigate({ pathname: newPathname, search }, { state, ...options });
    },
    [pathname, state, search, navigate],
  );

  return { oppijaNumero, setOppijaNumero };
};
