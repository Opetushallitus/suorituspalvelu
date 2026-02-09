import { ophColors, OphTypography } from '@opetushallitus/oph-design-system';
import { SuoritusInfoPaper } from './SuoritusInfoPaper';
import { SuorituksenPerustiedotIndicator } from './SuorituksenPerustiedotIndicator';
import type { KorkeakouluSuoritus } from '@/types/ui-types';
import { useTranslations } from '@/hooks/useTranslations';
import { isEmptyish } from 'remeda';

const SHOW_OPINTOJAKSOT = false;

export const KorkeakouluSuoritusPaper = ({
  suoritus,
}: {
  suoritus?: KorkeakouluSuoritus | undefined;
}) => {
  const { translateKielistetty } = useTranslations();
  return (
    suoritus && (
      <SuoritusInfoPaper suoritus={suoritus} topColor={ophColors.red1}>
        <SuorituksenPerustiedotIndicator perustiedot={suoritus} />
        {SHOW_OPINTOJAKSOT && !isEmptyish(suoritus?.opintojaksot) && (
          <>
            <OphTypography variant="h5" sx={{ marginTop: 2 }}>
              Opintosuoritukset
            </OphTypography>
            <ul>
              {suoritus.opintojaksot?.map((opintojakso) => {
                return (
                  <li key={opintojakso.tunniste}>
                    {translateKielistetty(opintojakso.nimi)}
                  </li>
                );
              })}
            </ul>
          </>
        )}
      </SuoritusInfoPaper>
    )
  );
};
