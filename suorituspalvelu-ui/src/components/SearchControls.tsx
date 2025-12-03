import { useOppijatSearchParamsState } from '@/hooks/useSearchOppijat';
import { DEFAULT_BOX_BORDER } from '@/lib/theme';
import {
  queryOptionsGetOppilaitosVuodet,
  queryOptionsGetOppilaitosVuosiLuokat,
  useKayttaja,
  useOppilaitoksetOptions,
} from '@/lib/suorituspalvelu-queries';
import { InputAdornment, Stack } from '@mui/material';
import {
  OphInputFormField,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { useTranslations } from '@/hooks/useTranslations';
import { useApiQuery } from '@/lib/http-client';
import { useSelectedSearchTab } from '@/hooks/useSelectedSearchTab';
import { SpinnerIcon } from './SpinnerIcon';

const OphSelectWithLoading = ({
  isLoading,
  ...props
}: React.ComponentProps<typeof OphSelectFormField> & {
  isLoading?: boolean;
}) => {
  const { t } = useTranslations();
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
      placeholder={isLoading ? t('ladataan') : props.placeholder}
      disabled={isLoading}
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

  const { data: oppilaitoksetOptions, isLoading } = useOppilaitoksetOptions();

  return (
    <OphSelectWithLoading
      isLoading={isLoading}
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

  return (
    <OphSelectWithLoading
      isLoading={isLoading}
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

  return (
    <OphSelectWithLoading
      isLoading={isLoading}
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
    </>
  );
};

export function SearchControls() {
  const { t } = useTranslations();

  const selectedSearchTab = useSelectedSearchTab();

  const { tunniste, setSearchParams } = useOppijatSearchParamsState();

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
          {selectedSearchTab === 'tarkistus' &&
            isOppilaitosOppijaHakuAllowed && <KatselijaSearchControls />}
        </Stack>
      </Stack>
    </Stack>
  );
}
