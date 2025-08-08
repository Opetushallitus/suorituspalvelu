import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import { LabeledInfoItem } from './LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { KorkeakouluSuoritus } from '@/types/ui-types';

export const KorkeakouluSuoritusPaper = ({
  suoritus,
}: {
  suoritus?: KorkeakouluSuoritus | undefined;
}) => {
  const { t, translateKielistetty } = useTranslations();

  return (
    suoritus && (
      <SuoritusInfoPaper
        key={suoritus.tunniste}
        suorituksenNimi={translateKielistetty(suoritus.nimi)}
        valmistumispaiva={suoritus.valmistumispaiva}
        topColor={ophColors.red1}
      >
        <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
        <LabeledInfoItem
          label={t('oppija.hakukohde')}
          value={translateKielistetty(suoritus.hakukohde.nimi)}
        />
      </SuoritusInfoPaper>
    )
  );
};
