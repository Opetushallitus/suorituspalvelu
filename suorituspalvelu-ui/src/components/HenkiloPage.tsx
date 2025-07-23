'use client';
import { FullSpinner } from '@/components/FullSpinner';
import { useOppija } from '@/queries';
import { Stack } from '@mui/material';
import { Suspense } from 'react';
import { Opiskeluoikeudet } from './Opiskeluoikeudet';

const OppijaContent = ({ oppijaNumero }: { oppijaNumero: string }) => {
  const { data: tiedot } = useOppija(oppijaNumero);
  return (
    <Stack sx={{ margin: 2, gap: 2 }}>
      <Opiskeluoikeudet opiskeluoikeudet={tiedot?.opiskeluoikeudet} />
    </Stack>
  );
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
