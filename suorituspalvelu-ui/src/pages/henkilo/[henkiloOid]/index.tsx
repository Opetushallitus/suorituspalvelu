'use client';
import { FullSpinner } from '@/components/FullSpinner';
import { configPromise } from '@/configuration';
import { client, useApiSuspenseQuery } from '@/http-client';
import { useRouter } from 'next/router';
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

export default function Page() {
  const router = useRouter();
  const { henkiloOid } = router.query;

  if (typeof henkiloOid === 'string') {
    return (
      <Suspense fallback={<FullSpinner />}>
        <Content oppijaNumero={henkiloOid} />
      </Suspense>
    );
  }
}
