import { FetchError, useApiSuspenseQuery } from '@/lib/http-client';
import { useTranslations } from '@/hooks/useTranslations';
import { isGenericBackendErrorResponse } from '@/types/ui-types';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { ErrorView } from '@/components/ErrorView';
import { ErrorAlert } from '@/components/ErrorAlert';
import { Box, Stack } from '@mui/material';
import {
  queryOptionsGetOppijanHaut,
  queryOptionsGetValintadata,
  useKayttaja,
} from '@/lib/suorituspalvelu-queries';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { DoNotDisturb } from '@mui/icons-material';
import { OphSelectFormField } from '@opetushallitus/oph-design-system';
import { only } from 'remeda';
import { useQueryParam } from '@/hooks/useQueryParam';
import { queryClient } from '@/lib/queryClient';
import {
  useOutletContext,
  useSearchParams,
  type OppijaContext,
} from 'react-router';
import { HAKU_QUERY_PARAM_NAME } from '@/lib/common';
import { OpiskelijavalinnanTiedotContent } from '@/components/opiskelijavalinnan-tiedot/OpiskelijavalinnanTiedotContent';

const OpiskelijavalinnanTiedotPageContent = ({
  oppijaNumero,
}: {
  oppijaNumero: string;
}) => {
  const { t, translateKielistetty } = useTranslations();

  const { data: kayttaja } = useKayttaja();

  const { data: haut } = useApiSuspenseQuery(
    queryOptionsGetOppijanHaut(oppijaNumero),
  );
  const [urlHakuOid, setUrlHakuOid] = useQueryParam(HAKU_QUERY_PARAM_NAME);

  const onlyHakuOid = only(haut)?.hakuOid;

  // Jos vain yksi haku valittavissa eikä URL-parametrissa valittu, asetetaan ainut haku URL-parametriksi
  if (!urlHakuOid && onlyHakuOid) {
    setUrlHakuOid(onlyHakuOid);
  }

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
            setUrlHakuOid(event.target.value);
          }}
        />
      </Stack>
      <Box sx={{ paddingTop: 1 }}>
        {isValidHakuOid && (
          <QuerySuspenseBoundary key={selectedHakuOid}>
            {selectedHakuOid ? (
              <OpiskelijavalinnanTiedotContent
                oppijaNumero={oppijaNumero}
                hakuOid={selectedHakuOid}
              />
            ) : (
              <ResultPlaceholder
                text={t('opiskelijavalinnan-tiedot.valitse-haku')}
              />
            )}
          </QuerySuspenseBoundary>
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
