import { afterEach, describe, expect, it, vi } from 'vitest';
import { queryOptionsGetOppilaitosVuosiOptions } from './suorituspalvelu-queries';

describe('oppilaitoksen valmistumisvuosivaihtoehdot', () => {
  afterEach(() => vi.useRealTimers());

  it.each(['2026-03-01', '2026-09-01'])(
    'sisältää kuluvan kalenterivuoden myös ilman sen oppilaita (%s)',
    (paiva) => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date(`${paiva}T12:00:00Z`));
      const { select } = queryOptionsGetOppilaitosVuosiOptions({
        oppilaitosOid: '1.2.246.562.10.12345678901',
      });
      const vuodet = ['2027'];

      expect(select?.(vuodet)).toEqual([
        { label: '2027', value: '2027' },
        { label: '2026', value: '2026' },
      ]);
      expect(vuodet).toEqual(['2027']);
      expect(select?.([])).toEqual([{ label: '2026', value: '2026' }]);
      expect(select?.(['2026', '2027'])).toEqual([
        { label: '2027', value: '2027' },
        { label: '2026', value: '2026' },
      ]);
    },
  );
});
