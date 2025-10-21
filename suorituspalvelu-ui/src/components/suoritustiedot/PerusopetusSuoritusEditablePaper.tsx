import {
  Alert,
  Box,
  Snackbar,
  Stack,
  type SnackbarCloseReason,
} from '@mui/material';
import { useRef, useState } from 'react';
import { useTranslations } from '@/hooks/useTranslations';
import { EditSuoritusPaper } from './EditSuoritusPaper';
import { useSuoritusState } from '@/hooks/useSuoritusState';
import {
  isGenericBackendError,
  isPerusopetusOppimaaraBackendError,
  type PerusopetuksenOppiaineenOppimaara,
  type PerusopetuksenOppimaara,
  type SuoritusFields,
} from '@/types/ui-types';
import { OphButton } from '@opetushallitus/oph-design-system';
import { PerusopetusSuoritusReadOnlyPaper } from './PerusopetusSuoritusReadOnlyPaper';
import { OphModal } from '../OphModal';
import { useQueryClient } from '@tanstack/react-query';
import { queryOptionsGetOppija } from '@/lib/suorituspalvelu-queries';
import { FullSpinner } from '../FullSpinner';
import { FetchError } from '@/lib/http-client';

const createSuoritusFields = ({
  oppijaOid,
  suoritus,
}: {
  oppijaOid: string;
  suoritus: PerusopetuksenOppimaara | PerusopetuksenOppiaineenOppimaara;
}): SuoritusFields => {
  return {
    versioTunniste:
      'versioTunniste' in suoritus && suoritus.versioTunniste
        ? suoritus.versioTunniste
        : suoritus.tunniste,
    oppijaOid,
    oppilaitosOid: suoritus.oppilaitos.oid,
    tila: suoritus.tila,
    tyyppi: suoritus.suoritustyyppi,
    valmistumispaiva: suoritus.valmistumispaiva
      ? new Date(suoritus.valmistumispaiva)
      : undefined,
    suorituskieli: suoritus.suorituskieli,
    yksilollistetty:
      'yksilollistaminen' in suoritus
        ? (suoritus.yksilollistaminen?.arvo ?? '').toString()
        : '',
    oppiaineet:
      'oppiaineet' in suoritus
        ? suoritus.oppiaineet.map((oa) => ({
            koodi: oa.koodi ?? '',
            kieli: oa.kieli,
            arvosana: oa.arvosana,
            valinnaisetArvosanat: oa.valinnaisetArvosanat,
          }))
        : [],
  };
};

const ErrorModal = ({
  mode,
  error,
  onClose,
}: {
  mode: 'save' | 'delete';
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
        mode === 'save'
          ? t('muokkaus.suoritus.tallennus-epaonnistui')
          : t('muokkaus.suoritus.poisto-epaonnistui')
      }
    >
      {message}
    </OphModal>
  );
};

export const MutationStatusIndicator = ({
  status,
  onClose,
  mode,
  error,
}: {
  status?: 'pending' | 'error' | 'success' | 'idle';
  onClose: () => void;
  error?: Error | null;
  mode: 'save' | 'delete';
}) => {
  const { t } = useTranslations();

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

  switch (status) {
    case 'error':
      return <ErrorModal mode={mode} error={error} onClose={onClose} />;
    case 'pending':
      return (
        <OphModal
          title={
            mode === 'save'
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
            {mode === 'save'
              ? t('muokkaus.suoritus.tallennus-onnistui')
              : t('muokkaus.suoritus.poisto-onnistui')}
          </Alert>
        </Snackbar>
      );
    default:
      return null;
  }
};

export const PerusopetusSuoritusEditablePaper = ({
  henkiloOID,
  suoritus: suoritusProp,
}: {
  henkiloOID: string;
  suoritus: PerusopetuksenOppimaara | PerusopetuksenOppiaineenOppimaara;
}) => {
  const { t } = useTranslations();
  const suoritusPaperRef = useRef<HTMLDivElement | null>(null);

  const queryClient = useQueryClient();
  const { suoritus, setSuoritus, suoritusMutation, mode } = useSuoritusState({
    onSuccess: () => {
      queryClient.invalidateQueries(queryOptionsGetOppija(henkiloOID));
      queryClient.refetchQueries(queryOptionsGetOppija(henkiloOID));
      setSuoritus(null);
    },
  });

  return (
    <Box>
      <MutationStatusIndicator
        status={suoritusMutation.status}
        onClose={() => suoritusMutation.reset()}
        error={suoritusMutation.error}
        mode={mode}
      />
      <Stack
        direction="column"
        spacing={2}
        sx={{ alignItems: 'flex-start', marginBottom: 2 }}
      >
        {suoritus ? (
          <EditSuoritusPaper
            suoritus={suoritus}
            ref={suoritusPaperRef}
            setSuoritus={setSuoritus}
            onSave={() => {
              suoritusMutation.mutate({ operation: 'save' });
            }}
            onCancel={() => {
              setSuoritus(null);
            }}
          />
        ) : (
          <PerusopetusSuoritusReadOnlyPaper
            suoritus={suoritusProp}
            actions={
              <Stack
                direction="row"
                spacing={2}
                sx={{ justifyContent: 'flex-end' }}
              >
                <OphButton
                  variant="outlined"
                  onClick={() => {
                    suoritusMutation.mutate({
                      operation: 'delete',
                      versioTunniste: suoritusProp.versioTunniste,
                    });
                  }}
                >
                  {t('muokkaus.suoritus.poista')}
                </OphButton>
                <OphButton
                  variant="contained"
                  onClick={() => {
                    setSuoritus(
                      createSuoritusFields({
                        oppijaOid: henkiloOID,
                        suoritus: suoritusProp,
                      }),
                    );
                  }}
                >
                  {t('muokkaus.suoritus.muokkaa')}
                </OphButton>
              </Stack>
            }
          />
        )}
      </Stack>
    </Box>
  );
};
