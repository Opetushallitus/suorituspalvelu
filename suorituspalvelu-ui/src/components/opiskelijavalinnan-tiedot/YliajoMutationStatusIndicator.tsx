import { FullSpinner } from '@/components/FullSpinner';
import { OphModal } from '@/components/OphModal';
import { isGenericBackendErrorResponse } from '@/types/ui-types';
import { Alert, Box, Snackbar, type SnackbarCloseReason } from '@mui/material';
import React, { useState } from 'react';
import { useTranslations } from '@/hooks/useTranslations';
import { FetchError } from '@/lib/http-client';
import type {
  YliajoMutationResult,
  YliajoMutationOperation,
} from '@/lib/yliajoManager';

const YliajoMutationErrorModal = ({
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
    if (isGenericBackendErrorResponse(responseJSON)) {
      message = (
        <Box>
          {responseJSON.virheAvaimet.map((virhe) => (
            <p key={virhe}>{t(virhe)}</p>
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
          ? t('opiskelijavalinnan-tiedot.yliajon-tallennus-epaonnistui')
          : t('opiskelijavalinnan-tiedot.yliajon-poisto-epaonnistui')
      }
    >
      {message}
    </OphModal>
  );
};

export const YliajoMutationStatusIndicator = ({
  operation,
  mutation,
}: {
  mutation: YliajoMutationResult;
  operation: YliajoMutationOperation | null;
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
        <YliajoMutationErrorModal
          isSaving={isSaving}
          error={error}
          onClose={mutation.reset}
        />
      );
    case 'pending':
      return (
        <OphModal
          title={
            isSaving
              ? t('opiskelijavalinnan-tiedot.tallennetaan-yliajoa')
              : t('opiskelijavalinnan-tiedot.poistetaan-yliajoa')
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
              ? t('opiskelijavalinnan-tiedot.yliajon-tallennus-onnistui')
              : t('opiskelijavalinnan-tiedot.yliajon-poisto-onnistui')}
          </Alert>
        </Snackbar>
      );
    default:
      return null;
  }
};
