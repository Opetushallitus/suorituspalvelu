import { BASENAME } from '@/lib/common';
import { useLocation } from 'react-router';

export const getActiveTiedotTab = (pathname: string) =>
  pathname.replace(BASENAME, '').split('/')[3];

export const setActiveTiedotTab = (pathname: string, tab: string) => {
  const hasBaseName = pathname.startsWith(BASENAME);
  const pathParts = pathname.replace(BASENAME, '').split('/');
  pathParts.splice(3, 1, tab);
  return (hasBaseName ? BASENAME : '') + pathParts.join('/');
};

export const useActiveTiedotTab = () => {
  const location = useLocation();
  return getActiveTiedotTab(location.pathname);
};
