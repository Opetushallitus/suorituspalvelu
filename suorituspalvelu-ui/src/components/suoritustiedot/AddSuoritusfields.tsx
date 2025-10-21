import { Stack } from '@mui/material';
import { OphButton } from '@opetushallitus/oph-design-system';
import { useRef } from 'react';
import type { SuoritusFields } from '@/types/ui-types';
import { Add } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import { EditSuoritusPaper } from './EditSuoritusPaper';
import { useSuoritusState } from '@/hooks/useSuoritusState';
import { queryOptionsGetOppija } from '@/lib/suorituspalvelu-queries';
import { useQueryClient } from '@tanstack/react-query';
import { MutationStatusIndicator } from './PerusopetusSuoritusEditablePaper';

const createSuoritusFields = (
  base: Partial<SuoritusFields> = {},
): SuoritusFields => {
  return {
    versioTunniste: '',
    oppijaOid: '',
    oppilaitosOid: '',
    tyyppi: 'perusopetuksenoppimaara',
    tila: 'suorituksentila_valmis',
    suorituskieli: 'FI',
    yksilollistetty: '1',
    oppiaineet: [],
    ...base,
  };
};

export const AddSuoritusFields = ({ henkiloOID }: { henkiloOID: string }) => {
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
    <Stack
      direction="column"
      spacing={2}
      sx={{ alignItems: 'flex-start', marginBottom: 2 }}
    >
      <MutationStatusIndicator
        mode={mode}
        status={suoritusMutation.status}
        error={suoritusMutation.error}
        onClose={() => suoritusMutation.reset()}
      />
      <OphButton
        variant="outlined"
        startIcon={<Add />}
        onClick={() => {
          if (!suoritus) {
            setSuoritus(
              createSuoritusFields({
                oppijaOid: henkiloOID,
              }),
            );
          }
          setTimeout(() => {
            suoritusPaperRef.current?.scrollIntoView({
              behavior: 'smooth',
            });
          }, 20);
        }}
      >
        {t('muokkaus.suoritus.lisaa')}
      </OphButton>
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
    </Stack>
  );
};
