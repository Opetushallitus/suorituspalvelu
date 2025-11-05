import { useOppijatSearchParamsState } from '@/hooks/useSearchOppijat';
import { DEFAULT_BOX_BORDER } from '@/lib/theme';
import { useOppilaitoksetOptions } from '@/lib/suorituspalvelu-queries';
import { Stack } from '@mui/material';
import {
  OphInputFormField,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';

const OppilaitosSelectField = ({
  value,
  onChange,
}: Pick<
  React.ComponentProps<typeof OphSelectFormField>,
  'value' | 'onChange'
>) => {
  const { t } = useTranslations();

  const oppilaitoksetOptions = useOppilaitoksetOptions();

  return (
    <OphSelectFormField
      sx={{ minWidth: '200px' }}
      options={oppilaitoksetOptions}
      clearable={true}
      placeholder={t('select.valitse')}
      value={value}
      label={t('oppilaitos')}
      onChange={onChange}
    />
  );
};

export function SearchControls() {
  const { t } = useTranslations();

  const { tunniste, setTunniste, oppilaitos, setOppilaitos } =
    useOppijatSearchParamsState();

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
