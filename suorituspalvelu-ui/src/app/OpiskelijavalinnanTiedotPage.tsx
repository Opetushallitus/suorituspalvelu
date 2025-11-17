import { FetchError, useApiSuspenseQuery } from '@/lib/http-client';
import type { Route } from './+types/OpiskelijavalinnanTiedotPage';
import { useTranslations } from '@/hooks/useTranslations';
import { isGenericBackendErrorResponse } from '@/types/ui-types';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { ErrorView } from '@/components/ErrorView';
import { ErrorAlert } from '@/components/ErrorAlert';
import { Stack, ToggleButton, ToggleButtonGroup } from '@mui/material';
import { useState } from 'react';
import {
  OpiskelijavalintaanSiirtyvatTiedot,
  type AvainarvoRyhma,
} from '@/components/opiskelijavalinnan-tiedot/OpiskelijavalintaanSiirtyvatTiedot';
import {
  queryOptionsGetOppijanHaut,
  useKayttaja,
} from '@/lib/suorituspalvelu-queries';
import { ResultPlaceholder } from '@/components/ResultPlaceholder';
import { DoNotDisturb } from '@mui/icons-material';
import { YliajoManagerProvider } from '@/lib/yliajoManager';
import {
  OphFormFieldWrapper,
  OphSelectFormField,
} from '@opetushallitus/oph-design-system';
import { first } from 'remeda';

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

  const hakuOptions = haut?.map((haku) => ({
    value: haku.hakuOid,
    label: translateKielistetty(haku.nimi),
  }));

  const firstHakuOid = first(haut)?.hakuOid;

  const [selectedHakuOid, setSelectedHakuOid] = useState<string>(
    firstHakuOid ?? '',
  );

  return kayttaja.isRekisterinpitaja ? (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ justifyContent: 'stretch', gap: 3 }}>
        <OphSelectFormField
          sx={{ flex: 1 }}
          label={t('opiskelijavalinnan-tiedot.haku')}
          value={selectedHakuOid}
          options={hakuOptions}
          onChange={(event) => setSelectedHakuOid(event.target.value)}
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
      {selectedHakuOid ? (
        <YliajoManagerProvider hakuOid={selectedHakuOid}>
          <OpiskelijavalintaanSiirtyvatTiedot
            avainarvoRyhma={avainarvoRyhma}
            oppijaNumero={oppijaNumero}
            hakuOid={selectedHakuOid}
          />
        </YliajoManagerProvider>
      ) : (
        <p>Valitse haku</p>
      )}
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

export default function OpiskelijavalinnanTiedotPage({
  params,
}: Route.ComponentProps) {
  return (
    <QuerySuspenseBoundary ErrorFallback={ErrorFallback}>
      <OpiskelijavalinnanTiedotPageContent oppijaNumero={params.oppijaNumero} />
    </QuerySuspenseBoundary>
  );
}
