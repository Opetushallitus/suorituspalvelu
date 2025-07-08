import { configPromise } from '@/configuration';
import { client } from '@/http-client';
import { useRouter } from 'next/router';
import { useEffect, useState } from 'react';

const getOppijanTiedot = async (oppijaNumero: string) => {
  const config = await configPromise;
  // eslint-disable-next-line
  return client.get<any>(
    `${config.routes.suorituspalvelu.oppijanTiedotUrl}/${oppijaNumero}`,
  );
};

export default function Page() {
  const router = useRouter();
  const { henkiloOid } = router.query;

  const [tiedot, setTiedot] = useState(null);
  useEffect(() => {
    const fetchData = async () => {
      if (typeof henkiloOid === 'string') {
        const response = await getOppijanTiedot(henkiloOid);
        setTiedot(response.data);
      }
    };
    fetchData();
  }, [henkiloOid]);

  return <p>{JSON.stringify(tiedot)}</p>;
}
