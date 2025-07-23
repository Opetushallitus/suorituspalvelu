'use client';
import { FullSpinner } from '@/components/FullSpinner';
import { configPromise } from '@/configuration';
import { client, useApiSuspenseQuery } from '@/http-client';
import { Suspense } from 'react';

const getOppijanTiedot = async (oppijaNumero?: string) => {
  const config = await configPromise;

  // eslint-disable-next-line
  return client.get<any>(
    `${config.routes.suorituspalvelu.oppijanTiedotUrl}/${oppijaNumero}`,
  );
};

const Content = ({ oppijaNumero }: { oppijaNumero: string }) => {
  const { data: tiedot } = useApiSuspenseQuery({
    queryKey: ['henkiloTiedot', oppijaNumero],
    queryFn: () => getOppijanTiedot(oppijaNumero),
  });

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
