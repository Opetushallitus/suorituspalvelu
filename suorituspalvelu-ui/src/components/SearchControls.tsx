import { useOppijatSearchParamsState } from '@/hooks/useSearchOppijat';
import { DEFAULT_BOX_BORDER } from '@/lib/theme';
import {
  queryOptionsGetOppilaitosVuosiOptions,
  queryOptionsGetOppilaitosVuosiLuokatOptions,
  useKayttaja,
  useOppilaitoksetOptions,
} from '@/lib/suorituspalvelu-queries';
import { InputAdornment, Stack } from '@mui/material';
import { OphSelectFormField } from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import { useApiQuery } from '@/lib/http-client';
import { useSelectedSearchTab } from '@/hooks/useSelectedSearchTab';
import { SpinnerIcon } from './SpinnerIcon';
import { useEffect } from 'react';
import { useParams } from 'react-router';
import { SearchInput } from './SearchInput';
import { isHenkiloOid } from '@/lib/common';
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

const OppilaitoksenOppijatSearchControls = () => {
  const { setSearchParams, oppilaitos, luokka, vuosi } =
    useOppijatSearchParamsState();

  return (
    <>
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
    </>
  );
};

const HenkiloTunnisteellaSearchControls = () => {
  const { t } = useTranslations();
  const { oppijaNumero } = useParams();

  const { tunniste, setSearchParams } = useOppijatSearchParamsState();

  // Asetetaan ensimmäisellä kerralla tunnisteeksi validi oppijanumero, jos tunnistetta ei asetettu
  useEffect(() => {
    if (!tunniste && isHenkiloOid(oppijaNumero)) {
      setSearchParams(
        {
          tunniste: oppijaNumero,
        },
        { replace: true },
      );
    }
  }, []);

  return (
    <SearchInput
      sx={{
        flex: 1,
        maxWidth: '400px',
      }}
      label={t('search.hae-henkilo')}
      value={tunniste ?? ''}
      placeholder={t('search.henkilo-input-placeholder')}
      onClear={() => {
        setSearchParams({ tunniste: '' });
      }}
      onChange={(value) => {
        setSearchParams({ tunniste: value });
      }}
    />
  );
};

export function SearchControls() {
  const selectedSearchTab = useSelectedSearchTab();

  const { data: kayttaja } = useKayttaja();

  const isOppilaitosOppijaHakuAllowed =
    kayttaja.isRekisterinpitaja || kayttaja.isOrganisaationKatselija;

  return (
    <Stack>
      <Stack
        direction="column"
        sx={{
          padding: 2,
          borderBottom: DEFAULT_BOX_BORDER,
          justifyContent: 'stretch',
          gap: 2,
        }}
      >
        <Stack direction="row" spacing={2}>
          {selectedSearchTab === 'henkilo' && (
            <HenkiloTunnisteellaSearchControls />
          )}
          {selectedSearchTab === 'tarkistus' &&
            isOppilaitosOppijaHakuAllowed && (
              <OppilaitoksenOppijatSearchControls />
            )}
        </Stack>
      </Stack>
    </Stack>
  );
}
