import { OphTypography } from '@opetushallitus/oph-design-system';
import { PaperWithTopColor } from './PaperWithTopColor';
import { formatDate } from 'date-fns';
import { Stack } from '@mui/material';

export const SuoritusInfoPaper = ({
  suorituksenNimi,
  valmistumispaiva,
  headingLevel = 'h3',
  topColor,
  children,
}: {
  valmistumispaiva?: Date;
  suorituksenNimi: string;
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5';
  topColor: string;
  children: React.ReactNode;
}) => {
  return (
    <PaperWithTopColor topColor={topColor}>
      <OphTypography
        variant="h5"
        component={headingLevel}
        sx={{ marginBottom: 2 }}
      >
        {suorituksenNimi}{' '}
        {valmistumispaiva && (
          <OphTypography variant="body1" component="span">
            ({formatDate(valmistumispaiva, 'y')})
          </OphTypography>
        )}
      </OphTypography>
      <Stack spacing={4}>{children}</Stack>
    </PaperWithTopColor>
  );
};
