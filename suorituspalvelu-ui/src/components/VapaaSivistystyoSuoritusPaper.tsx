import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import type { VapaaSivistystyoSuoritus } from '@/types/ui-types';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';

export const VapaaSivistystyoSuoritusPaper = ({
  suoritus,
}: {
  suoritus: VapaaSivistystyoSuoritus;
}) => {
  const { t } = useTranslations();

  return (
    <SuoritusInfoPaper suoritus={suoritus} topColor={ophColors.cyan1}>
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      <LabeledInfoItem
        label={t('oppija.suoritettu')}
        value={`${suoritus.laajuus} ${t('oppija.lyhenne-opintopiste')}`}
      />
    </SuoritusInfoPaper>
  );
};
