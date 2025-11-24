import { useOppijatSearchParamsState } from '@/hooks/useSearchOppijat';
import { DEFAULT_BOX_BORDER } from '@/lib/theme';
import {
  queryOptionsGetOppilaitosVuodet,
  queryOptionsGetOppilaitosVuosiLuokat,
  useKayttaja,
  useOppilaitoksetOptions,
} from '@/lib/suorituspalvelu-queries';
import { Stack, ToggleButton, ToggleButtonGroup } from '@mui/material';
import {
  OphInputFormField,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import { useApiQuery } from '@/lib/http-client';
import { useState } from 'react';
import { FullSpinner } from './FullSpinner';

const OppilaitosSelectField = ({
  value,
  onChange,
}: Pick<
  React.ComponentProps<typeof OphSelectFormField>,
  'value' | 'onChange'
>) => {
  const { t } = useTranslations();

  const { data: oppilaitoksetOptions, isLoading } = useOppilaitoksetOptions();

  return isLoading ? (
    <FullSpinner />
  ) : (
    <OphSelectFormField
      sx={{ minWidth: '250px' }}
      options={oppilaitoksetOptions ?? []}
      clearable={true}
      placeholder={t('select.valitse')}
      value={value}
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
> & { oppilaitosOid: string | null }) => {
  const { t } = useTranslations();

  const { data: vuodet = [], isLoading } = useApiQuery(
    queryOptionsGetOppilaitosVuodet({ oppilaitosOid }),
  );

  const vuodetOptions = vuodet.map((vuosi) => ({
    label: vuosi,
    value: vuosi,
  }));

  return isLoading ? (
    <FullSpinner />
  ) : (
    <OphSelectFormField
      sx={{ minWidth: '200px' }}
      options={vuodetOptions}
      clearable={true}
      placeholder={t('select.valitse')}
      value={value}
      disabled={!oppilaitosOid}
      label={t('vuosi')}
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

  const { data: luokat, isLoading } = useApiQuery(
    queryOptionsGetOppilaitosVuosiLuokat({
      vuosi,
      oppilaitosOid,
    }),
  );

  const luokatOptions = (luokat ?? []).map((luokka) => ({
    label: luokka,
    value: luokka,
  }));

  return isLoading ? (
    <FullSpinner />
  ) : (
    <OphSelectFormField
      sx={{ minWidth: '200px' }}
      options={luokatOptions}
      clearable={true}
      placeholder={t('select.valitse')}
      value={value}
      disabled={vuosi == null || oppilaitosOid == null}
      label={t('luokka')}
      onChange={onChange}
    />
  );
};

const KatselijaSearchControls = () => {
  const { t } = useTranslations();
  const { setSearchParams, tunniste, oppilaitos, luokka, vuosi } =
    useOppijatSearchParamsState();

  const onHakusanaChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchParams({
      tunniste: e.target.value,
    });
  };

  return (
    <>
      <OppilaitosSelectField
        value={oppilaitos ?? ''}
        onChange={(e) => {
          const newOppilaitos = e.target.value;
          if (newOppilaitos !== oppilaitos) {
            setSearchParams({
              oppilaitos: newOppilaitos,
              vuosi: '',
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
      <OphInputFormField
        sx={{
          flex: 1,
          maxWidth: '400px',
        }}
        label={t('henkilohaku.suodata-oppijoita')}
        disabled={oppilaitos == null || vuosi == null}
        value={tunniste ?? ''}
        onChange={onHakusanaChange}
      />
    </>
  );
};

type HenkilohakuTyyppi = 'vain-tunniste' | 'oppilaitoksen-oppijat';

export function SearchControls() {
  const { t } = useTranslations();

  const { tunniste, oppilaitos, setSearchParams } =
    useOppijatSearchParamsState();

  const [henkilohakuTyyppi, setHenkilohakuTyyppi] = useState<HenkilohakuTyyppi>(
    oppilaitos ? 'oppilaitoksen-oppijat' : 'vain-tunniste',
  );

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
        {isOppilaitosOppijaHakuAllowed && (
          <ToggleButtonGroup
            value={henkilohakuTyyppi}
            exclusive
            onChange={(_event, newValue) => {
              if (newValue && newValue !== henkilohakuTyyppi) {
                setHenkilohakuTyyppi(newValue);
                setSearchParams({
                  tunniste: '',
                  oppilaitos: '',
                  luokka: '',
                  vuosi: '',
                });
              }
            }}
          >
            <ToggleButton value="vain-tunniste">
              {t('henkilohaku.vain-tunniste')}
            </ToggleButton>
            <ToggleButton value="oppilaitoksen-oppijat">
              {t('henkilohaku.oppilaitoksen-oppijat')}
            </ToggleButton>
          </ToggleButtonGroup>
        )}
        <Stack direction="row" spacing={2}>
          {henkilohakuTyyppi === 'vain-tunniste' && (
            <OphInputFormField
              sx={{
                flex: 1,
                maxWidth: '400px',
              }}
              label={t('hae-henkiloa')}
              value={tunniste ?? ''}
              onChange={(e) => {
                setSearchParams({ tunniste: e.target.value });
              }}
            />
          )}
          {henkilohakuTyyppi === 'oppilaitoksen-oppijat' &&
            isOppilaitosOppijaHakuAllowed && <KatselijaSearchControls />}
        </Stack>
      </Stack>
    </Stack>
  );
}
