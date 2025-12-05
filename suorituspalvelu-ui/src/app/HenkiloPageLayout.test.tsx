import { describe, it, expect } from 'vitest';
import { getOppijanumeroRedirectURL } from './HenkiloPageLayout';
import type { Location } from 'react-router';

describe('getOppijanumeroRedirectURL', () => {
  const createLocation = (pathname: string): Location => ({
    pathname,
    search: '',
    hash: '',
    state: null,
    key: 'default',
  });

  it('should add oppijanumero to path with only base segment', () => {
    const location = createLocation('/henkilo');
    const result = getOppijanumeroRedirectURL(location, 'oid123');

    expect(result?.pathname).toBe('/henkilo/oid123/suoritustiedot');
  });

  it('should replace existing oppijanumero in path', () => {
    const location = createLocation('/henkilo/oldOid/suoritustiedot');
    const result = getOppijanumeroRedirectURL(location, 'newOid');

    expect(result?.pathname).toBe('/henkilo/newOid/suoritustiedot');
  });

  it('should remove oppijanumero when newOppijaNumero is null', () => {
    const location = createLocation('/henkilo/oid123/suoritustiedot');
    const result = getOppijanumeroRedirectURL(location, null);

    expect(result).not.toBeNull();
    expect(result?.pathname).toBe('/henkilo');
  });

  it('should remove oppijanumero when newOppijaNumero is undefined', () => {
    const location = createLocation('/henkilo/oid123/suoritustiedot');
    const result = getOppijanumeroRedirectURL(location, undefined);

    expect(result).not.toBeNull();
    expect(result?.pathname).toBe('/henkilo');
  });

  it('should return null when pathname does not change', () => {
    const location = createLocation('/henkilo/oid123/suoritustiedot');
    const result = getOppijanumeroRedirectURL(location, 'oid123');

    expect(result).toBeNull();
  });

  it('should handle empty oppijanumero string', () => {
    const location = createLocation('/henkilo/oldOid');
    const result = getOppijanumeroRedirectURL(location, '');

    expect(result).not.toBeNull();
    expect(result?.pathname).toBe('/henkilo');
  });

  it('should preserve "suoritustiedot"-segment tabs when switching oppijanumero', () => {
    const location = createLocation('/henkilo/oldOid/suoritustiedot');
    const result = getOppijanumeroRedirectURL(location, 'newOid');

    expect(result).not.toBeNull();
    expect(result?.pathname).toBe('/henkilo/newOid/suoritustiedot');
  });

  it('should preserve search and hash from original location', () => {
    const location: Location = {
      pathname: '/henkilo/oldOid',
      search: '?query=test',
      hash: '#section',
      state: { custom: 'data' },
      key: 'test-key',
    };
    const result = getOppijanumeroRedirectURL(location, 'newOid');

    expect(result).not.toBeNull();
    expect(result?.search).toBe('?query=test');
    expect(result?.hash).toBe('#section');
  });
});
