import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { VapaaSivistystyoSuoritus } from '@/types/ui-types';

export const VapaaSivistystyoSuoritusPaper = ({
  suoritus,
}: {
  suoritus: VapaaSivistystyoSuoritus;
}) => {
  return (
    <SuoritusInfoPaper
      key={suoritus.oppilaitos.oid}
      suorituksenNimi={suoritus.nimi}
      valmistumispaiva={suoritus.valmistumispaiva}
      topColor={ophColors.cyan1}
    >
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
    </SuoritusInfoPaper>
  );
};
