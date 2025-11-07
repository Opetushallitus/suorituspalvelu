import { Box } from '@mui/material';
import { useTranslations } from '@/hooks/useTranslations';
import { FetchError } from '@/lib/http-client';
import { isGenericBackendErrorResponse } from '@/types/ui-types';
import { OphModal } from '@/components/OphModal';
import React from 'react';

export const YliajoErrorModal = ({
  error,
  operation,
  onClose,
}: {
  error: Error;
  operation: 'save' | 'delete';
  onClose: () => void;
}) => {
  const { t } = useTranslations();

  let message: React.ReactNode = error.message;

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
        operation === 'save'
          ? t('opiskelijavalinnan-tiedot.yliajon-tallennus-epaonnistui')
          : t('opiskelijavalinnan-tiedot.yliajon-poisto-epaonnistui')
      }
    >
      {message}
    </OphModal>
  );
};
