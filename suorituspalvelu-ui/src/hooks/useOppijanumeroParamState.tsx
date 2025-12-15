import { useCallback } from 'react';
import { useLocation, useNavigate, useParams, type NavigateOptions } from 'react-router';
import { isEmptyish } from 'remeda';

export const useOppijaTunnisteParamState = () => {
  const { oppijaTunniste } = useParams();

  const location = useLocation();
  const navigate = useNavigate();

  const setOppijaTunniste = useCallback(
    (newOppijaTunniste: string, options?: NavigateOptions) => {
      const newLocation = { ...location };
      const pathParts = location.pathname.split('/');
      if (isEmptyish(newOppijaTunniste)) {
        pathParts.splice(2);
      } else {
        const encodedNewOppijanumero = encodeURIComponent(newOppijaTunniste);
        pathParts.splice(2, 1, encodedNewOppijanumero);
      }
      newLocation.pathname = pathParts.join('/');
      navigate(newLocation, options);
    },
    [location, navigate],
  );

  return { oppijaTunniste, setOppijaTunniste };
};
