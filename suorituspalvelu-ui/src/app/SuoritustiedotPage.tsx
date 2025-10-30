import { Stack } from '@mui/material';
import { Opiskeluoikeudet } from '@/components/suoritustiedot/Opiskeluoikeudet';
import { Suoritukset } from '@/components/suoritustiedot/Suoritukset';
import type { Route } from './+types/SuoritustiedotPage';
import { useOppija } from '@/lib/suorituspalvelu-queries';

export default function SuoritustiedotPage({ params }: Route.ComponentProps) {
  const { data: tiedot } = useOppija(params.oppijaNumero);

  return (
    <Stack spacing={6}>
      <Opiskeluoikeudet opiskeluoikeudet={tiedot?.opiskeluoikeudet} />
      <Suoritukset oppijanTiedot={tiedot} />
    </Stack>
  );
}
