import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AvainArvoDisplay } from './AvainArvoDisplay';
import type { AvainArvo } from '@/types/ui-types';

const YLIAJO_BASE = {
  avain: 'test_key',
  selite: '',
  hakuOid: '',
  henkiloOid: '',
  virkailijaOid: '',
};

const queryMuutoshistoriaButton = () =>
  screen.queryByRole('button', {
    name: 'opiskelijavalinnan-tiedot.nayta-muutoshistoria',
  });

const queryYliajamatonText = () =>
  screen.queryByText(`opiskelijavalinnan-tiedot.alkuperainen`);

vi.mock('@/hooks/useTranslations', () => ({
  useTranslations: () => ({
    t: (key: string, params?: Record<string, string>) => {
      if (params) {
        return `${key}-${JSON.stringify(params)}`;
      }
      return key;
    },
    translateKielistetty: (
      obj: { fi?: string; sv?: string; en?: string } | undefined,
    ) => obj?.fi ?? '',
    getLanguage: () => 'fi' as const,
  }),
}));

describe('AvainArvoDisplay', () => {
  const createAvainArvo = (overrides?: Partial<AvainArvo>): AvainArvo => ({
    avain: 'test_key',
    arvo: 'test_value',
    metadata: {
      selitteet: [],
      arvoOnHakemukselta: false,
      yliajo: undefined,
    },
    ...overrides,
  });

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('Ei yliajoja', () => {
    const avainArvo = createAvainArvo({
      metadata: {
        yliajo: undefined,
        arvoOnHakemukselta: false,
        selitteet: [],
      },
    });
    render(<AvainArvoDisplay avainArvo={avainArvo} />);

    expect(screen.getByText('test_key')).toBeInTheDocument();
    expect(screen.getByText('test_value')).toBeInTheDocument();
    expect(queryYliajamatonText()).not.toBeInTheDocument();
    expect(queryMuutoshistoriaButton()).not.toBeInTheDocument();
  });

  it('Yliajo poistettu', () => {
    const avainArvo = createAvainArvo({
      metadata: {
        yliajo: {
          ...YLIAJO_BASE,
          arvo: undefined,
        },
        arvoOnHakemukselta: false,
        selitteet: [],
      },
    });
    render(<AvainArvoDisplay avainArvo={avainArvo} />);

    expect(screen.getByText('test_key')).toBeInTheDocument();
    expect(screen.getByText('test_value')).toBeInTheDocument();
    expect(queryYliajamatonText()).not.toBeInTheDocument();
    expect(queryMuutoshistoriaButton()).toBeInTheDocument();
  });

  it('Yliajo olemassa', () => {
    const avainArvo = createAvainArvo({
      metadata: {
        yliajo: {
          ...YLIAJO_BASE,
          arvo: 'test_value',
        },
        arvoEnnenYliajoa: 'original_value',
        arvoOnHakemukselta: false,
        selitteet: [],
      },
    });

    render(<AvainArvoDisplay avainArvo={avainArvo} />);

    expect(screen.getByText('test_key')).toBeInTheDocument();
    expect(screen.getByText('test_value')).toBeInTheDocument();
    expect(queryYliajamatonText()).toBeInTheDocument();
    expect(queryMuutoshistoriaButton()).toBeInTheDocument();
  });

  it('Lisätty kenttä', () => {
    const avainArvo = createAvainArvo({
      metadata: {
        yliajo: {
          ...YLIAJO_BASE,
          arvo: 'test_value',
        },
        arvoEnnenYliajoa: undefined,
        arvoOnHakemukselta: false,
        selitteet: [],
      },
    });

    render(<AvainArvoDisplay avainArvo={avainArvo} />);

    expect(screen.getByText('test_key')).toBeInTheDocument();
    expect(screen.getByText('test_value')).toBeInTheDocument();
    expect(queryYliajamatonText()).not.toBeInTheDocument();
    expect(queryMuutoshistoriaButton()).toBeInTheDocument();
  });
});
