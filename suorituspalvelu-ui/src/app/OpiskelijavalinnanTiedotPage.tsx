import { FetchError, useApiSuspenseQuery } from '@/lib/http-client';
import type { Route } from './+types/OpiskelijavalinnanTiedotPage';
import { useTranslations } from '@/hooks/useTranslations';
import { isGenericBackendErrorResponse } from '@/types/ui-types';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { ErrorView } from '@/components/ErrorView';
import { ErrorAlert } from '@/components/ErrorAlert';
import { Box, Stack, ToggleButton, ToggleButtonGroup } from '@mui/material';
import { useState, useTransition } from 'react';
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
import { redirect } from 'react-router';

const HAKU_QUERY_PARAM = 'haku';

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

  const [urlHakuOid, setUrlHakuOid] = useQueryParam(HAKU_QUERY_PARAM);

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

  // Ilman transitiota hakua vaihdettaessa ei tule näkyviin latausindikaattoria
  const [isHakuSwitching, startHakuSwitchTransition] = useTransition();

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
            startHakuSwitchTransition(() => {
              setUrlHakuOid(event.target.value);
            });
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

// Vältetään awaitin käyttöä tässä, koska SPA-moodissa loadereille voi näyttää vain globaalin latausindikaattorin
export async function clientLoader({
  params,
  request,
}: Route.ClientLoaderArgs) {
  const { oppijaNumero } = params;
  if (oppijaNumero) {
    // Aloitetaan oppijan hakujen esilataus
    const hautPromise = queryClient.ensureQueryData(
      queryOptionsGetOppijanHaut(oppijaNumero),
    );

    const url = new URL(request.url);
    const hakuOidParam = url.searchParams.get(HAKU_QUERY_PARAM);

    if (hakuOidParam) {
      // Aloitetaan valindatadatan esilataus
      queryClient.ensureQueryData(
        queryOptionsGetValintadata({
          oppijaNumero,
          hakuOid: hakuOidParam,
        }),
      );
    } else {
      const haut = await hautPromise;
      const onlyHakuOid = only(haut)?.hakuOid;
      // Jos vain yksi haku valittavissa eikä URL-parametrissa valittu, asetetaan ainut haku URL-parametriksi
      if (onlyHakuOid) {
        url.searchParams.set(HAKU_QUERY_PARAM, onlyHakuOid);
        throw redirect(url.search);
      }
    }
  }
}

export default function OpiskelijavalinnanTiedotPage({
  params,
}: Route.ComponentProps) {
  if (!params.oppijaNumero) {
    throw new Error('Ei voida näyttää suoritustietoja ilman oppijanumeroa');
  }
  return (
    <QuerySuspenseBoundary ErrorFallback={ErrorFallback}>
      <OpiskelijavalinnanTiedotPageContent oppijaNumero={params.oppijaNumero} />
    </QuerySuspenseBoundary>
  );
}
