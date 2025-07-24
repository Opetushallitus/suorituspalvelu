'use client';

import { DEFAULT_BORDER } from '@/common';
import { useSearchOppijat, useURLParams } from '@/hooks/useSearchOppijat';
import Link from 'next/link';

export function HenkilotSidebar() {
  const { result } = useSearchOppijat();
  const params = useURLParams();
  return (
    <div
      style={{
        width: '200px',
        minWidth: '200px',
        minHeight: '100%',
        borderRight: DEFAULT_BORDER,
      }}
    >
      {result?.data?.oppijat?.map((oppija) => (
        <div key={oppija.oppijaNumero}>
          <Link
            href={{
              query: {
                oppijaNumero: oppija.oppijaNumero,
                ...params,
              },
            }}
          >
            {oppija.nimi}
          </Link>
        </div>
      ))}
    </div>
  );
}
