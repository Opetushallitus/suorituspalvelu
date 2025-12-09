import { useOppilaitoksenOppijatSearchParamsState } from '@/hooks/useOppilaitoksenOppijatSearch';
import {
  queryOptionsGetOppilaitosVuosiOptions,
  queryOptionsGetOppilaitosVuosiLuokatOptions,
  useOppilaitoksetOptions,
} from '@/lib/suorituspalvelu-queries';
import { InputAdornment } from '@mui/material';
import { OphSelectFormField } from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import { useApiQuery } from '@/lib/http-client';
import { SpinnerIcon } from './SpinnerIcon';
import { useEffect } from 'react';
import { only } from 'remeda';
import { StyledSearchControls } from './StyledSearchControls';
import { getCurrentYear } from '@/lib/common';

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

  const { setSearchParams } = useOppilaitoksenOppijatSearchParamsState();

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
      value={value ?? ''}
      label={t('oppilaitos')}
      onChange={onChange}
    />
  );
};

const VuosiSelectField = ({
  value,
  onChange,
  oppilaitosOid,
}: Pick<
  React.ComponentProps<typeof OphSelectFormField>,
  'value' | 'onChange'
> & { oppilaitosOid?: string }) => {
  const { t } = useTranslations();

  const { data: vuodetOptions, isLoading } = useApiQuery(
    queryOptionsGetOppilaitosVuosiOptions({ oppilaitosOid }),
  );

  const { setSearchParams } = useOppilaitoksenOppijatSearchParamsState();

  useEffect(() => {
    if (oppilaitosOid && !value) {
      setSearchParams({
        vuosi: getCurrentYear(),
      });
    }
  }, [oppilaitosOid, value, setSearchParams]);

  return (
    <OphSelectWithLoading
      isLoading={isLoading}
      sx={{ minWidth: '150px' }}
      options={vuodetOptions ?? []}
      placeholder={t('select.valitse')}
      value={value ?? ''}
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
> & { oppilaitosOid?: string; vuosi?: string }) => {
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
      value={value ?? ''}
      disabled={vuosi == null || oppilaitosOid == null}
      label={t('search.luokka')}
      onChange={onChange}
    />
  );
};

export const TarkastusSearchControls = () => {
  const { setSearchParams, searchParams } =
    useOppilaitoksenOppijatSearchParamsState();
  const { oppilaitos, vuosi, luokka } = searchParams;

  return (
    <StyledSearchControls>
      <OppilaitosSelectField
        value={oppilaitos}
        onChange={(e) => {
          const newOppilaitos = e.target.value;
          if (newOppilaitos !== oppilaitos) {
            setSearchParams({
              oppilaitos: newOppilaitos,
              vuosi: getCurrentYear(),
              luokka: '',
              suodatus: '',
            });
          }
        }}
      />
      <VuosiSelectField
        oppilaitosOid={oppilaitos}
        value={vuosi}
        onChange={(e) => {
          const newVuosi = e.target.value;
          if (newVuosi !== vuosi) {
            setSearchParams({ vuosi: newVuosi, luokka: '', suodatus: '' });
          }
        }}
      />
      <LuokkaSelectField
        value={luokka}
        vuosi={vuosi}
        oppilaitosOid={oppilaitos}
        onChange={(e) => {
          setSearchParams({ luokka: e.target.value, suodatus: '' });
        }}
      />
    </StyledSearchControls>
  );
};
