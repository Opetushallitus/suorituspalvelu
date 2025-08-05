import { OppijanTiedot } from '@/types/ui-types';
import { KorkeakouluSuoritusPaper } from './KorkeakouluSuoritusPaper';
import { LukioSuoritusPaper } from './LukioSuoritusPaper';
import { VapaaSivistystyoSuoritusPaper } from './VapaaSivistystyoSuoritusPaper';
import { TuvaSuoritusPaper } from './TuvaSuoritusPaper';
import { PerusopetusSuoritusPaper } from './PerusopetusSuoritusPaper';
import { Stack } from '@mui/material';
import { useSuorituksetFlattened } from '@/hooks/useSuorituksetFlattened';
import { AmmatillinenSuoritusPaper } from './AmmatillinenSuoritusPaper';

function SuoritusPaper({
  suoritus,
}: {
  suoritus: ReturnType<typeof useSuorituksetFlattened>[number];
}) {
  switch (suoritus.koulutustyyppi) {
    case 'korkeakoulutus':
      return <KorkeakouluSuoritusPaper suoritus={suoritus} />;
    case 'ammatillinen':
      return <AmmatillinenSuoritusPaper suoritus={suoritus} />;
    case 'lukio':
      return <LukioSuoritusPaper suoritus={suoritus} />;
    case 'vapaa-sivistystyo':
      return <VapaaSivistystyoSuoritusPaper suoritus={suoritus} />;
    case 'tuva':
      return <TuvaSuoritusPaper suoritus={suoritus} />;
    case 'perusopetus':
      return <PerusopetusSuoritusPaper suoritus={suoritus} />;
    default:
      return null;
  }
}

export function SuorituksetAikajarjestyksessa({
  oppijanTiedot,
}: {
  oppijanTiedot: OppijanTiedot;
}) {
  const suoritukset = useSuorituksetFlattened(oppijanTiedot, true);

  return (
    <Stack spacing={4}>
      {suoritukset.map((suoritus) => (
        <SuoritusPaper key={suoritus.key} suoritus={suoritus} />
      ))}
    </Stack>
  );
}
