import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslate } from '@tolgee/react';
import { KorkeakouluSuoritus } from '@/types/ui-types';

export const KorkeakouluSuoritusPaper = ({
  suoritus,
}: {
  suoritus?: KorkeakouluSuoritus | undefined;
}) => {
  const { t } = useTranslate();
  return (
    suoritus && (
      <SuoritusInfoPaper
        key={suoritus.tutkinto}
        suorituksenNimi={suoritus.tutkinto}
        valmistumispaiva={suoritus.valmistumispaiva}
        topColor={ophColors.red1}
      >
        <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
        <LabeledInfoItem
          label={t('oppija.hakukohde')}
          value={suoritus.hakukohde.nimi}
        />
      </SuoritusInfoPaper>
    )
  );
};
