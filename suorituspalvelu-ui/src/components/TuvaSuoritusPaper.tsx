import { TUVASuoritus } from '@/types/ui-types';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { useTranslate } from '@tolgee/react';
import { ophColors } from '@opetushallitus/oph-design-system';
import { LabeledInfoItem } from './LabeledInfoItem';

export const TuvaSuoritusPaper = ({ suoritus }: { suoritus: TUVASuoritus }) => {
  const { t } = useTranslate();
  return (
    <SuoritusInfoPaper
      key={suoritus.oppilaitos.oid}
      suorituksenNimi={t('oppija.tuva')}
      valmistumispaiva={suoritus.valmistumispaiva}
      topColor={ophColors.yellow1}
    >
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      <LabeledInfoItem
        label={t('oppija.suoritettu')}
        value={`${suoritus.laajuus} ${t('oppija.lyhenne-viikko')}`}
      />
    </SuoritusInfoPaper>
  );
};
