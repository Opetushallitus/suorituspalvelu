import { FetchError } from '@/lib/http-client';
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

export const OpiskelijavalinnanTiedotPageContent = ({
  oppijaNumero,
}: {
  oppijaNumero: string;
}) => {
  const { t } = useTranslations();

  const [avainarvoRyhma, setAvainarvoRyhma] =
    useState<AvainarvoRyhma>('uudet-avainarvot');

  return (
    <Stack spacing={3}>
      <ToggleButtonGroup
        sx={{ alignSelf: 'flex-end' }}
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
      <OpiskelijavalintaanSiirtyvatTiedot
        avainarvoRyhma={avainarvoRyhma}
        oppijaNumero={oppijaNumero}
      />
    </Stack>
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
