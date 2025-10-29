import { Stack } from '@mui/material';
import { OphButton } from '@opetushallitus/oph-design-system';
import { useRef, useState } from 'react';
import type { OppijanTiedot, SuoritusFields } from '@/types/ui-types';
import { Add } from '@mui/icons-material';
import { useTranslations } from '@/hooks/useTranslations';
import { QuerySuspenseBoundary } from '@/components/QuerySuspenseBoundary';
import { useMutation } from '@tanstack/react-query';
import { saveSuoritus } from '@/lib/suorituspalvelu-service';
import { EditSuoritusPaper, EMPTY_SUORITUS } from './EditSuoritusPaper';

export const AddSuoritusFields = ({
  oppijanTiedot,
}: {
  oppijanTiedot: OppijanTiedot;
}) => {
  const { t } = useTranslations();

  const [editableSuoritus, setEditableSuoritus] =
    useState<SuoritusFields | null>(null);

  const editableSuoritusRef = useRef<HTMLDivElement | null>(null);

  const suoritusMutation = useMutation({
    mutationFn: async (suoritus: SuoritusFields) => {
      console.log('Tallennetaan suoritus:', suoritus);
      await saveSuoritus({
        oppijaOid: oppijanTiedot.henkiloOID,
        oppilaitosOid: suoritus.oppilaitosOid || '',
        tyyppi: suoritus.tyyppi,
        suorituskieli: suoritus.suorituskieli,
        yksilollistetty: suoritus.yksilollistetty,
        valmistumispaiva: suoritus.valmistumispaiva,
        oppiaineet: [],
      });
    },
    onError: (error) => {
      console.error('Suorituksen tallennus epäonnistui:', error);
    },
  });

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
            if (!editableSuoritus) {
              setEditableSuoritus(EMPTY_SUORITUS);
            }
            setTimeout(() => {
              editableSuoritusRef.current?.scrollIntoView({
                behavior: 'smooth',
              });
            }, 20);
          }}
        >
          {t('muokkaus.suoritus.lisaa')}
        </OphButton>
      </Stack>
      <QuerySuspenseBoundary>
        {editableSuoritus && (
          <EditSuoritusPaper
            suoritus={editableSuoritus}
            ref={editableSuoritusRef}
            onSave={(data) => {
              suoritusMutation.mutate(data);
            }}
            onDelete={() => {
              setEditableSuoritus(null);
            }}
          />
        )}
      </QuerySuspenseBoundary>
    </>
  );
};
