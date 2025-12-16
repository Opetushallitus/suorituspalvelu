import { YliajoManagerProvider } from '@/lib/yliajoManager';
import { OpiskelijavalintaanSiirtyvatTiedot } from './OpiskelijavalintaanSiirtyvatTiedot';
import { HakemukseltaTulevatTiedot } from './HakemukseltaTulevatTiedot';
import { useApiSuspenseQuery } from '@/lib/http-client';
import { queryOptionsGetValintadata } from '@/lib/suorituspalvelu-queries';
import { Stack } from '@mui/material';

export function OpiskelijavalinnanTiedotContent({
  oppijaNumero,
  hakuOid,
}: {
  oppijaNumero: string;
  hakuOid: string;
}) {
  const { data: valintaData } = useApiSuspenseQuery(
    queryOptionsGetValintadata({ oppijaNumero, hakuOid }),
  );
  return (
    <YliajoManagerProvider hakuOid={hakuOid}>
      <Stack spacing={3}>
        <OpiskelijavalintaanSiirtyvatTiedot
          oppijaNumero={oppijaNumero}
          hakuOid={hakuOid}
          valintaData={valintaData}
        />
        <HakemukseltaTulevatTiedot valintaData={valintaData} />
      </Stack>
    </YliajoManagerProvider>
  );
}
