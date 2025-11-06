import { useOppijatSearchParamsState } from '@/hooks/useSearchOppijat';
import { DEFAULT_BOX_BORDER } from '@/lib/theme';
import { Stack } from '@mui/material';
import { OphInputFormField } from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';

export function SearchControls() {
  const { t } = useTranslations();

  const { tunniste, setTunniste } = useOppijatSearchParamsState();

  return (
    <Stack
      direction="row"
      sx={{
        padding: 2,
        borderBottom: DEFAULT_BOX_BORDER,
        justifyContent: 'stretch',
        gap: 2,
      }}
    >
      <OphInputFormField
        sx={{
          maxWidth: '400px',
        }}
        label={t('hae-henkiloa')}
        value={tunniste ?? ''}
        onChange={(e) => {
          setTunniste(e.target.value);
        }}
      />
    </Stack>
  );
}
