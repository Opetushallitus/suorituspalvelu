import { Stack } from '@mui/material';
import { Opiskeluoikeudet } from '@/components/suoritustiedot/Opiskeluoikeudet';
import { Suoritukset } from '@/components/suoritustiedot/Suoritukset';
import type { Route } from './+types/SuoritustiedotPage';
import { useOppija } from '@/lib/suorituspalvelu-queries';
import { SuoritusManagerProvider } from '@/lib/suoritusManager';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';

export default function SuoritustiedotPage({ params }: Route.ComponentProps) {
  if (!params.oppijaNumero) {
    throw new Error('Ei voida näyttää suoritustietoja ilman oppijanumeroa');
  }

  const { data: tiedot } = useOppija(params.oppijaNumero);

  return (
    <QuerySuspenseBoundary>
      <Stack spacing={6}>
        <SuoritusManagerProvider>
          <Opiskeluoikeudet opiskeluoikeudet={tiedot?.opiskeluoikeudet} />
          <Suoritukset oppijanTiedot={tiedot} />
        </SuoritusManagerProvider>
      </Stack>
    </QuerySuspenseBoundary>
  );
}
