import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { KorkeakouluSuoritus } from '@/types/ui-types';

export const KorkeakouluSuoritusPaper = ({
  suoritus,
}: {
  suoritus?: KorkeakouluSuoritus | undefined;
}) => {
  return (
    suoritus && (
      <SuoritusInfoPaper suoritus={suoritus} topColor={ophColors.red1}>
        <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      </SuoritusInfoPaper>
    )
  );
};
