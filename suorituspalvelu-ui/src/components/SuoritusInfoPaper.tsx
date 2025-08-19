import { OphTypography } from '@opetushallitus/oph-design-system';
import { PaperWithTopColor } from './PaperWithTopColor';
import { formatDate } from 'date-fns';
import { Stack } from '@mui/material';
import { SuoritusOtsikkoTiedot } from '@/types/ui-types';
import { useTranslations } from '@/hooks/useTranslations';
import { isNonNullish } from 'remeda';
import { NDASH } from '@/lib/common';

const Vuodet = ({ suoritus }: { suoritus: SuoritusOtsikkoTiedot }) => {
  const { t } = useTranslations();

  switch (true) {
    case isNonNullish(suoritus.valmistumispaiva):
      return (
        <OphTypography variant="body1" component="span">
          ({formatDate(suoritus.valmistumispaiva, 'y')})
        </OphTypography>
      );
    case isNonNullish(suoritus.aloituspaiva):
      return (
        <OphTypography variant="body1" component="span">
          {`(${formatDate(suoritus.aloituspaiva, 'y')} ${NDASH} ${t(`suorituksen-tila.${suoritus.tila}`)})`}
        </OphTypography>
      );
  }
};

export const SuoritusInfoPaper = ({
  suoritus,
  headingLevel = 'h3',
  topColor,
  children,
}: {
  suoritus: SuoritusOtsikkoTiedot;
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5';
  topColor: string;
  children: React.ReactNode;
}) => {
  const { translateKielistetty } = useTranslations();
  return (
    <PaperWithTopColor topColor={topColor} data-test-id="suoritus-paper">
      <OphTypography
        variant="h5"
        component={headingLevel}
        sx={{ marginBottom: 2 }}
      >
        {`${translateKielistetty(suoritus.nimi)} `}
        <Vuodet suoritus={suoritus} />
      </OphTypography>
      <Stack spacing={4}>{children}</Stack>
    </PaperWithTopColor>
  );
};
