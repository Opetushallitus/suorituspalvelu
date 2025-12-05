import { useOppijatSearchParamsState } from '@/hooks/useSearchOppijat';
import { DEFAULT_BOX_BORDER, styled } from '@/lib/theme';
import {
  queryOptionsGetOppilaitosVuosiOptions,
  queryOptionsGetOppilaitosVuosiLuokatOptions,
  useOppilaitoksetOptions,
} from '@/lib/suorituspalvelu-queries';
import { InputAdornment, Stack } from '@mui/material';
import { OphSelectFormField } from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import { useApiQuery } from '@/lib/http-client';
import { SpinnerIcon } from './SpinnerIcon';
import { useCallback, useEffect } from 'react';
import { SearchInput } from './SearchInput';
import { only } from 'remeda';

const OphSelectWithLoading = ({
  isLoading,
  disabled,
  ...props
}: React.ComponentProps<typeof OphSelectFormField> & {
  isLoading?: boolean;
}) => {
  return (
    <OphSelectFormField
      {...props}
      startAdornment={
        isLoading ? (
          <InputAdornment position="start" sx={{ paddingLeft: 1 }}>
            <SpinnerIcon />
          </InputAdornment>
        ) : undefined
      }
      value={isLoading ? '' : props.value}
      placeholder={isLoading ? '' : props.placeholder}
      disabled={disabled || isLoading}
    />
  );
};

const StyledSearchControls = styled(Stack)(({ theme }) => ({
  flexDirection: 'row',
  gap: theme.spacing(2),
  margin: theme.spacing(2, 2, 0, 2),
  paddingBottom: theme.spacing(2),
  borderBottom: DEFAULT_BOX_BORDER,
}));

const OppilaitosSelectField = ({
  value,
  onChange,
}: Pick<
  React.ComponentProps<typeof OphSelectFormField>,
  'value' | 'onChange'
>) => {
  const { t } = useTranslations();

  const { data: oppilaitoksetOptions = [], isLoading } =
    useOppilaitoksetOptions();

  const { setSearchParams } = useOppijatSearchParamsState();

  useEffect(() => {
    const onlyOppilaitosOption = only(oppilaitoksetOptions);
    if (onlyOppilaitosOption && !value) {
      setSearchParams({
        oppilaitos: onlyOppilaitosOption.value,
      });
    }
  }, [oppilaitoksetOptions, value, setSearchParams]);

  return (
    <OphSelectWithLoading
      isLoading={isLoading}
      sx={{ minWidth: '400px' }}
      options={oppilaitoksetOptions}
      placeholder={t('select.valitse')}
      value={value}
      label={t('oppilaitos')}
      onChange={onChange}
    />
  );
};

const CURRENT_YEAR = new Date().getFullYear().toString();

const DEFAULT_YEAR_OPTIONS = [
  {
    label: CURRENT_YEAR,
    value: CURRENT_YEAR,
  },
];

const VuosiSelectField = ({
  value,
  onChange,
  oppilaitosOid,
}: Pick<
  React.ComponentProps<typeof OphSelectFormField>,
  'value' | 'onChange'
> & { oppilaitosOid: string | null }) => {
  const { t } = useTranslations();

  const { data: vuodetOptions = DEFAULT_YEAR_OPTIONS, isLoading } = useApiQuery(
    queryOptionsGetOppilaitosVuosiOptions({ oppilaitosOid }),
  );

  const { setSearchParams } = useOppijatSearchParamsState();

  useEffect(() => {
    if (oppilaitosOid && !value) {
      setSearchParams({
        vuosi: CURRENT_YEAR,
      });
    }
  }, [oppilaitosOid, value, setSearchParams]);

  return (
    <OphSelectWithLoading
      isLoading={isLoading}
      sx={{ minWidth: '150px' }}
      options={vuodetOptions}
      placeholder={t('select.valitse')}
      value={value}
      disabled={!oppilaitosOid}
      label={t('search.valmistumisvuosi')}
      onChange={onChange}
    />
  );
};

const LuokkaSelectField = ({
  value,
  onChange,
  vuosi,
  oppilaitosOid,
}: Pick<
  React.ComponentProps<typeof OphSelectFormField>,
  'value' | 'onChange'
> & { oppilaitosOid: string | null; vuosi: string | null }) => {
  const { t } = useTranslations();

  const { data: luokatOptions = [], isLoading } = useApiQuery(
    queryOptionsGetOppilaitosVuosiLuokatOptions({
      vuosi,
      oppilaitosOid,
    }),
  );

  return (
    <OphSelectWithLoading
      isLoading={isLoading}
      sx={{ minWidth: '150px' }}
      options={luokatOptions}
      clearable={true}
      placeholder={t('select.valitse')}
      value={value}
      disabled={vuosi == null || oppilaitosOid == null}
      label={t('search.luokka')}
      onChange={onChange}
    />
  );
};

export const TarkistusSearchControls = () => {
  const { setSearchParams, oppilaitos, luokka, vuosi } =
    useOppijatSearchParamsState();

  return (
    <StyledSearchControls>
      <OppilaitosSelectField
        value={oppilaitos ?? ''}
        onChange={(e) => {
          const newOppilaitos = e.target.value;
          if (newOppilaitos !== oppilaitos) {
            setSearchParams({
              oppilaitos: newOppilaitos,
              vuosi: CURRENT_YEAR,
              luokka: '',
              tunniste: '',
            });
          }
        }}
      />
      <VuosiSelectField
        oppilaitosOid={oppilaitos}
        value={vuosi ?? ''}
        onChange={(e) => {
          const newVuosi = e.target.value;
          if (newVuosi !== vuosi) {
            setSearchParams({ vuosi: newVuosi, luokka: '', tunniste: '' });
          }
        }}
      />
      <LuokkaSelectField
        value={luokka ?? ''}
        vuosi={vuosi}
        oppilaitosOid={oppilaitos}
        onChange={(e) => {
          setSearchParams({ luokka: e.target.value, tunniste: '' });
        }}
      />
    </StyledSearchControls>
  );
};

export const HenkiloSearchControls = () => {
  const { t } = useTranslations();

  const { tunniste, setSearchParams } = useOppijatSearchParamsState();

  const onClear = useCallback(() => {
    setSearchParams({ tunniste: '' });
  }, [setSearchParams]);

  const onChange = useCallback(
    (value: string) => {
      setSearchParams({ tunniste: value });
    },
    [setSearchParams],
  );

  return (
    <StyledSearchControls>
      <SearchInput
        sx={{
          flex: 1,
          maxWidth: '400px',
        }}
        label={t('search.hae-henkilo')}
        value={tunniste ?? ''}
        placeholder={t('search.henkilo-input-placeholder')}
        onClear={onClear}
        onChange={onChange}
      />
    </StyledSearchControls>
  );
};
