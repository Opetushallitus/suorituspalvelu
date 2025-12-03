import { useCallback } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router';
import { isEmptyish } from 'remeda';

export const useOppijaTunnisteParamState = () => {
  const { oppijaTunniste } = useParams();

  const location = useLocation();
  const navigate = useNavigate();

  const setOppijaTunniste = useCallback(
    (newOppijaTunniste: string) => {
      const newLocation = { ...location };
      const pathParts = location.pathname.split('/');
      if (isEmptyish(newOppijaTunniste)) {
        pathParts.splice(2);
      } else {
        const encodedNewOppijanumero = encodeURIComponent(newOppijaTunniste);
        pathParts.splice(2, 1, encodedNewOppijanumero);
      }
      newLocation.pathname = pathParts.join('/');
      navigate(newLocation);
    },
    [location, navigate],
  );

  return { oppijaTunniste, setOppijaTunniste };
};
