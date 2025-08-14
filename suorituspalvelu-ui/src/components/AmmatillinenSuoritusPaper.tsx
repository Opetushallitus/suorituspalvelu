import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { AmmatillinenSuoritus } from '@/types/ui-types';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { Stack } from '@mui/material';

export const AmmatillinenSuoritusPaper = ({
  suoritus,
}: {
  suoritus: AmmatillinenSuoritus;
}) => {
  const { t, translateKielistetty } = useTranslations();
  return (
    <SuoritusInfoPaper
      suorituksenNimi={translateKielistetty(suoritus.nimi)}
      valmistumispaiva={suoritus.valmistumispaiva}
      topColor={ophColors.green2}
    >
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      <Stack direction="row">
        {'suoritustapa' in suoritus && suoritus.suoritustapa && (
          <LabeledInfoItem
            label={t('oppija.suoritustapa')}
            value={t(`suoritustapa.${suoritus.suoritustapa}`)}
          />
        )}
        {'painotettuKeskiarvo' in suoritus && (
          <LabeledInfoItem
            label={t('oppija.painotettu-keskiarvo')}
            value={suoritus.painotettuKeskiarvo}
          />
        )}
      </Stack>
    </SuoritusInfoPaper>
  );
};
