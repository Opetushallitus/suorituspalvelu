import { Stack } from '@mui/material';
import { OphButton } from '@opetushallitus/oph-design-system';
import { useRef, useState } from 'react';
import type { OppijanTiedot, SuoritusFields } from '@/types/ui-types';
import { Add } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import { useMutation } from '@tanstack/react-query';
import { saveSuoritus as saveSuoritusApi } from '@/lib/suorituspalvelu-service';
import { EditSuoritusPaper } from './EditSuoritusPaper';

const createSuoritusFields = (
  base: Partial<SuoritusFields> = {},
): SuoritusFields => {
  return {
    oppijaOid: '',
    oppilaitosOid: '',
    tyyppi: 'perusopetuksenoppimaara',
    tila: 'suorituksentila_valmis',
    suorituskieli: 'FI',
    koulusivistyskieli: 'FI',
    yksilollistetty: '1',
    oppiaineet: [],
    ...base,
  };
};

const useSuoritusState = () => {
  const [suoritusState, setSuoritusState] = useState<SuoritusFields | null>(
    null,
  );

  const suoritusMutation = useMutation({
    mutationFn: async () => {
      if (suoritusState) {
        await saveSuoritusApi({
          oppijaOid: suoritusState.oppijaOid,
          oppilaitosOid: suoritusState.oppilaitosOid,
          tyyppi: suoritusState.tyyppi,
          suorituskieli: suoritusState.suorituskieli,
          yksilollistetty: suoritusState.yksilollistetty,
          valmistumispaiva: suoritusState.valmistumispaiva,
          oppiaineet: suoritusState.oppiaineet,
        });
      }
    },
    onError: (error) => {
      console.error('Suorituksen tallennus epäonnistui:', error);
      alert('Suorituksen tallennus epäonnistui.');
    },
    onSuccess: () => {
      alert('Suoritus tallennettu onnistuneesti.');
      setSuoritusState(null);
    },
  });

  return {
    suoritus: suoritusState,
    setSuoritus: setSuoritusState,
    suoritusMutation,
  };
};

export const AddSuoritusFields = ({
  oppijanTiedot,
}: {
  oppijanTiedot: OppijanTiedot;
}) => {
  const { t } = useTranslations();

  const suoritusPaperRef = useRef<HTMLDivElement | null>(null);

  const { suoritus, setSuoritus, suoritusMutation } = useSuoritusState();

  return (
    <>
      <Stack
        direction="row"
        sx={{ justifyContent: 'flex-start', marginTop: 2, marginBottom: 2 }}
      >
        <OphButton
          variant="outlined"
          startIcon={<Add />}
          onClick={() => {
            if (!suoritus) {
              setSuoritus(
                createSuoritusFields({
                  oppijaOid: oppijanTiedot.henkiloOID,
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
      </Stack>
      <EditSuoritusPaper
        suoritus={suoritus}
        ref={suoritusPaperRef}
        setSuoritus={setSuoritus}
        onSave={() => {
          suoritusMutation.mutate();
        }}
      />
    </>
  );
};
