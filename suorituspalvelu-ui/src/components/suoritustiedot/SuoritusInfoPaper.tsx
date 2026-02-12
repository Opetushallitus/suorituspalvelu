import { OphTypography } from '@opetushallitus/oph-design-system';
import { PaperWithTopColor } from '@/components/PaperWithTopColor';
import { Stack } from '@mui/material';
import type { SuorituksenPerustiedot } from '@/types/ui-types';
import { useTranslations } from '@/hooks/useTranslations';
import { formatFinnishDate, NDASH } from '@/lib/common';

const Vuodet = ({ suoritus }: { suoritus: SuorituksenPerustiedot }) => {
  return (
    <OphTypography variant="body1" component="span">
      {suoritus.aloituspaiva || suoritus.valmistumispaiva
        ? `(${formatFinnishDate(suoritus.aloituspaiva)} ${NDASH} ${formatFinnishDate(suoritus.valmistumispaiva)})`
        : ''}
    </OphTypography>
  );
};

export const SuoritusInfoPaper = ({
  suoritus,
  headingLevel = 'h3',
  topColor,
  children,
}: {
  suoritus: SuorituksenPerustiedot;
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
