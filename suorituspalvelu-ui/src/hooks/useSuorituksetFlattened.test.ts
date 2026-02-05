import { describe, it, expect } from 'vitest';
import { sortSuoritukset } from './useSuorituksetFlattened';
import type { SuorituksenPerustiedot } from '@/types/ui-types';

describe('sortSuoritukset', () => {
  it('sorts suoritukset by aloituspaiva in descending order', () => {
    const suoritukset: Array<SuorituksenPerustiedot> = [
      { aloituspaiva: '2020-01-01' } as SuorituksenPerustiedot,
      { aloituspaiva: '2023-01-01' } as SuorituksenPerustiedot,
      { aloituspaiva: '2021-01-01' } as SuorituksenPerustiedot,
    ];

    const sorted = sortSuoritukset(suoritukset);

    expect(sorted[0]?.aloituspaiva).toBe('2023-01-01');
    expect(sorted[1]?.aloituspaiva).toBe('2021-01-01');
    expect(sorted[2]?.aloituspaiva).toBe('2020-01-01');
  });

  it('places items with aloituspaiva before items without', () => {
    const suoritukset: Array<SuorituksenPerustiedot> = [
      { aloituspaiva: undefined } as SuorituksenPerustiedot,
      { aloituspaiva: '2022-01-01' } as SuorituksenPerustiedot,
      { aloituspaiva: undefined } as SuorituksenPerustiedot,
      { aloituspaiva: '2021-01-01' } as SuorituksenPerustiedot,
    ];

    const sorted = sortSuoritukset(suoritukset);

    expect(sorted[0]?.aloituspaiva).toBe('2022-01-01');
    expect(sorted[1]?.aloituspaiva).toBe('2021-01-01');
    expect(sorted[2]?.aloituspaiva).toBeUndefined();
    expect(sorted[3]?.aloituspaiva).toBeUndefined();
  });

  it('keeps items with same aloituspaiva in original order', () => {
    const suoritukset: Array<SuorituksenPerustiedot & { id: number }> = [
      { id: 1, aloituspaiva: '2022-01-01' } as SuorituksenPerustiedot & {
        id: number;
      },
      { id: 2, aloituspaiva: '2022-01-01' } as SuorituksenPerustiedot & {
        id: number;
      },
      { id: 3, aloituspaiva: '2022-01-01' } as SuorituksenPerustiedot & {
        id: number;
      },
    ];

    const sorted = sortSuoritukset(suoritukset);

    expect(sorted[0]?.id).toBe(1);
    expect(sorted[1]?.id).toBe(2);
    expect(sorted[2]?.id).toBe(3);
  });

  it('handles empty array', () => {
    const suoritukset: Array<SuorituksenPerustiedot> = [];

    const sorted = sortSuoritukset(suoritukset);

    expect(sorted).toEqual([]);
  });

  it('handles array with single item', () => {
    const suoritukset: Array<SuorituksenPerustiedot> = [
      { aloituspaiva: '2022-01-01' } as SuorituksenPerustiedot,
    ];

    const sorted = sortSuoritukset(suoritukset);

    expect(sorted).toHaveLength(1);
    expect(sorted[0]?.aloituspaiva).toBe('2022-01-01');
  });

  it('handles all items without aloituspaiva', () => {
    const suoritukset: Array<SuorituksenPerustiedot & { id: number }> = [
      { id: 1, aloituspaiva: undefined } as SuorituksenPerustiedot & {
        id: number;
      },
      { id: 2, aloituspaiva: undefined } as SuorituksenPerustiedot & {
        id: number;
      },
      { id: 3, aloituspaiva: undefined } as SuorituksenPerustiedot & {
        id: number;
      },
    ];

    const sorted = sortSuoritukset(suoritukset);

    expect(sorted[0]?.id).toBe(1);
    expect(sorted[1]?.id).toBe(2);
    expect(sorted[2]?.id).toBe(3);
  });

  it('sorts correctly with mixed dates and undefined values', () => {
    const suoritukset: Array<SuorituksenPerustiedot & { id: number }> = [
      { id: 1, aloituspaiva: '2020-01-01' } as SuorituksenPerustiedot & {
        id: number;
      },
      { id: 2, aloituspaiva: undefined } as SuorituksenPerustiedot & {
        id: number;
      },
      { id: 3, aloituspaiva: '2023-01-01' } as SuorituksenPerustiedot & {
        id: number;
      },
      { id: 4, aloituspaiva: undefined } as SuorituksenPerustiedot & {
        id: number;
      },
      { id: 5, aloituspaiva: '2021-06-15' } as SuorituksenPerustiedot & {
        id: number;
      },
    ];

    const sorted = sortSuoritukset(suoritukset);

    expect(sorted[0]?.id).toBe(3); // 2023-01-01
    expect(sorted[1]?.id).toBe(5); // 2021-06-15
    expect(sorted[2]?.id).toBe(1); // 2020-01-01
    expect(sorted[3]?.id).toBe(2);
    expect(sorted[4]?.id).toBe(4);
  });
});
