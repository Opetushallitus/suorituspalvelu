import { ophColors } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import type { KorkeakouluSuoritus } from '@/types/ui-types';
import { useTranslations } from '@/hooks/useTranslations';
import { isEmptyish } from 'remeda';
import { SimpleAccordion } from '../SimpleAccordion';

export const KorkeakouluSuoritusPaper = ({
  suoritus,
}: {
  suoritus?: KorkeakouluSuoritus | undefined;
}) => {
  const { t, translateKielistetty } = useTranslations();
  return (
    suoritus && (
      <SuoritusInfoPaper suoritus={suoritus} topColor={ophColors.red1}>
        <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
        {!isEmptyish(suoritus?.opintojaksot) && (
          <SimpleAccordion
            titleClosed={t('oppija.nayta-opintosuoritukset')}
            titleOpen={t('oppija.piilota-opintosuoritukset')}
          >
            <ul>
              {suoritus.opintojaksot?.map((opintojakso) => {
                return (
                  <li key={opintojakso.tunniste}>
                    {translateKielistetty(opintojakso.nimi)}
                  </li>
                );
              })}
            </ul>
          </SimpleAccordion>
        )}
      </SuoritusInfoPaper>
    )
  );
};
