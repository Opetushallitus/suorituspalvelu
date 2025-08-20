import { TUVASuoritus } from '@/types/ui-types';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { useTranslations } from '@/hooks/useTranslations';
import { ophColors } from '@opetushallitus/oph-design-system';
import { LabeledInfoItem } from './LabeledInfoItem';

export const TuvaSuoritusPaper = ({ suoritus }: { suoritus: TUVASuoritus }) => {
  const { t, translateKielistetty } = useTranslations();
  return (
    <SuoritusInfoPaper suoritus={suoritus} topColor={ophColors.yellow1}>
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      <LabeledInfoItem
        label={t('oppija.suoritettu')}
        value={`${suoritus.laajuus?.arvo} ${translateKielistetty(suoritus.laajuus?.yksikko)}`}
      />
    </SuoritusInfoPaper>
  );
};
