import { BASENAME } from './common';

/**
 * Centralized navigation path utilities
 * Handles all path manipulation for the application
 */

type PathParts = {
  base: string; // '' or BASENAME
  searchTab: string; // e.g., 'henkilo', 'tarkastus'
  oppijaNumero?: string;
  tiedotTab?: string; // e.g., 'suoritustiedot', 'opiskelijavalinnan-tiedot'
};

export const parsePathname = (pathname: string): PathParts => {
  const hasBaseName = pathname.startsWith(BASENAME);
  const cleanPath = pathname.replace(BASENAME, '');
  const parts = cleanPath.split('/').filter(Boolean);

  return {
    base: hasBaseName ? BASENAME : '',
    searchTab: parts[0] || '',
    oppijaNumero: parts[1],
    tiedotTab: parts[2],
  };
};

export const buildPathname = (parts: PathParts): string => {
  const pathSegments = [parts.searchTab];

  if (parts.oppijaNumero) {
    pathSegments.push(parts.oppijaNumero);
  }

  if (parts.tiedotTab) {
    pathSegments.push(parts.tiedotTab);
  }

  return parts.base + '/' + pathSegments.join('/');
};

export const getOppijaNumeroFromPath = (
  pathname: string,
): string | undefined => {
  return parsePathname(pathname).oppijaNumero;
};

export const setOppijaNumeroInPath = (
  pathname: string,
  oppijaNumero: string | null | undefined,
  defaultTiedotTab: string = 'suoritustiedot',
): string => {
  const parts = parsePathname(pathname);

  if (!oppijaNumero) {
    // Clear oppijanumero and tiedotTab
    parts.oppijaNumero = undefined;
    parts.tiedotTab = undefined;
  } else {
    parts.oppijaNumero = encodeURIComponent(oppijaNumero);
    // Keep existing tiedotTab or use default
    if (!parts.tiedotTab) {
      parts.tiedotTab = defaultTiedotTab;
    }
  }

  return buildPathname(parts);
};

export const getTiedotTabFromPath = (pathname: string): string | undefined => {
  return parsePathname(pathname).tiedotTab;
};

export const setTiedotTabInPath = (
  pathname: string,
  tiedotTab: string,
): string => {
  const parts = parsePathname(pathname);
  parts.tiedotTab = tiedotTab;
  return buildPathname(parts);
};

export const getSearchTabFromPath = (pathname: string): string => {
  return parsePathname(pathname).searchTab;
};
