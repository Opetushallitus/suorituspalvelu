import { Stack } from '@mui/material';
import { Opiskeluoikeudet } from '@/components/suoritustiedot/Opiskeluoikeudet';
import { Suoritukset } from '@/components/suoritustiedot/Suoritukset';
import { Vastaanotot } from '@/components/suoritustiedot/Vastaanotot';
import { useOppija } from '@/lib/suorituspalvelu-queries';
import { SuoritusManagerProvider } from '@/lib/suoritusManager';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { useOutletContext, type OppijaContext } from 'react-router';

export default function SuoritustiedotPage() {
  const { oppijaNumero } = useOutletContext<OppijaContext>();

  const { data: tiedot } = useOppija(oppijaNumero);

  if (!tiedot) {
    throw new Error(
      `Oppijaa ei löytynyt suorituspalvelusta oppijanumerolla: ${oppijaNumero}`,
    );
  }

  return (
    <QuerySuspenseBoundary>
      <Stack spacing={6}>
        <SuoritusManagerProvider>
          <Opiskeluoikeudet oppijanTiedot={tiedot} />
          <Suoritukset oppijanTiedot={tiedot} />
          <Vastaanotot oppijaNumero={tiedot.oppijaNumero} />
        </SuoritusManagerProvider>
      </Stack>
    </QuerySuspenseBoundary>
  );
}
