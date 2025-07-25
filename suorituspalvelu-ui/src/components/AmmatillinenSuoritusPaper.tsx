import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { AmmatillinenSuoritus } from '@/types/ui-types';

export const AmmatillinenSuoritusPaper = ({
  ammatillinenSuoritus,
}: {
  ammatillinenSuoritus: AmmatillinenSuoritus;
}) => {
  return (
    <SuoritusInfoPaper
      suorituksenNimi={ammatillinenSuoritus.nimi}
      valmistumispaiva={ammatillinenSuoritus.valmistumispaiva}
      topColor={ophColors.red1}
    >
      <SuorituksenPerustiedotIndicator perustiedot={ammatillinenSuoritus} />
    </SuoritusInfoPaper>
  );
};
