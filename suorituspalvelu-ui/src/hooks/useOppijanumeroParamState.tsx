import { useCallback } from 'react';
import {
  useLocation,
  useNavigate,
  useParams,
  type NavigateOptions,
} from 'react-router';
import { isEmptyish } from 'remeda';
import { getSelectedTiedotTab } from './useSelectedTiedotTab';

export const useOppijaNumeroParamState = () => {
  const { oppijaNumero } = useParams();

  const location = useLocation();
  const navigate = useNavigate();

  const setOppijaNumero = useCallback(
    (newOppijaNumero: string, options?: NavigateOptions) => {
      const newLocation = { ...location };
      const pathParts = location.pathname.split('/');
      if (isEmptyish(newOppijaNumero)) {
        pathParts.splice(2);
      } else {
        const tiedotTab = getSelectedTiedotTab(location.pathname);
        const encodedNewOppijanumero = encodeURIComponent(newOppijaNumero);
        pathParts.splice(2, 1, encodedNewOppijanumero);
        pathParts.splice(3, 1, tiedotTab ?? 'suoritustiedot');
      }
      newLocation.pathname = pathParts.join('/');
      navigate(newLocation, { state: location.state, ...options });
    },
    [location, navigate],
  );

  return { oppijaNumero, setOppijaNumero };
};
