import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { AmmatillinenSuoritus } from '@/types/ui-types';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslate } from '@tolgee/react';

export const AmmatillinenSuoritusPaper = ({
  suoritus,
}: {
  suoritus: AmmatillinenSuoritus;
}) => {
  const { t } = useTranslate();
  return (
    <SuoritusInfoPaper
      suorituksenNimi={suoritus.nimi}
      valmistumispaiva={suoritus.valmistumispaiva}
      topColor={ophColors.green2}
    >
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      {'suoritustapa' in suoritus && (
        <LabeledInfoItem
          label={t('oppija.suoritustapa')}
          value={suoritus.suoritustapa}
        />
      )}
    </SuoritusInfoPaper>
  );
};
