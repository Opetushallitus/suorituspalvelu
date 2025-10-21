import { Stack } from '@mui/material';
import { useRef } from 'react';
import { useTranslations } from '@/hooks/useTranslations';
import { EditSuoritusPaper } from './EditSuoritusPaper';
import { useSuoritusState } from '@/hooks/useSuoritusState';
import type {
  PerusopetuksenOppiaineenOppimaara,
  PerusopetuksenOppimaara,
  SuoritusFields,
} from '@/types/ui-types';
import { OphButton } from '@opetushallitus/oph-design-system';
import { PerusopetusSuoritusReadOnlyPaper } from './PerusopetusSuoritusReadOnlyPaper';
import { OphModal } from '../OphModal';
import { useQueryClient } from '@tanstack/react-query';
import { queryOptionsGetOppija } from '@/lib/suorituspalvelu-queries';
import { FullSpinner } from '../FullSpinner';

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
    koulusivistyskieli: suoritus.suorituskieli, //TODO: Vaihda koulusivistyskieleen, kun se palautuu backendistä
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

export const EditStatusModal = ({
  status,
  message,
  onClose,
}: {
  status?: 'pending' | 'error' | 'success' | 'idle';
  message?: string;
  onClose: () => void;
}) => {
  const { t } = useTranslations();

  switch (status) {
    case 'error':
      return (
        <OphModal
          open={true}
          title={t('muokkaus.suoritus.tallennus-epaonnistui')}
          onClose={onClose}
        >
          {message ? t(message) : ''}
        </OphModal>
      );
    case 'pending':
      return (
        <OphModal title={t('muokkaus.suoritus.tallennetaan')} open={true}>
          <FullSpinner />
        </OphModal>
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
  const { suoritus, setSuoritus, suoritusMutation } = useSuoritusState(
    suoritusProp.versioTunniste ?? suoritusProp.tunniste,
    {
      onSuccess: () => {
        queryClient.invalidateQueries(queryOptionsGetOppija(henkiloOID));
        queryClient.refetchQueries(queryOptionsGetOppija(henkiloOID));
        setSuoritus(null);
      },
    },
  );

  return (
    <Stack
      direction="column"
      spacing={2}
      sx={{ alignItems: 'flex-start', marginBottom: 2 }}
    >
      <EditStatusModal
        status={suoritusMutation.status}
        onClose={() => suoritusMutation.reset()}
      />
      {suoritus ? (
        <EditSuoritusPaper
          suoritus={suoritus}
          ref={suoritusPaperRef}
          setSuoritus={setSuoritus}
          onSave={() => {
            suoritusMutation.mutate('save');
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
                  suoritusMutation.mutate('delete');
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
  );
};
