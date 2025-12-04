import { FetchError, useApiSuspenseQuery } from '@/lib/http-client';
import { useTranslations } from '@/hooks/useTranslations';
import { isGenericBackendErrorResponse } from '@/types/ui-types';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { ErrorView } from '@/components/ErrorView';
import { ErrorAlert } from '@/components/ErrorAlert';
import { Box, Stack, ToggleButton, ToggleButtonGroup } from '@mui/material';
import { useCallback, useEffect, useState, useTransition } from 'react';
import {
  OpiskelijavalintaanSiirtyvatTiedot,
  type AvainarvoRyhma,
} from '@/components/opiskelijavalinnan-tiedot/OpiskelijavalintaanSiirtyvatTiedot';
import {
  queryOptionsGetOppijanHaut,
  queryOptionsGetValintadata,
  useKayttaja,
} from '@/lib/suorituspalvelu-queries';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { DoNotDisturb } from '@mui/icons-material';
import { YliajoManagerProvider } from '@/lib/yliajoManager';
import {
  OphFormFieldWrapper,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { only } from 'remeda';
import { useQueryParam } from '@/hooks/useQueryParam';
import { FullSpinner } from '@/components/FullSpinner';
import { queryClient } from '@/lib/queryClient';
import {
  useOutletContext,
  useSearchParams,
  type OppijaContext,
} from 'react-router';
import { HAKU_QUERY_PARAM_NAME } from '@/lib/common';

const OpiskelijavalinnanTiedotPageContent = ({
  oppijaNumero,
}: {
  oppijaNumero: string;
}) => {
  const { t, translateKielistetty } = useTranslations();

  const { data: kayttaja } = useKayttaja();

  const [avainarvoRyhma, setAvainarvoRyhma] =
    useState<AvainarvoRyhma>('uudet-avainarvot');

  const { data: haut } = useApiSuspenseQuery(
    queryOptionsGetOppijanHaut(oppijaNumero),
  );
  const [urlHakuOid, setUrlHakuOid] = useQueryParam(HAKU_QUERY_PARAM_NAME);

  // Ilman transitiota hakua vaihdettaessa ei tule näkyviin latausindikaattoria
  const [isHakuSwitching, startHakuSwitchTransition] = useTransition();

  const setHakuOidWithTransition = useCallback(
    (hakuOid: string) => {
      startHakuSwitchTransition(() => {
        setUrlHakuOid(hakuOid);
      });
    },
    [setUrlHakuOid],
  );

  const onlyHakuOid = only(haut)?.hakuOid;

  // Jos vain yksi haku valittavissa eikä URL-parametrissa valittu, asetetaan ainut haku URL-parametriksi
  useEffect(() => {
    if (!urlHakuOid && onlyHakuOid) {
      setHakuOidWithTransition(onlyHakuOid);
    }
  }, [urlHakuOid, onlyHakuOid, setHakuOidWithTransition]);

  const isValidHakuOid =
    urlHakuOid == null || haut.find((h) => h.hakuOid === urlHakuOid);

  const hakuError = isValidHakuOid
    ? undefined
    : t('opiskelijavalinnan-tiedot.valittu-haku-virheellinen');

  const hakuOptionsBase = haut.map((haku) => ({
    value: haku.hakuOid,
    label: translateKielistetty(haku.nimi),
  }));

  const hakuOptions = isValidHakuOid
    ? hakuOptionsBase
    : hakuOptionsBase.concat([
        {
          label: urlHakuOid,
          value: urlHakuOid,
        },
      ]);

  const selectedHakuOid = urlHakuOid ?? '';

  return kayttaja.isRekisterinpitaja ? (
    <Stack spacing={3} sx={{ height: '100%' }}>
      <Stack direction="row" sx={{ justifyContent: 'stretch', gap: 3 }}>
        <OphSelectFormField
          sx={{ flex: 1 }}
          label={t('opiskelijavalinnan-tiedot.haku')}
          value={selectedHakuOid}
          options={hakuOptions}
          errorMessage={hakuError}
          onChange={(event) => {
            setHakuOidWithTransition(event.target.value);
          }}
        />
        <OphFormFieldWrapper
          label={t('opiskelijavalinnan-tiedot.nayta')}
          renderInput={({ labelId }) => (
            <ToggleButtonGroup
              sx={{ alignSelf: 'flex-end' }}
              aria-labelledby={labelId}
              value={avainarvoRyhma}
              exclusive
              onChange={(_event, newValue) => {
                if (newValue) {
                  setAvainarvoRyhma(newValue);
                }
              }}
            >
              <ToggleButton value="uudet-avainarvot">
                {t('opiskelijavalinnan-tiedot.uudet-avainarvot')}
              </ToggleButton>
              <ToggleButton value="vanhat-avainarvot">
                {t('opiskelijavalinnan-tiedot.vanhat-avainarvot')}
              </ToggleButton>
            </ToggleButtonGroup>
          )}
        />
      </Stack>
      <Box sx={{ paddingTop: 1 }}>
        {isHakuSwitching ? (
          <FullSpinner />
        ) : (
          isValidHakuOid && (
            <QuerySuspenseBoundary>
              {selectedHakuOid ? (
                <YliajoManagerProvider hakuOid={selectedHakuOid}>
                  <OpiskelijavalintaanSiirtyvatTiedot
                    avainarvoRyhma={avainarvoRyhma}
                    oppijaNumero={oppijaNumero}
                    hakuOid={selectedHakuOid}
                  />
                </YliajoManagerProvider>
              ) : (
                <ResultPlaceholder
                  text={t('opiskelijavalinnan-tiedot.valitse-haku')}
                />
              )}
            </QuerySuspenseBoundary>
          )
        )}
      </Box>
    </Stack>
  ) : (
    <ResultPlaceholder
      icon={<DoNotDisturb />}
      text={t('opiskelijavalinnan-tiedot.ei-kayttooikeutta')}
    />
  );
};

const ErrorFallback = ({
  reset,
  error,
}: {
  reset: () => void;
  error: Error;
}) => {
  const { t } = useTranslations();
  if (error instanceof FetchError) {
    const jsonBody = error.jsonBody;
    if (isGenericBackendErrorResponse(jsonBody)) {
      return (
        <ErrorAlert
          title={t('opiskelijavalinnan-tiedot.virhe-valintadatan-haussa')}
          message={jsonBody.virheAvaimet.map((virhe) => t(virhe))}
        />
      );
    }
  }

  return <ErrorView error={error} reset={reset} />;
};

export default function OpiskelijavalinnanTiedotPage() {
  const { oppijaNumero } = useOutletContext<OppijaContext>();

  queryClient.ensureQueryData(queryOptionsGetOppijanHaut(oppijaNumero));
  const [searchParams] = useSearchParams();
  const hakuOidParam = searchParams.get(HAKU_QUERY_PARAM_NAME);

  if (hakuOidParam) {
    // Aloitetaan valintadatan esilataus
    queryClient.ensureQueryData(
      queryOptionsGetValintadata({
        oppijaNumero,
        hakuOid: hakuOidParam,
      }),
    );
  }

  return (
    <QuerySuspenseBoundary ErrorFallback={ErrorFallback}>
      <OpiskelijavalinnanTiedotPageContent oppijaNumero={oppijaNumero} />
    </QuerySuspenseBoundary>
  );
}
