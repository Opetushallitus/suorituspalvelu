import { OppijaResponse } from '@/types/ui-types';
import { KorkeakouluSuoritusPaper } from './KorkeakouluSuoritusPaper';
import { LukioSuoritusPaper } from './LukioSuoritusPaper';
import { VapaaSivistystyoSuoritusPaper } from './VapaaSivistystyoSuoritusPaper';
import { TuvaSuoritusPaper } from './TuvaSuoritusPaper';
import { PerusopetusSuoritusPaper } from './PerusopetusSuoritusPaper';
import { Stack } from '@mui/material';
import { useSuorituksetFlattened } from '@/hooks/useSuorituksetFlattened';
import { AmmatillinenSuoritusPaper } from './AmmatillinenSuoritusPaper';

const SuoritusPaper = ({
  suoritus,
}: {
  suoritus: ReturnType<typeof useSuorituksetFlattened>[number];
}) => {
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
};

export const SuorituksetAikajarjestyksessa = ({
  tiedot,
}: {
  tiedot: OppijaResponse;
}) => {
  const suoritukset = useSuorituksetFlattened(tiedot, true);

  return (
    <Stack spacing={4}>
      {suoritukset.map((suoritus) => (
        <SuoritusPaper key={suoritus.key} suoritus={suoritus} />
      ))}
    </Stack>
  );
};
