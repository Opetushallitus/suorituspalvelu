import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { VapaaSivistystyoSuoritus } from '@/types/ui-types';

export const VapaaSivistystyoSuoritusPaper = ({
  vapaaSivistystyoSuoritus,
}: {
  vapaaSivistystyoSuoritus: VapaaSivistystyoSuoritus;
}) => {
  return (
    <SuoritusInfoPaper
      key={vapaaSivistystyoSuoritus.oppilaitos.oid}
      suorituksenNimi={vapaaSivistystyoSuoritus.nimi}
      valmistumispaiva={vapaaSivistystyoSuoritus.valmistumispaiva}
      topColor={ophColors.cyan1}
    >
      <SuorituksenPerustiedotIndicator perustiedot={vapaaSivistystyoSuoritus} />
    </SuoritusInfoPaper>
  );
};
