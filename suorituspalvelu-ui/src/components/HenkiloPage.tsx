'use client';
import { FullSpinner } from '@/components/FullSpinner';
import { useOppija } from '@/queries';
import { Suspense } from 'react';

const OppijaContent = ({ oppijaNumero }: { oppijaNumero: string }) => {
  const { data: tiedot } = useOppija(oppijaNumero);
  return <pre>{JSON.stringify(tiedot.data, null, 2)}</pre>;
};

export default function HenkiloPage({
  oppijaNumero,
}: {
  oppijaNumero: string;
}) {
  return (
    <div>
      <Suspense fallback={<FullSpinner />}>
        <OppijaContent oppijaNumero={oppijaNumero} />
      </Suspense>
    </div>
  );
}
