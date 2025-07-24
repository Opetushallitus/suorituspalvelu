import { useOppijatSearchParamsState } from '@/hooks/useSearchOppijat';
import { useApiSuspenseQuery } from '@/http-client';
import { DEFAULT_BOX_BORDER } from '@/lib/theme';
import { queryOptionsGetOppilaitokset } from '@/queries';
import { Stack } from '@mui/material';
import {
  OphInputFormField,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { useTranslate } from '@tolgee/react';

const OppilaitosSelectField = ({
  value,
  onChange,
}: Pick<
  React.ComponentProps<typeof OphSelectFormField>,
  'value' | 'onChange'
>) => {
  const { t } = useTranslate();

  const { data: oppilaitoksetOptions } = useApiSuspenseQuery({
    ...queryOptionsGetOppilaitokset(),
    select: (data) =>
      data?.data?.oppilaitokset?.map(($) => ({
        value: $.oid,
        label: $.nimi,
      })) ?? [],
  });

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
  const { t } = useTranslate();

  const { oppijaSearchTerm, setOppijaSearchTerm, oppilaitos, setOppilaitos } =
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
        value={oppijaSearchTerm ?? ''}
        onChange={(e) => {
          setOppijaSearchTerm(e.target.value);
        }}
      />
      <OppilaitosSelectField
        value={oppilaitos ?? ''}
        onChange={(e) => {
          setOppilaitos(e.target.value);
        }}
      />
    </Stack>
  );
}
