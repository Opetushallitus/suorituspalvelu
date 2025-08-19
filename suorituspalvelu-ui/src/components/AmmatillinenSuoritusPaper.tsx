import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { AmmatillinenSuoritus } from '@/types/ui-types';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { NonEmptyStack } from './NonEmptyStack';
import { YhteisetTutkinnonOsatTable } from './YhteisetTutkinnonOsatTable';

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
      <NonEmptyStack direction="row">
        {'suoritustapa' in suoritus && suoritus.suoritustapa && (
          <LabeledInfoItem
            label={t('oppija.suoritustapa')}
            value={t(`suoritustapa.${suoritus.suoritustapa}`)}
          />
        )}
        {'painotettuKeskiarvo' in suoritus && suoritus.painotettuKeskiarvo && (
          <LabeledInfoItem
            label={t('oppija.painotettu-keskiarvo')}
            value={suoritus.painotettuKeskiarvo}
          />
        )}
      </NonEmptyStack>
      <YhteisetTutkinnonOsatTable suoritus={suoritus} />
    </SuoritusInfoPaper>
  );
};
