import { FullSpinner } from '@/components/FullSpinner';
import { OphModal } from '@/components/OphModal';
import {
  isGenericBackendError,
  isPerusopetusOppimaaraBackendError,
} from '@/types/ui-types';
import { Alert, Box, Snackbar, type SnackbarCloseReason } from '@mui/material';
import React, { useState } from 'react';
import { useTranslations } from '../hooks/useTranslations';
import { FetchError } from '@/lib/http-client';
import type {
  SuoritusMutationResult,
  SuoritusOperation,
} from '@/lib/suoritusManager';

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
          {responseJSON.yleisetVirheAvaimet.map((yleinenVirhe) => (
            <p key={yleinenVirhe}>{t(yleinenVirhe)}</p>
          ))}
          {responseJSON.oppiaineKohtaisetVirheet.map((virhe) => (
            <Box key={virhe.oppiaineKoodiArvo} sx={{ marginBottom: 2 }}>
              <h4>{virhe.oppiaineKoodiArvo}</h4>
              {virhe.virheAvaimet.map((v) => (
                <p key={v}>{t(v)}</p>
              ))}
            </Box>
          ))}
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
  mode,
  mutation,
}: {
  mutation: SuoritusMutationResult;
  mode: SuoritusOperation;
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
  const isSaving = mode === 'add' || mode === 'edit';

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
          key={mode}
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
