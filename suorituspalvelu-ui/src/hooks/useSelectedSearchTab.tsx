import { useLocation } from 'react-router';

export type SearchTab = 'henkilo' | 'tarkistus';

export const useSelectedSearchTab = () => {
  const location = useLocation();
  return location.pathname.split('/')[1] as SearchTab;
};
