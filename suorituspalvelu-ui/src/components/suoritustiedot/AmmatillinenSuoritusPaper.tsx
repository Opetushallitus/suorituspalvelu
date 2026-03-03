import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import type { AmmatillinenSuoritus } from '@/types/ui-types';
import type { IOsittainenAmmatillinenTutkinto } from '@/types/backend';
import { LabeledInfoItem } from '../LabeledInfoItem';
import { useTranslations } from '@/hooks/useTranslations';
import { pointToComma } from '@/lib/common';
import { InfoItemRow } from '../InfoItemRow';
import { TutkinnonOsatTable } from './TutkinnonOsatTable';
import { OsittaisenTutkinnonOsatTable } from './OsittaisenTutkinnonOsatTable';

function isOsittainenSuoritus(
  suoritus: AmmatillinenSuoritus,
): suoritus is IOsittainenAmmatillinenTutkinto & {
  koulutustyyppi: 'ammatillinen';
  osittainen: true;
} {
  return suoritus.osittainen === true;
}

export const AmmatillinenSuoritusPaper = ({
  suoritus,
}: {
  suoritus: AmmatillinenSuoritus;
}) => {
  const { t } = useTranslations();
  return (
    <SuoritusInfoPaper
      suoritus={suoritus}
      topColor={ophColors.green2}
      nameSuffix={suoritus.osittainen ? t('oppija.osittainen') : undefined}
    >
      <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
      <InfoItemRow slotAmount={4}>
        {'suoritustapa' in suoritus && suoritus.suoritustapa && (
          <LabeledInfoItem
            label={t('oppija.suoritustapa')}
            value={t(`suoritustapa.${suoritus.suoritustapa}`)}
          />
        )}
        {'painotettuKeskiarvo' in suoritus && suoritus.painotettuKeskiarvo && (
          <LabeledInfoItem
            label={t('oppija.painotettu-keskiarvo')}
            value={pointToComma(suoritus.painotettuKeskiarvo)}
          />
        )}
        {'korotettuPainotettuKeskiarvo' in suoritus &&
          suoritus.korotettuPainotettuKeskiarvo && (
            <LabeledInfoItem
              label={t('oppija.korotettu-painotettu-keskiarvo')}
              value={pointToComma(suoritus.korotettuPainotettuKeskiarvo)}
            />
          )}
      </InfoItemRow>
      {isOsittainenSuoritus(suoritus) ? (
        <>
          <OsittaisenTutkinnonOsatTable
            tutkinnonOsat={suoritus.ytot}
            title={t('oppija.yhteiset-tutkinnon-osat')}
            maxKokonaislaajuus={35}
            testId="yhteiset-tutkinnon-osat-table"
          />
          <OsittaisenTutkinnonOsatTable
            tutkinnonOsat={suoritus.ammatillisenTutkinnonOsat}
            title={t('oppija.ammatilliset-tutkinnon-osat')}
            maxKokonaislaajuus={145}
            testId="ammatilliset-tutkinnon-osat-table"
          />
        </>
      ) : (
        <>
          {'ytot' in suoritus && (
            <TutkinnonOsatTable
              tutkinnonOsat={suoritus.ytot}
              title={t('oppija.yhteiset-tutkinnon-osat')}
              maxKokonaislaajuus={35}
              testId="yhteiset-tutkinnon-osat-table"
            />
          )}
          {'ammatillisenTutkinnonOsat' in suoritus && (
            <TutkinnonOsatTable
              tutkinnonOsat={suoritus.ammatillisenTutkinnonOsat}
              title={t('oppija.ammatilliset-tutkinnon-osat')}
              maxKokonaislaajuus={145}
              testId="ammatilliset-tutkinnon-osat-table"
            />
          )}
        </>
      )}
    </SuoritusInfoPaper>
  );
};
