import { Stack } from '@mui/material';
import { OphTypography } from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';

export default function OpiskelijavalinnanTiedotPage() {
  const { t } = useTranslations();

  return (
    <Stack spacing={3}>
      <OphTypography variant="h4">
        {t('opiskelijavalinnan-tiedot.otsikko')}
      </OphTypography>
      {/* TODO: Implement opiskelijavalinnan tiedot content */}
      <OphTypography>
        Opiskelijavalinnan tiedot sisältö tulossa...
      </OphTypography>
    </Stack>
  );
}
