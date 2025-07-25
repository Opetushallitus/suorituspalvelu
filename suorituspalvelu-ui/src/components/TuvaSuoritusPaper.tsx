import { TUVASuoritus } from '@/types/ui-types';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { useTranslate } from '@tolgee/react';
import { ophColors } from '@opetushallitus/oph-design-system';

export const TuvaSuoritusPaper = ({
  tuvaSuoritus,
}: {
  tuvaSuoritus: TUVASuoritus;
}) => {
  const { t } = useTranslate();
  return (
    <SuoritusInfoPaper
      key={tuvaSuoritus.oppilaitos.oid}
      suorituksenNimi={t('oppija.tuva')}
      valmistumispaiva={tuvaSuoritus.valmistumispaiva}
      topColor={ophColors.yellow2}
    >
      <SuorituksenPerustiedotIndicator perustiedot={tuvaSuoritus} />
    </SuoritusInfoPaper>
  );
};
