'use client';
import { FullSpinner } from '@/components/FullSpinner';
import { useOppija } from '@/queries';
import { Suspense } from 'react';

const Content = ({ oppijaNumero }: { oppijaNumero: string }) => {
  const { data: tiedot } = useOppija(oppijaNumero);
  return <p>{JSON.stringify(tiedot)}</p>;
};

export default function HenkiloPage({
  oppijaNumero,
}: {
  oppijaNumero: string;
}) {
  return (
    <div>
      <Suspense fallback={<FullSpinner />}>
        <Content oppijaNumero={oppijaNumero} />
      </Suspense>
    </div>
  );
}
