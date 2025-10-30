import { FullSpinner } from '@/components/FullSpinner';
import { OphModal } from '@/components/OphModal';
import {
  isGenericBackendError,
  isPerusopetusOppimaaraBackendError,
} from '@/types/ui-types';
import { Alert, Box, Snackbar, type SnackbarCloseReason } from '@mui/material';
import React, { useState } from 'react';
import { useTranslations } from '../hooks/useTranslations';
import { FetchError, useApiSuspenseQuery } from '@/lib/http-client';
import type {
  SuoritusMutationResult,
  SuoritusMutationOperation,
} from '@/lib/suoritusManager';
import { queryOptionsGetSuoritusvaihtoehdot } from '@/lib/suorituspalvelu-queries';
import { translateKielistetty } from '@/lib/translation-utils';

const SuoritusMutationErrorModal = ({
  isSaving,
  error,
  onClose,
}: {
  isSaving: boolean;
  error?: Error | null;
  onClose: () => void;
}) => {
  const { t } = useTranslations();

  const { data: suoritusvaihtoehdot } = useApiSuspenseQuery(
    queryOptionsGetSuoritusvaihtoehdot(),
  );

  const { oppiaineet } = suoritusvaihtoehdot;

  let message: React.ReactNode = error?.message;

  if (error instanceof FetchError) {
    const responseJSON = error.jsonBody;
    if (isGenericBackendError(responseJSON)) {
      message = (
        <Box>
          {responseJSON.virheAvaimet.map((virhe) => (
            <p key={virhe}>{t(virhe)}</p>
          ))}
        </Box>
      );
    } else if (isPerusopetusOppimaaraBackendError(responseJSON)) {
      message = (
        <Box>
          <Box>
            {responseJSON.yleisetVirheAvaimet.map((yleinenVirhe) => (
              <p key={yleinenVirhe}>{t(yleinenVirhe)}</p>
            ))}
          </Box>
          {responseJSON.oppiaineKohtaisetVirheet.map((virhe) => {
            const oppiaineErrorsLabelId = `oppiaine-virheet-label-${virhe.oppiaineKoodiArvo}`;
            const oppiaine = oppiaineet.find(
              (oa) => oa.arvo === virhe.oppiaineKoodiArvo,
            );

            const oppiaineNimi = oppiaine
              ? translateKielistetty(oppiaine.nimi)
              : virhe.oppiaineKoodiArvo;

            return (
              <Box
                key={virhe.oppiaineKoodiArvo}
                sx={{ marginBottom: 2 }}
                aria-labelledby={oppiaineErrorsLabelId}
              >
                <h4 id={oppiaineErrorsLabelId}>{oppiaineNimi}</h4>
                {virhe.virheAvaimet.map((v) => (
                  <p key={v}>{t(v)}</p>
                ))}
              </Box>
            );
          })}
        </Box>
      );
    }
  }

  return (
    <OphModal
      open={true}
      onClose={onClose}
      title={
        isSaving
          ? t('muokkaus.suoritus.tallennus-epaonnistui')
          : t('muokkaus.suoritus.poisto-epaonnistui')
      }
    >
      {message}
    </OphModal>
  );
};

export const SuoritusMutationStatusIndicator = ({
  operation,
  mutation,
}: {
  mutation: SuoritusMutationResult;
  operation: SuoritusMutationOperation | null;
}) => {
  const { t } = useTranslations();
  const { status, error } = mutation;

  const [isOpen, setIsOpen] = useState<boolean>(true);
  const [prevStatus, setPrevStatus] = useState(status);

  if (status !== prevStatus) {
    setPrevStatus(status);
    if (status !== 'idle') {
      setIsOpen(true);
    }
  }

  const handleSuccessToastClose = (
    _event?: React.SyntheticEvent | Event,
    reason?: SnackbarCloseReason,
  ) => {
    if (reason === 'clickaway') {
      return;
    }

    setIsOpen(false);
  };
  const isSaving = operation === 'save';

  switch (status) {
    case 'error':
      return (
        <SuoritusMutationErrorModal
          isSaving={isSaving}
          error={error}
          onClose={() => {
            mutation.reset();
          }}
        />
      );
    case 'pending':
      return (
        <OphModal
          title={
            isSaving
              ? t('muokkaus.suoritus.tallennetaan')
              : t('muokkaus.suoritus.poistetaan')
          }
          open={true}
        >
          <FullSpinner />
        </OphModal>
      );
    case 'success':
      return (
        <Snackbar
          key={operation}
          open={isOpen}
          onClose={handleSuccessToastClose}
          autoHideDuration={5000}
          anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        >
          <Alert severity="success" variant="filled">
            {isSaving
              ? t('muokkaus.suoritus.tallennus-onnistui')
              : t('muokkaus.suoritus.poisto-onnistui')}
          </Alert>
        </Snackbar>
      );
    default:
      return null;
  }
};
