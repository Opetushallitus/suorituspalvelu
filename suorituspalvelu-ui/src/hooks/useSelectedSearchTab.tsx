import { getSearchTabFromPath } from '@/lib/navigationPathUtils';
import { useLocation } from 'react-router';

export const useSelectedSearchTab = () => {
  const location = useLocation();
  return getSearchTabFromPath(location.pathname);
};
