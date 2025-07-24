'use client';

import {
  useOppijatSearch,
  useOppijatSearchURLParams,
} from '@/hooks/useSearchOppijat';
import { DEFAULT_BOX_BORDER } from '@/lib/theme';
import Link from 'next/link';
import { QuerySuspenseBoundary } from './QuerySuspenseBoundary';

const HenkilotSidebarContent = () => {
  const params = useOppijatSearchURLParams();

  const { result } = useOppijatSearch();

  return (
    <div>
      {result.data.oppijat?.map((oppija) => (
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
};

export function HenkilotSidebar() {
  return (
    <div
      style={{
        width: '200px',
        minWidth: '200px',
        minHeight: '100%',
        borderRight: DEFAULT_BOX_BORDER,
      }}
    >
      <QuerySuspenseBoundary>
        <HenkilotSidebarContent />
      </QuerySuspenseBoundary>
    </div>
  );
}
