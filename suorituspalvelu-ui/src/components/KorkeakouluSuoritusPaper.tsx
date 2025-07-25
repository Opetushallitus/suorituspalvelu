import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslate } from '@tolgee/react';
import { KorkeakouluSuoritus } from '@/types/ui-types';

export const KorkeakouluSuoritusPaper = ({
  korkeakouluSuoritus,
}: {
  korkeakouluSuoritus: KorkeakouluSuoritus;
}) => {
  const { t } = useTranslate();
  return (
    <SuoritusInfoPaper
      key={korkeakouluSuoritus.tutkinto}
      suorituksenNimi={korkeakouluSuoritus.tutkinto}
      valmistumispaiva={korkeakouluSuoritus.valmistumispaiva}
      topColor={ophColors.red1}
    >
      <SuorituksenPerustiedotIndicator perustiedot={korkeakouluSuoritus} />
      <LabeledInfoItem
        label={t('oppija.hakukohde')}
        value={korkeakouluSuoritus.hakukohde.nimi}
      />
    </SuoritusInfoPaper>
  );
};
