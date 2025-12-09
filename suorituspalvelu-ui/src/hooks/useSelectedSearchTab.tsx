import { useLocation } from 'react-router';

export type SearchTab = 'henkilo' | 'tarkastus';

export const getSelectedSearchTab = (pathname: string): SearchTab => {
  return pathname.split('/')[1] as SearchTab;
};

export const useSelectedSearchTab = () => {
  const location = useLocation();
  return getSelectedSearchTab(location.pathname);
};
