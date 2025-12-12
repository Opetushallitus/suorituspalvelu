import { useLocation } from 'react-router';
import { getTiedotTabFromPath } from '@/lib/navigationPathUtils';

export const useSelectedTiedotTab = () => {
  const location = useLocation();
  return getTiedotTabFromPath(location.pathname);
};
